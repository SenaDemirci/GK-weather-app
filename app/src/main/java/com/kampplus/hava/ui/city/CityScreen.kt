package com.kampplus.hava.ui.city

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.City
import com.kampplus.hava.model.CountryFilter
import com.kampplus.hava.model.CountryOption
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.assistant.AssistantSheet
import com.kampplus.hava.ui.assistant.AssistantViewModel
import com.kampplus.hava.ui.assistant.GeneralSuggestions
import com.kampplus.hava.ui.assistant.RobotMascot
import com.kampplus.hava.ui.component.EmptyView
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.component.ShimmerList
import com.kampplus.hava.ui.locale.AppLanguage
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.theme.MapCardBrush
import com.kampplus.hava.ui.theme.MaxContentWidth
import com.kampplus.hava.ui.theme.OnSky
import com.kampplus.hava.ui.theme.ThemeMode
import com.kampplus.hava.ui.theme.pillBrush
import com.kampplus.hava.ui.theme.raisedCard
import java.time.LocalTime

/**
 * Şehir arama ekranının "bağlayıcı" katmanı.
 *
 * Tek işi ViewModel'i edinip state'i toplamak ve [CityScreen]'e aktarmaktır.
 * Bu ayrım sayesinde [CityScreen] ViewModel tanımaz: @Preview'da ve testte
 * elle state verilerek çalıştırılabilir.
 *
 * `collectAsStateWithLifecycle`: StateFlow'u Compose state'ine
 * çevirir ve uygulama arka plandayken dinlemeyi durdurur.
 *
 * `rememberSaveable`: arama metnini Bundle'a yazar. Ekran döndüğünde metin geri gelir;
 * Android uygulamayı arka planda öldürüp yeniden kurduğunda da (ViewModel sıfırlanırken)
 * kaybolmaz. Seçili ülke filtresi de aynı yolla saklanır.
 */
