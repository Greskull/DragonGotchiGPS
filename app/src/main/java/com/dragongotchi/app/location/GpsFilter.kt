package com.dragongotchi.app.location

import com.dragongotchi.app.domain.GeoPoint
import com.dragongotchi.app.domain.StatsCalculator

/**
 * ============================================================
 *  FILTRO GPS ANTI-TRAMPAS (vehiculos y fake-GPS)
 * ============================================================
 *
 * Idea del usuario: "limitar la distancia entre tics a ~54 km/h, unos 250 ft".
 * Correccion importante: 54 km/h = 15 m/s = 49,2 ft POR SEGUNDO.
 *   - Si el tick es de 1 s  -> maximo ~15 m  (~49 ft)
 *   - Si el tick es de 5 s  -> maximo ~75 m  (~246 ft)  <- de ahi salen los "250 ft"
 * Por eso NO se usa una distancia fija: se usa VELOCIDAD implicita
 * (distancia / tiempo entre puntos), que es lo correcto sea cual sea el tick.
 *
 * Capas del filtro (como hacen Strava / apps profesionales):
 *  1. Ubicacion simulada (mock)          -> rechazada y marcada sospechosa.
 *  2. Precision GPS mala (>30 m)         -> rechazada (evita saltos en ciudad/tuneles).
 *  3. Velocidad reportada por el chip    -> si supera 15 m/s, rechazada.
 *  4. Velocidad implicita entre puntos   -> si supera 15 m/s (54 km/h), rechazada:
 *     en coche, bici rapida o con teletransportes de fake-GPS no suma distancia.
 *  5. Jitter parado (<2 m)               -> rechazado sin penalizar (ruido GPS normal).
 *  6. Timestamps invalidos/duplicados    -> rechazados.
 *
 * Ademas, la EXP del Tamagotchi NO viene del GPS: viene del podometro
 * hardware (100 pasos = 1 EXP). Ir en coche no mueve el podometro,
 * por lo que aunque alguien burlara el GPS no podria farmear niveles.
 *
 * Si muchos puntos son rechazados por velocidad/mock, la sesion se marca
 * como sospechosa (isFlagged) al guardarla.
 */
class GpsFilter(
    private val maxSpeedMps: Double = MAX_SPEED_MPS,
    private val maxAccuracyM: Float = MAX_ACCURACY_M,
    private val minDistanceM: Double = MIN_DISTANCE_M
) {
    companion object {
        const val MAX_SPEED_KMH = 54.0
        const val MAX_SPEED_MPS = 15.0          // 54 km/h
        const val MAX_ACCURACY_M = 30f
        const val MIN_DISTANCE_M = 2.0
        /** A partir de esta proporcion de rechazos sospechosos, la sesion se marca. */
        const val FLAG_RATIO = 0.15
    }

    enum class RejectReason {
        MOCK_LOCATION, LOW_ACCURACY, REPORTED_SPEED_TOO_HIGH,
        IMPLAUSIBLE_SPEED, JITTER_TOO_CLOSE, INVALID_TIMESTAMP
    }

    sealed class Result {
        data class Accepted(val point: GeoPoint, val distanceDeltaM: Double) : Result()
        data class Rejected(val reason: RejectReason, val isSuspicious: Boolean) : Result()
    }

    private var lastAccepted: GeoPoint? = null
    var acceptedCount: Int = 0; private set
    var rejectedCount: Int = 0; private set
    var suspiciousCount: Int = 0; private set
    var totalDistanceM: Double = 0.0; private set

    val isSessionSuspicious: Boolean
        get() {
            val total = acceptedCount + suspiciousCount
            return suspiciousCount >= 5 && total > 0 && (suspiciousCount.toDouble() / total) > FLAG_RATIO
        }

    fun reset() {
        lastAccepted = null
        acceptedCount = 0; rejectedCount = 0; suspiciousCount = 0; totalDistanceM = 0.0
    }

    /**
     * Evalua un punto crudo.
     * @param isMock true si Location.isMock (API 31+) / isFromMockProvider.
     */
    fun evaluate(point: GeoPoint, isMock: Boolean = false): Result {
        if (isMock) return reject(RejectReason.MOCK_LOCATION, suspicious = true)

        val acc = point.accuracyM
        if (acc == null || acc <= 0f || acc > maxAccuracyM) {
            return reject(RejectReason.LOW_ACCURACY, suspicious = false)
        }

        val reported = point.speedMps
        if (reported != null && reported > maxSpeedMps) {
            return reject(RejectReason.REPORTED_SPEED_TOO_HIGH, suspicious = true)
        }

        val last = lastAccepted
        if (last == null) {
            lastAccepted = point; acceptedCount++
            return Result.Accepted(point, 0.0)
        }

        val dtSec = (point.timeMillis - last.timeMillis) / 1000.0
        if (dtSec <= 0.0) return reject(RejectReason.INVALID_TIMESTAMP, suspicious = false)

        val distance = StatsCalculator.distanceMeters(last, point)
        val impliedSpeed = distance / dtSec // m/s

        // CLAVE anti-vehiculo / anti-teletransporte:
        // distancia maxima permitida entre tics = velocidad maxima x tiempo entre tics.
        val maxAllowedDistance = maxSpeedMps * dtSec
        if (impliedSpeed > maxSpeedMps) {
            // No actualizamos lastAccepted: el tramposo no "avanza" su posicion valida.
            return reject(RejectReason.IMPLAUSIBLE_SPEED, suspicious = true)
        }

        // Ruido GPS estando parado: puntos demasiado cercanos no suman distancia.
        if (distance < minDistanceM) {
            return reject(RejectReason.JITTER_TOO_CLOSE, suspicious = false)
        }

        check(distance <= maxAllowedDistance + 0.001) { "sanity check de velocidad" }
        lastAccepted = point
        acceptedCount++
        totalDistanceM += distance
        return Result.Accepted(point, distance)
    }

    private fun reject(reason: RejectReason, suspicious: Boolean): Result {
        rejectedCount++
        if (suspicious) suspiciousCount++
        return Result.Rejected(reason, suspicious)
    }
}
