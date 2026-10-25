package com.kampplus.hava.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.Province
import com.kampplus.hava.model.ProvinceTemperature
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.component.EmptyView
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.theme.MaxContentWidth
import com.kampplus.hava.ui.theme.TemperatureStops
import com.kampplus.hava.ui.theme.raisedCard
import kotlin.math.roundToInt

/** Harita ekranının bağlayıcı katmanı; [com.kampplus.hava.ui.city.CityRoute] ile aynı desen. */
@Composable
fun MapRoute(onBack: () -> Unit, onOpenProvince: (Province) -> Unit, modifier: Modifier = Modifier, viewModel: MapViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(uiState = uiState, onBack = onBack, onRetry = viewModel::onRetry, onOpenProvince = onOpenProvince, modifier = modifier)
}

/**
 * Türkiye sıcaklık haritası: 81 il bugünkü ortalama sıcaklığına göre renkli.
 *
 * Bir ile dokununca altındaki kartta adı, sıcaklığı ve "Hava durumunu gör" düğmesi çıkar;
 * düğme o ilin mevcut tahmin ekranını açar. Seçili il ekran dönünce korunur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: LoadState<List<ProvinceTemperature>>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenProvince: (Province) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (uiState) {
                LoadState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 48.dp))
                LoadState.Empty -> EmptyView(title = stringResource(R.string.map_empty))
                is LoadState.Error -> ErrorView(message = stringResource(uiState.messageRes), onRetry = onRetry)
                is LoadState.Success -> MapContent(temperatures = uiState.data, onOpenProvince = onOpenProvince)
            }
        }
    }
}

@Composable
private fun MapContent(temperatures: List<ProvinceTemperature>, onOpenProvince: (Province) -> Unit) {
    var selectedPlate by rememberSaveable { mutableStateOf<Int?>(null) }
    val selected = temperatures.firstOrNull { it.province.plate == selectedPlate }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val contentModifier = Modifier
            .widthIn(max = MaxContentWidth)
            .fillMaxWidth()
        Text(
            text = stringResource(R.string.map_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = contentModifier
        )
        Card(
            modifier = contentModifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.raisedCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            TurkeyDotMap(
                temperatures = temperatures,
                selectedPlate = selectedPlate,
                onSelect = { selectedPlate = it.plate },
                // Kenar boşluğu yok: deniz kartın köşelerine kadar uzanır.
                modifier = Modifier.fillMaxWidth()
            )
        }
        TemperatureLegend(modifier = contentModifier)
        SelectedProvinceCard(selected = selected, onOpen = onOpenProvince, modifier = contentModifier)
        Extremes(temperatures = temperatures, modifier = contentModifier)
    }
}

/** Renk ölçeği: soldan sağa soğuktan sıcağa, altında birkaç sıcaklık değeri. */
@Composable
private fun TemperatureLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Brush.horizontalGradient(TemperatureStops.map { it.second }), CircleShape)
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TemperatureStops.forEach { (celsius, _) ->
                Text(text = "${celsius.roundToInt()}°", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Seçili ilin kartı. İl seçilmediyse kısa bir yönlendirme; seçildiyse adı, bugünkü
 * ortalama sıcaklığı ve tahmin ekranını açan düğme.
 */
@Composable
private fun SelectedProvinceCard(selected: ProvinceTemperature?, onOpen: (Province) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (selected == null) {
            Text(
                text = stringResource(R.string.map_select_prompt),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(20.dp)
            )
            return@Card
        }
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val condition = selected.weatherCode?.let(WeatherCondition::fromWeatherCode)
            Text(text = condition?.emoji ?: "📍", fontSize = 40.sp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = selected.province.name, style = MaterialTheme.typography.headlineSmall)
                selected.meanC?.let {
                    Text(text = stringResource(R.string.map_today_mean, it.roundToInt()), style = MaterialTheme.typography.bodyLarge)
                }
                condition?.let { Text(text = stringResource(it.labelRes), style = MaterialTheme.typography.bodyMedium) }
            }
            Button(
                onClick = { onOpen(selected.province) },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Text(stringResource(R.string.map_open_forecast))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

/** "En sıcak: Antalya 22° · En soğuk: Ardahan 3°" */
@Composable
private fun Extremes(temperatures: List<ProvinceTemperature>, modifier: Modifier = Modifier) {
    val known = temperatures.filter { it.meanC != null }
    val hottest = known.maxByOrNull { it.meanC!! } ?: return
    val coldest = known.minByOrNull { it.meanC!! } ?: return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.map_hottest, hottest.province.name, hottest.meanC!!.roundToInt()),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.map_coldest, coldest.province.name, coldest.meanC!!.roundToInt()),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
