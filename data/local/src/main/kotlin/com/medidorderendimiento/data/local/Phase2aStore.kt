package com.medidorderendimiento.data.local

import com.medidorderendimiento.domain.*
import java.time.Instant

class Phase2aStore(private val database: PerformanceDatabase) {
    fun ensureProfile(profileId: LocalId, now: Instant) {
        if (database.userProfiles().get(profileId.value) == null) {
            database.userProfiles().save(UserProfileEntity(profileId.value, null, null, null, "DEVICE_ZONE", now.toEpochMilli()))
        }
    }

    fun addPlan(profileId: LocalId, plan: NutritionPlanVersion) = database.nutritionPlans().insert(plan.toEntity(profileId))
    fun latestPlan(profileId: LocalId): NutritionPlanVersion? = database.nutritionPlans().latest(profileId.value)?.toDomain()
    fun addWeight(profileId: LocalId, weight: WeightMeasurement) = database.weights().insert(weight.toEntity(profileId))
    fun latestWeight(profileId: LocalId): WeightMeasurement? = database.weights().latest(profileId.value)?.toDomain()
    fun saveProduct(product: StoredFoodProduct) = database.foodProducts().save(product.toEntity())
    fun products(): List<StoredFoodProduct> = database.foodProducts().list().map(FoodProductEntity::toDomain)
    fun addEntry(profileId: LocalId, entry: FoodEntry) = database.foodEntries().insert(entry.toEntity(profileId))
    fun entries(profileId: LocalId, day: CivilDay): List<FoodEntry> =
        database.foodEntries().listForDay(profileId.value, day.toEpochDay()).map { entity ->
            val product = requireNotNull(database.foodProducts().get(entity.productId)).toDomain().product
            entity.toDomain(product)
        }
    fun saveDiary(day: StoredDiaryDay) = database.diaryDays().save(day.toEntity())
    fun diary(profileId: LocalId, day: CivilDay): StoredDiaryDay? =
        database.diaryDays().get(profileId.value, day.toEpochDay())?.toDomain()
}
