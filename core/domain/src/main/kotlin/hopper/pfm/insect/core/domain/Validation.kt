package hopper.pfm.insect.core.domain

/** Password rule of the server: at least 8 characters, a lowercase letter, an uppercase letter, a digit, at most 72 bytes. */
object PasswordPolicy {
    const val MIN_LENGTH = 8
    const val MAX_BYTES = 72

    data class Check(
        val longEnough: Boolean,
        val hasLowerAndUpper: Boolean,
        val hasDigit: Boolean,
        val withinMaxBytes: Boolean,
    ) {
        val valid: Boolean get() = longEnough && hasLowerAndUpper && hasDigit && withinMaxBytes
    }

    fun check(password: String) = Check(
        longEnough = password.length >= MIN_LENGTH,
        hasLowerAndUpper = password.any { it.isLowerCase() } && password.any { it.isUpperCase() },
        hasDigit = password.any { it.isDigit() },
        withinMaxBytes = password.toByteArray(Charsets.UTF_8).size <= MAX_BYTES,
    )
}

object EmailPolicy {
    private val Pattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** A light check to catch typos; the server has the final say. */
    fun isValid(email: String): Boolean = Pattern.matches(email.trim())
}

object NamePolicy {
    const val MAX_LENGTH = 50

    fun isValid(name: String): Boolean = name.trim().length in 1..MAX_LENGTH
}
