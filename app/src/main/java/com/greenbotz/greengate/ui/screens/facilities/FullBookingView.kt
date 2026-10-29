package com.greenbotz.greengate.ui.screens.facilities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.NavyGoldButton
import com.greenbotz.greengate.ui.theme.NavyGoldCream
import com.greenbotz.greengate.ui.theme.NavyGoldGold
import com.greenbotz.greengate.ui.theme.NavyGoldLine
import com.greenbotz.greengate.ui.theme.NavyGoldNavy
import com.greenbotz.greengate.ui.theme.NavyGoldShadow
import com.greenbotz.greengate.ui.theme.NavyGoldTheme
import com.greenbotz.greengate.ui.theme.PlayfairDisplay
import com.greenbotz.greengate.ui.theme.SoftGreen
import java.util.Calendar

// Bookings run for one to three hours.
private val Durations = 1..3

private enum class FullSection { NONE, DATE, DURATION }

private val GoldEdge = Color(0xFFD9B36A)
// A chosen time, duration or mode: navy with a gold edge in Theme 4, solid green otherwise.
private val ChosenFill: Brush get() = if (NavyGoldTheme) NavyGoldButton else SolidColor(FacilityGreen)
private val ChosenEdge: Color get() = if (NavyGoldTheme) GoldEdge else FacilityGreen
private val FieldDivider: Color get() = if (NavyGoldTheme) NavyGoldLine else themed(Color(0xFFE3ECEA), ClassicLine)
private val FieldShadow: Color get() = if (NavyGoldTheme) NavyGoldShadow else Color(0x220F3B33)

private val BookingDaySaver = listSaver<BookingDay, Int>(
    save = { listOf(it.year, it.month, it.day) },
    restore = { BookingDay(it[0], it[1], it[2]) }
)

/** e.g. "09:00". The chips are too narrow for "9:00 AM". */
private fun shortHourLabel(hour: Int) = "${hour.toString().padStart(2, '0')}:00"

private fun durationLabel(hours: Int) = if (hours == 1) "1 hour" else "$hours hours"

