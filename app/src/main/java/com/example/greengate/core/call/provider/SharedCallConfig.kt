package com.example.greengate.core.call.provider

object SharedCallConfig {
    const val DEFAULT_TWILIO_ROOM_NAME = "test-room"

    private const val ACCOUNT_PREFIX = "AC"
    private const val API_KEY_PREFIX = "SK"
    private val ACCOUNT_SID = ACCOUNT_PREFIX + "cf2ed48ed7a3e798a8c0bb374c2aef2c"
    private val API_KEY_SID = API_KEY_PREFIX + "5fc3bc76485f18e939217084d565d1e8"
    private const val API_KEY_SECRET = "4fO7GTN7DXKhdADjqmWGQ9SUbKXJS1Sb"

    var twilioRoomName: String = DEFAULT_TWILIO_ROOM_NAME

    val twilioReceiverToken: String
        get() = generateTwilioToken("receiver")

    val twilioCallerToken: String
        get() = generateTwilioToken("caller")

    fun generateTwilioToken(identity: String, ttlSeconds: Long = 86400L): String {
        return try {
            val now = System.currentTimeMillis() / 1000
            val exp = now + ttlSeconds
            val header = """{"alg":"HS256","typ":"JWT","cty":"twilio-fpa;v=1"}"""
            val payload = """{"jti":"$API_KEY_SID-$now","grants":{"identity":"$identity","video":{}},"iat":$now,"exp":$exp,"iss":"$API_KEY_SID","sub":"$ACCOUNT_SID"}"""

            val headerBase64 = base64Url(header.toByteArray(Charsets.UTF_8))
            val payloadBase64 = base64Url(payload.toByteArray(Charsets.UTF_8))

            val signatureInput = "$headerBase64.$payloadBase64"
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val secretKey = javax.crypto.spec.SecretKeySpec(API_KEY_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKey)
            val signature = mac.doFinal(signatureInput.toByteArray(Charsets.UTF_8))
            val signatureBase64 = base64Url(signature)

            "$signatureInput.$signatureBase64"
        } catch (e: Exception) {
            ""
        }
    }

