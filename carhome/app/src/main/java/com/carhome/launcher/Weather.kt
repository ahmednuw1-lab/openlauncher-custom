package com.carhome.launcher

import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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
    val nextHours: List<HourForecast> = emptyList()
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

/** Free, key-less weather from Open-Meteo. Returns null on any failure. */
suspend fun fetchWeather(lat: Double, lon: Double): WeatherInfo? = withContext(Dispatchers.IO) {
    runCatching {
        val url = URL(
            String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                    "&current_weather=true&hourly=temperature_2m,weather_code,is_day" +
                    "&forecast_days=2&timezone=auto",
                lat, lon
            )
        )
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) return@runCatching null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val cw = root.getJSONObject("current_weather")
            WeatherInfo(
                temperatureC = cw.getDouble("temperature"),
                code = cw.getInt("weathercode"),
                isDay = cw.optInt("is_day", 1) == 1,
                nextHours = parseNextHours(root, cw.optString("time", ""))
            )
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

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
