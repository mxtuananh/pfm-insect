package hopper.pfm.insect.core.domain.sync

import hopper.pfm.insect.core.common.Outcome

/** Sync columns of one local record (see the offline-first contract of the server). */
data class LocalMeta(
    val id: String,
    /** Server version of the last copy received, 0 when the server has never seen the record. */
    val version: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    /** Grows on every local edit, so a push that finishes after a newer edit does not clear dirty. */
    val localRev: Long,
    val failure: String?,
)

/** A record received from the server; entity is the local representation (not dirty). */
class RemoteItem<T>(val id: String, val version: Long, val deleted: Boolean, val entity: T)

class ChangesPage<T>(val items: List<RemoteItem<T>>, val nextCursor: String, val hasMore: Boolean)

/** Everything needed to start from a cursor: the records and the cursor to continue from. */
class InitialLoad<T>(val items: List<RemoteItem<T>>, val cursor: String)

/**
 * One synchronized resource (categories, expenses, habits or budgets), seen from the engine. It is implemented in
 * core:data on top of Room and the API; the engine itself knows nothing about either, which keeps the algorithm testable.
 */
interface SyncAdapter<T> {
    /** Name of the resource, also the key of its cursor. */
    val name: String

    /** Dirty records that were not permanently rejected before. */
    suspend fun dirtyRecords(): List<LocalMeta>

    suspend fun meta(id: String): LocalMeta?

    /** Sends PUT (or DELETE for a deleted record) with the current local fields. Returns the version of the server. */
    suspend fun push(meta: LocalMeta): Outcome<Long>

    /** Stores the version; clears dirty only when localRev is still rev; removes a deleted row that is clean. */
    suspend fun markPushed(id: String, version: Long, rev: Long)

    suspend fun markFailed(id: String, code: String)

    suspend fun removeLocal(id: String)

    suspend fun upsertRemote(entity: T)

    suspend fun loadInitial(): Outcome<InitialLoad<T>>

    suspend fun fetchChanges(cursor: String): Outcome<ChangesPage<T>>

    suspend fun cursor(): String?

    suspend fun saveCursor(cursor: String)

    /** Deletes all rows that are not dirty (used before a full reload). */
    suspend fun clearNonDirty()

    /** Runs the block as one local transaction. */
    suspend fun <R> transaction(block: suspend () -> R): R
}
