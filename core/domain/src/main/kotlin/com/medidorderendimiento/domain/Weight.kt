package com.medidorderendimiento.domain

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

enum class ManualSource { MANUAL }

data class WeightMeasurement(
    val id: LocalId,
    val mass: BodyMass,
    val recordedAt: Instant,
    val civilDay: CivilDay,
    val source: ManualSource = ManualSource.MANUAL,
    val revision: Long = 1,
) {
    init { require(revision > 0) { "Revision must be greater than zero" } }
}

data class WeighingConditions(
    val usualScale: Boolean? = null,
    val morning: Boolean? = null,
    val fasting: Boolean? = null,
    val afterTraining: Boolean? = null,
    val usualClothing: Boolean? = null,
) {
    val isUnusual: Boolean
        get() = usualScale == false || morning == false || fasting == false || afterTraining == true || usualClothing == false

    internal val habitualScore: Int
        get() = listOf(usualScale, morning, fasting, usualClothing).count { it == true } + if (afterTraining == false) 1 else 0
}

data class WeightObservation(
    val measurement: WeightMeasurement,
    val conditions: WeighingConditions? = null,
)

enum class WeightTrendConfidence { UNAVAILABLE, LOW, MODERATE, HIGH }

enum class WeightTrendReason {
    INSUFFICIENT_DISTINCT_DAYS,
    INSUFFICIENT_COVERAGE,
    UNEVEN_DISTRIBUTION,
    UNUSUAL_WEIGHING_CONDITIONS,
    POSSIBLE_OUTLIERS,
    POSSIBLE_REGIME_CHANGE,
    CONTROLLED_NOISE,
}

data class WeightObservationSelection(
    val civilDay: CivilDay,
    val selected: WeightObservation,
    val alternatives: List<WeightObservation>,
    val reason: String,
)

data class WeightTrendCoverage(
    val distinctDays: Int,
    val spanDays: Long,
    val maximumGapDays: Long,
)

data class WeightTrend(
    val referenceDay: CivilDay,
    val latestObserved: WeightMeasurement?,
    val estimatedMass: BodyMass?,
    val weeklyRateGrams: Long?,
    val variabilityGrams: Long?,
    val selections: List<WeightObservationSelection>,
    val included: List<WeightObservation>,
    val possibleOutliers: List<WeightObservation>,
    val coverage: WeightTrendCoverage,
    val confidence: WeightTrendConfidence,
    val reasons: Set<WeightTrendReason>,
    val observedMonthlyChange: BodyMassChange? = null,
) {
    val isAvailable: Boolean get() = estimatedMass != null
}

data class BodyMassChange(val grams: Long)

data class WeightTrendPolicy(
    val windowDays: Long = 28,
    val isolatedOutlierMinimumGrams: Long = 1_000,
) {
    init {
        require(windowDays in 21..35) { "Weight trend window must be between 21 and 35 days" }
        require(isolatedOutlierMinimumGrams > 0)
    }
}

