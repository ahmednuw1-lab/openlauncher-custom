package com.carhome.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Choices ───────────────────────────────────────────────────────────────────

enum class AlertMode { SCREEN, BEEP_ONCE, BEEP_REPEAT }
/** Cards that can sit in the info column of the home screen. */
enum class CardType { CLOCK, SPEED, MUSIC, WEATHER, PRAYER, TRIP, CAR }
enum class Side { START, END }
enum class ColumnWidth(val fraction: Float) { NARROW(0.30f), NORMAL(0.36f), WIDE(0.44f) }
enum class TextSize(val scale: Float) { NORMAL(1f), LARGE(1.12f), XL(1.25f) }
enum class SpeedStyle { GAUGE, NUMBER }
enum class AppLanguage(val tag: String?) { SYSTEM(null), ARABIC("ar"), ENGLISH("en") }
enum class NoRoadData { USE_MINE, NO_ALERT }
enum class PrayerMethod { UMM_AL_QURA, MWL, EGYPT, KARACHI }
enum class TripReset { EACH_START, MANUAL }
enum class NightMode { OFF, AUTO, ALWAYS, CUSTOM }
enum class BubbleSize(val dp: Int) { SMALL(64), MEDIUM(84), LARGE(108) }
enum class VolumeBoost { OFF, LOW, MEDIUM, HIGH }
enum class Background { CLASSIC, SKY, PHOTO }
/** Where the shown speed comes from. AUTO = GPS, or the car (OBD) when GPS has no signal. */
enum class SpeedSource { AUTO, GPS, CAR }

/**
 * Every user choice. All features added in 1.2 start switched off, so the home
 * screen looks exactly like 1.1 until the user turns something on.
 * Speeds and limits are in the user's unit (km/h or mph).
 */
data class LauncherSettings(
    // Home layout
    val tileCount: Int = 8,
    val cards: List<CardType> = DEFAULT_CARDS,
    val showWeather: Boolean = true,          // weather strip in the top bar
    val showPrayerChip: Boolean = false,      // "next prayer" chip in the top bar
    val infoSide: Side = Side.START,
    val infoWidth: ColumnWidth = ColumnWidth.NORMAL,
    val showTileLabels: Boolean = true,
    val textSize: TextSize = TextSize.NORMAL,
    val use24h: Boolean = true,
    val clockSeconds: Boolean = false,
    val showDate: Boolean = true,
    val showHijri: Boolean = false,
    val speedStyle: SpeedStyle = SpeedStyle.GAUGE,
    val useKmh: Boolean = true,
    val speedSource: SpeedSource = SpeedSource.AUTO,
    // Speed alert
    val speedAlert: Boolean = true,
    val speedLimit: Int = 120,
    val alertMode: AlertMode = AlertMode.BEEP_ONCE,
    val speedMargin: Int = 0,
    // Automatic road speed limit
    val roadLimit: Boolean = false,
    val roadNoData: NoRoadData = NoRoadData.USE_MINE,
    val roadCap: Boolean = false,
    // Background tracking and floating bubble
    val backgroundTracking: Boolean = false,
    val bubble: Boolean = false,
    val bubbleSize: BubbleSize = BubbleSize.MEDIUM,
    val bubbleLock: Boolean = false,
    val bubbleOnlyMoving: Boolean = false,
    val speedVolume: VolumeBoost = VolumeBoost.OFF,
    // Prayer
    val prayerMethod: PrayerMethod = PrayerMethod.UMM_AL_QURA,
    val asrHanafi: Boolean = false,
    val prayerReminder: Int = 0,              // minutes before; 0 = off
    val prayerChime: Boolean = true,
    val nearbyButtons: Boolean = false,
    // Trip and car
    val tripReset: TripReset = TripReset.EACH_START,
    val driveScore: Boolean = false,
    val oilReminder: Boolean = false,
    val oilInterval: Int = 10_000,
    val fuelCost: Boolean = false,
    val fuelConsumption: Float = 10f,         // litres per 100 km
    val fuelPrice: Float = 2.33f,             // per litre, in the user's currency
    val breakReminder: Boolean = false,
    val breakAfterMin: Int = 120,
    val obd: Boolean = false,
    val obdAddress: String = "",
    // Startup
    val welcome: Boolean = false,
    val welcomeName: String = "",
    val autoStart: Boolean = false,
    val autoMusic: Boolean = true,
    val autoApp: String = "",
    val autoDelay: Int = 5,
    // Display
    val accent: Long = ACCENTS.first(),
    val songColours: Boolean = false,
    val background: Background = Background.CLASSIC,
    val nightMode: NightMode = NightMode.OFF,
    val nightFrom: Int = 19,
    val nightTo: Int = 6,
    val nightDim: Int = 40,                   // percent
    val nightSystem: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    // Smart
    val drivingMode: Boolean = false,
    val drivingSpeed: Int = 20,
    val swipeMusic: Boolean = false,
    val placeTiles: Boolean = false,
    val smartTile: Boolean = false,
    val parkedOnly: Boolean = false,
    val dustWarning: Boolean = false,
    val parkedScreen: Boolean = false,
    val parkedAfterMin: Int = 5,
    val voice: Boolean = false,
    val voiceSpeed: Boolean = true,
    val voicePrayer: Boolean = true,
    val voiceReminders: Boolean = true
)

