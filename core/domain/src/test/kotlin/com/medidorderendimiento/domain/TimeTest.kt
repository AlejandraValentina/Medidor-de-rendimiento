package com.medidorderendimiento.domain

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimeTest {
    @Test
    fun `civil day represents an ISO calendar date deterministically`() {
        val day = CivilDay.of(2026, 8, 23)

        assertEquals(LocalDate.of(2026, 8, 23), day.value)
        assertEquals(day, CivilDay.parse("2026-08-23"))
        assertFailsWith<DateTimeException> { CivilDay.of(2026, 2, 30) }
    }

    @Test
    fun `clock is injectable and deterministic`() {
        val expected = Instant.parse("2026-08-23T10:15:30Z")
        val clock = ClockProvider { expected }

        assertEquals(expected, clock.now())
        assertEquals(expected, clock.now())
    }
}
