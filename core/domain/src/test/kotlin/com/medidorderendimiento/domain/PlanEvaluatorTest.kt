package com.medidorderendimiento.domain

import java.time.Instant
import kotlin.test.*

class PlanEvaluatorTest {
    private val evaluator = PlanEvaluator()
    private val profile = LocalId("profile")

    @Test fun `VT-02 VT-03 VT-04 explicit safety drives normal limited and blocked shadow evaluations`() {
        val clear = evaluator.evaluate(input(0, -80, safety = SafetyStatus.CLEAR), null).evaluation
        assertEquals(PlanDecision.ADJUST_DOWN, clear.candidateDecision)
        assertTrue(clear.qualifiedForHysteresis)
        val caution = evaluator.evaluate(input(0, -80, safety = SafetyStatus.CAUTION), null).evaluation
        assertEquals(PlanDecision.OBSERVE, caution.effectiveDecision)
        assertTrue(PlanEvaluationReason.SAFETY_CAUTION in caution.reasons)
        val review = evaluator.evaluate(input(0, -80, safety = SafetyStatus.REVIEW_REQUIRED), null).evaluation
        assertEquals(DecisionAuthorization.BLOCKED, review.authorization)
        assertTrue(PlanEvaluationReason.SAFETY_REVIEW_REQUIRED in review.reasons)
    }

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

    @Test fun `PE-06 low weight confidence cannot authorize a cut`() {
        val result = evaluator.evaluate(input(0, -80, weightConfidence = WeightTrendConfidence.LOW), null).evaluation
        assertEquals(PlanDecision.ADJUST_DOWN, result.candidateDecision)
        assertEquals(DecisionAuthorization.BLOCKED, result.authorization)
        assertTrue(PlanEvaluationReason.WEIGHT_QUALITY_INSUFFICIENT in result.reasons)
    }

    @Test fun `PE-11 performance priority does not reuse loss policy`() {
        val result = evaluator.evaluate(input(0, -80, goal = NutritionGoal.PERFORMANCE_PRIORITY), null).evaluation
        assertEquals(PlanDecision.OBSERVE, result.candidateDecision)
        assertTrue(PlanEvaluationReason.DIRECTIONAL_POLICY_UNAVAILABLE in result.reasons)
    }

    @Test fun `HM-01 and HM-02 independent confirmations cross hysteresis`() {
        val first = evaluator.evaluate(input(0, -80), null)
        assertEquals(PlanDecision.OBSERVE, first.evaluation.effectiveDecision)
        val second = evaluator.evaluate(input(2, -80), first.memory)
        assertEquals(PlanDecision.ADJUST_DOWN, second.evaluation.effectiveDecision)
        assertEquals(DecisionAuthorization.OBSERVE_ONLY, second.evaluation.authorization)
        assertNull(second.evaluation.operationalDecision)
        assertFalse(second.evaluation.operational)
    }

