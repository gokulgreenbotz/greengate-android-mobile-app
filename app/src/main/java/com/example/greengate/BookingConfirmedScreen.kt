package com.example.greengate

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.launch

private val ConfirmedBackdrop = Color(0xFFE3F2EF)
private val ConfirmedDeepGreen = Color(0xFF0F7A5C)
private val ConfirmedMint = Color(0xFFDDF5EC)
private val ConfirmedDivider = Color(0x1F14232B)
private val ConfirmedRed = Color(0xFFD93A2B)
private val QrInk = Color(0xFF0B3B31)

// The QR stays valid from 15 minutes before the slot until 15 minutes after it ends.
private const val GraceMinutes = 15

internal fun clockLabel(totalMinutes: Int): String {
    val h = Math.floorMod(totalMinutes / 60, 24)
    val m = Math.floorMod(totalMinutes, 60)
    return "${(h + 11) % 12 + 1}:${m.toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
}

/**
 * Shown once payment goes through. Payment, the booking itself and its QR pass are not wired
 * to a backend yet, so everything here is a stand-in built from the chosen slot.
 */
@Composable
internal fun BookingConfirmedScreen(navController: NavController, booking: Booking) {
    val facility = booking.facility
    val day = booking.day
    val hour = booking.hour
    val guests = booking.guests
    val method = booking.method
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(activity) {
        val bars = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        bars?.isAppearanceLightStatusBars = false
        onDispose { bars?.isAppearanceLightStatusBars = true }
    }
    // TODO: use the reference and pass returned by the bookings API.
    val reference = booking.id
    val qrContent = booking.qrContent()
    var confirmCancel by remember { mutableStateOf(false) }
    val backToFacilities = { navController.popBackStack(Screen.BookFacility.route, inclusive = false) }

    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Cancel this booking?", fontFamily = DMSans) },
        text = {
            Text(
                if (facility.deposit > 0) "Your S$${facility.deposit} deposit will be refunded to your wallet."
                else "The slot will be released for other residents.",
                fontFamily = DMSans
            )
        },
        confirmButton = {
            TextButton(onClick = {
                confirmCancel = false
                // TODO: cancel through the bookings API once it exists.
                BookingStore.cancel(booking.id)
                Toast.makeText(context, "Booking $reference cancelled", Toast.LENGTH_SHORT).show()
                backToFacilities()
            }) { Text("Cancel booking", color = ConfirmedRed, fontFamily = DMSans) }
        },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it", fontFamily = DMSans) } }
    )

    Box(Modifier.fillMaxSize().background(ConfirmedBackdrop)) {
        // The facility photo sets the scene behind the header and QR, then fades into the page.
        Box(Modifier.fillMaxWidth().height(520.dp)) {
            Image(painterResource(facility.photo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0xB3235F6E), 0.42f to Color(0x4D3A8796), 0.78f to Color(0x99E3F2EF), 1f to ConfirmedBackdrop
                    )
                )
            )
        }
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxSize().align(Alignment.TopCenter)
                .statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Color(0x59FFFFFF))
                        .border(1.dp, Color(0x99FFFFFF), CircleShape)
                        .clickable(role = Role.Button) { backToFacilities() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to facilities", tint = FacilityInk, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.greengate_logo), null, Modifier.size(52.dp))
                    Text("Green Gate", fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("SECURE · SAFE · TOGETHER", fontFamily = DMSans, fontSize = 9.sp, letterSpacing = 1.2.sp, color = Color(0xE6FFFFFF))
                }
            }
            Spacer(Modifier.height(10.dp))
            ConfirmationSuccessAnimation(reference)
            Spacer(Modifier.height(8.dp))
            val heroShadow = Shadow(Color(0x66000000), Offset(0f, 2f), 8f)
            Text("Booking Confirmed!", fontFamily = DMSans, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                color = Color.White, style = TextStyle(shadow = heroShadow))
            Text(
                "Your QR code opens the ${facility.name.lowercase()} gate automatically",
                Modifier.padding(horizontal = 24.dp, vertical = 4.dp), fontFamily = DMSans, fontSize = 16.sp,
                color = Color.White, textAlign = TextAlign.Center, style = TextStyle(shadow = heroShadow)
            )
            Spacer(Modifier.height(14.dp))
            AnimatedQrPass(qrContent, reference)
            Spacer(Modifier.height(10.dp))
            ScanHint(facility.name, hour)
            Spacer(Modifier.height(16.dp))
            BookingDetailsCard(facility, day, hour, guests)
            Spacer(Modifier.height(12.dp))
            PaidSummaryCard(facility.deposit, method)
            Spacer(Modifier.height(14.dp))
            ActionButton(
                "Add to Calendar", Icons.Outlined.CalendarMonth, FacilityInk,
                Modifier.fillMaxWidth().height(58.dp),
                background = Brush.horizontalGradient(listOf(Color(0xFF9AF0D2), Color(0xFF7FE6C4)))
            ) {
                addToCalendar(context, facility, day, hour, reference)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("View Booking", Icons.Outlined.Description, FacilityInk, Modifier.weight(1f).height(54.dp)) {
                    navController.navigate(Screen.BookingDetail.create(booking.id))
                }
                ActionButton("Cancel Booking", Icons.Outlined.Delete, ConfirmedRed, Modifier.weight(1f).height(54.dp)) {
                    confirmCancel = true
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ConfirmationSuccessAnimation(reference: String) {
    val scale = remember(reference) { Animatable(0f) }
    val check = remember(reference) { Animatable(0f) }
    val ripple = remember(reference) { Animatable(0f) }
    LaunchedEffect(reference) {
        launch {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
        launch { ripple.animateTo(1f, tween(durationMillis = 1000, delayMillis = 150)) }
        check.animateTo(1f, tween(durationMillis = 420, delayMillis = 250))
    }
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val progress = ripple.value
            drawCircle(
                Color.White.copy(alpha = .5f * (1f - progress)),
                radius = size.minDimension * (.32f + .18f * progress),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Canvas(
            Modifier.size(64.dp).graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = scale.value.coerceIn(0f, 1f)
            }
        ) {
            drawCircle(ConfirmedDeepGreen)
            drawCircle(Color.White.copy(alpha = .65f), style = Stroke(2.dp.toPx()))
            val start = Offset(size.width * .27f, size.height * .51f)
            val bend = Offset(size.width * .44f, size.height * .67f)
            val end = Offset(size.width * .75f, size.height * .34f)
            val progress = check.value
            val path = Path().apply {
                moveTo(start.x, start.y)
                val first = (progress / .35f).coerceIn(0f, 1f)
                lineTo(start.x + (bend.x - start.x) * first, start.y + (bend.y - start.y) * first)
                if (progress > .35f) {
                    val second = (progress - .35f) / .65f
                    lineTo(bend.x + (end.x - bend.x) * second, bend.y + (end.y - bend.y) * second)
                }
            }
            if (progress > 0f) drawPath(path, Color.White, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun AnimatedQrPass(content: String, reference: String) {
    val reveal = remember(reference) { Animatable(0f) }
    LaunchedEffect(reference) {
        reveal.animateTo(1f, tween(durationMillis = 450, delayMillis = 450))
    }
    Box(Modifier.graphicsLayer {
        alpha = reveal.value
        translationY = 20.dp.toPx() * (1f - reveal.value)
    }) {
        QrPass(content, reference)
    }
}

private fun addToCalendar(context: android.content.Context, facility: Facility, day: BookingDay, hour: Int, reference: String) {
    val start = day.toCalendar().apply { set(java.util.Calendar.HOUR_OF_DAY, hour) }.timeInMillis
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 60 * 60 * 1000L)
        .putExtra(CalendarContract.Events.TITLE, "${facility.name} booking")
        .putExtra(CalendarContract.Events.EVENT_LOCATION, "Green Gate · ${facility.name}")
        .putExtra(CalendarContract.Events.DESCRIPTION, "Booking $reference. Scan your QR pass at the ${facility.name} gate on arrival.")
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun QrPass(content: String, reference: String) {
    val matrix = remember(content) {
        QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 0, 0,
            // High correction leaves room for the logo over the centre.
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0)
        )
    }
    Box(
        Modifier.shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x667FE6C4), spotColor = Color(0x667FE6C4))
            .clip(RoundedCornerShape(28.dp)).background(Color(0xE6E9FBF4))
            .border(2.dp, Color(0xCCBDF3DF), RoundedCornerShape(28.dp))
            .padding(14.dp)
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp)
                .semantics { contentDescription = "QR pass for booking $reference" },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(184.dp)) {
                val cell = size.width / matrix.width
                for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                    if (matrix[x, y]) drawRect(QrInk, Offset(x * cell, y * cell), Size(cell + .5f, cell + .5f))
                }
            }
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.greengate_logo), null, Modifier.size(40.dp))
            }
        }
    }
}

