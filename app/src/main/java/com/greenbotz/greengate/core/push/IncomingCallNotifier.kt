package com.greenbotz.greengate.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.greenbotz.greengate.MainActivity
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.call.provider.SharedCallConfig
import org.json.JSONObject

/**
 * Small shared helper for the incoming-call notification.
 *
 * - [appInForeground] is set by MainActivity (onStart/onStop). While the app
 *   is on screen the in-app ringing UI handles the call, so the background
 *   service must NOT post a notification on top of it.
 * - [show] rings a call from outside the app: wakes the screen, posts the
 *   full-screen notification and opens MainActivity, which starts the ringing UI.
 * - [cancel] removes the incoming-call notification. Called whenever the call
 *   leaves the ringing state: answered, declined, hung up, ring timeout, or the
 *   kiosk cancelled the call.
 */
object IncomingCallNotifier {

    /** Incoming-call notification removes itself after this, as a safety net. */
    private const val NOTIFICATION_TIMEOUT_MS = 45_000L

    @Volatile
    var appInForeground: Boolean = false

    private var permissionPromptShown = false

    /**
     * A call that arrives while the app is closed can only open the call screen by
     * itself with "Display over other apps" (and, from Android 14, "Full-screen
     * notifications"). Without them Android shows only a heads-up notification.
     * Sends the user to the first missing setting, once per app start.
     */
    fun promptForMissingPermissions(activity: android.app.Activity) {
        if (permissionPromptShown) return
        val packageUri = android.net.Uri.parse("package:${activity.packageName}")
        val intent = when {
            !android.provider.Settings.canDrawOverlays(activity) ->
                Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
            Build.VERSION.SDK_INT >= 34 &&
                !activity.getSystemService(NotificationManager::class.java).canUseFullScreenIntent() ->
                Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)
            else -> return
        }
        permissionPromptShown = true
        runCatching { activity.startActivity(intent) }
            .onFailure { Log.w("IncomingCallNotifier", "could not open call permission settings", it) }
    }

    fun cancel(context: Context) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(CallSignalingService.NOTIFICATION_ID)
        }.onFailure { Log.w("IncomingCallNotifier", "cancel failed", it) }
    }

    /**
     * Rings the call described by [json] (the kiosk's call payload: callId,
     * visitorName, unitName, kioskName, provider, optional twilio/vonage objects).
     *
     * [fromPush] = the call came by FCM. The in-app WebSocket may be down even
     * with the app on screen, so the activity is still opened (no notification);
     * for the relay service, an app on screen already handles the call itself.
     */
    fun show(context: Context, json: JSONObject, fromPush: Boolean = false) {
        try {
            val callId = json.optString("callId", "call-${System.currentTimeMillis()}")
            val visitorName = json.optString("visitorName", "Visitor at Kiosk")
            val unitName = json.optString("unitName", "Unit 1204")
            val kioskName = json.optString("kioskName", "Main Gate Kiosk")

            val provider = json.optString("provider", "twilio")
            val twilioObj = json.optJSONObject("twilio")
            val vonageObj = json.optJSONObject("vonage")

            val roomName = twilioObj?.optString("roomName")?.ifBlank { null }
                ?: json.optString("roomName").ifBlank { SharedCallConfig.twilioRoomName }
            val accessToken = twilioObj?.optString("accessToken")?.ifBlank { null }
                ?: json.optString("accessToken").ifBlank { SharedCallConfig.twilioReceiverToken }

            val vonageApiKey = vonageObj?.optString("apiKey")?.ifBlank { null }
                ?: json.optString("apiKey").ifBlank { SharedCallConfig.vonageApiKey }
            val vonageSessionId = vonageObj?.optString("sessionId")?.ifBlank { null }
                ?: json.optString("sessionId").ifBlank { SharedCallConfig.vonageSessionId }
            val vonageToken = vonageObj?.optString("token")?.ifBlank { null }
                ?: json.optString("token").ifBlank { SharedCallConfig.vonageToken }

            if (appInForeground && !fromPush) {
                Log.i("IncomingCallNotifier", "App in foreground — skipping incoming-call notification")
                return
            }

            val callIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(CallSignalingService.EXTRA_CALL_ID, callId)
                putExtra(CallSignalingService.EXTRA_VISITOR_NAME, visitorName)
                putExtra(CallSignalingService.EXTRA_UNIT_NAME, unitName)
                putExtra(CallSignalingService.EXTRA_KIOSK_NAME, kioskName)
                putExtra(CallSignalingService.EXTRA_PROVIDER, provider)
                putExtra(CallSignalingService.EXTRA_ROOM_NAME, roomName)
                putExtra(CallSignalingService.EXTRA_ACCESS_TOKEN, accessToken)
                putExtra(CallSignalingService.EXTRA_VONAGE_API_KEY, vonageApiKey)
                putExtra(CallSignalingService.EXTRA_VONAGE_SESSION_ID, vonageSessionId)
                putExtra(CallSignalingService.EXTRA_VONAGE_TOKEN, vonageToken)
                json.optJSONObject("livekit")?.let { lk ->
                    putExtra(CallSignalingService.EXTRA_LIVEKIT_URL, lk.optString("url"))
                    putExtra(CallSignalingService.EXTRA_LIVEKIT_ROOM, lk.optString("roomName"))
                    putExtra(CallSignalingService.EXTRA_LIVEKIT_TOKEN, lk.optString("token"))
                }
            }

            if (appInForeground) {
                context.startActivity(callIntent)
                return
            }

            wakeUpDevice(context)
            ensureChannel(context)

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CallSignalingService.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("INCOMING KIOSK CALL")
                .setContentText("$visitorName ($unitName)")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                // Safety net: never leave a stale "incoming call" behind if the
                // app process is killed before it can cancel the notification.
                .setTimeoutAfter(NOTIFICATION_TIMEOUT_MS)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(CallSignalingService.NOTIFICATION_ID, notification)

            // Also launch the activity directly to bring the call screen to the front at once.
            context.startActivity(callIntent)
        } catch (e: Exception) {
            Log.e("IncomingCallNotifier", "Error triggering incoming call notification", e)
        }
    }

    /** Same channel the relay service creates; FCM can ring before that service has ever run. */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CallSignalingService.CHANNEL_ID,
            "Incoming Kiosk Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High priority notifications for incoming video calls from kiosk"
            enableVibration(true)
            setBypassDnd(true)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun wakeUpDevice(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "GreenGate:IncomingCallWakeLock"
            )
            wakeLock.acquire(10_000L) // Hold wake lock for 10s to ensure screen lights up
        } catch (e: Exception) {
            Log.e("IncomingCallNotifier", "WakeLock error", e)
        }
    }
}
