package com.example.greengate

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

/** A labelled spot on the site map, as fractions of its width and height; the label sits at [labelX], [labelY]. */
internal class MapPin(val label: String, val x: Float, val y: Float, val labelX: Float, val labelY: Float)

internal sealed interface GalleryItem {
    class SiteMap(val title: String, val pins: List<MapPin>) : GalleryItem
    class Photo(@DrawableRes val image: Int) : GalleryItem
}

internal class FacilityInfo(
    val about: String,
    val gallery: List<GalleryItem>,
    val hours: List<Pair<String, String>>,
    val sessions: List<Pair<String, String>>,
    val bookingWindow: String,
    val terms: List<String>,
)

// The site map is 1120 x 1660.
private const val SiteMapAspect = 1120f / 1660f
private val PinPurple = Color(0xFFB45FD8)
private val PinRed = Color(0xFFE5262B)

// TODO: load from the facilities API; the hours and BBQ sessions follow the condo's published
// terms, the rest are sample house rules.
internal fun Facility.info(): FacilityInfo = when (id) {
    "bbq" -> FacilityInfo(
        about = "Three covered barbeque pits set in the garden beside the main pool, each with a stone counter and seating for your guests.",
        gallery = listOf(
            GalleryItem.SiteMap("BBQ Pits", listOf(
                MapPin("Pit 3", .718f, .112f, .42f, .060f),
                MapPin("Pit 2", .599f, .361f, .27f, .300f),
                MapPin("Pit 1", .603f, .399f, .27f, .430f),
            )),
            GalleryItem.Photo(R.drawable.facility_bbq_photo_pits),
            GalleryItem.Photo(R.drawable.facility_condo_pool_photo),
        ),
        hours = listOf(
            "Monday – Friday" to "10:00 AM – 10:00 PM",
            "Sat, Sun & Public Holidays" to "10:00 AM – 11:00 PM*",
        ),
        sessions = listOf(
            "Session I" to "10:00 AM – 2:00 PM",
            "Session II" to "3:00 PM – 6:00 PM",
            "Session III" to "7:00 PM – 10:00 PM (11:00 PM*)",
        ),
        bookingWindow = "9:00 AM – 5:00 PM daily",
        terms = listOf(
            "*Use until 11:00 PM needs Management's approval.",
            "Only residents may book; the resident must be present throughout the session.",
            "The S$$deposit deposit is refunded after the pit is inspected and found clean.",
            "Bring your own charcoal and utensils; clear all food and rubbish when done.",
            "Keep noise to a minimum after 10:00 PM out of respect for neighbours.",
            "Cancellations made less than 24 hours before the session forfeit the deposit.",
        ),
    )
    "tennis" -> FacilityInfo(
        about = "A full-size hard court beside Block 19, floodlit for evening play.",
        gallery = listOf(
            GalleryItem.SiteMap("Tennis Court", listOf(MapPin("Court 1", .866f, .442f, .80f, .36f))),
            GalleryItem.Photo(R.drawable.facility_tennis_photo_court),
            GalleryItem.Photo(photo),
        ),
        hours = listOf("Daily" to "7:00 AM – 10:00 PM"),
        sessions = emptyList(),
        bookingWindow = "Up to 7 days ahead, 1 hour per booking",
        terms = listOf(
            "Non-marking tennis shoes only.",
            "Up to $maxPax players per booking.",
            "Switch off the floodlights after evening sessions.",
            "Unclaimed bookings are released 15 minutes after the start time.",
        ),
    )
    "pool" -> FacilityInfo(
        about = "A 50 m lap pool with a shaded sun deck and loungers.",
        gallery = listOf(
            GalleryItem.SiteMap("Pool", listOf(MapPin("Lap Pool", .533f, .566f, .30f, .62f))),
            GalleryItem.Photo(R.drawable.facility_condo_pool_photo),
            GalleryItem.Photo(photo),
        ),
        hours = listOf("Daily" to "7:00 AM – 9:00 PM"),
        sessions = emptyList(),
        bookingWindow = "Up to 7 days ahead",
        terms = listOf(
            "No lifeguard on duty; children under 12 must be accompanied by an adult.",
            "Shower before entering the pool. No glass bottles on the deck.",
            "The pool closes during thunderstorms.",
        ),
    )
    else -> FacilityInfo(
        about = "A fully equipped gym with cardio machines, free weights and a stretching area.",
        gallery = listOf(GalleryItem.Photo(photo)),
        hours = listOf("Daily" to "6:00 AM – 11:00 PM"),
        sessions = emptyList(),
        bookingWindow = "Up to 3 days ahead, 1 hour per booking",
        terms = listOf(
            "Residents aged 16 and above only.",
            "Wipe down equipment after use and return weights to the rack.",
            "Proper workout attire and shoes must be worn.",
        ),
    )
}

