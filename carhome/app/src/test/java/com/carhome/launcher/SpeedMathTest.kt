package com.carhome.launcher

import com.carhome.launcher.SpeedMath.NoSpeedFix.ANCHOR
import com.carhome.launcher.SpeedMath.NoSpeedFix.DERIVE
import com.carhome.launcher.SpeedMath.NoSpeedFix.SKIP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedMathTest {

    private fun classify(acc: Float?, prevAcc: Float?, dtS: Float?) =
        SpeedMath.classifyNoSpeedFix(acc, hasPrev = dtS != null, prevAccuracy = prevAcc, dtS = dtS ?: 0f)

    @Test fun twoPreciseFixesASecondApartGiveASpeed() = assertEquals(DERIVE, classify(5f, 5f, 1f))

    @Test fun fixesTooCloseKeepTheOldReference() = assertEquals(SKIP, classify(5f, 5f, 0.5f))

    @Test fun firstPreciseFixBecomesTheReference() = assertEquals(ANCHOR, classify(5f, null, null))

    @Test fun impreciseFixIsNeverUsed() {
        assertEquals(SKIP, classify(25f, 5f, 1f))
        assertEquals(SKIP, classify(25f, null, null))
    }

    @Test fun preciseFixAfterALongGapBecomesTheNewReference() = assertEquals(ANCHOR, classify(5f, 5f, 12f))

    @Test fun preciseFixAfterAnImpreciseReferenceReplacesIt() = assertEquals(ANCHOR, classify(5f, 30f, 1f))

    @Test fun speedComesBackAfterAWeakSignalSpell() {
        // Reference at t=0, then 15 s of poor fixes, then good fixes every second again
        var refT = 0f
        var refAcc = 5f
        val results = mutableListOf<SpeedMath.NoSpeedFix>()
        val fixes = (1..15).map { it.toFloat() to 30f } + (16..20).map { it.toFloat() to 5f }
        for ((t, acc) in fixes) {
            val r = classify(acc, refAcc, t - refT)
            if (r != SKIP) { refT = t; refAcc = acc }
            results += r
        }
        assertEquals("poor fixes are skipped", List(15) { SKIP }, results.take(15))
        assertEquals("first good fix re-anchors", ANCHOR, results[15])
        assertEquals("speed resumes on the next fix", List(4) { DERIVE }, results.drop(16))
    }

    @Test fun badSpeedsAreRejected() {
        assertNull(SpeedMath.sanitize(Float.NaN))
        assertNull(SpeedMath.sanitize(Float.POSITIVE_INFINITY))
        assertNull("over 324 km/h is a glitch", SpeedMath.sanitize(95f))
    }

    @Test fun normalSpeedsPassAndNegativeBecomesZero() {
        assertEquals(27.8f, SpeedMath.sanitize(27.8f)!!, 0.0001f)
        assertEquals(0f, SpeedMath.sanitize(0f)!!, 0.0001f)
        assertEquals(0f, SpeedMath.sanitize(-0.4f)!!, 0.0001f)
    }

    @Test fun smoothingStartsFreshAfterAnotherSource() {
        // After approximate/car/held speed, the first real GPS speed is used as is
        assertEquals(10f, SpeedMath.smooth(previous = 40f, raw = 10f, fresh = true), 0.0001f)
        assertEquals(0.35f * 40f + 0.65f * 10f, SpeedMath.smooth(previous = 40f, raw = 10f, fresh = false), 0.0001f)
        assertEquals(10f, SpeedMath.smooth(previous = null, raw = 10f, fresh = false), 0.0001f)
    }

    @Test fun joggleUnderTwoKmhReadsAsStandingStill() {
        assertEquals(0f, SpeedMath.smooth(previous = null, raw = 0.5f, fresh = false), 0.0001f)
    }
}
