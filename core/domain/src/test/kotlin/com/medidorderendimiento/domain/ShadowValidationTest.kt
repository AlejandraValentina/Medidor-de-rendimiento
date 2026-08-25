package com.medidorderendimiento.domain

import java.time.Instant
import kotlin.test.*

class ShadowValidationTest {
    private val analyzer = ShadowValidationAnalyzer()
    private val plan = plan("plan")

    @Test fun `SV-01 no evaluations is insufficient`() = assertEquals(ShadowValidationStatus.INSUFFICIENT_EVIDENCE,
        analyzer.analyze(ShadowValidationInput(plan.id, emptyList())).status)

    @Test fun `SV-02 SV-03 require 28 evaluable and 14 proven prospective days`() {
        assertEquals(ShadowCriterionState.NOT_MET, report(evaluations(27)).criterion(ShadowValidationCriterion.PERSONAL_WINDOW))
        val thirteen = evaluations(28).mapIndexed { i, e -> e.copy(prospectiveObserved = i < 13) }
        assertEquals(ShadowCriterionState.NOT_MET, report(thirteen).criterion(ShadowValidationCriterion.PERSONAL_WINDOW))
        val revised = evaluations(28) + evaluation(0).copy(id = LocalId("revision"), revision = 2, prospectiveObserved = true)
        assertEquals(28, report(revised).evaluableDays)
        assertEquals(28, report(revised).prospectiveDays)
    }

    @Test fun `SV-04 SV-05 SV-06 weight and nutrition use real window evidence with exact boundaries`() {
        val base = evaluations(25)
        assertEquals(ShadowCriterionState.NOT_MET, report(base, weightCount = 7, eligibleDays = 21).criterion(ShadowValidationCriterion.WEIGHT_AND_NUTRITION))
        assertEquals(840_000, report(base, eligibleDays = 21).nutritionCoveragePermillion)
        assertEquals(ShadowCriterionState.NOT_MET, report(base, eligibleDays = 21).criterion(ShadowValidationCriterion.WEIGHT_AND_NUTRITION))
        assertEquals(850_000, report(evaluations(20), eligibleDays = 17).nutritionCoveragePermillion)
        assertEquals(ShadowCriterionState.MET, report(evaluations(20), eligibleDays = 17).criterion(ShadowValidationCriterion.WEIGHT_AND_NUTRITION))
    }

    @Test fun `history beyond 28 uses last evaluable dates and ignores evidence outside window`() {
        val base = evaluations(35)
        val report = report(base, weightCount = 8, eligibleDays = 24, evidenceOffset = 7)
        assertEquals(day(7), report.windowStart); assertEquals(day(34), report.windowEnd)
        assertEquals(28, report.totalWindowDays); assertEquals(857_142, report.nutritionCoveragePermillion)
        val outsideWeight = weight(day(0), "outside")
        val withOutside = analyzer.analyze(input(base, weights = weights(8, 7) + outsideWeight, nutrition = nutrition(24, 7)))
        assertEquals(8, withOutside.weightDistinctDays)
    }

    @Test fun `nutrition and previous plan outside selected window never count`() {
        val base = evaluations(28)
        val outside = TdeeNutritionDay(day(-1), TdeeDiaryState.CLOSED_CONFIRMED, EnergyAmount.ofKilocalories(2_000), planVersionId = plan.id)
        val oldPlan = TdeeNutritionDay(day(10), TdeeDiaryState.CLOSED_CONFIRMED, EnergyAmount.ofKilocalories(2_000), planVersionId = LocalId("old"))
        val report = analyzer.analyze(input(base, nutrition = nutrition(23) + outside + oldPlan))
        assertEquals(23, report.eligibleNutritionDays)
    }

    @Test fun `estimated energy is aggregated from eligible nutrition in selected window`() {
        val base = evaluations(28)
        val atLimit = nutrition(24, estimatedPermillion = 350_000)
        val over = nutrition(24, estimatedPermillion = 350_001)
        assertEquals(ShadowCriterionState.MET, analyzer.analyze(input(base, nutrition = atLimit)).criterion(ShadowValidationCriterion.ESTIMATED_ENERGY))
        assertEquals(ShadowCriterionState.NOT_MET, analyzer.analyze(input(base, nutrition = over)).criterion(ShadowValidationCriterion.ESTIMATED_ENERGY))
    }

