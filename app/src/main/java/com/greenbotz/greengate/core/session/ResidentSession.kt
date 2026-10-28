package com.greenbotz.greengate.core.session

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.greenbotz.greengate.BuildConfig
import com.greenbotz.greengate.core.push.currentPushToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** A 401: the access token expired, or (from a refresh) the session is over. */
private val ResidentApiException.unauthorized get() = status == 401

/** Errors after which the session is over and the resident has to sign in again. */
private val ResidentApiException.sessionEnded get() = unauthorized || code == "RESIDENT_NOT_FOUND"

/**
 * The resident's sign-in: SMS code against the GreenGate Backend's resident app API.
 * Call [init] before use. Functions are main-safe; [state] changes on the caller's thread.
 */
object ResidentSession {
    sealed interface State {
        /** Checking a saved sign-in at launch. */
        data object Restoring : State
        data object SignedOut : State
        /** [me] is null when the app started offline; it fills on the next launch. */
        data class SignedIn(val me: Me?) : State
        /** The built-in demo account: app screens only, no server session, not kept across launches. */
        data object Demo : State
    }

    var state by mutableStateOf<State>(State.Restoring)
        private set

    /** First name shown in greetings: the signed-in resident's, "Alex" for the demo account. */
    val firstName: String
        get() = when (val s = state) {
            is State.SignedIn -> s.me?.name?.split(' ')
                ?.firstOrNull { it.isNotBlank() && it.trimEnd('.').lowercase() !in Titles } ?: "there"
            else -> "Alex"
        }

    private val Titles = setOf("mr", "mrs", "ms", "miss", "mdm", "madam", "dr", "prof", "mx", "master")

    /** Property of the signed-in resident; blank until it loads. */
    val condoName: String
        get() = when (val s = state) {
            is State.SignedIn -> s.me?.property?.name.orEmpty()
            else -> "Marina Bay Residences"
        }

    /** The first household's place in the property, e.g. "Tower A · 12 · 12-03". */
    val unitLabel: String
        get() = when (val s = state) {
            is State.SignedIn -> s.me?.households?.firstOrNull()?.spacePath
                ?.split('/')?.map { it.trim() }?.filter { it.isNotEmpty() }?.joinToString(" · ").orEmpty()
            else -> "Tower A · Unit 12-03"
        }

    private lateinit var store: SessionStore
    private lateinit var api: ResidentAuthApi
    private val refreshLock = Mutex()
    @Volatile private var accessToken: String? = null

    fun init(context: Context) {
        if (::store.isInitialized) return
        store = SessionStore(context.applicationContext)
        api = ResidentAuthApi(API_URL)
    }

    /** Turns a saved refresh token back into a session. Runs once per process. */
    suspend fun restore() {
        if (state != State.Restoring) return
        state = if (io { store.refreshToken } == null) State.SignedOut else try {
            State.SignedIn(authorized(api::me))
        } catch (e: ResidentApiException) {
            // Offline or a server error: keep the sign-in and let the next call retry.
            if (e.sessionEnded) State.SignedOut else State.SignedIn(null)
        }
        registerPush()
    }

    /**
     * Gives the server this phone's FCM token so the office can send it notifications.
     * Best effort: a failure here never affects sign-in. [fcmToken] is the new token
     * from onNewToken; without it the current one is fetched.
     */
    suspend fun registerPush(fcmToken: String? = null) {
        if (state !is State.SignedIn) return
        try {
            val token = fcmToken ?: currentPushToken() ?: return
            authorized { access -> api.registerFcmToken(access, token) }
        } catch (_: Exception) {
            // Offline, or Firebase not ready: retried on the next launch or token refresh.
        }
    }

    /** Intercom calls from the Backend arrive by FCM push; there is no signal relay. */
    val usesCallRelay: Boolean get() = false

    /** The Backend has no call WebSocket; calls arrive by FCM push. */
    val callSocketUrl: String? get() = null

    /**
     * Access token for the call WebSocket, or null when signed out. [forceRefresh]
     * after the server refused the previous one (access tokens last 30 minutes).
     */
    suspend fun callSocketToken(forceRefresh: Boolean = false): String? {
        if (state !is State.SignedIn) return null
        return try {
            if (forceRefresh) refreshAccessToken(accessToken) else accessToken ?: refreshAccessToken(null)
        } catch (_: ResidentApiException) {
            null
        }
    }

