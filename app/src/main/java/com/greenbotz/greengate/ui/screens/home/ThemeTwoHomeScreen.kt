package com.greenbotz.greengate.ui.screens.home

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.navigation.Screen
import com.greenbotz.greengate.ui.screens.facilities.Facilities
import com.greenbotz.greengate.ui.screens.sos.SosScreen
import com.greenbotz.greengate.ui.screens.visitors.CreateInviteSheet
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.PlayfairDisplay
import kotlinx.coroutines.delay

private val FreshInk = Color(0xFF14271F)
private val FreshGreen = Color(0xFF0F5A44)
private val FreshMuted = Color(0xFF6B7A73)
private val FreshMint = Color(0xFFE3F2E8)
private val FreshLine = Color(0xFFE4EDE7)
private val FreshOrange = Color(0xFFF2762E)
private val FreshBackdrop = Brush.verticalGradient(listOf(Color(0xFFEDF6F0), Color(0xFFF6FAF7)))

// Laid out at a 411dp design width and scaled to the screen, like Theme 0.
private fun Float.u(scale: Float) = (this * scale).dp
private fun Int.u(scale: Float) = toFloat().u(scale)
private fun Float.t(scale: Float) = (this * scale).sp
private fun Int.t(scale: Float) = toFloat().t(scale)

@Composable
internal fun ThemeTwoHomeScreen(navController: NavController) {
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
    BoxWithConstraints(Modifier.fillMaxSize().background(FreshBackdrop), contentAlignment = Alignment.TopCenter) {
        val s = minOf(maxWidth.value, 600f) / 411f
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.u(s)),
            verticalArrangement = Arrangement.spacedBy(14.u(s))
        ) {
            FreshHeader(navController, s) { showSos = true }
            FreshGreeting(s)
            AnnouncementCarousel(s) { navController.navigate(Screen.Announcements.route) }
            SectionHeader("Quick Actions", s) { navController.navigate(Screen.Services.route) }
            QuickActions(navController, s) { showInvite = true }
            GreenBotPrompt(s) { navController.navigate(Screen.GreenBot.route) }
            SectionHeader("Latest Updates", s) { navController.navigate(Screen.Announcements.route) }
            LatestUpdates(s) { navController.navigate(Screen.Announcements.route) }
            Spacer(Modifier.height(4.u(s)))
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
private fun FreshHeader(nav: NavController, s: Float, onSos: () -> Unit) {
    Column(Modifier.padding(top = 8.u(s)), verticalArrangement = Arrangement.spacedBy(12.u(s))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.greengate_logo), "Green Gate logo", Modifier.size(40.u(s)))
            Text("Green Gate", Modifier.weight(1f).padding(start = 8.u(s)), color = FreshGreen, fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold, fontSize = 24.t(s), lineHeight = 28.t(s), maxLines = 1)
            RoundButton(Icons.Outlined.Search, "Search", s) { nav.navigate(Screen.Search.route) }
            Spacer(Modifier.width(8.u(s)))
            Box {
                RoundButton(Icons.Outlined.Notifications, "Notifications", s) { nav.navigate(Screen.Notifications.route) }
                Box(Modifier.align(Alignment.TopEnd).offset(x = 3.u(s), y = (-3).u(s)).size(18.u(s))
                    .background(FreshOrange, CircleShape).border(1.5f.u(s), Color.White, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("2", color = Color.White, fontFamily = DMSans, fontSize = 9.5f.t(s), lineHeight = 10.t(s), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.u(s)))
            Surface(onClick = { nav.navigate(Screen.Profile.route) }, modifier = Modifier.size(44.u(s)), shape = CircleShape,
                color = FreshGreen, shadowElevation = 3.u(s)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, "Profile", Modifier.size(26.u(s)), tint = Color.White)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.u(s), border = BorderStroke(1.dp, FreshLine)) {
                Row(Modifier.height(40.u(s)), verticalAlignment = Alignment.CenterVertically) {
                    PickerSegment(Icons.Outlined.Apartment, ResidentSession.condoName, s) { nav.navigate(Screen.LocationPicker.route) }
                    Box(Modifier.width(1.dp).height(20.u(s)).background(FreshLine))
                    PickerSegment(Icons.Outlined.Home, ResidentSession.unitLabel, s) { nav.navigate(Screen.LocationPicker.route) }
                }
            }
            Spacer(Modifier.weight(1f))
            // The reference has no SOS; it sits in the free space beside the unit picker.
            Surface(onClick = onSos, shape = CircleShape, color = MarinaSosRed, shadowElevation = 3.u(s)) {
                Row(Modifier.height(40.u(s)).padding(horizontal = 14.u(s)), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Call, "Emergency assistance", Modifier.size(16.u(s)), tint = Color.White)
                    Text("SOS", Modifier.padding(start = 5.u(s)), color = Color.White, fontFamily = DMSans,
                        fontSize = 13.t(s), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, s: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(42.u(s)), shape = CircleShape, color = Color.White,
        shadowElevation = 2.u(s), border = BorderStroke(1.dp, FreshLine)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(22.u(s)), tint = FreshInk) }
    }
}

