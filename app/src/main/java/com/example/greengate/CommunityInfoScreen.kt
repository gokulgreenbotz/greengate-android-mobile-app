package com.example.greengate

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans

private val InfoInk: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val InfoMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val InfoGreen: Color @Composable get() = MaterialTheme.colorScheme.primary
private val InfoCream: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

private class CommunityDocument(val title: String, val detail: String)
internal class CommunityContact(val role: String, val name: String, val phone: String, val email: String)

private val CommunityDocuments = listOf(
    CommunityDocument("House Rules & By-Laws", "PDF · Updated Aug 2026"),
    CommunityDocument("Facility Booking Policy", "PDF · Updated Jul 2026"),
    CommunityDocument("Renovation Guidelines", "PDF · Updated May 2026"),
    CommunityDocument("Move-In / Move-Out Procedure", "PDF · Updated Mar 2026"),
    CommunityDocument("Pet Policy", "PDF · Updated Jan 2026"),
    CommunityDocument("Fire Safety & Evacuation Plan", "PDF · Updated Dec 2025"),
    CommunityDocument("AGM Minutes 2026", "PDF · Updated Sep 2026")
)

// Also used by the SOS screen's management and security buttons.
internal val ManagementOffice = CommunityContact("Management Office", "Marina Bay Management", "+65 6123 4500", "office@marinabayresidences.sg")
internal val SecurityGuardhouse = CommunityContact("Security Guardhouse (24/7)", "Main Gate", "+65 6123 4511", "security@marinabayresidences.sg")

private val CommunityContacts = listOf(
    ManagementOffice,
    SecurityGuardhouse,
    CommunityContact("Maintenance & Repairs", "Facilities Team", "+65 6123 4522", "maintenance@marinabayresidences.sg"),
    CommunityContact("Condo Manager", "Sarah Lim", "+65 9123 4567", "sarah.lim@marinabayresidences.sg")
)

@Composable
fun CommunityInfoScreen(navController: NavController) {
    Column(Modifier.fillMaxSize().background(MarinaBackground).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = InfoInk)
                }
                Spacer(Modifier.width(2.dp))
                Text("About", fontFamily = DMSans, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InfoInk)
            }
            Spacer(Modifier.height(16.dp))
            AboutCard()
            Spacer(Modifier.height(24.dp))
            SectionLabel("AMENITIES")
            AmenityGallery { navController.navigate(Screen.BookFacility.route) }
            Spacer(Modifier.height(24.dp))
            SectionLabel("DOCUMENT HUB")
            InfoCard {
                CommunityDocuments.forEachIndexed { i, doc ->
                    if (i > 0) HorizontalDivider(color = Color(0xFFEDEAE0))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Description)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(doc.title, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InfoInk)
                            Text(doc.detail, fontFamily = DMSans, fontSize = 12.sp, color = InfoMuted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel("CONTACTS")
            InfoCard {
                CommunityContacts.forEachIndexed { i, contact ->
                    if (i > 0) HorizontalDivider(color = Color(0xFFEDEAE0))
                    ContactRow(contact)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AboutCard() {
    InfoCard {
        // Cropped to the waterfront clubhouse and skyline from the home background.
        Image(painterResource(R.drawable.gg_home_background), "Marina Bay Residences waterfront",
            Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop, alignment = BiasAlignment(.3f, .1f))
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Apartment)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Marina Bay Residences", fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InfoInk)
                    Text("8 Marina Boulevard, Singapore 018981", fontFamily = DMSans, fontSize = 12.sp, color = InfoMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "A waterfront residential community of two towers and 428 homes, completed in 2015. " +
                    "Residents enjoy a 50m infinity pool, gym, function rooms, BBQ pavilions and a sky garden, " +
                    "with 24-hour security and an on-site management office.",
                fontFamily = DMSans, fontSize = 14.sp, lineHeight = 20.sp, color = InfoInk
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Fact("Towers", "2")
                Fact("Units", "428")
                Fact("Completed", "2015")
            }
        }
    }
}

/** Photo strip of the bookable facilities; any tap opens Book Facility. */
@Composable
private fun AmenityGallery(onClick: () -> Unit) {
    // Vertical padding keeps the card shadows from being clipped by the scroll container.
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Facilities.forEach { facility ->
            Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = InfoCream, shadowElevation = 2.dp,
                modifier = Modifier.width(150.dp)) {
                Column {
                    Image(painterResource(facility.photo), null, Modifier.fillMaxWidth().height(100.dp),
                        contentScale = ContentScale.Crop)
                    Text(facility.name, Modifier.padding(horizontal = 12.dp, vertical = 10.dp), fontFamily = DMSans,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InfoInk, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RowScope.Fact(label: String, value: String) {
    Column(Modifier.weight(1f).background(MarinaBackground, RoundedCornerShape(12.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InfoGreen)
        Text(label, fontFamily = DMSans, fontSize = 11.sp, color = InfoMuted)
    }
}

@Composable
private fun ContactRow(contact: CommunityContact) {
    val context = LocalContext.current
    fun open(uri: String, action: String) = try {
        context.startActivity(Intent(action, Uri.parse(uri)))
    } catch (_: ActivityNotFoundException) {}
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(contact.role, fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InfoInk)
        Text(contact.name, fontFamily = DMSans, fontSize = 12.sp, color = InfoMuted)
        Spacer(Modifier.height(6.dp))
        ContactLink(Icons.Outlined.Phone, contact.phone) { open("tel:${contact.phone.replace(" ", "")}", Intent.ACTION_DIAL) }
        ContactLink(Icons.Outlined.Email, contact.email) { open("mailto:${contact.email}", Intent.ACTION_SENDTO) }
    }
}

@Composable
private fun ContactLink(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.heightIn(min = 36.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = InfoGreen)
        Spacer(Modifier.width(8.dp))
        Text(label, fontFamily = DMSans, fontSize = 14.sp, color = InfoGreen)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontFamily = DMSans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = InfoMuted)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = InfoCream, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(Modifier.size(40.dp).background(InfoGreen.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = InfoGreen)
    }
}
