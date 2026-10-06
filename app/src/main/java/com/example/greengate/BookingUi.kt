package com.example.greengate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greengate.ui.theme.DMSans

// Shared by My Bookings, Booking Details, Payments & Deposits and Transaction Details.
internal val BookingsBackdrop = Brush.verticalGradient(
    0f to Color(0xFFBFE3E6), 0.3f to Color(0xFFDCEFEE), 1f to Color(0xFFE8F5F2)
)
internal val BookingsDeepGreen = Color(0xFF0F6B54)
internal val BookingsMint = Color(0xFFDDF5EC)
internal val BookingsDivider = Color(0x1F14232B)
internal val BookingsRed = Color(0xFFD93A2B)

/** A status chip, e.g. a green "Confirmed" or an orange "Held". */
internal class StatusTone(val label: String, val color: Color, val background: Color)

internal fun greenTone(label: String) = StatusTone(label, FacilityGreen, BookingsMint)
internal fun orangeTone(label: String) = StatusTone(label, FacilityOrange, Color(0xFFFFEEDF))
internal fun redTone(label: String) = StatusTone(label, BookingsRed, Color(0xFFFDE7E4))

@Composable
internal fun StatusPill(tone: StatusTone, small: Boolean = false) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(tone.background)
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(if (small) 6.dp else 8.dp).clip(CircleShape).background(tone.color))
        Spacer(Modifier.width(if (small) 4.dp else 6.dp))
        Text(tone.label, fontFamily = DMSans, fontSize = if (small) 11.sp else 13.sp, fontWeight = FontWeight.Medium, color = tone.color)
    }
}

@Composable
internal fun GlassTopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.align(Alignment.CenterStart), onBack)
        Text(title, Modifier.align(Alignment.Center), fontFamily = DMSans, fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold, color = FacilityInk)
        if (action != null) Box(Modifier.align(Alignment.CenterEnd)) { action() }
    }
}

@Composable
internal fun GlassIconButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(48.dp).shadow(6.dp, CircleShape, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(CircleShape).background(Color(0xF2FFFFFF))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = FacilityInk, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().shadow(2.dp, shape, ambientColor = Color(0x1A0F3B33), spotColor = Color(0x1A0F3B33))
            .clip(shape).background(Color(0xB3FFFFFF)).border(1.5.dp, FacilityGlassBorder, shape)
            .padding(padding),
        content = content
    )
}

/** The facility's 3D artwork on a soft tile. */
@Composable
internal fun ArtworkTile(facility: Facility, size: Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * .2f))
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFFDDF0EA)))),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(facility.artwork), null, Modifier.size(size * .8f))
    }
}

/** Rounded pill button; [background] defaults to frosted glass. */
@Composable
internal fun PillButton(
    label: String, modifier: Modifier, tint: Color = FacilityInk, icon: ImageVector? = null,
    background: Brush = Brush.linearGradient(listOf(Color(0xE6FFFFFF), Color(0xCCF2FAF7))),
    onClick: () -> Unit
) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(background)
            .border(1.dp, FacilityGlassBorder, RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

internal val MintButton = Brush.horizontalGradient(listOf(Color(0xFF9AF0D2), Color(0xFF7FE6C4)))
internal val RedTintButton = Brush.linearGradient(listOf(Color(0xFFFDE9E6), Color(0xFFFBDCD7)))

/** Upcoming bookings read "Confirmed" once their QR pass is ready, "Upcoming" before that. */
internal fun Booking.statusTone(now: Long = System.currentTimeMillis()) = when (status(now)) {
    BookingStatus.UPCOMING -> if (qrReady(now)) greenTone("Confirmed") else orangeTone("Upcoming")
    BookingStatus.COMPLETED -> greenTone("Completed")
    BookingStatus.CANCELLED -> redTone("Cancelled")
}
