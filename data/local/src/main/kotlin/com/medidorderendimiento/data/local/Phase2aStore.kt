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

    fun tdeeNutritionDays(profileId: LocalId, start: CivilDay, end: CivilDay): List<TdeeNutritionDay> {
        val entries = database.foodEntries().listForRange(profileId.value, start.toEpochDay(), end.toEpochDay())
            .groupBy { it.civilDayEpochDay }
        val plans = database.nutritionPlans().list(profileId.value)
        return database.diaryDays().list(profileId.value)
            .filter { it.civilDayEpochDay in start.toEpochDay()..end.toEpochDay() }
            .map { diary ->
                val dayEntries = entries[diary.civilDayEpochDay].orEmpty()
                    .filter { it.confirmationStatus == EntryConfirmation.CONFIRMED.name }
                fun energy(nature: NutrientNature): EnergyAmount? {
                    val matching = dayEntries.filter { it.nutrientNature == nature.name }
                    if (matching.isEmpty() || matching.any { it.energyMillicalories == null }) return null
                    return EnergyAmount.ofMillicalories(matching.fold(0L) { sum, entry ->
                        Math.addExact(sum, requireNotNull(entry.energyMillicalories))
                    })
                }
                val activePlan = plans.lastOrNull { plan -> plan.validFromEpochDay <= diary.civilDayEpochDay &&
                    (plan.validUntilEpochDay == null || diary.civilDayEpochDay <= plan.validUntilEpochDay) }
                val sourceRevision = maxOf(diary.closureRevision, entries[diary.civilDayEpochDay].orEmpty().maxOfOrNull { it.revision } ?: 1)
                TdeeNutritionDay(diary.civilDayEpochDay.toCivilDay(), TdeeDiaryState.valueOf(diary.closureState),
                    energy(NutrientNature.DECLARED), energy(NutrientNature.ESTIMATED),
                    entries[diary.civilDayEpochDay].orEmpty().count { it.confirmationStatus == EntryConfirmation.PENDING.name },
                    dayEntries.count { it.energyMillicalories == null }, activePlan?.planVersionId?.let(::LocalId), sourceRevision)
            }
    }

    fun tdeeHistory(profileId: LocalId): List<TdeeEstimate> = database.tdeeEstimates().currentHistory(profileId.value).map(TdeeEstimateEntity::toDomain)

    fun affectedTdeeEstimates(profileId: LocalId, editedDay: CivilDay): List<TdeeEstimate> =
        tdeeHistory(profileId).filter { editedDay in it.windowStart..it.windowEnd }

    data class PreparedTdee(val estimate: TdeeEstimate, val currentHistory: List<TdeeEstimate>, val needsInsert: Boolean)

    fun prepareTdee(profileId: LocalId, estimate: TdeeEstimate): PreparedTdee {
        val history = tdeeHistory(profileId)
        val current = database.tdeeEstimates().latestForDay(profileId.value, estimate.referenceDay.toEpochDay())
        if (current?.evidenceKey == estimate.evidenceKey && current.inputRevision == estimate.inputRevision) {
            return PreparedTdee(current.toDomain(), history, false)
        }
        val revision = (current?.revision ?: 0) + 1
        val revised = estimate.copy(revision = revision)
        return PreparedTdee(revised, history.filterNot { it.referenceDay == revised.referenceDay } + revised, true)
    }

    fun saveTdee(profileId: LocalId, prepared: PreparedTdee, stability: EstimatorStability): TdeeEstimate {
        if (prepared.needsInsert) database.tdeeEstimates().insert(prepared.estimate.toEntity(profileId, stability))
        return prepared.estimate
    }

    fun evaluationMemory(planVersionId: LocalId): DecisionStateMemory? = database.decisionStateMemory().get(planVersionId.value)?.toDomain()
    fun currentEvaluations(profileId: LocalId): List<PlanEvaluation> = database.planEvaluations().currentHistory(profileId.value).map(PlanEvaluationEntity::toDomain)
    fun hasRetrospectiveEvaluationRevision(profileId: LocalId): Boolean =
        database.planEvaluations().history(profileId.value).any { it.revision > 1 }
    fun shadowReplayItems(profileId: LocalId, planVersionId: LocalId? = null, start: CivilDay? = null, end: CivilDay? = null): List<ShadowReplayItem> =
        currentEvaluations(profileId).filter { evaluation ->
            (planVersionId == null || evaluation.planVersionId == planVersionId) &&
                (start == null || evaluation.referenceDay >= start) && (end == null || evaluation.referenceDay <= end)
        }.mapNotNull { evaluation ->
        val plan = evaluation.planVersionId?.let { database.nutritionPlans().get(it.value)?.toDomain() } ?: return@mapNotNull null
        val tdee = evaluation.tdeeEstimateId?.let { database.tdeeEstimates().get(it.value)?.toDomain() }
        val trend = WeightTrend(evaluation.referenceDay, null, null, evaluation.observedWeeklyRateGrams, null,
            emptyList(), emptyList(), emptyList(), WeightTrendCoverage(evaluation.weightDistinctDays,
                evaluation.weightSpanDays, evaluation.weightMaximumGapDays), evaluation.weightConfidence, emptySet())
        val stability = EstimatorStability(evaluation.estimatorStabilityStatus, 0, 0, null, null, null, null, 0,
            emptySet(), evaluation.estimatorStabilityPolicyVersion.orEmpty())
        ShadowReplayItem(PlanEvaluatorInput(evaluation.id, profileId, evaluation.referenceDay, plan, trend, tdee,
            stability, evaluation.safetyStatus, evaluation.inputRevision, evaluation.evaluationMode), evaluation)
    }
    fun saveEvaluation(evaluation: PlanEvaluation, memory: DecisionStateMemory?) {
        val current = database.planEvaluations().latestForDay(evaluation.profileId.value, evaluation.referenceDay.toEpochDay())
        if (current?.evidenceKey == evaluation.evidenceKey && current.inputRevision == evaluation.inputRevision) return
        database.runInTransaction {
            val provenance = when {
                current?.prospectiveObserved == true -> true
                evaluation.prospectiveObserved != null -> evaluation.prospectiveObserved
                else -> current?.prospectiveObserved
            }
            database.planEvaluations().insert(evaluation.copy(revision = (current?.revision ?: 0) + 1,
                prospectiveObserved = provenance).toEntity())
            val rebuilt = DecisionStateMemoryRebuilder.rebuild(
                database.planEvaluations().currentHistory(evaluation.profileId.value).map(PlanEvaluationEntity::toDomain))
            (rebuilt ?: memory)?.let { database.decisionStateMemory().save(it.toEntity()) }
        }
    }
    fun rebuildDecisionMemory(profileId: LocalId): DecisionStateMemory? {
        val rebuilt = DecisionStateMemoryRebuilder.rebuild(currentEvaluations(profileId))
        rebuilt?.let { database.decisionStateMemory().save(it.toEntity()) }
        return rebuilt
    }
}
