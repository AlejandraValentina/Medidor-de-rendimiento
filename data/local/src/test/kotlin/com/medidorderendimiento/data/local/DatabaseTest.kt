package com.medidorderendimiento.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medidorderendimiento.domain.*
import java.time.Instant
import kotlin.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {
    private lateinit var database: PerformanceDatabase
    private val profile = UserProfileEntity("profile", null, null, null, "DEVICE_ZONE", 0)

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PerformanceDatabase::class.java)
            .allowMainThreadQueries().build()
        database.userProfiles().save(profile)
    }
    @After fun tearDown() = database.close()

    @Test fun `six current tables insert and retrieve entities`() {
        val product = StoredFoodProduct(FoodProduct(LocalId("food"), "Food"), NutritionFacts(null, null, null, null)).toEntity()
        database.foodProducts().save(product)
        val plan = NutritionPlanVersion(LocalId("plan"), NutritionGoal.MAINTENANCE, EnergyAmount.ofKilocalories(2000), null,
            null, CivilDay.of(2026, 1, 1), acceptance = PlanAcceptance(Instant.EPOCH)).toEntity(LocalId("profile"))
        database.nutritionPlans().insert(plan)
        val weight = WeightMeasurement(LocalId("weight"), BodyMass.ofGrams(60_000), Instant.EPOCH,
            CivilDay.of(2026, 1, 1)).toEntity(LocalId("profile"))
        database.weights().insert(weight)
        val entry = FoodEntry(LocalId("entry"), product.toDomain().product, Quantity.Mass.ofGrams(10),
            NutritionFacts(null, null, null, null), QuantityNature.DECLARED, Instant.EPOCH, CivilDay.of(2026, 1, 1)).toEntity(LocalId("profile"))
        database.foodEntries().insert(entry)
        val diary = NutritionDiaryDayEntity("profile", 20_454, DiaryClosureState.OPEN.name, null, 0, 1, null)
        database.diaryDays().save(diary)

        assertEquals(profile, database.userProfiles().get("profile"))
        assertEquals(plan, database.nutritionPlans().get("plan"))
        assertEquals(weight, database.weights().get("weight"))
        assertEquals(product, database.foodProducts().get("food"))
        assertEquals(entry, database.foodEntries().get("entry"))
        assertEquals(diary, database.diaryDays().get("profile", 20_454))
    }

    @Test fun `missing weight and diary remain absent`() {
        assertTrue(database.weights().list("profile").isEmpty())
        assertNull(database.diaryDays().get("profile", 1))
    }

    @Test fun `foreign keys reject orphan observations`() {
        val orphan = WeightMeasurementEntity("w", "missing", "w", 0, 0, 1, "MANUAL", 1)
        assertFails { database.weights().insert(orphan) }
    }

    @Test fun `favorites can be added listed and removed without deleting product or history`() {
        val product = StoredFoodProduct(FoodProduct(LocalId("food"), "Food"), NutritionFacts(null, null, null, null)).toEntity()
        database.foodProducts().save(product)
        database.foodEntries().insert(FoodEntry(LocalId("entry"), product.toDomain().product, Quantity.Mass.ofGrams(1),
            NutritionFacts(null, null, null, null), QuantityNature.DECLARED, Instant.EPOCH, CivilDay.of(2026, 1, 1)).toEntity(LocalId("profile")))
        database.favorites().save(FavoriteFoodEntity("favorite", "profile", "food", 50_000, "MASS_MG", 10))
        assertEquals("food", database.favorites().list("profile").single().productId)
        assertEquals(1, database.favorites().remove("profile", "food"))
        assertNotNull(database.foodProducts().get("food"))
        assertNotNull(database.foodEntries().get("entry"))
    }

    @Test fun `recents are distinct and ordered by last real consumption`() {
        val a = StoredFoodProduct(FoodProduct(LocalId("a"), "A"), NutritionFacts(null, null, null, null)).toEntity()
        val b = StoredFoodProduct(FoodProduct(LocalId("b"), "B"), NutritionFacts(null, null, null, null)).toEntity()
        database.foodProducts().save(a); database.foodProducts().save(b)
        fun entry(id: String, product: FoodProduct, at: Long) = FoodEntry(LocalId(id), product, Quantity.Mass.ofGrams(1),
            NutritionFacts(null, null, null, null), QuantityNature.DECLARED, Instant.ofEpochMilli(at), CivilDay.of(2026, 1, 1)).toEntity(LocalId("profile"))
        database.foodEntries().insert(entry("a1", a.toDomain().product, 1))
        database.foodEntries().insert(entry("b1", b.toDomain().product, 2))
        database.foodEntries().insert(entry("a2", a.toDomain().product, 3))
        assertEquals(listOf("a", "b"), database.foodEntries().recentProducts("profile", 8).map { it.productId })
    }

    @Test fun `saved meal preserves quantity types and deleting it leaves historical entry`() {
        val product = StoredFoodProduct(FoodProduct(LocalId("food"), "Food"), NutritionFacts(EnergyAmount.ofMillicalories(0), null, null, null)).toEntity()
        database.foodProducts().save(product)
        database.savedMeals().insert(SavedMealEntity("meal", "profile", "Meal", 0, 0, null))
        database.savedMeals().insertItems(listOf(
            SavedMealItemEntity("grams", "meal", "food", 10_000, "MASS_MG", 0),
            SavedMealItemEntity("milliliters", "meal", "food", 20_000, "VOLUME_UL", 1),
            SavedMealItemEntity("units", "meal", "food", 3_000, "UNITS_THOUSANDTHS", 2),
            SavedMealItemEntity("portions", "meal", "food", 4_000, "PORTIONS_THOUSANDTHS", 3)))
        assertEquals(listOf("MASS_MG", "VOLUME_UL", "UNITS_THOUSANDTHS", "PORTIONS_THOUSANDTHS"),
            database.savedMeals().items("meal").map { it.quantityUnit })
        database.foodEntries().insert(FoodEntry(LocalId("history"), product.toDomain().product, Quantity.Mass.ofGrams(10),
            NutritionFacts(EnergyAmount.ofMillicalories(0), null, null, null), QuantityNature.DECLARED, Instant.EPOCH,
            CivilDay.of(2026, 1, 1)).toEntity(LocalId("profile")))
        database.savedMeals().delete("meal")
        assertNotNull(database.foodEntries().get("history"))
    }
}
