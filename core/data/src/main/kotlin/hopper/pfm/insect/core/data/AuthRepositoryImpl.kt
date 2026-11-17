package hopper.pfm.insect.core.data

import hopper.pfm.insect.core.common.AuthTokens
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.common.map
import hopper.pfm.insect.core.database.PfmDatabase
import hopper.pfm.insect.core.datastore.AppPreferences
import hopper.pfm.insect.core.datastore.ProfileCache
import hopper.pfm.insect.core.domain.AuthRepository
import hopper.pfm.insect.core.model.SessionState
import hopper.pfm.insect.core.model.UserProfile
import hopper.pfm.insect.core.network.AuthApi
import hopper.pfm.insect.core.network.UpdateProfileRequest
import hopper.pfm.insect.core.network.UserProfileDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

internal fun UserProfileDto.toModel() =
    UserProfile(id = id, email = email, name = name, timezone = timezone, currency = currency)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
    private val prefs: AppPreferences,
    private val profileCache: ProfileCache,
    private val database: PfmDatabase,
) : AuthRepository {

    override val session: StateFlow<SessionState> = sessionManager.state


    override val profile: Flow<UserProfile?> = profileCache.profile

    override suspend fun restoreSession(): SessionState = sessionManager.restore()

    override suspend fun register(email: String, password: String): Outcome<Unit> =
        api.register(email.trim(), password)

    override suspend fun login(email: String, password: String): Outcome<Unit> {
        val login = api.login(email.trim(), password, prefs.deviceId())
        if (login is Outcome.Failure) return login
        val tokens = (login as Outcome.Success).value
        sessionManager.signIn(AuthTokens(tokens.accessToken, tokens.refreshToken))

        // Local data belongs to one account: when another account signs in, the old data must not be mixed in.
        val previousId = profileCache.profile.first()?.id
        val fetched = refreshProfile()
        if (fetched is Outcome.Success && previousId != null && previousId != fetched.value.id) {
            database.clearAllTables()
        }
        // A failed profile fetch must not fail the login: the profile is only a display cache.
        return Outcome.Success(Unit)
    }

    override suspend fun refreshProfile(): Outcome<UserProfile> =
        api.me().map { it.toModel() }.also { if (it is Outcome.Success) profileCache.save(it.value) }

    override suspend fun updateProfile(name: String?, timezone: String?, currency: String?): Outcome<UserProfile> =
        api.updateMe(UpdateProfileRequest(name?.trim(), timezone, currency)).map { it.toModel() }
            .also { if (it is Outcome.Success) profileCache.save(it.value) }

    override suspend fun logout(allDevices: Boolean) {
        val refreshToken = sessionManager.currentTokens()?.refreshToken
        // Best effort: the local session ends even when the server cannot be reached.
        if (allDevices) api.logoutAll() else refreshToken?.let { api.logout(it) }
        sessionManager.signOut()
    }

    override suspend fun discardLocalData() {
        sessionManager.signOut()
        profileCache.clear()
        database.clearAllTables()
    }
}
