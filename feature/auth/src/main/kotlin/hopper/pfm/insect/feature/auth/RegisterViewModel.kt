package hopper.pfm.insect.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.EmailPolicy
import hopper.pfm.insect.core.domain.PasswordPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    val emailError: AuthMessage? = null,
    val confirmError: AuthMessage? = null,
    val error: AuthMessage? = null,
) {
    val passwordCheck: PasswordPolicy.Check get() = PasswordPolicy.check(password)
}

/** Registers, then signs in with the same credentials; the session change takes the user into the app. */
@HiltViewModel
class RegisterViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun onConfirm(value: String) = _state.update { it.copy(confirm = value, confirmError = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val emailError = if (EmailPolicy.isValid(s.email)) null else AuthMessage.InvalidEmail
        val confirmError = if (s.password == s.confirm) null else AuthMessage.PasswordMismatch
        val rulesError = if (s.passwordCheck.valid) null else AuthMessage.PasswordRules
        if (emailError != null || confirmError != null || rulesError != null) {
            _state.update { it.copy(emailError = emailError, confirmError = confirmError, error = rulesError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            var failure: AuthMessage? = null
            var emailTaken = false
            when (val registered = auth.register(s.email, s.password)) {
                is Outcome.Failure -> {
                    val message = registered.error.toAuthMessage()
                    if (message == AuthMessage.EmailTaken) emailTaken = true else failure = message
                }
                is Outcome.Success -> {
                    val signedIn = auth.login(s.email, s.password)
                    if (signedIn is Outcome.Failure) failure = signedIn.error.toAuthMessage()
                }
            }
            _state.update {
                it.copy(
                    loading = false,
                    emailError = if (emailTaken) AuthMessage.EmailTaken else null,
                    error = failure,
                )
            }
        }
    }
}
