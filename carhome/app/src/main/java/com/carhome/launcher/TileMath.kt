package com.carhome.launcher

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Map tile maths (Web Mercator, the scheme OpenStreetMap tiles use). Positions are in
 * "tile units" at a zoom level: tile (x, y) covers x..x+1 and y..y+1. Pure functions.
 */
object TileMath {
    const val MIN_ZOOM = 3
    const val MAX_ZOOM = 18
    /** Mercator can't show the poles */
    const val MAX_LAT = 85.05112878

    fun tilesAcross(z: Int): Int = 1 shl z

    fun x(lon: Double, z: Int): Double = (lon + 180.0) / 360.0 * tilesAcross(z)

    fun y(lat: Double, z: Int): Double {
        val r = Math.toRadians(lat.coerceIn(-MAX_LAT, MAX_LAT))
        return (1.0 - ln(tan(r) + 1.0 / cos(r)) / PI) / 2.0 * tilesAcross(z)
    }

    fun lon(x: Double, z: Int): Double = x / tilesAcross(z) * 360.0 - 180.0

    fun lat(y: Double, z: Int): Double = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * y / tilesAcross(z)))))

    /** A position in tile units at zoom [to], given one at zoom [from]. */
    fun rescale(v: Double, from: Int, to: Int): Double = v * tilesAcross(to) / tilesAcross(from)

    /** Column wrapped round the world (the map repeats east and west). */
    fun wrapX(x: Int, z: Int): Int { val n = tilesAcross(z); return ((x % n) + n) % n }

    /**
     * The tiles a view of [widthTiles] × [heightTiles] centred on ([cx], [cy]) needs:
     * each as (column as drawn, row). Rows off the top or bottom of the world are left out.
     */
    fun visible(cx: Double, cy: Double, widthTiles: Double, heightTiles: Double, z: Int): List<Pair<Int, Int>> {
        val x0 = floor(cx - widthTiles / 2).toInt()
        val x1 = floor(cx + widthTiles / 2).toInt()
        val y0 = floor(cy - heightTiles / 2).toInt().coerceAtLeast(0)
        val y1 = floor(cy + heightTiles / 2).toInt().coerceAtMost(tilesAcross(z) - 1)
        val out = ArrayList<Pair<Int, Int>>()
        for (ty in y0..y1) for (tx in x0..x1) out += tx to ty
        return out
    }

    /** The zoom that suits a speed: close in town, further out on the motorway. */
    fun zoomForSpeed(kmh: Float?): Int = when {
        kmh == null || kmh < 20f -> 17
        kmh < 60f -> 16
        kmh < 100f -> 15
        else -> 14
    }

    /**
     * Automatic zoom with a little patience: it only changes when the speed has clearly
     * left the band of the [current] zoom, so it doesn't flick at a band's edge.
     */
    fun autoZoom(kmh: Float?, current: Int): Int {
        val target = zoomForSpeed(kmh)
        if (target == current || kmh == null) return target
        val k = kmh
        val margin = 8f
        // Same answer with the speed nudged towards the current band: stay put
        val nudged = if (target < current) k - margin else k + margin
        return if (zoomForSpeed(nudged.coerceAtLeast(0f)) == current) current else target
    }

    fun clampZoom(z: Int): Int = z.coerceIn(MIN_ZOOM, MAX_ZOOM)
}

/** How the map is coloured. NIGHT: dark after sunset, light in the day. */
enum class MapLook { DARK, LIGHT, NIGHT }

/** Colour sums for drawing the light OpenStreetMap tiles dark. */
object MapColours {
    /**
     * A 4×5 colour matrix (Android's layout, offsets in 0..255) that turns the map's
     * light colours dark and keeps its hues: invert, turn the hue half way round,
     * then dim a little and lean towards CarHome's navy.
     */
    fun darkMatrix(): FloatArray {
        val invert = floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
        // CSS hue-rotate(180deg)
        val hue = floatArrayOf(
            -0.574f, 1.430f, 0.144f, 0f, 0f,
            0.426f, 0.430f, 0.144f, 0f, 0f,
            0.426f, 1.430f, -0.856f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val tone = floatArrayOf(
            0.80f, 0f, 0f, 0f, 4f,
            0f, 0.82f, 0f, 0f, 8f,
            0f, 0f, 0.90f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
        return times(tone, times(hue, invert))
    }

    /** [a] after [b]: the matrix that applies [b] first, then [a]. */
    fun times(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) sum += a[r * 5 + k] * b[k * 5 + c]
                out[r * 5 + c] = sum
            }
            var off = a[r * 5 + 4]
            for (k in 0 until 4) off += a[r * 5 + k] * b[k * 5 + 4]
            out[r * 5 + 4] = off
        }
        return out
    }

    /** One opaque colour through [m]; channels 0..255, clamped. */
    fun apply(m: FloatArray, r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        fun ch(row: Int) = (m[row * 5] * r + m[row * 5 + 1] * g + m[row * 5 + 2] * b + m[row * 5 + 3] * 255f + m[row * 5 + 4])
            .toInt().coerceIn(0, 255)
        return Triple(ch(0), ch(1), ch(2))
    }
}
