package com.example.greengate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greengate.ui.theme.DMSans
import java.util.Calendar

/** A calendar day; months are 0-based like [Calendar.MONTH]. */
internal data class BookingDay(val year: Int, val month: Int, val day: Int) : Comparable<BookingDay> {
    override fun compareTo(other: BookingDay) = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    fun toCalendar(): Calendar = Calendar.getInstance().apply { clear(); set(year, month, day) }

    /** e.g. "Thu, 12 Mar". */
    fun label() = "${weekday().take(3)}, ${shortLabel()}"

    /** e.g. "12 Mar". */
    fun shortLabel() = "$day ${MonthNames[month].take(3)}"

    /** e.g. "Saturday, 21 March 2026". */
    fun longLabel() = "${weekday()}, $day ${MonthNames[month]} $year"

    private fun weekday() = WeekdayNames[toCalendar().get(Calendar.DAY_OF_WEEK) - 1]

    companion object {
        fun today(): BookingDay = Calendar.getInstance().let {
            BookingDay(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH))
        }
    }
}

private val MonthNames = listOf("January", "February", "March", "April", "May", "June", "July",
    "August", "September", "October", "November", "December")
private val WeekdayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

// Hourly slots from 7 AM; the last one starts at 9 PM.
private val SlotHours = 7..21

private enum class SlotState { OPEN, FEW_LEFT, FULL, PAST }

internal fun hourLabel(hour: Int) = "${(hour + 11) % 12 + 1}:00 ${if (hour < 12) "AM" else "PM"}"

/**
 * Placeholder availability until a bookings API exists: stable per facility, day and hour,
 * and busier for facilities already flagged as nearly full.
 */
private fun slotState(facility: String, day: BookingDay, hour: Int, busy: Boolean, today: BookingDay, nowHour: Int): SlotState {
    if (day == today && hour <= nowHour) return SlotState.PAST
    val roll = Math.floorMod((facility.hashCode() * 31 + day.hashCode()) * 17 + hour * 7919, 100)
    return when {
        roll < if (busy) 40 else 18 -> SlotState.FULL
        roll < if (busy) 60 else 30 -> SlotState.FEW_LEFT
        else -> SlotState.OPEN
    }
}

/**
 * Drops down under a facility card: pick a future date, then the calendar slides away for
 * that day's time slots. [onBooked] receives the day, the slot's start hour and the guest count.
 */
@Composable
internal fun FacilityBookingPanel(
    facilityName: String,
    busy: Boolean,
    maxGuests: Int,
    // The facility's icon, shown beside the chosen date and time.
    glyph: @Composable () -> Unit,
    onBooked: (BookingDay, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { BookingDay.today() }
    var shownMonth by remember { mutableStateOf(BookingDay(today.year, today.month, 1)) }
    var selectedDay by remember { mutableStateOf<BookingDay?>(null) }
    var showSlots by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xE6FFFFFF), Color(0xCCEFF8F5))))
            .border(1.5.dp, FacilityGlassBorder, shape)
            .padding(16.dp)
    ) {
        AnimatedContent(
            targetState = showSlots,
            transitionSpec = {
                val dir = if (targetState) 1 else -1
                (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
            },
            label = "bookingStep"
        ) { slots ->
            val day = selectedDay
            if (slots && day != null) {
                SlotPicker(facilityName, day, busy, today, maxGuests, glyph, onBack = { showSlots = false },
                    onBooked = { hour, guests -> onBooked(day, hour, guests) })
            } else {
                MonthCalendar(
                    month = shownMonth, today = today, selected = selectedDay,
                    onMonthChange = { shownMonth = it },
                    onSelect = { selectedDay = it; showSlots = true }
                )
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: BookingDay, today: BookingDay, selected: BookingDay?,
    onMonthChange: (BookingDay) -> Unit, onSelect: (BookingDay) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${MonthNames[month.month]} ${month.year}", Modifier.weight(1f),
                fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
            // Bookings can't be made in the past, so there's nothing to see before this month.
            val canGoBack = month.year > today.year || (month.year == today.year && month.month > today.month)
            MonthArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", canGoBack) {
                onMonthChange(month.toCalendar().apply { add(Calendar.MONTH, -1) }.toDay())
            }
            Spacer(Modifier.width(8.dp))
            MonthArrow(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", true) {
                onMonthChange(month.toCalendar().apply { add(Calendar.MONTH, 1) }.toDay())
            }
        }
        Spacer(Modifier.height(14.dp))
        Row {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, Modifier.weight(1f), fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(6.dp))
        // Always six weeks, padded with the neighbouring months' days, so the panel never jumps.
        val start = month.toCalendar().apply { add(Calendar.DAY_OF_MONTH, 1 - get(Calendar.DAY_OF_WEEK)) }
        repeat(6) {
            Row {
                repeat(7) {
                    val day = start.toDay()
                    DayCell(
                        day = day,
                        inMonth = day.month == month.month,
                        isPast = day < today,
                        isToday = day == today,
                        isSelected = day == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(day) }
                    )
                    start.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
    }
}

private fun Calendar.toDay() = BookingDay(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))

@Composable
private fun MonthArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(Color(0xB3FFFFFF))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else .35f),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = FacilityInk, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun DayCell(
    day: BookingDay, inMonth: Boolean, isPast: Boolean, isToday: Boolean, isSelected: Boolean,
    modifier: Modifier, onClick: () -> Unit
) {
    val selectable = inMonth && !isPast
    Box(modifier.height(52.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(16.dp))
                .then(if (isSelected) Modifier.background(Brush.verticalGradient(listOf(Color(0xFF2AA07D), FacilityGreen))) else Modifier)
                .clickable(enabled = selectable, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${day.day}", fontFamily = DMSans, fontSize = 17.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isSelected -> Color.White
                    !inMonth -> FacilityMuted.copy(alpha = .35f)
                    isPast -> FacilityMuted.copy(alpha = .45f)
                    else -> FacilityInk
                }
            )
            if (isToday || isSelected) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).size(5.dp).clip(CircleShape)
                    .background(if (isSelected) Color.White else FacilityGreen))
            }
        }
    }
}

