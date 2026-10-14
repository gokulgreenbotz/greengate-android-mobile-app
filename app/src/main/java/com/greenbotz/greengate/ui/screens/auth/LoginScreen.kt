package com.greenbotz.greengate.ui.screens.auth

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.ResidentApiException
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.theme.DMSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val WhiteGlass = Color.White.copy(alpha = .25f)
private val GlassBorder = Color.White.copy(alpha = .58f)

// Built-in demo account: signs in without the server and shows the app's sample data.
private const val DemoMobile = "000000"
private const val DemoPassword = "123456"

/** +country-code form the API expects. Numbers typed without a country code are Singapore numbers. */
private fun internationalNumber(input: String): String? {
    val digits = input.filter(Char::isDigit)
    if (digits.length < 8) return null
    return if (input.startsWith("+") || digits.length > 8) "+$digits" else "+65$digits"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen() {
    var mobile by rememberSaveable { mutableStateOf("") }
    // Step 2 input: the SMS code, or the demo password.
    var code by rememberSaveable { mutableStateOf("") }
    // Number the code was sent to (+country code), or DemoMobile.
    var phone by rememberSaveable { mutableStateOf("") }
    var enteringCode by rememberSaveable { mutableStateOf(false) }
    var resendIn by rememberSaveable { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clickProgress = remember { Animatable(0f) }
    var animatingClick by remember { mutableStateOf(false) }
    val animationScope = rememberCoroutineScope()

    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = true
            controller?.isAppearanceLightNavigationBars = true
        }
    }

    LaunchedEffect(resendIn) {
        if (resendIn > 0) {
            delay(1_000)
            resendIn -= 1
        }
    }

    // Step 1: the demo number goes straight to its password; any other number is texted a code.
    suspend fun requestCode(input: String) {
        if (input == DemoMobile) {
            phone = DemoMobile
            code = ""
            enteringCode = true
            message = null
            keyboardController?.hide()
            focusManager.clearFocus()
            return
        }
        val number = internationalNumber(input) ?: run {
            message = "Enter your mobile number."
            return
        }
        busy = true
        try {
            resendIn = ResidentSession.sendCode(number)
            phone = number
            code = ""
            enteringCode = true
            message = "If this number is registered, a verification code has been sent."
            keyboardController?.hide()
            focusManager.clearFocus()
        } catch (e: ResidentApiException) {
            message = e.message
        } finally {
            busy = false
        }
    }

    // Step 2: a successful sign-in changes ResidentSession.state, which swaps this screen for the app.
    suspend fun signIn() {
        if (phone == DemoMobile) {
            if (code == DemoPassword) {
                keyboardController?.hide()
                focusManager.clearFocus()
                ResidentSession.startDemo()
            } else {
                message = "For this demo, use password $DemoPassword."
            }
            return
        }
        if (code.length < 4) {
            message = "Enter the code we sent you."
            return
        }
        busy = true
        try {
            keyboardController?.hide()
            focusManager.clearFocus()
            ResidentSession.verifyCode(phone, code)
        } catch (e: ResidentApiException) {
            message = e.message
        } finally {
            busy = false
        }
    }

    fun continueLogin() {
        if (animatingClick || busy) return
        animatingClick = true
        animationScope.launch {
            try {
                clickProgress.animateTo(1f, tween(100, easing = FastOutSlowInEasing))
                clickProgress.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            } finally {
                animatingClick = false
            }
            if (enteringCode) signIn() else requestCode(mobile.trim())
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val shortScreen = maxHeight < 710.dp
        val narrowScreen = maxWidth < 350.dp
        Image(
            painter = painterResource(
                if (shortScreen || narrowScreen) R.drawable.login_bg_futuristic_small
                else R.drawable.login_bg_futuristic
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x240D6473),
                    .40f to Color.Transparent,
                    1f to Color(0x17002724)
                )
            )
        )

        // Header and form share one scrollable column so the keyboard pushes the form up
        // by scrolling the header away instead of drawing the form on top of it.
        val scrollState = rememberScrollState()
        val imeVisible = WindowInsets.isImeVisible
        LaunchedEffect(imeVisible) {
            if (imeVisible) {
                snapshotFlow { scrollState.maxValue }.first { it > 0 }
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .heightIn(min = maxHeight)
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 35.dp, top = if (shortScreen) 19.dp else 30.dp)
                ) {
                    // The art's glow overflows the box so the gate itself sits close to the wordmark.
                    Box(Modifier.offset(x = 20.dp).size(110.dp, 88.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.greengate_logo),
                            contentDescription = "Green Gate",
                            modifier = Modifier.requiredSize(110.dp)
                        )
                    }
                    Text(
                        "Green Gate",
                        fontFamily = DMSans,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                    Text(
                        "S E C U R E   ·   S A F E   ·   T O G E T H E R",
                        fontFamily = DMSans,
                        fontSize = 8.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = .88f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(if (shortScreen) 13.dp else 20.dp))
                    Text(
                        "H E L L O",
                        fontFamily = DMSans,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Box {
                        Text(
                            "Welcome",
                            fontFamily = DMSans,
                            fontSize = if (narrowScreen) 47.sp else 51.sp,
                            lineHeight = 61.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                        Image(
                            painter = painterResource(R.drawable.ic_sparkle),
                            contentDescription = null,
                            modifier = Modifier.offset(x = 202.dp, y = 2.dp).size(21.dp)
                        )
                    }
                    Text(
                        "Your community,\nnow smarter.",
                        fontFamily = DMSans,
                        fontSize = 19.sp,
                        lineHeight = 23.sp,
                        color = Color.White.copy(alpha = .70f)
                    )
                }

                Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 36.dp)
                        .padding(bottom = if (shortScreen) 23.dp else 42.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (enteringCode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$phone  ·  Change number",
                                modifier = Modifier.clickable {
                                    enteringCode = false
                                    code = ""
                                    resendIn = 0
                                    message = null
                                },
                                fontFamily = DMSans,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            if (phone != DemoMobile) {
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (resendIn > 0) "Resend in ${resendIn}s" else "Resend code",
                                    modifier = Modifier.clickable(enabled = resendIn == 0 && !busy) {
                                        animationScope.launch { requestCode(phone) }
                                    },
                                    fontFamily = DMSans,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = if (resendIn > 0) .6f else 1f)
                                )
                            }
                        }
                    }
                    GlassLoginField(
                        value = if (enteringCode) code else mobile,
                        onValueChange = { input ->
                            if (enteringCode) {
                                code = input.filter(Char::isDigit).take(10)
                            } else {
                                val digits = input.filter(Char::isDigit).take(15)
                                mobile = if (input.trimStart().startsWith("+")) "+$digits" else digits
                            }
                            message = null
                        },
                        placeholder = when {
                            !enteringCode -> "Mobile Number"
                            phone == DemoMobile -> "Password"
                            else -> "Verification Code"
                        },
                        leadingIcon = R.drawable.ic_phone_outline,
                        code = enteringCode,
                        onDone = ::continueLogin
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = 1f - .04f * clickProgress.value
                                scaleY = 1f - .04f * clickProgress.value
                            }
                            .border(1.dp, GlassBorder, CircleShape)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(Color(0xBD175F53), Color(0xDA0B554B))))
                            .clickable(enabled = !animatingClick && !busy, role = Role.Button, onClick = ::continueLogin),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when {
                                busy && enteringCode -> "Signing In…"
                                busy -> "Sending Code…"
                                enteringCode -> "Sign In"
                                else -> "Continue"
                            },
                            fontFamily = DMSans,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD6F7F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_arrow_right),
                                contentDescription = null,
                                modifier = Modifier.size(25.dp).graphicsLayer {
                                    translationX = 6.dp.toPx() * clickProgress.value
                                }
                            )
                        }
                    }
                    if (message != null) {
                        Text(
                            message!!,
                            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                            textAlign = TextAlign.Center,
                            fontFamily = DMSans,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(11.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = .55f)))
                        Text(
                            "OR",
                            modifier = Modifier.padding(horizontal = 15.dp),
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = .73f)
                        )
                        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = .55f)))
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(WhiteGlass, Color.White.copy(alpha = .34f))))
                            .clickable(role = Role.Button) {
                                message = "Face ID sign-in isn't available yet. Sign in with your mobile number."
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_face_id),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Login with Face ID",
                            fontFamily = DMSans,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: Int,
    code: Boolean,
    onDone: () -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(fontFamily = DMSans, fontSize = 16.sp, color = Color.White),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (code) KeyboardType.NumberPassword else KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { field ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, GlassBorder, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(WhiteGlass, Color.White.copy(alpha = .36f))))
                    .padding(start = 19.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (code) {
                    Icon(Icons.Outlined.Lock, null, tint = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Image(painterResource(leadingIcon), null, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.size(16.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontFamily = DMSans,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = .94f)
                        )
                    }
                    field()
                }
            }
        }
    )
}
