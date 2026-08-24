package com.medidorderendimiento.data.local

import com.medidorderendimiento.domain.*
import java.time.Instant
import kotlin.test.*

class MapperTest {
    private val profileId = LocalId("profile")
    private val day = CivilDay.of(2026, 8, 23)

    @Test fun `plan round trip preserves all values and absence`() {
        val plan = NutritionPlanVersion(LocalId("plan"), NutritionGoal.LOSS, EnergyAmount.ofMillicalories(2_000_001), null,
            TargetWeeklyRate.ofGrams(350), day, CivilDay.of(2026, 9, 23), PlanAcceptance(Instant.ofEpochMilli(1234)))
        assertEquals(plan, plan.toEntity(profileId).toDomain())
    }

    @Test fun `weight and civil day round trip exactly`() {
        val weight = WeightMeasurement(LocalId("weight"), BodyMass.ofGrams(61_234), Instant.ofEpochMilli(99), day)
        assertEquals(weight, weight.toEntity(profileId).toDomain())
    }

    @Test fun `food null and explicit zero remain different`() {
        val unknown = StoredFoodProduct(FoodProduct(LocalId("unknown"), "Unknown"), NutritionFacts(null, null, null, null))
        val zero = StoredFoodProduct(FoodProduct(LocalId("zero"), "Zero"), NutritionFacts(EnergyAmount.ofMillicalories(0), NutrientAmount.ofMilligrams(0), null, null))
        assertEquals(unknown, unknown.toEntity().toDomain())
        assertEquals(zero, zero.toEntity().toDomain())
        assertNull(unknown.toEntity().energyMillicalories)
        assertEquals(0, zero.toEntity().energyMillicalories)
    }

    @Test fun `all quantity kinds round trip without conversion`() {
        val quantities = listOf<Quantity>(Quantity.Mass.ofMilligrams(1), Quantity.Volume.ofMicroliters(2),
            Quantity.Units.ofThousandths(3), Quantity.Portions.ofThousandths(4))
        quantities.forEachIndexed { index, quantity ->
            val product = FoodProduct(LocalId("p$index"), "Product")
            val entry = FoodEntry(LocalId("e$index"), product, quantity, NutritionFacts(null, null, null, null),
                QuantityNature.MEASURED, Instant.EPOCH, day)
            assertEquals(entry, entry.toEntity(profileId).toDomain(product))
        }
    }

    @Test fun `all diary states round trip`() {
        DiaryClosureState.entries.forEach { state ->
            val record = StoredDiaryDay(profileId, day, state, if (state == DiaryClosureState.OPEN) null else Instant.EPOCH,
                Instant.ofEpochMilli(10), 1, if (state == DiaryClosureState.EXCLUDED_CONTEXT) "travel" else null)
            assertEquals(record, record.toEntity().toDomain())
        }
    }
}
