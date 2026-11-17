package hopper.pfm.insect.core.model

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    /** IANA time zone, for example Asia/Ho_Chi_Minh. */
    val timezone: String,
    /** VND or USD. */
    val currency: String,
)

sealed interface SessionState {
    /** Not signed in. */
    data object SignedOut : SessionState

    data object SignedIn : SessionState

    /** The server rejected the refresh token: local data stays, the user only has to enter the password again. */
    data object Expired : SessionState
}