@Composable
internal fun ScanHint(facilityName: String, hour: Int, hours: Int = 1) {
    Row(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xCCFFFFFF))
            .border(1.dp, FacilityGlassBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.QrCodeScanner, null, tint = FacilityInk, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Scan at $facilityName gate on arrival", fontFamily = DMSans, fontSize = 14.sp, color = FacilityInk)
            Text(
                "Valid: ${clockLabel(hour * 60 - GraceMinutes)} – ${clockLabel((hour + hours) * 60 + GraceMinutes)}",
                fontFamily = DMSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ConfirmedDeepGreen
            )
        }
    }
}

@Composable
private fun ConfirmedCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().shadow(2.dp, shape, ambientColor = Color(0x1A0F3B33), spotColor = Color(0x1A0F3B33))
            .clip(shape).background(Color(0xCCFFFFFF)).border(1.5.dp, FacilityGlassBorder, shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun BookingDetailsCard(facility: Facility, day: BookingDay, hour: Int, guests: Int) {
    ConfirmedCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Booking Details", Modifier.weight(1f), fontFamily = DMSans, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, color = FacilityInk)
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(ConfirmedMint).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = ConfirmedDeepGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirmed", fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
            }
        }
        Spacer(Modifier.height(6.dp))
        DetailRow(icon = { Image(painterResource(facility.artwork), null, Modifier.size(24.dp)) }, "Facility", facility.name)
        HorizontalDivider(Modifier.padding(start = 52.dp), color = ConfirmedDivider)
        DetailRow(Icons.Outlined.CalendarMonth, "Date", day.longLabel())
        HorizontalDivider(Modifier.padding(start = 52.dp), color = ConfirmedDivider)
        DetailRow(Icons.Outlined.Schedule, "Time", "${hourLabel(hour)} – ${hourLabel(hour + 1)}")
        HorizontalDivider(Modifier.padding(start = 52.dp), color = ConfirmedDivider)
        DetailRow(Icons.Outlined.Groups, "No. of Guests", if (guests == 1) "1 Person" else "$guests People")
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) =
    DetailRow({ Icon(icon, null, tint = ConfirmedDeepGreen, modifier = Modifier.size(22.dp)) }, label, value)

