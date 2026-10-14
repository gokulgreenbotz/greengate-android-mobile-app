package com.greenbotz.greengate.ui.screens.home

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.navigation.Screen
import com.greenbotz.greengate.ui.screens.facilities.Facility
import com.greenbotz.greengate.ui.screens.sos.SosScreen
import com.greenbotz.greengate.ui.screens.visitors.CreateInviteSheet
import com.greenbotz.greengate.ui.theme.DMSans

private val MeadowInk = Color(0xFF0E3B2C)
private val MeadowMuted = Color(0xFF6E7A72)
private val MeadowLine = Color(0xFFE3E8E1)
private val MeadowPage = Color(0xFFF8F9F5)
private val MeadowMint = Color(0xFFE5F3E0)
private val MeadowLimeWash = Color(0xFFF0F8D8)
private val MeadowLime = Color(0xFFC6E04A)
private val MeadowSosWash = Color(0xFFFFEDEA)

// Laid out at a 411dp design width and scaled to the screen, like Themes 0 and 2.
private fun Float.u(scale: Float) = (this * scale).dp
private fun Int.u(scale: Float) = toFloat().u(scale)
private fun Float.t(scale: Float) = (this * scale).sp
private fun Int.t(scale: Float) = toFloat().t(scale)

@Composable
internal fun ThemeThreeHomeScreen(navController: NavController) {
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
    BoxWithConstraints(Modifier.fillMaxSize().background(MeadowPage), contentAlignment = Alignment.TopCenter) {
        val s = minOf(maxWidth.value, 600f) / 411f
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.u(s)),
            verticalArrangement = Arrangement.spacedBy(12.u(s))
        ) {
            MeadowHeader(navController, s) { showSos = true }
            MeadowGreeting(s)
            PropertyCard(s) { navController.navigate(Screen.LocationPicker.route) }
            PriorityNotice(s) { navController.navigate(Screen.Announcements.route) }
            Row(Modifier.fillMaxWidth().padding(top = 8.u(s)), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Quick actions", s, Modifier.weight(1f))
                MeadowRoundButton(Icons.Rounded.Search, "Search", s) { navController.navigate(Screen.Search.route) }
            }
            QuickActions(navController, s) { showInvite = true }
            Row(Modifier.fillMaxWidth().padding(top = 8.u(s)).clip(RoundedCornerShape(12.u(s)))
                .clickable(role = Role.Button, onClickLabel = "See today's bookings") { navController.navigate(Screen.Bookings.route) },
                verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Today at a glance", s, Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(26.u(s)), tint = MeadowInk)
            }
            TodayAtAGlance(navController, s)
            AskGreenBot(s) { navController.navigate(Screen.GreenBot.route) }
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

/** The gate mark: two forest-green leaves of a gate around a lime post. */
@Composable
private fun GateMark(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val leaf = w * .3f
        drawPath(Path().apply {
            moveTo(0f, 0f); lineTo(leaf, h * .14f); lineTo(leaf, h * .86f); lineTo(0f, h); close()
        }, MeadowInk)
        drawPath(Path().apply {
            moveTo(w, 0f); lineTo(w - leaf, h * .14f); lineTo(w - leaf, h * .86f); lineTo(w, h); close()
        }, MeadowInk)
        val post = w * .16f
        drawRect(MeadowLime, Offset((w - post) / 2f, h * .2f), androidx.compose.ui.geometry.Size(post, h * .6f))
    }
}

