package com.dragongotchi.app.data.repository

import com.dragongotchi.app.data.preferences.UserPreferences
import com.dragongotchi.app.domain.RewardState
import com.dragongotchi.app.domain.RewardsSystem
import com.dragongotchi.app.domain.RollResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Fachada del sistema de recompensas: realiza tiradas contra el estado
 * persistido y registra sus resultados. La logica pura (intervalos,
 * anulaciones, probabilidades) vive en RewardsSystem y tiene tests.
 */
class RewardsRepository(private val preferences: UserPreferences) {

    val state: Flow<RewardState> = preferences.rewardState

    suspend fun currentState(): RewardState = preferences.rewardState.first()

    /** Hace una tirada 1..100, la persiste y devuelve el resultado. */
    suspend fun performRoll(): RollResult {
        val unlocked = currentState().unlockedRewards
        val result = RewardsSystem.roll(unlocked)
        preferences.recordRoll(result)
        return result
    }

    suspend fun setAuraEquipped(equipped: Boolean) = preferences.setAuraEquipped(equipped)
    suspend fun setShinyEquipped(equipped: Boolean) = preferences.setShinyEquipped(equipped)
    suspend fun setSelectedPet(petId: Int) = preferences.setSelectedPet(petId)
    suspend fun markRewardsSeen() = preferences.markRewardsSeen()
}
