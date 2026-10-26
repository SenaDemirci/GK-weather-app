package com.kampplus.hava.model

import com.kampplus.hava.R
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hava Robotu'nun soruları doğru konuya ve güne eşlediğini, cevabı doğru veriden
 * kurduğunu doğrular.
 */
class WeatherAssistantTest {

    /** 2026-10-23 cuma, saat 14:00. Önümüzdeki 7 gün: cuma … perşembe. */
    private val now = LocalDateTime.of(2026, 10, 23, 14, 0)

    private fun day(offset: Long, min: Double, max: Double, rain: Int, code: Int = CLEAR, uv: Double? = 4.0) = DailyForecast(
        date = now.toLocalDate().plusDays(offset),
        minTemperatureC = min,
        maxTemperatureC = max,
        weatherCode = code,
        precipitationProbability = rain,
        sunrise = now.toLocalDate().plusDays(offset).atTime(7, 26),
        sunset = now.toLocalDate().plusDays(offset).atTime(18, 9),
        uvIndexMax = uv
    )

    private val forecast = Forecast(
        current = CurrentWeather(
            temperatureC = 19.0,
            weatherCode = OVERCAST,
            observedAt = now,
            apparentTemperatureC = 18.0,
            windSpeedKmh = 12.0,
            uvIndex = 2.0
        ),
        hourly = emptyList(),
        daily = listOf(
            day(0, 14.0, 21.0, rain = 10, code = OVERCAST), // cuma
            day(1, 12.0, 16.0, rain = 80, code = RAIN), // cumartesi
            day(2, 15.0, 23.0, rain = 5), // pazar: en güzel gün
            day(3, 13.0, 18.0, rain = 30), // pazartesi
            day(4, 10.0, 14.0, rain = 60, code = RAIN),
            day(5, 9.0, 13.0, rain = 0, code = THUNDERSTORM),
            day(6, 8.0, 12.0, rain = 0)
        )
    )

    private fun ask(question: String, hours: List<HourlyForecast> = emptyList()) =
        WeatherAssistant.reply(question, forecast, "İstanbul", hours)

    private fun AssistantReply.firstRes() = lines.first().res
    private fun AssistantReply.firstDay() = (lines.first().args.first() as ReplyArg.Day).date

    @Test
    fun `sadelestirme buyuk harfi ve turkce harfleri duzler`() {
        assertEquals("yarin yagmur var mi", WeatherAssistant.normalize("  YARIN  Yağmur var mı?".dropLast(1)))
        assertEquals("rain", WeatherAssistant.normalize("RAIN"))
        assertEquals("gunes kremi", WeatherAssistant.normalize("Güneş kremi"))
    }

    @Test
    fun `yarin yagmur sorusu yarinin olasiligiyla yanitlanir`() {
        val reply = ask("Yarın yağmur var mı?")

        assertEquals(R.string.bot_rain_likely, reply.firstRes())
        assertEquals(now.toLocalDate().plusDays(1), reply.firstDay())
        assertEquals(ReplyArg.Number(80), reply.lines.first().args[1])
    }

    @Test
    fun `ingilizce soru da anlasilir`() {
        val reply = ask("Will it RAIN tomorrow?")

        assertEquals(R.string.bot_rain_likely, reply.firstRes())
    }

    @Test
    fun `bugun icin yagmur saati saatlik tahminden verilir`() {
        val hours = listOf(
            HourlyForecast(now.plusHours(1), 18.0, OVERCAST, precipitationProbability = 10),
            HourlyForecast(now.plusHours(3), 17.0, RAIN, precipitationProbability = 70)
        )

        val reply = ask("bugün yağar mı", hours)

        assertEquals(ReplyLine(R.string.bot_rain_at, listOf(ReplyArg.Text("17:00"), ReplyArg.Number(70))), reply.lines.first())
    }

    @Test
    fun `gun adi en yakin o gune eslenir ve pazartesi pazarla karismaz`() {
        assertEquals(DayOfWeek.MONDAY, ask("Pazartesi hava nasıl?").firstDay().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, ask("Pazar hava nasıl?").firstDay().dayOfWeek)
        assertEquals(DayOfWeek.SATURDAY, ask("cumartesi sıcaklık kaç derece").firstDay().dayOfWeek)
    }

    @Test
    fun `sicaklik sorusunda bugun icin anlik deger de verilir`() {
        val reply = ask("Bugün kaç derece?")

        assertEquals(listOf(R.string.bot_temperature_day, R.string.bot_temperature_now), reply.lines.map { it.res })
    }

    @Test
    fun `kiyafet sorusu ne giymeli karti ile ayni kurallari kullanir`() {
        val reply = ask("Ne giymeliyim?")
        val expected = ClothingAdvice.from(forecast.current, forecast.daily[0])

        assertEquals(R.string.bot_clothing_intro, reply.firstRes())
        assertEquals(expected.summaryRes, reply.lines[1].res)
        assertEquals(ReplyArg.Items(expected.items.map { it.labelRes }), reply.lines[2].args.first())
    }

    @Test
    fun `hafta sonu piknik sorusu iki gunu ayri degerlendirir`() {
        val reply = ask("Hafta sonu pikniğe uygun mu?")

        // Cumartesi yağmurlu, pazar güzel.
        assertEquals(listOf(R.string.bot_outdoor_rain, R.string.bot_outdoor_good), reply.lines.map { it.res })
    }

