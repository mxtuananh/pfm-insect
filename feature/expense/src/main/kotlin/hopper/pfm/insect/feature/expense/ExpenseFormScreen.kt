package hopper.pfm.insect.feature.expense

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.model.SyncStatus
import hopper.pfm.insect.core.ui.CategoryAvatar
import hopper.pfm.insect.core.ui.PfmBanner
import hopper.pfm.insect.core.ui.PfmPrimaryButton
import hopper.pfm.insect.core.ui.PfmTextField
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ExpenseFormScreen(
    expenseId: String?,
    onBack: () -> Unit,
    onNewCategory: () -> Unit,
    onDeleted: (deleted: hopper.pfm.insect.core.model.Expense) -> Unit,
    viewModel: ExpenseFormViewModel = hiltViewModel<ExpenseFormViewModel, ExpenseFormViewModel.Factory>(
        key = "expense-$expenseId",
        creationCallback = { factory -> factory.create(expenseId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    LaunchedEffect(state.deleted) { state.deleted?.let(onDeleted) }

    val errors = if (state.showErrors) state.errors else emptySet()
    val symbol = Money.symbol(state.currency)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.form_back))
                }
                Text(
                    stringResource(if (state.isNew) R.string.form_new_title else R.string.form_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (!state.isNew) {
                    IconButton(onClick = viewModel::delete) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.form_delete))
                    }
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            ) {
                when (state.syncStatus) {
                    SyncStatus.PENDING -> BannerRow(stringResource(R.string.form_pending_banner), tertiary = true)
                    SyncStatus.FAILED -> BannerRow(stringResource(R.string.form_rejected_banner), tertiary = false)
                    SyncStatus.SYNCED -> Unit
                }
                AmountField(
                    value = state.amountText,
                    symbol = symbol,
                    label = stringResource(R.string.form_amount),
                    error = if (FormError.AmountRequired in errors) stringResource(R.string.form_amount_error) else null,
                    keyboardType = if (Money.decimals(state.currency) > 0) KeyboardType.Decimal else KeyboardType.Number,
                    onChange = viewModel::onAmount,
                    autoFocus = state.isNew && state.loaded,
                )
                Spacer(Modifier.height(16.dp))
                PfmTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitle,
                    label = stringResource(R.string.form_title),
                    errorText = if (FormError.TitleRequired in errors) stringResource(R.string.form_title_error) else null,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.form_category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { viewModel.onCategory(category.id) },
                            label = { Text(category.name) },
                            leadingIcon = { CategoryAvatar(category.icon, category.color, size = 22.dp) },
                            shape = CircleShape,
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = onNewCategory,
                        label = { Text(stringResource(R.string.form_new_category)) },
                        shape = CircleShape,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Box {
                    PfmTextField(
                        value = state.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        onValueChange = {},
                        label = stringResource(R.string.form_date),
                        readOnly = true,
                        errorText = if (FormError.DateOutOfRange in errors) stringResource(R.string.form_date_error) else null,
                        trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    )
                    Box(Modifier.matchParentSize().clickable { pickDate = true })
                }
                Spacer(Modifier.height(16.dp))
                PfmTextField(
                    value = state.note,
                    onValueChange = viewModel::onNote,
                    label = stringResource(R.string.form_note),
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::save,
                )
                Spacer(Modifier.height(16.dp))
            }
            PfmPrimaryButton(
                text = stringResource(R.string.form_save),
                onClick = viewModel::save,
                enabled = state.loaded,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
    if (pickDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.form_date_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.form_date_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun BannerRow(text: String, tertiary: Boolean) {
    val colors = MaterialTheme.colorScheme
    PfmBanner(
        text = text,
        containerColor = if (tertiary) colors.tertiaryContainer else colors.errorContainer,
        contentColor = if (tertiary) colors.onTertiaryContainer else colors.onErrorContainer,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

/** The big amount input of the design: a large number with the currency symbol and an underline. */
@Composable
private fun AmountField(
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
                if (value.isEmpty()) {
                    Text("0", style = MaterialTheme.typography.displayMedium, color = colors.onSurfaceVariant.copy(alpha = 0.5f))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displayMedium.copy(color = colors.onSurface, textAlign = TextAlign.Start),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            Text(symbol, style = MaterialTheme.typography.headlineMedium, color = colors.onSurfaceVariant)
        }
        HorizontalDivider(thickness = 2.dp, color = accent)
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
