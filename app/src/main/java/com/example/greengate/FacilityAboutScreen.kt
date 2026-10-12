package com.example.greengate

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Countertops
import androidx.compose.material.icons.outlined.Deck
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OutdoorGrill
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Roofing
import androidx.compose.material.icons.outlined.Shower
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextAlign
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

internal class Amenity(val icon: ImageVector, val label: String)

internal class FacilityInfo(
    val about: String,
    val gallery: List<GalleryItem>,
    // Length of one booking, shown beside the pax and deposit facts.
    val slot: String,
    val amenities: List<Amenity>,
    val hours: List<Pair<String, String>>,
    val sessions: List<Pair<String, String>>,
    val bookingWindow: String,
    val cancellation: List<String>,
    val terms: List<String>,
)

// The site map is 1120 x 1660.
private const val SiteMapAspect = 1120f / 1660f
private val PinPurple = Color(0xFFB45FD8)
private val PinRed = Color(0xFFE5262B)
private val SheetColor = Color(0xFFF1F8F6)
private val HeroHeight = 300.dp
private val SheetOverlap = 28.dp

// TODO: load from the facilities API; the hours and BBQ sessions follow the condo's published
// terms, the rest are sample house rules.
internal fun Facility.info(): FacilityInfo = when (id) {
    "bbq" -> FacilityInfo(
        about = "Three covered barbeque pits set in the garden beside the main pool, each with a stone counter and seating for your guests.",
        gallery = listOf(
            GalleryItem.Photo(R.drawable.facility_bbq_photo_pits),
            GalleryItem.SiteMap("BBQ Pits", listOf(
                MapPin("Pit 3", .718f, .112f, .42f, .060f),
                MapPin("Pit 2", .599f, .361f, .27f, .300f),
                MapPin("Pit 1", .603f, .399f, .27f, .430f),
            )),
            GalleryItem.Photo(R.drawable.facility_condo_pool_photo),
        ),
        slot = "3 sessions",
        amenities = listOf(
            Amenity(Icons.Outlined.OutdoorGrill, "BBQ Pit"),
            Amenity(Icons.Outlined.Countertops, "Stone Counter"),
            Amenity(Icons.Outlined.Chair, "Seating"),
            Amenity(Icons.Outlined.Roofing, "Covered"),
            Amenity(Icons.Outlined.WaterDrop, "Wash Sink"),
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
        cancellation = listOf(
            "Free cancellation up to 24 hours before the session; the deposit is refunded in full.",
            "Cancellations made less than 24 hours before the session forfeit the deposit.",
            "No-shows forfeit the deposit and the session is released to other residents.",
        ),
        terms = listOf(
            "*Use until 11:00 PM needs Management's approval.",
            "Only residents may book; the resident must be present throughout the session.",
            "The $$deposit deposit is refunded after the pit is inspected and found clean.",
            "Bring your own charcoal and utensils; clear all food and rubbish when done.",
            "Keep noise to a minimum after 10:00 PM out of respect for neighbours.",
        ),
    )
    "tennis" -> FacilityInfo(
        about = "A full-size hard court beside Block 19, floodlit for evening play.",
        gallery = listOf(
            GalleryItem.Photo(R.drawable.facility_tennis_photo_court),
            GalleryItem.SiteMap("Tennis Court", listOf(MapPin("Court 1", .866f, .442f, .80f, .36f))),
            GalleryItem.Photo(photo),
        ),
        slot = "1 hour slot",
        amenities = listOf(
            Amenity(Icons.Outlined.SportsTennis, "Hard Court"),
            Amenity(Icons.Outlined.Lightbulb, "Floodlights"),
            Amenity(Icons.Outlined.Chair, "Bench"),
            Amenity(Icons.Outlined.WaterDrop, "Water Point"),
        ),
        hours = listOf("Daily" to "7:00 AM – 10:00 PM"),
        sessions = emptyList(),
        bookingWindow = "Up to 7 days ahead, 1 hour per booking",
        cancellation = listOf(
            "Cancel any time up to 2 hours before the slot at no charge.",
            "Unclaimed bookings are released 15 minutes after the start time.",
        ),
        terms = listOf(
            "Non-marking tennis shoes only.",
            "Up to $maxPax players per booking.",
            "Switch off the floodlights after evening sessions.",
        ),
    )
    "pool" -> FacilityInfo(
        about = "A 50 m lap pool with a shaded sun deck and loungers.",
        gallery = listOf(
            GalleryItem.Photo(R.drawable.facility_condo_pool_photo),
            GalleryItem.SiteMap("Pool", listOf(MapPin("Lap Pool", .533f, .566f, .30f, .62f))),
            GalleryItem.Photo(photo),
        ),
        slot = "1 hour slot",
        amenities = listOf(
            Amenity(Icons.Outlined.Checkroom, "Changing Room"),
            Amenity(Icons.Outlined.Shower, "Shower"),
            Amenity(Icons.Outlined.Deck, "Sun Deck"),
            Amenity(Icons.Outlined.Lock, "Lockers"),
        ),
        hours = listOf("Daily" to "7:00 AM – 9:00 PM"),
        sessions = emptyList(),
        bookingWindow = "Up to 7 days ahead",
        cancellation = listOf(
            "Cancel any time up to 1 hour before the slot at no charge.",
            "Unclaimed bookings are released 15 minutes after the start time.",
        ),
        terms = listOf(
            "No lifeguard on duty; children under 12 must be accompanied by an adult.",
            "Shower before entering the pool. No glass bottles on the deck.",
            "The pool closes during thunderstorms.",
        ),
    )
    else -> FacilityInfo(
        about = "A fully equipped gym with cardio machines, free weights and a stretching area.",
        gallery = listOf(GalleryItem.Photo(photo)),
        slot = "1 hour slot",
        amenities = listOf(
            Amenity(Icons.Outlined.FitnessCenter, "Free Weights"),
            Amenity(Icons.Outlined.AcUnit, "Air-con"),
            Amenity(Icons.Outlined.Lock, "Lockers"),
            Amenity(Icons.Outlined.WaterDrop, "Water Cooler"),
        ),
        hours = listOf("Daily" to "6:00 AM – 11:00 PM"),
        sessions = emptyList(),
        bookingWindow = "Up to 3 days ahead, 1 hour per booking",
        cancellation = listOf(
            "Free cancellation up to 2 hours before the slot.",
            "Later cancellations and no-shows forfeit the $$deposit deposit.",
        ),
        terms = listOf(
            "Residents aged 16 and above only.",
            "Wipe down equipment after use and return weights to the rack.",
            "Proper workout attire and shoes must be worn.",
        ),
    )
}

/** Hero photo, key facts, gallery, amenities, opening hours, booking window, cancellation policy and terms for one facility. */
@Composable
internal fun FacilityAboutScreen(navController: NavController, facility: Facility) {
    val info = remember(facility.id) { facility.info() }
    val heroIndex = remember(info) { info.gallery.indexOfFirst { it is GalleryItem.Photo }.coerceAtLeast(0) }
    var viewerPage by remember { mutableStateOf<Int?>(null) }
    // TODO: persist favourites once residents have a profile store.
    var favourite by rememberSaveable(facility.id) { mutableStateOf(false) }
    viewerPage?.let { GalleryViewer(info.gallery, it) { viewerPage = null } }

    Box(Modifier.fillMaxSize().background(SheetColor), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
            Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Hero(
                    info.gallery[heroIndex], favourite,
                    onBack = { navController.popBackStack() },
                    onFavourite = { favourite = !favourite },
                    onOpen = { viewerPage = heroIndex },
                )
                Column(
                    Modifier.padding(top = HeroHeight - SheetOverlap).fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = SheetOverlap, topEnd = SheetOverlap))
                        .background(SheetColor).padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(facility.name, fontFamily = DMSans, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                        FactChips(facility, info.slot)
                    }
                    Column {
                        SectionTitle("About this facility")
                        Text(info.about, fontFamily = DMSans, fontSize = 15.sp, lineHeight = 22.sp, color = FacilityMuted)
                        if (info.gallery.size > 1) {
                            Spacer(Modifier.height(14.dp))
                            Thumbnails(info.gallery) { viewerPage = it }
                        }
                    }
                    if (info.amenities.isNotEmpty()) Column {
                        SectionTitle("Amenities")
                        Amenities(info.amenities)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            SectionTitle("Pre-booking Time", Icons.Outlined.EventAvailable)
                            Text(info.bookingWindow, fontFamily = DMSans, fontSize = 14.sp, color = FacilityInk)
                        }
                        GlassCard(padding = 16.dp) {
                            SectionTitle("Cancellation Policy", Icons.Outlined.EventBusy)
                            info.cancellation.forEach { BulletLine(it) }
                        }
                        GlassCard(padding = 16.dp) {
                            SectionTitle("Terms & Conditions", Icons.Outlined.Gavel)
                            info.terms.forEachIndexed { i, term ->
                                Row(Modifier.padding(vertical = 4.dp)) {
                                    Text("${i + 1}.", Modifier.width(22.dp), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                                    Text(term, fontFamily = DMSans, fontSize = 14.sp, lineHeight = 20.sp, color = FacilityInk)
                                }
                            }
                        }
                    }
                }
            }
            SelectDateButton {
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
            }
        }
    }
}

