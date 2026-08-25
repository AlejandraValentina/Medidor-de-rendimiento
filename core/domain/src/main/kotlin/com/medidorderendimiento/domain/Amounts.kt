package com.medidorderendimiento.domain

import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class BodyMass private constructor(val grams: Long) {
    init {
        require(grams > 0) { "Body mass must be greater than zero" }
    }

    fun kilogramsForDisplay(scale: Int = 3): BigDecimal =
        BigDecimal.valueOf(grams).divide(GRAMS_PER_KILOGRAM, scale, RoundingMode.HALF_UP)

    companion object {
        private val GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1_000)

        fun ofGrams(grams: Long): BodyMass = BodyMass(grams)
    }
}

@JvmInline
value class EnergyAmount private constructor(val millicalories: Long) {
    init {
        require(millicalories >= 0) { "Energy must not be negative" }
    }

    operator fun plus(other: EnergyAmount): EnergyAmount =
        EnergyAmount(Math.addExact(millicalories, other.millicalories))

    operator fun minus(other: EnergyAmount): EnergyAmount {
        require(millicalories >= other.millicalories) { "Energy subtraction must not be negative" }
        return EnergyAmount(millicalories - other.millicalories)
    }

    companion object {
        fun ofMillicalories(millicalories: Long): EnergyAmount = EnergyAmount(millicalories)
        fun ofKilocalories(kilocalories: Long): EnergyAmount =
            EnergyAmount(Math.multiplyExact(kilocalories, 1_000))
    }
}

@JvmInline
value class NutrientAmount private constructor(val milligrams: Long) {
    init {
        require(milligrams >= 0) { "Nutrient amount must not be negative" }
    }

    fun gramsForDisplay(scale: Int = 3): BigDecimal =
        BigDecimal.valueOf(milligrams).divide(MILLIGRAMS_PER_GRAM, scale, RoundingMode.HALF_UP)

    operator fun plus(other: NutrientAmount): NutrientAmount =
        NutrientAmount(Math.addExact(milligrams, other.milligrams))

    companion object {
        private val MILLIGRAMS_PER_GRAM = BigDecimal.valueOf(1_000)

        fun ofMilligrams(milligrams: Long): NutrientAmount = NutrientAmount(milligrams)
        fun ofGrams(grams: Long): NutrientAmount =
            NutrientAmount(Math.multiplyExact(grams, 1_000))
    }
}

@JvmInline
value class VolumeAmount private constructor(val microliters: Long) {
    init {
        require(microliters >= 0) { "Volume must not be negative" }
    }

    companion object {
        fun ofMicroliters(microliters: Long): VolumeAmount = VolumeAmount(microliters)
        fun ofMilliliters(milliliters: Long): VolumeAmount =
            VolumeAmount(Math.multiplyExact(milliliters, 1_000))
    }
}
