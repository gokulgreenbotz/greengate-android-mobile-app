package com.example.greengate

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

internal class ActionIconSequence {
    var activeIndex by mutableIntStateOf(-1)
    val progress = Animatable(0f)
}

/** One clock owns every layer. animateTo suspends until the current icon finishes. */
@Composable
internal fun rememberActionIconSequence(iconSet: IconSet, durations: List<Int>): ActionIconSequence {
    val sequence = remember(iconSet) { ActionIconSequence() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(sequence, lifecycleOwner, durations) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                while (true) {
                    durations.forEachIndexed { index, duration ->
                        sequence.progress.snapTo(0f)
                        sequence.activeIndex = index
                        sequence.progress.animateTo(1f, tween(duration, easing = LinearEasing))
                        sequence.activeIndex = -1
                        delay(160)
                    }
                }
            } finally {
                sequence.activeIndex = -1
            }
        }
    }
    return sequence
}

/** The source files have transparent square canvases, with each moving piece isolated. */
@Composable
internal fun LayeredActionIcon(
    index: Int,
    progress: Float,
    active: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth().height(85.dp)
) {
    // A fixed design canvas makes the profile preview and home illustration identical.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth.value / 112f, maxHeight.value / 85f)
        Box(Modifier.requiredSize(112.dp, 85.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }, contentAlignment = Alignment.TopStart) {
            when (index) {
                0 -> FacilityLayers(progress)
                1 -> VisitorLayers(progress, active)
                2 -> FormLayers(progress, active)
                3 -> FeedbackLayers(progress, active)
            }
        }
    }
}

/**
 * Where a paper image's printed lines sit, in design dp (paper drawn at x 15, 85 dp square).
 * The strip is the long line, cropped from [source] and laid over the last line; it is
 * revealed from [lastLineWidth] by [extend] while the pen tip travels from ([endX], [endY]).
 * [slope] is the lines' rise per dp of run, for paper drawn at a tilt.
 */
private class PaperLines(
    val stripX: Float, val stripY: Float, val stripWidth: Float, val stripHeight: Float,
    val source: IntOffset, val sourceSize: IntSize,
    val lastLineWidth: Float, val endX: Float, val endY: Float, val extend: Float,
    val slope: Float = 0f
)

private val SetOnePaperLines = PaperLines(42.25f, 59.3f, 31.6f, 4.2f,
    IntOffset(402, 730), IntSize(466, 62), 16.75f, 59f, 61.4f, 14f)

// Lines at x 448..877 (last ends at 693), last line centred at y 849, 49 px thick.
private val SetZeroPaperLines = PaperLines(45.1f, 55.79f, 29.62f, 3.59f,
    IntOffset(444, 702), IntSize(437, 53), 16.88f, 62f, 57.55f, 12.47f)

// The clay sheet is tilted about 8.5°: its long line runs x 471..911 (y 663 → 598) and the
// last line x 505..834 (y 797 → 747). The long line's band is shifted by (34, 134) onto the last.
private val ClayPaperLines = PaperLines(49.23f, 48.6f, 29.89f, 6.98f,
    IntOffset(471, 583), IntSize(441, 103), 22.3f, 71.53f, 50.7f, 7.59f, slope = -.148f)

/** A pen drawn [size] dp square whose nib sits at ([nibX], [nibY]) within it. */
private class PenPlacement(val size: Float, val nibX: Float, val nibY: Float)

/** Where the person sits on the ID card: top-left, size and tilt to match the card face. */
private class PersonPlacement(val x: Float, val y: Float, val size: Float, val rotation: Float)

private data class IllustratedIconArt(
    @DrawableRes val calendar: Int,
    @DrawableRes val ball: Int,
    @DrawableRes val badge: Int,
    @DrawableRes val person: Int,
    @DrawableRes val paper: Int,
    @DrawableRes val pen: Int,
    @DrawableRes val bubbles: Int,
    @DrawableRes val dots: Int,
    @DrawableRes val heart: Int,
    val lines: PaperLines,
    val personAt: PersonPlacement,
    val penAt: PenPlacement = PenPlacement(38f, 8.5f, 34.3f)
)

