package com.example.greengate

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greengate.ui.theme.DMSans
import java.util.Calendar

private val CabFieldShape = RoundedCornerShape(14.dp)
private val CabFieldBorder = Color(0xFFE3ECE9)
private val CabSelectedFill = Color(0xFFE7F6F0)
private val CabErrorRed = Color(0xFFC2412D)

// TODO: replace the logos cut from the mock-up with the companies' official artwork.
private enum class CabCompany(val label: String, @DrawableRes val logo: Int?) {
    GRAB("Grab", R.drawable.cab_co_grab),
    GOJEK("Gojek", R.drawable.cab_co_gojek),
    TADA("TADA", R.drawable.cab_co_tada),
    RYDE("Ryde", R.drawable.cab_co_ryde),
    CDG("CDG Zig", R.drawable.cab_co_cdg),
    OTHER("Other", null),
}

private val ValidForOptions = listOf(1 to "1 Hour", 2 to "2 Hours", 3 to "3 Hours", 4 to "4 Hours")
private val ValidityOptions = listOf("1 Week", "2 Weeks", "1 Month", "3 Months", "6 Months", "1 Year")
private val EntriesPerDay = listOf<Pair<Int?, String>>(1 to "One Entry", 2 to "Two Entries", 3 to "Three Entries", null to "Unlimited")
private val WeekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
// Calendar.DAY_OF_WEEK for each of [WeekDays].
private val WeekDayNumbers = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
    Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
// Half hours across the day; a slot can end at 11:59 PM.
private val SlotStarts = (0..47).map { it * 30 }
private val SlotEnds = (1..47).map { it * 30 } + (24 * 60 - 1)

private fun nowMinutes() = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

private fun Calendar.toBookingDay() = BookingDay(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))
internal fun dayFromToday(offset: Int) = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offset) }.toBookingDay()

/** Start times for [day]: right now first when it's today, then the half hours still ahead. */
private fun cabStartTimes(day: BookingDay): List<Int> {
    if (day != BookingDay.today()) return SlotStarts
    val now = nowMinutes()
    return listOf(now) + SlotStarts.filter { it > now }
}

/**
 * Invite Cab, shown in place of the visitor types inside the Create Invite sheet. "Once" covers a
 * single pickup or drop; "Frequently" repeats on chosen weekdays. [onCreated] receives the saved invite.
 */