    @Test fun `SV-07 SV-08 SV-09 count current TDEE dates only and preserve critical unstable`() {
        val base = evaluations(28)
        assertEquals(ShadowCriterionState.NOT_MET, report(base, stable = 6).criterion(ShadowValidationCriterion.STABLE_TDEE))
        assertEquals(ShadowCriterionState.MET, report(base, stable = 7).criterion(ShadowValidationCriterion.STABLE_TDEE))
        val estimates = tdees(7) + tdee(10, EstimatorStabilityStatus.UNSTABLE)
        assertEquals(ShadowCriterionState.NOT_MET, analyzer.analyze(input(base, tdees = estimates)).criterion(ShadowValidationCriterion.STABLE_TDEE))
    }

    @Test fun `SV-10 plan versions and their prospective provenance are never mixed`() {
        val newPlan = LocalId("new")
        val mixed = evaluations(20) + evaluations(10, 20).map { it.copy(planVersionId = newPlan) }
        val report = analyzer.analyze(input(mixed, currentPlan = newPlan))
        assertEquals(10, report.evaluableDays); assertEquals(10, report.prospectiveDays)
    }

    @Test fun `alternation requires new evidence and accepts an explained maintain bridge`() {
        val direct = evaluations(4).toMutableList()
        direct[0] = direct[0].copy(candidateDecision = PlanDecision.ADJUST_DOWN, reasons = setOf(PlanEvaluationReason.SHADOW_NON_OPERATIONAL))
        direct[1] = direct[1].copy(candidateDecision = PlanDecision.ADJUST_UP, reasons = setOf(PlanEvaluationReason.SHADOW_NON_OPERATIONAL))
        assertEquals(ShadowValidationStatus.BLOCKED_BY_INCONSISTENCY, report(direct).status)
        val explained = direct.toMutableList()
        explained[1] = explained[1].copy(candidateDecision = PlanDecision.MAINTAIN, reasons = setOf(PlanEvaluationReason.WITHIN_TOLERANCE))
        explained[2] = explained[2].copy(candidateDecision = PlanDecision.ADJUST_UP, reasons = setOf(PlanEvaluationReason.DIRECTIONAL_THRESHOLD_MET))
        assertNotEquals(ShadowValidationStatus.BLOCKED_BY_INCONSISTENCY, report(explained).status)
    }

    @Test fun `required scenarios remain pending for presence and require verified behavior`() {
        val base = evaluations(28)
        assertEquals(ShadowCriterionState.PENDING, analyzer.analyze(input(base).copy(scenarios = ShadowScenarioEvidence()))
            .criterion(ShadowValidationCriterion.REQUIRED_SCENARIOS))
        assertEquals(ShadowCriterionState.MET, analyzer.analyze(input(base).copy(scenarios = ShadowScenarioEvidence(true, true, true)))
            .criterion(ShadowValidationCriterion.REQUIRED_SCENARIOS))
    }

    @Test fun `SH-09 policy versions are exposed and mismatches require new review`() {
        val changed = evaluations(28).mapIndexed { i, e -> if (i == 27) e.copy(evaluatorPolicyVersion = "next") else e }
        val report = analyzer.analyze(input(changed, replay = ShadowReplayStatus.INPUT_INCOMPLETE))
        assertEquals(setOf("plan-evaluator-v1", "next"), report.evaluatorPolicyVersions)
        assertEquals(ShadowCriterionState.NOT_MET, report.criterion(ShadowValidationCriterion.REPRODUCIBILITY))
    }

    @Test fun `RP-01 RP-02 RP-03 replay is deterministic and latest revision wins`() {
        val online = online(3)
        val items = online.mapIndexed { i, e -> ShadowReplayItem(evaluatorInput(i), if (i == 1) e.copy(revision = 2) else e) }
        val obsolete = items[1].copy(expected = items[1].expected.copy(revision = 1, evidenceKey = "obsolete"))
        assertEquals(ShadowReplayEngine().replay(items + obsolete), ShadowReplayEngine().replay((items + obsolete).reversed()))
    }

