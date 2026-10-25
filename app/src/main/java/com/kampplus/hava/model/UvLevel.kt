package com.kampplus.hava.model

import androidx.annotation.StringRes
import com.kampplus.hava.R

/**
 * UV indeksinin anlamı: sayı tek başına bir şey söylemez, "6" yüksek midir?
 *
 * Aralıklar Dünya Sağlık Örgütü'nün ölçeğidir. Her seviyenin ekranda bir rengi
 * vardır (yeşil → mor); renkler tema dosyasında, bkz. `ui/theme/WeatherColors.kt`.
 *
 * @param from Seviyenin başladığı değer (dahil). Bir sonraki seviyenin [from]'una kadar sürer.
 */
enum class UvLevel(val from: Double, @StringRes val labelRes: Int) {
    LOW(0.0, R.string.uv_low),
    MODERATE(3.0, R.string.uv_moderate),
    HIGH(6.0, R.string.uv_high),
    VERY_HIGH(8.0, R.string.uv_very_high),
    EXTREME(11.0, R.string.uv_extreme);

    companion object {
        /** Ölçeğin ekrandaki üst sınırı; çubuk 0'dan buraya kadar çizilir. */
        const val SCALE_MAX = 12.0

        /**
         * Servis UV'yi ondalıklı gönderir (2.6). Ölçek tam sayılarla tanımlı olduğu için
         * önce yuvarlarız: 5.6 → 6, yani "Yüksek".
         */
        fun of(index: Double): UvLevel {
            val rounded = Math.round(index).toDouble()
            return entries.last { rounded >= it.from }
        }
    }
}
