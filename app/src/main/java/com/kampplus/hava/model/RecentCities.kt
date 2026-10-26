package com.kampplus.hava.model

import kotlin.math.roundToLong

/**
 * "Son bakılanlar" listesinin kuralları: en yeni şehir başta, aynı şehir iki kez yer
 * almaz, liste [MAX_SIZE] şehirden uzun olmaz.
 *
 * Saf Kotlin'dir; listeyi cihazda saklamak [com.kampplus.hava.data.RecentCitiesRepository]'nin işidir.
 */
object RecentCities {

    const val MAX_SIZE = 8

    /**
     * Şehri listenin başına ekler. Aynı şehir listede zaten varsa eski yerinden alınıp
     * başa taşınır; böylece bir şehre tekrar bakmak onu "en son bakılan" yapar.
     */
    fun add(current: List<City>, city: City): List<City> = (listOf(city) + current.filterNot { sameCity(it, city) }).take(MAX_SIZE)

    /** Şehri listeden çıkarır. */
    fun remove(current: List<City>, city: City): List<City> = current.filterNot { sameCity(it, city) }

    /**
     * İki kayıt aynı şehir mi?
     *
     * Kimliğe (id) bakmayız: haritadan açılan illerin kimliği plaka numarasıdır, aramadan
     * gelen şehrinki ise servisin kimliği. Bunun yerine ad ve iki ondalığa yuvarlanmış
     * koordinat karşılaştırılır; aramadan ve haritadan açılan "İstanbul" aynı sayılır.
     */
    fun sameCity(a: City, b: City): Boolean =
        a.name == b.name && a.latitude.rounded() == b.latitude.rounded() && a.longitude.rounded() == b.longitude.rounded()

    private fun Double.rounded(): Long = (this * 100).roundToLong()
}
