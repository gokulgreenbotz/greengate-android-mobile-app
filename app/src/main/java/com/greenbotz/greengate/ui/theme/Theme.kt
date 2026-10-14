package com.greenbotz.greengate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.AppTheme

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

// Theme 2 - Fresh: white cards on a pale mint page, deep forest green and orange accents.
private val FreshColorScheme = lightColorScheme(
    primary = Color(0xFF0F5A44), secondary = Color(0xFF3F7A63), tertiary = Color(0xFFF2762E),
    background = Color(0xFFF1F8F3), surface = Color.White, surfaceVariant = Color.White,
    onPrimary = Color.White, onSecondary = Color.White, onTertiary = Color.White,
    onBackground = Color(0xFF14271F), onSurface = Color(0xFF14271F),
    onSurfaceVariant = Color(0xFF6B7A73)
)

// Theme 3 - Meadow: white cards on an off-white page, deep forest green with lime highlights.
private val MeadowColorScheme = lightColorScheme(
    primary = Color(0xFF0E3B2C), secondary = Color(0xFF3F6B57), tertiary = Color(0xFFC6E04A),
    background = Color(0xFFF8F9F5), surface = Color.White, surfaceVariant = Color(0xFFF0F8D8),
    onPrimary = Color.White, onSecondary = Color.White, onTertiary = Color(0xFF0E3B2C),
    onBackground = Color(0xFF0E3B2C), onSurface = Color(0xFF0E3B2C),
    onSurfaceVariant = Color(0xFF6E7A72)
)

@Composable
fun GreenGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when (AppPreferences.theme) {
            AppTheme.ZERO -> GlassColorScheme
            AppTheme.ONE -> LightColorScheme
            AppTheme.TWO -> FreshColorScheme
            AppTheme.THREE -> MeadowColorScheme
        },
        typography = Typography, content = content
    )
}
