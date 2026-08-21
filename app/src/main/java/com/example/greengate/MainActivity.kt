package com.example.greengate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.greengate.ui.theme.AccentPeach
import com.example.greengate.ui.theme.AccentPeachDeep
import com.example.greengate.ui.theme.BackgroundCream
import com.example.greengate.ui.theme.CardWhite
import com.example.greengate.ui.theme.DMSans
import com.example.greengate.ui.theme.GreenGateTheme
import com.example.greengate.ui.theme.IconCircleBlue
import com.example.greengate.ui.theme.IconCircleGreen
import com.example.greengate.ui.theme.IconCirclePeach
import com.example.greengate.ui.theme.IconCircleYellow
import com.example.greengate.ui.theme.NotificationDot
import com.example.greengate.ui.theme.PlayfairDisplay
import com.example.greengate.ui.theme.PrimaryGreen
import com.example.greengate.ui.theme.PrimaryGreenDark
import com.example.greengate.ui.theme.SoftBlue
import com.example.greengate.ui.theme.SoftCoral
import com.example.greengate.ui.theme.SoftGray
import com.example.greengate.ui.theme.SoftGreen
import com.example.greengate.ui.theme.SoftPeach
import com.example.greengate.ui.theme.SoftSage
import com.example.greengate.ui.theme.SoftYellow
import com.example.greengate.ui.theme.TextDark
import com.example.greengate.ui.theme.TextMuted

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
            BottomNavigationBar(navController, currentRoute)
        },
        containerColor = BackgroundCream
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
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

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        HeaderSection()
        Spacer(modifier = Modifier.height(20.dp))
        CommunityAnnouncementCard { navController.navigate(Screen.Announcements.route) }
        Spacer(modifier = Modifier.height(20.dp))
        ActionGrid(navController)
        Spacer(modifier = Modifier.height(16.dp))
        AnnouncementsBanner { navController.navigate(Screen.Announcements.route) }
        Spacer(modifier = Modifier.height(16.dp))
        TodayAtAGlance()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PlaceholderScreen(title: String, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontFamily = PlayfairDisplay,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Go Back", fontFamily = DMSans)
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Good Morning, Alex",
                    fontFamily = PlayfairDisplay,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.Eco,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { }
            ) {
                Text(
                    text = "Marina Bay Residences",
                    fontFamily = DMSans,
                    fontSize = 14.sp,
                    color = TextMuted
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Row {
            SoftCircleIconButton(Icons.Outlined.Search, "Search")
            Spacer(modifier = Modifier.width(10.dp))
            Box {
                SoftCircleIconButton(Icons.Outlined.Notifications, "Notifications")
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(NotificationDot, CircleShape)
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SoftCircleIconButton(icon: ImageVector, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(CircleShape)
            .background(CardWhite)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = TextDark,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun CommunityAnnouncementCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SoftPeach),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            // Soft leaf decoration
            Icon(
                Icons.Rounded.Eco,
                contentDescription = null,
                tint = AccentPeach.copy(alpha = 0.18f),
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 12.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClayIconBadge(
                    background = SoftCoral,
                    iconTint = AccentPeachDeep,
                    icon = Icons.Rounded.Campaign,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Community Announcement",
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPeachDeep
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pool maintenance on Friday,",
                        fontFamily = DMSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "10:00 AM – 12:00 PM",
                        fontFamily = DMSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View",
                        fontFamily = DMSans,
                        color = AccentPeachDeep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = AccentPeachDeep,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClayIconBadge(
    background: Color,
    iconTint: Color,
    icon: ImageVector,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x12000000), spotColor = Color(0x12000000))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(background, background.copy(alpha = 0.85f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun ActionGrid(navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActionCard(
                title = "Book Facility",
                subtitle = "Reserve amenities with ease",
                cardColor = SoftSage,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.BookFacility.route) }
            ) {
                BookFacilityClayIcon()
            }
            ActionCard(
                title = "Invite Visitors",
                subtitle = "Create visitor passes quickly",
                cardColor = SoftBlue,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.InviteVisitors.route) }
            ) {
                InviteVisitorsClayIcon()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActionCard(
                title = "E-Forms",
                subtitle = "Submit and manage digital forms",
                cardColor = SoftYellow,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.EForms.route) }
            ) {
                EFormsClayIcon()
            }
            ActionCard(
                title = "Feedback",
                subtitle = "Share ideas and help us improve",
                cardColor = SoftGreen,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.Feedback.route) }
            ) {
                FeedbackClayIcon()
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    cardColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(200.dp)
            .clickable(onClick = onClick)
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
                iconContent()
            }
            Column {
                Text(
                    text = title,
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = subtitle,
                        fontFamily = DMSans,
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .shadow(2.dp, CircleShape, ambientColor = Color(0x10000000), spotColor = Color(0x10000000))
                            .clip(CircleShape)
                            .background(CardWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = TextDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookFacilityClayIcon() {
    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .offset(x = (-10).dp, y = 6.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color(0x18000000), spotColor = Color(0x18000000))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(IconCircleGreen, PrimaryGreen.copy(alpha = 0.55f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SportsTennis, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-4).dp, y = 4.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(IconCircleYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = AccentPeachDeep, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun InviteVisitorsClayIcon() {
    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x18000000), spotColor = Color(0x18000000))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(listOf(IconCircleBlue, SoftBlue))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Badge, null, tint = Color(0xFF6B9BC3), modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun EFormsClayIcon() {
    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .offset(x = (-8).dp)
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(Color.White, IconCircleYellow))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Description, null, tint = Color(0xFFC9A84C), modifier = Modifier.size(28.dp))
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-6).dp, y = (-6).dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(IconCirclePeach),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Edit, null, tint = AccentPeachDeep, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun FeedbackClayIcon() {
    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .offset(x = (-12).dp, y = 4.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(IconCircleGreen, PrimaryGreen.copy(alpha = 0.65f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.Chat, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 8.dp)
                .shadow(5.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(IconCirclePeach, AccentPeach.copy(alpha = 0.7f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun AnnouncementsBanner(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SoftSage),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayIconBadge(
                background = CardWhite,
                iconTint = PrimaryGreen,
                icon = Icons.Rounded.Campaign,
                size = 52.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Announcements",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextDark
                )
                Text(
                    text = "Stay updated with the latest community news",
                    fontFamily = DMSans,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(2.dp, CircleShape, ambientColor = Color(0x10000000), spotColor = Color(0x10000000))
                    .clip(CircleShape)
                    .background(CardWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextDark
                )
            }
        }
    }
}

@Composable
fun TodayAtAGlance() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SoftSage.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Today at a Glance",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlanceItem(
                    count = "2",
                    label = "Bookings\nConfirmed",
                    badgeColor = SoftGreen,
                    modifier = Modifier.weight(1f)
                )
                GlanceDivider()
                GlanceItem(
                    count = "1",
                    label = "Visitor\nExpected",
                    badgeColor = SoftPeach,
                    modifier = Modifier.weight(1f)
                )
                GlanceDivider()
                GlanceItem(
                    count = "0",
                    label = "New\nAnnouncements",
                    badgeColor = SoftGray,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun GlanceDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(TextMuted.copy(alpha = 0.18f))
    )
}

@Composable
fun GlanceItem(
    count: String,
    label: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count,
                fontFamily = DMSans,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontFamily = DMSans,
            fontSize = 11.sp,
            color = TextMuted,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val mainTabs = setOf(
        Screen.Home.route,
        Screen.Community.route,
        Screen.Access.route,
        Screen.Profile.route
    )
    if (currentRoute !in mainTabs) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.Transparent)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000)
                ),
            color = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    icon = Icons.Rounded.Home,
                    label = "Home",
                    isSelected = currentRoute == Screen.Home.route
                ) {
                    if (currentRoute != Screen.Home.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
                NavItem(
                    icon = Icons.Rounded.Groups,
                    label = "Community",
                    isSelected = currentRoute == Screen.Community.route
                ) {
                    navController.navigate(Screen.Community.route) {
                        launchSingleTop = true
                    }
                }
                Spacer(modifier = Modifier.width(56.dp))
                NavItem(
                    icon = Icons.Rounded.Lock,
                    label = "Access",
                    isSelected = currentRoute == Screen.Access.route
                ) {
                    navController.navigate(Screen.Access.route) {
                        launchSingleTop = true
                    }
                }
                NavItem(
                    icon = Icons.Rounded.Person,
                    label = "Profile",
                    isSelected = currentRoute == Screen.Profile.route
                ) {
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(64.dp)
                .shadow(12.dp, CircleShape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(PrimaryGreen, PrimaryGreenDark)
                    )
                )
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Eco,
                contentDescription = "GreenGate",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryGreenDark else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = DMSans,
            fontSize = 10.sp,
            color = if (isSelected) PrimaryGreenDark else TextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) PrimaryGreenDark else Color.Transparent)
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
