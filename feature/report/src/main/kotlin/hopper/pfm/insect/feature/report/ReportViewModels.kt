package hopper.pfm.insect.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.domain.expense.CategoryRepository
import hopper.pfm.insect.core.domain.expense.DayGroup
import hopper.pfm.insect.core.domain.expense.ExpenseRepository
import hopper.pfm.insect.core.domain.report.MAX_CUSTOM_DAYS
import hopper.pfm.insect.core.domain.report.Report
import hopper.pfm.insect.core.domain.report.ReportKind
import hopper.pfm.insect.core.domain.report.ReportRange
import hopper.pfm.insect.core.domain.report.buildReport
import hopper.pfm.insect.core.domain.report.previousRange
import hopper.pfm.insect.core.model.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class RangeProblem { FromAfterTo, TooLong }

data class ReportUiState(
    val loading: Boolean = true,
    val kind: ReportKind = ReportKind.MONTH,
    val month: YearMonth = YearMonth.now(),
    val year: Int = LocalDate.now().year,
    val customFrom: LocalDate = YearMonth.now().atDay(1),
    val customTo: LocalDate = LocalDate.now(),
    val problem: RangeProblem? = null,
    val report: Report? = null,
    val categories: Map<String, Category> = emptyMap(),
    val currency: String = "VND",
    val today: LocalDate = LocalDate.now(),
)

private data class Selection(
    val kind: ReportKind = ReportKind.MONTH,
    val month: YearMonth = YearMonth.now(),
    val year: Int = LocalDate.now().year,
    val customFrom: LocalDate = YearMonth.now().atDay(1),
    val customTo: LocalDate = LocalDate.now(),
) {
    fun problem(): RangeProblem? = if (kind != ReportKind.CUSTOM) null else when {
        customFrom.isAfter(customTo) -> RangeProblem.FromAfterTo
        customTo.toEpochDay() - customFrom.toEpochDay() + 1 > MAX_CUSTOM_DAYS -> RangeProblem.TooLong
        else -> null
    }

    fun range(): ReportRange = when (kind) {
        ReportKind.MONTH -> ReportRange.month(month)
        ReportKind.YEAR -> ReportRange.year(year)
        ReportKind.CUSTOM -> ReportRange(customFrom, customTo)
    }
}

/** Everything is computed from the local database; the range and its predecessor decide what is observed. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val expenses: ExpenseRepository,
    categories: CategoryRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val selection = MutableStateFlow(Selection())

    val state: StateFlow<ReportUiState> = selection.flatMapLatest { s ->
        val problem = s.problem()
        val today = LocalDate.now()
        if (problem != null) {
            flowOf(
                ReportUiState(
                    loading = false, kind = s.kind, month = s.month, year = s.year,
                    customFrom = s.customFrom, customTo = s.customTo, problem = problem, today = today,
                ),
            )
        } else {
            val range = s.range()
            val previous = previousRange(range, s.kind)
            combine(
                expenses.observeBetween(previous.start, range.end),
                categories.observeAll(),
                auth.profile,
            ) { items, cats, profile ->
                ReportUiState(
                    loading = false,
                    kind = s.kind,
                    month = s.month,
                    year = s.year,
                    customFrom = s.customFrom,
                    customTo = s.customTo,
                    report = buildReport(s.kind, range, items, today),
                    categories = cats.associateBy { it.id },
                    currency = profile?.currency ?: "VND",
                    today = today,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun setKind(kind: ReportKind) = selection.update { it.copy(kind = kind) }

    fun shift(by: Int) = selection.update {
        when (it.kind) {
            ReportKind.MONTH -> it.copy(month = it.month.plusMonths(by.toLong()))
            ReportKind.YEAR -> it.copy(year = it.year + by)
            ReportKind.CUSTOM -> it
        }
    }

    fun setCustomFrom(date: LocalDate) = selection.update { it.copy(customFrom = date) }

    fun setCustomTo(date: LocalDate) = selection.update { it.copy(customTo = date) }
}

data class CategoryDetailUiState(
    val loading: Boolean = true,
    val category: Category? = null,
    val total: Long = 0,
    /** Total of all categories in the range, to show the share of this one. */
    val rangeTotal: Long = 0,
    val count: Int = 0,
    val groups: List<DayGroup> = emptyList(),
    val currency: String = "VND",
    val today: LocalDate = LocalDate.now(),
)

/** The expenses of one category (null = expenses without a category) in a range, grouped by day. */
@HiltViewModel(assistedFactory = CategoryDetailViewModel.Factory::class)
class CategoryDetailViewModel @AssistedInject constructor(
    @Assisted private val categoryId: String?,
    @Assisted("from") private val from: String,
    @Assisted("to") private val to: String,
    expenses: ExpenseRepository,
    categories: CategoryRepository,
    auth: AuthRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(categoryId: String?, @Assisted("from") from: String, @Assisted("to") to: String): CategoryDetailViewModel
    }

    val state: StateFlow<CategoryDetailUiState> = combine(
        expenses.observeBetween(LocalDate.parse(from), LocalDate.parse(to)),
        categories.observeAll(),
        auth.profile,
    ) { items, cats, profile ->
        val mine = items.filter { it.categoryId == categoryId }
        CategoryDetailUiState(
            loading = false,
            category = categoryId?.let { id -> cats.firstOrNull { it.id == id } },
            total = mine.sumOf { it.amount },
            rangeTotal = items.sumOf { it.amount },
            count = mine.size,
            groups = mine.groupBy { it.spentOn }.entries.sortedByDescending { it.key }
                .map { (date, list) -> DayGroup(date, list.sumOf { it.amount }, list) },
            currency = profile?.currency ?: "VND",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryDetailUiState())
}
