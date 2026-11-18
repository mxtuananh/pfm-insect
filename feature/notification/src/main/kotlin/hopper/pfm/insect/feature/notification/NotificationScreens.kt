package hopper.pfm.insect.feature.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.domain.budget.BudgetProgress
import hopper.pfm.insect.core.domain.budget.BudgetStatus
import hopper.pfm.insect.core.model.AlertKind
import hopper.pfm.insect.core.model.AppNotification
import hopper.pfm.insect.core.model.BudgetPeriod
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.ui.CategoryAvatar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeOnly: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateAndTime: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM · HH:mm")

@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onBudget: (budgetId: String) -> Unit,
    viewModel: NotificationCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted

    Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.notif_back))
                }
                Text(stringResource(R.string.notif_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (state.unread > 0) {
                    TextButton(onClick = viewModel::markAllRead) { Text(stringResource(R.string.notif_read_all)) }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (needsPermission) {
                    item(key = "permission") {
                        PermissionBanner(onTurnOn = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) })
                    }
                }
                if (state.offline) {
                    item(key = "offline") {
                        Surface(shape = CircleShape, color = colors.surfaceContainerHighest, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.notif_offline),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                if (!state.loading && state.isEmpty) {
                    item(key = "empty") { EmptyNotifications() }
                }
                if (state.warnings.isNotEmpty()) {
                    item(key = "warning-title") { SectionTitle(stringResource(R.string.notif_section_warning)) }
                    items(state.warnings, key = { "w-" + it.budget.id }) { progress ->
                        WarningCard(progress, progress.budget.categoryId?.let { state.categories[it] }, state.currency) {
                            onBudget(progress.budget.id)
                        }
                    }
                }
                if (state.days.isNotEmpty()) {
                    item(key = "recent-title") { SectionTitle(stringResource(R.string.notif_section_recent)) }
                    state.days.forEach { day ->
                        item(key = "day-${day.date}") {
                            Text(
                                when (day.date) {
                                    state.today -> stringResource(R.string.notif_today)
                                    state.today.minusDays(1) -> stringResource(R.string.notif_yesterday)
                                    else -> day.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            )
                        }
                        items(day.alerts, key = { it.id }) { alert ->
                            AlertRow(alert, alert.categoryId?.let { state.categories[it] }, state.currency, state.today) {
                                viewModel.markRead(alert.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun hasNotificationPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun PermissionBanner(onTurnOn: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = colors.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.notif_permission_title), style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
            Text(stringResource(R.string.notif_permission_body), style = MaterialTheme.typography.bodySmall, color = colors.onSecondaryContainer)
            Button(onClick = onTurnOn, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.notif_permission_action)) }
        }
    }
}

@Composable
private fun EmptyNotifications() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.notif_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.notif_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WarningCard(progress: BudgetProgress, category: Category?, currency: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (progress.status == BudgetStatus.EXCEEDED) colors.error else colors.tertiary
    val resources = LocalContext.current.resources
    val name = category?.name ?: stringResource(R.string.notif_all_expenses)
    val period = stringResource(
        when (progress.budget.period) {
            BudgetPeriod.DAY -> R.string.notif_period_day_word
            BudgetPeriod.MONTH -> R.string.notif_period_month_word
            BudgetPeriod.YEAR -> R.string.notif_period_year_word
        },
    )
    val spent = Money.formatWithSymbol(progress.spent, currency)
    val amount = Money.formatWithSymbol(progress.budget.amount, currency)
    val detail = if (progress.status == BudgetStatus.EXCEEDED) {
        resources.getString(R.string.notif_local_exceeded, spent, amount, (progress.percentUsed - 100).toInt())
    } else {
        resources.getString(R.string.notif_local_warning, spent, amount, progress.percentUsed.toInt())
    }
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryAvatar(category?.icon ?: "other", category?.color ?: "#90A4AE", size = 36.dp)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.notif_local_title, name, period), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            val fraction = (progress.percentUsed / 100f).coerceIn(0.02f, 1f)
            Box(Modifier.fillMaxWidth().height(8.dp).background(colors.surfaceContainerHighest, CircleShape)) {
                Box(Modifier.fillMaxWidth(fraction).height(8.dp).background(tint, CircleShape))
            }
        }
    }
}

@Composable
private fun AlertRow(alert: AppNotification, category: Category?, currency: String, today: LocalDate, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val resources = LocalContext.current.resources
    val text = alertText(resources, alert, category?.name, currency)
    val moment = Instant.ofEpochMilli(alert.createdAt).atZone(ZoneId.systemDefault())
    val time = if (moment.toLocalDate() == today) moment.format(TimeOnly) else moment.format(DateAndTime)
    val exceeded = alert.kind == AlertKind.EXCEEDED
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (alert.read) colors.surfaceContainerLow else colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(if (exceeded) colors.errorContainer else colors.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (exceeded) colors.onErrorContainer else colors.onTertiaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Text(text.body, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(time, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (!alert.read) {
                val unreadLabel = stringResource(R.string.notif_unread)
                Box(Modifier.size(10.dp).background(colors.primary, CircleShape).semantics { contentDescription = unreadLabel })
            }
        }
    }
}

