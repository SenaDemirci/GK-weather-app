package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.R
import com.kampplus.hava.model.PollenDay
import com.kampplus.hava.model.PollenForecast
import com.kampplus.hava.model.PollenLevel
import com.kampplus.hava.model.PollenType
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.theme.color
import com.kampplus.hava.ui.theme.raisedCard
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * "Polen ve Alerji" kartı: üstte herkese yönelik uyarı, altında 4 günlük renkli tablo.
 *
 * Polen verisi tahminden ayrı yüklenir; bu kart kendi yükleniyor / yok / hata hallerini
 * kendisi gösterir. Polen gelmese bile ekranın geri kalanı etkilenmez.
 */
@Composable
fun PollenCard(pollen: LoadState<PollenForecast>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.raisedCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (pollen) {
                LoadState.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
                LoadState.Empty -> InfoText(stringResource(R.string.pollen_unavailable))
                is LoadState.Error -> InfoText(stringResource(pollen.messageRes))
                is LoadState.Success -> {
                    PollenAlert(pollen.data)
                    if (!pollen.data.isQuiet) {
                        PollenTable(pollen.data)
                        PollenLegend()
                    }
                }
            }
        }
    }
}

/**
 * Bugünün uyarısı. Yüksek seviyede polen varsa turuncu çerçeveli, dikkat çekici bir kutu;
 * orta seviyede sarı; düşükse ya da hiç yoksa sakin bir cümle.
 */
@Composable
private fun PollenAlert(forecast: PollenForecast) {
    val alerts = forecast.alertsToday()
    val strongest = alerts.firstOrNull()?.second
    // `map` satır içi (inline) çalıştığı için içinde stringResource çağrılabilir; joinToString'in lambdası çağıramaz.
    val list = alerts.map { (type, level) -> "${stringResource(type.labelRes)} (${stringResource(level.labelRes)})" }.joinToString(", ")
    when {
        strongest != null && strongest >= PollenLevel.HIGH ->
            AlertBox(emoji = "⚠️", text = stringResource(R.string.pollen_alert_high, list), level = strongest)
        strongest == PollenLevel.MODERATE ->
            AlertBox(emoji = "🤧", text = stringResource(R.string.pollen_alert_moderate, list), level = strongest)
        forecast.isQuiet -> InfoText(stringResource(R.string.pollen_quiet))
        else -> InfoText(stringResource(R.string.pollen_alert_low))
    }
}

@Composable
private fun AlertBox(emoji: String, text: String, level: PollenLevel) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = level.color.copy(alpha = 0.16f),
        border = BorderStroke(2.dp, level.color)
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = emoji, fontSize = 26.sp)
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun InfoText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * 4 günlük tablo: her satır bir polen türü, her sütun bir gün. Hücrenin rengi o günün
 * en yüksek seviyesidir. Hiç verisi olmayan türler gösterilmez.
 */
@Composable
private fun PollenTable(forecast: PollenForecast) {
    val days = forecast.days
    val today = days.first().date
    val types = PollenType.entries.filter { type -> days.any { it.values[type] != null } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.pollen_type_header),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            days.forEach { day ->
                Text(
                    text = dayLabel(day.date, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(CELL_WIDTH)
                )
            }
        }
        types.forEach { type -> PollenRow(type, days) }
    }
}

@Composable
private fun PollenRow(type: PollenType, days: List<PollenDay>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = type.emoji, fontSize = 22.sp)
            Text(text = stringResource(type.labelRes), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
        days.forEach { day ->
            val level = day.levelOf(type)
            Box(modifier = Modifier.width(CELL_WIDTH), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = CELL_WIDTH - 8.dp, height = 28.dp)
                        .background((level ?: PollenLevel.NONE).color, MaterialTheme.shapes.small)
                )
            }
        }
    }
}

/** Renklerin anlamı: ● Yok ● Düşük ● Orta ● Yüksek ● Çok yüksek. */
@Composable
private fun PollenLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        PollenLevel.entries.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(level.color, CircleShape)
                )
                Text(text = stringResource(level.labelRes), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Sütun başlığı: "Bugün", "Yarın", sonra kısa gün adı ("Çar" / "Wed"). */
@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (date) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }
    }
}

private val CELL_WIDTH = 64.dp