@Composable
private fun MeadowHeader(nav: NavController, s: Float, onSos: () -> Unit) {
    Row(Modifier.padding(top = 10.u(s)), verticalAlignment = Alignment.CenterVertically) {
        GateMark(Modifier.size(32.u(s), 28.u(s)))
        Text("Green Gate", Modifier.weight(1f).padding(start = 9.u(s)), color = MeadowInk, fontFamily = DMSans,
            fontWeight = FontWeight.Bold, fontSize = 21.t(s), lineHeight = 26.t(s), maxLines = 1)
        Box {
            MeadowRoundButton(Icons.Outlined.Notifications, "Notifications", s) { nav.navigate(Screen.Notifications.route) }
            Box(Modifier.align(Alignment.TopEnd).offset(x = (-8).u(s), y = 8.u(s)).size(8.u(s))
                .background(MarinaSosRed, CircleShape).border(1.dp, Color.White, CircleShape))
        }
        Spacer(Modifier.width(8.u(s)))
        Surface(onClick = onSos, shape = CircleShape, color = MeadowSosWash, border = BorderStroke(1.5f.u(s), MarinaSosRed)) {
            Box(Modifier.height(40.u(s)).padding(horizontal = 13.u(s)), contentAlignment = Alignment.Center) {
                Text("SOS", color = MarinaSosRed, fontFamily = DMSans, fontSize = 14.t(s), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.u(s)))
        Surface(onClick = { nav.navigate(Screen.Profile.route) }, modifier = Modifier.size(42.u(s)), shape = CircleShape, color = MeadowMint) {
            Box(contentAlignment = Alignment.Center) {
                Text("A", color = MeadowInk, fontFamily = DMSans, fontSize = 17.t(s), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun MeadowRoundButton(icon: ImageVector, label: String, s: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(42.u(s)), shape = CircleShape, color = Color.White,
        border = BorderStroke(1.dp, MeadowLine)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(21.u(s)), tint = MeadowInk) }
    }
}

@Composable
private fun MeadowGreeting(s: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.u(s))) {
            Text("Good morning,", color = MeadowMuted, fontFamily = DMSans, fontSize = 17.t(s), lineHeight = 22.t(s))
            Text(ResidentSession.firstName, color = MeadowInk, fontFamily = DMSans, fontWeight = FontWeight.Bold,
                fontSize = 38.t(s), lineHeight = 44.t(s), letterSpacing = (-.5f).t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(shape = RoundedCornerShape(20.u(s)), color = Color(0xFFEAF4EC)) {
            Row(Modifier.padding(horizontal = 9.u(s), vertical = 10.u(s)), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.gg_weather_sunny), null, Modifier.size(36.u(s)))
                Column(Modifier.padding(start = 6.u(s))) {
                    Text("28°", color = MeadowInk, fontFamily = DMSans, fontSize = 21.t(s), lineHeight = 24.t(s), fontWeight = FontWeight.Bold)
                    Text("Mostly sunny", color = MeadowMuted, fontFamily = DMSans, fontSize = 10.5f.t(s), lineHeight = 13.t(s), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MeadowDisc(icon: ImageVector, size: Float, s: Float, modifier: Modifier = Modifier, color: Color = MeadowMint) {
    Box(modifier.size(size.u(s)).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size((size * .5f).u(s)), tint = MeadowInk)
    }
}

@Composable
private fun PropertyCard(s: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.u(s)), color = Color.White,
        border = BorderStroke(1.dp, MeadowLine)) {
        Row(Modifier.padding(horizontal = 12.u(s), vertical = 10.u(s)), verticalAlignment = Alignment.CenterVertically) {
            MeadowDisc(Icons.Outlined.Apartment, 46f, s, color = MeadowLimeWash)
            Column(Modifier.weight(1f).padding(horizontal = 12.u(s))) {
                Text("Marina Bay Residences", color = MeadowInk, fontFamily = DMSans, fontSize = 16.t(s), lineHeight = 20.t(s),
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tower A · Unit 12-03", color = MeadowMuted, fontFamily = DMSans, fontSize = 13.5f.t(s), lineHeight = 18.t(s), maxLines = 1)
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(24.u(s)), tint = MeadowInk)
        }
    }
}

@Composable
private fun PriorityNotice(s: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.u(s)), color = MeadowLimeWash) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.u(s)).fillMaxHeight().background(MeadowLime))
            Icon(Icons.Outlined.Campaign, null, Modifier.padding(start = 16.u(s)).size(40.u(s)), tint = MeadowInk)
            Column(Modifier.weight(1f).padding(start = 14.u(s), top = 14.u(s), bottom = 14.u(s), end = 8.u(s))) {
                Text("PRIORITY NOTICE", color = MeadowInk, fontFamily = DMSans, fontSize = 9.5f.t(s), letterSpacing = 2.2f.t(s),
                    fontWeight = FontWeight.Medium, maxLines = 1)
                Text("Fire drill in Tower A", Modifier.padding(top = 4.u(s)), color = MeadowInk, fontFamily = DMSans,
                    fontSize = 17.t(s), lineHeight = 21.t(s), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Monday · 11:00 AM", Modifier.padding(top = 2.u(s)), color = MeadowMuted, fontFamily = DMSans,
                    fontSize = 13.t(s), lineHeight = 17.t(s), maxLines = 1)
            }
            Box(Modifier.width(1.dp).height(40.u(s)).background(MeadowInk.copy(alpha = .12f)))
            Row(Modifier.padding(start = 14.u(s), end = 10.u(s)), verticalAlignment = Alignment.CenterVertically) {
                Text("View", color = MeadowInk, fontFamily = DMSans, fontSize = 14.t(s), fontWeight = FontWeight.Medium)
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(22.u(s)), tint = MeadowInk)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, s: Float, modifier: Modifier) {
    Text(title, modifier, color = MeadowInk, fontFamily = DMSans, fontWeight = FontWeight.Bold,
        fontSize = 20.t(s), lineHeight = 26.t(s), letterSpacing = (-.2f).t(s))
}

