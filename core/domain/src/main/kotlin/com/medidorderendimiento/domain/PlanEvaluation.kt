package com.medidorderendimiento.domain

enum class PlanDecision { MAINTAIN, OBSERVE, ADJUST_UP, ADJUST_DOWN, INSUFFICIENT_DATA }
enum class DecisionAuthorization { BLOCKED, MAINTAIN_ONLY, OBSERVE_ONLY, PROPOSAL_ALLOWED }
enum class SafetyStatus { CLEAR, CAUTION, REVIEW_REQUIRED }

enum class PlanEvaluationReason {
    PLAN_MISSING, PLAN_NOT_CURRENT, DIRECTIONAL_POLICY_UNAVAILABLE, WEIGHT_TREND_MISSING,
    TDEE_MISSING, TDEE_INPUT_INVALID, WEIGHT_QUALITY_INSUFFICIENT, NUTRITION_QUALITY_INSUFFICIENT,
    TDEE_MATURITY_INSUFFICIENT, TDEE_STABILITY_INSUFFICIENT, TDEE_UNSTABLE, SAFETY_CAUTION,
    SAFETY_REVIEW_REQUIRED, WITHIN_TOLERANCE, DIRECTIONAL_THRESHOLD_MET, HYSTERESIS_PENDING,
    HYSTERESIS_CONFIRMED, PLAN_CHANGED, DUPLICATE_EVIDENCE,
}

data class PlanEvaluatorPolicy(
    val version: String = "plan-evaluator-v1",
    val lossEntryDeviationGramsPerWeek: Long = 200,
    val lossExitDeviationGramsPerWeek: Long = 100,
    val requiredIndependentConfirmations: Int = 2,
    val minimumConfirmationSeparationDays: Long = 2,
) {
    init {
        require(lossEntryDeviationGramsPerWeek > lossExitDeviationGramsPerWeek)
        require(requiredIndependentConfirmations >= 2 && minimumConfirmationSeparationDays >= 1)
    }
}

data class PlanEvaluation(
    val id: LocalId,
    val profileId: LocalId,
    val referenceDay: CivilDay,
    val planVersionId: LocalId?,
    val candidateDecision: PlanDecision,
    val effectiveDecision: PlanDecision,
    val operationalDecision: PlanDecision,
    val authorization: DecisionAuthorization,
    val safetyStatus: SafetyStatus,
    val reasons: Set<PlanEvaluationReason>,
    val tdeeEstimateId: LocalId?,
    val observedWeeklyRateGrams: Long?,
    val evaluatorPolicyVersion: String,
    val evidenceKey: String,
    val inputRevision: Long,
    val revision: Long = 1,
) {
    init { require(inputRevision > 0 && revision > 0) }
}

data class DecisionStateMemory(
    val profileId: LocalId,
    val planVersionId: LocalId,
    val policyVersion: String,
    val lastProcessedDay: CivilDay,
    val lastEvidenceKey: String,
    val directionalCandidate: PlanDecision?,
    val qualifiedConfirmationCount: Int,
    val firstQualifiedDay: CivilDay?,
    val lastEffectiveDecision: PlanDecision,
    val revision: Long = 1,
) {
    init { require(qualifiedConfirmationCount >= 0 && revision > 0) }
}

data class PlanEvaluatorInput(
    val id: LocalId,
    val profileId: LocalId,
    val referenceDay: CivilDay,
    val plan: NutritionPlanVersion?,
    val weightTrend: WeightTrend,
    val tdeeEstimate: TdeeEstimate?,
    val estimatorStability: EstimatorStability,
    val safetyStatus: SafetyStatus,
    val inputRevision: Long,
)

data class PlanEvaluatorResult(val evaluation: PlanEvaluation, val memory: DecisionStateMemory?)

