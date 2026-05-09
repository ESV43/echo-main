package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Suppress("MemberVisibilityCanBePrivate")
@Serializable
data class Date(
    val epochTimeMs: Long
) : Comparable<Date> {

    private val parts by lazy { epochMillisToDate(epochTimeMs) }

    constructor(
        year: Int,
        month: Int? = null,
        day: Int? = null,
    ) : this(dateToEpochMillis(year, month ?: 1, day ?: 1))

    companion object {
        fun Int.toYearDate() = Date(this)

        private const val MILLIS_PER_DAY = 86_400_000L

        private fun floorDiv(x: Int, y: Int): Int {
            var result = x / y
            if ((x xor y) < 0 && result * y != x) result--
            return result
        }

        private fun floorDiv(x: Long, y: Long): Long {
            var result = x / y
            if ((x xor y) < 0 && result * y != x) result--
            return result
        }

        private fun dateToEpochMillis(year: Int, month: Int, day: Int): Long {
            val adjustedYear = year - if (month <= 2) 1 else 0
            val era = floorDiv(adjustedYear, 400)
            val yearOfEra = adjustedYear - era * 400
            val adjustedMonth = month + if (month > 2) -3 else 9
            val dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
            val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
            return (era * 146097L + dayOfEra - 719468L) * MILLIS_PER_DAY
        }

        private fun epochMillisToDate(epochTimeMs: Long): Triple<Int, Int, Int> {
            var days = floorDiv(epochTimeMs, MILLIS_PER_DAY) + 719468L
            val era = floorDiv(days, 146097L)
            val dayOfEra = (days - era * 146097L).toInt()
            val yearOfEra =
                (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
            var year = yearOfEra + era * 400
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val monthPrime = (5 * dayOfYear + 2) / 153
            val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
            val month = monthPrime + if (monthPrime < 10) 3 else -9
            year += if (month <= 2) 1 else 0
            return Triple(year.toInt(), month, day)
        }
    }


    val year: Int by lazy { parts.first }
    val month: Int? by lazy {
        val isFirstDayOfYear = parts.second == 1 && parts.third == 1
        if (!isFirstDayOfYear) parts.second else null
    }

    val day: Int? by lazy {
        if (parts.third == 1) null
        else parts.third
    }

    override fun compareTo(other: Date): Int {
        return epochTimeMs.compareTo(other.epochTimeMs)
    }

    override fun toString(): String = when {
        month == null || day == null -> year.toString()
        else -> "${month}/${day}/${year}"
    }

}
