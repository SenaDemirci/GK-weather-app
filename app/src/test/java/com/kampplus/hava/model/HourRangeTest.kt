package com.kampplus.hava.model

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saatlik tahmin filtresinin hangi saatleri bıraktığını doğrular. */
class HourRangeTest {

    /** 25 Ekim 13:00'ten başlayan 24 saatlik liste: öğleden ertesi gün öğlene. */
    private val hours = List(24) {
        HourlyForecast(
            time = LocalDateTime.of(2026, 10, 25, 13, 0).plusHours(it.toLong()),
            temperatureC = 15.0,
            weatherCode = 0
        )
    }

    @Test
    fun `tumu hicbir saati elemez`() {
        assertEquals(24, HourRange.ALL.filter(hours).size)
    }

    @Test
    fun `sabah yalnizca 06 ile 11 arasini birakir`() {
        val morning = HourRange.MORNING.filter(hours)

        assertEquals(listOf(6, 7, 8, 9, 10, 11), morning.map { it.time.hour })
    }

    @Test
    fun `aralik basta dahil sonda haric`() {
        val noon = LocalDateTime.of(2026, 10, 25, 12, 0)

        assertFalse(noon in HourRange.MORNING)
        assertTrue(noon in HourRange.NOON)
    }

    @Test
    fun `gece yarisi gece dilimine duser`() {
        assertTrue(LocalDateTime.of(2026, 10, 26, 0, 0) in HourRange.NIGHT)
        assertFalse(LocalDateTime.of(2026, 10, 26, 0, 0) in HourRange.EVENING)
    }

    @Test
    fun `her saat tam olarak bir dilime duser`() {
        val slices = HourRange.entries - HourRange.ALL

        hours.forEach { hour ->
            assertEquals("${hour.time} için", 1, slices.count { hour.time in it })
        }
    }

    @Test
    fun `filtre sirayi korur`() {
        // Akşam bugünün 18–23'ü; ertesi günün saatleri karışmaz.
        val evening = HourRange.EVENING.filter(hours)

        assertEquals(listOf(18, 19, 20, 21, 22, 23), evening.map { it.time.hour })
        assertTrue(evening.all { it.time.dayOfMonth == 25 })
    }

    @Test
    fun `ciplerdeki aralik metni iki haneli yazilir`() {
        assertEquals("06–12", HourRange.MORNING.span)
        assertEquals(null, HourRange.ALL.span)
    }
}
