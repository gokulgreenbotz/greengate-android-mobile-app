package com.greenbotz.greengate.ui.screens.visitors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.ui.screens.facilities.BookingDay
import com.greenbotz.greengate.ui.screens.facilities.ClassicLine
import com.greenbotz.greengate.ui.screens.facilities.ClassicMint
import com.greenbotz.greengate.ui.screens.facilities.themed
import com.greenbotz.greengate.ui.theme.DMSans

// The input look of Invite Cab, shared by every invite form in the Create Invite sheet:
// a label above each full-width white field, with the field's icon inside it.

private val InviteFieldShape = RoundedCornerShape(14.dp)
internal val InviteFieldBorder get() = if (NavyGoldInvites) Color(0xFFE6E1D8) else themed(Color(0xFFE3ECE9), ClassicLine)
/** The fill of a selected chip in an invite form; a gold-cream in Theme 4. */
internal val InviteSelectedFill get() = if (NavyGoldInvites) Color(0xFFFBF3E3) else themed(Color(0xFFE7F6F0), ClassicMint)
internal val InviteErrorRed = Color(0xFFC2412D)

internal val InviteCountryCodes = listOf("+65", "+60", "+62", "+63", "+66", "+91", "+44", "+1")

@Composable
internal fun InviteLabel(text: String) {
    Text(text, Modifier.padding(top = 16.dp, bottom = 8.dp), fontFamily = DMSans, fontSize = 14.sp,
        fontWeight = FontWeight.Medium, color = InviteInk)
}

@Composable
internal fun InviteError(text: String?) {
    if (text != null) Text(text, Modifier.padding(start = 2.dp, top = 6.dp), fontFamily = DMSans, fontSize = 12.sp, color = InviteErrorRed)
}

/** The white field box: optional leading [icon], [content], optional [trailing] icon; red edge on [error]. */
@Composable
internal fun InviteField(
    icon: ImageVector?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: ImageVector? = null,
    height: Dp = 52.dp, error: Boolean = false, content: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().height(height).clip(InviteFieldShape).background(Color.White)
            .border(if (error) 1.5.dp else 1.dp, if (error) InviteErrorRed else InviteFieldBorder, InviteFieldShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = InviteDeep, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Box(Modifier.weight(1f)) { content() }
        if (trailing != null) Icon(trailing, null, tint = InviteInk, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun InviteDropdown(icon: ImageVector?, value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        InviteField(icon, onClick = { open = true }, trailing = Icons.Rounded.KeyboardArrowDown) {
            Text(value, fontFamily = DMSans, fontSize = 15.sp, color = InviteInk, maxLines = 1)
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = Color.White,
            modifier = Modifier.heightIn(max = 320.dp)) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(option, fontFamily = DMSans, fontWeight = if (option == value) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { onSelect(i); open = false }
                )
            }
        }
    }
}

/** A single-line text input in an [InviteField], with its [error] underneath. */
@Composable
internal fun InviteTextField(
    icon: ImageVector?, value: String, onValueChange: (String) -> Unit, placeholder: String, error: String?,
    keyboardOptions: KeyboardOptions, modifier: Modifier = Modifier,
) {
    Column(modifier) {
        InviteField(icon, error = error != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, fontFamily = DMSans, fontSize = 15.sp, color = InviteMuted.copy(alpha = .6f), maxLines = 1)
                BasicTextField(
                    value, onValueChange, Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = TextStyle(fontFamily = DMSans, fontSize = 15.sp, color = InviteInk),
                    cursorBrush = SolidColor(InviteDeep), keyboardOptions = keyboardOptions
                )
            }
        }
        InviteError(error)
    }
}

/** Country code and number in one field. */
@Composable
internal fun InviteMobileField(code: String, onCode: (String) -> Unit, mobile: String, onMobile: (String) -> Unit, error: String?) {
    var open by remember { mutableStateOf(false) }
    InviteField(Icons.Outlined.Phone, error = error != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.DropdownList) { open = true }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(code, fontFamily = DMSans, fontSize = 15.sp, color = InviteInk)
                    Icon(Icons.Rounded.KeyboardArrowDown, "Country code", tint = InviteInk, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(open, onDismissRequest = { open = false }, containerColor = Color.White) {
                    InviteCountryCodes.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontFamily = DMSans, fontWeight = if (option == code) FontWeight.SemiBold else FontWeight.Normal) },
                            onClick = { onCode(option); open = false }
                        )
                    }
                }
            }
            Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(22.dp).background(InviteFieldBorder))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (mobile.isEmpty()) Text("8123 4567", fontFamily = DMSans, fontSize = 15.sp, color = InviteMuted.copy(alpha = .6f), maxLines = 1)
                BasicTextField(
                    mobile, onMobile, Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = TextStyle(fontFamily = DMSans, fontSize = 15.sp, color = InviteInk),
                    cursorBrush = SolidColor(InviteDeep),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                )
            }
        }
    }
    InviteError(error)
}

/** "Today" / "Tomorrow" (or the date) over the full date, opening a picker on tap. */
@Composable
internal fun InviteDateField(day: BookingDay, onClick: () -> Unit) {
    InviteField(Icons.Outlined.CalendarMonth, onClick = onClick, trailing = Icons.Rounded.ChevronRight, height = 60.dp) {
        Column {
            Text(quickDayName(day) ?: day.label(), fontFamily = DMSans, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InviteInk)
            Text("${day.label()} ${day.year}", fontFamily = DMSans, fontSize = 12.sp, color = InviteMuted)
        }
    }
}

internal fun quickDayName(day: BookingDay) = when (day) {
    BookingDay.today() -> "Today"
    dayFromToday(1) -> "Tomorrow"
    else -> null
}
