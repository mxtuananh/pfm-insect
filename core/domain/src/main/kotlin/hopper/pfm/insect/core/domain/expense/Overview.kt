package hopper.pfm.insect.core.domain.expense

import hopper.pfm.insect.core.model.Expense
import java.time.LocalDate

/** Total spent on one day (a bar of the week or month chart). */
data class DayTotal(val date: LocalDate, val amount: Long)

data class CategoryTotal(val categoryId: String?, val amount: Long)

data class DayGroup(val date: LocalDate, val total: Long, val items: List<Expense>)

/**
 * Everything the expense screen shows for one period, computed locally from the database (including changes that are
 * not synchronized yet). Expenses only have a date, not a time, so the day view has no hourly chart: it breaks the
 * day down by category instead.
 */
data class ExpenseOverview(
    val period: Period,
    val total: Long,
    /** Total of the previous period of the same kind, with the same category filter. */
    val previousTotal: Long,
    /** One entry per day of the period (days without spending are 0). */
    val dayTotals: List<DayTotal>,
    /** Biggest first. Not affected by the category filter, so the chips keep showing the whole picture. */
    val categoryTotals: List<CategoryTotal>,
    /** Newest day first, then the order given by the repository. */
    val groups: List<DayGroup>,
) {
    /** Change against the previous period in percent, or null when there is nothing to compare with. */
    val deltaPercent: Int?
        get() = if (previousTotal <= 0) null else (((total - previousTotal) * 100.0) / previousTotal).let { Math.round(it).toInt() }
}

/**
 * Builds the overview. [expenses] must contain the period and the previous period (the caller loads both ranges).
 * With [categoryFilter] set, the total, chart and list only count that category.
 */
fun buildOverview(
    period: Period,
    previous: Period,
    expenses: List<Expense>,
    categoryFilter: String? = null,
): ExpenseOverview {
    val inPeriod = expenses.filter { it.spentOn in period }
    val inPrevious = expenses.filter { it.spentOn in previous }
    val filtered = if (categoryFilter == null) inPeriod else inPeriod.filter { it.categoryId == categoryFilter }
    val previousFiltered = if (categoryFilter == null) inPrevious else inPrevious.filter { it.categoryId == categoryFilter }

    val perDay = filtered.groupBy { it.spentOn }
    val dayTotals = (0 until period.days).map { offset ->
        val date = period.start.plusDays(offset.toLong())
        DayTotal(date, perDay[date].orEmpty().sumOf { it.amount })
    }
    val categoryTotals = inPeriod.groupBy { it.categoryId }
        .map { (id, items) -> CategoryTotal(id, items.sumOf { it.amount }) }
        .sortedByDescending { it.amount }
    val groups = perDay.entries.sortedByDescending { it.key }
        .map { (date, items) -> DayGroup(date, items.sumOf { it.amount }, items) }

    return ExpenseOverview(
        period = period,
        total = filtered.sumOf { it.amount },
        previousTotal = previousFiltered.sumOf { it.amount },
        dayTotals = dayTotals,
        categoryTotals = categoryTotals,
        groups = groups,
    )
}
