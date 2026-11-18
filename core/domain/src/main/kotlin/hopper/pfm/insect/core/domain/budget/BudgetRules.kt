package hopper.pfm.insect.core.domain.budget

import hopper.pfm.insect.core.model.Budget
import hopper.pfm.insect.core.model.BudgetPeriod
import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.model.Habit
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/*
 * Budget rules, ported from the server (BudgetPeriods.kt) so that the numbers shown in the app are computed from the
 * local database, including expenses that have not been synchronized yet. Keep them identical to the server.
 */

const val MAX_BUDGETS = 100
const val MAX_ALERT_THRESHOLDS = 5
const val MAX_ALERT_THRESHOLD_PERCENT = 500
val DEFAULT_ALERT_THRESHOLDS = listOf(80, 100)

/** First and last day of a period, both included. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(end)
}

/** The calendar day, month or year that contains the date. */
fun BudgetPeriod.rangeContaining(date: LocalDate): DateRange = when (this) {
    BudgetPeriod.DAY -> DateRange(date, date)
    BudgetPeriod.MONTH -> DateRange(date.withDayOfMonth(1), date.with(TemporalAdjusters.lastDayOfMonth()))
    BudgetPeriod.YEAR -> DateRange(date.withDayOfYear(1), date.with(TemporalAdjusters.lastDayOfYear()))
}

/**
 * Whether the budget applies to the period containing the date: from the period that contains its start date on;
 * a non-repeating budget only to that one period; a repeating one until the period that begins after its end date.
 */
fun Budget.isActiveOn(date: LocalDate): Boolean {
    val first = period.rangeContaining(startDate)
    val current = period.rangeContaining(date)
    return when {
        current.start < first.start -> false
        !recurring && current != first -> false
        endDate != null && current.start > endDate -> false
        else -> true
    }
}

enum class BudgetStatus {
    /** Below every alert threshold. */
    OK,

    /** At or above a threshold, but not over the amount. */
    WARNING,

    /** More spent than the amount (exactly equal is not exceeded). */
    EXCEEDED,
}

data class BudgetUsage(val percentUsed: Long, val reachedThresholds: List<Int>, val status: BudgetStatus)

fun evaluateUsage(spent: Long, amount: Long, thresholds: List<Int>): BudgetUsage {
    // spent / amount compared to each threshold without rounding: spent * 100 >= threshold * amount.
    val reached = thresholds.sorted().filter { spent * 100 >= it.toLong() * amount }
    val status = when {
        spent > amount -> BudgetStatus.EXCEEDED
        reached.isNotEmpty() -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }
    return BudgetUsage(spent * 100 / amount, reached, status)
}

/** How many times each weekday occurs in the range; index 0 = Monday ... 6 = Sunday. */
fun weekdayCounts(range: DateRange): IntArray {
    val counts = IntArray(7)
    var day = range.start
    while (!day.isAfter(range.end)) {
        counts[day.dayOfWeek.value - 1]++
        day = day.plusDays(1)
    }
    return counts
}

/** What the habits would cost over the range if each were done on every weekday it applies to. */
fun plannedAmount(habits: List<Habit>, range: DateRange): Long {
    val counts = weekdayCounts(range)
    return habits.sumOf { habit -> habit.amount * habit.daysOfWeek.sumOf { day -> counts[day - 1].toLong() } }
}

data class BudgetProgress(
    val budget: Budget,
    val range: DateRange,
    val spent: Long,
    /** Amount minus spent; negative when exceeded. */
    val remaining: Long,
    val percentUsed: Long,
    val status: BudgetStatus,
    val reachedThresholds: List<Int>,
    val plannedByHabits: Long,
)

/**
 * Progress of every budget that applies to the date. The expenses must cover at least the calendar period of each
 * applicable budget (the caller passes the whole year of the date). A habit counts towards an overall budget and
 * towards the budget of its own category.
 */
fun computeProgress(budgets: List<Budget>, expenses: List<Expense>, habits: List<Habit>, date: LocalDate): List<BudgetProgress> =
    budgets.filter { it.isActiveOn(date) }.map { budget ->
        val range = budget.period.rangeContaining(date)
        val spent = expenses
            .filter { it.spentOn in range && (budget.categoryId == null || it.categoryId == budget.categoryId) }
            .sumOf { it.amount }
        val usage = evaluateUsage(spent, budget.amount, budget.alertThresholds)
        val relevantHabits = habits.filter { budget.categoryId == null || it.categoryId == budget.categoryId }
        BudgetProgress(
            budget = budget,
            range = range,
            spent = spent,
            remaining = budget.amount - spent,
            percentUsed = usage.percentUsed,
            status = usage.status,
            reachedThresholds = usage.reachedThresholds,
            plannedByHabits = plannedAmount(relevantHabits, range),
        )
    }
