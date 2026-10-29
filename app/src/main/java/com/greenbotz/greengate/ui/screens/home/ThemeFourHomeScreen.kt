package com.greenbotz.greengate.ui.screens.home

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.facility.BookingStatus
import com.greenbotz.greengate.core.facility.BookingStore
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.navigation.Screen
import com.greenbotz.greengate.ui.navigation.TabSlideMillis
import com.greenbotz.greengate.ui.screens.sos.SosScreen
import com.greenbotz.greengate.ui.screens.visitors.CreateInviteSheet
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.PlayfairDisplay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Theme 4 - Navy & Gold: warm white page with gold silk lines, navy banners and type, gold accents,
// and 3D tile art (drawable-nodpi/theme4_*).
private val RoyalInk = Color(0xFF0B1446)
private val RoyalTitle = Color(0xFF171B4E)
private val RoyalText = Color(0xFF1E2269)
private val RoyalMuted = Color(0xFF7882A5)
private val RoyalGold = Color(0xFFA36F28)
private val RoyalGoldLight = Color(0xFFE9B562)
private val RoyalPage = Color(0xFFFCFBF8)
private val RoyalTile = Color(0xFFFDFAF6) // The tile art's backdrop: keep the cards this colour.
private val RoyalLine = Color(0xFFF0ECE5)
private val RoyalShadow = Color(0x260B1446)
private val RoyalNavyLight = Color(0xFF1F437B)
private val RoyalNavy = Color(0xFF0E2E66)
private val RoyalNavyDeep = Color(0xFF092A5B)
private val RoyalRed = Color(0xFFFC4F4F)

// Laid out at a 411dp design width and scaled to the screen, like Themes 0, 2 and 3.
private fun Float.u(scale: Float) = (this * scale).dp
private fun Int.u(scale: Float) = toFloat().u(scale)
private fun Float.t(scale: Float) = (this * scale).sp
private fun Int.t(scale: Float) = toFloat().t(scale)

/** A soft navy shadow; on Android 8.1 and older it falls back to the platform's grey. */
private fun Modifier.royalShadow(shape: Shape, elevation: Dp) =
    shadow(elevation, shape, clip = false, ambientColor = RoyalShadow, spotColor = RoyalShadow)

private fun Modifier.royalCard(shape: Shape, s: Float, color: Color = Color.White) =
    royalShadow(shape, 5.u(s)).clip(shape).background(color).border(1.dp, RoyalLine, shape)

