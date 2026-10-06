package com.example.greengate

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.greengate.ui.theme.DMSans
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val WhiteGlass = Color.White.copy(alpha = .25f)
private val GlassBorder = Color.White.copy(alpha = .58f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    var mobile by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var enteringPassword by rememberSaveable { mutableStateOf(false) }
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

    fun continueLogin() {
        if (animatingClick) return
        animatingClick = true
        animationScope.launch {
            try {
                clickProgress.animateTo(1f, tween(100, easing = FastOutSlowInEasing))
                clickProgress.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                if (!enteringPassword) {
                    if (mobile.trim() == "000000") {
                        enteringPassword = true
                        message = null
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    } else {
                        message = "For this demo, use mobile number 000000."
                    }
                } else if (password == "123456") {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onSignedIn()
                } else {
                    message = "For this demo, use password 123456."
                }
            } finally {
                animatingClick = false
            }
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
                    if (enteringPassword) {
                        Text(
                            "000000  ·  Change number",
                            modifier = Modifier
                                .align(Alignment.Start)
                                .clickable {
                                    enteringPassword = false
                                    password = ""
                                    message = null
                                }
                                .padding(bottom = 8.dp),
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                    GlassLoginField(
                        value = if (enteringPassword) password else mobile,
                        onValueChange = {
                            if (enteringPassword) password = it else mobile = it.filter(Char::isDigit).take(15)
                            message = null
                        },
                        placeholder = if (enteringPassword) "Password" else "Mobile Number",
                        leadingIcon = R.drawable.ic_phone_outline,
                        password = enteringPassword,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
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
                            .clickable(enabled = !animatingClick, role = Role.Button, onClick = ::continueLogin),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (enteringPassword) "Sign In" else "Continue",
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
                                message = "Face ID is unavailable in this demo. Use mobile 000000."
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
    password: Boolean,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
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
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
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
                if (password) {
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
                if (password) {
                    IconButton(onClick = onTogglePassword, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    )
}
