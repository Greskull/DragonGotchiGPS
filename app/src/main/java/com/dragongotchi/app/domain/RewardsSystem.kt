package com.dragongotchi.app.domain

import androidx.annotation.StringRes
import com.dragongotchi.app.R

import kotlin.random.Random

/**
 * ============================================================
 *  FILTRO DE ACTIVIDAD VALIDA
 * ============================================================
 * Una actividad SOLO cuenta si cumple AMBAS condiciones:
 *  - pasos    >= 500
 *  - duracion >= 250 segundos (4 min 10 s)
 *
 * Este filtro decide dos cosas (y, al descartarse la actividad,
 * tampoco concede EXP: la sesion simplemente no cuenta):
 *  1. Si la actividad se GUARDA en el historial de rutas.
 *  2. Si se realiza la TIRADA (dice roll) de recompensas.
 */
object ActivityFilter {
    const val MIN_STEPS = 500
    const val MIN_DURATION_SEC = 250L

    fun isValid(steps: Int, durationSec: Long): Boolean =
        steps >= MIN_STEPS && durationSec >= MIN_DURATION_SEC

}

/**
 * ============================================================
 *  RECOMPENSAS DESBLOQUEABLES (DICE ROLL 1..100)
 * ============================================================
 * Cada recompensa posee un intervalo FIJO y DISJUNTO de numeros.
 * Por construccion es imposible que toquen 2 recompensas en la
 * misma tirada: cada numero pertenece como mucho a un intervalo.
 *
 * NOTA sobre porcentajes: en una tirada 1-100 cada numero vale 1%.
 * Un 15% son QUINCE numeros (1-15), no catorce (1-14 seria 14%).
 * Por eso los intervalos correctos, respetando los % pedidos, son:
 *
 *   1 - 15   Aura                 15%
 *  16 - 22   Sprite Shiny          7%
 *  23 - 25   Dragon 2               3%
 *  26 - 28   Dragon 3               3%
 *  29 - 100  Sin recompensa        72%
 *
 * REGLA DE ANULACION: cuando una recompensa ya esta desbloqueada,
 * su intervalo queda MUERTO. Si la tirada cae en el, no hay premio
 * (NO se repite la tirada ni se desplaza al siguiente intervalo).
 * Asi una recompensa nunca puede desbloquearse dos veces.
 *
 * AMPLIACIONES FUTURAS: asignar el siguiente bloque libre a partir
 * del 29 en orden ascendente (p. ej. 29-31, 32-34...), sin tocar
 * nunca los intervalos ya publicados.
 */
enum class RewardId(
    @StringRes val nameRes: Int,
    val emoji: String,
    @StringRes val descRes: Int,
    val rollRange: IntRange,
    val probabilityPercent: Int
) {
    AURA(
        nameRes = R.string.reward_aura_name,
        emoji = "✨",
        descRes = R.string.reward_aura_desc,
        rollRange = 1..15,
        probabilityPercent = 15
    ),
    SHINY_SKIN(
        nameRes = R.string.reward_shiny_name,
        emoji = "🌟",
        descRes = R.string.reward_shiny_desc,
        rollRange = 16..22,
        probabilityPercent = 7
    ),
    PET_DRAGON_2(
        nameRes = R.string.reward_dragon2_name,
        emoji = "🔴",
        descRes = R.string.reward_dragon2_desc,
        rollRange = 23..25,
        probabilityPercent = 3
    ),
    PET_DRAGON_3(
        nameRes = R.string.reward_dragon3_name,
        emoji = "🔵",
        descRes = R.string.reward_dragon3_desc,
        rollRange = 26..28,
        probabilityPercent = 3
    );

    companion object {
        fun fromStorageName(name: String?): RewardId? =
            entries.firstOrNull { it.name == name }
    }
}

/** Mascotas seleccionables. La 1 es la base (siempre disponible); 2 y 3 se desbloquean. */
enum class PetChoice(
    val petId: Int,
    @StringRes val nameRes: Int,
    val unlockReward: RewardId?
) {
    DRAGON_1(1, R.string.pet_dragon1_name, null),
    DRAGON_2(2, R.string.pet_dragon2_name, RewardId.PET_DRAGON_2),
    DRAGON_3(3, R.string.pet_dragon3_name, RewardId.PET_DRAGON_3);

    fun isUnlocked(unlocked: Set<RewardId>): Boolean =
        unlockReward == null || unlockReward in unlocked

    companion object {
        fun forId(petId: Int): PetChoice = entries.firstOrNull { it.petId == petId } ?: DRAGON_1
    }
}

data class RollResult(
    val value: Int,
    /** Recompensa desbloqueada en esta tirada, o null si no toco ninguna (o cayo en un intervalo ya anulado). */
    val unlockedReward: RewardId?
)

object RewardsSystem {
    const val ROLL_MIN = 1
    const val ROLL_MAX = 100

    /**
     * Recompensa asociada a un valor de tirada, teniendo en cuenta la
     * anulacion: si el dueno del intervalo ya esta desbloqueado, devuelve null.
     */
    fun rewardForRoll(value: Int, unlockedRewards: Set<RewardId>): RewardId? {
        require(value in ROLL_MIN..ROLL_MAX) { "La tirada debe estar entre 1 y 100" }
        val owner = RewardId.entries.firstOrNull { value in it.rollRange } ?: return null
        return if (owner in unlockedRewards) null else owner
    }

    /** Realiza una tirada 1..100 y devuelve su resultado (sin persistir nada). */
    fun roll(unlockedRewards: Set<RewardId>, random: Random = Random): RollResult {
        val value = random.nextInt(ROLL_MIN, ROLL_MAX + 1)
        return RollResult(value, rewardForRoll(value, unlockedRewards))
    }

    /** % real de que toque ALGUNA recompensa teniendo en cuenta las anuladas. */
    fun effectiveAnyRewardPercent(unlockedRewards: Set<RewardId>): Int =
        RewardId.entries.filter { it !in unlockedRewards }.sumOf { it.probabilityPercent }
}

/** Estado completo de recompensas/equipamiento que se persiste en DataStore. */
data class RewardState(
    val unlockedRewards: Set<RewardId> = emptySet(),
    val auraEquipped: Boolean = false,
    val shinyEquipped: Boolean = false,
    /** Mascota seleccionada: 1, 2 o 3 (PetChoice). */
    val selectedPetId: Int = 1,
    /** true si hay alguna recompensa nueva sin ver (muestra el «!» rojo). */
    val hasUnseenReward: Boolean = false,
    val totalRolls: Int = 0,
    val lastRollValue: Int? = null,
    val lastUnlockedReward: RewardId? = null
) {
    fun isUnlocked(reward: RewardId): Boolean = reward in unlockedRewards
    val selectedPet: PetChoice get() = PetChoice.forId(selectedPetId)
}