    @Test fun `confirmation spacing uses the last day that actually incremented the streak`() {
        val day0 = evaluator.evaluate(input(0, -80), null)
        assertEquals(1, day0.memory?.qualifiedConfirmationCount)
        assertEquals(CivilDay.parse("2026-08-01"), day0.memory?.lastQualifiedDay)

        val day1 = evaluator.evaluate(input(1, -80), day0.memory)
        assertEquals(1, day1.memory?.qualifiedConfirmationCount)
        assertEquals(CivilDay.parse("2026-08-02"), day1.memory?.lastProcessedDay)
        assertEquals(CivilDay.parse("2026-08-01"), day1.memory?.lastQualifiedDay)

        val day2 = evaluator.evaluate(input(2, -80), day1.memory)
        assertEquals(2, day2.memory?.qualifiedConfirmationCount)
        assertEquals(CivilDay.parse("2026-08-03"), day2.memory?.lastQualifiedDay)
        assertEquals(PlanDecision.ADJUST_DOWN, day2.evaluation.effectiveDecision)
        assertNull(day2.evaluation.operationalDecision)
        assertEquals(DecisionAuthorization.OBSERVE_ONLY, day2.evaluation.authorization)

        val rebuilt = DecisionStateMemoryRebuilder.rebuild(
            listOf(day0.evaluation, day1.evaluation, day2.evaluation),
        )
        assertEquals(day2.memory, rebuilt)
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
        val changed = evaluator.evaluate(input(2, -80, planId = "new-plan", planAgeDays = 2), first.memory)
        assertEquals(0, changed.memory?.qualifiedConfirmationCount)
        assertTrue(PlanEvaluationReason.PLAN_CHANGED in changed.evaluation.reasons)
        assertTrue(PlanEvaluationReason.COOLDOWN_ACTIVE in changed.evaluation.reasons)
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
            effectiveDecision = PlanDecision.MAINTAIN, operationalDecision = null, qualifiedForHysteresis = false, revision = 2)
        val rebuilt = DecisionStateMemoryRebuilder.rebuild(listOf(first, oldSecond, revisedSecond))
        assertEquals(0, rebuilt?.qualifiedConfirmationCount)
        assertEquals(PlanDecision.MAINTAIN, rebuilt?.lastEffectiveDecision)
        assertEquals(rebuilt, DecisionStateMemoryRebuilder.rebuild(listOf(revisedSecond, first, oldSecond)))
    }

    @Test fun `HY-03 qualified down persists between entry and exit thresholds`() {
        val first = evaluator.evaluate(input(0, -80), null)
        val qualified = evaluator.evaluate(input(2, -80), first.memory)
        val retained = evaluator.evaluate(input(3, -200), qualified.memory)
        assertEquals(PlanDecision.ADJUST_DOWN, retained.evaluation.effectiveDecision)
        assertEquals(2, retained.memory?.qualifiedConfirmationCount)
        assertTrue(PlanEvaluationReason.HYSTERESIS_RETAINED in retained.evaluation.reasons)
    }

    @Test fun `HY-04 down exits at positive 100 and up behaves symmetrically`() {
        val down = evaluator.evaluate(input(2, -80), evaluator.evaluate(input(0, -80), null).memory)
        assertEquals(PlanDecision.MAINTAIN, evaluator.evaluate(input(4, -250), down.memory).evaluation.candidateDecision)
        val up = evaluator.evaluate(input(2, -750), evaluator.evaluate(input(0, -750), null).memory)
        assertEquals(PlanDecision.ADJUST_UP, evaluator.evaluate(input(4, -500), up.memory).evaluation.effectiveDecision)
        assertEquals(PlanDecision.MAINTAIN, evaluator.evaluate(input(6, -450), up.memory).evaluation.candidateDecision)
    }

    @Test fun `PE-08 cooldown blocks day 13 and ends on day 14`() {
        val blocked = evaluator.evaluate(input(0, -80, planAgeDays = 13), null)
        assertTrue(PlanEvaluationReason.COOLDOWN_ACTIVE in blocked.evaluation.reasons)
        assertFalse(blocked.evaluation.qualifiedForHysteresis)
        assertFalse(PlanEvaluationReason.COOLDOWN_ACTIVE in evaluator.evaluate(input(0, -80, planAgeDays = 14), null).evaluation.reasons)
    }

    @Test fun `policy change has its own reason and discards old confirmations`() {
        val memory = evaluator.evaluate(input(0, -80), null).memory
        val changed = PlanEvaluator(PlanEvaluatorPolicy(version = "v2")).evaluate(input(2, -80), memory)
        assertTrue(PlanEvaluationReason.POLICY_CHANGED in changed.evaluation.reasons)
        assertFalse(PlanEvaluationReason.PLAN_CHANGED in changed.evaluation.reasons)
        assertEquals(1, changed.memory?.qualifiedConfirmationCount)
    }

    @Test fun `SH-01 to SH-03 shadow remains non operational when internally qualified`() {
        val down = evaluator.evaluate(input(2, -80), evaluator.evaluate(input(0, -80), null).memory).evaluation
        assertEquals(EvaluationMode.SHADOW, down.evaluationMode)
        assertEquals(PlanDecision.ADJUST_DOWN, down.effectiveDecision)
        assertNull(down.operationalDecision)
        assertEquals(DecisionAuthorization.OBSERVE_ONLY, down.authorization)
        val up = evaluator.evaluate(input(2, -750), evaluator.evaluate(input(0, -750), null).memory).evaluation
        assertEquals(PlanDecision.ADJUST_UP, up.effectiveDecision)
        assertFalse(up.operational)
    }

    @Test fun `blocked directional evaluation does not qualify during rebuild`() {
        val unstable = evaluator.evaluate(input(0, -80, stability = EstimatorStabilityStatus.UNSTABLE), null).evaluation
        val stable = evaluator.evaluate(input(2, -80), null).evaluation
        assertEquals(1, DecisionStateMemoryRebuilder.rebuild(listOf(unstable, stable))?.qualifiedConfirmationCount)
        val cooldown = evaluator.evaluate(input(0, -80, planAgeDays = 13), null).evaluation
        assertEquals(1, DecisionStateMemoryRebuilder.rebuild(listOf(cooldown, stable))?.qualifiedConfirmationCount)
    }

    @Test fun `HY-05 and HY-06 hard gates immediately invalidate a qualified direction`() {
        val qualified = evaluator.evaluate(input(2, -80), evaluator.evaluate(input(0, -80), null).memory)
        val unstable = evaluator.evaluate(input(4, -80, stability = EstimatorStabilityStatus.UNSTABLE), qualified.memory)
        assertEquals(0, unstable.memory?.qualifiedConfirmationCount)
        assertEquals(PlanDecision.OBSERVE, unstable.evaluation.effectiveDecision)
        val lowQuality = evaluator.evaluate(input(4, -80, quality = DataQualityLabel.LOW), qualified.memory)
        assertEquals(0, lowQuality.memory?.qualifiedConfirmationCount)
        assertEquals(DecisionAuthorization.BLOCKED, lowQuality.evaluation.authorization)
    }

    private fun input(dayOffset: Int, observed: Long?, stability: EstimatorStabilityStatus = EstimatorStabilityStatus.STABLE,
        safety: SafetyStatus = SafetyStatus.CLEAR, quality: DataQualityLabel = DataQualityLabel.HIGH,
        planId: String = "plan", revision: Long = 1, planAgeDays: Long = 31,
        goal: NutritionGoal = NutritionGoal.LOSS,
        weightConfidence: WeightTrendConfidence = WeightTrendConfidence.HIGH): PlanEvaluatorInput {
        val day = CivilDay.parse("2026-08-01").plusDays(dayOffset.toLong())
        val plan = NutritionPlanVersion(LocalId(planId), goal, EnergyAmount.ofKilocalories(2_000), null,
            TargetWeeklyRate.ofGrams(350).takeIf { goal == NutritionGoal.LOSS },
            CivilDay.parse(day.value.minusDays(planAgeDays).toString()), acceptance = PlanAcceptance(Instant.EPOCH))
        return PlanEvaluatorInput(LocalId("evaluation-$dayOffset-$revision"), profile, day, plan, trend(day, observed, weightConfidence),
            tdee(day, quality), stability(stability), safety, revision)
    }
    private fun trend(day: CivilDay, rate: Long?, confidence: WeightTrendConfidence) = WeightTrend(day, null,
        rate?.let { BodyMass.ofGrams(90_000) }, rate, 100, emptyList(), emptyList(), emptyList(),
        WeightTrendCoverage(10, 27, 4), if (rate == null) WeightTrendConfidence.UNAVAILABLE else confidence, emptySet())
    private fun tdee(day: CivilDay, label: DataQualityLabel) = TdeeEstimate(LocalId("tdee"), day, TdeeEstimateKind.OBSERVATIONAL,
        EnergyAmount.ofKilocalories(2_400), maturity = TdeeMaturity.ADAPTIVE,
        nutritionQuality = NutritionQuality(21, 21, 21, 0, 0, 0, 0, if (label == DataQualityLabel.HIGH) 900_000 else 300_000, label, emptySet()),
        weightConfidence = WeightTrendConfidence.HIGH, windowStart = CivilDay.parse("2026-07-01"), windowEnd = day,
        algorithmVersion = "a", policyVersion = "p", inputRevision = 1, evidenceKey = "tdee")
    private fun stability(status: EstimatorStabilityStatus) = EstimatorStability(status, 10, 14, EnergyAmount.ofKilocalories(2_400), 0, 0, 0, 0, emptySet(), "p")
    private fun CivilDay.plusDays(days: Long) = CivilDay.parse(value.plusDays(days).toString())
}
