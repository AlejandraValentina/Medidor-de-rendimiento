package com.medidorderendimiento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medidorderendimiento.data.local.*
import com.medidorderendimiento.domain.*
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = createPerformanceDatabase(applicationContext)
        val factory = Phase2aViewModel.Factory(Phase2aStore(database)) { Instant.now() }
        setContent { MaterialTheme { Phase2aScreen(viewModel(factory = factory)) } }
    }
}

@Composable
private fun Phase2aScreen(viewModel: Phase2aViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Medidor de rendimiento", style = MaterialTheme.typography.headlineSmall)
        Panel(state)
        PlanForm(viewModel)
        WeightForm(viewModel)
        ProductForm(viewModel)
        ConsumptionForm(state, viewModel)
        Text("DIARIO — ${state.diaryState}", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { viewModel.setDiaryState(DiaryClosureState.OPEN) }) { Text("Abrir") }
            Button(onClick = { viewModel.setDiaryState(DiaryClosureState.CLOSED_CONFIRMED) }) { Text("Cerrar") }
        }
        DiaryClosureState.entries.filter { it !in setOf(DiaryClosureState.OPEN, DiaryClosureState.CLOSED_CONFIRMED) }.forEach {
            TextButton(onClick = { viewModel.setDiaryState(it) }) { Text(it.name) }
        }
    }
}

@Composable private fun Panel(state: Phase2aUiState) {
    val plan = state.plan
    Text("PLAN", style = MaterialTheme.typography.titleMedium)
    Text(if (plan == null) "Sin plan registrado" else "${plan.goal}: ${plan.baseDailyEnergy.kcal()} kcal · proteína ${plan.proteinTarget?.grams() ?: "desconocida"}")
    Text("HOY", style = MaterialTheme.typography.titleMedium)
    Text("Plan base: ${plan?.baseDailyEnergy?.kcal() ?: "desconocido"} kcal")
    Text("Recomendado hoy: ${state.recommendedToday?.kcal() ?: "desconocido"} kcal")
    Text("Consumido confirmado: ${state.summary.confirmedEnergy?.kcal() ?: "desconocido"} kcal")
    Text("Consumido estimado: ${state.summary.estimatedEnergy?.kcal() ?: "desconocido"} kcal")
    Text("Pendiente: ${state.summary.pendingEnergy?.kcal() ?: "desconocido"} kcal")
    Text("Proteína: ${state.summary.protein?.grams() ?: "desconocida"}")
    Text("Restante: ${state.remaining?.kcal() ?: "no calculable"} kcal")
    Text("PESO", style = MaterialTheme.typography.titleMedium)
    Text(state.latestWeight?.let { "${it.mass.kilogramsForDisplay()} kg" } ?: "Sin pesaje registrado")
    Text("DIARIO — ${state.civilDay?.value ?: "hoy"}: ${state.diaryState}")
}

@Composable private fun PlanForm(vm: Phase2aViewModel) {
    var energy by remember { mutableStateOf("") }; var protein by remember { mutableStateOf("") }; var rate by remember { mutableStateOf("") }; var goal by remember { mutableStateOf(NutritionGoal.MAINTENANCE) }
    Text("Nuevo plan", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(energy, { energy = it }, label = { Text("Energía kcal") })
    OutlinedTextField(protein, { protein = it }, label = { Text("Proteína g (opcional)") })
    if (goal == NutritionGoal.LOSS || goal == NutritionGoal.GAIN) OutlinedTextField(rate, { rate = it }, label = { Text("Ritmo semanal g (opcional)") })
    TextButton(onClick = { goal = NutritionGoal.entries[(goal.ordinal + 1) % NutritionGoal.entries.size] }) { Text("Objetivo: $goal") }
    Button(onClick = { energy.toLongOrNull()?.let { vm.addPlan(goal, it, protein.toLongOrNull(), rate.toLongOrNull()) } }) { Text("Crear versión") }
}

@Composable private fun WeightForm(vm: Phase2aViewModel) {
    var value by remember { mutableStateOf("") }; Text("Registrar peso", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(value, { value = it }, label = { Text("kg") }); Button(onClick = { vm.addWeight(value) }) { Text("Guardar peso") }
}

@Composable private fun ProductForm(vm: Phase2aViewModel) {
    var name by remember { mutableStateOf("") }; var energy by remember { mutableStateOf("") }; var protein by remember { mutableStateOf("") }; var carbs by remember { mutableStateOf("") }; var fat by remember { mutableStateOf("") }; var unit by remember { mutableStateOf(FoodUnit.GRAMS) }
    Text("Producto manual", style = MaterialTheme.typography.titleMedium)
    TextButton(onClick = { unit = FoodUnit.entries[(unit.ordinal + 1) % FoodUnit.entries.size] }) { Text("Base: $unit") }
    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }); OutlinedTextField(energy, { energy = it }, label = { Text("kcal (vacío = desconocido)") })
    OutlinedTextField(protein, { protein = it }, label = { Text("proteína g (opcional)") })
    OutlinedTextField(carbs, { carbs = it }, label = { Text("carbohidratos g (opcional)") })
    OutlinedTextField(fat, { fat = it }, label = { Text("grasa g (opcional)") })
    Button(onClick = { if (name.isNotBlank()) vm.addProduct(name, unit, energy.toLongOrNull(), protein.toLongOrNull(), carbs.toLongOrNull(), fat.toLongOrNull()) }) { Text("Crear producto") }
}

@Composable private fun ConsumptionForm(state: Phase2aUiState, vm: Phase2aViewModel) {
    var amount by remember { mutableStateOf("") }; var estimated by remember { mutableStateOf(false) }; var pending by remember { mutableStateOf(false) }; var selected by remember { mutableIntStateOf(0) }
    val product = state.products.getOrNull(selected.coerceAtMost((state.products.size - 1).coerceAtLeast(0)))
    Text("Registrar consumo", style = MaterialTheme.typography.titleMedium)
    TextButton(enabled = state.products.isNotEmpty(), onClick = { selected = (selected + 1) % state.products.size }) { Text(product?.product?.name ?: "Primero crea un producto") }
    OutlinedTextField(amount, { amount = it }, label = { Text("Cantidad consumida en la unidad de la base") })
    Row { Checkbox(estimated, { estimated = it }); Text("Nutrientes estimados") }
    Row { Checkbox(pending, { pending = it }); Text("Registro pendiente") }
    Button(enabled = product != null, onClick = { amount.toLongOrNull()?.let { value -> product?.let { vm.addConsumption(it, value, estimated, pending) } } }) { Text("Registrar consumo") }
}

private fun EnergyAmount.kcal(): String = (millicalories.toBigDecimal() / 1_000.toBigDecimal()).stripTrailingZeros().toPlainString()
private fun NutrientAmount.grams(): String = "${(milligrams.toBigDecimal() / 1_000.toBigDecimal()).stripTrailingZeros().toPlainString()} g"
