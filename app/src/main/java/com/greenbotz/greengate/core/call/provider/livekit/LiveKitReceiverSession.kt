package com.greenbotz.greengate.core.call.provider.livekit

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.View
import com.greenbotz.greengate.core.call.provider.EndReason
import com.greenbotz.greengate.core.call.provider.ReceiverCallCredentials
import com.greenbotz.greengate.core.call.provider.ReceiverCallSession
import com.greenbotz.greengate.core.call.provider.ReceiverCallState
import io.livekit.android.AudioOptions
import io.livekit.android.LiveKit
import io.livekit.android.LiveKitOverrides
import io.livekit.android.RoomOptions
import io.livekit.android.audio.NoAudioHandler
import io.livekit.android.events.DisconnectReason
import io.livekit.android.events.RoomEvent
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.participant.AudioTrackPublishDefaults
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.participant.VideoTrackPublishDefaults
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.DataPublishReliability
import io.livekit.android.room.track.LocalAudioTrackOptions
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.RemoteAudioTrack
import io.livekit.android.room.track.RemoteVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoCaptureParameter
import io.livekit.android.room.track.VideoEncoding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import livekit.org.webrtc.RendererCommon
import livekit.org.webrtc.VideoSink

/**
 * LiveKit Cloud implementation of [ReceiverCallSession] — with "instant answer".
 *
 * 1. Ringing → [startPreview]: join the room subscribe-only. Nothing is
 *    published (no mic, no camera, the kiosk sees no answer) and the kiosk's
 *    audio is held at volume 0 under the ringtone, but the kiosk VIDEO plays
 *    on the ringing screen.
 * 2. Answer → [connect]: the connection already exists, so we only unmute
 *    the kiosk audio and publish the mic (the visitor hears the resident in a
 *    few hundred ms), then the camera.
 *
 * If the preview join has not finished (or failed) when Answer is tapped,
 * [connect] waits for / redoes the join, so it is never slower than a plain
 * connect.
 *
 * Must be driven from the main thread.
 */
