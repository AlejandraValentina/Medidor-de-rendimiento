package com.medidorderendimiento.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserProfileEntity::class, NutritionPlanVersionEntity::class, WeightMeasurementEntity::class,
    FoodProductEntity::class, FoodEntryEntity::class, NutritionDiaryDayEntity::class], version = 1, exportSchema = false)
abstract class PerformanceDatabase : RoomDatabase() {
    abstract fun userProfiles(): UserProfileDao
    abstract fun nutritionPlans(): NutritionPlanDao
    abstract fun weights(): WeightMeasurementDao
    abstract fun foodProducts(): FoodProductDao
    abstract fun foodEntries(): FoodEntryDao
    abstract fun diaryDays(): NutritionDiaryDayDao
}
