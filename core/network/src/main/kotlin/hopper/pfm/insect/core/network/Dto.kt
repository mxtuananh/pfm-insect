package hopper.pfm.insect.core.network

import kotlinx.serialization.Serializable

/** Error body of every non-2xx answer: code is stable and safe to branch on. */
@Serializable
data class ErrorDto(val error: String, val code: String)

@Serializable
data class HealthDto(
    val status: String,
    val version: String,
    val minAppVersion: String? = null,
)
