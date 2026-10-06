package com.example.greengate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Discount
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

private val PaymentBackdrop = Brush.verticalGradient(
    0f to Color(0xFFA9D6DF), 0.22f to Color(0xFFD3ECEC), 1f to Color(0xFFE6F4F1)
)
private val PaymentDeepGreen = Color(0xFF0F6B54)
private val PaymentMint = Color(0xFFDDF5EC)
private val PaymentDivider = Color(0x1F14232B)
private val PaymentError = Color(0xFFC2412D)

internal enum class PaymentMethod(val title: String, val subtitle: String, val icon: ImageVector, val shortName: String) {
    CARD("Credit / Debit Card", "Visa, Mastercard, AMEX", Icons.Outlined.CreditCard, "Card"),
    GOOGLE_PAY("Google Pay", "Pay with your saved Google account", Icons.Outlined.Smartphone, "Google Pay"),
    WALLET("Wallet", "Use your Green Gate wallet balance", Icons.Outlined.AccountBalanceWallet, "Wallet"),
}

/** Reached from a facility's slot picker once a date, time and guest count are chosen. */
@Composable
internal fun BookingPaymentScreen(
    navController: NavController,
    facility: Facility,
    day: BookingDay,
    hour: Int,
    guests: Int
) {
    var method by remember { mutableStateOf(PaymentMethod.CARD) }
    // No booking fee or tax yet; both rows stay so residents see the full breakdown.
    val bookingFee = 0
    val tax = 0
    val total = facility.deposit + bookingFee + tax

    Box(Modifier.fillMaxSize().background(PaymentBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Box(
                    Modifier.align(Alignment.CenterStart).size(52.dp).shadow(6.dp, CircleShape)
                        .clip(CircleShape).background(Color(0xF2FFFFFF))
                        .clickable(role = Role.Button) { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back", tint = FacilityInk, modifier = Modifier.size(30.dp))
                }
                Text("Booking Payment", Modifier.align(Alignment.Center), fontFamily = DMSans,
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                BookingSummaryCard(facility, day, hour, guests)
                PaymentSummaryCard(facility.deposit, bookingFee, tax, total)
                GlassSection {
                    SectionTitle("Payment Method")
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PaymentMethod.entries.forEach { m ->
                            PaymentMethodRow(m, selected = m == method) { method = m }
                        }
                    }
                }
                PromoCodeCard()
                if (facility.deposit > 0) DepositNote(facility.deposit)
                Spacer(Modifier.height(4.dp))
            }
            CheckoutBar(total) {
                // TODO: hand off to the payment gateway and bookings API once they exist; until
                // then every payment "succeeds" straight away.
                val booking = BookingStore.add(facility, day, hour, guests, bookingFee, method)
                navController.navigate(Screen.BookingConfirmed.create(booking.id)) {
                    // Back from the confirmation returns to the facility list, not to payment.
                    popUpTo(Screen.BookFacility.route)
                }
            }
        }
    }
}

@Composable
private fun GlassSection(padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().shadow(2.dp, shape, ambientColor = Color(0x1A0F3B33), spotColor = Color(0x1A0F3B33))
            .clip(shape).background(Color(0xB3FFFFFF)).border(1.5.dp, FacilityGlassBorder, shape)
            .padding(padding),
        content = content
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
}

@Composable
private fun BookingSummaryCard(facility: Facility, day: BookingDay, hour: Int, guests: Int) {
    GlassSection(padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(width = 128.dp, height = 132.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color.White, Color(0xFFDDF0EA)))),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(facility.artwork), null, Modifier.size(104.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(facility.name, fontFamily = DMSans, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(PaymentMint).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(FacilityGreen))
                    Spacer(Modifier.width(6.dp))
                    Text("Available", fontFamily = DMSans, fontSize = 13.sp, color = FacilityInk)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.height(IntrinsicSize.Min)) {
                    SummaryFact(Icons.Outlined.CalendarToday, "Date", "${day.label()}\n${day.year}", Modifier.weight(1.1f))
                    FactDivider()
                    SummaryFact(Icons.Outlined.Schedule, "Time", "${hourLabel(hour)} –\n${hourLabel(hour + 1)}", Modifier.weight(1.1f))
                    FactDivider()
                    SummaryFact(Icons.Outlined.Groups, "Guests", "$guests pax", Modifier.weight(.8f))
                }
            }
        }
    }
}

