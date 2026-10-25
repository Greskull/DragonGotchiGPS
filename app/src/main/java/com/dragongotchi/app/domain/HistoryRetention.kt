package com.dragongotchi.app.domain

/**
 * Retencion del historial: solo se guardan las [MAX_SESSIONS] rutas
 * mas recientes. Cuando entra una ruta nueva, la mas antigua se
 * elimina para dejar sitio; nunca hay mas de [MAX_SESSIONS] y nunca
 * se borra nada si no entra una nueva que desplace a la mas antigua.
 */
object HistoryRetention {
    const val MAX_SESSIONS = 5

    /**
     * Dada la lista de IDs de sesion ordenada de mas NUEVA a mas
     * antigua, devuelve los IDs que hay que eliminar (las mas
     * antiguas que excedan [maxSessions]). Lista vacia si caben todas.
     */
    fun idsToDelete(
        idsNewestFirst: List<Long>,
        maxSessions: Int = MAX_SESSIONS
    ): List<Long> =
        if (idsNewestFirst.size <= maxSessions) emptyList()
        else idsNewestFirst.drop(maxSessions)
}
