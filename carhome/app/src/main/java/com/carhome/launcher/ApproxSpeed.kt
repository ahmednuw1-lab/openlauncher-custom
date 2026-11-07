package com.carhome.launcher

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Rough speed from internet/Wi-Fi positions when there is no GPS fix.
 * Free of Android so it can be unit-tested.
 */
object ApproxSpeed {

    /** A position: time on the boot clock (ns), and accuracy in metres if known. */
    data class Fix(val tNs: Long, val lat: Double, val lon: Double, val accuracy: Float?)

    private const val SEC = 1_000_000_000L

    /**
     * Average speed (m/s) between two positions 20–90 s apart, or null when it
     * can't be told from position noise. 0 when clearly standing still.
     * [fixes] must be oldest first.
     */
    fun estimate(fixes: List<Fix>, nowNs: Long): Float? {
        val last = fixes.lastOrNull() ?: return null
        if (nowNs - last.tNs > 60 * SEC) return null
        val first = fixes.firstOrNull { (last.tNs - it.tNs) in 20 * SEC..90 * SEC } ?: return null
        val dt = (last.tNs - first.tNs) / 1e9f
        val d = distanceM(first.lat, first.lon, last.lat, last.lon).toFloat()
        val noise = (first.accuracy ?: 50f) + (last.accuracy ?: 50f)
        if (d < noise) return if (dt >= 45f && d < noise * 0.5f) 0f else null
        val v = d / dt
        return if (v > 70f) null else v   // over ~250 km/h is a position jump, not driving
    }

    /** Great-circle distance in metres. */
    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_008.8
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lon2 - lon1)
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
