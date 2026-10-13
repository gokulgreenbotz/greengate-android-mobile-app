package com.greenbotz.greengate

import android.app.Application
import com.greenbotz.greengate.core.session.AppPreferences

/**
 * App entry point. Loads saved settings once, before any screen is drawn.
 *
 * The call signaling service is NOT started here: Android 12+ blocks starting a
 * foreground service while the app is in the background (e.g. woken after a reboot),
 * so MainActivity keeps starting it.
 */
class GreenGateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPreferences.load(this)
    }
}
