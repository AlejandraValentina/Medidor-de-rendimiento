package com.medidorderendimiento.domain

sealed interface Quantity {
    @ConsistentCopyVisibility
    data class Mass private constructor(val milligrams: Long) : Quantity {
        init { require(milligrams > 0) { "Food mass must be greater than zero" } }

        companion object {
            fun ofMilligrams(milligrams: Long): Mass = Mass(milligrams)
            fun ofGrams(grams: Long): Mass = Mass(Math.multiplyExact(grams, 1_000))
        }
    }

    @ConsistentCopyVisibility
    data class Volume private constructor(val microliters: Long) : Quantity {
        init { require(microliters > 0) { "Food volume must be greater than zero" } }

        companion object {
            fun ofMicroliters(microliters: Long): Volume = Volume(microliters)
            fun ofMilliliters(milliliters: Long): Volume =
                Volume(Math.multiplyExact(milliliters, 1_000))
        }
    }

    @ConsistentCopyVisibility
    data class Units private constructor(val thousandths: Long) : Quantity {
        init { require(thousandths > 0) { "Unit quantity must be greater than zero" } }

        companion object {
            fun ofWholeUnits(units: Long): Units = Units(Math.multiplyExact(units, 1_000))
            fun ofThousandths(thousandths: Long): Units = Units(thousandths)
        }
    }

    @ConsistentCopyVisibility
    data class Portions private constructor(val thousandths: Long) : Quantity {
        init { require(thousandths > 0) { "Portion quantity must be greater than zero" } }

        companion object {
            fun ofWholePortions(portions: Long): Portions = Portions(Math.multiplyExact(portions, 1_000))
            fun ofThousandths(thousandths: Long): Portions = Portions(thousandths)
        }
    }
}
