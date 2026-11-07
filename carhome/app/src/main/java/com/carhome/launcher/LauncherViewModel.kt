package com.carhome.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Collator
import java.util.Calendar
import java.util.UUID

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap
)

enum class Page { HOME, APPS, SETTINGS }
enum class SettingsPage { HOME_LAYOUT, SPEED, PRAYER, TRIP, STARTUP, DISPLAY, SMART, PERMISSIONS }
enum class PickFor { TILE, AUTO_APP }

/** Which page is showing; with [pickFor] the app list is choosing an app. */
data class Nav(val page: Page = Page.HOME, val pickFor: PickFor? = null, val pickTile: Int = -1)

enum class NoticeKind { CAMERA, BREAK, OIL, DUST, FOG, PRAYER, UPDATE, REMINDER }
data class Notice(
    val kind: NoticeKind, val hours: Int = 0, val prayer: Prayer? = null, val at: Long = 0L,
    /** For UPDATE: the new version, and download progress (-1 when not downloading) */
    val version: String? = null, val progress: Int = -1,
    /** For REMINDER: which one, and how close it is */
    val due: DueStatus? = null,
    /** For CAMERA: the camera ahead */
    val camera: CameraAhead? = null
)

enum class Nearby { MOSQUE, FUEL, COFFEE }

