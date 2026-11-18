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

enum class AlertKind { THRESHOLD, EXCEEDED }

/**
 * A budget alert created by the server. It is structured on purpose: the app writes the sentence in the language of the
 * user from these fields.
 */
data class AppNotification(
    val id: String,
    val kind: AlertKind,
    /** The percent threshold that was reached (for THRESHOLD alerts). */
    val threshold: Int?,
    val budgetId: String,
    val categoryId: String?,
    val period: BudgetPeriod,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val amount: MinorUnits,
    val spent: MinorUnits,
    val percentUsed: Long,
    /** Epoch milliseconds, server clock. */
    val createdAt: Long,
    val read: Boolean,
)