@Composable
internal fun ThemeFourHomeScreen(navController: NavController) {
    var showSos by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var showAccessPass by remember { mutableStateOf(false) }
    if (showAccessPass) AccessQrScreen(onDismiss = { showAccessPass = false })
    if (showInvite) CreateInviteSheet(
        onDismiss = { showInvite = false },
        onHistory = { showInvite = false; navController.navigate(Screen.Visitors.route) },
        onCreated = { showInvite = false; navController.navigate(Screen.InviteCreated.create(it.id)) }
    )
    BoxWithConstraints(Modifier.fillMaxSize().background(RoyalPage), contentAlignment = Alignment.TopCenter) {
        val s = minOf(maxWidth.value, 600f) / 411f
        val scroll = rememberScrollState()
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(scroll)) {
            Box {
                SilkLines(Modifier.matchParentSize())
                Column(Modifier.statusBarsPadding().padding(horizontal = 17.u(s))) {
                    RoyalHeader(s) { navController.navigate(Screen.Notifications.route) }
                    RoyalGreeting(
                        s,
                        onProfile = { navController.navigate(Screen.Profile.route) },
                        onQuickAccess = { showAccessPass = true }
                    )
                    RoyalSearch(s) { navController.navigate(Screen.Search.route) }
                }
            }
            Column(Modifier.padding(horizontal = 17.u(s))) {
                Spacer(Modifier.height(16.u(s)))
                CreateInviteBanner(s) { showInvite = true }
                Spacer(Modifier.height(14.u(s)))
                Row(horizontalArrangement = Arrangement.spacedBy(7.u(s))) {
                    RoyalTileCard(R.drawable.theme4_tile_access, "Quick Access", "QR / Bluetooth", s, Modifier.weight(1f)) {
                        showAccessPass = true
                    }
                    RoyalTileCard(R.drawable.theme4_tile_facility, "Book Facility", "Amenities", s, Modifier.weight(1f)) {
                        navController.navigate(Screen.BookFacility.route)
                    }
                    RoyalTileCard(R.drawable.theme4_tile_community, "Community", "Notices & Events", s, Modifier.weight(1f)) {
                        navController.navigate(Screen.Community.route)
                    }
                    RoyalTileCard(R.drawable.theme4_tile_sos, "SOS", "Get Help", s, Modifier.weight(1f)) { showSos = true }
                }
                Spacer(Modifier.height(12.u(s)))
                MyUnitCard(s) { navController.navigate(Screen.LocationPicker.route) }
                Spacer(Modifier.height(12.u(s)))
                AnnouncementBanner(s) { navController.navigate(Screen.Announcements.route) }
                // Feedback appears only once it's switched on in Profile.
                if (AppPreferences.showFeedback) {
                    Spacer(Modifier.height(12.u(s)))
                    FeedbackRow(s) { navController.navigate(Screen.Feedback.route) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.u(s), bottom = 10.u(s)), verticalAlignment = Alignment.CenterVertically) {
                    Text("Upcoming Bookings", Modifier.weight(1f), color = RoyalInk, fontFamily = DMSans,
                        fontWeight = FontWeight.Bold, fontSize = 18.t(s), lineHeight = 24.t(s), maxLines = 1)
                    Row(
                        Modifier.clip(RoundedCornerShape(8.u(s))).clickable(role = Role.Button) { navController.navigate(Screen.Bookings.route) }
                            .padding(horizontal = 4.u(s), vertical = 4.u(s)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("View All", color = RoyalGold, fontFamily = DMSans, fontWeight = FontWeight.Medium,
                            fontSize = 14.5f.t(s), lineHeight = 18.t(s))
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.u(s)), tint = RoyalGold)
                    }
                }
                UpcomingBookingCard(navController, s)
                Spacer(Modifier.height(18.u(s)))
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

/** Gold and pale-blue silk lines sweeping across the top right, behind the header. Shared with the visitor invite flow. */
@Composable
internal fun SilkLines(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val haze = Offset(w * .95f, 0f)
        drawCircle(Brush.radialGradient(listOf(Color(0x40B8D2F2), Color(0x00B8D2F2)), center = haze, radius = w * .8f), w * .8f, haze)
        fun band(count: Int, color: Color, startX: Float, endY: Float, spread: Float, strength: Float) {
            repeat(count) { i ->
                val t = i / (count - 1f)
                val path = Path().apply {
                    moveTo(w * (startX + .12f * t), -8f)
                    cubicTo(
                        w * (startX + .16f + .06f * t), h * (.2f + .08f * t),
                        w * (.8f + .04f * t), h * (endY - .1f + spread * t),
                        w * 1.04f, h * (endY + spread * t)
                    )
                }
                // Brightest in the middle of the band, fading to both edges.
                val alpha = strength * (1f - abs(t - .5f) * 1.6f)
                drawPath(path, color.copy(alpha = alpha.coerceAtLeast(.03f)), style = Stroke(1.dp.toPx()))
            }
        }
        band(28, Color(0xFFD8AE62), .46f, .3f, .3f, .42f)
        band(14, Color(0xFFBFD3EE), .62f, .55f, .25f, .5f)
    }
}

@Composable
private fun RoyalHeader(s: Float, onNotifications: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.u(s)), verticalAlignment = Alignment.CenterVertically) {
        GreenGateWordmark(s * 1.12f)
        Spacer(Modifier.weight(1f))
        Box {
            RoyalRoundButton(Icons.Outlined.Notifications, "Notifications", s, onNotifications)
            Box(Modifier.align(Alignment.TopEnd).offset(x = (-2).u(s), y = 2.u(s)).size(10.u(s)).background(RoyalRed, CircleShape))
        }
    }
}

