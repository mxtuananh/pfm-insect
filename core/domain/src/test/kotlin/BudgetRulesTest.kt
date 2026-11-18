import hopper.pfm.insect.core.domain.budget.BudgetStatus
import hopper.pfm.insect.core.domain.budget.DateRange
import hopper.pfm.insect.core.domain.budget.computeProgress
import hopper.pfm.insect.core.domain.budget.evaluateUsage
import hopper.pfm.insect.core.domain.budget.isActiveOn
import hopper.pfm.insect.core.domain.budget.plannedAmount
import hopper.pfm.insect.core.domain.budget.rangeContaining
import hopper.pfm.insect.core.model.Budget
import hopper.pfm.insect.core.model.BudgetPeriod
import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetRulesTest {
    private fun d(s: String) = LocalDate.parse(s)

    private fun budget(
        period: BudgetPeriod = BudgetPeriod.MONTH,
        amount: Long = 1_000,
        start: String = "2026-11-10",
        end: String? = null,
        recurring: Boolean = true,
        category: String? = null,
        thresholds: List<Int> = listOf(80, 100),
    ) = Budget("b", category, period, amount, d(start), end?.let(::d), recurring, thresholds)

    private fun expense(date: String, amount: Long, category: String? = null) =
        Expense("e$date$amount", "t", null, amount, d(date), category)

    @Test fun calendarRanges() {
        assertEquals(DateRange(d("2026-02-01"), d("2026-02-28")), BudgetPeriod.MONTH.rangeContaining(d("2026-02-17")))
        assertEquals(DateRange(d("2028-02-01"), d("2028-02-29")), BudgetPeriod.MONTH.rangeContaining(d("2028-02-17")))
        assertEquals(DateRange(d("2026-01-01"), d("2026-12-31")), BudgetPeriod.YEAR.rangeContaining(d("2026-06-30")))
        assertEquals(DateRange(d("2026-11-17"), d("2026-11-17")), BudgetPeriod.DAY.rangeContaining(d("2026-11-17")))
    }

    @Test fun appliesFromThePeriodThatContainsTheStartDate() {
        val b = budget(start = "2026-11-20")
        assertTrue(b.isActiveOn(d("2026-11-01"))) // the whole first month counts, also before the start day
        assertFalse(b.isActiveOn(d("2026-10-31")))
        assertTrue(b.isActiveOn(d("2027-03-05"))) // repeats
    }

    @Test fun nonRepeatingBudgetOnlyCoversItsFirstPeriod() {
        val b = budget(start = "2026-11-20", recurring = false)
        assertTrue(b.isActiveOn(d("2026-11-30")))
        assertFalse(b.isActiveOn(d("2026-12-01")))
    }

    @Test fun repeatingBudgetStopsAfterThePeriodThatBeginsAfterTheEndDate() {
        val b = budget(start = "2026-01-15", end = "2026-03-10")
        assertTrue(b.isActiveOn(d("2026-03-31"))) // March starts before the end date
        assertFalse(b.isActiveOn(d("2026-04-01")))
    }

    @Test fun exactlyTheAmountIsNotExceeded() {
        assertEquals(BudgetStatus.WARNING, evaluateUsage(1_000, 1_000, listOf(80, 100)).status)
        assertEquals(BudgetStatus.EXCEEDED, evaluateUsage(1_001, 1_000, listOf(80, 100)).status)
        assertEquals(BudgetStatus.OK, evaluateUsage(799, 1_000, listOf(80, 100)).status)
        assertEquals(BudgetStatus.WARNING, evaluateUsage(800, 1_000, listOf(80, 100)).status)
    }

    @Test fun percentIsRoundedDownAndReachedThresholdsAreAscending() {
        val usage = evaluateUsage(1_999, 1_000, listOf(150, 80, 100, 300))
        assertEquals(199L, usage.percentUsed)
        assertEquals(listOf(80, 100, 150), usage.reachedThresholds)
        assertEquals(BudgetStatus.EXCEEDED, usage.status)
    }

    @Test fun plannedAmountCountsEveryMatchingWeekdayOfThePeriod() {
        // November 2026: 30 days starting on Sunday -> 5 Sundays and 5 Mondays, 4 of every other day.
        val november = DateRange(d("2026-11-01"), d("2026-11-30"))
        val mondays = Habit("h", "gym", null, 100, null, setOf(1), null, 0)
        val weekends = Habit("h2", "run", null, 10, null, setOf(6, 7), null, 1)
        assertEquals(500L, plannedAmount(listOf(mondays), november))
        assertEquals(10L * (4 + 5), plannedAmount(listOf(weekends), november))
    }

    @Test fun progressCountsTheWholePeriodAndTheCategory() {
        val food = budget(category = "food", amount = 1_000, start = "2026-11-10")
        val all = budget(amount = 5_000, start = "2026-11-10").copy(id = "all")
        val expenses = listOf(
            expense("2026-11-01", 300, "food"), // before the start day, still counted
            expense("2026-11-20", 200, "bus"),
            expense("2026-10-31", 999, "food"), // other month
        )
        val habits = listOf(
            Habit("h", "lunch", null, 10, "food", setOf(2), null, 0), // Tuesdays
            Habit("h2", "bus", null, 7, "bus", setOf(2), null, 1),
        )
        val result = computeProgress(listOf(food, all), expenses, habits, d("2026-11-17")).associateBy { it.budget.id }
        assertEquals(300L, result.getValue("b").spent)
        assertEquals(700L, result.getValue("b").remaining)
        assertEquals(40L, result.getValue("b").plannedByHabits) // 4 Tuesdays x 10, only the food habit
        assertEquals(500L, result.getValue("all").spent)
        assertEquals(4 * 17L, result.getValue("all").plannedByHabits)
        assertEquals(BudgetStatus.OK, result.getValue("all").status)
    }

    @Test fun budgetsNotYetActiveAreLeftOut() {
        assertTrue(computeProgress(listOf(budget(start = "2027-01-01")), emptyList(), emptyList(), d("2026-11-17")).isEmpty())
    }
}