val DEFAULT_CARDS = listOf(CardType.CLOCK, CardType.SPEED, CardType.MUSIC)
const val MAX_CARDS = 4
val SPEED_MARGINS = listOf(0, 5, 10)
const val SPEED_LIMIT_MIN = 30
const val SPEED_LIMIT_MAX = 220
val TILE_COUNTS = listOf(4, 6, 8, 12)
val OIL_INTERVALS = listOf(5_000, 7_500, 10_000)
val BREAK_AFTER = listOf(60, 90, 120, 150)
val PARKED_AFTER = listOf(2, 5, 10)
val DRIVING_SPEEDS = listOf(10, 20, 30)
val REMINDER_MINUTES = listOf(0, 5, 10, 15)
val AUTO_DELAYS = listOf(0, 5, 10)
val NIGHT_DIMS = listOf(20, 40, 60, 80)

/** Modern accent palette offered in Settings (ARGB). */
val ACCENTS: List<Long> = listOf(
    0xFF4F8CFF, // blue
    0xFF22C55E, // green
    0xFFF59E0B, // amber
    0xFFEF4444, // red
    0xFFA855F7, // purple
    0xFF14B8A6  // teal
)

// ── Small stored records ──────────────────────────────────────────────────────

/** A saved destination for a place tile. Either coordinates or a typed address. */
data class Place(val id: String, val name: String, val lat: Double?, val lon: Double?, val address: String)

/** One drive. Distance in metres, moving time in ms, top speed in m/s. */
data class TripStats(
    val startedAt: Long = System.currentTimeMillis(),
    val distanceM: Double = 0.0,
    val movingMs: Long = 0L,
    val maxMps: Float = 0f,
    val harshEvents: Int = 0
) {
    val avgMps: Float get() = if (movingMs > 0) (distanceM / (movingMs / 1000.0)).toFloat() else 0f

    /** Smooth-driving score 0..100; none for trips under 1 km. */
    val score: Int?
        get() {
            val km = distanceM / 1000.0
            if (km < 1.0) return null
            // Each sudden brake/acceleration costs 6 points on a 10 km trip, less on longer ones
            val penalty = harshEvents * 6.0 * 10.0 / maxOf(km, 10.0)
            return (100 - penalty).toInt().coerceIn(0, 100)
        }
}

data class DayScore(val day: String, val sum: Int, val count: Int) {
    val average: Int get() = if (count > 0) sum / count else 0
}

const val TILE_PLACE = "place:"
const val TILE_SMART = "smart"

