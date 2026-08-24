package com.medidorderendimiento.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

enum class TdeeDiaryState { OPEN, CLOSED_CONFIRMED, CLOSED_WITH_ESTIMATES, CLOSED_INCOMPLETE, EXCLUDED_CONTEXT, ZERO_INTAKE_CONFIRMED }
enum class DataQualityLabel { INSUFFICIENT, LOW, MODERATE, HIGH }
enum class TdeeMaturity { UNAVAILABLE, PRIOR_ONLY, PROVISIONAL, ADAPTIVE, HIGH_QUALITY }
enum class TdeeEstimateKind { USER_PROVIDED, POPULATION_PRIOR, WEARABLE_CONTEXT, OBSERVATIONAL, BLENDED }
enum class EstimatorStabilityStatus { INSUFFICIENT_HISTORY, UNSTABLE, STABILIZING, STABLE }
enum class TdeeEstimationReason { NON_POSITIVE_OBSERVATIONAL_RESULT }

enum class NutritionQualityReason {
    INSUFFICIENT_ELIGIBLE_DAYS, OPEN_OR_INCOMPLETE_DAYS, EXCLUDED_CONTEXT_DAYS,
    ESTIMATION_TOO_HIGH, PENDING_ENTRIES, UNKNOWN_ENERGY, ZERO_INTAKE_REQUIRES_REVIEW, MIXED_PLAN_VERSIONS,
}

data class TdeeNutritionDay(
    val civilDay: CivilDay,
    val state: TdeeDiaryState,
    val confirmedEnergy: EnergyAmount?,
    val estimatedEnergy: EnergyAmount? = null,
    val pendingEntries: Int = 0,
    val unknownEnergyEntries: Int = 0,
    val planVersionId: LocalId? = null,
    val sourceRevision: Long = 1,
) {
    init { require(pendingEntries >= 0 && unknownEnergyEntries >= 0 && sourceRevision > 0) }
    val actualEnergy: EnergyAmount?
        get() {
            val known = listOfNotNull(confirmedEnergy, estimatedEnergy)
            return known.takeIf { it.isNotEmpty() }?.reduce(EnergyAmount::plus)
        }
}

data class NutritionQuality(
    val requiredDays: Int,
    val candidateDays: Int,
    val eligibleDays: Int,
    val estimatedEnergyPermillion: Int,
    val excludedDays: Int,
    val pendingEntries: Int,
    val unknownEnergyEntries: Int,
    val indexPermillion: Int,
    val label: DataQualityLabel,
    val reasons: Set<NutritionQualityReason>,
)

data class TdeePolicy(
    val version: String = "tdee-v1",
    val algorithmVersion: String = "theil-sen-energy-balance-v1",
    val energyCoefficientKcalPerKg: Long = 7_700,
    val maximumEstimatedEnergyPermillion: Int = 350_000,
    val estimatedEnergyPenaltyPermillion: Int = 500_000,
    val provisionalMinimumDays: Int = 14,
    val adaptiveMinimumDays: Int = 21,
    val highQualityMinimumDays: Int = 28,
) {
    init {
        require(energyCoefficientKcalPerKg > 0)
        require(maximumEstimatedEnergyPermillion in 0..1_000_000)
        require(estimatedEnergyPenaltyPermillion in 0..1_000_000)
    }
}

object TdeeEvidenceKey {
    fun build(
        referenceDay: CivilDay,
        policy: TdeePolicy,
        weightTrend: WeightTrend,
        nutritionDays: List<TdeeNutritionDay>,
    ): String = buildString {
        append("reference=").append(referenceDay.value)
        append("|algorithm=").append(policy.algorithmVersion)
        append("|policy=").append(policy.version)
        append("|weight=").append(weightTrend.weeklyRateGrams)
            .append(':').append(weightTrend.confidence)
            .append(':').append(weightTrend.coverage.distinctDays)
            .append(':').append(weightTrend.coverage.spanDays)
            .append(':').append(weightTrend.coverage.maximumGapDays)
            .append(':').append(weightTrend.variabilityGrams)
        append("|weightReasons=").append(weightTrend.reasons.map { it.name }.sorted().joinToString(","))
        append("|outliers=").append(weightTrend.possibleOutliers.map { it.measurement.id.value }.sorted().joinToString(","))
        nutritionDays.sortedBy { it.civilDay }.forEach { day ->
            append("|day=").append(day.civilDay.value)
                .append(':').append(day.state)
                .append(':').append(day.confirmedEnergy?.millicalories)
                .append(':').append(day.estimatedEnergy?.millicalories)
                .append(':').append(day.pendingEntries)
                .append(':').append(day.unknownEnergyEntries)
                .append(':').append(day.planVersionId?.value)
                .append(':').append(day.sourceRevision)
        }
    }
}

