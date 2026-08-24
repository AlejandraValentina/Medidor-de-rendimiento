package com.medidorderendimiento.domain

import kotlin.test.*

class EstimatorStabilityCalculatorTest {
    private val calculator = EstimatorStabilityCalculator()

    @Test fun `ES-01 close estimates over sufficient horizon are stable`() {
        assertEquals(EstimatorStabilityStatus.STABLE, calculator.calculate(series(List(10) { 2_400L + it % 3 * 5 }, spacing = 2)).status)
    }

    @Test fun `ES-02 alternating high quality estimates remain unstable`() {
        val result = calculator.calculate(series(listOf(2_450, 2_310, 2_420, 2_300)))
        assertEquals(EstimatorStabilityStatus.UNSTABLE, result.status)
    }

    @Test fun `ES-03 two dates are insufficient history`() {
        assertEquals(EstimatorStabilityStatus.INSUFFICIENT_HISTORY, calculator.calculate(series(listOf(2_400, 2_405))).status)
    }

    @Test fun `ES-04 seven improving estimates without horizon are stabilizing`() {
        assertEquals(EstimatorStabilityStatus.STABILIZING, calculator.calculate(series((0..6).map { 2_300L + it * 10 })).status)
    }

    @Test fun `ES-05 repeated runs on one day count once`() {
        val repeated = (1..12).map { estimate(0, 2_400, revision = it.toLong()) }
        assertEquals(1, calculator.calculate(repeated).distinctEstimateDays)
    }

    @Test fun `ES-06 identical evidence does not create independent confirmations`() {
        val values = (0..11).map { estimate(it, 2_400, evidence = "same-window") }
        assertEquals(1, calculator.calculate(values).distinctEstimateDays)
    }

    @Test fun `ES-07 chronological drift is unstable`() {
        assertEquals(EstimatorStabilityStatus.UNSTABLE,
            calculator.calculate(series(listOf(2_300, 2_305, 2_310, 2_315, 2_450, 2_455, 2_460, 2_465), spacing = 2)).status)
    }

    @Test fun `ES-08 low MAD cannot compensate excessive amplitude`() {
        val result = calculator.calculate(series(listOf(2_400, 2_400, 2_400, 2_400, 2_400, 2_400, 2_550)))
        assertEquals(0, result.madPermillion)
        assertEquals(EstimatorStabilityStatus.UNSTABLE, result.status)
    }

    @Test fun `ES-09 retrospective revision replaces same-day observation and invalidates old evidence`() {
        val base = series(List(10) { 2_400L }, spacing = 2).toMutableList()
        val original = base[4]
        base += original.copy(id = LocalId("revised"), centralEnergy = kcal(2_700), revision = 2, inputRevision = 2)
        val result = calculator.calculate(base)
        assertEquals(10, result.distinctEstimateDays)
        assertTrue(StabilityReason.INPUTS_REVISED in result.reasons)
        assertEquals(EstimatorStabilityStatus.UNSTABLE, result.status)
    }

    @Test fun `ES-10 input quality does not override estimator instability`() {
        val result = calculator.calculate(series(listOf(2_450, 2_310, 2_420, 2_300)))
        assertEquals(EstimatorStabilityStatus.UNSTABLE, result.status)
        assertTrue(series(listOf(2_450)).single().nutritionQuality.label == DataQualityLabel.HIGH)
    }

    private fun series(values: List<Long>, spacing: Int = 1) = values.mapIndexed { index, value -> estimate(index * spacing, value) }
    private fun estimate(dayOffset: Int, kcal: Long, revision: Long = 1, evidence: String = "e-$dayOffset") = TdeeEstimate(
        LocalId("t-$dayOffset-$revision"), CivilDay.parse("2026-08-01").plusDays(dayOffset.toLong()), TdeeEstimateKind.OBSERVATIONAL,
        kcal(kcal), maturity = TdeeMaturity.ADAPTIVE, nutritionQuality = quality(), weightConfidence = WeightTrendConfidence.HIGH,
        windowStart = CivilDay.parse("2026-07-05"), windowEnd = CivilDay.parse("2026-08-01").plusDays(dayOffset.toLong()),
        algorithmVersion = "a", policyVersion = "p", inputRevision = revision, evidenceKey = evidence, revision = revision)
    private fun quality() = NutritionQuality(10, 10, 10, 0, 0, 0, 0, 1_000_000, DataQualityLabel.HIGH, emptySet())
    private fun kcal(value: Long) = EnergyAmount.ofKilocalories(value)
    private fun CivilDay.plusDays(days: Long) = CivilDay.parse(value.plusDays(days).toString())
}
