package com.example.greengate

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans
import java.util.Calendar
import java.util.TimeZone

private enum class Relationship { Family, Friend, Guest }

private val CountryCodes = listOf("+65", "+60", "+62", "+63", "+66", "+91", "+44", "+1")
private val ValidityOptions = listOf(1 to "1 Hour", 2 to "2 Hours", 4 to "4 Hours", 8 to "8 Hours", 24 to "Full Day")

private val FieldShape = RoundedCornerShape(16.dp)
private val FieldBorder = Color(0xFFE3ECE9)
private val ErrorRed = Color(0xFFC2412D)

private enum class InvitePage { BASIC, OPTIONS, REVIEW }

// Half-hourly start times from 6:00 AM to 11:30 PM, in minutes after midnight.
private val StartTimes = (12..47).map { it * 30 }

/** Start times still ahead on [day]; the current half hour counts, so "now" can be picked. */
private fun startTimesFor(day: BookingDay): List<Int> {
    if (day != BookingDay.today()) return StartTimes
    val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
    return StartTimes.filter { it >= now / 30 * 30 }.ifEmpty { listOf(StartTimes.last()) }
}

/** Family / Friend invite: basic details, Additional Options, then a review before creating it. */
@Composable
internal fun NewVisitorInviteScreen(navController: NavController) {
    var name by rememberSaveable { mutableStateOf("") }
    var countryCode by rememberSaveable { mutableStateOf(CountryCodes.first()) }
    var mobile by rememberSaveable { mutableStateOf("") }
    var relationship by rememberSaveable { mutableStateOf(Relationship.Family) }
    var dayKey by rememberSaveable { mutableStateOf(BookingDay.today().let { listOf(it.year, it.month, it.day) }) }
    val day = BookingDay(dayKey[0], dayKey[1], dayKey[2])
    val startTimes = startTimesFor(day)
    var startMinutes by rememberSaveable { mutableIntStateOf(startTimesFor(BookingDay.today()).first()) }
    // A date change can leave the chosen time in the past; snap to the first one still open.
    if (startMinutes !in startTimes) startMinutes = startTimes.first()
    var validHours by rememberSaveable { mutableIntStateOf(2) }
    var pickDate by remember { mutableStateOf(false) }
    var switchType by remember { mutableStateOf(false) }
    // All pages live here so nothing typed is lost moving between them.
    var page by rememberSaveable { mutableStateOf(InvitePage.BASIC) }
    var reviewFrom by rememberSaveable { mutableStateOf(InvitePage.BASIC) }
    var options by remember { mutableStateOf(InviteOptions()) }
    // Errors appear only after the first Next, so an untouched form isn't covered in red.
    var submitted by rememberSaveable { mutableStateOf(false) }
    val nameError = if (submitted && name.isBlank()) "Enter the visitor's name" else null
    val mobileDigits = mobile.filter { it.isDigit() }
    val mobileError = if (submitted && mobileDigits.length !in 7..15) "Enter a valid mobile number" else null

    fun draft() = VisitorInvite(
        id = "", name = name.trim(), type = InviteType.FAMILY, relationship = relationship.name,
        countryCode = countryCode, mobile = mobile.trim(), day = day, startMinutes = startMinutes, validHours = validHours,
        vehicleRegion = options.plateRegion.takeIf { options.vehicleAllowed },
        vehicle = options.vehicle.trim().takeIf { options.vehicleAllowed && it.isNotEmpty() },
        visitorCanEditVehicle = options.visitorCanEditVehicle, faceRegistration = options.faceRegistration,
        repeat = options.repeat.takeIf { options.recurring }, notifyOnEntry = options.notifyOnEntry,
        maxEntries = options.maxEntries.toIntOrNull(), note = options.note.trim(), createdAt = System.currentTimeMillis()
    )
    fun back() = when (page) {
        InvitePage.BASIC -> navController.popBackStack().let { }
        InvitePage.OPTIONS -> page = InvitePage.BASIC
        InvitePage.REVIEW -> page = reviewFrom
    }

    BackHandler(enabled = page != InvitePage.BASIC) { back() }
    if (pickDate) InviteDatePicker(day, onDismiss = { pickDate = false }) { picked ->
        dayKey = listOf(picked.year, picked.month, picked.day)
        pickDate = false
    }
    if (switchType) CreateInviteSheet(
        onDismiss = { switchType = false },
        onCreated = {
            switchType = false
            navController.navigate(Screen.InviteCreated.create(it.id)) { popUpTo(Screen.CreateInvite.route) { inclusive = true } }
        }
    ) { type ->
        switchType = false
        if (type != InviteType.FAMILY) navController.navigate(Screen.CreateInvite.create(type)) {
            popUpTo(Screen.CreateInvite.route) { inclusive = true }
        }
    }

    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding().imePadding()) {
            GlassTopBar(
                when (page) {
                    InvitePage.BASIC -> "New Visitor Invite"
                    InvitePage.OPTIONS -> "Additional Options"
                    InvitePage.REVIEW -> "Review Invite"
                },
                onBack = { back() },
                action = if (page == InvitePage.REVIEW) ({
                    Text("Edit", Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { page = InvitePage.BASIC }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                        fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FacilityGreen,
                        textDecoration = TextDecoration.Underline)
                }) else null
            )
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "invitePage"
            ) { shown ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    when (shown) {
                        InvitePage.OPTIONS -> AdditionalOptions(relationship, { relationship = it }, options) { options = it }
                        InvitePage.REVIEW -> InviteReview(draft(), options)
                        InvitePage.BASIC -> {
                            TypeHeader(InviteType.FAMILY) { switchType = true }
                            FieldLabel("Visitor Name *")
                            InputField(name, { name = it.take(60) }, "e.g. John Lim", nameError,
                                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
                            FieldLabel("Mobile Number *")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DropdownField(countryCode, CountryCodes, { countryCode = it }, Modifier.width(96.dp))
                                InputField(mobile, { mobile = it.filter { c -> c.isDigit() || c == ' ' }.take(18) }, "8123 4567",
                                    mobileError, KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done), Modifier.weight(1f))
                            }
                            FieldLabel("When")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    Modifier.weight(1.2f).height(54.dp).clip(FieldShape).background(Color.White)
                                        .border(1.dp, FieldBorder, FieldShape).clickable(role = Role.Button) { pickDate = true }
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dayLabel(day), Modifier.weight(1f), fontFamily = DMSans, fontSize = 16.sp, color = FacilityInk, maxLines = 1)
                                    Icon(Icons.Outlined.CalendarMonth, "Choose date", tint = FacilityInk, modifier = Modifier.size(22.dp))
                                }
                                DropdownField(clockLabel(startMinutes), startTimes.map { clockLabel(it) },
                                    { label -> startMinutes = startTimes.first { clockLabel(it) == label } }, Modifier.weight(1f))
                            }
                            FieldLabel("Valid for")
                            DropdownField(
                                ValidityOptions.first { it.first == validHours }.second, ValidityOptions.map { it.second },
                                { label -> validHours = ValidityOptions.first { it.second == label }.first }, Modifier.fillMaxWidth()
                            )
                            Row(
                                Modifier.padding(top = 18.dp).clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button) { page = InvitePage.OPTIONS }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Advanced options", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                                Icon(Icons.Rounded.ChevronRight, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            Row(
                Modifier.padding(16.dp).fillMaxWidth().height(58.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x330F6B54), spotColor = Color(0x330F6B54))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1D8A6A), BookingsDeepGreen)))
                    .clickable(role = Role.Button) {
                        if (page == InvitePage.REVIEW) {
                            // TODO: create the invite through the visitor API once it exists.
                            val invite = VisitorStore.add(draft())
                            navController.navigate(Screen.InviteCreated.create(invite.id)) {
                                popUpTo(Screen.CreateInvite.route) { inclusive = true }
                            }
                            return@clickable
                        }
                        submitted = true
                        if (name.isNotBlank() && mobileDigits.length in 7..15) {
                            reviewFrom = page
                            page = InvitePage.REVIEW
                        } else {
                            // The missing details are on the first page; take the resident back to them.
                            page = InvitePage.BASIC
                        }
                    }
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                Text(if (page == InvitePage.REVIEW) "Create Invite" else "Next",
                    fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun InviteReview(invite: VisitorInvite, options: InviteOptions) {
    Spacer(Modifier.height(4.dp))
    GlassCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InviteAvatar(invite, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(invite.name, fontFamily = DMSans, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                Text(invite.relationship, fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        ReviewLine("Mobile Number", invite.mobileLabel)
        ReviewLine("Date", "${invite.day.label()} ${invite.day.year}")
        ReviewLine("Time", invite.timeRange)
        ReviewLine("Valid For", invite.validForLabel)
        ReviewLine("Vehicle", invite.vehicleLabel)
        ReviewLine("Allow Vehicle Update", if (options.visitorCanEditVehicle) "Yes (Approval)" else "No")
        ReviewLine("Face Registration", if (options.faceRegistration) "Yes" else "No")
        ReviewLine("Notify me on entry", if (options.notifyOnEntry) "Yes" else "No")
        ReviewLine("Recurring", invite.repeat ?: "No")
        ReviewLine("Maximum entries", options.maxEntries)
        ReviewLine("Notes", invite.note.ifBlank { "-" }, last = true)
    }
}

@Composable
private fun ReviewLine(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
        Text(value, Modifier.weight(1.2f), fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = FacilityInk, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
    if (!last) HorizontalDivider(color = BookingsDivider)
}

private val PlateRegions = listOf("SG", "MY", "Other")
private val MaxEntryOptions = listOf("Unlimited", "1", "2", "3", "5", "10")
private val RepeatOptions = listOf("Daily", "Weekdays", "Weekly")

/** Everything on the Additional Options page; defaults follow the design. */
private data class InviteOptions(
    val vehicleAllowed: Boolean = true,
    val plateRegion: String = PlateRegions.first(),
    val vehicle: String = "",
    val visitorCanEditVehicle: Boolean = true,
    val faceRegistration: Boolean = true,
    val recurring: Boolean = false,
    val repeat: String = RepeatOptions.last(),
    val notifyOnEntry: Boolean = true,
    val maxEntries: String = MaxEntryOptions.first(),
    val note: String = "",
)

@Composable
private fun AdditionalOptions(
    relationship: Relationship, onRelationshipChange: (Relationship) -> Unit,
    options: InviteOptions, onChange: (InviteOptions) -> Unit
) {
    FieldLabel("Relationship")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Relationship.entries.forEach { r ->
            RelationshipChip(r.name, relationship == r, Modifier.weight(1f)) { onRelationshipChange(r) }
        }
    }
    Spacer(Modifier.height(16.dp))
    GlassCard(padding = 14.dp) {
        OptionToggle(null, "Allow vehicle number (optional)", null, options.vehicleAllowed) { onChange(options.copy(vehicleAllowed = it)) }
        AnimatedVisibility(options.vehicleAllowed, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(Modifier.padding(top = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownField(options.plateRegion, PlateRegions, { onChange(options.copy(plateRegion = it)) }, Modifier.width(100.dp))
                InputField(options.vehicle, { onChange(options.copy(vehicle = it.uppercase().filter { c -> c.isLetterOrDigit() || c == ' ' }.take(12))) },
                    "SMC 1234A", null,
                    KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done), Modifier.weight(1f))
            }
        }
        OptionDivider()
        OptionToggle(Icons.Outlined.DirectionsCar, "Allow visitor to add/update vehicle", "Changes require your approval",
            options.visitorCanEditVehicle) { onChange(options.copy(visitorCanEditVehicle = it)) }
        OptionDivider()
        OptionToggle(Icons.Outlined.Face, "Allow Face Registration", "Visitor can register face via link/QR code",
            options.faceRegistration) { onChange(options.copy(faceRegistration = it)) }
        OptionDivider()
        OptionToggle(Icons.Outlined.EventRepeat, "Recurring Invite", null, options.recurring) { onChange(options.copy(recurring = it)) }
        AnimatedVisibility(options.recurring, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text("Repeats", Modifier.padding(bottom = 6.dp), fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
                DropdownField(options.repeat, RepeatOptions, { onChange(options.copy(repeat = it)) }, Modifier.fillMaxWidth())
            }
        }
        OptionDivider()
        OptionToggle(Icons.Outlined.NotificationsActive, "Notify me on entry", null, options.notifyOnEntry) { onChange(options.copy(notifyOnEntry = it)) }
        FieldLabel("Maximum entries")
        DropdownField(options.maxEntries, MaxEntryOptions, { onChange(options.copy(maxEntries = it)) }, Modifier.fillMaxWidth())
        FieldLabel("Notes (Optional)")
        InputField(options.note, { onChange(options.copy(note = it.take(120))) }, "e.g. visiting for family gathering", null,
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done))
    }
}

@Composable
private fun OptionToggle(icon: ImageVector?, title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
            if (subtitle != null) Text(subtitle, fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
        }
        // The row handles the toggle, so the switch itself stays passive for accessibility.
        Switch(checked, onCheckedChange = null, colors = SwitchDefaults.colors(
            checkedTrackColor = FacilityGreen, checkedThumbColor = Color.White, checkedBorderColor = FacilityGreen,
            uncheckedTrackColor = Color(0xFFDCE3E1), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color(0xFFDCE3E1)
        ))
    }
}

@Composable
private fun OptionDivider() = HorizontalDivider(color = BookingsDivider)

private fun dayLabel(day: BookingDay): String {
    val today = BookingDay.today()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        .let { BookingDay(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH)) }
    return when (day) {
        today -> "Today"
        tomorrow -> "Tomorrow"
        else -> "${day.label()} ${day.year}"
    }
}

