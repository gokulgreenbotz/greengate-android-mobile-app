package com.greenbotz.greengate.ui.screens.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.ui.theme.DMSans

private class BotQuickPrompt(val label: String, val icon: ImageVector)

private val BotQuickPrompts = listOf(
    BotQuickPrompt("Book a facility", Icons.Outlined.CalendarMonth),
    BotQuickPrompt("Invite a visitor", Icons.Outlined.PersonAddAlt1),
    BotQuickPrompt("My bookings", Icons.Outlined.EventAvailable),
    BotQuickPrompt("Payments & deposits", Icons.Outlined.AccountBalanceWallet)
)

/** One-tap starting points that send the same plain-language question shown on the card. */
@Composable
internal fun GreenBotQuickPrompts(onPrompt: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BotQuickPrompts.chunked(2).forEach { prompts ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                prompts.forEach { prompt ->
                    Surface(
                        onClick = { onPrompt(prompt.label) },
                        modifier = Modifier.weight(1f).heightIn(min = 72.dp)
                            .semantics(mergeDescendants = true) { role = Role.Button },
                        shape = RoundedCornerShape(18.dp),
                        color = lerp(colors.background, Color.White, .70f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .88f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(32.dp).background(colors.primary.copy(alpha = .08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(prompt.icon, null, Modifier.size(19.dp), tint = colors.primary)
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(
                                prompt.label, Modifier.weight(1f), fontFamily = DMSans,
                                fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A recoverable voice or permission error that remains readable at larger font sizes. */
@Composable
internal fun GreenBotErrorNotice(
    message: String,
    onDismiss: () -> Unit,
    onUseKeyboard: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onTurnUpVolume: (() -> Unit)? = null,
    icon: ImageVector = Icons.Outlined.ErrorOutline
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(18.dp), color = colors.errorContainer,
        border = BorderStroke(1.dp, colors.error.copy(alpha = .12f))
    ) {
        Column {
            Row(
                Modifier.padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = colors.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    message, Modifier.weight(1f), fontFamily = DMSans, fontSize = 12.sp,
                    lineHeight = 18.sp, color = colors.onErrorContainer
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, "Dismiss error", Modifier.size(18.dp), tint = colors.onErrorContainer)
                }
            }
            if (onUseKeyboard != null || onOpenSettings != null || onTurnUpVolume != null) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Turn up volume" to onTurnUpVolume, "Use keyboard" to onUseKeyboard, "Open settings" to onOpenSettings)
                        .forEach { (label, action) ->
                            if (action != null) {
                                TextButton(
                                    onClick = action, modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = colors.onErrorContainer)
                                ) {
                                    Text(label, fontFamily = DMSans, fontSize = 12.sp, lineHeight = 18.sp,
                                        fontWeight = FontWeight.SemiBold, color = colors.onErrorContainer)
                                }
                            }
                        }
                }
            }
        }
    }
}
