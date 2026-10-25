package com.kampplus.hava.model

import androidx.annotation.StringRes
import com.kampplus.hava.R
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * "Bugün ne giysem?" sorusunun cevabı: kısa bir cümle, öneri listesi ve ipuçları.
 *
 * [WeatherCondition] gibi ekran bilmez, ağ bilmez. Kurallar tek yerde durduğu için
 * birim testleriyle kolayca doğrulanır.
 *
 * Metinleri kendisi yazmaz; `R.string` kimliklerini döndürür. Cümleyi ekran, o anki
 * dile göre `stringResource` ile kurar. Böylece aynı kural Türkçe de İngilizce de konuşur.
 *
 * @param summaryRes Ekranda öneri listesinin üstünde gösterilen cümle.
 * @param items Giyilecek/yanına alınacak şeyler, önem sırasıyla.
 * @param tips "Şemsiyeni al" gibi yapılacak şeyler; en önemlisi başta.
 */
data class ClothingAdvice(@StringRes val summaryRes: Int, val items: List<ClothingItem>, val tips: List<ClothingTip> = emptyList()) {

    companion object {
        /**
         * Öneriyi üretir.
         *
         * Temel karar **hissedilen** sıcaklığa göre verilir; rüzgâr ve nem yüzünden
         * 10° bazen 5° gibi hissettirir. Servis hissedileni göndermezse gerçek sıcaklık kullanılır.
         * Üstüne yağış, kar, rüzgâr, güneş ve gün içindeki sıcaklık farkı için ekler yapılır.
         *
         * @param today Günün tahmini. Yağış olasılığı ve sıcaklık farkı buradan okunur;
         *   yoksa bu ekler atlanır.
         * @param upcomingHours Şu andan itibaren saatlik tahmin. Şimdi yağmıyor ama birkaç
         *   saat sonra yağacaksa "15:00 civarı yağmur bekleniyor" demek için kullanılır.
         */
        fun from(current: CurrentWeather, today: DailyForecast? = null, upcomingHours: List<HourlyForecast> = emptyList()): ClothingAdvice {
            val feelsLike = current.apparentTemperatureC ?: current.temperatureC
            val condition = WeatherCondition.fromWeatherCode(current.weatherCode)
            val band = TemperatureBand.of(feelsLike)

            val items = band.items.toMutableList()
            val tips = mutableListOf<ClothingTip>()

            rainTip(condition, today, upcomingHours)?.let { (tip, needsUmbrella) ->
                tips += tip
                if (needsUmbrella) items += ClothingItem.UMBRELLA
            }
            if (condition == WeatherCondition.THUNDERSTORM) {
                tips += ClothingTip("⚡", R.string.tip_thunderstorm)
            }
            if (condition == WeatherCondition.SNOW || condition == WeatherCondition.SNOW_SHOWERS) {
                items += ClothingItem.BOOTS
                tips += ClothingTip("🥾", R.string.tip_snow)
            }
            val wind = current.windSpeedKmh ?: 0.0
            if (wind >= WINDY_KMH) {
                items += ClothingItem.WINDBREAKER
                tips += ClothingTip("🌬️", R.string.tip_windy, listOf(wind.roundToInt()))
            }
            // UV, sıcaklıktan bağımsızdır: serin ama açık bir dağ gününde de yanılabilir.
            // Bu yüzden günün en yüksek UV'si "Yüksek" ve üstündeyse krem her zaman önerilir.
            val uvMax = today?.uvIndexMax
            val uvHigh = uvMax != null && UvLevel.of(uvMax) >= UvLevel.HIGH
            if (uvHigh) {
                items += ClothingItem.SUNGLASSES
                items += ClothingItem.SUNSCREEN
                tips += ClothingTip("🧴", R.string.tip_uv, listOf(uvMax.roundToInt()))
            }
            if (condition in SUNNY_CONDITIONS && feelsLike >= SUNNY_WARM_C) {
                items += ClothingItem.SUNGLASSES
                items += ClothingItem.SUNSCREEN
                // UV ipucu zaten verildiyse aynı şeyi iki kez söylemeyelim.
                if (!uvHigh) tips += ClothingTip("🕶️", R.string.tip_sunny)
            }
            if (today != null && today.maxTemperatureC - today.minTemperatureC >= LAYERING_RANGE_C) {
                items += ClothingItem.LAYERS
                val range = (today.maxTemperatureC - today.minTemperatureC).roundToInt()
                tips += ClothingTip("👚", R.string.tip_layers, listOf(range))
            }
            band.tip?.let { tips += it }

            return ClothingAdvice(summaryRes = band.sentenceRes, items = items.distinct(), tips = tips)
        }

        /**
         * Yağmur ipucunu seçer; en kesin bilgi önce gelir:
         * 1. Şu an yağıyor → "şemsiyeni al".
         * 2. Önümüzdeki saatlerde yağacak → saatiyle birlikte uyar.
         * 3. Yağış olasılığı yüksek → "yanına al". Saatlik veri varsa önümüzdeki saatlerin,
         *    yoksa günün olasılığına bakılır.
         * 4. Olasılık orta → "çantada dursun" (şemsiye kutucuğu eklenmez).
         *
         * @return İpucu ve şemsiye kutucuğunun eklenip eklenmeyeceği; yağmur yoksa null.
         */
        private fun rainTip(
            condition: WeatherCondition,
            today: DailyForecast?,
            upcomingHours: List<HourlyForecast>
        ): Pair<ClothingTip, Boolean>? {
            if (condition in WET_CONDITIONS) {
                return ClothingTip("☂️", R.string.tip_raining_now) to true
            }
            val rainyHour = upcomingHours
                .take(RAIN_LOOKAHEAD_HOURS)
                .firstOrNull { (it.precipitationProbability ?: 0) >= RAIN_CHANCE_THRESHOLD }
            if (rainyHour != null) {
                val time = rainyHour.time.format(HOUR_FORMAT)
                return ClothingTip("☂️", R.string.tip_rain_at, listOf(time)) to true
            }
            // Saatlik veri varsa ona güvenilir: günlük olasılık sabah yağıp geçmiş yağmuru da
            // kapsar, kalan saatler kuruysa şemsiye önermek yanlış olur.
            val chance = if (upcomingHours.isNotEmpty()) {
                upcomingHours.take(RAIN_LOOKAHEAD_HOURS).maxOf { it.precipitationProbability ?: 0 }
            } else {
                today?.precipitationProbability ?: 0
            }
            return when {
                chance >= RAIN_CHANCE_THRESHOLD ->
                    ClothingTip("☂️", R.string.tip_rain_likely, listOf(chance)) to true
                chance >= MAYBE_RAIN_CHANCE ->
                    ClothingTip("🌂", R.string.tip_rain_maybe, listOf(chance)) to false
                else -> null
            }
        }

        private val WET_CONDITIONS = setOf(
            WeatherCondition.DRIZZLE,
            WeatherCondition.RAIN,
            WeatherCondition.RAIN_SHOWERS,
            WeatherCondition.THUNDERSTORM
        )
        private val SUNNY_CONDITIONS = setOf(WeatherCondition.CLEAR, WeatherCondition.MAINLY_CLEAR)

        /** Bu olasılıktan itibaren şemsiye önerilir (%). */
        private const val RAIN_CHANCE_THRESHOLD = 50

        /** Bu olasılıktan itibaren "belki yağar" uyarısı verilir (%). */
        private const val MAYBE_RAIN_CHANCE = 30

        /** Saatlik tahminde kaç saat ileriye bakılır. */
        private const val RAIN_LOOKAHEAD_HOURS = 12

        /** Bu hızdan itibaren rüzgâr "sert" sayılır (km/sa). */
        private const val WINDY_KMH = 30.0

        /** Güneş gözlüğü ve krem için en düşük hissedilen sıcaklık. */
        private const val SUNNY_WARM_C = 20.0

        /** Gün içindeki fark bu kadar büyükse katmanlı giyim önerilir. */
        private const val LAYERING_RANGE_C = 10.0

        private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

/**
 * Yapılacak tek bir şey: bir emoji ve kısa bir cümle ("☂️ Şemsiyeni al.").
 *
 * @param args Cümledeki boşlukları dolduran değerler: saat ("15:00"), yüzde (70), hız (33).
 */
data class ClothingTip(val emoji: String, @StringRes val textRes: Int, val args: List<Any> = emptyList())

/** Tek bir öneri: bir emoji ve kısa bir etiket ("🧥 Mont"). */
enum class ClothingItem(val emoji: String, @StringRes val labelRes: Int) {
    HEAVY_COAT("🧥", R.string.item_heavy_coat),
    COAT("🧥", R.string.item_coat),
    JACKET("🧥", R.string.item_jacket),
    SWEATER("🧶", R.string.item_sweater),
    LONG_SLEEVE("👕", R.string.item_long_sleeve),
    T_SHIRT("👕", R.string.item_t_shirt),
    LIGHT_TOP("🎽", R.string.item_light_top),
    TROUSERS("👖", R.string.item_trousers),
    SHORTS("🩳", R.string.item_shorts),
    SCARF("🧣", R.string.item_scarf),
    GLOVES("🧤", R.string.item_gloves),
    BEANIE("🧢", R.string.item_beanie),
    BOOTS("🥾", R.string.item_boots),
    UMBRELLA("☂️", R.string.item_umbrella),
    WINDBREAKER("🌬️", R.string.item_windbreaker),
    SUNGLASSES("🕶️", R.string.item_sunglasses),
    SUNSCREEN("🧴", R.string.item_sunscreen),
    WATER("💧", R.string.item_water),
    LAYERS("👚", R.string.item_layers)
}

/**
 * Hissedilen sıcaklık aralıkları ve her birinin temel kıyafetleri.
 *
 * Sıra önemlidir: [of] ilk uyan aralığı seçer, bu yüzden soğuktan sıcağa dizilir.
 */
private enum class TemperatureBand(
    val upToC: Double,
    @StringRes val sentenceRes: Int,
    val items: List<ClothingItem>,
    val tip: ClothingTip? = null
) {
    FREEZING(
        0.0,
        R.string.clothing_freezing,
        listOf(ClothingItem.HEAVY_COAT, ClothingItem.SCARF, ClothingItem.GLOVES, ClothingItem.BEANIE),
        ClothingTip("🧤", R.string.tip_freezing)
    ),
    COLD(8.0, R.string.clothing_cold, listOf(ClothingItem.COAT, ClothingItem.SCARF, ClothingItem.TROUSERS)),
    COOL(15.0, R.string.clothing_cool, listOf(ClothingItem.JACKET, ClothingItem.SWEATER, ClothingItem.TROUSERS)),
    MILD(22.0, R.string.clothing_mild, listOf(ClothingItem.LONG_SLEEVE, ClothingItem.TROUSERS)),
    WARM(28.0, R.string.clothing_warm, listOf(ClothingItem.T_SHIRT, ClothingItem.SHORTS)),
    HOT(
        Double.MAX_VALUE,
        R.string.clothing_hot,
        listOf(ClothingItem.LIGHT_TOP, ClothingItem.SHORTS, ClothingItem.WATER),
        ClothingTip("💧", R.string.tip_hot)
    );

    companion object {
        fun of(feelsLikeC: Double): TemperatureBand = entries.first { feelsLikeC <= it.upToC }
    }
}
