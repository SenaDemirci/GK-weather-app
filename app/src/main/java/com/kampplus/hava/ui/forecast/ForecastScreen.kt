package com.kampplus.hava.ui.forecast

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.ClothingAdvice
import com.kampplus.hava.model.ClothingItem
import com.kampplus.hava.model.ClothingTip
import com.kampplus.hava.model.CurrentWeather
import com.kampplus.hava.model.DailyForecast
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.HourRange
import com.kampplus.hava.model.HourlyForecast
import com.kampplus.hava.model.PollenForecast
import com.kampplus.hava.model.UvLevel
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.assistant.AssistantCity
import com.kampplus.hava.ui.assistant.AssistantSheet
import com.kampplus.hava.ui.assistant.AssistantViewModel
import com.kampplus.hava.ui.assistant.CitySuggestions
import com.kampplus.hava.ui.assistant.RobotMascot
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.theme.MaxContentWidth
import com.kampplus.hava.ui.theme.OnSky
import com.kampplus.hava.ui.theme.UvScaleColors
import com.kampplus.hava.ui.theme.color
import com.kampplus.hava.ui.theme.raisedCard
import com.kampplus.hava.ui.theme.skyBrush
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Tahmin ekranının bağlayıcı katmanı; ViewModel'i edinir ve state'i aktarır.
 * (Bkz. [com.kampplus.hava.ui.city.CityRoute] — aynı desen.)
 */
@Composable
fun ForecastRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ForecastViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pollen by viewModel.pollen.collectAsStateWithLifecycle()

    ForecastScreen(
        cityName = viewModel.cityName,
        subtitle = viewModel.subtitle,
        uiState = uiState,
        pollen = pollen,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        modifier = modifier
    )
}

/**
 * Bir şehrin hava tahmini.
 *
 * Üç bölüm: anlık durum başlığı, saatlik yatay liste, günlük dikey liste.
 * Dört durumun tamamı burada da karşılanır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    cityName: String,
    subtitle: String,
    uiState: LoadState<Forecast>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    pollen: LoadState<PollenForecast> = LoadState.Empty
) {
    // Robot paneli açık mı; ekran dönünce korunur. Sohbetin kendisi ViewModel'de yaşar.
    var assistantOpen by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        // Robot yalnızca tahmin geldiğinde görünür; boş tahminle konuşacak bir şey yok.
        floatingActionButton = {
            if (uiState is LoadState.Success) {
                RobotMascot(
                    message = stringResource(R.string.bot_hello_forecast),
                    onClick = { assistantOpen = true },
                    contentDescription = stringResource(R.string.bot_open)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(cityName.ifEmpty { stringResource(R.string.detail_title) }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                // Tek bir kart beklendiği için burada Shimmer yerine basit gösterge yeterli.
                LoadState.Loading -> CircularProgressIndicator()
                LoadState.Empty -> ErrorView(message = stringResource(R.string.detail_empty))
                is LoadState.Error -> ErrorView(message = stringResource(uiState.messageRes), onRetry = onRetry)
                is LoadState.Success -> ForecastContent(subtitle = subtitle, forecast = uiState.data, pollen = pollen)
            }
        }
    }
    if (assistantOpen && uiState is LoadState.Success) {
        // Sohbet bu şehrin tahminiyle başlar; kullanıcı başka bir şehir yazarsa ona geçer.
        val assistant = viewModel(key = "assistant-forecast") {
            AssistantViewModel(initialCity = AssistantCity(cityName, subtitle, uiState.data))
        }
        AssistantSheet(
            viewModel = assistant,
            cityName = cityName,
            suggestions = CitySuggestions,
            onDismiss = { assistantOpen = false }
        )
    }
}

@Composable
private fun ForecastContent(subtitle: String, forecast: Forecast, pollen: LoadState<PollenForecast>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // İçerik ekrana sığmayabilir; dikey kaydırma ekliyoruz.
            .verticalScroll(rememberScrollState())
            // Alttaki boşluk, robot düğmesi son kartın üstünü kapatmasın diye.
            .padding(top = 8.dp, bottom = 96.dp),
        // Tablette içerik ortada, en fazla [MaxContentWidth] genişliğinde durur.
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val contentModifier = Modifier
            .widthIn(max = MaxContentWidth)
            .fillMaxWidth()
        // "Şimdi" telefonun saati değil, şehrin saatidir: Londra'da 10:30'ken İstanbul'daki
        // telefon 13:30 gösterir. Servisin gözlem zamanı şehrin yerel saatinde geldiği
        // için onu saat başına yuvarlayıp kullanıyoruz.
        val now = forecast.current.observedAt.truncatedTo(ChronoUnit.HOURS)
        val upcomingHours = forecast.hourly.filter { !it.time.isBefore(now) }
        CurrentWeatherHeader(
            subtitle = subtitle,
            current = forecast.current,
            today = forecast.daily.firstOrNull(),
            modifier = contentModifier.padding(horizontal = 16.dp)
        )
        SectionTitle(stringResource(R.string.detail_clothing), contentModifier)
        ClothingCard(
            advice = ClothingAdvice.from(
                current = forecast.current,
                today = forecast.daily.firstOrNull(),
                upcomingHours = upcomingHours
            ),
            modifier = contentModifier.padding(horizontal = 16.dp)
        )
        forecast.daily.firstOrNull()?.let { today ->
            SectionTitle(stringResource(R.string.detail_sun), contentModifier)
            SunCard(
                today = today,
                currentUv = forecast.current.uvIndex,
                modifier = contentModifier.padding(horizontal = 16.dp)
            )
        }
        SectionTitle(stringResource(R.string.detail_pollen), contentModifier)
        PollenCard(pollen = pollen, modifier = contentModifier.padding(horizontal = 16.dp))
        SectionTitle(stringResource(R.string.detail_hourly), contentModifier)
        // Filtre her zaman aynı 24 saatlik pencereye uygulanır; "Sabah" yarının sabahı da olabilir.
        // `rememberSaveable`: ekran dönünce seçili aralık kaybolmaz.
        var hourRange by rememberSaveable { mutableStateOf(HourRange.ALL) }
        HourRangeFilter(selected = hourRange, onSelect = { hourRange = it }, modifier = contentModifier)
        HourlyRow(
            hours = hourRange.filter(upcomingHours.take(HOURLY_COUNT)),
            now = now,
            modifier = contentModifier.padding(top = 12.dp)
        )
        SectionTitle(stringResource(R.string.detail_daily), contentModifier)
        DailyCard(days = forecast.daily, modifier = contentModifier.padding(horizontal = 16.dp))
    }
}

/**
 * Ekranın yıldızı: hava durumuna göre renklenen büyük kart.
 *
 * Zemin [skyBrush] ile çizilir; açık havada parlak mavi, yağmurda koyu lacivert.
 * İçinde şehir, büyük sıcaklık, durum, günün en yüksek/en düşüğü ve
 * hissedilen/nem/rüzgâr üçlüsü yer alır.
 */
