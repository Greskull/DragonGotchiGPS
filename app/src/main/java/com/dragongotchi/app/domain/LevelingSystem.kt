package com.dragongotchi.app.domain

import androidx.annotation.StringRes
import com.dragongotchi.app.R

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Sistema de niveles del Tamagotchi (15 niveles).
 *
 * Regla pedida:
 *  - 1 EXP por cada 100 pasos (pasos del podometro hardware, no del GPS:
 *    asi conducir o usar fake-GPS no farmea EXP).
 *  - Nivel 1 -> 2 cuesta 100 EXP, y cada nivel siguiente cuesta un 50% mas:
 *    100, 150, 225, 338, 506, ...
 *  - Nivel maximo: 15 (Dragon Ancestral Gigante).
 */
object LevelingSystem {
    const val MAX_LEVEL = 15
    const val STEPS_PER_EXP = 100
    const val BASE_EXP = 100.0
    const val GROWTH_FACTOR = 1.5

    /** EXP necesaria para pasar de [level] a level+1. En el nivel maximo devuelve Int.MAX_VALUE. */
    fun expNeededForLevel(level: Int): Int {
        if (level >= MAX_LEVEL) return Int.MAX_VALUE
        require(level >= 1) { "El nivel minimo es 1" }
        return (BASE_EXP * GROWTH_FACTOR.pow((level - 1).toDouble())).roundToInt()
    }

    /** EXP total acumulada necesaria para ALCANZAR [level] desde el nivel 1 con 0 EXP. */
    fun cumulativeExpToReach(level: Int): Int {
        if (level <= 1) return 0
        return (1 until level.coerceAtMost(MAX_LEVEL)).sumOf { expNeededForLevel(it) }
    }

    /** Pasos necesarios para subir del nivel [level] al siguiente. */
    fun stepsNeededForLevel(level: Int): Int =
        if (level >= MAX_LEVEL) Int.MAX_VALUE else expNeededForLevel(level) * STEPS_PER_EXP

    /** Resultado de aplicar EXP a la mascota: puede subir varios niveles de golpe. */
    data class LevelUpResult(
        val newLevel: Int,
        val newCurrentExp: Int,
        val levelsGained: Int,
        val isMaxLevel: Boolean
    )

    fun applyExp(currentLevel: Int, currentExp: Int, expGained: Int): LevelUpResult {
        var level = currentLevel.coerceIn(1, MAX_LEVEL)
        var exp = currentExp + expGained.coerceAtLeast(0)
        var gained = 0
        while (level < MAX_LEVEL) {
            val needed = expNeededForLevel(level)
            if (exp < needed) break
            exp -= needed
            level++
            gained++
        }
        if (level >= MAX_LEVEL) exp = 0 // en maximo no acumulamos barra
        return LevelUpResult(level, exp, gained, level >= MAX_LEVEL)
    }

    /** Progreso 0f..1f dentro del nivel actual, para la barra de EXP. */
    fun progressInLevel(level: Int, currentExp: Int): Float {
        if (level >= MAX_LEVEL) return 1f
        val needed = expNeededForLevel(level)
        return (currentExp.toFloat() / needed.toFloat()).coerceIn(0f, 1f)
    }
}

/** Las 15 evoluciones. El drawable asociado vive en res/drawable/stage_XX_*.xml */
enum class DragonStage(
    val level: Int,
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    val drawableName: String
) {
    L01(1, R.string.stage_01_name, R.string.stage_01_desc, "stage_01_huevo_mistico"),
    L02(2, R.string.stage_02_name, R.string.stage_02_desc, "stage_02_eclosion"),
    L03(3, R.string.stage_03_name, R.string.stage_03_desc, "stage_03_cria_dragon"),
    L04(4, R.string.stage_04_name, R.string.stage_04_desc, "stage_04_dragoncito"),
    L05(5, R.string.stage_05_name, R.string.stage_05_desc, "stage_05_joven_dragon"),
    L06(6, R.string.stage_06_name, R.string.stage_06_desc, "stage_06_dragon_explorador"),
    L07(7, R.string.stage_07_name, R.string.stage_07_desc, "stage_07_dragon_veloz"),
    L08(8, R.string.stage_08_name, R.string.stage_08_desc, "stage_08_dragon_guerrero"),
    L09(9, R.string.stage_09_name, R.string.stage_09_desc, "stage_09_dragon_alado"),
    L10(10, R.string.stage_10_name, R.string.stage_10_desc, "stage_10_dragon_fuego"),
    L11(11, R.string.stage_11_name, R.string.stage_11_desc, "stage_11_dragon_magma"),
    L12(12, R.string.stage_12_name, R.string.stage_12_desc, "stage_12_dragon_solar"),
    L13(13, R.string.stage_13_name, R.string.stage_13_desc, "stage_13_dragon_real"),
    L14(14, R.string.stage_14_name, R.string.stage_14_desc, "stage_14_dragon_titan"),
    L15(15, R.string.stage_15_name, R.string.stage_15_desc, "stage_15_dragon_ancestral");

    companion object {
        fun forLevel(level: Int): DragonStage =
            entries.firstOrNull { it.level == level.coerceIn(1, 15) } ?: L01
    }
}
