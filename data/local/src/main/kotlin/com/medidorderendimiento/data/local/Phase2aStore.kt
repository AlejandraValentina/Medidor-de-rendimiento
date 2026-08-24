package com.medidorderendimiento.data.local

import com.medidorderendimiento.domain.*
import java.time.Instant

class Phase2aStore(private val database: PerformanceDatabase) {
    data class FavoriteFood(val id: LocalId, val product: StoredFoodProduct, val preferredQuantity: Quantity, val lastUsedAt: Instant)
    data class SavedMealItem(val id: LocalId, val product: StoredFoodProduct, val quantity: Quantity, val ordering: Int)
    data class SavedMeal(val id: LocalId, val name: String, val items: List<SavedMealItem>, val createdAt: Instant, val updatedAt: Instant)
    fun ensureProfile(profileId: LocalId, now: Instant) {
        if (database.userProfiles().get(profileId.value) == null) {
            database.userProfiles().save(UserProfileEntity(profileId.value, null, null, null, "DEVICE_ZONE", now.toEpochMilli()))
        }
    }

    fun addPlan(profileId: LocalId, plan: NutritionPlanVersion) = database.nutritionPlans().insert(plan.toEntity(profileId))
    fun latestPlan(profileId: LocalId): NutritionPlanVersion? = database.nutritionPlans().latest(profileId.value)?.toDomain()
    fun addWeight(profileId: LocalId, weight: WeightMeasurement) = database.weights().insert(weight.toEntity(profileId))
    fun latestWeight(profileId: LocalId): WeightMeasurement? = database.weights().latest(profileId.value)?.toDomain()
    fun weights(profileId: LocalId): List<WeightMeasurement> = database.weights().list(profileId.value).map(WeightMeasurementEntity::toDomain)
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

    fun saveFavorite(profileId: LocalId, id: LocalId, product: StoredFoodProduct, quantity: Quantity, now: Instant) {
        val (value, unit) = quantity.stored()
        database.favorites().save(FavoriteFoodEntity(id.value, profileId.value, product.product.id.value, value, unit, now.toEpochMilli()))
    }
    fun favorites(profileId: LocalId): List<FavoriteFood> = database.favorites().list(profileId.value).map { entity ->
        FavoriteFood(LocalId(entity.favoriteId), requireNotNull(database.foodProducts().get(entity.productId)).toDomain(),
            storedQuantity(entity.preferredQuantityValue, entity.preferredQuantityUnit), Instant.ofEpochMilli(entity.lastUsedAtEpochMillis))
    }
    fun removeFavorite(profileId: LocalId, productId: LocalId) = database.favorites().remove(profileId.value, productId.value)
    fun recentProducts(profileId: LocalId, limit: Int = 8): List<StoredFoodProduct> =
        database.foodEntries().recentProducts(profileId.value, limit).map(FoodProductEntity::toDomain)

    fun saveMeal(profileId: LocalId, id: LocalId, name: String, items: List<Pair<StoredFoodProduct, Quantity>>, now: Instant) {
        require(name.isNotBlank() && items.isNotEmpty())
        database.runInTransaction {
            database.savedMeals().insert(SavedMealEntity(id.value, profileId.value, name.trim(), now.toEpochMilli(), now.toEpochMilli(), null))
            database.savedMeals().insertItems(items.mapIndexed { index, (product, quantity) ->
                val (value, unit) = quantity.stored()
                SavedMealItemEntity("${id.value}-$index", id.value, product.product.id.value, value, unit, index)
            })
        }
    }
    fun savedMeals(profileId: LocalId): List<SavedMeal> = database.savedMeals().list(profileId.value).map { meal ->
        SavedMeal(LocalId(meal.savedMealId), meal.name, database.savedMeals().items(meal.savedMealId).map { item ->
            SavedMealItem(LocalId(item.savedMealItemId), requireNotNull(database.foodProducts().get(item.productId)).toDomain(),
                storedQuantity(item.quantityValue, item.quantityUnit), item.ordering)
        }, Instant.ofEpochMilli(meal.createdAtEpochMillis), Instant.ofEpochMilli(meal.updatedAtEpochMillis))
    }
    fun deleteMeal(id: LocalId) = database.savedMeals().delete(id.value)
}
