package com.kampplus.hava.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tema seçiminin koyu/açık kararını doğrular. */
class ThemeModeTest {

    @Test
    fun `sistem secimi telefonun ayarini izler`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
    }

    @Test
    fun `acik ve koyu secimi telefonun ayarini yok sayar`() {
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }
}
