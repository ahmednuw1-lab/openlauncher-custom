package com.carhome.launcher

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Direction of the Kaaba, and straight-line distances, for the home screen. */
object Qibla {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206

    /** Degrees clockwise from true north (0–360) from the given point to the Kaaba. */
    fun bearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        if (x == 0.0 && y == 0.0) return 0.0
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Where the Qibla is relative to the car's heading: 0 = straight ahead, 90 = to the right. */
    fun relative(qibla: Double, heading: Double): Double = ((qibla - heading) % 360.0 + 360.0) % 360.0
}

object Geo {
    /** Great-circle distance in metres. */
    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }
}
