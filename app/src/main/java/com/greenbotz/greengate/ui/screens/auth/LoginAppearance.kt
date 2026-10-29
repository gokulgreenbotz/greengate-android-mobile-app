package com.greenbotz.greengate.ui.screens.auth

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.LoginExperience

/** The login theme is independent of the signed-in app's home-screen theme. */
internal data class LoginPalette(
    val light: Boolean,
    @param:DrawableRes val artwork: Int,
    val background: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val selectedText: Color,
    val border: Color,
    val panel: Color
)

private val DarkLoginPalette = LoginPalette(
    light = false, artwork = R.drawable.login_bg_luxury,
    background = Color(0xFF061A2E), text = Color(0xFFFFF8E8),
    muted = Color(0xFFB7C3D0), accent = Color(0xFFE7C26A),
    selectedText = Color(0xFFFFDFA1), border = Color(0xFF6C87A5), panel = Color(0xE80A2036)
)

private val WhiteLoginPalette = LoginPalette(
    light = true, artwork = R.drawable.login_bg_luxury_white,
    background = Color(0xFFFFFCF7), text = Color(0xFF111827),
    muted = Color(0xFF626773), accent = Color(0xFF98611E),
    selectedText = Color(0xFF805015), border = Color(0xFFBDC0C5), panel = Color(0xD9FFFFFF)
)

internal val LocalLoginPalette = staticCompositionLocalOf { DarkLoginPalette }

@Composable
internal fun LoginAppearance(experience: LoginExperience, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLoginPalette provides if (experience == LoginExperience.WHITE) WhiteLoginPalette else DarkLoginPalette,
        content = content
    )
}

internal val Panel: Color @Composable get() = LocalLoginPalette.current.panel
internal val Gold: Color @Composable get() = LocalLoginPalette.current.accent
internal val GoldLight: Color @Composable get() = LocalLoginPalette.current.selectedText
internal val Ivory: Color @Composable get() = LocalLoginPalette.current.text
internal val Muted: Color @Composable get() = LocalLoginPalette.current.muted
internal val Stroke: Color @Composable get() = LocalLoginPalette.current.border
