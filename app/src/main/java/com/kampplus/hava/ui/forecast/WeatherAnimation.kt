package com.kampplus.hava.ui.forecast

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.kampplus.hava.model.WeatherCondition
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tahmin ekranındaki büyük kartın arkasında oynayan canlı hava animasyonu:
 * güneşte dönen ışınlar, bulutlu havada süzülen bulutlar, yağmurda düşen damlalar,
 * karda salınarak inen kar taneleri, fırtınada şimşek çakması, siste kayan pus.
 *
 * **Nasıl çalışır?** Tek bir sayaç ([rememberInfiniteTransition]) [LOOP_MILLIS] sürede
 * 0'dan 1'e gider ve başa döner. Her çizimde bu sayaçtan her parçacığın yeri hesaplanır:
 * bir damlanın yüksekliği `(başlangıç + zaman × hız) mod 1`'dir. Hızlar tam sayı
 * olduğu için sayaç başa döndüğünde her parçacık tam başladığı yerdedir; döngü atlamaz.
 *
 * Parçacıkların başlangıç yerleri sabit bir rastgele tohumla bir kez üretilir
 * ([remember]); her çizimde yeniden üretilmez, ekran titremez.
 *
 * Çizimler yarı saydam beyazdır; kartın üzerindeki yazılar okunur kalır.
 */
@Composable
fun WeatherAnimation(condition: WeatherCondition, modifier: Modifier = Modifier) {
    val time by rememberInfiniteTransition(label = "weather").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = LOOP_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "weatherTime"
    )
    val scene = remember(condition) { Scene.of(condition) }
    val particles = remember { Particles(Random(SEED)) }

    Canvas(modifier = modifier) {
        if (scene.sun) drawSun(time)
        if (scene.fog) drawFog(time, particles)
        scene.clouds.takeIf { it > 0 }?.let { drawClouds(time, particles, count = it, alpha = scene.cloudAlpha) }
        if (scene.rainDrops > 0) drawRain(time, particles, count = scene.rainDrops)
        if (scene.snowFlakes > 0) drawSnow(time, particles, count = scene.snowFlakes)
        if (scene.lightning) drawLightning(time)
    }
}

/** Bir hava durumunda hangi efektlerin, ne yoğunlukta oynayacağı. */
private data class Scene(
    val sun: Boolean = false,
    val clouds: Int = 0,
    val cloudAlpha: Float = 0.2f,
    val rainDrops: Int = 0,
    val snowFlakes: Int = 0,
    val lightning: Boolean = false,
    val fog: Boolean = false
) {
    companion object {
        fun of(condition: WeatherCondition): Scene = when (condition) {
            WeatherCondition.CLEAR -> Scene(sun = true)
            WeatherCondition.MAINLY_CLEAR -> Scene(sun = true, clouds = 2, cloudAlpha = 0.18f)
            WeatherCondition.PARTLY_CLOUDY -> Scene(sun = true, clouds = 4)
            WeatherCondition.OVERCAST -> Scene(clouds = 6, cloudAlpha = 0.16f)
            WeatherCondition.FOG -> Scene(fog = true, clouds = 2, cloudAlpha = 0.14f)
            WeatherCondition.DRIZZLE -> Scene(clouds = 4, rainDrops = 35)
            WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> Scene(clouds = 5, rainDrops = 90)
            WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> Scene(clouds = 3, cloudAlpha = 0.16f, snowFlakes = 60)
            WeatherCondition.THUNDERSTORM -> Scene(clouds = 6, cloudAlpha = 0.18f, rainDrops = 110, lightning = true)
            WeatherCondition.UNKNOWN -> Scene()
        }
    }
}

/** Parçacıkların sabit başlangıç değerleri (0–1 arası oranlar). */
private class Particles(random: Random) {
    val drops = List(MAX_PARTICLES) { Triple(random.nextFloat(), random.nextFloat(), random.nextFloat()) }
    val clouds = List(8) { Triple(random.nextFloat(), random.nextFloat(), random.nextFloat()) }
}

