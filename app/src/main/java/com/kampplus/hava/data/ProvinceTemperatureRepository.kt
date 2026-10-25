package com.kampplus.hava.data

import com.kampplus.hava.model.Province
import com.kampplus.hava.model.ProvinceTemperature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** İllerin bugünkü ortalama sıcaklığının kaynağı; testlerde sahtesi verilir. */
interface ProvinceTemperatureRepository {
    suspend fun getTemperatures(provinces: List<Province>): List<ProvinceTemperature>
}

class OpenMeteoProvinceTemperatureRepository(
    private val api: ForecastApi = ApiClient.forecastApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProvinceTemperatureRepository {

    /**
     * 81 il için tek istek atılır; yanıt gönderilen sırayla geldiği için illerle
     * sırayla eşleştirilir. Bir ilin değeri eksikse o il haritada gri çizilir.
     */
    override suspend fun getTemperatures(provinces: List<Province>): List<ProvinceTemperature> = withContext(ioDispatcher) {
        val responses = api.getDailyMeans(
            latitudes = provinces.joinToString(",") { it.latitude.toString() },
            longitudes = provinces.joinToString(",") { it.longitude.toString() }
        )
        provinces.mapIndexed { index, province ->
            val daily = responses.getOrNull(index)?.daily
            ProvinceTemperature(
                province = province,
                meanC = daily?.temperatureMean?.firstOrNull(),
                weatherCode = daily?.weatherCode?.firstOrNull()
            )
        }
    }
}
