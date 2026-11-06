package com.carhome.launcher

import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherInfo(
    val temperatureC: Double,
    val code: Int,
    val isDay: Boolean
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
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true",
                lat, lon
            )
        )
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) return@runCatching null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val cw = JSONObject(body).getJSONObject("current_weather")
            WeatherInfo(
                temperatureC = cw.getDouble("temperature"),
                code = cw.getInt("weathercode"),
                isDay = cw.optInt("is_day", 1) == 1
            )
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}
