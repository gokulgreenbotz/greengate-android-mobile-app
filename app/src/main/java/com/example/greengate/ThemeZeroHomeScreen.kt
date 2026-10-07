package com.example.greengate

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans
import kotlinx.coroutines.delay

private val GateInk = Color(0xFF091329)
private val GateGreen = Color(0xFF00665A)
private val GateMuted = Color(0xFF527F96)

// Measured against the 852px reference at a 426dp design width. Text and controls
// remain native; only decorative illustrations use tightly bounded source crops.
private fun Float.u(scale: Float) = (this * scale).dp
private fun Int.u(scale: Float) = toFloat().u(scale)
private fun Float.t(scale: Float) = (this * scale).sp
private fun Int.t(scale: Float) = toFloat().t(scale)

private const val AnnouncementCardHeight = 100f
private const val FeatureCardHeight = 175f

@Composable
internal fun ThemeZeroHomeScreen(navController: NavController) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val bars = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        bars?.isAppearanceLightStatusBars = false
        bars?.isAppearanceLightNavigationBars = true
        onDispose { bars?.isAppearanceLightStatusBars = true }
    }
    var showSos by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    if (showInvite) CreateInviteSheet(
        onDismiss = { showInvite = false },
        onHistory = { showInvite = false; navController.navigate(Screen.Visitors.route) },
        onCreated = { showInvite = false; navController.navigate(Screen.InviteCreated.create(it.id)) }
    ) { type ->
        showInvite = false
        navController.navigate(Screen.CreateInvite.create(type))
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val scale = minOf(maxWidth.value, 600f) / 426f
        // The header takes the top 20% of the screen; the cards start right below it.
        val headerHeight = LocalConfiguration.current.screenHeightDp * .2f
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            ReferenceHeader(navController, scale, headerHeight.dp) { showSos = true }
            Column(Modifier.padding(horizontal = 18.u(scale)), verticalArrangement = Arrangement.spacedBy(9.u(scale))) {
                ReferenceAnnouncement(scale) { navController.navigate(Screen.Announcements.route) }
                ReferenceFeatures(navController, scale) { showInvite = true }
                ReferenceGlance(navController, scale)
                GreenBotCard(scale) { navController.navigate(Screen.GreenBot.route) }
                ReferenceAllAnnouncements(scale) { navController.navigate(Screen.Announcements.route) }
                Spacer(Modifier.height(4.u(scale)))
            }
        }
    }
    if (showSos) SosScreen(
        onDismiss = { showSos = false },
        onCallReceiverHarness = {
            showSos = false
            navController.navigate(Screen.CallTest.route)
        }
    )
}

