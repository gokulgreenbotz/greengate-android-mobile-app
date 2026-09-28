package com.example.greengate.core.call.provider

sealed interface ReceiverCallState {
    data object Idle : ReceiverCallState
    data class Incoming(val credentials: ReceiverCallCredentials) : ReceiverCallState
    data object Connecting : ReceiverCallState
    data class Connected(val sinceMs: Long) : ReceiverCallState
    data class Ended(val reason: EndReason, val cause: String? = null) : ReceiverCallState

    val isTerminal: Boolean get() = this is Ended || this is Idle
}

enum class EndReason {
    LOCAL_HANGUP,
    REMOTE_HANGUP,
    DECLINED,
    RING_TIMEOUT,
    PERMISSION_DENIED,
    PROVIDER_ERROR,
    NETWORK_LOST
}
