package hopper.pfm.insect.core.network

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String, val deviceId: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LoginResponse(val accessToken: String, val refreshToken: String)

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    val name: String,
    val pfp: String? = null,
    val timezone: String,
    val currency: String,
    val version: Long,
    val updatedAt: Long,
)

/** Only the fields to change are sent (explicitNulls is off, so null means "leave as is"). */
@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val timezone: String? = null,
    val currency: String? = null,
)