@Composable
private fun SlotPicker(
    facilityName: String, day: BookingDay, busy: Boolean, today: BookingDay, maxGuests: Int,
    glyph: @Composable () -> Unit, onBack: () -> Unit, onBooked: (hour: Int, guests: Int) -> Unit
) {
    var selectedHour by remember(day) { mutableStateOf<Int?>(null) }
    var guests by remember { mutableIntStateOf(1) }
    val nowHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back to calendar", true, onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(day.label(), fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                Text("Choose a time slot", fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(FacilityGreen, "Selected")
            LegendDot(FacilityOrange, "Few left")
            LegendDot(FacilityMuted.copy(alpha = .4f), "Full")
        }
        Spacer(Modifier.height(12.dp))
        SlotHours.chunked(3).forEach { row ->
            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { hour ->
                    val state = slotState(facilityName, day, hour, busy, today, nowHour)
                    SlotChip(hour, state, selected = hour == selectedHour, Modifier.weight(1f)) { selectedHour = hour }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Guests", fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                Text("Up to $maxGuests pax", fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
            }
            StepButton("−", "Fewer guests", guests > 1) { guests-- }
            Text("$guests", Modifier.widthIn(min = 40.dp), fontFamily = DMSans, fontSize = 18.sp,
                fontWeight = FontWeight.Bold, color = FacilityInk, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            StepButton("+", "More guests", guests < maxGuests) { guests++ }
        }
        Spacer(Modifier.height(12.dp))
        SelectionBar(day, selectedHour, glyph) { hour -> onBooked(hour, guests) }
    }
}

/** Glass bar under the slots: the facility icon, the chosen date then time, and Continue. */
@Composable
private fun SelectionBar(day: BookingDay, hour: Int?, glyph: @Composable () -> Unit, onContinue: (Int) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Color(0xCCFFFFFF))
            .border(1.5.dp, FacilityGlassBorder, shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color.White, Color(0xFFE3F5EE))))
                .border(1.dp, FacilityGlassBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) { glyph() }
        Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(44.dp).background(FacilityMuted.copy(alpha = .2f)))
        Column(Modifier.weight(1f)) {
            Text(if (hour != null) "Selected" else "No slot yet", fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
            // Date over time: on one line the pair doesn't fit beside the Continue button.
            Text(
                if (hour != null) day.shortLabel() else "Pick a time",
                fontFamily = DMSans, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = FacilityInk, maxLines = 1
            )
            if (hour != null) Text(
                hourLabel(hour), fontFamily = DMSans, fontSize = 16.sp, lineHeight = 20.sp,
                fontWeight = FontWeight.Bold, color = FacilityInk, maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.height(50.dp).clip(RoundedCornerShape(18.dp))
                .background(if (hour != null) Color(0xFF8EF0CF) else FacilityMuted.copy(alpha = .18f))
                .clickable(enabled = hour != null, role = Role.Button) { hour?.let(onContinue) }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = if (hour != null) FacilityInk else FacilityMuted
            Text("Continue", fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = tint)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(Color(0xE6FFFFFF))
            .border(1.dp, FacilityGlassBorder, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick)
            .alpha(if (enabled) 1f else .35f),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FacilityGreen)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
    }
}

@Composable
private fun SlotChip(hour: Int, state: SlotState, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val enabled = state == SlotState.OPEN || state == SlotState.FEW_LEFT
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.height(48.dp).clip(shape)
            .background(when {
                selected -> FacilityGreen
                enabled -> Color(0xE6FFFFFF)
                else -> Color(0x66FFFFFF)
            })
            .border(1.dp, if (selected) FacilityGreen else FacilityGlassBorder, shape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            hourLabel(hour), fontFamily = DMSans, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                selected -> Color.White
                enabled -> FacilityInk
                else -> FacilityMuted.copy(alpha = .45f)
            },
            textDecoration = if (state == SlotState.FULL) TextDecoration.LineThrough else null
        )
        if (state == SlotState.FEW_LEFT && !selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 7.dp).size(6.dp).clip(CircleShape).background(FacilityOrange))
        }
    }
}
