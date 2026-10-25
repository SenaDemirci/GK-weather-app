package com.kampplus.hava.ui.map

import com.kampplus.hava.R
import com.kampplus.hava.data.ProvinceTemperatureRepository
import com.kampplus.hava.model.Province
import com.kampplus.hava.model.ProvinceTemperature
import com.kampplus.hava.testing.MainDispatcherRule
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Harita ekranının yükleniyor / veri / boş / hata durumlarını doğrular. */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeRepository(var meanC: Double? = 15.0, var error: Throwable? = null) : ProvinceTemperatureRepository {
        var requested: List<Province> = emptyList()

        override suspend fun getTemperatures(provinces: List<Province>): List<ProvinceTemperature> {
            requested = provinces
            error?.let { throw it }
            return provinces.map { ProvinceTemperature(it, meanC, 0) }
        }
    }

    @Test
    fun `81 ilin hepsi istenir ve Success olur`() = runTest {
        val repository = FakeRepository()
        val viewModel = MapViewModel(repository)
        advanceUntilIdle()

        assertEquals(81, repository.requested.size)
        assertTrue(viewModel.uiState.value is LoadState.Success)
    }

    @Test
    fun `hicbir ilde deger yoksa Empty olur`() = runTest {
        val viewModel = MapViewModel(FakeRepository(meanC = null))
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `baglanti hatasi Error olur, tekrar dene yeniden yukler`() = runTest {
        val repository = FakeRepository(error = IOException("no network"))
        val viewModel = MapViewModel(repository)
        advanceUntilIdle()
        assertEquals(LoadState.Error(R.string.error_connection), viewModel.uiState.value)

        repository.error = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoadState.Success)
    }
}