@Composable
private fun TypeHeader(type: InviteType, onClick: () -> Unit) {
    GlassCard(Modifier.clickable(role = Role.Button, onClick = onClick), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp)) {
                Image(painterResource(R.drawable.ic_clay_community_man), null, Modifier.size(58.dp))
                Image(painterResource(R.drawable.ic_clay_community_heart), null,
                    Modifier.align(Alignment.BottomEnd).size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(type.title, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                Text(type.subtitle, fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
            }
            Icon(Icons.Rounded.KeyboardArrowDown, "Change visitor type", tint = FacilityInk, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, Modifier.padding(top = 16.dp, bottom = 8.dp), fontFamily = DMSans, fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold, color = FacilityInk)
}

@Composable
private fun InputField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, error: String?,
    keyboardOptions: KeyboardOptions, modifier: Modifier = Modifier
) {
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().height(54.dp).clip(FieldShape).background(Color.White)
                .border(if (error != null) 1.5.dp else 1.dp, if (error != null) ErrorRed else FieldBorder, FieldShape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) Text(placeholder, fontFamily = DMSans, fontSize = 16.sp, color = FacilityMuted.copy(alpha = .6f))
            BasicTextField(
                value, onValueChange, Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(fontFamily = DMSans, fontSize = 16.sp, color = FacilityInk),
                cursorBrush = SolidColor(BookingsDeepGreen), keyboardOptions = keyboardOptions
            )
        }
        if (error != null) Text(error, Modifier.padding(start = 4.dp, top = 4.dp), fontFamily = DMSans, fontSize = 12.sp, color = ErrorRed)
    }
}

