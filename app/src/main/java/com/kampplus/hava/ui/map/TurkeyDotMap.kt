package com.kampplus.hava.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.R
import com.kampplus.hava.model.Province
import com.kampplus.hava.model.ProvinceTemperature
import com.kampplus.hava.model.TurkeyOutline
import com.kampplus.hava.ui.theme.raisedCard
import com.kampplus.hava.ui.theme.temperatureColor
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Türkiye'nin nokta haritası: mavi denizin üstünde Türkiye'nin kara parçası
 * ([TurkeyOutline]), üzerinde her il merkezinin gerçek konumunda sıcaklığına göre
 * renklenmiş bir daire.
 *
 * **İzdüşüm.** Enlem/boylam düz bir ekrana çizilirken boylam derecesi kuzeye çıktıkça
 * kısalır; Türkiye'nin ortası olan 39° enlemde bir boylam derecesi bir enlem derecesinin
 * yaklaşık `cos(39°) ≈ 0,78` katıdır. Boylamı bununla çarpınca harita basık görünmez.
 *
 * **Yakınlaştırma.** İki parmakla büyütülüp kaydırılabilir; çift dokunuş eski haline döndürür.
 * Daireler yakınlaşınca büyümez, yalnızca birbirinden uzaklaşır; böylece Marmara'daki
 * sık iller ayrışır. Yakınlaşınca il adları da görünür.
 *
 * @param selectedPlate Seçili ilin plakası; büyük ve halkalı çizilir, adı yazılır.
 * @param onSelect Bir ile dokunulunca çağrılır.
 */
