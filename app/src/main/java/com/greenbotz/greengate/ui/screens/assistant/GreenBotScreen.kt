package com.greenbotz.greengate.ui.screens.assistant

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.greenbotz.greengate.core.assistant.GreenBotAction
import com.greenbotz.greengate.core.assistant.GreenBotReply
import com.greenbotz.greengate.core.assistant.askGreenBot
import com.greenbotz.greengate.core.assistant.replyToGreenBot
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.AppTheme
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.navigation.Screen
import com.greenbotz.greengate.ui.screens.facilities.BookingsBackdrop
import com.greenbotz.greengate.ui.theme.DMSans
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// Keep secondary text readable over the pale mint glass without changing its theme hue.
private val BotSecondaryText: Color
    @Composable get() = MaterialTheme.colorScheme.let { lerp(it.onSurfaceVariant, it.onSurface, .12f) }

private enum class BotPanel { INTRO, CHAT, VOICE }

/** How long the concierge waits for its opening greeting to start before listening anyway. */
private const val GreetingStartTimeoutMs = 5_000L

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
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var draft by rememberSaveable { mutableStateOf("") }
    var questions by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    // One entry per answered question; the action is its enum name, or "" for no button.
    var replies by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var replyActions by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val waitingForReply = replies.size < questions.size
    fun replyAt(index: Int): GreenBotReply? = replies.getOrNull(index)?.let { text ->
        GreenBotReply(text, GreenBotAction.entries.find { it.name == replyActions.getOrNull(index) })
    }
    // Claude answers only when the server has a key; otherwise everything works as it always did.
    val claudeOn = ResidentSession.greenBotOnline
    LaunchedEffect(Unit) { ResidentSession.checkGreenBot() }
    var revealedQuestions by rememberSaveable { mutableIntStateOf(questions.size) }
    // Theme 4 opens on its voice concierge (see below); the other themes open on chat.
    var voiceMode by rememberSaveable { mutableStateOf(AppPreferences.theme == AppTheme.FOUR) }
    var permissionError by remember { mutableStateOf<String?>(null) }
    var pendingSpokenReply by remember { mutableStateOf<String?>(null) }
    var spokenReplyIndex by remember { mutableIntStateOf(-1) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val panelEntrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { panelEntrance.animateTo(1f, tween(480, easing = FastOutSlowInEasing)) }

    /** Adds a question. Without Claude the built-in guide answers at once, as before. */
    fun addQuestion(question: String, speak: Boolean) {
        val answerNow = !ResidentSession.greenBotOnline && replies.size == questions.size
        questions = ArrayList(questions).apply { add(question) }
        if (!answerNow) return
        val reply = replyToGreenBot(question)
        replies = ArrayList(replies).apply { add(reply.text) }
        replyActions = ArrayList(replyActions).apply { add(reply.action?.name.orEmpty()) }
        if (speak) {
            spokenReplyIndex = questions.lastIndex
            pendingSpokenReply = reply.text
        }
    }

    val onRecognized by rememberUpdatedState<(String) -> Unit> { question ->
        if (voiceMode && voiceAllowed) {
            val trimmed = question.trim()
            // With Claude on, the reply is spoken when it arrives (see the reply effect below).
            if (trimmed.isNotEmpty()) addQuestion(trimmed, speak = true)
        }
    }
    val voice = remember(context.applicationContext) {
        GreenBotVoice(context.applicationContext) { onRecognized(it) }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (voiceMode && voiceAllowed && !showResetDialog && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                voice.startListening()
            }
        } else {
            permissionError = "Allow microphone access to speak with Green Bot. You can also switch to Chat and type."
        }
    }

    DisposableEffect(voice) {
        onDispose { voice.release() }
    }
    // Replies play on the media stream, so the volume keys should adjust it here instead of the ringer.
    DisposableEffect(context) {
        val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
        val previous = activity?.volumeControlStream
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
        onDispose { if (activity != null && previous != null) activity.volumeControlStream = previous }
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
    // Answers questions in order, one at a time. A question asked meanwhile waits its turn
    // without restarting the request in flight; a new chat cancels it; a recreated screen
    // picks up a question left unanswered.
    LaunchedEffect(replies.size, waitingForReply) {
        val index = replies.size
        if (index >= questions.size) return@LaunchedEffect
        val reply = askGreenBot(questions.subList(0, index + 1), replies)
        replies = ArrayList(replies).apply { add(reply.text) }
        replyActions = ArrayList(replyActions).apply { add(reply.action?.name.orEmpty()) }
        if (voiceMode && voiceAllowed && index == questions.lastIndex) {
            spokenReplyIndex = index
            pendingSpokenReply = reply.text
        }
    }
    LaunchedEffect(questions.size, voiceMode) {
        if (!voiceMode && questions.isNotEmpty()) listState.animateScrollToItem(questions.size)
    }

    fun sendQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        voice.cancel()
        pendingSpokenReply = null
        permissionError = null
        voice.clearError()
        addQuestion(trimmed, speak = false)
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
        if (!voiceAllowed || !voiceMode || voice.isProcessing) return
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
        focusManager.clearFocus()
        navController.popBackStack()
    }
    BackHandler(enabled = voiceAllowed && !keyboardVisible, onBack = ::goBack)

    fun copyReply(text: String) {
        ContextCompat.getSystemService(context, ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Green Bot reply", text))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            scope.launch { snackbar.showSnackbar("Reply copied") }
        }
    }

    fun readReply(index: Int, text: String) {
        if (!voiceAllowed) return
        pendingSpokenReply = null
        if (voice.isSpeaking && spokenReplyIndex == index) {
            voice.cancel()
        } else {
            spokenReplyIndex = index
            voice.speak(text)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Outlined.RestartAlt, null, tint = colors.primary) },
            title = { Text("Start a new chat?", fontFamily = DMSans) },
            text = { Text("This clears this conversation and your unsent message.", fontFamily = DMSans) },
            confirmButton = {
                TextButton(onClick = {
                    voice.cancel()
                    voice.clearError()
                    pendingSpokenReply = null
                    permissionError = null
                    questions = arrayListOf()
                    replies = arrayListOf()
                    replyActions = arrayListOf()
                    revealedQuestions = 0
                    draft = ""
                    spokenReplyIndex = -1
                    showResetDialog = false
                    focusManager.clearFocus()
                }) { Text("New chat", fontFamily = DMSans, color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Keep chatting", fontFamily = DMSans, color = colors.primary) }
            },
            containerColor = colors.surface.copy(alpha = 1f),
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Theme 4 - Navy & Gold has its own voice screen; "Type instead" switches to the chat below.
    if (AppPreferences.theme == AppTheme.FOUR && voiceMode) {
        // Opening the screen (the AI Chat button) greets the resident, then turns the microphone on, once.
        var autoListened by rememberSaveable { mutableStateOf(false) }
        var greeting by remember { mutableStateOf<ConciergeGreeting?>(null) }
        LaunchedEffect(Unit) {
            if (!autoListened) {
                autoListened = true
                if (voiceAllowed) {
                    val hello = conciergeGreeting()
                    greeting = hello
                    voice.speak(hello.spoken)
                    // Wait for it to start (the voice service may still be loading), then to end.
                    // Without a voice service it never starts, and listening begins after the wait.
                    val started = withTimeoutOrNull(GreetingStartTimeoutMs) {
                        snapshotFlow { voice.isSpeaking }.first { it }
                    } != null
                    if (started) snapshotFlow { voice.isSpeaking }.first { !it }
                    greeting = null
                }
                // Unless the resident already tapped the microphone or a suggestion during the greeting.
                if (questions.isEmpty() && !voice.isListening && !voice.isProcessing) toggleMicrophone()
            }
        }
        val last = questions.lastIndex
        GreenBotConcierge(
            listening = voice.isListening,
            processing = voice.isProcessing || waitingForReply,
            speaking = voice.isSpeaking,
            level = voice.level,
            partialText = voice.partialText,
            question = questions.lastOrNull(),
            reply = replyAt(last),
            replySpeaking = voice.isSpeaking && spokenReplyIndex == last,
            greeting = greeting?.takeIf { questions.isEmpty() },
            error = permissionError ?: voice.error,
            replyMuted = voice.replyMuted,
            privacyNotice = claudeOn,
            voiceAllowed = voiceAllowed,
            canReset = questions.isNotEmpty(),
            snackbar = snackbar,
            onBack = ::goBack,
            onMicrophone = ::toggleMicrophone,
            onStop = { pendingSpokenReply = null; voice.cancel() },
            onTypeInstead = { selectMode(false) },
            onPrompt = ::sendQuestion,
            onAction = ::openAction,
            onCopy = ::copyReply,
            onRead = { replyAt(last)?.let { readReply(last, it.text) } },
            onReset = { voice.cancel(); pendingSpokenReply = null; showResetDialog = true },
            onDismissError = { permissionError = null; voice.clearError() },
            onTurnUpVolume = voice::turnUpVolume,
            onOpenSettings = if (permissionError != null) ({
                voice.cancel()
                pendingSpokenReply = null
                context.startActivity(Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.packageName)
                ))
            }) else null
        )
        return
    }

    Box(
        Modifier.fillMaxSize().background(backdrop)
            .consumeWindowInsets(WindowInsets.navigationBars).imePadding()
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            BotTopBar(
                onClose = ::goBack,
                canReset = questions.isNotEmpty() || draft.isNotBlank(),
                speaking = voice.isSpeaking,
                onReset = {
                    voice.cancel()
                    pendingSpokenReply = null
                    focusManager.clearFocus()
                    showResetDialog = true
                }
            )
            BotModeSwitch(voiceMode, ::selectMode)
            Surface(
                Modifier.weight(1f).widthIn(max = 640.dp).fillMaxWidth().graphicsLayer {
                    alpha = panelEntrance.value
                    translationY = (1f - panelEntrance.value) * 14.dp.toPx()
                },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = colors.surface.copy(alpha = .48f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .72f))
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val panelHeight = maxHeight
                        val panelTarget = when {
                            voiceMode -> BotPanel.VOICE
                            questions.isEmpty() -> BotPanel.INTRO
                            else -> BotPanel.CHAT
                        }
                        AnimatedContent(
                            targetState = panelTarget, modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                (fadeIn(tween(300, delayMillis = 70)) + slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it / 24 })
                                    .togetherWith(fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 40 })
                                    .using(SizeTransform(clip = false))
                            },
                            label = "Assistant panel"
                        ) { shownPanel ->
                            val shownVoice = shownPanel == BotPanel.VOICE
                            Box(Modifier.fillMaxSize()) {
                                if (shownPanel != BotPanel.CHAT) {
                                    val compactVoice = shownVoice && questions.isNotEmpty() && !voice.isListening && !voice.isProcessing
                                    val targetOrbSize = if (compactVoice) 96.dp
                                        else if (shownVoice) (panelHeight * .34f).coerceIn(120.dp, 200.dp)
                                        else (panelHeight * .30f).coerceIn(100.dp, 176.dp)
                                    val orbSize by animateDpAsState(
                                        targetOrbSize, spring(dampingRatio = .9f, stiffness = Spring.StiffnessLow),
                                        label = "Assistant size"
                                    )
                                    Column(
                                        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                            .padding(horizontal = 24.dp, vertical = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        val orbProcessing = voice.isProcessing || (shownVoice && waitingForReply)
                                        // Theme 4's chat uses the same orb as its voice screen.
                                        if (AppPreferences.theme == AppTheme.FOUR) {
                                            ConciergeOrb(
                                                Modifier.size(orbSize), listening = voice.isListening,
                                                speaking = voice.isSpeaking, level = voice.level, processing = orbProcessing
                                            )
                                        } else {
                                            GreenBotOrb(
                                                Modifier.size(orbSize), listening = voice.isListening,
                                                speaking = voice.isSpeaking, level = voice.level, processing = orbProcessing
                                            )
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        BotHeading(shownVoice, voice.isListening, voice.isProcessing || (shownVoice && waitingForReply),
                                            voice.isSpeaking, compactVoice, privacyNotice = claudeOn)
                                        if (shownVoice) {
                                            if (voice.partialText.isNotBlank() && (voice.isListening || voice.isProcessing)) {
                                                Spacer(Modifier.height(20.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(18.dp),
                                                    color = colors.surface.copy(alpha = .85f),
                                                    border = BorderStroke(1.dp, colors.primary.copy(alpha = .09f))
                                                ) {
                                                    Text(
                                                        voice.partialText, Modifier.fillMaxWidth().padding(16.dp),
                                                        fontFamily = DMSans, fontSize = 16.sp, lineHeight = 24.sp,
                                                        textAlign = TextAlign.Center, color = colors.onSurface
                                                    )
                                                }
                                            } else if (questions.isNotEmpty()) {
                                                Spacer(Modifier.height(16.dp))
                                                val lastReply = replyAt(questions.lastIndex)
                                                BotVoiceReply(
                                                    questions.last(), lastReply,
                                                    ::openAction, ::copyReply,
                                                    speaking = voice.isSpeaking && spokenReplyIndex == questions.lastIndex,
                                                    onRead = { lastReply?.let { readReply(questions.lastIndex, it.text) } }
                                                )
                                            }
                                        } else if (!keyboardVisible && draft.isBlank()) {
                                            Spacer(Modifier.height(28.dp))
                                            Text(
                                                "A few things I can help with", Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                                fontFamily = DMSans, fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium, color = BotSecondaryText
                                            )
                                            GreenBotQuickPrompts { if (shownPanel == panelTarget) sendQuestion(it) }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState, modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
                                        verticalArrangement = Arrangement.spacedBy(24.dp)
                                    ) {
                                        itemsIndexed(questions, key = { index, _ -> "question-$index" }) { index, question ->
                                            val reply = replyAt(index)
                                            BotConversation(
                                                question, reply, ::openAction, ::copyReply,
                                                speaking = voice.isSpeaking && spokenReplyIndex == index,
                                                onRead = { reply?.let { readReply(index, it.text) } },
                                                announce = index == questions.lastIndex,
                                                animateEntry = index >= revealedQuestions,
                                                onEntryStarted = { if (index < questions.size) revealedQuestions = maxOf(revealedQuestions, index + 1) }
                                            )
                                        }
                                        item(key = "end") { Spacer(Modifier.height(4.dp)) }
                                    }
                                    val showLatest by remember { derivedStateOf { listState.canScrollForward } }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showLatest,
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { scope.launch { listState.animateScrollToItem(questions.size) } },
                                            shape = CircleShape,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = colors.surface.copy(alpha = 1f), contentColor = colors.primary
                                            ),
                                            elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 3.dp)
                                        ) {
                                            Icon(Icons.Rounded.ArrowDownward, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Latest message", fontFamily = DMSans, fontSize = 12.sp, color = colors.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val error = permissionError ?: voice.error
                    if (error == null && voice.replyMuted) {
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            GreenBotErrorNotice(
                                message = "Your media volume is off, so you can't hear Green Bot's reply.",
                                onDismiss = voice::clearError,
                                onTurnUpVolume = voice::turnUpVolume,
                                icon = Icons.AutoMirrored.Outlined.VolumeOff
                            )
                        }
                    }
                    if (error != null) {
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            GreenBotErrorNotice(
                                message = error,
                                onDismiss = { permissionError = null; voice.clearError() },
                                onUseKeyboard = if (voiceMode) ({ selectMode(false) }) else null,
                                onOpenSettings = if (permissionError != null) ({
                                    voice.cancel()
                                    pendingSpokenReply = null
                                    context.startActivity(Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + context.packageName)
                                    ))
                                }) else null
                            )
                        }
                    }
                    AnimatedContent(
                        targetState = voiceMode, modifier = Modifier.fillMaxWidth(),
                        transitionSpec = {
                            fadeIn(tween(240, delayMillis = 60)).togetherWith(fadeOut(tween(120)))
                                .using(SizeTransform(clip = false))
                        }, label = "Assistant controls"
                    ) { shownVoice ->
                        if (shownVoice) {
                            BotVoiceControls(
                                voice.isListening, voice.isSpeaking, voice.isProcessing, voiceAllowed && voiceMode,
                                ::toggleMicrophone,
                                onCancel = { pendingSpokenReply = null; voice.cancel() }
                            )
                        } else {
                            BotComposer(
                                draft, onDraftChange = { if (!voiceMode) draft = it.take(500) },
                                onSend = { if (!voiceMode) sendQuestion(draft) }, onVoice = { selectMode(true) },
                                enabled = !voiceMode, privacyNotice = claudeOn
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 112.dp)) { data ->
            Snackbar(shape = RoundedCornerShape(16.dp), containerColor = colors.onSurface) {
                Text(data.visuals.message, fontFamily = DMSans, color = colors.surface.copy(alpha = 1f))
            }
        }
    }
}

@Composable
private fun BotTopBar(onClose: () -> Unit, canReset: Boolean, speaking: Boolean, onReset: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotMark(Modifier.size(44.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Green Bot", fontFamily = DMSans, fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (speaking) "Reading your reply..." else "Your community guide",
                fontFamily = DMSans, fontSize = 12.sp, color = BotSecondaryText,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (canReset) {
            IconButton(onClick = onReset, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.RestartAlt, "Start a new chat", Modifier.size(22.dp), tint = BotSecondaryText)
            }
        }
        IconButton(
            onClick = onClose, modifier = Modifier.size(48.dp)
                .clip(CircleShape).background(colors.surface.copy(alpha = .85f))
                .border(1.dp, Color.White.copy(alpha = .8f), CircleShape)
        ) {
            Icon(Icons.Rounded.Close, "Close Green Bot", Modifier.size(22.dp), tint = colors.onSurface)
        }
    }
}

@Composable
private fun BotMark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(colors.primary, lerp(colors.primary, Color.White, .22f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(23.dp), tint = colors.onPrimary)
    }
}

