package com.medidorderendimiento.domain

import java.time.Instant
import kotlin.test.*

class PlanEvaluatorTest {
    private val evaluator = PlanEvaluator()
    private val profile = LocalId("profile")

    @Test fun `PE-01 missing evidence is insufficient`() = assertEquals(PlanDecision.INSUFFICIENT_DATA,
        evaluator.evaluate(input(0, observed = null), null).evaluation.candidateDecision)

    @Test fun `PE-02 response within tolerance maintains`() = assertEquals(PlanDecision.MAINTAIN,
        evaluator.evaluate(input(0, observed = -340), null).evaluation.candidateDecision)

    @Test fun `PE-03 strong excessive loss suggests adjust up`() = assertEquals(PlanDecision.ADJUST_UP,
        evaluator.evaluate(input(0, observed = -750), null).evaluation.candidateDecision)

    @Test fun `PE-04 strong slow loss suggests adjust down`() = assertEquals(PlanDecision.ADJUST_DOWN,
        evaluator.evaluate(input(0, observed = -80), null).evaluation.candidateDecision)

    @Test fun `PE-05 and PE-06 non-stable estimator preserves candidate but limits authorization`() {
        listOf(EstimatorStabilityStatus.STABILIZING, EstimatorStabilityStatus.UNSTABLE).forEach { status ->
            val result = evaluator.evaluate(input(0, -80, stability = status), null).evaluation
            assertEquals(PlanDecision.ADJUST_DOWN, result.candidateDecision)
            assertEquals(PlanDecision.OBSERVE, result.effectiveDecision)
            assertEquals(DecisionAuthorization.OBSERVE_ONLY, result.authorization)
        }
    }

    @Test fun `PE-07 caution cannot increase permission`() {
        val result = evaluator.evaluate(input(0, -80, safety = SafetyStatus.CAUTION), null).evaluation
        assertEquals(DecisionAuthorization.OBSERVE_ONLY, result.authorization)
    }

    @Test fun `PE-08 review required blocks immediately`() {
        val result = evaluator.evaluate(input(0, -80, safety = SafetyStatus.REVIEW_REQUIRED), null).evaluation
        assertEquals(DecisionAuthorization.BLOCKED, result.authorization)
        assertEquals(PlanDecision.ADJUST_DOWN, result.candidateDecision)
    }

    @Test fun `PE-09 high quality does not compensate unstable estimator`() {
        assertEquals(DecisionAuthorization.OBSERVE_ONLY,
            evaluator.evaluate(input(0, -80, stability = EstimatorStabilityStatus.UNSTABLE), null).evaluation.authorization)
    }

    @Test fun `PE-10 stable estimator does not compensate insufficient nutrition`() {
        val result = evaluator.evaluate(input(0, -80, quality = DataQualityLabel.INSUFFICIENT), null).evaluation
        assertEquals(DecisionAuthorization.BLOCKED, result.authorization)
    }

    @Test fun `HM-01 and HM-02 independent confirmations cross hysteresis`() {
        val first = evaluator.evaluate(input(0, -80), null)
        assertEquals(PlanDecision.OBSERVE, first.evaluation.effectiveDecision)
        val second = evaluator.evaluate(input(2, -80), first.memory)
        assertEquals(PlanDecision.ADJUST_DOWN, second.evaluation.effectiveDecision)
        assertEquals(DecisionAuthorization.PROPOSAL_ALLOWED, second.evaluation.authorization)
    }

    @Test fun `HM-03 HM-04 and HM-05 same date revision or evidence does not add a day`() {
        val first = evaluator.evaluate(input(0, -80), null)
        val repeated = evaluator.evaluate(input(0, -80, revision = 2), first.memory)
        assertEquals(1, repeated.memory?.qualifiedConfirmationCount)
        assertEquals(first.memory, repeated.memory)
    }

    @Test fun `HM-06 direction inversion resets streak`() {
        val down = evaluator.evaluate(input(0, -80), null)
        val up = evaluator.evaluate(input(2, -750), down.memory)
        assertEquals(1, up.memory?.qualifiedConfirmationCount)
        assertEquals(PlanDecision.ADJUST_UP, up.memory?.directionalCandidate)
    }

