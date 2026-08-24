package com.medidorderendimiento.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.*
import kotlin.math.abs

class WeightTrendCalculatorTest {
    private val calculator = WeightTrendCalculator()
    private val reference = day("2026-08-24")

    @Test fun `WT-01 irregular real observations produce trend without filling missing days`() {
        val observations = (0..2).flatMap { week -> listOf(0, 2, 3, 5).map { offset ->
            observation(week * 7 + offset, 92_000L - (week * 7 + offset) * 30)
        } }
        val result = calculator.calculate(observations, reference)
        assertTrue(result.isAvailable)
        assertEquals(observations.size, result.selections.size)
        assertEquals(observations.map { it.measurement.id }.toSet(), result.included.map { it.measurement.id }.toSet())
    }

    @Test fun `WT-02 isolated high value is signaled and does not dominate slope`() {
        val observations = (0..24 step 3).map { observation(it, 92_000L - it * 20) }.toMutableList()
        observations[4] = observation(12, 95_000)
        val result = calculator.calculate(observations, reference)
        assertEquals(1, result.possibleOutliers.size)
        assertTrue(abs(requireNotNull(result.weeklyRateGrams) + 140) < 30)
    }

    @Test fun `WT-03 two observations keep latest but trend is unavailable`() {
        val values = listOf(observation(0, 92_000), observation(3, 91_900))
        val result = calculator.calculate(values, reference)
        assertEquals(values.last().measurement, result.latestObserved)
        assertNull(result.estimatedMass)
        assertNull(result.weeklyRateGrams)
        assertEquals(WeightTrendConfidence.UNAVAILABLE, result.confidence)
    }

    @Test fun `WT-04 same-day originals remain traceable and selection is deterministic`() {
        val sameDay = listOf(
            observation(10, 92_200, "a", WeighingConditions(morning = false)),
            observation(10, 92_000, "b", WeighingConditions(usualScale = true, morning = true, fasting = true)),
            observation(10, 92_100, "c", WeighingConditions(morning = true)),
        )
        val result = calculator.calculate(sameDay + (0..24 step 4).map { observation(it, 92_500L - it * 20) }, reference)
        val selection = result.selections.single { it.civilDay == dayAtOffset(10) }
        assertEquals("b", selection.selected.measurement.id.value)
        assertEquals(2, selection.alternatives.size)
        assertEquals(3, sameDay.size)
    }

    @Test fun `WT-05 persistent shift is marked as regime and not discarded as a series of outliers`() {
        val before = (0..8 step 2).map { observation(it, 92_000L + it * 10) }
        val after = (12..22 step 2).map { observation(it, 93_300L + (it - 12) * 10) }
        val result = calculator.calculate(before + after, reference)
        assertTrue(WeightTrendReason.POSSIBLE_REGIME_CHANGE in result.reasons)
        assertTrue(result.included.count { it.measurement in after.map(WeightObservation::measurement) } >= 3)
    }

    @Test fun `WT-06 unusual conditions warn but do not remove observation`() {
        val values = (0..24 step 3).map { offset ->
            observation(offset, 92_000L - offset * 10, conditions = if (offset == 12) WeighingConditions(afterTraining = true) else null)
        }
        val result = calculator.calculate(values, reference)
        assertTrue(WeightTrendReason.UNUSUAL_WEIGHING_CONDITIONS in result.reasons)
        assertTrue(result.selections.any { it.selected.conditions?.afterTraining == true })
        assertTrue(result.confidence != WeightTrendConfidence.HIGH)
    }

    @Test fun `WT-07 civil days define window without duplicating observations`() {
        val values = (0..24 step 4).map { offset -> observation(offset, 92_000L - offset * 15, instantHour = if (offset == 12) 23 else 7) }
        val result = calculator.calculate(values.shuffled(java.util.Random(4)), reference)
        assertEquals(values.size, result.selections.size)
        assertEquals(values.map { it.measurement.id }.toSet(), result.selections.map { it.selected.measurement.id }.toSet())
    }

    @Test fun `WT-08 short window never reports observed monthly change`() {
        val result = calculator.calculate((0..24 step 3).map { observation(it, 92_000L - it * 10) }, reference)
        assertNull(result.observedMonthlyChange)
    }

    @Test fun `AR-04 result references only real inputs and recomputation is deterministic`() {
        val values = (0..24 step 3).map { observation(it, 92_000L - it * 20) }
        val first = calculator.calculate(values, reference)
        val second = calculator.calculate(values.reversed(), reference)
        assertEquals(first, second)
        assertTrue(first.included.all { it in values })
        assertEquals(values.size, first.selections.sumOf { 1 + it.alternatives.size })
    }

    @Test fun `weekly slope keeps grams-per-week units`() {
        val result = calculator.calculate((0..24 step 3).map { observation(it, 92_000L + it * 100) }, reference)
        assertEquals(700, result.weeklyRateGrams)
    }

    @Test fun `last real weight remains visible when it is older than analysis window`() {
        val oldDay = CivilDay.parse("2026-06-01")
        val old = WeightObservation(WeightMeasurement(LocalId("old"), BodyMass.ofGrams(93_000),
            Instant.parse("2026-06-01T07:00:00Z"), oldDay))
        val result = calculator.calculate(listOf(old), reference)
        assertEquals(old.measurement, result.latestObserved)
        assertFalse(result.isAvailable)
    }

    private fun observation(
        offsetFromStart: Int,
        grams: Long,
        id: String = "w-$offsetFromStart",
        conditions: WeighingConditions? = null,
        instantHour: Int = 7,
    ): WeightObservation {
        val civilDay = dayAtOffset(offsetFromStart)
        val instant = civilDay.value.atTime(instantHour, 0).toInstant(ZoneOffset.UTC)
        return WeightObservation(WeightMeasurement(LocalId(id), BodyMass.ofGrams(grams), instant, civilDay), conditions)
    }

    private fun dayAtOffset(offset: Int): CivilDay {
        val date = reference.value.minusDays(24).plusDays(offset.toLong())
        return CivilDay.of(date.year, date.monthValue, date.dayOfMonth)
    }

    private fun day(value: String): CivilDay {
        val date = LocalDate.parse(value)
        return CivilDay.of(date.year, date.monthValue, date.dayOfMonth)
    }
}
