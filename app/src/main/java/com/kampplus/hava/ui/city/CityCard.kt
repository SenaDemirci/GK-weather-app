package com.kampplus.hava.ui.city

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kampplus.hava.model.City
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.theme.raisedCard

/**
 * Listedeki tek bir şehir satırı.
 *
 * Yapısı: `Card` → `Row` → rozet + `Column(weight(1f))` (ad + konum) + daire içinde sağ ok.
 *
 * `Modifier.weight(1f)`: ortadaki sütun kalan boşluğun tamamını kaplar, böylece
 * sağ oku sağ kenara iter. Uzun şehir adları (Afyonkarahisar) rozeti ezmez.
 *
 * @param onClick Karta dokunulduğunda çağrılır. Bileşen ne olacağını bilmez,
 *   yalnızca haber verir — olay yukarı, veri aşağı (state hoisting).
 */
@Composable
fun CityCard(city: City, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        // Beyaz kart, ince çerçeve ve gölge: açık mavi zeminden belirgin biçimde ayrılır.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.raisedCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CountryBadge(code = city.countryCode)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleLarge,
                    // Tek satıra sığmayan ad "..." ile kısaltılır, satır kaymaz.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = city.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Ok, açık mavi bir dairenin içinde: kartın dokunulabilir olduğu ilk bakışta anlaşılır.
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    // Ok yalnızca görseldir; "dokunulabilir" bilgisini Card zaten verir.
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

/**
 * Ülke rozeti: bayrak emojisi ve altında ülke kodu ("🇹🇷 TR").
 *
 * Veri API'den geldiği için elimizde plaka numarası yok; rozete ülkeyi
 * koyuyoruz. Kod gelmezse "?" gösterilir.
 */
@Composable
private fun CountryBadge(code: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(52.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val flag = code?.let(::flagEmoji)
            if (flag != null) Text(text = flag, style = MaterialTheme.typography.titleLarge)
            Text(text = code ?: "?", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * "TR" → "🇹🇷". Bayrak emojileri, ülke kodunun her harfinin "bölge göstergesi"
 * karşılığının yan yana yazılmasından oluşur (A → 🇦, ... Z → 🇿).
 *
 * İki büyük harf değilse null döner; rozet yalnızca kodu gösterir.
 */
internal fun flagEmoji(code: String): String? {
    if (code.length != 2 || code.any { it !in 'A'..'Z' }) return null
    return buildString {
        code.forEach { appendCodePoint(REGIONAL_INDICATOR_A + (it - 'A')) }
    }
}

private const val REGIONAL_INDICATOR_A = 0x1F1E6

@Preview(showBackground = true)
@Composable
private fun CityCardPreview() {
    HavaTheme {
        CityCard(
            city = City(
                id = 323786,
                name = "Ankara",
                latitude = 39.91987,
                longitude = 32.85427,
                countryCode = "TR",
                country = "Türkiye",
                admin1 = "Ankara"
            ),
            onClick = {}
        )
    }
}