    @Test fun `HM-07 plan change resets memory`() {
        val first = evaluator.evaluate(input(0, -80), null)
        val changed = evaluator.evaluate(input(2, -80, planId = "new-plan"), first.memory)
        assertEquals(1, changed.memory?.qualifiedConfirmationCount)
        assertTrue(PlanEvaluationReason.PLAN_CHANGED in changed.evaluation.reasons)
    }

    @Test fun `HM-08 safety overrides a qualified streak immediately`() {
        val first = evaluator.evaluate(input(0, -80), null)
        val qualified = evaluator.evaluate(input(2, -80), first.memory)
        val blocked = evaluator.evaluate(input(4, -80, safety = SafetyStatus.REVIEW_REQUIRED), qualified.memory)
        assertEquals(DecisionAuthorization.BLOCKED, blocked.evaluation.authorization)
        assertEquals(0, blocked.memory?.qualifiedConfirmationCount)
    }

    @Test fun `HM-09 and HM-10 latest revisions rebuild deterministically`() {
        val first = evaluator.evaluate(input(0, -80), null).evaluation
        val oldSecond = evaluator.evaluate(input(2, -80), null).evaluation.copy(revision = 1)
        val revisedSecond = oldSecond.copy(id = LocalId("revision"), candidateDecision = PlanDecision.MAINTAIN,
            effectiveDecision = PlanDecision.MAINTAIN, operationalDecision = PlanDecision.MAINTAIN, revision = 2)
        val rebuilt = DecisionStateMemoryRebuilder.rebuild(listOf(first, oldSecond, revisedSecond))
        assertEquals(0, rebuilt?.qualifiedConfirmationCount)
        assertEquals(PlanDecision.MAINTAIN, rebuilt?.lastEffectiveDecision)
        assertEquals(rebuilt, DecisionStateMemoryRebuilder.rebuild(listOf(revisedSecond, first, oldSecond)))
    }

    private fun input(dayOffset: Int, observed: Long?, stability: EstimatorStabilityStatus = EstimatorStabilityStatus.STABLE,
        safety: SafetyStatus = SafetyStatus.CLEAR, quality: DataQualityLabel = DataQualityLabel.HIGH,
        planId: String = "plan", revision: Long = 1): PlanEvaluatorInput {
        val day = CivilDay.parse("2026-08-01").plusDays(dayOffset.toLong())
        val plan = NutritionPlanVersion(LocalId(planId), NutritionGoal.LOSS, EnergyAmount.ofKilocalories(2_000), null,
            TargetWeeklyRate.ofGrams(350), CivilDay.parse("2026-07-01"), acceptance = PlanAcceptance(Instant.EPOCH))
        return PlanEvaluatorInput(LocalId("evaluation-$dayOffset-$revision"), profile, day, plan, trend(day, observed),
            tdee(day, quality), stability(stability), safety, revision)
    }
    private fun trend(day: CivilDay, rate: Long?) = WeightTrend(day, null, rate?.let { BodyMass.ofGrams(90_000) }, rate, 100,
        emptyList(), emptyList(), emptyList(), WeightTrendCoverage(10, 27, 4), if (rate == null) WeightTrendConfidence.UNAVAILABLE else WeightTrendConfidence.HIGH, emptySet())
    private fun tdee(day: CivilDay, label: DataQualityLabel) = TdeeEstimate(LocalId("tdee"), day, TdeeEstimateKind.OBSERVATIONAL,
        EnergyAmount.ofKilocalories(2_400), maturity = TdeeMaturity.ADAPTIVE,
        nutritionQuality = NutritionQuality(21, 21, 21, 0, 0, 0, 0, if (label == DataQualityLabel.HIGH) 900_000 else 300_000, label, emptySet()),
        weightConfidence = WeightTrendConfidence.HIGH, windowStart = CivilDay.parse("2026-07-01"), windowEnd = day,
        algorithmVersion = "a", policyVersion = "p", inputRevision = 1, evidenceKey = "tdee")
    private fun stability(status: EstimatorStabilityStatus) = EstimatorStability(status, 10, 14, EnergyAmount.ofKilocalories(2_400), 0, 0, 0, 0, emptySet(), "p")
    private fun CivilDay.plusDays(days: Long) = CivilDay.parse(value.plusDays(days).toString())
}
