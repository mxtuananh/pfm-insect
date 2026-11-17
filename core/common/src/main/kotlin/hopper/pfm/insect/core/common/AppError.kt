package hopper.pfm.insect.core.common

/** Failure reasons the UI can translate. Branch on these, never on message text. */
sealed interface AppError {
    /** No connection or the server could not be reached. The app keeps working from local data. */
    data object Offline : AppError

    data object Timeout : AppError

    /** Access was refused and could not be renewed. The user has to sign in again. */
    data object Unauthorized : AppError

    /** The server answered with an error; code is the stable code of the error body (may be null). */
    data class Server(val httpStatus: Int, val code: String?, val message: String?) : AppError

    data class Unknown(val cause: Throwable?) : AppError
}
