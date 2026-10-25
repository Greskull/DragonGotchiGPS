package com.dragongotchi.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Una sesion de entrenamiento guardada en el historial (como una actividad de Strava). */
@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationSec: Long,
    val distanceM: Double,
    val steps: Int,
    val stepsAreEstimated: Boolean = false,
    val kcal: Double,
    val expGained: Int,
    val avgPaceSecPerKm: Double,
    val maxSpeedKmh: Double,
    val activityType: String, // RUN | WALK
    val acceptedGpsPoints: Int,
    val rejectedGpsPoints: Int,
    val isFlaggedSuspicious: Boolean = false
)

/** Cada punto GPS ACEPTADO por el filtro, para poder redibujar la ruta en el detalle. */
@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val timeMillis: Long,
    val speedMps: Float?,
    val accuracyM: Float?,
    val sequence: Int
)

/** Estado persistente del Tamagotchi (una sola fila, id = 1). */
@Entity(tableName = "pet_state")
data class PetStateEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Draco",
    val level: Int = 1,
    val currentExp: Int = 0,
    val totalExp: Int = 0,
    val lifetimeSteps: Long = 0,
    /** Pasos acumulados hacia la siguiente EXP (0..99), se conserva entre sesiones. */
    val stepsTowardsNextExp: Int = 0,
    val lifetimeDistanceM: Double = 0.0,
    val totalSessions: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastTrainedAtMillis: Long? = null
)
