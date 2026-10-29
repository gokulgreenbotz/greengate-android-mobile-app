package com.greenbotz.greengate.ui.screens.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.PlayfairDisplay
import java.security.SecureRandom
import kotlin.math.ceil
import kotlinx.coroutines.delay

private val PassInk = Color(0xFF0A0E5F)
private val PassText = Color(0xFF1E2269)
private val PassMuted = Color(0xFF5A5F8F)
private val PassGold = Color(0xFFC0812A)
private val PassGoldLight = Color(0xFFE9B562)
private val PassCream = Color(0xFFF6EAD5)
private val PassShadow = Color(0x2E0A0E5F)

/** How long one code is shown before a new one replaces it. */
private const val PassLifetimeMs = 10_000L

private val PassRandom = SecureRandom()

/**
 * A random one-time code and its expiry, with no resident details in it.
 * Door readers can't verify it yet: a real pass needs a code issued and signed by the server.
 */
private fun newPassCode(): String {
    val nonce = ByteArray(12).also(PassRandom::nextBytes).joinToString("") { "%02x".format(it) }
    val expiresAt = (System.currentTimeMillis() + PassLifetimeMs) / 1000
    return "GREENGATE|ACCESS|$nonce|$expiresAt"
}

/**
 * Theme 4's Quick Access pass, full screen. The code is valid for 10 seconds, counted down to 0,
 * then a new one replaces it. The screen stays on at full brightness while it's open, for the reader.
 */
@Composable
internal fun AccessQrScreen(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.attributes = window.attributes.apply { screenBrightness = 1f }
            }
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }

        var code by remember { mutableStateOf(newPassCode()) }
        var remainingMs by remember { mutableLongStateOf(PassLifetimeMs) }
        LaunchedEffect(code) {
            // Counted from the clock, so a paused screen catches up and refreshes on return.
            val start = SystemClock.elapsedRealtime()
            while (true) {
                val left = PassLifetimeMs - (SystemClock.elapsedRealtime() - start)
                remainingMs = left.coerceAtLeast(0L)
                if (left <= 0L) break
                withFrameMillis { }
            }
            delay(350) // "0" shows while the spent code fades out.
            code = newPassCode()
        }
        val seconds by remember { derivedStateOf { ceil(remainingMs / 1000.0).toInt() } }

        BoxWithConstraints(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(0f to Color(0xFFF4F8FD), .4f to Color(0xFFFCFBF8)))
        ) {
            val qrSize = minOf(maxWidth - 96.dp, 320.dp)
            val condo = ResidentSession.condoName
            val spent by animateFloatAsState(if (remainingMs == 0L) .25f else 1f, tween(250), label = "Spent code")
            // The QR card sits in the middle of the screen, the heading above it and the countdown below.
            // On a short screen the card moves down just enough for the heading to fit.
            Layout(
                content = {
                    Column(Modifier.padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(R.drawable.theme4_gate), null, Modifier.size(84.dp))
                        Text("Your Access QR", Modifier.padding(top = 10.dp), color = PassInk, fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, textAlign = TextAlign.Center)
                        Text(
                            buildAnnotatedString {
                                append("Hold it up to the ")
                                withStyle(SpanStyle(color = PassGold, fontWeight = FontWeight.Bold)) { append("entrance reader") }
                                if (condo.isNotBlank()) {
                                    append(" at ")
                                    withStyle(SpanStyle(color = PassGold, fontWeight = FontWeight.Bold)) { append(condo) }
                                }
                                append(".")
                            },
                            Modifier.padding(top = 6.dp), color = PassText, fontFamily = DMSans, fontSize = 16.sp,
                            lineHeight = 23.sp, textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        Modifier.shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = PassShadow, spotColor = PassShadow)
                            .clip(RoundedCornerShape(28.dp)).background(Color.White).padding(22.dp)
                            .semantics { contentDescription = "Access QR code, valid for $seconds seconds" }
                    ) {
                        AnimatedContent(
                            targetState = code,
                            transitionSpec = { (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = .96f)).togetherWith(fadeOut(tween(150))) },
                            label = "Access code"
                        ) { shown ->
                            RoundedQr(shown, Modifier.size(qrSize).alpha(spent))
                        }
                    }

                    Row(
                        Modifier.padding(top = 24.dp).clip(CircleShape).background(PassCream)
                            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountdownRing({ remainingMs }, seconds)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(if (seconds > 0) "Valid for $seconds s" else "Refreshing…", color = PassInk, fontFamily = DMSans,
                                fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
                            Text("A new code appears every 10 seconds", color = PassMuted, fontFamily = DMSans,
                                fontSize = 12.5f.sp, lineHeight = 16.sp)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)
            ) { measurables, space ->
                val (heading, qr, countdown) = measurables.map { it.measure(space.copy(minWidth = 0, minHeight = 0)) }
                val width = space.maxWidth
                val qrTop = maxOf((space.maxHeight - qr.height) / 2, heading.height)
                layout(width, space.maxHeight) {
                    heading.placeRelative((width - heading.width) / 2, qrTop - heading.height)
                    qr.placeRelative((width - qr.width) / 2, qrTop)
                    countdown.placeRelative((width - countdown.width) / 2, qrTop + qr.height)
                }
            }

            Box(
                Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(top = 8.dp, end = 24.dp).size(46.dp)
                    .shadow(6.dp, CircleShape, ambientColor = PassShadow, spotColor = PassShadow)
                    .clip(CircleShape).background(Color.White)
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, "Close", Modifier.size(24.dp), tint = PassInk)
            }
        }
    }
}