@Composable
private fun DropdownField(value: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(FieldShape).background(Color.White)
                .border(1.dp, FieldBorder, FieldShape).clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, Modifier.weight(1f), fontFamily = DMSans, fontSize = 16.sp, color = FacilityInk)
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = Color.White) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontFamily = DMSans, fontWeight = if (option == value) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { onSelect(option); open = false }
                )
            }
        }
    }
}

@Composable
private fun RelationshipChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.height(50.dp)
            .then(if (selected) Modifier.shadow(6.dp, shape, ambientColor = Color(0x330F6B54), spotColor = Color(0x330F6B54)) else Modifier)
            .clip(shape)
            .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF1D8A6A), BookingsDeepGreen))
                else Brush.linearGradient(listOf(Color.White, Color.White)))
            .border(1.dp, if (selected) BookingsDeepGreen else FieldBorder, shape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = DMSans, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else FacilityInk)
    }
}

/** Material date picker that only allows today onwards; shared by the invite forms. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InviteDatePicker(current: BookingDay, onDismiss: () -> Unit, onPick: (BookingDay) -> Unit) {
    // The picker works in UTC midnights.
    fun utcMillis(d: BookingDay) = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(d.year, d.month, d.day) }.timeInMillis
    val todayUtc = utcMillis(BookingDay.today())
    val state = rememberDatePickerState(
        initialSelectedDateMillis = utcMillis(current),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = ms }
                    onPick(BookingDay(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)))
                } ?: onDismiss()
            }) { Text("OK", fontFamily = DMSans, color = BookingsDeepGreen) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", fontFamily = DMSans) } }
    ) {
        DatePicker(state, colors = DatePickerDefaults.colors(
            selectedDayContainerColor = BookingsDeepGreen, todayDateBorderColor = BookingsDeepGreen, todayContentColor = BookingsDeepGreen
        ))
    }
}