@Composable
private fun RoyalRoundButton(icon: ImageVector, label: String, s: Float, onClick: () -> Unit) {
    Box(
        Modifier.size(46.u(s)).royalShadow(CircleShape, 6.u(s)).clip(CircleShape).background(Color.White)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, Modifier.size(25.u(s)), tint = RoyalInk)
    }
}

/** The GREENGATE / CONDO lockup with the gold gate, at the 411dp design scale [s]. Shared with the AI Concierge. */
@Composable
internal fun GreenGateWordmark(s: Float, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.theme4_gate), null, Modifier.size(36.u(s)))
        Column(Modifier.padding(start = 3.u(s)), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GREENGATE", color = RoyalInk, fontFamily = PlayfairDisplay, fontSize = 19.t(s),
                lineHeight = 22.t(s), letterSpacing = .6f.t(s), maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(18.u(s)).height(1.dp).background(RoyalGoldLight))
                Text("CONDO", Modifier.padding(horizontal = 6.u(s)), color = RoyalInk, fontFamily = DMSans,
                    fontSize = 7.5f.t(s), lineHeight = 10.t(s), letterSpacing = 3.t(s), maxLines = 1)
                Box(Modifier.width(18.u(s)).height(1.dp).background(RoyalGoldLight))
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning,"
    in 12..16 -> "Good afternoon,"
    else -> "Good evening,"
}

@Composable
private fun RoyalGreeting(s: Float, onProfile: () -> Unit, onQuickAccess: () -> Unit) {
    Row(Modifier.padding(top = 14.u(s)), verticalAlignment = Alignment.CenterVertically) {
        RoyalAvatar(s, onProfile)
        Column(Modifier.weight(1f).padding(start = 15.u(s), end = 8.u(s))) {
            Text(greeting(), color = RoyalText, fontFamily = DMSans, fontSize = 15.5f.t(s), lineHeight = 20.t(s), maxLines = 1)
            Text(ResidentSession.firstName, color = RoyalInk, fontFamily = DMSans, fontWeight = FontWeight.Bold,
                fontSize = 27.t(s), lineHeight = 33.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
            ResidentSession.condoName.takeIf { it.isNotBlank() }?.let {
                Text(it, color = RoyalText, fontFamily = DMSans, fontSize = 15.5f.t(s), lineHeight = 20.t(s),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(
            Modifier.clickable(role = Role.Button, onClickLabel = "Open access pass", onClick = onQuickAccess),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val shape = RoundedCornerShape(14.u(s))
            Box(
                Modifier.size(72.u(s), 64.u(s)).royalShadow(shape, 6.u(s)).background(Color.White, shape),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.theme4_qr), null, Modifier.size(48.u(s)))
            }
            Text("Quick Access", Modifier.padding(top = 7.u(s)), color = RoyalInk, fontFamily = DMSans,
                fontWeight = FontWeight.Medium, fontSize = 13.t(s), lineHeight = 16.t(s), maxLines = 1)
        }
    }
}

/** The resident's initial in a gold ring. There's no profile photo in the app yet. */
@Composable
private fun RoyalAvatar(s: Float, onClick: () -> Unit) {
    // firstName falls back to "there" (as in "Good morning, there") when the household has no name.
    val initial = ResidentSession.firstName.takeIf { it != "there" }?.firstOrNull()?.uppercaseChar()
    Box(
        Modifier.size(80.u(s)).royalShadow(CircleShape, 6.u(s)).clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFF7D58E), Color(0xFFC0812A), Color(0xFFF3C97A))))
            .clickable(role = Role.Button, onClickLabel = "Profile", onClick = onClick)
            .padding(3.u(s)).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFCF5), Color(0xFFF6ECDA)))),
        contentAlignment = Alignment.Center
    ) {
        if (initial != null) Text(initial.toString(), color = RoyalInk, fontFamily = PlayfairDisplay,
            fontWeight = FontWeight.Bold, fontSize = 32.t(s))
        else Icon(Icons.Rounded.Person, null, Modifier.size(40.u(s)), tint = RoyalInk)
    }
}

