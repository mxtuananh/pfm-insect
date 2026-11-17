package hopper.pfm.insect.core.network

import hopper.pfm.insect.core.common.AuthTokens
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.plugins.auth.Auth as AuthPlugin

/** What the network layer needs from the session: current tokens, saving new ones, and the refusal signal. */
interface TokenProvider {
    suspend fun tokens(): AuthTokens?
    suspend fun save(tokens: AuthTokens)

    /** The server rejected the refresh token: the session is over and the user must sign in again. */
    suspend fun onRefreshRejected()
}

private val PublicPaths = listOf("/health", "/v1/auth/")

/**
 * Adds "Authorization: Bearer" to API calls and, on a 401, exchanges the refresh token once (Ktor serializes
 * concurrent refreshes) and retries the call. Refresh tokens are single-use, so the new pair is saved immediately.
 * A network failure while refreshing keeps the session; only a rejection by the server ends it.
 */
fun HttpClientConfig<*>.installBearerAuth(provider: TokenProvider) {
    install(AuthPlugin) {
        bearer {
            loadTokens { provider.tokens()?.let { BearerTokens(it.accessToken, it.refreshToken) } }
            refreshTokens {
                val refresh = oldTokens?.refreshToken ?: return@refreshTokens null
                val response = client.post("/v1/auth/refresh") {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(refresh))
                }
                when {
                    response.status.isSuccess() -> {
                        val fresh = response.body<LoginResponse>()
                        provider.save(AuthTokens(fresh.accessToken, fresh.refreshToken))
                        BearerTokens(fresh.accessToken, fresh.refreshToken)
                    }
                    response.status.value == 401 || response.status.value == 400 -> {
                        provider.onRefreshRejected()
                        null
                    }
                    else -> null
                }
            }
            sendWithoutRequest { request ->
                val path = request.url.build().encodedPath
                PublicPaths.none { path.startsWith(it) } && path.startsWith("/v1/")
            }
        }
    }
}
