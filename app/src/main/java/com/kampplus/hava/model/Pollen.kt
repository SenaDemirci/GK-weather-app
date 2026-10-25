package com.kampplus.hava.model

import androidx.annotation.StringRes
import com.kampplus.hava.R
import java.time.LocalDate

/**
 * Takip ettiğimiz polen türleri ve her birinin yoğunluk ölçeği.
 *
 * Polen yoğunluğu havadaki tane sayısıdır (tane/m³). Aynı sayı her tür için aynı anlama
 * gelmez: ağaç polenleri binlerle ölçülürken 20 tane çimen poleni bile alerjiyi tetikler.
 * Bu yüzden her türün kendi eşikleri vardır. Eşikler ABD Ulusal Alerji Bürosu'nun (NAB)
 * ağaç, çimen ve ot polenleri için kullandığı ölçektir.
 *
 * @param low Düşük seviyenin başladığı yoğunluk; altı "Yok" sayılır.
 * @param moderate Orta seviyenin başladığı yoğunluk.
 * @param high Yüksek seviyenin başladığı yoğunluk.
 * @param veryHigh Çok yüksek seviyenin başladığı yoğunluk.
 */
enum class PollenType(
    @StringRes val labelRes: Int,
    val emoji: String,
    private val low: Double,
    private val moderate: Double,
    private val high: Double,
    private val veryHigh: Double
) {
    GRASS(R.string.pollen_grass, "🌾", low = 1.0, moderate = 5.0, high = 20.0, veryHigh = 200.0),
    BIRCH(R.string.pollen_birch, "🌳", low = 1.0, moderate = 15.0, high = 90.0, veryHigh = 1500.0),
    ALDER(R.string.pollen_alder, "🌲", low = 1.0, moderate = 15.0, high = 90.0, veryHigh = 1500.0),
    OLIVE(R.string.pollen_olive, "🍃", low = 1.0, moderate = 15.0, high = 90.0, veryHigh = 1500.0),
    MUGWORT(R.string.pollen_mugwort, "🌿", low = 1.0, moderate = 10.0, high = 50.0, veryHigh = 500.0),
    RAGWEED(R.string.pollen_ragweed, "🌼", low = 1.0, moderate = 10.0, high = 50.0, veryHigh = 500.0);

    /** Yoğunluğu seviyeye çevirir; veri yoksa null. */
    fun levelOf(grainsPerCubicMeter: Double?): PollenLevel? = when {
        grainsPerCubicMeter == null -> null
        grainsPerCubicMeter >= veryHigh -> PollenLevel.VERY_HIGH
        grainsPerCubicMeter >= high -> PollenLevel.HIGH
        grainsPerCubicMeter >= moderate -> PollenLevel.MODERATE
        grainsPerCubicMeter >= low -> PollenLevel.LOW
        else -> PollenLevel.NONE
    }
}

/** Polen seviyesi; renkleri tema dosyasında, bkz. `ui/theme/WeatherColors.kt`. */
enum class PollenLevel(@StringRes val labelRes: Int) {
    NONE(R.string.pollen_level_none),
    LOW(R.string.pollen_level_low),
    MODERATE(R.string.pollen_level_moderate),
    HIGH(R.string.pollen_level_high),
    VERY_HIGH(R.string.pollen_level_very_high)
}

/**
 * Bir günün polen durumu: her tür için o günün en yüksek yoğunluğu (tane/m³).
 * Servis bir tür için veri göndermediyse o türün değeri null'dır.
 */
data class PollenDay(val date: LocalDate, val values: Map<PollenType, Double?>) {
    fun levelOf(type: PollenType): PollenLevel? = type.levelOf(values[type])
}

/**
 * Önümüzdeki günlerin polen tahmini.
 *
 * Polen verisi yalnızca Avrupa ve çevresi (Türkiye dahil) için vardır; başka bölgelerde
 * servis hiç değer göndermez ve tahmin hiç oluşturulmaz.
 */
data class PollenForecast(val days: List<PollenDay>) {

    /**
     * Bugün en az "Orta" seviyede olan türler, en yoğun olandan başlayarak.
     * Ekrandaki uyarı bunlardan kurulur.
     */
    fun alertsToday(): List<Pair<PollenType, PollenLevel>> {
        val today = days.firstOrNull() ?: return emptyList()
        return PollenType.entries
            .mapNotNull { type -> today.levelOf(type)?.let { type to it } }
            .filter { (_, level) -> level >= PollenLevel.MODERATE }
            .sortedByDescending { (_, level) -> level }
    }

    /** Önümüzdeki günlerde hiçbir türde polen yok mu? (Mevsim dışı.) */
    val isQuiet: Boolean
        get() = days.all { day -> PollenType.entries.all { (day.levelOf(it) ?: PollenLevel.NONE) == PollenLevel.NONE } }
}