/** Seconds left in a gold ring that drains smoothly; [remainingMs] is read while drawing, once a frame. */
@Composable
private fun CountdownRing(remainingMs: () -> Long, seconds: Int) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                Brush.sweepGradient(listOf(PassGoldLight, PassGold, PassGoldLight)),
                -90f, 360f * remainingMs() / PassLifetimeMs, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text("$seconds", color = PassInk, fontFamily = DMSans, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

/**
 * The code drawn like the design: neighbouring modules join into rounded runs, the three corner
 * markers are rounded squares, and the gate sits in a cleared centre (high error correction covers it).
 */
@Composable
private fun RoundedQr(content: String, modifier: Modifier) {
    val matrix = remember(content) {
        QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0)
        )
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawRoundedQr(matrix) }
        Box(
            Modifier.fillMaxSize(.2f).clip(RoundedCornerShape(12.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.theme4_gate), null, Modifier.fillMaxSize(.86f))
        }
    }
}

private fun DrawScope.drawRoundedQr(matrix: BitMatrix) {
    val n = matrix.width
    val cell = size.width / n
    fun finder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
    // Modules under the centre logo (plus a one-module margin) are left out.
    val clear = (n * .2f / 2f + 1f)
    fun cleared(x: Int, y: Int) = kotlin.math.abs(x + .5f - n / 2f) < clear && kotlin.math.abs(y + .5f - n / 2f) < clear
    fun dark(x: Int, y: Int) = x in 0 until n && y in 0 until n && matrix[x, y] && !finder(x, y) && !cleared(x, y)

    for (y in 0 until n) for (x in 0 until n) {
        if (!dark(x, y)) continue
        val cx = (x + .5f) * cell
        val cy = (y + .5f) * cell
        drawCircle(PassInk, cell * .5f, Offset(cx, cy))
        if (dark(x + 1, y)) drawRect(PassInk, Offset(cx, y * cell), Size(cell, cell))
        if (dark(x, y + 1)) drawRect(PassInk, Offset(x * cell, cy), Size(cell, cell))
    }
    listOf(0 to 0, n - 7 to 0, 0 to n - 7).forEach { (fx, fy) ->
        val origin = Offset(fx * cell, fy * cell)
        drawRoundRect(
            PassInk, origin + Offset(cell / 2f, cell / 2f), Size(cell * 6f, cell * 6f),
            CornerRadius(cell * 2f), style = Stroke(cell)
        )
        drawRoundRect(PassInk, origin + Offset(cell * 2f, cell * 2f), Size(cell * 3f, cell * 3f), CornerRadius(cell))
    }
}
