package hopper.pfm.insect.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.R
import hopper.pfm.insect.core.domain.sync.SyncProblem

/** Top bar indicator: syncing, waiting, offline, rejected or all synced. Tapping it syncs now. */
@Composable
fun SyncIndicator(
    modifier: Modifier = Modifier,
    viewModel: SyncStatusViewModel = hiltViewModel()
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    IconButton(
        modifier = modifier,
        onClick = viewModel::syncNow
    ) {
        when {
            overview.syncing -> {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            overview.rejected > 0 -> Icon(
                Icons.Filled.ErrorOutline, stringResource(R.string.sync_rejected), tint = colors.error,
            )
            overview.problem == SyncProblem.Retry -> Icon(
                Icons.Filled.CloudOff, stringResource(R.string.sync_offline), tint = colors.onSurfaceVariant,
            )
            overview.pending > 0 -> Icon(
                Icons.Filled.Sync, stringResource(R.string.sync_pending), tint = colors.tertiary,
            )
            else -> Icon(
                Icons.Filled.CloudDone, stringResource(R.string.sync_synced), tint = colors.onSurfaceVariant,
            )
        }
    }
}