private val SetOneArt = IllustratedIconArt(
    R.drawable.ic_calendar_racket, R.drawable.ic_tennis_ball, R.drawable.ic_id_card,
    R.drawable.ic_person_profile, R.drawable.ic_paper_with_lines, R.drawable.ic_pen,
    R.drawable.ic_chat_bubbles, R.drawable.ic_chat_dots, R.drawable.ic_chat_heart,
    SetOnePaperLines, PersonPlacement(27f, 29f, 43f, 9f)
)
// Set 0's card sits 0.8 dp right and 0.8 dp higher than Set 1's.
private val SetZeroArt = IllustratedIconArt(
    R.drawable.ic_calendar_racket_with_shadow, R.drawable.ic_tennis_ball_without_shadow,
    R.drawable.ic_id_card_with_shadow, R.drawable.ic_person_profile_with_shadow,
    R.drawable.ic_paper_with_lines_shadow, R.drawable.ic_pen_with_shadow,
    R.drawable.ic_chat_bubbles_without_dots_heart_shadow, R.drawable.ic_chat_dots_with_shadow,
    R.drawable.ic_chat_heart_with_shadow,
    SetZeroPaperLines, PersonPlacement(27.8f, 28.2f, 43f, 9f)
)
// The clay pack's ball and calendar are reframed to Set 0's canvases, so they share its
// motion. It has no chat bubbles, so Feedback keeps Set 0's. Its landscape card tilts 12°
// with the face centred near (602, 740) px; its steeper pen has the nib at (416, 1086) px.
private val SetClayArt = SetZeroArt.copy(
    calendar = R.drawable.ic_clay_calendar_racket, ball = R.drawable.ic_clay_ball,
    badge = R.drawable.ic_clay_id_card, person = R.drawable.ic_clay_person,
    paper = R.drawable.ic_clay_paper, pen = R.drawable.ic_clay_pen,
    lines = ClayPaperLines, personAt = PersonPlacement(35.9f, 30.6f, 38f, 12f),
    penAt = PenPlacement(43.7f, 14.5f, 37.85f)
)

