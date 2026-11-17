import hopper.pfm.insect.core.common.AppError
import hopper.pfm.insect.core.common.AuthTokens
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.network.ApiConfig
import hopper.pfm.insect.core.network.AuthApi
import hopper.pfm.insect.core.network.TokenProvider
import hopper.pfm.insect.core.network.createApiClient
import hopper.pfm.insect.core.network.installBearerAuth
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BearerAuthTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val profile = """{"id":"u1","email":"a@b.co","name":"A","timezone":"Asia/Ho_Chi_Minh","currency":"VND","version":1,"updatedAt":1}"""

    private class FakeTokens(var current: AuthTokens?) : TokenProvider {
        var rejected = false
        override suspend fun tokens() = current
        override suspend fun save(tokens: AuthTokens) { current = tokens }
        override suspend fun onRefreshRejected() { rejected = true; current = null }
    }

    private fun api(tokens: FakeTokens, engine: MockEngine) =
        AuthApi(createApiClient(engine, ApiConfig("http://localhost:8080")) { installBearerAuth(tokens) })

    @Test fun expiredAccessTokenIsRefreshedOnceAndTheCallRetried() = runTest {
        val tokens = FakeTokens(AuthTokens("old-access", "old-refresh"))
        val seen = mutableListOf<String>()
        val engine = MockEngine { request ->
            seen += request.url.encodedPath + " " + request.headers[HttpHeaders.Authorization]
            when (request.url.encodedPath) {
                "/v1/auth/refresh" -> {
                    assertTrue(request.body.toByteArray().decodeToString().contains("old-refresh"))
                    respond("""{"accessToken":"new-access","refreshToken":"new-refresh"}""", HttpStatusCode.OK, json)
                }
                else ->
                    if (request.headers[HttpHeaders.Authorization] == "Bearer new-access") {
                        respond(profile, HttpStatusCode.OK, json)
                    } else {
                        respond("""{"error":"expired","code":"unauthorized"}""", HttpStatusCode.Unauthorized, json)
                    }
            }
        }
        val result = api(tokens, engine).me()
        assertTrue(result is Outcome.Success)
        assertEquals(AuthTokens("new-access", "new-refresh"), tokens.current)
        assertEquals(1, seen.count { it.startsWith("/v1/auth/refresh") })
    }

    @Test fun rejectedRefreshEndsTheSession() = runTest {
        val tokens = FakeTokens(AuthTokens("old-access", "old-refresh"))
        val engine = MockEngine { request ->
            respond("""{"error":"no","code":"invalid_refresh_token"}""", HttpStatusCode.Unauthorized, json)
        }
        val result = api(tokens, engine).me()
        assertEquals(AppError.Unauthorized, (result as Outcome.Failure).error)
        assertTrue(tokens.rejected)
    }

    @Test fun wrongPasswordIsNotASessionProblem() = runTest {
        val tokens = FakeTokens(null)
        val engine = MockEngine {
            respond("""{"error":"bad","code":"invalid_credentials"}""", HttpStatusCode.Unauthorized, json)
        }
        val result = api(tokens, engine).login("a@b.co", "x", "dev1")
        assertEquals(AppError.Server(401, "invalid_credentials", "bad"), (result as Outcome.Failure).error)
        assertEquals(false, tokens.rejected)
    }
}
