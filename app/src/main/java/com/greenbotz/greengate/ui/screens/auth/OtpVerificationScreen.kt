package com.greenbotz.greengate.ui.screens.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.ui.theme.DMSans
import kotlinx.coroutines.delay

/** Digits in the SMS code. Twilio Verify and the dev server both send 6; the API accepts 4 to 10. */
internal const val OtpLength = 6

/**
 * Sign-in step 2, on a screen of its own: the code texted to [phone].
 * The back arrow and the system Back (handled by the caller) return to the number screen.
 * [onResend] is null for the demo account, which has a fixed password instead of an SMS.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun OtpVerificationScreen(
    phone: String,
    code: String,
    onCodeChange: (String) -> Unit,
    message: String?,
    busy: Boolean,
    resendIn: Int,
    onVerify: () -> Unit,
    onResend: (() -> Unit)?,
    onBack: () -> Unit
) {
    // Open the number pad straight away.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    var codeFocused by remember { mutableStateOf(false) }

    // While the keyboard is up, scroll the whole panel into view above it.
    val bringPanelIntoView = remember { BringIntoViewRequester() }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            delay(180)
            bringPanelIntoView.bringIntoView()
        }
    }

    LuxuryLoginBackdrop(showBranding = false) {
        BoxWithConstraints(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
        ) {
            val viewportHeight = maxHeight
            // Same layout as the number screen, so the panel sits in the same place, and is
            // back there as soon as the keyboard closes.
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight).padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GreenGateLoginBranding(Modifier.padding(top = 12.dp))
                Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
                LoginGlassPanel(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth()
                        .bringIntoViewRequester(bringPanelIntoView).testTag("otp_glass_panel")
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Verify Your Number",
                            color = Ivory,
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 25.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Enter the $OtpLength-digit code sent to $phone",
                            color = Muted,
                            fontFamily = DMSans,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        CodeBoxes(
                            code = code,
                            focused = codeFocused,
                            onCodeChange = onCodeChange,
                            onDone = onVerify,
                            modifier = Modifier.focusRequester(focusRequester)
                                .onFocusChanged { codeFocused = it.isFocused }
                        )
                        Spacer(Modifier.height(16.dp))
                        LuxuryGoldButton(if (busy) "Please wait…" else "Verify", enabled = !busy, onClick = onVerify)
                        message?.let {
                            Text(it, color = Muted, fontFamily = DMSans, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                        }
                        if (onResend != null) {
                            val waiting = resendIn > 0
                            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Didn't get it? ", color = Muted, fontFamily = DMSans, fontSize = 13.sp)
                                Text(
                                    if (waiting) "Resend in ${resendIn}s" else "Resend code",
                                    color = if (waiting) Muted else Gold,
                                    fontFamily = DMSans,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable(enabled = !waiting && !busy, role = Role.Button, onClick = onResend)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f).heightIn(min = 18.dp))
            }

            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 12.dp)
                    .size(44.dp)
                    .loginGlassControl()
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Ivory, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** One box per digit. The boxes show the code, so the field's own text and cursor are not drawn. */
@Composable
private fun CodeBoxes(
    code: String,
    focused: Boolean,
    onCodeChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier
) {
    BasicTextField(
        value = code,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(OtpLength)) },
        modifier = modifier.fillMaxWidth().testTag("otp_code_input"),
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box {
                // Keep the editable core attached for IME, paste and accessibility input.
                // The six boxes below supply the visible digits and focus indication.
                Box(Modifier.matchParentSize()) { innerTextField() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(OtpLength) { index ->
                    // The glow marks the box the next digit goes into.
                    val next = focused && index == minOf(code.length, OtpLength - 1)
                    Box(
                        Modifier.weight(1f).height(52.dp).loginGlassControl(focused = next),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(code.getOrNull(index)?.toString().orEmpty(), color = Ivory, fontFamily = DMSans, fontSize = 20.sp)
                    }
                }
            }
            }
        }
    )
}
