package hopper.pfm.insect.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    val profile: StateFlow<UserProfile?> =
        auth.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Refresh the cached copy in the background; offline simply keeps the cached one.
        viewModelScope.launch { auth.refreshProfile() }
    }

    fun logout(allDevices: Boolean) {
        viewModelScope.launch { auth.logout(allDevices) }
    }
}
