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
import kotlinx.coroutines.flow.MutableStateFlow
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

    override suspend fun connect() {
        _state.value = ReceiverCallState.Connecting

        try {
            // Create local audio track
            localAudioTrack = LocalAudioTrack.create(context, true)

            // Create local video track
            val enumerator = Camera1Enumerator()
            val frontCameraId = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()

            if (frontCameraId != null) {
                val cameraCapturer = CameraCapturer(context, frontCameraId)
                localVideoTrack = LocalVideoTrack.create(context, true, cameraCapturer)
            }

            val builder = ConnectOptions.Builder(credentials.accessToken)
                .roomName(credentials.roomName)

            localAudioTrack?.let { builder.audioTracks(listOf(it)) }
            localVideoTrack?.let { builder.videoTracks(listOf(it)) }

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
                override fun onDataTrackSubscribed(participant: RemoteParticipant, publication: RemoteDataTrackPublication, dataTrack: RemoteDataTrack) {}
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
            }

            Video.connect(context, connectOptions, object : Room.Listener {
                override fun onConnected(room: Room) {
                    this@TwilioReceiverSession.room = room
                    _state.value = ReceiverCallState.Connected(System.currentTimeMillis())
                    room.remoteParticipants.forEach { bindParticipant(it) }
                }

                override fun onConnectFailure(room: Room, twilioException: TwilioException) {
                    Log.e("TwilioReceiverSession", "Connect failure: ${twilioException.message}")
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
                    _state.value = ReceiverCallState.Ended(EndReason.REMOTE_HANGUP)
                    release()
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

    override fun createLocalView(context: Context): View? {
        val track = localVideoTrack ?: return null
        val view = VideoView(context).apply { mirror = true }
        track.addSink(view)
        return view
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
        if (_state.value.isTerminal) return
        _state.value = ReceiverCallState.Ended(reason)
        room?.disconnect()
        room = null
        release()
    }

    private fun release() {
        remoteVideoTrack?.let { track ->
            remoteVideoView?.let { track.removeSink(it) }
        }
        remoteVideoTrack = null
        remoteVideoView = null
        _remoteView.value = null

        localAudioTrack?.release()
        localAudioTrack = null
        localVideoTrack?.release()
        localVideoTrack = null
    }
}
