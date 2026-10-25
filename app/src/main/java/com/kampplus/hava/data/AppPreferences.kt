package com.kampplus.hava.data

import android.content.Context
import androidx.core.content.edit
import com.kampplus.hava.ui.locale.AppLanguage
import com.kampplus.hava.ui.theme.ThemeMode

/**
 * Kullanıcının seçtiği tema ve dili cihazda saklar.
 *
 * `SharedPreferences`: küçük anahtar–değer çiftleri için Android'in hazır deposu.
 * Birkaç ayar için veritabanı ya da ek kütüphane gerekmez. Enum adını ("DARK") metin
 * olarak yazarız; okurken tanınmayan bir değer gelirse varsayılana döneriz.
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = readEnum(KEY_THEME_MODE, ThemeMode.entries) ?: ThemeMode.SYSTEM
        set(value) = prefs.edit { putString(KEY_THEME_MODE, value.name) }

    /**
     * Henüz seçim yapılmadıysa (uygulama ilk kez açılıyor) Türkçe başlar; telefonun
     * dili ne olursa olsun. Kullanıcı istediği an "Sistem dili"ne geçebilir.
     */
    var language: AppLanguage
        get() = readEnum(KEY_LANGUAGE, AppLanguage.entries) ?: AppLanguage.TURKISH
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value.name) }

    private fun <T : Enum<T>> readEnum(key: String, entries: List<T>): T? =
        prefs.getString(key, null)?.let { name -> entries.firstOrNull { it.name == name } }

    private companion object {
        const val FILE_NAME = "settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LANGUAGE = "language"
    }
}