/** Sağ üstte, emojinin arkasında parlayan ve yavaşça dönen güneş. */
private fun DrawScope.drawSun(time: Float) {
    val center = Offset(size.width - 70.dp.toPx(), 80.dp.toPx())
    val pulse = 1f + 0.08f * sin(time * 2 * PI.toFloat() * 3)
    drawCircle(
        brush = Brush.radialGradient(listOf(SunGlow.copy(alpha = 0.55f), Color.Transparent), center, 120.dp.toPx() * pulse),
        radius = 120.dp.toPx() * pulse,
        center = center
    )
    rotate(degrees = time * 360f, pivot = center) {
        repeat(12) { i ->
            rotate(degrees = i * 30f, pivot = center) {
                drawLine(
                    color = SunGlow.copy(alpha = 0.45f),
                    start = center + Offset(0f, 62.dp.toPx()),
                    end = center + Offset(0f, (82 + 10 * pulse).dp.toPx()),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/** Soldan sağa süzülen bulutlar; kartın sağından çıkan bulut soldan yeniden girer. */
private fun DrawScope.drawClouds(time: Float, particles: Particles, count: Int, alpha: Float) {
    val cloudWidth = 150.dp.toPx()
    particles.clouds.take(count).forEachIndexed { index, (startX, startY, scale) ->
        // Yarısı bir, yarısı iki tur atar: bulutlar farklı hızlarda kayar.
        val laps = if (index % 2 == 0) 1 else 2
        val x = ((startX + time * laps) % 1f) * (size.width + cloudWidth) - cloudWidth
        val y = size.height * (0.05f + startY * 0.45f)
        drawCloud(Offset(x, y), sizeScale = 0.7f + scale * 0.6f, alpha = alpha)
    }
}

/** Üç daire ve yuvarlak tabandan oluşan pamuk gibi bir bulut. */
private fun DrawScope.drawCloud(topLeft: Offset, sizeScale: Float, alpha: Float) {
    val unit = 40.dp.toPx() * sizeScale
    val color = Color.White.copy(alpha = alpha)
    drawCircle(color, radius = unit * 0.9f, center = topLeft + Offset(unit * 1.2f, unit * 1.1f))
    drawCircle(color, radius = unit * 1.2f, center = topLeft + Offset(unit * 2.2f, unit * 0.9f))
    drawCircle(color, radius = unit * 0.85f, center = topLeft + Offset(unit * 3.2f, unit * 1.2f))
    drawRoundRect(
        color = color,
        topLeft = topLeft + Offset(unit * 0.6f, unit * 1.2f),
        size = Size(unit * 3.2f, unit * 0.9f),
        cornerRadius = CornerRadius(unit * 0.45f)
    )
}

/** Hafif eğik düşen yağmur damlaları. Her damla döngü boyunca [RAIN_LAPS] kez düşer. */
private fun DrawScope.drawRain(time: Float, particles: Particles, count: Int) {
    val length = 18.dp.toPx()
    val slant = 5.dp.toPx()
    particles.drops.take(count).forEach { (startX, startY, speed) ->
        val laps = RAIN_LAPS + (speed * 4).toInt()
        val y = ((startY + time * laps) % 1f) * (size.height + length) - length
        val x = startX * size.width
        drawLine(
            color = Color.White.copy(alpha = 0.35f + speed * 0.3f),
            start = Offset(x, y),
            end = Offset(x - slant, y + length),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/** Sağa sola salınarak yavaşça inen kar taneleri. */
private fun DrawScope.drawSnow(time: Float, particles: Particles, count: Int) {
    val sway = 14.dp.toPx()
    particles.drops.take(count).forEach { (startX, startY, size01) ->
        val laps = SNOW_LAPS + (size01 * 2).toInt()
        val y = ((startY + time * laps) % 1f) * (size.height + 20f) - 10f
        val x = startX * size.width + sway * sin((time * laps + startX) * 2 * PI.toFloat())
        drawCircle(Color.White.copy(alpha = 0.55f + size01 * 0.35f), radius = (2.5f + size01 * 3f).dp.toPx(), center = Offset(x, y))
    }
}

/** Yavaşça kayan, yatay pus bantları. */
private fun DrawScope.drawFog(time: Float, particles: Particles) {
    particles.clouds.take(4).forEachIndexed { index, (startX, startY, _) ->
        val bandHeight = 34.dp.toPx()
        val y = size.height * (0.2f + startY * 0.65f)
        val shift = ((startX + time * (if (index % 2 == 0) 1 else -1)) % 1f + 1f) % 1f
        val x = shift * size.width - size.width * 0.5f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                startX = x,
                endX = x + size.width
            ),
            topLeft = Offset(0f, y),
            size = Size(size.width, bandHeight),
            cornerRadius = CornerRadius(bandHeight / 2)
        )
    }
}

/**
 * Şimşek: döngünün iki anında kısa, art arda iki beyaz parlama. Parlaklık zamana
 * göre çizilir; ayrı bir zamanlayıcı gerekmez.
 */
private fun DrawScope.drawLightning(time: Float) {
    val alpha = when (time) {
        in 0.30f..0.31f, in 0.70f..0.705f -> 0.45f
        in 0.325f..0.335f, in 0.715f..0.72f -> 0.3f
        else -> 0f
    }
    if (alpha > 0f) drawRect(Color.White.copy(alpha = alpha))
}

/** Bir tur süresi; parçacıkların hızları bu sürenin tam katı olarak tanımlanır. */
private const val LOOP_MILLIS = 12_000
private const val RAIN_LAPS = 8
private const val SNOW_LAPS = 2
private const val MAX_PARTICLES = 120
private const val SEED = 7

/** Güneşin parıltısı: sıcak, açık bir sarı. */
private val SunGlow = Color(0xFFFFE082)
