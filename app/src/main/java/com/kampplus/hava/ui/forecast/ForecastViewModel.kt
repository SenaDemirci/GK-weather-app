package com.kampplus.hava.ui.forecast

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import com.kampplus.hava.R
import com.kampplus.hava.data.OpenMeteoPollenRepository
import com.kampplus.hava.data.OpenMeteoWeatherRepository
import com.kampplus.hava.data.PollenRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.PollenForecast
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Tahmin ekranının beyni. [com.kampplus.hava.ui.city.CityViewModel] ile aynı desen.
 *
 * Fark: hangi şehrin tahmini isteneceğini navigasyondan öğrenir.
 * `SavedStateHandle` rota parametrelerini taşır ve süreç
 * öldürülüp geri dönüldüğünde de bu değerler korunur.
 */
class ForecastViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: WeatherRepository,
    private val pollenRepository: PollenRepository
) : ViewModel() {

    /**
     * Compose'un `viewModel()` yardımcısı, tam olarak `(SavedStateHandle)` imzalı bir
     * kurucu arar. Varsayılan parametre kullanmak bu imzayı üretmediği için ikincil
     * kurucuyu elle yazıyoruz.
     */
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle, OpenMeteoWeatherRepository(), OpenMeteoPollenRepository())

    /** Başlıkta gösterilecek şehir adı; rotadan gelir. */
    val cityName: String = savedStateHandle.get<String>(ARG_NAME).orEmpty()

    /** Başlığın altındaki "Ankara, Türkiye" satırı. */
    val subtitle: String = savedStateHandle.get<String>(ARG_SUBTITLE).orEmpty()

    private val latitude: Double = savedStateHandle.get<String>(ARG_LATITUDE)?.toDoubleOrNull() ?: 0.0
    private val longitude: Double = savedStateHandle.get<String>(ARG_LONGITUDE)?.toDoubleOrNull() ?: 0.0

    private val _uiState = MutableStateFlow<LoadState<Forecast>>(LoadState.Loading)
    val uiState: StateFlow<LoadState<Forecast>> = _uiState.asStateFlow()

    /**
     * Polen tahmini; yoksa ya da alınamadıysa null.
     *
     * Tahminden ayrı tutulur ve ayrı yüklenir: polen servisi hata verse ya da bölge
     * kapsam dışında olsa bile hava tahmini ekranda görünmeye devam eder.
     */
    private val _pollen = MutableStateFlow<LoadState<PollenForecast>>(LoadState.Loading)
    val pollen: StateFlow<LoadState<PollenForecast>> = _pollen.asStateFlow()

    init {
        load()
    }

    /** Hata ekranındaki "Tekrar dene" düğmesi buraya bağlanır. */
    fun onRetry() = load()

    private fun load() {
        loadPollen()
        viewModelScope.launch {
            _uiState.value = LoadState.Loading
            _uiState.value = try {
                LoadState.Success(repository.getForecast(latitude, longitude))
            } catch (e: IOException) {
                Log.e(TAG, "Tahmin alınamadı: bağlantı hatası", e)
                LoadState.Error(R.string.error_connection)
            } catch (e: HttpException) {
                Log.e(TAG, "Tahmin alınamadı: HTTP ${e.code()}", e)
                LoadState.Error(R.string.error_server)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Tahmin alınamadı: yanıt çözümlenemedi", e)
                LoadState.Error(R.string.error_unexpected)
            } catch (e: IllegalStateException) {
                // ForecastResponse.toForecast(), "current" bloğu yoksa bunu fırlatır.
                Log.e(TAG, "Tahmin alınamadı: eksik veri", e)
                LoadState.Error(R.string.error_unexpected)
            }
        }
    }

    /** Polen verisini getirir. Kapsam dışı bölge "Empty", ağ hatası "Error" olur. */
    private fun loadPollen() {
        viewModelScope.launch {
            _pollen.value = LoadState.Loading
            _pollen.value = try {
                pollenRepository.getPollen(latitude, longitude)?.let { LoadState.Success(it) } ?: LoadState.Empty
            } catch (e: IOException) {
                Log.w(TAG, "Polen alınamadı: bağlantı hatası", e)
                LoadState.Error(R.string.error_connection)
            } catch (e: HttpException) {
                Log.w(TAG, "Polen alınamadı: HTTP ${e.code()}", e)
                LoadState.Error(R.string.error_server)
            } catch (e: JsonSyntaxException) {
                Log.w(TAG, "Polen alınamadı: yanıt çözümlenemedi", e)
                LoadState.Error(R.string.error_unexpected)
            }
        }
    }

    companion object {
        private const val TAG = "ForecastViewModel"

        /** Rota parametrelerinin adları; [com.kampplus.hava.ui.HavaNavHost] ile ortak. */
        const val ARG_NAME = "name"
        const val ARG_SUBTITLE = "subtitle"
        const val ARG_LATITUDE = "latitude"
        const val ARG_LONGITUDE = "longitude"
    }
}