data class PrayerInfo(val day: PrayerDay, val next: NextPrayer)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    val core = CarHomeApp.core(app)
    val engine = core.engine
    private val prefs = core.prefs
    private val ctx: Context get() = getApplication()

    val settings: StateFlow<LauncherSettings> = core.settings

    /** Both pages one after the other: page 1 is the first [LauncherSettings.tileCount] tiles. */
    private fun loadAllTiles(s: LauncherSettings): List<String> =
        prefs.loadTiles(s.tileCount) + if (s.secondPage) prefs.loadTiles(s.tileCount, "tiles2") else emptyList()

    private val _tiles = MutableStateFlow(loadAllTiles(settings.value))
    val tiles: StateFlow<List<String>> = _tiles.asStateFlow()

    private val _places = MutableStateFlow(prefs.loadPlaces())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _nav = MutableStateFlow(Nav())
    val nav: StateFlow<Nav> = _nav.asStateFlow()

    private val _settingsPage = MutableStateFlow(SettingsPage.HOME_LAYOUT)
    val settingsPage: StateFlow<SettingsPage> = _settingsPage.asStateFlow()

    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private val _dustInHours = MutableStateFlow<Int?>(null)
    val dustInHours: StateFlow<Int?> = _dustInHours.asStateFlow()

    private val _prayer = MutableStateFlow<PrayerInfo?>(null)
    val prayer: StateFlow<PrayerInfo?> = _prayer.asStateFlow()

    private val _sun = MutableStateFlow<Pair<Long, Long>?>(null)
    /** Today's sunrise and sunset for the living sky and night mode. */
    val sun: StateFlow<Pair<Long, Long>?> = _sun.asStateFlow()

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices: StateFlow<List<Notice>> = _notices.asStateFlow()

    private val _smartPick = MutableStateFlow<String?>(null)
    val smartPick: StateFlow<String?> = _smartPick.asStateFlow()

    private val _parkedOnly = MutableStateFlow(prefs.parkedOnlyApps)
    val parkedOnlyApps: StateFlow<Set<String>> = _parkedOnly.asStateFlow()

    private val _songAccent = MutableStateFlow<Long?>(null)
    val songAccent: StateFlow<Long?> = _songAccent.asStateFlow()

    private val _photo = MutableStateFlow<ImageBitmap?>(null)
    val photo: StateFlow<ImageBitmap?> = _photo.asStateFlow()

    private val _night = MutableStateFlow(false)
    val night: StateFlow<Boolean> = _night.asStateFlow()

    private val _parked = MutableStateFlow(false)
    val parkedScreen: StateFlow<Boolean> = _parked.asStateFlow()

    private val _welcome = MutableStateFlow(false)
    val welcome: StateFlow<Boolean> = _welcome.asStateFlow()

    private val _driving = MutableStateFlow(false)
    val drivingMode: StateFlow<Boolean> = _driving.asStateFlow()

    val nowPlaying: StateFlow<NowPlaying?> = MediaService.nowPlaying
    val voiceStatus: StateFlow<VoiceStatus> = core.voice.status

    // ── Navigation ───────────────────────────────────────────────────────────
    fun goHome() { _nav.value = Nav() }
    fun openApps() { _nav.value = Nav(Page.APPS) }
    fun openSettings() { _nav.value = Nav(Page.SETTINGS) }
    fun openSettingsPage(p: SettingsPage) { _settingsPage.value = p }
    fun pickForTile(index: Int) { _nav.value = Nav(Page.APPS, PickFor.TILE, index) }
    fun pickAutoApp() { _nav.value = Nav(Page.APPS, PickFor.AUTO_APP) }

    /** Result of the app list in picker mode. [value] is a package, "place:<id>" or "smart". */
    fun picked(value: String) {
        val n = _nav.value
        when (n.pickFor) {
            PickFor.TILE -> { setTile(n.pickTile, value); goHome() }
            PickFor.AUTO_APP -> { updateSettings { it.copy(autoApp = value) }; _nav.value = Nav(Page.SETTINGS) }
            null -> Unit
        }
    }

    // ── Tiles ────────────────────────────────────────────────────────────────
    fun setTile(index: Int, value: String) {
        val list = _tiles.value.toMutableList()
        if (index !in list.indices) return
        list[index] = value
        _tiles.value = list
        val n = settings.value.tileCount
        prefs.saveTiles(list.take(n))
        if (list.size > n) prefs.saveTiles(list.drop(n), "tiles2")
        refreshSmartPick()
    }

    /** Puts [value] in the first empty tile; returns false when every tile is used. */
    fun addToFirstEmptyTile(value: String): Boolean {
        val i = _tiles.value.indexOfFirst { it.isEmpty() }
        if (i < 0) return false
        setTile(i, value)
        return true
    }

    // ── Settings ─────────────────────────────────────────────────────────────
    fun updateSettings(block: (LauncherSettings) -> LauncherSettings) {
        val (old, new) = core.updateSettings(block)
        if (new.tileCount != old.tileCount || new.secondPage != old.secondPage) {
            // Shrinking only hides the tail (it stays saved); growing brings it back.
            // Turning the second page off hides it; on again brings its tiles back.
            _tiles.value = loadAllTiles(new)
        }
        if (new.bubble != old.bubble || new.backgroundTracking != old.backgroundTracking) DriveService.sync(ctx)
        if (new.obd != old.obd || new.obdAddress != old.obdAddress) core.obd.sync()
        if (new.voice && !old.voice) core.voice.prepare()
        if (new.dustWarning && !old.dustWarning) refreshWeatherSoon()
        if (new.songColours != old.songColours) updateSongAccent(nowPlaying.value)
        if (new.prayerMethod != old.prayerMethod || new.asrHanafi != old.asrHanafi) refreshPrayer()
        if (new.nightMode != old.nightMode || new.nightFrom != old.nightFrom || new.nightTo != old.nightTo ||
            new.nightSystem != old.nightSystem || new.nightDim != old.nightDim
        ) updateNight()
        if (new.smartTile != old.smartTile) refreshSmartPick()
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
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance()
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == ctx.packageName) return@mapNotNull null
                runCatching {
                    AppEntry(pkg, ri.loadLabel(pm).toString(), ri.loadIcon(pm).toBitmap(144, 144).asImageBitmap())
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
                !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            ) {
                val pkg = intent.data?.schemeSpecificPart ?: return
                _tiles.value.forEachIndexed { i, p -> if (p == pkg) setTile(i, "") }
                prefs.forgetTile(pkg)
            }
        }
    }

    private fun toast(id: Int) = Toast.makeText(ctx, core.localized().getString(id), Toast.LENGTH_SHORT).show()

    fun isParkedOnlyBlocked(pkg: String): Boolean =
        settings.value.parkedOnly && pkg in _parkedOnly.value && engine.moving.value

    fun launchApp(pkg: String, record: Boolean = true, ignoreParked: Boolean = false) {
        if (!ignoreParked && isParkedOnlyBlocked(pkg)) { toast(R.string.parked_only_blocked); return }
        val ok = runCatching {
            val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return@runCatching false
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ctx.startActivity(i)
            true
        }.getOrDefault(false)
        if (!ok) { toast(R.string.cant_open); return }
        if (record) recordLaunch(pkg)
    }

    /** Opens what a home tile holds. */
    fun openTile(index: Int, value: String) {
        when {
            value.isEmpty() -> pickForTile(index)
            value == TILE_SMART -> _smartPick.value?.let { launchApp(it) } ?: toast(R.string.smart_tile_learning)
            value.startsWith(TILE_PLACE) -> _places.value.firstOrNull { it.id == value.removePrefix(TILE_PLACE) }?.let { navigateTo(it) }
            else -> launchApp(value)
        }
    }

    fun toggleParkedOnly(pkg: String) {
        val set = _parkedOnly.value.toMutableSet()
        if (!set.add(pkg)) set.remove(pkg)
        _parkedOnly.value = set
        prefs.parkedOnlyApps = set
    }

    // ── Smart tile: the app usually opened at this time of day ──────────────
    private val history = prefs.loadLaunchHistory()

    private fun bucket(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) / 3

    private fun recordLaunch(pkg: String) {
        val counts = history.getOrPut(pkg) { IntArray(8) }
        counts[bucket()]++
        prefs.saveLaunchHistory(history)
        refreshSmartPick()
    }

    private fun refreshSmartPick() {
        if (!settings.value.smartTile) { _smartPick.value = null; return }
        val b = bucket()
        val onTiles = _tiles.value.toSet()
        val installed = _apps.value.map { it.packageName }.toSet()
        _smartPick.value = history.entries
            .filter { (pkg, c) -> c[b] > 0 && pkg !in onTiles && (installed.isEmpty() || pkg in installed) }
            .maxByOrNull { it.value[b] }?.key
    }

    // ── Places ───────────────────────────────────────────────────────────────
    /** Saves a place; with [useCurrent] it takes the car's position. Returns false if no position yet. */
    fun addPlace(name: String, useCurrent: Boolean, address: String): Place? {
        val loc = engine.location.value
        val p = if (useCurrent) {
            loc ?: return null
            Place(UUID.randomUUID().toString(), name.trim(), loc.latitude, loc.longitude, "")
        } else {
            if (address.isBlank()) return null
            Place(UUID.randomUUID().toString(), name.trim(), null, null, address.trim())
        }
        _places.value = _places.value + p
        prefs.savePlaces(_places.value)
        return p
    }

    fun removePlace(id: String) {
        _places.value = _places.value.filterNot { it.id == id }
        prefs.savePlaces(_places.value)
        _tiles.value.forEachIndexed { i, v -> if (v == TILE_PLACE + id) setTile(i, "") }
        prefs.forgetTile(TILE_PLACE + id)
    }

    fun navigateTo(p: Place) {
        val q = if (p.lat != null && p.lon != null) "${p.lat},${p.lon}" else Uri.encode(p.address)
        val ok = tryStart(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$q"))) ||
            tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(
                if (p.lat != null && p.lon != null) "geo:${p.lat},${p.lon}?q=${p.lat},${p.lon}(${Uri.encode(p.name)})"
                else "geo:0,0?q=${Uri.encode(p.address)}"
            )))
        if (!ok) toast(R.string.no_maps)
    }

    fun openNearby(kind: Nearby) {
        val q = core.localized().getString(
            when (kind) {
                Nearby.MOSQUE -> R.string.q_mosque
                Nearby.FUEL -> R.string.q_fuel
                Nearby.COFFEE -> R.string.q_coffee
            }
        )
        val loc = engine.location.value
        val geo = if (loc != null) "geo:${loc.latitude},${loc.longitude}?q=${Uri.encode(q)}" else "geo:0,0?q=${Uri.encode(q)}"
        if (!tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(geo)))) toast(R.string.no_maps)
    }

    private fun tryStart(i: Intent): Boolean =
        runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)

    // ── Media controls ───────────────────────────────────────────────────────
    fun playPause() {
        val np = nowPlaying.value
        if (np == null) { mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY); return }
        val t = np.controller.transportControls
        if (np.isPlaying) t.pause() else t.play()
    }
    fun next() { nowPlaying.value?.controller?.transportControls?.skipToNext() ?: mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) }
    fun previous() { nowPlaying.value?.controller?.transportControls?.skipToPrevious() ?: mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }

    private fun resumeMusic() {
        val np = nowPlaying.value
        if (np != null) np.controller.transportControls.play() else mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    /** Sends a media button to whichever app played last. */
    private fun mediaKey(code: Int) {
        runCatching {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }

    private var lastArtId = 0

    private fun updateSongAccent(np: NowPlaying?) {
        val art = np?.art
        if (!settings.value.songColours || art == null) { _songAccent.value = null; lastArtId = 0; return }
        val id = System.identityHashCode(art) * 31 + art.generationId
        if (id == lastArtId) return
        lastArtId = id
        viewModelScope.launch(Dispatchers.Default) {
            val c = runCatching {
                val p = Palette.from(art).maximumColorCount(16).generate()
                val sw = p.vibrantSwatch ?: p.lightVibrantSwatch ?: p.dominantSwatch
                sw?.rgb
            }.getOrNull()
            _songAccent.value = c?.let { brighten(it) }
        }
    }

    /** Makes a cover colour bright and saturated enough to use as the accent. */
    private fun brighten(rgb: Int): Long {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rgb, hsv)
        hsv[1] = hsv[1].coerceAtLeast(0.45f)
        hsv[2] = hsv[2].coerceIn(0.78f, 0.95f)
        return android.graphics.Color.HSVToColor(hsv).toLong() and 0xFFFFFFFFL
    }

    // ── Background photo ────────────────────────────────────────────────────
    private val photoFile get() = File(ctx.filesDir, "background.jpg")

    fun setPhoto(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val cr = ctx.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 1280 && bounds.outHeight / (sample * 2) >= 720) sample *= 2
                val bmp = cr.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: return@runCatching
                // Soft blur: shrink, then enlarge with filtering
                val small = Bitmap.createScaledBitmap(bmp, (bmp.width / 8).coerceAtLeast(1), (bmp.height / 8).coerceAtLeast(1), true)
                val soft = Bitmap.createScaledBitmap(small, bmp.width, bmp.height, true)
                photoFile.outputStream().use { soft.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                _photo.value = soft.asImageBitmap()
            }
        }
        updateSettings { it.copy(background = Background.PHOTO) }
    }

    private fun loadPhoto() {
        viewModelScope.launch(Dispatchers.IO) {
            val f = photoFile
            if (f.exists()) _photo.value = runCatching { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }.getOrNull()
        }
    }

    // ── Weather (every 30 min, retry every 2 min) ───────────────────────────
    private val weatherKick = MutableStateFlow(0)
    fun refreshWeatherSoon() { weatherKick.value = weatherKick.value + 1 }

    private fun startWeatherLoop() {
        viewModelScope.launch {
            var lastKick = -1
            var nextAt = 0L
            var hadLocation = false
            var failures = 0
            while (true) {
                val now = System.currentTimeMillis()
                val loc = engine.location.value
                val kicked = weatherKick.value != lastKick || (loc != null && !hadLocation)
                val s = settings.value
                val wanted = s.showWeather || CardType.WEATHER in s.cards || s.dustWarning ||
                    s.background == Background.SKY || s.welcome || s.parkedScreen
                if (wanted && loc != null && (kicked || now >= nextAt)) {
                    lastKick = weatherKick.value
                    hadLocation = true
                    val w = fetchWeather(loc.latitude, loc.longitude)
                    if (w != null) _weather.value = w
                    // A failed download keeps the current warning instead of clearing it
                    if (s.dustWarning) fetchDustInHours(loc.latitude, loc.longitude).onSuccess { _dustInHours.value = it }
                    // After a failed download retry soon, then back off: 20 s, 1 min, 2 min
                    failures = if (w != null) 0 else failures + 1
                    nextAt = System.currentTimeMillis() + when {
                        w != null -> 30 * 60_000L
                        failures == 1 -> 20_000L
                        failures == 2 -> 60_000L
                        else -> 2 * 60_000L
                    }
                }
                delay(5_000)
            }
        }
    }

    // ── Prayer, sun, notices, night, parked screen ──────────────────────────
    private var remindedFor = 0L
    private val dismissed = mutableSetOf<String>()
    private var spokenOil = false
    private var spokenDust = false
    @Volatile private var lastTouch = SystemClock.elapsedRealtime()

    fun refreshPrayer() {
        val loc = engine.location.value ?: return
        val s = settings.value
        val now = System.currentTimeMillis()
        val ramadan = Astro.isRamadan(now)
        val today = Astro.prayerDay(Calendar.getInstance(), loc.latitude, loc.longitude, s.prayerMethod, s.asrHanafi, ramadan)
        val next = Astro.nextPrayer(now, loc.latitude, loc.longitude, s.prayerMethod, s.asrHanafi, ramadan)
        _prayer.value = PrayerInfo(today, next)
        _sun.value = Astro.sunTimes(now, loc.latitude, loc.longitude)
    }

    fun dismissNotice(n: Notice) {
        when (n.kind) {
            NoticeKind.BREAK -> engine.snoozeBreak()
            NoticeKind.OIL -> dismissed.add("OIL")
            NoticeKind.DUST -> dismissed.add("DUST")
            NoticeKind.FOG -> dismissed.add("FOG")
            NoticeKind.PRAYER -> dismissed.add("PRAYER${n.at}")
            NoticeKind.UPDATE -> core.prefs.dismissedUpdate = n.version.orEmpty()
            NoticeKind.REMINDER -> n.due?.let { snoozeReminder(it.reminder.id) }
            NoticeKind.CAMERA -> core.cameras.hide()
        }
        updateNotices()
    }

    private fun updateNotices() {
        val s = settings.value
        val now = System.currentTimeMillis()
        val list = mutableListOf<Notice>()
        // A speed camera ahead comes first
        core.cameras.ahead.value?.let { list += Notice(NoticeKind.CAMERA, camera = it) }
        if (engine.breakDue.value) list += Notice(NoticeKind.BREAK)

        val next = _prayer.value?.next
        if (s.prayerReminder > 0 && next != null) {
            val left = next.at - now
            if (left in 0..(s.prayerReminder * 60_000L)) {
                if (remindedFor != next.at && prefs.remindedPrayerAt != next.at) {
                    remindedFor = next.at
                    prefs.remindedPrayerAt = next.at   // a restart inside the window doesn't chime again
                    if (s.prayerChime) core.voice.chime()
                    if (s.voicePrayer) {
                        // The real minutes left (the car may start 3 min before), in proper grammar
                        val mins = ((left + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                        val ctx = core.localized()
                        core.voice.say(ctx.resources.getQuantityString(
                            R.plurals.voice_prayer_min, mins, ctx.getString(prayerName(next.prayer)), mins))
                    }
                }
                if ("PRAYER${next.at}" !in dismissed) list += Notice(NoticeKind.PRAYER, prayer = next.prayer, at = next.at)
            }
        }

        if (s.oilReminder && engine.odometerKm.value - engine.oilChangedAtKm.value >= s.oilInterval && "OIL" !in dismissed) {
            list += Notice(NoticeKind.OIL)
            if (!spokenOil) { spokenOil = true; if (s.voiceReminders) core.voice.say(R.string.voice_oil) }
        }
        if (s.dustWarning) {
            _dustInHours.value?.let { h ->
                if ("DUST" !in dismissed) {
                    list += Notice(NoticeKind.DUST, hours = h)
                    if (!spokenDust) { spokenDust = true; if (s.voiceReminders) core.voice.say(R.string.voice_dust) }
                }
            }
            _weather.value?.fogInHours?.let { h -> if ("FOG" !in dismissed) list += Notice(NoticeKind.FOG, hours = h) }
        }
        // Car paperwork and maintenance: while parked, the most urgent first
        if (s.carReminders && !engine.moving.value) {
            Reminders.due(_reminders.value, now, engine.odometerKm.value, reminderSnooze).forEach { st ->
                list += Notice(NoticeKind.REMINDER, due = st)
            }
        }
        // A new CarHome version: only offered while parked (installing takes a few taps)
        val u = core.updater.state.value
        if (u.version != null && u.version != core.prefs.dismissedUpdate && !engine.moving.value) {
            when (u.stage) {
                UpdateStage.AVAILABLE, UpdateStage.NEED_PERMISSION, UpdateStage.FAILED ->
                    list += Notice(NoticeKind.UPDATE, version = u.version)
                UpdateStage.DOWNLOADING, UpdateStage.INSTALLING ->
                    list += Notice(NoticeKind.UPDATE, version = u.version, progress = u.progress)
                else -> Unit
            }
        }
        if (list != _notices.value) _notices.value = list
    }

    // ── Car reminders ─────────────────────────────────────────────────────────
    private val _reminders = MutableStateFlow(prefs.loadReminders())
    val reminders: StateFlow<List<CarReminder>> = _reminders.asStateFlow()
    private var reminderSnooze: Map<String, Long> = prefs.loadReminderSnooze()

    fun addReminder(r: CarReminder) {
        _reminders.value = _reminders.value + r
        prefs.saveReminders(_reminders.value)
        updateNotices()
    }

    fun removeReminder(id: String) {
        _reminders.value = _reminders.value.filterNot { it.id == id }
        prefs.saveReminders(_reminders.value)
        reminderSnooze = reminderSnooze - id
        prefs.saveReminderSnooze(reminderSnooze)
        updateNotices()
    }

    /** "Renewed": moves the reminder to its next period (see [Reminders.renewed]). */
    fun renewReminder(id: String) {
        val now = System.currentTimeMillis()
        _reminders.value = _reminders.value.map { if (it.id == id) Reminders.renewed(it, now, engine.odometerKm.value) else it }
        prefs.saveReminders(_reminders.value)
        reminderSnooze = reminderSnooze - id
        prefs.saveReminderSnooze(reminderSnooze)
        updateNotices()
    }

    /** Closed on the home screen: shown again in 3 days if still due. */
    private fun snoozeReminder(id: String) {
        reminderSnooze = reminderSnooze + (id to System.currentTimeMillis() + 3 * 86_400_000L)
        prefs.saveReminderSnooze(reminderSnooze)
    }

    // ── What's new ────────────────────────────────────────────────────────────
    private val _whatsNew = MutableStateFlow<List<String>?>(null)
    /** The list to show in the "What's new" card, or null when it's closed. */
    val whatsNew: StateFlow<List<String>?> = _whatsNew.asStateFlow()

    private val pkgInfo get() = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
    val versionName: String get() = pkgInfo?.versionName ?: ""
    private val versionCode: Int get() = pkgInfo?.let {
        if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode.toInt() else @Suppress("DEPRECATION") it.versionCode
    } ?: 0

    private fun whatsNewItems(): List<String> = WhatsNew.parse(
        runCatching { ctx.assets.open(WhatsNew.ASSET).bufferedReader().use { it.readText() } }.getOrNull(),
        core.appLocale().language
    )

    /** Once after CarHome was updated (not after a fresh install). */
    private fun checkWhatsNew() {
        val cur = versionCode
        val last = prefs.lastVersionCode
        val updated = if (last == 0) pkgInfo?.let { it.lastUpdateTime > it.firstInstallTime + 1_000L } == true else last < cur
        if (updated) _whatsNew.value = whatsNewItems().ifEmpty { null } else prefs.lastVersionCode = cur
    }

    fun showWhatsNew() { _whatsNew.value = whatsNewItems().ifEmpty { null } }

    fun closeWhatsNew() {
        prefs.lastVersionCode = versionCode
        _whatsNew.value = null
    }

    fun installUpdate() = core.updater.install()
    fun checkForUpdate() = core.updater.check()

    fun userTouched() {
        lastTouch = SystemClock.elapsedRealtime()
        if (_parked.value) _parked.value = false
    }

    private fun updateParked() {
        val s = settings.value
        val idle = SystemClock.elapsedRealtime() - lastTouch
        // Only when we know the car is stopped (speed unknown is not "stopped": no GPS,
        // a tunnel) and CarHome itself is on screen
        val stopped = engine.speedMps.value != null && !engine.speedStale.value && !engine.moving.value
        val should = s.parkedScreen && _nav.value.page == Page.HOME && stopped && core.uiVisible.value &&
            idle >= s.parkedAfterMin * 60_000L
        if (should != _parked.value) _parked.value = should
    }

    private fun updateNight() {
        val s = settings.value
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val now = cal.timeInMillis
        val on = when (s.nightMode) {
            NightMode.OFF -> false
            NightMode.ALWAYS -> true
            NightMode.CUSTOM -> if (s.nightFrom <= s.nightTo) hour in s.nightFrom until s.nightTo
                else hour >= s.nightFrom || hour < s.nightTo
            NightMode.AUTO -> _sun.value?.let { (rise, set) -> now < rise || now >= set } ?: (hour >= 18 || hour < 6)
        }
        _night.value = on
        applySystemBrightness(on && s.nightSystem, s)
    }

    fun canWriteSystem(): Boolean = Settings.System.canWrite(ctx)

    /** Last dim level written, so the minute loop doesn't undo the user's own changes. */
    private var dimApplied: Int? = null

    private fun applySystemBrightness(dim: Boolean, s: LauncherSettings) {
        if (!canWriteSystem()) return
        val want = if (dim) s.nightDim else -1
        if (want == dimApplied) return
        runCatching {
            val cr = ctx.contentResolver
            if (dim) {
                if (prefs.savedBrightness < 0) {
                    prefs.savedBrightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 160)
                    prefs.savedBrightnessMode = Settings.System.getInt(
                        cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                }
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                val level = (prefs.savedBrightness * (1f - s.nightDim / 100f)).toInt().coerceIn(8, 255)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, level)
            } else if (prefs.savedBrightness >= 0) {
                // Put back both the level and automatic brightness, if it was on
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, prefs.savedBrightness)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, prefs.savedBrightnessMode)
                prefs.savedBrightness = -1
            }
            dimApplied = want
        }
    }

    private fun startMinuteLoop() {
        viewModelScope.launch {
            var tickN = 0
            while (true) {
                if (tickN % 12 == 0) {   // every minute
                    refreshPrayer()
                    updateNight()
                    refreshSmartPick()
                    core.updater.checkIfDue()   // itself only every 6 h, and only if switched on
                }
                updateNotices()
                updateParked()
                tickN++
                delay(5_000)
            }
        }
    }

    // Driving mode: on above the chosen speed, off after 15 s below it
    private fun startDrivingLoop() {
        viewModelScope.launch {
            var belowSince = 0L
            while (true) {
                val s = settings.value
                val kmh = (engine.speedMps.value ?: 0f) * 3.6f
                val threshold = if (s.useKmh) s.drivingSpeed.toFloat() else s.drivingSpeed * 1.609344f
                val now = SystemClock.elapsedRealtime()
                if (!s.drivingMode) {
                    _driving.value = false
                } else if (kmh >= threshold) {
                    _driving.value = true
                    belowSince = 0L
                } else if (_driving.value) {
                    if (belowSince == 0L) belowSince = now
                    if (now - belowSince >= 15_000L) _driving.value = false
                }
                if (engine.moving.value && _parked.value) _parked.value = false
                delay(1_000)
            }
        }
    }

    // ── Car start: welcome screen and auto-start ────────────────────────────
    private fun onCarStart() {
        val s = settings.value
        dismissed.clear()
        spokenOil = false
        spokenDust = false
        lastTouch = SystemClock.elapsedRealtime()
        if (s.welcome) _welcome.value = true   // the screen hides it 4.5 s after it appears
        if (s.autoStart && (s.autoMusic || s.autoApp.isNotEmpty())) {
            viewModelScope.launch {
                delay(maxOf(s.autoDelay * 1_000L, if (s.welcome) 4_500L else 0L))
                if (s.autoMusic) resumeMusic()
                if (s.autoApp.isNotEmpty()) launchApp(s.autoApp, record = false, ignoreParked = true)
            }
        }
    }

    fun dismissWelcome() { _welcome.value = false }

    // ── Pass-throughs for the Settings page ─────────────────────────────────
    fun testAlert() = engine.testAlert()
    fun testChime() = core.voice.chime()
    fun testVoice() = core.voice.say(R.string.voice_test, force = true)
    fun newTrip() = engine.newTrip()
    fun oilChanged() { engine.oilChanged(); dismissed.remove("OIL"); spokenOil = false }
    fun adjustOil(km: Double) = engine.adjustOil(km)
    fun pairedObd() = core.obd.pairedDevices()
    fun readCodes() = core.obd.readCodes()
    fun scores(): List<DayScore> = prefs.loadScores()

    override fun onCleared() {
        runCatching { ctx.unregisterReceiver(packageReceiver) }
        super.onCleared()
    }

    init {
        loadApps()
        loadPhoto()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        runCatching { ContextCompat.registerReceiver(app, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED) }
        if (settings.value.voice) core.voice.prepare()
        core.obd.sync()
        startWeatherLoop()
        startMinuteLoop()
        startDrivingLoop()
        viewModelScope.launch { nowPlaying.collect { updateSongAccent(it) } }
        viewModelScope.launch {
            engine.location.collect { loc -> if (loc != null && _prayer.value == null) { refreshPrayer(); updateNight() } }
        }
        viewModelScope.launch {
            core.carStarts.collect { n ->
                if (n > core.handledCarStarts) {
                    core.handledCarStarts = n
                    onCarStart()
                }
            }
        }
        viewModelScope.launch { _apps.collect { refreshSmartPick() } }
        viewModelScope.launch { core.updater.state.collect { updateNotices() } }
        viewModelScope.launch { core.cameras.ahead.collect { updateNotices() } }
        checkWhatsNew()
    }
}

fun prayerName(p: Prayer): Int = when (p) {
    Prayer.FAJR -> R.string.p_fajr
    Prayer.DHUHR -> R.string.p_dhuhr
    Prayer.ASR -> R.string.p_asr
    Prayer.MAGHRIB -> R.string.p_maghrib
    Prayer.ISHA -> R.string.p_isha
}
