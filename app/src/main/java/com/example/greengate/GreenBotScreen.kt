package com.example.greengate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

// Keep secondary text readable over the pale mint glass without changing its theme hue.
private val BotSecondaryText: Color
    @Composable get() = MaterialTheme.colorScheme.let { lerp(it.onSurfaceVariant, it.onSurface, .12f) }

/** The reference-style assistant lives behind the original home card. */
@Composable
internal fun GreenBotScreen(navController: NavController, voiceAllowed: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = MaterialTheme.colorScheme
    val backdrop = if (AppPreferences.theme == AppTheme.ZERO) BookingsBackdrop else
        Brush.verticalGradient(listOf(colors.surfaceVariant, colors.background))
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var draft by rememberSaveable { mutableStateOf("") }
    var questions by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var voiceMode by rememberSaveable { mutableStateOf(false) }
    var permissionError by remember { mutableStateOf<String?>(null) }
    var pendingSpokenReply by remember { mutableStateOf<String?>(null) }

    val onRecognized by rememberUpdatedState<(String) -> Unit> { question ->
        if (voiceMode && voiceAllowed) {
            val trimmed = question.trim()
            if (trimmed.isNotEmpty()) {
                questions = ArrayList(questions).apply { add(trimmed) }
                pendingSpokenReply = replyToGreenBot(trimmed).text
            }
        }
    }
    val voice = remember(context.applicationContext) {
        GreenBotVoice(context.applicationContext) { onRecognized(it) }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (voiceMode && voiceAllowed && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                voice.startListening()
            }
        } else {
            permissionError = "Allow microphone access to speak with Green Bot. You can also switch to Chat and type."
        }
    }

    DisposableEffect(voice) {
        onDispose { voice.release() }
    }
    LaunchedEffect(voiceAllowed) {
        if (!voiceAllowed) {
            pendingSpokenReply = null
            voice.cancel()
        }
    }
    DisposableEffect(voice, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                pendingSpokenReply = null
                voice.cancel()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pendingSpokenReply = null
            voice.cancel()
        }
    }
    LaunchedEffect(pendingSpokenReply) {
        pendingSpokenReply?.let { reply ->
            if (voiceMode && voiceAllowed && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                voice.speak(reply)
            }
            pendingSpokenReply = null
        }
    }
    LaunchedEffect(questions.size, keyboardVisible, voiceMode) {
        if (!voiceMode && questions.isNotEmpty()) listState.animateScrollToItem(questions.size)
    }

    fun sendQuestion() {
        val trimmed = draft.trim()
        if (trimmed.isEmpty()) return
        voice.cancel()
        questions = ArrayList(questions).apply { add(trimmed) }
        draft = ""
    }

    fun selectMode(useVoice: Boolean) {
        if (voiceMode == useVoice) return
        focusManager.clearFocus()
        voice.cancel()
        pendingSpokenReply = null
        permissionError = null
        voice.clearError()
        voiceMode = useVoice
    }

    fun openAction(action: GreenBotAction) {
        voice.cancel()
        pendingSpokenReply = null
        focusManager.clearFocus()
        navController.navigate(when (action) {
            GreenBotAction.BOOK_FACILITY -> Screen.BookFacility.route
            GreenBotAction.MY_BOOKINGS -> Screen.Bookings.route
            GreenBotAction.INVITE_VISITOR -> Screen.Visitors.route
            GreenBotAction.PAYMENTS -> Screen.Payments.route
            GreenBotAction.COMMUNITY -> Screen.CommunityInfo.route
            GreenBotAction.PROFILE -> Screen.Profile.route
        })
    }

    fun toggleMicrophone() {
        if (!voiceAllowed || voice.isProcessing) return
        permissionError = null
        voice.clearError()
        if (voice.isListening) {
            voice.finishListening()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voice.startListening()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun goBack() {
        pendingSpokenReply = null
        voice.cancel()
        navController.popBackStack()
    }
    BackHandler(enabled = voiceAllowed, onBack = ::goBack)

    Column(
        Modifier.fillMaxSize().background(backdrop)
            .consumeWindowInsets(WindowInsets.navigationBars).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BotTopBar(
            onBack = ::goBack,
            canReset = questions.isNotEmpty(),
            onReset = {
                voice.cancel()
                pendingSpokenReply = null
                permissionError = null
                questions = arrayListOf()
                draft = ""
                focusManager.clearFocus()
            }
        )
        Surface(
            Modifier.weight(1f).widthIn(max = 640.dp).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
            color = colors.surface.copy(alpha = .76f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .8f)),
            shadowElevation = 6.dp
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val orbSize = (maxHeight * .30f).coerceIn(130.dp, 210.dp)
                Column(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                        colors.surfaceVariant.copy(alpha = .62f), colors.background.copy(alpha = .78f)
                    ))),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (voiceMode || questions.isEmpty()) {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            BotHeading()
                            Spacer(Modifier.height(22.dp))
                            GreenBotOrb(Modifier.size(orbSize), listening = voice.isListening,
                                speaking = voice.isSpeaking || voice.isProcessing, level = voice.level)
                            Spacer(Modifier.height(22.dp))
                            val status = when {
                                voice.isListening -> "I'm listening to you..."
                                voice.isProcessing -> "Preparing your reply..."
                                voice.isSpeaking -> "Green Bot is speaking..."
                                voiceMode -> "Tap the microphone. I'm here to listen."
                                else -> "I'm here to help you."
                            }
                            Text(status, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                fontFamily = DMSans, fontSize = 15.sp, lineHeight = 23.sp,
                                color = BotSecondaryText, textAlign = TextAlign.Center)
                            if (voiceMode) {
                                if (voice.partialText.isNotBlank() && voice.isListening) {
                                    Spacer(Modifier.height(16.dp))
                                    Text(voice.partialText, fontFamily = DMSans, fontSize = 15.sp,
                                        lineHeight = 22.sp, textAlign = TextAlign.Center, color = colors.onSurface)
                                } else if (questions.isNotEmpty()) {
                                    Spacer(Modifier.height(22.dp))
                                    BotVoiceReply(questions.last(), replyToGreenBot(questions.last()), ::openAction)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            itemsIndexed(questions, key = { index, _ -> "question-$index" }) { _, question ->
                                BotConversation(question, replyToGreenBot(question), ::openAction)
                            }
                            item(key = "end") { Spacer(Modifier.height(4.dp)) }
                        }
                    }
                    val error = permissionError ?: voice.error
                    if (error != null) {
                        Text(error, Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }, fontFamily = DMSans,
                            fontSize = 12.sp, lineHeight = 18.sp, color = BotSecondaryText, textAlign = TextAlign.Center)
                    }
                    BotModeSwitch(voiceMode, ::selectMode)
                    if (voiceMode) {
                        BotVoiceControls(voice.isListening, voice.isSpeaking, voice.isProcessing, voiceAllowed,
                            ::toggleMicrophone, onStopSpeaking = {
                            pendingSpokenReply = null
                            voice.cancel()
                        })
                    } else {
                        BotComposer(draft, onDraftChange = { draft = it.take(500) }, onSend = ::sendQuestion)
                    }
                }
            }
        }
    }
}

