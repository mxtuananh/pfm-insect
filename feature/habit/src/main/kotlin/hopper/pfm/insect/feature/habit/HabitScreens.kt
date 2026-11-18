package hopper.pfm.insect.feature.habit

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.domain.habit.isDueOn
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.Habit
import hopper.pfm.insect.core.model.SyncStatus
import hopper.pfm.insect.core.ui.CategoryAvatar
import hopper.pfm.insect.core.ui.PfmPrimaryButton
import hopper.pfm.insect.core.ui.PfmTextField

private val DayLabels = listOf(
    1 to R.string.day_mon, 2 to R.string.day_tue, 3 to R.string.day_wed, 4 to R.string.day_thu,
    5 to R.string.day_fri, 6 to R.string.day_sat, 7 to R.string.day_sun,
)

@Composable
private fun daysSummary(days: Set<Int>): String {
    val everyDay = stringResource(R.string.habit_every_day)
    val weekdays = stringResource(R.string.habit_weekdays)
    val names = DayLabels.map { it.first to stringResource(it.second) }
    return when {
        days.size == 7 -> everyDay
        days == setOf(1, 2, 3, 4, 5) -> weekdays
        else -> names.filter { it.first in days }.joinToString(" ") { it.second }
    }
}

@Composable
fun HabitListScreen(
    onBack: () -> Unit,
    onEdit: (habitId: String?) -> Unit,
    viewModel: HabitListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastAdded by viewModel.lastAdded.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val undoText = stringResource(R.string.habit_undo)
    val added = lastAdded
    val addedMessage = added?.let {
        stringResource(R.string.habit_added, it.first.title, Money.formatWithSymbol(it.second.amount, state.currency))
    }

    LaunchedEffect(lastAdded) {
        if (addedMessage != null) {
            val result = snackbar.showSnackbar(addedMessage, actionLabel = undoText, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoQuickAdd() else viewModel.clearLastAdded()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.habit_back))
                    }
                    Text(stringResource(R.string.habit_title), style = MaterialTheme.typography.titleLarge)
                }
                if (state.habits.isEmpty() && !state.loading) {
                    EmptyHabits()
                } else {
                    Text(
                        stringResource(R.string.habit_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.habits, key = { it.id }) { habit ->
                            val index = state.habits.indexOf(habit)
                            HabitRow(
                                habit = habit,
                                category = habit.categoryId?.let { state.categories[it] },
                                currency = state.currency,
                                dueToday = habit.isDueOn(state.today),
                                canMoveUp = index > 0,
                                canMoveDown = index < state.habits.lastIndex,
                                onClick = { onEdit(habit.id) },
                                onQuickAdd = { viewModel.quickAdd(habit) },
                                onMove = { direction -> viewModel.move(habit, direction) },
                            )
                        }
                    }
                }
            }
            if (state.canAddMore) {
                ExtendedFloatingActionButton(
                    onClick = { onEdit(null) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.habit_add)) },
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
                )
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 88.dp),
            )
        }
    }
}

