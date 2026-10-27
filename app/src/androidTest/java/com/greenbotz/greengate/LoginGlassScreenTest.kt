package com.greenbotz.greengate

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.greenbotz.greengate.ui.screens.auth.LuxuryLoginScreen
import com.greenbotz.greengate.ui.theme.GreenGateTheme
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise the form without launching MainActivity or making authentication requests. */
@RunWith(AndroidJUnit4::class)
class LoginGlassScreenTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation

    @Test fun phoneStaysVisibleAboveKeyboardAndCountrySearchKeepsTheNumber() {
        val keyguard = instrumentation.targetContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        assumeFalse("The test device must be unlocked", keyguard.isKeyguardLocked)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.enableEdgeToEdge()
                it.setContent { GreenGateTheme { LuxuryLoginScreen() } }
            }
            waitFor { nodes().any { it.text?.contains("Welcome Back") == true } }
            assertTrue(nodes().any { it.text?.contains("Login with QR Code") == true && it.isVisibleToUser })
            screenshot("login-glass-idle")

            editText().performAction(AccessibilityNodeInfo.ACTION_CLICK)
            enterText("81234567")
            val visibleBottom = AtomicInteger(0)
            waitFor {
                scenario.onActivity {
                    val decor = it.window.decorView
                    val insets = ViewCompat.getRootWindowInsets(decor)
                    if (insets?.isVisible(WindowInsetsCompat.Type.ime()) == true) {
                        visibleBottom.set(decor.height - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
                    }
                }
                visibleBottom.get() > 0
            }
            waitFor {
                val bounds = Rect()
                editText().getBoundsInScreen(bounds)
                bounds.bottom <= visibleBottom.get() + 1
            }
            assertTrue(editText().text.toString().contains("81234567"))
            screenshot("login-glass-typing")

            val selector = nodes().first { it.contentDescription?.contains("Choose country") == true }
            click(selector)
            waitFor { nodes().any { it.text?.contains("Choose country") == true } }
            screenshot("country-picker-default")
            click(editText())
            enterText("zzzzzz")
            waitFor { nodes().any { it.text?.contains("No countries found") == true } }
            screenshot("country-picker-empty")
            click(nodes().first { it.contentDescription?.contains("Clear country search") == true })
            waitFor { nodes().none { it.text?.contains("No countries found") == true } }
            enterText("India")
            // "India" also matches "British Indian Ocean Territory" in the search results.
            val india = Regex("\\bIndia\\b")
            waitFor { nodes().any { it.text?.let(india::containsMatchIn) == true && !it.isEditable } }
            screenshot("country-picker-search")
            click(nodes().first { it.text?.let(india::containsMatchIn) == true && !it.isEditable })
            waitFor {
                val screen = nodes()
                screen.none { it.text?.contains("Choose country") == true } &&
                    screen.any { it.text?.contains("+91") == true }
            }
            assertTrue(editText().text.toString().contains("81234567"))
            click(nodes().first { it.contentDescription?.contains("Choose country") == true })
            waitFor { nodes().any { it.text?.contains("Choose country") == true } }
            click(nodes().first { it.contentDescription?.contains("Close country picker") == true })
            waitFor { nodes().none { it.text?.contains("Choose country") == true } }
            assertTrue(editText().text.toString().contains("81234567"))
        }
    }

    private fun nodes(root: AccessibilityNodeInfo? = automation.rootInActiveWindow): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        return listOf(root) + (0 until root.childCount).flatMap { nodes(root.getChild(it)) }
    }

    private fun editText() = nodes().first { it.className == "android.widget.EditText" && it.isEditable }

    private fun click(node: AccessibilityNodeInfo) {
        var target = node
        while (!target.isClickable && target.parent != null) target = target.parent
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun enterText(value: String) {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        assertTrue(editText().performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments))
        automation.waitForIdle(250, 5_000)
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Timed out waiting for the login UI" }
            SystemClock.sleep(100)
        }
        automation.waitForIdle(250, 5_000)
    }

    private fun screenshot(name: String) {
        automation.waitForIdle(250, 5_000)
        val bitmap = automation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
