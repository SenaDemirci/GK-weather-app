package com.kampplus.hava.model

import androidx.annotation.StringRes
import com.kampplus.hava.R
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hava Robotu'nun cevabı: bir ya da birkaç cümle.
 *
 * [ClothingAdvice] gibi metni kendisi yazmaz; her cümle bir `R.string` kimliği ve
 * boşlukları dolduracak değerlerdir. Ekran bunları o anki dilde birleştirir.
 */
data class AssistantReply(val lines: List<ReplyLine>)

/** Tek bir cümle: metnin kimliği ve içindeki `%1$s`, `%2$d`... yerlerine gelecek değerler. */
data class ReplyLine(@StringRes val res: Int, val args: List<ReplyArg> = emptyList())

/**
 * Cümledeki bir boşluğun değeri.
 *
 * Bazı değerler dile göre yazılır ("yarın" / "tomorrow", "Yağmurlu" / "Rain");
 * bunları burada çözmeyip ekrana bırakırız.
 */
sealed interface ReplyArg {
    data class Text(val value: String) : ReplyArg
    data class Number(val value: Int) : ReplyArg

    /** Başka bir metin kaynağı, örneğin hava durumu adı. */
    data class Res(@StringRes val res: Int) : ReplyArg

    /** Bir gün: ekran "bugün", "yarın" ya da gün adı yazar. */
    data class Day(val date: LocalDate) : ReplyArg

    /** Virgülle sıralanacak metinler: "Mont, Atkı, Pantolon". */
    data class Items(val res: List<Int>) : ReplyArg
}

/**
 * Hava Robotu: tahmin ekranındaki sohbetin "beyni".
 *
 * Yapay zeka değildir; internete ya da bir dil modeline sormaz, ücretsizdir ve çevrimdışı
 * çalışır. Sorudaki anahtar kelimelerden **ne** sorulduğunu ([Topic]) ve **hangi gün**
 * için sorulduğunu çıkarır, cevabı eldeki tahminden kurar. Tanımadığı soruya
 * "anlayamadım" der ve ekran örnek sorular önerir.
 *
 * Türkçe ve İngilizce soruları anlar. Kelimeler karşılaştırılmadan önce sadeleştirilir:
 * küçük harfe çevrilir, Türkçe harfler düz harfe indirilir ("Yağmur" → "yagmur").
 * Böylece "yağmur", "YAĞMUR" ve "yagmur" aynı kelime sayılır. Kelimeleri kök olarak
 * ararız: "yagmur" kökü "yağmurlu", "yağmurdan" gibi ekli halleri de yakalar.
 * Ek alınca sonu değişen kelimelerde kök kısaltılır: "piknik" → "pikniğe" olduğu için
 * kök "pikni", "sıcak" → "sıcağı" olduğu için kök "sica".
 */
object WeatherAssistant {

    /** Robotun anlayabildiği konular. Sıra önemlidir: üstteki önce denenir. */
    internal enum class Topic(val stems: List<String>, val words: List<String> = emptyList()) {
        // "Hafta sonu pikniğe uygun mu?" hem etkinlik hem gün sorusudur; önce etkinlik yakalanır.
        BEST_DAY(listOf("en guzel", "en iyi gun", "hangi gun", "best day", "which day", "nicest")),
        ACTIVITY(
            listOf(
                "pikni", "yuruyus", "yuruyu", "kosu", "bisiklet", "disari", "plaj", "deniz", "mangal",
                "picnic", "walk", "running", "bike", "outside", "outdoor", "beach", "hike", "jog"
            ),
            words = listOf("run")
        ),
        CLOTHING(listOf("giy", "kiyafet", "mont", "ceket", "kazak", "wear", "jacket", "coat", "clothes", "outfit")),
        RAIN(listOf("yagmur", "yagis", "yagar", "yagacak", "semsiye", "rain", "umbrella", "shower")),
        SUN(
            listOf(
                "gunes dog", "gunes bat", "gun dog", "gun bat", "batiyor", "batacak", "doguyor", "dogacak",
                "sunrise", "sunset", "hava karar", "aydinlik"
            )
        ),
        UV(listOf("krem", "sunscreen", "gunes", "sunburn"), words = listOf("uv")),
        WIND(listOf("ruzgar", "wind", "esinti", "firtina", "storm")),
        TEMPERATURE(
            listOf("sica", "sogu", "derece", "serin", "ilik", "temperature", "degree", "cold", "warm", "chilly"),
            words = listOf("hot")
        ),
        SUMMARY(listOf("hava nasil", "nasil olacak", "hava durumu", "weather", "forecast", "tahmin")),
        GREETING(listOf("merhaba", "selam", "gunaydin", "hello"), words = listOf("hi", "hey", "slm")),
        THANKS(listOf("tesekkur", "sagol", "eyvallah", "thank"));

