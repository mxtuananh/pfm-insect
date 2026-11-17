package hopper.pfm.insect.core.common

/** Access token (15 minutes) and the single-use refresh token (30 days). Always keep the newest refresh token. */
data class AuthTokens(val accessToken: String, val refreshToken: String)

/** Secure storage of the tokens. The implementation encrypts them with a key that never leaves the Android Keystore. */
interface TokenStore {
    suspend fun get(): AuthTokens?
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}
