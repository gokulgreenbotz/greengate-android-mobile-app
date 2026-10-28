package com.greenbotz.greengate.core.session

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ResidentAuthApiTest {
    private val server = MockWebServer()
    private lateinit var api: ResidentAuthApi

    @Before fun setUp() {
        server.start()
        api = ResidentAuthApi(server.url("/v1/resident").toString())
    }

    @After fun tearDown() = server.shutdown()

    private fun answer(status: Int, body: String, retryAfter: String? = null) {
        val response = MockResponse().setResponseCode(status).setBody(body)
        if (retryAfter != null) response.setHeader("Retry-After", retryAfter)
        server.enqueue(response)
    }

    @Test fun sendWithOnePropertyIsSent() {
        answer(200, """{"status":"success","data":{"properties":[{"id":"p-1","name":"Palm Court"}],"sent":true}}""")
        val result = api.sendCode("+6591110001", null)
        assertEquals(SendResult.Sent(PropertyChoice("p-1", "Palm Court")), result)
        val request = server.takeRequest()
        assertEquals("/v1/resident/auth/otp/send", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("+6591110001", body.getString("phone"))
        assertEquals(false, body.has("property_id"))
    }

    @Test fun sendWithSeveralPropertiesAsksToChoose() {
        answer(200, """{"status":"success","data":{"properties":[{"id":"p-1","name":"A"},{"id":"p-2","name":"B"}],"sent":false}}""")
        val result = api.sendCode("+6591110001", null)
        assertEquals(SendResult.ChooseProperty(listOf(PropertyChoice("p-1", "A"), PropertyChoice("p-2", "B"))), result)
    }

    @Test fun sendForAPropertyNamesIt() {
        answer(200, """{"status":"success","data":{"properties":[{"id":"p-2","name":"B"}],"sent":true}}""")
        api.sendCode("+6591110001", "p-2")
        assertEquals("p-2", JSONObject(server.takeRequest().body.readUtf8()).getString("property_id"))
    }

    @Test fun verifyPostsSnakeCaseAndReadsTokens() {
        answer(200, """{"status":"success","data":{"access_token":"a","refresh_token":"r","expires_at":"x","property":{"id":"p-1","name":"A"}}}""")
        val device = JSONObject().put("platform", "android").put("model", "Pixel").put("app_version", "1.0")
        val tokens = api.verifyCode("+6591110001", "p-1", "123456", device)
        assertEquals("a", tokens.accessToken)
        assertEquals("r", tokens.refreshToken)
        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("p-1", body.getString("property_id"))
        assertEquals("123456", body.getString("code"))
        assertEquals("1.0", body.getJSONObject("device").getString("app_version"))
    }

    @Test fun errorsCarryTheCodeAndMessage() {
        answer(400, """{"status":"error","data":{"code":"INVALID_CODE"},"message":"That code isn't right or has expired"}""")
        try {
            api.verifyCode("+6591110001", "p-1", "000000", JSONObject())
            fail("expected an error")
        } catch (e: ResidentApiException) {
            assertEquals(400, e.status)
            assertEquals("INVALID_CODE", e.code)
            assertEquals("That code isn't right or has expired", e.message)
            assertNull(e.retryAfterSeconds)
        }
    }

    @Test fun tooManyCodesSaysWhenToRetry() {
        answer(429, """{"status":"error","message":"Too many codes"}""", retryAfter = "600")
        try {
            api.sendCode("+6591110001", null)
            fail("expected an error")
        } catch (e: ResidentApiException) {
            assertEquals(600, e.retryAfterSeconds)
        }
    }

    @Test fun meReadsNamePropertyAndHouseholds() {
        answer(200, """{"status":"success","data":{"property":{"id":"p-1","name":"Palm Court"},"name":"Ana Tan","households":[{"space_id":"s-1","space_path":"Tower A / 12 / 12-03","role":"owner"}],"phone_masked":"+65*****001"}}""")
        val me = api.me("access")
        assertEquals(Me("Ana Tan", PropertyChoice("p-1", "Palm Court"), listOf(Household("s-1", "Tower A / 12 / 12-03", "owner"))), me)
        assertEquals("Bearer access", server.takeRequest().getHeader("Authorization"))
    }

    @Test fun pushTokenBody() {
        answer(200, """{"status":"success","data":{}}""")
        api.registerFcmToken("access", "fcm-1")
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/v1/resident/device/push-token", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("fcm-1", body.getString("fcm_token"))
        assertEquals("", body.getString("voip_token"))
        assertEquals("android", body.getString("platform"))
    }

    @Test fun accessQrReadsValueAndExpiry() {
        answer(200, """{"status":"success","data":{"qr":"GG1.abc","expires_at":"2026-10-28T09:00:30.25Z"}}""")
        val qr = api.accessQr("access")
        assertEquals("GG1.abc", qr.value)
        assertEquals(1793178030250L, qr.expiresAtMillis)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/resident/access-qr", request.path)
    }
}
