package com.medidorderendimiento.domain

enum class ShadowValidationStatus { INSUFFICIENT_EVIDENCE, IN_PROGRESS, READY_FOR_HUMAN_REVIEW, BLOCKED_BY_INCONSISTENCY }
enum class ShadowCriterionState { MET, NOT_MET, PENDING, HUMAN_REVIEW_REQUIRED }
enum class ShadowValidationCriterion {
    PERSONAL_WINDOW, WEIGHT_AND_NUTRITION, ESTIMATED_ENERGY, STABLE_TDEE, DIRECTIONAL_CANDIDATES,
    DIRECTIONAL_CONSISTENCY, EVALUATION_CONSISTENCY, REPRODUCIBILITY, REQUIRED_SCENARIOS, HUMAN_REVIEW,
}

data class ShadowValidationPolicy(
    val version: String = "shadow-validation-v1",
    val targetEvaluableDays: Int = 28,
    val minimumProspectiveDays: Int = 14,
    val minimumWeightDays: Int = 8,
    val minimumEligibleNutritionPermillion: Int = 850_000,
    val minimumStableTdeeDates: Int = 7,
) { init { require(listOf(targetEvaluableDays, minimumProspectiveDays, minimumWeightDays, minimumStableTdeeDates).all { it > 0 }); require(minimumEligibleNutritionPermillion in 0..1_000_000) } }

object ShadowInputRevision {
    fun combine(vararg revisions: Long): Long {
        require(revisions.isNotEmpty() && revisions.all { it > 0 })
        return revisions.fold(1L) { result, revision -> Math.addExact(Math.multiplyExact(result, 31L), revision) }
    }
}

data class ShadowValidationCriterionResult(
    val criterion: ShadowValidationCriterion,
    val state: ShadowCriterionState,
    val evidence: List<String>,
)

data class ShadowScenarioEvidence(val outlier: Boolean = false, val incompleteDay: Boolean = false, val retrospectiveCorrection: Boolean = false)

data class ShadowValidationInput(
    val currentPlanVersionId: LocalId?,
    val evaluations: List<PlanEvaluation>,
    val tdeeEstimates: List<TdeeEstimate> = emptyList(),
    val prospectiveDays: Set<CivilDay> = emptySet(),
    val replayStatus: ShadowReplayStatus = ShadowReplayStatus.INPUT_INCOMPLETE,
    val scenarios: ShadowScenarioEvidence = ShadowScenarioEvidence(),
)

data class ShadowValidationReport(
    val planVersionId: LocalId?, val policyVersion: String, val evaluatorPolicyVersions: Set<String>,
    val stabilityPolicyVersions: Set<String>, val tdeePolicyVersions: Set<String>,
    val windowStart: CivilDay?, val windowEnd: CivilDay?,
    val evaluableDays: Int, val prospectiveDays: Int, val weightDistinctDays: Int, val weightSpanDays: Long,
    val weightMaximumGapDays: Long, val eligibleNutritionDays: Int, val totalWindowDays: Int,
    val nutritionCoveragePermillion: Int, val estimatedEnergyPermillion: Int?, val nutritionQualityLabel: DataQualityLabel?,
    val stableTdeeDates: Int, val adjustUpCandidates: Int, val adjustDownCandidates: Int, val alternations: Int,
    val inconsistencies: List<String>, val criteria: List<ShadowValidationCriterionResult>, val status: ShadowValidationStatus,
)

