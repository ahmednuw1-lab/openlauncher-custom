package com.carhome.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class GpsState { NO_PERMISSION, NO_HARDWARE, SEARCHING, OK }

/** Converts metres/second to the user's unit. */
fun speedInUnit(mps: Float, useKmh: Boolean): Float = if (useKmh) mps * 3.6f else mps * 2.23694f

fun kmhToUnit(kmh: Int, useKmh: Boolean): Int = if (useKmh) kmh else (kmh / 1.609344).roundToInt()

/**
 * The limit the alert uses right now, in the user's unit. [limit] null = no alert.
 * [roadLimit] is the road's posted limit when the map knows it.
 */
data class LimitInfo(val limit: Int?, val roadLimit: Int?, val fromRoad: Boolean)

/**
 * Everything that happens while driving: GPS, speed, the alert, trip and
 * odometer, the break timer, road limits and speed-sensitive volume.
 * Shared by the home screen and the background service; GPS runs while either
 * one is attached.
 */
class DriveEngine(private val core: Core) {

    private val app = core.app
    private val voice = core.voice
    private val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val roads = RoadLimits()

    private val _speedMps = MutableStateFlow<Float?>(null)
    /** Current speed in m/s, or null when there is no recent GPS fix. */
    val speedMps: StateFlow<Float?> = _speedMps.asStateFlow()

    private val _gpsState = MutableStateFlow(GpsState.SEARCHING)
    val gpsState: StateFlow<GpsState> = _gpsState.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    /** Best known position (GPS or network) for weather, prayer times and the sky. */
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _overspeed = MutableStateFlow(false)
    val overspeed: StateFlow<Boolean> = _overspeed.asStateFlow()

    private val _limit = MutableStateFlow(LimitInfo(core.settings.value.speedLimit, null, false))
    val limit: StateFlow<LimitInfo> = _limit.asStateFlow()

    private val _trip = MutableStateFlow(core.prefs.loadTrip() ?: TripStats())
    val trip: StateFlow<TripStats> = _trip.asStateFlow()

    private val _lastTrip = MutableStateFlow(core.prefs.loadTrip("last_trip"))
    val lastTrip: StateFlow<TripStats?> = _lastTrip.asStateFlow()

    private val _odometer = MutableStateFlow(core.prefs.odometerKm)
    val odometerKm: StateFlow<Double> = _odometer.asStateFlow()

    private val _oilAt = MutableStateFlow(core.prefs.oilChangedAtKm)
    /** Odometer reading at the last oil change. */
    val oilChangedAtKm: StateFlow<Double> = _oilAt.asStateFlow()

    private val _breakDue = MutableStateFlow(false)
    val breakDue: StateFlow<Boolean> = _breakDue.asStateFlow()

    private val _moving = MutableStateFlow(false)
    /** True above ~8 km/h (drops back below 5 km/h). */
    val moving: StateFlow<Boolean> = _moving.asStateFlow()

    private val owners = mutableSetOf<String>()
    private var gpsOn = false
    private var netOn = false
    private var lastFix: Location? = null
    private var lastFixAt = 0L
    private var prevSample: Pair<Float, Long>? = null
    private var lastHarshAt = 0L
    private var lastTripSave = 0L

    private var roadKmh: Int? = null
    private var roadAt = 0L
    private var roadJob: Job? = null

    private var testUntil = 0L
    private var episodeBeeped = false
    private var spokeEpisode = false
    private var lastBeepAt = 0L

    private var drivingMs = 0L
    private var stillMs = 0L
    private var breakSnoozeUntil = 0L

    private var volBase = -1
    private var volApplied = 0
    private var volLastSet = -1

    // ── Who needs GPS ───────────────────────────────────────────────────────

    fun attach(owner: String) {
        owners += owner
        start()
    }

    fun detach(owner: String) {
        owners -= owner
        if (owners.isEmpty()) stopListening()
    }