// ── Storage ───────────────────────────────────────────────────────────────────

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("carhome", Context.MODE_PRIVATE)

    // Settings are one JSON string: easy to extend, and missing keys fall back to defaults
    fun loadSettings(): LauncherSettings {
        val json = sp.getString("settings", null)
        if (json == null) return migrateFrom11()
        return runCatching { settingsFromJson(JSONObject(json)) }.getOrElse { LauncherSettings() }
    }

    fun saveSettings(s: LauncherSettings) {
        sp.edit().putString("settings", settingsToJson(s).toString()).apply()
    }

    /** CarHome 1.1 stored each setting under its own key. */
    private fun migrateFrom11(): LauncherSettings {
        val d = LauncherSettings()
        if (!sp.contains("tile_count") && !sp.contains("speed_limit")) return d
        val showSpeed = sp.getBoolean("show_speed", true)
        return d.copy(
            tileCount = sp.getInt("tile_count", d.tileCount).takeIf { it in TILE_COUNTS } ?: d.tileCount,
            accent = sp.getLong("accent", d.accent),
            use24h = sp.getBoolean("use_24h", d.use24h),
            showWeather = sp.getBoolean("show_weather", d.showWeather),
            cards = if (showSpeed) DEFAULT_CARDS else listOf(CardType.CLOCK, CardType.MUSIC),
            useKmh = sp.getBoolean("use_kmh", d.useKmh),
            speedAlert = sp.getBoolean("speed_alert", d.speedAlert),
            speedLimit = sp.getInt("speed_limit", d.speedLimit).coerceIn(SPEED_LIMIT_MIN, SPEED_LIMIT_MAX),
            alertMode = sp.getString("alert_mode", null)
                ?.let { n -> AlertMode.entries.firstOrNull { it.name == n } } ?: d.alertMode,
            speedMargin = sp.getInt("speed_margin", d.speedMargin).takeIf { it in SPEED_MARGINS } ?: d.speedMargin
        ).also { saveSettings(it) }
    }

    /** Tile contents: package name, "place:<id>", "smart", or "" for empty. Always [count] long. */
    fun loadTiles(count: Int): List<String> {
        val saved = sp.getString("tiles", null)?.split('|') ?: emptyList()
        return List(count) { i -> saved.getOrNull(i).orEmpty() }
    }

    fun saveTiles(tiles: List<String>) {
        sp.edit().putString("tiles", tiles.joinToString("|")).apply()
    }

    fun loadPlaces(): List<Place> = runCatching {
        val arr = JSONArray(sp.getString("places", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Place(
                id = o.getString("id"),
                name = o.optString("name"),
                lat = if (o.has("lat")) o.getDouble("lat") else null,
                lon = if (o.has("lon")) o.getDouble("lon") else null,
                address = o.optString("address")
            )
        }
    }.getOrDefault(emptyList())

    fun savePlaces(list: List<Place>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("address", p.address)
                if (p.lat != null && p.lon != null) { put("lat", p.lat); put("lon", p.lon) }
            })
        }
        sp.edit().putString("places", arr.toString()).apply()
    }

    var bubbleX: Int
        get() = sp.getInt("bubble_x", -1)
        set(v) = sp.edit().putInt("bubble_x", v).apply()
    var bubbleY: Int
        get() = sp.getInt("bubble_y", 160)
        set(v) = sp.edit().putInt("bubble_y", v).apply()

    var odometerKm: Double
        get() = sp.getString("odometer_km", null)?.toDoubleOrNull() ?: 0.0
        set(v) = sp.edit().putString("odometer_km", v.toString()).apply()
    /** Odometer reading at the last oil change. */
    var oilChangedAtKm: Double
        get() = sp.getString("oil_at_km", null)?.toDoubleOrNull() ?: 0.0
        set(v) = sp.edit().putString("oil_at_km", v.toString()).apply()

    fun loadTrip(key: String = "trip"): TripStats? = runCatching {
        val o = JSONObject(sp.getString(key, null) ?: return null)
        TripStats(
            startedAt = o.getLong("start"),
            distanceM = o.getDouble("dist"),
            movingMs = o.getLong("moving"),
            maxMps = o.getDouble("max").toFloat(),
            harshEvents = o.optInt("harsh", 0)
        )
    }.getOrNull()

    fun saveTrip(t: TripStats?, key: String = "trip") {
        if (t == null) { sp.edit().remove(key).apply(); return }
        val o = JSONObject().apply {
            put("start", t.startedAt); put("dist", t.distanceM); put("moving", t.movingMs)
            put("max", t.maxMps.toDouble()); put("harsh", t.harshEvents)
        }
        sp.edit().putString(key, o.toString()).apply()
    }

    /** Launch counts per app in 8 three-hour slots of the day, for the smart tile. */
    fun loadLaunchHistory(): MutableMap<String, IntArray> = runCatching {
        val o = JSONObject(sp.getString("launches", "{}"))
        val out = mutableMapOf<String, IntArray>()
        o.keys().forEach { pkg ->
            val a = o.getJSONArray(pkg)
            out[pkg] = IntArray(8) { i -> a.optInt(i, 0) }
        }
        out
    }.getOrDefault(mutableMapOf())

    fun saveLaunchHistory(map: Map<String, IntArray>) {
        val o = JSONObject()
        map.forEach { (pkg, counts) -> o.put(pkg, JSONArray().apply { counts.forEach { put(it) } }) }
        sp.edit().putString("launches", o.toString()).apply()
    }

    var parkedOnlyApps: Set<String>
        get() = sp.getStringSet("parked_only", emptySet())?.toSet() ?: emptySet()
        set(v) = sp.edit().putStringSet("parked_only", v).apply()

    fun loadScores(): List<DayScore> = runCatching {
        val arr = JSONArray(sp.getString("scores", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DayScore(o.getString("d"), o.getInt("s"), o.getInt("n"))
        }
    }.getOrDefault(emptyList())

    /** Adds one trip score to today's average; keeps two weeks. */
    fun addScore(score: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val list = loadScores().toMutableList()
        val i = list.indexOfFirst { it.day == today }
        if (i >= 0) list[i] = list[i].copy(sum = list[i].sum + score, count = list[i].count + 1)
        else list += DayScore(today, score, 1)
        val keep = list.sortedBy { it.day }.takeLast(14)
        val arr = JSONArray()
        keep.forEach { arr.put(JSONObject().apply { put("d", it.day); put("s", it.sum); put("n", it.count) }) }
        sp.edit().putString("scores", arr.toString()).apply()
    }

    var lastBootCount: Int
        get() = sp.getInt("boot_count", -2)
        set(v) = sp.edit().putInt("boot_count", v).apply()

    var askedLocation: Boolean
        get() = sp.getBoolean("asked_location", false)
        set(v) = sp.edit().putBoolean("asked_location", v).apply()

    /** System brightness saved before night mode lowered it; -1 = nothing saved. */
    var savedBrightness: Int
        get() = sp.getInt("saved_brightness", -1)
        set(v) = sp.edit().putInt("saved_brightness", v).apply()
}