    @Test fun `RP-04 duplicate evidence does not create an independent confirmation`() {
        val first = online(1, -80).single()
        val duplicate = first.copy(id = LocalId("dup"), referenceDay = day(2), revision = 1)
        val replay = ShadowReplayEngine().replay(listOf(ShadowReplayItem(evaluatorInput(0, -80), first),
            ShadowReplayItem(evaluatorInput(2, -80), duplicate)))
        assertEquals(ShadowReplayStatus.MISMATCH, replay.status)
    }

    @Test fun `RP-05 day zero one two and memory comparison match online`() {
        val online = online(3, -80); val expectedMemory = DecisionStateMemoryRebuilder.rebuild(online)
        val replay = ShadowReplayEngine().replay(online.mapIndexed { i, e -> ShadowReplayItem(evaluatorInput(i, -80), e) }, expectedMemory)
        assertEquals(ShadowReplayStatus.MATCH, replay.status); assertEquals(2, replay.memory?.qualifiedConfirmationCount)
        assertEquals(day(2), replay.memory?.lastQualifiedDay)
    }

    @Test fun `RP-06 plan change resets the directional streak`() {
        val a = online(2, -80)
        val planB = plan("plan-b", validFrom = day(-20))
        var memory = DecisionStateMemoryRebuilder.rebuild(a)
        val bInput = evaluatorInput(2, -80, planB)
        val b = PlanEvaluator().evaluate(bInput, memory).also { memory = it.memory }.evaluation
        val replay = ShadowReplayEngine().replay(a.mapIndexed { i, e -> ShadowReplayItem(evaluatorInput(i, -80), e) } + ShadowReplayItem(bInput, b), memory)
        assertEquals(ShadowReplayStatus.MATCH, replay.status); assertEquals(planB.id, replay.memory?.planVersionId)
        assertEquals(1, replay.memory?.qualifiedConfirmationCount)
    }

    @Test fun `RP-07 retrospective latest revision rebuilds memory and detects obsolete future`() {
        val original = online(3, -80)
        val correctedInput = evaluatorInput(0, -340)
        val corrected = PlanEvaluator().evaluate(correctedInput, null).evaluation.copy(revision = 2)
        val revisedItems = listOf(ShadowReplayItem(correctedInput, corrected)) + original.drop(1).mapIndexed { i, e -> ShadowReplayItem(evaluatorInput(i + 1, -80), e) }
        assertEquals(ShadowReplayStatus.MISMATCH, ShadowReplayEngine().replay(revisedItems).status)
        var memory: DecisionStateMemory? = null
        val coherent = (0..2).map { i -> val input = evaluatorInput(i, if (i == 0) -340 else -80)
            PlanEvaluator().evaluate(input, memory).also { memory = it.memory }.evaluation }
        assertEquals(ShadowReplayStatus.MATCH, ShadowReplayEngine().replay(coherent.mapIndexed { i, e ->
            ShadowReplayItem(evaluatorInput(i, if (i == 0) -340 else -80), e) }, memory).status)
    }

