package com.carhome.launcher

import android.content.Context

/** User choices, stored in plain SharedPreferences (small, fast, no extra library). */
data class LauncherSettings(
    val tileCount: Int = 8,
    val accent: Long = ACCENTS.first(),
    val use24h: Boolean = true,
    val showWeather: Boolean = true
)

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
            showWeather = sp.getBoolean("show_weather", d.showWeather)
        )
    }

    fun saveSettings(s: LauncherSettings) {
        sp.edit()
            .putInt("tile_count", s.tileCount)
            .putLong("accent", s.accent)
            .putBoolean("use_24h", s.use24h)
            .putBoolean("show_weather", s.showWeather)
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