@Composable
fun CityRoute(
    onCityClick: (City) -> Unit,
    onOpenMap: () -> Unit = {},
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    language: AppLanguage = AppLanguage.SYSTEM,
    onLanguageChange: (AppLanguage) -> Unit = {},
    viewModel: CityViewModel = viewModel()
) {
    var query by rememberSaveable { mutableStateOf(viewModel.query.value) }
    // null = "Tüm ülkeler". Ülke kodu ("TR") düz metin olduğu için Bundle'a doğrudan yazılır.
    var selectedCountry by rememberSaveable { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Süreç öldükten sonra ViewModel başlangıç metniyle kurulur, rememberSaveable ise
    // kullanıcının yazdığını geri getirir. Fark varsa ViewModel'i kaydedilen metne eşitle.
    LaunchedEffect(Unit) {
        if (viewModel.query.value != query) {
            viewModel.onQueryChange(query)
            viewModel.search()
        } else {
            // Dil değiştiyse sonuçlar yeni dilde ("Germany") yeniden alınır.
            viewModel.refreshIfLanguageChanged()
        }
    }

    CityScreen(
        query = query,
        uiState = uiState,
        onQueryChange = {
            query = it
            viewModel.onQueryChange(it)
        },
        onSearch = {
            // Yeni aramada eski ülke seçimi anlamını yitirir; filtre sıfırlanır.
            selectedCountry = null
            viewModel.search()
        },
        onCityClick = onCityClick,
        onOpenMap = onOpenMap,
        modifier = modifier,
        selectedCountry = selectedCountry,
        onCountrySelect = { selectedCountry = it },
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
        language = language,
        onLanguageChange = onLanguageChange
    )
}

/**
 * Şehir arama ekranı: `when` ile her duruma bir görünüm.
 *
 * State tutmaz, yalnızca aldığı değerleri çizer ve olayları yukarı bildirir.
 */
@Composable
fun CityScreen(
    query: String,
    uiState: LoadState<List<City>>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCityClick: (City) -> Unit,
    onOpenMap: () -> Unit = {},
    modifier: Modifier = Modifier,
    selectedCountry: String? = null,
    onCountrySelect: (String?) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    language: AppLanguage = AppLanguage.SYSTEM,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    // Robot sohbeti açık mı; ekran dönünce korunur. Sohbetin kendisi ViewModel'de yaşar.
    var assistantOpen by rememberSaveable { mutableStateOf(false) }
    if (assistantOpen) {
        AssistantSheet(
            viewModel = viewModel(key = "assistant-general") { AssistantViewModel() },
            cityName = null,
            suggestions = GeneralSuggestions,
            onDismiss = { assistantOpen = false }
        )
    }
    // `contentWindowInsets = 0`: Scaffold içeriği durum çubuğunun altından itmesin; başlığın
    // mavi degradesi ekranın en üstüne kadar uzansın. Boşlukları aşağıda kendimiz veriyoruz.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        // Açılışta robot büyüyerek gelir ve yardımcı olabileceğini söyler. Dokununca sohbet
        // açılır; kullanıcı istediği şehri yazarak havayı sorabilir.
        floatingActionButton = {
            RobotMascot(
                message = stringResource(R.string.bot_hello_city),
                onClick = { assistantOpen = true },
                modifier = Modifier.navigationBarsPadding(),
                contentDescription = stringResource(R.string.bot_open)
            )
        }
    ) { innerPadding ->
        val sky = MaterialTheme.colorScheme.primaryContainer
        val ground = MaterialTheme.colorScheme.background
        val glow = MaterialTheme.colorScheme.primary
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Gökyüzü degradesi bütün ekranın arkasında. Alt kısım bile zemin rengine tam
                // dönmez, hafif mavi kalır; böylece beyaz şehir kartları her yerde zeminden ayrışır.
                .background(
                    Brush.verticalGradient(
                        0f to sky,
                        0.3f to sky,
                        1f to lerp(sky, ground, 0.7f)
                    )
                )
                // Arkaya iki yumuşak ışık halesi: sağ üstte güneş parıltısı, sol altta mavi bir ışık.
                // `drawBehind` içeriğin arkasına çizer; yazıları ve kartları etkilemez.
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(SunGlow.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.04f),
                            radius = size.width * 0.5f
                        ),
                        radius = size.width * 0.5f,
                        center = Offset(size.width * 0.92f, size.height * 0.04f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(glow.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.05f, size.height * 0.82f),
                            radius = size.width * 0.6f
                        ),
                        radius = size.width * 0.6f,
                        center = Offset(size.width * 0.05f, size.height * 0.82f)
                    )
                }
                .padding(innerPadding),
            // Tablette içerik ortada, en fazla [MaxContentWidth] genişliğinde durur.
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val contentModifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = contentModifier
                        .statusBarsPadding()
                        .padding(top = 16.dp, bottom = 12.dp)
                ) {
                    Header(
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        language = language,
                        onLanguageChange = onLanguageChange,
                        modifier = Modifier.padding(start = 20.dp, end = 12.dp)
                    )
                    SearchBar(
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = onSearch,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
                    )
                    MapEntryCard(onClick = onOpenMap, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                }
            }
            Box(
                modifier = contentModifier
                    .weight(1f)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                // Dört durumun tamamı burada karşılanır. `sealed` olduğu için
                // bir dalı unutursak Kotlin derleme hatası verir.
                when (uiState) {
                    LoadState.Loading -> ShimmerList()
                    is LoadState.Success -> CityList(
                        cities = uiState.data,
                        selectedCountry = selectedCountry,
                        onCountrySelect = onCountrySelect,
                        onCityClick = onCityClick
                    )
                    LoadState.Empty -> EmptyView(
                        title = stringResource(R.string.empty_city_title),
                        message = stringResource(R.string.empty_city_message)
                    )
                    is LoadState.Error -> ErrorView(message = stringResource(uiState.messageRes), onRetry = onSearch)
                }
            }
        }
    }
}

/**
 * Ekranın başlığı: üstte selam ile dil ve tema düğmeleri, altında büyük soru başlığı
 * ve tek satırlık kısa açıklama.
 */
@Composable
private fun Header(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Üst satır: solda selam, sağda dil ve tema düğmeleri. Başlık ve alt yazı alttaki
        // satırlarda ekranın tüm genişliğini kullanır; düğmeler onları daraltmaz.
        Row(verticalAlignment = Alignment.CenterVertically) {
            val greeting = Greeting.of(LocalTime.now())
            Text(
                text = "${greeting.emoji}  ${stringResource(greeting.textRes)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageMenu(selected = language, onSelect = onLanguageChange)
                ThemeMenu(selected = themeMode, onSelect = onThemeModeChange)
            }
        }
        Text(text = stringResource(R.string.city_title), style = MaterialTheme.typography.displaySmall)
        // Alt yazı her zaman tek satırdır: dar ekranda sığmazsa yazı boyutu kendiliğinden küçülür.
        // `autoSize`, metin sığana kadar boyutu 18sp'den adım adım 12sp'ye kadar indirir.
        BasicText(
            text = stringResource(R.string.city_header_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = MaterialTheme.typography.bodyLarge.fontSize)
        )
    }
}

