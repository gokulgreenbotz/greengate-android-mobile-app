package com.greenbotz.greengate.ui.screens.assistant

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.core.assistant.GreenBotAction
import com.greenbotz.greengate.core.assistant.GreenBotReply
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.screens.home.GreenGateWordmark
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.PlayfairDisplay
import java.util.Calendar

// Theme 4 - Navy & Gold's voice screen: the same Green Bot, presented as an AI concierge.
private val ConciergeInk = Color(0xFF0A0E5F)
private val ConciergeBlue = Color(0xFF2B4AA0)
private val ConciergeGold = Color(0xFFC0812A)
private val ConciergeGoldLine = Color(0xFFE2C48F)
private val ConciergeCream = Color(0xFFF6EAD5)
private val ConciergeShadow = Color(0x260A0E5F)
private val ConciergeNavyTop = Color(0xFF1B2F6E)
private val ConciergeNavy = Color(0xFF071A45)
private val ConciergePageTop = Color(0xFFFCFBF8) // Theme 4's page colour, so the status bar strip matches.
private val ConciergePage = Color(0xFFFCFBF8)

// Laid out at a 411dp design width and scaled to the screen, like the Theme 4 home screen.
private fun Float.u(scale: Float) = (this * scale).dp
private fun Int.u(scale: Float) = toFloat().u(scale)
private fun Float.t(scale: Float) = (this * scale).sp
private fun Int.t(scale: Float) = toFloat().t(scale)

private fun Modifier.conciergeShadow(shape: Shape, elevation: Dp) =
    shadow(elevation, shape, clip = false, ambientColor = ConciergeShadow, spotColor = ConciergeShadow)

private class ConciergePrompt(val text: String, val icon: ImageVector)

// The built-in guide answers each of these with a button to the right screen.
private val ConciergePrompts = listOf(
    ConciergePrompt("Help me create an invite", Icons.Outlined.PersonAddAlt),
    ConciergePrompt("Show available facilities", Icons.Outlined.CalendarMonth),
    ConciergePrompt("Show community announcements", Icons.Outlined.Campaign)
)

private enum class ConciergeState { READY, LISTENING, THINKING, SPEAKING }

/** What the concierge says as it opens: shown as [title] and [line] while it is read aloud. */
internal class ConciergeGreeting(val title: String, val line: String) {
    val spoken: String get() = "$title. $line"
}

/** "Good evening, Gokul" by the phone's clock, or just "Good evening" when the household has no name. */
internal fun conciergeGreeting(): ConciergeGreeting {
    val timeOfDay = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    // firstName is "there" when the household has no name.
    val name = ResidentSession.firstName.takeIf { it != "there" }
    return ConciergeGreeting(
        if (name != null) "$timeOfDay, $name" else timeOfDay,
        "I'm your GreenGate concierge. How can I help you today?"
    )
}

/**
 * Theme 4's voice mode. All state and actions belong to [GreenBotScreen]; this only lays them out.
 * [greeting] is set while the opening greeting is being read.
 */
