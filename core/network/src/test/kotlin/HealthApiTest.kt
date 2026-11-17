import hopper.pfm.insect.core.common.AppError
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.network.ApiConfig
import hopper.pfm.insect.core.network.HealthApi
import hopper.pfm.insect.core.network.createApiClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HealthApiTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(handler: io.ktor.client.engine.mock.MockRequestHandler) =
        HealthApi(createApiClient(MockEngine(handler), ApiConfig("http://localhost:8080")))

    @Test fun parsesHealthAndIgnoresUnknownFields() = runTest {
        val api = api { respond("""{"status":"ok","version":"1.2","minAppVersion":"0.5.0","extra":1}""", HttpStatusCode.OK, json) }
        val result = api.health()
        assertTrue(result is Outcome.Success)
        assertEquals("0.5.0", (result as Outcome.Success).value.minAppVersion)
    }

    @Test fun minAppVersionMayBeMissing() = runTest {
        val api = api { respond("""{"status":"ok","version":"dev"}""", HttpStatusCode.OK, json) }
        assertEquals(null, ((api.health()) as Outcome.Success).value.minAppVersion)
    }

    @Test fun serviceUnavailableBecomesServerError() = runTest {
        val api = api { respond("""{"error":"db down","code":"service_unavailable"}""", HttpStatusCode.ServiceUnavailable, json) }
        val failure = api.health() as Outcome.Failure
        assertEquals(AppError.Server(503, "service_unavailable", "db down"), failure.error)
    }

    @Test fun ioFailureMeansOffline() = runTest {
        val api = api { throw IOException("no route") }
        assertEquals(AppError.Offline, (api.health() as Outcome.Failure).error)
    }

    @Test fun errorWithoutBodyStillMapped() = runTest {
        val api = api { respondError(HttpStatusCode.BadGateway) }
        val failure = api.health() as Outcome.Failure
        assertEquals(AppError.Server(502, null, null), failure.error)
    }
}
