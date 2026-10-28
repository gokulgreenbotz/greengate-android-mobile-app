package com.greenbotz.greengate.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.greenbotz.greengate.MainActivity
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.ResidentSession
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject

private const val CHANNEL_ID = "announcements"

/** This install's FCM token, or null while Firebase can't supply one (offline, no Play services). */
suspend fun currentPushToken(): String? = suspendCancellableCoroutine { continuation ->
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        // task.result throws on a failed task, so it is read only on success.
        if (continuation.isActive) continuation.resume(if (task.isSuccessful) task.result else null)
    }
}

/**
 * Receives push notifications from the management office (sent from bms-admin).
 * FCM shows notification messages itself while the app is in the background;
 * this posts one when the app is on screen, where FCM would stay silent.
 */
class GreenGateMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNewToken(token: String) {
        ResidentSession.init(applicationContext)
        scope.launch { ResidentSession.registerPush(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Kiosk call from the onboarding backend: a data-only message, so this runs
        // with the app in the background too. Other pushes (com-core's own call
        // pushes, announcements) are not ours to ring for.
        if (message.data["type"] == "kiosk_call") {
            handleKioskCall(message.data)
            return
        }
        val title = message.notification?.title ?: return
        val body = message.notification?.body.orEmpty()
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Announcements", NotificationManager.IMPORTANCE_HIGH))
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun handleKioskCall(data: Map<String, String>) {
        when (data["event"]) {
            // Same keys as the relay payload, so the same ringing path handles it.
            "CALL_INITIATED" -> IncomingCallNotifier.show(this, callJson(data), fromPush = true)
            // Taken down here; a ringing screen already open ends by its own timeout.
            "CALL_ENDED" -> IncomingCallNotifier.cancel(this)
        }
    }

    // FCM data is flat strings: the provider credentials (twilio / vonage / livekit)
    // arrive as JSON text and go back to objects, as on the relay.
    private fun callJson(data: Map<String, String>): JSONObject {
        val json = JSONObject(data)
        for (key in listOf("twilio", "vonage", "livekit")) {
            data[key]?.let { text -> runCatching { json.put(key, JSONObject(text)) } }
        }
        return json
    }
}