class PlanEvaluator(private val policy: PlanEvaluatorPolicy = PlanEvaluatorPolicy()) {
    fun evaluate(input: PlanEvaluatorInput, previousMemory: DecisionStateMemory?): PlanEvaluatorResult {
        val reasons = linkedSetOf<PlanEvaluationReason>()
        val plan = input.plan
        val candidate = candidate(input, reasons)
        val evidenceKey = evidenceKey(input, candidate)
        val hard = hardGate(input, candidate, reasons)
        val memoryBase = previousMemory?.takeIf { plan != null && it.planVersionId == plan.id && it.policyVersion == policy.version }
        if (previousMemory != null && memoryBase == null) reasons += PlanEvaluationReason.PLAN_CHANGED
        val memoryResult = if (plan == null) null else updateMemory(input, candidate, evidenceKey, memoryBase, hard != null, reasons)
        val effective = hard ?: when {
            candidate == PlanDecision.MAINTAIN -> PlanDecision.MAINTAIN
            candidate == PlanDecision.OBSERVE -> PlanDecision.OBSERVE
            candidate == PlanDecision.INSUFFICIENT_DATA -> PlanDecision.INSUFFICIENT_DATA
            memoryResult?.qualifiedConfirmationCount ?: 0 >= policy.requiredIndependentConfirmations -> candidate
            else -> PlanDecision.OBSERVE
        }
        val authorization = when {
            input.safetyStatus == SafetyStatus.REVIEW_REQUIRED || effective == PlanDecision.INSUFFICIENT_DATA -> DecisionAuthorization.BLOCKED
            effective == PlanDecision.MAINTAIN -> DecisionAuthorization.MAINTAIN_ONLY
            effective == PlanDecision.ADJUST_UP || effective == PlanDecision.ADJUST_DOWN -> DecisionAuthorization.PROPOSAL_ALLOWED
            else -> DecisionAuthorization.OBSERVE_ONLY
        }
        val evaluation = PlanEvaluation(input.id, input.profileId, input.referenceDay, plan?.id, candidate, effective,
            effective, authorization, input.safetyStatus, reasons, input.tdeeEstimate?.id,
            input.weightTrend.weeklyRateGrams, policy.version, evidenceKey, input.inputRevision)
        return PlanEvaluatorResult(evaluation, memoryResult?.copy(lastEffectiveDecision = effective))
    }

    private fun candidate(input: PlanEvaluatorInput, reasons: MutableSet<PlanEvaluationReason>): PlanDecision {
        val plan = input.plan ?: return PlanDecision.INSUFFICIENT_DATA.also { reasons += PlanEvaluationReason.PLAN_MISSING }
        if (input.referenceDay < plan.validFrom || plan.validUntil?.let { input.referenceDay > it } == true) {
            reasons += PlanEvaluationReason.PLAN_NOT_CURRENT; return PlanDecision.INSUFFICIENT_DATA
        }
        val observed = input.weightTrend.weeklyRateGrams ?: return PlanDecision.INSUFFICIENT_DATA.also { reasons += PlanEvaluationReason.WEIGHT_TREND_MISSING }
        if (plan.goal != NutritionGoal.LOSS || plan.targetWeeklyRate == null) {
            reasons += PlanEvaluationReason.DIRECTIONAL_POLICY_UNAVAILABLE; return PlanDecision.OBSERVE
        }
        val target = -plan.targetWeeklyRate.grams
        val deviation = observed - target
        return when {
            deviation >= policy.lossEntryDeviationGramsPerWeek -> PlanDecision.ADJUST_DOWN.also { reasons += PlanEvaluationReason.DIRECTIONAL_THRESHOLD_MET }
            deviation <= -policy.lossEntryDeviationGramsPerWeek -> PlanDecision.ADJUST_UP.also { reasons += PlanEvaluationReason.DIRECTIONAL_THRESHOLD_MET }
            kotlin.math.abs(deviation) <= policy.lossExitDeviationGramsPerWeek -> PlanDecision.MAINTAIN.also { reasons += PlanEvaluationReason.WITHIN_TOLERANCE }
            else -> PlanDecision.OBSERVE
        }
    }

    private fun hardGate(input: PlanEvaluatorInput, candidate: PlanDecision, reasons: MutableSet<PlanEvaluationReason>): PlanDecision? {
        if (input.safetyStatus == SafetyStatus.REVIEW_REQUIRED) {
            reasons += PlanEvaluationReason.SAFETY_REVIEW_REQUIRED; return PlanDecision.OBSERVE
        }
        if (input.safetyStatus == SafetyStatus.CAUTION) reasons += PlanEvaluationReason.SAFETY_CAUTION
        if (candidate == PlanDecision.INSUFFICIENT_DATA) return PlanDecision.INSUFFICIENT_DATA
        if (input.weightTrend.confidence == WeightTrendConfidence.UNAVAILABLE) {
            reasons += PlanEvaluationReason.WEIGHT_QUALITY_INSUFFICIENT; return PlanDecision.INSUFFICIENT_DATA
        }
        val tdee = input.tdeeEstimate
        if (tdee?.centralEnergy == null) {
            reasons += if (tdee?.estimationReasons?.isNotEmpty() == true) PlanEvaluationReason.TDEE_INPUT_INVALID else PlanEvaluationReason.TDEE_MISSING
            return PlanDecision.INSUFFICIENT_DATA
        }
        if (tdee.nutritionQuality.label == DataQualityLabel.INSUFFICIENT || tdee.nutritionQuality.label == DataQualityLabel.LOW) {
            reasons += PlanEvaluationReason.NUTRITION_QUALITY_INSUFFICIENT; return PlanDecision.INSUFFICIENT_DATA
        }
        if (tdee.maturity == TdeeMaturity.UNAVAILABLE || tdee.maturity == TdeeMaturity.PRIOR_ONLY || tdee.maturity == TdeeMaturity.PROVISIONAL) {
            reasons += PlanEvaluationReason.TDEE_MATURITY_INSUFFICIENT; return PlanDecision.OBSERVE
        }
        if (input.estimatorStability.status != EstimatorStabilityStatus.STABLE) {
            reasons += if (input.estimatorStability.status == EstimatorStabilityStatus.UNSTABLE) PlanEvaluationReason.TDEE_UNSTABLE else PlanEvaluationReason.TDEE_STABILITY_INSUFFICIENT
            return PlanDecision.OBSERVE
        }
        if (input.safetyStatus == SafetyStatus.CAUTION) return PlanDecision.OBSERVE
        return null
    }

