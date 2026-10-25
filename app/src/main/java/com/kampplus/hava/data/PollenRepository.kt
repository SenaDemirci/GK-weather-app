package com.kampplus.hava.data

import com.kampplus.hava.model.PollenForecast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Polen tahmininin kaynağı; testlerde sahtesi verilir. */
interface PollenRepository {
    /** Polen tahmini; bölge kapsam dışındaysa null. */
    suspend fun getPollen(latitude: Double, longitude: Double): PollenForecast?
}

class OpenMeteoPollenRepository(
    private val api: AirQualityApi = ApiClient.airQualityApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PollenRepository {

    override suspend fun getPollen(latitude: Double, longitude: Double): PollenForecast? = withContext(ioDispatcher) {
        api.getPollen(latitude = latitude, longitude = longitude).toPollenForecast()
    }
}
