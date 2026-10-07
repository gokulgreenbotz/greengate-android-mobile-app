package com.example.greengate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A decorative, living glass orb, with microphone and speech activity reflected in its motion. */
@Composable
internal fun GreenBotOrb(
    modifier: Modifier = Modifier,
    listening: Boolean = false,
    speaking: Boolean = false,
    level: Float = 0f
) {
    val primary = MaterialTheme.colorScheme.primary
    val emerald = lerp(primary, Color(0xFF23B68B), .45f)
    val mint = lerp(primary, Color.White, .78f)
    val forest = lerp(primary, Color.Black, .62f)
    val depth = lerp(primary, Color.Black, .89f)
    val transition = rememberInfiniteTransition(label = "Assistant orb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(8600, easing = LinearEasing)),
        label = "Organic movement"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1450, easing = LinearEasing)),
        label = "Voice rhythm"
    )
    val activity by animateFloatAsState(
        targetValue = if (listening || speaking) 1f else 0f,
        animationSpec = tween(400), label = "Voice activity"
    )
    val voiceLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(160), label = "Microphone energy"
    )

    Canvas(modifier.clearAndSetSemantics { }) {
        val unit = size.minDimension
        if (unit <= 0f) return@Canvas
        val mid = Offset(size.width / 2f, size.height / 2f)
        val breath = sin(phase)
        val energy = activity * (.24f + voiceLevel * .58f + if (speaking) .18f * sin(pulse) else 0f)
        val center = mid + Offset(0f, unit * .012f * breath)
        val radius = unit * .33f

        drawCircle(
            Brush.radialGradient(
                colorStops = arrayOf(
                    0f to mint.copy(alpha = .15f),
                    .6f to emerald.copy(alpha = .065f + activity * .025f),
                    1f to emerald.copy(alpha = 0f)
                ),
                center = mid, radius = unit * .5f
            ),
            radius = unit * .5f, center = mid
        )

        scale(1f + .025f * breath + .045f * energy, pivot = center) {
            rotate(7f * sin(phase * 1f) + 3f * activity * sin(pulse), pivot = center) {
                val body = organicOrbPath(center, radius, phase, energy)
                drawPath(
                    body,
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to mint,
                            .28f to emerald,
                            .53f to primary,
                            .8f to forest,
                            1f to depth
                        ),
                        center = center + Offset(-radius * .55f, -radius * .7f),
                        radius = radius * 2.3f
                    )
                )

                clipPath(body) {
                    // Reflected light rolls around the edges of a deep, glassy central fold.
                    drawCircle(
                        Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = .9f),
                                .18f to mint.copy(alpha = .85f),
                                .5f to emerald.copy(alpha = .9f),
                                1f to emerald.copy(alpha = 0f)
                            ),
                            center = center + Offset(-radius * .92f, -radius * .24f),
                            radius = radius * .83f
                        ),
                        radius = radius * .83f,
                        center = center + Offset(-radius * .92f, -radius * .24f)
                    )
                    val rightLight = center + Offset(radius * .79f, radius * .2f)
                    drawCircle(
                        Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to mint,
                                .24f to lerp(mint, Color.White, .45f),
                                .48f to emerald,
                                1f to primary.copy(alpha = 0f)
                            ),
                            center = rightLight, radius = radius * 1.06f
                        ),
                        radius = radius * 1.06f, center = rightLight
                    )
                    val darkFold = center + Offset(-radius * .12f, -radius * .14f)
                    drawCircle(
                        Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to depth,
                                .46f to depth.copy(alpha = .99f),
                                .66f to forest.copy(alpha = .92f),
                                .83f to primary.copy(alpha = .35f),
                                1f to primary.copy(alpha = 0f)
                            ),
                            center = darkFold, radius = radius * 1.02f
                        ),
                        radius = radius * 1.02f, center = darkFold
                    )

                    val fold = Path().apply {
                        moveTo(center.x - radius * .72f, center.y + radius * .52f)
                        cubicTo(
                            center.x - radius * .9f, center.y - radius * .12f,
                            center.x - radius * .32f, center.y - radius * .97f,
                            center.x + radius * .02f, center.y - radius * .73f
                        )
                        cubicTo(
                            center.x + radius * .5f, center.y - radius * .47f,
                            center.x + radius * .34f, center.y - radius * .08f,
                            center.x + radius * .62f, center.y + radius * .09f
                        )
                        cubicTo(
                            center.x + radius * .97f, center.y + radius * .46f,
                            center.x + radius * .35f, center.y + radius * .95f,
                            center.x - radius * .16f, center.y + radius * .88f
                        )
                    }
                    drawPath(
                        fold,
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to mint.copy(alpha = .05f),
                                .36f to emerald.copy(alpha = .38f),
                                .58f to mint.copy(alpha = .9f),
                                .77f to mint.copy(alpha = .32f),
                                1f to Color.White.copy(alpha = .82f)
                            ),
                            start = center - Offset(radius, radius),
                            end = center + Offset(radius * .4f, radius)
                        ),
                        style = Stroke(radius * .15f, cap = StrokeCap.Round)
                    )

                    val reflection = center + Offset(radius * .48f, -radius * .12f)
                    rotate(-26f, reflection) {
                        scale(.38f, 1f, pivot = reflection) {
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(Color.White.copy(alpha = .94f), mint.copy(alpha = .6f), mint.copy(alpha = 0f)),
                                    center = reflection, radius = radius * .24f
                                ),
                                radius = radius * .24f, center = reflection
                            )
                        }
                    }
                    val lowerReflection = center + Offset(-radius * .12f, radius * .93f)
                    drawOval(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = .88f), mint.copy(alpha = .48f), mint.copy(alpha = 0f)),
                            center = lowerReflection, radius = radius * .57f
                        ),
                        topLeft = lowerReflection - Offset(radius * .63f, radius * .14f),
                        size = Size(radius * 1.26f, radius * .36f)
                    )
                    val upperReflection = center + Offset(-radius * .29f, -radius * .91f)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(mint.copy(alpha = .95f), emerald.copy(alpha = .48f), emerald.copy(alpha = 0f)),
                            center = upperReflection, radius = radius * .49f
                        ),
                        radius = radius * .49f, center = upperReflection
                    )
                }

                drawPath(
                    body,
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to mint.copy(alpha = .72f),
                            .4f to emerald.copy(alpha = .18f),
                            .72f to mint.copy(alpha = .38f),
                            1f to Color.White.copy(alpha = .8f)
                        ),
                        start = center - Offset(radius, radius),
                        end = center + Offset(radius, radius)
                    ),
                    style = Stroke(unit * .0045f)
                )
            }
        }
    }
}

private fun organicOrbPath(center: Offset, radius: Float, phase: Float, energy: Float): Path {
    val count = 72
    val points = List(count) { index ->
        val angle = index * (2 * PI).toFloat() / count
        val ripple = 1f + (.063f + .016f * energy) * sin(3f * angle + phase)
            + .043f * cos(5f * angle - phase)
            + .023f * sin(2f * angle - phase * 2f)
        center + Offset(cos(angle), sin(angle)) * radius * ripple
    }
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        for (index in points.indices) {
            val previous = points[(index - 1 + count) % count]
            val current = points[index]
            val next = points[(index + 1) % count]
            val afterNext = points[(index + 2) % count]
            val firstControl = current + (next - previous) / 6f
            val secondControl = next - (afterNext - current) / 6f
            cubicTo(firstControl.x, firstControl.y, secondControl.x, secondControl.y, next.x, next.y)
        }
        close()
    }
}
