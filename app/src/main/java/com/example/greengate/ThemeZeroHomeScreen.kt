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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val scale = minOf(maxWidth.value, 600f) / 426f
        // The cards start just below the unit chip (which ends at 293); spare space falls below them.
        val headerHeight = 305f * scale
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            ReferenceHeader(navController, scale, headerHeight.dp) { showSos = true }
            Column(Modifier.padding(horizontal = 18.u(scale)), verticalArrangement = Arrangement.spacedBy(9.u(scale))) {
                ReferenceAnnouncement(scale) { navController.navigate(Screen.Announcements.route) }
                ReferenceFeatures(navController, scale)
                ReferenceGlance(navController, scale)
            }
        }
    }
    if (showSos) AlertDialog(
        onDismissRequest = { showSos = false }, title = { Text("Emergency assistance") },
        text = { Text("An emergency contact has not been configured for this residence. For immediate help, call your local emergency number using your phone.") },
        confirmButton = { TextButton(onClick = { showSos = false }) { Text("Close") } }
    )
}

@Composable
private fun ReferenceHeader(nav: NavController, s: Float, height: Dp, onSos: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height)) {
        Image(painterResource(R.drawable.greengate_logo), "Green Gate logo",
            Modifier.offset(45.u(s), 30.u(s)).size(96.u(s)))
        Text("Green Gate", Modifier.offset(38.u(s), 107.u(s)), color = Color.White,
            fontFamily = DMSans, fontSize = 22.t(s), lineHeight = 27.t(s))
        Text("SECURE · SAFE · TOGETHER", Modifier.offset(39.u(s), 135.u(s)), color = Color.White,
            fontFamily = DMSans, fontSize = 5.5f.t(s), letterSpacing = 2.t(s))
        ReferenceHeaderButton(Icons.Outlined.Search, "Search", 242f, 86f, s) { nav.navigate(Screen.Search.route) }
        ReferenceHeaderButton(Icons.Rounded.Notifications, "Notifications", 288f, 86f, s) { nav.navigate(Screen.Notifications.route) }
        Box(Modifier.offset(296.u(s), 69.u(s)).size(9.u(s)).background(Color(0xFFFF603D), CircleShape))
        Box(Modifier.offset(312.u(s), 58.u(s)).size(53.u(s)).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFFF8053), Color(0xFFFF5238))))
            .clickable(role = Role.Button, onClick = onSos)) {
            Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color.Transparent,
                border = BorderStroke(3.u(s), Color.White.copy(alpha = .85f))) {
                Box(contentAlignment = Alignment.Center) { Text("SOS", color = Color.White, fontFamily = DMSans, fontSize = 15.t(s)) }
            }
        }
        ReferenceHeaderButton(Icons.Rounded.Person, "Profile", 390f, 86f, s, profile = true) { nav.navigate(Screen.Profile.route) }
        Row(Modifier.offset(349.u(s), 118.u(s)), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.gg_weather_sunny), null, Modifier.size(30.u(s)))
            Text("28°", color = Color.White, fontFamily = DMSans, fontSize = 20.t(s))
        }
        Text("Mostly Sunny", Modifier.offset(352.u(s), 143.u(s)), color = Color.White, fontFamily = DMSans, fontSize = 10.t(s))
        Text("Thu, 2 Oct", Modifier.offset(369.u(s), 157.u(s)), color = Color.White.copy(alpha = .9f), fontFamily = DMSans, fontSize = 8.t(s))
        Text("Good Morning,", Modifier.offset(32.u(s), 168.u(s)), color = Color.White,
            fontFamily = DMSans, fontSize = 26.t(s), lineHeight = 31.t(s), fontWeight = FontWeight.Medium)
        Row(Modifier.offset(33.u(s), 193.u(s)), verticalAlignment = Alignment.CenterVertically) {
            Text("Alex", color = Color.White, fontFamily = DMSans, fontSize = 34.t(s), lineHeight = 39.t(s), fontWeight = FontWeight.Medium)
            Image(painterResource(R.drawable.gg_leaf_accent), null, Modifier.padding(start = 2.u(s)).size(35.u(s)))
        }
        Row(Modifier.offset(31.u(s), 233.u(s)), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.heightIn(min = 28.u(s)).clickable(role = Role.Button) { nav.navigate(Screen.LocationPicker.route) },
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, Modifier.size(17.u(s)), tint = Color.White)
                Text("Marina Bay Residences", Modifier.padding(start = 4.u(s)), color = Color.White, fontFamily = DMSans, fontSize = 13.t(s))
                Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.padding(start = 7.u(s)).size(20.u(s)), tint = Color.White)
            }
            IconButton({ nav.navigate(Screen.CommunityInfo.route) }, Modifier.size(32.u(s))) {
                Icon(Icons.Outlined.Info, "About Marina Bay Residences", Modifier.size(18.u(s)), tint = Color.White)
            }
        }
        Box(Modifier.offset(30.u(s), 263.u(s))) {
            ReferenceChip("Tower A · Unit 12-03", Icons.Rounded.Apartment, 190f, s) { nav.navigate(Screen.LocationPicker.route) }
        }
    }
}

