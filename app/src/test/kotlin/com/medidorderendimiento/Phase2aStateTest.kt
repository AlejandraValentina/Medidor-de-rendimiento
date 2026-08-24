package com.medidorderendimiento

import com.medidorderendimiento.domain.*
import java.time.Instant
import kotlin.test.*

class Phase2aStateTest {
    private val day = CivilDay.of(2026, 8, 24)

    @Test fun `BASE_ONLY recommends exactly plan base`() {
        val plan = NutritionPlanVersion(LocalId("plan"), NutritionGoal.MAINTENANCE, EnergyAmount.ofKilocalories(2_000),
            null, null, day, acceptance = PlanAcceptance(Instant.EPOCH))
        val state = Phase2aUiState(civilDay = day, plan = plan)
        assertEquals(plan.baseDailyEnergy, state.recommendedToday)
    }

    @Test fun `missing observations and nutrients never become zero`() {
        val state = Phase2aUiState(civilDay = day)
        assertNull(state.latestWeight)
        assertNull(state.summary.confirmedEnergy)
        assertNull(state.summary.protein)
        assertNull(state.remaining)
        assertFalse(state.summary.hasEntries)
        assertFalse(state.summary.energyComplete)
    }

    @Test fun `weight input converts decimal kilograms deterministically`() {
        assertEquals(61_250, parseKilograms("61.250")?.grams)
        assertNull(parseKilograms("invalid"))
        assertNull(parseKilograms("0"))
        assertNull(parseKilograms("61.2501"))
    }

    @Test fun `consumed quantity controls nutrition and preserves explicit zero`() {
        val facts = NutritionFacts(EnergyAmount.ofKilocalories(200), NutrientAmount.ofGrams(10), null, null)
        val half = scaleNutrition(facts, Quantity.Mass.ofGrams(100), Quantity.Mass.ofGrams(50))
        assertEquals(EnergyAmount.ofKilocalories(100), half.energy)
        assertEquals(NutrientAmount.ofGrams(5), half.protein)
        val zero = scaleNutrition(NutritionFacts(EnergyAmount.ofMillicalories(0), null, null, null),
            Quantity.Volume.ofMilliliters(100), Quantity.Volume.ofMilliliters(50))
        assertEquals(EnergyAmount.ofMillicalories(0), zero.energy)
    }

    @Test fun `unknown energy or protein makes aggregate unknown rather than zero`() {
        val entry = FoodEntry(LocalId("unknown"), FoodProduct(LocalId("p"), "Food"), Quantity.Mass.ofGrams(1),
            NutritionFacts(null, null, null, null), QuantityNature.DECLARED, Instant.EPOCH, day)
        val summary = summarize(listOf(entry))
        assertNull(summary.confirmedEnergy)
        assertNull(summary.protein)
        assertFalse(summary.energyComplete)
        assertNull(Phase2aUiState(civilDay = day, entries = listOf(entry)).remaining)
    }

    @Test fun `all four food units remain distinct`() {
        assertIs<Quantity.Mass>(quantityOf(FoodUnit.GRAMS, 1))
        assertIs<Quantity.Volume>(quantityOf(FoodUnit.MILLILITERS, 1))
        assertIs<Quantity.Units>(quantityOf(FoodUnit.UNITS, 1))
        assertIs<Quantity.Portions>(quantityOf(FoodUnit.PORTIONS, 1))
    }

    @Test fun `mass and volume cannot be interchanged`() {
        assertFailsWith<IllegalArgumentException> {
            scaleNutrition(NutritionFacts(null, null, null, null), Quantity.Mass.ofGrams(100), Quantity.Volume.ofMilliliters(100))
        }
    }

    @Test fun `entry snapshot does not change when product facts change`() {
        val product = FoodProduct(LocalId("food"), "Food")
        val snapshot = scaleNutrition(NutritionFacts(EnergyAmount.ofKilocalories(100), null, null, null),
            Quantity.Mass.ofGrams(100), Quantity.Mass.ofGrams(50))
        val entry = FoodEntry(LocalId("entry"), product, Quantity.Mass.ofGrams(50), snapshot, QuantityNature.DECLARED,
            Instant.EPOCH, day)
        val editedFacts = NutritionFacts(EnergyAmount.ofKilocalories(300), null, null, null)
        assertEquals(EnergyAmount.ofKilocalories(50), entry.nutrition.energy)
        assertNotEquals(editedFacts.energy, entry.nutrition.energy)
    }

    @Test fun `estimated and declared confirmed energy are mutually exclusive`() {
        fun entry(id: String, nature: NutrientNature) = FoodEntry(LocalId(id), FoodProduct(LocalId("p$id"), "Food"),
            Quantity.Mass.ofGrams(1), NutritionFacts(EnergyAmount.ofKilocalories(10), null, null, null),
            QuantityNature.DECLARED, Instant.EPOCH, day, nutrientNature = nature)
        val summary = summarize(listOf(entry("a", NutrientNature.DECLARED), entry("b", NutrientNature.ESTIMATED)))
        assertEquals(EnergyAmount.ofKilocalories(10), summary.confirmedEnergy)
        assertEquals(EnergyAmount.ofKilocalories(10), summary.estimatedEnergy)
    }
}
