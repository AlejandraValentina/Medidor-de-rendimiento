package com.medidorderendimiento.domain

import java.time.Instant
import kotlin.test.*

class ShadowValidationTest {
    private val analyzer = ShadowValidationAnalyzer()
    private val plan = NutritionPlanVersion(LocalId("plan"), NutritionGoal.LOSS, EnergyAmount.ofKilocalories(2_000), null,
        TargetWeeklyRate.ofGrams(350), CivilDay.parse("2026-01-01"), acceptance = PlanAcceptance(Instant.EPOCH))

    @Test fun `SV-01 no evaluations is insufficient`() {
        assertEquals(ShadowValidationStatus.INSUFFICIENT_EVIDENCE,
            analyzer.analyze(ShadowValidationInput(plan.id, emptyList())).status)
    }

    @Test fun `prospective input revision combines real source revisions deterministically`() {
        assertEquals(ShadowInputRevision.combine(2, 3, 4), ShadowInputRevision.combine(2, 3, 4))
        assertNotEquals(ShadowInputRevision.combine(2, 3, 4), ShadowInputRevision.combine(2, 3, 5))
    }

    @Test fun `SV-02 and SV-03 require 28 evaluable and 14 prospective days`() {
        val twentySeven = evaluations(27)
        assertEquals(ShadowCriterionState.NOT_MET, report(twentySeven, twentySeven.map { it.referenceDay }.toSet()).criterion(ShadowValidationCriterion.PERSONAL_WINDOW))
        val twentyEight = evaluations(28)
        assertNotEquals(ShadowValidationStatus.READY_FOR_HUMAN_REVIEW, report(twentyEight, twentyEight.take(13).map { it.referenceDay }.toSet()).status)
    }

    @Test fun `SV-04 to SV-06 weight and nutrition boundaries are exact`() {
        val base = evaluations(28)
        assertEquals(ShadowCriterionState.NOT_MET, report(base.map { it.copy(weightDistinctDays = 7) }, base.days()).criterion(ShadowValidationCriterion.WEIGHT_AND_NUTRITION))
        assertEquals(ShadowCriterionState.NOT_MET, report(base.map { it.copy(eligibleNutritionDays = 23) }, base.days()).criterion(ShadowValidationCriterion.WEIGHT_AND_NUTRITION))
        assertEquals(ShadowCriterionState.MET, report(base.map { it.copy(eligibleNutritionDays = 24) }, base.days()).criterion(ShadowValidationCriterion.WEIGHT_AND_NUTRITION))
    }

    @Test fun `SV-07 to SV-09 stable TDEE gate uses distinct current dates`() {
        val base = evaluations(28)
        val sixStable = base.mapIndexed { index, e -> if (index < 6) e else e.copy(estimatorStabilityStatus = EstimatorStabilityStatus.STABILIZING) }
        assertEquals(ShadowCriterionState.NOT_MET, report(sixStable, base.days()).criterion(ShadowValidationCriterion.STABLE_TDEE))
        val sevenStable = base.mapIndexed { index, e -> if (index < 7) e else e.copy(estimatorStabilityStatus = EstimatorStabilityStatus.STABILIZING) }
        assertEquals(ShadowCriterionState.MET, report(sevenStable, base.days()).criterion(ShadowValidationCriterion.STABLE_TDEE))
        val unstable = base.mapIndexed { index, e -> if (index == 10) e.copy(estimatorStabilityStatus = EstimatorStabilityStatus.UNSTABLE) else e }
        assertEquals(ShadowCriterionState.NOT_MET, report(unstable, base.days()).criterion(ShadowValidationCriterion.STABLE_TDEE))
    }

    @Test fun `estimated energy criterion reuses the TDEE policy boundary`() {
        val base = evaluations(28)
        assertEquals(ShadowCriterionState.MET,
            report(base.map { it.copy(estimatedEnergyPermillion = 350_000) }, base.days()).criterion(ShadowValidationCriterion.ESTIMATED_ENERGY))
        assertEquals(ShadowCriterionState.NOT_MET,
            report(base.map { it.copy(estimatedEnergyPermillion = 350_001) }, base.days()).criterion(ShadowValidationCriterion.ESTIMATED_ENERGY))
    }

    @Test fun `SV-10 plan versions are never mixed`() {
        val old = evaluations(20)
        val newPlan = LocalId("new-plan")
        val newer = evaluations(10, 20).map { it.copy(planVersionId = newPlan) }
        val report = analyzer.analyze(ShadowValidationInput(newPlan, old + newer, emptyList(), newer.days(), ShadowReplayStatus.MATCH,
            ShadowScenarioEvidence(true, true, true)))
        assertEquals(10, report.evaluableDays)
    }

    @Test fun `SH-05 to SH-07 incomplete inputs prolong shadow and technical completion still requires human review`() {
        val base = evaluations(28)
        val incomplete = base.map { it.copy(nutritionQualityLabel = DataQualityLabel.LOW, eligibleNutritionDays = 10) }
        assertEquals(ShadowValidationStatus.IN_PROGRESS, report(incomplete, base.days()).status)
        val ready = report(base, base.days())
        assertEquals(ShadowValidationStatus.READY_FOR_HUMAN_REVIEW, ready.status)
        assertEquals(ShadowCriterionState.HUMAN_REVIEW_REQUIRED, ready.criterion(ShadowValidationCriterion.HUMAN_REVIEW))
        assertTrue(base.all { it.evaluationMode == EvaluationMode.SHADOW && !it.operational && it.operationalDecision == null })
    }

