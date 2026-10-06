package com.example.greengate

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.greengate.ui.theme.DMSans
import com.example.greengate.ui.theme.GreenGateTheme

internal val MarinaBackground: Color @Composable get() = MaterialTheme.colorScheme.background
internal val MarinaSosRed = Color(0xFFDF4336)
private val MarinaInk: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val MarinaMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val MarinaGreen: Color @Composable get() = MaterialTheme.colorScheme.primary
private val MarinaCoral = Color(0xFFFF704F)
private val MarinaCream: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

/** The supplied images are screenshot crops. Source rectangles exclude baked-in labels
 * and card edges; all text and interactive controls are rendered by Compose. */
@Composable
private fun MarinaArt(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    crop: IntArray? = null,
    description: String? = null,
    scale: ContentScale = ContentScale.Fit
) {
    val bitmap = ImageBitmap.imageResource(resource)
    val painter = remember(bitmap, crop?.toList()) {
        BitmapPainter(
            bitmap,
            srcOffset = crop?.let { IntOffset(it[0], it[1]) } ?: IntOffset.Zero,
            srcSize = crop?.let { IntSize(it[2], it[3]) } ?: IntSize(bitmap.width, bitmap.height)
        )
    }
    Image(painter, description, modifier, contentScale = scale)
}

// Feather the edges of screenshot illustrations into the native card surface.
private fun Modifier.softArtEdges() = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    drawRect(Brush.horizontalGradient(0f to Color.Transparent, .12f to Color.Black,
        .88f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    drawRect(Brush.verticalGradient(0f to Color.Transparent, .12f to Color.Black,
        .90f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
}

@Composable
fun HomeScreen(navController: NavController) {
    if (AppPreferences.theme == AppTheme.ZERO) {
        ThemeZeroHomeScreen(navController)
        return
    }
    var showSos by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().then(if (AppPreferences.theme == AppTheme.ONE) Modifier.background(MarinaBackground) else Modifier).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
            MarinaHeader(navController, onSos = { showSos = true })
            CommunityAnnouncementCard { navController.navigate(Screen.Announcements.route) }
            Spacer(Modifier.height(9.dp))
            ActionGrid(navController)
            Spacer(Modifier.height(9.dp))
            TodayAtAGlance(navController)
            Spacer(Modifier.height(9.dp))
            if (AppPreferences.theme == AppTheme.ONE) AnnouncementsBanner { navController.navigate(Screen.Announcements.route) }
            Spacer(Modifier.height(18.dp))
        }
    }
    if (showSos) {
        AlertDialog(
            onDismissRequest = { showSos = false },
            icon = { Icon(Icons.Rounded.Call, null, tint = MarinaCoral) },
            title = { Text("Emergency assistance") },
            text = { Text("An emergency contact has not been configured for this residence. For immediate help, call your local emergency number using your phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSos = false
                        navController.navigate(Screen.CallTest.route)
                    }
                ) {
                    Text("Call Receiver Harness")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSos = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MarinaHeader(navController: NavController, onSos: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        MarinaArt(R.drawable.marina_decor_top_left_leaves,
            Modifier.offset(x = (-16).dp, y = (-3).dp).size(110.dp, 65.dp).softArtEdges(),
            crop = intArrayOf(18, 40, 210, 110))
        Column {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 13.dp),
                horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                HeaderButton(Icons.Outlined.Search, "Search") { navController.navigate(Screen.Search.route) }
                Spacer(Modifier.width(9.dp))
                Box {
                    HeaderButton(Icons.Outlined.Notifications, "Notifications") { navController.navigate(Screen.Notifications.route) }
                    Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(8.dp).background(MarinaCoral, CircleShape))
                }
                Spacer(Modifier.width(9.dp))
                Surface(onClick = onSos, shape = CircleShape, color = MarinaSosRed,
                    shadowElevation = 4.dp, modifier = Modifier.size(48.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.Call, "Emergency assistance", tint = Color.White, modifier = Modifier.size(21.dp))
                        Text("SOS", color = Color.White, fontFamily = DMSans, fontSize = 10.sp, lineHeight = 12.sp)
                    }
                }
                Spacer(Modifier.width(9.dp))
                Surface(onClick = { navController.navigate(Screen.Profile.route) }, shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp, modifier = Modifier.size(48.dp)) {
                    MarinaArt(R.drawable.marina_profile_avatar_reference, Modifier.fillMaxSize().padding(2.dp).clip(CircleShape),
                        crop = intArrayOf(5, 10, 88, 88), description = "Alex's profile", scale = ContentScale.Crop)
                }
            }
            Text("Good Morning, Alex", fontFamily = DMSans, fontSize = 24.sp,
                lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = MarinaInk)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Marina Bay Residences", fontFamily = DMSans, fontSize = 13.sp,
                    lineHeight = 20.sp, letterSpacing = 2.4.sp, color = MarinaMuted)
                IconButton({ navController.navigate(Screen.CommunityInfo.route) }, Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Info, "About Marina Bay Residences", tint = MarinaMuted, modifier = Modifier.size(18.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(100.dp)) {
                MarinaArt(R.drawable.marina_hero_residence_skyline,
                    Modifier.align(Alignment.BottomEnd).offset(x = 16.dp).fillMaxWidth(.76f).height(92.dp).softArtEdges(),
                    crop = intArrayOf(20, 45, 560, 165), scale = ContentScale.Crop)
                Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp)) {
                    LocationPill("Tower A · Unit 12-03", Icons.Rounded.Apartment) { navController.navigate(Screen.LocationPicker.route) }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(bottom = 15.dp)) {
                    Box(Modifier.padding(bottom = 10.dp).size(34.dp, 2.dp).background(MarinaCoral, CircleShape))
                    Text("A more connected\nand comfortable\ntomorrow, together.", fontFamily = DMSans,
                        fontSize = 13.sp, lineHeight = 15.sp, color = MarinaMuted)
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        IconButton(onClick, Modifier.size(44.dp)) {
            Icon(icon, label, tint = MarinaInk, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun LocationPill(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .96f), shadowElevation = 3.dp) {
        Row(Modifier.heightIn(min = 36.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(16.dp), tint = MarinaGreen)
            Spacer(Modifier.width(4.dp))
            Text(label, fontFamily = DMSans, fontSize = 10.sp, lineHeight = 14.sp, color = MarinaInk)
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(15.dp), tint = MarinaInk)
        }
    }
}

@Composable
private fun MarinaCard(modifier: Modifier = Modifier, color: Color = MarinaCream, onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit) {
    Surface(modifier = modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        shape = RoundedCornerShape(if (AppPreferences.theme == AppTheme.ZERO) 22.dp else 16.dp), color = color, shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .85f)), content = content)
}

