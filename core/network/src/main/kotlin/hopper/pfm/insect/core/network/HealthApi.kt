package hopper.pfm.insect.core.network

import hopper.pfm.insect.core.common.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import javax.inject.Inject

/** GET /health, the only endpoint outside /v1. */
class HealthApi @Inject constructor(private val client: HttpClient) {
    suspend fun health(): Outcome<HealthDto> = client.callApi { get("/health") }
}
