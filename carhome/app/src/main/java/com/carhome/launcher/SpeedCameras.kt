package com.carhome.launcher

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** A speed camera from OpenStreetMap (highway=speed_camera). */
data class SpeedCamera(
    val id: Long, val lat: Double, val lon: Double, val maxKmh: Int? = null,
    /** Direction of travel it checks (compass degrees), when the map says */
    val directionDeg: Float? = null
)

/** The camera the car is heading to, and how far it is. */
data class CameraAhead(val camera: SpeedCamera, val distanceM: Double)

/** Which camera is ahead (no Android, so it can be unit-tested). */
object SpeedCameras {

    /** Warn this far before a camera. */
    const val WARN_M = 500.0

    /** Without a heading only a camera this close counts (it's on our road, whichever way). */
    private const val NO_HEADING_M = 150.0

    /** Cameras must be within this angle of the direction of travel. */
    private const val AHEAD_DEG = 35f

    /** A camera tagged for another direction (e.g. the opposite carriageway) is skipped. */
    private const val SAME_WAY_DEG = 60f

    fun ahead(lat: Double, lon: Double, headingDeg: Float?, speedMps: Float, cams: List<SpeedCamera>): CameraAhead? {
        if (speedMps < 5f) return null   // under ~18 km/h: parking, traffic lights
        var best: CameraAhead? = null
        for (c in cams) {
            val d = ApproxSpeed.distanceM(lat, lon, c.lat, c.lon)
            if (d > WARN_M) continue
            if (headingDeg == null) {
                if (d > NO_HEADING_M) continue
            } else {
                if (angleDiff(headingDeg, bearing(lat, lon, c.lat, c.lon)) > AHEAD_DEG) continue
                if (c.directionDeg != null && angleDiff(headingDeg, c.directionDeg) > SAME_WAY_DEG) continue
            }
            if (best == null || d < best.distanceM) best = CameraAhead(c, d)
        }
        return best
    }

    private val COMPASS = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")

    /**
     * OpenStreetMap's direction tag as compass degrees: "90" or "NE". Null when it covers
     * both ways ("90;270"), is relative to the road ("forward"), or is missing.
     */
    fun parseDirection(tag: String?): Float? {
        val t = tag?.trim()?.uppercase() ?: return null
        if (t.isEmpty() || ';' in t) return null
        t.toFloatOrNull()?.let { return if (it in 0f..360f) it % 360f else null }
        val i = COMPASS.indexOf(t)
        return if (i >= 0) i * 22.5f else null
    }

    /** Compass bearing (0–360°) from the first point to the second. */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    /** Smallest angle between two compass directions (0–180°). */
    fun angleDiff(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }
}

/** Beeps once when a camera first comes into range; the same camera stays quiet for 10 minutes. */
class CameraAlerter {
    private val warnedAt = mutableMapOf<Long, Long>()

    /** True when this update should beep. */
    fun update(ahead: CameraAhead?, now: Long): Boolean {
        warnedAt.entries.removeAll { now - it.value > 10 * 60_000L }
        val c = ahead?.camera ?: return false
        if (c.id in warnedAt) return false
        warnedAt[c.id] = now
        return true
    }
}