        fun matches(text: String, tokens: Set<String>): Boolean = stems.any { it in text } || words.any { it in tokens }
    }

    /** Sorunun hangi günü kastettiği. */
    internal sealed interface DayQuery {
        data class Single(val offset: Int) : DayQuery
        data class Weekday(val day: DayOfWeek) : DayQuery
        data object Weekend : DayQuery
        data object Week : DayQuery
    }

    /** Şehir seçilmeden açılan sohbetin ilk cümlesi: herhangi bir şehri sorabileceğini söyler. */
    fun generalGreeting(): AssistantReply = AssistantReply(listOf(ReplyLine(R.string.bot_greeting_general)))

    /**
     * Soru yalnızca selam ya da teşekkürse cevabını verir; değilse null.
     * Bunlara şehir aramadan, tahmine bakmadan cevap verilir.
     *
     * @param cityName Sohbet bir şehir hakkındaysa selamda o şehir anılır.
     */
    fun smallTalk(question: String, cityName: String?): AssistantReply? = when (topicOf(question)) {
        Topic.GREETING -> cityName?.let(::greeting) ?: generalGreeting()
        Topic.THANKS -> AssistantReply(listOf(ReplyLine(R.string.bot_thanks)))
        else -> null
    }

    private fun topicOf(question: String): Topic? {
        val text = normalize(question)
        val tokens = text.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
        // Selam ve teşekkür listenin sonundadır; "merhaba, yarın yağmur?" önce yağmur olarak yakalanır.
        return Topic.entries.firstOrNull { it.matches(text, tokens) }
    }

    /** Robotun açılışta söylediği ilk cümle. */
    fun greeting(cityName: String): AssistantReply = AssistantReply(
        listOf(ReplyLine(R.string.bot_greeting, listOf(ReplyArg.Text(cityName))))
    )

    /**
     * Soruyu yanıtlar.
     *
     * @param cityName Selamlaşmada kullanılır.
     * @param upcomingHours Şehrin yerel saatine göre şu andan sonraki saatler;
     *   "bugün yağmur var mı?" sorusunda saat vermek için kullanılır.
     * @param defaultToSummary Konu anlaşılmazsa "anlayamadım" yerine günün özetini ver.
     */
    fun reply(
        question: String,
        forecast: Forecast,
        cityName: String,
        upcomingHours: List<HourlyForecast> = emptyList(),
        defaultToSummary: Boolean = false
    ): AssistantReply {
        val text = normalize(question)
        // Konu bulunamadıysa ama soru bir şehir adı içeriyorsa ("İzmir?") o şehrin özetini veririz.
        val topic = topicOf(question) ?: if (defaultToSummary) Topic.SUMMARY else null
        val dayQuery = parseDay(text)
        val today = forecast.current.observedAt.toLocalDate()

        return when (topic) {
            null -> AssistantReply(listOf(ReplyLine(R.string.bot_unknown)))
            Topic.GREETING -> greeting(cityName)
            Topic.THANKS -> AssistantReply(listOf(ReplyLine(R.string.bot_thanks)))
            Topic.BEST_DAY -> bestDay(forecast, dayQuery)
            Topic.ACTIVITY -> activity(forecast, today, dayQuery)
            else -> {
                // Geri kalan konular tek bir güne bakar; "hafta"/"hafta sonu" gelirse ilk uygun gün alınır.
                val day = resolveSingleDay(forecast, today, dayQuery)
                    ?: return AssistantReply(listOf(ReplyLine(R.string.bot_out_of_range)))
                val isToday = day.date == today
                when (topic) {
                    Topic.RAIN -> rain(forecast, day, isToday, upcomingHours)
                    Topic.TEMPERATURE -> temperature(forecast, day, isToday)
                    Topic.CLOTHING -> clothing(forecast, day, isToday, upcomingHours)
                    Topic.UV -> uv(forecast, day, isToday)
                    Topic.SUN -> sun(day)
                    Topic.WIND -> wind(forecast)
                    else -> AssistantReply(listOf(summaryLine(day)))
                }
            }
        }
    }

    // region Konular