/** Photos, site map, opening hours, sessions and terms for one facility. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FacilityAboutScreen(navController: NavController, facility: Facility) {
    val info = remember(facility.id) { facility.info() }
    val pager = rememberPagerState { info.gallery.size }
    var viewer by remember { mutableStateOf(false) }
    if (viewer) GalleryViewer(info.gallery, pager.currentPage) { viewer = false }

    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            GlassTopBar(facility.name, onBack = { navController.popBackStack() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(320.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White)
                ) {
                    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                        Box(Modifier.fillMaxSize().clickable(role = Role.Image, onClickLabel = "View full screen") { viewer = true }) {
                            GalleryPage(info.gallery[page], compact = true)
                        }
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(12.dp).size(36.dp).clip(CircleShape).background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.OpenInFull, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    PagerDots(pager, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), onDark = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FactTile(Icons.Outlined.Groups, "Max ${facility.maxPax} pax", Modifier.weight(1f))
                    FactTile(Icons.Outlined.Payments, if (facility.deposit > 0) "S$${facility.deposit} deposit" else "No deposit", Modifier.weight(1f))
                }
                GlassCard(padding = 16.dp) {
                    SectionTitle("About")
                    Text(info.about, fontFamily = DMSans, fontSize = 14.sp, lineHeight = 21.sp, color = FacilityInk)
                }
                GlassCard(padding = 16.dp) {
                    SectionTitle("Operation Time", Icons.Outlined.AccessTime)
                    info.hours.forEach { (days, time) -> InfoLine(days, time) }
                    if (info.sessions.isNotEmpty()) {
                        HorizontalDividerLine()
                        SectionTitle("Sessions")
                        info.sessions.forEach { (name, time) -> InfoLine(name, time) }
                    }
                }
                GlassCard(padding = 16.dp) {
                    SectionTitle("Booking Time", Icons.Outlined.EventAvailable)
                    Text(info.bookingWindow, fontFamily = DMSans, fontSize = 14.sp, color = FacilityInk)
                }
                GlassCard(padding = 16.dp) {
                    SectionTitle("Terms & Conditions")
                    info.terms.forEachIndexed { i, term ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${i + 1}.", Modifier.width(22.dp), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                            Text(term, fontFamily = DMSans, fontSize = 14.sp, lineHeight = 20.sp, color = FacilityInk)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Box(
                Modifier.padding(16.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2E9A79), BookingsDeepGreen)))
                    .clickable(role = Role.Button) {
                        // Book Facility opens this facility's date and slot picker when it comes back.
                        val previous = navController.previousBackStackEntry
                        if (previous?.destination?.route == Screen.BookFacility.route) {
                            previous.savedStateHandle[ExpandFacilityKey] = facility.id
                            navController.popBackStack()
                        } else {
                            // Opened from elsewhere, e.g. Booking Details: start Book Facility afresh.
                            navController.navigate(Screen.BookFacility.route) { popUpTo(Screen.BookFacility.route) { inclusive = true } }
                            navController.currentBackStackEntry?.savedStateHandle?.set(ExpandFacilityKey, facility.id)
                        }
                    },
                contentAlignment = Alignment.Center
            ) { Text("Book Now", fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        }
    }
}

internal const val ExpandFacilityKey = "expandFacility"

@Composable
private fun SectionTitle(text: String, icon: ImageVector? = null) {
    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = BookingsDeepGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
        Text(value, fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
    }
}

@Composable
private fun HorizontalDividerLine() {
    Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(BookingsDivider))
}

@Composable
private fun FactTile(icon: ImageVector, text: String, modifier: Modifier) {
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xB3FFFFFF))
            .border(1.dp, FacilityGlassBorder, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = BookingsDeepGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FacilityInk, maxLines = 1)
    }
}

@Composable
private fun GalleryPage(item: GalleryItem, compact: Boolean) {
    when (item) {
        is GalleryItem.Photo -> Image(painterResource(item.image), null, Modifier.fillMaxSize(),
            contentScale = if (compact) ContentScale.Crop else ContentScale.Fit)
        is GalleryItem.SiteMap -> Column(Modifier.fillMaxSize().background(Color.White)) {
            Box(Modifier.fillMaxWidth().background(Color.Black).padding(vertical = if (compact) 8.dp else 14.dp),
                contentAlignment = Alignment.Center) {
                Text(item.title, fontFamily = DMSans, fontSize = if (compact) 16.sp else 24.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                SiteMap(item.pins, compact)
            }
        }
    }
}

/** The condo site map with each pin's dot, leader line and label drawn over it. */
@Composable
private fun SiteMap(pins: List<MapPin>, compact: Boolean) {
    BoxWithConstraints(Modifier.aspectRatio(SiteMapAspect)) {
        Image(painterResource(R.drawable.facility_site_map), "Site map", Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            pins.forEach { pin ->
                val dot = Offset(pin.x * size.width, pin.y * size.height)
                drawLine(PinRed, Offset(pin.labelX * size.width, pin.labelY * size.height), dot, strokeWidth = 2.dp.toPx())
                drawCircle(PinRed, radius = (if (compact) 4 else 6).dp.toPx(), center = dot)
            }
        }
        pins.forEach { pin ->
            Text(
                pin.label,
                Modifier.align(Alignment.TopStart)
                    .offset(maxWidth * pin.labelX, maxHeight * pin.labelY)
                    .centredOnPoint()
                    .clip(RoundedCornerShape(10.dp)).background(PinPurple)
                    .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = if (compact) 3.dp else 6.dp),
                fontFamily = DMSans, fontSize = if (compact) 11.sp else 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White
            )
        }
    }
}

