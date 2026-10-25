package com.kampplus.hava.model

import com.kampplus.hava.R
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kıyafet önerisi kurallarını doğrular.
 *
 * [WeatherConditionTest] gibi saf Kotlin testidir; Android gerekmez.
 */
class ClothingAdviceTest {

    private fun current(
        temperatureC: Double,
        weatherCode: Int = CLEAR_CODE,
        apparentTemperatureC: Double? = null,
        windSpeedKmh: Double? = null
    ) = CurrentWeather(
        temperatureC = temperatureC,
        weatherCode = weatherCode,
        observedAt = LocalDateTime.of(2026, 1, 15, 14, 0),
        apparentTemperatureC = apparentTemperatureC,
        windSpeedKmh = windSpeedKmh
    )

    private fun today(min: Double, max: Double, rainChance: Int? = null, uvMax: Double? = null) = DailyForecast(
        date = LocalDate.of(2026, 1, 15),
        minTemperatureC = min,
        maxTemperatureC = max,
        weatherCode = CLEAR_CODE,
        precipitationProbability = rainChance,
        uvIndexMax = uvMax
    )

    private fun hour(hourOfDay: Int, rainChance: Int) = HourlyForecast(
        time = LocalDateTime.of(2026, 1, 15, 0, 0).plusHours(hourOfDay.toLong()),
        temperatureC = 14.0,
        weatherCode = OVERCAST_CODE,
        precipitationProbability = rainChance
    )

    @Test
    fun `dondurucu sogukta kalin mont eldiven ve atki onerilir`() {
        val advice = ClothingAdvice.from(current(-3.0))

        assertTrue(ClothingItem.HEAVY_COAT in advice.items)
        assertTrue(ClothingItem.GLOVES in advice.items)
        assertTrue(ClothingItem.SCARF in advice.items)
    }

    @Test
    fun `sicak havada sort onerilir mont onerilmez`() {
        val advice = ClothingAdvice.from(current(26.0, weatherCode = OVERCAST_CODE))

        assertTrue(ClothingItem.SHORTS in advice.items)
        assertFalse(ClothingItem.COAT in advice.items)
    }

    @Test
    fun `karar hissedilen sicakliga gore verilir`() {
        // Termometre 10° gösteriyor ama rüzgârla 2° gibi hissediliyor: mont gerekir.
        val advice = ClothingAdvice.from(current(10.0, apparentTemperatureC = 2.0))

        assertTrue(ClothingItem.COAT in advice.items)
    }

    @Test
    fun `yagmurlu havada semsiye onerilir`() {
        val advice = ClothingAdvice.from(current(14.0, weatherCode = RAIN_CODE))

        assertTrue(ClothingItem.UMBRELLA in advice.items)
        assertTrue(advice.tips.any { it.textRes == R.string.tip_raining_now })
    }

    @Test
    fun `birkac saat sonra yagacaksa saatiyle birlikte uyarilir`() {
        val hours = listOf(
            hour(13, rainChance = 10),
            hour(14, rainChance = 20),
            hour(15, rainChance = 80)
        )

        val advice = ClothingAdvice.from(current(14.0), upcomingHours = hours)

        assertTrue(ClothingItem.UMBRELLA in advice.items)
        // Saat, cümlenin içine dile göre yerleştirilir; model yalnızca kimliği ve saati verir.
        assertEquals(ClothingTip("☂️", R.string.tip_rain_at, listOf("15:00")), advice.tips.first())
    }

    @Test
    fun `cok ilerideki yagmur icin simdiden uyarilmaz`() {
        // Yağmur 13 saat sonra: bugünkü kıyafet kararını etkilemez.
        val hours = List(14) { hour(it, rainChance = if (it == 13) 90 else 0) }

        val advice = ClothingAdvice.from(current(14.0), upcomingHours = hours)

        assertFalse(ClothingItem.UMBRELLA in advice.items)
    }

    @Test
    fun `sabah yagip gecen yagmur icin semsiye onerilmez`() {
        // Günlük olasılık %100 ama kalan saatlerin hepsi kuru.
        val hours = List(6) { hour(14 + it, rainChance = 0) }

        val advice = ClothingAdvice.from(current(14.0), today(min = 10.0, max = 16.0, rainChance = 100), hours)

        assertFalse(ClothingItem.UMBRELLA in advice.items)
    }

