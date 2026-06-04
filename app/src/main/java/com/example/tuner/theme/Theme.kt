package com.example.tuner.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TunerDarkColorScheme = darkColorScheme(
    primary = ColorInTune,
    secondary = ColorInTune,
    tertiary = ColorSharp,
    background = DarkBackground,
    surface = CardBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary
)

@Composable
fun TunerTheme(
    content: @Composable () -> Unit
) {
    // For a premium tuning experience (e.g. on dark stages or rooms),
    // we use a dedicated dark theme for the entire app.
    MaterialTheme(
        colorScheme = TunerDarkColorScheme,
        typography = Typography,
        content = content
    )
}
