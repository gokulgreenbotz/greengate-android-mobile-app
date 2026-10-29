package com.greenbotz.greengate.ui.screens.facilities

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.OutdoorGrill
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.AppTheme
import com.greenbotz.greengate.core.session.IconSet
import com.greenbotz.greengate.ui.navigation.Screen
import com.greenbotz.greengate.ui.screens.home.SilkLines
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.NavyGoldButton
import com.greenbotz.greengate.ui.theme.NavyGoldCream
import com.greenbotz.greengate.ui.theme.NavyGoldGold
import com.greenbotz.greengate.ui.theme.NavyGoldInk
import com.greenbotz.greengate.ui.theme.NavyGoldLine
import com.greenbotz.greengate.ui.theme.NavyGoldMuted
import com.greenbotz.greengate.ui.theme.NavyGoldNavy
import com.greenbotz.greengate.ui.theme.NavyGoldPage
import com.greenbotz.greengate.ui.theme.NavyGoldShadow
import com.greenbotz.greengate.ui.theme.NavyGoldTheme
import com.greenbotz.greengate.ui.theme.PrimaryGreen
import com.greenbotz.greengate.ui.theme.SoftGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Theme 4 - Navy & Gold turns the ink navy and the glass edges warm (see NavyGold.kt). Its greens stay
// for availability and status, so each screen picks navy or gold for its own accents.
internal val FacilityInk get() = if (NavyGoldTheme) NavyGoldInk else Color(0xFF14232B)
internal val FacilityMuted get() = if (NavyGoldTheme) NavyGoldMuted else Color(0xFF5B6B72)
internal val FacilityGreen get() = themed(Color(0xFF1E8A6A), PrimaryGreen)
internal val FacilityOrange = Color(0xFFE8772E)
internal val FacilityGlassBorder get() = if (NavyGoldTheme) NavyGoldLine else themed(Color(0xB3FFFFFF), ClassicLine)
private val GlassFacilityBackdrop = Brush.verticalGradient(
    0f to Color(0xFF5E9FB4), 0.18f to Color(0xFF9DCBD3), 0.45f to Color(0xFFD5EDEA), 1f to Color(0xFFEAF6F2)
)
private val FacilityBackdrop: Brush get() = if (NavyGoldTheme) SolidColor(NavyGoldPage) else themed(GlassFacilityBackdrop, ClassicBackdrop)

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
    // The same artwork in Theme 4's navy and gold.
    @DrawableRes val navyGoldArtwork: Int,
    val maxPax: Int,
    // Refundable deposit in whole $; 0 means none.
    val deposit: Int,
    val nextAvailable: String,
    // null means plenty of availability; a number shows the "N slots left" warning state.
    val slotsLeft: Int? = null,
) {
    /** The artwork for the current theme. */
    @get:DrawableRes
    val themedArtwork: Int get() = if (NavyGoldTheme) navyGoldArtwork else artwork
}