    @Test
    fun `orta yagis ihtimalinde guzel denmez, semsiye hatirlatilir`() {
        // Pazartesi: 13–18°, yağmur %30.
        assertEquals(R.string.bot_outdoor_maybe, ask("pazartesi pikniğe gidebilir miyim").firstRes())
    }

    @Test
    fun `firtinali gunde disari onerilmez`() {
        val stormDay = forecast.daily[5].date.dayOfWeek
        val name = mapOf(DayOfWeek.WEDNESDAY to "çarşamba").getValue(stormDay)

        assertEquals(R.string.bot_outdoor_storm, ask("$name yürüyüşe çıkabilir miyim").firstRes())
    }

    @Test
    fun `en guzel gun yagissiz ve iliman gun secilir`() {
        val reply = ask("Bu hafta en güzel gün hangisi?")

        assertEquals(R.string.bot_best_day, reply.firstRes())
        assertEquals(forecast.daily[2].date, reply.firstDay())
    }

    @Test
    fun `15 gunluk tahminde de hafta sorulari ilk 7 gune bakar`() {
        // 8. günden sonra bulutsuz, 23° ve yağışsız "mükemmel" günler: hafta sorusunda seçilmemeli.
        val longForecast = forecast.copy(daily = forecast.daily + List(8) { day(7L + it, 18.0, 23.0, rain = 0) })

        val reply = WeatherAssistant.reply("Bu hafta en güzel gün hangisi?", longForecast, "İstanbul")

        assertEquals(forecast.daily[2].date, reply.firstDay())
    }

    @Test
    fun `uv sorusu seviye ve onlemle yanitlanir`() {
        val reply = ask("UV nasıl?")

        assertEquals(R.string.bot_uv_day, reply.firstRes())
        assertEquals(ReplyArg.Res(R.string.uv_moderate), reply.lines.first().args[2])
        assertEquals(R.string.bot_uv_fine, reply.lines.last().res)
    }

    @Test
    fun `gunes batimi sorusu gun dogumu ve batimiyla yanitlanir`() {
        val reply = ask("Güneş ne zaman batıyor?")

        assertEquals(
            ReplyLine(R.string.bot_sun, listOf(ReplyArg.Day(now.toLocalDate()), ReplyArg.Text("07:26"), ReplyArg.Text("18:09"))),
            reply.lines.first()
        )
    }

    @Test
    fun `yedi gunden sonrasi icin veri olmadigi soylenir`() {
        // Bugün cuma; "öbür gün" pazar, ama 7 günün ötesi yok. Listeyi kısaltıp deneriz.
        val short = forecast.copy(daily = forecast.daily.take(1))

        assertEquals(R.string.bot_out_of_range, WeatherAssistant.reply("yarın yağmur", short, "X").firstRes())
    }

    @Test
    fun `konusu anlasilmayan ama gun iceren soru o gunun ozetiyle yanitlanir`() {
        // Klavye "hava nasıl"ı "have nsil" yapsa da "yarın" anlaşılır.
        val reply = ask("yarin have nsil")

        assertEquals(R.string.bot_summary, reply.firstRes())
        assertEquals(now.toLocalDate().plusDays(1), reply.firstDay())
    }

    @Test
    fun `selam ve tesekkur taninir, anlamsiz soru anlasilmaz`() {
        assertEquals(R.string.bot_greeting, ask("Merhaba").firstRes())
        assertEquals(R.string.bot_greeting, ask("hi").firstRes())
        assertEquals(R.string.bot_thanks, ask("Teşekkürler!").firstRes())
        assertEquals(R.string.bot_unknown, ask("Bitcoin kaç para?").firstRes())
    }

    /**
     * Sohbette öneri olarak gösterilen soruların hepsi iki dilde de anlaşılmalı;
     * yoksa kullanıcı robotun kendi önerisine "anlayamadım" cevabı alır.
     */
    @Test
    fun `onerilen sorularin hepsi iki dilde de anlasilir`() {
        val keys = listOf(
            "bot_suggest_rain",
            "bot_suggest_wear",
            "bot_suggest_picnic",
            "bot_suggest_best",
            "bot_suggest_uv",
            "bot_suggest_sun"
        )
        listOf("src/main/res/values/strings.xml", "src/main/res/values-en/strings.xml").forEach { path ->
            val xml = File(path).readText()
            keys.forEach { key ->
                val question = Regex("""<string name="$key">(.*?)</string>""").find(xml)!!.groupValues[1].replace("\\'", "'")
                assertNotEquals("$path: \"$question\" anlaşılmadı", R.string.bot_unknown, ask(question).firstRes())
            }
        }
    }

    @Test
    fun `en guzel gun hesabi firtinayi cezalandirir`() {
        val calmButCool = day(0, 10.0, 18.0, rain = 0)
        val stormyButIdeal = day(1, 18.0, 23.0, rain = 0, code = THUNDERSTORM)

        assertEquals(calmButCool, WeatherAssistant.bestOf(listOf(calmButCool, stormyButIdeal)))
        assertTrue(LocalDate.of(2026, 10, 23).dayOfWeek == DayOfWeek.FRIDAY)
    }

    private companion object {
        const val CLEAR = 0
        const val OVERCAST = 3
        const val RAIN = 61
        const val THUNDERSTORM = 95
    }
}