private class MeadowAction(val title: String, val subtitle: String, val route: String, val icon: ImageVector)

private val MeadowActions = listOf(
    MeadowAction("Book Facility", "Reserve amenities", Screen.BookFacility.route, Icons.Outlined.CalendarToday),
    MeadowAction("Pre-Invites", "Welcome your guests", Screen.InviteVisitors.route, Icons.Outlined.PersonAddAlt),
    MeadowAction("E-Forms", "Submit and track forms", Screen.EForms.route, Icons.Outlined.Description),
    MeadowAction("My Community", "Connect with neighbours", Screen.Community.route, Icons.Outlined.Groups)
)
private val MeadowFeedback = MeadowAction("Feedback", "Tell us how we're doing", Screen.Feedback.route, Icons.Outlined.Feedback)

@Composable
private fun QuickActions(nav: NavController, s: Float, onInvite: () -> Unit) {
    // Feedback appears only once it's switched on in Profile.
    val actions = if (AppPreferences.showFeedback) MeadowActions + MeadowFeedback else MeadowActions
    Column(verticalArrangement = Arrangement.spacedBy(10.u(s))) {
        actions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.u(s))) {
                row.forEach { action ->
                    ActionTile(action, s, Modifier.weight(1f)) {
                        if (action.route == Screen.InviteVisitors.route) onInvite() else nav.navigate(action.route)
                    }
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ActionTile(action: MeadowAction, s: Float, modifier: Modifier, onClick: () -> Unit) {
    val view = LocalView.current
    Surface(
        onClick = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); onClick() },
        modifier = modifier.height(100.u(s)), shape = RoundedCornerShape(16.u(s)), color = Color.White,
        border = BorderStroke(1.dp, MeadowLine)
    ) {
        Box(Modifier.padding(12.u(s))) {
            MeadowDisc(action.icon, 36f, s, color = MeadowLimeWash)
            Icon(Icons.Rounded.ChevronRight, null, Modifier.align(Alignment.TopEnd).padding(top = 6.u(s)).size(20.u(s)), tint = MeadowInk)
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(action.title, color = MeadowInk, fontFamily = DMSans, fontSize = 14.t(s), lineHeight = 18.t(s),
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(action.subtitle, color = MeadowMuted, fontFamily = DMSans, fontSize = 11.5f.t(s), lineHeight = 15.t(s),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TodayAtAGlance(nav: NavController, s: Float) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.u(s)), color = Color.White, border = BorderStroke(1.dp, MeadowLine)) {
        Row(Modifier.height(IntrinsicSize.Min).padding(vertical = 4.u(s)), verticalAlignment = Alignment.CenterVertically) {
            GlanceItem("2", "Bookings", Icons.Outlined.CalendarToday, s, Modifier.weight(1f)) { nav.navigate(Screen.Bookings.route) }
            Box(Modifier.width(1.dp).height(44.u(s)).background(MeadowLine))
            GlanceItem("1", "Visitor\nexpected", Icons.Outlined.PersonOutline, s, Modifier.weight(1f)) { nav.navigate(Screen.Visitors.route) }
            Box(Modifier.width(1.dp).height(44.u(s)).background(MeadowLine))
            GlanceItem("3", "New\nnotices", Icons.Outlined.Campaign, s, Modifier.weight(1f)) { nav.navigate(Screen.Announcements.route) }
        }
    }
}

@Composable
private fun GlanceItem(count: String, label: String, icon: ImageVector, s: Float, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.u(s), vertical = 10.u(s)),
        verticalAlignment = Alignment.CenterVertically) {
        MeadowDisc(icon, 36f, s, color = Color(0xFFE9F4EC))
        Column(Modifier.padding(start = 8.u(s))) {
            Text(count, color = MeadowInk, fontFamily = DMSans, fontSize = 22.t(s), lineHeight = 26.t(s), fontWeight = FontWeight.Bold)
            Text(label, color = MeadowMuted, fontFamily = DMSans, fontSize = 11.t(s), lineHeight = 13.t(s), maxLines = 2)
        }
    }
}

