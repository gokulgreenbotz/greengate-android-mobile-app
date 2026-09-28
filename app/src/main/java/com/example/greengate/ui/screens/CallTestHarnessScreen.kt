package com.example.greengate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greengate.core.call.ReceiverCallController
import com.example.greengate.core.call.provider.SharedCallConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallTestHarnessScreen(
    controller: ReceiverCallController,
    onBack: () -> Unit
) {
    var twilioRoom by remember { mutableStateOf(SharedCallConfig.twilioRoomName) }
    var twilioToken by remember { mutableStateOf(SharedCallConfig.twilioReceiverToken) }

    var vonageApiKey by remember { mutableStateOf(SharedCallConfig.vonageApiKey) }
    var vonageSessionId by remember { mutableStateOf(SharedCallConfig.vonageSessionId) }
    var vonageToken by remember { mutableStateOf(SharedCallConfig.vonageToken) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Receiver Test Harness", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF1F8F1),
                    titleContentColor = Color(0xFF1B2A1E)
                )
            )
        },
        containerColor = Color(0xFFF8FAF8)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Intro Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kiosk <-> Mobile Receiver Bridge",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use this harness to trigger simulated or real paired calls from the GreenGate Kiosk device.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }
            }

            // Twilio Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Twilio Receiver Config",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1B2A1E)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = twilioRoom,
                        onValueChange = {
                            twilioRoom = it
                            SharedCallConfig.twilioRoomName = it
                        },
                        label = { Text("Room Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = twilioToken,
                        onValueChange = {
                            twilioToken = it
                        },
                        label = { Text("Receiver Access Token") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val creds = SharedCallConfig.createTwilioReceiverCredentials(
                                roomName = twilioRoom,
                                token = twilioToken
                            )
                            controller.triggerIncomingCall(creds)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F6F52)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Twilio Incoming Call")
                    }
                }
            }

            // Vonage Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vonage Receiver Config",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1B2A1E)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = vonageApiKey,
                        onValueChange = {
                            vonageApiKey = it
                            SharedCallConfig.vonageApiKey = it
                        },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = vonageSessionId,
                        onValueChange = {
                            vonageSessionId = it
                            SharedCallConfig.vonageSessionId = it
                        },
                        label = { Text("Session ID") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = vonageToken,
                        onValueChange = {
                            vonageToken = it
                            SharedCallConfig.vonageToken = it
                        },
                        label = { Text("Token") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val creds = SharedCallConfig.createVonageReceiverCredentials(
                                apiKey = vonageApiKey,
                                sessionId = vonageSessionId,
                                token = vonageToken
                            )
                            controller.triggerIncomingCall(creds)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.VpnKey, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Vonage Incoming Call")
                    }
                }
            }
        }
    }
}
