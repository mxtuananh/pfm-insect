package hopper.pfm.insect

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.model.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the session so the root navigation can leave the app when the user signs out or the session expires. */
@HiltViewModel
class AppViewModel @Inject constructor(auth: AuthRepository) : ViewModel() {
    val session: StateFlow<SessionState> = auth.session

    private val _openNotifications = MutableStateFlow(false)

    /**
     * True after the user tapped a system notification. It stays true until the root navigation has opened the
     * notification center, so a tap that cold-starts the app is not lost while the splash screen is still running.
     */
    val openNotifications: StateFlow<Boolean> = _openNotifications

    fun requestOpenNotifications() {
        _openNotifications.value = true
    }

    fun openNotificationsHandled() {
        _openNotifications.value = false
    }
}