data class TdeeEstimate(
    val id: LocalId,
    val referenceDay: CivilDay,
    val kind: TdeeEstimateKind,
    val centralEnergy: EnergyAmount?,
    val lowEnergy: EnergyAmount? = null,
    val highEnergy: EnergyAmount? = null,
    val maturity: TdeeMaturity,
    val nutritionQuality: NutritionQuality,
    val weightConfidence: WeightTrendConfidence,
    val stabilityStatus: EstimatorStabilityStatus = EstimatorStabilityStatus.INSUFFICIENT_HISTORY,
    val windowStart: CivilDay,
    val windowEnd: CivilDay,
    val algorithmVersion: String,
    val policyVersion: String,
    val inputRevision: Long,
    val evidenceKey: String,
    val estimationReasons: Set<TdeeEstimationReason> = emptySet(),
    val revision: Long = 1,
) {
    init { require(inputRevision > 0 && revision > 0) }
}

class NutritionQualityCalculator(private val policy: TdeePolicy = TdeePolicy()) {
    fun calculate(days: List<TdeeNutritionDay>, requiredDays: Int): Pair<NutritionQuality, List<TdeeNutritionDay>> {
        require(requiredDays > 0)
        val reasons = linkedSetOf<NutritionQualityReason>()
        val candidate = days.distinctBy { it.civilDay }.sortedBy { it.civilDay }
        val latestPlan = candidate.lastOrNull { it.planVersionId != null }?.planVersionId
        val planSegment = if (candidate.mapNotNull { it.planVersionId }.distinct().size > 1) {
            reasons += NutritionQualityReason.MIXED_PLAN_VERSIONS
            candidate.filter { it.planVersionId == latestPlan }
        } else candidate
        val eligible = planSegment.filter { day ->
            when (day.state) {
                TdeeDiaryState.CLOSED_CONFIRMED -> day.actualEnergy != null && day.pendingEntries == 0 && day.unknownEnergyEntries == 0
                TdeeDiaryState.CLOSED_WITH_ESTIMATES -> day.actualEnergy != null && day.pendingEntries == 0 &&
                    day.unknownEnergyEntries == 0 && estimatedRatio(day) <= policy.maximumEstimatedEnergyPermillion
                else -> false
            }
        }
        val estimated = eligible.fold(0L) { sum, day -> Math.addExact(sum, day.estimatedEnergy?.millicalories ?: 0L) }
        val total = eligible.fold(0L) { sum, day -> Math.addExact(sum, day.actualEnergy?.millicalories ?: 0L) }
        val estimatedRatio = if (total == 0L) 0 else ratio(estimated, total)
        val excluded = candidate.count { it.state == TdeeDiaryState.EXCLUDED_CONTEXT }
        val pending = candidate.sumOf { it.pendingEntries }
        val unknown = candidate.sumOf { it.unknownEnergyEntries }
        if (eligible.size < requiredDays) reasons += NutritionQualityReason.INSUFFICIENT_ELIGIBLE_DAYS
        if (candidate.any { it.state == TdeeDiaryState.OPEN || it.state == TdeeDiaryState.CLOSED_INCOMPLETE }) reasons += NutritionQualityReason.OPEN_OR_INCOMPLETE_DAYS
        if (excluded > 0) reasons += NutritionQualityReason.EXCLUDED_CONTEXT_DAYS
        if (candidate.any { it.state == TdeeDiaryState.CLOSED_WITH_ESTIMATES && estimatedRatio(it) > policy.maximumEstimatedEnergyPermillion }) reasons += NutritionQualityReason.ESTIMATION_TOO_HIGH
        if (pending > 0) reasons += NutritionQualityReason.PENDING_ENTRIES
        if (unknown > 0) reasons += NutritionQualityReason.UNKNOWN_ENERGY
        if (candidate.any { it.state == TdeeDiaryState.ZERO_INTAKE_CONFIRMED }) reasons += NutritionQualityReason.ZERO_INTAKE_REQUIRES_REVIEW
        val closureFactor = (eligible.size.toLong() * 1_000_000 / requiredDays).coerceAtMost(1_000_000).toInt()
        val estimationPenalty = estimatedRatio.toLong() * policy.estimatedEnergyPenaltyPermillion / 1_000_000
        val estimationFactor = 1_000_000 - estimationPenalty.toInt()
        val pendingFactor = if (pending == 0) 1_000_000 else 0
        val consistencyFactor = if (unknown == 0) 1_000_000 else 0
        val index = minOf(closureFactor, estimationFactor, pendingFactor, consistencyFactor)
        val label = when {
            index < 400_000 -> DataQualityLabel.INSUFFICIENT
            index < 600_000 -> DataQualityLabel.LOW
            index < 750_000 -> DataQualityLabel.MODERATE
            else -> DataQualityLabel.HIGH
        }
        return NutritionQuality(requiredDays, candidate.size, eligible.size, estimatedRatio, excluded, pending, unknown,
            index, label, reasons) to eligible
    }

