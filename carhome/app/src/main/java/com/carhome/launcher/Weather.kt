package com.carhome.launcher

import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HourForecast(
    val hour: Int,          // 0..23, local time at the car's location
    val temperatureC: Double,
    val code: Int,
    val isDay: Boolean
)

data class WeatherInfo(
    val temperatureC: Double,
    val code: Int,
    val isDay: Boolean,
    val nextHours: List<HourForecast> = emptyList(),
    /** Hours until visibility drops under 1 km within the next 12 h (0 = now), or null. */
    val fogInHours: Int? = null
)

enum class WeatherKind(@StringRes val label: Int) {
    CLEAR(R.string.w_clear),
    PARTLY(R.string.w_partly),
    CLOUDY(R.string.w_cloudy),
    FOG(R.string.w_fog),
    DRIZZLE(R.string.w_drizzle),
    RAIN(R.string.w_rain),
    SNOW(R.string.w_snow),
    STORM(R.string.w_storm)
}

/** WMO weather codes as used by Open-Meteo. */
fun weatherKind(code: Int): WeatherKind = when (code) {
    0 -> WeatherKind.CLEAR
    1, 2 -> WeatherKind.PARTLY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    in 51..57 -> WeatherKind.DRIZZLE
    in 61..67, in 80..82 -> WeatherKind.RAIN
    in 71..77, 85, 86 -> WeatherKind.SNOW
    in 95..99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUDY
}

private fun httpGet(url: String): String? {
    val conn = URL(url).openConnection() as HttpURLConnection
    return try {
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "CarHome/1.2 (Android launcher)")
        if (conn.responseCode != 200) null else conn.inputStream.use { Net.readCapped(it, Net.MAX_WEATHER) }
    } finally {
        conn.disconnect()
    }
}

/** Free, key-less weather from Open-Meteo. Returns null on any failure. */
suspend fun fetchWeather(lat: Double, lon: Double): WeatherInfo? = withContext(Dispatchers.IO) {
    runCatching {
        val body = httpGet(
            String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f" +
                    "&current_weather=true&hourly=temperature_2m,weather_code,is_day,visibility" +
                    "&forecast_days=2&timezone=auto",
                lat, lon
            )
        ) ?: return@runCatching null
        val root = JSONObject(body)
        val cw = root.getJSONObject("current_weather")
        val now = cw.optString("time", "")
        WeatherInfo(
            temperatureC = cw.getDouble("temperature"),
            code = cw.getInt("weathercode"),
            isDay = cw.optInt("is_day", 1) == 1,
            nextHours = parseNextHours(root, now),
            fogInHours = firstHourBelow(root, now, "visibility", 1000.0)
        )
    }.getOrNull()
}

/**
 * Hours until dust in the air reaches a level that cuts visibility (≥ 150 µg/m³)
 * within the next 12 hours, from Open-Meteo's free air-quality forecast. 0 = now.
 */
/**
 * Hours until heavy dust (>= 150 µg/m³) within the next 12 h; success(null) when none.
 * A failure is reported as such, so the caller can keep the last answer. Times are
 * asked for as Unix time, so a box set to the wrong time zone can't shift the window.
 */
suspend fun fetchDustInHours(lat: Double, lon: Double): Result<Int?> = withContext(Dispatchers.IO) {
    runCatching {
        val body = httpGet(
            String.format(
                Locale.US,
                "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%.2f&longitude=%.2f" +
                    "&hourly=dust&forecast_days=2&timeformat=unixtime",
                lat, lon
            )
        ) ?: error("no reply")
        val hourly = JSONObject(body).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val dust = hourly.getJSONArray("dust")
        val nowHour = System.currentTimeMillis() / 1000L / 3600L * 3600L
        var ahead = 0
        var found: Int? = null
        for (i in 0 until times.length()) {
            if (times.getLong(i) < nowHour) continue
            if (ahead > 12) break
            if (!dust.isNull(i) && dust.getDouble(i) >= 150.0) { found = ahead; break }
            ahead++
        }
        found
    }
}

/** First hourly slot from the current hour on (within 12 h) where [key] < [limit]. */
private fun firstHourBelow(root: JSONObject, nowTime: String, key: String, limit: Double): Int? =
    runCatching {
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val values = hourly.optJSONArray(key) ?: return@runCatching null
        val nowHour = nowTime.take(13)   // "yyyy-MM-ddTHH"
        var ahead = 0
        for (i in 0 until times.length()) {
            val t = times.getString(i).take(13)
            if (t < nowHour) continue
            if (ahead > 12) break
            if (!values.isNull(i) && values.getDouble(i) < limit) return@runCatching ahead
            ahead++
        }
        null
    }.getOrNull()

/**
 * The 5 hourly slots after the current time. With timezone=auto every time is a
 * local "yyyy-MM-ddTHH:mm" string, so plain string comparison orders them.
 */
private fun parseNextHours(root: JSONObject, nowTime: String, count: Int = 5): List<HourForecast> =
    runCatching {
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weather_code")
        val days = hourly.optJSONArray("is_day")
        val out = mutableListOf<HourForecast>()
        for (i in 0 until times.length()) {
            val t = times.getString(i)
            if (t <= nowTime) continue
            out += HourForecast(
                hour = t.substring(11, 13).toInt(),
                temperatureC = temps.getDouble(i),
                code = codes.getInt(i),
                isDay = (days?.optInt(i, 1) ?: 1) == 1
            )
            if (out.size >= count) break
        }
        out.toList()
    }.getOrDefault(emptyList())
