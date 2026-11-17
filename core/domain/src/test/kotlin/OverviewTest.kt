import hopper.pfm.insect.core.domain.expense.Period
import hopper.pfm.insect.core.domain.expense.PeriodType
import hopper.pfm.insect.core.domain.expense.buildOverview
import hopper.pfm.insect.core.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class OverviewTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun e(id: String, date: String, amount: Long, cat: String? = "food") =
        Expense(id, "t$id", null, amount, d(date), cat)

    @Test fun weekStartsOnTheChosenDay() {
        val sunday = d("2026-11-15")
        val monday = Period.of(PeriodType.WEEK, sunday, DayOfWeek.MONDAY)
        assertEquals(d("2026-11-09"), monday.start)
        assertEquals(d("2026-11-15"), monday.end)
        val sundayStart = Period.of(PeriodType.WEEK, sunday, DayOfWeek.SUNDAY)
        assertEquals(d("2026-11-15"), sundayStart.start)
        assertEquals(d("2026-11-21"), sundayStart.end)
    }

    @Test fun monthPeriodHasTheRightLength() {
        assertEquals(30, Period.of(PeriodType.MONTH, d("2026-11-17"), DayOfWeek.MONDAY).days)
        assertEquals(29, Period.of(PeriodType.MONTH, d("2028-02-10"), DayOfWeek.MONDAY).days)
    }

    @Test fun shiftingMovesByWholePeriods() {
        val month = Period.of(PeriodType.MONTH, d("2026-01-31"), DayOfWeek.MONDAY)
        assertEquals(d("2025-12-01"), month.shifted(-1, DayOfWeek.MONDAY).start)
        assertEquals(d("2026-02-28"), month.shifted(1, DayOfWeek.MONDAY).end)
        val week = Period.of(PeriodType.WEEK, d("2026-11-17"), DayOfWeek.MONDAY)
        assertEquals(d("2026-11-09"), week.shifted(-1, DayOfWeek.MONDAY).start)
    }

    @Test fun totalsDeltaAndBars() {
        val period = Period.of(PeriodType.MONTH, d("2026-11-17"), DayOfWeek.MONDAY)
        val previous = period.shifted(-1, DayOfWeek.MONDAY)
        val items = listOf(
            e("1", "2026-11-01", 100), e("2", "2026-11-01", 50), e("3", "2026-11-17", 200),
            e("4", "2026-10-20", 250), e("5", "2026-09-30", 999), // outside both periods
        )
        val o = buildOverview(period, previous, items)
        assertEquals(350, o.total)
        assertEquals(250, o.previousTotal)
        assertEquals(40, o.deltaPercent)
        assertEquals(30, o.dayTotals.size)
        assertEquals(150, o.dayTotals.first().amount)
        assertEquals(0, o.dayTotals[1].amount)
        assertEquals(200, o.dayTotals[16].amount)
    }

    @Test fun noDeltaWithoutAPreviousTotal() {
        val period = Period.of(PeriodType.WEEK, d("2026-11-17"), DayOfWeek.MONDAY)
        val o = buildOverview(period, period.shifted(-1, DayOfWeek.MONDAY), listOf(e("1", "2026-11-17", 10)))
        assertNull(o.deltaPercent)
    }

    @Test fun categoryFilterNarrowsTotalsAndListButNotTheChips() {
        val period = Period.of(PeriodType.WEEK, d("2026-11-17"), DayOfWeek.MONDAY)
        val items = listOf(e("1", "2026-11-17", 100, "food"), e("2", "2026-11-16", 300, "bus"), e("3", "2026-11-17", 50, "bus"))
        val o = buildOverview(period, period.shifted(-1, DayOfWeek.MONDAY), items, categoryFilter = "bus")
        assertEquals(350, o.total)
        assertEquals(2, o.groups.sumOf { it.items.size })
        assertEquals(listOf("bus", "food"), o.categoryTotals.map { it.categoryId })
        assertEquals(350, o.categoryTotals.first().amount)
    }

    @Test fun groupsAreNewestDayFirstWithDailyTotals() {
        val period = Period.of(PeriodType.WEEK, d("2026-11-17"), DayOfWeek.MONDAY)
        val items = listOf(e("1", "2026-11-17", 5), e("2", "2026-11-19", 7), e("3", "2026-11-19", 3))
        val o = buildOverview(period, period.shifted(-1, DayOfWeek.MONDAY), items)
        assertEquals(listOf(d("2026-11-19"), d("2026-11-17")), o.groups.map { it.date })
        assertEquals(10, o.groups.first().total)
    }
}