// Static until a facilities API exists; high-resolution images follow the design's scenes.
internal val Facilities = listOf(
    Facility("pool", "50m Lap Pool", FacilityCategory.POOL, R.drawable.facility_pool_hd, Icons.Rounded.Pool,
        R.drawable.ic_facility_pool, R.drawable.theme4_facility_pool, 20, 0, "10:00 AM today"),
    Facility("gym", "Gym (Level 3)", FacilityCategory.GYM, R.drawable.facility_gym_hd, Icons.Rounded.FitnessCenter,
        R.drawable.ic_facility_gym, R.drawable.theme4_facility_gym, 15, 5, "4:00 PM today", slotsLeft = 2),
    Facility("bbq", "BBQ Area", FacilityCategory.BBQ, R.drawable.facility_bbq_hd, Icons.Rounded.OutdoorGrill,
        R.drawable.ic_facility_bbq, R.drawable.theme4_facility_bbq, 25, 10, "11:00 AM today"),
    Facility("tennis", "Tennis Court", FacilityCategory.TENNIS, R.drawable.facility_tennis_hd, Icons.Rounded.SportsTennis,
        R.drawable.ic_facility_tennis, R.drawable.theme4_facility_tennis, 4, 0, "9:00 AM today"),
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
        // Small artwork first (the current theme's set, then the other), so the icons appear before the larger photos finish.
        val (shown, other) = Facilities.map { it.themedArtwork } to Facilities.map { if (NavyGoldTheme) it.artwork else it.navyGoldArtwork }
        ((shown + other).map { it to artworkWidth } + Facilities.map { it.photo to photoWidth })
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
internal fun FacilityImage(@DrawableRes id: Int, modifier: Modifier, contentScale: ContentScale = ContentScale.Fit) {
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
    // The glass sky is dark at the top, so it takes light status bar icons; Classic's white and Theme 4's warm white keep dark ones.
    val darkBackdrop = !ClassicTheme && !NavyGoldTheme
    DisposableEffect(activity, darkBackdrop) {
        val bars = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        bars?.isAppearanceLightStatusBars = !darkBackdrop
        onDispose { bars?.isAppearanceLightStatusBars = true }
    }
    // The facility whose Full booking view is open; null shows the card list, where Quick booking drops down under a card.
    var fullFacilityId by rememberSaveable { mutableStateOf<String?>(null) }
    // Back from the Full view returns to the list rather than leaving the screen.
    BackHandler(enabled = fullFacilityId != null) { fullFacilityId = null }
    var category by remember { mutableStateOf(FacilityCategory.ALL) }
    // "Book Now" on a facility's About page comes back here with that facility to open.
    val fromAbout = remember { navController.currentBackStackEntry?.savedStateHandle?.remove<String>(ExpandFacilityKey) }
    // Only one facility's date and slot picker is open at a time.
    var expandedName by remember { mutableStateOf(Facilities.find { it.id == fromAbout }?.name) }
    // The facility just opened, whose picker scrolls into view once it has finished expanding.
    // Cleared after that one scroll, so scrolling the open picker back on screen later doesn't jump.
    var scrollToName by remember { mutableStateOf(expandedName) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Hearted facilities are pinned to the top, the rest keep their usual order.
    val favourites = AppPreferences.favouriteFacilities
    val visible = Facilities.filter { category == FacilityCategory.ALL || it.category == category }
        .sortedByDescending { it.id in favourites }

    Box(Modifier.fillMaxSize().background(FacilityBackdrop), contentAlignment = Alignment.TopCenter) {
        if (NavyGoldTheme) SilkLines(Modifier.fillMaxWidth().height(260.dp))
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            FacilityHeader(
                onBack = { if (fullFacilityId != null) fullFacilityId = null else navController.popBackStack() },
                onCalendar = { navController.navigate(Screen.Bookings.route) }
            )
            AnimatedContent(
                targetState = fullFacilityId,
                // Picking another facility inside the Full view isn't a change of view.
                contentKey = { it != null },
                // The Full view pushes in from the side and Back slides it out again, like the booking pages.
                // Slides only, no fades (see NavTransitions), and the two never overlap, so the backdrop stays clean.
                transitionSpec = {
                    val forward = targetState != null
                    val spec = tween<IntOffset>(ViewSlideMillis, easing = FastOutSlowInEasing)
                    slideInHorizontally(spec) { if (forward) it else -it } togetherWith
                        slideOutHorizontally(spec) { if (forward) -it else it }
                },
                modifier = Modifier.weight(1f),
                label = "bookingView"
            ) { fullId ->
                if (fullId != null) {
                    // No category filters here: the facility strip in the view already shows every facility.
                    FullBookingView(
                        initialFacilityId = fullId,
                        onAbout = { navController.navigate(Screen.FacilityAbout.create(it.id)) },
                        onCheck = { facility, day, hour, guests, hours ->
                            navController.navigate(Screen.BookingPayment.create(facility.id, day, hour, guests, hours))
                        },
                        modifier = Modifier.fillMaxSize().padding(top = 6.dp)
                    )
                } else Column(Modifier.fillMaxSize()) {
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
                            val favourite = facility.id in favourites
                            // animateItem slides a newly pinned card up to the top.
                            Column(Modifier.animateItem()) {
                                FacilityCard(
                                    facility, expanded, favourite,
                                    onFavourite = {
                                        AppPreferences.toggleFavouriteFacility(context, facility.id)
                                        // Pinned above the cards on screen; scroll up so it's seen landing.
                                        if (!favourite) scope.launch { listState.animateScrollToItem(0) }
                                    },
                                    onAbout = { navController.navigate(Screen.FacilityAbout.create(facility.id)) },
                                    onFullBooking = { fullFacilityId = facility.id }
                                ) {
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
                                        glyph = { FacilityGlyph(facility.icon, facility.themedArtwork, 46.dp) },
                                        modifier = Modifier.padding(top = 10.dp),
                                        // Quick bookings are always one hour; Full view offers longer ones.
                                        onBooked = { day, hour, guests ->
                                            navController.navigate(Screen.BookingPayment.create(facility.id, day, hour, guests, 1))
                                        }
                                    )
                                }
                            }
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
        Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Book Facility", textAlign = TextAlign.Center, maxLines = 1,
                fontFamily = DMSans, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = if (NavyGoldTheme) FacilityInk else themed(Color.White, FacilityInk)
            )
            Text(
                "Reserve a space for your lifestyle", textAlign = TextAlign.Center, maxLines = 1,
                fontFamily = DMSans, fontSize = 14.sp,
                color = if (NavyGoldTheme) FacilityMuted else themed(Color(0xE6FFFFFF), FacilityMuted)
            )
        }
        // Sized and shadowed to match Back on this screen.
        FoldingLabelButton(Icons.Rounded.CalendarMonth, "History", size = 52.dp, iconSize = 24.dp,
            ambientShadow = if (NavyGoldTheme) NavyGoldShadow else Color(0x33000000),
            spotShadow = if (NavyGoldTheme) NavyGoldShadow else Color.Black, onClick = onCalendar)
    }
}

@Composable
private fun GlassCircleButton(icon: ImageVector, description: String, modifier: Modifier, onClick: () -> Unit) {
    val shadow = if (NavyGoldTheme) NavyGoldShadow else Color(0x33000000)
    Box(
        modifier.size(52.dp).shadow(6.dp, CircleShape, ambientColor = shadow, spotColor = if (NavyGoldTheme) shadow else Color.Black)
            .clip(CircleShape).background(if (NavyGoldTheme) Color.White else Color(0xF2FFFFFF))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = FacilityInk, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    // Theme 4: the selected chip is navy with a gold edge and white type; the rest are white with a warm edge.
    val navyGold = NavyGoldTheme
    Box(
        Modifier.height(44.dp)
            .then(if (navyGold) Modifier.shadow(3.dp, shape, ambientColor = NavyGoldShadow, spotColor = NavyGoldShadow) else Modifier)
            .clip(shape)
            .background(when {
                navyGold && selected -> NavyGoldButton
                navyGold -> SolidColor(Color.White)
                else -> SolidColor(if (selected) themed(Color(0xFFBDF0DC), SoftGreen) else themed(Color(0xCCFFFFFF), ClassicCream))
            })
            .border(1.5.dp, when {
                navyGold -> if (selected) Color(0xFFD9B36A) else Color(0xFFEADFCB)
                selected -> themed(Color(0xFF8FE3C4), PrimaryGreen)
                else -> FacilityGlassBorder
            }, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontFamily = DMSans, fontSize = 16.sp, color = if (navyGold && selected) Color.White else FacilityInk,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun FacilityCard(
    facility: Facility, expanded: Boolean, favourite: Boolean, onFavourite: () -> Unit, onAbout: () -> Unit,
    onFullBooking: () -> Unit, onClick: () -> Unit
) {
    val navyGold = NavyGoldTheme
    Surface(
        shape = RoundedCornerShape(26.dp), color = if (navyGold) Color.White else themed(Color(0xF2FFFFFF), ClassicCream),
        border = BorderStroke(if (navyGold) 1.dp else 1.5.dp, FacilityGlassBorder),
        shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(Modifier.padding(6.dp)) {
            // A wide photo of the place leads the card, with About and availability over its top corners.
            Box(Modifier.fillMaxWidth().aspectRatio(2.2f).clip(RoundedCornerShape(22.dp))
                .background(if (navyGold) NavyGoldCream else themed(Color(0xFFD5EDEA), ClassicMint))) {
                FacilityImage(facility.photo, Modifier.fillMaxSize(), ContentScale.Crop)
                Row(Modifier.align(Alignment.TopStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AboutChip(facility.name, Modifier, onAbout)
                    Spacer(Modifier.width(8.dp))
                    FavouriteButton(facility.name, favourite, onFavourite)
                }
                AvailabilityBadge(facility.slotsLeft, Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                FacilityIconTile(facility.icon, facility.themedArtwork)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(facility.name, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FacilityInk, maxLines = 1)
                    Text(
                        "Max ${facility.maxPax} pax  •  ${if (facility.deposit > 0) "$${facility.deposit} deposit" else "No deposit"}",
                        fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = if (navyGold) NavyGoldLine else themed(Color(0xFFE3ECEA), ClassicLine))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Theme 4: a gold clock, and the open time in navy.
                        Icon(Icons.Outlined.Schedule, null, tint = if (navyGold) NavyGoldGold else FacilityMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            buildAnnotatedString {
                                append("Next available: ")
                                withStyle(SpanStyle(
                                    color = when {
                                        facility.slotsLeft != null -> FacilityOrange
                                        navyGold -> FacilityInk
                                        else -> FacilityGreen
                                    },
                                    fontWeight = if (navyGold) FontWeight.Bold else FontWeight.SemiBold
                                )) { append(facility.nextAvailable) }
                            },
                            fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Quick booking drops the date and slot picker under this card (filled while it's open);
                // the calendar opens the Full booking view for this facility.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardIconButton(Icons.Rounded.Bolt,
                        if (expanded) "Close quick booking for ${facility.name}" else "Quick booking for ${facility.name}",
                        active = expanded, onClick = onClick)
                    CardIconButton(Icons.Rounded.EventAvailable, "Full booking for ${facility.name}", active = false, onClick = onFullBooking)
                }
            }
        }
    }
}

@Composable
internal fun AvailabilityBadge(slotsLeft: Int?, modifier: Modifier) {
    val (dot, label) = if (slotsLeft == null) FacilityGreen to "Available"
    else FacilityOrange to "$slotsLeft slot${if (slotsLeft == 1) "" else "s"} left"
    Row(
        modifier.shadow(4.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
            // Theme 4 keeps every badge white, so only the dot carries the colour.
            .background(when {
                NavyGoldTheme -> Color(0xF7FFFFFF)
                slotsLeft == null -> themed(Color(0xF2E9FBF3), ClassicMint)
                else -> Color(0xF2FFF2E8)
            })
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
            .background(Brush.linearGradient(listOf(Color.White, if (NavyGoldTheme) NavyGoldCream else themed(Color(0xFFE3F5EE), ClassicMint)))),
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
    if (flat) Icon(icon, null, tint = if (NavyGoldTheme) NavyGoldNavy else themed(Color(0xFF2BA383), PrimaryGreen), modifier = Modifier.size(size * .7f))
    else FacilityImage(artwork, Modifier.size(size))
}

/** Hearts or un-hearts the facility; hearted ones are pinned to the top of the list. Same heart as on its About page. */
@Composable
private fun FavouriteButton(facilityName: String, favourite: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(Color(0xE6FFFFFF))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (favourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (favourite) "Unpin $facilityName from favourites" else "Pin $facilityName to favourites",
            tint = if (favourite) BookingsRed else FacilityInk, modifier = Modifier.size(18.dp)
        )
    }
}

/** Opens the facility's About page: photos, site map, hours and terms. */
@Composable
internal fun AboutChip(facilityName: String, modifier: Modifier, onClick: () -> Unit) {
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

private const val ViewSlideMillis = 380

/**
 * A round icon-only button on a facility card. Theme 4: cream and gold under a warm shadow, or navy
 * with a gold edge while [active]; elsewhere mint, or solid green while [active].
 */
@Composable
private fun CardIconButton(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    val navyGold = NavyGoldTheme
    val shadow = if (navyGold) Color(0x40B9852F) else Color(0x3316A37F)
    Box(
        Modifier.size(44.dp).shadow(4.dp, CircleShape, ambientColor = if (navyGold) shadow else Color(0x2216A37F), spotColor = shadow)
            .clip(CircleShape)
            .background(when {
                active && navyGold -> NavyGoldButton
                active -> SolidColor(FacilityGreen)
                navyGold -> MintButton
                else -> SolidColor(themed(Color(0xFFDDF6EE), ClassicMint))
            })
            .then(if (navyGold) Modifier.border(1.dp, if (active) Color(0xFFD9B36A) else Color(0xFFE9D3A6), CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = if (active) Color.White else FacilityInk, modifier = Modifier.size(22.dp))
    }
}
