package com.kampplus.hava.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kampplus.hava.model.PollenLevel
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

/** Polen seviyesinin rengi; UV ile aynı yeşil → kırmızı ölçek, "Yok" için gri. */
val PollenLevel.color: Color
    get() = when (this) {
        PollenLevel.NONE -> PollenNone
        PollenLevel.LOW -> UvGreen
        PollenLevel.MODERATE -> UvYellow
        PollenLevel.HIGH -> UvOrange
        PollenLevel.VERY_HIGH -> UvRed
    }

/**
 * Sıcaklığın haritadaki rengi. Her renk bir sıcaklığa bağlıdır; aradaki değerler iki
 * komşu rengin karışımıdır (`lerp`): 17° rengi, 15°'nin yeşili ile 20°'nin sarısı arasındadır.
 * Değer yoksa gri.
 */
fun temperatureColor(celsius: Double?): Color {
    if (celsius == null) return PollenNone
    val stops = TemperatureStops
    if (celsius <= stops.first().first) return stops.first().second
    if (celsius >= stops.last().first) return stops.last().second
    val upper = stops.indexOfFirst { it.first >= celsius }
    val (lowC, lowColor) = stops[upper - 1]
    val (highC, highColor) = stops[upper]
    return lerp(lowColor, highColor, ((celsius - lowC) / (highC - lowC)).toFloat())
}

/** Harita lejantı için ölçeğin durakları: (sıcaklık, renk). */
val TemperatureStops: List<Pair<Double, Color>> = listOf(
    -10.0 to TempFreezing,
    0.0 to TempCold,
    8.0 to TempCool,
    15.0 to TempMild,
    20.0 to TempWarm,
    25.0 to TempHot,
    30.0 to TempVeryHot,
    38.0 to TempExtreme
)

/**
 * Şehir ekranındaki "Türkiye sıcaklık haritası" düğmesinin zemini: haritanın ölçeğinden
 * koyu mavi → camgöbeği → turuncu. Yazının durduğu sol taraf koyu kalır, beyaz yazı okunur.
 */
val MapCardBrush: Brush = Brush.horizontalGradient(listOf(TempFreezing, TempCold, TempCool, TempHot))

/** UV ölçek çubuğunun renkleri, düşükten aşırıya. */
val UvScaleColors = UvLevel.entries.map { it.color }

/**
 * Tema düğmesinin zemini: "Açık" için gündüz mavisi, "Koyu" için gece laciverti,
 * "Sistem" için ikisinin yan yana geçişi. Üzerindeki yazı her zaman beyazdır ([OnSky]).
 */
val ThemeMode.pillBrush: Brush
    get() = when (this) {
        ThemeMode.LIGHT -> Brush.linearGradient(listOf(PillDayTop, PillDayBottom))
        ThemeMode.DARK -> Brush.linearGradient(listOf(PillNightTop, PillNightBottom))
        ThemeMode.SYSTEM -> Brush.horizontalGradient(listOf(PillDayBottom, PillNightBottom))
    }

/**
 * Zeminden "kalkık" duran kartların rengi: açık temada saf beyaz, koyu temada zeminden
 * belirgin biçimde açık bir gri.
 *
 * `surfaceContainerLowest` açık temada beyazdır ama koyu temada zeminden de koyudur;
 * bu yüzden temanın açık mı koyu mu olduğuna zeminin parlaklığından ([luminance]) bakarız.
 */
val ColorScheme.raisedCard: Color
    get() = if (background.luminance() > 0.5f) surfaceContainerLowest else surfaceContainerHigh

/** Gökyüzü kartının üzerindeki yazı rengi. */
val OnSky = Color.White

/**
 * Tabletlerde içeriğin ulaşabileceği en geniş genişlik.
 *
 * Geniş ekranda kartlar kenardan kenara uzarsa satırı gözle takip etmek zorlaşır;
 * içeriği ortada tutarız.
 */
val MaxContentWidth = 720.dp
