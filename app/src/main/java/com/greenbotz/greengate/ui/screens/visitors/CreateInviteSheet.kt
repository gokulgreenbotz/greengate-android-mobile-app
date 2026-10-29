package com.greenbotz.greengate.ui.screens.visitors

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.visitor.VisitorInvite
import com.greenbotz.greengate.ui.screens.facilities.ClassicCream
import com.greenbotz.greengate.ui.screens.facilities.ClassicLine
import com.greenbotz.greengate.ui.screens.facilities.ClassicMint
import com.greenbotz.greengate.ui.screens.facilities.themed
import com.greenbotz.greengate.ui.screens.home.SilkLines
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.SoftGreen
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal enum class InviteType(val title: String, val subtitle: String) {
    FAMILY("Family / Friend", "For people you know"),
    DELIVERY("Delivery", "Food, parcel or courier"),
    CAB("Cab", "Pickup or drop-off"),
    OTHER("Other", "Contractor, technician,\nservice & more"),
}

/**
 * Slides up from the bottom when a resident starts a visitor invite. Every visit type's invite is
 * made right here ([onCreated] gets the saved invite). Dragging the sheet down doesn't close it,
 * so a half-filled form isn't lost; use Close or back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateInviteSheet(
    onDismiss: () -> Unit,
    onHistory: (() -> Unit)? = null,
    onCreated: (VisitorInvite) -> Unit,
) {
    // Vetoing Hidden stops a drag (or scrim tap) from dismissing; hide() below still animates out.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden })
    val scope = rememberCoroutineScope()
    var form by rememberSaveable { mutableStateOf<InviteType?>(null) }
    // Let the sheet slide away before it leaves composition.
    fun close(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        BackHandler(enabled = form != null) { form = null }
        Box {
            // Theme 4's silk lines sweep across the top of the sheet, from behind the drag handle.
            if (NavyGoldInvites) SilkLines(Modifier.offset(y = (-34).dp).fillMaxWidth().height(240.dp))
            AnimatedContent(form, label = "inviteType") { shown ->
                val onClose = { close(onDismiss) }
                val created = { invite: VisitorInvite -> close { onCreated(invite) } }
                val backToTypes = { form = null }
                when (shown) {
                    InviteType.FAMILY -> FamilyInviteForm(onBack = backToTypes, onClose = onClose, onCreated = created)
                    InviteType.DELIVERY -> DeliveryInviteForm(onBack = backToTypes, onClose = onClose, onCreated = created)
                    InviteType.CAB -> CabInviteForm(onClose = onClose, onCreated = created)
                    InviteType.OTHER -> OtherInviteForm(onClose = onClose, onCreated = created)
                    null -> InviteTypes(onHistory?.let { { close(it) } }, onClose = onClose) { form = it }
                }
            }
        }
    }
}

/** A visit type's art beside an invite form's title: on its soft circle in Theme 4. */
@Composable
internal fun InviteHeaderArt(type: InviteType) {
    if (NavyGoldInvites) NavyGoldArtCircle(type.art(), 58.dp)
    else InviteTypeArt(type, Modifier.requiredSize(72.dp), blend = false)
}

/** Top of an invite form in the sheet, laid out like Invite Cab's; [onBack] adds a back arrow, a null title, subtitle or art is left out. */
@Composable
internal fun InviteFormHeader(
    title: String?, subtitle: String?, onBack: (() -> Unit)?, onClose: () -> Unit, art: (@Composable () -> Unit)?,
) {
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = InviteInk, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(4.dp))
        }
        if (art != null) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) { art() }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (title != null) Text(title, fontFamily = DMSans, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InviteInk)
            if (subtitle != null) Text(subtitle, fontFamily = DMSans, fontSize = 13.sp, color = InviteMuted)
        }
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, "Close", tint = InviteInk, modifier = Modifier.size(24.dp))
        }
    }
}