@Composable
internal fun GreenBotConcierge(
    listening: Boolean, processing: Boolean, speaking: Boolean, level: Float, partialText: String,
    question: String?, reply: GreenBotReply?, replySpeaking: Boolean, greeting: ConciergeGreeting?,
    error: String?, replyMuted: Boolean, privacyNotice: Boolean, voiceAllowed: Boolean, canReset: Boolean,
    snackbar: SnackbarHostState,
    onBack: () -> Unit, onMicrophone: () -> Unit, onStop: () -> Unit, onTypeInstead: () -> Unit,
    onPrompt: (String) -> Unit, onAction: (GreenBotAction) -> Unit, onCopy: (String) -> Unit, onRead: () -> Unit,
    onReset: () -> Unit, onDismissError: () -> Unit, onTurnUpVolume: () -> Unit, onOpenSettings: (() -> Unit)?
) {
    val state = when {
        listening -> ConciergeState.LISTENING
        processing -> ConciergeState.THINKING
        speaking -> ConciergeState.SPEAKING
        else -> ConciergeState.READY
    }
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        icon = { Icon(Icons.Outlined.Info, null, tint = ConciergeGold) },
        title = { Text("Your AI Concierge", fontFamily = DMSans) },
        text = {
            Text(
                "Ask about facility bookings, visitor invites, payments, community information and settings. " +
                    "The microphone is on only while this screen is open." +
                    if (privacyNotice) " Please don't share personal details." else "",
                fontFamily = DMSans
            )
        },
        confirmButton = {
            TextButton(onClick = { showInfo = false }) { Text("Done", fontFamily = DMSans, color = ConciergeInk) }
        },
        dismissButton = if (canReset) ({
            TextButton(onClick = { showInfo = false; onReset() }) { Text("New chat", fontFamily = DMSans, color = ConciergeInk) }
        }) else null,
        containerColor = Color.White
    )

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to ConciergePageTop, .35f to ConciergePage))
            .consumeWindowInsets(WindowInsets.navigationBars).imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val s = minOf(maxWidth.value, 600f) / 411f
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.u(s)).padding(top = 6.u(s)).height(52.u(s))) {
                ConciergeRoundButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back", s, Modifier.align(Alignment.CenterStart), onClick = onBack)
                GreenGateWordmark(s, Modifier.align(Alignment.Center))
                ConciergeRoundButton(Icons.Outlined.Info, "About your AI Concierge", s, Modifier.align(Alignment.CenterEnd),
                    tint = ConciergeGold) { showInfo = true }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.u(s)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your AI Concierge", Modifier.padding(top = 10.u(s)), color = ConciergeInk, fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold, fontSize = 32.t(s), lineHeight = 38.t(s), textAlign = TextAlign.Center)
                Text("Here to help you feel at home", Modifier.padding(top = 2.u(s)), color = ConciergeBlue,
                    fontFamily = DMSans, fontSize = 17.t(s), lineHeight = 22.t(s), textAlign = TextAlign.Center)
                ResidentSession.condoName.takeIf { it.isNotBlank() }?.let { condo ->
                    Row(Modifier.padding(top = 8.u(s)), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, Modifier.size(19.u(s)), tint = ConciergeGold)
                        Text(condo, Modifier.padding(start = 6.u(s)), color = ConciergeBlue, fontFamily = DMSans,
                            fontSize = 15.t(s), lineHeight = 20.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                ConciergePill(state, s, Modifier.padding(top = 16.u(s)))

                // The orb steps back once there's a reply to read.
                val compact = question != null && state == ConciergeState.READY
                val orbSize by animateDpAsState(
                    if (compact) 140.u(s) else 230.u(s),
                    spring(dampingRatio = .9f, stiffness = Spring.StiffnessLow), label = "Concierge orb"
                )
                ConciergeOrb(
                    Modifier.size(orbSize), listening = listening, speaking = speaking,
                    level = level, processing = processing
                )
                Text(
                    when (state) {
                        ConciergeState.LISTENING -> "I’m listening…"
                        ConciergeState.THINKING -> "One moment…"
                        ConciergeState.SPEAKING -> greeting?.title ?: "Here’s what I found"
                        ConciergeState.READY -> if (question != null) "Anything else?" else "How can I help?"
                    },
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = ConciergeInk,
                    fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 32.t(s), lineHeight = 38.t(s),
                    textAlign = TextAlign.Center
                )
                Text(
                    when (state) {
                        ConciergeState.LISTENING -> "Ask about your home or request help."
                        ConciergeState.THINKING -> "Working on your answer."
                        ConciergeState.SPEAKING -> greeting?.line ?: "You can stop the reply at any time."
                        ConciergeState.READY -> if (privacyNotice) "Ask about the app. Please don't share personal details."
                            else "Tap Start listening and ask away."
                    },
                    Modifier.padding(top = 4.u(s)), color = ConciergeBlue, fontFamily = DMSans,
                    fontSize = 16.5f.t(s), lineHeight = 22.t(s), textAlign = TextAlign.Center
                )

                if (partialText.isNotBlank() && (listening || processing)) {
                    Text(
                        partialText,
                        Modifier.padding(top = 16.u(s)).fillMaxWidth().conciergeShadow(RoundedCornerShape(16.u(s)), 4.u(s))
                            .background(Color.White, RoundedCornerShape(16.u(s))).padding(16.u(s)),
                        color = ConciergeInk, fontFamily = DMSans, fontSize = 16.t(s), lineHeight = 23.t(s),
                        textAlign = TextAlign.Center
                    )
                } else if (question != null && !listening) {
                    Spacer(Modifier.height(16.u(s)))
                    BotVoiceReply(question, reply, onAction, onCopy, replySpeaking, onRead)
                }

                Text("You can say", Modifier.fillMaxWidth().padding(top = 22.u(s), bottom = 10.u(s)), color = ConciergeInk,
                    fontFamily = DMSans, fontWeight = FontWeight.Medium, fontSize = 16.t(s), lineHeight = 20.t(s))
                ConciergePrompts.forEach { prompt ->
                    ConciergeSuggestion(prompt, s, enabled = state != ConciergeState.THINKING) { onPrompt(prompt.text) }
                    Spacer(Modifier.height(10.u(s)))
                }
            }

            Column(Modifier.padding(horizontal = 18.u(s))) {
                if (error == null && replyMuted) {
                    GreenBotErrorNotice(
                        message = "Your media volume is off, so you can't hear the reply.",
                        onDismiss = onDismissError, onTurnUpVolume = onTurnUpVolume,
                        icon = Icons.AutoMirrored.Outlined.VolumeOff
                    )
                }
                if (error != null) {
                    GreenBotErrorNotice(message = error, onDismiss = onDismissError, onUseKeyboard = onTypeInstead,
                        onOpenSettings = onOpenSettings)
                }
                Row(Modifier.padding(top = 10.u(s)), horizontalArrangement = Arrangement.spacedBy(12.u(s))) {
                    ConciergeTypeButton(s, Modifier.weight(1f), onTypeInstead)
                    ConciergeMainButton(state, s, Modifier.weight(1f), enabled = voiceAllowed) {
                        when (state) {
                            // Nothing heard yet: just stop. Otherwise send what was said.
                            ConciergeState.LISTENING -> if (partialText.isBlank()) onStop() else onMicrophone()
                            ConciergeState.READY -> onMicrophone()
                            ConciergeState.THINKING, ConciergeState.SPEAKING -> onStop()
                        }
                    }
                }
                Text("Voice is active only while this screen is open.",
                    Modifier.fillMaxWidth().padding(top = 10.u(s), bottom = 12.u(s)), color = ConciergeBlue,
                    fontFamily = DMSans, fontSize = 12.5f.t(s), lineHeight = 16.t(s), textAlign = TextAlign.Center)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 112.dp)) { data ->
            Snackbar(shape = RoundedCornerShape(16.dp), containerColor = ConciergeInk) {
                Text(data.visuals.message, fontFamily = DMSans, color = Color.White)
            }
        }
    }
}

