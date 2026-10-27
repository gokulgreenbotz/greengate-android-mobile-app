package com.greenbotz.greengate.ui.screens.auth

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.greenbotz.greengate.R

private val ChampagneGlow = Color(0xFFDDB07F)
private val ChampagneHighlight = Color(0xFFF4D19B)

@Composable
fun GreenGateLoginBranding(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        GlowingLoginAsset(
            painter = painterResource(R.drawable.greengate_brand_lockup),
            contentDescription = "GreenGate Condo",
            modifier = Modifier.width(290.dp).height(194.dp),
            outerGlowAlpha = .30f,
            innerGlowAlpha = .20f
        )
        Spacer(Modifier.height(14.dp))
        GlowingLoginAsset(
            painter = painterResource(R.drawable.greengate_tagline),
            contentDescription = "A smarter way of living",
            modifier = Modifier.width(245.dp).height(82.dp),
            glowColor = ChampagneHighlight,
            outerGlowAlpha = .12f,
            innerGlowAlpha = .08f
        )
        Spacer(Modifier.height(20.dp))
        GlowingProgressAsset(Modifier.width(108.dp).height(7.dp))
    }
}

@Composable
private fun GlowingLoginAsset(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier,
    glowColor: Color = ChampagneGlow,
    outerGlowAlpha: Float,
    innerGlowAlpha: Float
) {
    Box(modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.matchParentSize().graphicsLayer {
                    alpha = outerGlowAlpha
                    renderEffect = RenderEffect.createBlurEffect(26f, 26f, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }.scale(1.025f),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(glowColor)
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.matchParentSize().graphicsLayer {
                    alpha = innerGlowAlpha
                    renderEffect = RenderEffect.createBlurEffect(11f, 11f, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }.scale(1.01f),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(ChampagneHighlight)
            )
        } else {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.matchParentSize().scale(1.035f).alpha(outerGlowAlpha * .55f),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(glowColor)
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.matchParentSize().scale(1.015f).alpha(innerGlowAlpha * .65f),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(ChampagneHighlight)
            )
        }
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun GlowingProgressAsset(modifier: Modifier) {
    Box(modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Image(
                painter = painterResource(R.drawable.greengate_progress_bar),
                contentDescription = null,
                modifier = Modifier.matchParentSize().graphicsLayer {
                    alpha = .55f
                    renderEffect = RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.CLAMP).asComposeRenderEffect()
                },
                contentScale = ContentScale.FillBounds
            )
        }
        Image(
            painter = painterResource(R.drawable.greengate_progress_bar),
            contentDescription = "Loading progress",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}
