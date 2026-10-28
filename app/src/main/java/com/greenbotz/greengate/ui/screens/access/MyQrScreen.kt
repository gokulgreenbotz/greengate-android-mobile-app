package com.greenbotz.greengate.ui.screens.access

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.greenbotz.greengate.BuildConfig
import com.greenbotz.greengate.core.access.AccessQrClock
import com.greenbotz.greengate.core.session.AccessQr
import com.greenbotz.greengate.core.session.ResidentApiException
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.PrimaryGreen
import com.greenbotz.greengate.ui.theme.PrimaryGreenDark
import kotlinx.coroutines.delay

private val Ink = Color(0xFF1F2A1D)
private val Muted = Color(0xFF6B7468)
private val Backdrop = Color(0xFFF3EFE6)

/**
 * The resident's gate pass: a QR the intercom checks with the Backend. Each pass
 * lives 10 seconds and opens one gate once, so a new one is fetched shortly before
 * it expires and whenever the screen comes back. Screen brightness is raised while
 * it shows, so the intercom's camera reads it easily.
 */
@Composable
fun MyQrScreen(onCallTest: () -> Unit) {
    val demo = ResidentSession.state == ResidentSession.State.Demo
    var qr by remember { mutableStateOf<AccessQr?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    FullBrightness()

    LaunchedEffect(retry, demo) {
        if (demo) return@LaunchedEffect
        // Runs only while the screen is visible; leaving it and coming back fetches a fresh pass.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val pass = try {
                    ResidentSession.accessQr()
                } catch (e: ResidentApiException) {
                    qr = null
                    error = if (e.code == "network_error") "Can't load your QR. Check your connection." else e.message
                    return@repeatOnLifecycle
                }
                qr = pass
                error = null
                while (System.currentTimeMillis() < AccessQrClock.refreshAt(pass)) {
                    secondsLeft = AccessQrClock.secondsLeft(pass, System.currentTimeMillis())
                    delay(250)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Backdrop).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("My QR", fontFamily = DMSans, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "Show this to the intercom camera to open the gate.",
                fontFamily = DMSans, fontSize = 15.sp, color = Muted, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.size(300.dp).clip(RoundedCornerShape(28.dp)).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val pass = qr
                when {
                    demo -> Text("Not available in the demo", fontFamily = DMSans, color = Muted)
                    pass != null -> QrCode(pass.value)
                    error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error.orEmpty(), fontFamily = DMSans, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                        Button(onClick = { retry++ }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) { Text("Retry", fontFamily = DMSans) }
                    }
                    else -> CircularProgressIndicator(color = PrimaryGreen)
                }
            }
            Spacer(Modifier.height(20.dp))
            if (qr != null) {
                Text(
                    "Refreshes in ${(secondsLeft - (AccessQrClock.REFRESH_EARLY_MILLIS / 1000).toInt()).coerceAtLeast(0)}s",
                    fontFamily = DMSans, fontSize = 15.sp, color = PrimaryGreenDark
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                listOf(ResidentSession.firstName.takeIf { !demo }, ResidentSession.unitLabel).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                fontFamily = DMSans, fontSize = 15.sp, color = Ink, textAlign = TextAlign.Center
            )
            Text(ResidentSession.condoName, fontFamily = DMSans, fontSize = 14.sp, color = Muted)
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onCallTest) { Text("Call Receiver Test Harness", fontFamily = DMSans) }
            }
        }
    }
}

/** Plain black-on-white QR with a quiet zone: what intercom cameras read best. */
@Composable
private fun QrCode(content: String) {
    val matrix = remember(content) {
        QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 2)
        )
    }
    Canvas(Modifier.size(264.dp).semantics { contentDescription = "Your gate QR code" }) {
        val cell = size.width / matrix.width
        drawRect(Color.White)
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + .5f, cell + .5f))
        }
    }
}

/** Full screen brightness while shown; the previous level comes back on leave. */
@Composable
private fun FullBrightness() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = previous } }
    }
}
