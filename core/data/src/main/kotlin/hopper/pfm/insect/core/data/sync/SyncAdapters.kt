package hopper.pfm.insect.core.data.sync

import androidx.room.withTransaction
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.common.map
import hopper.pfm.insect.core.database.BudgetDao
import hopper.pfm.insect.core.database.BudgetEntity
import hopper.pfm.insect.core.database.CategoryDao
import hopper.pfm.insect.core.database.CategoryEntity
import hopper.pfm.insect.core.database.ExpenseDao
import hopper.pfm.insect.core.database.ExpenseEntity
import hopper.pfm.insect.core.database.HabitDao
import hopper.pfm.insect.core.database.HabitEntity
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.database.SyncCursorDao
import hopper.pfm.insect.core.database.SyncCursorEntity
import hopper.pfm.insect.core.database.SyncDao
import hopper.pfm.insect.core.domain.sync.ChangesPage
import hopper.pfm.insect.core.domain.sync.InitialLoad
import hopper.pfm.insect.core.domain.sync.LocalMeta
import hopper.pfm.insect.core.domain.sync.RemoteItem
import hopper.pfm.insect.core.domain.sync.SyncAdapter
import hopper.pfm.insect.core.network.BudgetApi
import hopper.pfm.insect.core.network.BudgetDto
import hopper.pfm.insect.core.network.CategoryApi
import hopper.pfm.insect.core.network.CategoryDto
import hopper.pfm.insect.core.network.ExpenseApi
import hopper.pfm.insect.core.network.ExpenseDto
import hopper.pfm.insect.core.network.HabitApi
import hopper.pfm.insect.core.network.HabitDto
import hopper.pfm.insect.core.network.UpsertBudgetRequest
import hopper.pfm.insect.core.network.UpsertCategoryRequest
import hopper.pfm.insect.core.network.UpsertExpenseRequest
import hopper.pfm.insect.core.network.UpsertHabitRequest

/** Parts every resource shares: the sync columns, the cursor and the transaction. */
internal abstract class RoomSyncAdapter<T>(
    final override val name: String,
    private val dao: SyncDao,
    private val cursors: SyncCursorDao,
    private val db: PfmDatabase,
) : SyncAdapter<T> {

    override suspend fun dirtyRecords() = dao.dirtyMeta().map { it.toMeta() }

    override suspend fun meta(id: String) = dao.meta(id)?.toMeta()

    override suspend fun markPushed(id: String, version: Long, rev: Long) {
        db.withTransaction {
            dao.markPushed(id, version, rev)
            dao.removeIfDeletedAndClean(id)
        }
    }

    override suspend fun markFailed(id: String, code: String) = dao.markFailed(id, code)

    override suspend fun cursor(): String? = cursors.get(name)

    override suspend fun saveCursor(cursor: String) = cursors.set(SyncCursorEntity(name, cursor))

    override suspend fun clearNonDirty() = dao.clearClean()

    override suspend fun <R> transaction(block: suspend () -> R): R = db.withTransaction { block() }

    private fun hopper.pfm.insect.core.database.SyncMetaRow.toMeta() =
        LocalMeta(id, version, deleted, dirty, localRev, failure)
}

internal class CategorySyncAdapter(
    private val dao: CategoryDao,
    private val api: CategoryApi,
    cursors: SyncCursorDao,
    db: PfmDatabase,
) : RoomSyncAdapter<CategoryEntity>("categories", dao, cursors, db) {

    override suspend fun push(meta: LocalMeta): Outcome<Long> {
        val row = dao.get(meta.id) ?: return Outcome.Success(meta.version)
        return api.put(row.id, UpsertCategoryRequest(row.name, row.icon, row.color, row.archived)).map { it.version }
    }

    override suspend fun removeLocal(id: String) = dao.remove(id)

    override suspend fun upsertRemote(entity: CategoryEntity) = dao.upsert(entity)

    override suspend fun loadInitial(): Outcome<InitialLoad<CategoryEntity>> =
        api.list().map { InitialLoad(it.items.map { dto -> dto.toItem() }, it.syncCursor) }

    override suspend fun fetchChanges(cursor: String): Outcome<ChangesPage<CategoryEntity>> =
        api.changes(cursor).map { ChangesPage(it.items.map { dto -> dto.toItem() }, it.nextCursor, it.hasMore) }
}

