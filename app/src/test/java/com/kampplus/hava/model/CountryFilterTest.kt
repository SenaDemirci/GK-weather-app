package com.kampplus.hava.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Arama sonuçlarının ülkeye göre süzülmesini doğrular. */
class CountryFilterTest {

    private fun city(id: Long, code: String?, country: String?) = City(id, "Paris", 0.0, 0.0, code, country, null)

    /** "Paris" araması gibi: üç Fransa, iki ABD, bir Kanada, bir ülkesiz kayıt. */
    private val cities = listOf(
        city(1, "FR", "Fransa"),
        city(2, "US", "Amerika Birleşik Devletleri"),
        city(3, "FR", "Fransa"),
        city(4, "CA", "Kanada"),
        city(5, "US", "Amerika Birleşik Devletleri"),
        city(6, "FR", "Fransa"),
        city(7, null, null)
    )

    @Test
    fun `secenekler sonuc sayisina gore siralanir`() {
        val options = CountryFilter.options(cities)

        assertEquals(listOf("FR", "US", "CA", CountryFilter.UNKNOWN_KEY).toSet(), options.map { it.key }.toSet())
        assertEquals("FR", options[0].key)
        assertEquals(3, options[0].count)
        assertEquals("US", options[1].key)
    }

    @Test
    fun `esit sayida sonucu olan ulkeler ada gore siralanir ve adsiz ulke sona kalir`() {
        val options = CountryFilter.options(cities).filter { it.count == 1 }

        // Adı bilinmeyen ülke (null) eşitlikte sona kalır; ekran onu "Bilinmeyen ülke" yazar.
        assertEquals(listOf("Kanada", null), options.map { it.name })
    }

    @Test
    fun `ulke secilince yalnizca o ulkenin sehirleri kalir`() {
        val filtered = CountryFilter.apply(cities, "US")

        assertEquals(listOf(2L, 5L), filtered.map { it.id })
    }

    @Test
    fun `ulkesi bilinmeyen sehirler de suzulebilir`() {
        val filtered = CountryFilter.apply(cities, CountryFilter.UNKNOWN_KEY)

        assertEquals(listOf(7L), filtered.map { it.id })
    }

    @Test
    fun `secim yoksa liste oldugu gibi doner`() {
        assertEquals(cities, CountryFilter.apply(cities, null))
    }

    @Test
    fun `yeni sonuclarda olmayan ulke secimi yok sayilir`() {
        // Önce Almanya seçildi, sonra "Paris" arandı: ekran boş kalmamalı.
        assertEquals(cities, CountryFilter.apply(cities, "DE"))
        assertNull(CountryFilter.effectiveSelection(cities, "DE"))
        assertEquals("FR", CountryFilter.effectiveSelection(cities, "FR"))
    }
}
