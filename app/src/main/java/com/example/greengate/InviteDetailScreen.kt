package com.example.greengate

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

private enum class InviteTab { DETAILS, ACCESS, HISTORY }

@Composable
internal fun InviteDetailScreen(navController: NavController, invite: VisitorInvite) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val status = invite.status(now)
    val open = status == InviteStatus.ACTIVE || status == InviteStatus.EXPECTED || status == InviteStatus.UPCOMING
    var tab by rememberSaveable { mutableStateOf(InviteTab.DETAILS) }
    var menu by remember { mutableStateOf(false) }
    var confirmRevoke by remember { mutableStateOf(false) }

    if (confirmRevoke) AlertDialog(
        onDismissRequest = { confirmRevoke = false },
        title = { Text("Revoke this invite?", fontFamily = DMSans) },
        text = { Text("${invite.name}'s pass will stop opening the gates.", fontFamily = DMSans) },
        confirmButton = {
            TextButton(onClick = {
                confirmRevoke = false
                // TODO: revoke through the visitor API once it exists.
                VisitorStore.revoke(invite.id)
                Toast.makeText(context, "Invite revoked", Toast.LENGTH_SHORT).show()
            }) { Text("Revoke", color = BookingsRed, fontFamily = DMSans) }
        },
        dismissButton = { TextButton(onClick = { confirmRevoke = false }) { Text("Keep it", fontFamily = DMSans) } }
    )

    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            GlassTopBar("Invite Details", onBack = { navController.popBackStack() }) {
                if (open) Box {
                    GlassIconButton(Icons.Rounded.MoreVert, "More options") { menu = true }
                    DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = Color.White) {
                        DropdownMenuItem(text = { Text("Revoke invite", fontFamily = DMSans, color = BookingsRed) }, onClick = {
                            menu = false
                            confirmRevoke = true
                        })
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InviteAvatar(invite, 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(invite.name, fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                            Text(invite.kindLabel, fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                        }
                        StatusPill(status.tone())
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color(0x99FFFFFF)).padding(4.dp)
                ) {
                    listOf(
                        InviteTab.DETAILS to "Details",
                        InviteTab.ACCESS to "Access History (${invite.access.size})",
                        InviteTab.HISTORY to "History",
                    ).forEach { (t, label) ->
                        val selected = t == tab
                        Box(
                            Modifier.weight(if (t == InviteTab.ACCESS) 1.5f else 1f).height(38.dp).clip(RoundedCornerShape(50))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable(role = Role.Tab) { tab = t },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontFamily = DMSans, fontSize = 13.sp, maxLines = 1,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) FacilityInk else FacilityMuted)
                        }
                    }
                }
                when (tab) {
                    InviteTab.DETAILS -> {
                        VehicleApproval(invite)
                        InviteFacts(invite)
                    }
                    InviteTab.ACCESS -> AccessTimeline(invite)
                    InviteTab.HISTORY -> ChangeTimeline(invite)
                }
                Spacer(Modifier.height(4.dp))
            }
            if (open) Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Share Pass", Modifier.fillMaxWidth().height(54.dp), icon = Icons.Outlined.Share, background = MintButton) {
                    navController.navigate(Screen.InviteCreated.create(invite.id))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Edit", Modifier.weight(1f).height(54.dp),
                        background = Brush.linearGradient(listOf(Color(0xFFEEF2F1), Color(0xFFE6ECEA)))) {
                        // TODO: open the invite form pre-filled once editing is supported by the visitor API.
                        Toast.makeText(context, "Editing invites isn't available yet", Toast.LENGTH_SHORT).show()
                    }
                    PillButton("Revoke", Modifier.weight(1f).height(54.dp), tint = BookingsRed, background = RedTintButton) {
                        confirmRevoke = true
                    }
                }
            }
        }
    }
}

/** A visitor's pending vehicle change, approved or rejected right here. */
@Composable
private fun VehicleApproval(invite: VisitorInvite) {
    val request = invite.vehicleRequest ?: return
    val context = LocalContext.current
    fun resolve(approve: Boolean) {
        // TODO: send the decision to the visitor API once it exists.
        VisitorStore.resolveVehicleRequest(invite.id, approve)
        Toast.makeText(context, if (approve) "Vehicle updated" else "Request rejected", Toast.LENGTH_SHORT).show()
    }
    GlassCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.DirectionsCar, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Vehicle update request", Modifier.weight(1f), fontFamily = DMSans, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = FacilityInk)
            StatusPill(orangeTone("Awaiting approval"), small = true)
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = BookingsDivider)
        RequestLine("Current Vehicle", invite.vehicleLabel)
        RequestLine("Requested Vehicle", "${request.region} • ${request.plate}", highlight = true)
        RequestLine("Requested on", dateTimeLabel(request.requestedAt))
        RequestLine("Reason", request.reason)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(1.5.dp, BookingsDeepGreen, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { resolve(false) },
                contentAlignment = Alignment.Center
            ) { Text("Reject", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BookingsDeepGreen) }
            Box(
                Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1D8A6A), BookingsDeepGreen)))
                    .clickable(role = Role.Button) { resolve(true) },
                contentAlignment = Alignment.Center
            ) { Text("Approve", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        }
    }
}

