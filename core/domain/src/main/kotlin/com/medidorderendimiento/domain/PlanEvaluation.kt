package com.medidorderendimiento.domain

enum class PlanDecision { MAINTAIN, OBSERVE, ADJUST_UP, ADJUST_DOWN, INSUFFICIENT_DATA }
enum class DecisionAuthorization { BLOCKED, MAINTAIN_ONLY, OBSERVE_ONLY, PROPOSAL_ALLOWED }
enum class SafetyStatus { CLEAR, CAUTION, REVIEW_REQUIRED }
enum class EvaluationMode { SHADOW, ADVISORY }

enum class PlanEvaluationReason {
    PLAN_MISSING, PLAN_NOT_CURRENT, DIRECTIONAL_POLICY_UNAVAILABLE, WEIGHT_TREND_MISSING,
    TDEE_MISSING, TDEE_INPUT_INVALID, WEIGHT_QUALITY_INSUFFICIENT, NUTRITION_QUALITY_INSUFFICIENT,
    TDEE_MATURITY_INSUFFICIENT, TDEE_STABILITY_INSUFFICIENT, TDEE_UNSTABLE, SAFETY_CAUTION,
    SAFETY_REVIEW_REQUIRED, WITHIN_TOLERANCE, DIRECTIONAL_THRESHOLD_MET, HYSTERESIS_RETAINED,
    HYSTERESIS_PENDING, HYSTERESIS_CONFIRMED, PLAN_CHANGED, POLICY_CHANGED, DUPLICATE_EVIDENCE,
    COOLDOWN_ACTIVE, SHADOW_NON_OPERATIONAL,
}

data class PlanEvaluatorPolicy(
    val version: String = "plan-evaluator-v1",
    val lossEntryDeviationGramsPerWeek: Long = 200,
    val lossExitDeviationGramsPerWeek: Long = 100,
    val requiredIndependentConfirmations: Int = 2,
    val minimumConfirmationSeparationDays: Long = 2,
    val cooldownDays: Long = 14,
) {
    init {
        require(lossEntryDeviationGramsPerWeek > lossExitDeviationGramsPerWeek)
        require(requiredIndependentConfirmations >= 2 && minimumConfirmationSeparationDays >= 1 && cooldownDays >= 0)
    }
}

data class PlanEvaluation(
    val id: LocalId, val profileId: LocalId, val referenceDay: CivilDay, val planVersionId: LocalId?,
    val evaluationMode: EvaluationMode, val candidateDecision: PlanDecision, val effectiveDecision: PlanDecision,
    val operationalDecision: PlanDecision?, val operational: Boolean, val authorization: DecisionAuthorization,
    val safetyStatus: SafetyStatus, val qualifiedForHysteresis: Boolean, val reasons: Set<PlanEvaluationReason>,
    val windowStart: CivilDay?, val windowEnd: CivilDay?, val tdeeEstimateId: LocalId?, val tdeeReferenceDay: CivilDay?,
    val tdeeRevision: Long?,
    val observedWeeklyRateGrams: Long?, val weightConfidence: WeightTrendConfidence,
    val weightDistinctDays: Int, val weightSpanDays: Long, val weightMaximumGapDays: Long,
    val tdeeMaturity: TdeeMaturity?, val estimatorStabilityStatus: EstimatorStabilityStatus,
    val estimatorStabilityPolicyVersion: String?,
    val nutritionQualityLabel: DataQualityLabel?, val eligibleNutritionDays: Int?, val requiredNutritionDays: Int?,
    val estimatedEnergyPermillion: Int?, val evaluatorPolicyVersion: String, val evidenceKey: String,
    val inputRevision: Long, val revision: Long = 1,
) {
    init {
        require(inputRevision > 0 && revision > 0)
        require(!operational || evaluationMode == EvaluationMode.ADVISORY)
        require(evaluationMode != EvaluationMode.SHADOW || (!operational && operationalDecision == null))
    }
}

data class DecisionStateMemory(
    val profileId: LocalId, val planVersionId: LocalId, val policyVersion: String, val lastProcessedDay: CivilDay,
    val lastEvidenceKey: String, val directionalCandidate: PlanDecision?, val qualifiedConfirmationCount: Int,
    val firstQualifiedDay: CivilDay?, val lastQualifiedDay: CivilDay?, val lastEffectiveDecision: PlanDecision,
    val revision: Long = 1,
) { init { require(qualifiedConfirmationCount >= 0 && revision > 0) } }

