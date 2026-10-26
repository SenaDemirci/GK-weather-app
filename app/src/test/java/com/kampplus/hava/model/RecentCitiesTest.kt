package com.kampplus.hava.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Son bakılanlar" listesinin sıralama, tekrar ve uzunluk kurallarını doğrular. */
class RecentCitiesTest {

    private fun city(name: String, lat: Double = 40.0, lon: Double = 30.0, id: Long = name.hashCode().toLong()) =
        City(id, name, lat, lon, "TR", "Türkiye", name)

    @Test
    fun `yeni sehir basa eklenir`() {
        val list = RecentCities.add(listOf(city("Ankara")), city("İzmir"))

        assertEquals(listOf("İzmir", "Ankara"), list.map { it.name })
    }

    @Test
    fun `tekrar bakilan sehir basa tasinir, iki kez yer almaz`() {
        val list = RecentCities.add(listOf(city("İzmir"), city("Ankara"), city("Bursa")), city("Ankara"))

        assertEquals(listOf("Ankara", "İzmir", "Bursa"), list.map { it.name })
    }

    @Test
    fun `aramadan ve haritadan acilan ayni sehir ayni sayilir`() {
        // Kimlikleri farklı (servis kimliği ve plaka) ama ad ve konum aynı.
        val fromSearch = city("İstanbul", 41.01384, 28.94966, id = 745044)
        val fromMap = city("İstanbul", 41.01384, 28.94966, id = 34)

        assertEquals(listOf(fromMap), RecentCities.add(listOf(fromSearch), fromMap))
    }

    @Test
    fun `ayni adli farkli sehirler ayri tutulur`() {
        val list = RecentCities.add(listOf(city("Paris", 48.85, 2.35)), city("Paris", 33.66, -95.55))

        assertEquals(2, list.size)
    }

    @Test
    fun `liste en fazla sekiz sehir tutar, en eskisi duser`() {
        var list = emptyList<City>()
        (1..10).forEach { list = RecentCities.add(list, city("Şehir $it", lat = it.toDouble())) }

        assertEquals(RecentCities.MAX_SIZE, list.size)
        assertEquals("Şehir 10", list.first().name)
        assertTrue(list.none { it.name == "Şehir 1" || it.name == "Şehir 2" })
    }

    @Test
    fun `sehir listeden cikarilir`() {
        val list = RecentCities.remove(listOf(city("İzmir"), city("Ankara")), city("İzmir"))

        assertEquals(listOf("Ankara"), list.map { it.name })
    }
}
