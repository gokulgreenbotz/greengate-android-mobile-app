package com.example.greengate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = SecondaryGreen,
    tertiary = AccentPeach,
    background = Color.White,
    surface = CardWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFFCF9F3),
    onSurfaceVariant = Color(0xFF687390),
)

private val GlassColorScheme = lightColorScheme(
    primary = Color(0xFF00685C), secondary = Color(0xFF567D92),
    background = Color(0xFFE0F3EF), surface = Color(0xCFFFFFFF),
    surfaceVariant = Color(0xBBF3FFFC), onPrimary = Color.White,
    onBackground = Color(0xFF0D1728), onSurface = Color(0xFF0D1728),
    onSurfaceVariant = Color(0xFF527C94)
)
@Composable
fun GreenGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (com.example.greengate.AppPreferences.theme == com.example.greengate.AppTheme.ZERO) GlassColorScheme else LightColorScheme,
        typography = Typography, content = content
    )
}