@Composable
internal fun CabInviteForm(onClose: () -> Unit, onCreated: (VisitorInvite) -> Unit) {
    var frequent by remember { mutableStateOf(false) }
    var pickup by remember { mutableStateOf(true) }
    var day by remember { mutableStateOf(BookingDay.today()) }
    val startTimes = cabStartTimes(day)
    var startMinutes by remember { mutableIntStateOf(startTimes.first()) }
    if (startMinutes !in startTimes) startMinutes = startTimes.first()
    var validHours by remember { mutableIntStateOf(1) }
    var days by remember { mutableStateOf(setOf(0)) }
    var validity by remember { mutableStateOf("6 Months") }
    var slotStart by remember { mutableIntStateOf(0) }
    var slotEnd by remember { mutableIntStateOf(24 * 60 - 1) }
    var entries by remember { mutableStateOf(EntriesPerDay.first()) }
    var vehicle by remember { mutableStateOf("") }
    var company by remember { mutableStateOf(CabCompany.GRAB) }
    var advanced by remember { mutableStateOf(false) }
    var notifyOnEntry by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val daysError = if (submitted && frequent && days.isEmpty()) "Pick at least one day" else null
    val slotError = if (submitted && frequent && slotEnd <= slotStart) "End time must be after the start time" else null

    if (pickDate) SelectDateSheet(day, onDismiss = { pickDate = false }) { day = it; pickDate = false }

    fun create() {
        submitted = true
        if (frequent && (days.isEmpty() || slotEnd <= slotStart)) return
        // A recurring invite starts on the next chosen weekday, today included.
        val firstDay = if (!frequent) day else (0..6).map { dayFromToday(it) }.first { d ->
            val dow = d.toCalendar().get(Calendar.DAY_OF_WEEK)
            days.any { WeekDayNumbers[it] == dow }
        }
        val plate = vehicle.trim()
        val invite = VisitorStore.add(VisitorInvite(
            id = "", name = if (company == CabCompany.OTHER) "Cab Driver" else "${company.label} Driver",
            type = InviteType.CAB, relationship = if (pickup) "Pickup" else "Drop",
            countryCode = "", mobile = "", day = firstDay,
            startMinutes = if (frequent) slotStart else startMinutes,
            validHours = if (frequent) ((slotEnd - slotStart) + 59) / 60 else validHours,
            vehicleRegion = "SG".takeIf { plate.isNotEmpty() }, vehicle = plate.ifEmpty { null },
            visitorCanEditVehicle = false, faceRegistration = false,
            repeat = if (frequent) "${days.sorted().joinToString(", ") { WeekDays[it] }} · $validity" else null,
            notifyOnEntry = notifyOnEntry, maxEntries = if (frequent) entries.first else 1,
            note = note.trim(), createdAt = System.currentTimeMillis()
        ))
        onCreated(invite)
    }

    Column(Modifier.fillMaxWidth().imePadding()) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.cab_invite_header), null, Modifier.size(60.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Invite Cab", fontFamily = DMSans, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                Text("Create a cab invite for pickup or drop", fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Close", tint = FacilityInk, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 20.dp)) {
            listOf(false to "Once", true to "Frequently").forEach { (value, label) ->
                val selected = frequent == value
                Column(Modifier.weight(1f).clickable(role = Role.Tab) { frequent = value; submitted = false },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, Modifier.padding(vertical = 10.dp), fontFamily = DMSans, fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) BookingsDeepGreen else FacilityMuted)
                    Box(Modifier.fillMaxWidth().height(if (selected) 2.5.dp else 1.dp)
                        .background(if (selected) BookingsDeepGreen else CabFieldBorder))
                }
            }
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (!frequent) {
                CabLabel("Invite for")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DirectionChip("Pickup", Icons.Rounded.ArrowUpward, pickup, Modifier.weight(1f)) { pickup = true }
                    DirectionChip("Drop", Icons.Rounded.ArrowDownward, !pickup, Modifier.weight(1f)) { pickup = false }
                }
                CabLabel("Date")
                CabField(Icons.Outlined.CalendarMonth, onClick = { pickDate = true }, trailing = Icons.Rounded.ChevronRight, height = 60.dp) {
                    Column {
                        Text(quickDayName(day) ?: day.label(), fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
                        Text("${day.label()} ${day.year}", fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        CabLabel("Start Time")
                        CabDropdown(Icons.Outlined.Schedule, clockLabel(startMinutes), startTimes.map { clockLabel(it) }) {
                            startMinutes = startTimes[it]
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        CabLabel("Valid for next")
                        CabDropdown(Icons.Outlined.Schedule, ValidForOptions.first { it.first == validHours }.second,
                            ValidForOptions.map { it.second }) { validHours = ValidForOptions[it].first }
                    }
                }
            } else {
                CabLabel("Select Days of Week")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WeekDays.forEachIndexed { i, name ->
                        val selected = i in days
                        Box(
                            Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (selected) BookingsDeepGreen else Color(0xFFF2F5F4))
                                .toggleable(selected, role = Role.Checkbox) { days = if (it) days + i else days - i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, fontFamily = DMSans, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Color.White else FacilityInk)
                        }
                    }
                }
                CabError(daysError)
                CabLabel("Select Validity")
                CabDropdown(Icons.Outlined.CalendarMonth, validity, ValidityOptions) { validity = ValidityOptions[it] }
                CabLabel("Select Time Slot")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CabDropdown(Icons.Outlined.Schedule, clockLabel(slotStart), SlotStarts.map { clockLabel(it) }, Modifier.weight(1f)) {
                        slotStart = SlotStarts[it]
                    }
                    CabDropdown(Icons.Outlined.Schedule, clockLabel(slotEnd), SlotEnds.map { clockLabel(it) }, Modifier.weight(1f)) {
                        slotEnd = SlotEnds[it]
                    }
                }
                CabError(slotError)
                CabLabel("Select Entries Per Day")
                CabDropdown(Icons.Rounded.Repeat, entries.second, EntriesPerDay.map { it.second }) { entries = EntriesPerDay[it] }
            }

            Row(Modifier.padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Vehicle Number", fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
                Icon(Icons.Outlined.Info, null, Modifier.padding(start = 6.dp).size(16.dp), tint = FacilityMuted)
            }
            CabField(Icons.Outlined.DirectionsCar) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (vehicle.isEmpty()) Text("SMC 1234 A", fontFamily = DMSans, fontSize = 15.sp, color = FacilityMuted.copy(alpha = .6f))
                    BasicTextField(
                        vehicle, { vehicle = it.uppercase().filter { c -> c.isLetterOrDigit() || c == ' ' }.take(12) },
                        Modifier.fillMaxWidth().padding(end = 30.dp), singleLine = true,
                        textStyle = TextStyle(fontFamily = DMSans, fontSize = 15.sp, color = FacilityInk),
                        cursorBrush = SolidColor(BookingsDeepGreen),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done)
                    )
                    if (vehicle.isNotEmpty()) Icon(Icons.Rounded.Cancel, "Clear vehicle number",
                        Modifier.align(Alignment.CenterEnd).size(22.dp).clip(CircleShape).clickable { vehicle = "" },
                        tint = Color(0xFFC5CFCC))
                }
            }
            Text("Singapore vehicle format (e.g. SMC 1234 A). Format can be configured later.",
                Modifier.padding(top = 6.dp, start = 2.dp), fontFamily = DMSans, fontSize = 11.sp, lineHeight = 15.sp, color = FacilityMuted)

            CabLabel("Company Name")
            CabDropdown(Icons.Outlined.DirectionsCar, company.label, CabCompany.entries.map { it.label }) { company = CabCompany.entries[it] }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CabCompany.entries.filter { it.logo != null }.forEach { co ->
                    CompanyChip(co, co == company, Modifier.weight(1f)) { company = co }
                }
            }

            if (!frequent) {
                Text(if (advanced) "Hide advanced options" else "Advanced options »",
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp).clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) { advanced = !advanced }.padding(6.dp),
                    fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BookingsDeepGreen,
                    textDecoration = TextDecoration.Underline)
            }
            AnimatedVisibility(advanced && !frequent, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp))
                            .toggleable(notifyOnEntry, role = Role.Switch) { notifyOnEntry = it }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Notify me on entry", Modifier.weight(1f), fontFamily = DMSans, fontSize = 15.sp, color = FacilityInk)
                        Switch(notifyOnEntry, onCheckedChange = null, colors = SwitchDefaults.colors(
                            checkedTrackColor = FacilityGreen, checkedThumbColor = Color.White, checkedBorderColor = FacilityGreen,
                            uncheckedTrackColor = Color(0xFFDCE3E1), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color(0xFFDCE3E1)
                        ))
                    }
                    CabLabel("Notes (Optional)")
                    CabField(null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (note.isEmpty()) Text("e.g. pick up at the lobby", fontFamily = DMSans, fontSize = 15.sp, color = FacilityMuted.copy(alpha = .6f))
                            BasicTextField(note, { note = it.take(120) }, Modifier.fillMaxWidth(), singleLine = true,
                                textStyle = TextStyle(fontFamily = DMSans, fontSize = 15.sp, color = FacilityInk),
                                cursorBrush = SolidColor(BookingsDeepGreen),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(
            Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 20.dp).fillMaxWidth().height(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2E9A79), BookingsDeepGreen)))
                .clickable(role = Role.Button) { create() },
            contentAlignment = Alignment.Center
        ) {
            Text("Create Invite", fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

private fun quickDayName(day: BookingDay) = when (day) {
    BookingDay.today() -> "Today"
    dayFromToday(1) -> "Tomorrow"
    else -> null
}

@Composable
private fun CabLabel(text: String) {
    Text(text, Modifier.padding(top = 16.dp, bottom = 8.dp), fontFamily = DMSans, fontSize = 14.sp,
        fontWeight = FontWeight.Medium, color = FacilityInk)
}

@Composable
private fun CabError(text: String?) {
    if (text != null) Text(text, Modifier.padding(start = 2.dp, top = 6.dp), fontFamily = DMSans, fontSize = 12.sp, color = CabErrorRed)
}

@Composable
private fun CabField(
    icon: ImageVector?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: ImageVector? = null,
    height: androidx.compose.ui.unit.Dp = 52.dp, content: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().height(height).clip(CabFieldShape).background(Color.White)
            .border(1.dp, CabFieldBorder, CabFieldShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = BookingsDeepGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Box(Modifier.weight(1f)) { content() }
        if (trailing != null) Icon(trailing, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CabDropdown(icon: ImageVector, value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        CabField(icon, onClick = { open = true }, trailing = Icons.Rounded.KeyboardArrowDown) {
            Text(value, fontFamily = DMSans, fontSize = 15.sp, color = FacilityInk, maxLines = 1)
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = Color.White,
            modifier = Modifier.heightIn(max = 320.dp)) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(option, fontFamily = DMSans, fontWeight = if (option == value) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { onSelect(i); open = false }
                )
            }
        }
    }
}

@Composable
private fun DirectionChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(50.dp).clip(CabFieldShape)
            .background(if (selected) CabSelectedFill else Color.White)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) FacilityGreen else CabFieldBorder, CabFieldShape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(FacilityGreen), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
    }
}

