package com.medidorderendimiento.domain

import java.time.Instant

data class FoodProduct(
    val id: LocalId,
    val name: String,
) {
    init { require(name.isNotBlank()) { "Food name must not be blank" } }
}

data class NutritionFacts(
    val energy: EnergyAmount?,
    val protein: NutrientAmount?,
    val carbohydrates: NutrientAmount?,
    val fat: NutrientAmount?,
)

enum class QuantityNature { MEASURED, DECLARED, ESTIMATED }
enum class EntryConfirmation { DRAFT, PENDING, CONFIRMED, REJECTED }
enum class NutrientNature { DECLARED, ESTIMATED }

data class FoodEntry(
    val id: LocalId,
    val product: FoodProduct,
    val consumedQuantity: Quantity,
    val nutrition: NutritionFacts,
    val quantityNature: QuantityNature,
    val recordedAt: Instant,
    val civilDay: CivilDay,
    val source: ManualSource = ManualSource.MANUAL,
    val revision: Long = 1,
    val confirmation: EntryConfirmation = EntryConfirmation.CONFIRMED,
    val nutrientNature: NutrientNature = NutrientNature.DECLARED,
) {
    init { require(revision > 0) { "Revision must be greater than zero" } }
}
