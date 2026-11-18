package hopper.pfm.insect.core.data

import androidx.room.withTransaction
import hopper.pfm.insect.core.database.HabitDao
import hopper.pfm.insect.core.database.HabitEntity
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.domain.habit.HabitRepository
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.model.Habit
import hopper.pfm.insect.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private fun HabitEntity.toModel() = Habit(
    id = id,
    title = title,
    note = note,
    amount = amount,
    categoryId = categoryId,
    daysOfWeek = daysOfWeek.split(',').filter { it.isNotBlank() }.map { it.trim().toInt() }.toSet(),
    timeOfDay = timeOfDay,
    sortOrder = sortOrder,
    syncStatus = when {
        failure != null -> SyncStatus.FAILED
        dirty -> SyncStatus.PENDING
        else -> SyncStatus.SYNCED
    },
)

/** Same offline-first write rules as the expenses: one transaction, dirty + localRev, then ask for a sync. */
@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitDao,
    private val db: PfmDatabase,
    private val sync: SyncRepository,
) : HabitRepository {

    override fun observeAll(): Flow<List<Habit>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun get(id: String): Habit? = dao.get(id)?.takeIf { !it.deleted }?.toModel()

    override suspend fun save(habit: Habit) {
        db.withTransaction {
            val existing = dao.get(habit.id)
            val order = when {
                habit.sortOrder >= 0 -> habit.sortOrder
                existing != null -> existing.sortOrder
                else -> (dao.listLive().maxOfOrNull { it.sortOrder } ?: -1) + 1
            }
            dao.upsert(
                HabitEntity(
                    id = habit.id,
                    title = habit.title.trim(),
                    note = habit.note?.trim()?.takeIf { it.isNotEmpty() },
                    amount = habit.amount,
                    categoryId = habit.categoryId,
                    daysOfWeek = habit.daysOfWeek.sorted().joinToString(","),
                    timeOfDay = habit.timeOfDay,
                    sortOrder = order,
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

    override suspend fun move(id: String, direction: Int) {
        db.withTransaction {
            val list = dao.listLive()
            val index = list.indexOfFirst { it.id == id }
            val other = index + direction
            if (index < 0 || other !in list.indices) return@withTransaction
            // Renumber the whole list so equal sort orders (from other devices) cannot make the swap a no-op.
            val reordered = list.toMutableList().apply { add(other, removeAt(index)) }
            reordered.forEachIndexed { position, row ->
                if (row.sortOrder != position) {
                    dao.upsert(row.copy(sortOrder = position, dirty = true, localRev = row.localRev + 1, failure = null))
                }
            }
        }
        sync.requestSync()
    }
}
