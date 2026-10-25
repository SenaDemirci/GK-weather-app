package com.kampplus.hava.data

import com.google.gson.annotations.SerializedName
import com.kampplus.hava.model.PollenDay
import com.kampplus.hava.model.PollenForecast
import com.kampplus.hava.model.PollenType
import java.time.LocalDateTime

/**
 * Hava kalitesi yanıtının ham hali. Tahmin yanıtı gibi sütun biçimindedir (bkz.
 * [ForecastResponse]): her polen türü saat başına bir değer içeren ayrı bir listedir.
 */
data class PollenResponse(val hourly: PollenHourly? = null)

data class PollenHourly(
    val time: List<String>? = null,
    @SerializedName("grass_pollen") val grass: List<Double?>? = null,
    @SerializedName("birch_pollen") val birch: List<Double?>? = null,
    @SerializedName("alder_pollen") val alder: List<Double?>? = null,
    @SerializedName("olive_pollen") val olive: List<Double?>? = null,
    @SerializedName("mugwort_pollen") val mugwort: List<Double?>? = null,
    @SerializedName("ragweed_pollen") val ragweed: List<Double?>? = null
)

/**
 * Saatlik değerleri günlere toplar: her gün için her türün o günkü **en yüksek** değeri.
 *
 * Alerjisi olan biri için önemli olan günün ortalaması değil, en kötü anıdır.
 * Hiçbir türde hiç değer yoksa (bölge kapsam dışı) null döner.
 */
fun PollenResponse.toPollenForecast(): PollenForecast? {
    val hourly = hourly ?: return null
    val times = hourly.time ?: return null
    val columns = mapOf(
        PollenType.GRASS to hourly.grass,
        PollenType.BIRCH to hourly.birch,
        PollenType.ALDER to hourly.alder,
        PollenType.OLIVE to hourly.olive,
        PollenType.MUGWORT to hourly.mugwort,
        PollenType.RAGWEED to hourly.ragweed
    )
    if (columns.values.all { column -> column.orEmpty().all { it == null } }) return null

    val days = times.indices
        .groupBy { LocalDateTime.parse(times[it]).toLocalDate() }
        .map { (date, indices) ->
            PollenDay(
                date = date,
                values = columns.mapValues { (_, column) -> indices.mapNotNull { column?.getOrNull(it) }.maxOrNull() }
            )
        }
    return PollenForecast(days)
}
