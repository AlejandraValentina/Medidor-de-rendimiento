package com.medidorderendimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medidorderendimiento.data.local.*
import com.medidorderendimiento.domain.*
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Phase2aViewModel(
    private val store: Phase2aStore,
    private val clock: ClockProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val weightTrendCalculator = WeightTrendCalculator()
    private val tdeeEstimator = TdeeEstimator()
    private val stabilityCalculator = EstimatorStabilityCalculator()
    private val profileId = LocalId("local-profile")
    private val _state = MutableStateFlow(Phase2aUiState())
    val state: StateFlow<Phase2aUiState> = _state

    init { refresh() }

    private fun today(): CivilDay {
        val date = clock.now().atZone(zoneId).toLocalDate()
        return CivilDay.of(date.year, date.monthValue, date.dayOfMonth)
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        store.ensureProfile(profileId, clock.now())
        val day = today()
        val weights = store.weights(profileId)
        val trend = weightTrendCalculator.calculate(weights.map(::WeightObservation), day)
        val windowStart = CivilDay.parse(day.value.minusDays(27).toString())
        val nutritionDays = store.tdeeNutritionDays(profileId, windowStart, day)
        val evidenceKey = nutritionDays.joinToString("|") {
            "${it.civilDay.value}:${it.state}:${it.actualEnergy?.millicalories}:${it.estimatedEnergy?.millicalories}:${it.pendingEntries}:${it.unknownEnergyEntries}:${it.planVersionId?.value}"
        } + "|weight:${trend.weeklyRateGrams}:${trend.coverage.distinctDays}:${trend.coverage.spanDays}"
        val estimate = tdeeEstimator.estimate(LocalId(UUID.randomUUID().toString()), day, windowStart, nutritionDays,
            trend, 1, evidenceKey)
        val previous = store.tdeeHistory(profileId)
        val stability = stabilityCalculator.calculate(previous + estimate)
        val persistedEstimate = store.saveTdee(profileId, estimate.copy(stabilityStatus = stability.status), stability)
        _state.value = Phase2aUiState(
            civilDay = day,
            plan = store.latestPlan(profileId),
            latestWeight = weights.maxByOrNull(WeightMeasurement::recordedAt),
            weightTrend = trend,
            tdeeEstimate = persistedEstimate,
            estimatorStability = stability,
            products = store.products(),
            entries = store.entries(profileId, day),
            diaryState = store.diary(profileId, day)?.state ?: DiaryClosureState.OPEN,
            favorites = store.favorites(profileId),
            recentProducts = store.recentProducts(profileId),
            savedMeals = store.savedMeals(profileId),
        )
    }

    fun addPlan(goal: NutritionGoal, energyKcal: Long, proteinGrams: Long?, rateGrams: Long?) = viewModelScope.launch(Dispatchers.IO) {
        val plan = NutritionPlanVersion(LocalId(UUID.randomUUID().toString()), goal, EnergyAmount.ofKilocalories(energyKcal),
            proteinGrams?.let(NutrientAmount::ofGrams), rateGrams?.let(TargetWeeklyRate::ofGrams), today(),
            acceptance = PlanAcceptance(clock.now()))
        store.addPlan(profileId, plan); refresh()
    }

    fun addWeight(inputKg: String): Boolean {
        val mass = parseKilograms(inputKg) ?: return false
        viewModelScope.launch(Dispatchers.IO) {
            store.addWeight(profileId, WeightMeasurement(LocalId(UUID.randomUUID().toString()), mass, clock.now(), today()))
            refresh()
        }
        return true
    }

    fun addProduct(name: String, basisUnit: FoodUnit, energyKcal: Long?, proteinGrams: Long?, carbsGrams: Long?, fatGrams: Long?) =
        viewModelScope.launch(Dispatchers.IO) {
            val facts = NutritionFacts(energyKcal?.let(EnergyAmount::ofKilocalories), proteinGrams?.let(NutrientAmount::ofGrams),
                carbsGrams?.let(NutrientAmount::ofGrams), fatGrams?.let(NutrientAmount::ofGrams))
            val basis = quantityOf(basisUnit, if (basisUnit == FoodUnit.GRAMS || basisUnit == FoodUnit.MILLILITERS) 100 else 1)
            store.saveProduct(StoredFoodProduct(FoodProduct(LocalId(UUID.randomUUID().toString()), name), facts, basis))
            refresh()
        }

    fun addConsumption(product: StoredFoodProduct, amount: Long, estimated: Boolean, pending: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val quantity = when (product.basisQuantity) {
            is Quantity.Mass -> Quantity.Mass.ofGrams(amount)
            is Quantity.Volume -> Quantity.Volume.ofMilliliters(amount)
            is Quantity.Units -> Quantity.Units.ofWholeUnits(amount)
            is Quantity.Portions -> Quantity.Portions.ofWholePortions(amount)
        }
        val facts = scaleNutrition(product.nutrition, product.basisQuantity, quantity)
        store.addEntry(profileId, FoodEntry(LocalId(UUID.randomUUID().toString()), product.product, quantity, facts,
            QuantityNature.DECLARED, clock.now(), today(), confirmation = if (pending) EntryConfirmation.PENDING else EntryConfirmation.CONFIRMED,
            nutrientNature = if (estimated) NutrientNature.ESTIMATED else NutrientNature.DECLARED))
        refresh()
    }

    fun setDiaryState(value: DiaryClosureState) = viewModelScope.launch(Dispatchers.IO) {
        val now = clock.now()
        store.saveDiary(StoredDiaryDay(profileId, today(), value, if (value == DiaryClosureState.OPEN) null else now,
            now, 1, null)); refresh()
    }

    fun addFavorite(product: StoredFoodProduct, amount: Long) = viewModelScope.launch(Dispatchers.IO) {
        store.saveFavorite(profileId, LocalId(UUID.randomUUID().toString()), product, quantityFor(product, amount), clock.now()); refresh()
    }
    fun removeFavorite(product: StoredFoodProduct) = viewModelScope.launch(Dispatchers.IO) {
        store.removeFavorite(profileId, product.product.id); refresh()
    }
    fun consumeFavorite(favorite: Phase2aStore.FavoriteFood) = viewModelScope.launch(Dispatchers.IO) {
        addEntry(favorite.product, favorite.preferredQuantity); refresh()
    }
    fun saveMeal(name: String, selections: List<Pair<StoredFoodProduct, Long>>) = viewModelScope.launch(Dispatchers.IO) {
        store.saveMeal(profileId, LocalId(UUID.randomUUID().toString()), name,
            selections.map { (product, amount) -> product to quantityFor(product, amount) }, clock.now()); refresh()
    }
    fun consumeMeal(meal: Phase2aStore.SavedMeal, amounts: Map<LocalId, Long> = emptyMap()) = viewModelScope.launch(Dispatchers.IO) {
        meal.items.forEach { item -> addEntry(item.product, amounts[item.id]?.let { quantityFor(item.product, it) } ?: item.quantity) }
        refresh()
    }
    fun deleteMeal(meal: Phase2aStore.SavedMeal) = viewModelScope.launch(Dispatchers.IO) { store.deleteMeal(meal.id); refresh() }

    private fun quantityFor(product: StoredFoodProduct, amount: Long): Quantity = when (product.basisQuantity) {
        is Quantity.Mass -> Quantity.Mass.ofGrams(amount)
        is Quantity.Volume -> Quantity.Volume.ofMilliliters(amount)
        is Quantity.Units -> Quantity.Units.ofWholeUnits(amount)
        is Quantity.Portions -> Quantity.Portions.ofWholePortions(amount)
    }
    private fun addEntry(product: StoredFoodProduct, quantity: Quantity) {
        store.addEntry(profileId, FoodEntry(LocalId(UUID.randomUUID().toString()), product.product, quantity,
            scaleNutrition(product.nutrition, product.basisQuantity, quantity), QuantityNature.DECLARED, clock.now(), today()))
    }

    class Factory(private val store: Phase2aStore, private val clock: ClockProvider) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = Phase2aViewModel(store, clock) as T
    }
}
