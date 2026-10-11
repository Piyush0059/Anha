package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = CosmicDarkBg,
    onBackground = DarkTextPrimary,
    surface = CosmicCardBg,
    onSurface = DarkTextPrimary,
    surfaceVariant = CosmicCardBorder,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = SoftBlueAccent,
    onPrimary = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = SlateLightBg,
    onBackground = LightTextPrimary,
    surface = SlateLightCard,
    onSurface = LightTextPrimary,
    surfaceVariant = SlateLightBorder,
    onSurfaceVariant = LightTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark for premium AI look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
