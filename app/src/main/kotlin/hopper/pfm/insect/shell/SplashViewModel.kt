package hopper.pfm.insect.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.CheckAppSupportUseCase
import hopper.pfm.insect.core.model.AppSupport
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Where the splash screen sends the user. Auth is added in phase 3. */
enum class StartDestination { Main, UpdateRequired }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAppSupport: CheckAppSupportUseCase,
) : ViewModel() {

    private val _destination = MutableStateFlow<StartDestination?>(null)
    val destination: StateFlow<StartDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // Show the splash for a moment, but never wait long for the server: the app works offline.
            val minimumShown = async { delay(MIN_SPLASH_MS) }
            val support = withTimeoutOrNull(HEALTH_TIMEOUT_MS) { checkAppSupport() } ?: AppSupport.Supported
            minimumShown.await()
            _destination.value = when (support) {
                is AppSupport.Supported -> StartDestination.Main
                is AppSupport.UpdateRequired -> StartDestination.UpdateRequired
            }
        }
    }

    private companion object {
        const val MIN_SPLASH_MS = 600L
        const val HEALTH_TIMEOUT_MS = 3_000L
    }
}