/**
 * Başlıktaki selam: telefonun saatine göre "Günaydın", "İyi akşamlar"...
 *
 * Burada şehrin değil kullanıcının saati doğrudur; selamı okuyan kullanıcıdır.
 */
private enum class Greeting(val emoji: String, @StringRes val textRes: Int) {
    MORNING("☀️", R.string.greeting_morning),
    DAY("🌤️", R.string.greeting_day),
    EVENING("🌆", R.string.greeting_evening),
    NIGHT("🌙", R.string.greeting_night);

    companion object {
        fun of(time: LocalTime): Greeting = when (time.hour) {
            in 5..11 -> MORNING
            in 12..17 -> DAY
            in 18..21 -> EVENING
            else -> NIGHT
        }
    }
}

/** Arka plandaki güneş parıltısının rengi: sıcak, açık bir sarı. */
private val SunGlow = Color(0xFFFFE08A)

/**
 * Başlıktaki hap biçimli düğme: emoji, kısa etiket ve aşağı ok ("☀️ Açık ▾").
 *
 * Yalnızca emoji yerine etiket de yazdığı için seçili ayar okunarak anlaşılır.
 * Emoji ve etiket ekran okuyucuya tek bir açıklama ([description]) olarak söylenir.
 */
@Composable
private fun PillButton(
    emoji: String,
    label: String,
    background: Brush,
    contentColor: Color,
    borderColor: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(background)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, tint = contentColor)
    }
}

/**
 * Dil düğmesi ve açılır menüsü: Sistem dili · Türkçe · English.
 *
 * Düğme seçili dilin bayrağını gösterir (Sistem için 🌐). Bir dil seçilince Activity
 * yeniden kurulur ve bütün metinler yeni dilde okunur.
 */
@Composable
private fun LanguageMenu(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val systemLabel = stringResource(R.string.language_system)
    val description = stringResource(R.string.language_button, selected.nativeName ?: systemLabel)
    Box(modifier = modifier) {
        PillButton(
            emoji = selected.emoji,
            label = selected.tag?.uppercase() ?: stringResource(R.string.language_auto),
            background = SolidColor(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentColor = MaterialTheme.colorScheme.onSurface,
            borderColor = MaterialTheme.colorScheme.primary,
            description = description,
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text("${language.emoji}  ${language.nativeName ?: systemLabel}") },
                    onClick = {
                        expanded = false
                        // Aynı dil yeniden seçildiyse Activity'yi boşuna yeniden kurmayalım.
                        if (language != selected) onSelect(language)
                    },
                    trailingIcon = if (language == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

/**
 * Tema düğmesi ve açılır menüsü: Sistem · Açık · Koyu.
 *
 * Düğme o anki seçimin emojisini gösterir. `DropdownMenu` düğmenin hemen altında açılır;
 * `expanded` false olunca kapanır. Açık/kapalı bilgisi yalnızca bu bileşeni ilgilendirdiği
 * için burada `remember` ile tutulur, yukarı taşınmaz.
 */
@Composable
private fun ThemeMenu(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.theme_button, stringResource(selected.labelRes))
    Box(modifier = modifier) {
        // Güneş gündüz mavisinin, ay gece lacivertinin üstünde: hangi temanın seçili olduğu
        // ilk bakışta anlaşılır, güneş açık zeminde kaybolmaz.
        PillButton(
            emoji = selected.emoji,
            label = stringResource(selected.labelRes),
            background = selected.pillBrush,
            contentColor = OnSky,
            borderColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            description = description,
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text("${mode.emoji}  ${stringResource(mode.labelRes)}") },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                    trailingIcon = if (mode == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

/**
 * Türkiye sıcaklık haritasını açan geniş düğme: "🗺️ Türkiye sıcaklık haritası ›".
 *
 * Şehir kartlarıyla karışmasın diye beyaz değil, haritanın kendi sıcaklık renkleriyle
 * (maviden turuncuya) boyanır; yazılar beyazdır.
 */
@Composable
private fun MapEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = OnSky),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MapCardBrush)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Emoji beyaz bir dairede: renkli zeminde de net görünsün.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(OnSky.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🗺️", fontSize = 26.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.map_open), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.map_open_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSky.copy(alpha = 0.9f)
                )
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/**
 * Arama kutusu.
 *
 * State tutmaz: değeri yukarıdan alır, her değişikliği yukarıya bildirir
 * ("değer aşağı, olay yukarı" — state hoisting).
 */
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            // Hafif gölge: kutu degradenin üstünde "yüzüyor" gibi durur.
            .shadow(elevation = 6.dp, shape = CircleShape),
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            // Temizle düğmesi yalnızca kutu doluyken görünür.
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        // Hap biçimli, kenarlıksız bir kutu; odaklanınca ana renkte çerçeve belirir.
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = Color.Transparent
        ),
        // Klavyedeki Enter tuşu "Ara" olur.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                onSearch()
            }
        )
    )
}

