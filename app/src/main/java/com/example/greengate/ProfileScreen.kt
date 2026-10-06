package com.example.greengate

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

private val ProfileInk: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val ProfileMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val ProfileGreen: Color @Composable get() = MaterialTheme.colorScheme.primary
private val ProfileCream: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

private val ProfileLogoutRed = Color(0xFFDF4336)

@Composable
fun ProfileScreen(navController: NavController, onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            icon = { Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = ProfileLogoutRed) },
            title = { Text("Log out?", fontFamily = DMSans) },
            text = { Text("You'll need to sign in again to use GreenGate.", fontFamily = DMSans) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text("Log out", color = ProfileLogoutRed, fontFamily = DMSans) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel", fontFamily = DMSans) }
            }
        )
    }
    Column(
        Modifier.fillMaxSize().background(MarinaBackground).verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ProfileInk)
                }
                Spacer(Modifier.width(2.dp))
                Text("Profile", fontFamily = DMSans, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ProfileInk)
            }
            Spacer(Modifier.height(24.dp))
            Text("HOME SCREEN ICONS", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp), color = ProfileCream, shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val useIconSet = AppPreferences.theme == AppTheme.ONE || !AppPreferences.useReferenceArtwork
                    if (AppPreferences.theme == AppTheme.ZERO) {
                        IconSetOption(
                            title = "Design artwork",
                            subtitle = "Glossy illustrations from Theme 0",
                            selected = AppPreferences.useReferenceArtwork,
                            onClick = { AppPreferences.setUseReferenceArtwork(context, true) }
                        ) {
                            ReferenceArtworkPreview()
                        }
                        HorizontalDivider(color = Color(0xFFEDEAE0))
                    }
                    IconSetOption(
                        title = "Icon Set 0",
                        subtitle = "Illustrated icons with shadows",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.ZERO,
                        onClick = { AppPreferences.setIconSet(context, IconSet.ZERO) }
                    ) {
                        LayeredIconSetOne(0, 0f, false, Modifier.size(40.dp), IconSet.ZERO)
                    }
                    HorizontalDivider(color = Color(0xFFEDEAE0))
                    IconSetOption(
                        title = "Icon Set 0.1",
                        subtitle = "Soft clay icons with Set 0 animations",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.CLAY,
                        onClick = { AppPreferences.setIconSet(context, IconSet.CLAY) }
                    ) {
                        LayeredIconSetOne(0, 0f, false, Modifier.size(40.dp), IconSet.CLAY)
                    }
                    HorizontalDivider(color = Color(0xFFEDEAE0))
                    IconSetOption(
                        title = "Icon Set 1",
                        subtitle = "Photo-style illustrated icons",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.ONE,
                        onClick = { AppPreferences.setIconSet(context, IconSet.ONE) }
                    ) {
                        LayeredIconSetOne(0, 0f, false, Modifier.size(40.dp))
                    }
                    HorizontalDivider(color = Color(0xFFEDEAE0))
                    IconSetOption(
                        title = "Icon Set 2",
                        subtitle = "3D icons with sequential animations",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.TWO,
                        onClick = { AppPreferences.setIconSet(context, IconSet.TWO) }
                    ) {
                        LayeredActionIcon(0, 0f, false, Modifier.size(40.dp))
                    }
                    HorizontalDivider(color = Color(0xFFEDEAE0))
                    IconSetOption(
                        title = "Icon Set 3",
                        subtitle = "Simple flat icons",
                        selected = useIconSet && AppPreferences.iconSet == IconSet.THREE,
                        onClick = { AppPreferences.setIconSet(context, IconSet.THREE) }
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(ProfileGreen.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = ProfileGreen, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("THEME", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = ProfileCream,
                shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    AppTheme.entries.forEach { theme ->
                        val title = when (theme) {
                            AppTheme.ZERO -> "Theme 0 - Glass"
                            AppTheme.ONE -> "Theme 1 - Classic"
                        }
                        Row(Modifier.fillMaxWidth().selectable(
                            selected = AppPreferences.theme == theme,
                            role = Role.RadioButton,
                            onClick = { AppPreferences.setTheme(context, theme) }
                        ).padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(title, modifier = Modifier.weight(1f), fontFamily = DMSans,
                                color = ProfileInk, fontSize = 15.sp)
                            RadioButton(selected = AppPreferences.theme == theme, onClick = null)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("HOME SCREEN CARDS", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp), color = ProfileCream, shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val showFeedback = AppPreferences.showFeedback
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(role = Role.Switch) { AppPreferences.setShowFeedback(context, !showFeedback) }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(ProfileGreen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Feedback, null, tint = ProfileGreen, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Show Feedback", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ProfileInk)
                        Text("Adds the Feedback card after Community", fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted)
                    }
                    Switch(
                        checked = showFeedback,
                        onCheckedChange = { AppPreferences.setShowFeedback(context, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = ProfileGreen)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("ACCOUNT", fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ProfileMuted)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp), color = ProfileCream, shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { confirmLogout = true }
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(ProfileLogoutRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = ProfileLogoutRed, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Log out", fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        color = ProfileLogoutRed, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReferenceArtworkPreview() {
    val bitmap = ImageBitmap.imageResource(R.drawable.gg_home_reference_art)
    val painter = remember(bitmap) {
        BitmapPainter(bitmap, IntOffset(105, 940), IntSize(185, 138))
    }
    Image(painter, null, Modifier.size(40.dp), contentScale = ContentScale.Fit)
}

@Composable
private fun IconSetOption(
    title: String, subtitle: String, selected: Boolean, onClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        preview()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ProfileInk)
            Text(subtitle, fontFamily = DMSans, fontSize = 12.sp, color = ProfileMuted)
        }
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = ProfileGreen))
    }
}
