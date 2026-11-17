package hopper.pfm.insect

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.model.SessionState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the session so the root navigation can leave the app when the user signs out or the session expires. */
@HiltViewModel
class AppViewModel @Inject constructor(auth: AuthRepository) : ViewModel() {
    val session: StateFlow<SessionState> = auth.session
}
