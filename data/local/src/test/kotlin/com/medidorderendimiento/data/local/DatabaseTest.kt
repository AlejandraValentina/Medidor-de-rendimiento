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

    @Test fun `tdee history preserves canonical integers and current revision per civil day`() {
        fun estimate(id: String, revision: Long, energy: Long) = TdeeEstimate(LocalId(id), CivilDay.parse("2026-08-24"),
            TdeeEstimateKind.OBSERVATIONAL, EnergyAmount.ofMillicalories(energy), maturity = TdeeMaturity.ADAPTIVE,
            nutritionQuality = NutritionQuality(10, 12, 10, 100_000, 1, 0, 0, 800_000, DataQualityLabel.HIGH, emptySet()),
            weightConfidence = WeightTrendConfidence.HIGH, windowStart = CivilDay.parse("2026-08-01"),
            windowEnd = CivilDay.parse("2026-08-24"), algorithmVersion = "algorithm-v1", policyVersion = "tdee-v1",
            inputRevision = revision, evidenceKey = "e-$revision", revision = revision)
        database.tdeeEstimates().insert(estimate("one", 1, 2_400_000).toEntity(LocalId("profile")))
        database.tdeeEstimates().insert(estimate("two", 2, 2_410_000).copy(
            estimationReasons = setOf(TdeeEstimationReason.NON_POSITIVE_OBSERVATIONAL_RESULT)).toEntity(LocalId("profile")))
        assertEquals(2, database.tdeeEstimates().history("profile").size)
        val current = database.tdeeEstimates().currentHistory("profile").single().toDomain()
        assertEquals(2_410_000, current.centralEnergy?.millicalories)
        assertEquals(2, current.revision)
        assertNull(current.lowEnergy)
        assertNull(current.highEnergy)
        assertEquals(setOf(TdeeEstimationReason.NON_POSITIVE_OBSERVATIONAL_RESULT), current.estimationReasons)
    }


    @Test fun `retrospective correction prepares next revision before stability calculation`() {
        fun estimate(day: Int, energy: Long, revision: Long = 1, evidence: String = "e-$day") = TdeeEstimate(
            LocalId("t-$day-$revision"), CivilDay.parse("2026-08-${day.toString().padStart(2, '0')}"),
            TdeeEstimateKind.OBSERVATIONAL, EnergyAmount.ofKilocalories(energy), maturity = TdeeMaturity.ADAPTIVE,
            nutritionQuality = NutritionQuality(10, 10, 10, 0, 0, 0, 0, 1_000_000, DataQualityLabel.HIGH, emptySet()),
            weightConfidence = WeightTrendConfidence.HIGH, windowStart = CivilDay.parse("2026-07-01"),
            windowEnd = CivilDay.parse("2026-08-${day.toString().padStart(2, '0')}"), algorithmVersion = "a",
            policyVersion = "p", inputRevision = revision, evidenceKey = evidence, revision = revision)
        val store = Phase2aStore(database)
        (1..19 step 2).forEach { day -> database.tdeeEstimates().insert(estimate(day, 2_400).toEntity(LocalId("profile"))) }
        assertEquals(EstimatorStabilityStatus.STABLE, EstimatorStabilityCalculator().calculate(store.tdeeHistory(LocalId("profile"))).status)
        val corrected = estimate(19, 2_700, revision = 2, evidence = "corrected")
        val prepared = store.prepareTdee(LocalId("profile"), corrected)
        assertEquals(2, prepared.estimate.revision)
        assertEquals(10, prepared.currentHistory.size)
        assertEquals(2_700_000, prepared.currentHistory.single { it.referenceDay == corrected.referenceDay }.centralEnergy?.millicalories)
        val stability = EstimatorStabilityCalculator().calculate(prepared.currentHistory)
        store.saveTdee(LocalId("profile"), prepared, stability)
        assertEquals(2, database.tdeeEstimates().latestForDay("profile", corrected.referenceDay.value.toEpochDay())?.revision)
        assertEquals(10, stability.distinctEstimateDays)
        assertEquals(EstimatorStabilityStatus.UNSTABLE, stability.status)
    }

    @Test fun `retrospective day identifies only estimates whose windows are affected`() {
        fun entity(id: String, reference: String, start: String) = TdeeEstimate(LocalId(id), CivilDay.parse(reference),
            TdeeEstimateKind.OBSERVATIONAL, EnergyAmount.ofKilocalories(2_400), maturity = TdeeMaturity.ADAPTIVE,
            nutritionQuality = NutritionQuality(10, 10, 10, 0, 0, 0, 0, 1_000_000, DataQualityLabel.HIGH, emptySet()),
            weightConfidence = WeightTrendConfidence.HIGH, windowStart = CivilDay.parse(start), windowEnd = CivilDay.parse(reference),
            algorithmVersion = "a", policyVersion = "p", inputRevision = 1, evidenceKey = id).toEntity(LocalId("profile"))
        database.tdeeEstimates().insert(entity("affected", "2026-08-24", "2026-08-01"))
        database.tdeeEstimates().insert(entity("unaffected", "2026-09-24", "2026-09-01"))
        val store = Phase2aStore(database)
        assertEquals(listOf("affected"), store.affectedTdeeEstimates(LocalId("profile"), CivilDay.parse("2026-08-10")).map { it.id.value })
    }

    @Test fun `plan evaluation and decision memory round trip preserve layered decisions and latest revision`() {
        val plan = NutritionPlanVersion(LocalId("plan"), NutritionGoal.LOSS, EnergyAmount.ofKilocalories(2_000), null,
            TargetWeeklyRate.ofGrams(350), CivilDay.parse("2026-07-01"), acceptance = PlanAcceptance(Instant.EPOCH))
        database.nutritionPlans().insert(plan.toEntity(LocalId("profile")))
        val day = CivilDay.parse("2026-08-20")
        fun evaluation(id: String, revision: Long, effective: PlanDecision) = PlanEvaluation(LocalId(id), LocalId("profile"), day,
            LocalId("plan"), PlanDecision.ADJUST_DOWN, effective, PlanDecision.OBSERVE, DecisionAuthorization.OBSERVE_ONLY,
            SafetyStatus.CAUTION, setOf(PlanEvaluationReason.SAFETY_CAUTION), null, -80, "policy", "e-$revision", revision, revision)
        database.planEvaluations().insert(evaluation("one",1,PlanDecision.OBSERVE).toEntity())
        database.planEvaluations().insert(evaluation("two",2,PlanDecision.MAINTAIN).toEntity())
        assertEquals(PlanDecision.MAINTAIN, database.planEvaluations().currentHistory("profile").single().toDomain().effectiveDecision)
        val memory = DecisionStateMemory(LocalId("profile"), LocalId("plan"), "policy", day, "e-2", PlanDecision.ADJUST_DOWN,
            1, day, PlanDecision.MAINTAIN, 2)
        database.decisionStateMemory().save(memory.toEntity())
        assertEquals(memory, database.decisionStateMemory().get("plan")?.toDomain())
    }
}
