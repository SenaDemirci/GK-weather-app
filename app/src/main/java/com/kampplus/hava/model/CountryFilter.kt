package com.kampplus.hava.model

/**
 * Arama sonuçlarındaki bir ülke seçeneği: "🇹🇷 Türkiye · 5".
 *
 * @param key Filtrede kullanılan anahtar: ülke kodu ("TR"). Kodu gelmeyen şehirler
 *   [CountryFilter.UNKNOWN_KEY] altında toplanır.
 * @param code Bayrak için ülke kodu; bilinmiyorsa null.
 * @param name Çipte gösterilen ad ("Türkiye"); servis göndermediyse null, ekran
 *   "Bilinmeyen ülke" yazar.
 * @param count Bu ülkeden kaç sonuç geldiği.
 */
data class CountryOption(val key: String, val code: String?, val name: String?, val count: Int)

/**
 * Şehir arama sonuçlarını ülkeye göre süzer.
 *
 * Saf Kotlin'dir; filtreleme sunucuya yeni istek atmadan, gelen liste üzerinde yapılır.
 * Seçim `String?` olarak tutulur: null "Tümü" demektir.
 */
object CountryFilter {

    /** Ülke kodu gelmeyen şehirlerin anahtarı. */
    const val UNKNOWN_KEY = "?"

    /**
     * Sonuçlardaki ülkeler; en çok sonuç veren ülke başta, eşitlikte ada göre.
     * Adı bilinmeyen ülke, eşitlikte sona kalır.
     */
    fun options(cities: List<City>): List<CountryOption> = cities
        .groupBy { it.countryKey }
        .map { (key, group) ->
            val code = group.first().countryCode
            CountryOption(
                key = key,
                code = code,
                name = group.first().country ?: code,
                count = group.size
            )
        }
        .sortedWith(compareByDescending<CountryOption> { it.count }.thenBy(nullsLast()) { it.name })

    /**
     * Seçili ülkenin şehirlerini bırakır.
     *
     * Seçim null ise ya da yeni sonuçlarda o ülke hiç yoksa (başka bir şehir arandı)
     * liste olduğu gibi döner; kullanıcı boş bir ekranla karşılaşmaz.
     */
    fun apply(cities: List<City>, selectedKey: String?): List<City> {
        if (selectedKey == null || cities.none { it.countryKey == selectedKey }) return cities
        return cities.filter { it.countryKey == selectedKey }
    }

    /** Seçim hâlâ geçerli mi? Değilse "Tümü" seçili gösterilir. */
    fun effectiveSelection(cities: List<City>, selectedKey: String?): String? =
        selectedKey?.takeIf { key -> cities.any { it.countryKey == key } }

    private val City.countryKey: String
        get() = countryCode ?: UNKNOWN_KEY
}
