package com.carhome.launcher

import org.json.JSONArray
import org.json.JSONObject

/**
 * Settings backup: everything CarHome keeps (settings, tiles, places, trips,
 * reminders…) in one JSON file, with each value's type, so it can be loaded on
 * any box. Values that only make sense on this box are left out.
 */
object Backup {

    private const val FORMAT = "carhome-backup"
    private const val VERSION = 1

    /**
     * Kept per box: brightness before night mode, update bookkeeping, reminders already
     * given, and first-run marks (a new box must still ask for location the first time).
     */
    val LOCAL_ONLY = setOf(
        "saved_brightness", "saved_brightness_mode", "update_checked_at", "dismissed_update",
        "last_version_code", "reminded_prayer_at", "asked_location", "boot_count"
    )

    /**
     * The type each key CarHome reads must have ("s" text, "i" Int, "l" Long, "b" yes/no,
     * "f" Float, "set" text set). A backup value of another type is dropped: reading it
     * would crash CarHome on every start. Unknown keys (from newer versions) are kept.
     */
    private val TYPES: Map<String, String> = mapOf(
        "settings" to "s", "tiles" to "s", "tiles2" to "s", "places" to "s", "odometer_km" to "s",
        "oil_at_km" to "s", "launches" to "s", "scores" to "s", "trip" to "s", "last_trip" to "s",
        "trip_history" to "s", "car_reminders" to "s", "reminder_snooze" to "s", "alert_mode" to "s",
        "bubble_x" to "i", "bubble_y" to "i", "speed_limit" to "i", "speed_margin" to "i", "tile_count" to "i",
        "accent" to "l",
        "show_speed" to "b", "show_weather" to "b", "speed_alert" to "b", "use_24h" to "b", "use_kmh" to "b",
        "parked_only" to "set"
    )

    fun encode(values: Map<String, *>, appVersion: String): String {
        val data = JSONObject()
        for ((k, v) in values.toSortedMap()) {
            if (k in LOCAL_ONLY) continue
            val (t, jv) = when (v) {
                is String -> "s" to v
                is Int -> "i" to v
                is Long -> "l" to v
                is Float -> "f" to v.toDouble()
                is Boolean -> "b" to v
                is Set<*> -> "set" to JSONArray(v.filterIsInstance<String>().sorted())
                else -> continue
            }
            data.put(k, JSONObject().put("t", t).put("v", jv))
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("app", appVersion)
            .put("data", data)
            .toString()
    }

    /** The saved values, or null when this isn't a CarHome backup this version can read. */
    fun decode(text: String): Map<String, Any>? {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (root.optString("format") != FORMAT) return null
        if (root.optInt("version", 0) !in 1..VERSION) return null
        val data = root.optJSONObject("data") ?: return null
        val out = mutableMapOf<String, Any>()
        for (k in data.keys()) {
            if (k in LOCAL_ONLY) continue
            val e = data.optJSONObject(k) ?: continue
            val t = e.optString("t")
            if (TYPES[k]?.let { it != t } == true) continue   // wrong type for a known key
            val value: Any = runCatching {
                when (t) {
                    "s" -> e.getString("v")
                    "i" -> e.getInt("v")
                    "l" -> e.getLong("v")
                    "f" -> e.getDouble("v").toFloat()
                    "b" -> e.getBoolean("v")
                    "set" -> e.getJSONArray("v").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                    else -> null
                }
            }.getOrNull() ?: continue
            out[k] = value
        }
        // A backup must carry readable settings; without them restoring would just wipe everything
        val settings = out["settings"] as? String ?: return null
        if (runCatching { JSONObject(settings) }.isFailure) return null
        return out
    }
}