@Composable
private fun CommunityAnnouncementCard(onClick: () -> Unit) {
    MarinaCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            MarinaArt(R.drawable.marina_icon_community_announcement, Modifier.size(65.dp, 53.dp).softArtEdges(),
                crop = intArrayOf(28, 18, 118, 100))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(if (AppPreferences.theme == AppTheme.ZERO) "Priority Announcements" else "Important Message", fontFamily = DMSans, fontSize = 11.sp, lineHeight = 16.sp, color = if (AppPreferences.theme == AppTheme.ZERO) MarinaGreen else MarinaCoral)
                Spacer(Modifier.height(3.dp))
                Text("Pool maintenance on Friday,\n10:00 AM – 12:00 PM", fontFamily = DMSans,
                    fontSize = 12.sp, lineHeight = 16.sp, color = MarinaInk)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View", fontFamily = DMSans, fontSize = 11.sp, lineHeight = 16.sp, color = if (AppPreferences.theme == AppTheme.ZERO) MarinaGreen else MarinaCoral)
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = MarinaCoral)
            }
        }
    }
}

// Each entry's `icon` renders that card's animation only while it is told it's active; a
// duration tells the sequencer how long that animation takes to play through once. Appending
// a new entry here is all a future icon needs to join the rotation below.
// `layeredArt` picks the layered sets' artwork (0 facility, 1 invite, 2 forms, 3 feedback);
// null means Community, which has its own layered illustration.
private data class ActionIconSpec(
    val title: String,
    val subtitle: String,
    val route: String,
    val durationMillis: Int,
    val layeredArt: Int?,
    val color: Color = Color(0xFFFFFFFF),
    val icon: @Composable (isActive: Boolean) -> Unit
)