data class PlanEvaluatorInput(
    val id: LocalId, val profileId: LocalId, val referenceDay: CivilDay, val plan: NutritionPlanVersion?,
    val weightTrend: WeightTrend, val tdeeEstimate: TdeeEstimate?, val estimatorStability: EstimatorStability,
    val safetyStatus: SafetyStatus, val inputRevision: Long, val evaluationMode: EvaluationMode = EvaluationMode.SHADOW,
)

data class PlanEvaluatorResult(val evaluation: PlanEvaluation, val memory: DecisionStateMemory?)

class PlanEvaluator(private val policy: PlanEvaluatorPolicy = PlanEvaluatorPolicy()) {
    fun evaluate(input: PlanEvaluatorInput, previousMemory: DecisionStateMemory?): PlanEvaluatorResult {
        require(input.evaluationMode == EvaluationMode.SHADOW) { "ADVISORY activation is not available in Phase 3c" }
        val reasons = linkedSetOf<PlanEvaluationReason>()
        val plan = input.plan
        val compatible = previousMemory?.takeIf { plan != null && it.planVersionId == plan.id && it.policyVersion == policy.version }
        if (previousMemory != null && compatible == null) reasons += if (plan != null && previousMemory.planVersionId == plan.id) PlanEvaluationReason.POLICY_CHANGED else PlanEvaluationReason.PLAN_CHANGED
        val candidate = candidate(input, compatible, reasons)
        val evidenceKey = evidenceKey(input, candidate)
        val hard = hardGate(input, candidate, reasons)
        val qualified = hard == null && candidate.isDirectional()
        val memory = if (plan == null) null else updateMemory(input, candidate, evidenceKey, compatible, !qualified, reasons)
        val effective = hard ?: when {
            candidate == PlanDecision.MAINTAIN -> PlanDecision.MAINTAIN
            candidate == PlanDecision.OBSERVE -> PlanDecision.OBSERVE
            candidate == PlanDecision.INSUFFICIENT_DATA -> PlanDecision.INSUFFICIENT_DATA
            (memory?.qualifiedConfirmationCount ?: 0) >= policy.requiredIndependentConfirmations -> candidate
            else -> PlanDecision.OBSERVE
        }
        reasons += PlanEvaluationReason.SHADOW_NON_OPERATIONAL
        val authorization = when {
            hard == PlanDecision.INSUFFICIENT_DATA || input.safetyStatus == SafetyStatus.REVIEW_REQUIRED -> DecisionAuthorization.BLOCKED
            effective == PlanDecision.MAINTAIN -> DecisionAuthorization.MAINTAIN_ONLY
            else -> DecisionAuthorization.OBSERVE_ONLY
        }
        val tdee = input.tdeeEstimate
        val evaluation = PlanEvaluation(input.id, input.profileId, input.referenceDay, plan?.id, EvaluationMode.SHADOW,
            candidate, effective, null, false, authorization, input.safetyStatus, qualified, reasons,
            tdee?.windowStart, tdee?.windowEnd, tdee?.id, tdee?.referenceDay, tdee?.revision, input.weightTrend.weeklyRateGrams,
            input.weightTrend.confidence, input.weightTrend.coverage.distinctDays, input.weightTrend.coverage.spanDays,
            input.weightTrend.coverage.maximumGapDays, tdee?.maturity, input.estimatorStability.status,
            input.estimatorStability.policyVersion,
            tdee?.nutritionQuality?.label, tdee?.nutritionQuality?.eligibleDays, tdee?.nutritionQuality?.requiredDays,
            tdee?.nutritionQuality?.estimatedEnergyPermillion, policy.version, evidenceKey, input.inputRevision)
        return PlanEvaluatorResult(evaluation, memory?.copy(lastEffectiveDecision = effective))
    }

