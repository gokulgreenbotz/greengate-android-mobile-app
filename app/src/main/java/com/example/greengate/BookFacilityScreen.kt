package com.example.greengate

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.OutdoorGrill
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.SportsTennis
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

internal val FacilityInk = Color(0xFF14232B)
internal val FacilityMuted = Color(0xFF5B6B72)
internal val FacilityGreen = Color(0xFF1E8A6A)
internal val FacilityOrange = Color(0xFFE8772E)
internal val FacilityGlass = Color(0xD9FFFFFF)
internal val FacilityGlassBorder = Color(0xB3FFFFFF)
private val FacilityBackdrop = Brush.verticalGradient(
    0f to Color(0xFF5E9FB4), 0.18f to Color(0xFF9DCBD3), 0.45f to Color(0xFFD5EDEA), 1f to Color(0xFFEAF6F2)
)

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
    // Refundable deposit in whole S$; 0 means none.
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

@Composable
fun BookFacilityScreen(navController: NavController) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val bars = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        bars?.isAppearanceLightStatusBars = false
        onDispose { bars?.isAppearanceLightStatusBars = true }
    }
    var category by remember { mutableStateOf(FacilityCategory.ALL) }
    // Only one facility's date and slot picker is open at a time.
    var expandedName by remember { mutableStateOf<String?>(null) }
    val visible = Facilities.filter { category == FacilityCategory.ALL || it.category == category }

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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visible, key = { it.name }) { facility ->
                    val expanded = facility.name == expandedName
                    Column {
                        FacilityCard(facility, expanded) { expandedName = if (expanded) null else facility.name }
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                        ) {
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
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.align(Alignment.CenterStart), onBack)
        Text(
            "Book Facility", Modifier.align(Alignment.Center),
            fontFamily = DMSans, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White
        )
        GlassCircleButton(Icons.Rounded.CalendarMonth, "My bookings", Modifier.align(Alignment.CenterEnd), onCalendar)
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
            .background(if (selected) Color(0xFFBDF0DC) else Color(0xCCFFFFFF))
            .border(1.5.dp, if (selected) Color(0xFF8FE3C4) else FacilityGlassBorder, shape)
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
private fun FacilityCard(facility: Facility, expanded: Boolean, onClick: () -> Unit) {
    // The arrow turns to point at the open picker below.
    val arrowTurn by animateFloatAsState(if (expanded) 90f else 0f, label = "arrowTurn")
    val shape = RoundedCornerShape(24.dp)
    Surface(
        shape = shape, color = Color(0x99FFFFFF), border = BorderStroke(1.5.dp, FacilityGlassBorder),
        shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
    ) {
        Box {
            Image(
                painterResource(facility.photo), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
            AvailabilityBadge(facility.slotsLeft, Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 12.dp))
            // The info panel overlaps the photo's lower edge like a frosted sheet.
            Row(
                Modifier.padding(start = 10.dp, end = 10.dp, top = 126.dp, bottom = 10.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)).background(FacilityGlass)
                    .border(1.dp, FacilityGlassBorder, RoundedCornerShape(20.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FacilityIconTile(facility.icon, facility.artwork)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(facility.name, fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                    Text(
                        "Max ${facility.maxPax} pax  •  ${if (facility.deposit > 0) "S$${facility.deposit} deposit" else "No deposit"}",
                        fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted
                    )
                    Spacer(Modifier.height(6.dp))
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
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(46.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White),
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
            .background(if (slotsLeft == null) Color(0xF2E9FBF3) else Color(0xF2FFF2E8))
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
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFFE3F5EE)))),
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
        (AppPreferences.theme == AppTheme.ONE || !AppPreferences.useReferenceArtwork)
    if (flat) Icon(icon, null, tint = Color(0xFF2BA383), modifier = Modifier.size(size * .7f))
    else Image(painterResource(artwork), null, Modifier.size(size))
}
