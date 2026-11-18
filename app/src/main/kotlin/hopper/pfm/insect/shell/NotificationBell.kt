package hopper.pfm.insect.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.R
import hopper.pfm.insect.feature.notification.UnreadBadgeViewModel

/** Bell of the top bar with the number of unread alerts. */
@Composable
fun NotificationBell(onClick: () -> Unit, viewModel: UnreadBadgeViewModel = hiltViewModel()) {
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (unread > 0) Badge { Text(if (unread > 9) "9+" else unread.toString()) }
            },
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.notifications))
        }
    }
}
