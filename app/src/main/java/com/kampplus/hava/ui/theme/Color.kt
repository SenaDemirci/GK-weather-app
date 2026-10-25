package com.kampplus.hava.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Uygulamanın renk paleti. MaterialTheme sayesinde renkler tek yerden yönetilir.
 *
 * Renkler burada **isimlendirilir**, [Theme] dosyasında açık/koyu temaya dağıtılır.
 * Bileşenler hiçbir zaman buradaki sabitleri doğrudan kullanmaz; `MaterialTheme.colorScheme`
 * üzerinden okur. Böylece tek satır değişince bütün uygulama yeni kıyafetini giyer.
 *
 * İsimlerdeki sayı tonu belirtir: 0 siyah, 100 beyaz; 10 en koyu, 99 en açık.
 */

// Ana renk: gökyüzü mavisi. İkincil roller de (kıyafet kartı, seçili filtre çipi,
// "Yarın" etiketi) aynı tonları kullanır; uygulama tek renkli, sakin bir görünümde kalır.
internal val Blue10 = Color(0xFF001B3E)
internal val Blue20 = Color(0xFF003063)
internal val Blue30 = Color(0xFF00468C)
internal val Blue40 = Color(0xFF1B63C9)
internal val Blue80 = Color(0xFFA9C7FF)
internal val Blue90 = Color(0xFFD6E3FF)

// Güneş sarısı: yalnızca sıcaklık çubuğunun sıcak ucu.
internal val Amber70 = Color(0xFFF59E0B)

// Zemin ve yazı tonları. Hafif mavimsi gri; saf griden daha sıcak durur.
internal val Neutral4 = Color(0xFF0C0E13)
internal val Neutral6 = Color(0xFF111318)
internal val Neutral10 = Color(0xFF191C20)
internal val Neutral12 = Color(0xFF1D2024)
internal val Neutral17 = Color(0xFF282A2F)
internal val Neutral22 = Color(0xFF33353A)
internal val Neutral87 = Color(0xFFE1E2E8)
internal val Neutral90 = Color(0xFFE6E8EE)
internal val Neutral92 = Color(0xFFECEEF4)
internal val Neutral95 = Color(0xFFF2F3FA)
internal val Neutral98 = Color(0xFFF8F9FF)
internal val Neutral100 = Color(0xFFFFFFFF)
internal val NeutralVariant30 = Color(0xFF43474E)
internal val NeutralVariant50 = Color(0xFF74777F)
internal val NeutralVariant60 = Color(0xFF8E9099)
internal val NeutralVariant80 = Color(0xFFC4C6D0)
internal val NeutralVariant90 = Color(0xFFE0E2EC)

internal val Red10 = Color(0xFF410002)
internal val Red30 = Color(0xFF93000A)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)
internal val Red90 = Color(0xFFFFDAD6)

// Tahmin ekranındaki büyük kartın gökyüzü renkleri: üst ton → alt ton.
// Üzerine beyaz yazı geleceği için hepsi yeterince koyu seçildi.
internal val SkyClearTop = Color(0xFF3A9BE0)
internal val SkyClearBottom = Color(0xFF1F5FC4)
internal val SkyCloudyTop = Color(0xFF6A9BDA)
internal val SkyCloudyBottom = Color(0xFF3E6DB5)
internal val SkyOvercastTop = Color(0xFF8391A3)
internal val SkyOvercastBottom = Color(0xFF515F70)
internal val SkyRainTop = Color(0xFF5675A3)
internal val SkyRainBottom = Color(0xFF2C3E66)
internal val SkySnowTop = Color(0xFF8FA9C8)
internal val SkySnowBottom = Color(0xFF5C7EA6)
internal val SkyStormTop = Color(0xFF5A4C7A)
internal val SkyStormBottom = Color(0xFF2B2440)

// UV seviyeleri: Dünya Sağlık Örgütü'nün renk ölçeği (yeşil → mor).
internal val UvGreen = Color(0xFF3BA55C)
internal val UvYellow = Color(0xFFF2C12E)
internal val UvOrange = Color(0xFFF08C2E)
internal val UvRed = Color(0xFFE5484D)
internal val UvPurple = Color(0xFF8E4EC6)
