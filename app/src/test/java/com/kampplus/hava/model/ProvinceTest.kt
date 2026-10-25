package com.kampplus.hava.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 81 il listesinin eksiksiz ve Türkiye sınırları içinde olduğunu doğrular. */
class ProvinceTest {

    @Test
    fun `81 il plaka sirasiyla eksiksiz`() {
        assertEquals((1..81).toList(), TurkishProvinces.map { it.plate })
        assertEquals(81, TurkishProvinces.map { it.name }.toSet().size)
    }

    @Test
    fun `butun il merkezleri turkiye sinirlari icinde`() {
        TurkishProvinces.forEach {
            assertTrue("${it.name} enlemi", it.latitude in 35.8..42.2)
            assertTrue("${it.name} boylamı", it.longitude in 25.6..44.9)
        }
    }

    @Test
    fun `butun il merkezleri harita cizgisinin icinde`() {
        // Sınır elle sadeleştirildiği için kıyıdaki bir ilin denize düşmediğini burada yakalarız.
        TurkishProvinces.forEach {
            val onLand = TurkeyOutline.contains(TurkeyOutline.anatolia, it.latitude, it.longitude) ||
                TurkeyOutline.contains(TurkeyOutline.thrace, it.latitude, it.longitude)
            assertTrue("${it.name} kara dışında kaldı", onLand)
        }
    }

    @Test
    fun `istanbul bogazinin iki yakasi ayri parcalardir`() {
        assertTrue(TurkeyOutline.contains(TurkeyOutline.thrace, 41.68, 26.56)) // Edirne
        assertFalse(TurkeyOutline.contains(TurkeyOutline.anatolia, 41.68, 26.56))
        assertFalse(TurkeyOutline.contains(TurkeyOutline.anatolia, 43.0, 35.0)) // Karadeniz'in ortası
    }

    @Test
    fun `il tahmin ekranina sehir olarak gider`() {
        val city = TurkishProvinces.first { it.plate == 34 }.toCity()

        assertEquals("İstanbul", city.name)
        assertEquals("İstanbul, Türkiye", city.subtitle)
    }
}
