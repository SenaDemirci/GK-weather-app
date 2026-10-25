package com.kampplus.hava

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kampplus.hava.data.AppPreferences
import com.kampplus.hava.ui.HavaNavHost
import com.kampplus.hava.ui.theme.HavaTheme

/**
 * Uygulamanın tek Activity'si; kullanıcının girdiği kapı.
 *
 * Activity bir **pencereyi** yönetir. Pencerenin içine ne çizileceğini ise
 * Compose söyler: sahne Activity'dir, dekoru Compose kurar.
 */
class MainActivity : ComponentActivity() {

    /**
     * `onCreate`'ten bile önce çağrılır; Activity'nin kullanacağı bağlamı burada veririz.
     * Kayıtlı dil seçimine göre bağlamı o dile çeviriyoruz; bütün metinler o dilde okunur.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppPreferences(newBase).language.wrap(newBase))
    }

    /**
     * Ekran oluşturulurken Android bu fonksiyonu **kendisi** çağırır; biz çağırmayız.
     * Yaşam döngüsünün ilk adımıdır.
     *
     * Logcat'i açıp uygulamayı arka plana alıp geri dönerek `onCreate` / `onStart` /
     * `onResume` sırasını gözlemleyebilirsin.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        val preferences = AppPreferences(this)

        // Süslü parantezin içi Compose dünyasıdır.
        setContent {
            // Seçim burada tutulur ki değişince bütün uygulama yeni temayla yeniden çizilsin.
            var themeMode by remember { mutableStateOf(preferences.themeMode) }
            val darkTheme = themeMode.isDark(systemDark = isSystemInDarkTheme())

            // İçeriği durum ve navigasyon çubuklarının altına kadar yayar. Çubuklardaki saat ve
            // pil simgelerinin rengi telefonun değil uygulamanın temasına uysun diye tema her
            // değiştiğinde yeniden çağrılır; yoksa koyu zeminde koyu simgeler kaybolur.
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            HavaTheme(darkTheme = darkTheme) {
                HavaNavHost(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        preferences.themeMode = it
                    },
                    language = preferences.language,
                    onLanguageChange = {
                        preferences.language = it
                        // Dil, Activity kurulurken bağlama işlenir (attachBaseContext). Değişince
                        // Activity'yi yeniden kurarız; ekran dönmesi gibidir, ViewModel'ler ve
                        // rememberSaveable değerleri korunur.
                        recreate()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    private companion object {
        /** Logcat'te filtrelemek için: arama kutusuna `Lifecycle` yaz. */
        const val TAG = "Lifecycle"
    }
}
