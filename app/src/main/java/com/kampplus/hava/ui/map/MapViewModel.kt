package com.kampplus.hava.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import com.kampplus.hava.R
import com.kampplus.hava.data.OpenMeteoProvinceTemperatureRepository
import com.kampplus.hava.data.ProvinceTemperatureRepository
import com.kampplus.hava.model.ProvinceTemperature
import com.kampplus.hava.model.TurkishProvinces
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Türkiye sıcaklık haritasının beyni: 81 ilin bugünkü ortalama sıcaklığını tek istekte getirir.
 * [com.kampplus.hava.ui.city.CityViewModel] ile aynı desen.
 */
class MapViewModel(
    private val repository: ProvinceTemperatureRepository = OpenMeteoProvinceTemperatureRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadState<List<ProvinceTemperature>>>(LoadState.Loading)
    val uiState: StateFlow<LoadState<List<ProvinceTemperature>>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = LoadState.Loading
            _uiState.value = try {
                val temperatures = repository.getTemperatures(TurkishProvinces)
                if (temperatures.all { it.meanC == null }) LoadState.Empty else LoadState.Success(temperatures)
            } catch (e: IOException) {
                Log.e(TAG, "Harita yüklenemedi: bağlantı hatası", e)
                LoadState.Error(R.string.error_connection)
            } catch (e: HttpException) {
                Log.e(TAG, "Harita yüklenemedi: HTTP ${e.code()}", e)
                LoadState.Error(R.string.error_server)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Harita yüklenemedi: yanıt çözümlenemedi", e)
                LoadState.Error(R.string.error_unexpected)
            }
        }
    }

    private companion object {
        const val TAG = "MapViewModel"
    }
}
