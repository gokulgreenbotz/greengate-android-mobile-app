package com.example.greengate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

private enum class VisitorTab(val label: String) { ACTIVE("Active"), UPCOMING("Upcoming"), PAST("Past") }

// Expected (later today) sits with Active: both are today's visitors.
private fun InviteStatus.tab() = when (this) {
    InviteStatus.ACTIVE, InviteStatus.EXPECTED -> VisitorTab.ACTIVE
    InviteStatus.UPCOMING -> VisitorTab.UPCOMING
    else -> VisitorTab.PAST
}

internal fun InviteStatus.tone() = when (this) {
    InviteStatus.ACTIVE -> greenTone("Active")
    InviteStatus.EXPECTED -> orangeTone("Expected")
    InviteStatus.UPCOMING -> StatusTone("Upcoming", Color(0xFF2F7BD8), Color(0xFFE3EFFD))
    InviteStatus.USED -> StatusTone("Used", FacilityMuted, Color(0xFFEDF1F0))
    InviteStatus.EXPIRED -> StatusTone("Expired", FacilityMuted, Color(0xFFEDF1F0))
    InviteStatus.REVOKED -> redTone("Revoked")
}

/** A visitor's 3D avatar: the clay figure (with a heart for family) or their visit type's art. */
@Composable
internal fun InviteAvatar(invite: VisitorInvite, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFDF1E6), Color(0xFFF3E3D3)))),
        contentAlignment = Alignment.Center
    ) {
        when (invite.type) {
            InviteType.FAMILY -> {
                Image(painterResource(R.drawable.ic_clay_community_man), null, Modifier.size(size * .86f))
                if (invite.relationship == "Family") Image(painterResource(R.drawable.ic_clay_community_heart), null,
                    Modifier.align(Alignment.BottomEnd).padding(end = size * .06f, bottom = size * .06f).size(size * .42f))
            }
            InviteType.DELIVERY -> Image(painterResource(R.drawable.invite_delivery), null, Modifier.size(size * .8f))
            InviteType.CAB -> Image(painterResource(R.drawable.invite_cab), null, Modifier.size(size * .84f))
            InviteType.OTHER -> Image(painterResource(R.drawable.invite_other), null, Modifier.size(size * .78f))
        }
    }
}

@Composable
internal fun VisitorManagementScreen(navController: NavController) {
    var tab by rememberSaveable { mutableStateOf(VisitorTab.ACTIVE) }
    var showCreate by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val byTab = VisitorStore.invites.groupBy { it.status(now).tab() }
    val shown = (byTab[tab] ?: emptyList()).let { list ->
        if (tab == VisitorTab.PAST) list.sortedByDescending { it.startAt } else list.sortedBy { it.startAt }
    }

    if (showCreate) CreateInviteSheet(
        onDismiss = { showCreate = false },
        onCreated = { showCreate = false; navController.navigate(Screen.InviteCreated.create(it.id)) }
    ) { type ->
        showCreate = false
        navController.navigate(Screen.CreateInvite.create(type))
    }

    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            GlassTopBar("Visitor Management", onBack = { navController.popBackStack() })
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(58.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x330F6B54), spotColor = Color(0x330F6B54))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1D8A6A), BookingsDeepGreen)))
                    .clickable(role = Role.Button) { showCreate = true },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Invite", fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(50))
                    .background(Color(0x99FFFFFF)).padding(4.dp)
            ) {
                VisitorTab.entries.forEach { t ->
                    val selected = t == tab
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(50))
                            .background(if (selected) Color.White else Color.Transparent)
                            .clickable(role = Role.Tab) { tab = t },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${t.label} (${byTab[t]?.size ?: 0})", fontFamily = DMSans, fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) BookingsDeepGreen else FacilityMuted)
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (shown.isEmpty()) item {
                    Text("No ${tab.label.lowercase()} invites.", Modifier.fillMaxWidth().padding(top = 32.dp),
                        fontFamily = DMSans, fontSize = 15.sp, color = FacilityMuted, textAlign = TextAlign.Center)
                }
                items(shown, key = { it.id }) { invite ->
                    GlassCard(Modifier.clickable(role = Role.Button) { navController.navigate(Screen.InviteDetail.create(invite.id)) }, padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InviteAvatar(invite, 52.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(invite.name, fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
                                Text(invite.kindLabel, fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
                                Text(invite.whenLabel(now), fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
                            }
                            StatusPill(invite.status(now).tone())
                        }
                    }
                }
            }
        }
    }
}
