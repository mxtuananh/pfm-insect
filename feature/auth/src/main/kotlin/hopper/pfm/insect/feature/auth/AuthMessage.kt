package hopper.pfm.insect.feature.auth

import androidx.annotation.StringRes
import hopper.pfm.insect.core.common.AppError

/** Reasons shown to the user. ViewModels hold these; the screens translate them. */
enum class AuthMessage(@StringRes val res: Int) {
    InvalidEmail(R.string.error_invalid_email),
    PasswordRequired(R.string.error_password_required),
    PasswordRules(R.string.error_password_rules),
    PasswordMismatch(R.string.error_password_mismatch),
    InvalidCredentials(R.string.error_invalid_credentials),
    EmailTaken(R.string.error_email_taken),
    RateLimited(R.string.error_rate_limited),
    Offline(R.string.error_offline),
    Generic(R.string.error_generic),
    NameInvalid(R.string.error_name_invalid),
    CurrencyLocked(R.string.error_currency_locked),
}

/** Branches on the stable error code of the server, never on the English message. */
internal fun AppError.toAuthMessage(): AuthMessage = when (this) {
    AppError.Offline, AppError.Timeout -> AuthMessage.Offline
    AppError.Unauthorized -> AuthMessage.InvalidCredentials
    is AppError.Server -> when {
        code == "invalid_credentials" -> AuthMessage.InvalidCredentials
        code == "email_taken" -> AuthMessage.EmailTaken
        code == "currency_locked" -> AuthMessage.CurrencyLocked
        code == "rate_limited" || httpStatus == 429 -> AuthMessage.RateLimited
        else -> AuthMessage.Generic
    }
    is AppError.Unknown -> AuthMessage.Generic
}
