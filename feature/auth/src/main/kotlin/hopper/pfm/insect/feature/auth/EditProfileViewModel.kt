package hopper.pfm.insect.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.NamePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

val SupportedCurrencies = listOf("VND", "USD")

data class EditProfileUiState(
    val loaded: Boolean = false,
    val email: String = "",
    val name: String = "",
    val timezone: String = "",
    val currency: String = "VND",
    val saving: Boolean = false,
    val nameError: AuthMessage? = null,
    val error: AuthMessage? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    /** Region based zone ids only (for example Asia/Ho_Chi_Minh), sorted for the picker. */
    val timezones: List<String> = ZoneId.getAvailableZoneIds().filter { it.contains('/') && !it.startsWith("Etc/") }.sorted()

    private var original: EditProfileUiState? = null

    init {
        viewModelScope.launch {
            val profile = auth.profile.first()
            val loaded = EditProfileUiState(
                loaded = true,
                email = profile?.email.orEmpty(),
                name = profile?.name.orEmpty(),
                timezone = profile?.timezone ?: "Asia/Ho_Chi_Minh",
                currency = profile?.currency ?: "VND",
            )
            original = loaded
            _state.value = loaded
        }
    }

    fun onName(value: String) = _state.update { it.copy(name = value, nameError = null, error = null) }

    fun onTimezone(value: String) = _state.update { it.copy(timezone = value, error = null) }

    fun onCurrency(value: String) = _state.update { it.copy(currency = value, error = null) }

    fun save() {
        val s = _state.value
        val base = original ?: return
        if (s.saving) return
        if (!NamePolicy.isValid(s.name)) {
            _state.update { it.copy(nameError = AuthMessage.NameInvalid) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            // Only changed fields are sent.
            val result = auth.updateProfile(
                name = s.name.takeIf { it.trim() != base.name },
                timezone = s.timezone.takeIf { it != base.timezone },
                currency = s.currency.takeIf { it != base.currency },
            )
            _state.update {
                when (result) {
                    is Outcome.Success -> it.copy(saving = false, saved = true)
                    is Outcome.Failure -> it.copy(saving = false, error = result.error.toAuthMessage())
                }
            }
        }
    }
}
