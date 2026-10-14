package com.greenbotz.greengate.ui.screens.intercom

import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.greenbotz.greengate.core.call.ReceiverCallController

@Composable
fun InCallScreen(
    controller: ReceiverCallController,
    remoteView: View?,
    isMuted: Boolean,
    isCameraOff: Boolean,
    gateUnlockedMessage: String?,
    onHangUp: () -> Unit
) {
    val context = LocalContext.current
    val credentials = controller.currentCredentials

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Remote Kiosk Video Stream (Full Screen)
        if (remoteView != null) {
            // key(): if the provider hands us a *new* remote view (e.g. Vonage
            // re-subscribes to a fresher kiosk stream) AndroidView must be
            // rebuilt — its factory only runs once per composition key.
            key(remoteView) {
                AndroidView(
                    factory = {
                        (remoteView.parent as? ViewGroup)?.removeView(remoteView)
                        remoteView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Waiting for Kiosk Video...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 2. Own camera preview (top right). Observed from the controller so it
        // appears as soon as the camera starts — it used to be fetched once,
        // before the camera existed, and stayed empty for the whole call.
        val localView by controller.localView.collectAsState()
        Box(
            modifier = Modifier
                .padding(top = 48.dp, end = 20.dp)
                .size(width = 100.dp, height = 150.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            val preview = localView
            when {
                isCameraOff -> Icon(
                    imageVector = Icons.Rounded.VideocamOff,
                    contentDescription = "Your camera is off",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
                preview != null -> key(preview) {
                    AndroidView(
                        factory = {
                            (preview.parent as? ViewGroup)?.removeView(preview)
                            preview
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> Text(
                    text = "Starting camera…",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        // 3. Header Information (Visitor Name & Kiosk Info)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 20.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = credentials?.visitorName ?: "Visitor Call",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = credentials?.unitName ?: "In Call",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 4. Gate Unlocked Notification Banner
        AnimatedVisibility(
            visible = gateUnlockedMessage != null,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
        ) {
            val unlockFailed = gateUnlockedMessage?.let {
                it.startsWith("Couldn't") || it.startsWith("The gate didn't")
            } == true
            Surface(
                // green = unlocking / unlocked, red = kiosk reported failure
                color = if (unlockFailed) Color(0xFFC62828) else Color(0xFF2E7D32),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = gateUnlockedMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 5. Control Action Bar (Bottom) on a dark fade for legibility
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
                )
                .padding(top = 40.dp, bottom = 36.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gate actions — only once the call is really up and the kiosk's
            // video is on screen. Solid, high-contrast buttons so they read
            // clearly on top of any video.
            AnimatedVisibility(visible = remoteView != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Open Gate (call continues): white button, green content
                    Button(
                        onClick = { controller.openGate(endCall = false) },
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1B5E20)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Gate", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                    }

                    // Open Gate & End Call: solid green, white content
                    Button(
                        onClick = { controller.openGate(endCall = true) },
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open & End Call", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Mute, Toggle Camera, End Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Mic Toggle
                Surface(
                    shape = CircleShape,
                    color = if (isMuted) Color.White else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    IconButton(onClick = { controller.toggleMute() }) {
                        Icon(
                            imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Black else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Toggle Camera
                Surface(
                    shape = CircleShape,
                    color = if (isCameraOff) Color.White else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    IconButton(onClick = { controller.toggleCamera() }) {
                        Icon(
                            imageVector = if (isCameraOff) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam,
                            contentDescription = "Camera",
                            tint = if (isCameraOff) Color.Black else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // End Call Button
                FloatingActionButton(
                    onClick = onHangUp,
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
