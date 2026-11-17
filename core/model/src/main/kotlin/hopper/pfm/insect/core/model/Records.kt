package hopper.pfm.insect.core.model

import java.time.LocalDate

/** Money is a whole number in the smallest unit of the currency of the user (dong, cents). Never a decimal. */
typealias MinorUnits = Long

data class Category(
    val id: String,
    val name: String,
    val icon: String?,
    val color: String?,
    val archived: Boolean,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

data class Expense(
    val id: String,
    val title: String,
    val note: String?,
    val amount: MinorUnits,
    val spentOn: LocalDate,
    val categoryId: String?,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

data class Habit(
    val id: String,
    val title: String,
    val note: String?,
    val amount: MinorUnits,
    val categoryId: String?,
    /** ISO days of the week, 1 = Monday ... 7 = Sunday. */
    val daysOfWeek: Set<Int>,
    /** "HH:mm" or null. */
    val timeOfDay: String?,
    val sortOrder: Int,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

enum class BudgetPeriod { DAY, MONTH, YEAR }

data class Budget(
    val id: String,
    val categoryId: String?,
    val period: BudgetPeriod,
    val amount: MinorUnits,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val recurring: Boolean,
    /** Percent thresholds that raise an alert, for example 80 and 100. */
    val alertThresholds: List<Int>,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
