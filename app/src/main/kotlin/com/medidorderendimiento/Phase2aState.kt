package com.medidorderendimiento

import com.medidorderendimiento.data.local.DiaryClosureState
import com.medidorderendimiento.data.local.StoredFoodProduct
import com.medidorderendimiento.data.local.Phase2aStore
import com.medidorderendimiento.domain.*
import java.math.BigDecimal
import java.math.RoundingMode

data class DailySummary(
    val confirmedEnergy: EnergyAmount?,
    val estimatedEnergy: EnergyAmount?,
    val pendingEnergy: EnergyAmount?,
    val protein: NutrientAmount?,
    val hasEntries: Boolean,
    val energyComplete: Boolean,
)

data class Phase2aUiState(
    val civilDay: CivilDay? = null,
    val plan: NutritionPlanVersion? = null,
    val latestWeight: WeightMeasurement? = null,
    val weightTrend: WeightTrend? = null,
    val tdeeEstimate: TdeeEstimate? = null,
    val estimatorStability: EstimatorStability? = null,
    val products: List<StoredFoodProduct> = emptyList(),
    val entries: List<FoodEntry> = emptyList(),
    val diaryState: DiaryClosureState = DiaryClosureState.OPEN,
    val favorites: List<Phase2aStore.FavoriteFood> = emptyList(),
    val recentProducts: List<StoredFoodProduct> = emptyList(),
    val savedMeals: List<Phase2aStore.SavedMeal> = emptyList(),
) {
    val recommendedToday: EnergyAmount? get() = plan?.baseDailyEnergy
    val summary: DailySummary get() = summarize(entries)
    val remaining: EnergyAmount? get() {
        val base = recommendedToday ?: return null
        if (!summary.energyComplete) return null
        val consumed = listOfNotNull(summary.confirmedEnergy, summary.estimatedEnergy).reduceOrNull(EnergyAmount::plus) ?: return null
        return if (base.millicalories >= consumed.millicalories) base - consumed else EnergyAmount.ofMillicalories(0)
    }
}

fun summarize(entries: List<FoodEntry>): DailySummary {
    if (entries.isEmpty()) return DailySummary(null, null, null, null, false, false)
    val accepted = entries.filter { it.confirmation == EntryConfirmation.CONFIRMED }
    val pending = entries.filter { it.confirmation == EntryConfirmation.PENDING }
    val confirmed = accepted.filter { it.nutrientNature == NutrientNature.DECLARED }
    val estimated = accepted.filter { it.nutrientNature == NutrientNature.ESTIMATED }
    val energyComplete = accepted.isNotEmpty() && accepted.all { it.nutrition.energy != null }
    val protein = accepted.takeIf { it.isNotEmpty() && it.all { entry -> entry.nutrition.protein != null } }
        ?.map { requireNotNull(it.nutrition.protein) }?.reduce(NutrientAmount::plus)
    return DailySummary(confirmed.knownEnergy(), estimated.knownEnergy(), pending.knownEnergy(), protein, true, energyComplete)
}

enum class FoodUnit { GRAMS, MILLILITERS, UNITS, PORTIONS }

fun quantityOf(unit: FoodUnit, value: Long): Quantity = when (unit) {
    FoodUnit.GRAMS -> Quantity.Mass.ofGrams(value)
    FoodUnit.MILLILITERS -> Quantity.Volume.ofMilliliters(value)
    FoodUnit.UNITS -> Quantity.Units.ofWholeUnits(value)
    FoodUnit.PORTIONS -> Quantity.Portions.ofWholePortions(value)
}

private fun List<FoodEntry>.knownEnergy(): EnergyAmount? =
    takeIf { it.isNotEmpty() && it.all { entry -> entry.nutrition.energy != null } }
        ?.map { requireNotNull(it.nutrition.energy) }?.reduce(EnergyAmount::plus)

fun parseKilograms(text: String): BodyMass? = runCatching {
    val grams = BigDecimal(text.trim()).multiply(BigDecimal(1_000)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    BodyMass.ofGrams(grams)
}.getOrNull()

fun scaleNutrition(facts: NutritionFacts, basis: Quantity, consumed: Quantity): NutritionFacts {
    val (basisValue, consumedValue) = when {
        basis is Quantity.Mass && consumed is Quantity.Mass -> basis.milligrams to consumed.milligrams
        basis is Quantity.Volume && consumed is Quantity.Volume -> basis.microliters to consumed.microliters
        basis is Quantity.Units && consumed is Quantity.Units -> basis.thousandths to consumed.thousandths
        basis is Quantity.Portions && consumed is Quantity.Portions -> basis.thousandths to consumed.thousandths
        else -> throw IllegalArgumentException("Consumed quantity must use the product basis unit")
    }
    fun scaled(value: Long): Long = Math.multiplyExact(value, consumedValue) / basisValue
    return NutritionFacts(
        facts.energy?.let { EnergyAmount.ofMillicalories(scaled(it.millicalories)) },
        facts.protein?.let { NutrientAmount.ofMilligrams(scaled(it.milligrams)) },
        facts.carbohydrates?.let { NutrientAmount.ofMilligrams(scaled(it.milligrams)) },
        facts.fat?.let { NutrientAmount.ofMilligrams(scaled(it.milligrams)) },
    )
}
