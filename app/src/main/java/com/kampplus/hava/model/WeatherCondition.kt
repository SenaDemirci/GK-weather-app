package com.kampplus.hava.model

import androidx.annotation.StringRes
import com.kampplus.hava.R

/**
 * Hava durumunun ekrandaki karşılığı: bir emoji ve bir etiket. Etiket bir `R.string`
 * kimliğidir; ekranda o anki dile göre ("Yağmurlu" / "Rain") yazılır.
 *
 * Open-Meteo, sıcaklığın yanında bir de `weather_code` gönderir. Bu kod WMO 4677
 * standardındadır ve tek başına bir sayıdır (0, 61, 95...). Kullanıcıya sayı
 * gösteremeyiz; [fromWeatherCode] ile anlaşılır bir görünüme çeviririz.
 */
enum class WeatherCondition(val emoji: String, @StringRes val labelRes: Int) {
    CLEAR("☀️", R.string.condition_clear),
    MAINLY_CLEAR("🌤️", R.string.condition_mainly_clear),
    PARTLY_CLOUDY("⛅", R.string.condition_partly_cloudy),
    OVERCAST("☁️", R.string.condition_overcast),
    FOG("🌫️", R.string.condition_fog),
    DRIZZLE("🌦️", R.string.condition_drizzle),
    RAIN("🌧️", R.string.condition_rain),
    SNOW("🌨️", R.string.condition_snow),
    RAIN_SHOWERS("🌦️", R.string.condition_rain_showers),
    SNOW_SHOWERS("🌨️", R.string.condition_snow_showers),
    THUNDERSTORM("⛈️", R.string.condition_thunderstorm),
    UNKNOWN("🌡️", R.string.condition_unknown);

    companion object {
        /**
         * WMO 4677 kod tablosu. `when` ifadesi birden çok kodu tek bir görünüme eşler
         * (örneğin 61, 63 ve 65 hafif/orta/şiddetli yağmurdur; üçü de "Yağmurlu").
         *
         * Tanınmayan bir kod gelirse uygulama çökmez, [UNKNOWN] ile devam eder.
         */
        fun fromWeatherCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1 -> MAINLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 66, 67 -> RAIN
            71, 73, 75, 77 -> SNOW
            80, 81, 82 -> RAIN_SHOWERS
            85, 86 -> SNOW_SHOWERS
            95, 96, 99 -> THUNDERSTORM
            else -> UNKNOWN
        }

        /**
         * Kodun ayrıntılı, cümle içinde kullanılabilen karşılığı: "Hafif yağmurlu",
         * "Kuvvetli sağanak yağışlı", "Dolu ile fırtına"...
         *
         * [fromWeatherCode] kodları kabaca gruplar (emoji ve animasyon için yeterli);
         * bu fonksiyon ise WMO tablosundaki şiddet farkını da söyler: 61, 63 ve 65 hepsi
         * yağmurdur ama sırasıyla hafif, orta ve şiddetli. Tanınmayan kod için grubun
         * genel adı döner.
         */
        @StringRes
        fun detailLabelRes(code: Int): Int = when (code) {
            0 -> R.string.weather_clear
            1 -> R.string.weather_mainly_clear
            2 -> R.string.weather_partly_cloudy
            3 -> R.string.weather_overcast
            45 -> R.string.weather_fog
            48 -> R.string.weather_rime_fog
            51 -> R.string.weather_drizzle_light
            53 -> R.string.weather_drizzle
            55 -> R.string.weather_drizzle_dense
            56, 57 -> R.string.weather_freezing_drizzle
            61 -> R.string.weather_rain_light
            63 -> R.string.weather_rain
            65 -> R.string.weather_rain_heavy
            66, 67 -> R.string.weather_freezing_rain
            71 -> R.string.weather_snow_light
            73 -> R.string.weather_snow
            75 -> R.string.weather_snow_heavy
            77 -> R.string.weather_snow_grains
            80 -> R.string.weather_showers_light
            81 -> R.string.weather_showers
            82 -> R.string.weather_showers_violent
            85 -> R.string.weather_snow_showers_light
            86 -> R.string.weather_snow_showers_heavy
            95 -> R.string.weather_thunderstorm
            96, 99 -> R.string.weather_thunderstorm_hail
            else -> fromWeatherCode(code).labelRes
        }
    }
}