@Composable
private fun RoyalSearch(s: Float, onClick: () -> Unit) {
    val shape = CircleShape
    Row(
        Modifier.padding(top = 14.u(s)).fillMaxWidth().height(50.u(s)).royalShadow(shape, 5.u(s)).clip(shape)
            .background(Color.White).clickable(role = Role.Button, onClickLabel = "Search", onClick = onClick)
            .padding(start = 16.u(s), end = 16.u(s)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, Modifier.size(25.u(s)), tint = RoyalInk)
        Text("Search your community", Modifier.weight(1f).padding(start = 12.u(s)), color = RoyalMuted,
            fontFamily = DMSans, fontSize = 16.t(s), lineHeight = 20.t(s), maxLines = 1)
        Icon(Icons.Rounded.Tune, null, Modifier.size(23.u(s)), tint = RoyalGold)
    }
}

@Composable
private fun CreateInviteBanner(s: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.u(s))
    Box(
        Modifier.fillMaxWidth().height(134.u(s)).royalShadow(shape, 8.u(s)).clip(shape)
            .background(Brush.linearGradient(listOf(RoyalNavyLight, RoyalNavyDeep)))
            .clickable(role = Role.Button, onClickLabel = "Create invite", onClick = onClick)
    ) {
        Image(painterResource(R.drawable.theme4_invite_art), null, Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            contentScale = ContentScale.FillHeight)
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.66f).padding(start = 20.u(s))) {
            Text("Create Invite", color = Color.White, fontFamily = DMSans, fontWeight = FontWeight.Bold,
                fontSize = 27.t(s), lineHeight = 32.t(s), maxLines = 1)
            Text("Give access to your visitors easily", Modifier.padding(top = 2.u(s)), color = Color.White.copy(alpha = .9f),
                fontFamily = DMSans, fontSize = 14.t(s), lineHeight = 18.t(s), maxLines = 2)
            Row(
                Modifier.padding(top = 12.u(s)).height(40.u(s)).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFF8E2B8), Color(0xFFE9C27F))))
                    .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.u(s)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Create Invite", color = RoyalInk, fontFamily = DMSans, fontWeight = FontWeight.Bold,
                    fontSize = 15.t(s), lineHeight = 19.t(s), maxLines = 1)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 10.u(s)).size(20.u(s)), tint = RoyalInk)
            }
        }
    }
}

/**
 * A quarter-width tile: 3D art, then title and subtitle. The chevron sits top right, and the title shrinks
 * to fit, as four DM Sans titles with inline chevrons don't fit a phone. Long subtitles wrap to a second line.
 */
