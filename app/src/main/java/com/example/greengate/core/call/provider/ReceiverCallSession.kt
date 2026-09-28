package com.example.greengate.core.call.provider

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.StateFlow

interface ReceiverCallSession {
    val state: StateFlow<ReceiverCallState>
    val remoteView: StateFlow<View?>

    suspend fun connect()
    fun createLocalView(context: Context): View?
    fun setMicrophoneEnabled(enabled: Boolean)
    fun setCameraEnabled(enabled: Boolean)
    fun disconnect(reason: EndReason)
}
