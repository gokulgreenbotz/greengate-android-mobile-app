package com.example.greengate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greengate.ui.theme.DMSans
import com.example.greengate.ui.theme.PrimaryGreen
import com.example.greengate.ui.theme.PrimaryGreenDark
import com.example.greengate.ui.theme.SoftGreen
import kotlinx.coroutines.delay

// Theme 1 - Classic trades the glass for the plain white, cream and sage of its Home screen.
// AppPreferences.theme is snapshot state, so composables reading these recompose on a theme change.
internal val ClassicTheme: Boolean get() = AppPreferences.theme == AppTheme.ONE

/** [glass] normally; [classic] under Theme 1 - Classic. */
internal fun themed(glass: Color, classic: Color) = if (ClassicTheme) classic else glass
internal fun themed(glass: Brush, classic: Brush) = if (ClassicTheme) classic else glass

internal val ClassicCream = Color(0xFFFCF9F3)
internal val ClassicLine = Color(0xFFEAE3D6)
internal val ClassicMint = Color(0xFFEDF3EA)
internal val ClassicBackdrop = SolidColor(Color.White)

/** A primary button's green, lighter at the top; [glassTop] is that top colour in the glass theme. */
internal fun greenButton(glassTop: Color) = Brush.verticalGradient(listOf(themed(glassTop, PrimaryGreen), BookingsDeepGreen))

// Shared by My Bookings, Booking Details, Payments & Deposits and Transaction Details.
private val GlassBackdrop = Brush.verticalGradient(
    0f to Color(0xFFBFE3E6), 0.3f to Color(0xFFDCEFEE), 1f to Color(0xFFE8F5F2)
)
internal val BookingsBackdrop get() = themed(GlassBackdrop, ClassicBackdrop)
internal val BookingsDeepGreen get() = themed(Color(0xFF0F6B54), PrimaryGreenDark)
internal val BookingsMint get() = themed(Color(0xFFDDF5EC), SoftGreen)
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
    // A row rather than a centred box, so the title makes room for a widening action such as a FoldingLabelButton.
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
        Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontFamily = DMSans, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
        // Without an action, an empty space as wide as Back keeps the title centred.
        if (action != null) action() else Spacer(Modifier.size(48.dp))
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

/**
 * A round glass icon button that first opens as a pill with [label] beside the icon, then folds the
 * label away after a few seconds. Shows once per visit: coming back to the screen keeps it folded.
 */
@Composable
internal fun FoldingLabelButton(
    icon: ImageVector, label: String, size: Dp = 48.dp, iconSize: Dp = 22.dp,
    ambientShadow: Color = Color(0x22000000), spotShadow: Color = Color(0x22000000), onClick: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (!expanded) return@LaunchedEffect
        delay(FoldingLabelMillis)
        expanded = false
    }
    Row(
        Modifier.height(size).shadow(6.dp, CircleShape, ambientColor = ambientShadow, spotColor = spotShadow)
            .clip(CircleShape).background(Color(0xF2FFFFFF))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = (size - iconSize) / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, if (expanded) null else label, tint = FacilityInk, modifier = Modifier.size(iconSize))
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(FoldingLabelSpring, expandFrom = Alignment.Start) + fadeIn(),
            // The label fades out ahead of the shrink, so it never looks cut off by the closing edge.
            exit = shrinkHorizontally(FoldingLabelSpring, shrinkTowards = Alignment.Start) + fadeOut(tween(180))
        ) {
            Text(label, Modifier.padding(start = 8.dp, end = 4.dp), maxLines = 1,
                fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
        }
    }
}

private val FoldingLabelSpring = spring<IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
// Long enough to read past the 380 ms slide-in.
private const val FoldingLabelMillis = 2500L

@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier, padding: Dp = 16.dp, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().shadow(2.dp, shape, ambientColor = Color(0x1A0F3B33), spotColor = Color(0x1A0F3B33))
            .clip(shape).background(themed(Color(0xB3FFFFFF), ClassicCream)).border(1.5.dp, FacilityGlassBorder, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

/** The facility's 3D artwork on a soft tile. */
@Composable
internal fun ArtworkTile(facility: Facility, size: Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * .2f))
            .background(Brush.linearGradient(listOf(Color.White, themed(Color(0xFFDDF0EA), ClassicMint)))),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(facility.artwork), null, Modifier.size(size * .8f))
    }
}

/** Rounded pill button; [background] defaults to frosted glass, or plain white in Classic. */
@Composable
internal fun PillButton(
    label: String, modifier: Modifier, tint: Color = FacilityInk, icon: ImageVector? = null,
    background: Brush = themed(Brush.linearGradient(listOf(Color(0xE6FFFFFF), Color(0xCCF2FAF7))), SolidColor(Color.White)),
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

private val GlassMintButton = Brush.horizontalGradient(listOf(Color(0xFF9AF0D2), Color(0xFF7FE6C4)))
internal val MintButton get() = themed(GlassMintButton, SolidColor(SoftGreen))
internal val RedTintButton = Brush.linearGradient(listOf(Color(0xFFFDE9E6), Color(0xFFFBDCD7)))

/** Upcoming bookings read "Confirmed" once their QR pass is ready, "Upcoming" before that. */
internal fun Booking.statusTone(now: Long = System.currentTimeMillis()) = when (status(now)) {
    BookingStatus.UPCOMING -> if (qrReady(now)) greenTone("Confirmed") else orangeTone("Upcoming")
    BookingStatus.COMPLETED -> greenTone("Completed")
    BookingStatus.CANCELLED -> redTone("Cancelled")
}
