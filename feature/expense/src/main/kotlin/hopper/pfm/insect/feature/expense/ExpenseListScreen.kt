package hopper.pfm.insect.feature.expense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.domain.expense.CategoryTotal
import hopper.pfm.insect.core.domain.expense.DayTotal
import hopper.pfm.insect.core.domain.expense.PeriodType
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.model.SyncStatus
import hopper.pfm.insect.core.ui.CategoryAvatar
import hopper.pfm.insect.core.ui.CategoryColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Content of the Chi tiêu tab. The shell draws the top bar and the floating bottom bar; this screen reserves space for
 * the bar at the bottom of its list and places its own floating action button above it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAdd: () -> Unit,
    onEdit: (expenseId: String) -> Unit,
    bottomBarSpace: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastDeleted by viewModel.lastDeleted.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.deleted_message)
    val undoText = stringResource(R.string.undo)

    LaunchedEffect(lastDeleted) {
        if (lastDeleted != null) {
            val result = snackbar.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.clearLastDeleted()
        }
    }

    Box(modifier.fillMaxSize()) {
        val overview = state.overview
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomBarSpace),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "switcher") { PeriodSwitcher(state.periodType, viewModel::setPeriodType) }
            item(key = "navigator") {
                PeriodNavigator(
                    label = periodLabel(state),
                    showToday = !state.isCurrentPeriod,
                    onPrevious = { viewModel.shift(-1) },
                    onNext = { viewModel.shift(1) },
                    onToday = viewModel::goToToday,
                )
            }
            if (overview != null) {
                item(key = "summary") {
                    SummaryCard(
                        total = overview.total,
                        delta = overview.deltaPercent,
                        currency = state.currency,
                        unit = unitLabel(state.periodType),
                    )
                }
                item(key = "chart") {
                    if (state.periodType == PeriodType.DAY) {
                        CategoryBreakdown(overview.categoryTotals, state.categories, state.currency, state.categoryFilter)
                    } else {
                        DayChart(
                            days = overview.dayTotals,
                            periodType = state.periodType,
                            selected = state.selectedDay,
                            currency = state.currency,
                            onSelect = viewModel::selectDay,
                        )
                    }
                }
                item(key = "chips") {
                    CategoryChips(overview.categoryTotals, state.categories, state.categoryFilter, viewModel::filterByCategory)
                }
                if (overview.groups.isEmpty()) {
                    item(key = "empty") { EmptyState(filtered = state.categoryFilter != null, onAdd = onAdd) }
                }
                overview.groups.forEach { group ->
                    item(key = "header-${group.date}") {
                        DayHeader(group.date, group.total, state.currency, state.today)
                    }
                    items(group.items, key = { it.id }) { expense ->
                        ExpenseRow(
                            expense = expense,
                            category = expense.categoryId?.let { state.categories[it] },
                            currency = state.currency,
                            onClick = { onEdit(expense.id) },
                            onDelete = { viewModel.delete(expense) },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottomBarSpace - 16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_expense))
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomBarSpace - 16.dp, start = 16.dp, end = 88.dp),
        )
    }
}

@Composable
private fun unitLabel(type: PeriodType): String = stringResource(
    when (type) {
        PeriodType.DAY -> R.string.summary_unit_day
        PeriodType.WEEK -> R.string.summary_unit_week
        PeriodType.MONTH -> R.string.summary_unit_month
    },
)

@Composable
private fun periodLabel(state: ExpenseListUiState): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val p = state.period
    return when (p.type) {
        PeriodType.DAY -> p.start.format(DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", locale))
        PeriodType.WEEK -> stringResource(
            R.string.period_week_range,
            p.start.format(DateTimeFormatter.ofPattern("dd/MM")),
            p.end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        )
        PeriodType.MONTH -> stringResource(R.string.period_month) + " " + p.start.format(DateTimeFormatter.ofPattern("MM/yyyy"))
    }
}

@Composable
private fun PeriodSwitcher(selected: PeriodType, onSelect: (PeriodType) -> Unit) {
    val options = listOf(
        PeriodType.DAY to R.string.period_day,
        PeriodType.WEEK to R.string.period_week,
        PeriodType.MONTH to R.string.period_month,
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun PeriodNavigator(label: String, showToday: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.period_previous))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (showToday) TextButton(onClick = onToday) { Text(stringResource(R.string.period_today)) }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.period_next))
        }
    }
}

@Composable
private fun SummaryCard(total: Long, delta: Int?, currency: String, unit: String) {
    Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.summary_total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                Money.formatWithSymbol(total, currency),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            val text = when {
                delta == null -> null
                delta > 0 -> stringResource(R.string.summary_delta_up, delta, unit)
                delta < 0 -> stringResource(R.string.summary_delta_down, -delta, unit)
                else -> stringResource(R.string.summary_delta_same, unit)
            }
            if (text != null) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
                    Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}

