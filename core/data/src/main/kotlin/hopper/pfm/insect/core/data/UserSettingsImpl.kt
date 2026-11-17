package hopper.pfm.insect.core.data

import hopper.pfm.insect.core.datastore.AppPreferences
import hopper.pfm.insect.core.domain.expense.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsImpl @Inject constructor(private val prefs: AppPreferences) : UserSettings {
    override val weekStart: Flow<DayOfWeek> = prefs.weekStartDay.map { DayOfWeek.of(it) }

    override suspend fun setWeekStart(day: DayOfWeek) = prefs.setWeekStartDay(day.value)
}
