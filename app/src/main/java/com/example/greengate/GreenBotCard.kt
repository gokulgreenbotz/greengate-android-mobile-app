package com.example.greengate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greengate.ui.theme.DMSans

private val BotInk = Color(0xFF14302A)
private val BotMuted = Color(0xFF51665F)
private val BotGreen = Color(0xFF155F4B)

/** The original home entry point; the animated assistant appears after opening it. */
@Composable
internal fun GreenBotCard(s: Float = 1f, onClick: () -> Unit) {
    val shape = RoundedCornerShape((20 * s).dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height((104 * s).dp),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 2.dp,
        border = BorderStroke((1 * s).dp, Color.White.copy(alpha = .9f))
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFFEAF8F2), Color(0xFFD3F0E4), Color(0xFFE6F6EF))
                )
            )
        ) {
            Image(
                painter = painterResource(R.drawable.green_bot_mascot),
                contentDescription = "Green Bot",
                modifier = Modifier.align(Alignment.BottomStart)
                    .offset(x = (2 * s).dp, y = (8 * s).dp)
                    .size(width = (116 * s).dp, height = (105 * s).dp)
            )
            Row(
                Modifier.fillMaxSize().padding(start = (118 * s).dp, end = (6 * s).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("ASK GREEN BOT", fontFamily = DMSans, fontSize = (10 * s).sp, letterSpacing = (1.4 * s).sp,
                        fontWeight = FontWeight.Medium, color = BotMuted)
                    Text("I'm here to help!", fontFamily = FontFamily.Serif, fontSize = (19 * s).sp,
                        lineHeight = (24 * s).sp, fontWeight = FontWeight.Bold, color = BotInk)
                    Text("Ask me anything about your home, facilities or community.", fontFamily = DMSans,
                        fontSize = (11.5f * s).sp, lineHeight = (15 * s).sp, color = BotMuted)
                }
                Spacer(Modifier.width((8 * s).dp))
                Box(
                    Modifier.size((54 * s).dp).shadow((6 * s).dp, CircleShape).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF1F7A61), BotGreen))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, null, tint = Color.White,
                        modifier = Modifier.size((26 * s).dp))
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = BotMuted,
                    modifier = Modifier.size((22 * s).dp))
            }
        }
    }
}
