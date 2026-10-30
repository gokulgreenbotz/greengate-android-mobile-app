package com.greenbotz.greengate.ui.screens.facilities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.greenbotz.greengate.core.facility.Booking
import com.greenbotz.greengate.core.facility.BookingStatus
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.AppTheme
import com.greenbotz.greengate.ui.screens.home.SilkLines
import com.greenbotz.greengate.ui.screens.visitors.tone
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.NavyGoldButton
import com.greenbotz.greengate.ui.theme.NavyGoldCream
import com.greenbotz.greengate.ui.theme.NavyGoldNavy
import com.greenbotz.greengate.ui.theme.NavyGoldPage
import com.greenbotz.greengate.ui.theme.NavyGoldShadow
import com.greenbotz.greengate.ui.theme.NavyGoldTheme
import com.greenbotz.greengate.ui.theme.PrimaryGreen
import com.greenbotz.greengate.ui.theme.PrimaryGreenDark
import com.greenbotz.greengate.ui.theme.SoftGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

// Theme 4 - Navy & Gold swaps these greens for navy, gold and warm white (see NavyGold.kt); checked first below.

/** A primary button's green, lighter at the top; [glassTop] is that top colour in the glass theme. Navy in Theme 4. */
internal fun greenButton(glassTop: Color): Brush =
    if (NavyGoldTheme) NavyGoldButton else Brush.verticalGradient(listOf(themed(glassTop, PrimaryGreen), BookingsDeepGreen))

// Shared by My Bookings, Booking Details, Payments & Deposits and Transaction Details.
private val GlassBackdrop = Brush.verticalGradient(
    0f to Color(0xFFBFE3E6), 0.3f to Color(0xFFDCEFEE), 1f to Color(0xFFE8F5F2)
)
internal val BookingsBackdrop: Brush get() = if (NavyGoldTheme) SolidColor(NavyGoldPage) else themed(GlassBackdrop, ClassicBackdrop)
internal val BookingsDeepGreen get() = if (NavyGoldTheme) NavyGoldNavy else themed(Color(0xFF0F6B54), PrimaryGreenDark)
/** Soft accent tiles and fills: mint, or a pale navy in Theme 4. */
internal val BookingsMint get() = if (NavyGoldTheme) Color(0xFFEEF1F8) else themed(Color(0xFFDDF5EC), SoftGreen)

/** A full booking screen's background: [BookingsBackdrop], plus the Home screen's silk lines in Theme 4. */
@Composable
internal fun BookingsScreen(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        if (NavyGoldTheme) SilkLines(Modifier.fillMaxWidth().height(260.dp))
        content()
    }
}
internal val BookingsDivider = Color(0x1F14232B)
internal val BookingsRed = Color(0xFFD93A2B)

/** A status chip, e.g. a green "Confirmed" or an orange "Held". */
internal class StatusTone(val label: String, val color: Color, val background: Color)

// Status greens stay green in Theme 4; only the mint behind them is pinned, as BookingsMint turns navy there.
internal fun greenTone(label: String) = StatusTone(label, FacilityGreen, if (NavyGoldTheme) Color(0xFFE3F4EC) else BookingsMint)
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
            fontFamily = DMSans, fontSize = 22.sp, fontWeight = if (NavyGoldTheme) FontWeight.Bold else FontWeight.SemiBold, color = FacilityInk)
        // Without an action, an empty space as wide as Back keeps the title centred.
        if (action != null) action() else Spacer(Modifier.size(48.dp))
    }
}

@Composable
internal fun GlassIconButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shadow = if (NavyGoldTheme) NavyGoldShadow else Color(0x22000000)
    Box(
        modifier.size(48.dp).shadow(6.dp, CircleShape, ambientColor = shadow, spotColor = shadow)
            .clip(CircleShape).background(if (NavyGoldTheme) Color.White else Color(0xF2FFFFFF))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = FacilityInk, modifier = Modifier.size(22.dp))
    }
}

/**
 * A round icon button that first opens as a pill with [label] beside the icon, then folds the
 * label away after a few seconds. Shows once per visit: coming back to the screen keeps it folded.
 * Filled with the booking green (navy with a gold edge in Theme 4) so it isn't mistaken for the
 * white Back button beside it, and a soft halo ripples out from it a few times while the label shows.
 */