@Composable
private fun InviteFacts(invite: VisitorInvite) {
    GlassCard(padding = 14.dp) {
        FactRow(Icons.Outlined.CalendarMonth, "Date", "${invite.day.label()} ${invite.day.year}")
        FactRow(Icons.Outlined.Schedule, "Time", invite.timeRange)
        FactRow(Icons.Outlined.HourglassEmpty, "Valid For", invite.validForLabel)
        FactRow(Icons.Outlined.DirectionsCar, "Vehicle", invite.vehicleLabel)
        FactRow(Icons.Outlined.Face, "Face Registration", when {
            !invite.faceRegistration -> "Off"
            invite.faceRegistered -> "Registered"
            else -> "Pending"
        }, valueColor = if (invite.faceRegistered) FacilityGreen else FacilityInk)
        FactRow(Icons.Outlined.EventRepeat, "Recurring", invite.repeat ?: "No")
        FactRow(Icons.Outlined.NotificationsActive, "Notify me on entry", if (invite.notifyOnEntry) "Yes" else "No")
        FactRow(Icons.Outlined.Description, "Notes", invite.note.ifBlank { "-" }, last = true)
    }
}

@Composable
private fun FactRow(icon: ImageVector, label: String, value: String, valueColor: Color = FacilityInk, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
        Text(value, fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = valueColor, textAlign = TextAlign.End)
    }
    if (!last) HorizontalDivider(Modifier.padding(start = 34.dp), color = BookingsDivider)
}

@Composable
private fun AccessTimeline(invite: VisitorInvite) {
    GlassCard(padding = 14.dp) {
        if (invite.access.isEmpty()) {
            Text("No entries yet. Each gate and door the visitor passes will appear here.",
                fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
            return@GlassCard
        }
        val events = invite.access.sortedBy { it.at }
        events.forEachIndexed { i, event ->
            TimelineStep(
                if (event.entry) Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout,
                event.place, null, event.at, last = i == events.lastIndex,
                pill = if (event.entry) greenTone("Entry") else redTone("Exit")
            )
        }
    }
}

private class HistoryStep(val icon: ImageVector, val title: String, val detail: String?, val at: Long,
                          val tint: Color = BookingsDeepGreen, val tile: Color = BookingsMint)

/** The invite's own changes: when it was created, every vehicle change request and decision, and a revoke. */
@Composable
private fun ChangeTimeline(invite: VisitorInvite) {
    val created = if (invite.createdVehicle.isNullOrBlank()) "No vehicle"
        else "Vehicle ${invite.createdVehicleRegion ?: "SG"} • ${invite.createdVehicle}"
    val steps = buildList {
        add(HistoryStep(Icons.Outlined.AddCircleOutline, "Invite Created", created, invite.createdAt))
        invite.vehicleChanges.forEach { change ->
            val plate = "${change.region} • ${change.plate}"
            add(when (change.kind) {
                VehicleChangeKind.REQUESTED -> HistoryStep(Icons.Outlined.DirectionsCar, "Vehicle Change Requested",
                    if (change.reason.isBlank()) plate else "$plate\n“${change.reason}”", change.at, FacilityOrange, Color(0xFFFFEEDF))
                VehicleChangeKind.APPROVED -> HistoryStep(Icons.Outlined.CheckCircle, "Vehicle Change Approved", plate, change.at)
                VehicleChangeKind.REJECTED -> HistoryStep(Icons.Outlined.HighlightOff, "Vehicle Change Rejected", plate, change.at,
                    BookingsRed, Color(0xFFFDE7E4))
            })
        }
        invite.revokedAt?.let { add(HistoryStep(Icons.Outlined.Block, "Invite Revoked", null, it, BookingsRed, Color(0xFFFDE7E4))) }
    }.sortedBy { it.at }
    GlassCard(padding = 14.dp) {
        steps.forEachIndexed { i, step ->
            TimelineStep(step.icon, step.title, step.detail, step.at, last = i == steps.lastIndex, tint = step.tint, tile = step.tile)
        }
    }
}

@Composable
private fun TimelineStep(
    icon: ImageVector, title: String, detail: String?, at: Long, last: Boolean,
    pill: StatusTone? = null, tint: Color = BookingsDeepGreen, tile: Color = BookingsMint,
) {
    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tile), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).heightIn(min = 16.dp).background(BookingsMint))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(top = 2.dp, bottom = if (last) 0.dp else 14.dp)) {
            Text(title, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
            if (detail != null) Text(detail, fontFamily = DMSans, fontSize = 13.sp, color = FacilityInk.copy(alpha = .8f))
            Text(dateTimeLabel(at), fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
        }
        if (pill != null) StatusPill(pill)
    }
}

@Composable
private fun RequestLine(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
        Text(value, Modifier.weight(1.2f), fontFamily = DMSans, fontSize = 15.sp, textAlign = TextAlign.End,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium, color = if (highlight) BookingsDeepGreen else FacilityInk)
    }
}
