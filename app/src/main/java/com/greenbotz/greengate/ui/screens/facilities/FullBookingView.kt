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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HourglassBottom
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

// Bookings run for one to three hours, one hourly slot each.
private const val MaxHours = 3

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

/** e.g. "9 AM". The chips are too narrow for "9:00 AM". */
private fun shortHourLabel(hour: Int) = "${(hour + 11) % 12 + 1} ${if (hour < 12) "AM" else "PM"}"

private fun durationLabel(hours: Int) = if (hours == 1) "1 hour" else "$hours hours"

/**
 * Book Facility's Full view: one facility under a large photo, a strip of every facility to switch
 * between, and date, time, duration and guests together in one card. The date is Today, Tomorrow or
 * any other from a calendar; the time is up to [MaxHours] back-to-back hourly slots, picked from the
 * morning or afternoon, and their count is the duration. [onCheck] receives the facility, day, first
 * slot's hour, guests and hours.
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
    // The calendar opens only for "Other".
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var shownMonth by remember { mutableStateOf(BookingDay(day.year, day.month, 1)) }
    // The chosen run of back-to-back slots: its first hour and how many. Nothing is chosen at first.
    var chosenStart by rememberSaveable { mutableStateOf<Int?>(null) }
    var chosenHours by rememberSaveable { mutableIntStateOf(0) }
    // Set when a tap would make the booking longer than MaxHours.
    var overLimit by remember { mutableStateOf(false) }
    var guests by rememberSaveable { mutableIntStateOf(1) }
    val guestCount = guests.coerceIn(1, facility.maxPax)
    // Once today's morning has gone, a day opens on the afternoon.
    val morningOver = day == today && nowHour >= SlotPeriod.MORNING.hours.last
    var period by rememberSaveable(day) { mutableStateOf(if (morningOver) SlotPeriod.AFTERNOON else SlotPeriod.MORNING) }
    val periodPosition by animateFloatAsState(period.ordinal.toFloat(), tween(250), label = "period")

    val states = SlotHours.associateWith { slotState(facility.name, day, it, facility.slotsLeft != null, today, nowHour) }
    val free = states.filterValues { it == SlotState.OPEN || it == SlotState.FEW_LEFT }.keys
    val selected = chosenStart?.let { it until it + chosenHours }?.takeIf { run -> !run.isEmpty() && run.all { it in free } }
    // A new facility or date that rules out the chosen slots drops them.
    LaunchedEffect(facility.id, day) { if (selected == null) chosenStart = null }

    fun onSlot(hour: Int) {
        val run = selected
        overLimit = false
        when {
            run == null -> { chosenStart = hour; chosenHours = 1 }
            // An end slot comes off the run; taking the last one clears the choice.
            hour == run.first -> { chosenStart = hour + 1; chosenHours = run.count() - 1 }
            hour == run.last -> chosenHours = run.count() - 1
            // The slot just before or after the run extends it, up to MaxHours.
            hour == run.first - 1 || hour == run.last + 1 ->
                if (run.count() < MaxHours) { chosenStart = minOf(run.first, hour); chosenHours = run.count() + 1 }
                else overLimit = true
            // Anywhere else starts a new run there.
            else -> { chosenStart = hour; chosenHours = 1 }
        }
    }

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
                FieldRow(Icons.Rounded.CalendarMonth, "Date") {
                    FieldValue("${day.label()} ${day.year}")
                }
                // The same Today / Tomorrow / Other chips as the Quick view, across the card.
                Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    QuickDayChips(day, otherOpen = showCalendar,
                        onPick = { savedDay = it; showCalendar = false },
                        onOther = {
                            if (!showCalendar) shownMonth = BookingDay(day.year, day.month, 1)
                            showCalendar = !showCalendar
                        })
                }
                RevealSection(showCalendar) {
                    MonthCalendar(
                        month = shownMonth, today = today, selected = day,
                        onMonthChange = { shownMonth = it },
                        onSelect = { savedDay = it; showCalendar = false }
                    )
                }
                FieldDividerLine()
                FieldRow(Icons.Rounded.Schedule, "Time") {
                    if (selected != null) FieldValue("${hourLabel(selected.first)} – ${hourLabel(selected.last + 1)}")
                    else FieldHint("Pick one or more slots")
                }
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    if (free.isEmpty()) {
                        Text("No free times on this day. Try another date.",
                            fontFamily = DMSans, fontSize = 14.sp, lineHeight = 19.sp, color = FacilityOrange)
                    } else {
                        PeriodToggle({ periodPosition }) { period = it }
                        Spacer(Modifier.height(12.dp))
                        Crossfade(period, label = "periodSlots") { shown ->
                            SlotGrid(shown.hours, states, selected, ::onSlot)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (overLimit) "Bookings are up to $MaxHours hours" else "Tap up to $MaxHours back-to-back slots",
                            fontFamily = DMSans, fontSize = 13.sp, color = if (overLimit) FacilityOrange else FacilityMuted
                        )
                    }
                }
                FieldDividerLine()
                // Set by the slots: one hour each.
                FieldRow(Icons.Rounded.HourglassBottom, "Duration") {
                    if (selected != null) FieldValue(durationLabel(selected.count()))
                    else FieldHint("From the slots you pick")
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
        ContinueButton(
            enabled = selected != null,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) { selected?.let { onCheck(facility, day, it.first, guestCount, it.count()) } }
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

/** One line of the booking card: a round icon, an optional small [label] over [content], and an optional [trailing]. */
@Composable
private fun FieldRow(
    icon: ImageVector, label: String?,
    iconTint: Color = if (NavyGoldTheme) NavyGoldGold else FacilityGreen,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
        trailing?.invoke(this)
    }
}

