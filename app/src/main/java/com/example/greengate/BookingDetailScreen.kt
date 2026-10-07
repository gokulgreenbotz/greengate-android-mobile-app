package com.example.greengate

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

@Composable
internal fun BookingDetailScreen(navController: NavController, booking: Booking) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val status = booking.status(now)
    var confirmCancel by remember { mutableStateOf(false) }

    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Cancel this booking?", fontFamily = DMSans) },
        text = {
            Text(
                if (booking.total > 0) "The full ${money(booking.total)} will be refunded via ${booking.method.shortName}."
                else "The slot will be released for other residents.",
                fontFamily = DMSans
            )
        },
        confirmButton = {
            TextButton(onClick = {
                confirmCancel = false
                // TODO: cancel through the bookings API once it exists.
                BookingStore.cancel(booking.id)
                Toast.makeText(context, "Booking ${booking.id} cancelled", Toast.LENGTH_SHORT).show()
            }) { Text("Cancel booking", color = BookingsRed, fontFamily = DMSans) }
        },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it", fontFamily = DMSans) } }
    )

    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            GlassTopBar("Booking Details", onBack = { navController.popBackStack() })
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                GlassCard(padding = 12.dp, onClick = { navController.navigate(Screen.FacilityAbout.create(booking.facility.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ArtworkTile(booking.facility, 104.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(booking.facility.name, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                            Spacer(Modifier.height(4.dp))
                            StatusPill(booking.statusTone(now))
                            Spacer(Modifier.height(6.dp))
                            Text("${booking.day.label()} ${booking.day.year}", fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                            Text("${booking.timeRange} (${booking.durationLabel})", fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                            Text(booking.guestsLabel, fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "About ${booking.facility.name}",
                            tint = FacilityMuted, modifier = Modifier.size(24.dp))
                    }
                }
                if (status == BookingStatus.UPCOMING) {
                    GlassCard {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            QrPass(booking.qrContent(), booking.id)
                            Spacer(Modifier.height(12.dp))
                            ScanHint(booking.facility.name, booking.hour, booking.hours)
                        }
                    }
                }
                PaymentCard(booking) { id -> navController.navigate(Screen.TransactionDetail.create(id)) }
                if (booking.refundAmount > 0) DepositCard(booking, now)
                Spacer(Modifier.height(4.dp))
            }
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (status == BookingStatus.UPCOMING) {
                    PillButton("Cancel Booking", Modifier.weight(1f).height(54.dp), tint = BookingsRed,
                        icon = Icons.Outlined.Delete, background = RedTintButton) { confirmCancel = true }
                } else {
                    PillButton("Book Again", Modifier.weight(1f).height(54.dp), icon = Icons.Outlined.Home) {
                        navController.navigate(Screen.BookFacility.route) { popUpTo(Screen.BookFacility.route) { inclusive = true } }
                    }
                }
                PillButton("Need Help", Modifier.weight(1f).height(54.dp), icon = Icons.Outlined.HeadsetMic) {
                    navController.navigate(Screen.Feedback.route)
                }
            }
        }
    }
}

@Composable
private fun PaymentCard(booking: Booking, onReceipt: (String) -> Unit) {
    GlassCard {
        Text("Payment Summary", fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
        Spacer(Modifier.height(8.dp))
        AmountRow("Booking Fee", money(booking.bookingFee))
        AmountRow("Security Deposit", money(booking.deposit))
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = BookingsDivider)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Total Paid", Modifier.weight(1f), fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
            Text(money(booking.total), fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BookingsDeepGreen)
        }
        if (booking.total > 0) {
            Text("Paid on ${dateTimeLabel(booking.bookedAt)} via ${booking.method.shortName}",
                Modifier.padding(top = 4.dp), fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
            Row(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button) { onReceipt(booking.transactionId(TransactionKind.PAYMENT)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.ReceiptLong, null, tint = FacilityInk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Receipt", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 15.sp, color = FacilityMuted)
        Text(value, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
    }
}

@Composable
private fun DepositCard(booking: Booking, now: Long) {
    val cancelled = booking.cancelledAt != null
    GlassCard {
        Text(if (cancelled) "Refund Status" else "Deposit Status", fontFamily = DMSans, fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold, color = FacilityInk)
        Spacer(Modifier.height(10.dp))
        if (booking.status(now) == BookingStatus.UPCOMING) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(FacilityOrange), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    StatusPill(orangeTone("Held"), small = true)
                    Text("Will be refunded after the booking, subject to facility terms and no damages.",
                        Modifier.padding(top = 4.dp), fontFamily = DMSans, fontSize = 13.sp, lineHeight = 18.sp, color = FacilityMuted)
                }
            }
            return@GlassCard
        }
        val refunded = booking.isRefunded(now)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(if (refunded) greenTone("Refunded") else orangeTone("Processing"))
            Spacer(Modifier.weight(1f))
            if (refunded) Text("${money(booking.refundAmount)} refunded", fontFamily = DMSans, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, color = FacilityGreen)
        }
        Spacer(Modifier.height(10.dp))
        val steps = listOf(
            "Paid" to booking.bookedAt,
            (if (cancelled) "Booking cancelled" else "Booking completed") to (booking.cancelledAt ?: booking.endAt),
            "Refund processed" to booking.refundProcessedAt!!,
            "Refunded to ${booking.method.shortName}" to booking.refundedAt!!,
        )
        steps.forEach { (label, at) -> TimelineStep(label, at, done = at <= now) }
    }
}

@Composable
private fun TimelineStep(label: String, at: Long, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(if (done) BookingsDeepGreen else FacilityMuted.copy(alpha = .25f)),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 14.sp, color = if (done) FacilityInk else FacilityMuted)
        Text(if (done) shortDateTimeLabel(at) else "Pending", fontFamily = DMSans, fontSize = 13.sp,
            color = FacilityMuted, textAlign = TextAlign.End)
    }
}