class ShadowValidationAnalyzer(
    private val policy: ShadowValidationPolicy = ShadowValidationPolicy(),
    private val tdeePolicy: TdeePolicy = TdeePolicy(),
) {
    fun analyze(input: ShadowValidationInput): ShadowValidationReport {
        val current = input.evaluations.groupBy { it.referenceDay }.values.map { it.maxBy(PlanEvaluation::revision) }
            .filter { it.planVersionId == input.currentPlanVersionId }.sortedBy { it.referenceDay }
            .distinctBy { it.evidenceKey }
        val evaluable = current.filter { it.candidateDecision != PlanDecision.INSUFFICIENT_DATA }
        val start = evaluable.minOfOrNull { it.referenceDay }
        val end = evaluable.maxOfOrNull { it.referenceDay }
        val prospective = evaluable.map { it.referenceDay }.toSet().intersect(input.prospectiveDays).size
        val latest = evaluable.lastOrNull()
        val totalWindow = if (start == null || end == null) 0 else (end.value.toEpochDay() - start.value.toEpochDay() + 1).toInt()
        val eligible = latest?.eligibleNutritionDays ?: 0
        val coverage = if (totalWindow == 0) 0 else ((eligible.toLong() * 1_000_000L) / totalWindow).coerceAtMost(1_000_000).toInt()
        val stableDates = evaluable.count { it.estimatorStabilityStatus == EstimatorStabilityStatus.STABLE }
        val directionals = evaluable.filter { it.candidateDecision.isDirectional() }
        val alternations = directionals.zipWithNext().count { (a, b) -> a.candidateDecision != b.candidateDecision }
        val unjustified = directionals.zipWithNext().count { (a, b) ->
            a.candidateDecision != b.candidateDecision && b.reasons.none { it in justifiedTransitionReasons }
        }
        val inconsistencies = evaluable.flatMap(::inconsistencies) +
            List(unjustified) { "UNEXPLAINED_DIRECTIONAL_ALTERNATION" }
        val criticalTdee = evaluable.any { it.estimatorStabilityStatus == EstimatorStabilityStatus.UNSTABLE || PlanEvaluationReason.TDEE_UNSTABLE in it.reasons }

        val results = listOf(
            result(ShadowValidationCriterion.PERSONAL_WINDOW,
                if (evaluable.size >= policy.targetEvaluableDays && prospective >= policy.minimumProspectiveDays) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "${evaluable.size}/${policy.targetEvaluableDays} evaluable days", "$prospective/${policy.minimumProspectiveDays} prospective days"),
            result(ShadowValidationCriterion.WEIGHT_AND_NUTRITION,
                if ((latest?.weightDistinctDays ?: 0) >= policy.minimumWeightDays && coverage >= policy.minimumEligibleNutritionPermillion) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "${latest?.weightDistinctDays ?: 0}/${policy.minimumWeightDays} weight days", "$coverage/${policy.minimumEligibleNutritionPermillion} eligible nutrition ppm",
                "span=${latest?.weightSpanDays ?: 0}", "maximumGap=${latest?.weightMaximumGapDays ?: 0}"),
            result(ShadowValidationCriterion.ESTIMATED_ENERGY, when {
                latest?.estimatedEnergyPermillion == null -> ShadowCriterionState.PENDING
                latest.estimatedEnergyPermillion <= tdeePolicy.maximumEstimatedEnergyPermillion -> ShadowCriterionState.MET
                else -> ShadowCriterionState.NOT_MET
            }, "estimatedEnergyPermillion=${latest?.estimatedEnergyPermillion}",
                "maximum=${tdeePolicy.maximumEstimatedEnergyPermillion}", "quality=${latest?.nutritionQualityLabel}"),
            result(ShadowValidationCriterion.STABLE_TDEE,
                if (stableDates >= policy.minimumStableTdeeDates && !criticalTdee) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "$stableDates/${policy.minimumStableTdeeDates} stable TDEE dates", "criticalInstability=$criticalTdee"),
            result(ShadowValidationCriterion.DIRECTIONAL_CANDIDATES, ShadowCriterionState.HUMAN_REVIEW_REQUIRED,
                "ADJUST_UP=${directionals.count { it.candidateDecision == PlanDecision.ADJUST_UP }}",
                "ADJUST_DOWN=${directionals.count { it.candidateDecision == PlanDecision.ADJUST_DOWN }}", "HIPOTÉTICO — NO OPERATIVO"),
            result(ShadowValidationCriterion.DIRECTIONAL_CONSISTENCY,
                if (unjustified == 0) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET, "alternations=$alternations", "unjustified=$unjustified"),
            result(ShadowValidationCriterion.EVALUATION_CONSISTENCY,
                if (inconsistencies.isEmpty()) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET, *inconsistencies.toTypedArray()),
            result(ShadowValidationCriterion.REPRODUCIBILITY,
                if (input.replayStatus == ShadowReplayStatus.MATCH) ShadowCriterionState.MET else ShadowCriterionState.NOT_MET,
                "replay=${input.replayStatus}"),
            result(ShadowValidationCriterion.REQUIRED_SCENARIOS,
                if (input.scenarios.outlier && input.scenarios.incompleteDay && input.scenarios.retrospectiveCorrection) ShadowCriterionState.MET else ShadowCriterionState.PENDING,
                "outlier=${input.scenarios.outlier}", "incompleteDay=${input.scenarios.incompleteDay}", "retrospectiveCorrection=${input.scenarios.retrospectiveCorrection}"),
            result(ShadowValidationCriterion.HUMAN_REVIEW, ShadowCriterionState.HUMAN_REVIEW_REQUIRED, "Explicit human review is still required"),
        )
        val technical = results.filter { it.criterion !in setOf(ShadowValidationCriterion.DIRECTIONAL_CANDIDATES, ShadowValidationCriterion.HUMAN_REVIEW) }
        val status = when {
            inconsistencies.isNotEmpty() -> ShadowValidationStatus.BLOCKED_BY_INCONSISTENCY
            evaluable.isEmpty() -> ShadowValidationStatus.INSUFFICIENT_EVIDENCE
            technical.all { it.state == ShadowCriterionState.MET } -> ShadowValidationStatus.READY_FOR_HUMAN_REVIEW
            else -> ShadowValidationStatus.IN_PROGRESS
        }
        val usedTdeeIds = evaluable.mapNotNull { it.tdeeEstimateId }.toSet()
        return ShadowValidationReport(input.currentPlanVersionId, policy.version,
            evaluable.map { it.evaluatorPolicyVersion }.toSet(), evaluable.mapNotNull { it.estimatorStabilityPolicyVersion }.toSet(),
            input.tdeeEstimates.filter { it.id in usedTdeeIds }.map { it.policyVersion }.toSet(), start, end, evaluable.size, prospective,
            latest?.weightDistinctDays ?: 0, latest?.weightSpanDays ?: 0, latest?.weightMaximumGapDays ?: 0,
            eligible, totalWindow, coverage, latest?.estimatedEnergyPermillion, latest?.nutritionQualityLabel, stableDates,
            directionals.count { it.candidateDecision == PlanDecision.ADJUST_UP },
            directionals.count { it.candidateDecision == PlanDecision.ADJUST_DOWN }, alternations, inconsistencies, results, status)
    }

    private fun result(criterion: ShadowValidationCriterion, state: ShadowCriterionState, vararg evidence: String) =
        ShadowValidationCriterionResult(criterion, state, evidence.toList())
    private fun inconsistencies(e: PlanEvaluation): List<String> = buildList {
        if (e.evaluationMode == EvaluationMode.SHADOW && (e.operational || e.operationalDecision != null)) add("SHADOW_OPERATIONAL")
        if (e.safetyStatus == SafetyStatus.REVIEW_REQUIRED && e.authorization != DecisionAuthorization.BLOCKED) add("REVIEW_REQUIRED_NOT_BLOCKED")
        if (e.estimatorStabilityStatus != EstimatorStabilityStatus.STABLE && e.qualifiedForHysteresis) add("UNSTABLE_QUALIFIED")
        if (e.candidateDecision == PlanDecision.INSUFFICIENT_DATA && e.reasons.isEmpty()) add("INSUFFICIENT_WITHOUT_REASON")
    }
    private fun PlanDecision.isDirectional() = this == PlanDecision.ADJUST_UP || this == PlanDecision.ADJUST_DOWN
    private companion object {
        val justifiedTransitionReasons = setOf(PlanEvaluationReason.PLAN_CHANGED, PlanEvaluationReason.POLICY_CHANGED,
            PlanEvaluationReason.SAFETY_REVIEW_REQUIRED, PlanEvaluationReason.SAFETY_CAUTION,
            PlanEvaluationReason.TDEE_UNSTABLE, PlanEvaluationReason.TDEE_STABILITY_INSUFFICIENT,
            PlanEvaluationReason.WITHIN_TOLERANCE)
    }
}

