package hopper.pfm.insect.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReloginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: AuthMessage? = null,
)

/** Session expired: the email is fixed (taken from the cached profile) and only the password is asked. */
@HiltViewModel
class ReloginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReloginUiState())
    val state: StateFlow<ReloginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = auth.profile.first()
            _state.update { it.copy(email = profile?.email.orEmpty()) }
        }
    }

    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        if (s.password.isEmpty()) {
            _state.update { it.copy(error = AuthMessage.PasswordRequired) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.login(s.email, s.password)
            _state.update {
                if (result is Outcome.Failure) it.copy(loading = false, error = result.error.toAuthMessage()) else it.copy(loading = false)
            }
        }
    }

    fun discardLocalData() {
        viewModelScope.launch { auth.discardLocalData() }
    }
}
