package com.carhome.launcher

/** Speed rules that don't need Android, so they can be unit-tested. */
object SpeedMath {

    /** What to do with a position that came without a measured speed. */
    enum class NoSpeedFix {
        /** Work the speed out from this fix and the reference fix. */
        DERIVE,
        /** Use no speed, but make this fix the new reference. */
        ANCHOR,
        /** Ignore it for speed (it still counts for the approximate speed). */
        SKIP
    }

    private const val PRECISE_M = 10f

    private fun precise(acc: Float?) = acc == null || acc <= PRECISE_M

    /**
     * A speed is only derived from two precise fixes about 1–10 s apart, so that
     * position noise can't read as a high speed. A precise fix replaces a reference
     * that is too old or imprecise, so speed always comes back after a weak spell.
     */
    fun classifyNoSpeedFix(accuracy: Float?, hasPrev: Boolean, prevAccuracy: Float?, dtS: Float): NoSpeedFix = when {
        !precise(accuracy) -> NoSpeedFix.SKIP
        !hasPrev || !precise(prevAccuracy) || dtS > 10f -> NoSpeedFix.ANCHOR
        dtS < 0.9f -> NoSpeedFix.SKIP          // too close: keep the older reference
        else -> NoSpeedFix.DERIVE
    }

    /** A provider speed (m/s) that can be trusted, or null for NaN, infinity or a glitch over ~324 km/h. */
    fun sanitize(mps: Float): Float? =
        if (!mps.isFinite() || mps > 90f) null else mps.coerceAtLeast(0f)

    /**
     * Light smoothing; jitter under ~2 km/h reads as standing still. [fresh] starts
     * over when the previous value came from another source (approximate, car, held).
     */
    fun smooth(previous: Float?, raw: Float, fresh: Boolean): Float {
        val s = if (previous == null || fresh) raw else previous * 0.35f + raw * 0.65f
        return if (s < 0.55f) 0f else s
    }
}
