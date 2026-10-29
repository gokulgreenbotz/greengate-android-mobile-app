package com.greenbotz.greengate.ui.screens.assistant

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Turn = (2 * PI).toFloat()
private val Degrees = (180 / PI).toFloat()

private val OrbPearl = Color(0xFFF4F8FF)
private val OrbSky = Color(0xFFA9C8FA)
private val OrbBlue = Color(0xFF5D8EEB)
private val OrbDeep = Color(0xFF2C5FD0)
private val OrbCyan = Color(0xFF8FD6FF)
private val OrbHalo = Color(0xFF6F9BFF)
private val OrbGold = Color(0xFFE8C68A)

/**
 * Theme 4's assistant orb: a round glass sphere with liquid colour turning inside and
 * light rings wobbling at its rim. The rings follow the microphone while listening,
 * breathe while speaking and spin faster while thinking.
 */
@Composable
internal fun ConciergeOrb(
    modifier: Modifier = Modifier,
    listening: Boolean = false,
    speaking: Boolean = false,
    level: Float = 0f,
    processing: Boolean = false
) {
    // Animated values are read in the draw phase, so frames redraw without recomposing.
    val transition = rememberInfiniteTransition(label = "Concierge orb")
    val swirl by transition.animateFloat(0f, Turn, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "Liquid swirl")
    val wobble by transition.animateFloat(0f, Turn, infiniteRepeatable(tween(4600, easing = LinearEasing)), label = "Rim wobble")
    val pulse by transition.animateFloat(0f, Turn, infiniteRepeatable(tween(1700, easing = LinearEasing)), label = "Pulse")
    val listeningAmount by animateFloatAsState(if (listening) 1f else 0f, tween(400), label = "Listening")
    val speakingAmount by animateFloatAsState(if (speaking) 1f else 0f, tween(400), label = "Speaking")
    val processingAmount by animateFloatAsState(if (processing) 1f else 0f, tween(400), label = "Thinking")
    val voiceLevel by animateFloatAsState(
        if (level.isFinite()) level.coerceIn(0f, 1f) else 0f, tween(120), label = "Microphone energy"
    )

    Canvas(modifier.clearAndSetSemantics { }) {
        val unit = size.minDimension
        if (unit <= 0f) return@Canvas
        val c = center
        val beat = .5f + .5f * sin(pulse)
        val energy = listeningAmount * (.25f + .75f * voiceLevel) + speakingAmount * (.3f + .4f * beat) + processingAmount * .3f
        val r = unit * .34f * (1f + .03f * energy + .01f * sin(wobble))

        // Halo.
        drawCircle(
            Brush.radialGradient(
                0f to OrbHalo.copy(alpha = .46f + .2f * energy), .62f to OrbHalo.copy(alpha = .16f + .08f * energy),
                1f to OrbHalo.copy(alpha = 0f), center = c, radius = unit * .5f
            ),
            radius = unit * .5f, center = c
        )

        // Glass body, lit from the top left and kept light so the liquid shows through.
        drawCircle(
            Brush.radialGradient(
                0f to OrbPearl, .45f to OrbSky, .85f to OrbBlue, 1f to OrbBlue,
                center = c + Offset(-r * .3f, -r * .4f), radius = r * 1.7f
            ),
            radius = r, center = c
        )

        clipPath(Path().apply { addOval(Rect(c, r)) }) {
            // Liquid: soft pools of colour circling inside at different speeds.
            fun pool(color: Color, angle: Float, distance: Float, spread: Float) {
                val at = c + Offset(cos(angle), sin(angle)) * (r * distance)
                drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = at, radius = r * spread),
                    radius = r * spread, center = at)
            }
            pool(OrbDeep.copy(alpha = .9f), swirl + 2.2f, .58f, .95f)
            pool(OrbDeep.copy(alpha = .55f), .8f * swirl + 5.1f, .62f, .7f)
            pool(Color.White.copy(alpha = .7f), -1.4f * swirl, .45f, .75f)
            pool(OrbCyan.copy(alpha = .45f + .2f * energy), .7f * swirl + 4f, .38f, .7f)

            // The pearl-and-gold ribbon from the design, turning slowly through the middle.
            // A wide faint outline under the fill softens its edges.
            rotate(swirl * Degrees * .5f - 20f, pivot = c) {
                val ribbon = ribbonPath(c, r, sin(wobble))
                val sheen = Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0f), .25f to Color.White.copy(alpha = .8f),
                    .5f to OrbGold.copy(alpha = .85f), .75f to Color.White.copy(alpha = .75f), 1f to Color.White.copy(alpha = 0f),
                    start = c - Offset(r, 0f), end = c + Offset(r, 0f)
                )
                drawPath(ribbon, sheen, alpha = .3f, style = Stroke(r * .14f))
                drawPath(ribbon, sheen, alpha = .85f)
            }

            // Glass depth: the edge darkens toward the rim, with a thin bright line just inside it.
            drawCircle(
                Brush.radialGradient(
                    0f to OrbDeep.copy(alpha = 0f), .78f to OrbDeep.copy(alpha = 0f), 1f to OrbDeep.copy(alpha = .38f),
                    center = c, radius = r
                ),
                radius = r, center = c
            )
            drawCircle(Color.White.copy(alpha = .45f), radius = r * .965f, center = c, style = Stroke(r * .018f))

            // Specular highlight.
            val shine = c + Offset(-r * .38f, -r * .5f)
            rotate(-35f, pivot = shine) {
                scale(1f, .45f, pivot = shine) {
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = .85f), Color.White.copy(alpha = 0f)), center = shine, radius = r * .5f),
                        radius = r * .5f, center = shine
                    )
                }
            }
        }

        // Light rings at the rim, each wobbling and turning on its own beat.
        val spin = swirl + processingAmount * pulse
        repeat(3) { index ->
            val ring = wobblyRing(c, r * (1f + .012f * index), wobble * (1f + .35f * index) + index * 2.1f, .014f + .05f * energy + .006f * index)
            rotate((spin * (index + 1) * .6f + index * 1.7f) * Degrees, pivot = c) {
                drawPath(ring, OrbHalo.copy(alpha = .22f + .14f * energy), style = Stroke(r * .12f))
                drawPath(ring, Color.White.copy(alpha = .25f + .15f * energy), style = Stroke(r * .05f))
                drawPath(
                    ring,
                    Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = .95f), OrbSky.copy(alpha = .35f), Color.White.copy(alpha = .85f),
                            OrbCyan.copy(alpha = .3f), Color.White.copy(alpha = .95f)
                        ),
                        center = c
                    ),
                    style = Stroke(r * (.026f - .005f * index))
                )
            }
        }
    }
}

