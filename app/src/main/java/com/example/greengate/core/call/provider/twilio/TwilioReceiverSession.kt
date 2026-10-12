package com.example.greengate.core.call.provider.twilio

import android.content.Context
import android.util.Log
import android.view.View
import com.example.greengate.core.call.provider.EndReason
import com.example.greengate.core.call.provider.ReceiverCallCredentials
import com.example.greengate.core.call.provider.ReceiverCallSession
import com.example.greengate.core.call.provider.ReceiverCallState
import com.twilio.video.AudioTrack
import com.twilio.video.CameraCapturer
import com.twilio.video.ConnectOptions
import com.twilio.video.LocalAudioTrack
import com.twilio.video.LocalDataTrack
import com.twilio.video.LocalVideoTrack
import com.twilio.video.RemoteAudioTrack
import com.twilio.video.RemoteAudioTrackPublication
import com.twilio.video.RemoteDataTrack
import com.twilio.video.RemoteDataTrackPublication
import com.twilio.video.RemoteParticipant
import com.twilio.video.RemoteVideoTrack
import com.twilio.video.RemoteVideoTrackPublication
import com.twilio.video.Room
import com.twilio.video.TwilioException
import com.twilio.video.Video
import com.twilio.video.VideoView
import com.twilio.video.VideoTrack
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tvi.webrtc.Camera1Enumerator

