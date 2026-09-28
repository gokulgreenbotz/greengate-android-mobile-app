package com.example.greengate.core.call.provider

enum class CallProvider {
    TWILIO,
    VONAGE;

    companion object {
        fun fromString(value: String?): CallProvider {
            return when (value?.lowercase()?.trim()) {
                "vonage", "opentok" -> VONAGE
                else -> TWILIO
            }
        }
    }
}