    private fun estimatedRatio(day: TdeeNutritionDay): Int {
        val total = day.actualEnergy?.millicalories ?: return 1_000_000
        return if (total == 0L) 0 else ratio(day.estimatedEnergy?.millicalories ?: 0, total)
    }

    private fun ratio(numerator: Long, denominator: Long): Int = BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(1_000_000)).divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP).intValueExact()
}

class TdeeEstimator(private val policy: TdeePolicy = TdeePolicy()) {
    private val qualityCalculator = NutritionQualityCalculator(policy)

    fun estimate(
        id: LocalId,
        referenceDay: CivilDay,
        windowStart: CivilDay,
        nutritionDays: List<TdeeNutritionDay>,
        weightTrend: WeightTrend,
        inputRevision: Long,
        evidenceKey: String,
    ): TdeeEstimate {
        val inWindow = nutritionDays.filter { it.civilDay in windowStart..referenceDay }
        val span = referenceDay.value.toEpochDay() - windowStart.value.toEpochDay() + 1
        val required = when {
            span >= 28 -> policy.highQualityMinimumDays
            span >= 21 -> policy.adaptiveMinimumDays
            else -> policy.provisionalMinimumDays
        }
        val (quality, eligible) = qualityCalculator.calculate(inWindow, required)
        val canObserve = weightTrend.weeklyRateGrams != null && weightTrend.confidence != WeightTrendConfidence.UNAVAILABLE &&
            eligible.size >= policy.provisionalMinimumDays
        val calculated = if (canObserve) calculateCentral(eligible, requireNotNull(weightTrend.weeklyRateGrams)) else null
        val invalidResult = calculated != null && calculated <= 0
        val central = calculated?.takeIf { it > 0 }?.let(EnergyAmount::ofMillicalories)
        val maturity = when {
            central == null -> TdeeMaturity.UNAVAILABLE
            span >= 28 && eligible.size >= policy.highQualityMinimumDays && quality.label == DataQualityLabel.HIGH && weightTrend.confidence == WeightTrendConfidence.HIGH -> TdeeMaturity.HIGH_QUALITY
            span >= 21 && eligible.size >= policy.adaptiveMinimumDays -> TdeeMaturity.ADAPTIVE
            else -> TdeeMaturity.PROVISIONAL
        }
        return TdeeEstimate(id, referenceDay, TdeeEstimateKind.OBSERVATIONAL,
            central, maturity = maturity, nutritionQuality = quality, weightConfidence = weightTrend.confidence,
            windowStart = windowStart, windowEnd = referenceDay, algorithmVersion = policy.algorithmVersion,
            policyVersion = policy.version, inputRevision = inputRevision, evidenceKey = evidenceKey,
            estimationReasons = if (invalidResult) setOf(TdeeEstimationReason.NON_POSITIVE_OBSERVATIONAL_RESULT) else emptySet())
    }

    private fun calculateCentral(days: List<TdeeNutritionDay>, weeklyRateGrams: Long): Long {
        val meanMillicalories = days.map { requireNotNull(it.actualEnergy).millicalories }.averageBigDecimal()
        val adjustmentKcal = BigDecimal.valueOf(policy.energyCoefficientKcalPerKg)
            .multiply(BigDecimal.valueOf(weeklyRateGrams)).divide(BigDecimal.valueOf(7_000), 12, RoundingMode.HALF_UP)
        val resultMillicalories = meanMillicalories.subtract(adjustmentKcal.multiply(BigDecimal.valueOf(1_000)))
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
        return resultMillicalories
    }

    private fun List<Long>.averageBigDecimal(): BigDecimal = fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal.valueOf(value) }
        .divide(BigDecimal.valueOf(size.toLong()), 12, RoundingMode.HALF_UP)
}

data class StabilityPolicy(
    val version: String = "stability-v1",
    val minimumObservationDays: Int = 7,
    val stableHorizonDays: Long = 14,
    val stableDistinctEstimateDays: Int = 10,
    val maximumRelativeMadPermillion: Int = 25_000,
    val maximumPeakToPeakPermillion: Int = 50_000,
    val maximumConsecutivePeriodDriftPermillion: Int = 40_000,
    val alternatingInversionThresholdPermillion: Int = 35_000,
    val earlyUnstablePeakToPeakPermillion: Int = 60_000,
)

enum class StabilityReason { INSUFFICIENT_DISTINCT_DAYS, INSUFFICIENT_HORIZON, EXCESSIVE_MAD, EXCESSIVE_AMPLITUDE, EXCESSIVE_DRIFT, ALTERNATING_INVERSION, DUPLICATE_EVIDENCE, INPUTS_REVISED }

