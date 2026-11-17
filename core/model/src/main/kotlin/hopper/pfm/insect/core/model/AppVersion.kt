package hopper.pfm.insect.core.model

/**
 * Dotted numeric version such as "1.2.3". Anything after the first non-numeric character of a part
 * (for example "-debug") is ignored, and missing parts count as zero.
 */
class AppVersion private constructor(private val parts: List<Int>) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (i in 0 until size) {
            val c = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    override fun equals(other: Any?): Boolean = other is AppVersion && compareTo(other) == 0

    override fun hashCode(): Int = parts.dropLastWhile { it == 0 }.hashCode()

    override fun toString(): String = parts.joinToString(".")

    companion object {
        /** Returns null when the text does not start with a number. */
        fun parseOrNull(text: String?): AppVersion? {
            if (text.isNullOrBlank()) return null
            val numbers = text.trim().split('.').map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }
            val leading = numbers.takeWhile { it != null }.filterNotNull()
            return if (leading.isEmpty()) null else AppVersion(leading)
        }
    }
}
