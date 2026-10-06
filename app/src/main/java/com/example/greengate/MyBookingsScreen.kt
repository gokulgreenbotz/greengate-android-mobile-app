package com.example.greengate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

/** Reached from the calendar button on Book Facility. */
@Composable
internal fun MyBookingsScreen(navController: NavController) {
    var tab by rememberSaveable { mutableStateOf(BookingStatus.UPCOMING) }
    var qrFor by remember { mutableStateOf<Booking?>(null) }
    val now = System.currentTimeMillis()
    val shown = BookingStore.bookings.filter { it.status(now) == tab }.let { list ->
        // Soonest first while waiting; most recent first once it's history.
        if (tab == BookingStatus.UPCOMING) list.sortedBy { it.startAt } else list.sortedByDescending { it.startAt }
    }

    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            GlassTopBar("My Bookings", onBack = { navController.popBackStack() }) {
                GlassIconButton(Icons.Outlined.ReceiptLong, "Payments and deposits") {
                    navController.navigate(Screen.Payments.route)
                }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(BookingStatus.UPCOMING to "Upcoming", BookingStatus.COMPLETED to "Completed", BookingStatus.CANCELLED to "Cancelled")
                    .forEach { (status, label) -> SegmentChip(label, selected = tab == status, Modifier.weight(1f)) { tab = status } }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (shown.isEmpty()) item {
                    Text(
                        when (tab) {
                            BookingStatus.UPCOMING -> "No upcoming bookings. Book a facility to see it here."
                            BookingStatus.COMPLETED -> "No completed bookings yet."
                            BookingStatus.CANCELLED -> "No cancelled bookings."
                        },
                        Modifier.fillMaxWidth().padding(top = 40.dp), fontFamily = DMSans, fontSize = 15.sp,
                        color = FacilityMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                items(shown, key = { it.id }) { booking ->
                    BookingCard(
                        booking, now,
                        onQr = { qrFor = booking },
                        onDetails = { navController.navigate(Screen.BookingDetail.create(booking.id)) }
                    )
                }
            }
        }
    }

    qrFor?.let { booking ->
        Dialog(onDismissRequest = { qrFor = null }) {
            GlassCard(padding = 20.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(booking.facility.name, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                    Text("${booking.day.label()} · ${booking.timeRange}", fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                    Spacer(Modifier.height(16.dp))
                    QrPass(booking.qrContent(), booking.id)
                    Spacer(Modifier.height(12.dp))
                    ScanHint(booking.facility.name, booking.hour, booking.hours)
                    TextButton(onClick = { qrFor = null }) { Text("Close", fontFamily = DMSans, color = BookingsDeepGreen) }
                }
            }
        }
    }
}

@Composable
private fun SegmentChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.height(42.dp).clip(shape)
            .background(if (selected) BookingsDeepGreen else Color(0xB3FFFFFF))
            .border(1.dp, if (selected) BookingsDeepGreen else FacilityGlassBorder, shape)
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = DMSans, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else FacilityMuted)
    }
}

@Composable
private fun BookingCard(booking: Booking, now: Long, onQr: () -> Unit, onDetails: () -> Unit) {
    val facility = booking.facility
    GlassCard(Modifier.clickable(role = Role.Button, onClick = onDetails), padding = 14.dp) {
        Row {
            ArtworkTile(facility, 96.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(facility.name, Modifier.weight(1f).padding(top = 2.dp), fontFamily = DMSans, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, color = FacilityInk)
                    StatusPill(booking.statusTone(now))
                }
                Spacer(Modifier.height(4.dp))
                Text("${booking.day.label()} ${booking.day.year}", fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                Text(booking.timeRange, fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                Text(booking.guestsLabel, fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                Spacer(Modifier.height(8.dp))
                Text(paidLine(booking, now), fontFamily = DMSans, fontSize = 14.sp, color = FacilityInk)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (booking.qrReady(now)) {
                PillButton("View QR", Modifier.weight(1f).height(46.dp), icon = Icons.Outlined.QrCode2, background = MintButton, onClick = onQr)
                PillButton("View Details", Modifier.weight(1f).height(46.dp), onClick = onDetails)
            } else {
                Spacer(Modifier.weight(1f))
                PillButton("View Details", Modifier.weight(1f).height(46.dp), onClick = onDetails)
            }
        }
    }
}

/** e.g. "S$16 paid · S$8 deposit (Held)", with the deposit coloured by its state. */
private fun paidLine(booking: Booking, now: Long) = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
        append(if (booking.total > 0) "S$${booking.total} paid" else "No payment")
    }
    if (booking.deposit > 0) {
        val refunded = booking.isRefunded(now)
        append("  ·  ")
        withStyle(SpanStyle(color = if (refunded) FacilityGreen else FacilityOrange, fontWeight = FontWeight.SemiBold)) {
            append("S$${booking.deposit}")
        }
        withStyle(SpanStyle(color = FacilityMuted)) { append(if (refunded) " deposit (Refunded)" else " deposit (Held)") }
    }
}
