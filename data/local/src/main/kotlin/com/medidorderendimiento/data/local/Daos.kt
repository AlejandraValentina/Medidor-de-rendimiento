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
}
@Dao interface WeightMeasurementDao {
    @Insert fun insert(entity: WeightMeasurementEntity)
    @Query("SELECT * FROM weight_measurements WHERE weightId = :id") fun get(id: String): WeightMeasurementEntity?
    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY recordedAtEpochMillis") fun list(profileId: String): List<WeightMeasurementEntity>
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
}
@Dao interface NutritionDiaryDayDao {
    @Upsert fun save(entity: NutritionDiaryDayEntity)
    @Query("SELECT * FROM nutrition_diary_days WHERE profileId = :profileId AND civilDayEpochDay = :day") fun get(profileId: String, day: Long): NutritionDiaryDayEntity?
    @Query("SELECT * FROM nutrition_diary_days WHERE profileId = :profileId ORDER BY civilDayEpochDay") fun list(profileId: String): List<NutritionDiaryDayEntity>
}
