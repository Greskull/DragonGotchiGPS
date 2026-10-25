package com.dragongotchi.app.location

import com.dragongotchi.app.domain.ActivityType
import com.dragongotchi.app.domain.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TrackingStatus { IDLE, RUNNING, PAUSED }

/**
 * Estado EN VIVO de la sesion. Lo escribe SOLO el LocationTrackingService
 * y lo lee Compose. Compose nunca pide ubicaciones ni calcula la ruta:
 * solo pinta este estado. Por eso el tracking sobrevive a que la UI
 * se minimice, se bloquee la pantalla o Android cierre la Activity.
 */
data class LiveTrackingState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val activityType: ActivityType = ActivityType.RUN,
    /**
     * Nivel del Tamagotchi CAPTURADO al pulsar "Iniciar" en esta sesion.
     * Es el sprite que se usa como marcador de posicion en el mapa durante
     * toda la sesion, aunque la mascota suba de nivel al finalizarla.
     */
    val dragonLevelAtStart: Int = 1,
    /** Apariencia capturada tambien al iniciar: mascota elegida y skins equipadas. */
    val dragonPetIdAtStart: Int = 1,
    val dragonShinyAtStart: Boolean = false,
    val dragonAuraAtStart: Boolean = false,
    val startTimeMillis: Long = 0L,
    val elapsedSec: Long = 0L,
    val distanceM: Double = 0.0,
    val steps: Int = 0,
    val stepsAreEstimated: Boolean = false,
    val kcal: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val currentPoint: GeoPoint? = null,
    val gpsAccuracyM: Float? = null,
    /**
     * Error GPS visible para el usuario (ubicacion del dispositivo
     * desactivada, permiso denegado o fallo al pedir ubicaciones).
     * Antes estos fallos se tragaban en silencio y la app parecia
     * "no encontrar senal" sin explicar por que.
     */
    val gpsError: String? = null,
    val routePoints: List<GeoPoint> = emptyList(),
    val acceptedPoints: Int = 0,
    val rejectedPoints: Int = 0,
    val suspiciousPoints: Int = 0,
    val lastRejectedReason: String? = null,
    /** true justo despues de finalizar (la sesion puede no haberse guardado si fue demasiado corta). */
    val hasJustFinished: Boolean = false,
    /** Resultado del filtro de actividad valida (>=500 pasos y >=250 s). */
    val activityWasValid: Boolean = false,
    /** Tirada de recompensas realizada al finalizar (solo si fue valida). */
    val rollValue: Int? = null,
    val unlockedRewardName: String? = null,
    /** Resultado al finalizar: id de la sesion guardada y recompensa del Tamagotchi. */
    val finishedSessionId: Long? = null,
    val lastExpGained: Int = 0,
    val lastLevelsGained: Int = 0
)

class TrackingManager {
    private val _state = MutableStateFlow(LiveTrackingState())
    val state: StateFlow<LiveTrackingState> = _state.asStateFlow()

    fun update(transform: (LiveTrackingState) -> LiveTrackingState) {
        _state.value = transform(_state.value)
    }

    fun reset() { _state.value = LiveTrackingState() }
}
