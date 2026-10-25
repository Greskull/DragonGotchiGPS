package com.dragongotchi.app.domain

import androidx.annotation.StringRes
import com.dragongotchi.app.R

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timeMillis: Long = 0L,
    val speedMps: Float? = null,
    val accuracyM: Float? = null
)

enum class ActivityType(@StringRes val nameRes: Int, val emoji: String) {
    RUN(R.string.activity_run, "🏃"),
    WALK(R.string.activity_walk, "🚶"),
    BIKE(R.string.activity_bike, "🚴")
}

object StatsCalculator {
    const val EARTH_RADIUS_M = 6_371_000.0
    /** Longitud de zancada media para estimar pasos si no hay podometro hardware. */
    const val DEFAULT_STRIDE_M = 0.78

    /** Distancia haversiana en metros entre dos puntos GPS. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Ritmo en segundos por km. 0 si aun no hay distancia suficiente. */
    fun paceSecPerKm(distanceM: Double, elapsedSec: Long): Double {
        if (distanceM < 10.0 || elapsedSec <= 0) return 0.0
        return elapsedSec / (distanceM / 1000.0)
    }

    fun speedKmh(distanceM: Double, elapsedSec: Long): Double {
        if (elapsedSec <= 0) return 0.0
        return (distanceM / elapsedSec) * 3.6
    }

    /**
     * Calorias con METs segun velocidad (compendio de actividades fisicas,
     * el mismo enfoque que usan apps deportivas profesionales):
     * kcal = MET x peso(kg) x duracion(horas)
     */
    fun caloriesBurned(weightKg: Double, elapsedSec: Long, distanceM: Double, type: ActivityType): Double {
        if (elapsedSec <= 0 || weightKg <= 0) return 0.0
        val kmh = speedKmh(distanceM, elapsedSec)
        val met = when (type) {
            ActivityType.WALK -> when {
                kmh < 3.0 -> 2.0
                kmh < 4.5 -> 3.0
                kmh < 5.5 -> 3.8
                kmh < 6.5 -> 5.0
                else -> 6.3
            }
            ActivityType.RUN -> when {
                kmh < 8.0 -> 7.0
                kmh < 9.5 -> 9.0
                kmh < 11.0 -> 10.5
                kmh < 12.5 -> 11.5
                kmh < 14.0 -> 12.8
                else -> 14.5
            }
            ActivityType.BIKE -> when {
                kmh < 10.0 -> 4.0
                kmh < 14.0 -> 5.8
                kmh < 18.0 -> 7.0
                kmh < 22.0 -> 8.5
                kmh < 26.0 -> 10.0
                else -> 12.0
            }
        }
        return met * weightKg * (elapsedSec / 3600.0)
    }

    /** Estimacion de pasos a partir de la distancia (fallback sin sensor). */
    fun estimateSteps(distanceM: Double, strideM: Double = DEFAULT_STRIDE_M): Int =
        if (distanceM <= 0) 0 else (distanceM / strideM).toInt()

    // ---------- Formato para UI ----------
    fun formatDuration(totalSec: Long): String {
        val h = totalSec / 3600; val m = (totalSec % 3600) / 60; val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun formatPace(paceSecPerKm: Double): String {
        if (paceSecPerKm <= 0 || paceSecPerKm.isInfinite() || paceSecPerKm.isNaN()) return "--:--"
        val total = paceSecPerKm.toInt()
        return "%d:%02d".format(total / 60, total % 60)
    }

    fun formatDistanceKm(distanceM: Double): String = "%.2f".format(distanceM / 1000.0)
    fun formatKcal(kcal: Double): String = "%.0f".format(kcal)
}