internal const val ExpandFacilityKey = "expandFacility"

/** Full-bleed photo under the status bar, with back and favourite buttons over it; tapping it opens the gallery. */
@Composable
private fun Hero(item: GalleryItem, favourite: Boolean, onBack: () -> Unit, onFavourite: () -> Unit, onOpen: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(HeroHeight).clickable(role = Role.Image, onClickLabel = "View photos", onClick = onOpen)) {
        GalleryPage(item, compact = true)
        // Keeps the status bar and buttons legible over bright photos.
        Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Color(0x59000000), Color.Transparent))))
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeroButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", FacilityInk, onBack)
            HeroButton(
                if (favourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (favourite) "Remove from favourites" else "Add to favourites",
                if (favourite) BookingsRed else FacilityInk, onFavourite
            )
        }
    }
}

@Composable
private fun HeroButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color(0xE6FFFFFF))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(22.dp)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactChips(facility: Facility, slot: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FactChip(Icons.Outlined.Groups, "Max ${facility.maxPax} pax")
        FactChip(Icons.Outlined.Payments, if (facility.deposit > 0) "$${facility.deposit} deposit" else "No deposit")
        FactChip(Icons.Outlined.AccessTime, slot)
    }
}

@Composable
private fun FactChip(icon: ImageVector, text: String) {
    Row(
        Modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.dp, BookingsDivider, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = BookingsDeepGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FacilityInk, maxLines = 1)
    }
}

