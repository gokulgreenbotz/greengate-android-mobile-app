package com.example.greengate

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.OutdoorGrill
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans
import com.example.greengate.ui.theme.PrimaryGreen
import com.example.greengate.ui.theme.SoftGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal val FacilityInk = Color(0xFF14232B)
internal val FacilityMuted = Color(0xFF5B6B72)
internal val FacilityGreen get() = themed(Color(0xFF1E8A6A), PrimaryGreen)
internal val FacilityOrange = Color(0xFFE8772E)
internal val FacilityGlassBorder get() = themed(Color(0xB3FFFFFF), ClassicLine)
private val GlassFacilityBackdrop = Brush.verticalGradient(
    0f to Color(0xFF5E9FB4), 0.18f to Color(0xFF9DCBD3), 0.45f to Color(0xFFD5EDEA), 1f to Color(0xFFEAF6F2)
)
private val FacilityBackdrop get() = themed(GlassFacilityBackdrop, ClassicBackdrop)

internal enum class FacilityCategory(val label: String) { ALL("All"), POOL("Pool"), GYM("Gym"), BBQ("BBQ"), TENNIS("Tennis") }

internal data class Facility(
    // Stable key used in navigation routes.
    val id: String,
    val name: String,
    val category: FacilityCategory,
    @DrawableRes val photo: Int,
    val icon: ImageVector,
    // 3D artwork; the flat [icon] is used only with Icon Set 3.
    @DrawableRes val artwork: Int,
    val maxPax: Int,
    // Refundable deposit in whole $; 0 means none.
    val deposit: Int,
    val nextAvailable: String,
    // null means plenty of availability; a number shows the "N slots left" warning state.
    val slotsLeft: Int? = null,
)

// Static until a facilities API exists; high-resolution images follow the design's scenes.
internal val Facilities = listOf(
    Facility("pool", "50m Lap Pool", FacilityCategory.POOL, R.drawable.facility_pool_hd, Icons.Rounded.Pool, R.drawable.ic_facility_pool, 20, 0, "10:00 AM today"),
    Facility("gym", "Gym (Level 3)", FacilityCategory.GYM, R.drawable.facility_gym_hd, Icons.Rounded.FitnessCenter, R.drawable.ic_facility_gym, 15, 5, "4:00 PM today", slotsLeft = 2),
    Facility("bbq", "BBQ Area", FacilityCategory.BBQ, R.drawable.facility_bbq_hd, Icons.Rounded.OutdoorGrill, R.drawable.ic_facility_bbq, 25, 10, "11:00 AM today"),
    Facility("tennis", "Tennis Court", FacilityCategory.TENNIS, R.drawable.facility_tennis_hd, Icons.Rounded.SportsTennis, R.drawable.ic_facility_tennis, 4, 0, "9:00 AM today"),
)

/**
 * The facility artwork is multi-megabyte PNGs. Decoding it with painterResource happens on the
 * main thread in the screen's first frame and stalls the slide-in, so it is decoded here in the
 * background, scaled to what the screen can show, and kept for reuse.
 */
internal object FacilityImages {
    private val cache = mutableStateMapOf<Int, ImageBitmap>()
    private val loading = Mutex()

    operator fun get(@DrawableRes id: Int): ImageBitmap? = cache[id]

    /** Safe to call repeatedly; images already decoded are skipped. */
    suspend fun preload(context: Context) = loading.withLock {
        val res = context.resources
        val artworkWidth = (76 * res.displayMetrics.density).toInt()
        // Card photos span the screen, capped at the 600 dp the list can grow to.
        val photoWidth = minOf(res.displayMetrics.widthPixels, (600 * res.displayMetrics.density).toInt())
        // Small artwork first, so the icons appear before the larger photos finish.
        (Facilities.map { it.artwork to artworkWidth } + Facilities.map { it.photo to photoWidth })
            .filter { (id, _) -> id !in cache }
            .forEach { (id, width) -> cache[id] = withContext(Dispatchers.Default) { decode(res, id, width) } }
    }

    private fun decode(res: Resources, @DrawableRes id: Int, maxWidth: Int): ImageBitmap {
        val full = BitmapFactory.decodeResource(res, id)
        val bitmap = if (full.width <= maxWidth) full
            else Bitmap.createScaledBitmap(full, maxWidth, full.height * maxWidth / full.width, true).also { full.recycle() }
        // Uploads the texture now rather than on the first frame that draws it.
        bitmap.prepareToDraw()
        return bitmap.asImageBitmap()
    }
}

@Composable
private fun FacilityImage(@DrawableRes id: Int, modifier: Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val bitmap = FacilityImages[id]
    if (bitmap != null) Image(bitmap, null, modifier, contentScale = contentScale)
    else Box(modifier)
}