enum class ShadowReplayStatus { MATCH, MISMATCH, INPUT_INCOMPLETE }
enum class ShadowReplayReason { LEGACY_STABILITY_POLICY_UNKNOWN, POLICY_VERSION_MISMATCH, OUTPUT_MISMATCH }
data class ShadowReplayItem(val input: PlanEvaluatorInput, val expected: PlanEvaluation)
data class ShadowReplayReport(val status: ShadowReplayStatus, val evaluations: List<PlanEvaluation>, val memory: DecisionStateMemory?, val reasons: Set<ShadowReplayReason>)

class ShadowReplayEngine(private val policy: PlanEvaluatorPolicy = PlanEvaluatorPolicy()) {
    fun replay(items: List<ShadowReplayItem>): ShadowReplayReport {
        val current = items.groupBy { it.expected.referenceDay }.values.map { it.maxBy { item -> item.expected.revision } }
            .sortedBy { it.expected.referenceDay }
        if (current.any { it.expected.estimatorStabilityPolicyVersion == null })
            return ShadowReplayReport(ShadowReplayStatus.INPUT_INCOMPLETE, emptyList(), null, setOf(ShadowReplayReason.LEGACY_STABILITY_POLICY_UNKNOWN))
        if (current.any { it.expected.evaluatorPolicyVersion != policy.version || it.input.estimatorStability.policyVersion != it.expected.estimatorStabilityPolicyVersion })
            return ShadowReplayReport(ShadowReplayStatus.INPUT_INCOMPLETE, emptyList(), null, setOf(ShadowReplayReason.POLICY_VERSION_MISMATCH))
        var memory: DecisionStateMemory? = null
        val actual = current.map { item -> PlanEvaluator(policy).evaluate(item.input, memory).also { memory = it.memory }.evaluation }
        val mismatch = actual.zip(current).any { (a, item) ->
            val e = item.expected
            a.candidateDecision != e.candidateDecision || a.effectiveDecision != e.effectiveDecision ||
                a.authorization != e.authorization || a.qualifiedForHysteresis != e.qualifiedForHysteresis ||
                a.reasons != e.reasons || a.evidenceKey != e.evidenceKey
        }
        return ShadowReplayReport(if (mismatch) ShadowReplayStatus.MISMATCH else ShadowReplayStatus.MATCH,
            actual, memory, if (mismatch) setOf(ShadowReplayReason.OUTPUT_MISMATCH) else emptySet())
    }
}
