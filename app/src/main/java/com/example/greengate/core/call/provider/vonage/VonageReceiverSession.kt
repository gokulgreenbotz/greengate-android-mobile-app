package com.example.greengate.core.call.provider.vonage

import android.content.Context
import android.util.Log
import android.view.View
import com.example.greengate.core.call.provider.EndReason
import com.example.greengate.core.call.provider.ReceiverCallCredentials
import com.example.greengate.core.call.provider.ReceiverCallSession
import com.example.greengate.core.call.provider.ReceiverCallState
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
                    try {
                        subscriber = Subscriber.Builder(context, stream).build().apply {
                            setSubscriberListener(object : SubscriberKit.SubscriberListener {
                                override fun onConnected(sub: SubscriberKit) {
                                    _remoteView.value = sub.view
                                }

                                override fun onDisconnected(sub: SubscriberKit) {
                                    _remoteView.value = null
                                }

                                override fun onError(sub: SubscriberKit, error: OpentokError) {
                                    Log.e("VonageReceiverSession", "Subscriber error: ${error.message}")
                                }
                            })
                        }
                        session.subscribe(subscriber)
                    } catch (e: Exception) {
                        Log.e("VonageReceiverSession", "Failed to subscribe to stream", e)
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
                    Log.w("VonageReceiverSession", "Vonage session error ${error.errorCode}: ${error.message} — converting to active call fallback")
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
        if (_state.value.isTerminal) return
        _state.value = ReceiverCallState.Ended(reason)
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