@Composable
private fun FactDivider() {
    Box(Modifier.padding(horizontal = 6.dp).width(1.dp).fillMaxHeight().background(PaymentDivider))
}

@Composable
private fun SummaryFact(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xE6FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = FacilityInk, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
        Text(value, fontFamily = DMSans, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
    }
}

@Composable
private fun PaymentSummaryCard(deposit: Int, bookingFee: Int, tax: Int, total: Int) {
    GlassSection {
        SectionTitle("Payment Summary")
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x99FFFFFF))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            ChargeRow(Icons.Outlined.AccountBalanceWallet, "Deposit", deposit)
            HorizontalDivider(Modifier.padding(start = 38.dp), color = PaymentDivider)
            ChargeRow(Icons.Outlined.Description, "Booking fee", bookingFee)
            HorizontalDivider(Modifier.padding(start = 38.dp), color = PaymentDivider)
            ChargeRow(Icons.Rounded.Percent, "Tax (0%)", tax)
            HorizontalDivider(color = PaymentDivider.copy(alpha = .2f))
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Total", Modifier.weight(1f), fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                Text("S$$total", fontFamily = DMSans, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
            }
        }
    }
}

@Composable
private fun ChargeRow(icon: ImageVector, label: String, amount: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 15.sp, color = FacilityMuted)
        Text("S$$amount", fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
    }
}

@Composable
private fun PaymentMethodRow(method: PaymentMethod, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) Color(0xCCE4F7EF) else Color(0x99FFFFFF))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Color(0xFF5CC9A7) else FacilityGlassBorder, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) Color(0xFFC6EEDD) else Color(0xE6FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(method.icon, null, tint = FacilityInk, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(method.title, fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
            Text(method.subtitle, fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
        }
        // Drawn rather than Material's RadioButton to match the design's soft ring.
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .border(1.5.dp, if (selected) FacilityGreen else FacilityMuted.copy(alpha = .45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(14.dp).clip(CircleShape).background(FacilityGreen))
        }
    }
}

@Composable
private fun PromoCodeCard() {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    // TODO: validate against the promotions API once it exists; until then no code applies.
    val apply = {
        focus.clearFocus()
        error = if (code.isBlank()) "Enter a promo code first" else "\"${code.trim()}\" isn't a valid promo code"
    }
    GlassSection(padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.Discount, null, tint = FacilityInk, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Promo Code", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
            Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(30.dp).background(PaymentDivider))
            Box(Modifier.weight(1f)) {
                if (code.isEmpty()) Text("Enter promo code", fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
                BasicTextField(
                    value = code,
                    onValueChange = { code = it.take(24); error = null },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = DMSans, fontSize = 14.sp, color = FacilityInk),
                    cursorBrush = SolidColor(FacilityGreen),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { apply() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFDCEBE8))
                    .clickable(role = Role.Button) { apply() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("Apply", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
            }
        }
        error?.let {
            Text(it, Modifier.padding(start = 6.dp, top = 6.dp), fontFamily = DMSans, fontSize = 12.sp, color = PaymentError)
        }
    }
}

@Composable
private fun DepositNote(deposit: Int) {
    GlassSection(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(PaymentMint), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = FacilityInk, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Refundable security deposit", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                Text(
                    "The deposit of S$$deposit is fully refundable after your booking and subject to our facility terms.",
                    fontFamily = DMSans, fontSize = 13.sp, lineHeight = 18.sp, color = FacilityMuted
                )
            }
        }
    }
}

@Composable
private fun CheckoutBar(total: Int, onConfirm: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Color(0x260F3B33), spotColor = Color(0x260F3B33))
            .clip(shape).background(Color(0xE6FFFFFF)).border(1.5.dp, FacilityGlassBorder, shape)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.padding(end = 14.dp)) {
            Text("Total", fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
            Text("S$$total", fontFamily = DMSans, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
        }
        Box(Modifier.width(1.dp).height(44.dp).background(PaymentDivider))
        Spacer(Modifier.width(14.dp))
        Row(
            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(PaymentDeepGreen, Color(0xFF1B8A6B))))
                .clickable(role = Role.Button, onClick = onConfirm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nothing to charge when the facility takes no deposit.
            Text(if (total > 0) "Pay & Confirm" else "Confirm Booking", fontFamily = DMSans,
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}
