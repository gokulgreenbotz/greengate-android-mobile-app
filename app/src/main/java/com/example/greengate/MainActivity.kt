package com.example.greengate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.greengate.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreenGateTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Community : Screen("community")
    data object Access : Screen("access")
    data object Profile : Screen("profile")
    data object BookFacility : Screen("book_facility")
    data object InviteVisitors : Screen("invite_visitors")
    data object EForms : Screen("e_forms")
    data object Feedback : Screen("feedback")
    data object Announcements : Screen("announcements")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (isMainTab(currentRoute)) {
                BottomNavigationBar(navController, currentRoute)
            }
        },
        containerColor = BackgroundGreen
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Community.route) { PlaceholderScreen("Community", navController) }
            composable(Screen.Access.route) { PlaceholderScreen("Access", navController) }
            composable(Screen.Profile.route) { PlaceholderScreen("Profile", navController) }
            composable(Screen.BookFacility.route) { PlaceholderScreen("Book Facility", navController) }
            composable(Screen.InviteVisitors.route) { PlaceholderScreen("Invite Visitors", navController) }
            composable(Screen.EForms.route) { PlaceholderScreen("E-Forms", navController) }
            composable(Screen.Feedback.route) { PlaceholderScreen("Feedback", navController) }
            composable(Screen.Announcements.route) { PlaceholderScreen("Announcements", navController) }
        }
    }
}

fun isMainTab(route: String?): Boolean {
    return route in listOf(Screen.Home.route, Screen.Community.route, Screen.Access.route, Screen.Profile.route)
}

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        HeaderSection()
        Spacer(modifier = Modifier.height(24.dp))
        CommunityAnnouncementCard { navController.navigate(Screen.Announcements.route) }
        Spacer(modifier = Modifier.height(24.dp))
        ActionGrid(navController)
        Spacer(modifier = Modifier.height(24.dp))
        AnnouncementsBanner { navController.navigate(Screen.Announcements.route) }
        Spacer(modifier = Modifier.height(24.dp))
        TodayAtAGlance()
        Spacer(modifier = Modifier.height(130.dp))
    }
}

@Composable
fun PlaceholderScreen(title: String, navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundGreen), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Go Back")
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Good Morning, Alex",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Rounded.Eco,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(24.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Marina Bay Residences",
                    fontSize = 14.sp,
                    color = TextLight
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Row {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = TextDark, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Box {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = TextDark, modifier = Modifier.size(22.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFF8A65), CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityAnnouncementCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7F2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFFFEBDD), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Campaign,
                    contentDescription = null,
                    tint = Color(0xFFFF8A65),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Community Announcement",
                    fontSize = 12.sp,
                    color = Color(0xFFFF8A65),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pool maintenance on Friday,",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "10:00 AM - 12:00 PM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View", color = Color(0xFFFF8A65), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    Icons.AutoMirrored.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFFF8A65),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ActionGrid(navController: NavController) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(
                title = "Book Facility",
                subtitle = "Reserve amenities with ease",
                icon = Icons.Rounded.SportsTennis,
                backgroundColor = Color(0xFFF1F8F1),
                iconColor = Color(0xFF81C784),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.BookFacility.route) }
            )
            ActionCard(
                title = "Invite Visitors",
                subtitle = "Create visitor passes quickly",
                icon = Icons.Rounded.Badge,
                backgroundColor = Color(0xFFF0F7FF),
                iconColor = Color(0xFF90CAF9),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.InviteVisitors.route) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(
                title = "E-Forms",
                subtitle = "Submit and manage digital forms",
                icon = Icons.Rounded.Description,
                backgroundColor = Color(0xFFFFFBF0),
                iconColor = Color(0xFFFFD54F),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.EForms.route) }
            )
            ActionCard(
                title = "Feedback",
                subtitle = "Share ideas and help us improve",
                icon = Icons.Rounded.ChatBubble,
                backgroundColor = Color(0xFFF1F8F1),
                iconColor = Color(0xFF81C784),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.Feedback.route) }
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier.height(190.dp).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(70.dp), 
                    tint = iconColor.copy(alpha = 0.4f)
                )
            }
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextLight,
                        lineHeight = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementsBanner(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F1)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Campaign,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Announcements", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Text(text = "Stay updated with the latest community news", fontSize = 12.sp, color = TextLight)
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextDark)
                }
            }
        }
    }
}

@Composable
fun TodayAtAGlance() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFF1F8F1), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Today at a Glance", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlanceItem("2", "Bookings Confirmed", Color(0xFFE8F5E9))
                GlanceItem("1", "Visitor Expected", Color(0xFFFFF3E0))
                GlanceItem("0", "New Announcements", Color(0xFFF1F8E9))
            }
        }
    }
}

@Composable
fun GlanceItem(count: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = count, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label.replace(" ", "\n"),
            fontSize = 11.sp,
            color = TextLight,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(75.dp),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Rounded.Home, "Home", currentRoute == Screen.Home.route) {
                    if (currentRoute != Screen.Home.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
                NavItem(Icons.Rounded.Groups, "Community", currentRoute == Screen.Community.route) {
                    if (currentRoute != Screen.Community.route) {
                        navController.navigate(Screen.Community.route)
                    }
                }
                Spacer(modifier = Modifier.width(64.dp))
                NavItem(Icons.Rounded.Lock, "Access", currentRoute == Screen.Access.route) {
                    if (currentRoute != Screen.Access.route) {
                        navController.navigate(Screen.Access.route)
                    }
                }
                NavItem(Icons.Rounded.Person, "Profile", currentRoute == Screen.Profile.route) {
                    if (currentRoute != Screen.Profile.route) {
                        navController.navigate(Screen.Profile.route)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(68.dp),
            shape = CircleShape,
            color = PrimaryGreen,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Eco,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        
        // Indicator dot for Home
        if (currentRoute == Screen.Home.route) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 48.dp, bottom = 8.dp)
                    .size(4.dp)
                    .background(Color(0xFF4F6F52), CircleShape)
            )
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryGreen else TextLight,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) PrimaryGreen else TextLight,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun MainScreenPreview() {
    GreenGateTheme {
        MainScreen()
    }
}
