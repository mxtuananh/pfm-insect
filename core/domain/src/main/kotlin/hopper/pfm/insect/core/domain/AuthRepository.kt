package hopper.pfm.insect.core.domain

import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.model.SessionState
import hopper.pfm.insect.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    /** Current session. Call restoreSession() once at start-up to load it from secure storage. */
    val session: StateFlow<SessionState>

    /** Profile cached on the device, so the app can show it offline. */
    val profile: Flow<UserProfile?>

    suspend fun restoreSession(): SessionState

    suspend fun register(email: String, password: String): Outcome<Unit>

    suspend fun login(email: String, password: String): Outcome<Unit>

    /** Fetches the profile from the server and updates the local copy. */
    suspend fun refreshProfile(): Outcome<UserProfile>

    suspend fun updateProfile(name: String?, timezone: String?, currency: String?): Outcome<UserProfile>

    /**
     * Signs out. The local session always ends, even when the server cannot be reached (the refresh token is then
     * left to expire on the server). Local data is kept.
     */
    suspend fun logout(allDevices: Boolean)

    /** "Use another account": signs out and deletes all local data, including changes not yet synchronized. */
    suspend fun discardLocalData()
}
