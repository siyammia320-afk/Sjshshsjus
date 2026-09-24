package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = InstagramBlue,
    onPrimary = Color.White,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFD1D5DB),
    background = InstagramDark,
    onBackground = TextPrimaryDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = InstagramBlue,
    onPrimary = Color.White,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF4B5563),
    background = Color(0xFFF9FAFB),
    onBackground = TextPrimaryLight,
    outline = OutlineLight
)

@Composable
fun InstagramAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