/** The primary button pinned under an invite form in the sheet: green, or navy in Theme 4. */
@Composable
internal fun InviteFormButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 20.dp).fillMaxWidth().height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(inviteButton(Color(0xFF2E9A79)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun InviteTypes(onHistory: (() -> Unit)?, onClose: () -> Unit, onSelect: (InviteType) -> Unit) {
    val navyGold = NavyGoldInvites
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Create Visitor Invite", Modifier.weight(1f), fontFamily = DMSans, fontSize = if (navyGold) 25.sp else 22.sp,
                fontWeight = FontWeight.Bold, color = InviteInk)
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, "Close", tint = InviteInk, modifier = Modifier.size(if (navyGold) 26.dp else 24.dp))
            }
        }
        Text("Who are you inviting?", fontFamily = DMSans, fontSize = if (navyGold) 16.sp else 15.sp, color = InviteMuted)
        Spacer(Modifier.height(18.dp))
        InviteType.entries.chunked(2).forEach { row ->
            // Theme 4's cards share their row's height, as Other's subtitle runs to two lines.
            Row(Modifier.padding(bottom = 14.dp).then(if (navyGold) Modifier.height(IntrinsicSize.Max) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { type ->
                    if (navyGold) NavyGoldTypeCard(type, Modifier.weight(1f).fillMaxHeight()) { onSelect(type) }
                    else InviteTypeCard(type, Modifier.weight(1f)) { onSelect(type) }
                }
            }
        }
        if (onHistory != null) {
            Spacer(Modifier.height(4.dp))
            if (navyGold) NavyGoldHistoryBar(onHistory) else HistoryBar(onHistory)
        }
    }
}

/** Full-width bar under the invite types that opens Visitor Management. */
@Composable
private fun HistoryBar(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().height(60.dp).clip(shape)
            .background(themed(Brush.horizontalGradient(listOf(Color(0xFFE9F7F1), Color(0xFFDDF2EA))), SolidColor(ClassicMint)))
            .border(1.dp, themed(Color(0xFFCDEBDF), SoftGreen), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.History, null, tint = InviteDeep, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Visitor History", fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = InviteInk)
            Text("Active, upcoming and past invites", fontFamily = DMSans, fontSize = 12.sp, color = InviteMuted)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = InviteInk, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun InviteTypeCard(type: InviteType, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.height(178.dp)
            .shadow(6.dp, shape, ambientColor = Color(0x1A0F3B33), spotColor = Color(0x1A0F3B33))
            .clip(shape).background(Brush.verticalGradient(listOf(Color.White, themed(Color(0xFFF6FAF8), ClassicCream))))
            .border(1.dp, themed(Color(0xFFEAF1EE), ClassicLine), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(width = 120.dp, height = 92.dp), contentAlignment = Alignment.Center) {
            // The art's white margin may spill past the box; multiplied onto the card, it doesn't show.
            InviteTypeArt(type, Modifier.requiredSize(112.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(type.title, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InviteInk)
        Spacer(Modifier.height(2.dp))
        Text(type.subtitle, fontFamily = DMSans, fontSize = 12.sp, lineHeight = 16.sp, color = InviteMuted, textAlign = TextAlign.Center)
    }
}

/** A visit type's 3D art; Theme 4 has its own navy and gold set, cut out on transparency. */
@DrawableRes
internal fun InviteType.art() = if (NavyGoldInvites) when (this) {
    InviteType.FAMILY -> R.drawable.theme4_invite_family
    InviteType.DELIVERY -> R.drawable.theme4_invite_delivery
    InviteType.CAB -> R.drawable.theme4_invite_cab
    InviteType.OTHER -> R.drawable.theme4_invite_other
} else when (this) {
    InviteType.FAMILY -> R.drawable.invite_family
    InviteType.DELIVERY -> R.drawable.invite_delivery
    InviteType.CAB -> R.drawable.invite_cab
    InviteType.OTHER -> R.drawable.invite_other
}

/**
 * A visit type's 3D art. It's rendered on white, so by default it's multiplied into whatever is behind
 * it and the white drops out over a tinted backdrop. [blend] = false draws it as is, for the white
 * sheet: there it may sit inside a fading layer, where multiplying has nothing beneath it to blend with.
 * Theme 4's art is already transparent, so it's always drawn as is.
 */
@Composable
internal fun InviteTypeArt(type: InviteType, modifier: Modifier, blend: Boolean = true) {
    if (!blend || NavyGoldInvites) {
        Image(painterResource(type.art()), null, modifier)
        return
    }
    val bitmap = ImageBitmap.imageResource(type.art())
    Canvas(modifier) {
        drawImage(bitmap, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            blendMode = BlendMode.Multiply, filterQuality = FilterQuality.High)
    }
}
