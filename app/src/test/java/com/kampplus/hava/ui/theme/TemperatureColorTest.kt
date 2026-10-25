package com.kampplus.hava.ui.theme

import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Haritadaki sıcaklık renginin ölçek duraklarına ve aradaki karışımlara uyduğunu doğrular. */
class TemperatureColorTest {

    @Test
    fun `durak sicakliklari tam o rengi verir`() {
        assertEquals(TempMild, temperatureColor(15.0))
        assertEquals(TempWarm, temperatureColor(20.0))
    }

    @Test
    fun `iki durak arasi iki rengin karisimidir`() {
        assertEquals(lerp(TempMild, TempWarm, 0.5f), temperatureColor(17.5))
    }

    @Test
    fun `olcek disi degerler uc renklere sabitlenir, deger yoksa gri`() {
        assertEquals(TempFreezing, temperatureColor(-25.0))
        assertEquals(TempExtreme, temperatureColor(45.0))
        assertEquals(PollenNone, temperatureColor(null))
    }
}