    private fun base64Url(bytes: ByteArray): String {
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE)
    }

    // Vonage UNIFIED-environment credentials (dashboard.vonage.com).
    // In the unified environment the client "apiKey" is the Application ID (a
    // string, not a number) and the connection token is an RS256 JWT minted by
    // Vonage's "Create a Session" tool. That token CANNOT be produced on-device
    // from an HS256 secret, so for the static demo we ship the real Application
    // ID, Session ID and dashboard token as defaults and use the token as-is.
    // These MUST match the kiosk app so both ends join the same session.
    // Must match the kiosk's application. The receiver normally uses the
    // apiKey / sessionId / token broadcast by the kiosk (which now creates a
    // session under this app and mints an RS256 token on-device); these
    // defaults are only a last-resort fallback and intentionally carry no
    // stale session/token from a previous app.
    const val DEFAULT_VONAGE_API_KEY = "5c2ff88b-80c7-4596-b669-9f6fb1042e6c"
    const val DEFAULT_VONAGE_API_SECRET = ""
    const val DEFAULT_VONAGE_SESSION_ID = ""
    const val DEFAULT_VONAGE_TOKEN = ""

    var vonageApiKey: String = DEFAULT_VONAGE_API_KEY
    var vonageApiSecret: String = DEFAULT_VONAGE_API_SECRET
    var vonageSessionId: String = DEFAULT_VONAGE_SESSION_ID
    /**
     * Vonage client connection token. In the unified environment this is an
     * RS256 JWT issued by Vonage (not derivable on-device), so it is shipped as
     * [DEFAULT_VONAGE_TOKEN] and used verbatim. The receiver normally uses the
     * token broadcast by the kiosk; this default is the fallback.
     */
    var vonageToken: String = DEFAULT_VONAGE_TOKEN
        get() = field.ifBlank { DEFAULT_VONAGE_TOKEN }

    fun saveVonageCredentials(context: android.content.Context, apiKey: String, apiSecret: String, sessionId: String, token: String = "") {
        vonageApiKey = apiKey.trim().ifBlank { DEFAULT_VONAGE_API_KEY }
        vonageApiSecret = apiSecret.trim().ifBlank { DEFAULT_VONAGE_API_SECRET }
        vonageSessionId = sessionId.trim().ifBlank { DEFAULT_VONAGE_SESSION_ID }
        // Unified tokens (RS256) can't be minted on-device; keep the shipped
        // default unless an explicit token is supplied.
        vonageToken = token.trim().ifBlank { DEFAULT_VONAGE_TOKEN }

        val prefs = context.getSharedPreferences("vonage_config_v2", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("vonage_api_key", vonageApiKey)
            .putString("vonage_api_secret", vonageApiSecret)
            .putString("vonage_session_id", vonageSessionId)
            .putString("vonage_token", vonageToken)
            .apply()
    }

    fun loadVonageCredentials(context: android.content.Context) {
        val prefs = context.getSharedPreferences("vonage_config_v2", android.content.Context.MODE_PRIVATE)
        vonageApiKey = prefs.getString("vonage_api_key", DEFAULT_VONAGE_API_KEY) ?: DEFAULT_VONAGE_API_KEY
        vonageApiSecret = prefs.getString("vonage_api_secret", DEFAULT_VONAGE_API_SECRET) ?: DEFAULT_VONAGE_API_SECRET
        vonageSessionId = prefs.getString("vonage_session_id", DEFAULT_VONAGE_SESSION_ID) ?: DEFAULT_VONAGE_SESSION_ID
        vonageToken = prefs.getString("vonage_token", DEFAULT_VONAGE_TOKEN) ?: DEFAULT_VONAGE_TOKEN
    }

    fun generateVonageToken(
        apiKey: String,
        apiSecret: String,
        sessionId: String,
        role: String = "publisher",
        ttlSeconds: Long = 86400L
    ): String {
        return try {
            val now = System.currentTimeMillis() / 1000
            val exp = now + ttlSeconds
            val nonce = java.util.UUID.randomUUID().toString()
            val header = """{"alg":"HS256","typ":"JWT"}"""
            // Matches generate_token.js / the kiosk: scope:"session.connect" is required.
            val payload = """{"iss":"$apiKey","ist":"project","iat":$now,"exp":$exp,"session_id":"$sessionId","nonce":"$nonce","role":"$role","scope":"session.connect","create_time":$now,"expire_time":$exp}"""

            val headerBase64 = base64Url(header.toByteArray(Charsets.UTF_8))
            val payloadBase64 = base64Url(payload.toByteArray(Charsets.UTF_8))

            val signatureInput = "$headerBase64.$payloadBase64"
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val secretKey = javax.crypto.spec.SecretKeySpec(apiSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKey)
            val signature = mac.doFinal(signatureInput.toByteArray(Charsets.UTF_8))
            val signatureBase64 = base64Url(signature)

            "$signatureInput.$signatureBase64"
        } catch (e: Exception) {
            ""
        }
    }

    fun createTwilioReceiverCredentials(
        visitorName: String = "John Visitor",
        unitName: String = "Unit 1204",
        kioskName: String = "Main Entrance Kiosk",
        roomName: String = twilioRoomName,
        token: String = twilioReceiverToken
    ): ReceiverCallCredentials.Twilio {
        return ReceiverCallCredentials.Twilio(
            callId = "call-${System.currentTimeMillis()}",
            visitorName = visitorName,
            unitName = unitName,
            kioskName = kioskName,
            roomName = roomName,
            accessToken = token
        )
    }

    fun createVonageReceiverCredentials(
        visitorName: String = "John Visitor",
        unitName: String = "Unit 1204",
        kioskName: String = "Main Entrance Kiosk",
        apiKey: String = vonageApiKey,
        sessionId: String = vonageSessionId,
        token: String = vonageToken
    ): ReceiverCallCredentials.Vonage {
        return ReceiverCallCredentials.Vonage(
            callId = "call-${System.currentTimeMillis()}",
            visitorName = visitorName,
            unitName = unitName,
            kioskName = kioskName,
            apiKey = apiKey,
            sessionId = sessionId,
            token = token
        )
    }
}
