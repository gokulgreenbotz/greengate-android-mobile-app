package com.greenbotz.greengate.core.call.provider

enum class CallProvider {
    TWILIO,
    VONAGE,
    LIVEKIT;

    companion object {
        fun fromString(value: String?): CallProvider {
            return when (value?.lowercase()?.trim()) {
                "vonage", "opentok" -> VONAGE
                "livekit" -> LIVEKIT
                else -> TWILIO
            }
        }
    }
}
