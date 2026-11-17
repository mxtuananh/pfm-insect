package hopper.pfm.insect.core.common

/**
 * Money is a whole number in the smallest unit of the currency: dong for VND (no decimals), cents for USD.
 * These helpers only format and parse text; amounts are never floating point.
 */
object Money {
    const val MAX_AMOUNT = 9_999_999_999_999L

    fun decimals(currency: String): Int = if (currency == "USD") 2 else 0

    fun symbol(currency: String): String = if (currency == "USD") "$" else "₫"

    /** "45.000" for 45000 VND, "12.34" for 1234 USD cents (digits grouped with a dot for VND, a comma for USD). */
    fun format(amount: Long, currency: String): String {
        val negative = amount < 0
        val abs = if (negative) -amount else amount
        val decimals = decimals(currency)
        val unit = pow10(decimals)
        val major = abs / unit
        val groupSeparator = if (decimals == 0) '.' else ','
        val grouped = major.toString().reversed().chunked(3).joinToString(groupSeparator.toString()).reversed()
        val text = if (decimals == 0) grouped else grouped + "." + (abs % unit).toString().padStart(decimals, '0')
        return if (negative) "-$text" else text
    }

    /** Amount with its symbol: "45.000 ₫" or "$12.34". */
    fun formatWithSymbol(amount: Long, currency: String): String =
        if (currency == "USD") symbol(currency) + format(amount, currency) else format(amount, currency) + " " + symbol(currency)

    /**
     * Reads what the user typed: digits for VND; digits with at most one "." and two decimals for USD. Separators
     * typed for grouping are ignored. Returns null for empty, malformed or too large input.
     */
    fun parse(text: String, currency: String): Long? {
        val decimals = decimals(currency)
        val cleaned = text.filter { it.isDigit() || (decimals > 0 && it == '.') }
        if (cleaned.isEmpty() || cleaned.count { it == '.' } > 1) return null
        val parts = cleaned.split('.')
        val whole = parts[0].ifEmpty { "0" }
        val fraction = parts.getOrElse(1) { "" }
        if (fraction.length > decimals) return null
        val digits = whole + fraction.padEnd(decimals, '0')
        if (digits.length > 15) return null
        return digits.toLongOrNull()?.takeIf { it <= MAX_AMOUNT }
    }

    private fun pow10(n: Int): Long {
        var r = 1L
        repeat(n) { r *= 10 }
        return r
    }
}