@Composable
private fun PickerSegment(icon: ImageVector, label: String, s: Float, onClick: () -> Unit) {
    Row(Modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.u(s)),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.u(s)), tint = FreshInk)
        Text(label, Modifier.padding(start = 8.u(s), end = 4.u(s)), color = FreshInk, fontFamily = DMSans, fontSize = 13.5f.t(s), maxLines = 1)
        Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.u(s)), tint = FreshInk)
    }
}

@Composable
private fun FreshGreeting(s: Float) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(end = 10.u(s))) {
            Text("GOOD MORNING", color = FreshMuted, fontFamily = DMSans, fontSize = 10.5f.t(s), letterSpacing = 2.6f.t(s),
                fontWeight = FontWeight.Medium)
            // The leaf rides inline so it stays beside "Alex" when the name wraps.
            Text(buildAnnotatedString { append("Good Morning, ${ResidentSession.firstName} "); appendInlineContent("leaf") },
                Modifier.padding(top = 4.u(s)), color = FreshInk, fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold, fontSize = 22.t(s), lineHeight = 28.t(s),
                inlineContent = mapOf("leaf" to InlineTextContent(Placeholder(20.t(s), 20.t(s), PlaceholderVerticalAlign.TextCenter)) {
                    Image(painterResource(R.drawable.gg_leaf_accent), null, Modifier.fillMaxSize())
                }))
            Text("A cleaner, safer and happier community together.", Modifier.padding(top = 2.u(s)), color = FreshMuted,
                fontFamily = DMSans, fontSize = 12.5f.t(s), lineHeight = 17.t(s))
        }
        Surface(shape = RoundedCornerShape(18.u(s)), color = Color.White, shadowElevation = 2.u(s), border = BorderStroke(1.dp, FreshLine)) {
            Column(Modifier.padding(horizontal = 12.u(s), vertical = 10.u(s))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.gg_weather_sunny), null, Modifier.size(40.u(s)))
                    Column(Modifier.padding(start = 6.u(s))) {
                        Text("28°C", color = FreshInk, fontFamily = DMSans, fontSize = 19.t(s), lineHeight = 22.t(s), fontWeight = FontWeight.Medium)
                        Text("Partly cloudy", color = FreshMuted, fontFamily = DMSans, fontSize = 10.5f.t(s), lineHeight = 13.t(s), maxLines = 1)
                    }
                }
                Row(Modifier.padding(top = 6.u(s)), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, Modifier.size(14.u(s)), tint = FreshInk)
                    Text("Singapore", Modifier.padding(start = 4.u(s)), color = FreshInk, fontFamily = DMSans, fontSize = 11.5f.t(s))
                }
            }
        }
    }
}

private class FreshAnnouncement(val title: String, val body: String)
private val CommunityAnnouncements = listOf(
    FreshAnnouncement("Pool maintenance on\nFriday, 10:00 AM – 12:00 PM", "The pool will be closed for routine maintenance."),
    FreshAnnouncement("Water supply interruption,\nSaturday 9:00 AM – 1:00 PM", "Please store water ahead of the scheduled works."),
    FreshAnnouncement("Fire drill in Tower A,\nMonday 11:00 AM", "Residents will be guided to the assembly point.")
)
private const val AnnouncementIntervalMillis = 4000L

