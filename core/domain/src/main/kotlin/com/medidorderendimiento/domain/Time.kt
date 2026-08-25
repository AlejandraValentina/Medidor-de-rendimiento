package com.medidorderendimiento.domain

import java.time.Instant
import java.time.LocalDate

@JvmInline
value class CivilDay private constructor(val value: LocalDate) : Comparable<CivilDay> {
    override fun compareTo(other: CivilDay): Int = value.compareTo(other.value)

    companion object {
        fun of(year: Int, month: Int, dayOfMonth: Int): CivilDay =
            CivilDay(LocalDate.of(year, month, dayOfMonth))

        fun parse(isoDate: String): CivilDay = CivilDay(LocalDate.parse(isoDate))
    }
}

fun interface ClockProvider {
    fun now(): Instant
}
