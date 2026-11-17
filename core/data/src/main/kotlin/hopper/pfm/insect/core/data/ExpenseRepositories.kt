package hopper.pfm.insect.core.data

import androidx.room.withTransaction
import hopper.pfm.insect.core.database.CategoryDao
import hopper.pfm.insect.core.database.CategoryEntity
import hopper.pfm.insect.core.database.ExpenseDao
import hopper.pfm.insect.core.database.ExpenseEntity
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.expense.ExpenseRepository
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private fun syncStatusOf(dirty: Boolean, failure: String?) = when {
    failure != null -> SyncStatus.FAILED
    dirty -> SyncStatus.PENDING
    else -> SyncStatus.SYNCED
}

private fun ExpenseEntity.toModel() = Expense(
    id = id, title = title, note = note, amount = amount, spentOn = LocalDate.parse(spentOn),
    categoryId = categoryId, syncStatus = syncStatusOf(dirty, failure),
)

private fun CategoryEntity.toModel() = Category(
    id = id, name = name, icon = icon, color = color, archived = archived, syncStatus = syncStatusOf(dirty, failure),
)

/**
 * Local writes follow the offline-first contract: in one transaction the row is written with dirty = true and a higher
 * localRev (so a push that finishes later cannot clear a newer edit) and any earlier rejection is cleared; then the
 * sync service is asked to run. The network is never involved here.
 */
@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao,
    private val db: PfmDatabase,
    private val sync: SyncRepository,
) : ExpenseRepository {

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Expense>> =
        dao.observeBetween(from.toString(), to.toString()).map { rows -> rows.map { it.toModel() } }

    override suspend fun get(id: String): Expense? = dao.get(id)?.takeIf { !it.deleted }?.toModel()

    override suspend fun save(expense: Expense) {
        db.withTransaction {
            val existing = dao.get(expense.id)
            dao.upsert(
                ExpenseEntity(
                    id = expense.id,
                    title = expense.title.trim(),
                    note = expense.note?.trim()?.takeIf { it.isNotEmpty() },
                    amount = expense.amount,
                    spentOn = expense.spentOn.toString(),
                    categoryId = expense.categoryId,
                    version = existing?.version ?: 0,
                    deleted = false,
                    dirty = true,
                    localRev = (existing?.localRev ?: 0) + 1,
                    failure = null,
                ),
            )
        }
        sync.requestSync()
    }

    override suspend fun delete(id: String) {
        db.withTransaction {
            val existing = dao.get(id) ?: return@withTransaction
            dao.upsert(existing.copy(deleted = true, dirty = true, localRev = existing.localRev + 1, failure = null))
        }
        sync.requestSync()
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
    private val db: PfmDatabase,
    private val sync: SyncRepository,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun get(id: String): Category? = dao.get(id)?.takeIf { !it.deleted }?.toModel()

    override suspend fun save(category: Category) {
        db.withTransaction {
            val existing = dao.get(category.id)
            dao.upsert(
                CategoryEntity(
                    id = category.id,
                    name = category.name.trim(),
                    icon = category.icon,
                    color = category.color,
                    archived = category.archived,
                    version = existing?.version ?: 0,
                    deleted = false,
                    dirty = true,
                    localRev = (existing?.localRev ?: 0) + 1,
                    failure = null,
                ),
            )
        }
        sync.requestSync()
    }
}