@Composable
private fun BoxScope.ReferenceHeaderButton(icon: ImageVector, label: String, x: Float, y: Float, s: Float, profile: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.offset((x - 24).u(s), (y - 24).u(s)).size(48.u(s)).semantics { contentDescription = label }
        .clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Surface(Modifier.size(36.u(s)), shape = CircleShape, color = Color(0xEDF7FFFF)) {
            Box(contentAlignment = Alignment.Center) {
                if (profile) ReferenceArt(intArrayOf(761, 150, 39, 46), Modifier.size(20.u(s), 24.u(s)), feather = true)
                else Icon(icon, label, Modifier.size(20.u(s)), tint = if (label == "Notifications") Color(0xFF123D50) else GateInk)
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
private fun ReferenceGlass(modifier: Modifier, s: Float, onClick: (() -> Unit)? = null, radius: Float = 17f, content: @Composable BoxScope.() -> Unit) {
    Surface(modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        shape = RoundedCornerShape(radius.u(s)), color = Color.Transparent,
        border = BorderStroke(.75f.u(s), Color.White.copy(alpha = .9f))) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xD8F9FFFC), Color(0xBED8F4EF)))), content = content)
    }
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
    ReferenceGlass(Modifier.fillMaxWidth().height(AnnouncementCardHeight.u(s)), s, onClick) {
        ReferenceArt(intArrayOf(55, 780, 148, 116), Modifier.align(Alignment.CenterStart).padding(start = 12.u(s)).size(72.u(s), 59.u(s)), feather = true)
        Column(Modifier.align(Alignment.CenterStart).padding(start = 96.u(s), end = 82.u(s))) {
            Text("Priority Announcements", color = GateInk, fontFamily = DMSans, fontSize = 15.t(s), lineHeight = 19.t(s), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.u(s)))
            AnimatedContent(current, transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            }, label = "priorityAnnouncement") { index ->
                Text(PriorityAnnouncements[index], color = GateInk, fontFamily = DMSans, fontSize = 12.t(s), lineHeight = 16.t(s), minLines = 2, maxLines = 2)
            }
            Spacer(Modifier.height(7.u(s)))
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
private fun ReferenceFeatures(nav: NavController, s: Float) {
    val actions = if (AppPreferences.showFeedback) ReferenceActions + ReferenceFeature("Feedback", "Share ideas and\nhelp us improve", Screen.Feedback.route, intArrayOf(510, 1207, 178, 143)) else ReferenceActions
    val sequence = if (AppPreferences.useReferenceArtwork) null else rememberActionIconSequence(AppPreferences.iconSet, actions.map { 1800 })
    Column(verticalArrangement = Arrangement.spacedBy(7.u(s))) {
        actions.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.u(s))) {
                pair.forEach { feature ->
                    ReferenceGlass(Modifier.weight(1f).height(FeatureCardHeight.u(s)), s, { nav.navigate(feature.route) }, radius = 13f) {
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
        ReferenceArt(intArrayOf(62, 1484, 88, 89), Modifier.offset(13.u(s), 11.u(s)).size(44.u(s)).clip(RoundedCornerShape(14.u(s))))
        Text("Today at a Glance", Modifier.offset(68.u(s), 10.u(s)), color = GateInk, fontFamily = DMSans, fontSize = 13.t(s), fontWeight = FontWeight.Medium)
        Icon(Icons.Rounded.ChevronRight, "View bookings", Modifier.align(Alignment.TopEnd).padding(end = 10.u(s), top = 8.u(s)).size(23.u(s))
            .clickable(role = Role.Button) { nav.navigate(Screen.Bookings.route) }, tint = GateInk)
        Row(Modifier.offset(62.u(s), 35.u(s)).fillMaxWidth().padding(end = 68.u(s)), verticalAlignment = Alignment.CenterVertically) {
            ReferenceStat("2", "Bookings\nConfirmed", intArrayOf(162, 1531, 70, 77), s, Modifier.weight(1f)) { nav.navigate(Screen.Bookings.route) }
            Box(Modifier.width(.5f.u(s)).height(36.u(s)).background(GateGreen.copy(alpha = .2f)))
            ReferenceStat("1", "Visitor\nExpected", intArrayOf(380, 1530, 72, 78), s, Modifier.weight(1f)) { nav.navigate(Screen.Visitors.route) }
            Box(Modifier.width(.5f.u(s)).height(36.u(s)).background(GateGreen.copy(alpha = .2f)))
            ReferenceStat("3", "New\nAnnouncements", intArrayOf(579, 1530, 76, 78), s, Modifier.weight(1.15f)) { nav.navigate(Screen.Announcements.route) }
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
