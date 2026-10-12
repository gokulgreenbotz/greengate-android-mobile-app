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

private val OrbTurn = (2 * PI).toFloat()
private val OrbDegrees = (180 / PI).toFloat()

/** Liquid glass with calm idle motion, voice ripples and a distinct processing orbit. */
@Composable
internal fun GreenBotOrb(
    modifier: Modifier = Modifier,
    listening: Boolean = false,
    speaking: Boolean = false,
    level: Float = 0f,
    processing: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val emerald = lerp(primary, Color(0xFF53D6AB), .46f)
    val mint = lerp(primary, Color.White, .82f)
    val seafoam = lerp(emerald, mint, .46f)
    val forest = lerp(primary, Color.Black, .28f)
    val pearl = lerp(mint, Color.White, .8f)

    // Compose's animation clock honors system duration scale, including disabled animations.
    // Animated values are read in the draw phase so frames do not recompose the chat.
    val transition = rememberInfiniteTransition(label = "Liquid assistant")
    val flow by transition.animateFloat(
        0f, OrbTurn, infiniteRepeatable(tween(9200, easing = LinearEasing)), label = "Liquid flow"
    )
    val drift by transition.animateFloat(
        0f, OrbTurn, infiniteRepeatable(tween(14200, easing = LinearEasing)), label = "Glass reflections"
    )
    val rhythm by transition.animateFloat(
        0f, OrbTurn, infiniteRepeatable(tween(1900, easing = LinearEasing)), label = "Voice and orbit rhythm"
    )
    val listeningAmount by animateFloatAsState(
        if (listening) 1f else 0f, tween(450), label = "Listening transition"
    )
    val speakingAmount by animateFloatAsState(
        if (speaking) 1f else 0f, tween(450), label = "Speaking transition"
    )
    val processingAmount by animateFloatAsState(
        if (processing) 1f else 0f, tween(450), label = "Processing transition"
    )
    val voiceLevel by animateFloatAsState(
        if (level.isFinite()) level.coerceIn(0f, 1f) else 0f,
        tween(140), label = "Microphone energy"
    )

    Canvas(modifier.clearAndSetSemantics { }) {
        val unit = size.minDimension
        if (unit <= 0f) return@Canvas
        val mid = Offset(size.width / 2f, size.height / 2f)
        val breath = sin(drift)
        val speechBeat = (.5f + .5f * sin(rhythm)) * (.8f + .2f * sin(2f * rhythm))
        val energy = listeningAmount * (.14f + .7f * voiceLevel) + speakingAmount * (.24f + .34f * speechBeat)
        val center = mid + Offset(unit * .004f * sin(flow), -unit * .008f + unit * .009f * breath)
        val radius = unit * .345f * (1f + .014f * breath + .028f * energy)

        drawCircle(
            Brush.radialGradient(
                colorStops = arrayOf(
                    0f to mint.copy(alpha = .24f),
                    .53f to emerald.copy(alpha = .09f + .045f * energy),
                    1f to emerald.copy(alpha = 0f)
                ),
                center = mid, radius = unit * .495f
            ),
            radius = unit * .495f, center = mid
        )
        val shadowCenter = mid + Offset(0f, unit * .39f)
        scale(1f, .15f, pivot = shadowCenter) {
            drawCircle(
                Brush.radialGradient(
                    listOf(primary.copy(alpha = .11f - .015f * breath), primary.copy(alpha = 0f)),
                    center = shadowCenter, radius = unit * .26f
                ),
                radius = unit * .26f, center = shadowCenter
            )
        }

        // Two soft outward waves respond to real microphone energy rather than flashing.
        repeat(2) { index ->
            val progress = (rhythm / OrbTurn + index * .5f) % 1f
            val envelope = sin(progress * OrbTurn * .5f)
            val fade = envelope * envelope * (1f - .4f * progress)
            drawCircle(
                emerald.copy(alpha = listeningAmount * fade * (.09f + .2f * voiceLevel)),
                radius = unit * (.375f + .105f * progress), center = mid,
                style = Stroke(unit * (.0035f + .003f * voiceLevel))
            )
        }

        val body = organicOrbPath(center, radius, flow, drift, energy)
        drawPath(
            body,
            Brush.radialGradient(
                colorStops = arrayOf(
                    0f to pearl,
                    .24f to mint,
                    .48f to seafoam,
                    .76f to emerald,
                    1f to forest
                ),
                center = center + Offset(-radius * .5f, -radius * .62f), radius = radius * 2.15f
            )
        )

        clipPath(body) {
            // A translucent emerald lower hemisphere gives depth without a hollow black core.
            val lowerLight = center + Offset(radius * .44f, radius * .68f)
            drawCircle(
                Brush.radialGradient(
                    listOf(forest.copy(alpha = .5f), emerald.copy(alpha = .16f), emerald.copy(alpha = 0f)),
                    center = lowerLight, radius = radius * 1.3f
                ),
                radius = radius * 1.3f, center = lowerLight
            )
            val innerLight = center + Offset(radius * .34f * cos(drift), radius * .3f * sin(drift))
            drawCircle(
                Brush.radialGradient(
                    listOf(mint.copy(alpha = .48f + .12f * speechBeat * speakingAmount), seafoam.copy(alpha = .2f), seafoam.copy(alpha = 0f)),
                    center = innerLight, radius = radius * .9f
                ),
                radius = radius * .9f, center = innerLight
            )

            // Counter-flowing, curved ribbons roll through the volume, not just around its edge.
            rotate(flow * OrbDegrees + 24f, pivot = center) {
                drawPath(
                    liquidRibbonPath(center, radius, sin(drift)),
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to mint.copy(alpha = .04f),
                            .24f to seafoam.copy(alpha = .38f),
                            .51f to pearl.copy(alpha = .76f),
                            .72f to mint.copy(alpha = .36f),
                            1f to emerald.copy(alpha = 0f)
                        ),
                        start = center - Offset(radius, radius), end = center + Offset(radius, radius * .8f)
                    )
                )
            }
            rotate(-flow * OrbDegrees + 76f, pivot = center) {
                scale(.91f, 1.08f, pivot = center) {
                    drawPath(
                        liquidRibbonPath(center, radius, cos(drift)),
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to emerald.copy(alpha = 0f),
                                .3f to emerald.copy(alpha = .28f),
                                .58f to mint.copy(alpha = .6f),
                                .78f to pearl.copy(alpha = .42f),
                                1f to mint.copy(alpha = 0f)
                            ),
                            start = center - Offset(radius, radius * .8f), end = center + Offset(radius, radius)
                        )
                    )
                }
            }

            val reflection = center + Offset(-radius * (.32f + .07f * sin(flow)), -radius * .57f)
            rotate(-32f + 9f * sin(drift), pivot = reflection) {
                scale(1f, .22f, pivot = reflection) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(pearl.copy(alpha = .92f), mint.copy(alpha = .3f), mint.copy(alpha = 0f)),
                            center = reflection, radius = radius * .63f
                        ),
                        radius = radius * .63f, center = reflection
                    )
                }
            }

            val caustic = Path().apply {
                moveTo(center.x - radius * .67f, center.y + radius * .56f)
                cubicTo(
                    center.x - radius * .25f, center.y + radius * (1.02f + .04f * sin(flow)),
                    center.x + radius * .43f, center.y + radius * .91f,
                    center.x + radius * .78f, center.y + radius * .43f
                )
            }
            val causticBrush = Brush.linearGradient(
                listOf(mint.copy(alpha = 0f), mint.copy(alpha = .86f), pearl.copy(alpha = .7f), mint.copy(alpha = 0f)),
                center + Offset(-radius * .7f, radius * .7f), center + Offset(radius * .8f, radius * .6f)
            )
            drawPath(caustic, causticBrush, alpha = .28f, style = Stroke(radius * .11f, cap = StrokeCap.Round))
            drawPath(caustic, causticBrush, style = Stroke(radius * .025f, cap = StrokeCap.Round))

            // A wandering specular glint makes the surface read as glass at compact sizes too.
            val glint = center + Offset(radius * .76f * cos(-flow), radius * .76f * sin(-flow))
            drawCircle(
                Brush.radialGradient(
                    listOf(pearl.copy(alpha = .82f), mint.copy(alpha = .26f), mint.copy(alpha = 0f)),
                    center = glint, radius = radius * .23f
                ),
                radius = radius * .23f, center = glint
            )
        }

        drawPath(
            body,
            Brush.linearGradient(
                colorStops = arrayOf(
                    0f to pearl.copy(alpha = .85f),
                    .34f to mint.copy(alpha = .28f),
                    .65f to emerald.copy(alpha = .12f),
                    1f to mint.copy(alpha = .8f)
                ),
                start = center - Offset(radius, radius), end = center + Offset(radius, radius)
            ),
            style = Stroke(unit * .0055f)
        )

        // One quiet orbit replaces particles; its brighter, faster trail signals processing.
        val orbitRadius = unit * .425f
        val orbitStart = flow * OrbDegrees - 100f
        val orbitBounds = mid - Offset(orbitRadius, orbitRadius)
        val orbitSize = Size(orbitRadius * 2f, orbitRadius * 2f)
        drawArc(
            emerald.copy(alpha = .12f * (1f - processingAmount)),
            startAngle = orbitStart, sweepAngle = 56f, useCenter = false,
            topLeft = orbitBounds, size = orbitSize, style = Stroke(unit * .003f, cap = StrokeCap.Round)
        )
        if (processingAmount > 0f) {
            val start = rhythm * OrbDegrees - 110f
            drawArc(
                primary.copy(alpha = .25f * processingAmount),
                startAngle = start, sweepAngle = 108f, useCenter = false,
                topLeft = orbitBounds, size = orbitSize,
                style = Stroke(unit * .006f, cap = StrokeCap.Round)
            )
            drawArc(
                emerald.copy(alpha = .7f * processingAmount),
                startAngle = start + 66f, sweepAngle = 42f, useCenter = false,
                topLeft = orbitBounds, size = orbitSize,
                style = Stroke(unit * .008f, cap = StrokeCap.Round)
            )
            val tipAngle = (start + 108f) / OrbDegrees
            val tip = mid + Offset(cos(tipAngle), sin(tipAngle)) * orbitRadius
            drawCircle(
                Brush.radialGradient(
                    listOf(mint.copy(alpha = .6f * processingAmount), mint.copy(alpha = 0f)),
                    center = tip, radius = unit * .032f
                ),
                radius = unit * .032f, center = tip
            )
            drawCircle(pearl.copy(alpha = processingAmount), radius = unit * .012f, center = tip)
        }
    }
}

