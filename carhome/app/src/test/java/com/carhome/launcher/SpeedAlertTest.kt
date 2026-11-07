package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedAlertTest {

    private val alert = SpeedAlert()
    private var now = 1_000L

    /** One tick, one second after the previous one. */
    private fun at(
        speed: Float?, limit: Int? = 120, margin: Int = 0, mode: AlertMode = AlertMode.BEEP_ONCE,
        enabled: Boolean = true, speak: Boolean = false, testing: Boolean = false
    ): SpeedAlert.Result {
        now += 1_000L
        return alert.update(enabled, speed, limit, margin, mode, speak, testing, now)
    }

    @Test fun belowLimitIsQuiet() {
        val r = at(110f)
        assertFalse(r.overspeed); assertFalse(r.beep); assertFalse(r.speak)
    }

    @Test fun overLimitTurnsRedAndBeepsOnce() {
        val first = at(125f)
        assertTrue(first.overspeed); assertTrue(first.beep)
        val second = at(126f)
        assertTrue(second.overspeed); assertFalse(second.beep)
    }

    @Test fun graceMarginMovesTheTrigger() {
        assertFalse(at(124f, margin = 5).overspeed)
        assertTrue(at(126f, margin = 5).overspeed)
    }

    @Test fun hysteresisKeepsAlertUntilThreeBelowTrigger() {
        at(125f)
        assertTrue("just under the limit stays red", at(119f).overspeed)
        assertTrue("exactly 3 under still red", at(117f).overspeed)
        assertFalse("more than 3 under clears", at(116.9f).overspeed)
    }

    @Test fun comingUpFromBelowIntoTheBandIsNotAnAlert() {
        at(100f)
        assertFalse(at(119f).overspeed)
    }

    @Test fun newEpisodeBeepsAgainAfterSlowingDown() {
        assertTrue(at(125f).beep)
        at(110f)
        assertTrue(at(125f).beep)
    }

    @Test fun hoveringInTheBandDoesNotBeepAgain() {
        assertTrue(at(125f).beep)
        assertFalse(at(119f).beep)
        assertFalse(at(121f).beep)
    }

    @Test fun repeatModeBeepsImmediatelyThenEveryTenSeconds() {
        val beeps = (1..25).map { at(130f, mode = AlertMode.BEEP_REPEAT).beep }
        assertTrue("first tick beeps", beeps[0])
        assertEquals("ticks 1, 11 and 21 beep", listOf(0, 10, 20), beeps.withIndex().filter { it.value }.map { it.index })
    }

    @Test fun repeatModeBeepsRightAwayEvenSoonAfterBoot() {
        val fresh = SpeedAlert()
        assertTrue(fresh.update(true, 130f, 120, 0, AlertMode.BEEP_REPEAT, false, false, now = 2_000L).beep)
    }

    @Test fun screenOnlyNeverBeeps() {
        repeat(30) { assertFalse(at(150f, mode = AlertMode.SCREEN).beep) }
        assertTrue(at(150f, mode = AlertMode.SCREEN).overspeed)
    }

    @Test fun speaksOncePerEpisode() {
        assertTrue(at(125f, speak = true).speak)
        assertFalse(at(126f, speak = true).speak)
        at(100f, speak = true)
        assertTrue(at(125f, speak = true).speak)
    }

    @Test fun noSpeedNoLimitOrOffMeansNoAlert() {
        assertFalse(at(null).overspeed)
        assertFalse(at(150f, limit = null).overspeed)
        assertFalse(at(150f, enabled = false).overspeed)
    }

    @Test fun losingSpeedEndsTheEpisode() {
        assertTrue(at(125f).beep)
        at(null)
        assertTrue("alert starts fresh after the gap", at(125f).beep)
    }

    @Test fun testButtonShowsRedEvenWhenSlow() {
        assertTrue(at(50f, testing = true).overspeed)
        assertTrue(at(null, testing = true).overspeed)
        assertFalse(at(50f, testing = false).overspeed)
    }
}