@Composable
private fun AnnouncementCarousel(s: Float, onClick: () -> Unit) {
    var current by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(AnnouncementIntervalMillis)
            current = (current + 1) % CommunityAnnouncements.size
        }
    }
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(118.u(s)), shape = RoundedCornerShape(20.u(s)),
        color = Color.Transparent, border = BorderStroke(1.dp, Color.White), shadowElevation = 2.u(s)) {
        Box(Modifier.background(Brush.linearGradient(listOf(FreshMint, Color(0xFFF5FBF7), Color(0xFFE5F3EA))))) {
            // Theme 0's megaphone; its mint backdrop feathers into this card.
            ReferenceArt(intArrayOf(55, 780, 148, 116), Modifier.align(Alignment.CenterStart).padding(start = 8.u(s)).size(88.u(s), 70.u(s)), feather = true)
            Column(Modifier.fillMaxSize().padding(start = 104.u(s), end = 12.u(s), top = 12.u(s), bottom = 10.u(s))) {
                Text("COMMUNITY ANNOUNCEMENT", color = FreshMuted, fontFamily = DMSans, fontSize = 9.5f.t(s),
                    letterSpacing = 1.8f.t(s), fontWeight = FontWeight.Medium, maxLines = 1)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(current, Modifier.weight(1f), transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                    }, label = "communityAnnouncement") { index ->
                        Column {
                            Text(CommunityAnnouncements[index].title, color = FreshInk, fontFamily = DMSans, fontSize = 13.5f.t(s),
                                lineHeight = 17.t(s), fontWeight = FontWeight.Medium, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(CommunityAnnouncements[index].body, Modifier.padding(top = 2.u(s)), color = FreshMuted, fontFamily = DMSans,
                                fontSize = 10.5f.t(s), lineHeight = 13.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.width(8.u(s)))
                    PillButton("View", null, s, onClick)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.u(s)), verticalAlignment = Alignment.CenterVertically) {
                    CommunityAnnouncements.indices.forEach { index ->
                        val active = index == current
                        val width by animateDpAsState(if (active) 14.u(s) else 6.u(s), label = "dotWidth")
                        val color by animateColorAsState(if (active) FreshInk else FreshInk.copy(alpha = .18f), label = "dotColor")
                        Box(Modifier.size(width, 6.u(s)).background(color, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButton(label: String, icon: ImageVector?, s: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = FreshGreen, shadowElevation = 2.u(s)) {
        Row(Modifier.height(38.u(s)).padding(start = if (icon != null) 10.u(s) else 14.u(s), end = 8.u(s)),
            verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null, Modifier.padding(end = 5.u(s)).size(17.u(s)), tint = Color.White)
            Text(label, color = Color.White, fontFamily = DMSans, fontSize = 12.5f.t(s), fontWeight = FontWeight.Medium, maxLines = 1)
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(17.u(s)), tint = Color.White)
        }
    }
}

@Composable
private fun SectionHeader(title: String, s: Float, onSeeAll: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 2.u(s)), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = FreshInk, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold,
            fontSize = 20.t(s), lineHeight = 24.t(s))
        Row(Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = "See all $title", onClick = onSeeAll)
            .padding(start = 8.u(s), top = 6.u(s), bottom = 6.u(s)), verticalAlignment = Alignment.CenterVertically) {
            Text("See all", color = FreshInk, fontFamily = DMSans, fontSize = 13.t(s))
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.u(s)), tint = FreshInk)
        }
    }
}

/** A tile's glyph: [icon] on a mint disc, with an optional orange [badge] and a white [inner] mark. */
private class FreshAction(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val badge: ImageVector? = null,
    val badgeAlignment: Alignment = Alignment.BottomEnd,
    val inner: ImageVector? = null
)

