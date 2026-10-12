package com.example.greengate.core.call.signaling

import android.app.NotificationManager
import android.content.Context
import android.util.Log

/**
 * Small shared helper for the incoming-call notification.
 *
 * - [appInForeground] is set by MainActivity (onStart/onStop). While the app
 *   is on screen the in-app ringing UI handles the call, so the background
 *   service must NOT post a notification on top of it.
 * - [cancel] removes the incoming-call notification. Called whenever the call
 *   leaves the ringing state: answered, declined, hung up, ring timeout, or the
 *   kiosk cancelled the call.
 */
object IncomingCallNotifier {

    @Volatile
    var appInForeground: Boolean = false

    fun cancel(context: Context) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(CallSignalingService.NOTIFICATION_ID)
        }.onFailure { Log.w("IncomingCallNotifier", "cancel failed", it) }
    }
}
