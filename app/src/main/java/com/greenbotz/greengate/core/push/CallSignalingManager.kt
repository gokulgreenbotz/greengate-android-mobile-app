package com.greenbotz.greengate.core.push

import android.content.Context
import android.util.Log
import com.greenbotz.greengate.core.call.ReceiverCallController
import com.greenbotz.greengate.core.call.provider.CallProvider
import com.greenbotz.greengate.core.call.provider.ReceiverCallCredentials
import com.greenbotz.greengate.core.call.provider.SharedCallConfig
import com.greenbotz.greengate.core.call.provider.livekit.LiveKitConfig
import com.greenbotz.greengate.core.session.ResidentSession
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

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
        startSocket()
        if (executor != null && !executor!!.isShutdown) return

        executor = Executors.newSingleThreadScheduledExecutor()
        executor?.scheduleWithFixedDelay({
            pollCallSignal()
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)

        Log.i("CallSignalingManager", "Started HTTPS real-time call signaling listener: $signalingUrl")
    }

    // ---- WebSocket (onboarding backend, Stage/Local) ----
    // Open only while this manager is listening, i.e. while the app is on screen;
    // calls that arrive with the app closed come by FCM instead. The server sends
    // the same JSON the relay carries, so it goes through the same handlers.

    private val socketClient = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val socketScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socketJob: Job? = null
    @Volatile private var socket: WebSocket? = null

    /**
     * Closes the socket while the app is off screen: the server rings sessions
     * without a live socket by FCM, so a socket left open in the background would
     * swallow the call. [startSocket] reopens it when the app is back.
     */
    fun pauseSocket() {
        socketJob?.cancel()
        socketJob = null
        socket?.close(1000, null)
        socket = null
    }

    fun startSocket() {
        if (ResidentSession.callSocketUrl == null || socketJob?.isActive == true) return
        socketJob = socketScope.launch {
            var forceRefresh = false
            var failures = 0
            while (isActive) {
                val url = ResidentSession.callSocketUrl ?: break
                val token = ResidentSession.callSocketToken(forceRefresh) ?: break
                forceRefresh = false
                val opened = AtomicBoolean(false)
                val closed = CompletableDeferred<Int>()
                val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
                socket = socketClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        opened.set(true)
                        Log.i("CallSignalingManager", "Call socket connected")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) = handleSocketMessage(text)

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        closed.complete(0)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.w("CallSignalingManager", "Call socket failed: ${t.message}")
                        closed.complete(response?.code ?: 0)
                    }
                })
                val status = closed.await()
                socket = null
                if (status == 401) forceRefresh = true
                failures = if (opened.get()) 0 else failures + 1
                delay(minOf(30_000L, 1_000L shl minOf(failures, 5)))
            }
        }
    }

    private fun handleSocketMessage(text: String) {
        try {
            val json = JSONObject(text)
            val callId = json.optString("callId")
            when (json.optString("event")) {
                "CALL_INITIATED" -> if (callId.isNotBlank() && callId != lastProcessedCallId) {
                    lastProcessedCallId = callId
                    lastProcessedTimestamp = json.optLong("timestamp", 0)
                    Log.i("CallSignalingManager", "SOCKET SIGNAL RECEIVED: INCOMING CALL callId=$callId")
                    handleIncomingCall(json)
                }
                // Hang-up by the kiosk, or "answered on another phone of this household".
                "CALL_ENDED" -> if (callId.isNotBlank() && callId == controller.currentCredentials?.callId) {
                    controller.onKioskEndedCall(callId)
                }
            }
        } catch (e: Exception) {
            Log.w("CallSignalingManager", "Bad call socket message: ${e.message}")
        }
    }

    private fun sendOverSocket(event: String, callId: String?, reason: String? = null) {
        if (callId.isNullOrBlank()) return
        val payload = JSONObject().put("event", event).put("callId", callId)
        if (reason != null) payload.put("reason", reason)
        socket?.send(payload.toString())
    }

    /** Tells the server this phone answered, so the household's other phones stop ringing. */
    fun notifyAnswered(callId: String?) = sendOverSocket("CALL_ANSWERED", callId)

    private fun pollCallSignal() {
        try {
            val request = Request.Builder()
                .url(signalingUrl)
                // Never accept a cached copy: a stale "still ringing" value is
                // exactly what kept the phone ringing after the kiosk hung up.
                .header("Cache-Control", "no-cache, no-store")
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
                    // Clock-based staleness only applies to NEW calls. It used to
                    // apply to every event, so when the kiosk clock was >10 min off
                    // the phone's, the kiosk's CALL_ENDED was ignored and the phone
                    // kept showing a call that no longer existed (QA #6).
                    if (event == "CALL_INITIATED" && timestamp > 0 &&
                        Math.abs(now - timestamp) > 600_000L && callId == lastProcessedCallId
                    ) return

                    if (event == "CALL_INITIATED" && (callId != lastProcessedCallId || timestamp > lastProcessedTimestamp)) {
                        lastProcessedCallId = callId
                        lastProcessedTimestamp = timestamp
                        Log.i("CallSignalingManager", "HTTPS SIGNAL RECEIVED: INCOMING CALL from Kiosk! callId=$callId " +
                            "age(kioskClock->phoneClock)=${now - timestamp}ms")
                        handleIncomingCall(json)
                    } else if (event == "CALL_ANSWERED" && callId.isNotBlank() &&
                        callId == controller.currentCredentials?.callId
                    ) {
                        // First answer wins: another family phone took the call.
                        controller.onAnsweredElsewhere(callId, json.optString("answeredBy"))
                    } else if ((event == "DOOR_UNLOCKED" || event == "DOOR_UNLOCK_FAILED") &&
                        callId == controller.currentCredentials?.callId &&
                        json.optString("requestId") != lastDoorResultRequestId
                    ) {
                        // Kiosk's answer to our unlock request.
                        lastDoorResultRequestId = json.optString("requestId")
                        val ok = event == "DOOR_UNLOCKED"
                        Log.i("CallSignalingManager", "Door result from kiosk: $event callId=$callId")
                        controller.onDoorUnlockResult(ok, json.optString("message"), json.optString("requestId"))
                    } else if (event == "CALL_ENDED" && json.optString("sender") != "MOBILE_RECEIVER" &&
                        callId.isNotBlank() && callId == controller.currentCredentials?.callId
                    ) {
                        Log.i("CallSignalingManager", "HTTPS SIGNAL RECEIVED: CALL ENDED from Kiosk for callId=$callId " +
                            "reason=${json.optString("reason")}")
                        // Ends ringing OR an answered call the kiosk already left.
                        // Does not echo a CALL_ENDED back (it used to, via declineCall).
                        controller.onKioskEndedCall(callId)
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
            // Let the kiosk know at least one phone of the unit is reachable.
            RingReceipt.send(context, callId)
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

                CallProvider.LIVEKIT -> {
                    val lk = json.optJSONObject("livekit")
                    LiveKitConfig.receiverCredentials(
                        context = context,
                        callId = callId,
                        visitorName = visitorName,
                        unitName = unitName,
                        kioskName = kioskName,
                        url = lk?.optString("url"),
                        roomName = lk?.optString("roomName"),
                        tokenFromSignal = lk?.optString("token")
                    )
                }
            }

            // Trigger incoming call UI immediately on mobile phone!
            // Ring exactly as long as the kiosk does (+5 s grace so the kiosk's
            // own "no answer" normally ends it first) — QA #7.
            // QA #7: the phone hears about the call ~1 s after the kiosk starts
            // ringing, so ringing for the same time ends both screens together.
            // (The old +5 s grace kept the phone ringing after the kiosk had
            // already shown "not answered".) If the phone's timer fires first,
            // it tells the kiosk "NO_ANSWER" and the kiosk shows not-answered.
            val ringSeconds = json.optInt("ringTimeoutSeconds", 45).coerceIn(15, 120)
            controller.triggerIncomingCall(credentials, ringTimeoutMs = ringSeconds * 1000L)
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
    /** One ordered queue for phone → kiosk writes, so a retried older message can't overwrite a newer one. */
    private val writeQueue = Executors.newSingleThreadExecutor()

    private fun postToKiosk(payload: JSONObject, what: String) {
        writeQueue.execute {
            try {
                payload.put("sender", "MOBILE_RECEIVER")
                if (!payload.has("messageId")) {
                    payload.put("messageId", "m-${System.currentTimeMillis()}-${(1000..9999).random()}")
                }
                payload.put("timestamp", System.currentTimeMillis())
                val body = payload.toString()
                // Before the call is answered the relay is the ONLY way to reach
                // the kiosk (e.g. "declined"), so a failed write is retried.
                // Same messageId every time → the kiosk acts on it once.
                for (attempt in 1..RELAY_WRITE_ATTEMPTS) {
                    val ok = try {
                        val req = Request.Builder()
                            .url(kioskInboxUrl)
                            .post(body.toRequestBody(jsonMediaType))
                            .build()
                        client.newCall(req).execute().use { resp ->
                            Log.i("CallSignalingManager", "$what sent to kiosk via relay http=${resp.code} attempt=$attempt")
                            resp.isSuccessful
                        }
                    } catch (e: Exception) {
                        Log.w("CallSignalingManager", "Relay send of $what failed (attempt $attempt): ${e.message}")
                        false
                    }
                    if (ok) break
                    Thread.sleep(600L * attempt)
                }
            } catch (e: Exception) {
                Log.w("CallSignalingManager", "Relay send of $what failed: ${e.message}")
            }
        }
    }

    fun broadcastCallEnded(callId: String?, reason: String = "HANGUP") {
        sendOverSocket("CALL_ENDED", callId, reason)
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
        pauseSocket()
    }

    private companion object {
        /** Relay poll interval. Was 1.5 s; ringing state changes need to show fast. */
        const val POLL_INTERVAL_MS = 1000L
        const val RELAY_WRITE_ATTEMPTS = 3
    }
}
