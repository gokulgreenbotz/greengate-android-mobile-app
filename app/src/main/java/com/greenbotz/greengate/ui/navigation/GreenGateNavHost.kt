package com.greenbotz.greengate.ui.navigation

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.greenbotz.greengate.MainActivity
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.call.ReceiverCallController
import com.greenbotz.greengate.core.call.provider.ReceiverCallState
import com.greenbotz.greengate.core.facility.BookingStore
import com.greenbotz.greengate.core.facility.transactionsOf
import com.greenbotz.greengate.core.push.CallSignalingManager
import com.greenbotz.greengate.core.push.CallSignalingService
import com.greenbotz.greengate.core.session.ApiEnvironment
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.AppTheme
import com.greenbotz.greengate.core.visitor.VisitorStore
import com.greenbotz.greengate.ui.screens.assistant.GreenBotScreen
import com.greenbotz.greengate.ui.screens.community.CommunityInfoScreen
import com.greenbotz.greengate.ui.screens.facilities.BookFacilityScreen
import com.greenbotz.greengate.ui.screens.facilities.BookingConfirmedScreen
import com.greenbotz.greengate.ui.screens.facilities.BookingDay
import com.greenbotz.greengate.ui.screens.facilities.BookingDetailScreen
import com.greenbotz.greengate.ui.screens.facilities.BookingPaymentScreen
import com.greenbotz.greengate.ui.screens.facilities.Facilities
import com.greenbotz.greengate.ui.screens.facilities.Facility
import com.greenbotz.greengate.ui.screens.facilities.FacilityAboutScreen
import com.greenbotz.greengate.ui.screens.facilities.FacilityImages
import com.greenbotz.greengate.ui.screens.facilities.MyBookingsScreen
import com.greenbotz.greengate.ui.screens.home.BottomBarHeight
import com.greenbotz.greengate.ui.screens.home.HomeScreen
import com.greenbotz.greengate.ui.screens.home.MarinaBackground
import com.greenbotz.greengate.ui.screens.intercom.CallTestHarnessScreen
import com.greenbotz.greengate.ui.screens.intercom.InCallScreen
import com.greenbotz.greengate.ui.screens.intercom.IncomingCallScreen
import com.greenbotz.greengate.ui.screens.payments.PaymentsScreen
import com.greenbotz.greengate.ui.screens.payments.TransactionDetailScreen
import com.greenbotz.greengate.ui.screens.profile.ProfileScreen
import com.greenbotz.greengate.ui.screens.visitors.InviteCreatedScreen
import com.greenbotz.greengate.ui.screens.visitors.InviteDetailScreen
import com.greenbotz.greengate.ui.screens.visitors.InviteType
import com.greenbotz.greengate.ui.screens.visitors.VisitorManagementScreen
import kotlinx.coroutines.delay

// Package stays com.greenbotz.greengate until the project-wide
// "Package name does not match containing directory" fix moves it to ui.navigation.