    private fun candidate(input: PlanEvaluatorInput, memory: DecisionStateMemory?, reasons: MutableSet<PlanEvaluationReason>): PlanDecision {
        val plan = input.plan ?: return PlanDecision.INSUFFICIENT_DATA.also { reasons += PlanEvaluationReason.PLAN_MISSING }
        if (input.referenceDay < plan.validFrom || plan.validUntil?.let { input.referenceDay > it } == true) return PlanDecision.INSUFFICIENT_DATA.also { reasons += PlanEvaluationReason.PLAN_NOT_CURRENT }
        val observed = input.weightTrend.weeklyRateGrams ?: return PlanDecision.INSUFFICIENT_DATA.also { reasons += PlanEvaluationReason.WEIGHT_TREND_MISSING }
        if (plan.goal != NutritionGoal.LOSS || plan.targetWeeklyRate == null) return PlanDecision.OBSERVE.also { reasons += PlanEvaluationReason.DIRECTIONAL_POLICY_UNAVAILABLE }
        val deviation = observed + plan.targetWeeklyRate.grams
        if (memory?.qualifiedConfirmationCount ?: 0 >= policy.requiredIndependentConfirmations) {
            if (memory?.directionalCandidate == PlanDecision.ADJUST_DOWN && deviation > policy.lossExitDeviationGramsPerWeek)
                return PlanDecision.ADJUST_DOWN.also { reasons += PlanEvaluationReason.HYSTERESIS_RETAINED }
            if (memory?.directionalCandidate == PlanDecision.ADJUST_UP && deviation < -policy.lossExitDeviationGramsPerWeek)
                return PlanDecision.ADJUST_UP.also { reasons += PlanEvaluationReason.HYSTERESIS_RETAINED }
        }
        return when {
            deviation >= policy.lossEntryDeviationGramsPerWeek -> PlanDecision.ADJUST_DOWN.also { reasons += PlanEvaluationReason.DIRECTIONAL_THRESHOLD_MET }
            deviation <= -policy.lossEntryDeviationGramsPerWeek -> PlanDecision.ADJUST_UP.also { reasons += PlanEvaluationReason.DIRECTIONAL_THRESHOLD_MET }
            kotlin.math.abs(deviation) <= policy.lossExitDeviationGramsPerWeek -> PlanDecision.MAINTAIN.also { reasons += PlanEvaluationReason.WITHIN_TOLERANCE }
            else -> PlanDecision.OBSERVE
        }
    }

    private fun hardGate(input: PlanEvaluatorInput, candidate: PlanDecision, reasons: MutableSet<PlanEvaluationReason>): PlanDecision? {
        if (input.safetyStatus == SafetyStatus.REVIEW_REQUIRED) return PlanDecision.OBSERVE.also { reasons += PlanEvaluationReason.SAFETY_REVIEW_REQUIRED }
        if (input.safetyStatus == SafetyStatus.CAUTION) reasons += PlanEvaluationReason.SAFETY_CAUTION
        if (candidate == PlanDecision.INSUFFICIENT_DATA) return candidate
        val plan = input.plan
        if (plan != null && input.referenceDay.value.toEpochDay() - plan.validFrom.value.toEpochDay() < policy.cooldownDays)
            return PlanDecision.OBSERVE.also { reasons += PlanEvaluationReason.COOLDOWN_ACTIVE }
        if (input.weightTrend.confidence in setOf(WeightTrendConfidence.UNAVAILABLE, WeightTrendConfidence.LOW))
            return PlanDecision.INSUFFICIENT_DATA.also { reasons += PlanEvaluationReason.WEIGHT_QUALITY_INSUFFICIENT }
        val tdee = input.tdeeEstimate
        if (tdee?.centralEnergy == null) return PlanDecision.INSUFFICIENT_DATA.also { reasons += if (tdee?.estimationReasons?.isNotEmpty() == true) PlanEvaluationReason.TDEE_INPUT_INVALID else PlanEvaluationReason.TDEE_MISSING }
        if (tdee.nutritionQuality.label == DataQualityLabel.INSUFFICIENT || tdee.nutritionQuality.label == DataQualityLabel.LOW)
            return PlanDecision.INSUFFICIENT_DATA.also { reasons += PlanEvaluationReason.NUTRITION_QUALITY_INSUFFICIENT }
        if (tdee.maturity in setOf(TdeeMaturity.UNAVAILABLE, TdeeMaturity.PRIOR_ONLY, TdeeMaturity.PROVISIONAL))
            return PlanDecision.OBSERVE.also { reasons += PlanEvaluationReason.TDEE_MATURITY_INSUFFICIENT }
        if (input.estimatorStability.status != EstimatorStabilityStatus.STABLE) return PlanDecision.OBSERVE.also {
            reasons += if (input.estimatorStability.status == EstimatorStabilityStatus.UNSTABLE) PlanEvaluationReason.TDEE_UNSTABLE else PlanEvaluationReason.TDEE_STABILITY_INSUFFICIENT }
        if (input.safetyStatus == SafetyStatus.CAUTION) return PlanDecision.OBSERVE
        return null
    }