@Composable
private fun ReferenceHeader(nav: NavController, s: Float, height: Dp, onSos: () -> Unit) {
    // Three compact rows: brand + actions, greeting + weather, community + unit.
    // The block is at least 20% of the screen tall; extra room is shared between the rows.
    Column(
        Modifier.fillMaxWidth().heightIn(min = height).statusBarsPadding()
            .padding(start = 18.u(s), end = 14.u(s), top = 4.u(s), bottom = 8.u(s)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.greengate_logo), "Green Gate logo", Modifier.size(34.u(s)))
            Column(Modifier.weight(1f).padding(start = 6.u(s))) {
                Text("Green Gate", color = Color.White, fontFamily = DMSans, fontSize = 16.t(s), lineHeight = 18.t(s))
                Text("SECURE · SAFE · TOGETHER", color = Color.White, fontFamily = DMSans, fontSize = 5.t(s),
                    lineHeight = 7.t(s), letterSpacing = 1.5f.t(s), maxLines = 1)
            }
            ReferenceHeaderButton(Icons.Outlined.Search, "Search", s) { nav.navigate(Screen.Search.route) }
            Box {
                ReferenceHeaderButton(Icons.Rounded.Notifications, "Notifications", s) { nav.navigate(Screen.Notifications.route) }
                Box(Modifier.align(Alignment.TopEnd).padding(top = 6.u(s), end = 8.u(s)).size(8.u(s)).background(Color(0xFFFF603D), CircleShape))
            }
            Box(Modifier.padding(horizontal = 3.u(s)).size(38.u(s)).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFF8053), Color(0xFFFF5238))))
                .clickable(role = Role.Button, onClick = onSos)) {
                Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color.Transparent,
                    border = BorderStroke(2.u(s), Color.White.copy(alpha = .85f))) {
                    Box(contentAlignment = Alignment.Center) { Text("SOS", color = Color.White, fontFamily = DMSans, fontSize = 11.t(s)) }
                }
            }
            ReferenceHeaderButton(Icons.Rounded.Person, "Profile", s, profile = true) { nav.navigate(Screen.Profile.route) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Good Morning,", color = Color.White, fontFamily = DMSans, fontSize = 16.t(s), lineHeight = 20.t(s), fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Alex", color = Color.White, fontFamily = DMSans, fontSize = 30.t(s), lineHeight = 34.t(s), fontWeight = FontWeight.SemiBold)
                    Image(painterResource(R.drawable.gg_leaf_accent), null, Modifier.padding(start = 2.u(s)).size(28.u(s)))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.gg_weather_sunny), null, Modifier.size(38.u(s)))
                    Text("28°", Modifier.padding(start = 3.u(s)), color = Color.White, fontFamily = DMSans, fontSize = 24.t(s), lineHeight = 28.t(s))
                }
                Text("Mostly Sunny", color = Color.White, fontFamily = DMSans, fontSize = 11.t(s), lineHeight = 14.t(s), maxLines = 1)
                Text("Thu, 2 Oct", color = Color.White.copy(alpha = .9f), fontFamily = DMSans, fontSize = 9.t(s), lineHeight = 12.t(s), maxLines = 1)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.heightIn(min = 28.u(s)).clickable(role = Role.Button) { nav.navigate(Screen.LocationPicker.route) },
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, Modifier.size(15.u(s)), tint = Color.White)
                Text("Marina Bay Residences", Modifier.padding(start = 3.u(s)), color = Color.White, fontFamily = DMSans,
                    fontSize = 12.t(s), maxLines = 1)
            }
            IconButton({ nav.navigate(Screen.CommunityInfo.route) }, Modifier.size(28.u(s))) {
                Icon(Icons.Outlined.Info, "About Marina Bay Residences", Modifier.size(16.u(s)), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            ReferenceChip("Tower A · Unit 12-03", Icons.Rounded.Apartment, 168f, s) { nav.navigate(Screen.LocationPicker.route) }
        }
    }
}

@Composable
private fun ReferenceHeaderButton(icon: ImageVector, label: String, s: Float, profile: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(42.u(s)).semantics { contentDescription = label }
        .clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Surface(Modifier.size(32.u(s)), shape = CircleShape, color = Color(0xEDF7FFFF)) {
            Box(contentAlignment = Alignment.Center) {
                if (profile) ReferenceArt(intArrayOf(761, 150, 39, 46), Modifier.size(17.u(s), 20.u(s)), feather = true)
                else Icon(icon, label, Modifier.size(18.u(s)), tint = if (label == "Notifications") Color(0xFF123D50) else GateInk)
            }
        }
    }
}

@Composable
private fun ReferenceChip(label: String, icon: ImageVector, width: Float, s: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.width(width.u(s)).height(30.u(s)), shape = CircleShape,
        color = Color(0xD7ECFCFD), border = BorderStroke(.5f.u(s), Color.White.copy(alpha = .7f))) {
        Row(Modifier.padding(horizontal = 10.u(s)), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(16.u(s)), tint = GateGreen)
            Text(label, Modifier.weight(1f).padding(start = 6.u(s)), fontFamily = DMSans, fontSize = 11.t(s), color = GateInk, maxLines = 1)
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(15.u(s)), tint = GateInk)
        }
    }
}

@Composable
private fun ReferenceGlass(modifier: Modifier, s: Float, onClick: (() -> Unit)? = null, radius: Float = 17f, blurBackdrop: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Surface(modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        shape = RoundedCornerShape(radius.u(s)), color = Color.Transparent,
        border = BorderStroke(.75f.u(s), Color.White.copy(alpha = .9f))) {
        Box {
            if (blurBackdrop) BlurredBackdrop(Modifier.matchParentSize(), 18.u(s))
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(0xD8F9FFFC), Color(0xBED8F4EF)))))
            content()
        }
    }
}

/**
 * Frosted-glass backdrop: redraws the window background (drawn full-screen in MainActivity) blurred and
 * aligned to this node's on-screen position. Blur needs API 31+; older devices fall back to the plain tint.
 */
@Composable
private fun BlurredBackdrop(modifier: Modifier, radius: Dp) {
    val painter = painterResource(R.drawable.gg_home_reference_background)
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(Size.Zero) }
    Box(modifier
        .onGloballyPositioned { offset = it.positionInRoot(); rootSize = it.findRootCoordinates().size.toSize() }
        .blur(radius, BlurredEdgeTreatment.Rectangle)
        .drawBehind { translate(-offset.x, -offset.y) { with(painter) { draw(rootSize) } } })
}

