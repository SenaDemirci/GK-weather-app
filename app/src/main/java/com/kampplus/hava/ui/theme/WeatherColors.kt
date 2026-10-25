package com.kampplus.hava.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kampplus.hava.model.UvLevel
import com.kampplus.hava.model.WeatherCondition

/**
 * Hava durumuna göre gökyüzü degradesi: açık havada parlak mavi, yağmurda
 * koyu lacivert, fırtınada mor.
 *
 * Renkler [Color.kt]'de tanımlıdır; bu fonksiyon yalnızca hangi durumda
 * hangisinin kullanılacağına karar verir. Üzerindeki yazı her zaman beyazdır.
 */
fun WeatherCondition.skyBrush(): Brush {
    val (top, bottom) = when (this) {
        WeatherCondition.CLEAR -> SkyClearTop to SkyClearBottom
        WeatherCondition.MAINLY_CLEAR, WeatherCondition.PARTLY_CLOUDY -> SkyCloudyTop to SkyCloudyBottom
        WeatherCondition.OVERCAST, WeatherCondition.FOG -> SkyOvercastTop to SkyOvercastBottom
        WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> SkyRainTop to SkyRainBottom
        WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> SkySnowTop to SkySnowBottom
        WeatherCondition.THUNDERSTORM -> SkyStormTop to SkyStormBottom
        WeatherCondition.UNKNOWN -> SkyCloudyTop to SkyCloudyBottom
    }
    return Brush.verticalGradient(listOf(top, bottom))
}

/** UV seviyesinin rengi; seviye noktasında ve ölçek çubuğunda kullanılır. */
val UvLevel.color: Color
    get() = when (this) {
        UvLevel.LOW -> UvGreen
        UvLevel.MODERATE -> UvYellow
        UvLevel.HIGH -> UvOrange
        UvLevel.VERY_HIGH -> UvRed
        UvLevel.EXTREME -> UvPurple
    }

/** UV ölçek çubuğunun renkleri, düşükten aşırıya. */
val UvScaleColors = UvLevel.entries.map { it.color }

/** Gökyüzü kartının üzerindeki yazı rengi. */
val OnSky = Color.White

/**
 * Tabletlerde içeriğin ulaşabileceği en geniş genişlik.
 *
 * Geniş ekranda kartlar kenardan kenara uzarsa satırı gözle takip etmek zorlaşır;
 * içeriği ortada tutarız.
 */
val MaxContentWidth = 720.dp