internal class ExpenseSyncAdapter(
    private val dao: ExpenseDao,
    private val api: ExpenseApi,
    cursors: SyncCursorDao,
    db: PfmDatabase,
) : RoomSyncAdapter<ExpenseEntity>("expenses", dao, cursors, db) {

    override suspend fun push(meta: LocalMeta): Outcome<Long> {
        val row = dao.get(meta.id) ?: return Outcome.Success(meta.version)
        if (row.deleted) return api.delete(row.id).map { meta.version + 1 }
        return api.put(row.id, UpsertExpenseRequest(row.title, row.note, row.amount, row.spentOn, row.categoryId)).map { it.version }
    }

    override suspend fun removeLocal(id: String) = dao.remove(id)

    override suspend fun upsertRemote(entity: ExpenseEntity) = dao.upsert(entity)

    /**
     * Pages newest first; the sync cursor of the FIRST page is the one to continue from (records changed while we page
     * are caught by /changes). The history is capped so a huge account does not block the first start.
     */
    override suspend fun loadInitial(): Outcome<InitialLoad<ExpenseEntity>> {
        val items = mutableListOf<RemoteItem<ExpenseEntity>>()
        var before: String? = null
        var syncCursor: String? = null
        repeat(MAX_INITIAL_PAGES) {
            when (val page = api.page(before)) {
                is Outcome.Failure -> return page
                is Outcome.Success -> {
                    if (syncCursor == null) syncCursor = page.value.syncCursor
                    items += page.value.items.map { it.toItem() }
                    before = page.value.nextCursor ?: return Outcome.Success(InitialLoad(items, syncCursor!!))
                }
            }
        }
        return Outcome.Success(InitialLoad(items, syncCursor!!))
    }

    override suspend fun fetchChanges(cursor: String): Outcome<ChangesPage<ExpenseEntity>> =
        api.changes(cursor).map { ChangesPage(it.items.map { dto -> dto.toItem() }, it.nextCursor, it.hasMore) }

    private companion object {
        const val MAX_INITIAL_PAGES = 25 // 5000 expenses
    }
}

internal class HabitSyncAdapter(
    private val dao: HabitDao,
    private val api: HabitApi,
    cursors: SyncCursorDao,
    db: PfmDatabase,
) : RoomSyncAdapter<HabitEntity>("habits", dao, cursors, db) {

    override suspend fun push(meta: LocalMeta): Outcome<Long> {
        val row = dao.get(meta.id) ?: return Outcome.Success(meta.version)
        if (row.deleted) return api.delete(row.id).map { meta.version + 1 }
        val days = row.daysOfWeek.split(',').filter { it.isNotBlank() }.map { it.trim().toInt() }
        return api.put(
            row.id,
            UpsertHabitRequest(row.title, row.note, row.amount, row.categoryId, days, row.timeOfDay, row.sortOrder),
        ).map { it.version }
    }

    override suspend fun removeLocal(id: String) = dao.remove(id)

    override suspend fun upsertRemote(entity: HabitEntity) = dao.upsert(entity)

    override suspend fun loadInitial(): Outcome<InitialLoad<HabitEntity>> =
        api.list().map { InitialLoad(it.items.map { dto -> dto.toItem() }, it.syncCursor) }

    override suspend fun fetchChanges(cursor: String): Outcome<ChangesPage<HabitEntity>> =
        api.changes(cursor).map { ChangesPage(it.items.map { dto -> dto.toItem() }, it.nextCursor, it.hasMore) }
}

internal class BudgetSyncAdapter(
    private val dao: BudgetDao,
    private val api: BudgetApi,
    cursors: SyncCursorDao,
    db: PfmDatabase,
) : RoomSyncAdapter<BudgetEntity>("budgets", dao, cursors, db) {

    override suspend fun push(meta: LocalMeta): Outcome<Long> {
        val row = dao.get(meta.id) ?: return Outcome.Success(meta.version)
        if (row.deleted) return api.delete(row.id).map { meta.version + 1 }
        val thresholds = row.alertThresholds.split(',').filter { it.isNotBlank() }.map { it.trim().toInt() }
        return api.put(
            row.id,
            UpsertBudgetRequest(row.categoryId, row.period, row.amount, row.startDate, row.endDate, row.recurring, thresholds),
        ).map { it.version }
    }

    override suspend fun removeLocal(id: String) = dao.remove(id)

    override suspend fun upsertRemote(entity: BudgetEntity) = dao.upsert(entity)

    override suspend fun loadInitial(): Outcome<InitialLoad<BudgetEntity>> =
        api.list().map { InitialLoad(it.items.map { dto -> dto.toItem() }, it.syncCursor) }

    override suspend fun fetchChanges(cursor: String): Outcome<ChangesPage<BudgetEntity>> =
        api.changes(cursor).map { ChangesPage(it.items.map { dto -> dto.toItem() }, it.nextCursor, it.hasMore) }
}

