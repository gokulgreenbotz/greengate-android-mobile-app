package com.greenbotz.greengate.core.session

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.greenbotz.greengate.BuildConfig

/** Resident app API of the GreenGate Backend (api-gateway). */
const val API_URL = BuildConfig.API_BASE_URL + "/v1/resident"

/** A property the signed-in phone belongs to. */
data class PropertyChoice(val id: String, val name: String)

/** One unit of the signed-in resident, e.g. spacePath "Tower A / 12 / 12-03". */
data class Household(val spaceId: String, val spacePath: String, val role: String)

/** The signed-in resident: their name, the property of the session and their units there. */
data class Me(val name: String, val property: PropertyChoice, val households: List<Household>)

/** Result of asking for a code: sent for one property, or the phone is in several and one must be chosen. */
sealed interface SendResult {
    data class Sent(val property: PropertyChoice) : SendResult
    data class ChooseProperty(val properties: List<PropertyChoice>) : SendResult
}

/**
 * A failed call. [code] is the API's `data.code` (or `network_error`, or `http_<status>`);
 * [message] is written for the resident; [retryAfterSeconds] comes with a 429.
 */
class ResidentApiException(
    val status: Int,
    val code: String,
    override val message: String,
    val retryAfterSeconds: Int? = null
) : Exception(message)

internal class Tokens(val accessToken: String, val refreshToken: String)

/**
 * Resident app sign-in API of the GreenGate Backend (`/v1/resident`). Blocking calls:
 * run them off the main thread. Responses are `{status, message, data}`.
 */
internal class ResidentAuthApi(apiUrl: String) {
    private val root = apiUrl.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    // Claude can take a while to answer; the server gives up after about 40 seconds.
    private val assistantClient by lazy { client.newBuilder().readTimeout(45, TimeUnit.SECONDS).build() }

    /** Texts a code. [propertyId] picks one property when the phone is in several. */
    fun sendCode(phone: String, propertyId: String?): SendResult {
        val body = JSONObject().put("phone", phone)
        if (propertyId != null) body.put("property_id", propertyId)
        val data = post("/auth/otp/send", body)
        val properties = data.optJSONArray("properties") ?: JSONArray()
        val choices = List(properties.length()) { property(properties.getJSONObject(it)) }
        return if (data.optBoolean("sent")) SendResult.Sent(choices.first()) else SendResult.ChooseProperty(choices)
    }

    fun verifyCode(phone: String, propertyId: String, code: String, device: JSONObject): Tokens = tokens(
        post(
            "/auth/otp/verify",
            JSONObject().put("phone", phone).put("property_id", propertyId).put("code", code).put("device", device)
        )
    )

    /** Rotates the refresh token: the one passed in stops working. */
    fun refresh(refreshToken: String): Tokens =
        tokens(post("/auth/refresh", JSONObject().put("refresh_token", refreshToken)))

    fun me(accessToken: String): Me {
        val data = execute(request("/me", accessToken).get().build())
        val list = data.optJSONArray("households") ?: JSONArray()
        return Me(
            name = data.text("name"),
            property = property(data.optJSONObject("property") ?: JSONObject()),
            households = List(list.length()) { index ->
                val row = list.getJSONObject(index)
                Household(spaceId = row.text("space_id"), spacePath = row.text("space_path"), role = row.text("role"))
            }
        )
    }

    fun logout(accessToken: String) {
        execute(request("/auth/logout", accessToken).post(EmptyJson).build())
    }

    /** Registers this device's FCM token on the signed-in session, so calls and notices reach it. */
    fun registerFcmToken(accessToken: String, fcmToken: String) {
        val body = JSONObject().put("fcm_token", fcmToken).put("voip_token", "").put("platform", "android")
        execute(request("/device/push-token", accessToken).put(body.toString().toRequestBody(JsonType)).build())
    }

    /** Whether the server answers Green Bot with Claude (it has an Anthropic key). */
    fun greenBotEnabled(accessToken: String): Boolean =
        execute(request("/assistant/status", accessToken).get().build()).optBoolean("enabled", false)

    /**
     * Green Bot reply from Claude, through the onboarding service (the Anthropic key stays there).
     * [messages] are `{ role, text }` turns, oldest first, ending with the resident's question.
     * Returns `{ reply, action }`.
     */
    fun askGreenBot(accessToken: String, messages: JSONArray): JSONObject = execute(
        request("/assistant/chat", accessToken).post(JSONObject().put("messages", messages).toString().toRequestBody(JsonType)).build(),
        assistantClient
    )

    private fun post(path: String, body: JSONObject) =
        execute(request(path).post(body.toString().toRequestBody(JsonType)).build())

    private fun request(path: String, accessToken: String? = null) = Request.Builder().url(root + path).apply {
        if (accessToken != null) header("Authorization", "Bearer $accessToken")
    }

    /** Returns the response's `data`. */
    private fun execute(request: Request, http: OkHttpClient = client): JSONObject {
        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw ResidentApiException(0, "network_error", "Can't reach GreenGate right now. Check your connection and try again.")
        }
        response.use {
            val body = runCatching { JSONObject(it.body?.string().orEmpty()) }.getOrDefault(JSONObject())
            val data = body.optJSONObject("data") ?: JSONObject()
            if (!it.isSuccessful) {
                throw ResidentApiException(
                    it.code,
                    data.optString("code").ifBlank { "http_${it.code}" },
                    body.optString("message").ifBlank { "Something went wrong. Please try again." },
                    it.header("Retry-After")?.toIntOrNull()
                )
            }
            return data
        }
    }

    private fun tokens(data: JSONObject) = Tokens(data.getString("access_token"), data.getString("refresh_token"))

    private fun property(row: JSONObject) = PropertyChoice(id = row.text("id"), name = row.text("name"))

    // optString turns a JSON null into the text "null".
    private fun JSONObject.text(name: String) = if (isNull(name)) "" else optString(name)

    private companion object {
        val JsonType = "application/json; charset=utf-8".toMediaType()
        val EmptyJson = "{}".toRequestBody(JsonType)
    }
}