@Composable
private fun FieldValue(text: String) {
    Text(text, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FacilityInk, maxLines = 1)
}

/** Stands in for a [FieldValue] until there is one. */
@Composable
private fun FieldHint(text: String) {
    Text(text, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = FacilityMuted, maxLines = 1)
}

@Composable
private fun FieldDividerLine() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FieldDivider)
}

/**
 * The calendar under the date chips. It appears at full height and only fades, then scrolls into
 * view, so it doesn't open out of sight.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RevealSection(visible: Boolean, content: @Composable () -> Unit) {
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
        Box(Modifier.bringIntoViewRequester(requester).padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) { content() }
    }
}

private const val GridColumns = 4
private val ChipHeight = 46.dp
private val ChipGap = 8.dp
private val GridRows = SlotPeriod.entries.maxOf { (it.hours.count() + GridColumns - 1) / GridColumns }

/**
 * One half of the day's hourly slots, four to a row. Each half is as tall as the longest, so the
 * card doesn't jump when switching. Past and booked-out slots can't be tapped; full ones are struck
 * through, and an orange dot marks one with few places left.
 */
@Composable
private fun SlotGrid(hours: IntRange, states: Map<Int, SlotState>, selected: IntRange?, onTap: (Int) -> Unit) {
    Column(
        Modifier.heightIn(min = ChipHeight * GridRows + ChipGap * (GridRows - 1)),
        verticalArrangement = Arrangement.spacedBy(ChipGap)
    ) {
        hours.chunked(GridColumns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
                row.forEach { hour ->
                    TimeChip(hour, states[hour] ?: SlotState.FULL, selected?.contains(hour) == true, Modifier.weight(1f)) { onTap(hour) }
                }
                repeat(GridColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TimeChip(hour: Int, state: SlotState, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val enabled = state == SlotState.OPEN || state == SlotState.FEW_LEFT
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.height(ChipHeight)
            .then(if (selected) Modifier.shadow(3.dp, shape, ambientColor = FieldShadow, spotColor = FieldShadow) else Modifier)
            .clip(shape)
            .background(when {
                selected -> ChosenFill
                enabled -> SolidColor(Color.White)
                else -> SolidColor(FacilityMuted.copy(alpha = .08f))
            })
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ChosenEdge else FieldDivider, shape)
            // Slots toggle on and off, since several can be chosen.
            .toggleable(selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() }),
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
            textDecoration = if (state == SlotState.FULL) TextDecoration.LineThrough else null
        )
        if (state == SlotState.FEW_LEFT && !selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 6.dp).size(6.dp).clip(CircleShape).background(FacilityOrange))
        }
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

/** Navy with a gold edge in Theme 4, the booking green otherwise; greyed until a slot is chosen. */
@Composable
private fun ContinueButton(enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
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
        Text("Continue", fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = tint)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}