private fun liquidRibbonPath(center: Offset, radius: Float, wave: Float): Path = Path().apply {
    fun point(x: Float, y: Float) = center + Offset(x, y) * radius
    val start = point(-1.22f, -.45f + wave * .08f)
    moveTo(start.x, start.y)
    var first = point(-.64f, -.99f + wave * .12f)
    var second = point(-.34f, .62f + wave * .2f)
    var end = point(.08f, .02f + wave * .12f)
    cubicTo(first.x, first.y, second.x, second.y, end.x, end.y)
    first = point(.58f, -.66f + wave * .1f)
    second = point(.58f, .89f - wave * .13f)
    end = point(1.24f, .44f)
    cubicTo(first.x, first.y, second.x, second.y, end.x, end.y)
    end = point(1.24f, .77f)
    lineTo(end.x, end.y)
    first = point(.48f, 1.05f - wave * .13f)
    second = point(.5f, -.25f + wave * .1f)
    end = point(.01f, .27f + wave * .12f)
    cubicTo(first.x, first.y, second.x, second.y, end.x, end.y)
    first = point(-.53f, .78f + wave * .2f)
    second = point(-.72f, -.56f + wave * .12f)
    end = point(-1.22f, -.14f + wave * .08f)
    cubicTo(first.x, first.y, second.x, second.y, end.x, end.y)
    close()
}

private fun organicOrbPath(center: Offset, radius: Float, flow: Float, drift: Float, energy: Float): Path {
    val count = 48
    val points = List(count) { index ->
        val angle = index * OrbTurn / count
        val ripple = 1f + (.045f + .014f * energy) * sin(3f * angle + flow)
            + .024f * cos(4f * angle - drift)
            + .018f * sin(2f * angle + 2f * flow)
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
