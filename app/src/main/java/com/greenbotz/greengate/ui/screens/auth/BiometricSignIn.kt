package com.greenbotz.greengate.ui.screens.auth

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.greenbotz.greengate.core.session.AppPreferences
import com.greenbotz.greengate.core.session.ResidentSession
import com.greenbotz.greengate.ui.theme.DMSans
import com.greenbotz.greengate.ui.theme.PlayfairDisplay

// Fingerprint / face sign-in. "Weak" (class 2) biometrics are accepted so most phones' face unlock
// counts too; the check guards the saved sign-in, which stays Keystore-encrypted as before.
private const val Authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK

internal enum class BiometricStatus { READY, NOT_ENROLLED, UNAVAILABLE }

/**
 * Whether this phone can check a fingerprint or face for the app. Android 10+ only: on older versions
 * androidx.biometric shows its own fingerprint dialog, which needs an AppCompat theme this app doesn't use.
 */
internal fun biometricStatus(context: Context): BiometricStatus {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return BiometricStatus.UNAVAILABLE
    return when (BiometricManager.from(context).canAuthenticate(Authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.READY
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
        else -> BiometricStatus.UNAVAILABLE
    }
}

/** "fingerprint", "face" or "fingerprint or face", from the sensors this phone has. */
internal fun biometricLabel(context: Context): String {
    val pm = context.packageManager
    val finger = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    val face = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm.hasSystemFeature(PackageManager.FEATURE_FACE)
    return when {
        finger && face -> "fingerprint or face"
        face -> "face"
        else -> "fingerprint"
    }
}

internal fun biometricIcon(label: String): ImageVector = if (label == "face") Icons.Rounded.Face else Icons.Rounded.Fingerprint

internal fun Context.findFragmentActivity(): FragmentActivity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<FragmentActivity>().firstOrNull()

/**
 * Shows the system fingerprint / face prompt. [onError] gets null when the resident backed out
 * (Cancel, Back, or the prompt was dismissed), else the system's message, e.g. after too many tries.
 */
internal fun FragmentActivity.askBiometric(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit = {}
) {
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            val backedOut = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_CANCELED
            onError(if (backedOut) null else errString.toString())
        }
        // A single miss: the prompt itself says "Not recognized" and lets the resident try again.
    }
    BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(Authenticators)
            // Face unlock goes straight through, without an extra Confirm tap.
            .setConfirmationRequired(false)
            .build()
    )
}

/** Turns fingerprint / face sign-in on once the resident has passed the check, so it's known to work. */
internal fun FragmentActivity.turnOnBiometricLogin(label: String, onError: (String?) -> Unit = {}) = askBiometric(
    title = "Turn on ${label} sign-in",
    subtitle = "Confirm it's you",
    onSuccess = { ResidentSession.useBiometricLogin(true) },
    onError = onError
)

/**
 * The login page while a saved sign-in is locked: asks for the resident's fingerprint or face straight
 * away, with "Use SMS code instead" for when that can't work.
 */
@Composable
internal fun BiometricUnlockScreen(onUseSms: () -> Unit) {
    LoginAppearance(AppPreferences.loginExperience) { BiometricUnlockContent(onUseSms) }
}

@Composable
private fun BiometricUnlockContent(onUseSms: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val label = remember(context) { biometricLabel(context) }
    val ready = remember(context) { biometricStatus(context) == BiometricStatus.READY }
    val title = label.replaceFirstChar { it.uppercase() }
    var message by remember {
        mutableStateOf(if (ready) null else "$title isn't set up on this phone any more. Use an SMS code to sign in.")
    }

    // Light or dark status bar icons to suit the artwork, as on the login page.
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

    fun ask() {
        message = null
        activity?.askBiometric(
            title = "Unlock GreenGate",
            subtitle = "Use your $label",
            onSuccess = ResidentSession::unlock,
            onError = { message = it }
        )
    }

    // Ask straight away, like the phone's own lock screen.
    LaunchedEffect(Unit) { if (ready) ask() }

    LuxuryLoginBackdrop(showBranding = false) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val viewportHeight = maxHeight
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight).padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GreenGateLoginBranding(Modifier.padding(top = 12.dp))
                Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
                LoginGlassPanel(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
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
                            "Use your $label to continue",
                            color = Muted, fontFamily = DMSans, fontSize = 12.sp, lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Box(
                            Modifier.size(84.dp).clip(CircleShape).background(Gold.copy(alpha = .10f))
                                .border(1.5.dp, Gold, CircleShape)
                                .clickable(enabled = ready, role = Role.Button, onClickLabel = "Unlock with $label") { ask() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(biometricIcon(label), null, tint = Gold, modifier = Modifier.size(46.dp))
                        }
                        Spacer(Modifier.height(20.dp))
                        if (ready) LuxuryGoldButton("Unlock with $title", enabled = true) { ask() }
                        message?.let {
                            Text(
                                it, color = Muted, fontFamily = DMSans, fontSize = 12.sp, textAlign = TextAlign.Center,
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
                                .clickable(role = Role.Button, onClick = onUseSms)
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Sms, null, tint = Gold, modifier = Modifier.size(22.dp))
                            Text(
                                "Use SMS code instead", color = Ivory, fontFamily = DMSans,
                                fontSize = 14.sp, modifier = Modifier.padding(start = 14.dp).weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Ivory, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Spacer(Modifier.weight(1f).heightIn(min = 18.dp))
            }
        }
    }
}

/** Asked right after an SMS sign-in while fingerprint / face sign-in is off. */
@Composable
internal fun BiometricOfferDialog() {
    val context = LocalContext.current
    if (remember(context) { biometricStatus(context) } != BiometricStatus.READY) {
        LaunchedEffect(Unit) { ResidentSession.dismissBiometricOffer() }
        return
    }
    val label = remember(context) { biometricLabel(context) }
    AlertDialog(
        onDismissRequest = ResidentSession::dismissBiometricOffer,
        icon = { Icon(biometricIcon(label), null) },
        title = { Text("Sign in faster?", fontFamily = DMSans) },
        text = {
            Text("Open GreenGate with your $label next time, instead of waiting for an SMS code.", fontFamily = DMSans)
        },
        confirmButton = {
            TextButton(onClick = {
                ResidentSession.dismissBiometricOffer()
                context.findFragmentActivity()?.turnOnBiometricLogin(label)
            }) { Text("Turn on", fontFamily = DMSans) }
        },
        dismissButton = {
            TextButton(onClick = ResidentSession::dismissBiometricOffer) { Text("Not now", fontFamily = DMSans) }
        }
    )
}
