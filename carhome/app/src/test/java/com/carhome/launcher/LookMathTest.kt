package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The weather strip's hours and the speed gauge's scale. */
class LookMathTest {

    @Test fun fewerHoursOnANarrowScreenMoreOnAWideOne() {
        assertEquals(0, LookMath.hourCells(40f))
        assertEquals(1, LookMath.hourCells(58f))
        assertEquals(3, LookMath.hourCells(200f))
        assertEquals(5, LookMath.hourCells(300f))
        assertEquals(10, LookMath.hourCells(600f))
    }

    @Test fun noRoomOrBadNumbersGiveNoHours() {
        assertEquals(0, LookMath.hourCells(0f))
        assertEquals(0, LookMath.hourCells(-20f))
        assertEquals(0, LookMath.hourCells(Float.NaN))
        assertEquals(0, LookMath.hourCells(300f, 0f))
    }

    @Test fun warmestHourIsHighestOnTheCurve() {
        val l = LookMath.tempLevels(listOf(27.0, 26.0, 24.0, 21.0, 30.0))
        assertEquals(1f, l[4], 0.0001f)
        assertEquals(0f, l[3], 0.0001f)
        assertEquals(6f / 9f, l[0], 0.0001f)
    }

    @Test fun aSmallChangeStaysNearTheMiddle() {
        // 1° of change spread over the 6° minimum: no jump from bottom to top
        val l = LookMath.tempLevels(listOf(22.0, 23.0))
        assertEquals(5f / 12f, l[0], 0.0001f)
        assertEquals(7f / 12f, l[1], 0.0001f)
        // One temperature, or the same all day: a flat line in the middle
        assertEquals(listOf(0.5f), LookMath.tempLevels(listOf(25.0)))
        assertEquals(listOf(0.5f, 0.5f, 0.5f), LookMath.tempLevels(listOf(25.0, 25.0, 25.0)))
    }

    @Test fun badTemperaturesDoNotBreakTheCurve() {
        assertEquals(emptyList<Float>(), LookMath.tempLevels(emptyList()))
        assertEquals(listOf(0.5f, 0.5f), LookMath.tempLevels(listOf(Double.NaN, Double.POSITIVE_INFINITY)))
        val l = LookMath.tempLevels(listOf(20.0, Double.NaN, 32.0))
        assertEquals(0f, l[0], 0.0001f)
        assertEquals(0.5f, l[1], 0.0001f)
        assertEquals(1f, l[2], 0.0001f)
        // A bad minimum spread falls back to 1°
        assertEquals(1f, LookMath.tempLevels(listOf(20.0, 21.0), Double.NaN)[1], 0.0001f)
    }

    @Test fun gaugeFractionStaysOnTheDial() {
        assertEquals(0f, LookMath.gaugeFraction(0f, 200f), 0f)
        assertEquals(0.5f, LookMath.gaugeFraction(100f, 200f), 0.0001f)
        assertEquals(1f, LookMath.gaugeFraction(260f, 200f), 0f)
        assertEquals(0f, LookMath.gaugeFraction(-5f, 200f), 0f)
        assertEquals(0f, LookMath.gaugeFraction(Float.NaN, 200f), 0f)
        assertEquals(0f, LookMath.gaugeFraction(50f, 0f), 0f)
    }

    @Test fun redZoneStartsAtTheLimit() {
        assertEquals(0.6f, LookMath.redZoneStart(120, 200f)!!, 0.0001f)
        assertNull(LookMath.redZoneStart(null, 200f))
        assertNull(LookMath.redZoneStart(0, 200f))
        assertNull("a limit at or past the end of the dial has no red zone", LookMath.redZoneStart(200, 200f))
    }

    @Test fun usualScaleUnlessTheLimitIsNearItsEnd() {
        assertEquals(200f, LookMath.gaugeMax(true, null), 0f)
        assertEquals(200f, LookMath.gaugeMax(true, 120), 0f)
        assertEquals(130f, LookMath.gaugeMax(false, 75), 0f)
        // A 220 km/h limit still leaves room for a red zone
        val max = LookMath.gaugeMax(true, 220)
        assertEquals(260f, max, 0f)
        assertEquals(true, LookMath.redZoneStart(220, max) != null)
    }
}
