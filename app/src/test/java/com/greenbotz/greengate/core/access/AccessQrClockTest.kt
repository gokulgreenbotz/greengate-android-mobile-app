package com.greenbotz.greengate.core.access

import com.greenbotz.greengate.core.session.AccessQr
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessQrClockTest {
    private val qr = AccessQr("GG1.x", expiresAtMillis = 100_000)

    @Test fun refreshesFiveSecondsEarly() = assertEquals(95_000, AccessQrClock.refreshAt(qr))

    @Test fun secondsLeftRoundsUp() {
        assertEquals(30, AccessQrClock.secondsLeft(qr, 70_000))
        assertEquals(30, AccessQrClock.secondsLeft(qr, 70_001))
        assertEquals(1, AccessQrClock.secondsLeft(qr, 99_999))
    }

    @Test fun secondsLeftNeverNegative() = assertEquals(0, AccessQrClock.secondsLeft(qr, 120_000))
}
