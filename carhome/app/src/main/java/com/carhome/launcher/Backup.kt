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

    /** Kept per box: brightness before night mode, update bookkeeping, reminders already given. */
    val LOCAL_ONLY = setOf(
        "saved_brightness", "saved_brightness_mode", "update_checked_at", "dismissed_update",
        "last_version_code", "reminded_prayer_at"
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
            val value: Any = runCatching {
                when (e.optString("t")) {
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
        return out
    }
}
