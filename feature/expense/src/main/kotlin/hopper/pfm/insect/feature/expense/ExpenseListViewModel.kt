package hopper.pfm.insect.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.expense.ExpenseOverview
import hopper.pfm.insect.core.domain.expense.ExpenseRepository
import hopper.pfm.insect.core.domain.expense.Period
import hopper.pfm.insect.core.domain.expense.PeriodType
import hopper.pfm.insect.core.domain.expense.UserSettings
import hopper.pfm.insect.core.domain.expense.buildOverview
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.Expense
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class ExpenseListUiState(
    val loading: Boolean = true,
    val periodType: PeriodType = PeriodType.MONTH,
    val period: Period = Period.of(PeriodType.MONTH, LocalDate.now(), DayOfWeek.MONDAY),
    val categoryFilter: String? = null,
    val currency: String = "VND",
    val overview: ExpenseOverview? = null,
    val categories: Map<String, Category> = emptyMap(),
    /** The bar the user tapped in the chart, shown with a tooltip. */
    val selectedDay: LocalDate? = null,
    val today: LocalDate = LocalDate.now(),
) {
    val isCurrentPeriod: Boolean get() = today in period
}

private data class Selection(
    val type: PeriodType = PeriodType.MONTH,
    val anchor: LocalDate = LocalDate.now(),
    val categoryFilter: String? = null,
    val selectedDay: LocalDate? = null,
)

/**
 * All numbers come from the local database (so unsynchronized changes are included and the screen works offline).
 * The period, its predecessor and the category filter pick what is observed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenses: ExpenseRepository,
    categories: CategoryRepository,
    auth: AuthRepository,
    settings: UserSettings,
) : ViewModel() {

    private val selection = MutableStateFlow(Selection())
    private val weekStart: StateFlow<DayOfWeek> =
        settings.weekStart.stateIn(viewModelScope, SharingStarted.Eagerly, DayOfWeek.MONDAY)

    /** The expense removed last, kept so the undo action of the snackbar can bring it back. */
    private val _lastDeleted = MutableStateFlow<Expense?>(null)
    val lastDeleted: StateFlow<Expense?> = _lastDeleted

    val state: StateFlow<ExpenseListUiState> = combine(selection, weekStart) { s, weekStart -> s to weekStart }
        .flatMapLatest { (s, weekStart) ->
            val period = Period.of(s.type, s.anchor, weekStart)
            val previous = period.shifted(-1, weekStart)
            combine(
                expenses.observeBetween(previous.start, period.end),
                categories.observeAll(),
                auth.profile,
            ) { items, cats, profile ->
                ExpenseListUiState(
                    loading = false,
                    periodType = s.type,
                    period = period,
                    categoryFilter = s.categoryFilter,
                    currency = profile?.currency ?: "VND",
                    overview = buildOverview(period, previous, items, s.categoryFilter),
                    categories = cats.associateBy { it.id },
                    selectedDay = s.selectedDay?.takeIf { it in period },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseListUiState())

    fun setPeriodType(type: PeriodType) = selection.update { it.copy(type = type, selectedDay = null) }

    /** Moves to the previous (-1) or next (+1) period; the anchor stays inside the new period. */
    fun shift(by: Int) = selection.update { s ->
        val period = Period.of(s.type, s.anchor, weekStart.value).shifted(by, weekStart.value)
        s.copy(anchor = period.start, selectedDay = null)
    }

    fun goToToday() = selection.update { it.copy(anchor = LocalDate.now(), selectedDay = null) }

    fun filterByCategory(id: String?) = selection.update { it.copy(categoryFilter = if (it.categoryFilter == id) null else id) }

    fun selectDay(date: LocalDate?) = selection.update { it.copy(selectedDay = if (it.selectedDay == date) null else date) }

    fun delete(expense: Expense) {
        viewModelScope.launch {
            expenses.delete(expense.id)
            _lastDeleted.value = expense
        }
    }

    fun undoDelete() {
        val expense = _lastDeleted.value ?: return
        _lastDeleted.value = null
        viewModelScope.launch { expenses.save(expense) }
    }

    fun clearLastDeleted() {
        _lastDeleted.value = null
    }

}
