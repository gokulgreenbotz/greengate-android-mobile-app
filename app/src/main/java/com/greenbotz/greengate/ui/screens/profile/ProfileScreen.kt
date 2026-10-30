package com.greenbotz.greengate.ui.screens.profile

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.ApiEnvironment
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.AppTheme
import com.greenbotz.greengate.core.session.IconSet
import com.greenbotz.greengate.core.session.LoginExperience
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.screens.assistant.greenBotVoiceFor
import com.greenbotz.greengate.ui.screens.auth.BiometricStatus
import com.greenbotz.greengate.ui.screens.auth.biometricIcon
import com.greenbotz.greengate.ui.screens.auth.biometricLabel
import com.greenbotz.greengate.ui.screens.auth.biometricStatus
import com.greenbotz.greengate.ui.screens.auth.findFragmentActivity
import com.greenbotz.greengate.ui.screens.auth.turnOnBiometricLogin
import com.greenbotz.greengate.ui.screens.home.LayeredActionIcon
import com.greenbotz.greengate.ui.screens.home.LayeredIconSetZero
import com.greenbotz.greengate.ui.screens.home.MarinaBackground
import com.greenbotz.greengate.ui.theme.DMSans
import java.util.Locale

private val ProfileInk: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val ProfileMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val ProfileGreen: Color @Composable get() = MaterialTheme.colorScheme.primary
private val ProfileCream: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

private val ProfileLogoutRed = Color(0xFFDF4336)

