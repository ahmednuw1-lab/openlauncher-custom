package com.carhome.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/** Holds the parts shared by the screens and the background service. */
class CarHomeApp : Application() {
    lateinit var core: Core
        private set

    override fun onCreate() {
        super.onCreate()
        core = Core(this)
    }

    companion object {
        fun core(context: Context): Core = (context.applicationContext as CarHomeApp).core
    }
}

class Core(val app: Application) {
    val prefs = Prefs(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _settings = MutableStateFlow(prefs.loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    /** Bumped each time the car starts (box boots, or wakes after 10+ min off). */
    private val _carStarts = MutableStateFlow(0)
    val carStarts: StateFlow<Int> = _carStarts.asStateFlow()

    /** How many car starts the screen has already reacted to (welcome, auto-start). */
    var handledCarStarts = 0

    /** True while CarHome itself is on screen (the bubble hides then). */
    val uiVisible = MutableStateFlow(false)

    val voice = Voice(this)
    val engine = DriveEngine(this)
    val obd = ObdClient(this)
    val updater by lazy { Updater(this) }
    val cameras by lazy { CameraWatch(this) }

    fun updateSettings(block: (LauncherSettings) -> LauncherSettings): Pair<LauncherSettings, LauncherSettings> {
        val old = _settings.value
        var new = block(old)
        if (new.useKmh != old.useKmh) {
            // Keep the same real-world limit when switching km/h ↔ mph (rounded to 5)
            val factor = if (new.useKmh) 1.609344f else 1f / 1.609344f
            val converted = (Math.round(new.speedLimit * factor / 5f) * 5).coerceIn(SPEED_LIMIT_MIN, SPEED_LIMIT_MAX)
            new = new.copy(speedLimit = converted)
        }
        _settings.value = new
        prefs.saveSettings(new)
        return old to new
    }

    /** The language CarHome shows: the user's choice, or the box's. */
    /** The app's language, always with Western digits (123). */
    fun appLocale(): Locale {
        val tag = _settings.value.language.tag
        val base = if (tag != null) Locale.forLanguageTag(tag)
        else app.resources.configuration.locales[0] ?: Locale.getDefault()
        return Digits.latin(base)
    }

    /** Context whose strings follow the chosen language (for voice and notifications). */
    private var localizedTag: String? = null
    private var localizedCtx: Context? = null

    fun localized(): Context {
        val tag = _settings.value.language.tag ?: "system"
        localizedCtx?.let { if (localizedTag == tag) return it }
        val cfg = Configuration(app.resources.configuration)
        val loc = appLocale()
        cfg.setLocale(loc)
        cfg.setLayoutDirection(loc)
        return app.createConfigurationContext(cfg).also { localizedCtx = it; localizedTag = tag }
    }

    // ── Car start detection ─────────────────────────────────────────────────
    // CarPlay boxes either reboot when the car starts or wake from sleep.
    private var screenOffAt = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> screenOffAt = SystemClock.elapsedRealtime()
                Intent.ACTION_SCREEN_ON -> {
                    val off = screenOffAt
                    if (off > 0 && SystemClock.elapsedRealtime() - off >= 10 * 60_000L) carStarted()
                    screenOffAt = 0L
                }
            }
        }
    }

    private fun carStarted() {
        _carStarts.value = _carStarts.value + 1
        engine.onCarStart()
    }

    init {
        runCatching {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else app.registerReceiver(screenReceiver, filter)
        }
        val boots = runCatching {
            Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrDefault(-1)
        if (boots != prefs.lastBootCount) {
            prefs.lastBootCount = boots
            // Posted so every part of Core exists before listeners react
            scope.launch { carStarted() }
        }
    }
}