private val FreshActions = listOf(
    FreshAction("Invite Visitors", Screen.InviteVisitors.route, Icons.Rounded.Group, Icons.Rounded.Add),
    FreshAction("Book Facilities", Screen.BookFacility.route, Icons.Rounded.CalendarMonth, Icons.Rounded.Schedule),
    FreshAction("E-Forms", Screen.EForms.route, Icons.Rounded.Description, Icons.Rounded.Edit),
    FreshAction("My Community", Screen.Community.route, Icons.Rounded.Groups, Icons.Rounded.Favorite, Alignment.TopCenter),
    FreshAction("Announcements", Screen.Announcements.route, Icons.Rounded.Campaign),
    FreshAction("Security Access", Screen.Access.route, Icons.Rounded.Shield, inner = Icons.Rounded.Lock)
)
private val FreshFeedback = FreshAction("Feedback", Screen.Feedback.route, Icons.Rounded.Feedback, Icons.Rounded.Favorite)

@Composable
private fun QuickActions(nav: NavController, s: Float, onInvite: () -> Unit) {
    // Feedback appears only once it's switched on in Profile.
    val actions = if (AppPreferences.showFeedback) FreshActions + FreshFeedback else FreshActions
    Column(verticalArrangement = Arrangement.spacedBy(9.u(s))) {
        actions.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.u(s))) {
                row.forEach { action ->
                    ActionTile(action, s, Modifier.weight(1f)) {
                        if (action.route == Screen.InviteVisitors.route) onInvite() else nav.navigate(action.route)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ActionTile(action: FreshAction, s: Float, modifier: Modifier, onClick: () -> Unit) {
    val view = LocalView.current
    Surface(
        onClick = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); onClick() },
        modifier = modifier.height(112.u(s)), shape = RoundedCornerShape(18.u(s)), color = Color.White,
        border = BorderStroke(1.dp, FreshLine), shadowElevation = 1.u(s)
    ) {
        Box(Modifier.padding(10.u(s))) {
            ActionGlyph(action, s, Modifier.align(Alignment.TopCenter).padding(top = 2.u(s)))
            // DM Sans runs wider than the reference's face, so the chevron moves off the label's line.
            Surface(Modifier.align(Alignment.TopEnd).size(22.u(s)), shape = CircleShape, color = Color(0xFFF2F6F3)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.u(s)), tint = FreshInk)
                }
            }
            Text(action.title, Modifier.align(Alignment.BottomStart), color = FreshInk, fontFamily = DMSans, fontSize = 12.5f.t(s),
                lineHeight = 16.t(s), fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ActionGlyph(action: FreshAction, s: Float, modifier: Modifier) {
    Box(modifier.size(58.u(s))) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFFF4FAF6), FreshMint)), CircleShape))
        Icon(action.icon, null, Modifier.align(Alignment.Center).size(32.u(s)), tint = FreshGreen)
        action.inner?.let { Icon(it, null, Modifier.align(Alignment.Center).offset(y = 1.u(s)).size(13.u(s)), tint = Color.White) }
        action.badge?.let { badge ->
            val nudge = if (action.badgeAlignment == Alignment.TopCenter) Modifier.offset(y = 1.u(s)) else Modifier.offset((-3).u(s), (-5).u(s))
            Box(Modifier.align(action.badgeAlignment).then(nudge).size(21.u(s))
                .background(FreshOrange, CircleShape).border(1.5f.u(s), Color.White, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(badge, null, Modifier.size(13.u(s)), tint = Color.White)
            }
        }
    }
}

@Composable
private fun GreenBotPrompt(s: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(110.u(s)), shape = RoundedCornerShape(20.u(s)),
        color = Color.Transparent, border = BorderStroke(1.dp, Color.White), shadowElevation = 2.u(s)) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color.White, Color(0xFFEFF8F2), Color(0xFFDDF0E3))))) {
            Image(painterResource(R.drawable.green_bot_mascot), "Green Bot",
                Modifier.align(Alignment.BottomStart).offset(x = (-6).u(s), y = 6.u(s)).size(108.u(s), 98.u(s)))
            Icon(painterResource(R.drawable.ic_sparkle), null, Modifier.offset(x = 82.u(s), y = 8.u(s)).size(15.u(s)), tint = Color(0xFFF5B92E))
            Row(Modifier.fillMaxSize().padding(start = 104.u(s), end = 12.u(s)), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ASK GREEN BOT", color = FreshMuted, fontFamily = DMSans, fontSize = 9.5f.t(s), letterSpacing = 1.8f.t(s),
                        fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("Got a question?", color = FreshInk, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold,
                        fontSize = 18.t(s), lineHeight = 24.t(s), maxLines = 1)
                    Text("Ask me anything about your home, facilities, rules or services.", color = FreshMuted, fontFamily = DMSans,
                        fontSize = 10.5f.t(s), lineHeight = 14.t(s), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.u(s)))
                PillButton("Chat Now", Icons.Rounded.Sms, s, onClick)
            }
        }
    }
}