@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit = {},
    onSwitchServer: (ApiEnvironment) -> Unit = {}
) {
    val context = LocalContext.current
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmServer by remember { mutableStateOf<ApiEnvironment?>(null) }
    confirmServer?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmServer = null },
            title = { Text("Switch to ${target.label}?", fontFamily = DMSans) },
            text = {
                Text(
                    "You'll be signed out. Sign in again to use ${target.apiUrl}.",
                    fontFamily = DMSans
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmServer = null
                    onSwitchServer(target)
                }) { Text("Switch", fontFamily = DMSans) }
            },
            dismissButton = {
                TextButton(onClick = { confirmServer = null }) { Text("Cancel", fontFamily = DMSans) }
            }
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            icon = { Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = ProfileLogoutRed) },
            title = { Text("Log out?", fontFamily = DMSans) },
            text = { Text("You'll need to sign in again to use GreenGate.", fontFamily = DMSans) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text("Log out", color = ProfileLogoutRed, fontFamily = DMSans) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel", fontFamily = DMSans) }
            }
        )
    }
    Column(
        Modifier.fillMaxSize().background(MarinaBackground).verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ProfileInk)
                }
                Spacer(Modifier.width(2.dp))
                Text("Profile", fontFamily = DMSans, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ProfileInk)
            }
            Spacer(Modifier.height(24.dp))
            Text("HOME SCREEN ICONS", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp), color = ProfileCream, shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val useIconSet = AppPreferences.theme != AppTheme.ZERO || !AppPreferences.useReferenceArtwork
                    if (AppPreferences.theme == AppTheme.ZERO) {
                        IconSetOption(
                            title = "Design artwork",
                            subtitle = "Glossy illustrations from Theme 0",
                            selected = AppPreferences.useReferenceArtwork,
                            onClick = { AppPreferences.setUseReferenceArtwork(context, true) }
                        ) {
                            ReferenceArtworkPreview()
                        }
                        HorizontalDivider(color = Color(0xFFEDEAE0))
                    }
                    IconSetOption(
                        title = "Icon Set 0",
                        subtitle = "Illustrated icons with shadows",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.ZERO,
                        onClick = { AppPreferences.setIconSet(context, IconSet.ZERO) }
                    ) {
                        LayeredIconSetZero(0, 0f, false, Modifier.size(40.dp), IconSet.ZERO)
                    }
                    HorizontalDivider(color = Color(0xFFEDEAE0))
                    IconSetOption(
                        title = "Icon Set 0.1",
                        subtitle = "Soft clay icons with Set 0 animations",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.CLAY,
                        onClick = { AppPreferences.setIconSet(context, IconSet.CLAY) }
                    ) {
                        LayeredIconSetZero(0, 0f, false, Modifier.size(40.dp), IconSet.CLAY)
                    }
                    HorizontalDivider(color = Color(0xFFEDEAE0))
                    IconSetOption(
                        title = "Icon Set 2",
                        subtitle = "3D icons with sequential animations",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.TWO,
                        onClick = { AppPreferences.setIconSet(context, IconSet.TWO) }
                    ) {
                        LayeredActionIcon(0, 0f, false, Modifier.size(40.dp))
                    }
                    HorizontalDivider(color = Color(0xFFEDEAE0))
                    IconSetOption(
                        title = "Icon Set 3",
                        subtitle = "Simple flat icons",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.THREE,
                        onClick = { AppPreferences.setIconSet(context, IconSet.THREE) }
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(ProfileGreen.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = ProfileGreen, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("THEME", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = ProfileCream,
                shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    AppTheme.entries.forEach { theme ->
                        val title = when (theme) {
                            AppTheme.ZERO -> "Theme 0 - Glass"
                            AppTheme.ONE -> "Theme 1 - Classic"
                            AppTheme.TWO -> "Theme 2 - Fresh"
                            AppTheme.THREE -> "Theme 3 - Meadow"
                            AppTheme.FOUR -> "Theme 4 - Navy & Gold"
                        }
                        Row(Modifier.fillMaxWidth().selectable(
                            selected = AppPreferences.theme == theme,
                            role = Role.RadioButton,
                            onClick = { AppPreferences.setTheme(context, theme) }
                        ).padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(title, modifier = Modifier.weight(1f), fontFamily = DMSans,
                                color = ProfileInk, fontSize = 15.sp)
                            RadioButton(selected = AppPreferences.theme == theme, onClick = null)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("LOGIN EXPERIENCE", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = ProfileCream,
                shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    LoginExperience.entries.forEachIndexed { index, experience ->
                        if (index > 0) HorizontalDivider(color = Color(0xFFEDEAE0))
                        val selected = AppPreferences.loginExperience == experience
                        Row(
                            Modifier.fillMaxWidth().selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { AppPreferences.setLoginExperience(context, experience) }
                            ).padding(vertical = 10.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    when (experience) {
                                        LoginExperience.NEW -> "Dark luxury login"
                                        LoginExperience.WHITE -> "White luxury login"
                                        LoginExperience.CURRENT -> "Current login"
                                    },
                                    fontFamily = DMSans, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, color = ProfileInk
                                )
                                Text(
                                    when (experience) {
                                        LoginExperience.NEW -> "Navy and gold GreenGate login"
                                        LoginExperience.WHITE -> "White marble, gold branding and frosted glass"
                                        LoginExperience.CURRENT -> "Use the existing glass login"
                                    },
                                    fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted
                                )
                            }
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("HOME SCREEN CARDS", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp), color = ProfileCream, shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val showFeedback = AppPreferences.showFeedback
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(role = Role.Switch) { AppPreferences.setShowFeedback(context, !showFeedback) }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(ProfileGreen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Feedback, null, tint = ProfileGreen, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Show Feedback", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ProfileInk)
                        Text("Adds the Feedback card after Community", fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted)
                    }
                    Switch(
                        checked = showFeedback,
                        onCheckedChange = { AppPreferences.setShowFeedback(context, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = ProfileGreen)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("GREEN BOT VOICE", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            GreenBotVoiceSetting()
            Spacer(Modifier.height(24.dp))
            Text("SERVER", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = ProfileCream,
                shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    ApiEnvironment.entries.forEachIndexed { index, server ->
                        if (index > 0) HorizontalDivider(color = Color(0xFFEDEAE0))
                        val selected = ResidentSession.environment == server
                        Row(Modifier.fillMaxWidth().selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { if (!selected) confirmServer = server }
                        ).padding(vertical = 10.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(server.label, fontFamily = DMSans, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, color = ProfileInk)
                                Text(server.apiUrl, fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted)
                            }
                            RadioButton(selected = selected, onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = ProfileGreen))
                        }
                    }
                }
            }
            if (ResidentSession.state is ResidentSession.State.SignedIn) BiometricLoginSetting()
            Spacer(Modifier.height(24.dp))
            Text("ACCOUNT", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp), color = ProfileCream, shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { confirmLogout = true }
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(ProfileLogoutRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = ProfileLogoutRed, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Log out", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        color = ProfileLogoutRed, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Fingerprint / face sign-in switch. Hidden on phones that can't check either for the app. */
@Composable
private fun BiometricLoginSetting() {
    val context = LocalContext.current
    val status = remember(context) { biometricStatus(context) }
    if (status == BiometricStatus.UNAVAILABLE) return
    val label = remember(context) { biometricLabel(context) }
    val ready = status == BiometricStatus.READY
    val on = ResidentSession.biometricLogin
    var note by remember { mutableStateOf<String?>(null) }
    fun toggle(want: Boolean) {
        note = null
        if (!want) ResidentSession.useBiometricLogin(false)
        // Turning on asks for the fingerprint / face first, so it's known to work.
        else context.findFragmentActivity()?.turnOnBiometricLogin(label) { note = it }
    }
    Spacer(Modifier.height(24.dp))
    Text("SIGN-IN", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
    Spacer(Modifier.height(8.dp))
    Surface(
        shape = RoundedCornerShape(16.dp), color = ProfileCream, shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(enabled = ready, role = Role.Switch) { toggle(!on) }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(ProfileGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(biometricIcon(label), null, tint = ProfileGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${label.replaceFirstChar { it.uppercase() }} sign-in", fontFamily = DMSans, fontSize = 15.sp,
                    fontWeight = FontWeight.Medium, color = ProfileInk)
                Text(
                    note ?: if (ready) "Open GreenGate with your $label instead of an SMS code"
                    else "Add a $label in your phone's Settings first",
                    fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted
                )
            }
            Switch(
                checked = on,
                onCheckedChange = { toggle(it) },
                enabled = ready,
                colors = SwitchDefaults.colors(checkedTrackColor = ProfileGreen)
            )
        }
    }
}

@Composable
private fun ReferenceArtworkPreview() {
    val bitmap = ImageBitmap.imageResource(R.drawable.gg_home_reference_art)
    val painter = remember(bitmap) {
        BitmapPainter(bitmap, IntOffset(105, 940), IntSize(185, 138))
    }
    Image(painter, null, Modifier.size(40.dp), contentScale = ContentScale.Fit)
}

@Composable
private fun IconSetOption(
    title: String, subtitle: String, selected: Boolean, onClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        preview()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ProfileInk)
            Text(subtitle, fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted)
        }
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = ProfileGreen))
    }
}

/** Read aloud when a voice is previewed. */
private const val VoiceSample = "Hi, I'm Green Bot. How can I help you today?"

/** One installed English voice, numbered within its accent, e.g. "English (India) · Voice 2". */
private class BotVoiceOption(val voice: Voice, val label: String, val needsInternet: Boolean)

/** The phone's installed English voices: its own accent first, one entry per voice. */
private fun botVoiceOptions(tts: TextToSpeech): List<BotVoiceOption> {
    val installed = (tts.voices ?: emptySet()).filter {
        it.locale.language == "en" && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
    }
    // Google lists most voices twice, on-device and online. Keep one of each, preferring on-device.
    val unique = installed.groupBy { it.name.removeSuffix("-local").removeSuffix("-network") }
        .values.map { copies -> copies.minBy { it.isNetworkConnectionRequired } }
    val phoneAccent = Locale.getDefault().takeIf { it.language == "en" }?.country
    return unique.groupBy { it.locale.displayName }.toList()
        .sortedWith(compareBy({ (_, voices) -> voices.first().locale.country != phoneAccent }, { it.first }))
        .flatMap { (accent, voices) ->
            voices.sortedBy { it.name }.mapIndexed { index, voice ->
                BotVoiceOption(voice, "$accent · Voice ${index + 1}", voice.isNetworkConnectionRequired)
            }
        }
}

/** Plays the sample in [voice], or in the voice Green Bot uses by default when it is null. */
private fun previewVoice(tts: TextToSpeech, voice: Voice?) {
    runCatching {
        tts.stop()
        if (voice != null) {
            tts.setVoice(voice)
        } else {
            // Same order as Green Bot: the phone's English, then Indian, US and UK English.
            listOfNotNull(Locale.getDefault().takeIf { it.language == "en" }, Locale.forLanguageTag("en-IN"), Locale.US, Locale.UK)
                .firstOrNull { tts.setLanguage(it) >= TextToSpeech.LANG_AVAILABLE }
        }
        tts.speak(VoiceSample, TextToSpeech.QUEUE_FLUSH, null, "green-bot-voice-preview")
    }
}

/** Profile row for Green Bot's voice. The choice is kept on this phone and used the next time Green Bot opens. */
@Composable
private fun GreenBotVoiceSetting() {
    val context = LocalContext.current
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    // Null while the text-to-speech service starts.
    var engineReady by remember { mutableStateOf<Boolean?>(null) }
    DisposableEffect(Unit) {
        val main = Handler(Looper.getMainLooper())
        val tts = runCatching {
            TextToSpeech(context.applicationContext) { status -> main.post { engineReady = status == TextToSpeech.SUCCESS } }
        }.getOrNull()
        if (tts == null) engineReady = false
        engine = tts
        onDispose { runCatching { tts?.stop(); tts?.shutdown() } }
    }
    val options = remember(engine, engineReady) {
        val tts = engine
        if (tts == null || engineReady != true) emptyList() else runCatching { botVoiceOptions(tts) }.getOrDefault(emptyList())
    }
    // The voice Green Bot will use: the one picked here, else its default; null is the phone's own voice.
    val inUse = remember(options, AppPreferences.greenBotVoice) {
        engine?.takeIf { options.isNotEmpty() }?.let { runCatching { greenBotVoiceFor(it) }.getOrNull() }
    }
    val selected = options.firstOrNull { it.voice.name == inUse?.name }
    var choosing by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp), color = ProfileCream, shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(enabled = options.isNotEmpty(), role = Role.Button) { choosing = true }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(ProfileGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.RecordVoiceOver, null, tint = ProfileGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Voice", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ProfileInk)
                Text(
                    when {
                        engineReady == null -> "Loading voices…"
                        options.isEmpty() -> "No voices found. Turn on a text-to-speech service in phone settings."
                        selected == null -> "Phone default"
                        selected.needsInternet -> "${selected.label} · needs internet"
                        else -> selected.label
                    },
                    fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted
                )
            }
            if (options.isNotEmpty()) Icon(Icons.Rounded.ChevronRight, null, tint = ProfileMuted)
        }
    }

    val tts = engine
    if (choosing && tts != null) {
        fun close() {
            runCatching { tts.stop() }
            choosing = false
        }
        AlertDialog(
            onDismissRequest = ::close,
            title = { Text("Green Bot voice", fontFamily = DMSans) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).selectableGroup()) {
                    VoiceChoice("Phone default", null, selected = selected == null,
                        onSelect = { AppPreferences.setGreenBotVoice(context, AppPreferences.PhoneDefaultVoice) },
                        onPlay = { previewVoice(tts, null) })
                    options.forEach { option ->
                        VoiceChoice(option.label, if (option.needsInternet) "Needs internet" else null,
                            selected = option == selected,
                            onSelect = { AppPreferences.setGreenBotVoice(context, option.voice.name) },
                            onPlay = { previewVoice(tts, option.voice) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = ::close) { Text("Done", fontFamily = DMSans) } }
        )
    }
}

@Composable
private fun VoiceChoice(label: String, note: String?, selected: Boolean, onSelect: () -> Unit, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = ProfileGreen))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontFamily = DMSans, fontSize = 14.sp, color = ProfileInk)
            note?.let { Text(it, fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted) }
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Rounded.PlayArrow, "Play sample of $label", tint = ProfileGreen)
        }
    }
}
