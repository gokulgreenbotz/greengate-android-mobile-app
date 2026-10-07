package com.example.greengate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** A single, explicitly started voice turn. No automatic or background listening. */
internal class GreenBotVoice(
    context: Context,
    private val onRecognized: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    var isListening by mutableStateOf(false)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var isSpeaking by mutableStateOf(false)
        private set
    var level by mutableFloatStateOf(0f)
        private set
    var partialText by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var released = false
    private var recognizer: SpeechRecognizer? = null
    private var recognitionVersion = 0L
    private var activeRecognition: Long? = null
    private var recognitionTimeout: Runnable? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInitializing = false
    private var ttsInitializationVersion = 0L
    private var ttsInitializationTimeout: Runnable? = null
    private var pendingSpeech: String? = null
    private var speechVersion = 0L
    private val activeUtterances = mutableSetOf<String>()

    init {
        onMain { initializeSpeechOutput() }
    }

    fun startListening() = onMain {
        if (released || isProcessing) return@onMain
        stopSpeechOutput()
        closeRecognition()
        error = null
        partialText = ""
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            error = "Allow microphone access to use voice mode."
            return@onMain
        }

        val session = ++recognitionVersion
        try {
            val service = createRecognizer()
            if (service == null) {
                error = "Voice input is unavailable on this device. Install or enable a speech recognition service."
                return@onMain
            }
            recognizer = service
            activeRecognition = session
            service.setRecognitionListener(listenerFor(session))
            val locale = Locale.getDefault().takeIf { it.language == "en" } ?: Locale.US
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            isListening = true
            setRecognitionTimeout(session, MAX_LISTENING_MS) { finishListening() }
            service.startListening(request)
        } catch (_: SecurityException) {
            closeRecognition()
            error = "Allow microphone access to use voice mode."
        } catch (_: RuntimeException) {
            closeRecognition()
            error = "The speech recognition service couldn't start. Try again or use the keyboard."
        }
    }

    /** Stops microphone capture while allowing the service to return the final transcript. */
    fun finishListening() = onMain {
        if (released || activeRecognition == null || isProcessing) return@onMain
        isListening = false
        level = 0f
        isProcessing = true
        val session = activeRecognition ?: return@onMain
        try {
            recognizer?.stopListening()
            waitForTranscript(session)
        } catch (_: SecurityException) {
            closeRecognition()
            error = "Allow microphone access to use voice mode."
        } catch (_: RuntimeException) {
            closeRecognition()
            error = "The speech recognition service stopped unexpectedly. Try again."
        }
    }

    /** Discards any unfinished transcript and pending speech, including late service callbacks. */
    fun cancel() = onMain {
        if (released) return@onMain
        closeRecognition()
        stopSpeechOutput()
        partialText = ""
    }

    fun speak(text: String) = onMain {
        if (released) return@onMain
        val reply = text.trim()
        if (reply.isEmpty()) return@onMain
        closeRecognition()
        stopSpeechOutput()
        error = null
        pendingSpeech = reply
        when {
            ttsReady -> speakPendingReply()
            ttsInitializing -> Unit
            // Retry on a new request: the user may have enabled a voice service in settings.
            else -> initializeSpeechOutput()
        }
    }

    fun clearError() = onMain {
        if (!released) error = null
    }

    fun release() = onMain {
        if (released) return@onMain
        released = true
        closeRecognition()
        stopSpeechOutput()
        ttsReady = false
        ttsInitializing = false
        ttsInitializationVersion++
        clearSpeechInitializationTimeout()
        val engine = tts
        tts = null
        runCatching { engine?.shutdown() }
    }

    private fun createRecognizer(): SpeechRecognizer? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)) {
                    return SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
                }
            } catch (_: RuntimeException) {
                // Some devices advertise an on-device service that cannot actually be bound.
            }
        }
        return if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } else null
    }

    private fun listenerFor(session: Long) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = updateRecognition(session) { level = 0f }
        override fun onBeginningOfSpeech() = updateRecognition(session) { level = .15f }
        override fun onRmsChanged(rmsdB: Float) = updateRecognition(session) {
            if (isListening) level = if (rmsdB.isFinite()) ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) else 0f
        }
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = updateRecognition(session) {
            isListening = false
            level = 0f
            isProcessing = true
            waitForTranscript(session)
        }
        override fun onError(errorCode: Int) = updateRecognition(session) {
            closeRecognition()
            error = recognitionError(errorCode)
        }
        override fun onResults(results: Bundle?) = updateRecognition(session) {
            val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull { it.isNotBlank() }?.trim()
            // Invalidate the session before delivering the result, even if the callback starts a new turn.
            closeRecognition()
            if (transcript == null) {
                error = "I didn't catch that. Tap the microphone and try again."
            } else {
                partialText = transcript
                error = null
                onRecognized(transcript)
            }
        }
        override fun onPartialResults(partialResults: Bundle?) = updateRecognition(session) {
            partialText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
        }
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun updateRecognition(session: Long, block: () -> Unit) = onMain {
        if (!released && activeRecognition == session) block()
    }

    private fun waitForTranscript(session: Long) {
        setRecognitionTimeout(session, RESULTS_TIMEOUT_MS) {
            closeRecognition()
            error = "Voice input timed out. Tap the microphone and try again."
        }
    }

    private fun setRecognitionTimeout(session: Long, delay: Long, block: () -> Unit) {
        if (released || activeRecognition != session) return
        recognitionTimeout?.let(mainHandler::removeCallbacks)
        val timeout = Runnable {
            if (!released && activeRecognition == session) block()
        }
        recognitionTimeout = timeout
        mainHandler.postDelayed(timeout, delay)
    }

    private fun closeRecognition() {
        activeRecognition = null
        recognitionVersion++
        recognitionTimeout?.let(mainHandler::removeCallbacks)
        recognitionTimeout = null
        isProcessing = false
        isListening = false
        level = 0f
        val service = recognizer
        recognizer = null
        runCatching { service?.cancel() }
        runCatching { service?.destroy() }
    }

    private fun initializeSpeechOutput() {
        if (released || ttsInitializing || ttsReady) return
        ttsInitializing = true
        val initialization = ++ttsInitializationVersion
        val timeout = Runnable {
            if (!released && ttsInitializing && initialization == ttsInitializationVersion) {
                failSpeechOutput("The voice service didn't respond. Try reopening voice mode.")
            }
        }
        ttsInitializationTimeout = timeout
        mainHandler.postDelayed(timeout, RESULTS_TIMEOUT_MS)
        try {
            tts = TextToSpeech(appContext) { status ->
                // Always post: some engines invoke initialization before the constructor returns.
                mainHandler.post {
                    if (!released && ttsInitializing && initialization == ttsInitializationVersion) {
                        finishSpeechInitialization(status)
                    }
                }
            }
        } catch (_: RuntimeException) {
            failSpeechOutput("Spoken replies are unavailable. Enable a text-to-speech service in device settings.")
        }
    }

    private fun finishSpeechInitialization(status: Int) {
        ttsInitializing = false
        clearSpeechInitializationTimeout()
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            failSpeechOutput("Spoken replies are unavailable. Enable a text-to-speech service in device settings.")
            return
        }
        try {
            val preferred = Locale.getDefault().takeIf { it.language == "en" }
            val languages = listOfNotNull(preferred, Locale.forLanguageTag("en-IN"), Locale.US, Locale.UK, Locale.ENGLISH)
                .distinct()
            val supported = languages.firstOrNull { engine.setLanguage(it) >= TextToSpeech.LANG_AVAILABLE }
            if (supported == null) {
                failSpeechOutput("Install an English voice in your device's text-to-speech settings to hear replies.")
                return
            }
            val listenerResult = engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = updateUtterance(utteranceId) { isSpeaking = true }
                override fun onDone(utteranceId: String?) = completeUtterance(utteranceId)
                override fun onStop(utteranceId: String?, interrupted: Boolean) = completeUtterance(utteranceId)
                @Deprecated("Required by the platform listener")
                override fun onError(utteranceId: String?) = speechError(utteranceId, TextToSpeech.ERROR)
                override fun onError(utteranceId: String?, errorCode: Int) = speechError(utteranceId, errorCode)
            })
            if (listenerResult != TextToSpeech.SUCCESS) {
                failSpeechOutput("The voice service couldn't prepare spoken replies. Try reopening voice mode.")
                return
            }
            ttsReady = true
            speakPendingReply()
        } catch (_: RuntimeException) {
            failSpeechOutput("The voice service couldn't prepare spoken replies. Try reopening voice mode.")
        }
    }

    private fun speakPendingReply() {
        val reply = pendingSpeech ?: return
        val engine = tts ?: return
        pendingSpeech = null
        val version = ++speechVersion
        val chunks = reply.chunked(TextToSpeech.getMaxSpeechInputLength())
        try {
            chunks.forEachIndexed { index, chunk ->
                val utterance = "green-bot-$version-$index"
                activeUtterances.add(utterance)
                val result = engine.speak(chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, utterance)
                if (result != TextToSpeech.SUCCESS) {
                    stopSpeechOutput()
                    error = "The voice service couldn't read this reply. You can read it in the chat."
                    return
                }
            }
            isSpeaking = activeUtterances.isNotEmpty()
        } catch (_: RuntimeException) {
            stopSpeechOutput()
            error = "The voice service stopped unexpectedly. You can read the reply in the chat."
        }
    }

    private fun updateUtterance(utteranceId: String?, block: () -> Unit) = onMain {
        if (!released && utteranceId != null && utteranceId in activeUtterances) block()
    }

    private fun completeUtterance(utteranceId: String?) = updateUtterance(utteranceId) {
        activeUtterances.remove(utteranceId)
        if (activeUtterances.isEmpty()) isSpeaking = false
    }

    private fun speechError(utteranceId: String?, errorCode: Int) = updateUtterance(utteranceId) {
        stopSpeechOutput()
        error = when (errorCode) {
            TextToSpeech.ERROR_NETWORK, TextToSpeech.ERROR_NETWORK_TIMEOUT -> "The voice service needs a network connection to read this reply."
            TextToSpeech.ERROR_NOT_INSTALLED_YET -> "The selected voice is still downloading. Try again after it finishes."
            else -> "The voice service couldn't read this reply. You can read it in the chat."
        }
    }

    private fun stopSpeechOutput() {
        pendingSpeech = null
        activeUtterances.clear()
        speechVersion++
        isSpeaking = false
        runCatching { tts?.stop() }
    }

    private fun failSpeechOutput(message: String) {
        val requestedReply = pendingSpeech != null
        stopSpeechOutput()
        ttsInitializing = false
        ttsInitializationVersion++
        clearSpeechInitializationTimeout()
        ttsReady = false
        val engine = tts
        tts = null
        runCatching { engine?.shutdown() }
        if (requestedReply) error = message
    }

    private fun clearSpeechInitializationTimeout() {
        ttsInitializationTimeout?.let(mainHandler::removeCallbacks)
        ttsInitializationTimeout = null
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun recognitionError(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access to use voice mode."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Tap the microphone when you're ready."
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Tap the microphone and try again."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice input couldn't connect. Check your connection and try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The microphone service is busy. Wait a moment and try again."
        SpeechRecognizer.ERROR_AUDIO -> "The microphone couldn't capture audio. Check whether another app is using it."
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "The voice service received too many requests. Wait a moment and try again."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "This speech service doesn't support English. Try a different voice service in device settings."
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "English voice input isn't downloaded yet. Download it in your device's speech settings."
        SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "The speech recognition service is unavailable. Try again or use the keyboard."
        else -> "Voice input stopped unexpectedly. Tap the microphone and try again."
    }

    private companion object {
        const val MAX_LISTENING_MS = 30_000L
        const val RESULTS_TIMEOUT_MS = 8_000L
    }
}
