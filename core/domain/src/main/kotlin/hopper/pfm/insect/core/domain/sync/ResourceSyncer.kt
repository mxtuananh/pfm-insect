package hopper.pfm.insect.core.domain.sync

import hopper.pfm.insect.core.common.AppError
import hopper.pfm.insect.core.common.Outcome

/** Why a step stopped early. */
enum class SyncProblem {
    /** No connection, timeout, 5xx or 429: try again later with a back-off. */
    Retry,

    /** The session is over; the user has to sign in again. Local data and dirty flags are kept. */
    AuthRequired,
}

data class PushReport(
    val pushed: Int = 0,
    /** Records the server rejected for good (they stay dirty with a failure code until the user edits them). */
    val rejected: Int = 0,
    /** Records waiting for something else (for example a category that is not on the server yet). */
    val deferred: Int = 0,
    val problem: SyncProblem? = null,
)

data class PullReport(val applied: Int = 0, val resynced: Boolean = false, val problem: SyncProblem? = null)

/** Push and pull of one resource, following the client algorithm of the server integration guide. */
class ResourceSyncer<T>(private val adapter: SyncAdapter<T>) {

    val name: String get() = adapter.name

    suspend fun push(): PushReport {
        var pushed = 0
        var rejected = 0
        var deferred = 0
        for (record in adapter.dirtyRecords()) {
            // Created and deleted while offline: the server never heard of it, nothing to send.
            if (record.deleted && record.version == 0L) {
                adapter.removeLocal(record.id)
                continue
            }
            when (val result = adapter.push(record)) {
                is Outcome.Success -> {
                    adapter.markPushed(record.id, result.value, record.localRev)
                    pushed++
                }
                is Outcome.Failure -> when (val problem = classify(result.error)) {
                    is Classified.Stop -> return PushReport(pushed, rejected, deferred, problem.problem)
                    Classified.Defer -> deferred++
                    is Classified.Reject -> {
                        adapter.markFailed(record.id, problem.code)
                        rejected++
                    }
                }
            }
        }
        return PushReport(pushed, rejected, deferred)
    }

    suspend fun pull(): PullReport {
        val stored = adapter.cursor() ?: return initialLoad(resynced = false)
        var applied = 0
        var cursor: String = stored
        while (true) {
            when (val result = adapter.fetchChanges(cursor)) {
                is Outcome.Success -> {
                    val page = result.value
                    adapter.transaction {
                        page.items.forEach { if (apply(it)) applied++ }
                        adapter.saveCursor(page.nextCursor)
                    }
                    cursor = page.nextCursor
                    if (!page.hasMore) return PullReport(applied)
                }
                is Outcome.Failure -> {
                    val error = result.error
                    if (error is AppError.Server && error.httpStatus == 410) {
                        // Too old to learn about deletions: drop the clean rows and load everything again.
                        adapter.clearNonDirty()
                        return initialLoad(resynced = true)
                    }
                    return PullReport(applied, problem = (classify(error) as? Classified.Stop)?.problem ?: SyncProblem.Retry)
                }
            }
        }
    }

    private suspend fun initialLoad(resynced: Boolean): PullReport =
        when (val result = adapter.loadInitial()) {
            is Outcome.Success -> {
                var applied = 0
                adapter.transaction {
                    result.value.items.forEach { if (apply(it)) applied++ }
                    adapter.saveCursor(result.value.cursor)
                }
                PullReport(applied, resynced)
            }
            is Outcome.Failure -> PullReport(
                resynced = resynced,
                problem = (classify(result.error) as? Classified.Stop)?.problem ?: SyncProblem.Retry,
            )
        }

    /** The table of "apply(item)": returns true when the local copy changed. */
    private suspend fun apply(item: RemoteItem<T>): Boolean {
        val local = adapter.meta(item.id)
        return when {
            local == null && item.deleted -> false
            local == null -> { adapter.upsertRemote(item.entity); true }
            local.dirty -> false // the local edit is pushed and wins (last write wins)
            item.version <= local.version -> false // the 5 second overlap re-delivers items
            item.deleted -> { adapter.removeLocal(item.id); true }
            else -> { adapter.upsertRemote(item.entity); true }
        }
    }

    private sealed interface Classified {
        data class Stop(val problem: SyncProblem) : Classified
        data object Defer : Classified
        data class Reject(val code: String) : Classified
    }

    private fun classify(error: AppError): Classified = when (error) {
        AppError.Unauthorized -> Classified.Stop(SyncProblem.AuthRequired)
        AppError.Offline, AppError.Timeout -> Classified.Stop(SyncProblem.Retry)
        is AppError.Unknown -> Classified.Stop(SyncProblem.Retry)
        is AppError.Server -> when {
            error.httpStatus >= 500 || error.httpStatus == 429 -> Classified.Stop(SyncProblem.Retry)
            error.code == "unknown_category" -> Classified.Defer
            error.httpStatus == 401 -> Classified.Stop(SyncProblem.AuthRequired)
            else -> Classified.Reject(error.code ?: "http_${error.httpStatus}")
        }
    }
}