class TwilioReceiverSession(
    private val context: Context,
    private val credentials: ReceiverCallCredentials.Twilio
) : ReceiverCallSession {

    private val _state = MutableStateFlow<ReceiverCallState>(ReceiverCallState.Idle)
    override val state: StateFlow<ReceiverCallState> = _state.asStateFlow()

    private val _remoteView = MutableStateFlow<View?>(null)
    override val remoteView: StateFlow<View?> = _remoteView.asStateFlow()

    private var room: Room? = null
    private var localAudioTrack: LocalAudioTrack? = null
    private var localVideoTrack: LocalVideoTrack? = null
    private var remoteVideoTrack: RemoteVideoTrack? = null
    private var remoteVideoView: VideoView? = null

    private var isMicEnabled = true
    private var isCameraEnabled = true

    /** Set once we decide this call is over; any late onConnected must leave. */
    @Volatile private var torndown = false

    /** In-call message channel to the kiosk (gate requests / results, hang-up). */
    private var localDataTrack: LocalDataTrack? = null
    private val _appMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val appMessages: SharedFlow<String> = _appMessages
    private val dataListener = object : RemoteDataTrack.Listener {
        override fun onMessage(remoteDataTrack: RemoteDataTrack, messageBuffer: java.nio.ByteBuffer) = Unit
        override fun onMessage(remoteDataTrack: RemoteDataTrack, message: String) {
            Log.d("TwilioReceiverSession", "in-call message: $message")
            _appMessages.tryEmit(message)
        }
    }

    override fun sendAppMessage(message: String): Boolean {
        val track = localDataTrack ?: return false
        if (room?.state != Room.State.CONNECTED) return false
        return runCatching { track.send(message) }.isSuccess
    }

    private val _localView = MutableStateFlow<View?>(null)
    override val localView: StateFlow<View?> = _localView.asStateFlow()
    private var localPreview: VideoView? = null
    private var cameraId: String? = null
    private var backgrounded = false

    override suspend fun connect() {
        _state.value = ReceiverCallState.Connecting
        Log.i("TwilioReceiverSession", "connecting to room=${credentials.roomName}")

        try {
            // Create local audio track
            localAudioTrack = LocalAudioTrack.create(context, true)

            // Create local video track
            val enumerator = Camera1Enumerator()
            val frontCameraId = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()

            cameraId = frontCameraId
            if (frontCameraId != null) {
                val cameraCapturer = CameraCapturer(context, frontCameraId)
                localVideoTrack = LocalVideoTrack.create(context, isCameraEnabled, cameraCapturer)
            }
            // Own-camera preview, published as soon as the track exists so the
            // in-call screen can show it immediately.
            localVideoTrack?.let { track ->
                // applyZOrder(true): the preview is a SurfaceView drawn on top of
                // the full-screen kiosk video (also a SurfaceView). Without it
                // Android may draw the preview *behind* the remote video, so
                // the resident never saw their own camera.
                val preview = VideoView(context).apply {
                    mirror = true
                    applyZOrder(true)
                }
                track.addSink(preview)
                localPreview = preview
                _localView.value = preview
            }

            val builder = ConnectOptions.Builder(credentials.accessToken)
                .roomName(credentials.roomName)

            localAudioTrack?.let { builder.audioTracks(listOf(it)) }
            localVideoTrack?.let { builder.videoTracks(listOf(it)) }
            localDataTrack = LocalDataTrack.create(context)
            localDataTrack?.let { builder.dataTracks(listOf(it)) }

            val connectOptions = builder.build()

            val remoteParticipantListener = object : RemoteParticipant.Listener {
                override fun onAudioTrackPublished(participant: RemoteParticipant, publication: RemoteAudioTrackPublication) {}
                override fun onAudioTrackUnpublished(participant: RemoteParticipant, publication: RemoteAudioTrackPublication) {}
                override fun onAudioTrackSubscribed(participant: RemoteParticipant, publication: RemoteAudioTrackPublication, audioTrack: RemoteAudioTrack) {
                    Log.d("TwilioReceiverSession", "Remote audio track subscribed from ${participant.identity}")
                    audioTrack.enablePlayback(true)
                }
                override fun onAudioTrackSubscriptionFailed(participant: RemoteParticipant, publication: RemoteAudioTrackPublication, twilioException: TwilioException) {}
                override fun onAudioTrackUnsubscribed(participant: RemoteParticipant, publication: RemoteAudioTrackPublication, audioTrack: RemoteAudioTrack) {}

                override fun onVideoTrackPublished(participant: RemoteParticipant, publication: RemoteVideoTrackPublication) {}
                override fun onVideoTrackUnpublished(participant: RemoteParticipant, publication: RemoteVideoTrackPublication) {}
                override fun onVideoTrackSubscribed(
                    participant: RemoteParticipant,
                    publication: RemoteVideoTrackPublication,
                    videoTrack: RemoteVideoTrack
                ) {
                    Log.d("TwilioReceiverSession", "Remote video track subscribed from ${participant.identity}")
                    attachRemoteVideoTrack(videoTrack)
                }

                override fun onVideoTrackSubscriptionFailed(participant: RemoteParticipant, publication: RemoteVideoTrackPublication, twilioException: TwilioException) {}
                override fun onVideoTrackUnsubscribed(
                    participant: RemoteParticipant,
                    publication: RemoteVideoTrackPublication,
                    videoTrack: RemoteVideoTrack
                ) {
                    detachRemoteVideoTrack(videoTrack)
                }

                override fun onDataTrackPublished(participant: RemoteParticipant, publication: RemoteDataTrackPublication) {}
                override fun onDataTrackUnpublished(participant: RemoteParticipant, publication: RemoteDataTrackPublication) {}
                override fun onDataTrackSubscribed(participant: RemoteParticipant, publication: RemoteDataTrackPublication, dataTrack: RemoteDataTrack) {
                    dataTrack.setListener(dataListener)
                }
                override fun onDataTrackSubscriptionFailed(participant: RemoteParticipant, publication: RemoteDataTrackPublication, twilioException: TwilioException) {}
                override fun onDataTrackUnsubscribed(participant: RemoteParticipant, publication: RemoteDataTrackPublication, dataTrack: RemoteDataTrack) {}

                override fun onAudioTrackEnabled(participant: RemoteParticipant, publication: RemoteAudioTrackPublication) {}
                override fun onAudioTrackDisabled(participant: RemoteParticipant, publication: RemoteAudioTrackPublication) {}
                override fun onVideoTrackEnabled(participant: RemoteParticipant, publication: RemoteVideoTrackPublication) {}
                override fun onVideoTrackDisabled(participant: RemoteParticipant, publication: RemoteVideoTrackPublication) {}
            }

            fun bindParticipant(participant: RemoteParticipant) {
                participant.setListener(remoteParticipantListener)
                participant.remoteVideoTracks.forEach { publication ->
                    if (publication.isTrackSubscribed) {
                        publication.remoteVideoTrack?.let { attachRemoteVideoTrack(it) }
                    }
                }
                participant.remoteAudioTracks.forEach { publication ->
                    if (publication.isTrackSubscribed) {
                        publication.remoteAudioTrack?.enablePlayback(true)
                    }
                }
                participant.remoteDataTracks.forEach { publication ->
                    publication.remoteDataTrack?.setListener(dataListener)
                }
            }

            // FIX: keep the Room handle from Video.connect() right away. Before,
            // `room` was only set in onConnected, so hanging up / a kiosk
            // CALL_ENDED while still connecting called `room?.disconnect()` on
            // null — the connect then completed in the background and left a
            // ZOMBIE "receiver" participant in the room with its mic open.
            room = Video.connect(context, connectOptions, object : Room.Listener {
                override fun onConnected(room: Room) {
                    this@TwilioReceiverSession.room = room
                    if (torndown) {
                        Log.w("TwilioReceiverSession", "connected after hang-up — leaving room ${room.name}")
                        room.disconnect()
                        return
                    }
                    Log.i("TwilioReceiverSession", "connected room=${room.name} others=${room.remoteParticipants.map { it.identity }}")
                    _state.value = ReceiverCallState.Connected(System.currentTimeMillis())
                    room.remoteParticipants.forEach { bindParticipant(it) }
                }

                override fun onConnectFailure(room: Room, twilioException: TwilioException) {
                    Log.e("TwilioReceiverSession", "Connect failure code=${twilioException.code} room=${credentials.roomName}: ${twilioException.message}")
                    _state.value = ReceiverCallState.Ended(EndReason.PROVIDER_ERROR, twilioException.message)
                    release()
                }

                override fun onReconnecting(room: Room, twilioException: TwilioException) {
                    Log.d("TwilioReceiverSession", "Reconnecting...")
                }

                override fun onReconnected(room: Room) {
                    Log.d("TwilioReceiverSession", "Reconnected")
                }

                override fun onDisconnected(room: Room, twilioException: TwilioException?) {
                    Log.d("TwilioReceiverSession", "Disconnected")
                    if (!_state.value.isTerminal) {
                        _state.value = ReceiverCallState.Ended(
                            if (twilioException != null) EndReason.PROVIDER_ERROR else EndReason.REMOTE_HANGUP,
                            twilioException?.message
                        )
                    }
                    release()
                }

                override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
                    Log.d("TwilioReceiverSession", "Participant connected: ${participant.identity}")
                    bindParticipant(participant)
                }

                override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
                    Log.d("TwilioReceiverSession", "Participant disconnected: ${participant.identity}")
                    // Only end when nobody is left (the kiosk hung up). And
                    // actually LEAVE the room — before, this only released the
                    // tracks, so the phone stayed in the room as a ghost.
                    if (room.remoteParticipants.isEmpty() && !_state.value.isTerminal) {
                        _state.value = ReceiverCallState.Ended(EndReason.REMOTE_HANGUP)
                        release()
                    }
                }

                override fun onRecordingStarted(room: Room) {}
                override fun onRecordingStopped(room: Room) {}
            })
        } catch (e: Exception) {
            Log.e("TwilioReceiverSession", "Exception connecting Twilio call", e)
            _state.value = ReceiverCallState.Ended(EndReason.PROVIDER_ERROR, e.message)
            release()
        }
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun attachRemoteVideoTrack(videoTrack: RemoteVideoTrack) {
        mainHandler.post {
            remoteVideoTrack = videoTrack
            val view = VideoView(context).apply { mirror = false }
            remoteVideoView = view
            videoTrack.addSink(view)
            _remoteView.value = view
        }
    }

    private fun detachRemoteVideoTrack(videoTrack: RemoteVideoTrack) {
        mainHandler.post {
            remoteVideoView?.let { view ->
                videoTrack.removeSink(view)
            }
            remoteVideoView = null
            remoteVideoTrack = null
            _remoteView.value = null
        }
    }

    override fun createLocalView(context: Context): View? = _localView.value

    override fun onAppBackgrounded() {
        if (torndown) return
        backgrounded = true
        // Tell the kiosk our video is off (it shows a "camera off" placeholder)
        // rather than leaving the visitor looking at a frozen frame.
        localVideoTrack?.enable(false)
        Log.i("TwilioReceiverSession", "app backgrounded — local video paused")
    }

    override fun onAppForegrounded() {
        if (torndown || !backgrounded) return
        backgrounded = false
        restartCamera(attempt = 1)
    }

    /**
     * Android closed our camera while the screen was off; a WebRTC capturer
     * does not recover from that by itself. Build a fresh capturer + track and
     * republish it in the same room.
     */
    private fun restartCamera(attempt: Int) {
        val r = room ?: return
        val id = cameraId ?: return
        try {
            localVideoTrack?.let { old ->
                localPreview?.let { old.removeSink(it) }
                runCatching { r.localParticipant?.unpublishTrack(old) }
                old.release()
            }
            localVideoTrack = null

            val track = LocalVideoTrack.create(context, isCameraEnabled, CameraCapturer(context, id))
                ?: throw IllegalStateException("LocalVideoTrack.create returned null")
            localVideoTrack = track
            r.localParticipant?.publishTrack(track)
            localPreview?.let { track.addSink(it) }
            Log.i("TwilioReceiverSession", "camera restarted after resume (attempt $attempt)")
        } catch (t: Throwable) {
            Log.w("TwilioReceiverSession", "camera restart failed (attempt $attempt): ${t.message}")
            // Right after unlocking, the camera can still be busy for a moment.
            if (attempt < 3) mainHandler.postDelayed({ if (!torndown) restartCamera(attempt + 1) }, 700L)
        }
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        isMicEnabled = enabled
        localAudioTrack?.enable(enabled)
    }

    override fun setCameraEnabled(enabled: Boolean) {
        isCameraEnabled = enabled
        localVideoTrack?.enable(enabled)
    }

    override fun disconnect(reason: EndReason) {
        // Always leave the room, even if we already think the call is over —
        // the old early-return is how ghost participants survived.
        if (!_state.value.isTerminal) _state.value = ReceiverCallState.Ended(reason)
        release()
    }

    private fun release() {
        torndown = true
        room?.let { r ->
            runCatching { r.disconnect() }
                .onFailure { Log.w("TwilioReceiverSession", "room.disconnect failed", it) }
        }
        room = null
        remoteVideoTrack?.let { track ->
            remoteVideoView?.let { track.removeSink(it) }
        }
        remoteVideoTrack = null
        remoteVideoView = null
        _remoteView.value = null

        localAudioTrack?.release()
        localAudioTrack = null
        runCatching { localDataTrack?.release() }
        localDataTrack = null
        localVideoTrack?.let { t -> localPreview?.let { t.removeSink(it) } }
        localVideoTrack?.release()
        localVideoTrack = null
        localPreview = null
        _localView.value = null
    }
}
