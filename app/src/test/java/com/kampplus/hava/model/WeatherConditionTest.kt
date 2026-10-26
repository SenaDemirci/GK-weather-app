package com.kampplus.hava.model

import com.kampplus.hava.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * WMO hava durumu kodlarının doğru görünüme eşlendiğini doğrular.
 *
 * Bu eşleme saf Kotlin'dir: Android, ağ veya veritabanı gerekmez. Bu yüzden
 * testi milisaniyeler içinde çalışır.
 */
class WeatherConditionTest {

    @Test
    fun `acik hava kodu CLEAR dondurur`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWeatherCode(0))
    }

    @Test
    fun `ayni gorunume dusen kodlar gruplanir`() {
        // 61, 63 ve 65 hafif/orta/şiddetli yağmurdur; üçü de "Yağmurlu" gösterilir.
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWeatherCode(61))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWeatherCode(63))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWeatherCode(65))
    }

    @Test
    fun `firtina kodlari THUNDERSTORM dondurur`() {
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWeatherCode(95))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWeatherCode(99))
    }

    @Test
    fun `taninmayan kod UNKNOWN dondurur ve uygulama cokmez`() {
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWeatherCode(12345))
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWeatherCode(-1))
    }

    @Test
    fun `her durumun bir emojisi ve etiketi vardir`() {
        // Boş bir etiket ekranda boşluk olarak görünürdü; hepsinin dolu olduğunu garanti ediyoruz.
        WeatherCondition.entries.forEach { condition ->
            assert(condition.emoji.isNotBlank()) { "${condition.name} için emoji boş" }
            assert(condition.labelRes != 0) { "${condition.name} için etiket yok" }
        }
    }

    @Test
    fun `ayrintili ifade siddet farkini soyler`() {
        // Üçü de "Yağmurlu" grubunda ama ayrıntılı ifadeleri farklı.
        assertEquals(R.string.weather_rain_light, WeatherCondition.detailLabelRes(61))
        assertEquals(R.string.weather_rain_heavy, WeatherCondition.detailLabelRes(65))
        assertEquals(R.string.weather_showers, WeatherCondition.detailLabelRes(81))
        assertEquals(R.string.weather_showers_violent, WeatherCondition.detailLabelRes(82))
    }

    @Test
    fun `taninan her kodun kendi ayrintili ifadesi vardir, taninmayan kod grubun adini alir`() {
        val known = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        known.forEach { code ->
            assertNotEquals("$code için ifade yok", R.string.condition_unknown, WeatherCondition.detailLabelRes(code))
        }
        assertEquals(R.string.condition_unknown, WeatherCondition.detailLabelRes(12345))
    }
}