@Composable
private fun RoyalTileCard(@DrawableRes art: Int, title: String, subtitle: String, s: Float, modifier: Modifier, onClick: () -> Unit) {
    val view = LocalView.current
    val shape = RoundedCornerShape(16.u(s))
    Box(
        modifier.height(120.u(s)).royalCard(shape, s, RoyalTile)
            .clickable(role = Role.Button) { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); onClick() }
    ) {
        Image(painterResource(art), null, Modifier.align(Alignment.TopCenter).padding(top = 8.u(s)).size(56.u(s)))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
            Modifier.align(Alignment.TopEnd).padding(top = 6.u(s), end = 4.u(s)).size(16.u(s)), tint = RoyalGold)
        // Title and subtitle centred under the centred icon.
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 5.u(s)).padding(top = 70.u(s)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FitText(title, RoyalTitle, FontWeight.Bold, 12.5f.t(s), 10.5f.t(s))
            Text(subtitle, Modifier.fillMaxWidth(), color = RoyalMuted, fontFamily = DMSans, fontSize = 10.5f.t(s),
                lineHeight = 13.t(s), maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

/** One line that steps its size down from [max] toward [min] until it fits the width it's given. */
@Composable
private fun FitText(text: String, color: Color, weight: FontWeight, max: TextUnit, min: TextUnit) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val size = remember(text, width, max, min) {
            var sp = max.value
            while (sp > min.value && measurer.measure(
                    text, TextStyle(fontFamily = DMSans, fontWeight = weight, fontSize = sp.sp), maxLines = 1, softWrap = false
                ).size.width > width
            ) sp -= .5f
            sp.sp
        }
        Text(text, Modifier.fillMaxWidth(), color = color, fontFamily = DMSans, fontWeight = weight, fontSize = size,
            lineHeight = (size.value * 1.25f).sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun MyUnitCard(s: Float, onClick: () -> Unit) {
    // unitLabel reads like "Block A · #05-01": the unit is shown large and the block beside the condo.
    val parts = ResidentSession.unitLabel.split(" · ").filter { it.isNotBlank() }
    val unit = parts.lastOrNull() ?: "Your home"
    val details = (parts.dropLast(1) + listOfNotNull(ResidentSession.condoName.takeIf { it.isNotBlank() })).joinToString(" • ")
    val shape = RoundedCornerShape(16.u(s))
    Row(
        Modifier.fillMaxWidth().royalCard(shape, s).clickable(role = Role.Button, onClick = onClick).padding(8.u(s)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.theme4_unit_photo), null,
            Modifier.size(116.u(s), 64.u(s)).clip(RoundedCornerShape(12.u(s))), contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f).padding(horizontal = 14.u(s))) {
            Text("My Unit", color = RoyalMuted, fontFamily = DMSans, fontSize = 12.5f.t(s), lineHeight = 16.t(s))
            Text(unit, color = RoyalInk, fontFamily = DMSans, fontWeight = FontWeight.Bold, fontSize = 24.t(s),
                lineHeight = 29.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (details.isNotBlank()) Text(details, color = RoyalMuted, fontFamily = DMSans, fontSize = 13.t(s),
                lineHeight = 17.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(26.u(s)), tint = RoyalInk)
    }
}

@Composable
private fun AnnouncementBanner(s: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.u(s))
    Row(
        Modifier.fillMaxWidth().height(64.u(s)).royalShadow(shape, 6.u(s)).clip(shape)
            .background(Brush.horizontalGradient(listOf(RoyalNavy, RoyalNavyDeep, Color(0xFF1A447F))))
            .clickable(role = Role.Button, onClickLabel = "Read announcement", onClick = onClick)
            .padding(start = 12.u(s), end = 10.u(s)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.theme4_megaphone), null,
            Modifier.size(46.u(s), 36.u(s)).clip(RoundedCornerShape(10.u(s))), contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f).padding(horizontal = 12.u(s))) {
            Text("Latest Announcement", color = Color.White, fontFamily = DMSans, fontWeight = FontWeight.Medium,
                fontSize = 15.5f.t(s), lineHeight = 20.t(s), maxLines = 1)
            Text("Water supply maintenance on 28 Oct, 9 AM – 12 PM", color = Color.White.copy(alpha = .85f),
                fontFamily = DMSans, fontSize = 12.t(s), lineHeight = 16.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(8.u(s)).background(RoyalRed, CircleShape))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.padding(start = 4.u(s)).size(26.u(s)), tint = Color.White)
    }
}

@Composable
private fun FeedbackRow(s: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.u(s))
    Row(
        Modifier.fillMaxWidth().height(52.u(s)).royalCard(shape, s).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.u(s)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Feedback, null, Modifier.size(24.u(s)), tint = RoyalInk)
        Text("Feedback", Modifier.weight(1f).padding(start = 12.u(s)), color = RoyalInk, fontFamily = DMSans,
            fontWeight = FontWeight.Bold, fontSize = 15.t(s))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(24.u(s)), tint = RoyalInk)
    }
}

