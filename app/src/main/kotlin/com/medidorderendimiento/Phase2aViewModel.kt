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
        _state.value = Phase2aUiState(day, store.latestPlan(profileId), store.latestWeight(profileId), store.products(),
            store.entries(profileId, day), store.diary(profileId, day)?.state ?: DiaryClosureState.OPEN)
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

    class Factory(private val store: Phase2aStore, private val clock: ClockProvider) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = Phase2aViewModel(store, clock) as T
    }
}
