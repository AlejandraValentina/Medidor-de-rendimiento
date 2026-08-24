package com.medidorderendimiento.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val profileId: String,
    val displayName: String?,
    val heightMm: Long?,
    val birthYear: Int?,
    val timezonePolicy: String,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "nutrition_plan_versions",
    foreignKeys = [ForeignKey(entity = UserProfileEntity::class, parentColumns = ["profileId"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId", "validFromEpochDay")],
)
data class NutritionPlanVersionEntity(
    @PrimaryKey val planVersionId: String,
    val profileId: String,
    val objectiveKind: String,
    val baseEnergyMillicalories: Long,
    val proteinTargetMilligrams: Long?,
    val rateTargetGrams: Long?,
    val validFromEpochDay: Long,
    val validUntilEpochDay: Long?,
    val acceptedAtEpochMillis: Long,
)

@Entity(
    tableName = "weight_measurements",
    foreignKeys = [ForeignKey(entity = UserProfileEntity::class, parentColumns = ["profileId"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId", "recordedAtEpochMillis"), Index("profileId", "civilDayEpochDay"), Index(value = ["logicalWeightId", "revision"], unique = true)],
)
data class WeightMeasurementEntity(
    @PrimaryKey val weightId: String,
    val profileId: String,
    val logicalWeightId: String,
    val recordedAtEpochMillis: Long,
    val civilDayEpochDay: Long,
    val bodyMassGrams: Long,
    val sourceKind: String,
    val revision: Long,
)

@Entity(tableName = "food_products", indices = [Index("normalizedName")])
data class FoodProductEntity(
    @PrimaryKey val productId: String,
    val displayName: String,
    val normalizedName: String,
    val energyMillicalories: Long?,
    val proteinMilligrams: Long?,
    val carbohydratesMilligrams: Long?,
    val fatMilligrams: Long?,
    val revision: Long,
    val basisQuantityValue: Long = 100_000,
    val basisQuantityUnit: String = "MASS_MG",
)

@Entity(
    tableName = "food_entries",
    foreignKeys = [
        ForeignKey(entity = UserProfileEntity::class, parentColumns = ["profileId"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FoodProductEntity::class, parentColumns = ["productId"], childColumns = ["productId"]),
    ],
    indices = [Index("profileId", "civilDayEpochDay"), Index("productId"), Index(value = ["logicalEntryId", "revision"], unique = true)],
)
data class FoodEntryEntity(
    @PrimaryKey val foodEntryId: String,
    val profileId: String,
    val logicalEntryId: String,
    val productId: String,
    val recordedAtEpochMillis: Long,
    val civilDayEpochDay: Long,
    val quantityValue: Long,
    val quantityUnit: String,
    val energyMillicalories: Long?,
    val proteinMilligrams: Long?,
    val carbohydratesMilligrams: Long?,
    val fatMilligrams: Long?,
    val quantityNature: String,
    val sourceKind: String,
    val revision: Long,
    val confirmationStatus: String = "CONFIRMED",
    val nutrientNature: String = "DECLARED",
)

enum class DiaryClosureState { OPEN, CLOSED_CONFIRMED, CLOSED_WITH_ESTIMATES, CLOSED_INCOMPLETE, EXCLUDED_CONTEXT, ZERO_INTAKE_CONFIRMED }

@Entity(
    tableName = "nutrition_diary_days",
    primaryKeys = ["profileId", "civilDayEpochDay"],
    foreignKeys = [ForeignKey(entity = UserProfileEntity::class, parentColumns = ["profileId"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["profileId", "civilDayEpochDay"], unique = true)],
)
data class NutritionDiaryDayEntity(
    val profileId: String,
    val civilDayEpochDay: Long,
    val closureState: String,
    val closedAtEpochMillis: Long?,
    val updatedAtEpochMillis: Long,
    val closureRevision: Long,
    val exclusionReason: String?,
)

@Entity(tableName = "favorite_foods", foreignKeys = [
    ForeignKey(entity = UserProfileEntity::class, parentColumns = ["profileId"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = FoodProductEntity::class, parentColumns = ["productId"], childColumns = ["productId"]),
], indices = [Index("profileId"), Index("productId"), Index(value = ["profileId", "productId"], unique = true)])
data class FavoriteFoodEntity(@PrimaryKey val favoriteId: String, val profileId: String, val productId: String,
    val preferredQuantityValue: Long, val preferredQuantityUnit: String, val lastUsedAtEpochMillis: Long)

@Entity(tableName = "saved_meals", foreignKeys = [
    ForeignKey(entity = UserProfileEntity::class, parentColumns = ["profileId"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
], indices = [Index("profileId", "updatedAtEpochMillis")])
data class SavedMealEntity(@PrimaryKey val savedMealId: String, val profileId: String, val name: String,
    val createdAtEpochMillis: Long, val updatedAtEpochMillis: Long, val archivedAtEpochMillis: Long?)

@Entity(tableName = "saved_meal_items", foreignKeys = [
    ForeignKey(entity = SavedMealEntity::class, parentColumns = ["savedMealId"], childColumns = ["savedMealId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = FoodProductEntity::class, parentColumns = ["productId"], childColumns = ["productId"]),
], indices = [Index("productId"), Index(value = ["savedMealId", "ordering"], unique = true)])
data class SavedMealItemEntity(@PrimaryKey val savedMealItemId: String, val savedMealId: String, val productId: String,
    val quantityValue: Long, val quantityUnit: String, val ordering: Int)
