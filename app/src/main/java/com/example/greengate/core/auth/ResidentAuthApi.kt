package com.example.greengate.core.auth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One household the signed-in number belongs to. A number can be registered in several units or condos. */
data class Household(
    val personId: Int,
    val requestId: Int,
    val condoId: Int,
    val condoName: String,
    val block: String,
    val floor: String,
    val unit: String,
    /** "Owner" or "Tenant". */
    val residentType: String,
    /** "Primary", "Secondary", "Child" or "Other". */
    val role: String,
    val name: String
)

/** A failed call. [code] is the API's `error` value (or `network_error`); [message] is written for the resident. */
class ResidentApiException(val status: Int, val code: String, override val message: String) : Exception(message)

internal class Tokens(val accessToken: String, val refreshToken: String)

/**
 * Resident app sign-in API of the onboarding service (app-web-ui-client-onboarding,
 * docs/RESIDENT_APP_API.md). Blocking calls: run them off the main thread.
 */
internal class ResidentAuthApi(baseUrl: String) {
    private val root = baseUrl.trimEnd('/') + "/api/db/resident-app"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Texts a code if the number is registered (the response is the same either way). Returns seconds until a resend is allowed. */
    fun sendCode(phone: String): Int =
        post("/auth/otp/send", JSONObject().put("phone", phone)).optInt("retryAfter", 60)

    fun verifyCode(phone: String, code: String, device: JSONObject): Pair<Tokens, List<Household>> {
        val body = post("/auth/otp/verify", JSONObject().put("phone", phone).put("code", code).put("device", device))
        return tokens(body) to households(body)
    }

    /** Rotates the refresh token: the one passed in stops working. */
    fun refresh(refreshToken: String): Tokens =
        tokens(post("/auth/refresh", JSONObject().put("refreshToken", refreshToken)))

    fun me(accessToken: String): List<Household> =
        households(execute(request("/me", accessToken).get().build()))

    fun logout(accessToken: String) {
        execute(request("/auth/logout", accessToken).post(EmptyJson).build())
    }

    private fun post(path: String, body: JSONObject) =
        execute(request(path).post(body.toString().toRequestBody(JsonType)).build())

    private fun request(path: String, accessToken: String? = null) = Request.Builder().url(root + path).apply {
        if (accessToken != null) header("Authorization", "Bearer $accessToken")
    }

    private fun execute(request: Request): JSONObject {
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw ResidentApiException(0, "network_error", "Can't reach GreenGate right now. Check your connection and try again.")
        }
        response.use {
            val body = runCatching { JSONObject(it.body?.string().orEmpty()) }.getOrDefault(JSONObject())
            if (!it.isSuccessful) {
                throw ResidentApiException(
                    it.code,
                    body.optString("error").ifBlank { "http_${it.code}" },
                    body.optString("message").ifBlank { "Something went wrong. Please try again." }
                )
            }
            return body
        }
    }

    private fun tokens(body: JSONObject) = Tokens(body.getString("accessToken"), body.getString("refreshToken"))

    private fun households(body: JSONObject): List<Household> {
        val list = body.optJSONArray("households") ?: return emptyList()
        return List(list.length()) { index ->
            val row = list.getJSONObject(index)
            Household(
                personId = row.optInt("personId"),
                requestId = row.optInt("requestId"),
                condoId = row.optInt("condoId"),
                condoName = row.text("condoName"),
                block = row.text("block"),
                floor = row.text("floor"),
                unit = row.text("unit"),
                residentType = row.text("residentType"),
                role = row.text("role"),
                name = row.text("name")
            )
        }
    }

    // optString turns a JSON null into the text "null".
    private fun JSONObject.text(name: String) = if (isNull(name)) "" else optString(name)

    private companion object {
        val JsonType = "application/json; charset=utf-8".toMediaType()
        val EmptyJson = "{}".toRequestBody(JsonType)
    }
}
