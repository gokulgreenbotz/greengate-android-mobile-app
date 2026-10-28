package com.greenbotz.greengate.core.access

import com.greenbotz.greengate.core.session.AccessQr
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessQrClockTest {
    private val qr = AccessQr("GG1.x", expiresAtMillis = 100_000)

    @Test fun refreshesOneSecondEarly() = assertEquals(99_000, AccessQrClock.refreshAt(qr))

    @Test fun secondsLeftRoundsUp() {
        assertEquals(10, AccessQrClock.secondsLeft(qr, 90_000))
        assertEquals(10, AccessQrClock.secondsLeft(qr, 90_001))
        assertEquals(1, AccessQrClock.secondsLeft(qr, 99_999))
    }

    @Test fun secondsLeftNeverNegative() = assertEquals(0, AccessQrClock.secondsLeft(qr, 120_000))
}