// Centres the label on the point it was offset to.
private fun Modifier.centredOnPoint() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { placeable.place(-placeable.width / 2, -placeable.height / 2) }
}

@Composable
private fun PagerDots(pager: PagerState, modifier: Modifier, onDark: Boolean) {
    if (pager.pageCount < 2) return
    Row(modifier.clip(RoundedCornerShape(50)).background(if (onDark) Color.Transparent else Color(0x66000000))
        .padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pager.pageCount) { i ->
            Box(Modifier.size(8.dp).clip(CircleShape)
                .background(if (i == pager.currentPage) Color(0xFF5CC8C8) else Color.White.copy(alpha = .7f)))
        }
    }
}

/** Full-screen gallery, opened on the page that was showing. */
@Composable
private fun GalleryViewer(gallery: List<GalleryItem>, startPage: Int, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = startPage) { gallery.size }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.White).systemBarsPadding()) {
            HorizontalPager(pager, Modifier.fillMaxSize().padding(top = 64.dp, bottom = 48.dp)) { page ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GalleryPage(gallery[page], compact = false) }
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(12.dp).size(48.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Close, "Close", tint = Color(0xFF5CC8C8), modifier = Modifier.size(34.dp)) }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(gallery.size) { i ->
                    Box(Modifier.size(10.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) Color(0xFF5CC8C8) else Color(0xFF808080)))
                }
            }
        }
    }
}
