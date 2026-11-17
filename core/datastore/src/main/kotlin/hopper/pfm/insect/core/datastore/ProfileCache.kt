package hopper.pfm.insect.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hopper.pfm.insect.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Copy of the profile for offline display (the server stays the source of truth). */
@Singleton
class ProfileCache @Inject constructor(private val store: DataStore<Preferences>) {

    val profile: Flow<UserProfile?> = store.data.map { prefs ->
        val id = prefs[ID] ?: return@map null
        UserProfile(
            id = id,
            email = prefs[EMAIL].orEmpty(),
            name = prefs[NAME].orEmpty(),
            timezone = prefs[TIMEZONE] ?: "Asia/Ho_Chi_Minh",
            currency = prefs[CURRENCY] ?: "VND",
        )
    }

    suspend fun save(profile: UserProfile) {
        store.edit {
            it[ID] = profile.id
            it[EMAIL] = profile.email
            it[NAME] = profile.name
            it[TIMEZONE] = profile.timezone
            it[CURRENCY] = profile.currency
        }
    }

    suspend fun clear() {
        store.edit {
            it.remove(ID)
            it.remove(EMAIL)
            it.remove(NAME)
            it.remove(TIMEZONE)
            it.remove(CURRENCY)
        }
    }

    private companion object {
        val ID = stringPreferencesKey("profile_id")
        val EMAIL = stringPreferencesKey("profile_email")
        val NAME = stringPreferencesKey("profile_name")
        val TIMEZONE = stringPreferencesKey("profile_timezone")
        val CURRENCY = stringPreferencesKey("profile_currency")
    }
}
