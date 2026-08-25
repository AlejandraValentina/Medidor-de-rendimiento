package com.medidorderendimiento.domain

enum class ShadowValidationStatus { INSUFFICIENT_EVIDENCE, IN_PROGRESS, READY_FOR_HUMAN_REVIEW, BLOCKED_BY_INCONSISTENCY }
enum class ShadowCriterionState { MET, NOT_MET, PENDING, HUMAN_REVIEW_REQUIRED }
enum class ShadowValidationCriterion {
    PERSONAL_WINDOW, WEIGHT_AND_NUTRITION, ESTIMATED_ENERGY, STABLE_TDEE, DIRECTIONAL_CANDIDATES,
    DIRECTIONAL_CONSISTENCY, EVALUATION_CONSISTENCY, REPRODUCIBILITY, REQUIRED_SCENARIOS, HUMAN_REVIEW,
}

data class ShadowValidationPolicy(
    val version: String = "shadow-validation-v1", val targetEvaluableDays: Int = 28,
    val minimumProspectiveDays: Int = 14, val minimumWeightDays: Int = 8,
    val minimumEligibleNutritionPermillion: Int = 850_000, val minimumStableTdeeDates: Int = 7,
) { init { require(listOf(targetEvaluableDays, minimumProspectiveDays, minimumWeightDays, minimumStableTdeeDates).all { it > 0 }); require(minimumEligibleNutritionPermillion in 0..1_000_000) } }

object ShadowInputRevision {
    fun combine(vararg revisions: Long): Long {
        require(revisions.isNotEmpty() && revisions.all { it > 0 })
        return revisions.fold(1L) { result, revision -> Math.addExact(Math.multiplyExact(result, 31L), revision) }
    }
}

data class ShadowValidationCriterionResult(val criterion: ShadowValidationCriterion, val state: ShadowCriterionState, val evidence: List<String>)
data class ShadowScenarioEvidence(
    val outlierVerified: Boolean = false, val incompleteDayVerified: Boolean = false,
    val retrospectiveCorrectionVerified: Boolean = false,
)
data class ShadowValidationInput(
    val currentPlanVersionId: LocalId?, val evaluations: List<PlanEvaluation>,
    val tdeeEstimates: List<TdeeEstimate> = emptyList(), val weights: List<WeightMeasurement> = emptyList(),
    val nutritionDays: List<TdeeNutritionDay> = emptyList(),
    val replayStatus: ShadowReplayStatus = ShadowReplayStatus.INPUT_INCOMPLETE,
    val scenarios: ShadowScenarioEvidence = ShadowScenarioEvidence(),
)
data class ShadowValidationReport(
    val planVersionId: LocalId?, val policyVersion: String, val evaluatorPolicyVersions: Set<String>,
    val stabilityPolicyVersions: Set<String>, val tdeePolicyVersions: Set<String>,
    val windowStart: CivilDay?, val windowEnd: CivilDay?, val evaluableDays: Int, val prospectiveDays: Int,
    val weightDistinctDays: Int, val weightSpanDays: Long, val weightMaximumGapDays: Long,
    val eligibleNutritionDays: Int, val totalWindowDays: Int, val nutritionCoveragePermillion: Int,
    val estimatedEnergyPermillion: Int?, val nutritionQualityLabel: DataQualityLabel?, val stableTdeeDates: Int,
    val adjustUpCandidates: Int, val adjustDownCandidates: Int, val alternations: Int,
    val inconsistencies: List<String>, val criteria: List<ShadowValidationCriterionResult>, val status: ShadowValidationStatus,
)