    private fun report(e: List<PlanEvaluation>, weightCount: Int = 8, eligibleDays: Int = minOf(24, e.size),
        stable: Int = 7, evidenceOffset: Int = 0) = analyzer.analyze(input(e, weights = weights(weightCount, evidenceOffset),
        nutrition = nutrition(eligibleDays, evidenceOffset), tdees = tdees(stable, evidenceOffset)))
    private fun input(e: List<PlanEvaluation>, currentPlan: LocalId = plan.id, weights: List<WeightMeasurement> = weights(8),
        nutrition: List<TdeeNutritionDay> = nutrition(24), tdees: List<TdeeEstimate> = tdees(7),
        replay: ShadowReplayStatus = ShadowReplayStatus.MATCH) = ShadowValidationInput(currentPlan, e, tdees, weights, nutrition,
        replay, ShadowScenarioEvidence(true, true, true))
    private fun ShadowValidationReport.criterion(c: ShadowValidationCriterion) = criteria.single { it.criterion == c }.state
    private fun evaluations(count: Int, offset: Int = 0) = (0 until count).map { evaluation(it + offset).copy(prospectiveObserved = true) }
    private fun evaluation(i: Int) = PlanEvaluator().evaluate(evaluatorInput(i), null).evaluation
    private fun online(count: Int, observed: Long = -340): List<PlanEvaluation> { var memory: DecisionStateMemory? = null
        return (0 until count).map { i -> PlanEvaluator().evaluate(evaluatorInput(i, observed), memory).also { memory = it.memory }.evaluation } }
    private fun evaluatorInput(i: Int, observed: Long = -340, selectedPlan: NutritionPlanVersion = plan): PlanEvaluatorInput {
        val quality = NutritionQuality(28, 28, 24, 0, 0, 0, 0, 900_000, DataQualityLabel.HIGH, emptySet())
        val tdee = TdeeEstimate(LocalId("tdee-$i-${selectedPlan.id.value}"), day(i), TdeeEstimateKind.OBSERVATIONAL,
            EnergyAmount.ofKilocalories(2_300), maturity = TdeeMaturity.HIGH_QUALITY, nutritionQuality = quality,
            weightConfidence = WeightTrendConfidence.HIGH, stabilityStatus = EstimatorStabilityStatus.STABLE,
            windowStart = day(i - 27), windowEnd = day(i), algorithmVersion = "tdee-a", policyVersion = "tdee-v1",
            inputRevision = i + 100L, evidenceKey = "tdee-e-$i-${selectedPlan.id.value}")
        return PlanEvaluatorInput(LocalId("evaluation-$i-${selectedPlan.id.value}"), LocalId("profile"), day(i), selectedPlan,
            WeightTrend(day(i), null, null, observed, 50, emptyList(), emptyList(), emptyList(), WeightTrendCoverage(8, 28, 4),
                WeightTrendConfidence.HIGH, emptySet()), tdee,
            EstimatorStability(EstimatorStabilityStatus.STABLE, 10, 14, EnergyAmount.ofKilocalories(2_300), 1_000, 2_000, 1_000, 0,
                emptySet(), "stability-v1"), SafetyStatus.CLEAR, i + 100L)
    }
    private fun weights(count: Int, offset: Int = 0) = (0 until count).map { weight(day(offset + it * 2), "w$offset-$it") }
    private fun weight(day: CivilDay, id: String) = WeightMeasurement(LocalId(id), BodyMass.ofGrams(80_000), Instant.EPOCH, day)
    private fun nutrition(count: Int, offset: Int = 0, estimatedPermillion: Int = 0) = (0 until count).map { i ->
        val total = 1_000_000L; val estimated = total * estimatedPermillion / 1_000_000
        TdeeNutritionDay(day(offset + i), if (estimated > 0) TdeeDiaryState.CLOSED_WITH_ESTIMATES else TdeeDiaryState.CLOSED_CONFIRMED,
            EnergyAmount.ofMillicalories(total - estimated), estimated.takeIf { it > 0 }?.let(EnergyAmount::ofMillicalories),
            planVersionId = plan.id)
    }
    private fun tdees(count: Int, offset: Int = 0) = (0 until count).map { tdee(offset + it, EstimatorStabilityStatus.STABLE) }
    private fun tdee(i: Int, status: EstimatorStabilityStatus): TdeeEstimate {
        val q = NutritionQuality(28, 28, 24, 0, 0, 0, 0, 900_000, DataQualityLabel.HIGH, emptySet())
        return TdeeEstimate(LocalId("stored-$i"), day(i), TdeeEstimateKind.OBSERVATIONAL, EnergyAmount.ofKilocalories(2_300),
            maturity = TdeeMaturity.HIGH_QUALITY, nutritionQuality = q, weightConfidence = WeightTrendConfidence.HIGH,
            stabilityStatus = status, windowStart = day(i - 27), windowEnd = day(i), algorithmVersion = "a", policyVersion = "tdee-v1",
            inputRevision = i + 100L, evidenceKey = "stored-$i")
    }
    private fun day(offset: Int) = CivilDay.parse(java.time.LocalDate.of(2026, 2, 1).plusDays(offset.toLong()).toString())
    private fun plan(id: String, validFrom: CivilDay = CivilDay.parse("2026-01-01")) = NutritionPlanVersion(LocalId(id), NutritionGoal.LOSS,
        EnergyAmount.ofKilocalories(2_000), null, TargetWeeklyRate.ofGrams(350), validFrom, acceptance = PlanAcceptance(Instant.EPOCH))
}