@Composable
private fun BotHeading(
    voiceMode: Boolean, listening: Boolean, processing: Boolean, speaking: Boolean, compact: Boolean, privacyNotice: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val title = when {
        !voiceMode -> "How can I help you?"
        listening -> "I'm listening"
        processing -> "Got it. One moment..."
        speaking -> "Here's a little help"
        compact -> "Ready when you are"
        else -> "Let's talk"
    }
    val subtitle = when {
        !voiceMode -> "A little guidance for everyday life at home."
        listening -> "Speak naturally. Tap the mic when you're done."
        processing -> "Turning your words into a reply."
        speaking -> "You can stop the reply at any time."
        privacyNotice -> "Ask about the app. Please don't share personal details."
        else -> "Ask about your home or community."
    }
    Text(title, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        fontFamily = DMSans, fontWeight = FontWeight.SemiBold, fontSize = if (compact) 22.sp else 26.sp,
        lineHeight = if (compact) 30.sp else 34.sp, color = colors.onSurface, textAlign = TextAlign.Center)
    if (!compact) {
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontFamily = DMSans, fontSize = 14.sp, lineHeight = 21.sp,
            color = BotSecondaryText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BotModeSwitch(voiceMode: Boolean, onModeChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        Modifier.padding(horizontal = 24.dp).padding(bottom = 18.dp).widthIn(max = 360.dp)
            .fillMaxWidth().clip(CircleShape).background(colors.surface.copy(alpha = .55f))
            .border(1.dp, Color.White.copy(alpha = .75f), CircleShape)
            .selectableGroup().padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 2
        val selectionOffset by animateDpAsState(
            if (voiceMode) segmentWidth else 0.dp,
            spring(dampingRatio = .88f, stiffness = Spring.StiffnessMediumLow), label = "Selected mode"
        )
        Box(Modifier.matchParentSize()) {
            Box(Modifier.offset(x = selectionOffset).width(segmentWidth).fillMaxHeight()
                .background(colors.primary, CircleShape))
        }
        Row(Modifier.fillMaxWidth()) {
            listOf(false to "Chat", true to "Voice").forEach { (useVoice, label) ->
                val selected = useVoice == voiceMode
                val contentColor by animateColorAsState(
                    if (selected) colors.onPrimary else BotSecondaryText,
                    tween(220), label = "Mode label"
                )
                Row(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onModeChange(useVoice) })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                ) {
                    Icon(if (useVoice) Icons.Outlined.Mic else Icons.AutoMirrored.Outlined.Chat,
                        null, Modifier.size(18.dp), tint = contentColor)
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontFamily = DMSans, fontWeight = FontWeight.Medium,
                        fontSize = 14.sp, color = contentColor)
                }
            }
        }
    }
}

