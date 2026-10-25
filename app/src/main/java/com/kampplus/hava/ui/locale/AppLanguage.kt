package com.kampplus.hava.ui.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Uygulamanın dili: telefonu izle, Türkçe ya da İngilizce.
 *
 * Dil adları ("Türkçe", "English") çevrilmez; kullanıcı kendi dilini, o an hangi dil
 * seçili olursa olsun tanıyabilmeli.
 *
 * @param tag Dil kodu ("tr", "en"). [SYSTEM] için null: telefonun dili kullanılır.
 * @param nativeName Menüde gösterilen ad. [SYSTEM] için null; ekran "Sistem dili" yazar.
 */
enum class AppLanguage(val tag: String?, val nativeName: String?, val emoji: String) {
    SYSTEM(null, null, "🌐"),
    TURKISH("tr", "Türkçe", "🇹🇷"),
    ENGLISH("en", "English", "🇬🇧");

    /**
     * Activity'nin bağlamını ([Context]) bu dile çevirir.
     *
     * Android metni `res/values-xx/strings.xml` dosyalarından, bağlamın yapılandırmasındaki
     * dile göre seçer. Yapılandırmayı değiştirilmiş bir bağlam döndürürsek bütün
     * `stringResource` çağrıları kendiliğinden o dilde okunur.
     *
     * `Locale.setDefault`: tarih biçimleri ("Pazartesi" / "Monday") ve şehir aramasının
     * dili de aynı dili izlesin diye.
     */
    fun wrap(base: Context): Context {
        val locale = tag?.let(Locale::forLanguageTag) ?: Resources.getSystem().configuration.locales[0]
        Locale.setDefault(locale)
        if (tag == null) return base
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
