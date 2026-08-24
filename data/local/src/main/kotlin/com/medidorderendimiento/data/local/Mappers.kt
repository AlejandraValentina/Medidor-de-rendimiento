package com.medidorderendimiento.data.local

import com.medidorderendimiento.domain.*
import java.time.Instant
import java.time.LocalDate

internal fun CivilDay.toEpochDay(): Long = value.toEpochDay()
internal fun Long.toCivilDay(): CivilDay = CivilDay.parse(LocalDate.ofEpochDay(this).toString())

data class StoredFoodProduct(
    val product: FoodProduct,
    val nutrition: NutritionFacts,
    val basisQuantity: Quantity = Quantity.Mass.ofGrams(100),
)
data class StoredDiaryDay(
    val profileId: LocalId,
    val civilDay: CivilDay,
    val state: DiaryClosureState,
    val closedAt: Instant?,
    val updatedAt: Instant,
    val revision: Long,
    val exclusionReason: String?,
)

fun NutritionPlanVersion.toEntity(profileId: LocalId) = NutritionPlanVersionEntity(
    id.value, profileId.value, goal.name, baseDailyEnergy.millicalories,
    proteinTarget?.milligrams, targetWeeklyRate?.grams, validFrom.toEpochDay(),
    validUntil?.toEpochDay(), acceptance.acceptedAt.toEpochMilli(),
)
fun NutritionPlanVersionEntity.toDomain() = NutritionPlanVersion(
    LocalId(planVersionId), NutritionGoal.valueOf(objectiveKind), EnergyAmount.ofMillicalories(baseEnergyMillicalories),
    proteinTargetMilligrams?.let(NutrientAmount::ofMilligrams), rateTargetGrams?.let(TargetWeeklyRate::ofGrams),
    validFromEpochDay.toCivilDay(), validUntilEpochDay?.toCivilDay(), PlanAcceptance(Instant.ofEpochMilli(acceptedAtEpochMillis)),
)
fun WeightMeasurement.toEntity(profileId: LocalId) = WeightMeasurementEntity(
    id.value, profileId.value, id.value, recordedAt.toEpochMilli(), civilDay.toEpochDay(), mass.grams, source.name, revision,
)
fun WeightMeasurementEntity.toDomain() = WeightMeasurement(
    LocalId(weightId), BodyMass.ofGrams(bodyMassGrams), Instant.ofEpochMilli(recordedAtEpochMillis),
    civilDayEpochDay.toCivilDay(), ManualSource.valueOf(sourceKind), revision,
)
fun StoredFoodProduct.toEntity(revision: Long = 1): FoodProductEntity {
    val (basisValue, basisUnit) = basisQuantity.stored()
    return FoodProductEntity(
    product.id.value, product.name, product.name.trim().lowercase(), nutrition.energy?.millicalories,
        nutrition.protein?.milligrams, nutrition.carbohydrates?.milligrams, nutrition.fat?.milligrams, revision,
        basisValue, basisUnit,
    )
}
fun FoodProductEntity.toDomain() = StoredFoodProduct(
    FoodProduct(LocalId(productId), displayName), NutritionFacts(
        energyMillicalories?.let(EnergyAmount::ofMillicalories), proteinMilligrams?.let(NutrientAmount::ofMilligrams),
        carbohydratesMilligrams?.let(NutrientAmount::ofMilligrams), fatMilligrams?.let(NutrientAmount::ofMilligrams),
    ), storedQuantity(basisQuantityValue, basisQuantityUnit),
)
private fun Quantity.stored(): Pair<Long, String> = when (this) {
    is Quantity.Mass -> milligrams to "MASS_MG"
    is Quantity.Volume -> microliters to "VOLUME_UL"
    is Quantity.Units -> thousandths to "UNITS_THOUSANDTHS"
    is Quantity.Portions -> thousandths to "PORTIONS_THOUSANDTHS"
}
private fun storedQuantity(value: Long, unit: String): Quantity = when (unit) {
    "MASS_MG" -> Quantity.Mass.ofMilligrams(value)
    "VOLUME_UL" -> Quantity.Volume.ofMicroliters(value)
    "UNITS_THOUSANDTHS" -> Quantity.Units.ofThousandths(value)
    "PORTIONS_THOUSANDTHS" -> Quantity.Portions.ofThousandths(value)
    else -> error("Unsupported quantity unit: $unit")
}
fun FoodEntry.toEntity(profileId: LocalId): FoodEntryEntity {
    val (value, unit) = consumedQuantity.stored()
    return FoodEntryEntity(id.value, profileId.value, id.value, product.id.value, recordedAt.toEpochMilli(), civilDay.toEpochDay(),
        value, unit, nutrition.energy?.millicalories, nutrition.protein?.milligrams, nutrition.carbohydrates?.milligrams,
        nutrition.fat?.milligrams, quantityNature.name, source.name, revision, confirmation.name, nutrientNature.name)
}
fun FoodEntryEntity.toDomain(product: FoodProduct) = FoodEntry(
    LocalId(foodEntryId), product, storedQuantity(quantityValue, quantityUnit),
    NutritionFacts(energyMillicalories?.let(EnergyAmount::ofMillicalories), proteinMilligrams?.let(NutrientAmount::ofMilligrams),
        carbohydratesMilligrams?.let(NutrientAmount::ofMilligrams), fatMilligrams?.let(NutrientAmount::ofMilligrams)),
    QuantityNature.valueOf(quantityNature), Instant.ofEpochMilli(recordedAtEpochMillis), civilDayEpochDay.toCivilDay(),
    ManualSource.valueOf(sourceKind), revision, EntryConfirmation.valueOf(confirmationStatus), NutrientNature.valueOf(nutrientNature),
)
fun StoredDiaryDay.toEntity() = NutritionDiaryDayEntity(profileId.value, civilDay.toEpochDay(), state.name,
    closedAt?.toEpochMilli(), updatedAt.toEpochMilli(), revision, exclusionReason)
fun NutritionDiaryDayEntity.toDomain() = StoredDiaryDay(LocalId(profileId), civilDayEpochDay.toCivilDay(),
    DiaryClosureState.valueOf(closureState), closedAtEpochMillis?.let(Instant::ofEpochMilli),
    Instant.ofEpochMilli(updatedAtEpochMillis), closureRevision, exclusionReason)
