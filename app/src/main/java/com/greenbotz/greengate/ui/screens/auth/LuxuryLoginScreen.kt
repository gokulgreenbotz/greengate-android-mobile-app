package com.greenbotz.greengate.ui.screens.auth

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.greenbotz.greengate.R
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.LoginExperience
import com.greenbotz.greengate.core.session.ResidentApiException
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.PlayfairDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val Navy = Color(0xFF061A2E)

// Same slide as the booking flow (NavTransitions.kt).
private const val StepSlideMillis = 380

// How long the full code stays on screen before signing in on its own.
private const val FullCodePauseMillis = 400L

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LuxuryLoginScreen(experience: LoginExperience = AppPreferences.loginExperience) {
    LoginAppearance(experience) { LuxuryLoginContent() }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LuxuryLoginContent() {
    var mobile by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var selectedCountryIso2 by rememberSaveable { mutableStateOf("SG") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var enteringCode by rememberSaveable { mutableStateOf(false) }
    // Seconds until the server allows "Resend code".
    var resendIn by rememberSaveable { mutableStateOf(0) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    var inputFocused by remember { mutableStateOf(false) }
    val bringInputIntoView = remember { BringIntoViewRequester() }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible, inputFocused) {
        if (imeVisible && inputFocused) {
            delay(180)
            bringInputIntoView.bringIntoView()
        }
    }
    val selectedCountry = remember(selectedCountryIso2) {
        CountryCatalog.all.firstOrNull { it.iso2 == selectedCountryIso2 } ?: CountryCatalog.singapore
    }

    val palette = LocalLoginPalette.current
    DisposableEffect(activity, palette.light) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        controller?.isAppearanceLightStatusBars = palette.light
        controller?.isAppearanceLightNavigationBars = palette.light
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

    fun submit() = scope.launch {
        if (busy) return@launch
        if (!enteringCode) {
            if (mobile == "000000") {
                phone = mobile
                code = ""
                message = null
                enteringCode = true
                return@launch
            }
            val digits = mobile.filter(Char::isDigit)
            if (digits.length < 8) {
                message = "Enter your mobile number."
                return@launch
            }
            busy = true
            try {
                phone = "${selectedCountry.dialCode}$digits"
                resendIn = ResidentSession.sendCode(phone)
                code = ""
                enteringCode = true
                message = "If this number is registered, a verification code has been sent."
            } catch (e: ResidentApiException) {
                message = e.message
            } finally {
                busy = false
            }
        } else if (phone == "000000") {
            if (code == "123456") ResidentSession.startDemo()
            else message = "For this demo, use password 123456."
        } else {
            if (code.length < OtpLength) {
                message = "Enter the $OtpLength-digit code we sent you."
                return@launch
            }
            busy = true
            try {
                ResidentSession.verifyCode(phone, code)
            } catch (e: ResidentApiException) {
                message = e.message
            } finally {
                busy = false
            }
        }
    }

    // Signs in once the last digit is in, after a short pause so the whole code shows first.
    // Skipped if a digit is deleted during the pause.
    fun autoSubmit() = scope.launch {
        if (busy) return@launch
        busy = true
        delay(FullCodePauseMillis)
        busy = false
        if (code.length == OtpLength) submit()
    }

    fun resendCode() {
        scope.launch {
            busy = true
            try {
                resendIn = ResidentSession.sendCode(phone)
                code = ""
                message = "If this number is registered, a new code has been sent."
            } catch (e: ResidentApiException) {
                message = e.message
            } finally {
                busy = false
            }
        }
    }

    fun changeNumber() {
        enteringCode = false
        code = ""
        message = null
    }

    BackHandler(enabled = enteringCode) { changeNumber() }

    // The code step is its own screen: it slides in over the number screen, and Back slides it away.
    AnimatedContent(
        targetState = enteringCode,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val slide = tween<IntOffset>(StepSlideMillis, easing = FastOutSlowInEasing)
            if (targetState) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide) togetherWith
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide) { it / 4 }
            } else {
                // Going back, the code screen stays on top while it slides away.
                (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, slide) { it / 4 } togetherWith
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slide))
                    .apply { targetContentZIndex = -1f }
            }
        },
        label = "Sign-in step"
    ) { codeStep ->
        if (codeStep) {
            OtpVerificationScreen(
                phone = phone,
                code = code,
                onCodeChange = {
                    code = it
                    message = null
                    // Sign in once the last digit is in (typed, pasted or autofilled).
                    if (it.length == OtpLength) autoSubmit()
                },
                message = message,
                busy = busy,
                resendIn = resendIn,
                onVerify = { submit() },
                onResend = if (phone == "000000") null else ::resendCode,
                onBack = ::changeNumber
            )
        } else {
            LuxuryLoginBackdrop(showBranding = false) {
                BoxWithConstraints(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                ) {
                    val viewportHeight = maxHeight
                    // Scroll the branding away when the keyboard needs room for the form.
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .heightIn(min = viewportHeight).padding(horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GreenGateLoginBranding(Modifier.padding(top = 12.dp))
                        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
                        LoginGlassPanel(
                            Modifier.widthIn(max = 480.dp).fillMaxWidth().testTag("login_glass_panel")
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Welcome Back",
                                    color = Ivory,
                                    fontFamily = if (palette.light) PlayfairDisplay else DMSans,
                                    fontWeight = if (palette.light) FontWeight.Normal else FontWeight.Medium,
                                    fontSize = 25.sp,
                                    lineHeight = 32.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Enter your mobile number to continue",
                                    color = Muted,
                                    fontFamily = DMSans,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(18.dp))
                                LoginInput(
                                    value = mobile,
                                    placeholder = "Mobile Number",
                                    onValueChange = { value ->
                                        val digits = value.filter(Char::isDigit).take(15)
                                        // The lightest tick the phone has, per digit typed or deleted. Needs no
                                        // permission; silent while "Use vibration & haptics" is off.
                                        if (digits != mobile) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        mobile = digits
                                        message = null
                                    },
                                    onDone = { submit() },
                                    country = selectedCountry,
                                    onCountryClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        showCountryPicker = true
                                    },
                                    focused = inputFocused,
                                    onFocusChange = { inputFocused = it },
                                    modifier = Modifier.bringIntoViewRequester(bringInputIntoView)
                                )
                                Spacer(Modifier.height(16.dp))
                                LuxuryGoldButton(
                                    if (busy) "Please wait…" else "Continue",
                                    enabled = !busy,
                                    onClick = { submit() }
                                )
                                message?.let { currentMessage ->
                                    Text(
                                        currentMessage, color = Muted, fontFamily = DMSans,
                                        fontSize = 12.sp, textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                Spacer(Modifier.height(20.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    HorizontalRule(Modifier.weight(1f))
                                    Text(
                                        "OR", color = Muted, fontFamily = DMSans, fontSize = 12.sp,
                                        letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                    HorizontalRule(Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(18.dp))
                                Row(
                                    Modifier.fillMaxWidth().height(48.dp).loginGlassControl()
                                        .clickable(role = Role.Button) { message = "QR login is not available yet." }
                                        .padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painterResource(R.drawable.ic_qr_login), null, Modifier.size(23.dp),
                                        colorFilter = if (palette.light) ColorFilter.tint(Gold) else null
                                    )
                                    Text(
                                        "Login with QR Code", color = Ivory, fontFamily = DMSans,
                                        fontSize = 14.sp, modifier = Modifier.padding(start = 14.dp).weight(1f)
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowForward, null,
                                        tint = Ivory, modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        // Equal room above and below centres the panel in the space under the
                        // branding (OtpVerificationScreen uses the same layout).
                        Spacer(Modifier.weight(1f).heightIn(min = 18.dp))
                    }
                }
            }
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            selectedIso2 = selectedCountryIso2,
            onSelected = {
                selectedCountryIso2 = it.iso2
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }
}

/** Artwork and GreenGate branding behind both sign-in screens. */
@Composable
internal fun LuxuryLoginBackdrop(showBranding: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val palette = LocalLoginPalette.current
    val backdrop = ImageBitmap.imageResource(palette.artwork)
    var backdropSize by remember { mutableStateOf(IntSize.Zero) }
    var backdropOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.fillMaxSize().background(palette.background)
            .onSizeChanged { backdropSize = it }
            .onGloballyPositioned { backdropOrigin = it.positionInRoot() }
    ) {
        Image(
            bitmap = backdrop,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (showBranding) GreenGateLoginBranding(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 28.dp)
        )

        CompositionLocalProvider(
            LocalLoginGlassBackdrop provides LoginGlassBackdrop(backdrop, backdropSize, backdropOrigin)
        ) { content() }
    }
}

@Composable
internal fun LuxuryGoldButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(48.dp)) {
        LoginSurfaceGlow(Modifier.matchParentSize(), strength = .22f)
    Box(
        Modifier
            .matchParentSize()
            .loginGoldButton()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans)
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Navy,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(22.dp))
    }
    }
}

@Composable
private fun LoginInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    country: CountryInfo,
    onCountryClick: () -> Unit,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(50.dp)
            .onFocusChanged { onFocusChange(it.isFocused) }.testTag("login_phone_input"),
        singleLine = true,
        textStyle = TextStyle(color = Ivory, fontFamily = DMSans, fontSize = 15.sp),
        cursorBrush = SolidColor(Gold),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box(Modifier.fillMaxSize()) {
                if (focused) LoginSurfaceGlow(Modifier.matchParentSize(), strength = .12f)
            Row(
                Modifier.fillMaxSize().loginGlassControl(focused).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onCountryClick)
                        .testTag("login_country_selector"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(country.flag, fontSize = 19.sp)
                    Text(country.dialCode, color = Ivory, fontFamily = DMSans, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Choose country",
                        tint = Ivory,
                        modifier = Modifier.padding(start = 5.dp).size(18.dp)
                    )
                }
                Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(22.dp).background(Stroke.copy(alpha = .55f)))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = Muted, fontFamily = DMSans, fontSize = 15.sp, maxLines = 1)
                    innerTextField()
                }
            }
            }
        }
    )
}

@Composable
internal fun HorizontalRule(modifier: Modifier) {
    Box(modifier.height(1.dp).background(Stroke.copy(alpha = .55f)))
}