class WeightTrendCalculator(private val policy: WeightTrendPolicy = WeightTrendPolicy()) {
    fun calculate(
        observations: List<WeightObservation>,
        referenceDay: CivilDay,
    ): WeightTrend {
        val referenceDate = referenceDay.value
        val firstDate = referenceDate.minusDays(policy.windowDays - 1)
        val eligible = observations.filter { it.measurement.civilDay.value <= referenceDate }
        val latest = eligible.maxWithOrNull(compareBy<WeightObservation>({ it.measurement.recordedAt }, { it.measurement.id.value }))?.measurement
        val ordered = eligible
            .filter { it.measurement.civilDay.value in firstDate..referenceDate }
            .sortedWith(compareBy({ it.measurement.recordedAt }, { it.measurement.id.value }))
        val selections = ordered.groupBy { it.measurement.civilDay }.toSortedMap(compareBy { it.value.toEpochDay() }).map { (day, values) ->
            val selected = values.maxWithOrNull(compareBy<WeightObservation>({ it.conditions?.habitualScore ?: 0 },
                { it.measurement.recordedAt }, { it.measurement.id.value }))!!
            WeightObservationSelection(day, selected, values.filterNot { it === selected },
                if (values.size == 1) "Única observación del día" else "Mayor prioridad de condiciones habituales; desempate por instante e identificador")
        }
        val analytical = selections.map { it.selected }
        val coverage = coverage(analytical)
        val outlierCandidates = isolatedOutliers(analytical)
        val regimeIds = persistentRegimeIds(analytical)
        val outliers = outlierCandidates.filterNot { it.measurement.id in regimeIds }
        val included = analytical.filterNot { candidate -> outliers.any { it.measurement.id == candidate.measurement.id } }
        val unusual = analytical.any { it.conditions?.isUnusual == true }
        val reasons = linkedSetOf<WeightTrendReason>()
        if (coverage.distinctDays < 5) reasons += WeightTrendReason.INSUFFICIENT_DISTINCT_DAYS
        if (coverage.spanDays < 10) reasons += WeightTrendReason.INSUFFICIENT_COVERAGE
        if (coverage.maximumGapDays > 10) reasons += WeightTrendReason.UNEVEN_DISTRIBUTION
        if (unusual) reasons += WeightTrendReason.UNUSUAL_WEIGHING_CONDITIONS
        if (outliers.isNotEmpty()) reasons += WeightTrendReason.POSSIBLE_OUTLIERS
        if (regimeIds.isNotEmpty()) reasons += WeightTrendReason.POSSIBLE_REGIME_CHANGE

        if (coverage.distinctDays < 5 || coverage.spanDays < 10 || included.size < 2) {
            return WeightTrend(referenceDay, latest, null, null, null, selections, included, outliers, coverage,
                WeightTrendConfidence.UNAVAILABLE, reasons)
        }
        val x = included.map { ChronoUnit.DAYS.between(referenceDate, it.measurement.civilDay.value).toDouble() }
        val y = included.map { it.measurement.mass.grams.toDouble() }
        val slopes = buildList {
            for (i in x.indices) for (j in i + 1 until x.size) if (x[j] != x[i]) add((y[j] - y[i]) / (x[j] - x[i]))
        }
        val dailySlope = median(slopes)
        val intercept = median(x.indices.map { y[it] - dailySlope * x[it] })
        val residuals = x.indices.map { abs(y[it] - (intercept + dailySlope * x[it])) }
        val variability = median(residuals).roundToLong()
        val distributionReasonable = coverage.maximumGapDays <= 10
        val controlledNoise = variability <= 750
        if (controlledNoise) reasons += WeightTrendReason.CONTROLLED_NOISE
        val confidence = when {
            coverage.distinctDays >= 8 && coverage.spanDays >= 21 && distributionReasonable && controlledNoise && !unusual -> WeightTrendConfidence.HIGH
            coverage.distinctDays >= 5 && coverage.spanDays >= 14 && distributionReasonable -> WeightTrendConfidence.MODERATE
            else -> WeightTrendConfidence.LOW
        }
        val monthly = observedMonthlyChange(included, referenceDate)
        return WeightTrend(referenceDay, latest, BodyMass.ofGrams(intercept.roundToLong()), (dailySlope * 7).roundToLong(),
            variability, selections, included, outliers, coverage, confidence, reasons, monthly)
    }

    private fun coverage(values: List<WeightObservation>): WeightTrendCoverage {
        if (values.isEmpty()) return WeightTrendCoverage(0, 0, 0)
        val days = values.map { it.measurement.civilDay.value.toEpochDay() }.sorted()
        return WeightTrendCoverage(days.size, days.last() - days.first(), days.zipWithNext { a, b -> b - a }.maxOrNull() ?: 0)
    }

    private fun isolatedOutliers(values: List<WeightObservation>): List<WeightObservation> {
        if (values.size < 5) return emptyList()
        val masses = values.map { it.measurement.mass.grams.toDouble() }
        val localResiduals = masses.indices.map { index ->
            val neighbors = masses.filterIndexed { other, _ -> other != index && abs(other - index) <= 2 }
            masses[index] - median(neighbors)
        }
        val center = median(localResiduals)
        val mad = median(localResiduals.map { abs(it - center) })
        val threshold = max(policy.isolatedOutlierMinimumGrams.toDouble(), 4.0 * max(mad, 1.0))
        return values.filterIndexed { index, _ -> abs(localResiduals[index] - center) > threshold }
    }

    private fun persistentRegimeIds(values: List<WeightObservation>): Set<LocalId> {
        if (values.size < 6) return emptySet()
        for (start in 3..values.size - 3) {
            val before = values.subList(max(0, start - 3), start).map { it.measurement.mass.grams.toDouble() }
            val after = values.subList(start, minOf(values.size, start + 3)).map { it.measurement.mass.grams.toDouble() }
            if (abs(median(after) - median(before)) >= policy.isolatedOutlierMinimumGrams) {
                return values.subList(start, minOf(values.size, start + 3)).map { it.measurement.id }.toSet()
            }
        }
        return emptySet()
    }

    private fun observedMonthlyChange(values: List<WeightObservation>, referenceDate: java.time.LocalDate): BodyMassChange? {
        if (values.size < 8) return null
        val start = referenceDate.minusDays(30)
        val early = values.filter { abs(ChronoUnit.DAYS.between(start, it.measurement.civilDay.value)) <= 3 }
        val late = values.filter { abs(ChronoUnit.DAYS.between(it.measurement.civilDay.value, referenceDate)) <= 3 }
        if (early.size < 2 || late.size < 2) return null
        return BodyMassChange((median(late.map { it.measurement.mass.grams.toDouble() }) -
            median(early.map { it.measurement.mass.grams.toDouble() })).roundToLong())
    }

    private fun median(values: List<Double>): Double {
        require(values.isNotEmpty())
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
}
