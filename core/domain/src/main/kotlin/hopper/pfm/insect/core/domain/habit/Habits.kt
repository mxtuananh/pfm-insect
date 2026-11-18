package hopper.pfm.insect.core.domain.habit

import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.model.Habit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

const val MAX_HABITS = 200

interface HabitRepository {
    /** Habits that are not deleted, in the order of the user (sortOrder). */
    fun observeAll(): Flow<List<Habit>>

    suspend fun get(id: String): Habit?

    /** Creates or replaces a habit. A new habit (sortOrder < 0) goes to the end of the list. */
    suspend fun save(habit: Habit)

    suspend fun delete(id: String)

    /** Swaps the habit with its neighbour above (-1) or below (+1). */
    suspend fun move(id: String, direction: Int)
}

/** True when the habit is planned for the given day (ISO: 1 = Monday ... 7 = Sunday). */
fun Habit.isDueOn(date: LocalDate): Boolean = date.dayOfWeek.value in daysOfWeek

/** Habits planned for today come first; inside each group the order of the user is kept. */
fun List<Habit>.sortedForToday(today: LocalDate): List<Habit> =
    sortedWith(compareBy<Habit>({ if (it.isDueOn(today)) 0 else 1 }, { it.sortOrder }, { it.title.lowercase() }))

/** The expense a habit turns into when the user taps it: its title, note, amount and category, dated [date]. */
fun Habit.toExpense(date: LocalDate, id: String = UUID.randomUUID().toString()): Expense =
    Expense(id = id, title = title, note = note, amount = amount, spentOn = date, categoryId = categoryId)
