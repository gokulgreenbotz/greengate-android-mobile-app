package com.example.greengate.core.call.provider

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ReceiverCallSession {
    val state: StateFlow<ReceiverCallState>
    val remoteView: StateFlow<View?>

    /**
     * The resident's own camera preview. Emits null until the camera is
     * running, then the view — the UI observes this instead of asking once
     * (asking once, before the camera existed, is why the preview was blank).
     */
    val localView: StateFlow<View?>

    /**
     * Small JSON messages from the kiosk delivered over the call itself
     * (Twilio data track / Vonage signal): gate results and hang-up. Far
     * faster than the polling relay.
     */
    val appMessages: SharedFlow<String>

    /** Sends a JSON message to the kiosk over the call. False if not connected. */
    fun sendAppMessage(message: String): Boolean

    suspend fun connect()
    fun createLocalView(context: Context): View?

    /**
     * App left the screen (power button, home, another app). Android revokes
     * camera access here, so pause video instead of freezing on a dead frame.
     */
    fun onAppBackgrounded() {}

    /** App is visible again: reopen the camera and resume sending video. */
    fun onAppForegrounded() {}
    fun setMicrophoneEnabled(enabled: Boolean)
    fun setCameraEnabled(enabled: Boolean)
    fun disconnect(reason: EndReason)
}
