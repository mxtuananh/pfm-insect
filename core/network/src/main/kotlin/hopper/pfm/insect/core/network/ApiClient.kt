package hopper.pfm.insect.core.network

import hopper.pfm.insect.core.common.AppError
import hopper.pfm.insect.core.common.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * JSON used for responses. The server may add fields (not a breaking change), so unknown fields are ignored when
 * reading; requests are written without nulls and with only the fields the contract defines.
 */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}

/** Where the API lives, for example http://localhost:8080 (each API class adds the /v1 prefix). */
data class ApiConfig(val baseUrl: String)

fun createApiClient(
    engine: HttpClientEngine,
    config: ApiConfig,
    configure: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(ApiJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
    defaultRequest {
        url.takeFrom(config.baseUrl)
        accept(ContentType.Application.Json)
    }
    configure()
}

/** Runs a request and turns every failure into an AppError value (cancellation is rethrown). */
suspend inline fun <reified T> HttpClient.callApi(crossinline request: suspend HttpClient.() -> HttpResponse): Outcome<T> =
    try {
        val response = request()
        if (response.status.isSuccess()) {
            Outcome.Success(response.body<T>())
        } else {
            Outcome.Failure(response.toAppError())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        Outcome.Failure(AppError.Timeout)
    } catch (e: IOException) {
        Outcome.Failure(AppError.Offline)
    } catch (e: Exception) {
        Outcome.Failure(AppError.Unknown(e))
    }

suspend fun HttpResponse.toAppError(): AppError {
    val body = try {
        body<ErrorDto>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
    if (status.value == 401 && body?.code != "invalid_credentials") return AppError.Unauthorized
    return AppError.Server(status.value, body?.code, body?.error)
}

/** Like callApi for endpoints that answer with an empty body (204). */
suspend inline fun HttpClient.callApiUnit(crossinline request: suspend HttpClient.() -> HttpResponse): Outcome<Unit> =
    try {
        val response = request()
        if (response.status.isSuccess()) Outcome.Success(Unit) else Outcome.Failure(response.toAppError())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        Outcome.Failure(AppError.Timeout)
    } catch (e: IOException) {
        Outcome.Failure(AppError.Offline)
    } catch (e: Exception) {
        Outcome.Failure(AppError.Unknown(e))
    }