    private fun updateMemory(input: PlanEvaluatorInput, candidate: PlanDecision, key: String, previous: DecisionStateMemory?, reset: Boolean, reasons: MutableSet<PlanEvaluationReason>): DecisionStateMemory {
        val planId = requireNotNull(input.plan).id
        if (reset || !candidate.isDirectional()) return DecisionStateMemory(input.profileId, planId, policy.version,
            input.referenceDay, key, null, 0, null, null, PlanDecision.OBSERVE, (previous?.revision ?: 0) + 1)
        if (previous?.lastProcessedDay == input.referenceDay || previous?.lastEvidenceKey == key) { reasons += PlanEvaluationReason.DUPLICATE_EVIDENCE; return requireNotNull(previous) }
        val same = previous?.directionalCandidate == candidate
        val separated = previous?.lastQualifiedDay?.let {
            input.referenceDay.value.toEpochDay() - it.value.toEpochDay() >= policy.minimumConfirmationSeparationDays
        } ?: true
        val count = when {
            same && separated -> previous!!.qualifiedConfirmationCount + 1
            same -> previous!!.qualifiedConfirmationCount
            else -> 1
        }
        reasons += if (count >= policy.requiredIndependentConfirmations) PlanEvaluationReason.HYSTERESIS_CONFIRMED else PlanEvaluationReason.HYSTERESIS_PENDING
        return DecisionStateMemory(input.profileId, planId, policy.version, input.referenceDay, key, candidate, count,
            if (same) previous?.firstQualifiedDay else input.referenceDay,
            if (!same || separated) input.referenceDay else previous?.lastQualifiedDay,
            previous?.lastEffectiveDecision ?: PlanDecision.OBSERVE, (previous?.revision ?: 0) + 1)
    }

    private fun evidenceKey(input: PlanEvaluatorInput, candidate: PlanDecision): String {
        val t = input.tdeeEstimate
        return listOf(input.referenceDay.value, input.plan?.id?.value, input.plan?.goal,
            input.plan?.baseDailyEnergy?.millicalories, input.plan?.targetWeeklyRate?.grams, policy.version,
            input.evaluationMode, candidate, input.weightTrend.weeklyRateGrams, input.weightTrend.confidence,
            input.weightTrend.coverage.distinctDays, input.weightTrend.coverage.spanDays, input.weightTrend.coverage.maximumGapDays,
            t?.evidenceKey, t?.revision, t?.maturity, t?.nutritionQuality?.label, t?.nutritionQuality?.eligibleDays,
            t?.nutritionQuality?.requiredDays, t?.nutritionQuality?.estimatedEnergyPermillion,
            input.estimatorStability.status, input.estimatorStability.policyVersion, input.safetyStatus, input.inputRevision).joinToString("|")
    }
    private fun PlanDecision.isDirectional() = this == PlanDecision.ADJUST_UP || this == PlanDecision.ADJUST_DOWN
}

object DecisionStateMemoryRebuilder {
    fun rebuild(evaluations: List<PlanEvaluation>, policy: PlanEvaluatorPolicy = PlanEvaluatorPolicy()): DecisionStateMemory? {
        val current = evaluations.groupBy { it.referenceDay }.values.map { it.maxBy(PlanEvaluation::revision) }.sortedBy { it.referenceDay }
        var memory: DecisionStateMemory? = null
        current.forEach { e ->
            val plan = e.planVersionId ?: return@forEach
            val qualified = e.qualifiedForHysteresis && e.candidateDecision.isDirectional() && e.evaluatorPolicyVersion == policy.version
            val compatible = memory?.planVersionId == plan && memory?.policyVersion == policy.version
            val duplicate = memory?.lastEvidenceKey == e.evidenceKey
            val separated = memory?.lastQualifiedDay?.let {
                e.referenceDay.value.toEpochDay() - it.value.toEpochDay() >= policy.minimumConfirmationSeparationDays
            } ?: true
            val same = compatible && memory?.directionalCandidate == e.candidateDecision
            val count = when {
                !qualified -> 0
                duplicate && same -> memory!!.qualifiedConfirmationCount
                duplicate -> 0
                same && separated -> memory!!.qualifiedConfirmationCount + 1
                same -> memory!!.qualifiedConfirmationCount
                else -> 1
            }
            val lastQualifiedDay = when {
                !qualified -> null
                duplicate && same -> memory?.lastQualifiedDay
                duplicate -> null
                !same || separated -> e.referenceDay
                else -> memory?.lastQualifiedDay
            }
            val firstQualifiedDay = when {
                !qualified -> null
                same -> memory?.firstQualifiedDay
                else -> e.referenceDay
            }
            memory = DecisionStateMemory(e.profileId, plan, policy.version, e.referenceDay, e.evidenceKey,
                e.candidateDecision.takeIf { qualified }, count, firstQualifiedDay,
                lastQualifiedDay, e.effectiveDecision, (memory?.revision ?: 0) + 1)
        }
        return memory
    }
    private fun PlanDecision.isDirectional() = this == PlanDecision.ADJUST_UP || this == PlanDecision.ADJUST_DOWN
}