class ShadowValidationAnalyzer(
    private val policy: ShadowValidationPolicy = ShadowValidationPolicy(), private val tdeePolicy: TdeePolicy = TdeePolicy(),
) {
    fun selectWindow(input: ShadowValidationInput): Pair<CivilDay?, CivilDay?> {
        val selected = selectedEvaluations(input)
        return selected.firstOrNull()?.referenceDay to selected.lastOrNull()?.referenceDay
    }

    fun analyze(input: ShadowValidationInput): ShadowValidationReport {
        val selected = selectedEvaluations(input)
        val start = selected.firstOrNull()?.referenceDay
        val end = selected.lastOrNull()?.referenceDay
        val inWindow: (CivilDay) -> Boolean = { start != null && end != null && it in start..end }
        val totalWindow = if (start == null || end == null) 0 else (end.value.toEpochDay() - start.value.toEpochDay() + 1).toInt()
        val weightDays = input.weights.map { it.civilDay }.filter(inWindow).distinct().sorted()
        val weightSpan = weightDays.spanDays()
        val weightGap = weightDays.zipWithNext().maxOfOrNull { (a, b) -> b.value.toEpochDay() - a.value.toEpochDay() } ?: 0L
        val nutrition = input.nutritionDays.filter { it.planVersionId == input.currentPlanVersionId && inWindow(it.civilDay) }
        val (nutritionQuality, eligibleNutrition) = if (totalWindow > 0)
            NutritionQualityCalculator(tdeePolicy).calculate(nutrition, totalWindow) else null to emptyList()
        val coverage = if (totalWindow == 0) 0 else (eligibleNutrition.size.toLong() * 1_000_000 / totalWindow).toInt()
        val prospective = selected.count { it.prospectiveObserved == true }
        val currentTdee = input.tdeeEstimates.groupBy { it.referenceDay }.values.map { it.maxBy(TdeeEstimate::revision) }
            .filter { inWindow(it.referenceDay) }
        val stableDates = currentTdee.count { it.stabilityStatus == EstimatorStabilityStatus.STABLE }
        val criticalTdee = currentTdee.any { it.stabilityStatus == EstimatorStabilityStatus.UNSTABLE }
        val transitions = analyzeTransitions(selected)
        val directionals = selected.filter { it.candidateDecision.isDirectional() }
        val inconsistencies = selected.flatMap(::inconsistencies) + List(transitions.unjustified) { "UNEXPLAINED_DIRECTIONAL_ALTERNATION" }
        val results = listOf(
            result(ShadowValidationCriterion.PERSONAL_WINDOW,
                if (selected.size >= policy.targetEvaluableDays && prospective >= policy.minimumProspectiveDays) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "${selected.size}/${policy.targetEvaluableDays} evaluable days", "$prospective/${policy.minimumProspectiveDays} prospective days"),
            result(ShadowValidationCriterion.WEIGHT_AND_NUTRITION,
                if (weightDays.size >= policy.minimumWeightDays && coverage >= policy.minimumEligibleNutritionPermillion) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "${weightDays.size}/${policy.minimumWeightDays} weight days", "$coverage/${policy.minimumEligibleNutritionPermillion} eligible nutrition ppm",
                "span=$weightSpan", "maximumGap=$weightGap"),
            result(ShadowValidationCriterion.ESTIMATED_ENERGY, when {
                nutritionQuality == null -> ShadowCriterionState.PENDING
                NutritionQualityReason.ESTIMATION_TOO_HIGH !in nutritionQuality.reasons &&
                    nutritionQuality.estimatedEnergyPermillion <= tdeePolicy.maximumEstimatedEnergyPermillion -> ShadowCriterionState.MET
                else -> ShadowCriterionState.NOT_MET
            }, "estimatedEnergyPermillion=${nutritionQuality?.estimatedEnergyPermillion}",
                "maximum=${tdeePolicy.maximumEstimatedEnergyPermillion}", "quality=${nutritionQuality?.label}",
                "reasons=${nutritionQuality?.reasons.orEmpty().sortedBy { it.name }}"),
            result(ShadowValidationCriterion.STABLE_TDEE,
                if (stableDates >= policy.minimumStableTdeeDates && !criticalTdee) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "$stableDates/${policy.minimumStableTdeeDates} stable TDEE dates", "criticalInstability=$criticalTdee",
                "metrics persisted with tdee estimates; unknown policies are not reinterpreted"),
            result(ShadowValidationCriterion.DIRECTIONAL_CANDIDATES, ShadowCriterionState.HUMAN_REVIEW_REQUIRED,
                "ADJUST_UP=${directionals.count { it.candidateDecision == PlanDecision.ADJUST_UP }}",
                "ADJUST_DOWN=${directionals.count { it.candidateDecision == PlanDecision.ADJUST_DOWN }}", "HIPOTÉTICO — NO OPERATIVO"),
            result(ShadowValidationCriterion.DIRECTIONAL_CONSISTENCY,
                if (transitions.unjustified == 0) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "alternations=${transitions.alternations}", "unjustified=${transitions.unjustified}"),
            result(ShadowValidationCriterion.EVALUATION_CONSISTENCY,
                if (inconsistencies.isEmpty()) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET, *inconsistencies.toTypedArray()),
            result(ShadowValidationCriterion.REPRODUCIBILITY,
                if (input.replayStatus == ShadowReplayStatus.MATCH) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "replay=${input.replayStatus}"),
            result(ShadowValidationCriterion.REQUIRED_SCENARIOS,
                if (input.scenarios.outlierVerified && input.scenarios.incompleteDayVerified && input.scenarios.retrospectiveCorrectionVerified)
                    ShadowCriterionState.MET else ShadowCriterionState.PENDING,
                "outlierVerified=${input.scenarios.outlierVerified}", "incompleteDayVerified=${input.scenarios.incompleteDayVerified}",
                "retrospectiveCorrectionVerified=${input.scenarios.retrospectiveCorrectionVerified}"),
            result(ShadowValidationCriterion.HUMAN_REVIEW, ShadowCriterionState.HUMAN_REVIEW_REQUIRED, "Explicit human review is still required"),
        )
        val technical = results.filter { it.criterion !in setOf(ShadowValidationCriterion.DIRECTIONAL_CANDIDATES, ShadowValidationCriterion.HUMAN_REVIEW) }
        val status = when {
            inconsistencies.isNotEmpty() -> ShadowValidationStatus.BLOCKED_BY_INCONSISTENCY
            selected.isEmpty() -> ShadowValidationStatus.INSUFFICIENT_EVIDENCE
            technical.all { it.state == ShadowCriterionState.MET } -> ShadowValidationStatus.READY_FOR_HUMAN_REVIEW
            else -> ShadowValidationStatus.IN_PROGRESS
        }
        return ShadowValidationReport(input.currentPlanVersionId, policy.version, selected.map { it.evaluatorPolicyVersion }.toSet(),
            selected.mapNotNull { it.estimatorStabilityPolicyVersion }.toSet(), currentTdee.map { it.policyVersion }.toSet(), start, end,
            selected.size, prospective, weightDays.size, weightSpan, weightGap, eligibleNutrition.size, totalWindow, coverage,
            nutritionQuality?.estimatedEnergyPermillion, nutritionQuality?.label, stableDates,
            directionals.count { it.candidateDecision == PlanDecision.ADJUST_UP }, directionals.count { it.candidateDecision == PlanDecision.ADJUST_DOWN },
            transitions.alternations, inconsistencies, results, status)
    }

    private fun selectedEvaluations(input: ShadowValidationInput): List<PlanEvaluation> = input.evaluations
        .filter { it.planVersionId == input.currentPlanVersionId }.groupBy { it.referenceDay }.values
        .map { it.maxBy(PlanEvaluation::revision) }.sortedBy { it.referenceDay }.distinctBy { it.evidenceKey }
        .filter { it.candidateDecision != PlanDecision.INSUFFICIENT_DATA }.takeLast(policy.targetEvaluableDays)

    private data class Transitions(val alternations: Int, val unjustified: Int)
    private fun analyzeTransitions(evaluations: List<PlanEvaluation>): Transitions {
        var previousIndex: Int? = null; var alternations = 0; var unjustified = 0
        evaluations.forEachIndexed { index, current -> if (current.candidateDecision.isDirectional()) {
            previousIndex?.let { oldIndex ->
                val previous = evaluations[oldIndex]
                if (previous.candidateDecision != current.candidateDecision) {
                    alternations++
                    val path = evaluations.subList(oldIndex + 1, index + 1)
                    val newEvidence = current.evidenceKey != previous.evidenceKey && current.inputRevision != previous.inputRevision
                    if (!newEvidence || path.none { it.reasons.any(justifiedTransitionReasons::contains) }) unjustified++
                }
            }
            previousIndex = index
        } }
        return Transitions(alternations, unjustified)
    }
    private fun inconsistencies(e: PlanEvaluation): List<String> = buildList {
        if (e.evaluationMode != EvaluationMode.SHADOW) add("NON_SHADOW_EVALUATION")
        if (e.evaluationMode == EvaluationMode.SHADOW && (e.operational || e.operationalDecision != null)) add("SHADOW_OPERATIONAL")
        if (e.evaluationMode == EvaluationMode.SHADOW && e.authorization == DecisionAuthorization.PROPOSAL_ALLOWED) add("SHADOW_PROPOSAL_ALLOWED")
        if (e.safetyStatus == SafetyStatus.REVIEW_REQUIRED && e.authorization != DecisionAuthorization.BLOCKED) add("REVIEW_REQUIRED_NOT_BLOCKED")
        if (e.estimatorStabilityStatus != EstimatorStabilityStatus.STABLE && e.qualifiedForHysteresis) add("UNSTABLE_QUALIFIED")
        if (e.candidateDecision == PlanDecision.INSUFFICIENT_DATA && e.reasons.isEmpty()) add("INSUFFICIENT_WITHOUT_REASON")
    }
    private fun result(c: ShadowValidationCriterion, s: ShadowCriterionState, vararg evidence: String) = ShadowValidationCriterionResult(c, s, evidence.toList())
    private fun PlanDecision.isDirectional() = this == PlanDecision.ADJUST_UP || this == PlanDecision.ADJUST_DOWN
    private fun List<CivilDay>.spanDays() = if (isEmpty()) 0L else last().value.toEpochDay() - first().value.toEpochDay() + 1
    private companion object { val justifiedTransitionReasons = setOf(PlanEvaluationReason.WITHIN_TOLERANCE,
        PlanEvaluationReason.PLAN_CHANGED, PlanEvaluationReason.POLICY_CHANGED, PlanEvaluationReason.SAFETY_REVIEW_REQUIRED,
        PlanEvaluationReason.SAFETY_CAUTION, PlanEvaluationReason.TDEE_UNSTABLE, PlanEvaluationReason.TDEE_STABILITY_INSUFFICIENT,
        PlanEvaluationReason.DIRECTIONAL_THRESHOLD_MET) }
}