/**
 * Book Facility's Full view: one facility under a large photo, a strip of every facility to switch
 * between, and date, start time, duration and guests together in one card. Only start times whose
 * whole duration is free can be picked. [onCheck] receives the facility, day, start hour, guests and hours.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FullBookingView(
    // A facility to show first, e.g. from "Book Now" on its About page; it replaces any earlier choice.
    initialFacilityId: String?,
    onAbout: (Facility) -> Unit,
    onCheck: (facility: Facility, day: BookingDay, hour: Int, guests: Int, hours: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Hearted facilities lead, as in the Quick list.
    val favourites = AppPreferences.favouriteFacilities
    val ordered = Facilities.sortedByDescending { it.id in favourites }
    var facilityId by rememberSaveable { mutableStateOf(initialFacilityId ?: ordered.first().id) }
    LaunchedEffect(initialFacilityId) { initialFacilityId?.let { facilityId = it } }
    val facility = Facilities.find { it.id == facilityId } ?: ordered.first()

    val today = remember { BookingDay.today() }
    val nowHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    var savedDay by rememberSaveable(stateSaver = BookingDaySaver) { mutableStateOf(today) }
    // A day kept from yesterday's visit moves up to today.
    val day = maxOf(savedDay, today)
    var hours by rememberSaveable { mutableIntStateOf(Durations.first) }
    var chosenHour by rememberSaveable { mutableStateOf<Int?>(null) }
    var guests by rememberSaveable { mutableIntStateOf(1) }
    val guestCount = guests.coerceIn(1, facility.maxPax)
    var open by remember { mutableStateOf(FullSection.NONE) }
    var shownMonth by remember { mutableStateOf(BookingDay(day.year, day.month, 1)) }

    // Every start time left today (or any on a later day) that leaves room for the whole duration.
    val starts = (SlotHours.first..SlotHours.last - hours + 1).filter { day != today || it > nowHour }
    val states = starts.associateWith { rangeState(facility.name, day, it, hours, facility.slotsLeft != null, today, nowHour) }
    val bookable = starts.filter { states[it] == SlotState.OPEN || states[it] == SlotState.FEW_LEFT }
    // A choice that a new facility, date or duration rules out falls back to the earliest free start.
    val selectedHour = chosenHour?.takeIf { it in bookable } ?: bookable.firstOrNull()

    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            FullHero(facility) { onAbout(facility) }
            Spacer(Modifier.height(14.dp))
            FacilityStrip(ordered, facility.id) { facilityId = it }
            Spacer(Modifier.height(14.dp))
            GlassCard(padding = 0.dp) {
                FieldRow(Icons.Rounded.CalendarMonth, "Date", expanded = open == FullSection.DATE,
                    onClick = { open = if (open == FullSection.DATE) FullSection.NONE else FullSection.DATE }) {
                    FieldValue("${day.label()} ${day.year}")
                }
                RevealSection(open == FullSection.DATE) {
                    MonthCalendar(
                        month = shownMonth, today = today, selected = day,
                        onMonthChange = { shownMonth = it },
                        onSelect = { savedDay = it; open = FullSection.NONE }
                    )
                }
                FieldDividerLine()
                FieldRow(Icons.Rounded.Schedule, "Time") {
                    Spacer(Modifier.height(8.dp))
                    TimeChips(starts, states, selectedHour, scrollKey = listOf(facility.id, day, hours)) { chosenHour = it }
                }
                FieldDividerLine()
                FieldRow(Icons.Rounded.HourglassBottom, "Duration", expanded = open == FullSection.DURATION,
                    onClick = { open = if (open == FullSection.DURATION) FullSection.NONE else FullSection.DURATION }) {
                    FieldValue(durationLabel(hours))
                }
                RevealSection(open == FullSection.DURATION, start = FieldTextStart) {
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Durations.forEach { h ->
                            OptionPill(durationLabel(h), h == hours, Modifier.weight(1f)) { hours = h; open = FullSection.NONE }
                        }
                    }
                }
                FieldDividerLine()
                FieldRow(
                    Icons.Rounded.Groups, null, iconTint = if (NavyGoldTheme) NavyGoldNavy else FacilityGreen,
                    trailing = {
                        StepperButton(Icons.Rounded.Remove, "Fewer guests", guestCount > 1) { guests = guestCount - 1 }
                        Text("$guestCount", Modifier.widthIn(min = 44.dp), fontFamily = DMSans, fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = FacilityInk, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        StepperButton(Icons.Rounded.Add, "More guests", guestCount < facility.maxPax) { guests = guestCount + 1 }
                    }
                ) {
                    Text("No. of Guests", fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
                    Text("Up to ${facility.maxPax} pax", fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        CheckAvailabilityButton(
            enabled = selectedHour != null,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) { selectedHour?.let { onCheck(facility, day, it, guestCount, hours) } }
    }
}

/** The facility's photo with About and availability on top, and its name over a dark fade at the bottom. */
@Composable
private fun FullHero(facility: Facility, onAbout: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxWidth().aspectRatio(2.1f).shadow(6.dp, shape, ambientColor = FieldShadow, spotColor = FieldShadow)
            .clip(shape).background(if (NavyGoldTheme) NavyGoldCream else themed(Color(0xFFD5EDEA), ClassicMint))
    ) {
        Crossfade(facility, label = "heroPhoto") { FacilityImage(it.photo, Modifier.fillMaxSize(), ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0.4f to Color.Transparent, 1f to if (NavyGoldTheme) Color(0xD90B1446) else Color(0xB3000000)
        )))
        AboutChip(facility.name, Modifier.align(Alignment.TopStart).padding(12.dp), onAbout)
        AvailabilityBadge(facility.slotsLeft, Modifier.align(Alignment.TopEnd).padding(12.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 14.dp)) {
            Text(
                facility.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White,
                fontFamily = if (NavyGoldTheme) PlayfairDisplay else DMSans, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                // Playfair's default old-style figures make "50m" read as "5om".
                style = TextStyle(fontFeatureSettings = "lnum")
            )
            Text(
                "Max ${facility.maxPax} pax  •  ${if (facility.deposit > 0) "$${facility.deposit} deposit" else "No deposit"}",
                fontFamily = DMSans, fontSize = 15.sp, color = Color(0xE6FFFFFF), maxLines = 1
            )
        }
    }
}

private val ThumbGap = 10.dp

/** Every facility as a small photo; four fit across, and any more scroll. */
@Composable
private fun FacilityStrip(facilities: List<Facility>, selectedId: String, onSelect: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = (maxWidth - ThumbGap * 3) / 4
        LazyRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(ThumbGap)) {
            items(facilities, key = { it.id }) { f ->
                FacilityThumb(f, f.id == selectedId, Modifier.width(width)) { onSelect(f.id) }
            }
        }
    }
}

@Composable
private fun FacilityThumb(facility: Facility, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val outer = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(outer).selectable(selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.3f)
                .then(if (selected) Modifier.shadow(4.dp, outer, ambientColor = FieldShadow, spotColor = FieldShadow) else Modifier)
                .clip(outer).background(Color.White)
                .border(if (selected) 2.dp else 1.dp, if (selected) ChosenEdge else FacilityGlassBorder, outer)
                .padding(3.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (NavyGoldTheme) NavyGoldCream else themed(Color(0xFFD5EDEA), ClassicMint))
        ) {
            FacilityImage(facility.photo, Modifier.fillMaxSize(), ContentScale.Crop)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            facility.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = DMSans, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) FacilityInk else FacilityMuted
        )
    }
}

// Where a row's label starts: 16dp padding, the 52dp icon and a 14dp gap.
private val FieldTextStart = 82.dp