class LiveKitReceiverSession(
    context: Context,
    private val credentials: ReceiverCallCredentials.LiveKit
) : ReceiverCallSession {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow<ReceiverCallState>(ReceiverCallState.Idle)
    override val state: StateFlow<ReceiverCallState> = _state.asStateFlow()

    private val _remoteView = MutableStateFlow<View?>(null)
    override val remoteView: StateFlow<View?> = _remoteView.asStateFlow()

    private val _localView = MutableStateFlow<View?>(null)
    override val localView: StateFlow<View?> = _localView.asStateFlow()

    private val _appMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val appMessages: SharedFlow<String> = _appMessages

    override val supportsPreview: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sendScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastSendAt = 0L
    private var room: Room? = null
    private var eventsJob: Job? = null
    private var joinJob: Job? = null
    private var joined = false

    /** False until Answer: the kiosk's audio stays silent under the ringtone. */
    private var answered = false
    private val remoteAudioTracks = mutableListOf<RemoteAudioTrack>()

    private var remoteVideoTrack: RemoteVideoTrack? = null
    private var remoteVideoView: TextureViewRenderer? = null

    private var localVideo: LocalVideoTrack? = null
    private var localPreview: TextureViewRenderer? = null
    @Volatile private var firstFrameSeen = false
    private var cameraWatchdogRestarts = 0

    private var isMicEnabled = true
    private var isCameraEnabled = true
    private var backgrounded = false

    @Volatile private var torndown = false

    private val frameWatcher = VideoSink { _ ->
        if (!firstFrameSeen) {
            firstFrameSeen = true
            scope.launch {
                if (!torndown) {
                    Log.i(TAG, "first local camera frame")
                    _localView.value = localPreview
                }
            }
        }
    }

    // --------------------------------------------------------------- preview

    override fun startPreview() {
        if (torndown || joinJob != null) return
        joinJob = scope.launch { join(preview = true) }
    }

    private suspend fun join(preview: Boolean) {
        val started = SystemClock.elapsedRealtime()
        val r = room ?: createRoom().also { room = it }
        eventsJob = eventsJob ?: scope.launch { r.events.events.collect { onRoomEvent(it) } }
        try {
            r.connect(credentials.serverUrl, credentials.token)
            joined = true
            Log.i(TAG, "joined ${credentials.roomName} (${if (preview) "preview" else "answer"}) " +
                "in ${SystemClock.elapsedRealtime() - started} ms")
            // Kiosk may already be publishing: render what is there.
            r.remoteParticipants.values.forEach { bindExisting(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "join failed (${if (preview) "preview" else "answer"}): ${t.message}")
            // Throw the broken room away; Answer will start a fresh one.
            runCatching { r.release() }
            eventsJob?.cancel()
            eventsJob = null
            room = null
            if (!preview) throw t
        }
    }

    private fun createRoom(): Room = LiveKit.create(
        appContext = appContext,
        options = RoomOptions(
            adaptiveStream = false,
            dynacast = false,
            audioTrackCaptureDefaults = LocalAudioTrackOptions(
                noiseSuppression = true,
                echoCancellation = true,
                autoGainControl = true,
                highPassFilter = true,
                typingNoiseDetection = false
            ),
            videoTrackCaptureDefaults = LocalVideoTrackOptions(
                position = CameraPosition.FRONT,
                captureParams = VideoCaptureParameter(640, 480, 24)
            ),
            audioTrackPublishDefaults = AudioTrackPublishDefaults(dtx = true, red = true),
            videoTrackPublishDefaults = VideoTrackPublishDefaults(
                videoEncoding = VideoEncoding(maxBitrate = 800_000, maxFps = 24),
                simulcast = false
            )
        ),
        overrides = LiveKitOverrides(
            // ReceiverAudioController owns audio mode / speaker / focus.
            audioOptions = AudioOptions(audioHandler = NoAudioHandler())
        )
    )

    // ---------------------------------------------------------------- answer

    override suspend fun connect() {
        if (torndown) {
            // Kiosk left (or the room closed) while we were ringing.
            markEnded(EndReason.REMOTE_HANGUP, "session already closed")
            return
        }
        _state.value = ReceiverCallState.Connecting
        val started = SystemClock.elapsedRealtime()
        answered = true
        try {
            // Preview join still running → wait for it; never joined → join now.
            joinJob?.join()
            if (torndown) { markEnded(EndReason.REMOTE_HANGUP, "closed during join"); return }
            if (!joined || room == null) join(preview = false)
            val r = room ?: throw IllegalStateException("no room after join")

            // 1) Kiosk audio audible, 2) our mic live → the visitor hears us.
            remoteAudioTracks.forEach { runCatching { it.setVolume(1.0) } }
            r.localParticipant.setMicrophoneEnabled(isMicEnabled)
            if (torndown) { markEnded(EndReason.REMOTE_HANGUP, "closed while answering"); return }
            _state.value = ReceiverCallState.Connected(System.currentTimeMillis())
            Log.i(TAG, "answered: audio live ${SystemClock.elapsedRealtime() - started} ms after tap")

            // 3) Camera (opening it takes longer; the call is already live).
            startCamera(r)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "answer failed", t)
            if (_state.value !is ReceiverCallState.Ended) {
                _state.value = ReceiverCallState.Ended(EndReason.PROVIDER_ERROR, t.message)
            }
            release()
        }
    }

    private suspend fun startCamera(r: Room) {
        if (!isCameraEnabled) return
        try {
            r.localParticipant.setCameraEnabled(true)
            val track = r.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack
                ?: return
            localVideo = track
            val preview = localPreview ?: TextureViewRenderer(appContext).also { view ->
                r.initVideoRenderer(view)
                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                // Not mirrored (QA #1): same image the kiosk shows.
                view.setMirror(false)
                localPreview = view
            }
            track.addRenderer(preview)
            track.addRenderer(frameWatcher)
            armCameraWatchdog()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "camera start failed: ${t.message}")
        }
    }

    private fun armCameraWatchdog() {
        scope.launch {
            delay(4_000L)
            if (torndown || firstFrameSeen || !isCameraEnabled || backgrounded) return@launch
            if (cameraWatchdogRestarts >= 2) {
                Log.e(TAG, "camera produced no frames after 2 restarts")
                return@launch
            }
            cameraWatchdogRestarts++
            Log.w(TAG, "no camera frames — restarting camera (#$cameraWatchdogRestarts)")
            restartCapture()
            armCameraWatchdog()
        }
    }

    private fun restartCapture() {
        val track = localVideo ?: return
        runCatching { track.stopCapture() }
        runCatching { track.startCapture() }
            .onFailure { Log.w(TAG, "camera restart failed: ${it.message}") }
    }

    // ----------------------------------------------------------------- events

    private fun onRoomEvent(event: RoomEvent) {
        if (torndown) return
        when (event) {
            // Only the KIOSK's media: when another family phone answers and
            // publishes, a phone still ringing must keep showing the visitor.
            is RoomEvent.TrackSubscribed -> if (isKiosk(event.participant)) when (val track = event.track) {
                is RemoteVideoTrack -> attachRemoteVideo(track)
                is RemoteAudioTrack -> {
                    remoteAudioTracks += track
                    track.setVolume(if (answered) 1.0 else 0.0)
                }
                else -> Unit
            }

            is RoomEvent.TrackUnsubscribed -> {
                val track = event.track
                if (track is RemoteAudioTrack) remoteAudioTracks.remove(track)
                if (track is RemoteVideoTrack && track === remoteVideoTrack) detachRemoteVideo()
            }

            is RoomEvent.DataReceived -> {
                val message = runCatching { String(event.data, Charsets.UTF_8) }.getOrNull() ?: return
                Log.d(TAG, "in-call message: $message")
                _appMessages.tryEmit(message)
            }

            is RoomEvent.ParticipantDisconnected -> {
                val who = event.participant.identity?.value.orEmpty()
                Log.d(TAG, "participant left: $who")
                // Only the KIOSK leaving ends the call; other family phones
                // previewing/declining come and go.
                if (who.startsWith(LiveKitConfig.KIOSK_IDENTITY_PREFIX)) {
                    if (_state.value !is ReceiverCallState.Ended) {
                        _state.value = ReceiverCallState.Ended(EndReason.REMOTE_HANGUP)
                    }
                    release()
                }
            }

            is RoomEvent.Reconnecting -> Log.d(TAG, "reconnecting…")
            is RoomEvent.Reconnected -> Log.d(TAG, "reconnected")

            is RoomEvent.Disconnected -> {
                Log.d(TAG, "disconnected: ${event.reason}")
                // A join that failed before it ever succeeded is handled by
                // join(): a failed PREVIEW must not end the ringing.
                if (!joined) return
                if (_state.value !is ReceiverCallState.Ended) {
                    _state.value = ReceiverCallState.Ended(
                        when (event.reason) {
                            DisconnectReason.ROOM_DELETED,
                            DisconnectReason.ROOM_CLOSED,
                            DisconnectReason.PARTICIPANT_REMOVED,
                            DisconnectReason.CLIENT_INITIATED -> EndReason.REMOTE_HANGUP
                            DisconnectReason.SIGNAL_CLOSE,
                            DisconnectReason.CONNECTION_TIMEOUT,
                            DisconnectReason.MEDIA_FAILURE -> EndReason.NETWORK_LOST
                            else -> EndReason.PROVIDER_ERROR
                        },
                        event.error?.message ?: event.reason.name
                    )
                }
                release()
            }

            else -> Unit
        }
    }

    private fun isKiosk(participant: RemoteParticipant): Boolean =
        participant.identity?.value.orEmpty().startsWith(LiveKitConfig.KIOSK_IDENTITY_PREFIX)

    private fun bindExisting(participant: RemoteParticipant) {
        if (!isKiosk(participant)) return
        participant.trackPublications.values.forEach { pub ->
            when (val t = pub.track) {
                is RemoteVideoTrack -> attachRemoteVideo(t)
                is RemoteAudioTrack -> {
                    if (t !in remoteAudioTracks) remoteAudioTracks += t
                    t.setVolume(if (answered) 1.0 else 0.0)
                }
                else -> Unit
            }
        }
    }

    private fun attachRemoteVideo(track: RemoteVideoTrack) {
        if (track === remoteVideoTrack) return
        detachRemoteVideo()
        val r = room ?: return
        val view = TextureViewRenderer(appContext)
        r.initVideoRenderer(view)
        view.setMirror(false)
        track.addRenderer(view)
        remoteVideoTrack = track
        remoteVideoView = view
        _remoteView.value = view
    }

    private fun detachRemoteVideo() {
        val view = remoteVideoView
        if (view != null) {
            runCatching { remoteVideoTrack?.removeRenderer(view) }
            runCatching { view.release() }
        }
        remoteVideoTrack = null
        remoteVideoView = null
        _remoteView.value = null
    }

    // --------------------------------------------------------------- controls

    override fun sendAppMessage(message: String): Boolean {
        val r = room ?: return false
        if (r.state != Room.State.CONNECTED) return false
        lastSendAt = SystemClock.elapsedRealtime()
        // Own scope: a "HANGUP" sent right before disconnect() must survive it.
        sendScope.launch {
            r.localParticipant.publishData(message.toByteArray(Charsets.UTF_8), DataPublishReliability.RELIABLE)
                .onFailure { Log.w(TAG, "publishData failed: ${it.message}") }
        }
        return true
    }

    override fun createLocalView(context: Context): View? = _localView.value

    override fun setMicrophoneEnabled(enabled: Boolean) {
        isMicEnabled = enabled
        val r = room ?: return
        if (!answered) return
        scope.launch { runCatching { r.localParticipant.setMicrophoneEnabled(enabled) } }
    }

    override fun setCameraEnabled(enabled: Boolean) {
        isCameraEnabled = enabled
        val r = room ?: return
        if (!answered) return
        val pub = r.localParticipant.getTrackPublication(Track.Source.CAMERA)
        if (pub == null) {
            if (enabled) scope.launch { startCamera(r) }
            return
        }
        // Mute = kiosk shows "camera off"; also stop the sensor to save power.
        pub.muted = !enabled
        if (enabled) restartCapture() else runCatching { localVideo?.stopCapture() }
    }

    override fun onAppBackgrounded() {
        if (torndown || !answered) return
        backgrounded = true
        // Android revokes the camera in the background: tell the kiosk the
        // video is off instead of leaving the visitor on a frozen frame.
        room?.localParticipant?.getTrackPublication(Track.Source.CAMERA)?.muted = true
        runCatching { localVideo?.stopCapture() }
        Log.i(TAG, "app backgrounded — local video paused")
    }

    override fun onAppForegrounded() {
        if (torndown || !backgrounded) return
        backgrounded = false
        if (!isCameraEnabled) return
        firstFrameSeen = false
        _localView.value = null // "Starting camera…" until a fresh frame
        restartCapture()
        room?.localParticipant?.getTrackPublication(Track.Source.CAMERA)?.muted = false
        armCameraWatchdog()
    }

    // --------------------------------------------------------------- teardown

    override fun disconnect(reason: EndReason) {
        if (_state.value !is ReceiverCallState.Ended) _state.value = ReceiverCallState.Ended(reason)
        release()
    }

    /** Terminal state for the controller, which waits on `first { it.isTerminal }`. */
    private fun markEnded(reason: EndReason, cause: String?) {
        if (_state.value !is ReceiverCallState.Ended) _state.value = ReceiverCallState.Ended(reason, cause)
    }

    private fun release() {
        if (torndown) return
        torndown = true
        joinJob?.cancel()
        detachRemoteVideo()
        localVideo?.let { track ->
            runCatching { track.removeRenderer(frameWatcher) }
            localPreview?.let { runCatching { track.removeRenderer(it) } }
            runCatching { track.stopCapture() }
        }
        localPreview?.let { runCatching { it.release() } }
        localPreview = null
        localVideo = null
        _localView.value = null
        remoteAudioTracks.clear()
        // release() = disconnect (unpublishes + disposes our tracks) + free
        // resources — slightly delayed if an in-call message was just sent.
        val r = room
        room = null
        if (r != null) {
            val wait = SEND_GRACE_MS - (SystemClock.elapsedRealtime() - lastSendAt)
            val doRelease = Runnable { runCatching { r.release() }.onFailure { Log.w(TAG, "room.release failed", it) } }
            if (wait > 0) mainHandler.postDelayed(doRelease, wait) else doRelease.run()
        }
        eventsJob?.cancel()
        scope.cancel()
    }

    private companion object {
        const val TAG = "LiveKitReceiverSession"
        const val SEND_GRACE_MS = 400L
    }
}
