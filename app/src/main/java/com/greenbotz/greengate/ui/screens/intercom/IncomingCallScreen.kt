package com.greenbotz.greengate.ui.screens.intercom

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.core.call.provider.ReceiverCallCredentials

@Composable
fun IncomingCallScreen(
    credentials: ReceiverCallCredentials,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    /** Hide this screen without answering or declining (kiosk keeps ringing). */
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current
    var confirmSkip by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var mediaPlayer: MediaPlayer? = null
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (ringtoneUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context.applicationContext, ringtoneUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("IncomingCallScreen", "Failed to play ringtone via MediaPlayer", e)
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, ringtoneUri)
                ringtone?.play()
            } catch (_: Exception) {}
        }

        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.dp, horizontal = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "INCOMING KIOSK CALL",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = credentials.visitorName,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${credentials.unitName} • ${credentials.kioskName}",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Big Avatar Icon
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onDecline,
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = "Decline",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Decline", color = Color.White, fontSize = 14.sp)
                }

                // Accept Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onAccept,
                        containerColor = Color(0xFF43A047),
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Accept",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Accept", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Small "Skip" in the top-right corner.
        TextButton(
            onClick = { confirmSkip = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 12.dp)
        ) {
            Text("Skip", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (confirmSkip) {
        AlertDialog(
            onDismissRequest = { confirmSkip = false },
            title = { Text("Skip this call?") },
            text = {
                Text(
                    "The call screen will close and you can keep using the app. " +
                        "The visitor won't be told — the kiosk keeps ringing until it times out, " +
                        "and you won't be able to answer this call afterwards."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSkip = false
                    onSkip()
                }) { Text("Skip call") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSkip = false }) { Text("Cancel") }
            }
        )
    }
}