private val PriorityAnnouncements = listOf(
    "Pool maintenance on Friday,\n10:00 AM – 12:00 PM",
    "Water supply interruption,\nSaturday 9:00 AM – 1:00 PM",
    "Fire drill in Tower A,\nMonday 11:00 AM"
)
private const val AnnouncementIntervalMillis = 4000L

@Composable
private fun ReferenceAnnouncement(s: Float, onClick: () -> Unit) {
    var current by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(AnnouncementIntervalMillis)
            current = (current + 1) % PriorityAnnouncements.size
        }
    }
    ReferenceGlass(Modifier.fillMaxWidth().height(AnnouncementCardHeight.u(s)), s, onClick, blurBackdrop = true) {
        ReferenceArt(intArrayOf(55, 780, 148, 116), Modifier.align(Alignment.CenterStart).padding(start = 12.u(s)).size(72.u(s), 59.u(s)), feather = true)
        Column(Modifier.align(Alignment.CenterStart).padding(start = 96.u(s), end = 82.u(s))) {
            // A small label over the message, which is what residents need to read.
            Text("Priority Announcements", color = GateGreen, fontFamily = DMSans, fontSize = 11.t(s), lineHeight = 14.t(s),
                fontWeight = FontWeight.Medium, letterSpacing = .4f.t(s))
            Spacer(Modifier.height(3.u(s)))
            AnimatedContent(current, transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            }, label = "priorityAnnouncement") { index ->
                Text(PriorityAnnouncements[index], color = GateInk, fontFamily = DMSans, fontSize = 14.t(s), lineHeight = 18.t(s),
                    fontWeight = FontWeight.Medium, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.u(s)))
            Row(horizontalArrangement = Arrangement.spacedBy(4.u(s)), verticalAlignment = Alignment.CenterVertically) {
                PriorityAnnouncements.indices.forEach { index ->
                    val active = index == current
                    val width by animateDpAsState(if (active) 14.u(s) else 5.u(s), label = "dotWidth")
                    val color by animateColorAsState(if (active) GateGreen else GateGreen.copy(alpha = .25f), label = "dotColor")
                    Box(Modifier.size(width, 5.u(s)).background(color, CircleShape))
                }
            }
        }
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 71.u(s)).width(.5f.u(s)).height(36.u(s)).background(GateGreen.copy(alpha = .17f)))
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 10.u(s)), verticalAlignment = Alignment.CenterVertically) {
            Text("View", color = GateGreen, fontFamily = DMSans, fontSize = 14.t(s))
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(21.u(s)), tint = GateGreen)
        }
    }
}

private data class ReferenceFeature(val title: String, val subtitle: String, val route: String, val art: IntArray)
private val ReferenceActions = listOf(
    ReferenceFeature("Book Facility", "Reserve amenities\nwith ease", Screen.BookFacility.route, intArrayOf(105, 940, 185, 138)),
    ReferenceFeature("Invite Visitors", "Create visitor\npasses quickly", Screen.InviteVisitors.route, intArrayOf(538, 940, 163, 139)),
    ReferenceFeature("E-Forms", "Submit and manage\ndigital forms", Screen.EForms.route, intArrayOf(136, 1207, 168, 141)),
    ReferenceFeature("My Community", "Connect, share\nand stay updated", Screen.Community.route, intArrayOf(510, 1207, 178, 143))
)