@Composable
fun TurkeyDotMap(
    temperatures: List<ProvinceTemperature>,
    selectedPlate: Int?,
    onSelect: (Province) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()
    val seaLabel = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val sea = MaterialTheme.colorScheme.primaryContainer
    val land = MaterialTheme.colorScheme.raisedCard
    val coast = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val ring = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurface
    val seaNames = SEAS.map { (res, lat, lon) -> Triple(stringResource(res), lat, lon) }
    val description = stringResource(R.string.map_title)

    Canvas(
        modifier = modifier
            .aspectRatio(ASPECT_RATIO)
            .clipToBounds()
            .semantics { contentDescription = description }
            // İki parmak: büyüt ve kaydır. Parmakların ortası (centroid) olduğu yerde kalır.
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                    val newOffset = (offset - centroid) * (newScale / scale) + centroid + pan
                    scale = newScale
                    offset = clampOffset(newOffset, newScale, Size(size.width.toFloat(), size.height.toFloat()))
                }
            }
            // Tek dokunuş: en yakın ili seç. Çift dokunuş: yakınlaştırmayı sıfırla.
            .pointerInput(temperatures) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    onTap = { tap ->
                        val canvas = Size(size.width.toFloat(), size.height.toFloat())
                        val hit = temperatures.minByOrNull { (project(it.province, canvas) * scale + offset - tap).getDistance() }
                        if (hit != null && (project(hit.province, canvas) * scale + offset - tap).getDistance() < TAP_RADIUS.toPx()) {
                            onSelect(hit.province)
                        }
                    }
                )
            }
    ) {
        fun screen(lat: Double, lon: Double) = projectLatLon(lat, lon, size) * scale + offset

        // 1. Deniz: bütün zemin mavi.
        drawRect(color = sea)
        // 2. Kara: Anadolu ve Trakya, ince bir kıyı çizgisiyle.
        listOf(TurkeyOutline.anatolia, TurkeyOutline.thrace).forEach { outline ->
            val path = Path().apply {
                outline.forEachIndexed { index, (lat, lon) ->
                    val point = screen(lat, lon)
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
            drawPath(path, color = land)
            drawPath(path, color = coast, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
        }
        // 3. Göller: Van ve Tuz Gölü, deniz renginde elips.
        TurkeyOutline.lakes.forEach { lake ->
            val topLeft = screen(lake.latitude + lake.latitudeRadius, lake.longitude - lake.longitudeRadius)
            val bottomRight = screen(lake.latitude - lake.latitudeRadius, lake.longitude + lake.longitudeRadius)
            drawOval(color = sea, topLeft = topLeft, size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y))
        }
        seaNames.forEach { (name, lat, lon) ->
            drawLabel(textMeasurer, name, screen(lat, lon), TextStyle(color = seaLabel, fontSize = 13.sp, fontStyle = FontStyle.Italic))
        }

        val radius = DOT_RADIUS.toPx()
        // Seçili il en son çizilir ki diğerlerinin üstünde kalsın.
        temperatures.sortedBy { it.province.plate == selectedPlate }.forEach { item ->
            val center = project(item.province, size) * scale + offset
            val selected = item.province.plate == selectedPlate
            val color = temperatureColor(item.meanC)
            val r = if (selected) radius * 1.45f else radius
            if (selected) drawCircle(color = ring, radius = r + 3.dp.toPx(), center = center)
            drawCircle(color = color, radius = r, center = center)
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = r, center = center, style = Stroke(width = 1.5.dp.toPx()))
            item.meanC?.let { temp ->
                val textColor = if (color.luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White
                drawLabel(
                    textMeasurer,
                    "${temp.roundToInt()}°",
                    center,
                    TextStyle(color = textColor, fontSize = if (selected) 12.sp else 9.sp, fontWeight = FontWeight.Bold)
                )
            }
            // Adı: seçili ilde her zaman, yakınlaşınca bütün illerde.
            if (selected || scale >= NAMES_SCALE) {
                drawLabel(
                    textMeasurer,
                    item.province.name,
                    center + Offset(0f, r + 9.dp.toPx()),
                    TextStyle(color = labelColor, fontSize = if (selected) 13.sp else 10.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

/** Metni verilen noktaya ortalayarak yazar. */
private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

/** İlin merkezini, yakınlaştırma uygulanmamış harita koordinatına çevirir. */
private fun project(province: Province, size: Size): Offset = projectLatLon(province.latitude, province.longitude, size)

private fun projectLatLon(lat: Double, lon: Double, size: Size): Offset {
    val x = (lon - MIN_LON) / (MAX_LON - MIN_LON)
    val y = (MAX_LAT - lat) / (MAX_LAT - MIN_LAT)
    return Offset((x * size.width).toFloat(), (y * size.height).toFloat())
}

/** Kaydırmayı sınırlar: harita yakınlaşınca bile ekranın dışına kaçamaz. */
private fun clampOffset(offset: Offset, scale: Float, size: Size): Offset = Offset(
    x = offset.x.coerceIn(size.width * (1 - scale), 0f),
    y = offset.y.coerceIn(size.height * (1 - scale), 0f)
)

// Türkiye'yi ve denizlerin adlarını içine alan çerçeve (derece).
private const val MIN_LON = 25.3
private const val MAX_LON = 45.3
private const val MIN_LAT = 35.6
private const val MAX_LAT = 42.5

/** Genişlik / yükseklik oranı; boylam, 39° enlemdeki kısalmayla düzeltilir. */
private val ASPECT_RATIO = ((MAX_LON - MIN_LON) * cos(Math.toRadians(39.0)) / (MAX_LAT - MIN_LAT)).toFloat()

private const val MAX_SCALE = 5f
private const val NAMES_SCALE = 2.2f
private val DOT_RADIUS = 12.dp
private val TAP_RADIUS = 28.dp

/** Denizlerin adları ve haritada yazılacakları yer: (metin, enlem, boylam). */
private val SEAS = listOf(
    Triple(R.string.sea_black, 42.25, 32.5),
    Triple(R.string.sea_mediterranean, 35.85, 31.5),
    Triple(R.string.sea_aegean, 38.6, 25.9)
)
