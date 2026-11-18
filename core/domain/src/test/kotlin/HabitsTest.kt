import hopper.pfm.insect.core.domain.habit.isDueOn
import hopper.pfm.insect.core.domain.habit.sortedForToday
import hopper.pfm.insect.core.domain.habit.toExpense
import hopper.pfm.insect.core.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitsTest {
    private fun habit(id: String, days: Set<Int>, order: Int) =
        Habit(id, "t$id", "note", 30_000, "cat", days, "07:30", order)

    // 2026-11-17 is a Tuesday (ISO day 2).
    private val tuesday = LocalDate.parse("2026-11-17")

    @Test fun dueDayUsesIsoNumbers() {
        assertTrue(habit("a", setOf(2), 0).isDueOn(tuesday))
        assertFalse(habit("a", setOf(1, 3), 0).isDueOn(tuesday))
        assertTrue(habit("a", (1..7).toSet(), 0).isDueOn(LocalDate.parse("2026-11-22"))) // Sunday = 7
    }

    @Test fun todaysHabitsComeFirstKeepingTheUserOrder() {
        val habits = listOf(habit("gym", setOf(1, 3), 0), habit("coffee", setOf(2), 2), habit("bus", setOf(2, 4), 1))
        assertEquals(listOf("bus", "coffee", "gym"), habits.sortedForToday(tuesday).map { it.id })
    }

    @Test fun quickAddCopiesTheFieldsAndUsesTheGivenDate() {
        val expense = habit("a", setOf(2), 0).toExpense(tuesday, id = "new-id")
        assertEquals("new-id", expense.id)
        assertEquals("ta", expense.title)
        assertEquals("note", expense.note)
        assertEquals(30_000L, expense.amount)
        assertEquals("cat", expense.categoryId)
        assertEquals(tuesday, expense.spentOn)
    }
}
