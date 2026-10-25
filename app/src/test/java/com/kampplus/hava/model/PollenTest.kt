package com.kampplus.hava.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Polen yoğunluğunun seviyeye çevrilmesini ve uyarı kurallarını doğrular. */
class PollenTest {

    @Test
    fun `ayni yogunluk turune gore farkli seviyedir`() {
        // 30 tane/m³: çimen için yüksek, huş için yalnızca orta.
        assertEquals(PollenLevel.HIGH, PollenType.GRASS.levelOf(30.0))
        assertEquals(PollenLevel.MODERATE, PollenType.BIRCH.levelOf(30.0))
    }

    @Test
    fun `seviye sinirlari`() {
        assertEquals(PollenLevel.NONE, PollenType.GRASS.levelOf(0.4))
        assertEquals(PollenLevel.LOW, PollenType.GRASS.levelOf(1.0))
        assertEquals(PollenLevel.MODERATE, PollenType.GRASS.levelOf(5.0))
        assertEquals(PollenLevel.VERY_HIGH, PollenType.GRASS.levelOf(200.0))
        assertEquals(PollenLevel.VERY_HIGH, PollenType.BIRCH.levelOf(1500.0))
        assertNull(PollenType.GRASS.levelOf(null))
    }

    private fun day(offset: Long, vararg values: Pair<PollenType, Double?>) =
        PollenDay(LocalDate.of(2026, 4, 20).plusDays(offset), values.toMap())

    @Test
    fun `bugunun uyarilari en yogundan baslar ve dusukleri icermez`() {
        val forecast = PollenForecast(
            listOf(day(0, PollenType.GRASS to 3.0, PollenType.BIRCH to 200.0, PollenType.MUGWORT to 15.0))
        )

        assertEquals(
            listOf(PollenType.BIRCH to PollenLevel.HIGH, PollenType.MUGWORT to PollenLevel.MODERATE),
            forecast.alertsToday()
        )
    }

    @Test
    fun `mevsim disi tahmin sessiz sayilir`() {
        val quiet = PollenForecast(listOf(day(0, PollenType.GRASS to 0.2), day(1, PollenType.GRASS to null)))
        val active = PollenForecast(listOf(day(0, PollenType.GRASS to 0.2), day(1, PollenType.GRASS to 2.0)))

        assertTrue(quiet.isQuiet)
        assertFalse(active.isQuiet)
    }
}
