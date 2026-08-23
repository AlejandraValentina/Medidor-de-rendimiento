package com.medidorderendimiento.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class QuantityTest {
    @Test
    fun `mass volume units and portions remain different types`() {
        val grams: Quantity = Quantity.Mass.ofGrams(100)
        val milliliters: Quantity = Quantity.Volume.ofMilliliters(100)
        val units: Quantity = Quantity.Units.ofWholeUnits(1)
        val portions: Quantity = Quantity.Portions.ofWholePortions(1)

        assertIs<Quantity.Mass>(grams)
        assertIs<Quantity.Volume>(milliliters)
        assertIs<Quantity.Units>(units)
        assertIs<Quantity.Portions>(portions)
        assertFalse(grams.equals(milliliters))
        assertFalse(units.equals(portions))
    }

    @Test
    fun `quantities use scaled integers and reject non-positive consumption`() {
        assertEquals(100_000, Quantity.Mass.ofGrams(100).milligrams)
        assertEquals(250_000, Quantity.Volume.ofMilliliters(250).microliters)
        assertEquals(500, Quantity.Units.ofThousandths(500).thousandths)
        assertEquals(1_500, Quantity.Portions.ofThousandths(1_500).thousandths)

        assertFailsWith<IllegalArgumentException> { Quantity.Mass.ofMilligrams(0) }
        assertFailsWith<IllegalArgumentException> { Quantity.Volume.ofMicroliters(-1) }
        assertFailsWith<IllegalArgumentException> { Quantity.Units.ofWholeUnits(0) }
        assertFailsWith<IllegalArgumentException> { Quantity.Portions.ofWholePortions(0) }
    }

    @Test
    fun `there is no mass to volume conversion contract`() {
        val mass: Quantity = Quantity.Mass.ofGrams(250)

        assertIs<Quantity.Mass>(mass)
    }
}
