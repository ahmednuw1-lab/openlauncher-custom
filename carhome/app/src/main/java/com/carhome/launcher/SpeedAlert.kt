package com.carhome.launcher

/**
 * The speed-alert rules, free of Android so they can be unit-tested.
 * Call [update] once a second; it says whether the screen should be red and
 * whether to beep or speak on this tick.
 */
class SpeedAlert {

    data class Result(val overspeed: Boolean, val beep: Boolean, val speak: Boolean)

    private var overspeed = false
    private var episodeBeeped = false
    private var spoke = false
    private var lastBeepAt: Long? = null

    /**
     * @param speed current speed in the user's unit, or null when unknown (or only approximate)
     * @param limit the limit in the user's unit, or null when there is none
     * @param margin grace margin added to the limit before alerting
     * @param testing true while the Settings "Test alert" preview runs
     */
    fun update(
        enabled: Boolean, speed: Float?, limit: Int?, margin: Int,
        mode: AlertMode, speak: Boolean, testing: Boolean, now: Long
    ): Result {
        if (!enabled || speed == null || limit == null) {
            reset()
            return Result(testing, beep = false, speak = false)
        }
        val trigger = limit + margin
        var beep = false
        var say = false
        when {
            speed > trigger -> {
                overspeed = true
                when (mode) {
                    AlertMode.SCREEN -> Unit
                    AlertMode.BEEP_ONCE -> if (!episodeBeeped) { beep = true; episodeBeeped = true }
                    AlertMode.BEEP_REPEAT -> {
                        val last = lastBeepAt
                        if (last == null || now - last >= 10_000L) { beep = true; lastBeepAt = now }
                    }
                }
                if (speak && !spoke) { say = true; spoke = true }
            }
            // Small hysteresis so hovering right at the limit doesn't flicker
            speed < trigger - 3 -> reset()
            else -> Unit   // in the band: keep whatever state we were in
        }
        return Result(overspeed || testing, beep, say)
    }

    private fun reset() {
        overspeed = false
        episodeBeeped = false
        spoke = false
        lastBeepAt = null
    }
}
