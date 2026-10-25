package com.dragongotchi.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dragongotchi.app.domain.HistoryRetention
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SessionDao {
    @Insert
    abstract suspend fun insertSession(session: TrainingSessionEntity): Long

    @Insert
    abstract suspend fun insertPoints(points: List<RoutePointEntity>)

    /** Guarda la sesion y toda su ruta de forma atomica (una sola transaccion Room). */
    @Transaction
    open suspend fun insertSessionWithPoints(
        session: TrainingSessionEntity,
        points: List<RoutePointEntity>
    ): Long {
        val sessionId = insertSession(session)
        if (points.isNotEmpty()) insertPoints(points.map { it.copy(sessionId = sessionId) })
        return sessionId
    }

    @Query("SELECT * FROM training_sessions ORDER BY startTimeMillis DESC")
    abstract fun observeAllSessions(): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions WHERE id = :id")
    abstract fun observeSession(id: Long): Flow<TrainingSessionEntity?>

    @Query("SELECT * FROM route_points WHERE sessionId = :sessionId ORDER BY sequence ASC")
    abstract fun observePoints(sessionId: Long): Flow<List<RoutePointEntity>>

    @Query("DELETE FROM training_sessions WHERE id = :id")
    abstract suspend fun deleteSession(id: Long)

    /** IDs de todas las sesiones, de mas nueva a mas antigua. */
    @Query("SELECT id FROM training_sessions ORDER BY startTimeMillis DESC, id DESC")
    abstract suspend fun getAllSessionIdsNewestFirst(): List<Long>

    /**
     * Guarda la sesion y, en la MISMA transaccion, aplica la retencion
     * del historial: si la nueva ruta desplaza a las mas antiguas por
     * encima del maximo (HistoryRetention.MAX_SESSIONS = 5), esas se
     * eliminan (sus puntos GPS caen con ellas por la FK en cascada).
     * Si no sobra ninguna, no se borra nada.
     */
    @Transaction
    open suspend fun insertSessionWithRetention(
        session: TrainingSessionEntity,
        points: List<RoutePointEntity>
    ): Long {
        val newId = insertSessionWithPoints(session, points)
        HistoryRetention.idsToDelete(getAllSessionIdsNewestFirst())
            .forEach { oldId -> deleteSession(oldId) }
        return newId
    }
}

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_state WHERE id = 1")
    fun observePet(): Flow<PetStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPet(pet: PetStateEntity)

    @Query("SELECT * FROM pet_state WHERE id = 1")
    suspend fun getPet(): PetStateEntity?
}
