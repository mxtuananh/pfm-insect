package hopper.pfm.insect.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import hopper.pfm.insect.core.domain.sync.SyncProblem
import hopper.pfm.insect.core.domain.sync.SyncRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Background sync through WorkManager: runs only with a network, retries with an exponential back-off and keeps one
 * pending request at a time (extra requests while one is queued are absorbed, which batches quick edits).
 */
@Singleton
class SyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(connected)
            .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME, ExistingWorkPolicy.KEEP, request)
    }

    /** Safety net while the user is signed in: a sync at least every 15 minutes when online. */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancelAll() {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(ONE_TIME)
        manager.cancelUniqueWork(PERIODIC)
    }

    private companion object {
        const val ONE_TIME = "pfm-sync"
        const val PERIODIC = "pfm-sync-periodic"
        const val DEBOUNCE_SECONDS = 2L
        const val BACKOFF_SECONDS = 10L
    }
}

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    // Provider: SyncManager also depends on SyncScheduler, which must not need the worker to exist.
    private val sync: Provider<SyncRepository>,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = sync.get().syncNow()
        return when (result.problem) {
            null -> Result.success()
            SyncProblem.Retry -> Result.retry()
            // Signing in again triggers a new sync, so retrying now would only waste battery.
            SyncProblem.AuthRequired -> Result.success()
        }
    }
}