private const val PanelFadeMillis = 150
private const val PanelShrinkMillis = 220
private const val PanelScrollMillis = 250

/**
 * Scrolls just far enough for the item with [key] to end above the bottom padding, but never so far
 * that its top (the facility card) goes under the top padding; a panel taller than the screen stops
 * with its card at the top.
 */
private suspend fun LazyListState.reveal(key: Any) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.find { it.key == key } ?: return
    // Item offsets start after the top padding; viewportEndOffset reaches into the bottom padding.
    val bottomLimit = info.viewportEndOffset - info.afterContentPadding
    val distance = minOf(item.offset + item.size - bottomLimit, item.offset)
    if (distance > 0) animateScrollBy(distance.toFloat(), tween(PanelScrollMillis, easing = FastOutSlowInEasing))
}

@Composable
fun BookFacilityScreen(navController: NavController) {
    val context = LocalContext.current
    // Normally done already from Home; this covers opening the screen before that finished.
    LaunchedEffect(Unit) { FacilityImages.preload(context) }
    val activity = context as? Activity
    // The glass sky is dark at the top, so it takes light status bar icons; Classic's white keeps dark ones.
    val darkBackdrop = !ClassicTheme
    DisposableEffect(activity, darkBackdrop) {
        val bars = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        bars?.isAppearanceLightStatusBars = !darkBackdrop
        onDispose { bars?.isAppearanceLightStatusBars = true }
    }
    var category by remember { mutableStateOf(FacilityCategory.ALL) }
    // Only one facility's date and slot picker is open at a time.
    var expandedName by remember {
        // "Book Now" on a facility's About page comes back here with that facility to open.
        val fromAbout = navController.currentBackStackEntry?.savedStateHandle?.remove<String>(ExpandFacilityKey)
        mutableStateOf(Facilities.find { it.id == fromAbout }?.name)
    }
    // The facility just opened, whose picker scrolls into view once it has finished expanding.
    // Cleared after that one scroll, so scrolling the open picker back on screen later doesn't jump.
    var scrollToName by remember { mutableStateOf(expandedName) }
    val listState = rememberLazyListState()
    val visible =Facilities.filter { category == FacilityCategory.ALL || it.category == category }

    Box(Modifier.fillMaxSize().background(FacilityBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            FacilityHeader(
                onBack = { navController.popBackStack() },
                onCalendar = { navController.navigate(Screen.Bookings.route) }
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(FacilityCategory.entries) { c ->
                    CategoryChip(c.label, selected = c == category) { category = c }
                }
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visible, key = { it.name }) { facility ->
                    val expanded = facility.name == expandedName
                    Column {
                        FacilityCard(facility, expanded, onAbout = { navController.navigate(Screen.FacilityAbout.create(facility.id)) }) {
                            expandedName = if (expanded) null else facility.name
                            scrollToName = expandedName
                        }
                        AnimatedVisibility(
                            visible = expanded,
                            // The panel takes its full height at once and only fades, so the list can
                            // scroll to it straight away instead of waiting for it to unroll.
                            enter = fadeIn(tween(PanelFadeMillis)),
                            // Switching to another facility drops this panel instantly: shrinking it would
                            // shift the list under that facility's scroll and overshoot it.
                            exit = if (expandedName != null) ExitTransition.None
                                else shrinkVertically(tween(PanelShrinkMillis), shrinkTowards = Alignment.Top) + fadeOut(tween(PanelFadeMillis))
                        ) {
                            LaunchedEffect(Unit) {
                                if (scrollToName != facility.name) return@LaunchedEffect
                                scrollToName = null
                                // One frame, so the list has measured the panel before working out the scroll.
                                withFrameNanos { }
                                listState.reveal(facility.name)
                            }
                            FacilityBookingPanel(
                                facilityName = facility.name,
                                busy = facility.slotsLeft != null,
                                maxGuests = facility.maxPax,
                                glyph = { FacilityGlyph(facility.icon, facility.artwork, 46.dp) },
                                modifier = Modifier.padding(top = 10.dp),
                                onBooked = { day, hour, guests ->
                                    navController.navigate(Screen.BookingPayment.create(facility.id, day, hour, guests))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacilityHeader(onBack: () -> Unit, onCalendar: () -> Unit) {
    // A row rather than a centred box, so the title makes room as the History pill grows on small phones.
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier, onBack)
        Text(
            "Book Facility", Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.Center, maxLines = 1,
            fontFamily = DMSans, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = themed(Color.White, FacilityInk)
        )
        // Sized and shadowed to match Back on this screen.
        FoldingLabelButton(Icons.Rounded.CalendarMonth, "History", size = 52.dp, iconSize = 24.dp,
            ambientShadow = Color(0x33000000), spotShadow = Color.Black, onClick = onCalendar)
    }
}

@Composable
private fun GlassCircleButton(icon: ImageVector, description: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(52.dp).shadow(6.dp, CircleShape, ambientColor = Color(0x33000000))
            .clip(CircleShape).background(Color(0xF2FFFFFF))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = FacilityInk, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.height(44.dp).clip(shape)
            .background(if (selected) themed(Color(0xFFBDF0DC), SoftGreen) else themed(Color(0xCCFFFFFF), ClassicCream))
            .border(1.5.dp, if (selected) themed(Color(0xFF8FE3C4), PrimaryGreen) else FacilityGlassBorder, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontFamily = DMSans, fontSize = 16.sp, color = FacilityInk,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun FacilityCard(facility: Facility, expanded: Boolean, onAbout: () -> Unit, onClick: () -> Unit) {
    // The arrow turns to point at the open picker below.
    val arrowTurn by animateFloatAsState(if (expanded) 90f else 0f, label = "arrowTurn")
    Surface(
        shape = RoundedCornerShape(26.dp), color = themed(Color(0xF2FFFFFF), ClassicCream), border = BorderStroke(1.5.dp, FacilityGlassBorder),
        shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(Modifier.padding(6.dp)) {
            // A wide photo of the place leads the card, with About and availability over its top corners.
            Box(Modifier.fillMaxWidth().aspectRatio(2.2f).clip(RoundedCornerShape(22.dp)).background(themed(Color(0xFFD5EDEA), ClassicMint))) {
                FacilityImage(facility.photo, Modifier.fillMaxSize(), ContentScale.Crop)
                AboutChip(facility.name, Modifier.align(Alignment.TopStart).padding(12.dp), onAbout)
                AvailabilityBadge(facility.slotsLeft, Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                FacilityIconTile(facility.icon, facility.artwork)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(facility.name, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FacilityInk, maxLines = 1)
                    Text(
                        "Max ${facility.maxPax} pax  •  ${if (facility.deposit > 0) "$${facility.deposit} deposit" else "No deposit"}",
                        fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = themed(Color(0xFFE3ECEA), ClassicLine))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = FacilityMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            buildAnnotatedString {
                                append("Next available: ")
                                withStyle(SpanStyle(
                                    color = if (facility.slotsLeft != null) FacilityOrange else FacilityGreen,
                                    fontWeight = FontWeight.SemiBold
                                )) { append(facility.nextAvailable) }
                            },
                            fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(50.dp).shadow(4.dp, CircleShape, ambientColor = Color(0x2216A37F), spotColor = Color(0x3316A37F))
                        .clip(CircleShape).background(themed(Color(0xFFDDF6EE), ClassicMint)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Book ${facility.name}", tint = FacilityInk,
                        modifier = Modifier.size(22.dp).rotate(arrowTurn))
                }
            }
        }
    }
}

@Composable
private fun AvailabilityBadge(slotsLeft: Int?, modifier: Modifier) {
    val (dot, label) = if (slotsLeft == null) FacilityGreen to "Available"
    else FacilityOrange to "$slotsLeft slot${if (slotsLeft == 1) "" else "s"} left"
    Row(
        modifier.shadow(4.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
            .background(if (slotsLeft == null) themed(Color(0xF2E9FBF3), ClassicMint) else Color(0xF2FFF2E8))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(label, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
    }
}

@Composable
private fun FacilityIconTile(icon: ImageVector, @DrawableRes artwork: Int) {
    Box(
        Modifier.size(76.dp).shadow(3.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color.White, themed(Color(0xFFE3F5EE), ClassicMint)))),
        contentAlignment = Alignment.Center
    ) {
        FacilityGlyph(icon, artwork, 60.dp)
    }
}

/** The facility's 3D artwork at [size], or its flat icon (a little smaller) with Icon Set 3. */
@Composable
private fun FacilityGlyph(icon: ImageVector, @DrawableRes artwork: Int, size: Dp) {
    // Same rule as Profile's selection: the icon set applies unless Theme 0's design artwork is on.
    val flat = AppPreferences.iconSet == IconSet.THREE &&
        (AppPreferences.theme != AppTheme.ZERO || !AppPreferences.useReferenceArtwork)
    if (flat) Icon(icon, null, tint = themed(Color(0xFF2BA383), PrimaryGreen), modifier = Modifier.size(size * .7f))
    else FacilityImage(artwork, Modifier.size(size))
}

/** Opens the facility's About page: photos, site map, hours and terms. */
@Composable
private fun AboutChip(facilityName: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(34.dp).clip(RoundedCornerShape(50)).background(Color(0xE6FFFFFF))
            .clickable(role = Role.Button, onClickLabel = "About $facilityName", onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, null, tint = FacilityInk, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("About", fontFamily = DMSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
    }
}
