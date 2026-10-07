package com.example.greengate

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greengate.ui.theme.DMSans
import kotlinx.coroutines.launch

internal enum class InviteType(val title: String, val subtitle: String) {
    FAMILY("Family / Friend", "For people you know"),
    DELIVERY("Delivery", "Food, parcel or courier"),
    CAB("Cab", "Pickup or drop-off"),
    OTHER("Other", "Contractor, technician,\ntutor & more"),
}

/**
 * Slides up from the bottom when a resident starts a visitor invite. Family / Friend, Delivery and
 * Cab invites are made right here ([onCreated] gets the saved invite); Other goes to [onSelect].
 * Dragging the sheet down doesn't close it, so a half-filled form isn't lost; use Close or back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateInviteSheet(
    onDismiss: () -> Unit,
    onHistory: (() -> Unit)? = null,
    onCreated: (VisitorInvite) -> Unit,
    onSelect: (InviteType) -> Unit,
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
        AnimatedContent(form, label = "inviteType") { shown ->
            val onClose = { close(onDismiss) }
            val created = { invite: VisitorInvite -> close { onCreated(invite) } }
            val backToTypes = { form = null }
            when (shown) {
                InviteType.FAMILY -> FamilyInviteForm(onBack = backToTypes, onClose = onClose, onCreated = created)
                InviteType.DELIVERY -> DeliveryInviteForm(onBack = backToTypes, onClose = onClose, onCreated = created)
                InviteType.CAB -> CabInviteForm(onClose = onClose, onCreated = created)
                else -> InviteTypes(onHistory?.let { { close(it) } }, onClose = onClose) { type ->
                    // TODO: make Other in the sheet too once its form is designed.
                    if (type == InviteType.OTHER) close { onSelect(type) } else form = type
                }
            }
        }
    }
}

/** Top of an invite form in the sheet, laid out like Invite Cab's; [onBack] adds a back arrow. */
@Composable
internal fun InviteFormHeader(title: String, subtitle: String, onBack: (() -> Unit)?, onClose: () -> Unit, art: @Composable () -> Unit) {
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = FacilityInk, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(4.dp))
        }
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) { art() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = DMSans, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
            Text(subtitle, fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
        }
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, "Close", tint = FacilityInk, modifier = Modifier.size(24.dp))
        }
    }
}

/** The green button pinned under an invite form in the sheet. */
@Composable
internal fun InviteFormButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 20.dp).fillMaxWidth().height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2E9A79), BookingsDeepGreen)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun InviteTypes(onHistory: (() -> Unit)?, onClose: () -> Unit, onSelect: (InviteType) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Create Visitor Invite", Modifier.weight(1f), fontFamily = DMSans, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, color = FacilityInk)
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, "Close", tint = FacilityInk, modifier = Modifier.size(24.dp))
            }
        }
        Text("Who are you inviting?", fontFamily = DMSans, fontSize = 15.sp, color = FacilityMuted)
        Spacer(Modifier.height(18.dp))
        InviteType.entries.chunked(2).forEach { row ->
            Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { type ->
                    InviteTypeCard(type, Modifier.weight(1f)) { onSelect(type) }
                }
            }
        }
        if (onHistory != null) {
            Spacer(Modifier.height(4.dp))
            HistoryBar(onHistory)
        }
    }
}

/** Full-width bar under the invite types that opens Visitor Management. */
@Composable
private fun HistoryBar(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().height(60.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFE9F7F1), Color(0xFFDDF2EA))))
            .border(1.dp, Color(0xFFCDEBDF), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.History, null, tint = BookingsDeepGreen, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Visitor History", fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
            Text("Active, upcoming and past invites", fontFamily = DMSans, fontSize = 12.sp, color = FacilityMuted)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = FacilityInk, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun InviteTypeCard(type: InviteType, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.height(178.dp)
            .shadow(6.dp, shape, ambientColor = Color(0x1A0F3B33), spotColor = Color(0x1A0F3B33))
            .clip(shape).background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF6FAF8))))
            .border(1.dp, Color(0xFFEAF1EE), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(width = 120.dp, height = 92.dp), contentAlignment = Alignment.Center) {
            when (type) {
                // Built from Icon Set 0.1's clay figure and heart, which are full resolution.
                InviteType.FAMILY -> {
                    Image(painterResource(R.drawable.ic_clay_community_man), null, Modifier.size(100.dp))
                    Image(painterResource(R.drawable.ic_clay_community_heart), null,
                        Modifier.align(Alignment.BottomEnd).offset(x = (-14).dp, y = 2.dp).size(52.dp))
                }
                InviteType.DELIVERY -> InviteArt(R.drawable.invite_delivery)
                InviteType.CAB -> InviteArt(R.drawable.invite_cab)
                InviteType.OTHER -> InviteArt(R.drawable.invite_other)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(type.title, fontFamily = DMSans, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
        Spacer(Modifier.height(2.dp))
        Text(type.subtitle, fontFamily = DMSans, fontSize = 12.sp, lineHeight = 16.sp, color = FacilityMuted, textAlign = TextAlign.Center)
    }
}

// TODO: replace these with full-resolution transparent artwork; they are cut from the mock-up.
@Composable
private fun InviteArt(@DrawableRes art: Int) {
    Image(painterResource(art), null, Modifier.fillMaxHeight())
}
