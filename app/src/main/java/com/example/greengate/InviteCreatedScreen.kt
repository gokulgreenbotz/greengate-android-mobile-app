package com.example.greengate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greengate.ui.theme.DMSans
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

// TODO: replace with the real invite link from the visitor API; .example never resolves.
private fun inviteLink(invite: VisitorInvite) = "https://greengate.example/invite/${invite.id}"

private fun inviteMessage(invite: VisitorInvite) =
    "Hi ${invite.name}, you're invited to visit on ${invite.day.label()}, ${invite.timeRange}. " +
        "Show this pass at the gate: ${inviteLink(invite)}"

/** Shown after Create Invite, and from Invite Details → Share pass. */
@Composable
internal fun InviteCreatedScreen(navController: NavController, invite: VisitorInvite) {
    val context = LocalContext.current
    val pop = remember(invite.id) { Animatable(0f) }
    LaunchedEffect(invite.id) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    val done = {
        navController.navigate(Screen.Visitors.route) { popUpTo(Screen.Home.route) }
    }

    Box(Modifier.fillMaxSize().background(BookingsBackdrop), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding()) {
            GlassTopBar("", onBack = { navController.popBackStack() })
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(150.dp, 110.dp), contentAlignment = Alignment.Center) {
                    // Confetti dots burst outward as the tick pops in.
                    Canvas(Modifier.fillMaxSize()) {
                        val colors = listOf(Color(0xFF4CC38A), Color(0xFFF2B33D), Color(0xFF5BA4F5), Color(0xFFB08AF0), Color(0xFFF07D6A))
                        repeat(12) { i ->
                            val angle = i * (Math.PI * 2 / 12) + .3
                            val r = (34 + 34 * pop.value).dp.toPx()
                            drawCircle(colors[i % colors.size].copy(alpha = pop.value.coerceIn(0f, 1f)), 3.dp.toPx(),
                                Offset(center.x + (cos(angle) * r * 1.3).toFloat(), center.y + (sin(angle) * r * .8).toFloat()))
                        }
                    }
                    Box(
                        Modifier.size(78.dp).scale(pop.value).shadow(10.dp, CircleShape).clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(0xFF2AA07D), BookingsDeepGreen))),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(44.dp)) }
                }
                Text("Invite Created!", fontFamily = DMSans, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = FacilityInk)
                Text("Share the invite with your visitor", fontFamily = DMSans, fontSize = 15.sp, color = FacilityMuted)
                Spacer(Modifier.height(16.dp))
                QrPass(invite.qrContent(), invite.id)
                Spacer(Modifier.height(8.dp))
                Text("ID: ${invite.id}", fontFamily = DMSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FacilityInk)
                Text("${invite.name} · ${invite.whenLabel()}", fontFamily = DMSans, fontSize = 13.sp, color = FacilityMuted)
                Spacer(Modifier.height(16.dp))
                ShareRow(Icons.Outlined.Share, "Share via WhatsApp", Color(0xFF25D366)) { shareToWhatsApp(context, invite) }
                Spacer(Modifier.height(10.dp))
                ShareRow(Icons.Outlined.Link, "Copy Link", FacilityInk) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite link", inviteLink(invite)))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(10.dp))
                ShareRow(Icons.Outlined.Download, "Download Pass", FacilityInk) { savePass(context, invite) }
                Spacer(Modifier.height(16.dp))
            }
            Box(
                Modifier.padding(16.dp).fillMaxWidth().height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x330F6B54), spotColor = Color(0x330F6B54))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1D8A6A), BookingsDeepGreen)))
                    .clickableButton { done() },
                contentAlignment = Alignment.Center
            ) { Text("Done", fontFamily = DMSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        }
    }
}

private fun Modifier.clickableButton(onClick: () -> Unit) =
    this.then(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick))

@Composable
private fun ShareRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    GlassCard(Modifier.clickableButton(onClick), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, fontFamily = DMSans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FacilityInk)
        }
    }
}

private fun shareToWhatsApp(context: Context, invite: VisitorInvite) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, inviteMessage(invite))
    try {
        context.startActivity(Intent(send).setPackage("com.whatsapp"))
    } catch (_: android.content.ActivityNotFoundException) {
        // WhatsApp isn't installed; let the resident pick another app.
        context.startActivity(Intent.createChooser(send, "Share invite"))
    }
}

/** Saves the pass (QR, name, time and ID) as a PNG in Pictures/GreenGate. */
private fun savePass(context: Context, invite: VisitorInvite) {
    val bitmap = renderPass(invite)
    val fileName = "${invite.id}.png"
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GreenGate")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("No media store")
            context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(context, "Pass saved to Pictures/GreenGate", Toast.LENGTH_SHORT).show()
        } else {
            // Before Android 10, the shared Pictures folder needs a storage permission; keep it app-private.
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            File(dir, fileName).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(context, "Pass saved to the app's Pictures folder", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't save the pass", Toast.LENGTH_SHORT).show()
    }
}

private fun renderPass(invite: VisitorInvite): Bitmap {
    val width = 900
    val height = 1150
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val matrix = QRCodeWriter().encode(
        invite.qrContent(), BarcodeFormat.QR_CODE, 0, 0,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0)
    )
    val qrSize = 640f
    val cell = qrSize / matrix.width
    val left = (width - qrSize) / 2
    val top = 150f
    val ink = Paint().apply { color = 0xFF0B3B31.toInt() }
    for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
        if (matrix[x, y]) canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell, top + (y + 1) * cell, ink)
    }
    fun text(value: String, y: Float, size: Float, bold: Boolean, color: Int) {
        canvas.drawText(value, width / 2f, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = size; this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        })
    }
    text("Green Gate Visitor Pass", 95f, 46f, true, 0xFF14232B.toInt())
    text(invite.name, top + qrSize + 90f, 52f, true, 0xFF14232B.toInt())
    text("${invite.day.label()} ${invite.day.year} · ${invite.timeRange}", top + qrSize + 150f, 34f, false, 0xFF5B6B72.toInt())
    text("ID: ${invite.id}", top + qrSize + 205f, 32f, false, 0xFF5B6B72.toInt())
    return bitmap
}