/** A band shaped like a lazy S, crossing the sphere from left to right. */
private fun ribbonPath(c: Offset, r: Float, wave: Float): Path = Path().apply {
    fun at(x: Float, y: Float) = c + Offset(x * r, y * r)
    val start = at(-1.15f, .12f + .05f * wave)
    moveTo(start.x, start.y)
    var a = at(-.4f, -.8f - .1f * wave); var b = at(.4f, .7f + .1f * wave); var end = at(1.15f, -.18f)
    cubicTo(a.x, a.y, b.x, b.y, end.x, end.y)
    end = at(1.15f, .08f)
    lineTo(end.x, end.y)
    a = at(.4f, 1f + .1f * wave); b = at(-.4f, -.5f - .1f * wave); end = at(-1.15f, .4f + .05f * wave)
    cubicTo(a.x, a.y, b.x, b.y, end.x, end.y)
    close()
}

/** A closed, smooth ring around [c] whose radius ripples by [amount] with a slowly moving [phase]. */
private fun wobblyRing(c: Offset, radius: Float, phase: Float, amount: Float): Path {
    val count = 48
    val points = List(count) { index ->
        val angle = index * Turn / count
        val ripple = 1f + amount * (.6f * sin(2f * angle + phase) + .4f * sin(3f * angle - 1.3f * phase))
        c + Offset(cos(angle), sin(angle)) * radius * ripple
    }
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        for (index in points.indices) {
            val previous = points[(index - 1 + count) % count]
            val current = points[index]
            val next = points[(index + 1) % count]
            val afterNext = points[(index + 2) % count]
            val first = current + (next - previous) / 6f
            val second = next - (afterNext - current) / 6f
            cubicTo(first.x, first.y, second.x, second.y, next.x, next.y)
        }
        close()
    }
}