data class EstimatorStability(
    val status: EstimatorStabilityStatus,
    val distinctEstimateDays: Int,
    val horizonDays: Long,
    val medianEnergy: EnergyAmount?,
    val madPermillion: Int?,
    val peakToPeakPermillion: Int?,
    val consecutivePeriodDriftPermillion: Int?,
    val alternatingInversions: Int,
    val reasons: Set<StabilityReason>,
    val policyVersion: String,
)

class EstimatorStabilityCalculator(private val policy: StabilityPolicy = StabilityPolicy()) {
    fun calculate(estimates: List<TdeeEstimate>): EstimatorStability {
        val latestPerDay = estimates.groupBy { it.referenceDay }.values.map { revisions -> revisions.maxBy { it.revision } }
        val observational = latestPerDay.filter { it.centralEnergy != null && it.kind == TdeeEstimateKind.OBSERVATIONAL }.sortedBy { it.referenceDay }
        val independent = observational.distinctBy { it.evidenceKey }
        val reasons = linkedSetOf<StabilityReason>()
        if (independent.size < observational.size) reasons += StabilityReason.DUPLICATE_EVIDENCE
        if (latestPerDay.any { it.revision > 1 }) reasons += StabilityReason.INPUTS_REVISED
        val values = independent.map { requireNotNull(it.centralEnergy).millicalories }
        val horizon = if (independent.size < 2) 0 else independent.last().referenceDay.value.toEpochDay() - independent.first().referenceDay.value.toEpochDay() + 1
        if (values.isEmpty()) return EstimatorStability(EstimatorStabilityStatus.INSUFFICIENT_HISTORY, 0, 0, null, null, null, null, 0, reasons + StabilityReason.INSUFFICIENT_DISTINCT_DAYS, policy.version)
        val median = median(values)
        val mad = median(values.map { abs(it - median) })
        val madRatio = ratio(mad, median)
        val peak = ratio(values.max() - values.min(), median)
        val lastDay = independent.last().referenceDay.value
        val recent = independent.filter { it.referenceDay.value in lastDay.minusDays(6)..lastDay }
            .map { requireNotNull(it.centralEnergy).millicalories }
        val previous = independent.filter { it.referenceDay.value in lastDay.minusDays(13)..lastDay.minusDays(7) }
            .map { requireNotNull(it.centralEnergy).millicalories }
        val drift = if (recent.size < 2 || previous.size < 2) null else ratio(abs(median(previous) - median(recent)), median)
        val inversions = values.zipWithNext { a, b -> b - a }.zipWithNext().count { (a, b) ->
            a.sign != b.sign && maxOf(abs(a), abs(b)) * 1_000_000L / median >= policy.alternatingInversionThresholdPermillion
        }
        if (values.size < policy.minimumObservationDays) reasons += StabilityReason.INSUFFICIENT_DISTINCT_DAYS
        if (horizon < policy.stableHorizonDays) reasons += StabilityReason.INSUFFICIENT_HORIZON
        if (madRatio > policy.maximumRelativeMadPermillion) reasons += StabilityReason.EXCESSIVE_MAD
        if (peak > policy.maximumPeakToPeakPermillion) reasons += StabilityReason.EXCESSIVE_AMPLITUDE
        if (drift != null && drift > policy.maximumConsecutivePeriodDriftPermillion) reasons += StabilityReason.EXCESSIVE_DRIFT
        if (inversions >= 2) reasons += StabilityReason.ALTERNATING_INVERSION
        val critical = peak > policy.earlyUnstablePeakToPeakPermillion || StabilityReason.EXCESSIVE_DRIFT in reasons || StabilityReason.ALTERNATING_INVERSION in reasons
        val stable = independent.size >= policy.stableDistinctEstimateDays && horizon >= policy.stableHorizonDays &&
            madRatio <= policy.maximumRelativeMadPermillion && peak <= policy.maximumPeakToPeakPermillion &&
            drift != null && drift <= policy.maximumConsecutivePeriodDriftPermillion && inversions < 2
        val status = when {
            critical -> EstimatorStabilityStatus.UNSTABLE
            stable -> EstimatorStabilityStatus.STABLE
            independent.size < policy.minimumObservationDays -> EstimatorStabilityStatus.INSUFFICIENT_HISTORY
            else -> EstimatorStabilityStatus.STABILIZING
        }
        return EstimatorStability(status, independent.size, horizon, EnergyAmount.ofMillicalories(median), madRatio, peak, drift, inversions, reasons, policy.version)
    }

    private val Long.sign: Int get() = compareTo(0)
    private fun median(values: List<Long>): Long {
        val sorted = values.sorted(); val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else Math.addExact(sorted[middle - 1], sorted[middle]) / 2
    }
    private fun ratio(numerator: Long, denominator: Long): Int = if (denominator == 0L) 1_000_000 else
        BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(1_000_000)).divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP).intValueExact()
}
