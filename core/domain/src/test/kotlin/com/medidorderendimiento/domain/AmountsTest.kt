package com.medidorderendimiento.domain

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AmountsTest {
    @Test
    fun `body mass stores exact grams and converts only for display`() {
        val mass = BodyMass.ofGrams(62_345)

        assertEquals(62_345, mass.grams)
        assertEquals(BigDecimal("62.345"), mass.kilogramsForDisplay())
    }

    @Test
    fun `body mass rejects zero and negative values`() {
        assertFailsWith<IllegalArgumentException> { BodyMass.ofGrams(0) }
        assertFailsWith<IllegalArgumentException> { BodyMass.ofGrams(-1) }
    }

    @Test
    fun `unknown body mass remains outside the numeric value`() {
        val unknownMass: BodyMass? = null

        assertNull(unknownMass)
    }

    @Test
    fun `zero energy is valid and unknown energy is distinct`() {
        val zero = EnergyAmount.ofMillicalories(0)
        val unknown: EnergyAmount? = null

        assertEquals(0, zero.millicalories)
        assertNull(unknown)
    }

    @Test
    fun `energy arithmetic is exact and cannot produce negative energy`() {
        val first = EnergyAmount.ofMillicalories(1_001)
        val second = EnergyAmount.ofMillicalories(2_002)

        assertEquals(3_003, (first + second).millicalories)
        assertEquals(1_001, ((first + second) - second).millicalories)
        assertFailsWith<IllegalArgumentException> { first - second }
        assertFailsWith<ArithmeticException> {
            EnergyAmount.ofMillicalories(Long.MAX_VALUE) + EnergyAmount.ofMillicalories(1)
        }
    }

    @Test
    fun `energy rejects negative values and converts whole kilocalories exactly`() {
        assertEquals(2_347_000, EnergyAmount.ofKilocalories(2_347).millicalories)
        assertFailsWith<IllegalArgumentException> { EnergyAmount.ofMillicalories(-1) }
    }

    @Test
    fun `nutrients preserve zero separately from absence and convert exactly`() {
        val zero = NutrientAmount.ofMilligrams(0)
        val protein = NutrientAmount.ofGrams(125)
        val unknown: NutrientAmount? = null

        assertEquals(0, zero.milligrams)
        assertEquals(125_000, protein.milligrams)
        assertEquals(BigDecimal("125.000"), protein.gramsForDisplay())
        assertNull(unknown)
        assertFailsWith<IllegalArgumentException> { NutrientAmount.ofMilligrams(-1) }
    }

    @Test
    fun `volume has an exact integer representation`() {
        assertEquals(500_000, VolumeAmount.ofMilliliters(500).microliters)
        assertFailsWith<IllegalArgumentException> { VolumeAmount.ofMicroliters(-1) }
    }
}
