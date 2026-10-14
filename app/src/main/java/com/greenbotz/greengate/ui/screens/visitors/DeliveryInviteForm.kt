package com.greenbotz.greengate.ui.screens.visitors

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.visitor.VisitorInvite
import com.greenbotz.greengate.core.visitor.VisitorStore
import com.greenbotz.greengate.ui.screens.facilities.BookingDay
import com.greenbotz.greengate.ui.screens.facilities.BookingsDivider
import com.greenbotz.greengate.ui.screens.facilities.FacilityGreen
import com.greenbotz.greengate.ui.screens.facilities.FacilityInk
import com.greenbotz.greengate.ui.screens.facilities.FacilityMuted
import com.greenbotz.greengate.ui.screens.facilities.GlassCard
import com.greenbotz.greengate.ui.screens.facilities.clockLabel
import com.greenbotz.greengate.ui.theme.DMSans
import java.util.Calendar

// TODO: replace the logos cut from the mock-up with the partners' official artwork.
private enum class DeliveryPartner(val label: String, @DrawableRes val logo: Int?) {
    GRABFOOD("GrabFood", R.drawable.delivery_co_grabfood),
    FOODPANDA("foodpanda", R.drawable.delivery_co_foodpanda),
    DELIVEROO("Deliveroo", R.drawable.delivery_co_deliveroo),
    AMAZON_FRESH("Amazon Fresh", R.drawable.delivery_co_amazon),
    LALAMOVE("Lalamove", R.drawable.delivery_co_lalamove),
    OTHER("Other", null),
}

private enum class DeliveryPage { FORM, SUMMARY }

private val DeliveryValidFor = listOf(1 to "1 hour", 2 to "2 hours", 3 to "3 hours", 4 to "4 hours")

// Fields come from InviteFields.kt; these style the partner chips and summary.
private val DeliveryBorder get() = InviteFieldBorder
private val DeliverySelectedFill get() = InviteSelectedFill

// Half hours across the day, in minutes after midnight.
private val DeliverySlots = (0..47).map { it * 30 }

/** Start times for [day]: right now first when it's today, then the half hours still ahead. */
private fun deliveryStartTimes(day: BookingDay): List<Int> {
    if (day != BookingDay.today()) return DeliverySlots
    val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
    return listOf(now) + DeliverySlots.filter { it > now }
}

/**
 * Delivery invite, shown in place of the visitor types inside the Create Invite sheet: partner and
 * rider details, then a summary before the invite is created. [onBack] returns to the types.
 */
