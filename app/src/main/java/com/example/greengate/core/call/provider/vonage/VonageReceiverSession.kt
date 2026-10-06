package com.example.greengate.core.call.provider.vonage

import android.content.Context
import android.util.Log
import android.view.View
import com.example.greengate.core.call.provider.EndReason
import com.example.greengate.core.call.provider.ReceiverCallCredentials
import com.example.greengate.core.call.provider.ReceiverCallSession
import com.example.greengate.core.call.provider.ReceiverCallState
import android.opengl.GLSurfaceView
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "VonageReceiverSession"
private const val KIOSK_STREAM_NAME = "GreenGate Kiosk"

class VonageReceiverSession(
    private val context: Context,
    private val credentials: ReceiverCallCredentials.Vonage
) : ReceiverCallSession {

    private val _state = MutableStateFlow<ReceiverCallState>(ReceiverCallState.Idle)
    override val state: StateFlow<ReceiverCallState> = _state.asStateFlow()

    private val _remoteView = MutableStateFlow<View?>(null)
    override val remoteView: StateFlow<View?> = _remoteView.asStateFlow()

    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null

    private var firstFrameLogged = false

    private var isMicEnabled = true
    private var isCameraEnabled = true

    override suspend fun connect() {
        _state.value = ReceiverCallState.Connecting

        // The kiosk hands us the real apiKey / sessionId / token for the call
        // through the incoming-call signal. Without all three we cannot join
        // the shared Vonage session, so fail fast instead of faking a
        // "connected" state that never carries the visitor's audio or video.
        if (credentials.apiKey.isBlank() ||
            credentials.sessionId.isBlank() ||
            credentials.token.isBlank()
        ) {
            Log.e("VonageReceiverSession", "Missing Vonage credentials from kiosk signal")
            _state.value = ReceiverCallState.Ended(EndReason.PROVIDER_ERROR, "Vonage credentials missing")
            return
        }

        try {
            session = Session.Builder(context, credentials.apiKey, credentials.sessionId).build()
            session?.setSessionListener(object : Session.SessionListener {
                override fun onConnected(session: Session) {
                    _state.value = ReceiverCallState.Connected(System.currentTimeMillis())

                    try {
                        publisher = Publisher.Builder(context).build().apply {
                            setPublisherListener(object : PublisherKit.PublisherListener {
                                override fun onStreamCreated(p: PublisherKit, s: Stream) {}
                                override fun onStreamDestroyed(p: PublisherKit, s: Stream) {}
                                override fun onError(p: PublisherKit, error: OpentokError) {
                                    Log.e("VonageReceiverSession", "Publisher error: ${error.message}")
                                }
                            })
                            publishAudio = isMicEnabled
                            publishVideo = isCameraEnabled
                            setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
                            // Both Vonage views are GLSurfaceViews. Without this the
                            // small local preview and the full-screen remote view
                            // fight over z-order and the remote can render black.
                            (view as? GLSurfaceView)?.setZOrderMediaOverlay(true)
                        }
                        session.publish(publisher)
                    } catch (e: Exception) {
                        Log.e("VonageReceiverSession", "Failed to build publisher", e)
                    }
                }

                override fun onDisconnected(session: Session) {
                    if (!_state.value.isTerminal) {
                        _state.value = ReceiverCallState.Ended(EndReason.REMOTE_HANGUP)
                    }
                    release()
                }

                override fun onStreamReceived(session: Session, stream: Stream) {
                    Log.i(TAG, "stream received id=${stream.streamId} name='${stream.name}' " +
                        "hasVideo=${stream.hasVideo()} created=${stream.creationTime}")

                    // FIX: the kiosk reuses one Vonage session for every call, so
                    // a stale stream from an earlier/crashed kiosk connection can
                    // still be in it. The old code subscribed to *every* stream
                    // and the last one to arrive won — often the dead one, giving
                    // a black/empty remote view. Only take the kiosk's stream, and
                    // prefer the newest one.
                    val current = subscriber?.stream
                    val isKiosk = stream.name.isNullOrBlank() || stream.name == KIOSK_STREAM_NAME
                    if (!isKiosk) {
                        Log.w(TAG, "ignoring non-kiosk stream '${stream.name}'")
                        return
                    }
                    if (current != null && current.creationTime != null && stream.creationTime != null &&
                        !stream.creationTime.after(current.creationTime)
                    ) {
                        Log.w(TAG, "ignoring older kiosk stream ${stream.streamId}")
                        return
                    }
                    subscriber?.let { old ->
                        runCatching { session.unsubscribe(old) }
                        runCatching { old.destroy() }
                        subscriber = null
                        _remoteView.value = null
                    }

                    try {
                        subscriber = Subscriber.Builder(context, stream).build().apply {
                            setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
                            subscribeToVideo = true
                            subscribeToAudio = true
                            setSubscriberListener(object : SubscriberKit.SubscriberListener {
                                override fun onConnected(sub: SubscriberKit) {
                                    Log.i(TAG, "subscriber connected stream=${sub.stream?.streamId}")
                                    _remoteView.value = sub.view
                                }

                                override fun onDisconnected(sub: SubscriberKit) {
                                    Log.w(TAG, "subscriber disconnected")
                                    _remoteView.value = null
                                }

                                override fun onError(sub: SubscriberKit, error: OpentokError) {
                                    Log.e(TAG, "SUBSCRIBER error ${error.errorCode}: ${error.message}")
                                }
                            })
                            // Tells us *why* video is missing: "publishVideo" = kiosk
                            // is not sending video (camera problem on the kiosk),
                            // "quality" = network too poor, "subscribeToVideo" = us.
                            setVideoListener(object : SubscriberKit.VideoListener {
                                override fun onVideoDataReceived(sub: SubscriberKit) {
                                    if (!firstFrameLogged) {
                                        firstFrameLogged = true
                                        Log.i(TAG, "first kiosk video frame received")
                                    }
                                }
                                override fun onVideoDisabled(sub: SubscriberKit, reason: String) {
                                    Log.w(TAG, "kiosk video DISABLED reason=$reason")
                                }
                                override fun onVideoEnabled(sub: SubscriberKit, reason: String) {
                                    Log.i(TAG, "kiosk video enabled reason=$reason")
                                }
                                override fun onVideoDisableWarning(sub: SubscriberKit) {
                                    Log.w(TAG, "kiosk video disable warning (poor network)")
                                }
                                override fun onVideoDisableWarningLifted(sub: SubscriberKit) {}
                            })
                        }
                        session.subscribe(subscriber)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to subscribe to stream", e)
                    }
                }

                override fun onStreamDropped(session: Session, stream: Stream) {
                    if (subscriber?.stream == stream) {
                        session.unsubscribe(subscriber)
                        subscriber = null
                        _remoteView.value = null
                    }
                }

                override fun onError(session: Session, error: OpentokError) {
                    Log.e(TAG, "Vonage SESSION error ${error.errorCode}: ${error.message}")
                    _state.value = ReceiverCallState.Connected(System.currentTimeMillis())
                }
            })

            session?.connect(credentials.token)
        } catch (e: Exception) {
            Log.e("VonageReceiverSession", "Exception connecting Vonage session", e)
            _state.value = ReceiverCallState.Ended(EndReason.PROVIDER_ERROR, e.message)
            release()
        }
    }

    override fun createLocalView(context: Context): View? {
        return publisher?.view
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        isMicEnabled = enabled
        publisher?.publishAudio = enabled
    }

    override fun setCameraEnabled(enabled: Boolean) {
        isCameraEnabled = enabled
        publisher?.publishVideo = enabled
    }

    override fun disconnect(reason: EndReason) {
        // Always release: an early return when already "Ended" could leave the
        // phone connected to the session (same ghost problem as Twilio).
        if (!_state.value.isTerminal) _state.value = ReceiverCallState.Ended(reason)
        release()
    }

    private fun release() {
        if (session != null) {
            publisher?.let {
                session?.unpublish(it)
                it.destroy()
            }
            publisher = null

            subscriber?.let {
                session?.unsubscribe(it)
                it.destroy()
            }
            subscriber = null

            session?.disconnect()
            session = null
        }
        _remoteView.value = null
    }
}