@Composable
private fun ReferenceFeatures(nav: NavController, s: Float, onInvite: () -> Unit) {
    val actions = if (AppPreferences.showFeedback) ReferenceActions + ReferenceFeature("Feedback", "Share ideas and\nhelp us improve", Screen.Feedback.route, intArrayOf(510, 1207, 178, 143)) else ReferenceActions
    val sequence = if (AppPreferences.useReferenceArtwork) null else rememberActionIconSequence(AppPreferences.iconSet, actions.map { 1800 })
    Column(verticalArrangement = Arrangement.spacedBy(7.u(s))) {
        actions.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.u(s))) {
                pair.forEach { feature ->
                    ReferenceGlass(Modifier.weight(1f).height(FeatureCardHeight.u(s)), s, { if (feature.route == Screen.InviteVisitors.route) onInvite() else nav.navigate(feature.route) }, radius = 13f,
                        blurBackdrop = feature.route == Screen.BookFacility.route || feature.route == Screen.InviteVisitors.route) {
                        if (!AppPreferences.useReferenceArtwork) {
                            val index = actions.indexOf(feature)
                            val active = sequence?.activeIndex == index
                            val progress = if (active) sequence?.progress?.value ?: 0f else 0f
                            val modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.u(s)).size(150.u(s), 104.u(s))
                            when {
                                AppPreferences.iconSet == IconSet.THREE -> Icon(
                                    when (feature.route) {
                                        Screen.BookFacility.route -> Icons.Rounded.CalendarMonth
                                        Screen.InviteVisitors.route -> Icons.Rounded.PersonAdd
                                        Screen.EForms.route -> Icons.Rounded.Description
                                        Screen.Feedback.route -> Icons.Rounded.Feedback
                                        else -> Icons.Rounded.Groups
                                    }, null, modifier.padding(16.u(s)), tint = GateGreen)
                                feature.route == Screen.Community.route && (AppPreferences.iconSet == IconSet.ZERO || AppPreferences.iconSet == IconSet.CLAY) -> CommunitySetZeroIcon(progress, active, modifier, AppPreferences.iconSet)
                                feature.route == Screen.Community.route -> CommunityIcon(progress, active, modifier)
                                AppPreferences.iconSet == IconSet.TWO -> LayeredActionIcon(if (feature.route == Screen.Feedback.route) 3 else index, progress, active, modifier)
                                else -> LayeredIconSetOne(if (feature.route == Screen.Feedback.route) 3 else index, progress, active, modifier, AppPreferences.iconSet)
                            }
                        } else {
                        if (feature.route == Screen.Feedback.route) Icon(Icons.Rounded.Feedback, null, Modifier.align(Alignment.TopCenter).padding(top = 22.u(s)).size(76.u(s)), tint = GateGreen)
                        else ReferenceArt(feature.art, Modifier.align(Alignment.TopCenter).padding(top = 10.u(s)).size(134.u(s), 104.u(s)), feather = true)
                        }
                        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.u(s), bottom = 12.u(s))) {
                            Text(feature.title, color = GateInk, fontFamily = DMSans, fontSize = 16.t(s), lineHeight = 20.t(s), fontWeight = FontWeight.Medium)
                            Text(feature.subtitle, color = GateMuted, fontFamily = DMSans, fontSize = 11.5f.t(s), lineHeight = 14.t(s))
                        }
                        Surface(Modifier.align(Alignment.BottomEnd).padding(end = 12.u(s), bottom = 18.u(s)).size(34.u(s)), shape = CircleShape,
                            color = Color.White, shadowElevation = 3.u(s)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.u(s)), tint = GateInk) }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReferenceGlance(nav: NavController, s: Float) {
    ReferenceGlass(Modifier.fillMaxWidth().height(86.u(s)), s) {
        Text("Today at a Glance", Modifier.offset(16.u(s), 10.u(s)), color = GateInk, fontFamily = DMSans, fontSize = 13.t(s), fontWeight = FontWeight.Medium)
        Icon(Icons.Rounded.ChevronRight, "View bookings", Modifier.align(Alignment.TopEnd).padding(end = 10.u(s), top = 8.u(s)).size(23.u(s))
            .clickable(role = Role.Button) { nav.navigate(Screen.Bookings.route) }, tint = GateInk)
        Row(Modifier.offset(10.u(s), 35.u(s)).fillMaxWidth().padding(end = 20.u(s)), verticalAlignment = Alignment.CenterVertically) {
            ReferenceStat("2", "Bookings\nConfirmed", intArrayOf(162, 1531, 70, 77), s, Modifier.weight(1f)) { nav.navigate(Screen.Bookings.route) }
            Box(Modifier.width(.5f.u(s)).height(36.u(s)).background(GateGreen.copy(alpha = .2f)))
            ReferenceStat("1", "Visitor\nExpected", intArrayOf(380, 1530, 72, 78), s, Modifier.weight(1f)) { nav.navigate(Screen.Visitors.route) }
            Box(Modifier.width(.5f.u(s)).height(36.u(s)).background(GateGreen.copy(alpha = .2f)))
            ReferenceStat("3", "New\nAnnouncements", intArrayOf(579, 1530, 76, 78), s, Modifier.weight(1.15f)) { nav.navigate(Screen.Announcements.route) }
        }
    }
}

