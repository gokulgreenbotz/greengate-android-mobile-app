package com.greenbotz.greengate.core.call

import android.content.Context
import android.util.Log
import android.view.View
import com.greenbotz.greengate.core.call.media.ReceiverAudioController
import com.greenbotz.greengate.core.call.provider.EndReason
import com.greenbotz.greengate.core.call.provider.ReceiverCallCredentials
import com.greenbotz.greengate.core.call.provider.ReceiverCallSession
import com.greenbotz.greengate.core.call.provider.ReceiverCallSessionFactory
import com.greenbotz.greengate.core.call.provider.ReceiverCallState
import com.greenbotz.greengate.core.push.IncomingCallNotifier
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

    /** Calls the resident chose to skip: never ring for these again. */
    private val skippedCallIds = LinkedHashSet<String>()

    /**
     * May be called from the relay polling thread. Everything runs on the main
     * thread: tearing down an old session (LiveKit views / Room) or starting a
     * preview from a background thread races the SDK's main-thread callbacks.
     */
    fun triggerIncomingCall(credentials: ReceiverCallCredentials, ringTimeoutMs: Long = 50_000L) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            triggerIncomingCallOnMain(credentials, ringTimeoutMs)
        } else {
            mainHandler.post { triggerIncomingCallOnMain(credentials, ringTimeoutMs) }
        }
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun triggerIncomingCallOnMain(credentials: ReceiverCallCredentials, ringTimeoutMs: Long) {
        if (credentials.callId in skippedCallIds) {
            Log.i("ReceiverCallController", "Ignoring skipped call ${credentials.callId}")
            return
        }
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
            // Matches the kiosk's ring time (was a fixed 30 s while the kiosk
            // rang for 60 s, so the phone gave up first — QA #7).
            delay(ringTimeoutMs)
            if (_state.value is ReceiverCallState.Incoming) {
                declineCall(EndReason.RING_TIMEOUT)
            }
        }

        startPreview(credentials)
    }

    // ------------------------------------------------- ringing-time preview

    /** Session that joined while ringing (LiveKit); answerCall() reuses it. */
    private var previewSession: ReceiverCallSession? = null
    private var previewCallId: String? = null
    private var previewJob: Job? = null

    /**
     * "Instant answer": for providers that support it, join the call right
     * now — subscribe-only, nothing published — so the ringing screen shows
     * the visitor live and Answer only has to switch the mic/camera on.
     * May be called from the signaling thread; all work hops to main.
     */
    private fun startPreview(credentials: ReceiverCallCredentials) {
        scope.launch {
            if (activeCredentials?.callId != credentials.callId ||
                _state.value !is ReceiverCallState.Incoming
            ) return@launch
            if (previewCallId == credentials.callId) return@launch

            val session = runCatching { ReceiverCallSessionFactory.create(appContext, credentials) }
                .getOrNull() ?: return@launch
            if (!session.supportsPreview) {
                // Twilio / Vonage: nothing to pre-join; answerCall() connects as before.
                return@launch
            }
            previewSession = session
            previewCallId = credentials.callId
            activeSession = session

            previewJob = scope.launch {
                // Visitor video for the ringing screen.
                launch { session.remoteView.collect { v -> _remoteView.value = v } }
                // Kiosk says another phone answered (instant path, over the call).
                launch {
                    session.appMessages.collect { raw ->
                        val msg = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return@collect
                        if (msg.optString("type") == "ANSWERED") {
                            onAnsweredElsewhere(credentials.callId, msg.optString("by"))
                        }
                    }
                }
                // Kiosk left / gave up while we were still ringing → stop ringing.
                launch {
                    session.state.first { it is ReceiverCallState.Ended }
                    val ended = session.state.value as ReceiverCallState.Ended
                    if (_state.value is ReceiverCallState.Incoming && previewSession === session) {
                        if (ended.reason == EndReason.REMOTE_HANGUP) {
                            Log.i("ReceiverCallController", "Kiosk left during ringing — ending")
                            endedByKiosk()
                        } else {
                            // Preview join failed (network, token…): keep ringing;
                            // Answer will connect from scratch.
                            Log.w("ReceiverCallController", "Preview failed (${ended.cause}) — will connect on answer")
                            dropPreview()
                        }
                    }
                }
            }
            session.startPreview()
            Log.i("ReceiverCallController", "Preview join started for call ${credentials.callId}")
        }
    }

    /** Forget the preview session (does not tear it down). */
    private fun dropPreview() {
        previewJob?.cancel()
        previewJob = null
        if (activeSession === previewSession) activeSession = null
        previewSession = null
        previewCallId = null
        _remoteView.value = null
    }

    fun answerCall() {
        val creds = activeCredentials ?: run {
            Log.e("ReceiverCallController", "Cannot answer call: credentials missing")
            return
        }

        ringTimeoutJob?.cancel()
        // Answered: remove the incoming-call notification.
        IncomingCallNotifier.cancel(appContext)
        onCallAnswered?.invoke(creds.callId)
        _state.value = ReceiverCallState.Connecting

        // Reuse the ringing-time preview connection if there is one: Answer
        // then only turns the mic/camera on (LiveKit "instant answer").
        val preview = previewSession?.takeIf { previewCallId == creds.callId }
        previewJob?.cancel()
        previewJob = null
        previewSession = null
        previewCallId = null

        callJob = scope.launch {
            audioController.activate()
            try {
                val session = preview ?: ReceiverCallSessionFactory.create(appContext, creds)
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
                val callId = creds.callId
                // QA #6: if the visitor left before we answered (and the "call
                // ended" message was missed), we'd sit in an empty call. If the
                // kiosk's video never shows up, end it instead of hanging.
                val kioskWatchdog = launch {
                    // The kiosk joins the call as soon as it starts ringing, so
                    // once we're connected its video should be there within a
                    // few seconds. If not, the visitor has already left.
                    delay(10_000L)
                    if (_remoteView.value == null && activeCredentials?.callId == callId &&
                        !_state.value.isTerminal
                    ) {
                        Log.w("ReceiverCallController", "Kiosk never joined call $callId — ending")
                        endedByKiosk()
                    }
                }
                // First answer wins (all providers): tell the kiosk which phone
                // this is. If another phone got there first, the kiosk answers
                // ANSWERED(by=other) and onAnsweredElsewhere() leaves quietly.
                val claimJob = launch {
                    session.state.first { it is ReceiverCallState.Connected }
                    val claim = org.json.JSONObject()
                        .put("type", "CLAIM")
                        .put("device", com.greenbotz.greengate.core.call.provider.livekit.LiveKitConfig.residentIdentity(appContext))
                        .toString()
                    for (wait in longArrayOf(300L, 1200L, 2500L)) {
                        delay(wait)
                        session.sendAppMessage(claim)
                    }
                }
                // Once the kiosk is on, tell it our mic/camera state (QA #2).
                val mediaStateJob = launch {
                    session.remoteView.first { it != null }
                    delay(1500L)
                    sendMediaState()
                }

                session.connect()
                session.state.first { it.isTerminal }

                stateJob.cancel()
                claimJob.cancel()
                kioskWatchdog.cancel()
                mediaStateJob.cancel()
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

    /**
     * Tells the kiosk whether our mic / camera are on, so it can show
     * "Resident's microphone is muted" (QA #2). Needed for Vonage; a harmless
     * duplicate of Twilio's own event.
     */
    private fun sendMediaState() {
        activeSession?.sendAppMessage(
            org.json.JSONObject()
                .put("type", "MEDIA")
                .put("audio", !_isMuted.value)
                .put("video", !_isCameraOff.value)
                .toString()
        )
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        activeSession?.setMicrophoneEnabled(!newMute)
        sendMediaState()
    }

    fun toggleCamera() {
        val newCam = !_isCameraOff.value
        _isCameraOff.value = newCam
        activeSession?.setCameraEnabled(!newCam)
        sendMediaState()
    }

    var onCallEndedSignal: ((String?) -> Unit)? = null

    /**
     * Preferred over [onCallEndedSignal]: also says WHY (DECLINED, NO_ANSWER,
     * HANGUP) so the kiosk can show "not answered" instead of just resetting.
     */
    var onCallEndedWithReason: ((callId: String?, reason: String) -> Unit)? = null

    private fun notifyKioskEnded(callId: String?, reason: String) {
        val withReason = onCallEndedWithReason
        if (withReason != null) withReason(callId, reason) else onCallEndedSignal?.invoke(callId)
    }

    /** Told the callId when this phone answers, so the household's other phones can stop ringing. */
    var onCallAnswered: ((String?) -> Unit)? = null

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
            "ANSWERED" -> activeCredentials?.callId?.let { onAnsweredElsewhere(it, msg.optString("by")) }
        }
    }

    /**
     * The kiosk's CALL_ENDED arrived over the relay (visitor cancelled while we
     * were ringing, or the kiosk gave up). Only for the call we are on.
     * Runs on the main thread; never echoes a hang-up back to the kiosk.
     */
    fun onKioskEndedCall(callId: String) {
        scope.launch {
            if (activeCredentials?.callId != callId) return@launch
            endedByKiosk()
        }
    }

    /**
     * First answer wins. The kiosk reports that a phone answered [callId]
     * ([answeredBy] = that phone's LiveKit identity, may be blank).
     *  - Still ringing here → stop ringing and hide the incoming screen,
     *    WITHOUT telling the kiosk anything (no decline).
     *  - We also pressed Answer at the same moment but the other phone won
     *    (LiveKit only) → leave quietly, again without a hang-up message.
     * Called from any thread; safe to call twice (relay + in-call message).
     */
    fun onAnsweredElsewhere(callId: String, answeredBy: String) {
        scope.launch {
            val creds = activeCredentials ?: return@launch
            if (creds.callId != callId) return@launch
            val me = runCatching {
                com.greenbotz.greengate.core.call.provider.livekit.LiveKitConfig.residentIdentity(appContext)
            }.getOrNull()
            val s = _state.value
            when {
                s is ReceiverCallState.Incoming -> {
                    Log.i("ReceiverCallController", "Call $callId answered on another phone ($answeredBy) — stop ringing")
                    skippedCallIds.add(callId)
                    cleanup() // disconnects the preview, removes the notification
                    _state.value = ReceiverCallState.Idle
                }
                answeredBy.isNotBlank() && me != null && answeredBy != me && !s.isTerminal -> {
                    Log.i("ReceiverCallController", "Call $callId taken by $answeredBy (we are $me) — leaving quietly")
                    skippedCallIds.add(callId)
                    cleanup()
                    _state.value = ReceiverCallState.Idle
                }
            }
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
        notifyKioskEnded(callId, if (reason == EndReason.RING_TIMEOUT) "NO_ANSWER" else "DECLINED")
        activeSession?.disconnect(reason)
        _state.value = ReceiverCallState.Ended(reason)
        cleanup()
    }

    fun hangUp() {
        ringTimeoutJob?.cancel()
        val callId = activeCredentials?.callId
        // Instant notice over the call; the relay message is the backup.
        activeSession?.sendAppMessage("{\"type\":\"HANGUP\"}")
        notifyKioskEnded(callId, "HANGUP")
        activeSession?.disconnect(EndReason.LOCAL_HANGUP)
        _state.value = ReceiverCallState.Ended(EndReason.LOCAL_HANGUP)
        cleanup()
    }

    /**
     * "Skip": hide the ringing screen and stop the ringtone WITHOUT answering or
     * declining. The kiosk is not told anything — it keeps ringing until its
     * own no-answer timeout. The same call will not ring on this phone again.
     */
    fun skipCall() {
        val callId = activeCredentials?.callId ?: return
        if (_state.value !is ReceiverCallState.Incoming) return
        Log.i("ReceiverCallController", "Resident skipped call $callId")
        skippedCallIds.add(callId)
        while (skippedCallIds.size > 20) skippedCallIds.remove(skippedCallIds.first())
        cleanup() // also removes the incoming-call notification
        _state.value = ReceiverCallState.Idle
    }

    fun resetToIdle() {
        cleanup()
        _state.value = ReceiverCallState.Idle
    }

    private fun cleanup() {
        ringTimeoutJob?.cancel()
        // Declined, hung up, timed out, kiosk ended, or reset: the
        // incoming-call notification must not outlive the call.
        IncomingCallNotifier.cancel(appContext)
        // FIX: always tear the provider session down. Before, a new incoming
        // call (or resetToIdle) just dropped the reference, leaving the old
        // Twilio/Vonage connection — mic and all — alive in the room.
        activeSession?.let { s -> runCatching { s.disconnect(EndReason.LOCAL_HANGUP) } }
        // A ringing-time preview connection must never outlive the ringing.
        previewSession?.takeIf { it !== activeSession }
            ?.let { s -> runCatching { s.disconnect(EndReason.LOCAL_HANGUP) } }
        previewJob?.cancel()
        previewJob = null
        previewSession = null
        previewCallId = null
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