@Composable
internal fun DeliveryInviteForm(onBack: () -> Unit, onClose: () -> Unit, onCreated: (VisitorInvite) -> Unit) {
    var partner by rememberSaveable { mutableStateOf(DeliveryPartner.GRABFOOD) }
    var name by rememberSaveable { mutableStateOf("") }
    var countryCode by rememberSaveable { mutableStateOf(InviteCountryCodes.first()) }
    var mobile by rememberSaveable { mutableStateOf("") }
    var dayKey by rememberSaveable { mutableStateOf(BookingDay.today().let { listOf(it.year, it.month, it.day) }) }
    val day = BookingDay(dayKey[0], dayKey[1], dayKey[2])
    val startTimes = deliveryStartTimes(day)
    var startMinutes by rememberSaveable { mutableIntStateOf(startTimes.first()) }
    // A date change can leave the chosen time in the past; snap to the first one still open.
    if (startMinutes !in startTimes) startMinutes = startTimes.first()
    var validHours by rememberSaveable { mutableIntStateOf(1) }
    var remarks by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableStateOf(DeliveryPage.FORM) }
    var pickDate by remember { mutableStateOf(false) }
    // Errors appear only after the first Continue, so an untouched form isn't covered in red.
    var submitted by rememberSaveable { mutableStateOf(false) }
    val nameError = if (submitted && name.isBlank()) "Enter the delivery person's name" else null
    val mobileDigits = mobile.filter { it.isDigit() }
    val mobileError = if (submitted && mobileDigits.length !in 7..15) "Enter a valid mobile number" else null

    fun draft() = VisitorInvite(
        id = "", name = name.trim(), type = InviteType.DELIVERY, relationship = partner.label,
        countryCode = countryCode, mobile = mobile.trim(), day = day, startMinutes = startMinutes, validHours = validHours,
        vehicleRegion = null, vehicle = null, visitorCanEditVehicle = false, faceRegistration = false,
        repeat = null, notifyOnEntry = true, maxEntries = 1, note = remarks.trim(), createdAt = System.currentTimeMillis()
    )

    BackHandler(enabled = page == DeliveryPage.SUMMARY) { page = DeliveryPage.FORM }
    if (pickDate) InviteDatePicker(day, onDismiss = { pickDate = false }) { picked ->
        dayKey = listOf(picked.year, picked.month, picked.day)
        pickDate = false
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        InviteFormHeader(
            if (page == DeliveryPage.FORM) "Delivery Invite" else "Invite Summary",
            if (page == DeliveryPage.FORM) "Create an invite for a food, parcel or courier drop" else "Check the details before creating it",
            onBack = if (page == DeliveryPage.SUMMARY) ({ page = DeliveryPage.FORM }) else null,
            onClose = onClose,
        ) {
            InviteTypeArt(InviteType.DELIVERY, Modifier.requiredSize(72.dp), blend = false)
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "deliveryPage"
        ) { shown ->
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                if (shown == DeliveryPage.SUMMARY) DeliverySummary(draft(), partner)
                else {
                    InviteLabel("Delivery Partner")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DeliveryPartner.entries.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { p -> PartnerChip(p, p == partner, Modifier.weight(1f)) { partner = p } }
                            }
                        }
                    }
                    InviteLabel("Name")
                    InviteTextField(Icons.Outlined.Person, name, { name = it.take(60) }, "e.g. Ahmad", nameError,
                        KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
                    InviteLabel("Mobile Number")
                    InviteMobileField(countryCode, { countryCode = it }, mobile,
                        { mobile = it.filter { c -> c.isDigit() || c == ' ' }.take(18) }, mobileError)
                    InviteLabel("Date")
                    InviteDateField(day) { pickDate = true }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            InviteLabel("Start Time")
                            InviteDropdown(Icons.Outlined.Schedule, clockLabel(startMinutes), startTimes.map { clockLabel(it) }) {
                                startMinutes = startTimes[it]
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            InviteLabel("Valid for")
                            InviteDropdown(Icons.Outlined.Schedule, DeliveryValidFor.first { it.first == validHours }.second,
                                DeliveryValidFor.map { it.second }) { validHours = DeliveryValidFor[it].first }
                        }
                    }
                    InviteLabel("Remarks (Optional)")
                    InviteTextField(Icons.Outlined.Description, remarks, { remarks = it.take(120) }, "e.g. Food, Parcel", null,
                        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        InviteFormButton(if (page == DeliveryPage.SUMMARY) "Create Invite" else "Continue") {
            if (page == DeliveryPage.SUMMARY) {
                // TODO: create the invite through the visitor API once it exists.
                onCreated(VisitorStore.add(draft()))
                return@InviteFormButton
            }
            submitted = true
            if (name.isNotBlank() && mobileDigits.length in 7..15) page = DeliveryPage.SUMMARY
        }
    }
}

@Composable
private fun DeliverySummary(invite: VisitorInvite, partner: DeliveryPartner) {
    Spacer(Modifier.height(4.dp))
    GlassCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White).border(1.dp, DeliveryBorder, CircleShape),
                contentAlignment = Alignment.Center) {
                PartnerLogo(partner, 34.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(invite.name, fontFamily = DMSans, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                Text(invite.kindLabel, fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        SummaryLine("Mobile Number", invite.mobileLabel)
        SummaryLine("Delivery Partner", partner.label)
        SummaryLine("Date", "${invite.day.label()} ${invite.day.year}")
        SummaryLine("Time", invite.timeRange)
        SummaryLine("Valid For", invite.validForLabel)
        SummaryLine("Remarks", invite.note.ifBlank { "-" }, last = true)
    }
}

@Composable
private fun SummaryLine(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
        Text(value, Modifier.weight(1.2f), fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = FacilityInk, textAlign = TextAlign.End)
    }
    if (!last) HorizontalDivider(color = BookingsDivider)
}

@Composable
private fun PartnerLogo(partner: DeliveryPartner, size: androidx.compose.ui.unit.Dp) {
    if (partner.logo != null) Image(painterResource(partner.logo), null, Modifier.size(size))
    else Box(Modifier.size(size * .85f).border(1.5.dp, FacilityInk, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.MoreHoriz, null, tint = FacilityInk, modifier = Modifier.size(size * .6f))
    }
}

@Composable
private fun PartnerChip(partner: DeliveryPartner, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    // Sized like Invite Cab's company chips.
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.height(70.dp).clip(shape)
            .background(if (selected) DeliverySelectedFill else Color.White)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) FacilityGreen else DeliveryBorder, shape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        PartnerLogo(partner, 26.dp)
        Spacer(Modifier.height(6.dp))
        Text(partner.label, fontFamily = DMSans, fontSize = 11.sp, color = FacilityInk, maxLines = 1)
    }
}