/** Sets 0, 0.1 and 1 share placement and animation; only their supplied artwork differs. */
@Composable
internal fun LayeredIconSetOne(
    index: Int,
    progress: Float,
    active: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth().height(85.dp),
    iconSet: IconSet = IconSet.ONE
) {
    val art = when (iconSet) {
        IconSet.ZERO -> SetZeroArt
        IconSet.CLAY -> SetClayArt
        else -> SetOneArt
    }
    // Set 0.1 animates exactly like Set 0.
    val isZero = iconSet == IconSet.ZERO || iconSet == IconSet.CLAY
    androidx.compose.foundation.layout.BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth.value / 112f, maxHeight.value / 85f)
        Box(Modifier.requiredSize(112.dp, 85.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }, contentAlignment = Alignment.TopStart) {
            when (index) {
                0 -> {
                    if (isZero) {
                        // This pack's calendar is landscape; fit its shadow without stretching it.
                        Image(painterResource(art.calendar), null, Modifier.size(112.dp, 85.dp))
                    } else {
                        Art(art.calendar, 13f, 0f, 85f)
                    }
                    val hop = if (active) when {
                        progress in .08f.. .55f -> ballArc((progress - .08f) / .47f, 18f)
                        progress in .60f.. .83f -> ballArc((progress - .60f) / .23f, 4.3f)
                        else -> 0f
                    } else 0f
                    val squash = if (active) when {
                        progress < .08f -> sin(PI * progress / .08f).toFloat() * .08f
                        progress in .55f.. .60f -> sin(PI * (progress - .55f) / .05f).toFloat() * .12f
                        progress in .83f.. .91f -> sin(PI * (progress - .83f) / .08f).toFloat() * .04f
                        else -> 0f
                    } else 0f
                    if (isZero) {
                        // Keep the shadow on the ground at (53.5, 81.5), independent of the ball.
                        // Its opacity and spread follow the same height, so both settle together.
                        val heightFraction = (hop / 18f).coerceIn(0f, 1f)
                        Art(R.drawable.ic_tennis_ball_ground_shadow, 36.2f, 52.62f, 34.6f,
                            Modifier.graphicsLayer {
                                transformOrigin = TransformOrigin(.5006f, .8348f)
                                scaleX = 1f + heightFraction * .3f
                                scaleY = 1f + heightFraction * .2f
                                alpha = 1f - heightFraction * .65f
                            })
                    }
                    // The new shadow-free ball has less transparent padding. Preserve its visible
                    // diameter and the existing landing point rather than its old canvas size.
                    val ballAt = if (isZero) floatArrayOf(38.65f, 54.71f, 29.7f)
                    else floatArrayOf(37f, 52f, 33f)
                    Art(art.ball, ballAt[0], ballAt[1], ballAt[2],
                        Modifier.graphicsLayer {
                            translationY = -hop.dp.toPx()
                            transformOrigin = TransformOrigin(.5f, if (isZero) .902f else .89f)
                            scaleX = 1f + squash
                            scaleY = 1f - squash
                        })
                }
                1 -> {
                    Art(art.badge, 14f, 0f, 85f)
                    val pop = when {
                        !active -> 1f
                        progress < .15f -> 0f
                        progress < .48f -> smoothStep((progress - .15f) / .33f) * 1.12f
                        progress < .65f -> 1.12f - smoothStep((progress - .48f) / .17f) * .12f
                        else -> 1f
                    }
                    val at = art.personAt
                    Art(art.person, at.x, at.y, at.size,
                        Modifier.graphicsLayer {
                            rotationZ = at.rotation
                            alpha = if (pop == 0f) 0f else 1f
                            scaleX = pop
                            scaleY = pop
                        })
                }
                2 -> {
                    Art(art.paper, 15f, 0f, 85f)
                    // Each pack prints its lines in different places, so its measurements differ.
                    val lines = art.lines
                    val approach = smoothStep((progress / .2f).coerceIn(0f, 1f))
                    // Set 0 keeps its written line at rest and clears it only when a new run starts;
                    // Set 1 fades its line out as the pen returns.
                    val keepsLine = isZero
                    val written = when {
                        active -> ((progress - .2f) / .55f).coerceIn(0f, 1f)
                        keepsLine -> 1f
                        else -> 0f
                    }
                    val returning = smoothStep(((progress - .75f) / .25f).coerceIn(0f, 1f))
                    val restX = lines.endX + lines.extend - 1f
                    // On tilted paper the line's far end sits higher (or lower) than where it starts.
                    val farY = lines.endY + lines.slope * lines.extend
                    val restY = farY + 15.6f
                    // Reuse the paper's own long printed stroke so its color, bevel and thickness match.
                    CroppedArt(art.paper, lines.stripX, lines.stripY, lines.stripWidth, lines.stripHeight,
                        lines.source, lines.sourceSize,
                        Modifier.drawWithContent {
                            clipRect(right = (lines.lastLineWidth + lines.extend * written).dp.toPx()) {
                                this@drawWithContent.drawContent()
                            }
                        }.graphicsLayer {
                            alpha = when {
                                !active -> if (keepsLine) 1f else 0f
                                progress < .2f -> 0f
                                keepsLine -> 1f
                                else -> 1f - returning
                            }
                        })
                    val tipX = when {
                        !active -> restX
                        progress < .2f -> restX - (restX - lines.endX) * approach
                        progress < .75f -> lines.endX + written * lines.extend
                        else -> restX + 1f - returning
                    }
                    val tipY = when {
                        !active -> restY
                        progress < .2f -> restY - (restY - lines.endY) * approach
                        progress < .75f -> lines.endY + lines.slope * written * lines.extend
                        else -> farY + 15.6f * returning
                    }
                    val pen = art.penAt
                    Art(art.pen, tipX - pen.nibX, tipY - pen.nibY, pen.size)
                }
                3 -> {
                    // Set 0's bubbles sit higher: green 0.9 dp left, 0.8 dp up; peach 1.2 dp up.
                    val dotsDx = if (isZero) -.87f else 0f
                    val dotsDy = if (isZero) -.8f else 0f
                    val heartDy = if (isZero) -1.16f else 0f
                    // Once revealed, the reply bubble and heart stay for good, even after leaving Home.
                    var feedbackRevealed by rememberSaveable(iconSet) { mutableStateOf(false) }
                    LaunchedEffect(active, progress >= .97f) {
                        if (active && progress >= .97f) feedbackRevealed = true
                    }
                    // These independent assets let the green bubble remain visible throughout.
                    Art(R.drawable.ic_feedback_green_bubble, 8.27f + dotsDx, .8f + dotsDy, 84f)
                    repeat(3) { dot ->
                        val lift = if (active) typingDotLift(progress / .85f, dot) else 0f
                        // Row of dots sits just above the green bubble body's centre (50.3, 40.6).
                        CroppedArt(art.dots, 34.7f + dotsDx + dot * 11.1f, 34.85f + dotsDy, 9f, 9f,
                            IntOffset(216 + dot * 300, 519), IntSize(224, 224),
                            Modifier.graphicsLayer { translationY = (-lift * 5f).dp.toPx() })
                    }
                    // All dots land by .58. Reveal the reply bubble first, then its heart.
                    // Set 0 hides them only at the start of each run and shows them at rest;
                    // Set 1 reveals them once, on its first run, and then keeps them.
                    val bubbleT = when {
                        !active -> if (isZero || feedbackRevealed) 1f else 0f
                        !isZero && feedbackRevealed -> 1f
                        else -> ((progress - .58f) / .24f).coerceIn(0f, 1f)
                    }
                    // A cheerful reply: springs out of its tail with a soft overshoot and wiggle.
                    val bubbleScale = if (bubbleT >= 1f) 1f else replyPop(bubbleT)
                    val settle = (1f - bubbleT) * (1f - bubbleT)
                    Art(R.drawable.ic_feedback_red_bubble, 50.4f, 32.76f + heartDy, 52f,
                        Modifier.graphicsLayer {
                            transformOrigin = TransformOrigin(.72f, .8f)
                            alpha = (bubbleT / .25f).coerceIn(0f, 1f)
                            scaleX = bubbleScale
                            scaleY = bubbleScale
                            rotationZ = sin(3f * PI * bubbleT).toFloat() * 7f * settle
                            translationY = (4f * settle).dp.toPx()
                        })
                    val heartScale = when {
                        !active -> if (isZero || feedbackRevealed) 1f else 0f
                        !isZero && feedbackRevealed -> 1f
                        progress < .80f -> 0f
                        progress < .90f -> smoothStep((progress - .80f) / .10f) * 1.18f
                        progress < .97f -> 1.18f - smoothStep((progress - .90f) / .07f) * .18f
                        else -> 1f
                    }
                    // Heart sits just above the peach bubble body's centre (76.4, 58.3).
                    Art(art.heart, 67.65f, 49.45f + heartDy, 17.5f,
                        Modifier.graphicsLayer {
                            alpha = if (heartScale == 0f) 0f else 1f
                            scaleX = heartScale
                            scaleY = heartScale
                        })
                }
            }
        }
    }
}