@Composable
private fun CurrentWeatherHeader(subtitle: String, current: CurrentWeather, today: DailyForecast?, modifier: Modifier = Modifier) {
    val condition = WeatherCondition.fromWeatherCode(current.weatherCode)
    // Emoji yavaşça yukarı aşağı süzülür; kartın arkasında hava durumuna göre animasyon oynar.
    val float by rememberInfiniteTransition(label = "heroEmoji").animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2_000), RepeatMode.Reverse),
        label = "heroEmojiFloat"
    )
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(condition.skyBrush())
    ) {
        WeatherAnimation(condition = condition, modifier = Modifier.matchParentSize())
        HeroContent(subtitle = subtitle, current = current, today = today, condition = condition, emojiOffsetDp = float)
    }
}

@Composable
private fun HeroContent(
    subtitle: String,
    current: CurrentWeather,
    today: DailyForecast?,
    condition: WeatherCondition,
    emojiOffsetDp: Float
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = subtitle, style = MaterialTheme.typography.titleMedium, color = OnSky.copy(alpha = 0.85f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = degrees(current.temperatureC),
                style = MaterialTheme.typography.displayLarge,
                color = OnSky,
                modifier = Modifier.weight(1f)
            )
            Text(text = condition.emoji, fontSize = 72.sp, modifier = Modifier.offset(y = emojiOffsetDp.dp))
        }
        // Ayrıntılı ifade: "Kapalı" yerine örneğin "Hafif sağanak yağışlı".
        val nowLabel = WeatherCondition.detailLabelRes(current.weatherCode)
        Text(text = stringResource(nowLabel), style = MaterialTheme.typography.headlineSmall, color = OnSky)
        // Gün içinde başka bir hava bekleniyorsa onu da söyle: "Gün içinde: Sağanak yağışlı".
        today?.let { WeatherCondition.detailLabelRes(it.weatherCode) }?.takeIf { it != nowLabel }?.let { laterLabel ->
            Text(
                text = stringResource(R.string.detail_later_today, stringResource(laterLabel)),
                style = MaterialTheme.typography.titleMedium,
                color = OnSky
            )
        }
        today?.let {
            Text(
                text = stringResource(R.string.detail_high_low, degrees(it.maxTemperatureC), degrees(it.minTemperatureC)),
                style = MaterialTheme.typography.titleMedium,
                color = OnSky.copy(alpha = 0.85f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bu üç alan servis tarafından gönderilmeyebilir; yalnızca varsa çizilir.
            current.apparentTemperatureC?.let {
                Metric("🌡️", stringResource(R.string.detail_feels_like), degrees(it), Modifier.weight(1f))
            }
            current.humidityPercent?.let {
                Metric("💧", stringResource(R.string.detail_humidity), stringResource(R.string.percent, it), Modifier.weight(1f))
            }
            current.windSpeedKmh?.let {
                Metric(
                    "💨",
                    stringResource(R.string.detail_wind),
                    stringResource(R.string.wind_speed, it.roundToInt()),
                    Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * "Ne giymeli?" kartı: kısa bir cümle, öneri kutucukları ve "şemsiyeni al" gibi ipuçları.
 *
 * Kart açık mavi (ikincil) renkte çizilir; ekrandaki diğer kartlardan ayrışsın, göz önce
 * buraya gitsin. Her öneri büyük bir emoji ve altında adıyla kendi kutucuğundadır.
 *
 * `FlowRow`: kutucuklar yan yana dizilir, satıra sığmayan bir sonraki satıra geçer.
 * Kaç öneri geleceğini bilmediğimiz için `Row` yerine bunu kullanıyoruz.
 */
@Composable
private fun ClothingCard(advice: ClothingAdvice, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(advice.summaryRes), style = MaterialTheme.typography.titleLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                advice.items.forEach { item -> ClothingTile(item) }
            }
            // İpuçları kutucukların altında, her biri kendi satırında.
            if (advice.tips.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    advice.tips.forEach { tip -> ClothingTipRow(tip) }
                }
            }
        }
    }
}

/** "☂️ Şemsiyeni al." gibi tek bir ipucu satırı; emoji beyaz bir dairenin içinde. */
@Composable
private fun ClothingTipRow(tip: ClothingTip, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = tip.emoji, fontSize = 20.sp)
            }
        }
        Text(
            text = stringResource(tip.textRes, *tip.args.toTypedArray()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Tek bir öneri kutucuğu: ortada büyük emoji, altında en fazla iki satırlık ad. */
@Composable
private fun ClothingTile(item: ClothingItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(width = 124.dp, height = 160.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 1.dp
    ) {
        // İçerik kutucuğun tam ortasında durur; ad tek satırlık da olsa iki satırlık da.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Text(text = item.emoji, fontSize = 44.sp)
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Güneş ve UV kartı: solda gün doğumu, gün batımı ve gün uzunluğu; sağda UV indeksi.
 *
 * UV için şu anki değer büyük yazılır (gece 0'dır); altında seviyesi ("Orta") ve
 * yeşilden mora giden bir ölçek çubuğu vardır. Günün en yüksek değeri en altta durur;
 * kıyafet önerisi o değere bakar.
 *
 * @param currentUv Şu anki UV; servis göndermezse günün en yüksek değeri gösterilir.
 */
@Composable
private fun SunCard(today: DailyForecast, currentUv: Double?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.raisedCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                today.sunrise?.let { SunTime(emoji = "🌅", label = stringResource(R.string.sunrise), time = it) }
                today.sunset?.let { SunTime(emoji = "🌇", label = stringResource(R.string.sunset), time = it) }
                val sunrise = today.sunrise
                val sunset = today.sunset
                if (sunrise != null && sunset != null) {
                    val daylight = Duration.between(sunrise, sunset)
                    Text(
                        text = stringResource(R.string.day_length, daylight.toHours().toInt(), (daylight.toMinutes() % 60).toInt()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            (currentUv ?: today.uvIndexMax)?.let { uv ->
                UvGauge(uv = uv, todayMax = today.uvIndexMax, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** "🌅 Gün doğumu 07:26" satırı. */
@Composable
private fun SunTime(emoji: String, label: String, time: LocalDateTime) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = emoji, fontSize = 28.sp)
        Column {
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = time.format(HOUR_FORMAT), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * UV göstergesi: büyük değer, renkli seviye noktası ve 0–12 ölçek çubuğu.
 *
 * Çubuk, seviyelerin renklerinden oluşan bir degradedir; beyaz daire değerin
 * ölçekteki yerini gösterir.
 */
@Composable
private fun UvGauge(uv: Double, todayMax: Double?, modifier: Modifier = Modifier) {
    val level = UvLevel.of(uv)
    val marker = MaterialTheme.colorScheme.surfaceContainerLowest
    val outline = MaterialTheme.colorScheme.onSurface
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.uv_index),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = uv.roundToInt().toString(), style = MaterialTheme.typography.headlineLarge)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(level.color, CircleShape)
            )
            Text(text = stringResource(level.labelRes), style = MaterialTheme.typography.titleLarge)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val barHeight = 6.dp.toPx()
            val top = (size.height - barHeight) / 2
            drawRoundRect(
                brush = Brush.horizontalGradient(UvScaleColors),
                topLeft = Offset(0f, top),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(barHeight / 2)
            )
            val x = size.width * (uv / UvLevel.SCALE_MAX).coerceIn(0.0, 1.0).toFloat()
            val radius = size.height / 2 - 1.dp.toPx()
            val center = Offset(x.coerceIn(radius, size.width - radius), size.height / 2)
            drawCircle(color = marker, radius = radius, center = center)
            drawCircle(color = outline, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
        }
        todayMax?.let {
            Text(
                text = stringResource(R.string.uv_today_max, "%.1f".format(LocalConfiguration.current.locales[0], it)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Saat aralığı çipleri: Tümü · Sabah · Öğle · Akşam · Gece.
 *
 * `FilterChip` seçiliyken tik işareti ve dolgu rengiyle belli olur. Çipler yan yana
 * sığmazsa satır yatay kaydırılır.
 */
@Composable
private fun HourRangeFilter(selected: HourRange, onSelect: (HourRange) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = HourRange.entries, key = { it.name }) { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = {
                    Text(
                        text = stringResource(range.labelRes).let { label -> range.span?.let { "$label · $it" } ?: label },
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = if (range == selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                }
            )
        }
    }
}

/**
 * Seçili aralıktaki saatler, yatay kaydırılabilir kartlar hâlinde.
 *
 * Geçmiş saatler ve filtre çağıran tarafta uygulanmış gelir. "Şimdi" kartı vurgulu çizilir.
 * Filtre değişince liste başa sarılır; yoksa yeni listenin ortasından açılabilir.
 *
 * Ertesi güne düşen kartların üstünde "Yarın" yazar; 13:00'te "Sabah" seçilince gelen
 * 06:00 bugünün değil yarının sabahıdır. Liste yalnızca bugünü içeriyorsa bu satır hiç
 * çizilmez, kartlar boşuna uzamaz.
 *
 * @param now Şehrin yerel saatinde, saat başına yuvarlanmış şu an.
 */
@Composable
private fun HourlyRow(hours: List<HourlyForecast>, now: LocalDateTime, modifier: Modifier = Modifier) {
    if (hours.isEmpty()) {
        Text(
            text = stringResource(R.string.detail_hourly_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        )
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(hours.first().time) { listState.scrollToItem(0) }
    val today = now.toLocalDate()
    val showsDay = hours.any { it.time.toLocalDate() != today }
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = hours, key = { it.time.toString() }) { hour ->
            val isNow = hour.time == now
            Card(
                modifier = Modifier.width(84.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNow) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.raisedCard
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showsDay) {
                        Text(
                            // Bugünün kartlarında boşluk: kartların yüksekliği eşit kalsın.
                            text = if (hour.time.toLocalDate() != today) stringResource(R.string.tomorrow) else " ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = if (isNow) stringResource(R.string.now) else hour.time.format(HOUR_FORMAT),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = WeatherCondition.fromWeatherCode(hour.weatherCode).emoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(text = degrees(hour.temperatureC), style = MaterialTheme.typography.titleLarge)
                    Text(
                        // Boş string yerine boşluk: kartların yüksekliği eşit kalsın.
                        text = percent(hour.precipitationProbability) ?: " ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Günlük tahmin kartı.
 *
 * Haftanın en düşük ve en yüksek sıcaklığı bulunur; her gün bu aralıkta bir
 * çubukla gösterilir. Böylece hangi günün daha sıcak olduğu bir bakışta görülür.
 */
@Composable
private fun DailyCard(days: List<DailyForecast>, modifier: Modifier = Modifier) {
    val weekMin = days.minOfOrNull { it.minTemperatureC } ?: 0.0
    val weekMax = days.maxOfOrNull { it.maxTemperatureC } ?: 0.0
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.raisedCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            days.forEachIndexed { index, day ->
                DailyRow(day = day, isToday = index == 0, weekMin = weekMin, weekMax = weekMax)
            }
        }
    }
}

/** Günlük tahmin satırı: gün adı · emoji + yağış · en düşük · sıcaklık çubuğu · en yüksek. */
@Composable
private fun DailyRow(day: DailyForecast, isToday: Boolean, weekMin: Double, weekMax: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gün adının altında o günün hava ifadesi: "Sağanak yağışlı".
        Column(modifier = Modifier.width(124.dp)) {
            Text(
                text = if (isToday) stringResource(R.string.today) else dayName(day.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday) FontWeight.SemiBold else null,
                maxLines = 1
            )
            Text(
                text = stringResource(WeatherCondition.detailLabelRes(day.weatherCode)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        Column(modifier = Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = WeatherCondition.fromWeatherCode(day.weatherCode).emoji, style = MaterialTheme.typography.titleLarge)
            percent(day.precipitationProbability)?.let {
                Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            text = degrees(day.minTemperatureC),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
        TemperatureBar(
            min = day.minTemperatureC,
            max = day.maxTemperatureC,
            weekMin = weekMin,
            weekMax = weekMax,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        Text(
            text = degrees(day.maxTemperatureC),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.width(52.dp)
        )
    }
}

/**
 * Bir günün sıcaklık aralığını haftanın aralığı içinde gösteren çubuk.
 *
 * Gri iz haftanın tamamıdır; renkli parça o günün en düşüğünden en yükseğine uzanır.
 * Renk soğuktan (mavi) sıcağa (sarı) geçer.
 */
@Composable
private fun TemperatureBar(min: Double, max: Double, weekMin: Double, weekMax: Double, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val cold = MaterialTheme.colorScheme.primary
    val warm = MaterialTheme.colorScheme.tertiary
    val range = (weekMax - weekMin).takeIf { it > 0 } ?: 1.0
    val start = ((min - weekMin) / range).toFloat()
    val end = ((max - weekMin) / range).toFloat()
    Canvas(modifier = modifier.height(6.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(cold, warm), startX = 0f, endX = size.width),
            topLeft = Offset(size.width * start, 0f),
            size = Size(size.width * (end - start).coerceAtLeast(0.04f), size.height),
            cornerRadius = radius
        )
    }
}

/** Gökyüzü kartının altındaki yarı saydam küçük kutular: emoji, değer, etiket. */
@Composable
private fun Metric(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(OnSky.copy(alpha = 0.16f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = OnSky)
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = OnSky.copy(alpha = 0.85f))
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
    )
}

/** 18.4 → "18°". Kullanıcıya ondalık göstermeye gerek yok. */
private fun degrees(celsius: Double) = "${celsius.roundToInt()}°"

/**
 * Yağış olasılığı yalnızca sıfırdan büyükse anlamlıdır.
 *
 * Yüzde işaretinin yeri dile göre değişir: Türkçede "%40", İngilizcede "40%".
 * Bu yüzden biçim `strings.xml`'den okunur.
 */
@Composable
private fun percent(value: Int?): String? = value?.takeIf { it > 0 }?.let { stringResource(R.string.percent, it) }

/**
 * Gün adı, uygulamanın dilinde: "Pazartesi" / "Monday".
 *
 * Dil `LocalConfiguration`'dan okunur; uygulama içinden dil değişince bu da değişir.
 */
@Composable
private fun dayName(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return date.format(DateTimeFormatter.ofPattern("EEEE", locale)).replaceFirstChar { it.titlecase(locale) }
}

private const val HOURLY_COUNT = 24
private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Preview(showBackground = true)
@Composable
private fun ForecastScreenPreview() {
    HavaTheme {
        ForecastScreen(
            cityName = "Ankara",
            subtitle = "Ankara, Türkiye",
            uiState = LoadState.Success(
                Forecast(
                    current = CurrentWeather(
                        temperatureC = 21.0,
                        weatherCode = 0,
                        observedAt = LocalDateTime.of(2026, 1, 15, 14, 0),
                        apparentTemperatureC = 20.0,
                        humidityPercent = 45,
                        windSpeedKmh = 12.0
                    ),
                    hourly = List(8) {
                        HourlyForecast(
                            time = LocalDateTime.of(2026, 1, 15, 14, 0).plusHours(it.toLong()),
                            temperatureC = 18.0 + it,
                            weatherCode = 2,
                            precipitationProbability = it * 5
                        )
                    },
                    daily = List(7) {
                        DailyForecast(
                            date = LocalDate.of(2026, 1, 15).plusDays(it.toLong()),
                            minTemperatureC = 9.0 + it,
                            maxTemperatureC = 19.0 + it,
                            weatherCode = 61,
                            precipitationProbability = 40
                        )
                    }
                )
            ),
            onBack = {},
            onRetry = {}
        )
    }
}
