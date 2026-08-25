package com.medidorderendimiento.domain

import java.time.Instant

enum class NutritionGoal {
    MAINTENANCE,
    LOSS,
    GAIN,
    RECOMPOSITION,
    PERFORMANCE_PRIORITY,
}

@JvmInline
value class TargetWeeklyRate private constructor(val grams: Long) {
    init { require(grams > 0) { "Target rate magnitude must be greater than zero" } }

    companion object {
        fun ofGrams(grams: Long): TargetWeeklyRate = TargetWeeklyRate(grams)
    }
}

data class PlanAcceptance(
    val acceptedAt: Instant,
)

data class NutritionPlanVersion(
    val id: LocalId,
    val goal: NutritionGoal,
    val baseDailyEnergy: EnergyAmount,
    val proteinTarget: NutrientAmount?,
    val targetWeeklyRate: TargetWeeklyRate?,
    val validFrom: CivilDay,
    val validUntil: CivilDay? = null,
    val acceptance: PlanAcceptance,
) {
    init {
        require(validUntil == null || validUntil >= validFrom) {
            "Plan validity end must not precede its start"
        }
        require(targetWeeklyRate == null || goal == NutritionGoal.LOSS || goal == NutritionGoal.GAIN) {
            "A target weight-change rate is only compatible with loss or gain"
        }
    }
}