private fun ballArc(phase: Float, height: Float) = 4f * height * phase * (1f - phase)

/** Person and add button stay visible; pressing the plus releases the bouncing heart. */
@Composable
internal fun CommunitySetZeroIcon(
    progress: Float,
    active: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth().height(85.dp),
    iconSet: IconSet = IconSet.ZERO
) {
    // Set 0.1's pieces are reframed to Set 0's canvases, so they share every position below.
    val clay = iconSet == IconSet.CLAY
    val man = if (clay) R.drawable.ic_clay_community_man else R.drawable.ic_community_man
    val button = if (clay) R.drawable.ic_clay_community_button else R.drawable.ic_community_green_circle
    val plus = if (clay) R.drawable.ic_clay_community_plus else R.drawable.ic_community_plus
    val heart = if (clay) R.drawable.ic_clay_community_heart else R.drawable.ic_community_heart
    val t = if (active) progress else 1f
    val press = when {
        t in .08f.. .18f -> smoothStep((t - .08f) / .10f)
        t in .18f.. .28f -> 1f - smoothStep((t - .18f) / .10f)
        else -> 0f
    }
    val ripple = if (t in .18f.. .40f) (t - .18f) / .22f else -1f
    val reveal = smoothStep(((t - .26f) / .14f).coerceIn(0f, 1f))
    val hop = when {
        t in .40f.. .65f -> ballArc((t - .40f) / .25f, 7f)
        t in .69f.. .84f -> ballArc((t - .69f) / .15f, 2.5f)
        t in .88f.. .96f -> ballArc((t - .88f) / .08f, .75f)
        else -> 0f
    }
    val squash = when {
        t in .65f.. .69f -> sin(PI * (t - .65f) / .04f).toFloat() * .07f
        t in .84f.. .88f -> sin(PI * (t - .84f) / .04f).toFloat() * .03f
        else -> 0f
    }
    androidx.compose.foundation.layout.BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth.value / 112f, maxHeight.value / 85f)
        Box(Modifier.requiredSize(112.dp, 85.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }, contentAlignment = Alignment.TopStart) {
            Art(man, 12f, 0f, 85f)
            if (ripple >= 0f) {
                Canvas(Modifier.offset(66.dp, (-3).dp).size(42.dp)) {
                    val eased = 1f - (1f - ripple) * (1f - ripple)
                    drawCircle(
                        color = Color(0xFF6FA86B).copy(alpha = .55f * (1f - ripple)),
                        radius = size.minDimension * (.36f + .30f * eased),
                        style = Stroke(width = (2.2f * (1f - ripple) + .6f).dp.toPx())
                    )
                }
            }
            Art(button, 66f, -3f, 42f,
                Modifier.graphicsLayer {
                    scaleX = 1f - .12f * press
                    scaleY = 1f - .12f * press
                })
            // Set 0.1's cross was lifted off its disc on the disc's own 42 dp canvas.
            Art(plus, if (clay) 66f else 72f, if (clay) -3f else 3f, if (clay) 42f else 29f,
                Modifier.graphicsLayer {
                    scaleX = 1f - .30f * press
                    scaleY = 1f - .30f * press
                    translationY = (1.5f * press).dp.toPx()
                    alpha = 1f - .2f * press
                })
            Art(heart, 8f, 37f, 48f,
                Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(.5f, .80f)
                    alpha = reveal
                    scaleX = reveal * (1f + squash)
                    scaleY = reveal * (1f - squash)
                    translationY = -hop.dp.toPx()
                })
        }
    }
}