/** Bars for the days of a week or month. Tapping a bar shows its amount; the selected bar uses the primary colour. */
@Composable
private fun DayChart(days: List<DayTotal>, periodType: PeriodType, selected: LocalDate?, currency: String, onSelect: (LocalDate?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val max = days.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1L
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.chart_by_day), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                selected?.let { day ->
                    val amount = days.firstOrNull { it.date == day }?.amount ?: 0
                    Text(
                        day.format(DateTimeFormatter.ofPattern("dd/MM")) + " · " + Money.formatWithSymbol(amount, currency),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                    )
                }
            }
            val barColor = colors.primaryContainer
            val selectedColor = colors.primary
            val emptyColor = colors.outlineVariant
            Canvas(
                Modifier.fillMaxWidth().height(120.dp).pointerInput(days) {
                    detectTapGestures { offset ->
                        val index = (offset.x / (size.width / days.size)).toInt().coerceIn(0, days.size - 1)
                        onSelect(days[index].date)
                    }
                },
            ) {
                val slot = size.width / days.size
                val gap = slot * 0.25f
                val barWidth = slot - gap
                days.forEachIndexed { i, day ->
                    val height = if (day.amount <= 0) 3.dp.toPx() else (size.height * day.amount / max).toFloat().coerceAtLeast(6.dp.toPx())
                    val color = when {
                        day.date == selected -> selectedColor
                        day.amount <= 0 -> emptyColor
                        else -> barColor
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(i * slot + gap / 2, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                    )
                }
            }
            if (periodType == PeriodType.WEEK) {
                Row(Modifier.fillMaxWidth()) {
                    days.forEach { day ->
                        Text(
                            day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(days.first(), days[days.size / 2], days.last()).forEach {
                        Text(it.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** The day view: expenses have a date but no time, so the day is broken down by category instead of by hour. */
@Composable
private fun CategoryBreakdown(totals: List<CategoryTotal>, categories: Map<String, Category>, currency: String, filter: String?) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.chart_by_category), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            val max = totals.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1L
            totals.forEach { total ->
                val category = total.categoryId?.let { categories[it] }
                val dim = filter != null && filter != total.categoryId
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category?.name ?: stringResource(R.string.filter_uncategorized), style = MaterialTheme.typography.bodyMedium)
                        Text(Money.formatWithSymbol(total.amount, currency), style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).background(colors.surfaceContainerHighest, CircleShape)) {
                        Box(
                            Modifier.fillMaxWidth((total.amount.toFloat() / max).coerceIn(0.02f, 1f)).height(8.dp)
                                .background(CategoryColors.parse(category?.color).copy(alpha = if (dim) 0.35f else 1f), CircleShape),
                        )
                    }
                }
            }
            if (totals.isEmpty()) Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CategoryChips(totals: List<CategoryTotal>, categories: Map<String, Category>, filter: String?, onFilter: (String?) -> Unit) {
    if (totals.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") {
            FilterChip(selected = filter == null, onClick = { onFilter(null) }, label = { Text(stringResource(R.string.filter_all)) }, shape = CircleShape)
        }
        items(totals.filter { it.categoryId != null }, key = { it.categoryId!! }) { total ->
            val category = categories[total.categoryId]
            FilterChip(
                selected = filter == total.categoryId,
                onClick = { onFilter(total.categoryId) },
                label = { Text(category?.name ?: "?") },
                leadingIcon = { CategoryAvatar(category?.icon, category?.color, size = 22.dp) },
                shape = CircleShape,
            )
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, total: Long, currency: String, today: LocalDate) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val name = when (date) {
        today -> stringResource(R.string.list_today)
        today.minusDays(1) -> stringResource(R.string.list_yesterday)
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "$name · " + date.format(DateTimeFormatter.ofPattern("dd/MM")),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(Money.formatWithSymbol(total, currency), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseRow(expense: Expense, category: Category?, currency: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(28.dp)).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(stringResource(R.string.form_delete), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge)
            }
        },
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryAvatar(category?.icon, category?.color)
                Column(Modifier.weight(1f)) {
                    Text(expense.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val marker = when (expense.syncStatus) {
                        SyncStatus.PENDING -> " · ⟳ " + stringResource(R.string.item_pending)
                        SyncStatus.FAILED -> " · ⚠ " + stringResource(R.string.item_rejected)
                        SyncStatus.SYNCED -> ""
                    }
                    Text(
                        (category?.name ?: stringResource(R.string.filter_uncategorized)) + marker,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (expense.syncStatus == SyncStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("−" + Money.formatWithSymbol(expense.amount, currency), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun EmptyState(filtered: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(if (filtered) R.string.empty_filtered else R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (!filtered) {
            Text(stringResource(R.string.empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onAdd) { Text(stringResource(R.string.add_expense)) }
        }
    }
}