/** One line of the booking card: a round icon, an optional small [label] over [content], and a chevron or [trailing]. */
@Composable
private fun FieldRow(
    icon: ImageVector, label: String?,
    iconTint: Color = if (NavyGoldTheme) NavyGoldGold else FacilityGreen,
    expanded: Boolean? = null, onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val turn by animateFloatAsState(if (expanded == true) 180f else 0f, label = "chevron")
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = label?.let { "Change $it" }, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(if (NavyGoldTheme) NavyGoldCream else BookingsMint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (label != null) Text(label, fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
            content()
        }
        if (expanded != null) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = FacilityInk, modifier = Modifier.size(28.dp).rotate(turn))
        }
        trailing?.invoke(this)
    }
}

@Composable
private fun FieldValue(text: String) {
    Text(text, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FacilityInk, maxLines = 1)
}

@Composable
private fun FieldDividerLine() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FieldDivider)
}

/**
 * The calendar or duration choices under their row. They appear at full height and only fade, then
 * scroll into view, so a row near the bottom of the screen doesn't open out of sight.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RevealSection(visible: Boolean, start: androidx.compose.ui.unit.Dp = 16.dp, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = shrinkVertically(tween(200), shrinkTowards = Alignment.Top) + fadeOut(tween(150))
    ) {
        val requester = remember { BringIntoViewRequester() }
        LaunchedEffect(Unit) {
            // One frame, so the section has been measured before scrolling to it.
            withFrameNanos { }
            requester.bringIntoView()
        }
        Box(Modifier.bringIntoViewRequester(requester).padding(start = start, end = 16.dp, bottom = 14.dp)) { content() }
    }
}

/**
 * Start times in one sliding row. Booked-out starts stay visible but struck through; an orange dot
 * marks one with few places left. The row slides to the chosen time whenever [scrollKey] changes.
 */
@Composable
private fun TimeChips(starts: List<Int>, states: Map<Int, SlotState>, selected: Int?, scrollKey: Any, onSelect: (Int) -> Unit) {
    if (starts.none { states[it] == SlotState.OPEN || states[it] == SlotState.FEW_LEFT }) {
        Text("No free times on this day. Try another date or a shorter duration.",
            fontFamily = DMSans, fontSize = 14.sp, lineHeight = 19.sp, color = FacilityOrange)
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(scrollKey) {
        val index = starts.indexOf(selected)
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyRow(state = listState, modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(starts, key = { it }) { hour ->
            TimeChip(hour, states[hour] ?: SlotState.FULL, hour == selected) { onSelect(hour) }
        }
    }
}

@Composable
private fun TimeChip(hour: Int, state: SlotState, selected: Boolean, onClick: () -> Unit) {
    val enabled = state == SlotState.OPEN || state == SlotState.FEW_LEFT
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.width(70.dp).height(46.dp)
            .then(if (selected) Modifier.shadow(3.dp, shape, ambientColor = FieldShadow, spotColor = FieldShadow) else Modifier)
            .clip(shape)
            .background(when {
                selected -> ChosenFill
                enabled -> SolidColor(Color.White)
                else -> SolidColor(FacilityMuted.copy(alpha = .08f))
            })
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ChosenEdge else FieldDivider, shape)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            shortHourLabel(hour), fontFamily = DMSans, fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                selected -> Color.White
                enabled -> FacilityInk
                else -> FacilityMuted.copy(alpha = .5f)
            },
            textDecoration = if (enabled) null else TextDecoration.LineThrough
        )
        if (state == SlotState.FEW_LEFT && !selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 6.dp).size(6.dp).clip(CircleShape).background(FacilityOrange))
        }
    }
}

@Composable
private fun OptionPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.height(42.dp).clip(shape)
            .background(if (selected) ChosenFill else SolidColor(Color.White))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ChosenEdge else FieldDivider, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = DMSans, fontSize = 15.sp, maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else FacilityInk)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(46.dp).shadow(if (enabled) 3.dp else 0.dp, shape, ambientColor = FieldShadow, spotColor = FieldShadow)
            .clip(shape).background(Color.White).border(1.dp, FieldDivider, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else .4f),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = FacilityInk, modifier = Modifier.size(22.dp))
    }
}

/** Navy with a gold edge in Theme 4, the booking green otherwise; greyed while no start time is free. */
@Composable
private fun CheckAvailabilityButton(enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val shadow = if (NavyGoldTheme) NavyGoldShadow else Color(0x4016A37F)
    Row(
        modifier.fillMaxWidth().height(58.dp)
            .shadow(if (enabled) 8.dp else 0.dp, shape, ambientColor = shadow, spotColor = shadow)
            .clip(shape)
            .background(if (enabled) greenButton(Color(0xFF2AA07D)) else SolidColor(FacilityMuted.copy(alpha = .18f)))
            .then(if (NavyGoldTheme && enabled) Modifier.border(1.5.dp, GoldEdge, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (enabled) Color.White else FacilityMuted
        Text("Check Availability", fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = tint)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}