@Composable
private fun BotTopBar(onBack: () -> Unit, canReset: Boolean, onReset: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val buttonModifier = Modifier.size(48.dp)
        .shadow(4.dp, CircleShape, ambientColor = colors.primary.copy(alpha = .10f),
            spotColor = colors.primary.copy(alpha = .10f))
        .clip(CircleShape).background(colors.surface.copy(alpha = .92f))
        .border(1.dp, Color.White.copy(alpha = .8f), CircleShape)
    Row(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = buttonModifier) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onSurface)
        }
        Text("Green Bot", Modifier.weight(1f), fontFamily = DMSans, fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp, textAlign = TextAlign.Center, color = colors.onSurface)
        IconButton(onClick = onReset, enabled = canReset, modifier = buttonModifier) {
            Icon(Icons.Outlined.RestartAlt, "Start a new chat",
                tint = BotSecondaryText.copy(alpha = if (canReset) 1f else .35f))
        }
    }
}

@Composable
private fun BotHeading() {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("I'm your Green Bot", fontFamily = DMSans, fontSize = 16.sp, lineHeight = 24.sp,
            color = BotSecondaryText, textAlign = TextAlign.Center)
        Text("How can I help you?", fontFamily = DMSans, fontWeight = FontWeight.Medium,
            fontSize = 27.sp, lineHeight = 35.sp, color = colors.onSurface, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BotModeSwitch(voiceMode: Boolean, onModeChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(top = 8.dp, bottom = 16.dp).clip(CircleShape)
        .background(colors.surface.copy(alpha = .72f))
        .border(1.dp, Color.White.copy(alpha = .85f), CircleShape).padding(4.dp)) {
        listOf(false to "Chat", true to "Voice").forEach { (useVoice, label) ->
            val selected = useVoice == voiceMode
            Row(
                Modifier.widthIn(min = 96.dp).heightIn(min = 44.dp).clip(CircleShape)
                    .background(if (selected) colors.primary else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onModeChange(useVoice) })
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
            ) {
                Icon(if (useVoice) Icons.Outlined.Mic else Icons.AutoMirrored.Outlined.Chat,
                    null, Modifier.size(18.dp), tint = if (selected) colors.onPrimary else BotSecondaryText)
                Spacer(Modifier.width(7.dp))
                Text(label, fontFamily = DMSans, fontSize = 13.sp,
                    color = if (selected) colors.onPrimary else BotSecondaryText)
            }
        }
    }
}

