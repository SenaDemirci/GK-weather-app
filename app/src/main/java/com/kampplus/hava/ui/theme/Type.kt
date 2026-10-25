package com.kampplus.hava.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Yazı tipi ölçeği. Tema dört dosyadan oluşur: Color, Theme, Type, Shape.
 *
 * Material 3'ün hazır ölçeğinden başlıyoruz ama yazıları okunaklı olsun diye büyütüp
 * kalınlaştırıyoruz: gövde metni 16 yerine 18sp ve orta kalınlıkta, başlıklar kalın.
 * `displayLarge` tahmin ekranındaki büyük sıcaklıktır: iri ama ince bir yazı,
 * sayıyı bağırtmadan öne çıkarır.
 *
 * Bileşenler boyutu elle yazmaz; `MaterialTheme.typography.titleMedium` gibi
 * **rol** isimleriyle okur. Böylece tipografi tek yerden değişir.
 */
private val base = Typography()

internal val HavaTypography = base.copy(
    displayLarge = base.displayLarge.copy(
        fontSize = 96.sp,
        lineHeight = 100.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-2).sp
    ),
    // Şehir ekranının "Hava nasıl?" başlığı.
    displaySmall = base.displaySmall.copy(
        fontSize = 38.sp,
        lineHeight = 46.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = base.headlineLarge.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodySmall = base.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
)
