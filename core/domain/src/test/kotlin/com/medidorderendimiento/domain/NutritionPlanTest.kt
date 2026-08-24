package com.medidorderendimiento.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NutritionPlanTest {
    private val accepted = PlanAcceptance(Instant.parse("2026-08-23T12:00:00Z"))
    private val start = CivilDay.of(2026, 8, 23)

    @Test
    fun `all specified goal types can be represented without automatic changes`() {
        NutritionGoal.entries.forEach { goal ->
            val plan = plan(goal = goal)

            assertEquals(goal, plan.goal)
            assertEquals(2_100_000, plan.baseDailyEnergy.millicalories)
            assertEquals(accepted, plan.acceptance)
        }
    }

    @Test
    fun `protein can be known or unknown without becoming zero`() {
        val unknownProtein = plan(protein = null)
        val knownProtein = plan(protein = NutrientAmount.ofGrams(120))

        assertNull(unknownProtein.proteinTarget)
        assertEquals(120_000, knownProtein.proteinTarget?.milligrams)
    }

    @Test
    fun `loss and gain accept an explicit positive rate magnitude`() {
        val rate = TargetWeeklyRate.ofGrams(250)

        assertEquals(rate, plan(goal = NutritionGoal.LOSS, rate = rate).targetWeeklyRate)
        assertEquals(rate, plan(goal = NutritionGoal.GAIN, rate = rate).targetWeeklyRate)
        assertFailsWith<IllegalArgumentException> { TargetWeeklyRate.ofGrams(0) }
    }

    @Test
    fun `incompatible goals reject a target weight-change rate`() {
        val rate = TargetWeeklyRate.ofGrams(250)
        val incompatible = NutritionGoal.entries - setOf(NutritionGoal.LOSS, NutritionGoal.GAIN)

        incompatible.forEach { goal ->
            assertFailsWith<IllegalArgumentException> { plan(goal = goal, rate = rate) }
        }
    }

    @Test
    fun `validity end cannot precede validity start`() {
        assertFailsWith<IllegalArgumentException> {
            plan(validUntil = CivilDay.of(2026, 8, 22))
        }
        assertEquals(start, plan(validUntil = start).validUntil)
    }

    private fun plan(
        goal: NutritionGoal = NutritionGoal.MAINTENANCE,
        protein: NutrientAmount? = null,
        rate: TargetWeeklyRate? = null,
        validUntil: CivilDay? = null,
    ) = NutritionPlanVersion(
        id = LocalId("plan-1"),
        goal = goal,
        baseDailyEnergy = EnergyAmount.ofKilocalories(2_100),
        proteinTarget = protein,
        targetWeeklyRate = rate,
        validFrom = start,
        validUntil = validUntil,
        acceptance = accepted,
    )
}