@Composable
private fun BotVoiceControls(
    listening: Boolean, speaking: Boolean, processing: Boolean, enabled: Boolean,
    onMicrophone: () -> Unit, onCancel: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 24.dp).padding(top = 10.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(100.dp).background(colors.primary.copy(alpha = .07f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            FilledIconButton(
                onClick = onMicrophone, enabled = enabled && !processing, modifier = Modifier.size(76.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.primary, contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = .18f),
                    disabledContentColor = colors.primary
                )
            ) {
                if (processing) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = colors.primary, strokeWidth = 2.dp)
                } else {
                    Icon(if (listening) Icons.Outlined.StopCircle else Icons.Outlined.Mic,
                        if (listening) "Finish speaking" else "Start voice input", Modifier.size(32.dp))
                }
            }
        }
        if (listening || processing || speaking) {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                if (speaking) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeOff, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (speaking) "Stop reply" else "Cancel", fontFamily = DMSans, fontSize = 13.sp, color = colors.primary)
            }
        } else {
            Text("Tap to speak", Modifier.padding(top = 8.dp), fontFamily = DMSans,
                fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.primary)
            Text("Microphone stays off until you tap.", Modifier.padding(top = 4.dp),
                fontFamily = DMSans, fontSize = 11.sp, color = BotSecondaryText, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun BotVoiceReply(
    question: String, reply: GreenBotReply?, onAction: (GreenBotAction) -> Unit,
    onCopy: (String) -> Unit, speaking: Boolean, onRead: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp), color = colors.surface.copy(alpha = .88f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .9f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("YOU ASKED", fontFamily = DMSans, fontSize = 9.sp, letterSpacing = 1.sp, color = BotSecondaryText)
            Text(question, Modifier.padding(top = 6.dp, bottom = 16.dp), fontFamily = DMSans,
                fontSize = 14.sp, lineHeight = 21.sp, color = BotSecondaryText)
            if (reply == null) {
                BotThinking()
            } else {
                SelectionContainer {
                    Text(reply.text, fontFamily = DMSans, fontSize = 15.sp, lineHeight = 24.sp, color = colors.onSurface)
                }
                reply.action?.let { Spacer(Modifier.height(12.dp)); BotActionButton(it, onAction) }
                BotReplyTools(speaking, onCopy = { onCopy(reply.text) }, onRead = onRead)
            }
        }
    }
}

