package com.medidorderendimiento.domain

import java.time.Instant
import kotlin.test.*

class TdeeEstimatorTest {
    private val reference = CivilDay.parse("2026-08-28")
    private val start = CivilDay.parse("2026-08-01")
    private val estimator = TdeeEstimator()

    @Test fun `TD-01 stable intake and weight produces observed intake`() {
        assertEquals(2_100, estimate(days(28, 2_100), trend(0)).centralEnergy?.millicalories?.div(1_000))
    }

    @Test fun `TD-02 weight loss uses versioned 7700 policy`() {
        assertEquals(2_485, estimate(days(28, 2_100), trend(-350)).centralEnergy?.millicalories?.div(1_000))
    }

    @Test fun `TD-03 missing nutrition is never filled`() {
        val result = estimate(days(8, 2_100), trend(0))
        assertNull(result.centralEnergy)
        assertEquals(8, result.nutritionQuality.eligibleDays)
        assertEquals(DataQualityLabel.INSUFFICIENT, result.nutritionQuality.label)
    }

    @Test fun `TD-04 robust trend output is consumed without raw weight duplication`() {
        val result = estimate(days(28, 2_100), trend(-100))
        assertEquals(2_210, result.centralEnergy?.millicalories?.div(1_000))
    }

    @Test fun `TD-05 no contemporary weight trend is unavailable without invented prior`() {
        assertEquals(TdeeMaturity.UNAVAILABLE, estimate(days(28, 2_100), trend(null)).maturity)
    }

    @Test fun `TD-06 estimator contract has no wearable energy input`() {
        assertEquals(2_100, estimate(days(28, 2_100), trend(0)).centralEnergy?.millicalories?.div(1_000))
    }

    @Test fun `TD-07 actual intake is used instead of plan target`() {
        val actual = days(28, 2_200, plan = "plan-2050")
        assertEquals(2_200, estimate(actual, trend(0)).centralEnergy?.millicalories?.div(1_000))
    }

    @Test fun `TD-08 plan change deterministically uses latest homogeneous segment`() {
        val first = days(14, 2_000, "old", 0)
        val second = days(14, 2_200, "new", 14)
        val result = estimate(first + second, trend(0))
        assertEquals(2_200, result.centralEnergy?.millicalories?.div(1_000))
        assertTrue(NutritionQualityReason.MIXED_PLAN_VERSIONS in result.nutritionQuality.reasons)
        assertEquals(14, result.nutritionQuality.eligibleDays)
    }

    @Test fun `estimated days above policy and zero intake require explicit review`() {
        val estimated = TdeeNutritionDay(start, TdeeDiaryState.CLOSED_WITH_ESTIMATES, kcal(1_000), kcal(1_000))
        val zero = TdeeNutritionDay(CivilDay.parse("2026-08-02"), TdeeDiaryState.ZERO_INTAKE_CONFIRMED, kcal(0))
        val (quality, eligible) = NutritionQualityCalculator().calculate(listOf(estimated, zero), 1)
        assertTrue(eligible.isEmpty())
        assertTrue(NutritionQualityReason.ESTIMATION_TOO_HIGH in quality.reasons)
        assertTrue(NutritionQualityReason.ZERO_INTAKE_REQUIRES_REVIEW in quality.reasons)
    }

    private fun estimate(days: List<TdeeNutritionDay>, trend: WeightTrend) = estimator.estimate(
        LocalId("tdee"), reference, start, days, trend, 1, "evidence")

    private fun days(count: Int, kcal: Long, plan: String = "plan", offset: Int = 0) = (0 until count).map {
        TdeeNutritionDay(CivilDay.parse("2026-08-01").plusDays((offset + it).toLong()), TdeeDiaryState.CLOSED_CONFIRMED,
            kcal(kcal), planVersionId = LocalId(plan))
    }

    private fun trend(rate: Long?): WeightTrend = WeightTrend(reference,
        WeightMeasurement(LocalId("w"), BodyMass.ofGrams(90_000), Instant.parse("2026-08-28T07:00:00Z"), reference),
        rate?.let { BodyMass.ofGrams(90_000) }, rate, 100, emptyList(), emptyList(), emptyList(),
        WeightTrendCoverage(10, 27, 4), if (rate == null) WeightTrendConfidence.UNAVAILABLE else WeightTrendConfidence.HIGH, emptySet())

    private fun kcal(value: Long) = EnergyAmount.ofKilocalories(value)
    private fun CivilDay.plusDays(days: Long) = CivilDay.parse(value.plusDays(days).toString())
}
