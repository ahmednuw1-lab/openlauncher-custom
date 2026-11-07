package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApproxSpeedTest {

    private val s = 1_000_000_000L          // one second in nanoseconds
    private val lat0 = 25.38
    private val lon0 = 49.59
    private val mPerDegLat = 111_195.0

    /** A fix [north] metres north of the start, [t] seconds in. */
    private fun fix(t: Long, north: Double, acc: Float? = 30f) =
        ApproxSpeed.Fix(t * s, lat0 + north / mPerDegLat, lon0, acc)

    @Test fun drivingAt72kmhOverAMinute() {
        val v = ApproxSpeed.estimate(listOf(fix(0, 0.0), fix(30, 600.0), fix(60, 1200.0)), nowNs = 61 * s)
        assertNotNull(v)
        assertEquals(20f, v!!, 0.5f)        // 1200 m / 60 s = 20 m/s = 72 km/h
    }

    @Test fun positionsTooCloseInTimeGiveNothing() {
        assertNull(ApproxSpeed.estimate(listOf(fix(0, 0.0), fix(10, 200.0)), nowNs = 11 * s))
    }

    @Test fun movementInsideTheNoiseIsUnknownNotZero() {
        // 40 m in 30 s with ±30 m positions: can't tell
        assertNull(ApproxSpeed.estimate(listOf(fix(0, 0.0), fix(30, 40.0)), nowNs = 31 * s))
    }

    @Test fun clearlyStandingStillReadsZero() {
        val v = ApproxSpeed.estimate(listOf(fix(0, 0.0), fix(50, 10.0)), nowNs = 51 * s)
        assertEquals(0f, v!!, 0.001f)
    }

    @Test fun jumpFasterThan250kmhIsIgnored() {
        assertNull(ApproxSpeed.estimate(listOf(fix(0, 0.0), fix(30, 3000.0)), nowNs = 31 * s))
    }

    @Test fun oldPositionsGiveNothing() {
        assertNull(ApproxSpeed.estimate(listOf(fix(0, 0.0), fix(60, 1200.0)), nowNs = 200 * s))
    }

    @Test fun usesThePairAtLeast20SecondsApart() {
        // first usable start point is the oldest one within 20–90 s of the newest
        val v = ApproxSpeed.estimate(listOf(fix(0, 0.0), fix(50, 1000.0), fix(80, 1600.0)), nowNs = 81 * s)
        assertEquals(20f, v!!, 0.5f)
    }

    @Test fun missingAccuracyCountsAsFiftyMetres() {
        // 80 m in 30 s: under the 100 m noise of two unknown-accuracy fixes
        assertNull(ApproxSpeed.estimate(listOf(fix(0, 0.0, null), fix(30, 80.0, null)), nowNs = 31 * s))
    }

    @Test fun emptyOrSingleGivesNothing() {
        assertNull(ApproxSpeed.estimate(emptyList(), nowNs = s))
        assertNull(ApproxSpeed.estimate(listOf(fix(0, 0.0)), nowNs = s))
    }

    @Test fun distanceIsAccurate() {
        // 1 km due north
        val d = ApproxSpeed.distanceM(lat0, lon0, lat0 + 1000.0 / mPerDegLat, lon0)
        assertEquals(1000.0, d, 3.0)
    }
}
