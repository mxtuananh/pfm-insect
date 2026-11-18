package hopper.pfm.insect.core.data

import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.model.SessionState
import hopper.pfm.insect.core.network.NotificationApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to the live alert stream while the app is on screen. On every event ("ready" after each (re)connection and
 * "notification" for a new alert) it runs a sync round, which pulls /notifications/changes and so also catches up on
 * what was missed while the connection was down. The connection is re-opened with a growing back-off.
 */
@Singleton
class NotificationStreamManager @Inject constructor(
    private val api: NotificationApi,
    private val sync: SyncRepository,
    private val session: SessionManager,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            var backoff = INITIAL_BACKOFF_MS
            while (isActive) {
                if (session.state.value != SessionState.SignedIn) {
                    delay(SIGNED_OUT_POLL_MS)
                    continue
                }
                try {
                    api.stream().collect {
                        backoff = INITIAL_BACKOFF_MS
                        sync.syncNow()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Offline or the server restarted: try again after the back-off.
                }
                delay(backoff)
                backoff = minOf(backoff * 2, MAX_BACKOFF_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val INITIAL_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 60_000L
        const val SIGNED_OUT_POLL_MS = 5_000L
    }
}