    fun hasFine(): Boolean = app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    fun hasAnyLocation(): Boolean = hasFine() ||
        app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** (Re)starts listening; call again after the user grants location. */
    @SuppressLint("MissingPermission")
    fun start() {
        if (owners.isEmpty()) return
        if (!netOn && hasAnyLocation()) {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            if (_location.value == null) {
                _location.value = providers
                    .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                    .maxByOrNull { it.time }
            }
            val net = if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false))
                LocationManager.NETWORK_PROVIDER else LocationManager.PASSIVE_PROVIDER
            netOn = runCatching {
                lm.requestLocationUpdates(net, 10 * 60_000L, 500f, netListener, Looper.getMainLooper())
            }.isSuccess
        }
        if (gpsOn) return
        if (!hasFine()) { _gpsState.value = GpsState.NO_PERMISSION; return }
        val hasGps = runCatching { lm.allProviders.contains(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        if (!hasGps) { _gpsState.value = GpsState.NO_HARDWARE; return }
        gpsOn = runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, gpsListener, Looper.getMainLooper())
        }.isSuccess
        if (gpsOn && _gpsState.value != GpsState.OK) _gpsState.value = GpsState.SEARCHING
    }

    private fun stopListening() {
        if (gpsOn) runCatching { lm.removeUpdates(gpsListener) }
        if (netOn) runCatching { lm.removeUpdates(netListener) }
        gpsOn = false
        netOn = false
        lastFix = null
        prevSample = null
        _speedMps.value = null
        _moving.value = false
        saveTrip()
        restoreVolume()
    }

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onFix(location)
        // Explicit overrides: these interface defaults only exist on Android 11+
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            _speedMps.value = null
            _gpsState.value = GpsState.SEARCHING
        }
    }

    private val netListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (lastFix == null) _location.value = location
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun onFix(loc: Location) {
        val now = SystemClock.elapsedRealtime()
        val prev = lastFix
        val raw = when {
            loc.hasSpeed() -> loc.speed
            // Some GPS chips report no speed: derive it from consecutive fixes
            prev != null && loc.time > prev.time -> prev.distanceTo(loc) / ((loc.time - prev.time) / 1000f)
            else -> 0f
        }.coerceAtLeast(0f)

        // Distance: only from accurate fixes while really moving
        if (prev != null) {
            val dt = (loc.time - prev.time) / 1000.0
            val accurate = (!loc.hasAccuracy() || loc.accuracy <= 40f)
            if (accurate && dt > 0 && dt < 30) {
                val d = prev.distanceTo(loc).toDouble()
                if (raw >= 0.8f && d / dt < 85.0) addDistance(d)
            }
        }

        // Sudden braking or acceleration (at most one per 4 s, only when moving)
        prevSample?.let { (v0, t0) ->
            val dt = (now - t0) / 1000f
            if (dt in 0.5f..3f && maxOf(raw, v0) > 3f) {
                val a = (raw - v0) / dt
                if ((a < -3.4f || a > 3.0f) && now - lastHarshAt > 4_000L) {
                    lastHarshAt = now
                    _trip.value = _trip.value.copy(harshEvents = _trip.value.harshEvents + 1)
                }
            }
        }
        prevSample = raw to now
        lastFix = loc
        lastFixAt = now

        // Light smoothing, and treat GPS jitter under ~2 km/h as standing still
        val smoothed = _speedMps.value?.let { it * 0.35f + raw * 0.65f } ?: raw
        val v = if (smoothed < 0.55f) 0f else smoothed
        _speedMps.value = v
        if (v > _trip.value.maxMps) _trip.value = _trip.value.copy(maxMps = v)
        _gpsState.value = GpsState.OK
        _location.value = loc
    }

    private fun addDistance(m: Double) {
        _trip.value = _trip.value.copy(distanceM = _trip.value.distanceM + m)
        _odometer.value = _odometer.value + m / 1000.0
    }

    // ── Every second ────────────────────────────────────────────────────────

    private fun tick() {
        val s = core.settings.value
        val now = SystemClock.elapsedRealtime()

        // A fix older than 6 s means GPS lost — show "waiting" instead of a stale number
        if (gpsOn && lastFixAt > 0 && now - lastFixAt > 6_000L) {
            if (_speedMps.value != null) _speedMps.value = null
            if (_gpsState.value == GpsState.OK) _gpsState.value = GpsState.SEARCHING
            prevSample = null
        }
        val mps = _speedMps.value
        val kmh = (mps ?: 0f) * 3.6f
        _moving.value = if (_moving.value) kmh >= 5f else kmh >= 8f

        if (mps != null && mps >= 1.5f) _trip.value = _trip.value.copy(movingMs = _trip.value.movingMs + 1_000L)

        // Continuous driving for the break reminder; 10 min stopped counts as a break
        if (mps != null && mps >= 2.5f) {
            drivingMs += 1_000L
            stillMs = 0L
        } else {
            stillMs += 1_000L
            if (stillMs >= 10 * 60_000L) { drivingMs = 0L; breakSnoozeUntil = 0L }
        }
        val due = s.breakReminder && drivingMs >= s.breakAfterMin * 60_000L && drivingMs >= breakSnoozeUntil
        if (due && !_breakDue.value) {
            voice.chime()
            if (s.voiceReminders) voice.say(R.string.voice_break)
        }
        _breakDue.value = due

        updateLimit(s, mps)
        updateAlert(s, mps, now)
        updateVolume(s, kmh, mps != null)
        if (now - lastTripSave > 30_000L) saveTrip()
    }

    private fun updateLimit(s: LauncherSettings, mps: Float?) {
        val loc = lastFix
        if (!s.roadLimit) {
            roadKmh = null
        } else if (loc != null && mps != null && SystemClock.elapsedRealtime() - roadAt > 3_000L &&
            roadJob?.isActive != true
        ) {
            roadAt = SystemClock.elapsedRealtime()
            val bearing = if (loc.hasBearing() && mps > 3f) loc.bearing else null
            roadJob = core.scope.launch {
                roadKmh = runCatching { roads.match(loc.latitude, loc.longitude, bearing) }.getOrNull()?.limitKmh
            }
        }
        val mine = s.speedLimit
        val road = roadKmh?.let { kmhToUnit(it, s.useKmh) }
        val info = when {
            !s.roadLimit -> LimitInfo(mine, null, false)
            road != null -> LimitInfo(if (s.roadCap) minOf(road, mine) else road, road, true)
            s.roadNoData == NoRoadData.USE_MINE -> LimitInfo(mine, null, false)
            else -> LimitInfo(null, null, false)
        }
        if (info != _limit.value) _limit.value = info
    }

    private fun updateAlert(s: LauncherSettings, mps: Float?, now: Long) {
        val testing = now < testUntil
        val limit = _limit.value.limit
        if (!s.speedAlert || mps == null || limit == null) {
            _overspeed.value = testing
            episodeBeeped = false
            spokeEpisode = false
            return
        }
        val speed = speedInUnit(mps, s.useKmh)
        val trigger = limit + s.speedMargin
        when {
            speed > trigger -> {
                _overspeed.value = true
                when (s.alertMode) {
                    AlertMode.SCREEN -> Unit
                    AlertMode.BEEP_ONCE -> if (!episodeBeeped) { voice.beep(); episodeBeeped = true }
                    AlertMode.BEEP_REPEAT -> if (now - lastBeepAt >= 10_000L) { voice.beep(); lastBeepAt = now }
                }
                if (!spokeEpisode && s.voiceSpeed) {
                    voice.say(R.string.voice_over_limit, limit)
                    spokeEpisode = true
                }
            }
            // Small hysteresis so hovering right at the limit doesn't flicker
            speed < trigger - 3 -> {
                _overspeed.value = testing
                episodeBeeped = false
                spokeEpisode = false
                lastBeepAt = 0L
            }
            else -> if (testing) _overspeed.value = true
        }
    }

    /** Preview from Settings: flashes the alert and plays the chosen sound once. */
    fun testAlert() {
        val s = core.settings.value
        testUntil = SystemClock.elapsedRealtime() + 2_500L
        _overspeed.value = true
        if (s.alertMode != AlertMode.SCREEN) voice.beep()
        if (s.voiceSpeed) voice.say(R.string.voice_over_limit, s.speedLimit)
    }

    // ── Speed-sensitive volume ──────────────────────────────────────────────

    private fun boostFor(kmh: Float, mode: VolumeBoost): Int {
        val steps = when (mode) {
            VolumeBoost.OFF -> return 0
            VolumeBoost.LOW -> intArrayOf(60, 100)
            VolumeBoost.MEDIUM -> intArrayOf(50, 80, 110)
            VolumeBoost.HIGH -> intArrayOf(40, 70, 100, 130)
        }
        return steps.count { kmh >= it }
    }

    private fun updateVolume(s: LauncherSettings, kmh: Float, hasFix: Boolean) {
        if (s.speedVolume == VolumeBoost.OFF) { restoreVolume(); return }
        if (!hasFix) return
        runCatching {
            val stream = AudioManager.STREAM_MUSIC
            val cur = am.getStreamVolume(stream)
            val max = am.getStreamMaxVolume(stream)
            if (volBase < 0) {
                volBase = cur; volApplied = 0; volLastSet = cur
            } else if (cur != volLastSet) {
                // The user changed the volume: that becomes their new level
                volBase = (cur - volApplied).coerceAtLeast(0)
                volLastSet = cur
            }
            var want = boostFor(kmh, s.speedVolume)
            if (want < volApplied && boostFor(kmh + 8f, s.speedVolume) >= volApplied) want = volApplied
            val target = (volBase + want).coerceIn(0, max)
            if (target != cur) {
                am.setStreamVolume(stream, target, 0)
                volLastSet = target
            }
            volApplied = target - volBase
        }
    }

    private fun restoreVolume() {
        if (volApplied > 0 && volLastSet >= 0) {
            runCatching {
                val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (cur == volLastSet) am.setStreamVolume(AudioManager.STREAM_MUSIC, (cur - volApplied).coerceAtLeast(0), 0)
            }
        }
        volBase = -1
        volApplied = 0
        volLastSet = -1
    }

    // ── Trip, oil and breaks ────────────────────────────────────────────────

    fun onCarStart() {
        if (core.settings.value.tripReset == TripReset.EACH_START) newTrip()
        drivingMs = 0L
        breakSnoozeUntil = 0L
    }

    /** Ends the current trip (kept as "last trip" if it was a real drive) and starts a new one. */
    fun newTrip() {
        val t = _trip.value
        if (t.distanceM >= 200.0) {
            _lastTrip.value = t
            core.prefs.saveTrip(t, "last_trip")
            if (core.settings.value.driveScore) t.score?.let { core.prefs.addScore(it) }
        }
        _trip.value = TripStats()
        saveTrip()
    }

    fun snoozeBreak() {
        breakSnoozeUntil = drivingMs + 30 * 60_000L
        _breakDue.value = false
    }

    fun oilChanged() {
        _oilAt.value = _odometer.value
        core.prefs.oilChangedAtKm = _oilAt.value
    }

    /** Adjusts "km since last oil change" by [deltaKm] (for a change done before CarHome). */
    fun adjustOil(deltaKm: Double) {
        _oilAt.value = (_oilAt.value - deltaKm).coerceAtMost(_odometer.value)
        core.prefs.oilChangedAtKm = _oilAt.value
    }

    private fun saveTrip() {
        lastTripSave = SystemClock.elapsedRealtime()
        core.prefs.saveTrip(_trip.value)
        core.prefs.odometerKm = _odometer.value
    }

    init {
        core.scope.launch {
            while (true) {
                delay(1_000L)
                runCatching { tick() }
            }
        }
    }
}
