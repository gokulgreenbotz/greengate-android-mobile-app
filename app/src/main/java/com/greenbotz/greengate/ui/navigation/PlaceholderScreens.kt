package com.greenbotz.greengate.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.NavyGoldButton
import com.greenbotz.greengate.ui.theme.NavyGoldShadow
import com.greenbotz.greengate.ui.theme.NavyGoldTheme
import com.greenbotz.greengate.ui.theme.PrimaryGreen

// Package stays com.greenbotz.greengate until the project-wide
// "Package name does not match containing directory" fix moves it to ui.navigation.

// Light gold on navy, and the gold edge Home's AI Chat button wears.
private val GoldOnNavy = Color(0xFFF3CF85)
private val GoldEdge = Brush.linearGradient(listOf(Color(0xFFF3CF85), Color(0xFFB7802E), Color(0xFFF3CF85)))

/** Green in the other themes; in Navy & Gold, a navy pill with a gold edge, arrow and label. */
@Composable
private fun GoBackButton(onClick: () -> Unit) {
    if (!NavyGoldTheme) {
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) { Text("Go Back") }
        return
    }
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.height(50.dp).shadow(8.dp, shape, ambientColor = NavyGoldShadow, spotColor = NavyGoldShadow)
            .clip(shape).background(NavyGoldButton).border(1.dp, GoldEdge, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = GoldOnNavy)
        Spacer(Modifier.width(8.dp))
        Text("Go Back", color = GoldOnNavy, fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PlaceholderScreen(title: String, navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            GoBackButton { navController.popBackStack() }
            if (title == "Access") {
                TextButton(onClick = { navController.navigate(Screen.CallTest.route) }) {
                    Text("Call Receiver Test Harness")
                }
            }
        }
    }
}

@Composable
fun AnnouncementsScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Announcements Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            GoBackButton { navController.popBackStack() }
        }
    }
}
