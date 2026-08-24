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
}
