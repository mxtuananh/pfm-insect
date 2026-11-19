package hopper.pfm.insect.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import hopper.pfm.insect.core.ui.PfmPrimaryButton
import hopper.pfm.insect.core.ui.PfmTextField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private fun LocalDate.toPickerMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toPickerDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun periodName(period: BudgetPeriod): String = stringResource(
    when (period) {
        BudgetPeriod.DAY -> R.string.budget_period_day
        BudgetPeriod.MONTH -> R.string.budget_period_month
        BudgetPeriod.YEAR -> R.string.budget_period_year
    },
)

/** Content of the Hạn mức tab (the shell draws the top bar and the floating bar). */
@Composable
fun BudgetListScreen(
    onAdd: () -> Unit,
    onEdit: (budgetId: String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: BudgetListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val dateText = state.date.format(DateTimeFormatter.ofPattern("dd/MM"))

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
        if (state.canAddMore) {
            FloatingActionButton(
                onClick = onAdd,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() - 16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.budget_add))
            }
        }
    }
    if (pickDate) {
        val pickerState =
            rememberDatePickerState(initialSelectedDateMillis = state.date.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.setDate(it.toPickerDate()) }
                    pickDate = false
                }) { Text(stringResource(R.string.budget_ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pickDate = false
                }) { Text(stringResource(R.string.budget_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
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
                Box(Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .background(tint, CircleShape))
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

@Composable
fun BudgetEditScreen(
    budgetId: String?,
    onBack: () -> Unit,
    viewModel: BudgetEditViewModel = hiltViewModel<BudgetEditViewModel, BudgetEditViewModel.Factory>(
        key = "budget-$budgetId",
        creationCallback = { factory -> factory.create(budgetId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pick by rememberSaveable { mutableStateOf<String?>(null) } // "start" or "end"
    var addThreshold by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.saved, state.deleted) { if (state.saved || state.deleted) onBack() }
    val errors = if (state.showErrors) state.errors else emptySet()
    val colors = MaterialTheme.colorScheme
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
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
                        contentDescription = stringResource(R.string.budget_back)
                    )
                }
                Text(
                    stringResource(if (state.isNew) R.string.budget_new_title else R.string.budget_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (!state.isNew) {
                    IconButton(onClick = viewModel::delete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.budget_delete)
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                if (state.isNew && state.limitReached) {
                    Text(
                        stringResource(R.string.budget_limit),
                        color = colors.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                SectionLabel(stringResource(R.string.budget_applies_to))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.categoryId == null,
                        onClick = { viewModel.onCategory(null) },
                        label = { Text(stringResource(R.string.budget_all_short)) },
                        shape = CircleShape,
                    )
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { viewModel.onCategory(category.id) },
                            label = { Text(category.name) },
                            leadingIcon = {
                                CategoryAvatar(
                                    category.icon,
                                    category.color,
                                    size = 22.dp
                                )
                            },
                            shape = CircleShape,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.budget_period))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        BudgetPeriod.DAY to R.string.budget_period_day_label,
                        BudgetPeriod.MONTH to R.string.budget_period_month_label,
                        BudgetPeriod.YEAR to R.string.budget_period_year_label,
                    ).forEach { (period, label) ->
                        FilterChip(
                            selected = state.period == period,
                            onClick = { viewModel.onPeriod(period) },
                            label = { Text(stringResource(label)) },
                            shape = CircleShape
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                AmountInput(
                    value = state.amountText,
                    symbol = Money.symbol(state.currency),
                    label = stringResource(R.string.budget_amount),
                    error = if (BudgetFormError.AmountRequired in errors) stringResource(R.string.budget_amount_error) else null,
                    keyboardType = if (Money.decimals(state.currency) > 0) KeyboardType.Decimal else KeyboardType.Number,
                    onChange = viewModel::onAmount,
                    autoFocus = state.isNew && state.loaded,
                )
                Spacer(Modifier.height(16.dp))
                DateField(
                    value = state.startDate.format(dateFormat),
                    label = stringResource(R.string.budget_start),
                    error = if (BudgetFormError.YearOutOfRange in errors) stringResource(R.string.budget_year_error) else null,
                    onClick = { pick = "start" },
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(
                                    when (state.period) {
                                        BudgetPeriod.DAY -> R.string.budget_recurring_day
                                        BudgetPeriod.MONTH -> R.string.budget_recurring_month
                                        BudgetPeriod.YEAR -> R.string.budget_recurring_year
                                    },
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                if (state.recurring) stringResource(R.string.budget_recurring_on_hint)
                                else stringResource(
                                    R.string.budget_recurring_off_hint,
                                    periodName(state.period)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.recurring, onCheckedChange = viewModel::onRecurring)
                    }
                }
                if (state.recurring) {
                    Spacer(Modifier.height(16.dp))
                    Box {
                        DateField(
                            value = state.endDate?.format(dateFormat)
                                ?: stringResource(R.string.budget_end_none),
                            label = stringResource(R.string.budget_end),
                            error = if (BudgetFormError.EndBeforeStart in errors) stringResource(R.string.budget_end_error) else null,
                            onClick = { pick = "end" },
                            endInset = if (state.endDate != null) 56.dp else 0.dp,
                            trailing = state.endDate?.let {
                                {
                                    IconButton(onClick = { viewModel.onEndDate(null) }) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.budget_clear)
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.budget_thresholds))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.thresholds.forEach { percent ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.removeThreshold(percent) },
                            label = { Text("$percent%") },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = CircleShape,
                        )
                    }
                    if (state.thresholds.size < 5) {
                        FilterChip(
                            selected = false,
                            onClick = { addThreshold = true },
                            label = { Text(stringResource(R.string.budget_threshold_add)) },
                            shape = CircleShape
                        )
                    }
                }
                Text(
                    if (BudgetFormError.BadThresholds in errors) stringResource(R.string.budget_threshold_error) else stringResource(
                        R.string.budget_threshold_hint
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (BudgetFormError.BadThresholds in errors) colors.error else colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
            PfmPrimaryButton(
                text = stringResource(R.string.budget_save),
                onClick = viewModel::save,
                enabled = state.loaded,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    pick?.let { which ->
        val initial = if (which == "start") state.startDate else (state.endDate ?: state.startDate)
        val pickerState =
            rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { pick = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = millis.toPickerDate()
                        if (which == "start") viewModel.onStartDate(date) else viewModel.onEndDate(
                            date
                        )
                    }
                    pick = null
                }) { Text(stringResource(R.string.budget_ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pick = null
                }) { Text(stringResource(R.string.budget_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
    if (addThreshold) {
        var text by rememberSaveable { mutableStateOf("") }
        val value = text.toIntOrNull()
        AlertDialog(
            onDismissRequest = { addThreshold = false },
            title = { Text(stringResource(R.string.budget_threshold_dialog_title)) },
            text = {
                PfmTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    label = stringResource(R.string.budget_threshold_dialog_label),
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                )
            },
            confirmButton = {
                TextButton(enabled = value != null, onClick = {
                    if (value != null && viewModel.addThreshold(value)) addThreshold = false
                }) { Text(stringResource(R.string.budget_ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    addThreshold = false
                }) { Text(stringResource(R.string.budget_cancel)) }
            },
        )
    }
}

@Composable
private fun DateField(
    value: String,
    label: String,
    error: String?,
    onClick: () -> Unit,
    endInset: Dp = 0.dp,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box {
        PfmTextField(
            value = value,
            onValueChange = {},
            label = label,
            readOnly = true,
            errorText = error,
            trailingIcon = trailing ?: {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null
                )
            },
        )
        Box(Modifier
            .matchParentSize()
            .padding(end = endInset)
            .clickable(onClick = onClick))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun AmountInput(
    value: String,
    symbol: String,
    label: String,
    error: String?,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
    autoFocus: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (error != null) colors.error else colors.primary
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) focus.requestFocus() }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(
                    "0",
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.5f)
                )
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displayMedium.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }
            Text(
                symbol,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurfaceVariant
            )
        }
        HorizontalDivider(thickness = 2.dp, color = accent)
        if (error != null) Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = colors.error,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