@Composable
private fun EmptyHabits() {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(stringResource(R.string.habit_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.habit_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HabitRow(
    habit: Habit,
    category: Category?,
    currency: String,
    dueToday: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    onMove: (Int) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerLow,
        border = if (dueToday) BorderStroke(1.5.dp, colors.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryAvatar(category?.icon, category?.color)
            Column(Modifier.weight(1f)) {
                Text(habit.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val schedule = daysSummary(habit.daysOfWeek) + (habit.timeOfDay?.let { " · $it" } ?: "")
                val marker = when (habit.syncStatus) {
                    SyncStatus.PENDING -> " · ⟳ " + stringResource(R.string.habit_pending)
                    SyncStatus.FAILED -> " · ⚠ " + stringResource(R.string.habit_rejected)
                    SyncStatus.SYNCED -> ""
                }
                Text(
                    schedule + marker,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (habit.syncStatus == SyncStatus.FAILED) colors.error else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(Money.formatWithSymbol(habit.amount, currency), style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalIconButton(
                onClick = onQuickAdd,
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.habit_quick_add))
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.habit_menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.habit_edit)) }, onClick = { menu = false; onClick() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.habit_move_up)) }, enabled = canMoveUp, onClick = { menu = false; onMove(-1) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.habit_move_down)) }, enabled = canMoveDown, onClick = { menu = false; onMove(1) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditScreen(
    habitId: String?,
    onBack: () -> Unit,
    onNewCategory: () -> Unit,
    viewModel: HabitEditViewModel = hiltViewModel<HabitEditViewModel, HabitEditViewModel.Factory>(
        key = "habit-$habitId",
        creationCallback = { factory -> factory.create(habitId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickTime by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.saved, state.deleted) { if (state.saved || state.deleted) onBack() }
    val errors = if (state.showErrors) state.errors else emptySet()
    val colors = MaterialTheme.colorScheme

    Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.habit_back))
                }
                Text(
                    stringResource(if (state.isNew) R.string.habit_new_title else R.string.habit_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (!state.isNew) {
                    IconButton(onClick = viewModel::delete) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.habit_delete))
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                AmountInput(
                    value = state.amountText,
                    symbol = Money.symbol(state.currency),
                    label = stringResource(R.string.habit_amount),
                    error = if (HabitFormError.AmountRequired in errors) stringResource(R.string.habit_amount_error) else null,
                    keyboardType = if (Money.decimals(state.currency) > 0) KeyboardType.Decimal else KeyboardType.Number,
                    onChange = viewModel::onAmount,
                    autoFocus = state.isNew && state.loaded,
                )
                Spacer(Modifier.height(16.dp))
                PfmTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitle,
                    label = stringResource(R.string.habit_name),
                    errorText = if (HabitFormError.TitleRequired in errors) stringResource(R.string.habit_name_error) else null,
                )
                Spacer(Modifier.height(16.dp))
                Label(stringResource(R.string.habit_category))
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
                    FilterChip(selected = false, onClick = onNewCategory, label = { Text("＋") }, shape = CircleShape)
                }
                Spacer(Modifier.height(16.dp))
                Label(stringResource(R.string.habit_days))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayLabels.forEach { (day, label) ->
                        val selected = day in state.days
                        Box(
                            Modifier.size(40.dp)
                                .background(if (selected) colors.primary else colors.surfaceContainerHighest, CircleShape)
                                .clickable { viewModel.toggleDay(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.onPrimary else colors.onSurface,
                            )
                        }
                    }
                }
                if (HabitFormError.NoDays in errors) {
                    Text(stringResource(R.string.habit_days_error), style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
                Spacer(Modifier.height(16.dp))
                Box {
                    PfmTextField(
                        value = state.timeOfDay.orEmpty(),
                        onValueChange = {},
                        label = stringResource(R.string.habit_time),
                        readOnly = true,
                        supportingText = stringResource(R.string.habit_time_hint),
                        trailingIcon = {
                            if (state.timeOfDay != null) {
                                IconButton(onClick = { viewModel.onTime(null) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.habit_time_clear))
                                }
                            } else {
                                Icon(Icons.Filled.AccessTime, contentDescription = null)
                            }
                        },
                    )
                    Box(Modifier.matchParentSize().padding(end = if (state.timeOfDay == null) 0.dp else 56.dp).clickable { pickTime = true })
                }
                Spacer(Modifier.height(16.dp))
                PfmTextField(
                    value = state.note,
                    onValueChange = viewModel::onNote,
                    label = stringResource(R.string.habit_note),
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::save,
                )
                Spacer(Modifier.height(16.dp))
            }
            PfmPrimaryButton(
                text = stringResource(R.string.habit_save),
                onClick = viewModel::save,
                enabled = state.loaded,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
    if (pickTime) {
        val initial = state.timeOfDay?.split(':')
        val pickerState = rememberTimePickerState(
            initialHour = initial?.getOrNull(0)?.toIntOrNull() ?: 7,
            initialMinute = initial?.getOrNull(1)?.toIntOrNull() ?: 30,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTime("%02d:%02d".format(pickerState.hour, pickerState.minute))
                    pickTime = false
                }) { Text(stringResource(R.string.habit_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.habit_cancel)) } },
            text = { TimePicker(state = pickerState) },
        )
    }
}

@Composable
private fun Label(text: String) {
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
                if (value.isEmpty()) Text("0", style = MaterialTheme.typography.displayMedium, color = colors.onSurfaceVariant.copy(alpha = 0.5f))
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displayMedium.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            Text(symbol, style = MaterialTheme.typography.headlineMedium, color = colors.onSurfaceVariant)
        }
        HorizontalDivider(thickness = 2.dp, color = accent)
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(top = 4.dp))
    }
}
