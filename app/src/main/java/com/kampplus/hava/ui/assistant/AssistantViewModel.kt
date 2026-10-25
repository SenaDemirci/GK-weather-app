package com.kampplus.hava.ui.assistant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import com.kampplus.hava.R
import com.kampplus.hava.data.CityRepository
import com.kampplus.hava.data.OpenMeteoCityRepository
import com.kampplus.hava.data.OpenMeteoWeatherRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.model.AssistantReply
import com.kampplus.hava.model.CityMention
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.ReplyArg
import com.kampplus.hava.model.ReplyLine
import com.kampplus.hava.model.WeatherAssistant
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Sohbetteki tek bir mesaj. */
sealed interface ChatMessage {
    data class User(val text: String) : ChatMessage

    /** @param today Cevabın ait olduğu şehrin bugünü; "yarın", "cuma" gibi günler buna göre yazılır. */
    data class Bot(val reply: AssistantReply, val today: LocalDate? = null) : ChatMessage
}

/**
 * Sohbetin konuştuğu şehir: adı, "İzmir, Türkiye" alt yazısı ve tahmini.
 */
data class AssistantCity(val name: String, val subtitle: String, val forecast: Forecast) {
    /**
     * Başlıkta yazan ad: "İzmir, Türkiye". Alt yazı şehrin adıyla başlamıyorsa ("İngiltere,
     * Birleşik Krallık") başına şehrin adı eklenir: "Londra, İngiltere, Birleşik Krallık".
     */
    val label: String
        get() = when {
            subtitle.isEmpty() -> name
            subtitle.startsWith(name) -> subtitle
            else -> "$name, $subtitle"
        }

    /** Şehrin yerel tarihine göre bugün. */
    val today: LocalDate get() = forecast.current.observedAt.toLocalDate()
}

/**
 * Hava Robotu sohbetinin durumu ve şehir araması.
 *
 * Kurallar [WeatherAssistant]'tadır; bu sınıf yalnızca sırayı yönetir:
 * 1. Soru selam/teşekkürse hemen yanıtlanır.
 * 2. Değilse sorudaki şehir adayları ([CityMention]) şehir arama servisinde aranır;
 *    bulunan ilk şehrin tahmini çekilir ve sohbet artık o şehir hakkında konuşur.
 * 3. Soruda şehir yoksa son konuşulan şehir kullanılır ("İzmir hava nasıl?" → "ya yarın?").
 * 4. Hiç şehir yoksa robot hangi şehri merak ettiğini sorar.
 *
 * Ağ istekleri `viewModelScope` içinde çalışır; ekran kapanırsa yarım kalan istek iptal
 * olur. Cevap gelene kadar [isThinking] true olur ve ekran "yazıyor…" balonu gösterir.
 *
 * @param initialCity Tahmin ekranından açıldıysa o şehir; şehir ekranından açıldıysa null.
 */
class AssistantViewModel(
    initialCity: AssistantCity? = null,
    private val cityRepository: CityRepository = OpenMeteoCityRepository(),
    private val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository()
) : ViewModel() {

    private var currentCity: AssistantCity? = initialCity

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage.Bot(
                initialCity?.let { WeatherAssistant.greeting(it.name) } ?: WeatherAssistant.generalGreeting(),
                initialCity?.today
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    /** Kullanıcı bir soru gönderdi. */
    fun ask(question: String) {
        val text = question.trim()
        if (text.isEmpty()) return
        _messages.update { it + ChatMessage.User(text) }
        viewModelScope.launch {
            _isThinking.value = true
            val reply = try {
                answer(text)
            } catch (e: IOException) {
                Log.e(TAG, "Robot cevap veremedi: bağlantı hatası", e)
                errorReply(R.string.error_connection)
            } catch (e: HttpException) {
                Log.e(TAG, "Robot cevap veremedi: HTTP ${e.code()}", e)
                errorReply(R.string.error_server)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Robot cevap veremedi: yanıt çözümlenemedi", e)
                errorReply(R.string.error_unexpected)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Robot cevap veremedi: eksik veri", e)
                errorReply(R.string.error_unexpected)
            }
            _messages.update { it + ChatMessage.Bot(reply, currentCity?.today) }
            _isThinking.value = false
        }
    }

    private suspend fun answer(question: String): AssistantReply {
        WeatherAssistant.smallTalk(question, currentCity?.name)?.let { return it }

        val candidates = CityMention.candidates(question)
        val found = findCity(candidates)
        val city = found ?: currentCity
            ?: return AssistantReply(
                listOf(
                    candidates.firstOrNull()
                        ?.let { ReplyLine(R.string.bot_city_not_found, listOf(ReplyArg.Text(it))) }
                        ?: ReplyLine(R.string.bot_ask_city)
                )
            )
        currentCity = city

        val now = city.forecast.current.observedAt.truncatedTo(ChronoUnit.HOURS)
        val upcoming = city.forecast.hourly.filter { !it.time.isBefore(now) }
        val reply = WeatherAssistant.reply(question, city.forecast, city.name, upcoming, defaultToSummary = found != null)
        // Yeni bir şehre geçildiyse cevabın başına hangi şehir olduğu yazılır: "📍 İzmir, Türkiye".
        return if (found != null) {
            val header = ReplyLine(R.string.bot_city_header, listOf(ReplyArg.Text(found.label)))
            AssistantReply(listOf(header) + reply.lines)
        } else {
            reply
        }
    }

    /**
     * Adayları sırayla arar ve bulunan şehrin tahminini getirir.
     *
     * Sonuçlardan biri adayla aynı adı taşıyorsa aday gerçekten bir şehirdir; o zaman
     * listenin **ilk** sonucu seçilir, çünkü servis en büyük şehri başa koyar. Örneğin
     * Türkçe aramada "London" yazınca ilk sonuç "Londra" (İngiltere) gelir; adı tutan
     * Kanada'daki London ise ancak ikinci sıradadır.
     */
    private suspend fun findCity(candidates: List<String>): AssistantCity? {
        for (candidate in candidates) {
            val results = cityRepository.search(candidate)
            if (results.none { CityMention.matches(candidate, it.name) }) continue
            val city = results.first()
            val forecast = weatherRepository.getForecast(city.latitude, city.longitude)
            return AssistantCity(name = city.name, subtitle = city.subtitle, forecast = forecast)
        }
        return null
    }

    private fun errorReply(messageRes: Int) = AssistantReply(listOf(ReplyLine(messageRes)))

    private companion object {
        const val TAG = "AssistantViewModel"
    }
}
