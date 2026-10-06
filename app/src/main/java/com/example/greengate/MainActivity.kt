package com.example.greengate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.greengate.core.call.ReceiverCallController
import com.example.greengate.core.call.provider.ReceiverCallState
import com.example.greengate.ui.screens.CallTestHarnessScreen
import com.example.greengate.ui.screens.InCallScreen
import com.example.greengate.ui.screens.IncomingCallScreen
import com.example.greengate.ui.theme.*

import android.content.Intent
import android.os.Build
import android.view.WindowManager
import com.example.greengate.core.call.signaling.CallSignalingService

class MainActivity : ComponentActivity() {

    private lateinit var callController: ReceiverCallController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.load(applicationContext)
        enableEdgeToEdge()

        configureLockscreenFlags()
        callController = ReceiverCallController(applicationContext)

        CallSignalingService.startService(applicationContext)
        processCallIntent(intent)

        setContent {
            GreenGateTheme {
                var showSplash by remember { mutableStateOf(true) }
                var signedIn by rememberSaveable { mutableStateOf(false) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else if (!signedIn) {
                    LoginScreen(onSignedIn = { signedIn = true })
                } else {
                    MainScreen(controller = callController, onLogout = { signedIn = false })
                }
            }
        }
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

        val creds = if (providerStr.equals("vonage", ignoreCase = true)) {
            val apiKey = intent.getStringExtra(CallSignalingService.EXTRA_VONAGE_API_KEY).takeUnless { it.isNullOrBlank() }
                ?: com.example.greengate.core.call.provider.SharedCallConfig.vonageApiKey
            val sessionId = intent.getStringExtra(CallSignalingService.EXTRA_VONAGE_SESSION_ID).takeUnless { it.isNullOrBlank() }
                ?: com.example.greengate.core.call.provider.SharedCallConfig.vonageSessionId
            val token = intent.getStringExtra(CallSignalingService.EXTRA_VONAGE_TOKEN).takeUnless { it.isNullOrBlank() }
                ?: com.example.greengate.core.call.provider.SharedCallConfig.vonageToken

            com.example.greengate.core.call.provider.ReceiverCallCredentials.Vonage(
                callId = callId,
                visitorName = visitorName,
                unitName = unitName,
                kioskName = kioskName,
                apiKey = apiKey,
                sessionId = sessionId,
                token = token
            )
        } else {
            val roomName = intent.getStringExtra(CallSignalingService.EXTRA_ROOM_NAME) ?: com.example.greengate.core.call.provider.SharedCallConfig.twilioRoomName
            val accessToken = intent.getStringExtra(CallSignalingService.EXTRA_ACCESS_TOKEN) ?: com.example.greengate.core.call.provider.SharedCallConfig.twilioReceiverToken

            com.example.greengate.core.call.provider.ReceiverCallCredentials.Twilio(
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

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Community : Screen("community")
    data object Access : Screen("access")
    data object Profile : Screen("profile")
    data object BookFacility : Screen("book_facility")
    data object InviteVisitors : Screen("invite_visitors")
    data object EForms : Screen("e_forms")
    data object Feedback : Screen("feedback")
    data object Announcements : Screen("announcements")
    data object Search : Screen("search")
    data object Notifications : Screen("notifications")
    data object LocationPicker : Screen("location_picker")
    data object Bookings : Screen("bookings")
    data object BookingPayment : Screen("booking_payment/{facility}/{date}/{hour}/{guests}") {
        internal fun create(facilityId: String, day: BookingDay, hour: Int, guests: Int) =
            "booking_payment/${bookingPath(facilityId, day, hour, guests)}"
    }
    data object BookingConfirmed : Screen("booking_confirmed/{id}") {
        fun create(bookingId: String) = "booking_confirmed/$bookingId"
    }
    data object BookingDetail : Screen("booking/{id}") {
        fun create(bookingId: String) = "booking/$bookingId"
    }
    data object Payments : Screen("payments")
    data object TransactionDetail : Screen("transaction/{id}") {
        fun create(transactionId: String) = "transaction/$transactionId"
    }
    data object Visitors : Screen("visitors")
    data object CallTest : Screen("call_test")
    data object CommunityInfo : Screen("community_info")
}

@Composable
fun MainScreen(
    controller: ReceiverCallController? = null,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val activeController = remember(controller) {
        controller ?: ReceiverCallController(context.applicationContext)
    }
    val callState by activeController.state.collectAsState()
    val remoteView by activeController.remoteView.collectAsState()
    val isMuted by activeController.isMuted.collectAsState()
    val isCameraOff by activeController.isCameraOff.collectAsState()
    val gateUnlockedMessage by activeController.gateUnlockedMessage.collectAsState()

    val signalingManager = remember {
        com.example.greengate.core.call.signaling.CallSignalingManager(context.applicationContext, activeController).also { mgr ->
            activeController.onCallEndedSignal = { callId ->
                mgr.broadcastCallEnded(callId)
            }
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        android.util.Log.d("MainActivity", "Permissions updated: camera=$cameraGranted, audio=$audioGranted")
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add("android.permission.POST_NOTIFICATIONS")
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    DisposableEffect(Unit) {
        CallSignalingService.startService(context.applicationContext)
        signalingManager.startListening()
        onDispose {
            signalingManager.stop()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        if (AppPreferences.theme == AppTheme.ZERO) androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.gg_home_reference_background),
            contentDescription = null, modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
        )
        Scaffold(
            bottomBar = {
                if (isMainTab(currentRoute) && callState.isTerminal) {
                    BottomNavigationBar(navController, currentRoute)
                }
            },
            containerColor = if (AppPreferences.theme == AppTheme.ZERO) androidx.compose.ui.graphics.Color.Transparent else MarinaBackground
        ) { innerPadding ->
            // Read through State so padding changes don't rebuild the nav graph.
            val scaffoldPadding = rememberUpdatedState(innerPadding)
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                fun screen(route: String, underStatusBar: () -> Boolean = { false }, content: @Composable (NavBackStackEntry) -> Unit) =
                    composable(route) { entry ->
                        RoutePadding(entry, scaffoldPadding.value, underStatusBar()) { content(entry) }
                    }
                screen(Screen.Home.route, underStatusBar = { AppPreferences.theme == AppTheme.ZERO }) { HomeScreen(navController) }
                screen(Screen.Community.route) { PlaceholderScreen("Community", navController) }
                screen(Screen.Access.route) { PlaceholderScreen("Access", navController) }
                screen(Screen.Profile.route) { ProfileScreen(navController, onLogout) }
                screen(Screen.BookFacility.route, underStatusBar = { true }) { BookFacilityScreen(navController) }
                screen(Screen.BookingPayment.route, underStatusBar = { true }) { entry ->
                    entry.arguments.bookingArgs()?.let {
                        BookingPaymentScreen(navController, it.facility, it.day, it.hour, it.guests)
                    }
                }
                screen(Screen.BookingConfirmed.route, underStatusBar = { true }) { entry ->
                    BookingStore.find(entry.arguments?.getString("id"))?.let { BookingConfirmedScreen(navController, it) }
                }
                screen(Screen.BookingDetail.route, underStatusBar = { true }) { entry ->
                    BookingStore.find(entry.arguments?.getString("id"))?.let { BookingDetailScreen(navController, it) }
                }
                screen(Screen.Payments.route, underStatusBar = { true }) { PaymentsScreen(navController) }
                screen(Screen.TransactionDetail.route, underStatusBar = { true }) { entry ->
                    val id = entry.arguments?.getString("id")
                    transactionsOf(BookingStore.bookings).find { it.id == id }?.let { TransactionDetailScreen(navController, it) }
                }
                screen(Screen.InviteVisitors.route) { PlaceholderScreen("Green Invite", navController) }
                screen(Screen.EForms.route) { PlaceholderScreen("E-Forms", navController) }
                screen(Screen.Feedback.route) { PlaceholderScreen("Feedback", navController) }
                screen(Screen.Announcements.route) { AnnouncementsScreen(navController) }
                screen(Screen.Search.route) { PlaceholderScreen("Search", navController) }
                screen(Screen.Notifications.route) { PlaceholderScreen("Notifications", navController) }
                screen(Screen.LocationPicker.route) { PlaceholderScreen("Location", navController) }
                screen(Screen.Bookings.route, underStatusBar = { true }) { MyBookingsScreen(navController) }
                screen(Screen.Visitors.route) { PlaceholderScreen("Visitors", navController) }
                screen(Screen.CommunityInfo.route) { CommunityInfoScreen(navController) }
                screen(Screen.CallTest.route) {
                    CallTestHarnessScreen(
                        controller = activeController,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // --- Call UI Overlays ---
        when (val state = callState) {
            is ReceiverCallState.Incoming -> {
                IncomingCallScreen(
                    credentials = state.credentials,
                    onAccept = { activeController.answerCall() },
                    onDecline = { activeController.declineCall() }
                )
            }

            is ReceiverCallState.Connecting, is ReceiverCallState.Connected -> {
                InCallScreen(
                    controller = activeController,
                    remoteView = remoteView,
                    isMuted = isMuted,
                    isCameraOff = isCameraOff,
                    gateUnlockedMessage = gateUnlockedMessage,
                    onHangUp = { activeController.hangUp() }
                )
            }

            is ReceiverCallState.Ended -> {
                // Auto dismiss ended state after brief view
                LaunchedEffect(state) {
                    kotlinx.coroutines.delay(1500L)
                    activeController.resetToIdle()
                }
            }

            ReceiverCallState.Idle -> Unit
        }
    }
}

// Bottom space comes from the route, never from whether the bar is showing right now: the bar
// hides the moment a sub-page opens, and following it would resize the outgoing tab mid-transition
// so its content visibly slides down.
@Composable
private fun RoutePadding(
    entry: NavBackStackEntry,
    innerPadding: PaddingValues,
    underStatusBar: Boolean,
    content: @Composable () -> Unit
) {
    val direction = LocalLayoutDirection.current
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barSpace = if (isMainTab(entry.destination.route)) BottomBarHeight else 0.dp
    Box(Modifier.fillMaxSize().padding(
        start = innerPadding.calculateStartPadding(direction),
        top = if (underStatusBar) 0.dp else innerPadding.calculateTopPadding(),
        end = innerPadding.calculateEndPadding(direction),
        bottom = navBar + barSpace
    )) { content() }
}

// The booking routes share {facility}/{date}/{hour}/{guests}; the date is "yyyy-M-d".
private fun bookingPath(facilityId: String, day: BookingDay, hour: Int, guests: Int) =
    "$facilityId/${day.year}-${day.month + 1}-${day.day}/$hour/$guests"

private class BookingArgs(val facility: Facility, val day: BookingDay, val hour: Int, val guests: Int)

private fun Bundle?.bookingArgs(): BookingArgs? {
    val facility = Facilities.find { it.id == this?.getString("facility") } ?: return null
    val date = this?.getString("date")?.split("-")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 } ?: return null
    return BookingArgs(
        facility, BookingDay(date[0], date[1] - 1, date[2]),
        hour = getString("hour")?.toIntOrNull() ?: return null,
        guests = getString("guests")?.toIntOrNull() ?: 1
    )
}

fun isMainTab(route: String?): Boolean {
    return route in listOf(Screen.Home.route, Screen.Community.route, Screen.Access.route, Screen.Profile.route)
}

@Composable
fun PlaceholderScreen(title: String, navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Go Back")
            }
            if (title == "Access") {
                TextButton(onClick = { navController.navigate(Screen.CallTest.route) }) {
                    Text("Call Receiver Test Harness")
                }
            }
        }
    }
}

@Composable
fun AnnouncementsScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Announcements Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Go Back")
            }
        }
    }
}
