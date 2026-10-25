package com.dragongotchi.app

import com.dragongotchi.app.domain.GeoPoint
import com.dragongotchi.app.domain.LevelingSystem
import com.dragongotchi.app.location.GpsFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelingSystemTest {

    @Test
    fun `la curva de EXP es 100 150 225 creciendo un 50 por ciento`() {
        assertEquals(100, LevelingSystem.expNeededForLevel(1))
        assertEquals(150, LevelingSystem.expNeededForLevel(2))
        assertEquals(225, LevelingSystem.expNeededForLevel(3))
        assertEquals(338, LevelingSystem.expNeededForLevel(4)) // 337.5 redondeado
        assertEquals(Int.MAX_VALUE, LevelingSystem.expNeededForLevel(15))
    }

    @Test
    fun `100 pasos son 1 EXP y subir el primer nivel cuesta 10000 pasos`() {
        assertEquals(10_000, LevelingSystem.stepsNeededForLevel(1))
        assertEquals(15_000, LevelingSystem.stepsNeededForLevel(2))
    }

    @Test
    fun `aplicar EXP sube de nivel y conserva el sobrante`() {
        val result = LevelingSystem.applyExp(currentLevel = 1, currentExp = 90, expGained = 25)
        assertEquals(2, result.newLevel)
        assertEquals(15, result.newCurrentExp) // 90+25-100
        assertEquals(1, result.levelsGained)
    }

    @Test
    fun `se pueden subir varios niveles de golpe y el maximo es 15`() {
        val result = LevelingSystem.applyExp(1, 0, 100_000)
        assertEquals(15, result.newLevel)
        assertTrue(result.isMaxLevel)
    }
}

class GpsFilterTest {

    private fun point(lat: Double, timeMs: Long, accuracy: Float = 8f, speed: Float? = null) =
        GeoPoint(latitude = lat, longitude = -3.8000, timeMillis = timeMs, accuracyM = accuracy, speedMps = speed)

    @Test
    fun `acepta una caminata normal sumando distancia`() {
        val filter = GpsFilter()
        // ~11 metros cada 5 s andando/corriendo suave (lat: 0.0001 grados aprox 11.1 m)
        assertTrue(filter.evaluate(point(42.0000, 0L)) is GpsFilter.Result.Accepted)
        assertTrue(filter.evaluate(point(42.0001, 5_000L)) is GpsFilter.Result.Accepted)
        assertTrue(filter.evaluate(point(42.0002, 10_000L)) is GpsFilter.Result.Accepted)
        assertTrue(filter.totalDistanceM > 15.0)
        assertEquals(0, filter.suspiciousCount)
    }

    @Test
    fun `rechaza ir en coche a mas de 54 kmh`() {
        val filter = GpsFilter()
        assertTrue(filter.evaluate(point(42.0000, 0L)) is GpsFilter.Result.Accepted)
        // 0.001 grados en 2 s = ~111 m en 2 s = 200 km/h -> coche / teletransporte
        val rejected = filter.evaluate(point(42.0010, 2_000L))
        assertTrue(rejected is GpsFilter.Result.Rejected)
        assertEquals(
            GpsFilter.RejectReason.IMPLAUSIBLE_SPEED,
            (rejected as GpsFilter.Result.Rejected).reason
        )
        assertEquals(1, filter.suspiciousCount)
        assertEquals(0.0, filter.totalDistanceM, 0.001)
    }

    @Test
    fun `rechaza ubicaciones simuladas y precision mala`() {
        val filter = GpsFilter()
        val mock = filter.evaluate(point(42.0, 0L), isMock = true)
        assertTrue(mock is GpsFilter.Result.Rejected)
        assertEquals(GpsFilter.RejectReason.MOCK_LOCATION, (mock as GpsFilter.Result.Rejected).reason)

        val badAccuracy = filter.evaluate(point(42.0, 1_000L, accuracy = 80f))
        assertTrue(badAccuracy is GpsFilter.Result.Rejected)
        assertEquals(GpsFilter.RejectReason.LOW_ACCURACY, (badAccuracy as GpsFilter.Result.Rejected).reason)
    }

    @Test
    fun `el jitter parado no suma distancia ni es sospechoso`() {
        val filter = GpsFilter()
        assertTrue(filter.evaluate(point(42.00000, 0L)) is GpsFilter.Result.Accepted)
        // 0.5 m de ruido GPS en 3 s
        val jitter = filter.evaluate(point(42.0000045, 3_000L))
        assertTrue(jitter is GpsFilter.Result.Rejected)
        assertEquals(GpsFilter.RejectReason.JITTER_TOO_CLOSE, (jitter as GpsFilter.Result.Rejected).reason)
        assertEquals(0.0, filter.totalDistanceM, 0.001)
        assertEquals(0, filter.suspiciousCount)
    }
}
