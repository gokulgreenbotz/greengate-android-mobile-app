package com.greenbotz.greengate.core.access

import com.greenbotz.greengate.core.session.AccessQr

/** When the My QR screen fetches the next pass and what its countdown shows. */
object AccessQrClock {
    /** A new pass is fetched this long before the shown one expires. */
    const val REFRESH_EARLY_MILLIS = 1_000L

    fun refreshAt(qr: AccessQr): Long = qr.expiresAtMillis - REFRESH_EARLY_MILLIS

    /** Whole seconds left, rounded up, never below zero. */
    fun secondsLeft(qr: AccessQr, nowMillis: Long): Int =
        ((qr.expiresAtMillis - nowMillis + 999) / 1000).coerceAtLeast(0).toInt()
}
