package com.greenbotz.greengate.ui.screens.auth

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

internal data class LoginGlassBackdrop(
    val image: ImageBitmap,
    val size: IntSize,
    val origin: Offset
)

internal val LocalLoginGlassBackdrop = staticCompositionLocalOf<LoginGlassBackdrop> {
    error("LoginGlassPanel requires LuxuryLoginBackdrop")
}

/** Sample the same center-cropped image as the screen, blurring only the backdrop. */
@Composable
internal fun LoginGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalLoginPalette.current
    val (backdrop, backdropSize, backdropOrigin) = LocalLoginGlassBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .onGloballyPositioned { position = it.positionInRoot() - backdropOrigin }
            .clip(shape)
    ) {
        Canvas(
            Modifier.matchParentSize().graphicsLayer {
                // The text and controls are siblings, so they are never blurred.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect.createBlurEffect(
                        22.dp.toPx(), 22.dp.toPx(), Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
            }
        ) {
            if (backdropSize.width > 0 && backdropSize.height > 0) {
                val scale = max(
                    backdropSize.width.toFloat() / backdrop.width,
                    backdropSize.height.toFloat() / backdrop.height
                )
                val width = (backdrop.width * scale).roundToInt()
                val height = (backdrop.height * scale).roundToInt()
                drawImage(
                    image = backdrop,
                    dstOffset = IntOffset(
                        ((backdropSize.width - width) / 2f - position.x).roundToInt(),
                        ((backdropSize.height - height) / 2f - position.y).roundToInt()
                    ),
                    dstSize = IntSize(width, height),
                    filterQuality = FilterQuality.Medium
                )
            }
        }
        Box(
            Modifier.matchParentSize()
                .background(
                    Brush.verticalGradient(
                        if (palette.light) listOf(Color(0xABFFFFFF), Color(0xD6FFFFFF), Color(0xF0FFFFFF))
                        else listOf(Color(0xCC102339), Color(0xE8031B30), Color(0xF005182A))
                    )
                )
                .drawWithCache {
                    val warmReflection = Brush.radialGradient(
                        listOf(Color(if (palette.light) 0x24F0CD96 else 0x19DDB07F), Color.Transparent),
                        center = Offset(size.width * .09f, 0f),
                        radius = size.width * .72f
                    )
                    val coolReflection = Brush.radialGradient(
                        listOf(Color(if (palette.light) 0x66FFFFFF else 0x12CBDEEF), Color.Transparent),
                        center = Offset(size.width * .94f, size.height * .03f),
                        radius = size.width * .8f
                    )
                    onDrawBehind {
                        drawRect(warmReflection)
                        drawRect(coolReflection)
                    }
                }
                .border(
                    .6.dp,
                    if (palette.light) Brush.linearGradient(
                        listOf(Color.White, Color(0xA3E5C18E), Color(0xFFF4D7AA))
                    ) else Brush.linearGradient(
                        0f to Color(0x9CDDB07F),
                        .38f to Color(0x8EA7C2DD),
                        .68f to Color(0x366E8AAB),
                        1f to Color(0xC0D4A16C)
                    ),
                    shape
                )
        )
        content()
    }
}

/** The halo is a separate blurred layer so the control text stays crisp. */
@Composable
internal fun LoginSurfaceGlow(modifier: Modifier = Modifier, strength: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Canvas(
            modifier.graphicsLayer {
                renderEffect = RenderEffect.createBlurEffect(
                    7.dp.toPx(), 7.dp.toPx(), Shader.TileMode.DECAL
                ).asComposeRenderEffect()
            }
        ) {
            drawRoundRect(
                Color(0xFFF4D19B).copy(alpha = strength),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(23.dp.toPx())
            )
        }
    }
}

@Composable
internal fun Modifier.loginGlassControl(focused: Boolean = false): Modifier {
    val palette = LocalLoginPalette.current
    return this
    .clip(RoundedCornerShape(22.dp))
    .background(
        Brush.linearGradient(
            if (palette.light) listOf(Color(0xB8FFFFFF), Color(0x70FFFFFF), Color(0xABF8F8F8))
            else listOf(Color(0x243D536C), Color(0x161F3C57), Color(0x300A1E34))
        )
    )
    .border(
        if (focused) 1.dp else .65.dp,
        Brush.verticalGradient(
            if (palette.light && focused) listOf(Color(0xFFD5AA6A), Color(0xFFC3A273), Color(0xFFD5AA6A))
            else if (palette.light) listOf(Color(0xFFC9CBD0), Color(0xA8D1D2D5), Color(0xFFC5C7CB))
            else if (focused) listOf(Color(0xFFECD4AE), Color(0xB3A6C9E5), Color(0x89DDB07F))
            else listOf(Color(0xB08FA6C1), Color(0x54728BA6), Color(0x416F8197))
        ),
        RoundedCornerShape(22.dp)
    )
}

@Composable
internal fun Modifier.loginGoldButton(): Modifier {
    val light = LocalLoginPalette.current.light
    return this
    .clip(RoundedCornerShape(25.dp))
    .background(
        Brush.verticalGradient(
            if (light) listOf(Color(0xFFFFF0C9), Color(0xFFF8DDA8), Color(0xFFE9BE7A))
            else listOf(Color(0xFFFFE6B1), Color(0xFFF0CD96), Color(0xFFD0A779))
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color(0xFFFFF1CA), Color(0xFFDFB06D))),
        RoundedCornerShape(25.dp)
    )
}