    @Test fun `SH-06 unexplained alternation blocks transition`() {
        val base = evaluations(28).toMutableList()
        base[2] = base[2].copy(candidateDecision = PlanDecision.ADJUST_DOWN, reasons = setOf(PlanEvaluationReason.SHADOW_NON_OPERATIONAL))
        base[3] = base[3].copy(candidateDecision = PlanDecision.ADJUST_UP, reasons = setOf(PlanEvaluationReason.SHADOW_NON_OPERATIONAL))
        assertEquals(ShadowValidationStatus.BLOCKED_BY_INCONSISTENCY, report(base, base.days()).status)
    }

    @Test fun `RV-01 and RV-02 stability policy is copied rather than hardcoded`() {
        assertEquals("stability-v1", evaluation(0, "stability-v1").estimatorStabilityPolicyVersion)
        assertEquals("stability-test-v2", evaluation(0, "stability-test-v2").estimatorStabilityPolicyVersion)
    }

    @Test fun `RV-04 legacy replay stays incomplete without inventing policy`() {
        val e = evaluation(0).copy(estimatorStabilityPolicyVersion = null)
        val replay = ShadowReplayEngine().replay(listOf(ShadowReplayItem(input(0), e)))
        assertEquals(ShadowReplayStatus.INPUT_INCOMPLETE, replay.status)
        assertTrue(ShadowReplayReason.LEGACY_STABILITY_POLICY_UNKNOWN in replay.reasons)
        val validation = analyzer.analyze(ShadowValidationInput(plan.id, listOf(e), replayStatus = replay.status))
        assertEquals(ShadowCriterionState.NOT_MET, validation.criterion(ShadowValidationCriterion.REPRODUCIBILITY))
    }

    @Test fun `RP-01 RP-02 RP-03 and RV-05 replay is deterministic and latest revision wins`() {
        val online = online(3)
        val items = online.mapIndexed { index, evaluation -> ShadowReplayItem(input(index),
            if (index == 1) evaluation.copy(revision = 2) else evaluation) }
        val old = items[1].copy(expected = items[1].expected.copy(revision = 1, evidenceKey = "obsolete"))
        val first = ShadowReplayEngine().replay(items + old)
        val second = ShadowReplayEngine().replay((items + old).reversed())
        assertEquals(ShadowReplayStatus.MATCH, first.status)
        assertEquals(first, second)
        assertEquals("stability-v1", first.evaluations.last().estimatorStabilityPolicyVersion)
    }

    @Test fun `RP-05 day zero one two replay matches online memory`() {
        val online = online(3, -80)
        val replay = ShadowReplayEngine().replay(online.mapIndexed { index, e -> ShadowReplayItem(input(index, -80), e) })
        assertEquals(2, replay.memory?.qualifiedConfirmationCount)
        assertEquals(online.last().referenceDay, replay.memory?.lastQualifiedDay)
        assertEquals(PlanDecision.ADJUST_DOWN, replay.evaluations.last().effectiveDecision)
    }

    private fun report(evaluations: List<PlanEvaluation>, days: Set<CivilDay>) = analyzer.analyze(
        ShadowValidationInput(plan.id, evaluations, emptyList(), days, ShadowReplayStatus.MATCH, ShadowScenarioEvidence(true, true, true)))
    private fun ShadowValidationReport.criterion(value: ShadowValidationCriterion) = criteria.single { it.criterion == value }.state
    private fun List<PlanEvaluation>.days() = map { it.referenceDay }.toSet()
    private fun evaluations(count: Int, offset: Int = 0) = (0 until count).map { evaluation(it + offset) }
    private fun online(count: Int, observed: Long = -340): List<PlanEvaluation> {
        var memory: DecisionStateMemory? = null
        return (0 until count).map { index -> PlanEvaluator().evaluate(input(index, observed), memory).also { memory = it.memory }.evaluation }
    }
    private fun evaluation(day: Int, stabilityPolicy: String = "stability-v1") =
        PlanEvaluator().evaluate(input(day, stabilityPolicy = stabilityPolicy), null).evaluation
    private fun input(day: Int, observed: Long = -340, stabilityPolicy: String = "stability-v1"): PlanEvaluatorInput {
        val reference = CivilDay.parse("2026-02-01").let { CivilDay.parse(it.value.plusDays(day.toLong()).toString()) }
        val quality = NutritionQuality(28, 28, 24, 0, 0, 0, 0, 900_000, DataQualityLabel.HIGH, emptySet())
        val tdee = TdeeEstimate(LocalId("tdee-$day"), reference, TdeeEstimateKind.OBSERVATIONAL,
            EnergyAmount.ofKilocalories(2_300), maturity = TdeeMaturity.HIGH_QUALITY, nutritionQuality = quality,
            weightConfidence = WeightTrendConfidence.HIGH, stabilityStatus = EstimatorStabilityStatus.STABLE,
            windowStart = CivilDay.parse(reference.value.minusDays(27).toString()), windowEnd = reference,
            algorithmVersion = "tdee-a", policyVersion = "tdee-v1", inputRevision = day + 1L, evidenceKey = "tdee-e-$day")
        val trend = WeightTrend(reference, null, null, observed, 50, emptyList(), emptyList(), emptyList(),
            WeightTrendCoverage(8, 28, 4), WeightTrendConfidence.HIGH, emptySet())
        val stability = EstimatorStability(EstimatorStabilityStatus.STABLE, 10, 14, EnergyAmount.ofKilocalories(2_300),
            1_000, 2_000, 1_000, 0, emptySet(), stabilityPolicy)
        return PlanEvaluatorInput(LocalId("evaluation-$day"), LocalId("profile"), reference, plan, trend, tdee,
            stability, SafetyStatus.CLEAR, day + 1L)
    }
}