@Composable
fun MainScreen(
    controller: ReceiverCallController? = null,
    onLogout: () -> Unit = {},
    onSwitchServer: (ApiEnvironment) -> Unit = {}
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
        CallSignalingManager(context.applicationContext, activeController).also { mgr ->
            // Tells the kiosk the call ended and why (DECLINED / NO_ANSWER / HANGUP).
            activeController.onCallEndedWithReason = { callId, reason ->
                mgr.broadcastCallEnded(callId, reason)
            }
            // "Unlock Door" in the call screen → ask the kiosk to open the relay.
            activeController.onOpenGateRequested = { callId, endCall, requestId ->
                mgr.requestOpenGate(callId, endCall, requestId)
            }
            // Other phones of the household stop ringing once this one answers.
            activeController.onCallAnswered = { callId -> mgr.notifyAnswered(callId) }
        }
    }

    // The call socket is for "app is open"; with the app off screen, calls come by FCM.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, signalingManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> signalingManager.startSocket()
                Lifecycle.Event.ON_STOP -> signalingManager.pauseSocket()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        Log.d("MainActivity", "Permissions updated: camera=$cameraGranted, audio=$audioGranted")
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add("android.permission.POST_NOTIFICATIONS")
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Decode Book Facility's photos while Home is showing so the screen's slide-in doesn't stall on them.
    LaunchedEffect(Unit) { FacilityImages.preload(context.applicationContext) }

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
    val botVoiceAllowed = rememberUpdatedState(callState.isTerminal)

    Box(modifier = Modifier.fillMaxSize()) {
        if (AppPreferences.theme == AppTheme.ZERO) Image(
            painter = painterResource(R.drawable.gg_home_reference_background),
            contentDescription = null, modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Scaffold(
            bottomBar = {
                if (isMainTab(currentRoute) && callState.isTerminal) {
                    BottomNavigationBar(navController, currentRoute)
                }
            },
            containerColor = if (AppPreferences.theme == AppTheme.ZERO) Color.Transparent else MarinaBackground
        ) { innerPadding ->
            // Read through State so padding changes don't rebuild the nav graph.
            val scaffoldPadding = rememberUpdatedState(innerPadding)
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                enterTransition = { if (inBookingFlow()) bookingPushIn() else fadeIn(tween(
                    NavFadeMillis
                )) },
                exitTransition = { if (inBookingFlow()) bookingPushOut() else fadeOut(tween(
                    NavFadeMillis
                )) },
                popEnterTransition = { if (inBookingFlow()) bookingPopIn() else fadeIn(tween(
                    NavFadeMillis
                )) },
                popExitTransition = { if (inBookingFlow()) bookingPopOut() else fadeOut(tween(
                    NavFadeMillis
                )) }
            ) {
                fun screen(route: String, underStatusBar: () -> Boolean = { false }, content: @Composable (NavBackStackEntry) -> Unit) =
                    composable(route) { entry ->
                        RoutePadding(entry, scaffoldPadding.value, underStatusBar()) { content(entry) }
                    }
                screen(Screen.Home.route, underStatusBar = { AppPreferences.theme == AppTheme.ZERO }) {
                    HomeScreen(
                        navController
                    )
                }
                screen(Screen.Community.route) { PlaceholderScreen("Community", navController) }
                screen(Screen.Access.route) { PlaceholderScreen("Access", navController) }
                screen(Screen.Services.route) { PlaceholderScreen("Services", navController) }
                screen(Screen.Profile.route) { ProfileScreen(navController, onLogout, onSwitchServer) }
                screen(Screen.BookFacility.route, underStatusBar = { true }) {
                    BookFacilityScreen(
                        navController
                    )
                }
                screen(Screen.FacilityAbout.route, underStatusBar = { true }) { entry ->
                    Facilities.find { it.id == entry.arguments?.getString("id") }?.let {
                        FacilityAboutScreen(
                            navController,
                            it
                        )
                    }
                }
                screen(Screen.BookingPayment.route, underStatusBar = { true }) { entry ->
                    entry.arguments.bookingArgs()?.let {
                        BookingPaymentScreen(navController, it.facility, it.day, it.hour, it.guests)
                    }
                }
                screen(Screen.BookingConfirmed.route, underStatusBar = { true }) { entry ->
                    BookingStore.find(entry.arguments?.getString("id"))?.let {
                        BookingConfirmedScreen(
                            navController,
                            it
                        )
                    }
                }
                screen(Screen.BookingDetail.route, underStatusBar = { true }) { entry ->
                    BookingStore.find(entry.arguments?.getString("id"))?.let {
                        BookingDetailScreen(
                            navController,
                            it
                        )
                    }
                }
                screen(Screen.Payments.route, underStatusBar = { true }) {
                    PaymentsScreen(
                        navController
                    )
                }
                screen(Screen.GreenBot.route) {
                    GreenBotScreen(
                        navController,
                        voiceAllowed = botVoiceAllowed.value
                    )
                }
                screen(Screen.CreateInvite.route, underStatusBar = { true }) { entry ->
                    // Family / Friend, Delivery and Cab invites are made inside the Create Invite sheet.
                    // TODO: replace with the Other form once it is designed.
                    val type = InviteType.entries.find { it.name == entry.arguments?.getString("type") }
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        PlaceholderScreen("Invite: ${type?.title ?: "Visitor"}", navController)
                    }
                }
                screen(Screen.TransactionDetail.route, underStatusBar = { true }) { entry ->
                    val id = entry.arguments?.getString("id")
                    transactionsOf(BookingStore.bookings).find { it.id == id }?.let {
                        TransactionDetailScreen(
                            navController,
                            it
                        )
                    }
                }
                screen(Screen.InviteVisitors.route) {
                    PlaceholderScreen(
                        "Green Invite",
                        navController
                    )
                }
                screen(Screen.EForms.route) { PlaceholderScreen("E-Forms", navController) }
                screen(Screen.Feedback.route) { PlaceholderScreen("Feedback", navController) }
                screen(Screen.Announcements.route) { AnnouncementsScreen(navController) }
                screen(Screen.Search.route) { PlaceholderScreen("Search", navController) }
                screen(Screen.Notifications.route) {
                    PlaceholderScreen(
                        "Notifications",
                        navController
                    )
                }
                screen(Screen.LocationPicker.route) { PlaceholderScreen("Location", navController) }
                screen(Screen.Bookings.route, underStatusBar = { true }) {
                    MyBookingsScreen(
                        navController
                    )
                }
                screen(Screen.Visitors.route, underStatusBar = { true }) {
                    VisitorManagementScreen(
                        navController
                    )
                }
                screen(Screen.InviteCreated.route, underStatusBar = { true }) { entry ->
                    VisitorStore.find(entry.arguments?.getString("id"))?.let {
                        InviteCreatedScreen(
                            navController,
                            it
                        )
                    }
                }
                screen(Screen.InviteDetail.route, underStatusBar = { true }) { entry ->
                    VisitorStore.find(entry.arguments?.getString("id"))?.let {
                        InviteDetailScreen(
                            navController,
                            it
                        )
                    }
                }
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
                    onDecline = { activeController.declineCall() },
                    onSkip = { activeController.skipCall() }
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
                    delay(1500L)
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
