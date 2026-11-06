package com.carhome.launcher

import android.content.Context

/** User choices, stored in plain SharedPreferences (small, fast, no extra library). */
data class LauncherSettings(
    val tileCount: Int = 8,
    val accent: Long = ACCENTS.first(),
    val use24h: Boolean = true,
    val showWeather: Boolean = true,
    val showSpeed: Boolean = true,
    val useKmh: Boolean = true,
    // Speed alert — limit and margin are in the user's chosen unit (km/h or mph)
    val speedAlert: Boolean = true,
    val speedLimit: Int = 120,
    val alertMode: AlertMode = AlertMode.BEEP_ONCE,
    val speedMargin: Int = 0
)

enum class AlertMode { SCREEN, BEEP_ONCE, BEEP_REPEAT }

val SPEED_MARGINS = listOf(0, 5, 10)
const val SPEED_LIMIT_MIN = 30
const val SPEED_LIMIT_MAX = 220

/** Modern accent palette offered in Settings (ARGB). */
val ACCENTS: List<Long> = listOf(
    0xFF4F8CFF, // blue
    0xFF22C55E, // green
    0xFFF59E0B, // amber
    0xFFEF4444, // red
    0xFFA855F7, // purple
    0xFF14B8A6  // teal
)

val TILE_COUNTS = listOf(4, 6, 8, 12)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("carhome", Context.MODE_PRIVATE)

    fun loadSettings(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            tileCount   = sp.getInt("tile_count", d.tileCount).takeIf { it in TILE_COUNTS } ?: d.tileCount,
            accent      = sp.getLong("accent", d.accent),
            use24h      = sp.getBoolean("use_24h", d.use24h),
            showWeather = sp.getBoolean("show_weather", d.showWeather),
            showSpeed   = sp.getBoolean("show_speed", d.showSpeed),
            useKmh      = sp.getBoolean("use_kmh", d.useKmh),
            speedAlert  = sp.getBoolean("speed_alert", d.speedAlert),
            speedLimit  = sp.getInt("speed_limit", d.speedLimit).coerceIn(SPEED_LIMIT_MIN, SPEED_LIMIT_MAX),
            alertMode   = sp.getString("alert_mode", null)
                ?.let { name -> AlertMode.entries.firstOrNull { it.name == name } } ?: d.alertMode,
            speedMargin = sp.getInt("speed_margin", d.speedMargin).takeIf { it in SPEED_MARGINS } ?: d.speedMargin
        )
    }

    fun saveSettings(s: LauncherSettings) {
        sp.edit()
            .putInt("tile_count", s.tileCount)
            .putLong("accent", s.accent)
            .putBoolean("use_24h", s.use24h)
            .putBoolean("show_weather", s.showWeather)
            .putBoolean("show_speed", s.showSpeed)
            .putBoolean("use_kmh", s.useKmh)
            .putBoolean("speed_alert", s.speedAlert)
            .putInt("speed_limit", s.speedLimit)
            .putString("alert_mode", s.alertMode.name)
            .putInt("speed_margin", s.speedMargin)
            .apply()
    }

    /** Package names for the home tiles; "" = empty tile. Always exactly [count] long. */
    fun loadTiles(count: Int): List<String> {
        val saved = sp.getString("tiles", null)?.split('|') ?: emptyList()
        return List(count) { i -> saved.getOrNull(i).orEmpty() }
    }

    fun saveTiles(tiles: List<String>) {
        sp.edit().putString("tiles", tiles.joinToString("|")).apply()
    }
}