@Composable
private fun BotConversation(
    question: String, reply: GreenBotReply?, onAction: (GreenBotAction) -> Unit,
    onCopy: (String) -> Unit, speaking: Boolean, onRead: () -> Unit, announce: Boolean,
    animateEntry: Boolean, onEntryStarted: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // Consume entry motion immediately so scrolling or changing modes never replays old messages.
    LaunchedEffect(Unit) { if (animateEntry) onEntryStarted() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        BotMessageEnter(animateEntry, modifier = Modifier.align(Alignment.End)) {
            Surface(
                Modifier.widthIn(min = 48.dp, max = 360.dp),
                shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp), color = colors.primary
            ) {
                SelectionContainer {
                    Text(question, Modifier.padding(horizontal = 18.dp, vertical = 14.dp), fontFamily = DMSans,
                        fontSize = 15.sp, lineHeight = 23.sp, color = colors.onPrimary)
                }
            }
        }
        BotMessageEnter(animateEntry, delayMillis = 90, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 2.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotMark(Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Green Bot", fontFamily = DMSans, fontSize = 12.sp,
                        fontWeight = FontWeight.Medium, color = BotSecondaryText)
                }
                Surface(
                    Modifier.fillMaxWidth().then(if (announce) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
                    shape = RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp),
                    color = colors.surface.copy(alpha = .9f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .9f))
                ) {
                    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = if (reply == null) 18.dp else 4.dp)) {
                        if (reply == null) {
                            BotThinking()
                        } else {
                            SelectionContainer {
                                Text(reply.text, fontFamily = DMSans, fontSize = 15.sp, lineHeight = 24.sp, color = colors.onSurface)
                            }
                            reply.action?.let { Spacer(Modifier.height(14.dp)); BotActionButton(it, onAction) }
                            BotReplyTools(speaking, onCopy = { onCopy(reply.text) }, onRead = onRead)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotThinking() {
    Text("Thinking...", fontFamily = DMSans, fontSize = 15.sp, lineHeight = 24.sp, color = BotSecondaryText)
}

@Composable
private fun BotMessageEnter(
    animate: Boolean, modifier: Modifier = Modifier, delayMillis: Int = 0, content: @Composable () -> Unit
) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            progress.animateTo(1f, tween(380, delayMillis, FastOutSlowInEasing))
        }
    }
    Box(modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 14.dp.toPx()
        scaleX = .985f + .015f * progress.value
        scaleY = scaleX
    }) { content() }
}

