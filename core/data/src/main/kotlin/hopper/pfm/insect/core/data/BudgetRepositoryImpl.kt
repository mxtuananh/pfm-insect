package hopper.pfm.insect.core.data

import androidx.room.withTransaction
import hopper.pfm.insect.core.database.BudgetDao
import hopper.pfm.insect.core.database.BudgetEntity
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.domain.budget.BudgetRepository
import hopper.pfm.insect.core.domain.budget.DEFAULT_ALERT_THRESHOLDS
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.model.Budget
import hopper.pfm.insect.core.model.BudgetPeriod
import hopper.pfm.insect.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** The server and the database use the lower case keys "day", "month" and "year". */
private fun BudgetPeriod.key(): String = name.lowercase()

private fun periodOf(key: String): BudgetPeriod = BudgetPeriod.entries.first { it.key() == key }

private fun BudgetEntity.toModel() = Budget(
    id = id,
    categoryId = categoryId,
    period = periodOf(period),
    amount = amount,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    recurring = recurring,
    alertThresholds = alertThresholds.split(',').filter { it.isNotBlank() }.map { it.trim().toInt() }
        .ifEmpty { DEFAULT_ALERT_THRESHOLDS },
    syncStatus = when {
        failure != null -> SyncStatus.FAILED
        dirty -> SyncStatus.PENDING
        else -> SyncStatus.SYNCED
    },
)

/** Same offline-first write rules as expenses and habits: one transaction, dirty + localRev, then a sync request. */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao,
    private val db: PfmDatabase,
    private val sync: SyncRepository,
) : BudgetRepository {

    override fun observeAll(): Flow<List<Budget>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun get(id: String): Budget? = dao.get(id)?.takeIf { !it.deleted }?.toModel()

    override suspend fun save(budget: Budget) {
        db.withTransaction {
            val existing = dao.get(budget.id)
            dao.upsert(
                BudgetEntity(
                    id = budget.id,
                    categoryId = budget.categoryId,
                    period = budget.period.key(),
                    amount = budget.amount,
                    startDate = budget.startDate.toString(),
                    // The server only accepts an end date for repeating budgets.
                    endDate = budget.endDate?.takeIf { budget.recurring }?.toString(),
                    recurring = budget.recurring,
                    alertThresholds = budget.alertThresholds.distinct().sorted().joinToString(","),
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
