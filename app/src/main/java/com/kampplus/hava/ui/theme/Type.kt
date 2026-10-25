package com.kampplus.hava.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Yazı tipi ölçeği. Tema dört dosyadan oluşur: Color, Theme, Type, Shape.
 *
 * Material 3'ün hazır ölçeğinden başlayıp başlıkları kalınlaştırıyoruz.
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
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold)
)