    private fun rain(forecast: Forecast, day: DailyForecast, isToday: Boolean, upcoming: List<HourlyForecast>): AssistantReply {
        if (isToday) {
            val condition = WeatherCondition.fromWeatherCode(forecast.current.weatherCode)
            if (condition in WET) return AssistantReply(listOf(ReplyLine(R.string.bot_rain_now)))
            val rainyHour = upcoming
                .filter { it.time.toLocalDate() == day.date }
                .firstOrNull { (it.precipitationProbability ?: 0) >= RAIN_LIKELY }
            if (rainyHour != null) {
                return AssistantReply(
                    listOf(
                        ReplyLine(
                            R.string.bot_rain_at,
                            listOf(
                                ReplyArg.Text(rainyHour.time.format(HOUR_FORMAT)),
                                ReplyArg.Number(rainyHour.precipitationProbability ?: 0)
                            )
                        )
                    )
                )
            }
        }
        val chance = day.precipitationProbability
            ?: return AssistantReply(listOf(ReplyLine(R.string.bot_no_data)))
        val res = when {
            chance >= RAIN_LIKELY -> R.string.bot_rain_likely
            chance >= RAIN_MAYBE -> R.string.bot_rain_maybe
            else -> R.string.bot_rain_unlikely
        }
        return AssistantReply(listOf(ReplyLine(res, listOf(ReplyArg.Day(day.date), ReplyArg.Number(chance)))))
    }

    private fun temperature(forecast: Forecast, day: DailyForecast, isToday: Boolean): AssistantReply {
        val lines = mutableListOf(
            ReplyLine(
                R.string.bot_temperature_day,
                listOf(
                    ReplyArg.Day(day.date),
                    ReplyArg.Number(day.minTemperatureC.roundToInt()),
                    ReplyArg.Number(day.maxTemperatureC.roundToInt())
                )
            )
        )
        if (isToday) {
            val current = forecast.current
            val feels = current.apparentTemperatureC ?: current.temperatureC
            lines += ReplyLine(
                R.string.bot_temperature_now,
                listOf(ReplyArg.Number(current.temperatureC.roundToInt()), ReplyArg.Number(feels.roundToInt()))
            )
        }
        return AssistantReply(lines)
    }

    /**
     * Kıyafet önerisi, ekrandaki "Ne giymeli?" kartıyla aynı kurallarla ([ClothingAdvice]).
     *
     * Bugün için anlık hava kullanılır. Başka bir gün için anlık veri yoktur; o günün
     * en yüksek sıcaklığını gündüz sıcaklığı sayarız.
     */
    private fun clothing(forecast: Forecast, day: DailyForecast, isToday: Boolean, upcoming: List<HourlyForecast>): AssistantReply {
        val advice = if (isToday) {
            ClothingAdvice.from(forecast.current, day, upcoming)
        } else {
            val daytime = CurrentWeather(
                temperatureC = day.maxTemperatureC,
                weatherCode = day.weatherCode,
                observedAt = day.date.atTime(12, 0)
            )
            ClothingAdvice.from(daytime, day)
        }
        val lines = mutableListOf(
            ReplyLine(R.string.bot_clothing_intro, listOf(ReplyArg.Day(day.date))),
            ReplyLine(advice.summaryRes),
            ReplyLine(R.string.bot_clothing_items, listOf(ReplyArg.Items(advice.items.map { it.labelRes })))
        )
        advice.tips.forEach { tip ->
            lines += ReplyLine(tip.textRes, tip.args.map { if (it is Int) ReplyArg.Number(it) else ReplyArg.Text(it.toString()) })
        }
        return AssistantReply(lines)
    }

    private fun uv(forecast: Forecast, day: DailyForecast, isToday: Boolean): AssistantReply {
        val max = day.uvIndexMax ?: return AssistantReply(listOf(ReplyLine(R.string.bot_no_data)))
        val level = UvLevel.of(max)
        val lines = mutableListOf(
            ReplyLine(R.string.bot_uv_day, listOf(ReplyArg.Day(day.date), ReplyArg.Number(max.roundToInt()), ReplyArg.Res(level.labelRes)))
        )
        if (isToday) {
            forecast.current.uvIndex?.let { lines += ReplyLine(R.string.bot_uv_now, listOf(ReplyArg.Number(it.roundToInt()))) }
        }
        lines += ReplyLine(if (level >= UvLevel.HIGH) R.string.bot_uv_protect else R.string.bot_uv_fine)
        return AssistantReply(lines)
    }

