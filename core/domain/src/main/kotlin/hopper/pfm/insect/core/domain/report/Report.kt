package hopper.pfm.insect.core.domain.report

import hopper.pfm.insect.core.model.Expense
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Longest custom range, in days (about five years). */
const val MAX_CUSTOM_DAYS = 5 * 366

/** Number of categories listed on their own; the rest is merged into one "other" row. */
const val TOP_CATEGORIES = 4

/** A span of whole days, both ends included. */
data class ReportRange(val start: LocalDate, val end: LocalDate) {
    val days: Int get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()

    operator fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(end)

    companion object {
        fun month(month: YearMonth) = ReportRange(month.atDay(1), month.atEndOfMonth())

        fun year(year: Int) = ReportRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
    }
}

enum class TrendUnit { DAY, WEEK, MONTH }

/** One bar of the trend chart. */
data class TrendBucket(val start: LocalDate, val end: LocalDate, val amount: Long)

/** One row of "Theo danh mục". [mergedCategories] > 0 marks the merged "other" row (not a real category). */
data class CategoryShare(
    val categoryId: String?,
    val amount: Long,
    val count: Int,
    /** Share of the total in whole percent, rounded down. */
    val percent: Int,
    val mergedCategories: Int = 0,
)

data class Report(
    val range: ReportRange,
    val previous: ReportRange,
    val total: Long,
    val previousTotal: Long,
    val count: Int,
    /** Total divided by the days that have passed (the whole range for a finished one). */
    val averagePerDay: Long,
    /** Total divided by the months of the range (used by the yearly view). */
    val averagePerMonth: Long,
    /** The biggest bucket of the trend, or null when nothing was spent. */
    val peak: TrendBucket?,
    val trendUnit: TrendUnit,
    val trend: List<TrendBucket>,
    val categories: List<CategoryShare>,
) {
    /** Change against the previous range in percent, or null when there is nothing to compare with. */
    val deltaPercent: Int?
        get() = if (previousTotal <= 0) null else Math.round((total - previousTotal) * 100.0 / previousTotal).toInt()
}

/** The range right before this one, of the same kind: previous month, previous year, or the same number of days. */
fun previousRange(range: ReportRange, kind: ReportKind): ReportRange = when (kind) {
    ReportKind.MONTH -> ReportRange.month(YearMonth.from(range.start).minusMonths(1))
    ReportKind.YEAR -> ReportRange.year(range.start.year - 1)
    ReportKind.CUSTOM -> ReportRange(range.start.minusDays(range.days.toLong()), range.start.minusDays(1))
}

enum class ReportKind { MONTH, YEAR, CUSTOM }

/** How a range is split into bars: days up to a month, weeks up to about six months, months beyond that. */
fun trendUnitFor(kind: ReportKind, range: ReportRange): TrendUnit = when (kind) {
    ReportKind.MONTH -> TrendUnit.DAY
    ReportKind.YEAR -> TrendUnit.MONTH
    ReportKind.CUSTOM -> when {
        range.days <= 31 -> TrendUnit.DAY
        range.days <= 190 -> TrendUnit.WEEK
        else -> TrendUnit.MONTH
    }
}

private fun buckets(range: ReportRange, unit: TrendUnit): List<ReportRange> {
    val result = mutableListOf<ReportRange>()
    var start = range.start
    while (!start.isAfter(range.end)) {
        val naturalEnd = when (unit) {
            TrendUnit.DAY -> start
            TrendUnit.WEEK -> start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            TrendUnit.MONTH -> start.with(TemporalAdjusters.lastDayOfMonth())
        }
        val end = if (naturalEnd.isAfter(range.end)) range.end else naturalEnd
        result += ReportRange(start, end)
        start = end.plusDays(1)
    }
    return result
}

/**
 * Builds the report for a range from the expenses (which must contain the range and the previous range). Everything is
 * computed locally, so it includes expenses that have not been synchronized yet and works offline.
 */
fun buildReport(kind: ReportKind, range: ReportRange, expenses: List<Expense>, today: LocalDate): Report {
    val previous = previousRange(range, kind)
    val inRange = expenses.filter { it.spentOn in range }
    val total = inRange.sumOf { it.amount }
    val previousTotal = expenses.filter { it.spentOn in previous }.sumOf { it.amount }

    val unit = trendUnitFor(kind, range)
    val trend = buckets(range, unit).map { bucket ->
        TrendBucket(bucket.start, bucket.end, inRange.filter { it.spentOn in bucket }.sumOf { it.amount })
    }
    val peak = trend.filter { it.amount > 0 }.maxByOrNull { it.amount }

    // A range that has not ended yet is averaged over the days that have passed, so early in a month the figure is not tiny.
    val elapsedDays = when {
        today.isBefore(range.start) -> 1
        today.isAfter(range.end) -> range.days
        else -> (ChronoUnit.DAYS.between(range.start, today) + 1).toInt()
    }
    val months = maxOf(1, ChronoUnit.MONTHS.between(YearMonth.from(range.start), YearMonth.from(range.end)).toInt() + 1)
    val elapsedMonths = when {
        today.isBefore(range.start) -> 1
        today.isAfter(range.end) -> months
        else -> maxOf(1, ChronoUnit.MONTHS.between(YearMonth.from(range.start), YearMonth.from(today)).toInt() + 1)
    }

    return Report(
        range = range,
        previous = previous,
        total = total,
        previousTotal = previousTotal,
        count = inRange.size,
        averagePerDay = total / elapsedDays,
        averagePerMonth = total / elapsedMonths,
        peak = peak,
        trendUnit = unit,
        trend = trend,
        categories = categoryShares(inRange, total),
    )
}

/** Categories by amount, biggest first; everything after the top ones is merged into one row. */
fun categoryShares(expenses: List<Expense>, total: Long): List<CategoryShare> {
    if (total <= 0) return emptyList()
    val all = expenses.groupBy { it.categoryId }
        .map { (id, items) -> CategoryShare(id, items.sumOf { it.amount }, items.size, (items.sumOf { it.amount } * 100 / total).toInt()) }
        .sortedWith(compareByDescending<CategoryShare> { it.amount }.thenBy { it.categoryId ?: "" })
    if (all.size <= TOP_CATEGORIES + 1) return all
    val top = all.take(TOP_CATEGORIES)
    val rest = all.drop(TOP_CATEGORIES)
    val restAmount = rest.sumOf { it.amount }
    return top + CategoryShare(null, restAmount, rest.sumOf { it.count }, (restAmount * 100 / total).toInt(), mergedCategories = rest.size)
}
