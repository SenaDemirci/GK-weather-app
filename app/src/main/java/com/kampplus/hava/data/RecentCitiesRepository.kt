package com.kampplus.hava.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.kampplus.hava.model.City
import com.kampplus.hava.model.RecentCities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Son bakılan şehirler: hangi şehirlerin tahminine bakıldıysa, en yeni başta.
 *
 * Liste bir [StateFlow] olarak dışarı açılır; ekran onu dinler, bir şehir eklenince
 * kendiliğinden yeniden çizilir.
 */
interface RecentCitiesRepository {
    val cities: StateFlow<List<City>>
    fun add(city: City)
    fun remove(city: City)
    fun clear()
}

/**
 * Listeyi cihazda saklar; uygulama kapanıp açılsa da hatırlanır.
 *
 * Tema ve dil gibi `SharedPreferences`'a yazılır ama bu kez tek bir değer değil bir liste
 * olduğu için JSON metni olarak saklanır. JSON'a çeviriyi ağ yanıtlarında da kullandığımız
 * Gson yapar; [City] zaten Gson ile okunabilen bir data class'tır.
 */
class SharedPreferencesRecentCities(context: Context) : RecentCitiesRepository {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _cities = MutableStateFlow(read())
    override val cities: StateFlow<List<City>> = _cities.asStateFlow()

    override fun add(city: City) = save(RecentCities.add(_cities.value, city))

    override fun remove(city: City) = save(RecentCities.remove(_cities.value, city))

    override fun clear() = save(emptyList())

    private fun save(list: List<City>) {
        _cities.value = list
        prefs.edit { putString(KEY_CITIES, gson.toJson(list)) }
    }

    /** Kayıt yoksa ya da bozuksa (eski bir sürümden kalmış olabilir) boş liste. */
    private fun read(): List<City> {
        val json = prefs.getString(KEY_CITIES, null) ?: return emptyList()
        return try {
            gson.fromJson<List<City>>(json, object : TypeToken<List<City>>() {}.type).orEmpty()
        } catch (e: JsonParseException) {
            emptyList()
        }
    }

    private companion object {
        const val FILE_NAME = "settings"
        const val KEY_CITIES = "recent_cities"
    }
}
