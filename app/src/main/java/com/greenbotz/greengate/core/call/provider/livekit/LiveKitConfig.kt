package com.greenbotz.greengate.core.call.provider.livekit

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Base64
import com.greenbotz.greengate.core.call.provider.ReceiverCallCredentials
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * LiveKit Cloud project settings + in-app token minting (R&D ONLY).
 *
 * Use the SAME URL / API key / API secret as the kiosk's LiveKitConfig.
 *
 * Every phone mints its own token with a unique identity
 * ("resident-<device>") — LiveKit removes an earlier participant that joins
 * with a duplicate identity, so a shared token would let one family phone
 * kick another out of the call.
 *
 * Security: the API secret is compiled into the APK (same pattern as
 * SharedCallConfig). Before production the backend must issue tokens.
 */
object LiveKitConfig {

    // TODO(LiveKit): paste your LiveKit Cloud project values here (same as kiosk).
    const val SERVER_URL = "wss://greengage-intercom-z8gifw0p.livekit.cloud"
    private const val API_KEY = "APIXUkTYNGVosNt"
    private const val API_SECRET = "7njhkJbHTydxD4OFJ8OUflwaBmxqVrhefTzKxiIP1g3A"

    /** Kiosk identities start with this (see kiosk LiveKitConfig). */
    const val KIOSK_IDENTITY_PREFIX = "kiosk"

    val isConfigured: Boolean
        get() = SERVER_URL.startsWith("wss://") &&
            !SERVER_URL.contains("YOUR-PROJECT") &&
            !API_KEY.startsWith("YOUR_") &&
            !API_SECRET.startsWith("YOUR_")

    /** Stable per-device identity, unique across the household's phones. */
    @SuppressLint("HardwareIds")
    fun residentIdentity(context: Context): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        return "resident-" + (androidId?.takeIf { it.isNotBlank() } ?: fallbackId)
    }

    private val fallbackId: String by lazy { java.util.UUID.randomUUID().toString().take(12) }

    /**
     * Builds the phone's credentials for an incoming LiveKit call. A token in
     * the call signal (future: issued by the backend) wins; otherwise this
     * phone mints its own.
     */
    fun receiverCredentials(
        context: Context,
        callId: String,
        visitorName: String,
        unitName: String,
        kioskName: String,
        url: String?,
        roomName: String?,
        tokenFromSignal: String?
    ): ReceiverCallCredentials.LiveKit {
        val room = roomName?.takeIf { it.isNotBlank() } ?: "gg-$callId"
        val token = tokenFromSignal?.takeIf { it.isNotBlank() }
            ?: createToken(residentIdentity(context), room, "Resident")
        return ReceiverCallCredentials.LiveKit(
            callId = callId,
            visitorName = visitorName,
            unitName = unitName,
            kioskName = kioskName,
            serverUrl = url?.takeIf { it.startsWith("wss://") } ?: SERVER_URL,
            roomName = room,
            token = token
        )
    }

    /** HS256 LiveKit access token: join [roomName], publish/subscribe audio, video, data. */
    fun createToken(
        identity: String,
        roomName: String,
        displayName: String = identity,
        ttlSeconds: Long = 2 * 60 * 60
    ): String {
        val now = System.currentTimeMillis() / 1000
        val header = JSONObject()
            .put("alg", "HS256")
            .put("typ", "JWT")
        val grants = JSONObject()
            .put("room", roomName)
            .put("roomJoin", true)
            .put("canPublish", true)
            .put("canSubscribe", true)
            .put("canPublishData", true)
        val payload = JSONObject()
            .put("iss", API_KEY)
            .put("sub", identity)
            .put("name", displayName)
            .put("nbf", now - 10)
            .put("exp", now + ttlSeconds)
            .put("jti", "$identity-$now")
            .put("video", grants)

        val signingInput = b64(header.toString().toByteArray(Charsets.UTF_8)) + "." +
            b64(payload.toString().toByteArray(Charsets.UTF_8))
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(API_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return signingInput + "." + b64(mac.doFinal(signingInput.toByteArray(Charsets.UTF_8)))
    }

    private fun b64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
