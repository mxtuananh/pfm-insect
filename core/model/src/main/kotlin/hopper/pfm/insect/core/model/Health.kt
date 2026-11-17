package hopper.pfm.insect.core.model

/** Answer of GET /health. minAppVersion is the oldest app version the server still supports, if it says so. */
data class ServerHealth(
    val status: String,
    val serverVersion: String,
    val minAppVersion: AppVersion?,
)

/** Whether this app version may talk to the server. */
sealed interface AppSupport {
    data object Supported : AppSupport
    data class UpdateRequired(val minVersion: AppVersion) : AppSupport
}
