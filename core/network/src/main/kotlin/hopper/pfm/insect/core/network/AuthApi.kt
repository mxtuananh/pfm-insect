package hopper.pfm.insect.core.network

import hopper.pfm.insect.core.common.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

/** Endpoints under /v1/auth and /v1/users/me. */
class AuthApi @Inject constructor(private val client: HttpClient) {

    suspend fun register(email: String, password: String): Outcome<Unit> =
        client.callApiUnit {
            post("/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(email, password))
            }
        }

    suspend fun login(email: String, password: String, deviceId: String): Outcome<LoginResponse> =
        client.callApi {
            post("/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password, deviceId))
            }
        }

    /** Revokes the refresh token of this device. The server always answers 204. */
    suspend fun logout(refreshToken: String): Outcome<Unit> =
        client.callApiUnit {
            post("/v1/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }
        }

    suspend fun logoutAll(): Outcome<Unit> = client.callApiUnit { post("/v1/auth/logout-all") }

    suspend fun me(): Outcome<UserProfileDto> = client.callApi { get("/v1/users/me") }

    suspend fun updateMe(request: UpdateProfileRequest): Outcome<UserProfileDto> =
        client.callApi {
            put("/v1/users/me") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
}
