package com.kampplus.hava.data

import com.kampplus.hava.model.PollenLevel
import com.kampplus.hava.model.PollenType
import com.kampplus.hava.testing.createApi
import com.kampplus.hava.testing.readResource
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Saatlik polen sütunlarının günlük en yüksek değerlere doğru toplandığını doğrular. */
class PollenApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AirQualityApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = server.createApi()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `saatlik degerler gunluk en yuksege toplanir`() = runTest {
        server.enqueue(MockResponse().setBody(readResource("pollen_single.json")))

        val forecast = checkNotNull(api.getPollen(41.0, 29.0).toPollenForecast())

        assertEquals(listOf(LocalDate.parse("2026-04-20"), LocalDate.parse("2026-04-21")), forecast.days.map { it.date })
        assertEquals(25.5, checkNotNull(forecast.days[0].values[PollenType.GRASS]), 0.01)
        // İkinci günün ilk değeri null; kalan değerlerin en büyüğü alınır.
        assertEquals(4.0, checkNotNull(forecast.days[1].values[PollenType.GRASS]), 0.01)
        assertEquals(PollenLevel.HIGH, forecast.days[0].levelOf(PollenType.OLIVE))
        // Hiç değeri olmayan tür null kalır, tabloda gösterilmez.
        assertNull(forecast.days[0].values[PollenType.ALDER])
    }

    @Test
    fun `istek polen alanlarini ve 4 gunu ister`() = runTest {
        server.enqueue(MockResponse().setBody(readResource("pollen_single.json")))

        api.getPollen(41.0, 29.0)

        val path = checkNotNull(server.takeRequest().path)
        assertTrue(path, path.contains("grass_pollen"))
        assertTrue(path, path.contains("forecast_days=4"))
    }

    @Test
    fun `kapsam disi bolgede tahmin olusmaz`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"hourly":{"time":["2026-04-20T00:00"],"grass_pollen":[null],"birch_pollen":[null]}}"""
            )
        )

        assertNull(api.getPollen(40.7, -74.0).toPollenForecast())
    }
}
