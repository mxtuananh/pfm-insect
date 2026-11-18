package hopper.pfm.insect.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pfm_prefs")

/**
 * Non-secret settings. Tokens do not belong here; they go to Keystore-backed storage in the auth phase.
 */
@Singleton
class AppPreferences @Inject constructor(private val store: DataStore<Preferences>) {

    /**
     * Stable id of this installation, sent as deviceId at login (letters, digits, . _ : - and at most 64 characters;
     * a UUID satisfies that). Created on first use and never changes until the app is reinstalled.
     */
    suspend fun deviceId(): String {
        store.data.first()[DEVICE_ID]?.let { return it }
        var created = ""
        store.edit { prefs ->
            created = prefs[DEVICE_ID] ?: UUID.randomUUID().toString().also { prefs[DEVICE_ID] = it }
        }
        return created
    }

    /** First day of the week as ISO number, 1 = Monday (default) ... 7 = Sunday. */
    val weekStartDay: Flow<Int> = store.data.map { it[WEEK_START] ?: 1 }

    suspend fun setWeekStartDay(isoDay: Int) {
        require(isoDay in 1..7)
        store.edit { it[WEEK_START] = isoDay }
    }

    /** Server timestamp of the newest alert for which a system notification was already posted (0 = never set). */
    suspend fun lastNotifiedAlertAt(): Long = store.data.first()[LAST_NOTIFIED] ?.toLongOrNull() ?: 0L

    suspend fun setLastNotifiedAlertAt(value: Long) {
        store.edit { it[LAST_NOTIFIED] = value.toString() }
    }

    private companion object {
        val LAST_NOTIFIED = stringPreferencesKey("last_notified_alert_at")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val WEEK_START = intPreferencesKey("week_start_day")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore
}