// ── JSON mapping ──────────────────────────────────────────────────────────────

private inline fun <reified T : Enum<T>> JSONObject.enumOr(key: String, def: T): T {
    val n = optString(key, "")
    return enumValues<T>().firstOrNull { it.name == n } ?: def
}

private fun JSONObject.intFrom(key: String, def: Int, allowed: Collection<Int>): Int =
    optInt(key, def).takeIf { it in allowed } ?: def

fun settingsFromJson(o: JSONObject): LauncherSettings {
    val d = LauncherSettings()
    val cards = o.optJSONArray("cards")?.let { arr ->
        (0 until arr.length()).mapNotNull { i ->
            val n = arr.optString(i)
            CardType.entries.firstOrNull { it.name == n }
        }.distinct().take(MAX_CARDS)
    } ?: d.cards
    return LauncherSettings(
        tileCount = o.intFrom("tileCount", d.tileCount, TILE_COUNTS),
        cards = cards,
        showWeather = o.optBoolean("showWeather", d.showWeather),
        showPrayerChip = o.optBoolean("showPrayerChip", d.showPrayerChip),
        infoSide = o.enumOr("infoSide", d.infoSide),
        infoWidth = o.enumOr("infoWidth", d.infoWidth),
        showTileLabels = o.optBoolean("showTileLabels", d.showTileLabels),
        textSize = o.enumOr("textSize", d.textSize),
        use24h = o.optBoolean("use24h", d.use24h),
        clockSeconds = o.optBoolean("clockSeconds", d.clockSeconds),
        showDate = o.optBoolean("showDate", d.showDate),
        showHijri = o.optBoolean("showHijri", d.showHijri),
        speedStyle = o.enumOr("speedStyle", d.speedStyle),
        useKmh = o.optBoolean("useKmh", d.useKmh),
        speedSource = o.enumOr("speedSource", d.speedSource),
        speedAlert = o.optBoolean("speedAlert", d.speedAlert),
        speedLimit = o.optInt("speedLimit", d.speedLimit).coerceIn(SPEED_LIMIT_MIN, SPEED_LIMIT_MAX),
        alertMode = o.enumOr("alertMode", d.alertMode),
        speedMargin = o.intFrom("speedMargin", d.speedMargin, SPEED_MARGINS),
        roadLimit = o.optBoolean("roadLimit", d.roadLimit),
        roadNoData = o.enumOr("roadNoData", d.roadNoData),
        roadCap = o.optBoolean("roadCap", d.roadCap),
        backgroundTracking = o.optBoolean("backgroundTracking", d.backgroundTracking),
        bubble = o.optBoolean("bubble", d.bubble),
        bubbleSize = o.enumOr("bubbleSize", d.bubbleSize),
        bubbleLock = o.optBoolean("bubbleLock", d.bubbleLock),
        bubbleOnlyMoving = o.optBoolean("bubbleOnlyMoving", d.bubbleOnlyMoving),
        speedVolume = o.enumOr("speedVolume", d.speedVolume),
        prayerMethod = o.enumOr("prayerMethod", d.prayerMethod),
        asrHanafi = o.optBoolean("asrHanafi", d.asrHanafi),
        prayerReminder = o.intFrom("prayerReminder", d.prayerReminder, REMINDER_MINUTES),
        prayerChime = o.optBoolean("prayerChime", d.prayerChime),
        nearbyButtons = o.optBoolean("nearbyButtons", d.nearbyButtons),
        tripReset = o.enumOr("tripReset", d.tripReset),
        driveScore = o.optBoolean("driveScore", d.driveScore),
        oilReminder = o.optBoolean("oilReminder", d.oilReminder),
        oilInterval = o.intFrom("oilInterval", d.oilInterval, OIL_INTERVALS),
        fuelCost = o.optBoolean("fuelCost", d.fuelCost),
        fuelConsumption = o.optDouble("fuelConsumption", d.fuelConsumption.toDouble()).toFloat().coerceIn(3f, 30f),
        fuelPrice = o.optDouble("fuelPrice", d.fuelPrice.toDouble()).toFloat().coerceIn(0.1f, 20f),
        breakReminder = o.optBoolean("breakReminder", d.breakReminder),
        breakAfterMin = o.intFrom("breakAfterMin", d.breakAfterMin, BREAK_AFTER),
        obd = o.optBoolean("obd", d.obd),
        obdAddress = o.optString("obdAddress", d.obdAddress),
        welcome = o.optBoolean("welcome", d.welcome),
        welcomeName = o.optString("welcomeName", d.welcomeName).take(30),
        autoStart = o.optBoolean("autoStart", d.autoStart),
        autoMusic = o.optBoolean("autoMusic", d.autoMusic),
        autoApp = o.optString("autoApp", d.autoApp),
        autoDelay = o.intFrom("autoDelay", d.autoDelay, AUTO_DELAYS),
        accent = o.optLong("accent", d.accent),
        songColours = o.optBoolean("songColours", d.songColours),
        background = o.enumOr("background", d.background),
        nightMode = o.enumOr("nightMode", d.nightMode),
        nightFrom = o.optInt("nightFrom", d.nightFrom).coerceIn(0, 23),
        nightTo = o.optInt("nightTo", d.nightTo).coerceIn(0, 23),
        nightDim = o.intFrom("nightDim", d.nightDim, NIGHT_DIMS),
        nightSystem = o.optBoolean("nightSystem", d.nightSystem),
        language = o.enumOr("language", d.language),
        drivingMode = o.optBoolean("drivingMode", d.drivingMode),
        drivingSpeed = o.intFrom("drivingSpeed", d.drivingSpeed, DRIVING_SPEEDS),
        swipeMusic = o.optBoolean("swipeMusic", d.swipeMusic),
        placeTiles = o.optBoolean("placeTiles", d.placeTiles),
        smartTile = o.optBoolean("smartTile", d.smartTile),
        parkedOnly = o.optBoolean("parkedOnly", d.parkedOnly),
        dustWarning = o.optBoolean("dustWarning", d.dustWarning),
        parkedScreen = o.optBoolean("parkedScreen", d.parkedScreen),
        parkedAfterMin = o.intFrom("parkedAfterMin", d.parkedAfterMin, PARKED_AFTER),
        voice = o.optBoolean("voice", d.voice),
        voiceSpeed = o.optBoolean("voiceSpeed", d.voiceSpeed),
        voicePrayer = o.optBoolean("voicePrayer", d.voicePrayer),
        voiceReminders = o.optBoolean("voiceReminders", d.voiceReminders)
    )
}

