package com.example.greengate.core.call.provider

import android.content.Context
import com.example.greengate.core.call.provider.twilio.TwilioReceiverSession
import com.example.greengate.core.call.provider.vonage.VonageReceiverSession

object ReceiverCallSessionFactory {
    fun create(context: Context, credentials: ReceiverCallCredentials): ReceiverCallSession {
        return when (credentials) {
            is ReceiverCallCredentials.Twilio -> TwilioReceiverSession(context, credentials)
            is ReceiverCallCredentials.Vonage -> VonageReceiverSession(context, credentials)
        }
    }
}