private class FreshUpdate(val category: String, val title: String, val detail: String, @DrawableRes val photo: Int, val focus: Alignment)
private val LatestUpdateItems = listOf(
    FreshUpdate("FACILITIES", "Pool maintenance on Friday", "10:00 AM – 12:00 PM  ·  16 Oct", R.drawable.facility_pool, Alignment.Center),
    FreshUpdate("COMMUNITY", "Community Clean Up This Saturday", "Join us at 9:00 AM – 11:00 AM. Let's keep Green Gate green!", R.drawable.facility_bbq, Alignment.CenterEnd),
    FreshUpdate("FACILITIES", "Tennis courts are open again", "Courts 1 and 2 can be booked from today", R.drawable.facility_tennis, Alignment.Center)
)

@Composable
private fun LatestUpdates(s: Float, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.u(s)), color = Color.White, border = BorderStroke(1.dp, FreshLine), shadowElevation = 1.u(s)) {
        Column {
            LatestUpdateItems.forEachIndexed { index, update ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 14.u(s)), color = FreshLine)
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(12.u(s)),
                    verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(update.photo), null, Modifier.size(88.u(s), 52.u(s)).clip(RoundedCornerShape(10.u(s))),
                        contentScale = ContentScale.Crop, alignment = update.focus)
                    Column(Modifier.weight(1f).padding(horizontal = 12.u(s))) {
                        Text(update.category, color = FreshMuted, fontFamily = DMSans, fontSize = 9.5f.t(s), letterSpacing = 1.6f.t(s),
                            fontWeight = FontWeight.Medium)
                        Text(update.title, color = FreshInk, fontFamily = PlayfairDisplay, fontSize = 14.5f.t(s), lineHeight = 19.t(s),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(update.detail, color = FreshMuted, fontFamily = DMSans, fontSize = 10.5f.t(s), lineHeight = 14.t(s),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(22.u(s)), tint = FreshInk)
                }
            }
        }
    }
}

@Composable
internal fun ThemeTwoBottomNavigation(nav: NavController, route: String?) {
    fun navigate(next: String) { if (next != route) nav.navigate(next) { popUpTo(Screen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true } }
    Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), shadowElevation = 12.dp) {
        Row(Modifier.navigationBarsPadding().height(BottomBarHeight).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            FreshNavItem(Icons.Rounded.Home, Icons.Outlined.Home, "Home", route == Screen.Home.route, Modifier.weight(1f)) { navigate(Screen.Home.route) }
            FreshNavItem(Icons.Rounded.Groups, Icons.Outlined.Groups, "Community", route == Screen.Community.route, Modifier.weight(1f)) { navigate(Screen.Community.route) }
            FreshNavItem(Icons.Rounded.GridView, Icons.Outlined.GridView, "Services", route == Screen.Services.route, Modifier.weight(1f)) { navigate(Screen.Services.route) }
            FreshNavItem(Icons.Rounded.MeetingRoom, Icons.Outlined.MeetingRoom, "Access", route == Screen.Access.route, Modifier.weight(1f)) { navigate(Screen.Access.route) }
            FreshNavItem(Icons.Rounded.MoreHoriz, Icons.Rounded.MoreHoriz, "More", route == Screen.Profile.route, Modifier.weight(1f)) { navigate(Screen.Profile.route) }
        }
    }
}

@Composable
private fun FreshNavItem(selectedIcon: ImageVector, icon: ImageVector, title: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.selectable(selected, role = Role.Tab, onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(if (selected) selectedIcon else icon, null, Modifier.size(25.dp), tint = if (selected) FreshGreen else FreshMuted)
        Text(title, color = if (selected) FreshGreen else FreshMuted, fontFamily = DMSans, fontSize = 11.sp, lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(5.dp).background(if (selected) FreshGreen else Color.Transparent, CircleShape))
    }
}