@Composable
private fun ActionGrid(navController: NavController) {
    val iconSet = AppPreferences.iconSet
    val allIcons = remember {
        listOf(
            ActionIconSpec("Book Facility", "Reserve amenities\nwith ease", Screen.BookFacility.route, 1400, 0) {
                if (AppPreferences.iconSet == IconSet.THREE) DummyActionIcon(Icons.Rounded.CalendarMonth, MarinaGreen, it)
                else BookFacilityIcon(it)
            },
            ActionIconSpec("Green Invite", "Create visitor\npasses quickly", Screen.InviteVisitors.route, 1800, 1) {
                if (AppPreferences.iconSet == IconSet.THREE) DummyActionIcon(Icons.Rounded.PersonAdd, MarinaCoral, it)
                else InviteVisitorsIcon(it)
            },
            ActionIconSpec("E-Forms", "Submit and manage\ndigital forms", Screen.EForms.route, 1800, 2) {
                if (AppPreferences.iconSet == IconSet.THREE) DummyActionIcon(Icons.Rounded.Description, MarinaGreen, it)
                else EFormsIcon(it)
            },
            ActionIconSpec("Community", "Connect with\nyour neighbours", Screen.Community.route, 1800, null) {
                DummyActionIcon(Icons.Rounded.Groups, MarinaGreen, it)
            },
            ActionIconSpec("Feedback", "Share ideas and\nhelp us improve", Screen.Feedback.route, 1300, 3) {
                if (AppPreferences.iconSet == IconSet.THREE) DummyActionIcon(Icons.Rounded.Feedback, MarinaCoral, it)
                else FeedbackIcon(it)
            }
        )
    }
    // Feedback appears only once it's switched on in Profile; the rotation follows what's shown.
    val icons = if (AppPreferences.showFeedback) allIcons else allIcons.filter { it.route != Screen.Feedback.route }
    val sequence = rememberActionIconSequence(iconSet, icons.map { icon ->
        if ((iconSet == IconSet.ZERO || iconSet == IconSet.CLAY || iconSet == IconSet.ONE) && icon.layeredArt == 3) 2100 else icon.durationMillis
    })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        icons.withIndex().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { (index, spec) ->
                    ActionCard(if (AppPreferences.theme == AppTheme.ZERO && spec.route == Screen.InviteVisitors.route) "Invite Visitors" else if (AppPreferences.theme == AppTheme.ZERO && spec.route == Screen.Community.route) "My Community" else spec.title, spec.subtitle, Modifier.weight(1f), spec.color,
                        onClick = { navController.navigate(spec.route) }) {
                        val active = index == sequence.activeIndex
                        val progress = if (active) sequence.progress.value else 0f
                        val art = spec.layeredArt
                        when {
                            iconSet == IconSet.THREE -> spec.icon(active)
                            art == null && (iconSet == IconSet.ZERO || iconSet == IconSet.CLAY) -> CommunitySetZeroIcon(progress, active, iconSet = iconSet)
                            art == null -> CommunityIcon(progress, active)
                            iconSet == IconSet.TWO -> LayeredActionIcon(art, progress, active)
                            else -> LayeredIconSetOne(art, progress, active, iconSet = iconSet)
                        }
                    }
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// Each action icon keeps the rest of its illustration static and only animates the one
// element the design calls for. A flat, colour-matched patch is drawn over that element's
// resting spot (hiding the baked-in pixel copy underneath) and a live cutout of the same
// region is layered on top and animated away from it, so only that piece appears to move.
@Composable
private fun IconStage(width: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(85.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(width).height(85.dp), content = content)
    }
}

@Composable
private fun rememberVerticalBounce(): State<Float> {
    val transition = rememberInfiniteTransition(label = "ballBounce")
    return transition.animateFloat(0f, 0f, infiniteRepeatable(
        animation = keyframes {
            durationMillis = 1400
            0f at 0 using FastOutSlowInEasing
            -8f at 260 using FastOutSlowInEasing
            0f at 500 using LinearEasing
            -3f at 650 using FastOutSlowInEasing
            0f at 820
            0f at 1400
        }
    ), label = "ballBounceOffset")
}

@Composable
private fun rememberHorizontalGlide(): State<Float> {
    val transition = rememberInfiniteTransition(label = "penGlide")
    return transition.animateFloat(-5f, 5f, infiniteRepeatable(
        animation = tween(900, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ), label = "penGlideOffset")
}

@Composable
private fun rememberPopScale(): State<Float> {
    val transition = rememberInfiniteTransition(label = "personPop")
    return transition.animateFloat(1f, 1f, infiniteRepeatable(
        animation = keyframes {
            durationMillis = 1800
            1f at 0 using FastOutSlowInEasing
            1.2f at 220 using FastOutSlowInEasing
            0.95f at 340 using LinearEasing
            1.05f at 440 using FastOutSlowInEasing
            1f at 540
            1f at 1800
        }
    ), label = "personPopScale")
}

@Composable
private fun rememberTypingBounce(delayMillis: Int): State<Float> {
    val transition = rememberInfiniteTransition(label = "typingDot")
    return transition.animateFloat(0f, 0f, infiniteRepeatable(
        animation = keyframes {
            durationMillis = 900
            0f at 0 using FastOutSlowInEasing
            -5f at 150 using FastOutSlowInEasing
            0f at 300 using FastOutSlowInEasing
            0f at 900
        },
        initialStartOffset = StartOffset(delayMillis)
    ), label = "typingDotOffset")
}

// The original flat icons now live in Icon Set 3.
@Composable
private fun DummyActionIcon(icon: ImageVector, tint: Color, isActive: Boolean) {
    val bounce = if (isActive) { val b by rememberVerticalBounce(); b } else 0f
    Box(Modifier.fillMaxWidth().height(85.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(56.dp).graphicsLayer { translationY = bounce.dp.toPx() }
                .clip(CircleShape).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun BookFacilityIcon(isActive: Boolean) {
    IconStage(127.5.dp) {
        MarinaArt(R.drawable.marina_icon_book_facility, Modifier.fillMaxSize().softArtEdges(),
            intArrayOf(0, 0, 270, 180))
        val bounce = if (isActive) { val b by rememberVerticalBounce(); b } else 0f
        Box(Modifier.offset(x = 45.dp, y = 47.dp).size(35.dp, 35.dp).clip(CircleShape).background(Color(0xFFF5F7F1)))
        MarinaArt(R.drawable.marina_icon_book_facility,
            Modifier.offset(x = 45.dp, y = 47.dp).size(35.dp, 35.dp).clip(CircleShape)
                .graphicsLayer { translationY = bounce.dp.toPx() },
            intArrayOf(96, 99, 74, 73))
    }
}

@Composable
private fun InviteVisitorsIcon(isActive: Boolean) {
    IconStage(115.9.dp) {
        MarinaArt(R.drawable.marina_icon_invite_visitors, Modifier.fillMaxSize().softArtEdges(),
            intArrayOf(0, 14, 240, 176))
        val pop = if (isActive) { val p by rememberPopScale(); p } else 1f
        Box(Modifier.offset(x = 16.9.dp, y = 32.8.dp).size(33.3.dp, 37.7.dp).clip(CircleShape).background(Color(0xFFE7DECF)))
        MarinaArt(R.drawable.marina_icon_invite_visitors,
            Modifier.offset(x = 16.9.dp, y = 32.8.dp).size(33.3.dp, 37.7.dp).clip(CircleShape)
                .graphicsLayer {
                    scaleX = pop; scaleY = pop
                    transformOrigin = TransformOrigin(0.5f, 0.85f)
                },
            intArrayOf(35, 82, 69, 78))
    }
}

@Composable
private fun EFormsIcon(isActive: Boolean) {
    IconStage(138.9.dp) {
        MarinaArt(R.drawable.marina_icon_eforms, Modifier.fillMaxSize().softArtEdges(),
            intArrayOf(0, 22, 250, 153))
        val glide = if (isActive) { val g by rememberHorizontalGlide(); g } else 0f
        Box(Modifier.offset(x = 93.3.dp, y = 33.9.dp).size(18.9.dp, 11.7.dp).clip(CircleShape).background(Color(0xFFFAF7F0)))
        MarinaArt(R.drawable.marina_icon_eforms,
            Modifier.offset(x = 93.3.dp, y = 33.9.dp).size(18.9.dp, 11.7.dp).clip(CircleShape)
                .graphicsLayer { translationX = glide.dp.toPx() },
            intArrayOf(168, 83, 34, 21))
    }
}

private val FeedbackDotXs = floatArrayOf(32.4f, 48.1f, 63.2f)
private val FeedbackDotCrops = listOf(intArrayOf(58, 83, 22, 22), intArrayOf(86, 83, 22, 22), intArrayOf(113, 83, 22, 22))

@Composable
private fun FeedbackIcon(isActive: Boolean) {
    IconStage(148.2.dp) {
        MarinaArt(R.drawable.marina_icon_feedback, Modifier.fillMaxSize().softArtEdges(),
            intArrayOf(0, 23, 265, 152))
        FeedbackDotXs.forEach { x ->
            Box(Modifier.offset(x = x.dp, y = 33.6.dp).size(12.3.dp, 12.3.dp).clip(CircleShape).background(Color(0xFFA3B99C)))
        }
        FeedbackDotXs.forEachIndexed { i, x ->
            val bounce = if (isActive) { val b by rememberTypingBounce(i * 160); b } else 0f
            MarinaArt(R.drawable.marina_icon_feedback,
                Modifier.offset(x = x.dp, y = 33.6.dp).size(12.3.dp, 12.3.dp).clip(CircleShape)
                    .graphicsLayer { translationY = bounce.dp.toPx() },
                FeedbackDotCrops[i])
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, modifier: Modifier,
    color: Color = Color(0xFFFFFFFF), onClick: () -> Unit, icon: @Composable () -> Unit) {
    val view = LocalView.current
    val tap = {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onClick()
    }
    MarinaCard(modifier, if (color == Color.White) MaterialTheme.colorScheme.surface else color, tap) {
        Column(Modifier.padding(horizontal = 13.dp).padding(top = 6.dp, bottom = 9.dp)) {
            icon()
            Text(title, fontFamily = DMSans, fontSize = 16.sp, lineHeight = 21.sp,
                fontWeight = FontWeight.Medium, color = MarinaInk)
            Row(verticalAlignment = Alignment.CenterVertically) {
            Text(subtitle, fontFamily = DMSans, fontSize = 11.sp, lineHeight = 14.sp,
                color = MarinaMuted, modifier = Modifier.weight(1f).padding(top = 2.dp))
            if (AppPreferences.theme == AppTheme.ZERO) ArrowBubble()
            }
        }
    }
}

@Composable
private fun ArrowBubble() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp, modifier = Modifier.size(29.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(19.dp), tint = MarinaInk)
        }
    }
}

@Composable
private fun AnnouncementsBanner(onClick: () -> Unit) {
    MarinaCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            MarinaArt(R.drawable.marina_icon_announcements, Modifier.size(86.dp, 52.dp).softArtEdges(),
                crop = intArrayOf(0, 26, 185, 102))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("All Announcements", fontFamily = DMSans, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = MarinaInk)
                Text("Stay updated with the\nlatest community news", fontFamily = DMSans,
                    fontSize = 11.sp, lineHeight = 14.sp, color = MarinaMuted)
            }
            ArrowBubble()
        }
    }
}

@Composable
private fun TodayAtAGlance(navController: NavController) {
    MarinaCard(Modifier.fillMaxWidth(), color = MarinaCream) {
        Column(Modifier.padding(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today at a Glance", fontFamily = DMSans, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                    color = MarinaInk, modifier = Modifier.weight(1f))
                Row(Modifier.clickable(role = Role.Button) { navController.navigate(Screen.Bookings.route) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("View More", fontFamily = DMSans, fontSize = 10.sp, lineHeight = 14.sp, color = MarinaMuted)
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = MarinaInk)
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlanceItem("2", "Bookings\nConfirmed", R.drawable.marina_icon_glance_booking,
                    intArrayOf(5, 31, 85, 74), Modifier.weight(1f)) { navController.navigate(Screen.Bookings.route) }
                Box(Modifier.width(1.dp).height(30.dp).background(Color(0xFFE9EDE8)))
                GlanceItem("1", "Visitor\nExpected", R.drawable.marina_icon_glance_visitor,
                    intArrayOf(7, 31, 92, 74), Modifier.weight(1f)) { navController.navigate(Screen.Visitors.route) }
                Box(Modifier.width(1.dp).height(30.dp).background(Color(0xFFE9EDE8)))
                GlanceItem("3", "New\nAnnouncements", R.drawable.marina_icon_glance_announcement,
                    intArrayOf(5, 31, 88, 79), Modifier.weight(1f)) { navController.navigate(Screen.Announcements.route) }
            }
        }
    }
}

@Composable
private fun GlanceItem(count: String, label: String, @DrawableRes art: Int, crop: IntArray,
    modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clickable(role = Role.Button, onClick = onClick).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        MarinaArt(art, Modifier.size(39.dp).clip(CircleShape), crop, scale = ContentScale.Crop)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(count, fontFamily = DMSans, fontSize = 17.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, color = MarinaInk)
            Text(label, fontFamily = DMSans, fontSize = 9.sp, lineHeight = 11.sp, color = MarinaMuted)
        }
    }
}

