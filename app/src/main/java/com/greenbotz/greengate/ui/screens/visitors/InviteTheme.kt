package com.greenbotz.greengate.ui.screens.visitors

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
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
import com.greenbotz.greengate.R
import com.greenbotz.greengate.ui.screens.facilities.BookingsDeepGreen
import com.greenbotz.greengate.ui.screens.facilities.BookingsMint
import com.greenbotz.greengate.ui.screens.facilities.BookingsScreen
import com.greenbotz.greengate.ui.screens.facilities.FacilityGreen
import com.greenbotz.greengate.ui.screens.facilities.FacilityInk
import com.greenbotz.greengate.ui.screens.facilities.FacilityMuted
import com.greenbotz.greengate.ui.screens.facilities.GlassCard
import com.greenbotz.greengate.ui.screens.facilities.GlassIconButton
import com.greenbotz.greengate.ui.screens.facilities.GlassTopBar
import com.greenbotz.greengate.ui.screens.facilities.QrPass
import com.greenbotz.greengate.ui.screens.facilities.greenButton
import com.greenbotz.greengate.ui.screens.facilities.themed
import com.greenbotz.greengate.ui.theme.BackgroundCream
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.NavyGoldButton
import com.greenbotz.greengate.ui.theme.NavyGoldGold
import com.greenbotz.greengate.ui.theme.NavyGoldInk
import com.greenbotz.greengate.ui.theme.NavyGoldLine
import com.greenbotz.greengate.ui.theme.NavyGoldMuted
import com.greenbotz.greengate.ui.theme.NavyGoldShadow
import com.greenbotz.greengate.ui.theme.NavyGoldTheme

// Theme 4 - Navy & Gold dresses the whole visitor invite flow (the Create Invite sheet and its forms,
// Invite Created, Visitor Management and Invite Details) in navy and gold on warm white, like its Home.
// The facility palette and components already switch for Theme 4, so most of these are names for them;
// the rest (gold accents, the sheet's cards) are the invite flow's own.
internal val NavyGoldInvites: Boolean get() = NavyGoldTheme

internal val InviteInk get() = FacilityInk
internal val InviteMuted get() = FacilityMuted
/** Field icons, links, the selected tab and the date picker's selection. */
internal val InviteDeep get() = BookingsDeepGreen
/** Switches and the edge of a selected chip. */
internal val InviteAccent get() = if (NavyGoldInvites) NavyGoldGold else FacilityGreen
/** Timeline tiles and the rail between them. */
internal val InviteTile get() = BookingsMint
internal val InviteButtonShadow get() = if (NavyGoldInvites) Color(0x400B1446) else Color(0x330F6B54)
/** The track behind a row of tabs. */
internal val InviteSegmentFill get() = if (NavyGoldInvites) Color(0xFFF1EDE6) else themed(Color(0x99FFFFFF), BackgroundCream)

/** Theme 4's primary button fill. */
internal val NavyButton = NavyGoldButton

/** A primary button's fill: the facility green with [glassTop] at the top; navy in Theme 4. */
internal fun inviteButton(glassTop: Color): Brush = greenButton(glassTop)

/** A full invite screen's background; Theme 4 adds the Home screen's silk lines. */
@Composable
internal fun InviteScreen(content: @Composable () -> Unit) = BookingsScreen(content)

/** A card: frosted glass, or white with a hairline edge in Theme 4. */
@Composable
internal fun InviteCard(
    modifier: Modifier = Modifier, padding: Dp = 16.dp, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = GlassCard(modifier, padding, onClick, content)

@Composable
internal fun InviteTopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) =
    GlassTopBar(title, onBack, action)

@Composable
internal fun InviteIconButton(icon: ImageVector, description: String, onClick: () -> Unit) =
    GlassIconButton(icon, description, onClick = onClick)

/** The visitor's QR pass; the facility's [QrPass] already dresses itself for Theme 4. */
@Composable
internal fun InviteQrPass(content: String, reference: String) = QrPass(content, reference, "QR pass for invite $reference")

/** A Theme 4 icon on its soft round backdrop: warm for the visit types, pale blue for [blue]. */
@Composable
internal fun NavyGoldArtCircle(@DrawableRes art: Int, size: Dp, modifier: Modifier = Modifier, iconFraction: Float = .68f, blue: Boolean = false) {
    val tones = if (blue) listOf(Color(0xFFF4F7FC), Color(0xFFE2E9F5)) else listOf(Color(0xFFFBFAF7), Color(0xFFEFEBE4))
    Box(
        modifier.size(size).clip(CircleShape).background(Brush.radialGradient(tones)),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(art), null, Modifier.size(size * iconFraction))
    }
}

/** A visit type in the Theme 4 Create Invite sheet: icon on top, then title, subtitle and a chevron. */
@Composable
internal fun NavyGoldTypeCard(type: InviteType, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.shadow(4.dp, shape, ambientColor = NavyGoldShadow, spotColor = NavyGoldShadow).clip(shape)
            .background(Color.White).border(1.dp, NavyGoldLine, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 14.dp)
    ) {
        NavyGoldArtCircle(type.art(), 88.dp, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(type.title, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NavyGoldInk, maxLines = 1)
                Text(type.subtitle.replace('\n', ' '), fontFamily = DMSans, fontSize = 12.sp, lineHeight = 16.sp, color = NavyGoldMuted)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = NavyGoldInk, modifier = Modifier.size(22.dp))
        }
    }
}

/** The Theme 4 bar under the visit types that opens Visitor Management. */
@Composable
internal fun NavyGoldHistoryBar(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().height(66.dp).shadow(3.dp, shape, ambientColor = NavyGoldShadow, spotColor = NavyGoldShadow).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFF0F4FB))))
            .border(1.dp, Color(0xFFE4EAF5), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavyGoldArtCircle(R.drawable.theme4_invite_history, 46.dp, iconFraction = .6f, blue = true)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Visitor History", fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NavyGoldInk)
            Text("Active, upcoming and past invites", fontFamily = DMSans, fontSize = 13.sp, color = NavyGoldMuted, maxLines = 1)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = NavyGoldInk, modifier = Modifier.size(24.dp))
    }
}
