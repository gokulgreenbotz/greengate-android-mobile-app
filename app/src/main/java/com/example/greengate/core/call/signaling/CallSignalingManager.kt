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
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val signalingUrl = "https://kvdb.io/JvTpnXFCynR2tFAjEa8ysm/active_call"
    private val statusUrl = "https://kvdb.io/JvTpnXFCynR2tFAjEa8ysm/call_status"
    /** Phone → kiosk channel (hang-up, open gate). Kiosk → phone stays on active_call. */
    private val kioskInboxUrl = "https://kvdb.io/JvTpnXFCynR2tFAjEa8ysm/mobile_to_kiosk"

    private var executor: ScheduledExecutorService? = null
    private var lastProcessedCallId: String? = null
    private var lastProcessedTimestamp: Long = 0
    private var lastDoorResultRequestId: String? = null

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
                        Log.i("CallSignalingManager", "HTTPS SIGNAL RECEIVED: INCOMING CALL from Kiosk! callId=$callId " +
                            "age(kioskClock->phoneClock)=${now - timestamp}ms")
                        handleIncomingCall(json)
                    } else if ((event == "DOOR_UNLOCKED" || event == "DOOR_UNLOCK_FAILED") &&
                        callId == controller.currentCredentials?.callId &&
                        json.optString("requestId") != lastDoorResultRequestId
                    ) {
                        // Kiosk's answer to our unlock request.
                        lastDoorResultRequestId = json.optString("requestId")
                        val ok = event == "DOOR_UNLOCKED"
                        Log.i("CallSignalingManager", "Door result from kiosk: $event callId=$callId")
                        controller.onDoorUnlockResult(ok, json.optString("message"), json.optString("requestId"))
                    } else if (event == "CALL_ENDED" && callId == controller.currentCredentials?.callId) {
                        lastProcessedCallId = null
                        Log.i("CallSignalingManager", "HTTPS SIGNAL RECEIVED: CALL ENDED from Kiosk for callId=$callId")
                        controller.declineCall()
                    }
                }
            }
        } catch (e: Exception) {
            // Was Log.d, which hid relay timeouts / throttling completely.
            Log.w("CallSignalingManager", "Poll error: ${e.message}")
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

    /**
     * Phone → kiosk messages go to their OWN relay key. They used to share
     * "active_call" with the kiosk's messages, so either side could overwrite
     * the other's message before it was read — e.g. the resident's hang-up or
     * unlock request never reached the kiosk.
     */
    private fun postToKiosk(payload: JSONObject, what: String) {
        Executors.newSingleThreadExecutor().execute {
            try {
                payload.put("sender", "MOBILE_RECEIVER")
                if (!payload.has("messageId")) {
                    payload.put("messageId", "m-${System.currentTimeMillis()}-${(1000..9999).random()}")
                }
                payload.put("timestamp", System.currentTimeMillis())
                val req = Request.Builder()
                    .url(kioskInboxUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                // Relay is only the backup path now (the request also goes over
                // the call itself), so a relay failure is logged, not shown.
                client.newCall(req).execute().use { resp ->
                    Log.i("CallSignalingManager", "$what sent to kiosk via relay http=${resp.code}")
                }
            } catch (e: Exception) {
                Log.w("CallSignalingManager", "Relay send of $what failed: ${e.message}")
            }
        }
    }

    fun broadcastCallEnded(callId: String?, reason: String = "HANGUP") {
        postToKiosk(
            JSONObject().apply {
                put("event", "CALL_ENDED")
                put("callId", callId ?: "")
                put("reason", reason)
            },
            what = "CALL_ENDED"
        )
    }

    /**
     * Asks the kiosk on this call to open the gate. [endCall] = also finish the
     * call afterwards. The kiosk only acts on a request whose callId matches its
     * live call and replies with DOOR_UNLOCKED / DOOR_UNLOCK_FAILED on
     * "active_call" (handled in pollCallSignal).
     *
     * DEMO ONLY: the relay has no authentication. In production this must go
     * through the backend, which checks the resident may open this gate.
     */
    fun requestOpenGate(callId: String?, endCall: Boolean, requestId: String) {
        if (callId.isNullOrBlank()) return
        postToKiosk(
            JSONObject().apply {
                put("event", "OPEN_GATE")
                put("callId", callId)
                put("endCall", endCall)
                // Same id as the in-call message → kiosk opens only once.
                put("messageId", requestId)
            },
            what = "OPEN_GATE(endCall=$endCall)"
        )
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
