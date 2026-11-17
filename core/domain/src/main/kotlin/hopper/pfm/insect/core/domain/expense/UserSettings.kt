package hopper.pfm.insect.core.domain.expense

import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

/** Preferences of the user that change how data is shown (kept on the device, not on the server). */
interface UserSettings {
    /** First day of the week for the weekly view; Monday by default. */
    val weekStart: Flow<DayOfWeek>

    suspend fun setWeekStart(day: DayOfWeek)
}
