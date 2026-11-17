package hopper.pfm.insect.core.domain.expense

import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.Expense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Writes go to the local database first (the record becomes dirty) and the sync service is asked to run; reads come
 * from the database only, so the screens work offline and update at once.
 */
interface ExpenseRepository {
    /** Expenses of the days between from and to, both included; deleted ones are left out. */
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Expense>>

    suspend fun get(id: String): Expense?

    /** Creates the expense or replaces its fields. A record rejected earlier is sent again. */
    suspend fun save(expense: Expense)

    /** Marks the expense deleted (it disappears from the UI) and syncs the deletion. Undo = save the expense again. */
    suspend fun delete(id: String)
}

interface CategoryRepository {
    /** All categories, archived ones included. */
    fun observeAll(): Flow<List<Category>>

    suspend fun get(id: String): Category?

    suspend fun save(category: Category)
}
