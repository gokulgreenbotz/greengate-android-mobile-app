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

                session.connect()
                session.state.first { it.isTerminal }

                stateJob.cancel()
                viewJob.cancel()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("ReceiverCallController", "Error in call session", e)
                _state.value = ReceiverCallState.Ended(EndReason.PROVIDER_ERROR, e.message)
            } finally {
                audioController.deactivate()
                _remoteView.value = null
                activeSession = null
            }
        }
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

    fun unlockGate() {
        scope.launch {
            _gateUnlockedMessage.value = "Gate Unlocked Successfully!"
            delay(3000L)
            _gateUnlockedMessage.value = null
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
        callJob?.cancel()
        audioController.deactivate()
        activeSession = null
        activeCredentials = null
        _remoteView.value = null
        _isMuted.value = false
        _isCameraOff.value = false
    }
}
