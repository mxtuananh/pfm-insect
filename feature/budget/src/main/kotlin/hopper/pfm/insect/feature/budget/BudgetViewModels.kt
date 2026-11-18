package hopper.pfm.insect.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.budget.BudgetProgress
import hopper.pfm.insect.core.domain.budget.BudgetRepository
import hopper.pfm.insect.core.domain.budget.DEFAULT_ALERT_THRESHOLDS
import hopper.pfm.insect.core.domain.budget.MAX_ALERT_THRESHOLDS
import hopper.pfm.insect.core.domain.budget.MAX_ALERT_THRESHOLD_PERCENT
import hopper.pfm.insect.core.domain.budget.MAX_BUDGETS
import hopper.pfm.insect.core.domain.budget.computeProgress
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.expense.ExpenseRepository
import hopper.pfm.insect.core.domain.habit.HabitRepository
import hopper.pfm.insect.core.model.Budget
import hopper.pfm.insect.core.model.BudgetPeriod
import hopper.pfm.insect.core.model.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class BudgetListUiState(
    val loading: Boolean = true,
    /** The day whose periods are shown (today by default). */
    val date: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val items: List<BudgetProgress> = emptyList(),
    val categories: Map<String, Category> = emptyMap(),
    val currency: String = "VND",
    val canAddMore: Boolean = true,
)

/**
 * Progress is computed here from the local database (expenses of the year that contains the date, habits, budgets), so it
 * includes changes that are not synchronized yet and works offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetListViewModel @Inject constructor(
    budgets: BudgetRepository,
    expenses: ExpenseRepository,
    habits: HabitRepository,
    categories: CategoryRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val date = MutableStateFlow(LocalDate.now())

    val state: StateFlow<BudgetListUiState> = date.flatMapLatest { day ->
        val yearStart = day.withDayOfYear(1)
        val yearEnd = day.withDayOfYear(day.lengthOfYear())
        combine(
            budgets.observeAll(),
            expenses.observeBetween(yearStart, yearEnd),
            habits.observeAll(),
            categories.observeAll(),
            auth.profile,
        ) { allBudgets, yearExpenses, allHabits, cats, profile ->
            val items = computeProgress(allBudgets, yearExpenses, allHabits, day)
                // The most urgent first: exceeded, then warning, then ok; the fullest bar first inside a group.
                .sortedWith(compareByDescending<BudgetProgress> { it.status.ordinal }.thenByDescending { it.percentUsed })
            BudgetListUiState(
                loading = false,
                date = day,
                items = items,
                categories = cats.associateBy { it.id },
                currency = profile?.currency ?: "VND",
                canAddMore = allBudgets.size < MAX_BUDGETS,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetListUiState())

    fun setDate(value: LocalDate) {
        date.value = value
    }

    fun goToToday() {
        date.value = LocalDate.now()
    }
}

enum class BudgetFormError { AmountRequired, BadThresholds, EndBeforeStart, YearOutOfRange }

data class BudgetEditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val currency: String = "VND",
    /** null = all expenses. */
    val categoryId: String? = null,
    val period: BudgetPeriod = BudgetPeriod.MONTH,
    val amountText: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val recurring: Boolean = true,
    val endDate: LocalDate? = null,
    val thresholds: List<Int> = DEFAULT_ALERT_THRESHOLDS,
    val categories: List<Category> = emptyList(),
    val limitReached: Boolean = false,
    val showErrors: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val amount: Long? get() = Money.parse(amountText, currency)?.takeIf { it > 0 }

    val errors: Set<BudgetFormError>
        get() = buildSet {
            if (amount == null) add(BudgetFormError.AmountRequired)
            if (thresholds.isEmpty() || thresholds.size > MAX_ALERT_THRESHOLDS || thresholds.any { it !in 1..MAX_ALERT_THRESHOLD_PERCENT }) {
                add(BudgetFormError.BadThresholds)
            }
            if (recurring && endDate != null && endDate.isBefore(startDate)) add(BudgetFormError.EndBeforeStart)
            if (startDate.year !in 2000..2100 || (endDate?.year ?: 2000) !in 2000..2100) add(BudgetFormError.YearOutOfRange)
        }
}

@HiltViewModel(assistedFactory = BudgetEditViewModel.Factory::class)
class BudgetEditViewModel @AssistedInject constructor(
    @Assisted private val budgetId: String?,
    private val budgets: BudgetRepository,
    categories: CategoryRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(budgetId: String?): BudgetEditViewModel
    }

    private val _state = MutableStateFlow(BudgetEditUiState())
    val state: StateFlow<BudgetEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val currency = auth.profile.first()?.currency ?: "VND"
            val existing = budgetId?.let { budgets.get(it) }
            val count = budgets.observeAll().first().size
            _state.update {
                if (existing == null) {
                    it.copy(loaded = true, currency = currency, limitReached = count >= MAX_BUDGETS)
                } else {
                    it.copy(
                        loaded = true,
                        isNew = false,
                        currency = currency,
                        categoryId = existing.categoryId,
                        period = existing.period,
                        amountText = Money.format(existing.amount, currency),
                        startDate = existing.startDate,
                        recurring = existing.recurring,
                        endDate = existing.endDate,
                        thresholds = existing.alertThresholds,
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

    fun onCategory(id: String?) = _state.update { it.copy(categoryId = id) }
    fun onPeriod(period: BudgetPeriod) = _state.update { it.copy(period = period) }
    fun onStartDate(date: LocalDate) = _state.update { it.copy(startDate = date) }
    fun onRecurring(value: Boolean) = _state.update { it.copy(recurring = value, endDate = if (value) it.endDate else null) }
    fun onEndDate(date: LocalDate?) = _state.update { it.copy(endDate = date) }

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

    fun removeThreshold(percent: Int) = _state.update { it.copy(thresholds = it.thresholds - percent) }

    /** Adds a threshold (1 to 500 percent, at most five, no duplicates). Returns false when it cannot be added. */
    fun addThreshold(percent: Int): Boolean {
        val s = _state.value
        if (percent !in 1..MAX_ALERT_THRESHOLD_PERCENT || percent in s.thresholds || s.thresholds.size >= MAX_ALERT_THRESHOLDS) return false
        _state.update { it.copy(thresholds = (it.thresholds + percent).sorted()) }
        return true
    }

    fun save() {
        val s = _state.value
        val amount = s.amount
        if (!s.loaded || s.errors.isNotEmpty() || amount == null || (s.isNew && s.limitReached)) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            budgets.save(
                Budget(
                    id = budgetId ?: UUID.randomUUID().toString(),
                    categoryId = s.categoryId,
                    period = s.period,
                    amount = amount,
                    startDate = s.startDate,
                    endDate = s.endDate.takeIf { s.recurring },
                    recurring = s.recurring,
                    alertThresholds = s.thresholds.sorted(),
                ),
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val id = budgetId ?: return
        viewModelScope.launch {
            budgets.delete(id)
            _state.update { it.copy(deleted = true) }
        }
    }
}
