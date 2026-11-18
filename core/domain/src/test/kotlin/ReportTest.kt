import hopper.pfm.insect.core.domain.report.ReportKind
import hopper.pfm.insect.core.domain.report.ReportRange
import hopper.pfm.insect.core.domain.report.TrendUnit
import hopper.pfm.insect.core.domain.report.buildReport
import hopper.pfm.insect.core.domain.report.previousRange
import hopper.pfm.insect.core.domain.report.trendUnitFor
import hopper.pfm.insect.core.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportTest {
    private fun d(s: String) = LocalDate.parse(s)

    private fun e(date: String, amount: Long, category: String? = "food") =
        Expense("e$date$amount$category", "t", null, amount, d(date), category)

    private val november = ReportRange.month(YearMonth.of(2026, 11))

    @Test fun previousRangeOfEachKind() {
        assertEquals(ReportRange.month(YearMonth.of(2026, 10)), previousRange(november, ReportKind.MONTH))
        assertEquals(ReportRange.month(YearMonth.of(2025, 12)), previousRange(ReportRange.month(YearMonth.of(2026, 1)), ReportKind.MONTH))
        assertEquals(ReportRange.year(2025), previousRange(ReportRange.year(2026), ReportKind.YEAR))
        val custom = ReportRange(d("2026-11-10"), d("2026-11-19")) // 10 days
        assertEquals(ReportRange(d("2026-10-31"), d("2026-11-09")), previousRange(custom, ReportKind.CUSTOM))
    }

    @Test fun trendUnitDependsOnTheLengthOfACustomRange() {
        assertEquals(TrendUnit.DAY, trendUnitFor(ReportKind.CUSTOM, ReportRange(d("2026-11-01"), d("2026-12-01")))) // 31 days
        assertEquals(TrendUnit.WEEK, trendUnitFor(ReportKind.CUSTOM, ReportRange(d("2026-11-01"), d("2026-12-02")))) // 32 days
        assertEquals(TrendUnit.MONTH, trendUnitFor(ReportKind.CUSTOM, ReportRange(d("2026-01-01"), d("2026-12-31"))))
        assertEquals(TrendUnit.DAY, trendUnitFor(ReportKind.MONTH, november))
        assertEquals(TrendUnit.MONTH, trendUnitFor(ReportKind.YEAR, ReportRange.year(2026)))
    }

    @Test fun monthReportTotalsDeltaAndDailyBars() {
        val expenses = listOf(e("2026-11-01", 100), e("2026-11-01", 50), e("2026-11-17", 300), e("2026-10-20", 250), e("2026-09-01", 999))
        val report = buildReport(ReportKind.MONTH, november, expenses, today = d("2026-11-17"))
        assertEquals(450L, report.total)
        assertEquals(250L, report.previousTotal)
        assertEquals(80, report.deltaPercent)
        assertEquals(3, report.count)
        assertEquals(30, report.trend.size)
        assertEquals(150L, report.trend.first().amount)
        assertEquals(d("2026-11-17"), report.peak!!.start)
        // 17 days have passed: 450 / 17 = 26
        assertEquals(26L, report.averagePerDay)
    }

    @Test fun aFinishedRangeIsAveragedOverAllItsDays() {
        val report = buildReport(ReportKind.MONTH, november, listOf(e("2026-11-05", 3_000)), today = d("2027-02-01"))
        assertEquals(100L, report.averagePerDay)
    }

    @Test fun weeklyBucketsAreClippedToTheRange() {
        // Wed 2026-11-04 .. Sun 2026-12-20 is 47 days -> weeks (Monday to Sunday), the first one is partial.
        val range = ReportRange(d("2026-11-04"), d("2026-12-20"))
        val report = buildReport(ReportKind.CUSTOM, range, listOf(e("2026-11-04", 10), e("2026-11-09", 20)), today = d("2027-01-01"))
        assertEquals(TrendUnit.WEEK, report.trendUnit)
        assertEquals(d("2026-11-04"), report.trend.first().start)
        assertEquals(d("2026-11-08"), report.trend.first().end)
        assertEquals(10L, report.trend[0].amount)
        assertEquals(20L, report.trend[1].amount)
        assertEquals(range.end, report.trend.last().end)
        assertEquals(range.days, report.trend.sumOf { (it.end.toEpochDay() - it.start.toEpochDay() + 1).toInt() })
    }

    @Test fun yearReportHasTwelveMonthsAndAveragesOverElapsedMonths() {
        val expenses = listOf(e("2026-01-15", 1_200), e("2026-03-02", 600), e("2025-12-31", 900))
        val report = buildReport(ReportKind.YEAR, ReportRange.year(2026), expenses, today = d("2026-03-20"))
        assertEquals(12, report.trend.size)
        assertEquals(1_800L, report.total)
        assertEquals(900L, report.previousTotal)
        assertEquals(1_200L, report.trend[0].amount)
        assertEquals(600L, report.averagePerMonth) // 3 months have started
    }

    @Test fun categoriesAreSortedAndTheTailIsMerged() {
        val expenses = listOf(
            e("2026-11-01", 500, "a"), e("2026-11-01", 400, "b"), e("2026-11-01", 300, "c"), e("2026-11-01", 200, "d"),
            e("2026-11-01", 60, "e"), e("2026-11-01", 40, "f"),
        )
        val shares = buildReport(ReportKind.MONTH, november, expenses, today = d("2026-11-30")).categories
        assertEquals(listOf("a", "b", "c", "d", null), shares.map { it.categoryId })
        val other = shares.last()
        assertEquals(2, other.mergedCategories)
        assertEquals(100L, other.amount)
        assertEquals(6, other.percent) // 100 of 1500 = 6.67 percent, rounded down
    }

    @Test fun uncategorizedExpensesFormTheirOwnRow() {
        val shares = buildReport(ReportKind.MONTH, november, listOf(e("2026-11-01", 100, null), e("2026-11-02", 300, "a")), d("2026-11-30")).categories
        assertEquals(listOf("a", null), shares.map { it.categoryId })
        assertEquals(0, shares.last().mergedCategories)
        assertEquals(25, shares.last().percent)
    }

    @Test fun emptyRangeHasNoPeakNoCategoriesAndNoDelta() {
        val report = buildReport(ReportKind.MONTH, november, emptyList(), d("2026-11-17"))
        assertEquals(0L, report.total)
        assertNull(report.peak)
        assertNull(report.deltaPercent)
        assertTrue(report.categories.isEmpty())
        assertEquals(30, report.trend.size)
    }
}
