package com.dragongotchi.app.data.repository

import com.dragongotchi.app.data.local.PetDao
import com.dragongotchi.app.data.local.PetStateEntity
import com.dragongotchi.app.domain.LevelingSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class StepsReward(
    val expGained: Int,
    val levelsGained: Int,
    val newLevel: Int,
    val leveledUp: Boolean
)

class PetRepository(private val petDao: PetDao) {

    val pet: Flow<PetStateEntity> = petDao.observePet().map { it ?: PetStateEntity() }

    suspend fun ensurePetExists(name: String = "Draco") {
        if (petDao.getPet() == null) petDao.upsertPet(PetStateEntity(name = name))
    }

    /** Renombra la mascota (crea el estado si aun no existia). */
    suspend fun renamePet(name: String) {
        if (name.isBlank()) return
        val current = petDao.getPet()
        if (current == null) {
            petDao.upsertPet(PetStateEntity(name = name.trim().take(20)))
        } else {
            petDao.upsertPet(current.copy(name = name.trim().take(20)))
        }
    }

    /**
     * Convierte pasos de una sesion en EXP y sube de nivel si toca.
     * 1 EXP cada 100 pasos; el resto (0-99 pasos) se guarda para la proxima sesion.
     */
    suspend fun grantSteps(sessionSteps: Int, sessionDistanceM: Double): StepsReward {
        val current = petDao.getPet() ?: PetStateEntity().also { petDao.upsertPet(it) }
        if (sessionSteps <= 0) {
            petDao.upsertPet(
                current.copy(
                    lifetimeDistanceM = current.lifetimeDistanceM + sessionDistanceM,
                    totalSessions = current.totalSessions + 1,
                    lastTrainedAtMillis = System.currentTimeMillis()
                )
            )
            return StepsReward(0, 0, current.level, false)
        }

        val totalProgressSteps = current.stepsTowardsNextExp + sessionSteps
        val expGained = totalProgressSteps / LevelingSystem.STEPS_PER_EXP
        val remainder = totalProgressSteps % LevelingSystem.STEPS_PER_EXP

        val result = LevelingSystem.applyExp(current.level, current.currentExp, expGained)

        petDao.upsertPet(
            current.copy(
                level = result.newLevel,
                currentExp = result.newCurrentExp,
                totalExp = current.totalExp + expGained,
                lifetimeSteps = current.lifetimeSteps + sessionSteps,
                stepsTowardsNextExp = remainder,
                lifetimeDistanceM = current.lifetimeDistanceM + sessionDistanceM,
                totalSessions = current.totalSessions + 1,
                lastTrainedAtMillis = System.currentTimeMillis()
            )
        )
        return StepsReward(expGained, result.levelsGained, result.newLevel, result.levelsGained > 0)
    }
}
