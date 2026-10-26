package com.kampplus.hava.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Bütün ekranların ortak arka planı: gökyüzü degradesi ve iki yumuşak ışık halesi.
 *
 * - Üst kısım açık mavi (koyu temada lacivert); aşağı indikçe zemin rengine karışır ama
 *   tam dönmez, hafif mavi kalır. Böylece beyaz kartlar ekranın her yerinde zeminden ayrışır.
 * - Sağ üstte sıcak sarı bir güneş parıltısı, sol altta mavi bir ışık.
 *   `drawBehind` içeriğin arkasına çizer; yazıları ve kartları etkilemez.
 *
 * Kullanımı: `Scaffold(modifier = Modifier.skyBackground(), containerColor = Color.Transparent)`.
 */
@Composable
fun Modifier.skyBackground(): Modifier {
    val sky = MaterialTheme.colorScheme.primaryContainer
    val ground = MaterialTheme.colorScheme.background
    val glow = MaterialTheme.colorScheme.primary
    return this
        .background(
            Brush.verticalGradient(
                0f to sky,
                0.3f to sky,
                1f to lerp(sky, ground, 0.7f)
            )
        )
        .drawBehind {
            val sunCenter = Offset(size.width * 0.92f, size.height * 0.04f)
            drawCircle(
                brush = Brush.radialGradient(listOf(SunGlowLight.copy(alpha = 0.55f), Color.Transparent), sunCenter, size.width * 0.5f),
                radius = size.width * 0.5f,
                center = sunCenter
            )
            val glowCenter = Offset(size.width * 0.05f, size.height * 0.82f)
            drawCircle(
                brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.16f), Color.Transparent), glowCenter, size.width * 0.6f),
                radius = size.width * 0.6f,
                center = glowCenter
            )
        }
}
