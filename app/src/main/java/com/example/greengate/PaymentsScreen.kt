package com.example.greengate

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

private enum class TransactionFilter(val label: String, val kind: TransactionKind?) {
    ALL("All", null), PAYMENTS("Payments", TransactionKind.PAYMENT),
    DEPOSITS("Deposits", TransactionKind.DEPOSIT), REFUNDS("Refunds", TransactionKind.REFUND)
}

private fun Transaction.tone() = when (kind) {
    TransactionKind.PAYMENT -> greenTone("Paid")
    TransactionKind.DEPOSIT -> orangeTone("Held")
    TransactionKind.REFUND -> greenTone("Refunded")
}

/** Refunds come in (+), payments go out (−), held deposits are neither yet. */
private fun Transaction.signedAmount() = when (kind) {
    TransactionKind.PAYMENT -> "−${money(amount)}"
    TransactionKind.DEPOSIT -> money(amount)
    TransactionKind.REFUND -> "+${money(amount)}"
}

@Composable
internal fun PaymentsScreen(navController: NavController) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(TransactionFilter.ALL) }
    var oldestFirst by rememberSaveable { mutableStateOf(false) }
    val all = transactionsOf(BookingStore.bookings)
    val held = all.filter { it.kind == TransactionKind.DEPOSIT }.sumOf { it.amount }
    val refunded = all.filter { it.kind == TransactionKind.REFUND }.sumOf { it.amount }
    val shown = all.filter { filter.kind == null || it.kind == filter.kind }.let { if (oldestFirst) it.reversed() else it }
    val groups = shown.groupBy { dayHeaderLabel(it.at) }

    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            GlassTopBar("Payments & Deposits", onBack = { navController.popBackStack() }) {
                GlassIconButton(Icons.Rounded.SwapVert, "Change order") {
                    oldestFirst = !oldestFirst
                    Toast.makeText(context, if (oldestFirst) "Oldest first" else "Newest first", Toast.LENGTH_SHORT).show()
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TotalTile("Deposits Held", money(held), Modifier.weight(1f))
                        TotalTile("Refunds Received", money(refunded), Modifier.weight(1f))
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        items(TransactionFilter.entries) { f -> FilterChip(f.label, f == filter) { filter = f } }
                    }
                }
                if (shown.isEmpty()) item {
                    Text("Nothing here yet.", Modifier.fillMaxWidth().padding(top = 32.dp), fontFamily = DMSans,
                        fontSize = 15.sp, color = FacilityMuted, textAlign = TextAlign.Center)
                }
                groups.forEach { (header, transactions) ->
                    item(key = "h-$header") {
                        Text(header, Modifier.padding(top = 6.dp), fontFamily = DMSans, fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold, color = FacilityInk)
                    }
                    items(transactions, key = { it.id }) { t ->
                        TransactionRow(t) { navController.navigate(Screen.TransactionDetail.create(t.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalTile(label: String, value: String, modifier: Modifier) {
    GlassCard(modifier, padding = 14.dp) {
        Text(label, fontFamily = DMSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = DMSans, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.height(40.dp).clip(shape)
            .background(if (selected) BookingsDeepGreen else Color(0xB3FFFFFF))
            .border(1.dp, if (selected) BookingsDeepGreen else FacilityGlassBorder, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else FacilityMuted)
    }
}

@Composable
private fun TransactionRow(t: Transaction, onClick: () -> Unit) {
    GlassCard(Modifier.clickable(role = Role.Button, onClick = onClick), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtworkTile(t.booking.facility, 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.booking.facility.name, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                Text(t.description, fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
                Spacer(Modifier.height(3.dp))
                StatusPill(t.tone(), small = true)
                Spacer(Modifier.height(3.dp))
                Text(dateTimeLabel(t.at), fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(t.signedAmount(), fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = if (t.kind == TransactionKind.REFUND) FacilityGreen else FacilityInk)
                Text(t.booking.method.shortName, fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
            }
        }
    }
}

@Composable
internal fun TransactionDetailScreen(navController: NavController, t: Transaction) {
    val (title, icon, iconBrush) = when (t.kind) {
        TransactionKind.REFUND -> Triple("Refund Received", Icons.Rounded.Autorenew,
            Brush.linearGradient(listOf(Color(0xFF2AA07D), BookingsDeepGreen)))
        TransactionKind.PAYMENT -> Triple("Payment Made", Icons.Outlined.Payments,
            Brush.linearGradient(listOf(Color(0xFF2AA07D), BookingsDeepGreen)))
        TransactionKind.DEPOSIT -> Triple("Deposit Held", Icons.Outlined.Lock,
            Brush.linearGradient(listOf(Color(0xFFF29A55), FacilityOrange)))
    }
    val remarks = when (t.kind) {
        TransactionKind.REFUND -> if (t.booking.cancelledAt != null) "Booking cancelled. Full amount refunded."
            else "No damages. Full deposit refunded as per policy."
        TransactionKind.PAYMENT -> if (t.booking.deposit > 0) "Includes a refundable ${money(t.booking.deposit)} security deposit."
            else "Booking fee for this slot."
        TransactionKind.DEPOSIT -> "Held until the booking ends, then refunded subject to facility terms and no damages."
    }
    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            GlassTopBar("Transaction Details", onBack = { navController.popBackStack() })
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                GlassCard(padding = 20.dp) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(84.dp).shadow(10.dp, CircleShape, ambientColor = Color(0x552AA07D), spotColor = Color(0x552AA07D))
                                .clip(CircleShape).background(iconBrush).border(4.dp, Color(0xCCFFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = Color.White, modifier = Modifier.size(42.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(title, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                        Text(money(t.amount), fontFamily = DMSans, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                            color = if (t.kind == TransactionKind.DEPOSIT) FacilityOrange else BookingsDeepGreen)
                        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = BookingsDivider)
                        Text(t.booking.facility.name, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                        Text(t.description.replaceFirstChar { it.uppercase() }, fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = BookingsDivider)
                    DetailLine("Date & Time", dateTimeLabel(t.at))
                    DetailLine("Transaction ID", t.id)
                    DetailLine("Payment Method", t.booking.method.shortName)
                    DetailLine(
                        "Related Booking", "${t.booking.facility.name}\n${t.booking.day.shortLabel()} ${t.booking.day.year}, ${hourLabel(t.booking.hour)}",
                        onClick = { navController.navigate(Screen.BookingDetail.create(t.booking.id)) }
                    )
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Status", Modifier.weight(1f), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                        StatusPill(t.tone())
                    }
                    DetailLine("Remarks", remarks)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
        Text(value, Modifier.weight(1.4f), fontFamily = DMSans, fontSize = 14.sp, lineHeight = 20.sp,
            fontWeight = FontWeight.Medium, color = if (onClick != null) BookingsDeepGreen else FacilityInk, textAlign = TextAlign.End)
    }
}