/** ic_community.png's heart sits alone in clear space at x 548..706, y 290..478 (of 1254). */
private val CommunityHeartSource = IntOffset(548, 290)
private val CommunityHeartSize = IntSize(159, 189)
private const val CommunityArtX = 10.5f
private const val CommunityArtY = -6f
private const val CommunityArtSize = 91f

/** Shared by every icon set: the neighbours stay put while their heart floats and beats. */
@Composable
internal fun CommunityIcon(
    progress: Float,
    active: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth().height(85.dp)
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth.value / 112f, maxHeight.value / 85f)
        Box(Modifier.requiredSize(112.dp, 85.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }, contentAlignment = Alignment.TopStart) {
            val px = CommunityArtSize / 1254f
            val heartLeft = CommunityHeartSource.x * px
            val heartTop = CommunityHeartSource.y * px
            val heartWidth = CommunityHeartSize.width * px
            val heartHeight = CommunityHeartSize.height * px
            // Base illustration with the heart's clear box cut out, so only the live heart shows.
            Art(R.drawable.ic_community, CommunityArtX, CommunityArtY, CommunityArtSize,
                Modifier.drawWithContent {
                    clipRect(heartLeft.dp.toPx(), heartTop.dp.toPx(),
                        (heartLeft + heartWidth).dp.toPx(), (heartTop + heartHeight).dp.toPx(),
                        ClipOp.Difference) { this@drawWithContent.drawContent() }
                })
            val t = if (active) progress else 0f
            val float = sin(PI * t).toFloat()
            // Two heartbeats while it floats up, then it settles back into place.
            val beat = 1f + .26f * heartPulse(t, .32f) + .18f * heartPulse(t, .56f)
            CroppedArt(R.drawable.ic_community, CommunityArtX + heartLeft, CommunityArtY + heartTop,
                heartWidth, heartHeight, CommunityHeartSource, CommunityHeartSize,
                Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(.5f, .38f)
                    translationY = (-4f * float).dp.toPx()
                    scaleX = beat
                    scaleY = beat
                    rotationZ = sin(4f * PI * t).toFloat() * 6f * float
                })
        }
    }
}