/** Every gallery item as a small card, scrolling past the screen edge; tapping one opens it full screen. */
@Composable
private fun Thumbnails(gallery: List<GalleryItem>, onOpen: (Int) -> Unit) {
    Row(
        // Bleeds to the screen edge so the row visibly continues.
        Modifier.layout { measurable, constraints ->
            val bleed = 20.dp.roundToPx()
            val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + bleed * 2))
            layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
        }.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        gallery.forEachIndexed { i, item ->
            Box(
                Modifier.size(width = 112.dp, height = 80.dp).clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Image, onClickLabel = "View full screen") { onOpen(i) }
            ) {
                when (item) {
                    is GalleryItem.Photo -> Image(painterResource(item.image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    is GalleryItem.SiteMap -> {
                        Image(painterResource(R.drawable.facility_site_map), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Row(
                            Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color(0xB3000000)).padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Place, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Site map", fontFamily = DMSans, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Amenities(amenities: List<Amenity>) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        amenities.forEach { amenity ->
            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .border(1.dp, BookingsDivider, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(amenity.icon, null, tint = BookingsDeepGreen, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.height(6.dp))
                Text(amenity.label, fontFamily = DMSans, fontSize = 12.sp, lineHeight = 15.sp, color = FacilityInk,
                    textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
private fun SelectDateButton(onClick: () -> Unit) {
    Box(
        Modifier.background(SheetColor).padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(56.dp)
            .shadow(6.dp, RoundedCornerShape(28.dp), spotColor = BookingsDeepGreen).clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2E9A79), BookingsDeepGreen)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("Select Date & Time", fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(22.dp))
    }
}

@Composable
private fun SectionTitle(text: String, icon: ImageVector? = null) {
    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = BookingsDeepGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
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
private fun BulletLine(text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Box(Modifier.padding(top = 7.dp, end = 10.dp).size(6.dp).clip(CircleShape).background(BookingsDeepGreen))
        Text(text, fontFamily = DMSans, fontSize = 14.sp, lineHeight = 20.sp, color = FacilityInk)
    }
}

@Composable
private fun HorizontalDividerLine() {
    Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(BookingsDivider))
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

/** Full-screen gallery, opened on the page that was tapped. */
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
            if (gallery.size > 1) Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(gallery.size) { i ->
                    Box(Modifier.size(10.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) Color(0xFF5CC8C8) else Color(0xFF808080)))
                }
            }
        }
    }
}
