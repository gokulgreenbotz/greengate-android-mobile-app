package com.example.greengate.core.call.signaling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.greengate.MainActivity
import com.example.greengate.R
import com.example.greengate.core.call.provider.SharedCallConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class CallSignalingService : Service() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val signalingUrl = "https://kvdb.io/JvTpnXFCynR2tFAjEa8ysm/active_call"
    private var executor: ScheduledExecutorService? = null
    private var lastProcessedCallId: String? = null
    private var lastProcessedTimestamp: Long = 0

    companion object {
        const val CHANNEL_ID = "greengate_incoming_calls_channel"
        const val NOTIFICATION_ID = 9999
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 8888

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
            val channel = NotificationChannel(
                "greengate_service_channel",
                "GreenGate Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, "greengate_service_channel")
                .setContentTitle("GreenGate Call Listener")
                .setContentText("Listening for incoming kiosk calls")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        FOREGROUND_SERVICE_NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
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
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: return
                    if (bodyStr.isBlank() || bodyStr.trim() == "null") return

                    val json = JSONObject(bodyStr)
                    val event = json.optString("event")
                    val callId = json.optString("callId")
                    val timestamp = json.optLong("timestamp", 0)

                    val now = System.currentTimeMillis()
                    if (now - timestamp > 45_000L) return

                    if (event == "CALL_INITIATED" && (callId != lastProcessedCallId || timestamp > lastProcessedTimestamp)) {
                        lastProcessedCallId = callId
                        lastProcessedTimestamp = timestamp
                        Log.i("CallSignalingService", "BACKGROUND SIGNAL RECEIVED: INCOMING CALL! callId=$callId")
                        triggerIncomingCallNotificationAndActivity(json)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("CallSignalingService", "Poll error: ${e.message}")
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
                .setAutoCancel(true)
                .setOngoing(true)
                .build()

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)

            // 4. Directly launch activity to bring call screen to front immediately
            startActivity(fullScreenIntent)

        } catch (e: Exception) {
            Log.e("CallSignalingService", "Error triggering incoming call notification", e)
        }
    }

    private fun wakeUpDevice() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
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
