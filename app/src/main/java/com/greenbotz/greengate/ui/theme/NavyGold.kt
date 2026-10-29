package com.greenbotz.greengate.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.AppTheme

// Theme 4 - Navy & Gold, as its screens outside Home use it (visitor invites, facility booking):
// navy type and buttons, gold accents, white cards on a warm white page.
// AppPreferences.theme is snapshot state, so composables reading this recompose on a theme change.
internal val NavyGoldTheme: Boolean get() = AppPreferences.theme == AppTheme.FOUR

internal val NavyGoldInk = Color(0xFF0B1446)
internal val NavyGoldMuted = Color(0xFF6B7396)
internal val NavyGoldNavy = Color(0xFF0E2E66)
internal val NavyGoldGold = Color(0xFFB9852F)
internal val NavyGoldPage = Color(0xFFFCFBF8)
internal val NavyGoldLine = Color(0xFFEDE8E0)
internal val NavyGoldShadow = Color(0x260B1446)
/** A pale gold-cream for selected chips and soft gold buttons. */
internal val NavyGoldCream = Color(0xFFFBF3E3)
internal val NavyGoldButton = Brush.verticalGradient(listOf(Color(0xFF1F437B), Color(0xFF0A2253)))