/** "Today", "Tomorrow", or e.g. "Fri 30 Oct". */
private fun bookingDay(ms: Long, now: Long): String {
    fun dayOf(t: Long) = Calendar.getInstance().apply { timeInMillis = t }
        .let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
    val tomorrow = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    return when (dayOf(ms)) {
        dayOf(now) -> "Today"
        dayOf(tomorrow) -> "Tomorrow"
        else -> SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(Date(ms))
    }
}

/** The soonest upcoming booking with its facility photo; with none, an invitation to book. */
@Composable
private fun UpcomingBookingCard(nav: NavController, s: Float) {
    val now = System.currentTimeMillis()
    val next = BookingStore.bookings.filter { it.status(now) == BookingStatus.UPCOMING }.minByOrNull { it.startAt }
    val shape = RoundedCornerShape(16.u(s))
    Row(
        Modifier.fillMaxWidth().royalCard(shape, s)
            .clickable(role = Role.Button) {
                nav.navigate(if (next != null) Screen.BookingDetail.create(next.id) else Screen.BookFacility.route)
            }
            .padding(8.u(s)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (next != null) {
            Image(painterResource(next.facility.photo), null,
                Modifier.size(108.u(s), 56.u(s)).clip(RoundedCornerShape(12.u(s))), contentScale = ContentScale.Crop)
        } else {
            Image(painterResource(R.drawable.theme4_tile_facility), null, Modifier.size(56.u(s)))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.u(s))) {
            Text(next?.facility?.name ?: "No upcoming bookings", color = RoyalInk, fontFamily = DMSans,
                fontWeight = FontWeight.Bold, fontSize = 15.5f.t(s), lineHeight = 20.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (next != null) "${bookingDay(next.startAt, now)} • ${next.timeRange}" else "Book a facility in a few taps",
                Modifier.padding(top = 2.u(s)), color = RoyalMuted, fontFamily = DMSans, fontSize = 12.5f.t(s),
                lineHeight = 17.t(s), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(26.u(s)), tint = RoyalInk)
    }
}

@Composable
internal fun ThemeFourBottomNavigation(nav: NavController, route: String?) {
    fun navigate(next: String) {
        if (next != route) nav.navigate(next) { popUpTo(Screen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true }
    }
    // The selected tab's navy tile is one shape that glides to the tapped tab, on the same curve as the page slide.
    val tiles = remember { mutableStateMapOf<String, Rect>() }
    val tile = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    val target = tiles[route]
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        if (tile.value.isEmpty) tile.snapTo(target) else tile.animateTo(target, tween(TabSlideMillis, easing = FastOutSlowInEasing))
    }
    fun onTile(tab: String): (Rect) -> Unit = { if (tiles[tab] != it) tiles[tab] = it }
    // A floating white bar on the page colour; not clipped, so the AI Chat button can rise above it.
    Box(Modifier.fillMaxWidth().background(RoyalPage).navigationBarsPadding().height(BottomBarHeight).padding(horizontal = 12.dp)) {
        val shape = RoundedCornerShape(30.dp)
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).fillMaxWidth().height(64.dp)
                .royalShadow(shape, 10.dp).clip(shape).background(Color.White).padding(horizontal = 6.dp)
                .drawBehind {
                    val r = tile.value
                    if (route in tiles && !r.isEmpty) drawRoundRect(
                        Brush.verticalGradient(listOf(Color(0xFF1B4486), Color(0xFF0E2C5E)), startY = r.top, endY = r.bottom),
                        r.topLeft, r.size, CornerRadius(16.dp.toPx())
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoyalNavItem(Icons.Rounded.Home, "Home", route == Screen.Home.route, Modifier.weight(1f), onTile(Screen.Home.route)) {
                navigate(Screen.Home.route)
            }
            RoyalNavItem(Icons.Outlined.DoorFront, "Access", route == Screen.Access.route, Modifier.weight(1f), onTile(Screen.Access.route)) {
                navigate(Screen.Access.route)
            }
            Spacer(Modifier.weight(1f))
            RoyalNavItem(Icons.Outlined.Groups, "Community", route == Screen.Community.route, Modifier.weight(1f), onTile(Screen.Community.route)) {
                navigate(Screen.Community.route)
            }
            RoyalNavItem(Icons.Outlined.GridView, "More", route == Screen.Profile.route, Modifier.weight(1f), onTile(Screen.Profile.route)) {
                navigate(Screen.Profile.route)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                .clickable(interactionSource = null, indication = null, role = Role.Button, onClickLabel = "Open AI Chat") {
                    nav.navigate(Screen.GreenBot.route)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AiChatButton(Modifier.size(50.dp))
            Text("AI Chat", Modifier.padding(top = 1.dp), color = RoyalMuted, fontFamily = DMSans, fontSize = 12.sp,
                lineHeight = 15.sp, maxLines = 1)
        }
    }
}

/** Navy disc with a gold ring and a gold voice pulse. */
@Composable
private fun AiChatButton(modifier: Modifier) {
    Canvas(modifier.royalShadow(CircleShape, 6.dp)) {
        val r = size.minDimension / 2f
        drawCircle(Brush.linearGradient(listOf(Color(0xFFF3CF85), Color(0xFFB7802E), Color(0xFFF3CF85))), r)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF2E5592), RoyalNavyDeep), center = center - Offset(r * .3f, r * .35f), radius = r * 1.4f), r * .88f)
        val pulse = Path().apply {
            val y = center.y
            moveTo(center.x - r * .55f, y)
            lineTo(center.x - r * .22f, y)
            lineTo(center.x - r * .1f, y - r * .32f)
            lineTo(center.x + .05f * r, y + r * .3f)
            lineTo(center.x + r * .17f, y - r * .12f)
            lineTo(center.x + r * .25f, y)
            lineTo(center.x + r * .55f, y)
        }
        drawPath(pulse, Color(0xFFF3CF85), style = Stroke(r * .09f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

/**
 * A tab: a gold icon, and when selected a light gold icon and label on a navy tile. The bar draws
 * that tile; [onTile] reports where it goes, in the bar's coordinates.
 */
@Composable
private fun RoyalNavItem(
    icon: ImageVector, title: String, selected: Boolean, modifier: Modifier, onTile: (Rect) -> Unit, onClick: () -> Unit
) {
    val fade = tween<Color>(TabSlideMillis, easing = FastOutSlowInEasing)
    val iconTint by animateColorAsState(if (selected) RoyalGoldLight else RoyalGold, fade, label = "tabIcon")
    val textColor by animateColorAsState(if (selected) RoyalGoldLight else RoyalMuted, fade, label = "tabText")
    val tilePadding = with(LocalDensity.current) { 8.dp.toPx() }
    // No ripple: the default fills the whole slot with a pale blue square. The selected tile is the feedback.
    Box(
        modifier.fillMaxHeight().selectable(selected, interactionSource = null, indication = null, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .onGloballyPositioned { content ->
                    // The tile wraps the icon and label with 10dp each side, kept inside this tab's slot.
                    val slot = content.parentLayoutCoordinates ?: return@onGloballyPositioned
                    val bar = slot.parentLayoutCoordinates ?: return@onGloballyPositioned
                    val c = bar.localBoundingBoxOf(content, clipBounds = false)
                    val s = bar.localBoundingBoxOf(slot, clipBounds = false)
                    onTile(Rect(max(s.left, c.left - tilePadding), c.top, min(s.right, c.right + tilePadding), c.bottom))
                }
                .padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(25.dp), tint = iconTint)
            Text(title, color = textColor, fontFamily = DMSans,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, fontSize = 11.5f.sp, lineHeight = 15.sp,
                maxLines = 1, softWrap = false)
        }
    }
}
