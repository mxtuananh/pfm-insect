package hopper.pfm.insect.core.domain.budget

import hopper.pfm.insect.core.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    /** Budgets that are not deleted, newest start date first. */
    fun observeAll(): Flow<List<Budget>>

    suspend fun get(id: String): Budget?

    /** Creates or replaces a budget (written locally first, then synchronized). */
    suspend fun save(budget: Budget)

    suspend fun delete(id: String)
}
