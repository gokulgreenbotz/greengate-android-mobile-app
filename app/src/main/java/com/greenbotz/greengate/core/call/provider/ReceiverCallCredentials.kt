package com.greenbotz.greengate.core.call.provider

sealed interface ReceiverCallCredentials {
    val provider: CallProvider
    val callId: String
    val visitorName: String
    val unitName: String
    val kioskName: String

    data class Twilio(
        override val callId: String,
        override val visitorName: String,
        override val unitName: String,
        override val kioskName: String,
        val roomName: String,
        val accessToken: String
    ) : ReceiverCallCredentials {
        override val provider: CallProvider = CallProvider.TWILIO
    }

    data class Vonage(
        override val callId: String,
        override val visitorName: String,
        override val unitName: String,
        override val kioskName: String,
        val apiKey: String,
        val sessionId: String,
        val token: String
    ) : ReceiverCallCredentials {
        override val provider: CallProvider = CallProvider.VONAGE
    }

    /** LiveKit Cloud. [token] is minted per phone (unique identity per device). */
    data class LiveKit(
        override val callId: String,
        override val visitorName: String,
        override val unitName: String,
        override val kioskName: String,
        val serverUrl: String,
        val roomName: String,
        val token: String
    ) : ReceiverCallCredentials {
        override val provider: CallProvider = CallProvider.LIVEKIT
        override fun toString() =
            "LiveKit(callId=$callId, url=$serverUrl, room=$roomName, token=<${token.length} chars>)"
    }
}