enum class ShadowReplayStatus { MATCH, MISMATCH, INPUT_INCOMPLETE }
enum class ShadowReplayReason { LEGACY_STABILITY_POLICY_UNKNOWN, POLICY_VERSION_MISMATCH, OUTPUT_MISMATCH, MEMORY_MISMATCH }
data class ShadowReplayItem(val input: PlanEvaluatorInput, val expected: PlanEvaluation)
data class ShadowReplayReport(val status: ShadowReplayStatus, val evaluations: List<PlanEvaluation>, val memory: DecisionStateMemory?, val reasons: Set<ShadowReplayReason>)

class ShadowReplayEngine(private val policy: PlanEvaluatorPolicy = PlanEvaluatorPolicy()) {
    fun replay(items: List<ShadowReplayItem>, expectedMemory: DecisionStateMemory? = null): ShadowReplayReport {
        val current = items.groupBy { it.expected.referenceDay }.values.map { it.maxBy { item -> item.expected.revision } }.sortedBy { it.expected.referenceDay }
        if (current.any { it.expected.estimatorStabilityPolicyVersion == null })
            return ShadowReplayReport(ShadowReplayStatus.INPUT_INCOMPLETE, emptyList(), null, setOf(ShadowReplayReason.LEGACY_STABILITY_POLICY_UNKNOWN))
        if (current.any { it.expected.evaluatorPolicyVersion != policy.version || it.input.estimatorStability.policyVersion != it.expected.estimatorStabilityPolicyVersion })
            return ShadowReplayReport(ShadowReplayStatus.INPUT_INCOMPLETE, emptyList(), null, setOf(ShadowReplayReason.POLICY_VERSION_MISMATCH))
        var memory: DecisionStateMemory? = null
        val actual = current.map { item -> PlanEvaluator(policy).evaluate(item.input, memory).also { memory = it.memory }.evaluation }
        val reasons = linkedSetOf<ShadowReplayReason>()
        if (actual.zip(current).any { (a, item) -> !a.sameOutput(item.expected) }) reasons += ShadowReplayReason.OUTPUT_MISMATCH
        if (expectedMemory != null && !memory.sameSemanticMemory(expectedMemory)) reasons += ShadowReplayReason.MEMORY_MISMATCH
        return ShadowReplayReport(if (reasons.isEmpty()) ShadowReplayStatus.MATCH else ShadowReplayStatus.MISMATCH, actual, memory, reasons)
    }
    private fun PlanEvaluation.sameOutput(e: PlanEvaluation) = candidateDecision == e.candidateDecision && effectiveDecision == e.effectiveDecision &&
        authorization == e.authorization && qualifiedForHysteresis == e.qualifiedForHysteresis && reasons == e.reasons && evidenceKey == e.evidenceKey
    private fun DecisionStateMemory?.sameSemanticMemory(other: DecisionStateMemory?) = this == null && other == null || this != null && other != null &&
        planVersionId == other.planVersionId && policyVersion == other.policyVersion && directionalCandidate == other.directionalCandidate &&
        qualifiedConfirmationCount == other.qualifiedConfirmationCount && firstQualifiedDay == other.firstQualifiedDay &&
        lastQualifiedDay == other.lastQualifiedDay && lastEffectiveDecision == other.lastEffectiveDecision && lastEvidenceKey == other.lastEvidenceKey
}