@Composable
private fun DetailRow(icon: @Composable () -> Unit, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(ConfirmedMint), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(.8f), fontFamily = DMSans, fontSize = 14.sp, color = FacilityMuted)
        Text(value, Modifier.weight(1.4f), fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
    }
}

@Composable
private fun PaidSummaryCard(deposit: Int, method: PaymentMethod) {
    ConfirmedCard {
        Text("Payment Summary", fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
        Spacer(Modifier.height(6.dp))
        // Booking fees aren't charged yet; the row stays so the breakdown matches the payment page.
        PaidRow(Icons.Outlined.AccountBalanceWallet, "Booking Fee", 0)
        if (deposit > 0) {
            HorizontalDivider(Modifier.padding(start = 52.dp), color = ConfirmedDivider)
            PaidRow(Icons.Outlined.VerifiedUser, "Security Deposit (Refundable)", deposit)
        }
        HorizontalDivider(Modifier.padding(start = 52.dp), color = ConfirmedDivider)
        Row(Modifier.fillMaxWidth().padding(start = 52.dp, top = 12.dp), verticalAlignment = Alignment.Top) {
            Text("Total Paid", Modifier.weight(1f).padding(top = 6.dp), fontFamily = DMSans, fontSize = 18.sp,
                fontWeight = FontWeight.Medium, color = FacilityInk)
            Column(horizontalAlignment = Alignment.End) {
                Text("S$$deposit.00", fontFamily = DMSans, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ConfirmedDeepGreen)
                Text(if (deposit > 0) "Paid via ${method.title}" else "Nothing to pay",
                    fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
            }
        }
        if (deposit > 0) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x99E6F3F0)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Info, null, tint = FacilityInk, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "The security deposit will be refunded to your wallet after the booking, subject to facility terms and no damages.",
                    fontFamily = DMSans, fontSize = 13.sp, lineHeight = 18.sp, color = FacilityMuted
                )
            }
        }
    }
}

@Composable
private fun PaidRow(icon: ImageVector, label: String, amount: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(ConfirmedMint), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = ConfirmedDeepGreen, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontFamily = DMSans, fontSize = 15.sp, color = FacilityInk)
        Text("S$$amount.00", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
    }
}

@Composable
private fun ActionButton(
    label: String, icon: ImageVector, tint: Color, modifier: Modifier,
    background: Brush = Brush.linearGradient(listOf(Color(0xE6FFFFFF), Color(0xCCF2FAF7))),
    onClick: () -> Unit
) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(background)
            .border(1.5.dp, FacilityGlassBorder, RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}
