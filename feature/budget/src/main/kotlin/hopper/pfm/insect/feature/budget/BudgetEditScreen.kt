package hopper.pfm.insect.feature.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.model.BudgetPeriod
import hopper.pfm.insect.core.ui.CategoryAvatar
import hopper.pfm.insect.core.ui.DatePickerModal
import hopper.pfm.insect.core.ui.PfmPrimaryButton
import hopper.pfm.insect.core.ui.PfmTextField
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class BudgetEditDatePickerType {
    START, END
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
    var pickerType by remember { mutableStateOf<BudgetEditDatePickerType?>(null) }
    var addThreshold by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.saved, state.deleted) { if (state.saved || state.deleted) onBack() }
    val errors = if (state.showErrors) state.errors else emptySet()
    val colors = MaterialTheme.colorScheme
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
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
                    onClick = { pickerType = BudgetEditDatePickerType.START },
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
                            onClick = { pickerType = BudgetEditDatePickerType.END },
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

    pickerType?.let { which ->
        val initial = when (which) {
            BudgetEditDatePickerType.START -> state.startDate
            BudgetEditDatePickerType.END -> (state.endDate ?: state.startDate)
        }
        DatePickerModal(
            selectedDate = initial,
            onDateSelected = onDateSelected@{
                it ?: return@onDateSelected
                when (which) {
                    BudgetEditDatePickerType.START -> viewModel.onStartDate(it)
                    BudgetEditDatePickerType.END -> viewModel.onEndDate(it)
                }
                pickerType = null
            },
            onDismiss = {
                pickerType = null
            }
        )
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
        Box(
            Modifier
                .matchParentSize()
                .padding(end = endInset)
                .clickable(onClick = onClick)
        )
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