fun settingsToJson(s: LauncherSettings): JSONObject = JSONObject().apply {
    put("tileCount", s.tileCount)
    put("cards", JSONArray().apply { s.cards.forEach { put(it.name) } })
    put("showWeather", s.showWeather)
    put("showPrayerChip", s.showPrayerChip)
    put("infoSide", s.infoSide.name)
    put("infoWidth", s.infoWidth.name)
    put("showTileLabels", s.showTileLabels)
    put("textSize", s.textSize.name)
    put("use24h", s.use24h)
    put("clockSeconds", s.clockSeconds)
    put("showDate", s.showDate)
    put("showHijri", s.showHijri)
    put("speedStyle", s.speedStyle.name)
    put("useKmh", s.useKmh)
    put("speedSource", s.speedSource.name)
    put("speedAlert", s.speedAlert)
    put("speedLimit", s.speedLimit)
    put("alertMode", s.alertMode.name)
    put("speedMargin", s.speedMargin)
    put("roadLimit", s.roadLimit)
    put("roadNoData", s.roadNoData.name)
    put("roadCap", s.roadCap)
    put("backgroundTracking", s.backgroundTracking)
    put("bubble", s.bubble)
    put("bubbleSize", s.bubbleSize.name)
    put("bubbleLock", s.bubbleLock)
    put("bubbleOnlyMoving", s.bubbleOnlyMoving)
    put("speedVolume", s.speedVolume.name)
    put("prayerMethod", s.prayerMethod.name)
    put("asrHanafi", s.asrHanafi)
    put("prayerReminder", s.prayerReminder)
    put("prayerChime", s.prayerChime)
    put("nearbyButtons", s.nearbyButtons)
    put("tripReset", s.tripReset.name)
    put("driveScore", s.driveScore)
    put("oilReminder", s.oilReminder)
    put("oilInterval", s.oilInterval)
    put("fuelCost", s.fuelCost)
    put("fuelConsumption", s.fuelConsumption.toDouble())
    put("fuelPrice", s.fuelPrice.toDouble())
    put("breakReminder", s.breakReminder)
    put("breakAfterMin", s.breakAfterMin)
    put("obd", s.obd)
    put("obdAddress", s.obdAddress)
    put("welcome", s.welcome)
    put("welcomeName", s.welcomeName)
    put("autoStart", s.autoStart)
    put("autoMusic", s.autoMusic)
    put("autoApp", s.autoApp)
    put("autoDelay", s.autoDelay)
    put("accent", s.accent)
    put("songColours", s.songColours)
    put("background", s.background.name)
    put("nightMode", s.nightMode.name)
    put("nightFrom", s.nightFrom)
    put("nightTo", s.nightTo)
    put("nightDim", s.nightDim)
    put("nightSystem", s.nightSystem)
    put("language", s.language.name)
    put("drivingMode", s.drivingMode)
    put("drivingSpeed", s.drivingSpeed)
    put("swipeMusic", s.swipeMusic)
    put("placeTiles", s.placeTiles)
    put("smartTile", s.smartTile)
    put("parkedOnly", s.parkedOnly)
    put("dustWarning", s.dustWarning)
    put("parkedScreen", s.parkedScreen)
    put("parkedAfterMin", s.parkedAfterMin)
    put("voice", s.voice)
    put("voiceSpeed", s.voiceSpeed)
    put("voicePrayer", s.voicePrayer)
    put("voiceReminders", s.voiceReminders)
}
