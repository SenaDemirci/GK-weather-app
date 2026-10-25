package com.kampplus.hava.data

import com.kampplus.hava.model.Province
import com.kampplus.hava.testing.createApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Çok noktalı sıcaklık yanıtının illerle sırayla eşleştiğini doğrular. */
class ProvinceTemperatureApiTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: OpenMeteoProvinceTemperatureRepository

    private val provinces = listOf(
        Province(34, "İstanbul", 41.01, 28.95),
        Province(6, "Ankara", 39.92, 32.85),
        Province(35, "İzmir", 38.41, 27.14)
    )

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        repository = OpenMeteoProvinceTemperatureRepository(server.createApi(), UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `yanit sirayla illere eslenir, eksik deger null kalir`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[
                  {"daily":{"time":["2026-10-26"],"temperature_2m_mean":[17.4],"weather_code":[61]}},
                  {"daily":{"time":["2026-10-26"],"temperature_2m_mean":[11.2],"weather_code":[3]}},
                  {"daily":{"time":["2026-10-26"],"temperature_2m_mean":[null],"weather_code":[null]}}
                ]"""
            )
        )

        val result = repository.getTemperatures(provinces)

        assertEquals(listOf("İstanbul", "Ankara", "İzmir"), result.map { it.province.name })
        assertEquals(17.4, result[0].meanC!!, 0.01)
        assertEquals(61, result[0].weatherCode)
        assertEquals(11.2, result[1].meanC!!, 0.01)
        assertNull(result[2].meanC)
    }

    @Test
    fun `butun iller tek istekte virgulle gonderilir`() = runTest {
        server.enqueue(MockResponse().setBody("[]"))

        repository.getTemperatures(provinces)

        val path = checkNotNull(server.takeRequest().path)
        assertEquals(1, server.requestCount)
        assertTrue(path, path.contains("latitude=41.01%2C39.92%2C38.41") || path.contains("latitude=41.01,39.92,38.41"))
        assertTrue(path, path.contains("temperature_2m_mean"))
    }
}
