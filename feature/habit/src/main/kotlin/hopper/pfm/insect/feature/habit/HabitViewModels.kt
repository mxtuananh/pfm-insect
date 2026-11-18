package hopper.pfm.insect.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.expense.ExpenseRepository
import hopper.pfm.insect.core.domain.habit.HabitRepository
import hopper.pfm.insect.core.domain.habit.MAX_HABITS
import hopper.pfm.insect.core.domain.habit.sortedForToday
import hopper.pfm.insect.core.domain.habit.toExpense
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class HabitListUiState(
    val loading: Boolean = true,
    /** Habits planned for today first, then the rest, each group in the order of the user. */
    val habits: List<Habit> = emptyList(),
    val categories: Map<String, Category> = emptyMap(),
    val currency: String = "VND",
    val today: LocalDate = LocalDate.now(),
    val canAddMore: Boolean = true,
)

@HiltViewModel
class HabitListViewModel @Inject constructor(
    private val habits: HabitRepository,
    private val expenses: ExpenseRepository,
    categories: CategoryRepository,
    auth: AuthRepository,
) : ViewModel() {

    val state: StateFlow<HabitListUiState> = combine(habits.observeAll(), categories.observeAll(), auth.profile) { list, cats, profile ->
        val today = LocalDate.now()
        HabitListUiState(
            loading = false,
            habits = list.sortedForToday(today),
            categories = cats.associateBy { it.id },
            currency = profile?.currency ?: "VND",
            today = today,
            canAddMore = list.size < MAX_HABITS,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitListUiState())

    /** The expense created by the last quick add, kept for the undo action of the snackbar. */
    private val _lastAdded = MutableStateFlow<Pair<Habit, Expense>?>(null)
    val lastAdded: StateFlow<Pair<Habit, Expense>?> = _lastAdded.asStateFlow()

    /** One tap: the habit becomes an ordinary expense dated today (written locally, so it works offline). */
    fun quickAdd(habit: Habit) {
        viewModelScope.launch {
            val expense = habit.toExpense(LocalDate.now(), UUID.randomUUID().toString())
            expenses.save(expense)
            _lastAdded.value = habit to expense
        }
    }

    fun undoQuickAdd() {
        val added = _lastAdded.value ?: return
        _lastAdded.value = null
        viewModelScope.launch { expenses.delete(added.second.id) }
    }

    fun clearLastAdded() {
        _lastAdded.value = null
    }

    fun move(habit: Habit, direction: Int) {
        viewModelScope.launch { habits.move(habit.id, direction) }
    }
}

enum class HabitFormError { AmountRequired, TitleRequired, NoDays }

data class HabitEditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val currency: String = "VND",
    val amountText: String = "",
    val title: String = "",
    val categoryId: String? = null,
    val days: Set<Int> = (1..7).toSet(),
    /** "HH:mm" or null. */
    val timeOfDay: String? = null,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val showErrors: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val amount: Long? get() = Money.parse(amountText, currency)?.takeIf { it > 0 }

    val errors: Set<HabitFormError>
        get() = buildSet {
            if (amount == null) add(HabitFormError.AmountRequired)
            if (title.trim().isEmpty()) add(HabitFormError.TitleRequired)
            if (days.isEmpty()) add(HabitFormError.NoDays)
        }
}

@HiltViewModel(assistedFactory = HabitEditViewModel.Factory::class)
class HabitEditViewModel @AssistedInject constructor(
    @Assisted private val habitId: String?,
    private val habits: HabitRepository,
    categories: CategoryRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(habitId: String?): HabitEditViewModel
    }

    private val _state = MutableStateFlow(HabitEditUiState())
    val state: StateFlow<HabitEditUiState> = _state.asStateFlow()

    private var original: Habit? = null

    init {
        viewModelScope.launch {
            val currency = auth.profile.first()?.currency ?: "VND"
            val existing = habitId?.let { habits.get(it) }
            original = existing
            _state.update {
                if (existing == null) {
                    it.copy(loaded = true, currency = currency)
                } else {
                    it.copy(
                        loaded = true,
                        isNew = false,
                        currency = currency,
                        amountText = Money.format(existing.amount, currency),
                        title = existing.title,
                        categoryId = existing.categoryId,
                        days = existing.daysOfWeek,
                        timeOfDay = existing.timeOfDay,
                        note = existing.note.orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            categories.observeAll().collect { all ->
                _state.update { s -> s.copy(categories = all.filter { !it.archived || it.id == s.categoryId }.sortedBy { it.name.lowercase() }) }
            }
        }
    }

    fun onAmount(value: String) = _state.update { s ->
        val text = if (Money.decimals(s.currency) == 0) {
            val digits = value.filter { it.isDigit() }
            if (digits.isEmpty()) "" else Money.parse(digits, s.currency)?.let { Money.format(it, s.currency) } ?: s.amountText
        } else {
            val cleaned = value.filter { it.isDigit() || it == '.' }
            if (cleaned.isEmpty() || Money.parse(cleaned.trimEnd('.').ifEmpty { "0" }, s.currency) != null && cleaned.count { it == '.' } <= 1) cleaned else s.amountText
        }
        s.copy(amountText = text)
    }

    fun onTitle(value: String) = _state.update { it.copy(title = value.take(MAX_TITLE)) }
    fun onCategory(id: String?) = _state.update { it.copy(categoryId = if (it.categoryId == id) null else id) }
    fun onNote(value: String) = _state.update { it.copy(note = value.take(MAX_NOTE)) }
    fun onTime(value: String?) = _state.update { it.copy(timeOfDay = value) }

    fun toggleDay(day: Int) = _state.update { s ->
        s.copy(days = if (day in s.days) s.days - day else s.days + day)
    }

    fun save() {
        val s = _state.value
        val amount = s.amount
        if (!s.loaded || s.errors.isNotEmpty() || amount == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            habits.save(
                Habit(
                    id = habitId ?: UUID.randomUUID().toString(),
                    title = s.title,
                    note = s.note,
                    amount = amount,
                    categoryId = s.categoryId,
                    daysOfWeek = s.days,
                    timeOfDay = s.timeOfDay,
                    // New habits go to the end of the list; an existing one keeps its place.
                    sortOrder = original?.sortOrder ?: -1,
                ),
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val existing = original ?: return
        viewModelScope.launch {
            habits.delete(existing.id)
            _state.update { it.copy(deleted = true) }
        }
    }

    companion object {
        const val MAX_TITLE = 100
        const val MAX_NOTE = 500
    }
}
