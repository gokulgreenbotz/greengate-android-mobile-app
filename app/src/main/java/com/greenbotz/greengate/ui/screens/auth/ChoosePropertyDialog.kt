package com.greenbotz.greengate.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.core.session.PropertyChoice
import com.greenbotz.greengate.ui.theme.DMSans

/** Shown when the number belongs to several properties: the code is sent for the one picked. */
@Composable
internal fun ChoosePropertyDialog(
    properties: List<PropertyChoice>,
    onPick: (PropertyChoice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your property", fontFamily = DMSans) },
        text = {
            Column {
                Text("Your number is registered in more than one property.", fontFamily = DMSans, fontSize = 14.sp)
                properties.forEachIndexed { index, property ->
                    if (index > 0) HorizontalDivider()
                    Text(
                        property.name,
                        fontFamily = DMSans,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(property) }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", fontFamily = DMSans) } }
    )
}

/** Seconds before "Resend" is offered after a code was sent. */
internal const val ResendSeconds = 60
