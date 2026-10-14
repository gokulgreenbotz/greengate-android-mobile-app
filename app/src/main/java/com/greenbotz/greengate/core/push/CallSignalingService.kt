package com.greenbotz.greengate.core.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.greenbotz.greengate.MainActivity
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.call.provider.SharedCallConfig
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class CallSignalingService : Service() {

    // callTimeout caps the WHOLE request. Before, a stalled poll could hold the
    // single poll thread for ~10 s (5 s connect + 5 s read) before the next
    // 1.5 s tick, so one slow relay response delayed ringing by 10 s+.
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val signalingUrl = "https://kvdb.io/JvTpnXFCynR2tFAjEa8ysm/active_call"
    private var executor: ScheduledExecutorService? = null
    private var lastProcessedCallId: String? = null
    private var lastProcessedTimestamp: Long = 0
    /** callId seen on the previous successful poll (edge detection). */
    private var lastSeenCallId: String? = null
    private var hasBaseline = false
    private var consecutivePollFailures = 0

    companion object {
        const val CHANNEL_ID = "greengate_incoming_calls_channel"
        const val NOTIFICATION_ID = 9999
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 8888
        private const val SERVICE_CHANNEL_ID = "greengate_listener_silent"
        private const val LEGACY_SERVICE_CHANNEL_ID = "greengate_service_channel"
        /** Incoming-call notification removes itself after this, as a safety net. */
        private const val INCOMING_NOTIFICATION_TIMEOUT_MS = 45_000L

        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_VISITOR_NAME = "extra_visitor_name"
        const val EXTRA_UNIT_NAME = "extra_unit_name"
        const val EXTRA_KIOSK_NAME = "extra_kiosk_name"
        const val EXTRA_ROOM_NAME = "extra_room_name"
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"
        const val EXTRA_PROVIDER = "extra_provider"
        const val EXTRA_VONAGE_API_KEY = "extra_vonage_api_key"
        const val EXTRA_VONAGE_SESSION_ID = "extra_vonage_session_id"
        const val EXTRA_VONAGE_TOKEN = "extra_vonage_token"

        fun startService(context: Context) {
            val intent = Intent(context, CallSignalingService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("CallSignalingService", "startForegroundService failed, falling back to startService", e)
                try {
                    context.startService(intent)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
        startPolling()
        Log.i("CallSignalingService", "Persistent background signaling service created & listening")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPolling()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            // Android requires a notification for a background listener, but it
            // can be made as quiet as possible: MIN importance = no sound, no
            // status-bar icon, no badge, collapsed at the bottom of the shade,
            // hidden on the lock screen. A NEW channel id is needed because an
            // existing channel's importance can't be lowered by the app.
            nm?.deleteNotificationChannel(LEGACY_SERVICE_CHANNEL_ID)
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Background service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps GreenGate ready to receive gate calls"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            nm?.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
                .setContentTitle("GreenGate")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setSilent(true)
                .setShowWhen(false)
                .setOngoing(true)
                .build()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        FOREGROUND_SERVICE_NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    )
                } else {
                    startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.w("CallSignalingService", "startForeground exception caught cleanly: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Incoming Kiosk Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for incoming video calls from kiosk"
                enableVibration(true)
                setBypassDnd(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun startPolling() {
        if (executor != null && !executor!!.isShutdown) return

        executor = Executors.newSingleThreadScheduledExecutor()
        executor?.scheduleWithFixedDelay({
            pollCallSignal()
        }, 0, 1500, TimeUnit.MILLISECONDS)
    }

    private fun pollCallSignal() {
        try {
            val request = Request.Builder()
                .url(signalingUrl)
                .header("Cache-Control", "no-cache, no-store")
                .get()
                .build()

            val started = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    consecutivePollFailures++
                    Log.w("CallSignalingService", "Poll HTTP ${resp.code} (failures in a row=$consecutivePollFailures)")
                    return
                }
                consecutivePollFailures = 0
                val bodyStr = resp.body?.string() ?: return
                if (bodyStr.isBlank() || bodyStr.trim() == "null") return

                val json = JSONObject(bodyStr)
                val event = json.optString("event")
                val callId = json.optString("callId")
                val timestamp = json.optLong("timestamp", 0)
                val now = System.currentTimeMillis()

                // FIX: the old check `now - timestamp > 45 s` compared the KIOSK's
                // clock with the PHONE's clock. Kiosks often run with a drifted
                // clock, so every signal looked "old" and the background service
                // never rang — the call only appeared once the app was opened.
                // Now: a callId that changes between two polls is a new call,
                // whatever the clocks say. The clock check is only used for the
                // very first poll after the service starts (to avoid re-ringing a
                // call that was already on the relay before we started).
                // Kiosk cancelled / call ended while we were ringing in the
                // background: take the incoming-call notification down.
                if (event == "CALL_ENDED" && callId.isNotBlank() && callId == lastProcessedCallId) {
                    IncomingCallNotifier.cancel(this)
                }

                val isNewCallId = callId.isNotBlank() && callId != lastSeenCallId
                val firstPoll = !hasBaseline
                hasBaseline = true
                lastSeenCallId = callId

                if (event == "CALL_INITIATED" && isNewCallId && callId != lastProcessedCallId) {
                    if (firstPoll && Math.abs(now - timestamp) > 45_000L) {
                        Log.i("CallSignalingService", "Skipping pre-existing signal on first poll callId=$callId")
                        return
                    }
                    lastProcessedCallId = callId
                    lastProcessedTimestamp = timestamp
                    Log.i(
                        "CallSignalingService",
                        "BACKGROUND SIGNAL RECEIVED callId=$callId pollMs=${now - started} " +
                            "age(kioskClock->phoneClock)=${now - timestamp}ms"
                    )
                    triggerIncomingCallNotificationAndActivity(json)
                }
            }
        } catch (e: Exception) {
            consecutivePollFailures++
            Log.w("CallSignalingService", "Poll error (${consecutivePollFailures} in a row): ${e.message}")
        }
    }

    private fun triggerIncomingCallNotificationAndActivity(json: JSONObject) {
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

            // App is open: the in-app ringing screen (CallSignalingManager)
            // handles this call. Don't show a notification over it.
            if (IncomingCallNotifier.appInForeground) {
                Log.i("CallSignalingService", "App in foreground — skipping incoming-call notification")
                return
            }

            // 1. Wake up phone CPU and turn screen on
            wakeUpDevice()

            // 2. Intent to open MainActivity
            val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_VISITOR_NAME, visitorName)
                putExtra(EXTRA_UNIT_NAME, unitName)
                putExtra(EXTRA_KIOSK_NAME, kioskName)
                putExtra(EXTRA_PROVIDER, provider)
                putExtra(EXTRA_ROOM_NAME, roomName)
                putExtra(EXTRA_ACCESS_TOKEN, accessToken)
                putExtra(EXTRA_VONAGE_API_KEY, vonageApiKey)
                putExtra(EXTRA_VONAGE_SESSION_ID, vonageSessionId)
                putExtra(EXTRA_VONAGE_TOKEN, vonageToken)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 3. High priority notification with Full Screen Intent
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
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
                .setTimeoutAfter(INCOMING_NOTIFICATION_TIMEOUT_MS)
                .build()

            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)

            // 4. Directly launch activity to bring call screen to front immediately
            startActivity(fullScreenIntent)

        } catch (e: Exception) {
            Log.e("CallSignalingService", "Error triggering incoming call notification", e)
        }
    }

    private fun wakeUpDevice() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "GreenGate:IncomingCallWakeLock"
            )
            wakeLock.acquire(10_000L) // Hold wake lock for 10s to ensure screen lights up
        } catch (e: Exception) {
            Log.e("CallSignalingService", "WakeLock error", e)
        }
    }

    override fun onDestroy() {
        executor?.shutdownNow()
        executor = null
        super.onDestroy()
    }
}
