package hopper.pfm.insect.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sync columns shared by every synchronized table (see the offline-first contract of the server):
 * - version: server version of the last copy received (0 = never synced)
 * - deleted: tombstone, hidden from the UI; removed after the server confirms the delete
 * - dirty: changed locally and not yet pushed
 * - localRev: increased on every local edit, so a push that finishes after a newer edit does not clear dirty
 * - failure: error code of the last rejected push, null when none
 */
@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String?,
    val color: String?,
    val archived: Boolean,
    val version: Long = 0,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
    val localRev: Long = 0,
    val failure: String? = null,
)

@Entity(
    tableName = "expense",
    indices = [Index("spentOn"), Index("categoryId")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String?,
    /** Smallest currency unit, never a decimal. */
    val amount: Long,
    /** yyyy-MM-dd in the calendar of the user. */
    val spentOn: String,
    val categoryId: String?,
    val version: Long = 0,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
    val localRev: Long = 0,
    val failure: String? = null,
)

@Entity(tableName = "habit")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String?,
    val amount: Long,
    val categoryId: String?,
    /** Comma separated ISO days, for example "1,3,5". */
    val daysOfWeek: String,
    val timeOfDay: String?,
    val sortOrder: Int,
    val version: Long = 0,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
    val localRev: Long = 0,
    val failure: String? = null,
)

@Entity(tableName = "budget", indices = [Index("categoryId")])
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String?,
    /** DAY, MONTH or YEAR. */
    val period: String,
    val amount: Long,
    val startDate: String,
    val endDate: String?,
    val recurring: Boolean,
    /** Comma separated percentages, for example "80,100". */
    val alertThresholds: String,
    val version: Long = 0,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
    val localRev: Long = 0,
    val failure: String? = null,
)

/** Cursor of the /changes feed per resource ("categories", "expenses", ...). */
@Entity(tableName = "sync_cursor")
data class SyncCursorEntity(
    @PrimaryKey val resource: String,
    val cursor: String?,
)
