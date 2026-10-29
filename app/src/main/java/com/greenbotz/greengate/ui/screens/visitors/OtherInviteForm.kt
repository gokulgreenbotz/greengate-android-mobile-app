package com.greenbotz.greengate.ui.screens.visitors

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.core.visitor.VisitorInvite
import com.greenbotz.greengate.core.visitor.VisitorStore
import com.greenbotz.greengate.ui.screens.facilities.BookingDay
import com.greenbotz.greengate.ui.screens.facilities.clockLabel
import com.greenbotz.greengate.ui.theme.DMSans

private enum class OtherCategory(val label: String) {
    CONTRACTOR("Contractor"), TECHNICIAN("Technician"), SERVICE("Service"), OTHER("Other"),
}

private enum class OtherPage { FORM, SUMMARY }

private val OtherValidFor = listOf(1 to "1 Hour", 2 to "2 Hours", 4 to "4 Hours", 8 to "8 Hours", 24 to "Full Day")

/**
 * Other invite (a contractor, technician, service visit and so on), shown in place of the visitor types
 * inside the Create Invite sheet: who and when, then a summary before the invite is created. Laid out
 * like the Delivery invite; back from the form returns to the types.
 */
@Composable
internal fun OtherInviteForm(onClose: () -> Unit, onCreated: (VisitorInvite) -> Unit) {
    var category by rememberSaveable { mutableStateOf(OtherCategory.CONTRACTOR) }
    var name by rememberSaveable { mutableStateOf("") }
    var company by rememberSaveable { mutableStateOf("") }
    var countryCode by rememberSaveable { mutableStateOf(InviteCountryCodes.first()) }
    var mobile by rememberSaveable { mutableStateOf("") }
    var dayKey by rememberSaveable { mutableStateOf(BookingDay.today().let { listOf(it.year, it.month, it.day) }) }
    val day = BookingDay(dayKey[0], dayKey[1], dayKey[2])
    val startTimes = deliveryStartTimes(day)
    var startMinutes by rememberSaveable { mutableIntStateOf(startTimes.first()) }
    // A date change can leave the chosen time in the past; snap to the first one still open.
    if (startMinutes !in startTimes) startMinutes = startTimes.first()
    var validHours by rememberSaveable { mutableIntStateOf(2) }
    var vehicle by rememberSaveable { mutableStateOf("") }
    var remarks by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableStateOf(OtherPage.FORM) }
    var pickDate by remember { mutableStateOf(false) }
    // Errors appear only after the first Continue, so an untouched form isn't covered in red.
    var submitted by rememberSaveable { mutableStateOf(false) }
    val nameError = if (submitted && name.isBlank()) "Enter the visitor's name" else null
    val mobileDigits = mobile.filter { it.isDigit() }
    val mobileError = if (submitted && mobileDigits.length !in 7..15) "Enter a valid mobile number" else null

    fun draft(): VisitorInvite {
        val plate = vehicle.trim()
        return VisitorInvite(
            id = "", name = name.trim(), type = InviteType.OTHER,
            // e.g. "Technician • CoolAir Services", listed under the visitor's name.
            relationship = listOf(category.label, company.trim()).filter { it.isNotEmpty() }.joinToString(" • "),
            countryCode = countryCode, mobile = mobile.trim(), day = day, startMinutes = startMinutes, validHours = validHours,
            vehicleRegion = "SG".takeIf { plate.isNotEmpty() }, vehicle = plate.ifEmpty { null },
            visitorCanEditVehicle = false, faceRegistration = false, repeat = null, notifyOnEntry = true,
            // Workers come and go during the job, so entries aren't capped within the time window.
            maxEntries = null, note = remarks.trim(), createdAt = System.currentTimeMillis()
        )
    }

    BackHandler(enabled = page == OtherPage.SUMMARY) { page = OtherPage.FORM }
    if (pickDate) InviteDatePicker(day, onDismiss = { pickDate = false }) { picked ->
        dayKey = listOf(picked.year, picked.month, picked.day)
        pickDate = false
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        InviteFormHeader(
            if (page == OtherPage.FORM) "Other Visitor" else "Invite Summary",
            if (page == OtherPage.FORM) "Contractor, technician, service & more" else "Check the details before creating it",
            onBack = if (page == OtherPage.SUMMARY) ({ page = OtherPage.FORM }) else null,
            onClose = onClose,
        ) {
            InviteHeaderArt(InviteType.OTHER)
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "otherPage"
        ) { shown ->
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                if (shown == OtherPage.SUMMARY) OtherSummary(draft(), category, company.trim())
                else {
                    InviteLabel("Visitor Type")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OtherCategory.entries.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { c -> InviteChoiceChip(c.label, c == category, Modifier.weight(1f)) { category = c } }
                            }
                        }
                    }
                    InviteLabel("Name")
                    InviteTextField(Icons.Outlined.Person, name, { name = it.take(60) }, "e.g. Ravi Kumar", nameError,
                        KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
                    InviteLabel("Company (Optional)")
                    InviteTextField(Icons.Outlined.Business, company, { company = it.take(40) }, "e.g. CoolAir Services", null,
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
                            InviteDropdown(Icons.Outlined.Schedule, OtherValidFor.first { it.first == validHours }.second,
                                OtherValidFor.map { it.second }) { validHours = OtherValidFor[it].first }
                        }
                    }
                    InviteLabel("Vehicle Number (Optional)")
                    InviteTextField(Icons.Outlined.DirectionsCar, vehicle,
                        { vehicle = it.uppercase().filter { c -> c.isLetterOrDigit() || c == ' ' }.take(12) }, "SMC 1234 A", null,
                        KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next))
                    InviteLabel("Remarks (Optional)")
                    InviteTextField(Icons.Outlined.Description, remarks, { remarks = it.take(120) }, "e.g. Aircon servicing", null,
                        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        InviteFormButton(if (page == OtherPage.SUMMARY) "Create Invite" else "Continue") {
            if (page == OtherPage.SUMMARY) {
                // TODO: create the invite through the visitor API once it exists.
                onCreated(VisitorStore.add(draft()))
                return@InviteFormButton
            }
            submitted = true
            if (name.isNotBlank() && mobileDigits.length in 7..15) page = OtherPage.SUMMARY
        }
    }
}

@Composable
private fun OtherSummary(invite: VisitorInvite, category: OtherCategory, company: String) {
    Spacer(Modifier.height(4.dp))
    InviteCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InviteAvatar(invite, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(invite.name, fontFamily = DMSans, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = InviteInk)
                Text(category.label, fontFamily = DMSans, fontSize = 14.sp, color = InviteMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        SummaryLine("Mobile Number", invite.mobileLabel)
        SummaryLine("Company", company.ifEmpty { "-" })
        SummaryLine("Date", "${invite.day.label()} ${invite.day.year}")
        SummaryLine("Time", invite.timeRange)
        SummaryLine("Valid For", invite.validForLabel)
        SummaryLine("Vehicle", invite.vehicleLabel)
        SummaryLine("Remarks", invite.note.ifBlank { "-" }, last = true)
    }
}
