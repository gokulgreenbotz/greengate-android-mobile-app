package com.greenbotz.greengate.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.AppTheme
import com.greenbotz.greengate.ui.screens.home.BottomBarHeight
import com.greenbotz.greengate.ui.screens.home.MarinaArt
import com.greenbotz.greengate.ui.screens.home.MarinaBackground
import com.greenbotz.greengate.ui.screens.home.MarinaCream
import com.greenbotz.greengate.ui.screens.home.NavItem
import com.greenbotz.greengate.R
import com.greenbotz.greengate.ui.screens.home.ThemeZeroBottomNavigation

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    if (AppPreferences.theme == AppTheme.ZERO) {
        ThemeZeroBottomNavigation(navController, currentRoute)
        return
    }
    fun navigate(route: String) {
        if (route != currentRoute) navController.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Box(Modifier.fillMaxWidth().then(if (AppPreferences.theme == AppTheme.ONE) Modifier.background(MarinaBackground) else Modifier).padding(horizontal = if (AppPreferences.theme == AppTheme.ZERO) 12.dp else 0.dp).navigationBarsPadding().height(BottomBarHeight)) {
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(66.dp), color = MarinaCream,
            shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, Color.White), shadowElevation = 9.dp) {
            Row(Modifier.fillMaxSize().padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                NavItem(Icons.Rounded.Home, "Home", currentRoute == Screen.Home.route, Modifier.weight(1f)) { navigate(Screen.Home.route) }
                NavItem(Icons.Outlined.Groups, "Community", currentRoute == Screen.Community.route, Modifier.weight(1f)) { navigate(Screen.Community.route) }
                Spacer(Modifier.weight(1f))
                NavItem(Icons.Outlined.Lock, "Access", currentRoute == Screen.Access.route, Modifier.weight(1f)) { navigate(Screen.Access.route) }
                NavItem(if (AppPreferences.theme == AppTheme.ZERO) Icons.Outlined.GridView else Icons.Outlined.PersonOutline, if (AppPreferences.theme == AppTheme.ZERO) "More" else "Profile", currentRoute == Screen.Profile.route, Modifier.weight(1f)) { navigate(Screen.Profile.route) }
            }
        }
        Surface(onClick = { navigate(Screen.Home.route) }, modifier = Modifier.align(Alignment.TopCenter).size(65.dp),
            shape = CircleShape, color = Color(0xFFF2ECDD), border = BorderStroke(3.dp, Color.White), shadowElevation = 6.dp) {
            if (AppPreferences.theme == AppTheme.ZERO) Image(androidx.compose.ui.res.painterResource(R.drawable.greengate_logo), "Green Gate home", Modifier.padding(7.dp))
            else MarinaArt(R.drawable.marina_icon_bottom_center_leaf, Modifier.padding(4.dp).clip(CircleShape),
                crop = intArrayOf(29, 25, 109, 109), description = "Marina Bay home", scale = ContentScale.Crop)
        }
    }
}
