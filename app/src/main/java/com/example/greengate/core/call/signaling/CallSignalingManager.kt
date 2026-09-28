package com.example.greengate.core.call.signaling

import android.content.Context
import android.util.Log
import com.example.greengate.core.call.ReceiverCallController
import com.example.greengate.core.call.provider.CallProvider
import com.example.greengate.core.call.provider.ReceiverCallCredentials
import com.example.greengate.core.call.provider.SharedCallConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class CallSignalingManager(
    private val context: Context,
    private val controller: ReceiverCallController
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val signalingUrl = "https://kvdb.io/JvTpnXFCynR2tFAjEa8ysm/active_call"
    private val statusUrl = "https://kvdb.io/JvTpnXFCynR2tFAjEa8ysm/call_status"

    private var executor: ScheduledExecutorService? = null
    private var lastProcessedCallId: String? = null
    private var lastProcessedTimestamp: Long = 0

    fun startListening() {
        if (executor != null && !executor!!.isShutdown) return

        executor = Executors.newSingleThreadScheduledExecutor()
        executor?.scheduleWithFixedDelay({
            pollCallSignal()
        }, 0, 1500, TimeUnit.MILLISECONDS) // Poll HTTPS relay every 1.5 seconds

        Log.i("CallSignalingManager", "Started HTTPS real-time call signaling listener: $signalingUrl")
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
                    // Ignore old signals older than 10 minutes (allows for clock drift across devices)
                    if (timestamp > 0 && Math.abs(now - timestamp) > 600_000L && callId == lastProcessedCallId) return

                    if (event == "CALL_INITIATED" && (callId != lastProcessedCallId || timestamp > lastProcessedTimestamp)) {
                        lastProcessedCallId = callId
                        lastProcessedTimestamp = timestamp
                        Log.i("CallSignalingManager", "HTTPS SIGNAL RECEIVED: INCOMING CALL from Kiosk! callId=$callId")
                        handleIncomingCall(json)
                    } else if (event == "CALL_ENDED" && callId == controller.currentCredentials?.callId) {
                        lastProcessedCallId = null
                        Log.i("CallSignalingManager", "HTTPS SIGNAL RECEIVED: CALL ENDED from Kiosk for callId=$callId")
                        controller.declineCall()
                    }
                }
            }
        } catch (e: Exception) {
            // Quiet log for polling checks
            Log.d("CallSignalingManager", "Poll check: ${e.message}")
        }
    }

    private fun handleIncomingCall(json: JSONObject) {
        try {
            val callId = json.optString("callId", "call-${System.currentTimeMillis()}")
            val providerStr = json.optString("provider", "twilio")
            val provider = CallProvider.fromString(providerStr)
            val visitorName = json.optString("visitorName", "Visitor at Kiosk")
            val unitName = json.optString("unitName", "Unit 1204")
            val kioskName = json.optString("kioskName", "Main Gate Kiosk")

            val credentials = when (provider) {
                CallProvider.TWILIO -> {
                    val twilioObj = json.optJSONObject("twilio")
                    val roomName = twilioObj?.optString("roomName")?.ifBlank { null }
                        ?: json.optString("roomName").ifBlank { SharedCallConfig.twilioRoomName }
                    val accessToken = twilioObj?.optString("accessToken")?.ifBlank { null }
                        ?: json.optString("accessToken").ifBlank { SharedCallConfig.twilioReceiverToken }

                    ReceiverCallCredentials.Twilio(
                        callId = callId,
                        visitorName = visitorName,
                        unitName = unitName,
                        kioskName = kioskName,
                        roomName = roomName,
                        accessToken = accessToken
                    )
                }

                CallProvider.VONAGE -> {
                    val vonageObj = json.optJSONObject("vonage")
                    val apiKey = vonageObj?.optString("apiKey")?.ifBlank { null }
                        ?: json.optString("apiKey").ifBlank { SharedCallConfig.vonageApiKey }
                    val sessionId = vonageObj?.optString("sessionId")?.ifBlank { null }
                        ?: json.optString("sessionId").ifBlank { SharedCallConfig.vonageSessionId }
                    val token = vonageObj?.optString("token")?.ifBlank { null }
                        ?: json.optString("token").ifBlank { SharedCallConfig.vonageToken }

                    ReceiverCallCredentials.Vonage(
                        callId = callId,
                        visitorName = visitorName,
                        unitName = unitName,
                        kioskName = kioskName,
                        apiKey = apiKey,
                        sessionId = sessionId,
                        token = token
                    )
                }
            }

            // Trigger incoming call UI immediately on mobile phone!
            controller.triggerIncomingCall(credentials)
        } catch (e: Exception) {
            Log.e("CallSignalingManager", "Error handling incoming call payload", e)
        }
    }

    fun broadcastCallEnded(callId: String?, reason: String = "HANGUP") {
        Executors.newSingleThreadExecutor().execute {
            try {
                val json = JSONObject().apply {
                    put("event", "CALL_ENDED")
                    put("sender", "MOBILE_RECEIVER")
                    put("callId", callId ?: "")
                    put("reason", reason)
                    put("timestamp", System.currentTimeMillis())
                }

                val body = json.toString().toRequestBody(jsonMediaType)
                val req = Request.Builder()
                    .url(signalingUrl)
                    .post(body)
                    .build()

                client.newCall(req).execute().close()
            } catch (e: Exception) {
                Log.e("CallSignalingManager", "Failed to broadcast call ended signal", e)
            }
        }
    }

    fun postStatus(event: String) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val payload = JSONObject().apply {
                    put("event", event)
                    put("sender", "MOBILE_RECEIVER")
                    put("timestamp", System.currentTimeMillis())
                }

                val body = payload.toString().toRequestBody(jsonMediaType)
                val req = Request.Builder()
                    .url(statusUrl)
                    .post(body)
                    .build()

                client.newCall(req).execute().close()
            } catch (e: Exception) {
                Log.e("CallSignalingManager", "Failed to post status signal", e)
            }
        }
    }

    fun stop() {
        executor?.shutdownNow()
        executor = null
    }
}
