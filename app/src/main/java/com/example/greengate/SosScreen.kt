package com.example.greengate

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.greengate.ui.theme.DMSans
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SosInk = Color(0xFF0E1B17)
private val SosMuted = Color(0xFF6B7378)
// The tile art is cut from a mock-up on this exact card colour, so keep them in step.
private val SosCard = Color(0xFFF0F7F8)
private val SosMint = Color(0xFFDDF3E8)
private val SosSecurityBorder = Color(0xFF5BCDA6)
private val SosSecurityWash = Brush.horizontalGradient(listOf(Color(0xFFDEFAF0), Color(0xFFE6FBF4)))
private val SosGreen = Color(0xFF07563C)
private val SosHandle = Color(0xFFD9DEE2)

private class Hotline(val label: String, val number: String, @DrawableRes val art: Int)

// Singapore public hotlines. SCDF answers both ambulance and fire on 995.
private val Hotlines = listOf(
    Hotline("Call Police\nEmergency", "999", R.drawable.sos_police),
    Hotline("Call Ambulance", "995", R.drawable.sos_ambulance),
    Hotline("Call Fire\nEmergency", "995", R.drawable.sos_fire),
    Hotline("Dengue\nHotline", "1800 933 6483", R.drawable.sos_dengue),
    Hotline("NEA\nHotline", "1800 225 5632", R.drawable.sos_nea),
    Hotline("Animal\nHotline", "1800 476 1600", R.drawable.sos_animal),
)

/** "Call for Help" sheet opened from the home header's SOS button. Every call opens the dialer
 * with the number filled in, so nothing is placed without the resident confirming it there. */
@Composable
internal fun SosScreen(onDismiss: () -> Unit, onCallReceiverHarness: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    fun open(uri: String, action: String) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        try {
            context.startActivity(Intent(action, Uri.parse(uri)))
        } catch (_: ActivityNotFoundException) {}
    }
    fun dial(number: String) = open("tel:${number.replace(" ", "")}", Intent.ACTION_DIAL)

    // The sheet slides away first; whatever closed it runs once it is gone.
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    var afterClose by remember { mutableStateOf(onDismiss) }
    fun close(then: () -> Unit = onDismiss) {
        afterClose = then
        shown.targetState = false
    }
    LaunchedEffect(shown.isIdle, shown.currentState) {
        if (shown.isIdle && !shown.currentState) afterClose()
    }
    val dragOffset = remember { Animatable(0f) }
    val dismissDistance = with(density) { 140.dp.toPx() }

    Dialog({ close() }, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(.45f)
            // Frost the home screen behind the sheet where the device supports window blur.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window?.attributes = window?.attributes?.apply { blurBehindRadius = with(density) { 24.dp.roundToPx() } }
            }
        }
        Box(
            Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { close() }
        ) {
            AnimatedVisibility(shown, Modifier.align(Alignment.TopStart), enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    onClick = { close() }, shape = CircleShape, color = Color.Black.copy(alpha = .18f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = .75f)),
                    modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp).size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, "Close", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
            AnimatedVisibility(
                shown, Modifier.align(Alignment.BottomCenter).statusBarsPadding().padding(top = 60.dp),
                enter = slideInVertically { it }, exit = slideOutVertically { it }
            ) {
                Column(
                    Modifier.widthIn(max = 600.dp).fillMaxWidth()
                        .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                        .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        // Taps on the sheet itself must not reach the dismiss scrim.
                        .clickable(remember { MutableInteractionSource() }, indication = null) {}
                        .navigationBarsPadding()
                ) {
                    SheetHeader(Modifier.draggable(
                        rememberDraggableState { delta -> scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f)) } },
                        Orientation.Vertical,
                        onDragStopped = { velocity ->
                            if (dragOffset.value > dismissDistance || velocity > 1800f) close()
                            else dragOffset.animateTo(0f)
                        }
                    ))
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                        Hotlines.chunked(3).forEach { row ->
                            Row(Modifier.height(IntrinsicSize.Min).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { HotlineTile(it, Modifier.weight(1f)) { dial(it.number) } }
                            }
                        }
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecurityRow { dial(SecurityGuardhouse.phone) }
                            ManagementRow(Icons.Rounded.Call, "Call Management", "Contact building management") { dial(ManagementOffice.phone) }
                        }
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { close() }, shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SosGreen, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Close", fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = { close(onCallReceiverHarness) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Call Receiver Harness", color = SosGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.padding(top = 10.dp).size(36.dp, 4.dp).background(SosHandle, CircleShape))
        Image(painterResource(R.drawable.sos_siren), null, Modifier.padding(top = 20.dp).size(84.dp, 66.dp))
        Text("Call for Help", Modifier.padding(top = 10.dp), fontFamily = DMSans, fontSize = 28.sp,
            fontWeight = FontWeight.Bold, color = SosInk)
        Text("Quickly contact emergency services or\nbuilding support.", Modifier.padding(top = 6.dp, bottom = 18.dp),
            fontFamily = DMSans, fontSize = 15.sp, lineHeight = 21.sp, color = SosMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HotlineTile(hotline: Hotline, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = SosCard, modifier = modifier.fillMaxHeight()) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Image(painterResource(hotline.art), null, Modifier.size(70.dp, 52.dp))
            Spacer(Modifier.height(8.dp))
            Text(hotline.label, fontFamily = DMSans, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
                color = SosInk, textAlign = TextAlign.Center)
            Text(hotline.number, Modifier.padding(top = 3.dp), fontFamily = DMSans, fontSize = 13.sp, color = SosMuted,
                maxLines = 1)
        }
    }
}

@Composable
private fun ManagementRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = SosCard, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(SosMint, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = SosGreen, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SosInk)
                Text(subtitle, fontFamily = DMSans, fontSize = 13.sp, color = SosMuted)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SosInk, modifier = Modifier.size(24.dp))
        }
    }
}

/** Security is the row residents need fastest, so it stands out from the management rows. */
@Composable
private fun SecurityRow(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(SosSecurityWash).border(1.dp, SosSecurityBorder, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.sos_security), null, Modifier.size(56.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Call Security", fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SosInk)
            Text("Speak to our security team", fontFamily = DMSans, fontSize = 13.sp, color = SosMuted)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = SosInk, modifier = Modifier.size(24.dp))
    }
}
