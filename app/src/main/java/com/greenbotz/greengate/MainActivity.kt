package com.greenbotz.greengate

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.greenbotz.greengate.core.call.ReceiverCallController
import com.greenbotz.greengate.core.call.provider.ReceiverCallCredentials
import com.greenbotz.greengate.core.call.provider.SharedCallConfig
import com.greenbotz.greengate.core.push.CallSignalingService
import com.greenbotz.greengate.core.push.IncomingCallNotifier
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.navigation.MainScreen
import com.greenbotz.greengate.ui.screens.auth.LoginScreen
import com.greenbotz.greengate.ui.screens.auth.SplashScreen
import com.greenbotz.greengate.ui.theme.GreenGateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var callController: ReceiverCallController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        configureLockscreenFlags()
        callController = ReceiverCallController(applicationContext)

        CallSignalingService.startService(applicationContext)
        processCallIntent(intent)

        setContent {
            GreenGateTheme {
                var showSplash by remember { mutableStateOf(true) }
                val session = ResidentSession.state
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) { ResidentSession.restore() }
                if (showSplash) {
                    SplashScreen(
                        onFinished = { showSplash = false },
                        ready = session != ResidentSession.State.Restoring
                    )
                } else if (session is ResidentSession.State.SignedIn || session == ResidentSession.State.Demo) {
                    MainScreen(
                        controller = callController,
                        onLogout = { scope.launch { ResidentSession.signOut() } },
                        onSwitchServer = { server -> scope.launch { ResidentSession.switchEnvironment(server) } }
                    )
                } else {
                    LoginScreen()
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        IncomingCallNotifier.appInForeground = true
        // Screen back on / app reopened during a call: restart the camera.
        if (::callController.isInitialized) callController.onAppForegrounded()
    }

    override fun onStop() {
        IncomingCallNotifier.appInForeground = false
        // Power button / home during a call: pause our video cleanly.
        if (::callController.isInitialized) callController.onAppBackgrounded()
        super.onStop()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureLockscreenFlags()
        processCallIntent(intent)
    }

    private fun configureLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KEYGUARD_SERVICE) as? android.app.KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun processCallIntent(intent: Intent?) {
        if (intent == null) return
        val callId = intent.getStringExtra(CallSignalingService.EXTRA_CALL_ID) ?: return
        val visitorName = intent.getStringExtra(CallSignalingService.EXTRA_VISITOR_NAME) ?: "Visitor at Kiosk"
        val unitName = intent.getStringExtra(CallSignalingService.EXTRA_UNIT_NAME) ?: "Unit 1204"
        val kioskName = intent.getStringExtra(CallSignalingService.EXTRA_KIOSK_NAME) ?: "Main Gate Kiosk"
        val providerStr = intent.getStringExtra(CallSignalingService.EXTRA_PROVIDER) ?: "twilio"

        val creds = if (providerStr.equals("livekit", ignoreCase = true)) {
            com.greenbotz.greengate.core.call.provider.livekit.LiveKitConfig.receiverCredentials(
                context = applicationContext,
                callId = callId,
                visitorName = visitorName,
                unitName = unitName,
                kioskName = kioskName,
                url = intent.getStringExtra(CallSignalingService.EXTRA_LIVEKIT_URL),
                roomName = intent.getStringExtra(CallSignalingService.EXTRA_LIVEKIT_ROOM),
                tokenFromSignal = intent.getStringExtra(CallSignalingService.EXTRA_LIVEKIT_TOKEN)
            )
        } else if (providerStr.equals("vonage", ignoreCase = true)) {
            val apiKey = intent.getStringExtra(CallSignalingService.EXTRA_VONAGE_API_KEY).takeUnless { it.isNullOrBlank() }
                ?: SharedCallConfig.vonageApiKey
            val sessionId = intent.getStringExtra(CallSignalingService.EXTRA_VONAGE_SESSION_ID).takeUnless { it.isNullOrBlank() }
                ?: SharedCallConfig.vonageSessionId
            val token = intent.getStringExtra(CallSignalingService.EXTRA_VONAGE_TOKEN).takeUnless { it.isNullOrBlank() }
                ?: SharedCallConfig.vonageToken

            ReceiverCallCredentials.Vonage(
                callId = callId,
                visitorName = visitorName,
                unitName = unitName,
                kioskName = kioskName,
                apiKey = apiKey,
                sessionId = sessionId,
                token = token
            )
        } else {
            val roomName = intent.getStringExtra(CallSignalingService.EXTRA_ROOM_NAME) ?: SharedCallConfig.twilioRoomName
            val accessToken = intent.getStringExtra(CallSignalingService.EXTRA_ACCESS_TOKEN) ?: SharedCallConfig.twilioReceiverToken

            ReceiverCallCredentials.Twilio(
                callId = callId,
                visitorName = visitorName,
                unitName = unitName,
                kioskName = kioskName,
                roomName = roomName,
                accessToken = accessToken
            )
        }

        callController.triggerIncomingCall(creds)
    }
}
