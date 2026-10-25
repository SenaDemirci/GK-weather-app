package com.kampplus.hava.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Açık tema paleti. Her Material rolü ([Color.kt]'deki) bir renge bağlanır:
 * `primary` ana vurgu, `surface` zemin, `onSurface` o zemindeki yazı rengidir.
 *
 * `surfaceContainer*` rolleri kart zeminleridir (Lowest en açık, Highest en koyu).
 * Bunlar tanımlanmazsa Material varsayılan mor tonlarını kullanır; kartlar
 * paletten kopuk görünür.
 */
private val LightColors = lightColorScheme(
    primary = Blue40,
    onPrimary = Neutral100,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Blue40,
    onSecondary = Neutral100,
    secondaryContainer = Blue90,
    onSecondaryContainer = Blue10,
    tertiary = Amber70,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral95,
    surfaceContainer = Neutral92,
    surfaceContainerHigh = Neutral90,
    surfaceContainerHighest = Neutral87,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    error = Red40,
    errorContainer = Red90,
    onErrorContainer = Red10
)

/** Koyu tema paleti. Aynı roller, koyu zemine uygun tonlarla. */
private val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = Blue80,
    onSecondary = Blue20,
    secondaryContainer = Blue30,
    onSecondaryContainer = Blue90,
    tertiary = Amber70,
    background = Neutral6,
    onBackground = Neutral87,
    surface = Neutral6,
    onSurface = Neutral87,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    error = Red80,
    errorContainer = Red30,
    onErrorContainer = Red90
)

/**
 * Uygulamanın teması. Bütün ekranlar bunun içine sarmalanır.
 *
 * @param darkTheme Varsayılan olarak telefonun sistem ayarını izler
 *   ([isSystemInDarkTheme]). @Preview'larda elle `true` verip koyu temayı deneyebilirsin.
 */
@Composable
fun HavaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HavaTypography,
        shapes = HavaShapes,
        content = content
    )
}
