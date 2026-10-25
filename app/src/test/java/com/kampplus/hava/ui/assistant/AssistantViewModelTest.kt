package com.kampplus.hava.ui.assistant

import com.kampplus.hava.R
import com.kampplus.hava.model.DailyForecast
import com.kampplus.hava.model.ReplyLine
import com.kampplus.hava.testing.FakeCityRepository
import com.kampplus.hava.testing.FakeWeatherRepository
import com.kampplus.hava.testing.MainDispatcherRule
import com.kampplus.hava.testing.sampleCity
import com.kampplus.hava.testing.sampleForecast
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Hava Robotu sohbetinin şehir bulma ve cevap akışını doğrular. */
@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cities = FakeCityRepository(result = listOf(sampleCity(name = "İzmir")))
    private val weather = FakeWeatherRepository()

    private fun createViewModel(initialCity: AssistantCity? = null) = AssistantViewModel(initialCity, cities, weather)

    private fun AssistantViewModel.lastBot() = (messages.value.last() as ChatMessage.Bot).reply.lines

    @Test
    fun `sehirsiz sohbet genel selamla baslar`() {
        val viewModel = createViewModel()

        assertEquals(R.string.bot_greeting_general, viewModel.lastBot().first().res)
    }

    @Test
    fun `sorudaki sehir aranir, tahmini cekilir ve cevabin basina sehir yazilir`() = runTest {
        val viewModel = createViewModel()

        viewModel.ask("İzmir'de yarın yağmur var mı?")
        advanceUntilIdle()

        assertEquals("İzmir", cities.lastQuery)
        assertEquals(1, weather.callCount)
        assertEquals(R.string.bot_city_header, viewModel.lastBot().first().res)
        assertFalse(viewModel.isThinking.value)
    }

    @Test
    fun `sehir yoksa robot hangi sehri sorar`() = runTest {
        val viewModel = createViewModel()

        viewModel.ask("yarın yağmur var mı?")
        advanceUntilIdle()

        assertEquals(listOf(ReplyLine(R.string.bot_ask_city)), viewModel.lastBot())
        assertEquals(0, cities.callCount)
    }

    @Test
    fun `sonraki soruda sehir yazilmazsa onceki sehir kullanilir`() = runTest {
        val viewModel = createViewModel()
        viewModel.ask("İzmir hava nasıl?")
        advanceUntilIdle()

        viewModel.ask("yarın kaç derece?")
        advanceUntilIdle()

        // İkinci soruda yeni arama yapılmaz, başlık satırı da tekrar yazılmaz.
        assertEquals(1, cities.callCount)
        assertTrue(viewModel.lastBot().none { it.res == R.string.bot_city_header })
    }

    @Test
    fun `adi eslesmeyen sehir kabul edilmez`() = runTest {
        cities.result = listOf(sampleCity(name = "Antalya"))
        val viewModel = createViewModel()

        viewModel.ask("Ankara hava nasıl?")
        advanceUntilIdle()

        assertEquals(R.string.bot_city_not_found, viewModel.lastBot().first().res)
        assertEquals(0, weather.callCount)
    }

    @Test
    fun `ad tutan bir sonuc varsa en buyuk sehir olan ilk sonuc secilir`() = runTest {
        // Türkçe aramada "London" için ilk sonuç İngiltere'deki "Londra", ikincisi Kanada'daki "London".
        cities.result = listOf(sampleCity(id = 1, name = "Londra"), sampleCity(id = 2, name = "London"))
        val viewModel = createViewModel()

        viewModel.ask("London weather")
        advanceUntilIdle()

        val header = viewModel.lastBot().first()
        assertEquals(R.string.bot_city_header, header.res)
        assertTrue((header.args.first() as com.kampplus.hava.model.ReplyArg.Text).value.startsWith("Londra"))
    }

    @Test
    fun `tahmin ekranindan acilan sohbet o sehri bilir`() = runTest {
        val today = DailyForecast(LocalDate.parse("2026-09-24"), minTemperatureC = 12.0, maxTemperatureC = 22.0, weatherCode = 0)
        val viewModel = createViewModel(AssistantCity("Ankara", "Ankara, Türkiye", sampleForecast().copy(daily = listOf(today))))

        viewModel.ask("bugün kaç derece?")
        advanceUntilIdle()

        assertEquals(0, cities.callCount)
        assertEquals(R.string.bot_temperature_day, viewModel.lastBot().first().res)
    }

    @Test
    fun `selam icin sehir aranmaz`() = runTest {
        val viewModel = createViewModel()

        viewModel.ask("Merhaba")
        advanceUntilIdle()

        assertEquals(0, cities.callCount)
        assertEquals(R.string.bot_greeting_general, viewModel.lastBot().first().res)
    }

    @Test
    fun `baglanti hatasi anlasilir mesaja cevrilir`() = runTest {
        cities.error = IOException("no network")
        val viewModel = createViewModel()

        viewModel.ask("İzmir hava nasıl?")
        advanceUntilIdle()

        assertEquals(listOf(ReplyLine(R.string.error_connection)), viewModel.lastBot())
        assertFalse(viewModel.isThinking.value)
    }
}
