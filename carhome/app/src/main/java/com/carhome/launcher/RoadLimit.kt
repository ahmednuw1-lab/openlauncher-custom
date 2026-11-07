package com.carhome.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Result of matching the car to a road. */
data class RoadMatch(
    /** Posted limit in km/h, or null when the map has no limit for this road. */
    val limitKmh: Int?
)

/**
 * Posted speed limits from OpenStreetMap (free, no account) via the Overpass API.
 * Downloads every drivable road within 500 m once, keeps it, and matches the car
 * to the nearest road locally — so a weak signal doesn't stop it working.
 */
class RoadLimits {

    private class Way(
        val maxKmh: Int?,
        val forwardKmh: Int?,
        val backwardKmh: Int?,
        val pts: DoubleArray        // lat0, lon0, lat1, lon1, …
    )

    @Volatile private var ways: List<Way> = emptyList()
    @Volatile private var centerLat = Double.NaN
    @Volatile private var centerLon = Double.NaN
    @Volatile private var lastAttemptAt = 0L
    @Volatile private var fetching = false

    /** Null when the car is too far from any known road (or nothing downloaded yet). */
    suspend fun match(lat: Double, lon: Double, bearing: Float?): RoadMatch? {
        val needsArea = centerLat.isNaN() || metres(lat, lon, centerLat, centerLon) > 300.0
        val now = System.currentTimeMillis()
        if (needsArea && !fetching && now - lastAttemptAt > 20_000L) {
            fetching = true
            lastAttemptAt = now
            try {
                download(lat, lon)?.let { list ->
                    ways = list
                    centerLat = lat
                    centerLon = lon
                }
            } finally {
                fetching = false
            }
        }
        return nearest(lat, lon, bearing)
    }

    private suspend fun download(lat: Double, lon: Double): List<Way>? = withContext(Dispatchers.IO) {
        runCatching {
            val q = "[out:json][timeout:15];" +
                "way(around:500,$lat,$lon)[\"highway\"~\"^(motorway|trunk|primary|secondary|tertiary|" +
                "unclassified|residential|living_street|service|road|motorway_link|trunk_link|" +
                "primary_link|secondary_link|tertiary_link)$\"];out geom qt;"
            val conn = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10_000
                conn.readTimeout = 20_000
                conn.setRequestProperty("User-Agent", "CarHome/1.2 (Android launcher)")
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.outputStream.use { it.write(("data=" + URLEncoder.encode(q, "UTF-8")).toByteArray()) }
                if (conn.responseCode != 200) return@runCatching null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parse(body)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun parse(body: String): List<Way> {
        val els = JSONObject(body).optJSONArray("elements") ?: return emptyList()
        val out = ArrayList<Way>(els.length())
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            val geom = e.optJSONArray("geometry") ?: continue
            if (geom.length() < 2) continue
            val pts = DoubleArray(geom.length() * 2)
            for (j in 0 until geom.length()) {
                val p = geom.getJSONObject(j)
                pts[j * 2] = p.getDouble("lat")
                pts[j * 2 + 1] = p.getDouble("lon")
            }
            val tags = e.optJSONObject("tags")
            out += Way(
                maxKmh = parseSpeed(tags?.optString("maxspeed")),
                forwardKmh = parseSpeed(tags?.optString("maxspeed:forward")),
                backwardKmh = parseSpeed(tags?.optString("maxspeed:backward")),
                pts = pts
            )
        }
        return out
    }

    private fun nearest(lat: Double, lon: Double, bearing: Float?): RoadMatch? {
        var best: Way? = null
        var bestScore = Double.MAX_VALUE
        var bestForward = true
        val mPerLat = 111_320.0
        val mPerLon = 111_320.0 * cos(lat * PI / 180.0)
        for (w in ways) {
            val p = w.pts
            var k = 0
            while (k + 3 < p.size) {
                // Segment in a local metre grid centred on the car
                val ax = (p[k + 1] - lon) * mPerLon
                val ay = (p[k] - lat) * mPerLat
                val bx = (p[k + 3] - lon) * mPerLon
                val by = (p[k + 2] - lat) * mPerLat
                val dx = bx - ax
                val dy = by - ay
                val len2 = dx * dx + dy * dy
                val t = if (len2 > 0) (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0) else 0.0
                val cx = ax + t * dx
                val cy = ay + t * dy
                val dist = sqrt(cx * cx + cy * cy)
                var score = dist
                var forward = true
                if (bearing != null && len2 > 1.0) {
                    val seg = ((atan2(dx, dy) * 180.0 / PI) + 360.0) % 360.0
                    val diff = angleDiff(bearing.toDouble(), seg)
                    forward = diff <= 90.0
                    val aligned = if (forward) diff else 180.0 - diff
                    if (aligned > 35.0) score += 25.0   // crossing road, not ours
                }
                if (score < bestScore) {
                    bestScore = score
                    best = w
                    bestForward = forward
                }
                k += 2
            }
        }
        val w = best ?: return null
        if (bestScore > 25.0) return null
        val directional = if (bestForward) w.forwardKmh else w.backwardKmh
        return RoadMatch(directional ?: w.maxKmh)
    }

    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return if (d > 180.0) 360.0 - d else d
    }

    private fun metres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val x = (lon2 - lon1) * 111_320.0 * cos((lat1 + lat2) / 2 * PI / 180.0)
        val y = (lat2 - lat1) * 111_320.0
        return sqrt(x * x + y * y)
    }

    companion object {
        /** "120" → 120, "60 mph" → 97; anything else (none, signals, zones) → null. */
        fun parseSpeed(raw: String?): Int? {
            val s = raw?.trim()?.lowercase().orEmpty()
            if (s.isEmpty()) return null
            val num = s.takeWhile { it.isDigit() }.toIntOrNull() ?: return null
            if (num <= 0) return null
            return if (s.contains("mph")) (num * 1.609344).roundToInt() else num
        }
    }
}