    @Test
    fun `orta yagis olasiliginda semsiye kutucugu degil yalnizca ipucu verilir`() {
        val advice = ClothingAdvice.from(current(14.0), today(min = 10.0, max = 16.0, rainChance = 40))

        assertFalse(ClothingItem.UMBRELLA in advice.items)
        assertTrue(advice.tips.any { it.textRes == R.string.tip_rain_maybe && it.args == listOf(40) })
    }

    @Test
    fun `yagmur ipucu ilk sirada gosterilir`() {
        val advice = ClothingAdvice.from(
            current(24.0, weatherCode = RAIN_CODE, windSpeedKmh = 40.0),
            today(min = 10.0, max = 25.0)
        )

        assertEquals("☂️", advice.tips.first().emoji)
    }

    @Test
    fun `sakin ve ilik havada ipucu verilmez`() {
        val advice = ClothingAdvice.from(current(18.0, weatherCode = OVERCAST_CODE), today(min = 14.0, max = 20.0))

        assertTrue(advice.tips.isEmpty())
    }

    @Test
    fun `yagis olasiligi yuksekse acik havada da semsiye onerilir`() {
        val advice = ClothingAdvice.from(current(14.0), today(min = 10.0, max = 16.0, rainChance = 70))

        assertTrue(ClothingItem.UMBRELLA in advice.items)
    }

    @Test
    fun `yagis olasiligi dusukse semsiye onerilmez`() {
        val advice = ClothingAdvice.from(current(14.0), today(min = 10.0, max = 16.0, rainChance = 20))

        assertFalse(ClothingItem.UMBRELLA in advice.items)
    }

    @Test
    fun `karda bot onerilir`() {
        val advice = ClothingAdvice.from(current(-1.0, weatherCode = SNOW_CODE))

        assertTrue(ClothingItem.BOOTS in advice.items)
    }

    @Test
    fun `sert ruzgarda ruzgarlik onerilir`() {
        val advice = ClothingAdvice.from(current(16.0, windSpeedKmh = 40.0))

        assertTrue(ClothingItem.WINDBREAKER in advice.items)
    }

    @Test
    fun `gunesli ve ilik havada gunes gozlugu ve krem onerilir`() {
        val advice = ClothingAdvice.from(current(24.0, weatherCode = CLEAR_CODE))

        assertTrue(ClothingItem.SUNGLASSES in advice.items)
        assertTrue(ClothingItem.SUNSCREEN in advice.items)
    }

    @Test
    fun `yuksek uvde serin havada bile gunes kremi onerilir`() {
        // 12° kapalı hava ama günün UV'si 7: krem şart.
        val advice = ClothingAdvice.from(current(12.0, weatherCode = OVERCAST_CODE), today(min = 8.0, max = 14.0, uvMax = 7.2))

        assertTrue(ClothingItem.SUNSCREEN in advice.items)
        assertTrue(advice.tips.any { it.textRes == R.string.tip_uv && it.args == listOf(7) })
    }

    @Test
    fun `orta uvde krem onerilmez`() {
        val advice = ClothingAdvice.from(current(12.0, weatherCode = OVERCAST_CODE), today(min = 8.0, max = 14.0, uvMax = 4.0))

        assertFalse(ClothingItem.SUNSCREEN in advice.items)
    }

    @Test
    fun `gunesli ve yuksek uvli gunde gunes ipucu tekrarlanmaz`() {
        val advice = ClothingAdvice.from(current(26.0, weatherCode = CLEAR_CODE), today(min = 20.0, max = 27.0, uvMax = 9.0))

        assertTrue(advice.tips.any { it.textRes == R.string.tip_uv })
        assertFalse(advice.tips.any { it.textRes == R.string.tip_sunny })
    }

    @Test
    fun `gun icinde buyuk sicaklik farki varsa katmanli giyim onerilir`() {
        val advice = ClothingAdvice.from(current(12.0), today(min = 5.0, max = 19.0))

        assertTrue(ClothingItem.LAYERS in advice.items)
    }

    @Test
    fun `oneri listesinde ayni sey iki kez yer almaz`() {
        val advice = ClothingAdvice.from(
            current(-2.0, weatherCode = SNOW_CODE, windSpeedKmh = 45.0),
            today(min = -8.0, max = 4.0, rainChance = 90)
        )

        assertTrue(advice.items.size == advice.items.distinct().size)
    }

    private companion object {
        const val CLEAR_CODE = 0
        const val OVERCAST_CODE = 3
        const val RAIN_CODE = 61
        const val SNOW_CODE = 71
    }
}