/** A smooth 0→1→0 bump centred on [center], lasting .2 of the run. */
private fun heartPulse(t: Float, center: Float): Float {
    val phase = ((t - center) / .1f).coerceIn(-1f, 1f)
    return if (phase == -1f || phase == 1f) 0f else cos(PI.toFloat() * phase / 2f).let { it * it }
}

/** Damped spring from 0 to 1: peaks near 1.13, dips to about 0.98, then rests at 1. */
private fun replyPop(phase: Float) = 1f - exp(-5f * phase) * cos(2.5f * PI.toFloat() * phase)
private fun smoothStep(phase: Float) = phase * phase * (3f - 2f * phase)

@Composable
private fun BoxScope.CroppedArt(
    @DrawableRes resource: Int, x: Float, y: Float, width: Float, height: Float,
    source: IntOffset, sourceSize: IntSize, modifier: Modifier = Modifier
) {
    val bitmap = ImageBitmap.imageResource(resource)
    val painter = remember(bitmap, source, sourceSize) { BitmapPainter(bitmap, source, sourceSize) }
    Image(painter, contentDescription = null,
        modifier = Modifier.offset(x.dp, y.dp).size(width.dp, height.dp).then(modifier),
        contentScale = ContentScale.FillBounds)
}

@Composable
private fun BoxScope.Art(@DrawableRes resource: Int, x: Float, y: Float, size: Float,
    modifier: Modifier = Modifier) {
    Image(painterResource(resource), contentDescription = null,
        modifier = Modifier.offset(x.dp, y.dp).size(size.dp).then(modifier))
}

internal fun iconBounce(progress: Float): Float = when {
    progress < .42f -> sin(PI * progress / .42f).toFloat() * 14f
    progress < .72f -> sin(PI * (progress - .42f) / .30f).toFloat() * 6f
    else -> 0f
}

@Composable
private fun BoxScope.FacilityLayers(progress: Float) {
    Art(R.drawable.ic_book_facility_no_ball, 17f, 1f, 82f)
    Art(R.drawable.ic_book_facility_ball, 17f, 53f, 30f,
        Modifier.graphicsLayer { translationY = -iconBounce(progress).dp.toPx() })
}

