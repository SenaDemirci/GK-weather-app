package com.kampplus.hava.ui.theme

import androidx.annotation.StringRes
import com.kampplus.hava.R

/**
 * Kullanıcının seçtiği görünüm: telefonu izle, hep açık ya da hep koyu.
 *
 * Seçim [com.kampplus.hava.data.AppPreferences] ile kaydedilir; uygulama kapanıp
 * açılsa da hatırlanır.
 *
 * @param emoji Şehir ekranındaki tema düğmesinde ve menüde gösterilir.
 */
enum class ThemeMode(@StringRes val labelRes: Int, val emoji: String) {
    SYSTEM(R.string.theme_system, "🌓"),
    LIGHT(R.string.theme_light, "☀️"),
    DARK(R.string.theme_dark, "🌙");

    /**
     * Bu seçimde koyu tema mı çizilmeli?
     *
     * @param systemDark Telefonun kendi ayarı; yalnızca [SYSTEM] seçiliyken önemlidir.
     */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}