    private var greenBotEnabled by mutableStateOf(false)

    /** True only while signed in to a server that answers Green Bot with Claude (see [checkGreenBot]). */
    val greenBotOnline: Boolean get() = greenBotEnabled && state is State.SignedIn

    /** Asks the server whether Green Bot chat is on. Anything but a clear yes keeps the built-in help. */
    suspend fun checkGreenBot() {
        greenBotEnabled = state is State.SignedIn && try {
            authorized { api.greenBotEnabled(it) }
        } catch (_: ResidentApiException) {
            false
        }
    }

    /** Green Bot reply (`{ reply, action }`) for a signed-in resident. Throws [ResidentApiException]. */
    suspend fun askGreenBot(messages: JSONArray): JSONObject = authorized { api.askGreenBot(it, messages) }

    /**
     * Asks for an SMS code. [phone] includes the country code; [propertyId] is the
     * property chosen when the phone is in several.
     */
    suspend fun sendCode(phone: String, propertyId: String? = null): SendResult = io { api.sendCode(phone, propertyId) }

    /** Checks the code sent for [propertyId] and signs in, loading the resident for the dashboard. */
    suspend fun verifyCode(phone: String, propertyId: String, code: String) {
        val tokens = io { api.verifyCode(phone, propertyId, code, device()) }
        io { store.refreshToken = tokens.refreshToken }
        accessToken = tokens.accessToken
        val me = try {
            authorized(api::me)
        } catch (e: ResidentApiException) {
            if (e.sessionEnded) throw e
            null
        }
        state = State.SignedIn(me)
        registerPush()
    }

    fun startDemo() {
        state = State.Demo
    }

    /** Signs out on this phone at once, then ends the session on the server as well, if it can. */
    suspend fun signOut() {
        val server = api
        val access = accessToken
        val refresh = io { store.refreshToken }
        accessToken = null
        io { store.refreshToken = null }
        state = State.SignedOut
        try {
            io {
                // No live access token: spend the refresh token on one to sign out with.
                val token = access ?: refresh?.let { server.refresh(it).accessToken } ?: return@io
                try {
                    server.logout(token)
                } catch (e: ResidentApiException) {
                    if (!e.unauthorized || access == null || refresh == null) throw e
                    server.logout(server.refresh(refresh).accessToken)
                }
            }
        } catch (_: ResidentApiException) {
            // Already signed out here; the server session lapses with its refresh token.
        }
    }

    /** Calls the API with an access token, refreshing first when there is none and once more after a 401. */
    private suspend fun <T> authorized(call: (String) -> T): T {
        try {
            val token = accessToken ?: refreshAccessToken(null)
            return try {
                io { call(token) }
            } catch (e: ResidentApiException) {
                if (!e.unauthorized) throw e
                val fresh = refreshAccessToken(token)
                io { call(fresh) }
            }
        } catch (e: ResidentApiException) {
            if (e.sessionEnded) {
                accessToken = null
                io { store.refreshToken = null }
                state = State.SignedOut
            }
            throw e
        }
    }

    // One refresh at a time: a refresh token works once, and the API ends the session
    // when a used one comes back, so concurrent callers share the first caller's result.
    private suspend fun refreshAccessToken(stale: String?): String {
        refreshLock.withLock {
            accessToken?.takeIf { it != stale }?.let { return it }
            val refreshToken = io { store.refreshToken }
                ?: throw ResidentApiException(401, "SESSION_REVOKED", "Your session has expired. Please sign in again.")
            val tokens = io { api.refresh(refreshToken) }
            io { store.refreshToken = tokens.refreshToken }
            accessToken = tokens.accessToken
            return tokens.accessToken
        }
    }

    // Shown to admins as the resident's last login. Build.MODEL is the hardware model,
    // not the user-assigned device name (that is personal data).
    private fun device() = JSONObject()
        .put("platform", "android")
        .put("model", Build.MODEL)
        .put("app_version", BuildConfig.VERSION_NAME)

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
