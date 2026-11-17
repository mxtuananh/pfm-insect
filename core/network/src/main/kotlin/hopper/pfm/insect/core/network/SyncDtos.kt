package hopper.pfm.insect.core.network

import kotlinx.serialization.Serializable

// ---- Responses (unknown fields are ignored) ----

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val archived: Boolean,
    val version: Long,
    val updatedAt: Long,
)

@Serializable
data class ExpenseDto(
    val id: String,
    val title: String,
    val note: String? = null,
    val amount: Long,
    val spentOn: String,
    val categoryId: String? = null,
    val version: Long,
    val updatedAt: Long,
    val deleted: Boolean,
)

@Serializable
data class HabitDto(
    val id: String,
    val title: String,
    val note: String? = null,
    val amount: Long,
    val categoryId: String? = null,
    val daysOfWeek: List<Int>,
    val timeOfDay: String? = null,
    val sortOrder: Int,
    val version: Long,
    val updatedAt: Long,
    val deleted: Boolean,
)

@Serializable
data class BudgetDto(
    val id: String,
    val categoryId: String? = null,
    val period: String,
    val amount: Long,
    val startDate: String,
    val endDate: String? = null,
    val recurring: Boolean,
    val alertThresholds: List<Int>,
    val version: Long,
    val updatedAt: Long,
    val deleted: Boolean,
)

/** Items of /categories, /habits and /budgets: the whole list plus the cursor to continue from. */
@Serializable
data class ListResponse<T>(val items: List<T>, val syncCursor: String)

/** One page of /expenses (newest first): nextCursor goes to "before" for older pages, null at the end. */
@Serializable
data class ExpensePage(val items: List<ExpenseDto>, val nextCursor: String? = null, val syncCursor: String)

@Serializable
data class ChangesResponse<T>(val items: List<T>, val nextCursor: String, val hasMore: Boolean)

// ---- Requests: all fields are sent on every PUT, the server replaces the whole record ----

@Serializable
data class UpsertCategoryRequest(val name: String, val icon: String?, val color: String?, val archived: Boolean)

@Serializable
data class UpsertExpenseRequest(
    val title: String,
    val note: String?,
    val amount: Long,
    val spentOn: String,
    val categoryId: String?,
)

@Serializable
data class UpsertHabitRequest(
    val title: String,
    val note: String?,
    val amount: Long,
    val categoryId: String?,
    val daysOfWeek: List<Int>,
    val timeOfDay: String?,
    val sortOrder: Int,
)

@Serializable
data class UpsertBudgetRequest(
    val categoryId: String?,
    val period: String,
    val amount: Long,
    val startDate: String,
    val endDate: String?,
    val recurring: Boolean,
    val alertThresholds: List<Int>,
)