/** Listenin üstündeki "Sonuçlar · 6 şehir" satırı. */
@Composable
private fun ResultsHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.results_title), style = MaterialTheme.typography.titleLarge)
        // Koyu mavi: açık mavi degradenin üstünde kaybolmasın.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Text(
                text = stringResource(R.string.results_count, count),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Kaydırılabilir şehir listesi.
 *
 * `LazyColumn` tembeldir: yüzlerce şehir olsa da yalnızca ekranda görünen satırlar
 * çizilir, kaybolanlar geri dönüştürülür.
 *
 * `key = { it.id }`: listedeki öğeler değiştiğinde Compose hangi satırın hangisi
 * olduğunu bilir; gereksiz yeniden çizim yapmaz.
 */
@Composable
private fun CityList(
    cities: List<City>,
    selectedCountry: String?,
    onCountrySelect: (String?) -> Unit,
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(cities) { CountryFilter.options(cities) }
    val selection = CountryFilter.effectiveSelection(cities, selectedCountry)
    val visible = CountryFilter.apply(cities, selection)
    val listState = rememberLazyListState()
    // Ülke değişince liste başa sarılır; yoksa kısa listenin ortasında kalınabilir.
    LaunchedEffect(selection) { listState.scrollToItem(0) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") { ResultsHeader(count = visible.size) }
        // Tek ülkeden sonuç geldiyse süzecek bir şey yok; çipler gösterilmez.
        if (options.size > 1) {
            item(key = "countries") {
                CountryChips(options = options, selected = selection, onSelect = onCountrySelect)
            }
        }
        items(items = visible, key = { it.id }) { city ->
            CityCard(city = city, onClick = { onCityClick(city) })
        }
    }
}

/**
 * Ülke çipleri: "Tüm ülkeler" ve sonuçlardaki her ülke için bir çip ("🇹🇷 Türkiye 5").
 *
 * Satır yatay kaydırılır; ülke sayısı ekrana sığmasa da hepsine ulaşılır.
 * LazyColumn'un içinden kenara kadar uzansın diye yanlardaki 16dp boşluk geri alınır.
 */
@Composable
private fun CountryChips(options: List<CountryOption>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.layout { measurable, constraints ->
            val extra = 32.dp.roundToPx()
            val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra))
            layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
        },
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            CountryChip(
                label = stringResource(R.string.filter_all_countries),
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        items(items = options, key = { it.key }) { option ->
            val flag = option.code?.let(::flagEmoji)
            CountryChip(
                label = listOfNotNull(flag, option.name ?: stringResource(R.string.unknown_country)).joinToString(" "),
                count = option.count,
                selected = option.key == selected,
                onClick = { onSelect(option.key) }
            )
        }
    }
}

@Composable
private fun CountryChip(label: String, selected: Boolean, onClick: () -> Unit, count: Int? = null) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        // Seçili çip koyu mavi, diğerleri beyaz: ikisi de açık mavi degradeden ayrışır.
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        label = {
            // Sonuç sayısı adın yanında soluk renkte durur: "🇹🇷 Türkiye 5".
            val countColor = LocalContentColor.current.copy(alpha = 0.7f)
            Text(
                text = buildAnnotatedString {
                    append(label)
                    count?.let { withStyle(SpanStyle(color = countColor)) { append("  $it") } }
                },
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CityScreenPreview() {
    HavaTheme {
        CityScreen(
            query = "Ankara",
            uiState = LoadState.Success(
                listOf(
                    City(323786, "Ankara", 39.91987, 32.85427, "TR", "Türkiye", "Ankara"),
                    City(738648, "Ankara", 41.0050, 39.7269, "TR", "Türkiye", "Trabzon")
                )
            ),
            onQueryChange = {},
            onSearch = {},
            onCityClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CityScreenEmptyPreview() {
    HavaTheme {
        CityScreen(
            query = "xyz",
            uiState = LoadState.Empty,
            onQueryChange = {},
            onSearch = {},
            onCityClick = {}
        )
    }
}