    private fun sun(day: DailyForecast): AssistantReply {
        val sunrise = day.sunrise
        val sunset = day.sunset
        if (sunrise == null || sunset == null) return AssistantReply(listOf(ReplyLine(R.string.bot_no_data)))
        return AssistantReply(
            listOf(
                ReplyLine(
                    R.string.bot_sun,
                    listOf(ReplyArg.Day(day.date), ReplyArg.Text(sunrise.format(HOUR_FORMAT)), ReplyArg.Text(sunset.format(HOUR_FORMAT)))
                )
            )
        )
    }

    /** Servis rüzgârı yalnızca anlık gönderir; başka günler için bunu açıkça söyleriz. */
    private fun wind(forecast: Forecast): AssistantReply {
        val speed = forecast.current.windSpeedKmh ?: return AssistantReply(listOf(ReplyLine(R.string.bot_no_data)))
        val res = if (speed >= WINDY_KMH) R.string.bot_wind_strong else R.string.bot_wind_calm
        return AssistantReply(listOf(ReplyLine(res, listOf(ReplyArg.Number(speed.roundToInt())))))
    }

    /**
     * "Pikniğe uygun mu?" Bir günü dışarıda vakit geçirmeye uygunluğuna göre değerlendirir.
     *
     * Hafta sonu sorulduysa iki gün de ayrı ayrı yanıtlanır.
     */
    private fun activity(forecast: Forecast, today: LocalDate, query: DayQuery?): AssistantReply {
        val days = when (query) {
            DayQuery.Weekend -> forecast.daily.filter { it.date.dayOfWeek in WEEKEND }
            DayQuery.Week -> listOfNotNull(bestOf(forecast.daily))
            else -> listOfNotNull(resolveSingleDay(forecast, today, query))
        }
        if (days.isEmpty()) return AssistantReply(listOf(ReplyLine(R.string.bot_out_of_range)))
        return AssistantReply(days.map { outdoorLine(it) })
    }

    private fun outdoorLine(day: DailyForecast): ReplyLine {
        val condition = WeatherCondition.fromWeatherCode(day.weatherCode)
        val chance = day.precipitationProbability ?: 0
        val max = day.maxTemperatureC.roundToInt()
        val dayArg = ReplyArg.Day(day.date)
        return when {
            condition == WeatherCondition.THUNDERSTORM -> ReplyLine(R.string.bot_outdoor_storm, listOf(dayArg))
            chance >= RAIN_LIKELY || condition in WET -> ReplyLine(R.string.bot_outdoor_rain, listOf(dayArg, ReplyArg.Number(chance)))
            max < OUTDOOR_MIN_C -> ReplyLine(R.string.bot_outdoor_cold, listOf(dayArg, ReplyArg.Number(max)))
            max > OUTDOOR_MAX_C -> ReplyLine(R.string.bot_outdoor_hot, listOf(dayArg, ReplyArg.Number(max)))
            // Hava güzel ama yağmur ihtimali yok sayılacak kadar düşük değil.
            chance >= OUTDOOR_RAIN_MAYBE -> ReplyLine(R.string.bot_outdoor_maybe, listOf(dayArg, ReplyArg.Number(chance)))
            else -> ReplyLine(
                R.string.bot_outdoor_good,
                listOf(
                    dayArg,
                    ReplyArg.Res(condition.labelRes),
                    ReplyArg.Number(day.minTemperatureC.roundToInt()),
                    ReplyArg.Number(max),
                    ReplyArg.Number(chance)
                )
            )
        }
    }

    private fun bestDay(forecast: Forecast, query: DayQuery?): AssistantReply {
        val candidates = if (query == DayQuery.Weekend) forecast.daily.filter { it.date.dayOfWeek in WEEKEND } else forecast.daily
        val best = bestOf(candidates) ?: return AssistantReply(listOf(ReplyLine(R.string.bot_no_data)))
        return AssistantReply(
            listOf(
                ReplyLine(R.string.bot_best_day, listOf(ReplyArg.Day(best.date))),
                summaryLine(best)
            )
        )
    }

