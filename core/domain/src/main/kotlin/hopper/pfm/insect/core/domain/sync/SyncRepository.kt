package hopper.pfm.insect.core.domain.sync

import kotlinx.coroutines.flow.Flow

/** What the sync indicators of the UI show. */
data class SyncOverview(
    val syncing: Boolean = false,
    /** Records changed on this device and not yet accepted by the server. */
    val pending: Int = 0,
    /** Records the server rejected for good; the user has to edit or delete them. */
    val rejected: Int = 0,
    /** Why the last round stopped early (null when it finished or has not run). */
    val problem: SyncProblem? = null,
)

interface SyncRepository {
    val overview: Flow<SyncOverview>

    /** Asks for a sync soon (with a short delay so several edits are sent together). Returns at once. */
    fun requestSync()

    /** Runs a full round now and waits for it. Only one round runs at a time. */
    suspend fun syncNow(): SyncResult
}
