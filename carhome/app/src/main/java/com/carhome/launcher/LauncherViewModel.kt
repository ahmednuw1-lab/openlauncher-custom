package com.carhome.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap
)

enum class Page { HOME, APPS, SETTINGS }

/** Which page is showing; [pickTile] >= 0 means the app list is choosing an app for that tile. */
data class Nav(val page: Page = Page.HOME, val pickTile: Int = -1)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    private val _settings = MutableStateFlow(prefs.loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _tiles = MutableStateFlow(prefs.loadTiles(_settings.value.tileCount))
    val tiles: StateFlow<List<String>> = _tiles.asStateFlow()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _nav = MutableStateFlow(Nav())
    val nav: StateFlow<Nav> = _nav.asStateFlow()

    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    val nowPlaying: StateFlow<NowPlaying?> = MediaService.nowPlaying

    // ── Navigation ───────────────────────────────────────────────────────────
    fun goHome() { _nav.value = Nav() }
    fun openApps() { _nav.value = Nav(Page.APPS) }
    fun openSettings() { _nav.value = Nav(Page.SETTINGS) }
    fun pickAppForTile(index: Int) { _nav.value = Nav(Page.APPS, pickTile = index) }

    // ── Tiles ────────────────────────────────────────────────────────────────
    fun setTile(index: Int, pkg: String) {
        val list = _tiles.value.toMutableList()
        if (index !in list.indices) return
        list[index] = pkg
        _tiles.value = list
        prefs.saveTiles(list)
    }

    /** Puts [pkg] in the first empty tile; returns false when every tile is used. */
    fun addToFirstEmptyTile(pkg: String): Boolean {
        val i = _tiles.value.indexOfFirst { it.isEmpty() }
        if (i < 0) return false
        setTile(i, pkg)
        return true
    }

    // ── Settings ─────────────────────────────────────────────────────────────
    fun updateSettings(block: (LauncherSettings) -> LauncherSettings) {
        val old = _settings.value
        val new = block(old)
        _settings.value = new
        prefs.saveSettings(new)
        if (new.tileCount != old.tileCount) {
            // Keep the user's choices; growing adds empty tiles, shrinking hides the tail
            val current = _tiles.value
            val resized = List(new.tileCount) { i -> current.getOrNull(i).orEmpty() }
            _tiles.value = resized
            prefs.saveTiles(resized)
        }
        if (new.showWeather && !old.showWeather) startLocation()
    }

    // ── Apps ─────────────────────────────────────────────────────────────────
    @Volatile private var loading = false
    @Volatile private var reloadPending = false

    fun loadApps() {
        if (loading) { reloadPending = true; return }
        loading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                do {
                    reloadPending = false
                    _apps.value = queryApps()
                } while (reloadPending)
            } catch (e: Exception) {
                android.util.Log.e("CarHome", "App list failed", e)
            } finally {
                loading = false
            }
        }
    }

    private fun queryApps(): List<AppEntry> {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance()
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == ctx.packageName) return@mapNotNull null
                runCatching {
                    AppEntry(
                        packageName = pkg,
                        label = ri.loadLabel(pm).toString(),
                        icon = ri.loadIcon(pm).toBitmap(144, 144).asImageBitmap()
                    )
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
            .toList()
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadApps()
            // A removed app leaves its tile empty instead of a dead button
            if (intent?.action == Intent.ACTION_PACKAGE_REMOVED &&
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false).not()
            ) {
                val pkg = intent.data?.schemeSpecificPart ?: return
                _tiles.value.forEachIndexed { i, p -> if (p == pkg) setTile(i, "") }
            }
        }
    }

    fun launchApp(pkg: String) {
        val ctx = getApplication<Application>()
        val ok = runCatching {
            val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return@runCatching false
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ctx.startActivity(i)
            true
        }.getOrDefault(false)
        if (!ok) Toast.makeText(ctx, R.string.cant_open, Toast.LENGTH_SHORT).show()
    }

    // ── Media controls ───────────────────────────────────────────────────────
    fun playPause() {
        val np = nowPlaying.value ?: return
        val t = np.controller.transportControls
        if (np.isPlaying) t.pause() else t.play()
    }
    fun next() { nowPlaying.value?.controller?.transportControls?.skipToNext() }
    fun previous() { nowPlaying.value?.controller?.transportControls?.skipToPrevious() }

    // ── Weather (location → Open-Meteo every 30 min, retry every 2 min) ──────
    private val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    @Volatile private var lastLocation: Location? = null
    private var locationStarted = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val first = lastLocation == null
            lastLocation = location
            if (first || _weather.value == null) refreshWeatherSoon()
        }
        // Explicit overrides: these interface defaults only exist on Android 11+,
        // and older boxes throw AbstractMethodError without them
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        getApplication<Application>(), Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startLocation() {
        if (locationStarted || !hasLocationPermission()) return
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
        runCatching {
            lastLocation = providers
                .mapNotNull { p -> runCatching { locationManager.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
        }
        val live = providers.firstOrNull { p ->
            runCatching { locationManager.isProviderEnabled(p) }.getOrDefault(false) && p != LocationManager.PASSIVE_PROVIDER
        } ?: LocationManager.PASSIVE_PROVIDER
        locationStarted = runCatching {
            locationManager.requestLocationUpdates(live, 10 * 60_000L, 1_000f, locationListener, Looper.getMainLooper())
        }.isSuccess
        refreshWeatherSoon()
    }

    private val weatherKick = MutableStateFlow(0)
    private fun refreshWeatherSoon() { weatherKick.value = weatherKick.value + 1 }

    private fun startWeatherLoop() {
        viewModelScope.launch {
            var lastKick = -1
            var nextAt = 0L
            while (true) {
                val now = System.currentTimeMillis()
                val kicked = weatherKick.value != lastKick
                val loc = lastLocation
                if (_settings.value.showWeather && loc != null && (kicked || now >= nextAt)) {
                    lastKick = weatherKick.value
                    val w = fetchWeather(loc.latitude, loc.longitude)
                    if (w != null) _weather.value = w
                    nextAt = System.currentTimeMillis() + if (w != null) 30 * 60_000L else 2 * 60_000L
                }
                delay(5_000)
            }
        }
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(packageReceiver) }
        runCatching { locationManager.removeUpdates(locationListener) }
        super.onCleared()
    }

    init {
        loadApps()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        runCatching {
            ContextCompat.registerReceiver(app, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }
        startLocation()
        startWeatherLoop()
    }
}