@Composable
internal fun FoldingLabelButton(
    icon: ImageVector, label: String, size: Dp = 48.dp, iconSize: Dp = 22.dp,
    ambientShadow: Color = Color(0x22000000), spotShadow: Color = Color(0x22000000), onClick: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    // 0 to 1 as one halo grows and fades; 0 draws nothing.
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!expanded) return@LaunchedEffect
        launch {
            // Wait out the screen's slide-in, so the first halo is seen.
            delay(PulseDelayMillis)
            repeat(PulseCount) {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(PulseMillis, easing = LinearOutSlowInEasing))
                delay(PulseGapMillis)
            }
            pulse.snapTo(0f)
        }
        delay(FoldingLabelMillis)
        expanded = false
    }
    val haloColor = if (NavyGoldTheme) Color(0xFFD9B36A) else FacilityGreen
    Row(
        Modifier
            // Before the clip, so the halo can spread past the button's edge.
            .drawBehind {
                val p = pulse.value
                if (p > 0f) {
                    val grow = p * PulseSpread.toPx()
                    drawRoundRect(
                        haloColor.copy(alpha = (1f - p) * .45f),
                        topLeft = Offset(-grow, -grow),
                        size = Size(this.size.width + grow * 2, this.size.height + grow * 2),
                        cornerRadius = CornerRadius(this.size.height / 2 + grow)
                    )
                }
            }
            .height(size).shadow(6.dp, CircleShape, ambientColor = ambientShadow, spotColor = spotShadow)
            .clip(CircleShape).background(greenButton(Color(0xFF2AA07D)))
            .then(if (NavyGoldTheme) Modifier.border(1.dp, Color(0xFFD9B36A), CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = (size - iconSize) / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, if (expanded) null else label, tint = Color.White, modifier = Modifier.size(iconSize))
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(FoldingLabelSpring, expandFrom = Alignment.Start) + fadeIn(),
            // The label fades out ahead of the shrink, so it never looks cut off by the closing edge.
            exit = shrinkHorizontally(FoldingLabelSpring, shrinkTowards = Alignment.Start) + fadeOut(tween(180))
        ) {
            Text(label, Modifier.padding(start = 8.dp, end = 4.dp), maxLines = 1,
                fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

private val FoldingLabelSpring = spring<IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
// Long enough to notice and read the label well after the 380 ms slide-in.
private const val FoldingLabelMillis = 5000L
// Three halos, finished just before the label folds.
private const val PulseDelayMillis = 400L
private const val PulseCount = 3
private const val PulseMillis = 1100
private const val PulseGapMillis = 300L
private val PulseSpread = 10.dp

@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier, padding: Dp = 16.dp, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    // Theme 4: a solid white card with a hairline edge and a soft navy shadow.
    val navyGold = NavyGoldTheme
    val shadow = if (navyGold) NavyGoldShadow else Color(0x1A0F3B33)
    Column(
        modifier.fillMaxWidth().shadow(if (navyGold) 4.dp else 2.dp, shape, ambientColor = shadow, spotColor = shadow)
            .clip(shape).background(if (navyGold) Color.White else themed(Color(0xB3FFFFFF), ClassicCream))
            .border(if (navyGold) 1.dp else 1.5.dp, FacilityGlassBorder, shape)
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
            .background(Brush.linearGradient(listOf(Color.White, if (NavyGoldTheme) NavyGoldCream else themed(Color(0xFFDDF0EA), ClassicMint)))),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(facility.themedArtwork), null, Modifier.size(size * .8f))
    }
}

/** Rounded pill button; [background] defaults to frosted glass, or plain white in Classic and Theme 4. */
@Composable
internal fun PillButton(
    label: String, modifier: Modifier, tint: Color = FacilityInk, icon: ImageVector? = null,
    background: Brush = if (NavyGoldTheme) SolidColor(Color.White)
        else themed(Brush.linearGradient(listOf(Color(0xE6FFFFFF), Color(0xCCF2FAF7))), SolidColor(Color.White)),
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
// Theme 4's soft secondary button: cream into light gold, under navy type.
private val GoldCreamButton = Brush.verticalGradient(listOf(Color(0xFFFBF1DC), Color(0xFFF1DDB4)))
internal val MintButton get() = if (NavyGoldTheme) GoldCreamButton else themed(GlassMintButton, SolidColor(SoftGreen))
internal val RedTintButton = Brush.linearGradient(listOf(Color(0xFFFDE9E6), Color(0xFFFBDCD7)))

/** Upcoming bookings read "Confirmed" once their QR pass is ready, "Upcoming" before that. */
internal fun Booking.statusTone(now: Long = System.currentTimeMillis()) = when (status(now)) {
    BookingStatus.UPCOMING -> if (qrReady(now)) greenTone("Confirmed") else orangeTone("Upcoming")
    BookingStatus.COMPLETED -> greenTone("Completed")
    BookingStatus.CANCELLED -> redTone("Cancelled")
}
