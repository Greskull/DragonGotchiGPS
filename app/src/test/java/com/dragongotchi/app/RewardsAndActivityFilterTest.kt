package com.dragongotchi.app

import com.dragongotchi.app.domain.ActivityFilter
import com.dragongotchi.app.domain.RewardId
import com.dragongotchi.app.domain.RewardsSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ActivityFilterTest {

    @Test
    fun `actividad valida exige 500 pasos Y 250 segundos a la vez`() {
        assertFalse(ActivityFilter.isValid(steps = 499, durationSec = 250))
        assertFalse(ActivityFilter.isValid(steps = 500, durationSec = 249))
        assertFalse(ActivityFilter.isValid(steps = 499, durationSec = 249))
        assertFalse(ActivityFilter.isValid(steps = 0, durationSec = 0))
        assertTrue(ActivityFilter.isValid(steps = 500, durationSec = 250))
        assertTrue(ActivityFilter.isValid(steps = 800, durationSec = 600))
    }
}

class RewardsSystemTest {

    @Test
    fun `los intervalos son Aura 1-15 Shiny 16-22 Dragon2 23-25 Dragon3 26-28`() {
        val sinNada = emptySet<RewardId>()
        assertEquals(RewardId.AURA, RewardsSystem.rewardForRoll(1, sinNada))
        assertEquals(RewardId.AURA, RewardsSystem.rewardForRoll(15, sinNada))
        assertEquals(RewardId.SHINY_SKIN, RewardsSystem.rewardForRoll(16, sinNada))
        assertEquals(RewardId.SHINY_SKIN, RewardsSystem.rewardForRoll(22, sinNada))
        assertEquals(RewardId.PET_DRAGON_2, RewardsSystem.rewardForRoll(23, sinNada))
        assertEquals(RewardId.PET_DRAGON_2, RewardsSystem.rewardForRoll(25, sinNada))
        assertEquals(RewardId.PET_DRAGON_3, RewardsSystem.rewardForRoll(26, sinNada))
        assertEquals(RewardId.PET_DRAGON_3, RewardsSystem.rewardForRoll(28, sinNada))
        assertNull(RewardsSystem.rewardForRoll(29, sinNada))
        assertNull(RewardsSystem.rewardForRoll(100, sinNada))
    }

    @Test
    fun `los porcentajes suman 15 mas 7 mas 3 mas 3 y nunca se solapan`() {
        assertEquals(15, RewardId.AURA.probabilityPercent)
        assertEquals(15, RewardId.AURA.rollRange.count())
        assertEquals(7, RewardId.SHINY_SKIN.rollRange.count())
        assertEquals(3, RewardId.PET_DRAGON_2.rollRange.count())
        assertEquals(3, RewardId.PET_DRAGON_3.rollRange.count())
        val todos = RewardId.entries.flatMap { it.rollRange.toList() }
        assertEquals("ningun numero puede pertenecer a dos recompensas", todos.size, todos.toSet().size)
        assertTrue(todos.all { it in 1..100 })
        assertEquals(28, RewardsSystem.effectiveAnyRewardPercent(emptySet()))
    }

    @Test
    fun `una recompensa desbloqueada anula su intervalo y no puede volver a tocar`() {
        val conAura = setOf(RewardId.AURA)
        assertNull(RewardsSystem.rewardForRoll(1, conAura))
        assertNull(RewardsSystem.rewardForRoll(15, conAura))
        // Los demas intervalos siguen vivos
        assertEquals(RewardId.SHINY_SKIN, RewardsSystem.rewardForRoll(18, conAura))
        assertEquals(13, RewardsSystem.effectiveAnyRewardPercent(conAura))

        val todoDesbloqueado = RewardId.entries.toSet()
        assertNull(RewardsSystem.rewardForRoll(10, todoDesbloqueado))
        assertNull(RewardsSystem.rewardForRoll(20, todoDesbloqueado))
        assertNull(RewardsSystem.rewardForRoll(24, todoDesbloqueado))
        assertEquals(0, RewardsSystem.effectiveAnyRewardPercent(todoDesbloqueado))
    }

    @Test
    fun `la tirada siempre cae entre 1 y 100`() {
        val random = Random(42)
        repeat(2_000) {
            val result = RewardsSystem.roll(emptySet(), random)
            assertTrue(result.value in 1..100)
        }
    }
}