@Composable
private fun BoxScope.VisitorLayers(progress: Float, active: Boolean) {
    Art(R.drawable.ic_invite_visitor_no_qr, 15f, 0f, 85f)
    Art(R.drawable.ic_invite_visitor_qr, 53f, 42f, 40f)
    Canvas(Modifier.offset(58.dp, 47.dp).size(30.dp)) {
        val inset = 3.dp.toPx()
        if (active) {
            val scan = if (progress < .5f) progress * 2f else (1f - progress) * 2f
            val y = inset + scan * (size.height - 2 * inset)
            val opacity = (minOf(progress, 1f - progress) * 12f).coerceIn(0f, 1f)
            drawLine(MarinaSosRed.copy(alpha = opacity * .3f),
                Offset(inset, y), Offset(size.width - inset, y), 5.dp.toPx(), StrokeCap.Round)
            drawLine(MarinaSosRed.copy(alpha = opacity),
                Offset(inset, y), Offset(size.width - inset, y), 1.2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun BoxScope.FormLayers(progress: Float, active: Boolean) {
    Art(R.drawable.ic_eforms_no_leaf, 13f, 0f, 85f)
    // The leaf rests where its stroke finishes. Each cycle it lifts back to just past the last
    // printed line, writes forward, stays put, and the written stroke fades away.
    val moveBack = if (active) (progress / .2f).coerceIn(0f, 1f) else 1f
    val writing = if (active) ((progress - .2f) / .65f).coerceIn(0f, 1f) else 1f
    val fadeOut = ((progress - .85f) / .15f).coerceIn(0f, 1f)
    val isMovingBack = active && progress < .2f
    // The last printed line's right edge (paper art sits 13 dp in); the stroke's round cap
    // starts a 1 dp gap past it so the two never overlap.
    val printedLineRight = 54.2f
    val strokeRadius = 2f
    val lineEndX = printedLineRight + 1f + strokeRadius
    val lineCenterY = 57.75f
    // Finishes at the end of the paper's longest printed line.
    val writeLength = 12f
    Canvas(Modifier.offset((lineEndX - strokeRadius).dp, (lineCenterY - strokeRadius).dp)
        .size((writeLength + 2 * strokeRadius).dp, (2 * strokeRadius).dp)) {
        val length = writing * writeLength
        if (active && !isMovingBack && fadeOut < 1f) {
            val start = strokeRadius.dp.toPx()
            drawLine(MarinaSosRed.copy(alpha = 1f - fadeOut),
                Offset(start, size.height / 2), Offset(start + length.dp.toPx(), size.height / 2),
                (2 * strokeRadius).dp.toPx(), StrokeCap.Round)
        }
    }
    // The stem tip is 6.8 dp across and 36 dp down within the leaf's canvas.
    Art(R.drawable.ic_eforms_leaf, lineEndX - 6.8f, lineCenterY - 36f, 38f, Modifier.graphicsLayer {
        if (isMovingBack) {
            translationX = (writeLength * (1f - moveBack)).dp.toPx()
            translationY = (-sin(PI * moveBack).toFloat() * 3f).dp.toPx()
        } else {
            translationX = (writing * writeLength).dp.toPx()
        }
    })
}

internal fun typingDotLift(progress: Float, index: Int): Float {
    val phase = (progress - index * .16f) / .36f
    return if (phase in 0f..1f) sin(PI * phase).toFloat() else 0f
}

@Composable
private fun BoxScope.FeedbackLayers(progress: Float, active: Boolean) {
    Art(R.drawable.ic_feedback_no_dots, 9f, 0f, 94f)
    repeat(3) { index ->
        val lift = if (active) typingDotLift(progress, index) else 0f
        // Both supplied dots share a canvas: orange in the air, original green on landing.
        val dot = if (lift > .001f) R.drawable.ic_feedback_dot_active else R.drawable.ic_feedback_dot
        Art(dot, 29f + index * 15f, 31f, 24f,
            Modifier.graphicsLayer {
                translationY = (-lift * 4f).dp.toPx()
                scaleX = 1f + lift * .12f
                scaleY = 1f + lift * .12f
            })
    }
}
