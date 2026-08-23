package com.medidorderendimiento.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ObservationsTest {
    private val instant = Instant.parse("2026-08-23T07:00:00Z")
    private val day = CivilDay.of(2026, 8, 23)

    @Test
    fun `manual weight preserves its observed value and revision`() {
        val weight = WeightMeasurement(
            id = LocalId("weight-1"),
            mass = BodyMass.ofGrams(70_250),
            recordedAt = instant,
            civilDay = day,
        )

        assertEquals(70_250, weight.mass.grams)
        assertEquals(ManualSource.MANUAL, weight.source)
        assertEquals(1, weight.revision)
        assertFailsWith<IllegalArgumentException> { weight.copy(revision = 0) }
    }

    @Test
    fun `food facts preserve unknown nutrients independently from real zero energy`() {
        val facts = NutritionFacts(
            energy = EnergyAmount.ofMillicalories(0),
            protein = null,
            carbohydrates = null,
            fat = null,
        )

        assertEquals(0, facts.energy?.millicalories)
        assertNull(facts.protein)
        assertNull(facts.carbohydrates)
        assertNull(facts.fat)
    }

    @Test
    fun `food entry distinguishes declared estimated and measured quantity`() {
        val product = FoodProduct(LocalId("food-1"), "Bebida sin calorías")
        val facts = NutritionFacts(EnergyAmount.ofMillicalories(0), null, null, null)

        QuantityNature.entries.forEach { nature ->
            val entry = FoodEntry(
                id = LocalId("entry-${nature.name}"),
                product = product,
                consumedQuantity = Quantity.Volume.ofMilliliters(500),
                nutrition = facts,
                quantityNature = nature,
                recordedAt = instant,
                civilDay = day,
            )

            assertEquals(nature, entry.quantityNature)
            assertEquals(ManualSource.MANUAL, entry.source)
        }
    }

    @Test
    fun `identities and food names reject blank values`() {
        assertFailsWith<IllegalArgumentException> { LocalId(" ") }
        assertFailsWith<IllegalArgumentException> { FoodProduct(LocalId("food-1"), "") }
    }
}
