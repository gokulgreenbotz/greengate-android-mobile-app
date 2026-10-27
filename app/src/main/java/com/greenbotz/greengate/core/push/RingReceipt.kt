package com.greenbotz.greengate.core.push

import android.content.Context
import android.util.Log
import com.greenbotz.greengate.core.call.provider.livekit.LiveKitConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tells the kiosk "this phone is ringing" for a call. If no phone of the unit
 * reports in, the kiosk shows the visitor "Residents can't be reached"
 * instead of ringing into nothing. Sent once per call per phone (the
 * foreground poller and the background service may both see the call).
 */
object RingReceipt {
    private const val URL = "https://kvdb.io/JvTpnXFCynR2tFAjEa8ysm/ring_receipt"
    private val queue = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
    @Volatile private var lastSentCallId: String? = null

    fun send(context: Context, callId: String) {
        if (callId.isBlank() || callId == lastSentCallId) return
        lastSentCallId = callId
        val device = runCatching { LiveKitConfig.residentIdentity(context.applicationContext) }.getOrDefault("resident")
        queue.execute {
            val body = JSONObject()
                .put("event", "RINGING")
                .put("sender", "MOBILE_RECEIVER")
                .put("callId", callId)
                .put("device", device)
                .put("timestamp", System.currentTimeMillis())
                .toString()
            for (attempt in 1..3) {
                val ok = try {
                    client.newCall(
                        Request.Builder().url(URL)
                            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                            .build()
                    ).execute().use { it.isSuccessful }
                } catch (e: Exception) {
                    Log.w("RingReceipt", "send failed (attempt $attempt): ${e.message}")
                    false
                }
                if (ok) { Log.i("RingReceipt", "RINGING sent callId=$callId"); return@execute }
                Thread.sleep(500L * attempt)
            }
        }
    }
}
