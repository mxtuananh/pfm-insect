package hopper.pfm.insect.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.EmailPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val emailError: AuthMessage? = null,
    val passwordError: AuthMessage? = null,
    val error: AuthMessage? = null,
)

/**
 * Success is not reported here: signing in changes the session state, which the app observes to leave the auth screens.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val emailError = if (EmailPolicy.isValid(s.email)) null else AuthMessage.InvalidEmail
        val passwordError = if (s.password.isEmpty()) AuthMessage.PasswordRequired else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.login(s.email, s.password)
            _state.update {
                when (result) {
                    is Outcome.Success -> it.copy(loading = false)
                    is Outcome.Failure -> it.copy(loading = false, error = result.error.toAuthMessage())
                }
            }
        }
    }
}