@Composable
private fun BotVoiceControls(
    listening: Boolean, speaking: Boolean, processing: Boolean, enabled: Boolean,
    onMicrophone: () -> Unit, onStopSpeaking: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledIconButton(
                onClick = onMicrophone, enabled = enabled && !processing, modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.primary, contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = .18f),
                    disabledContentColor = BotSecondaryText)
            ) {
                if (processing) {
                    CircularProgressIndicator(Modifier.size(26.dp), color = colors.primary, strokeWidth = 2.dp)
                } else {
                    Icon(if (listening) Icons.Outlined.StopCircle else Icons.Outlined.Mic,
                        if (listening) "Finish speaking" else "Start voice input", Modifier.size(30.dp))
                }
            }
            if (speaking) {
                IconButton(onClick = onStopSpeaking) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeOff, "Stop spoken reply", tint = BotSecondaryText)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(when {
            listening -> "Tap to finish"
            processing -> "Getting your words ready"
            speaking -> "Tap the mic to speak again"
            else -> "Tap to speak"
        }, fontFamily = DMSans, fontSize = 12.sp, color = BotSecondaryText)
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun BotVoiceReply(question: String, reply: GreenBotReply, onAction: (GreenBotAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(20.dp), color = colors.surface.copy(alpha = .76f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .85f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("You: $question", fontFamily = DMSans, fontSize = 12.sp, lineHeight = 18.sp,
                color = BotSecondaryText)
            SelectionContainer {
                Text(reply.text, fontFamily = DMSans, fontSize = 14.sp, lineHeight = 22.sp, color = colors.onSurface)
            }
            reply.action?.let { BotActionButton(it, onAction) }
        }
    }
}

@Composable
private fun BotConversation(question: String, reply: GreenBotReply, onAction: (GreenBotAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(Modifier.align(Alignment.End).fillMaxWidth(.88f),
            shape = RoundedCornerShape(20.dp, 20.dp, 5.dp, 20.dp), color = colors.primary) {
            SelectionContainer {
                Text(question, Modifier.padding(horizontal = 16.dp, vertical = 13.dp), fontFamily = DMSans,
                    fontSize = 14.sp, lineHeight = 21.sp, color = colors.onPrimary)
            }
        }
        Surface(Modifier.fillMaxWidth(.94f).semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(5.dp, 20.dp, 20.dp, 20.dp), color = colors.surface.copy(alpha = .76f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .85f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("GREEN BOT", fontFamily = DMSans, fontSize = 9.sp, letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Medium, color = colors.primary)
                SelectionContainer {
                    Text(reply.text, fontFamily = DMSans, fontSize = 14.sp, lineHeight = 22.sp, color = colors.onSurface)
                }
                reply.action?.let { BotActionButton(it, onAction) }
            }
        }
    }
}

@Composable
private fun BotActionButton(action: GreenBotAction, onAction: (GreenBotAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(onClick = { onAction(action) }, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = .28f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Text(action.buttonLabel(), Modifier.weight(1f, fill = false), fontFamily = DMSans,
            fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.primary)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp), tint = colors.primary)
    }
}

@Composable
private fun BotComposer(draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(36.dp), color = colors.surface.copy(alpha = .88f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .9f)), shadowElevation = 2.dp) {
            Row(Modifier.padding(start = 22.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = draft, onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                        .semantics { contentDescription = "Message Green Bot" }, maxLines = 4,
                    textStyle = TextStyle(fontFamily = DMSans, fontSize = 15.sp, lineHeight = 22.sp, color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    decorationBox = { field ->
                        Box {
                            if (draft.isEmpty()) Text("Type your queries", fontFamily = DMSans,
                                fontSize = 15.sp, lineHeight = 22.sp, color = BotSecondaryText)
                            field()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onSend, enabled = draft.isNotBlank(), modifier = Modifier.size(50.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.primary, contentColor = colors.onPrimary,
                        disabledContainerColor = colors.primary.copy(alpha = .14f),
                        disabledContentColor = colors.primary.copy(alpha = .45f))) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send message", Modifier.size(24.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun GreenBotAction.buttonLabel(): String = when (this) {
    GreenBotAction.BOOK_FACILITY -> "Explore facilities"
    GreenBotAction.MY_BOOKINGS -> "Open my bookings"
    GreenBotAction.INVITE_VISITOR -> "Open visitor management"
    GreenBotAction.PAYMENTS -> "Payments & deposits"
    GreenBotAction.COMMUNITY -> "About my community"
    GreenBotAction.PROFILE -> "Open my profile"
}
