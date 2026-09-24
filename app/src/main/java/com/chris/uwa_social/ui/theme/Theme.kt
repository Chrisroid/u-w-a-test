package com.chris.uwa_social.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = UwaEmeraldDark,
    secondary = UwaLeafGreenDark,
    tertiary = UwaGold,
    background = UwaCanvasDark,
    surface = UwaSurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = UwaGrey,
    outlineVariant = Color(0xFF333333)
)

private val LightColorScheme = lightColorScheme(
    primary = UwaEmerald,
    secondary = UwaLeafGreen,
    tertiary = UwaGold,
    background = UwaCanvas,
    surface = UwaSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = UwaDark,
    onSurface = UwaDark,
    outline = UwaGrey,
    outlineVariant = UwaBorder
)

@Composable
fun Uwa_socialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve official brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}