package hopper.pfm.insect

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import hopper.pfm.insect.core.data.sync.SyncScheduler
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.sync.SyncRepository
import hopper.pfm.insect.core.model.SessionState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PfmApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var sync: SyncRepository
    @Inject lateinit var scheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        val process = ProcessLifecycleOwner.get()
        // Sync when the user signs in, keep a periodic safety net while signed in, stop when signed out.
        process.lifecycleScope.launch {
            auth.session.collect { state ->
                when (state) {
                    SessionState.SignedIn -> {
                        scheduler.schedulePeriodic()
                        sync.requestSync()
                    }
                    SessionState.SignedOut -> scheduler.cancelAll()
                    SessionState.Expired -> Unit
                }
            }
        }
        // And whenever the app comes back to the foreground.
        process.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (auth.session.value == SessionState.SignedIn) sync.requestSync()
            }
        })
    }
}
