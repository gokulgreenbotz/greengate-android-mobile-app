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
import org.json.JSONObject

private const val AUTH_REQUIRED = "resident_app_auth_required"

/** Errors after which the session is over and the resident has to sign in again. */
private val SessionEndedCodes = setOf(AUTH_REQUIRED, "invalid_refresh_token", "app_access_unavailable")

/**
 * The resident's sign-in: SMS code against the onboarding service's resident app API.
 * Call [init] before use. Functions are main-safe; [state] changes on the caller's thread.
 */
object ResidentSession {
    sealed interface State {
        /** Checking a saved sign-in at launch. */
        data object Restoring : State
        data object SignedOut : State
        /** [households] is empty when the app started offline; it fills on the next launch. */
        data class SignedIn(val households: List<Household>) : State
        /** The built-in demo account: app screens only, no server session, not kept across launches. */
        data object Demo : State
    }

    var state by mutableStateOf<State>(State.Restoring)
        private set

    /** First name shown in greetings: the signed-in resident's, "Alex" for the demo account. */
    val firstName: String
        get() = when (val s = state) {
            is State.SignedIn -> s.households.firstOrNull()?.name?.split(' ')
                ?.firstOrNull { it.isNotBlank() && it.trimEnd('.').lowercase() !in Titles } ?: "there"
            else -> "Alex"
        }

    private val Titles = setOf("mr", "mrs", "ms", "miss", "mdm", "madam", "dr", "prof", "mx", "master")

    /** Condo of the signed-in resident's first household; blank until it loads. */
    val condoName: String
        get() = when (val s = state) {
            is State.SignedIn -> s.households.firstOrNull()?.condoName.orEmpty()
            else -> "Marina Bay Residences"
        }

    /** Block and unit of the first household, e.g. "Block A · #05-01". */
    val unitLabel: String
        get() = when (val s = state) {
            is State.SignedIn -> s.households.firstOrNull()?.let { h ->
                val unit = if (h.floor.isNotBlank() && h.unit.isNotBlank()) "#${h.floor}-${h.unit}" else h.unit
                listOf(h.block.takeIf { it.isNotBlank() }?.let { "Block $it" }, unit.takeIf { it.isNotBlank() })
                    .filterNotNull().joinToString(" · ")
            }.orEmpty()
            else -> "Tower A · Unit 12-03"
        }

    /** Server the app signs in against. Debug builds start on Stage, release builds on Production. */
    var environment by mutableStateOf(if (BuildConfig.DEBUG) ApiEnvironment.STAGE else ApiEnvironment.PROD)
        private set

    private lateinit var store: SessionStore
    private lateinit var api: ResidentAuthApi
    private val refreshLock = Mutex()
    @Volatile private var accessToken: String? = null

    fun init(context: Context) {
        if (::store.isInitialized) return
        store = SessionStore(context.applicationContext)
        ApiEnvironment.entries.find { it.name == store.environment }?.let { environment = it }
        api = ResidentAuthApi(environment.apiUrl)
    }

    /** Points the app at another server. A session belongs to one server, so this signs out. */
    suspend fun switchEnvironment(target: ApiEnvironment) {
        if (target == environment) return
        val previous = api
        environment = target
        api = ResidentAuthApi(target.apiUrl)
        io { store.environment = target.name }
        signOut(previous)
    }

    /** Turns a saved refresh token back into a session. Runs once per process. */
    suspend fun restore() {
        if (state != State.Restoring) return
        state = if (io { store.refreshToken } == null) State.SignedOut else try {
            State.SignedIn(authorized(api::me))
        } catch (e: ResidentApiException) {
            // Offline or a server error: keep the sign-in and let the next call retry.
            if (e.code in SessionEndedCodes) State.SignedOut else State.SignedIn(emptyList())
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

    /**
     * WebSocket URL for kiosk calls on the onboarding backend, or null when there is
     * nothing to listen to: not signed in, or on Production, whose calls come from
     * com-core-service.
     */
    val callSocketUrl: String?
        get() = if (state is State.SignedIn && environment != ApiEnvironment.PROD) {
            environment.baseUrl.replaceFirst("http", "ws") + "/" + CALL_SOCKET_PATH
        } else null

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

    /** Asks for an SMS code. [phone] includes the country code. Returns seconds until "Resend" is allowed. */
    suspend fun sendCode(phone: String): Int = io { api.sendCode(phone) }

    suspend fun verifyCode(phone: String, code: String) {
        val (tokens, households) = io { api.verifyCode(phone, code, device()) }
        io { store.refreshToken = tokens.refreshToken }
        accessToken = tokens.accessToken
        state = State.SignedIn(households)
        registerPush()
    }

    fun startDemo() {
        state = State.Demo
    }

    /** Signs out on this phone at once, then ends the session on the server as well, if it can. */
    suspend fun signOut() = signOut(api)

    private suspend fun signOut(server: ResidentAuthApi) {
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
                    if (e.code != AUTH_REQUIRED || access == null || refresh == null) throw e
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
                if (e.code != AUTH_REQUIRED) throw e
                val fresh = refreshAccessToken(token)
                io { call(fresh) }
            }
        } catch (e: ResidentApiException) {
            if (e.code in SessionEndedCodes) {
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
                ?: throw ResidentApiException(401, "invalid_refresh_token", "Your session has expired. Please sign in again.")
            val tokens = io { api.refresh(refreshToken) }
            io { store.refreshToken = tokens.refreshToken }
            accessToken = tokens.accessToken
            return tokens.accessToken
        }
    }

    // Shown to admins as the resident's last login. Build.MODEL is the hardware model,
    // not the user-assigned device name (that is personal data).
    private fun device() = JSONObject()
        .put("id", store.deviceId)
        .put("model", Build.MODEL)
        .put("platform", "android")
        .put("appVersion", BuildConfig.VERSION_NAME)

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
