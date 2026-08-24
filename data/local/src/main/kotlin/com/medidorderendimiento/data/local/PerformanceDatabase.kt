package com.medidorderendimiento.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserProfileEntity::class, NutritionPlanVersionEntity::class, WeightMeasurementEntity::class,
    FoodProductEntity::class, FoodEntryEntity::class, NutritionDiaryDayEntity::class, FavoriteFoodEntity::class,
    SavedMealEntity::class, SavedMealItemEntity::class, TdeeEstimateEntity::class], version = 3, exportSchema = false)
abstract class PerformanceDatabase : RoomDatabase() {
    abstract fun userProfiles(): UserProfileDao
    abstract fun nutritionPlans(): NutritionPlanDao
    abstract fun weights(): WeightMeasurementDao
    abstract fun foodProducts(): FoodProductDao
    abstract fun foodEntries(): FoodEntryDao
    abstract fun diaryDays(): NutritionDiaryDayDao
    abstract fun favorites(): FavoriteFoodDao
    abstract fun savedMeals(): SavedMealDao
    abstract fun tdeeEstimates(): TdeeEstimateDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorite_foods` (`favoriteId` TEXT NOT NULL, `profileId` TEXT NOT NULL, `productId` TEXT NOT NULL, `preferredQuantityValue` INTEGER NOT NULL, `preferredQuantityUnit` TEXT NOT NULL, `lastUsedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`favoriteId`), FOREIGN KEY(`profileId`) REFERENCES `user_profiles`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`productId`) REFERENCES `food_products`(`productId`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_foods_profileId` ON `favorite_foods` (`profileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_foods_productId` ON `favorite_foods` (`productId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_foods_profileId_productId` ON `favorite_foods` (`profileId`, `productId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `saved_meals` (`savedMealId` TEXT NOT NULL, `profileId` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, `archivedAtEpochMillis` INTEGER, PRIMARY KEY(`savedMealId`), FOREIGN KEY(`profileId`) REFERENCES `user_profiles`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_meals_profileId_updatedAtEpochMillis` ON `saved_meals` (`profileId`, `updatedAtEpochMillis`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `saved_meal_items` (`savedMealItemId` TEXT NOT NULL, `savedMealId` TEXT NOT NULL, `productId` TEXT NOT NULL, `quantityValue` INTEGER NOT NULL, `quantityUnit` TEXT NOT NULL, `ordering` INTEGER NOT NULL, PRIMARY KEY(`savedMealItemId`), FOREIGN KEY(`savedMealId`) REFERENCES `saved_meals`(`savedMealId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`productId`) REFERENCES `food_products`(`productId`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_meal_items_productId` ON `saved_meal_items` (`productId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_meal_items_savedMealId_ordering` ON `saved_meal_items` (`savedMealId`, `ordering`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tdee_estimates` (`tdeeId` TEXT NOT NULL, `profileId` TEXT NOT NULL, `referenceDayEpochDay` INTEGER NOT NULL, `estimateKind` TEXT NOT NULL, `centralEnergyMillicalories` INTEGER, `lowEnergyMillicalories` INTEGER, `highEnergyMillicalories` INTEGER, `maturity` TEXT NOT NULL, `qualityLabel` TEXT NOT NULL, `qualityIndexPermillion` INTEGER NOT NULL, `requiredNutritionDays` INTEGER NOT NULL, `candidateNutritionDays` INTEGER NOT NULL, `eligibleNutritionDays` INTEGER NOT NULL, `estimatedEnergyPermillion` INTEGER NOT NULL, `excludedNutritionDays` INTEGER NOT NULL, `pendingEntries` INTEGER NOT NULL, `unknownEnergyEntries` INTEGER NOT NULL, `qualityReasons` TEXT NOT NULL, `weightConfidence` TEXT NOT NULL, `stabilityStatus` TEXT NOT NULL, `relativeMadPermillion` INTEGER, `peakToPeakPermillion` INTEGER, `periodDriftPermillion` INTEGER, `windowStartEpochDay` INTEGER NOT NULL, `windowEndEpochDay` INTEGER NOT NULL, `algorithmVersion` TEXT NOT NULL, `policyVersion` TEXT NOT NULL, `inputRevision` INTEGER NOT NULL, `evidenceKey` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`tdeeId`), FOREIGN KEY(`profileId`) REFERENCES `user_profiles`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tdee_estimates_profileId_referenceDayEpochDay` ON `tdee_estimates` (`profileId`, `referenceDayEpochDay`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tdee_estimates_profileId_referenceDayEpochDay_revision` ON `tdee_estimates` (`profileId`, `referenceDayEpochDay`, `revision`)")
    }
}

fun createPerformanceDatabase(context: Context): PerformanceDatabase =
    Room.databaseBuilder(context.applicationContext, PerformanceDatabase::class.java, "performance.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
