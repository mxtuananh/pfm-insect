package hopper.pfm.insect.feature.budget

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
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.domain.budget.BudgetProgress
import hopper.pfm.insect.core.domain.budget.BudgetStatus
import hopper.pfm.insect.core.model.BudgetPeriod
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.SyncStatus
import hopper.pfm.insect.core.ui.CategoryAvatar
import hopper.pfm.insect.core.ui.DatePickerModal
import java.time.format.DateTimeFormatter


@Composable
internal fun periodName(period: BudgetPeriod): String = stringResource(
    when (period) {
        BudgetPeriod.DAY -> R.string.budget_period_day
        BudgetPeriod.MONTH -> R.string.budget_period_month
        BudgetPeriod.YEAR -> R.string.budget_period_year
    },
)

@Stable
class BudgetScreenState {
    var canAddItem by mutableStateOf(false)
        internal set
}

/** Content of the Hạn mức tab (the shell draws the top bar and the floating bar). */
@Composable
fun BudgetListScreen(
    onAdd: () -> Unit,
    onEdit: (budgetId: String) -> Unit,
    contentPadding: PaddingValues,
    screenState: BudgetScreenState,
    modifier: Modifier = Modifier,
    viewModel: BudgetListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val dateText = state.date.format(DateTimeFormatter.ofPattern("dd/MM"))

    LaunchedEffect(screenState, state.canAddMore) {
        screenState.canAddItem = state.canAddMore
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp) + contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "date") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { pickDate = true },
                        label = {
                            Text(
                                if (state.date == state.today) stringResource(
                                    R.string.budget_today,
                                    dateText
                                )
                                else stringResource(
                                    R.string.budget_date_other,
                                    state.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                ),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = CircleShape,
                    )
                    if (state.date != state.today) {
                        TextButton(onClick = viewModel::goToToday) { Text(stringResource(R.string.budget_go_today)) }
                    }
                }
            }
            if (!state.loading && state.items.isEmpty()) {
                item(key = "empty") {
                    EmptyBudgets(
                        noneAtAll = state.date == state.today,
                        onAdd = onAdd
                    )
                }
            }
            items(state.items, key = { it.budget.id }) { progress ->
                BudgetCard(
                    progress = progress,
                    category = progress.budget.categoryId?.let { state.categories[it] },
                    currency = state.currency,
                    onClick = { onEdit(progress.budget.id) },
                )
            }
        }
    }
    if (pickDate) {
        DatePickerModal(
            selectedDate = state.date,
            onDateSelected = {
                it?.let(viewModel::setDate)
                pickDate = false
            },
            onDismiss = {
                pickDate = false
            }
        )
    }
}

@Composable
private fun EmptyBudgets(noneAtAll: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PieChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.budget_empty_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            stringResource(if (noneAtAll) R.string.budget_empty_body else R.string.budget_empty_filtered),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (noneAtAll) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAdd) { Text(stringResource(R.string.budget_add)) }
        }
    }
}

@Composable
private fun statusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.OK -> MaterialTheme.colorScheme.primary
    BudgetStatus.WARNING -> MaterialTheme.colorScheme.tertiary
    BudgetStatus.EXCEEDED -> MaterialTheme.colorScheme.error
}

@Composable
private fun BudgetCard(
    progress: BudgetProgress,
    category: Category?,
    currency: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val budget = progress.budget
    val tint = statusColor(progress.status)
    val name = category?.name ?: stringResource(R.string.budget_all)
    val marker = when (budget.syncStatus) {
        SyncStatus.PENDING -> "  ⟳"
        SyncStatus.FAILED -> "  ⚠"
        SyncStatus.SYNCED -> ""
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryAvatar(
                    category?.icon ?: "other",
                    category?.color ?: "#90A4AE",
                    size = 36.dp
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.budget_card_title, name, periodName(budget.period)),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(
                            R.string.budget_spent_of,
                            Money.formatWithSymbol(progress.spent, currency),
                            Money.formatWithSymbol(budget.amount, currency),
                        ) + marker,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (budget.syncStatus == SyncStatus.FAILED) colors.error else colors.onSurfaceVariant,
                    )
                }
            }
            // Progress bar: the filled part never exceeds the track, the text below says by how much it is exceeded.
            val fraction = (progress.percentUsed / 100f).coerceIn(0.02f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(colors.surfaceContainerHighest, CircleShape)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(10.dp)
                        .background(tint, CircleShape)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val left = when (progress.status) {
                    BudgetStatus.OK -> stringResource(
                        R.string.budget_used,
                        progress.percentUsed.toInt()
                    )

                    BudgetStatus.WARNING -> stringResource(
                        R.string.budget_nearly,
                        progress.percentUsed.toInt()
                    )

                    BudgetStatus.EXCEEDED -> stringResource(
                        R.string.budget_exceeded_by,
                        Money.formatWithSymbol(-progress.remaining, currency)
                    )
                }
                Text(left, style = MaterialTheme.typography.labelLarge, color = tint)
                Text(
                    if (progress.status == BudgetStatus.EXCEEDED) stringResource(R.string.budget_over_label)
                    else stringResource(
                        R.string.budget_left,
                        Money.formatWithSymbol(progress.remaining, currency)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (progress.status == BudgetStatus.EXCEEDED) colors.error else colors.onSurfaceVariant,
                )
            }
            Surface(shape = CircleShape, color = colors.surfaceContainerHigh) {
                Text(
                    stringResource(
                        R.string.budget_habits_planned,
                        Money.formatWithSymbol(progress.plannedByHabits, currency)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}