    /**
     * En güzel gün: yağış olasılığı düşük, sıcaklığı 23°'ye yakın, fırtınasız gün.
     * Her gün için bir "ceza" puanı hesaplanır; en düşük puanlı gün kazanır.
     */
    internal fun bestOf(days: List<DailyForecast>): DailyForecast? = days.minByOrNull { day ->
        val storm = if (WeatherCondition.fromWeatherCode(day.weatherCode) == WeatherCondition.THUNDERSTORM) STORM_PENALTY else 0.0
        (day.precipitationProbability ?: 0) + abs(day.maxTemperatureC - IDEAL_C) * TEMPERATURE_WEIGHT + storm
    }

    private fun summaryLine(day: DailyForecast): ReplyLine = ReplyLine(
        R.string.bot_summary,
        listOf(
            ReplyArg.Day(day.date),
            ReplyArg.Res(WeatherCondition.fromWeatherCode(day.weatherCode).labelRes),
            ReplyArg.Number(day.minTemperatureC.roundToInt()),
            ReplyArg.Number(day.maxTemperatureC.roundToInt()),
            ReplyArg.Number(day.precipitationProbability ?: 0)
        )
    )

    // endregion

    // region Gün ve metin çözümleme

    /**
     * Sorudaki gün ifadesini bulur; bulamazsa null (bugün sayılır).
     *
     * "pazartesi" içinde "pazar", "cumartesi" içinde "cuma" geçtiği için uzun adlar önce denenir.
     */
    internal fun parseDay(text: String): DayQuery? = when {
        "hafta sonu" in text || "haftasonu" in text || "weekend" in text -> DayQuery.Weekend
        "bu hafta" in text || "this week" in text || "hafta" in text || "week" in text -> DayQuery.Week
        "obur gun" in text || "ertesi gun" in text || "day after tomorrow" in text -> DayQuery.Single(2)
        "yarin" in text || "tomorrow" in text -> DayQuery.Single(1)
        "bugun" in text || "today" in text || "bu aksam" in text || "tonight" in text || "simdi" in text ||
            " now " in " $text " -> DayQuery.Single(0)
        else -> WEEKDAY_NAMES.firstOrNull { (name, _) -> name in text }?.let { DayQuery.Weekday(it.second) }
    }

    private fun resolveSingleDay(forecast: Forecast, today: LocalDate, query: DayQuery?): DailyForecast? {
        val target: LocalDate? = when (query) {
            null -> today
            is DayQuery.Single -> today.plusDays(query.offset.toLong())
            is DayQuery.Weekday -> forecast.daily.firstOrNull { it.date.dayOfWeek == query.day && !it.date.isBefore(today) }?.date
            DayQuery.Weekend -> forecast.daily.firstOrNull { it.date.dayOfWeek in WEEKEND }?.date
            DayQuery.Week -> today
        }
        return forecast.daily.firstOrNull { it.date == target }
    }

    /**
     * Karşılaştırma için sadeleştirir: küçük harf, Türkçe harfler düz harf, fazla boşluk tek boşluk.
     *
     * Türkçe kurallarla küçültürüz ("I" → "ı", "İ" → "i"), sonra ı → i ve aksanları atarız.
     * İngilizce kelimeler de aynı yoldan geçer: "RAIN" → "raın" → "rain".
     */
    internal fun normalize(text: String): String {
        val lower = text.lowercase(TURKISH).replace('ı', 'i')
        val withoutMarks = Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return withoutMarks.replace(Regex("\\s+"), " ").trim()
    }

    // endregion

    private val TURKISH: Locale = Locale.forLanguageTag("tr")
    private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val WET = setOf(WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS, WeatherCondition.THUNDERSTORM)
    private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    /** Uzun adlar önce: "pazartesi" "pazar"dan, "cumartesi" "cuma"dan önce gelir. */
    private val WEEKDAY_NAMES = listOf(
        "pazartesi" to DayOfWeek.MONDAY, "cumartesi" to DayOfWeek.SATURDAY,
        "sali" to DayOfWeek.TUESDAY, "carsamba" to DayOfWeek.WEDNESDAY, "persembe" to DayOfWeek.THURSDAY,
        "cuma" to DayOfWeek.FRIDAY, "pazar" to DayOfWeek.SUNDAY,
        "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY, "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY, "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY
    )

    private const val RAIN_LIKELY = 50
    private const val RAIN_MAYBE = 20
    private const val WINDY_KMH = 30.0
    private const val OUTDOOR_RAIN_MAYBE = 30
    private const val OUTDOOR_MIN_C = 15
    private const val OUTDOOR_MAX_C = 32
    private const val IDEAL_C = 23.0
    private const val TEMPERATURE_WEIGHT = 3.0
    private const val STORM_PENALTY = 100.0
}