@Composable
private fun CompanyChip(company: CabCompany, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(70.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) CabSelectedFill else Color.White)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) FacilityGreen else CabFieldBorder, RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        company.logo?.let { Image(painterResource(it), company.label, Modifier.height(26.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(company.label, fontFamily = DMSans, fontSize = 11.sp, color = FacilityInk, maxLines = 1)
    }
}

/** Today and Tomorrow as one-tap chips; "Other" opens a month calendar for any later date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectDateSheet(current: BookingDay, onDismiss: () -> Unit, onPick: (BookingDay) -> Unit) {
    val today = remember { BookingDay.today() }
    var shownMonth by remember { mutableStateOf(BookingDay(current.year, current.month, 1)) }
    // Start on the calendar only when a later date is already chosen.
    var showCalendar by remember { mutableStateOf(current != today && current != dayFromToday(1)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Select Date", Modifier.weight(1f), fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Close", tint = FacilityInk, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            QuickDayChips(current, otherOpen = showCalendar, onPick = onPick, onOther = { showCalendar = true })
            AnimatedVisibility(showCalendar, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Box(Modifier.padding(top = 16.dp)) {
                    MonthCalendar(month = shownMonth, today = today, selected = current,
                        onMonthChange = { shownMonth = it }, onSelect = onPick)
                }
            }
        }
    }
}
