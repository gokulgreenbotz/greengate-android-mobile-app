package com.greenbotz.greengate.core.call.provider

import android.content.Context
import com.greenbotz.greengate.core.call.provider.livekit.LiveKitReceiverSession
import com.greenbotz.greengate.core.call.provider.twilio.TwilioReceiverSession
import com.greenbotz.greengate.core.call.provider.vonage.VonageReceiverSession

object ReceiverCallSessionFactory {
    fun create(context: Context, credentials: ReceiverCallCredentials): ReceiverCallSession {
        return when (credentials) {
            is ReceiverCallCredentials.Twilio -> TwilioReceiverSession(context, credentials)
            is ReceiverCallCredentials.Vonage -> VonageReceiverSession(context, credentials)
            is ReceiverCallCredentials.LiveKit -> LiveKitReceiverSession(context, credentials)
        }
    }
}