@Composable
private fun AskGreenBot(s: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(84.u(s)), shape = RoundedCornerShape(18.u(s)),
        color = Color.Transparent) {
        Box(Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFE2F2E5), Color(0xFFEEF7EF))))) {
            Image(painterResource(R.drawable.green_bot_mascot), "Green Bot",
                Modifier.align(Alignment.BottomStart).offset(x = 12.u(s), y = 10.u(s)).size(88.u(s), 80.u(s)))
            Row(Modifier.fillMaxSize().padding(start = 108.u(s), end = 14.u(s)), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ask Green Bot", color = MeadowInk, fontFamily = DMSans, fontWeight = FontWeight.Bold,
                        fontSize = 15.5f.t(s), lineHeight = 20.t(s), maxLines = 1)
                    Text("A little help, right here.", color = MeadowMuted, fontFamily = DMSans,
                        fontSize = 13.t(s), lineHeight = 17.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(42.u(s)).background(MeadowInk, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(22.u(s)), tint = Color.White)
                }
            }
        }
    }
}

@Composable
internal fun ThemeThreeBottomNavigation(nav: NavController, route: String?) {
    fun navigate(next: String) { if (next != route) nav.navigate(next) { popUpTo(Screen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true } }
    Surface(Modifier.fillMaxWidth(), color = Color.White, border = BorderStroke(1.dp, MeadowLine)) {
        Row(Modifier.navigationBarsPadding().height(BottomBarHeight).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            MeadowNavItem(Icons.Rounded.Home, Icons.Outlined.Home, "Home", route == Screen.Home.route, Modifier.weight(1f)) { navigate(Screen.Home.route) }
            MeadowNavItem(Icons.Rounded.Groups, Icons.Outlined.Groups, "Community", route == Screen.Community.route, Modifier.weight(1f)) { navigate(Screen.Community.route) }
            MeadowNavItem(Icons.Rounded.Lock, Icons.Outlined.Lock, "Access", route == Screen.Access.route, Modifier.weight(1f)) { navigate(Screen.Access.route) }
            MeadowNavItem(Icons.Rounded.Notifications, Icons.Outlined.Notifications, "Updates", route == Screen.Announcements.route, Modifier.weight(1f)) { navigate(Screen.Announcements.route) }
            MeadowNavItem(Icons.Rounded.GridView, Icons.Outlined.GridView, "More", route == Screen.Profile.route, Modifier.weight(1f)) { navigate(Screen.Profile.route) }
        }
    }
}

@Composable
private fun MeadowNavItem(selectedIcon: ImageVector, icon: ImageVector, title: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.selectable(selected, role = Role.Tab, onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(if (selected) selectedIcon else icon, null, Modifier.size(26.dp), tint = if (selected) MeadowInk else MeadowMuted)
        Text(title, color = if (selected) MeadowInk else MeadowMuted, fontFamily = DMSans, fontSize = 11.sp, lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(6.dp).background(if (selected) MeadowLime else Color.Transparent, CircleShape))
    }
}
