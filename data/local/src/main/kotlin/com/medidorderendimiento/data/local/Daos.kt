package com.medidorderendimiento.data.local

import androidx.room.*

@Dao interface UserProfileDao {
    @Upsert fun save(entity: UserProfileEntity)
    @Query("SELECT * FROM user_profiles WHERE profileId = :id") fun get(id: String): UserProfileEntity?
}
@Dao interface NutritionPlanDao {
    @Insert fun insert(entity: NutritionPlanVersionEntity)
    @Query("SELECT * FROM nutrition_plan_versions WHERE planVersionId = :id") fun get(id: String): NutritionPlanVersionEntity?
    @Query("SELECT * FROM nutrition_plan_versions WHERE profileId = :profileId ORDER BY validFromEpochDay") fun list(profileId: String): List<NutritionPlanVersionEntity>
    @Query("SELECT * FROM nutrition_plan_versions WHERE profileId = :profileId ORDER BY validFromEpochDay DESC LIMIT 1") fun latest(profileId: String): NutritionPlanVersionEntity?
}
@Dao interface WeightMeasurementDao {
    @Insert fun insert(entity: WeightMeasurementEntity)
    @Query("SELECT * FROM weight_measurements WHERE weightId = :id") fun get(id: String): WeightMeasurementEntity?
    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY recordedAtEpochMillis") fun list(profileId: String): List<WeightMeasurementEntity>
    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY recordedAtEpochMillis DESC LIMIT 1") fun latest(profileId: String): WeightMeasurementEntity?
    @Query("DELETE FROM weight_measurements WHERE weightId = :id") fun delete(id: String): Int
}
@Dao interface FoodProductDao {
    @Upsert fun save(entity: FoodProductEntity)
    @Query("SELECT * FROM food_products WHERE productId = :id") fun get(id: String): FoodProductEntity?
    @Query("SELECT * FROM food_products ORDER BY normalizedName") fun list(): List<FoodProductEntity>
}
@Dao interface FoodEntryDao {
    @Insert fun insert(entity: FoodEntryEntity)
    @Query("SELECT * FROM food_entries WHERE foodEntryId = :id") fun get(id: String): FoodEntryEntity?
    @Query("SELECT * FROM food_entries WHERE profileId = :profileId AND civilDayEpochDay = :day ORDER BY recordedAtEpochMillis") fun listForDay(profileId: String, day: Long): List<FoodEntryEntity>
    @Query("SELECT food_products.* FROM food_products INNER JOIN food_entries ON food_products.productId = food_entries.productId WHERE food_entries.profileId = :profileId GROUP BY food_products.productId ORDER BY MAX(food_entries.recordedAtEpochMillis) DESC LIMIT :limit")
    fun recentProducts(profileId: String, limit: Int): List<FoodProductEntity>
    @Query("SELECT * FROM food_entries WHERE profileId = :profileId AND civilDayEpochDay BETWEEN :startDay AND :endDay ORDER BY civilDayEpochDay, recordedAtEpochMillis")
    fun listForRange(profileId: String, startDay: Long, endDay: Long): List<FoodEntryEntity>
}
@Dao interface NutritionDiaryDayDao {
    @Upsert fun save(entity: NutritionDiaryDayEntity)
    @Query("SELECT * FROM nutrition_diary_days WHERE profileId = :profileId AND civilDayEpochDay = :day") fun get(profileId: String, day: Long): NutritionDiaryDayEntity?
    @Query("SELECT * FROM nutrition_diary_days WHERE profileId = :profileId ORDER BY civilDayEpochDay") fun list(profileId: String): List<NutritionDiaryDayEntity>
}
@Dao interface FavoriteFoodDao {
    @Upsert fun save(entity: FavoriteFoodEntity)
    @Query("SELECT * FROM favorite_foods WHERE profileId = :profileId ORDER BY lastUsedAtEpochMillis DESC") fun list(profileId: String): List<FavoriteFoodEntity>
    @Query("DELETE FROM favorite_foods WHERE profileId = :profileId AND productId = :productId") fun remove(profileId: String, productId: String): Int
}
@Dao interface SavedMealDao {
    @Insert fun insert(entity: SavedMealEntity)
    @Insert fun insertItems(entities: List<SavedMealItemEntity>)
    @Query("SELECT * FROM saved_meals WHERE profileId = :profileId AND archivedAtEpochMillis IS NULL ORDER BY updatedAtEpochMillis DESC") fun list(profileId: String): List<SavedMealEntity>
    @Query("SELECT * FROM saved_meal_items WHERE savedMealId = :mealId ORDER BY ordering") fun items(mealId: String): List<SavedMealItemEntity>
    @Query("DELETE FROM saved_meals WHERE savedMealId = :mealId") fun delete(mealId: String): Int
}
@Dao interface TdeeEstimateDao {
    @Insert fun insert(entity: TdeeEstimateEntity)
    @Query("SELECT * FROM tdee_estimates WHERE profileId = :profileId ORDER BY referenceDayEpochDay, revision")
    fun history(profileId: String): List<TdeeEstimateEntity>
    @Query("SELECT t.* FROM tdee_estimates t INNER JOIN (SELECT referenceDayEpochDay, MAX(revision) AS latestRevision FROM tdee_estimates WHERE profileId = :profileId GROUP BY referenceDayEpochDay) latest ON t.referenceDayEpochDay = latest.referenceDayEpochDay AND t.revision = latest.latestRevision WHERE t.profileId = :profileId ORDER BY t.referenceDayEpochDay")
    fun currentHistory(profileId: String): List<TdeeEstimateEntity>
    @Query("SELECT * FROM tdee_estimates WHERE profileId = :profileId ORDER BY referenceDayEpochDay DESC, revision DESC LIMIT 1")
    fun latest(profileId: String): TdeeEstimateEntity?
    @Query("SELECT * FROM tdee_estimates WHERE profileId = :profileId AND referenceDayEpochDay = :day ORDER BY revision DESC LIMIT 1")
    fun latestForDay(profileId: String, day: Long): TdeeEstimateEntity?
    @Query("SELECT * FROM tdee_estimates WHERE tdeeId = :id LIMIT 1") fun get(id: String): TdeeEstimateEntity?
}
@Dao interface PlanEvaluationDao {
    @Insert fun insert(entity: PlanEvaluationEntity)
    @Query("SELECT * FROM plan_evaluations WHERE profileId = :profileId ORDER BY referenceDayEpochDay, revision") fun history(profileId: String): List<PlanEvaluationEntity>
    @Query("SELECT p.* FROM plan_evaluations p INNER JOIN (SELECT referenceDayEpochDay, MAX(revision) latestRevision FROM plan_evaluations WHERE profileId = :profileId GROUP BY referenceDayEpochDay) latest ON p.referenceDayEpochDay=latest.referenceDayEpochDay AND p.revision=latest.latestRevision WHERE p.profileId=:profileId ORDER BY p.referenceDayEpochDay")
    fun currentHistory(profileId: String): List<PlanEvaluationEntity>
    @Query("SELECT * FROM plan_evaluations WHERE profileId=:profileId AND referenceDayEpochDay=:day ORDER BY revision DESC LIMIT 1") fun latestForDay(profileId: String, day: Long): PlanEvaluationEntity?
}
@Dao interface DecisionStateMemoryDao {
    @Upsert fun save(entity: DecisionStateMemoryEntity)
    @Query("SELECT * FROM decision_state_memory WHERE planVersionId=:planVersionId") fun get(planVersionId: String): DecisionStateMemoryEntity?
    @Query("DELETE FROM decision_state_memory WHERE planVersionId=:planVersionId") fun delete(planVersionId: String): Int
}
