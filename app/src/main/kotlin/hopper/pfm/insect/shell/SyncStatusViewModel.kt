package hopper.pfm.insect.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.sync.SyncOverview
import hopper.pfm.insect.core.domain.sync.SyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncStatusViewModel @Inject constructor(private val sync: SyncRepository) : ViewModel() {

    val overview: StateFlow<SyncOverview> =
        sync.overview.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncOverview())

    /** Tapping the indicator syncs now instead of waiting for the next scheduled run. */
    fun syncNow() {
        viewModelScope.launch { sync.syncNow() }
    }
}
