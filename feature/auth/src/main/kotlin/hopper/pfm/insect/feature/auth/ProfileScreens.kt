package hopper.pfm.insect.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.model.UserProfile
import hopper.pfm.insect.core.ui.PfmBanner
import hopper.pfm.insect.core.ui.PfmPrimaryButton
import hopper.pfm.insect.core.ui.PfmTextField
import hopper.pfm.insect.core.ui.floatingBarSpace
import java.time.ZoneId
import java.time.ZonedDateTime

/** Entries of the profile list that open other features; the app wires them as the features arrive. */
data class ProfileLinks(
    val onCategories: () -> Unit = {},
    val onHabits: () -> Unit = {},
    val onNotifications: () -> Unit = {},
    val onSync: () -> Unit = {},
    val onSettings: () -> Unit = {},
)

private fun gmtOffset(zoneId: String): String = try {
    val offset = ZonedDateTime.now(ZoneId.of(zoneId)).offset.id
    "GMT" + if (offset == "Z") "+0" else offset.replace(":00", "").replace("+0", "+")
        .replace("-0", "-")
} catch (_: Exception) {
    zoneId
}

@Composable
private fun Avatar(name: String, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            color = MaterialTheme.colorScheme.onPrimary,
            style = if (size >= 64) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
        )
    }
}

/** Content of the Cá nhân tab (the shell draws the top bar and the floating bar). */
@Composable
fun ProfileScreen(
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    links: ProfileLinks = ProfileLinks(),
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val pending by viewModel.pendingChanges.collectAsStateWithLifecycle()
    var askLogout by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileCard(profile, onEdit)
        Spacer(Modifier.height(8.dp))
        ProfileRow(
            Icons.Filled.Category,
            stringResource(R.string.profile_categories),
            stringResource(R.string.profile_categories_sub),
            links.onCategories
        )
        ProfileRow(
            Icons.Filled.Repeat,
            stringResource(R.string.profile_habits),
            stringResource(R.string.profile_habits_sub),
            links.onHabits
        )
        ProfileRow(
            Icons.Filled.Notifications,
            stringResource(R.string.profile_notifications),
            stringResource(R.string.profile_notifications_sub),
            links.onNotifications
        )
        ProfileRow(
            Icons.Filled.Sync,
            stringResource(R.string.profile_sync),
            stringResource(R.string.profile_sync_sub),
            links.onSync
        )
        ProfileRow(
            Icons.Filled.Settings,
            stringResource(R.string.profile_settings),
            stringResource(R.string.profile_settings_sub),
            links.onSettings
        )
        ProfileRow(
            Icons.AutoMirrored.Filled.Logout,
            stringResource(R.string.profile_logout),
            stringResource(R.string.profile_logout_sub),
            onClick = { askLogout = true },
            danger = true,
        )
        Spacer(Modifier.height(floatingBarSpace()))
    }
    if (askLogout) {
        AlertDialog(
            onDismissRequest = { askLogout = false },
            title = { Text(stringResource(R.string.logout_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pending > 0) {
                        Text(
                            stringResource(R.string.logout_unsynced_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    OutlinedButton(
                        onClick = { askLogout = false; viewModel.logout(false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.logout_this_device))
                    }
                    OutlinedButton(
                        onClick = { askLogout = false; viewModel.logout(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.logout_all_devices))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    askLogout = false
                }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun ProfileCard(profile: UserProfile?, onEdit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Avatar(profile?.name.orEmpty(), 64)
                Column {
                    Text(
                        profile?.name.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        profile?.email.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profile?.let {
                    Chip(gmtOffset(it.timezone))
                    Chip(it.currency)
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer),
                ) { Text(stringResource(R.string.profile_edit)) }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val container =
        if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val onContainer =
        if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!danger) Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit, viewModel: EditProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickingZone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()) {
            Row(
                Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
                Text(
                    stringResource(R.string.edit_profile_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(state.name, 64)
                Text(
                    stringResource(R.string.edit_avatar_unsupported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                state.error?.let {
                    PfmBanner(
                        stringResource(it.res),
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(16.dp))
                }
                PfmTextField(
                    value = state.name,
                    onValueChange = viewModel::onName,
                    label = stringResource(R.string.edit_name),
                    errorText = state.nameError?.let { stringResource(it.res) },
                    supportingText = stringResource(R.string.edit_name_hint),
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::save,
                )
                Spacer(Modifier.height(8.dp))
                PfmTextField(
                    value = state.email,
                    onValueChange = {},
                    label = stringResource(R.string.auth_email),
                    enabled = false,
                    supportingText = stringResource(R.string.edit_email_hint),
                )
                Spacer(Modifier.height(8.dp))
                Box {
                    PfmTextField(
                        value = "${state.timezone} (${gmtOffset(state.timezone)})",
                        onValueChange = {},
                        label = stringResource(R.string.edit_timezone),
                        readOnly = true,
                    )
                    Box(Modifier
                        .matchParentSize()
                        .clickable { pickingZone = true })
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.edit_currency),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SupportedCurrencies.forEach { code ->
                        FilterChip(
                            selected = state.currency == code,
                            onClick = { viewModel.onCurrency(code) },
                            label = { Text(code) },
                            shape = CircleShape,
                        )
                    }
                }
            }
            PfmPrimaryButton(
                text = stringResource(R.string.common_save),
                onClick = viewModel::save,
                loading = state.saving,
                enabled = state.loaded,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
    if (pickingZone) {
        TimezonePicker(
            zones = viewModel.timezones,
            onPick = { viewModel.onTimezone(it); pickingZone = false },
            onDismiss = { pickingZone = false },
        )
    }
}

@Composable
private fun TimezonePicker(zones: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, zones) {
        if (query.isBlank()) zones else zones.filter {
            it.contains(
                query.trim(),
                ignoreCase = true
            )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_timezone)) },
        text = {
            Column {
                PfmTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(R.string.edit_search_timezone),
                    imeAction = ImeAction.Done
                )
                LazyColumn(Modifier.height(280.dp)) {
                    items(filtered) { zone ->
                        Text(
                            "$zone  (${gmtOffset(zone)})",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(zone) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