internal val BottomBarHeight = 78.dp

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    if (AppPreferences.theme == AppTheme.ZERO) {
        ThemeZeroBottomNavigation(navController, currentRoute)
        return
    }
    fun navigate(route: String) {
        if (route != currentRoute) navController.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Box(Modifier.fillMaxWidth().then(if (AppPreferences.theme == AppTheme.ONE) Modifier.background(MarinaBackground) else Modifier).padding(horizontal = if (AppPreferences.theme == AppTheme.ZERO) 12.dp else 0.dp).navigationBarsPadding().height(BottomBarHeight)) {
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(66.dp), color = MarinaCream,
            shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, Color.White), shadowElevation = 9.dp) {
            Row(Modifier.fillMaxSize().padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                NavItem(Icons.Rounded.Home, "Home", currentRoute == Screen.Home.route, Modifier.weight(1f)) { navigate(Screen.Home.route) }
                NavItem(Icons.Outlined.Groups, "Community", currentRoute == Screen.Community.route, Modifier.weight(1f)) { navigate(Screen.Community.route) }
                Spacer(Modifier.weight(1f))
                NavItem(Icons.Outlined.Lock, "Access", currentRoute == Screen.Access.route, Modifier.weight(1f)) { navigate(Screen.Access.route) }
                NavItem(if (AppPreferences.theme == AppTheme.ZERO) Icons.Outlined.GridView else Icons.Outlined.PersonOutline, if (AppPreferences.theme == AppTheme.ZERO) "More" else "Profile", currentRoute == Screen.Profile.route, Modifier.weight(1f)) { navigate(Screen.Profile.route) }
            }
        }
        Surface(onClick = { navigate(Screen.Home.route) }, modifier = Modifier.align(Alignment.TopCenter).size(65.dp),
            shape = CircleShape, color = Color(0xFFF2ECDD), border = BorderStroke(3.dp, Color.White), shadowElevation = 6.dp) {
            if (AppPreferences.theme == AppTheme.ZERO) Image(androidx.compose.ui.res.painterResource(R.drawable.greengate_logo), "Green Gate home", Modifier.padding(7.dp))
            else MarinaArt(R.drawable.marina_icon_bottom_center_leaf, Modifier.padding(4.dp).clip(CircleShape),
                crop = intArrayOf(29, 25, 109, 109), description = "Marina Bay home", scale = ContentScale.Crop)
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.selectable(selected, role = Role.Tab, onClick = onClick).padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(25.dp), tint = if (selected) MarinaGreen else MarinaMuted)
        Text(label, fontFamily = DMSans, fontSize = 10.sp, lineHeight = 14.sp, color = if (selected) MarinaInk else MarinaMuted)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(4.dp).background(if (selected) MarinaGreen else Color.Transparent, CircleShape))
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 860)
@Composable
private fun MarinaHomePreview() {
    GreenGateTheme {
        val navController = rememberNavController()
        Scaffold(bottomBar = { BottomNavigationBar(navController, Screen.Home.route) }) { padding ->
            Box(Modifier.padding(padding)) { HomeScreen(navController) }
        }
    }
}
