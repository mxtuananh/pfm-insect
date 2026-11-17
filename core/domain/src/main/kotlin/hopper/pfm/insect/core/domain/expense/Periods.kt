package hopper.pfm.insect.core.domain.expense

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class PeriodType { DAY, WEEK, MONTH }

/** A span of whole days, both ends included. */
data class Period(val type: PeriodType, val start: LocalDate, val end: LocalDate) {
    val days: Int get() = (end.toEpochDay() - start.toEpochDay()).toInt() + 1

    operator fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(end)

    /** The period of the same kind, n periods later (negative = earlier). */
    fun shifted(n: Int, weekStart: DayOfWeek): Period = when (type) {
        PeriodType.DAY -> of(type, start.plusDays(n.toLong()), weekStart)
        PeriodType.WEEK -> of(type, start.plusWeeks(n.toLong()), weekStart)
        PeriodType.MONTH -> of(type, start.plusMonths(n.toLong()), weekStart)
    }

    companion object {
        /** The period of the given kind that contains the anchor date. */
        fun of(type: PeriodType, anchor: LocalDate, weekStart: DayOfWeek): Period = when (type) {
            PeriodType.DAY -> Period(type, anchor, anchor)
            PeriodType.WEEK -> {
                val start = anchor.with(TemporalAdjusters.previousOrSame(weekStart))
                Period(type, start, start.plusDays(6))
            }
            PeriodType.MONTH -> {
                val month = YearMonth.from(anchor)
                Period(type, month.atDay(1), month.atEndOfMonth())
            }
        }
    }
}
