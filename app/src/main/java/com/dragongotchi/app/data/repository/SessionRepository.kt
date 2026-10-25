package com.dragongotchi.app.data.repository

import com.dragongotchi.app.data.local.RoutePointEntity
import com.dragongotchi.app.data.local.SessionDao
import com.dragongotchi.app.data.local.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    fun observeSessions(): Flow<List<TrainingSessionEntity>> = sessionDao.observeAllSessions()
    fun observeSession(id: Long): Flow<TrainingSessionEntity?> = sessionDao.observeSession(id)
    fun observePoints(sessionId: Long): Flow<List<RoutePointEntity>> = sessionDao.observePoints(sessionId)

    /** Guarda la sesion aplicando la retencion: maximo 5 rutas en el historial. */
    suspend fun saveSession(session: TrainingSessionEntity, points: List<RoutePointEntity>): Long =
        sessionDao.insertSessionWithRetention(session, points)

    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)
}
