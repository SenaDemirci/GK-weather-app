package com.kampplus.hava.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo hava kalitesi servisi; polen tahmini buradan gelir.
 *
 * Örnek istek:
 * `GET https://air-quality-api.open-meteo.com/v1/air-quality?latitude=41.01&longitude=28.95&hourly=grass_pollen,...`
 *
 * Polen verisi Avrupa ve çevresi (Türkiye dahil) için vardır; başka yerlerde değerler
 * null gelir.
 */
interface AirQualityApi {

    @GET("v1/air-quality")
    suspend fun getPollen(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = POLLEN_FIELDS,
        @Query("forecast_days") forecastDays: Int = POLLEN_DAYS,
        @Query("timezone") timezone: String = "auto"
    ): PollenResponse

    companion object {
        const val POLLEN_FIELDS =
            "grass_pollen,birch_pollen,alder_pollen,olive_pollen,mugwort_pollen,ragweed_pollen"

        /** Servis polen için en fazla 4 gün ileriye tahmin verir. */
        const val POLLEN_DAYS = 4
    }
}
