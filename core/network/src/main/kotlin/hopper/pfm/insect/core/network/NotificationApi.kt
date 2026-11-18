package hopper.pfm.insect.core.network

import hopper.pfm.insect.core.common.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class NotificationDto(
    val id: String,
    val kind: String,
    val threshold: Int? = null,
    val budgetId: String,
    val categoryId: String? = null,
    val period: String,
    val periodStart: String,
    val periodEnd: String,
    val amount: Long,
    val spent: Long,
    val percentUsed: Long,
    val createdAt: Long,
    val read: Boolean,
    val readAt: Long? = null,
    val version: Long,
    val updatedAt: Long,
)

@Serializable
data class NotificationPage(
    val items: List<NotificationDto>,
    val nextCursor: String? = null,
    val syncCursor: String,
    val unreadCount: Int = 0,
)

/** One Server-Sent Event: its name ("ready", "notification") and the data lines joined. */
data class StreamEvent(val name: String, val data: String)

class NotificationApi @Inject constructor(private val client: HttpClient) {

    /** Newest first; pass the nextCursor of the previous page as before to get older ones. */
    suspend fun page(before: String?): Outcome<NotificationPage> = client.callApi {
        get("/v1/notifications") {
            parameter("limit", CHANGES_PAGE_SIZE)
            before?.let { parameter("before", it) }
        }
    }

    suspend fun changes(cursor: String): Outcome<ChangesResponse<NotificationDto>> = client.callApi {
        get("/v1/notifications/changes") {
            parameter("cursor", cursor)
            parameter("limit", CHANGES_PAGE_SIZE)
        }
    }

    suspend fun markRead(id: String): Outcome<NotificationDto> = client.callApi { put("/v1/notifications/$id/read") }

    /**
     * Live alerts (Server-Sent Events). The flow ends when the connection closes and throws when it cannot be opened, so
     * the caller reconnects with a back-off. The request has no timeout: the server sends a comment every 20 seconds.
     */
    fun stream(): Flow<StreamEvent> = flow {
        client.prepareGet("/v1/notifications/stream") {
            header(HttpHeaders.Accept, "text/event-stream")
            header(HttpHeaders.CacheControl, "no-cache")
            // The default timeouts of the client would cut an idle stream.
            timeout {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
        }.execute { response ->
            if (!response.status.isSuccess()) throw java.io.IOException("stream answered ${response.status.value}")
            val channel = response.bodyAsChannel()
            var name = "message"
            val data = StringBuilder()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                when {
                    line.isEmpty() -> {
                        if (data.isNotEmpty()) emit(StreamEvent(name, data.toString().trimEnd('\n')))
                        name = "message"
                        data.clear()
                    }
                    line.startsWith(":") -> Unit // comment used as keep-alive
                    line.startsWith("event:") -> name = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> data.append(line.removePrefix("data:").trim()).append('\n')
                }
            }
        }
    }
}