/** Theme 0's counterpart to Theme 1's "All Announcements" banner. */
@Composable
private fun ReferenceAllAnnouncements(s: Float, onClick: () -> Unit) {
    ReferenceGlass(Modifier.fillMaxWidth().height(76.u(s)), s, onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.u(s)), verticalAlignment = Alignment.CenterVertically) {
            // Same megaphone as the glance row's "New Announcements".
            ReferenceArt(intArrayOf(579, 1530, 76, 78), Modifier.size(48.u(s)).clip(CircleShape))
            Spacer(Modifier.width(12.u(s)))
            Column(Modifier.weight(1f)) {
                Text("All Announcements", color = GateInk, fontFamily = DMSans, fontSize = 14.t(s), lineHeight = 18.t(s),
                    fontWeight = FontWeight.Medium)
                Text("Stay updated with the latest community news", color = GateMuted, fontFamily = DMSans,
                    fontSize = 10.5f.t(s), lineHeight = 13.t(s))
            }
            Surface(Modifier.size(30.u(s)), shape = CircleShape, color = Color.White, shadowElevation = 3.u(s)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.u(s)), tint = GateInk)
                }
            }
        }
    }
}

@Composable
private fun ReferenceStat(count: String, label: String, crop: IntArray, s: Float, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.heightIn(min = 40.u(s)).clickable(role = Role.Button, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        ReferenceArt(crop, Modifier.padding(horizontal = 4.u(s)).size(35.u(s)).clip(CircleShape))
        Column {
            Text(count, color = GateInk, fontFamily = DMSans, fontSize = 17.t(s), lineHeight = 19.t(s), fontWeight = FontWeight.Medium)
            Text(label, color = Color(0xFF125F89), fontFamily = DMSans, fontSize = 9.t(s), lineHeight = 10.5f.t(s))
        }
    }
}

@Composable
internal fun ThemeZeroBottomNavigation(nav: NavController, route: String?) {
    fun navigate(next: String) { if (next != route) nav.navigate(next) { popUpTo(Screen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true } }
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
        val s = (maxWidth.value + 28f) / 426f
        Box(Modifier.fillMaxWidth().height(78.u(s))) {
            ReferenceGlass(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(70.u(s)), s, radius = 35f) {
                Row(Modifier.fillMaxSize().padding(horizontal = 6.u(s)), verticalAlignment = Alignment.CenterVertically) {
                    ReferenceNavItem(Icons.Rounded.Home, "Home", route == Screen.Home.route, s, Modifier.weight(1f)) { navigate(Screen.Home.route) }
                    ReferenceNavItem(Icons.Outlined.Groups, "Community", route == Screen.Community.route, s, Modifier.weight(1f)) { navigate(Screen.Community.route) }
                    Spacer(Modifier.weight(1f))
                    ReferenceNavItem(Icons.Outlined.Lock, "Access", route == Screen.Access.route, s, Modifier.weight(1f)) { navigate(Screen.Access.route) }
                    ReferenceNavItem(Icons.Outlined.GridView, "More", route == Screen.Profile.route, s, Modifier.weight(1f)) { navigate(Screen.Profile.route) }
                }
            }
            Surface(onClick = { navigate(Screen.Home.route) }, Modifier.align(Alignment.TopCenter).size(67.u(s)), shape = CircleShape,
                color = Color.Transparent, border = BorderStroke(3.u(s), Color.White), shadowElevation = 3.u(s)) {
                Box(Modifier.background(Brush.radialGradient(listOf(Color(0xFF8ED9C9), Color(0xFF146456))))) {
                    Image(painterResource(R.drawable.greengate_logo), "Green Gate home", Modifier.fillMaxSize().padding(2.u(s)))
                }
            }
        }
    }
}

@Composable
private fun ReferenceNavItem(icon: ImageVector, title: String, selected: Boolean, s: Float, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.selectable(selected, role = Role.Tab, onClick = onClick).padding(top = 7.u(s)), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(23.u(s)), tint = if (selected) GateGreen else GateInk)
        Text(title, color = if (selected) GateGreen else GateInk, fontFamily = DMSans, fontSize = 10.t(s), lineHeight = 17.t(s))
        Box(Modifier.size(6.u(s)).background(if (selected) GateGreen else Color.Transparent, CircleShape))
    }
}

@Composable
private fun ReferenceArt(crop: IntArray, modifier: Modifier, feather: Boolean = false) {
    val bitmap = ImageBitmap.imageResource(R.drawable.gg_home_reference_art)
    val painter = remember(bitmap, crop.toList()) { BitmapPainter(bitmap, IntOffset(crop[0], crop[1]), IntSize(crop[2], crop[3])) }
    val edges = if (feather) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        drawRect(Brush.horizontalGradient(0f to Color.Transparent, .1f to Color.Black, .9f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
        drawRect(Brush.verticalGradient(0f to Color.Transparent, .08f to Color.Black, .92f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    } else Modifier
    Image(painter, null, modifier.then(edges), contentScale = ContentScale.Fit)
}
