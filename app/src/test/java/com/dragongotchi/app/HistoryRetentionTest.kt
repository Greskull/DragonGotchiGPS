package com.dragongotchi.app

import com.dragongotchi.app.domain.HistoryRetention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRetentionTest {

    @Test
    fun `con 5 o menos rutas no se borra ninguna`() {
        assertTrue(HistoryRetention.idsToDelete(emptyList()).isEmpty())
        assertTrue(HistoryRetention.idsToDelete(listOf(3L, 2L, 1L)).isEmpty())
        assertTrue(HistoryRetention.idsToDelete(listOf(5L, 4L, 3L, 2L, 1L)).isEmpty())
    }

    @Test
    fun `al entrar la sexta ruta se borra solo la mas antigua`() {
        // IDs de mas nueva (10) a mas antigua (5)
        val ids = listOf(10L, 9L, 8L, 7L, 6L, 5L)
        assertEquals(listOf(5L), HistoryRetention.idsToDelete(ids))
    }

    @Test
    fun `con 8 rutas se borran las 3 mas antiguas y se conservan las 5 nuevas`() {
        val ids = listOf(80L, 70L, 60L, 50L, 40L, 30L, 20L, 10L)
        assertEquals(listOf(30L, 20L, 10L), HistoryRetention.idsToDelete(ids))
    }
}