    private fun updateMemory(input: PlanEvaluatorInput, candidate: PlanDecision, evidenceKey: String,
        previous: DecisionStateMemory?, reset: Boolean, reasons: MutableSet<PlanEvaluationReason>): DecisionStateMemory {
        val planId = requireNotNull(input.plan).id
        val directional = candidate.takeIf { it == PlanDecision.ADJUST_UP || it == PlanDecision.ADJUST_DOWN }
        if (reset || directional == null) return DecisionStateMemory(input.profileId, planId, policy.version, input.referenceDay,
            evidenceKey, null, 0, null, PlanDecision.OBSERVE, (previous?.revision ?: 0) + 1)
        val duplicate = previous?.lastProcessedDay == input.referenceDay || previous?.lastEvidenceKey == evidenceKey
        if (duplicate) {
            reasons += PlanEvaluationReason.DUPLICATE_EVIDENCE; return requireNotNull(previous)
        }
        val sameDirection = previous?.directionalCandidate == directional
        val separated = previous?.lastProcessedDay?.let { input.referenceDay.value.toEpochDay() - it.value.toEpochDay() >= policy.minimumConfirmationSeparationDays } ?: true
        val count = if (sameDirection && separated) previous!!.qualifiedConfirmationCount + 1 else 1
        if (count >= policy.requiredIndependentConfirmations) reasons += PlanEvaluationReason.HYSTERESIS_CONFIRMED else reasons += PlanEvaluationReason.HYSTERESIS_PENDING
        return DecisionStateMemory(input.profileId, planId, policy.version, input.referenceDay, evidenceKey, directional,
            count, if (sameDirection) previous?.firstQualifiedDay else input.referenceDay, previous?.lastEffectiveDecision ?: PlanDecision.OBSERVE,
            (previous?.revision ?: 0) + 1)
    }

    private fun evidenceKey(input: PlanEvaluatorInput, candidate: PlanDecision): String = listOf(
        input.referenceDay.value, policy.version, input.plan?.id?.value, candidate, input.weightTrend.weeklyRateGrams,
        input.weightTrend.confidence, input.weightTrend.coverage, input.tdeeEstimate?.id?.value,
        input.tdeeEstimate?.revision, input.tdeeEstimate?.maturity, input.tdeeEstimate?.nutritionQuality?.label,
        input.estimatorStability.status, input.safetyStatus, input.inputRevision,
    ).joinToString("|")
}

object DecisionStateMemoryRebuilder {
    fun rebuild(evaluations: List<PlanEvaluation>, policy: PlanEvaluatorPolicy = PlanEvaluatorPolicy()): DecisionStateMemory? {
        val current = evaluations.groupBy { it.referenceDay }.values.map { it.maxBy(PlanEvaluation::revision) }
            .sortedBy { it.referenceDay }
        var memory: DecisionStateMemory? = null
        current.forEach { evaluation ->
            val planId = evaluation.planVersionId ?: return@forEach
            val directional = evaluation.candidateDecision.takeIf { it == PlanDecision.ADJUST_UP || it == PlanDecision.ADJUST_DOWN }
            val reset = evaluation.safetyStatus == SafetyStatus.REVIEW_REQUIRED || directional == null || memory?.planVersionId != planId
            val duplicate = memory?.lastEvidenceKey == evaluation.evidenceKey
            val separated = memory?.lastProcessedDay?.let { evaluation.referenceDay.value.toEpochDay() - it.value.toEpochDay() >= policy.minimumConfirmationSeparationDays } ?: true
            val count = if (!reset && !duplicate && separated && memory?.directionalCandidate == directional) memory!!.qualifiedConfirmationCount + 1 else if (reset) 0 else 1
            memory = DecisionStateMemory(evaluation.profileId, planId, policy.version, evaluation.referenceDay,
                evaluation.evidenceKey, if (reset) null else directional, count, if (count == 1) evaluation.referenceDay else memory?.firstQualifiedDay,
                evaluation.effectiveDecision, (memory?.revision ?: 0) + 1)
        }
        return memory
    }
}
