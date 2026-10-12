package com.example.greengate.core.call

import android.content.Context
import android.util.Log
import android.view.View
import com.example.greengate.core.call.media.ReceiverAudioController
import com.example.greengate.core.call.provider.EndReason
import com.example.greengate.core.call.provider.ReceiverCallCredentials
import com.example.greengate.core.call.provider.ReceiverCallSession
import com.example.greengate.core.call.provider.ReceiverCallSessionFactory
import com.example.greengate.core.call.provider.ReceiverCallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReceiverCallController(
    private val appContext: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _state = MutableStateFlow<ReceiverCallState>(ReceiverCallState.Idle)
    val state: StateFlow<ReceiverCallState> = _state.asStateFlow()

    private val _remoteView = MutableStateFlow<View?>(null)
    val remoteView: StateFlow<View?> = _remoteView.asStateFlow()

    /** Resident's own camera preview (null until the camera is running). */
    private val _localView = MutableStateFlow<View?>(null)
    val localView: StateFlow<View?> = _localView.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isCameraOff = MutableStateFlow(false)
    val isCameraOff: StateFlow<Boolean> = _isCameraOff.asStateFlow()

    private val _gateUnlockedMessage = MutableStateFlow<String?>(null)
    val gateUnlockedMessage: StateFlow<String?> = _gateUnlockedMessage.asStateFlow()

    private val audioController = ReceiverAudioController(appContext)
    private var activeSession: ReceiverCallSession? = null
    private var activeCredentials: ReceiverCallCredentials? = null
    private var callJob: Job? = null
    private var ringTimeoutJob: Job? = null

    val currentCredentials: ReceiverCallCredentials? get() = activeCredentials

    fun triggerIncomingCall(credentials: ReceiverCallCredentials) {
        // If a new call arrives with a different callId, teardown any stale prior call
        if (credentials.callId != activeCredentials?.callId) {
            cleanup()
            _state.value = ReceiverCallState.Idle
        } else if (_state.value != ReceiverCallState.Idle && !_state.value.isTerminal) {
            Log.w("ReceiverCallController", "Ignored duplicate call trigger for callId=${credentials.callId}")
            return
        }

        activeCredentials = credentials
        _state.value = ReceiverCallState.Incoming(credentials)

        ringTimeoutJob?.cancel()
        ringTimeoutJob = scope.launch {
            delay(30_000L) // 30s ring timeout
            if (_state.value is ReceiverCallState.Incoming) {
                declineCall(EndReason.RING_TIMEOUT)
            }
        }
    }

    fun answerCall() {
        val creds = activeCredentials ?: run {
            Log.e("ReceiverCallController", "Cannot answer call: credentials missing")
            return
        }

        ringTimeoutJob?.cancel()
        // Answered: remove the incoming-call notification.
        com.example.greengate.core.call.signaling.IncomingCallNotifier.cancel(appContext)
        _state.value = ReceiverCallState.Connecting

        callJob = scope.launch {
            audioController.activate()
            try {
                val session = ReceiverCallSessionFactory.create(appContext, creds)
                activeSession = session

                val stateJob = launch {
                    session.state.collect { s ->
                        _state.value = s
                    }
                }
                val viewJob = launch {
                    session.remoteView.collect { v ->
                        _remoteView.value = v
                    }
                }
                val localViewJob = launch {
                    session.localView.collect { v -> _localView.value = v }
                }
                // Fast path from the kiosk: gate result and hang-up.
                val messageJob = launch {
                    session.appMessages.collect { raw -> handleKioskMessage(raw) }
                }

                session.connect()
                session.state.first { it.isTerminal }

                stateJob.cancel()
                viewJob.cancel()
                localViewJob.cancel()
                messageJob.cancel()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("ReceiverCallController", "Error in call session", e)
                _state.value = ReceiverCallState.Ended(EndReason.PROVIDER_ERROR, e.message)
            } finally {
                audioController.deactivate()
                _remoteView.value = null
                _localView.value = null
                activeSession = null
            }
        }
    }

    /** Forwarded from MainActivity.onStop (screen off, home, app switch). */
    fun onAppBackgrounded() {
        activeSession?.onAppBackgrounded()
    }

    /** Forwarded from MainActivity.onStart: resume the camera stream. */
    fun onAppForegrounded() {
        activeSession?.onAppForegrounded()
    }

    fun createLocalView(context: Context): View? {
        return activeSession?.createLocalView(context)
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        activeSession?.setMicrophoneEnabled(!newMute)
    }

    fun toggleCamera() {
        val newCam = !_isCameraOff.value
        _isCameraOff.value = newCam
        activeSession?.setCameraEnabled(!newCam)
    }

    var onCallEndedSignal: ((String?) -> Unit)? = null

    /** Set by MainActivity: sends the open-gate request to the kiosk (callId, endCall). */
    var onOpenGateRequested: ((String?, Boolean, String) -> Unit)? = null

    /** id of the latest gate request, and the id we already showed success for. */
    private var lastGateRequestId: String? = null
    private var lastGateEndCall = false
    private var successShownFor: String? = null

    private var unlockPending = false
    private var pendingEndCall = false
    private var unlockTimeoutJob: Job? = null
    private var bannerJob: Job? = null

    /**
     * "Open Gate" ([endCall] = false) keeps the call going;
     * "Open Gate & End Call" ([endCall] = true) hangs up once the kiosk
     * confirms the gate opened. Taps while a request is in flight are ignored.
     */
    fun openGate(endCall: Boolean) {
        val callId = activeCredentials?.callId ?: return
        if (unlockPending) return
        unlockPending = true
        pendingEndCall = endCall
        val requestId = "gate-${System.currentTimeMillis()}-${(1000..9999).random()}"
        lastGateRequestId = requestId
        lastGateEndCall = endCall
        bannerJob?.cancel()
        _gateUnlockedMessage.value = "Opening gate…"

        // Fast path: straight to the kiosk over the call (well under a second).
        val sentInCall = activeSession?.sendAppMessage(
            org.json.JSONObject()
                .put("type", "OPEN_GATE")
                .put("id", requestId)
                .put("endCall", endCall)
                .toString()
        ) == true
        Log.i("ReceiverCallController", "OPEN_GATE id=$requestId endCall=$endCall sentInCall=$sentInCall")
        // Backup path: the relay, with the SAME id so the kiosk opens only once.
        onOpenGateRequested?.invoke(callId, endCall, requestId)

        unlockTimeoutJob?.cancel()
        unlockTimeoutJob = scope.launch {
            delay(12_000L)
            if (unlockPending) onDoorUnlockResult(false, "The gate didn't respond. Try again.", requestId)
        }
    }

    /** Messages from the kiosk over the call: {"type":"GATE_RESULT"|"HANGUP", ...}. */
    private fun handleKioskMessage(raw: String) {
        val msg = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return
        when (msg.optString("type")) {
            "GATE_RESULT" -> onDoorUnlockResult(
                success = msg.optBoolean("success", false),
                message = msg.optString("message"),
                requestId = msg.optString("id").ifBlank { null }
            )
            "HANGUP" -> endedByKiosk()
        }
    }

    /** Kiosk ended the call: tear down locally without echoing a hang-up back. */
    private fun endedByKiosk() {
        if (activeCredentials == null) return
        Log.i("ReceiverCallController", "Kiosk ended the call")
        ringTimeoutJob?.cancel()
        activeSession?.disconnect(EndReason.REMOTE_HANGUP)
        _state.value = ReceiverCallState.Ended(EndReason.REMOTE_HANGUP)
        cleanup()
    }

    /** Kept for older callers: same as "Open Gate" without ending the call. */
    fun unlockGate() = openGate(endCall = false)

    /** Called (from any thread) when the kiosk reports the door result. */
    fun onDoorUnlockResult(success: Boolean, message: String?, requestId: String? = null) {
        scope.launch {
            // The result can arrive twice (in-call + relay) and, on a slow
            // relay, after our timeout. Ignore results for other requests and
            // repeats — but DO show a late success even after "didn't respond".
            if (requestId != null && requestId != lastGateRequestId) return@launch
            if (success && requestId != null && successShownFor == requestId) return@launch
            if (!success && !unlockPending) return@launch
            if (success) successShownFor = requestId

            // lastGateEndCall (not pendingEndCall) so a late success for
            // "Open & End Call" still ends the call.
            val endAfter = success && lastGateEndCall
            unlockPending = false
            pendingEndCall = false
            unlockTimeoutJob?.cancel()
            _gateUnlockedMessage.value = if (success) {
                "Gate opened"
            } else {
                message?.ifBlank { null } ?: "Couldn't open the gate"
            }
            bannerJob?.cancel()
            bannerJob = scope.launch {
                if (endAfter) {
                    // Brief confirmation, then hang up (hangUp() tells the kiosk).
                    delay(800L)
                    _gateUnlockedMessage.value = null
                    if (activeCredentials != null) hangUp()
                } else {
                    delay(3000L)
                    _gateUnlockedMessage.value = null
                }
            }
        }
    }

    fun declineCall(reason: EndReason = EndReason.DECLINED) {
        ringTimeoutJob?.cancel()
        val callId = activeCredentials?.callId
        onCallEndedSignal?.invoke(callId)
        activeSession?.disconnect(reason)
        _state.value = ReceiverCallState.Ended(reason)
        cleanup()
    }

    fun hangUp() {
        ringTimeoutJob?.cancel()
        val callId = activeCredentials?.callId
        // Instant notice over the call; the relay message is the backup.
        activeSession?.sendAppMessage("{\"type\":\"HANGUP\"}")
        onCallEndedSignal?.invoke(callId)
        activeSession?.disconnect(EndReason.LOCAL_HANGUP)
        _state.value = ReceiverCallState.Ended(EndReason.LOCAL_HANGUP)
        cleanup()
    }

    fun resetToIdle() {
        cleanup()
        _state.value = ReceiverCallState.Idle
    }

    private fun cleanup() {
        ringTimeoutJob?.cancel()
        // Declined, hung up, timed out, kiosk ended, or reset: the
        // incoming-call notification must not outlive the call.
        com.example.greengate.core.call.signaling.IncomingCallNotifier.cancel(appContext)
        // FIX: always tear the provider session down. Before, a new incoming
        // call (or resetToIdle) just dropped the reference, leaving the old
        // Twilio/Vonage connection — mic and all — alive in the room.
        activeSession?.let { s -> runCatching { s.disconnect(EndReason.LOCAL_HANGUP) } }
        callJob?.cancel()
        audioController.deactivate()
        activeSession = null
        activeCredentials = null
        _remoteView.value = null
        _localView.value = null
        unlockPending = false
        pendingEndCall = false
        unlockTimeoutJob?.cancel()
        _isMuted.value = false
        _isCameraOff.value = false
    }
}
