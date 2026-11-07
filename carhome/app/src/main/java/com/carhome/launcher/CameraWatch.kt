package com.carhome.launcher

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Speed cameras from OpenStreetMap around the car (5 km), refreshed as you drive.
 * Not every camera is on the map, so it's a help, never a guarantee.
 */
class CameraWatch(private val core: Core) {

    private val _ahead = MutableStateFlow<CameraAhead?>(null)
    /** The camera the car is heading to, or null. */
    val ahead: StateFlow<CameraAhead?> = _ahead.asStateFlow()

    private val alerter = CameraAlerter()
    @Volatile private var cameras: List<SpeedCamera> = emptyList()
    private var centerLat = Double.NaN
    private var centerLon = Double.NaN
    private var lastAttemptAt = -1_000_000L
    private var job: Job? = null
    /** Camera the user closed on the home screen (until the car has passed it) */
    private var hiddenId: Long? = null

    /** Called by the drive engine once a second. */
    fun tick(lat: Double?, lon: Double?, headingDeg: Float?, speedMps: Float?) {
        val s = core.settings.value
        if (!s.speedCameras || lat == null || lon == null || speedMps == null) {
            if (_ahead.value != null) _ahead.value = null
            return
        }
        // A new area when we've moved 2 km from the last one (at most once a minute)
        val now = SystemClock.elapsedRealtime()
        val far = centerLat.isNaN() || ApproxSpeed.distanceM(lat, lon, centerLat, centerLon) > 2_000.0
        if (far && job?.isActive != true && now - lastAttemptAt > 60_000L) {
            lastAttemptAt = now
            job = core.scope.launch {
                val list = withContext(Dispatchers.IO) { runCatching { download(lat, lon) }.getOrNull() }
                if (list != null) { cameras = list; centerLat = lat; centerLon = lon }
            }
        }
        val a = SpeedCameras.ahead(lat, lon, headingDeg, speedMps, cameras)
        if (a?.camera?.id != hiddenId) hiddenId = null
        val shown = a?.takeIf { it.camera.id != hiddenId }
        if (alerter.update(shown, now)) {
            if (s.cameraSound) core.voice.beep()
            if (s.voice && s.voiceSpeed) core.voice.say(R.string.voice_camera)
        }
        if (shown != _ahead.value) _ahead.value = shown
    }

    /** Closes the banner for the camera ahead (it comes back for the next one). */
    fun hide() {
        hiddenId = _ahead.value?.camera?.id
        _ahead.value = null
    }

    private fun download(exactLat: Double, exactLon: Double): List<SpeedCamera>? {
        // A position rounded to ~100 m, for a 5 km circle: the map server never sees the exact track
        val lat = Math.round(exactLat * 1000.0) / 1000.0
        val lon = Math.round(exactLon * 1000.0) / 1000.0
        val q = "[out:json][timeout:15];node(around:5000,$lat,$lon)[\"highway\"=\"speed_camera\"];out;"
        val conn = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "CarHome (Android launcher)")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(("data=" + URLEncoder.encode(q, "UTF-8")).toByteArray()) }
            if (conn.responseCode != 200) return null
            val els = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).optJSONArray("elements") ?: return emptyList()
            return (0 until els.length()).mapNotNull { i ->
                val e = els.optJSONObject(i) ?: return@mapNotNull null
                val tags = e.optJSONObject("tags")
                val max = tags?.optString("maxspeed")?.let { Regex("""^\d+""").find(it)?.value?.toIntOrNull() }
                SpeedCamera(e.optLong("id"), e.optDouble("lat"), e.optDouble("lon"), max)
                    .takeIf { it.lat.isFinite() && it.lon.isFinite() }
            }
        } finally {
            conn.disconnect()
        }
    }
}