@Composable
private fun ConciergeRoundButton(
    icon: ImageVector, label: String, s: Float, modifier: Modifier, tint: Color = ConciergeInk, onClick: () -> Unit
) {
    Box(
        modifier.size(46.u(s)).conciergeShadow(CircleShape, 6.u(s)).clip(CircleShape).background(Color.White)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, Modifier.size(26.u(s)), tint = tint)
    }
}

@Composable
private fun ConciergePill(state: ConciergeState, s: Float, modifier: Modifier) {
    val (icon, label) = when (state) {
        ConciergeState.LISTENING -> Icons.Rounded.GraphicEq to "Listening"
        ConciergeState.THINKING -> Icons.Outlined.AutoAwesome to "Thinking"
        ConciergeState.SPEAKING -> Icons.AutoMirrored.Outlined.VolumeUp to "Speaking"
        ConciergeState.READY -> Icons.Outlined.Mic to "Ready"
    }
    Row(
        modifier.background(ConciergeCream, CircleShape).padding(horizontal = 18.u(s), vertical = 7.u(s)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.u(s)), tint = ConciergeInk)
        Text(label, Modifier.padding(start = 8.u(s)), color = ConciergeInk, fontFamily = DMSans,
            fontWeight = FontWeight.Medium, fontSize = 15.5f.t(s), lineHeight = 20.t(s))
    }
}

@Composable
private fun ConciergeSuggestion(prompt: ConciergePrompt, s: Float, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.u(s))
    Row(
        Modifier.fillMaxWidth().height(54.u(s)).conciergeShadow(shape, 4.u(s)).clip(shape).background(Color.White)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(start = 18.u(s), end = 12.u(s)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(prompt.icon, null, Modifier.size(27.u(s)), tint = ConciergeInk)
        Text(prompt.text, Modifier.weight(1f).padding(start = 16.u(s), end = 8.u(s)), color = ConciergeInk,
            fontFamily = DMSans, fontWeight = FontWeight.Medium, fontSize = 16.t(s), lineHeight = 20.t(s),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(26.u(s)), tint = ConciergeInk)
    }
}

@Composable
private fun ConciergeTypeButton(s: Float, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(54.u(s)).conciergeShadow(CircleShape, 5.u(s)).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFBF7EF))))
            .border(1.dp, ConciergeGoldLine, CircleShape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.u(s)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Keyboard, null, Modifier.size(26.u(s)), tint = ConciergeInk)
        Text("Type instead", Modifier.padding(start = 10.u(s)), color = ConciergeInk, fontFamily = DMSans,
            fontWeight = FontWeight.Medium, fontSize = 16.t(s), lineHeight = 20.t(s), maxLines = 1)
    }
}

@Composable
private fun ConciergeMainButton(state: ConciergeState, s: Float, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier.height(54.u(s)).alpha(if (enabled) 1f else .5f).conciergeShadow(CircleShape, 8.u(s)).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(ConciergeNavyTop, ConciergeNavy)))
            .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFF3CF85), ConciergeGold, Color(0xFFF3CF85))), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 14.u(s)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        when (state) {
            ConciergeState.LISTENING -> Box(Modifier.size(19.u(s)).background(Color.White, RoundedCornerShape(5.u(s))))
            ConciergeState.THINKING -> CircularProgressIndicator(Modifier.size(19.u(s)), color = Color.White, strokeWidth = 2.dp)
            ConciergeState.SPEAKING -> Icon(Icons.AutoMirrored.Outlined.VolumeOff, null, Modifier.size(22.u(s)), tint = Color.White)
            ConciergeState.READY -> Icon(Icons.Outlined.Mic, null, Modifier.size(22.u(s)), tint = Color.White)
        }
        Text(
            when (state) {
                ConciergeState.LISTENING -> "Stop listening"
                ConciergeState.THINKING -> "Cancel"
                ConciergeState.SPEAKING -> "Stop reply"
                ConciergeState.READY -> "Start listening"
            },
            Modifier.padding(start = 10.u(s)), color = Color.White, fontFamily = DMSans,
            fontWeight = FontWeight.Medium, fontSize = 16.t(s), lineHeight = 20.t(s), maxLines = 1
        )
    }
}