@Composable
private fun BotReplyTools(speaking: Boolean, onCopy: () -> Unit, onRead: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = onCopy, contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = BotSecondaryText)
        ) {
            Icon(Icons.Outlined.ContentCopy, null, Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Copy", fontFamily = DMSans, fontSize = 12.sp, color = BotSecondaryText)
        }
        TextButton(
            onClick = onRead, contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = if (speaking) colors.primary else BotSecondaryText)
        ) {
            Icon(if (speaking) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (speaking) "Stop" else "Listen", fontFamily = DMSans, fontSize = 12.sp,
                color = if (speaking) colors.primary else BotSecondaryText)
        }
    }
}

@Composable
private fun BotActionButton(action: GreenBotAction, onAction: (GreenBotAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = { onAction(action) }, shape = RoundedCornerShape(14.dp),
        modifier = Modifier.heightIn(min = 48.dp),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = .18f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.primary, containerColor = colors.primary.copy(alpha = .035f)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(action.buttonLabel(), Modifier.weight(1f, fill = false), fontFamily = DMSans,
            fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.primary)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp))
    }
}

@Composable
private fun BotComposer(
    draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit, onVoice: () -> Unit, enabled: Boolean,
    privacyNotice: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().background(colors.background.copy(alpha = .55f))
            .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
            color = colors.surface.copy(alpha = 1f),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = .12f)),
            shadowElevation = 2.dp
        ) {
            Row(
                Modifier.padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BasicTextField(
                    value = draft, onValueChange = onDraftChange, enabled = enabled,
                    modifier = Modifier.weight(1f).padding(vertical = 14.dp)
                        .semantics { contentDescription = "Message Green Bot" },
                    maxLines = 4,
                    textStyle = TextStyle(fontFamily = DMSans, fontSize = 15.sp, lineHeight = 22.sp, color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    decorationBox = { field ->
                        Box {
                            if (draft.isEmpty()) Text("Ask Green Bot...", fontFamily = DMSans,
                                fontSize = 15.sp, lineHeight = 22.sp, color = BotSecondaryText)
                            field()
                        }
                    }
                )
                IconButton(onClick = onVoice, enabled = enabled, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Mic, "Switch to voice mode", Modifier.size(22.dp), tint = BotSecondaryText)
                }
                FilledIconButton(
                    onClick = onSend, enabled = enabled && draft.isNotBlank(), modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.primary, contentColor = colors.onPrimary,
                        disabledContainerColor = colors.primary.copy(alpha = .10f),
                        disabledContentColor = colors.primary.copy(alpha = .4f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send message", Modifier.size(21.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (privacyNotice) "Please don't share personal details" else "Your guide to GreenGate",
                fontFamily = DMSans, fontSize = 10.sp, color = BotSecondaryText)
            if (draft.length >= 400) {
                Text(draft.length.toString() + "/500", fontFamily = DMSans, fontSize = 10.sp,
                    color = if (draft.length == 500) colors.error else BotSecondaryText)
            }
        }
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