// ---- server record -> local row (always clean: dirty = false) ----

private fun CategoryDto.toItem() = RemoteItem(
    id, version, deleted = false,
    CategoryEntity(id = id, name = name, icon = icon, color = color, archived = archived, version = version),
)

private fun ExpenseDto.toItem() = RemoteItem(
    id, version, deleted,
    ExpenseEntity(
        id = id, title = title, note = note, amount = amount, spentOn = spentOn, categoryId = categoryId,
        version = version, deleted = false,
    ),
)

private fun HabitDto.toItem() = RemoteItem(
    id, version, deleted,
    HabitEntity(
        id = id, title = title, note = note, amount = amount, categoryId = categoryId,
        daysOfWeek = daysOfWeek.joinToString(","), timeOfDay = timeOfDay, sortOrder = sortOrder, version = version,
    ),
)

private fun BudgetDto.toItem() = RemoteItem(
    id, version, deleted,
    BudgetEntity(
        id = id, categoryId = categoryId, period = period, amount = amount, startDate = startDate, endDate = endDate,
        recurring = recurring, alertThresholds = alertThresholds.joinToString(","), version = version,
    ),
)

/**
 * Alerts are created by the server, so there is nothing to create here: the only local change is "read", pushed as
 * PUT /v1/notifications/{id}/read. Pull works like the other resources (the cursor of /notifications/changes).
 */
internal class NotificationSyncAdapter(
    private val dao: hopper.pfm.insect.core.database.NotificationDao,
    private val api: hopper.pfm.insect.core.network.NotificationApi,
    cursors: SyncCursorDao,
    db: PfmDatabase,
) : RoomSyncAdapter<hopper.pfm.insect.core.database.NotificationEntity>("notifications", dao, cursors, db) {

    override suspend fun push(meta: LocalMeta): Outcome<Long> {
        dao.get(meta.id) ?: return Outcome.Success(meta.version)
        return api.markRead(meta.id).map { it.version }
    }

    override suspend fun removeLocal(id: String) = dao.remove(id)

    override suspend fun upsertRemote(entity: hopper.pfm.insect.core.database.NotificationEntity) = dao.upsert(entity)

    /** Newest first; the sync cursor of the FIRST page is the one to continue from. A few pages are enough for history. */
    override suspend fun loadInitial(): Outcome<InitialLoad<hopper.pfm.insect.core.database.NotificationEntity>> {
        val items = mutableListOf<RemoteItem<hopper.pfm.insect.core.database.NotificationEntity>>()
        var before: String? = null
        var syncCursor: String? = null
        repeat(MAX_INITIAL_PAGES) {
            when (val page = api.page(before)) {
                is Outcome.Failure -> return page
                is Outcome.Success -> {
                    if (syncCursor == null) syncCursor = page.value.syncCursor
                    items += page.value.items.map { it.toItem() }
                    before = page.value.nextCursor ?: return Outcome.Success(InitialLoad(items, syncCursor!!))
                }
            }
        }
        return Outcome.Success(InitialLoad(items, syncCursor!!))
    }

    override suspend fun fetchChanges(cursor: String): Outcome<ChangesPage<hopper.pfm.insect.core.database.NotificationEntity>> =
        api.changes(cursor).map { ChangesPage(it.items.map { dto -> dto.toItem() }, it.nextCursor, it.hasMore) }

    private companion object {
        const val MAX_INITIAL_PAGES = 5
    }
}

private fun hopper.pfm.insect.core.network.NotificationDto.toItem() = RemoteItem(
    id, version, deleted = false,
    hopper.pfm.insect.core.database.NotificationEntity(
        id = id, kind = kind, threshold = threshold, budgetId = budgetId, categoryId = categoryId, period = period,
        periodStart = periodStart, periodEnd = periodEnd, amount = amount, spent = spent, percentUsed = percentUsed,
        createdAt = createdAt, read = read, readAt = readAt, version = version,
    ),
)
