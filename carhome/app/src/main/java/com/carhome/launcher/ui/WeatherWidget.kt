package com.carhome.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.LookMath
import com.carhome.launcher.R
import com.carhome.launcher.WeatherInfo
import com.carhome.launcher.weatherKind
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/** One point of the forecast: now (hour = null) or a coming hour. */
private data class Slot(val hour: Int?, val temp: Double, val code: Int, val isDay: Boolean)

/**
 * Weather for the custom home, laid out for the size it is given:
 * one row high → the weather now, and the coming hours beside it;
 * taller → the weather now on top, and under it a temperature curve from now on.
 */
@Composable
internal fun WeatherWidget(st: HomeState, act: HomeActions, modifier: Modifier) {
    val s = st.settings
    val weather = st.weather
    BoxWithConstraints(
        modifier.glass().let { if (!st.hasLocation) it.clickable(onClick = act.requestLocation) else it }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        val roomH = maxHeight
        when {
            !st.hasLocation -> CardMessage(Icons.Rounded.LocationOn, stringResource(R.string.weather_off), st.accent)
            weather == null -> CardMessage(Icons.Rounded.Cloud, stringResource(R.string.weather_loading), TextSecondary)
            else -> {
                val warning = weatherWarning(weather, if (s.dustWarning) st.dustInHours else null)
                if (roomH >= 140.dp && weather.nextHours.isNotEmpty()) WeatherTall(weather, warning, s.use24h, st.accent)
                else WeatherRow(weather, warning, s.use24h)
            }
        }
    }
}

// ── One row high ──────────────────────────────────────────────────────────────

@Composable
private fun WeatherRow(w: WeatherInfo, warning: String?, use24h: Boolean) {
    val kind = weatherKind(w.code)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val low = maxHeight < 70.dp
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(weatherIcon(kind, w.isDay), null, tint = weatherTint(kind, w.isDay), modifier = Modifier.size(if (low) 28.dp else 36.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.widthIn(max = 130.dp)) {
                val t = if (low) 28.sp else 36.sp
                Text("${w.temperatureC.roundToInt()}°", fontSize = t, lineHeight = t * 1.1f, fontWeight = FontWeight.Light, color = TextPrimary, maxLines = 1)
                Text(
                    warning ?: stringResource(kind.label), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (warning != null) WarnAmber else TextSecondary
                )
            }
            if (w.nextHours.isNotEmpty()) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    // As many coming hours as fit beside the weather now
                    val n = LookMath.hourCells((maxWidth - 16.dp).value, 60f).coerceAtMost(6)
                    if (n > 0) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(14.dp))
                            Box(Modifier.width(1.dp).fillMaxHeight(0.7f).background(TextSecondary.copy(alpha = 0.25f)))
                            Row(
                                Modifier.weight(1f).fillMaxHeight(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                w.nextHours.take(n).forEach { h ->
                                    HourColumn(Slot(h.hour, h.temperatureC, h.code, h.isDay), use24h, low)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourColumn(slot: Slot, use24h: Boolean, low: Boolean) {
    val kind = weatherKind(slot.code)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HourText(slot, use24h, TextSecondary, 13.sp)
        if (low) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(weatherIcon(kind, slot.isDay), null, tint = weatherTint(kind, slot.isDay), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(3.dp))
                Text("${slot.temp.roundToInt()}°", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        } else {
            Spacer(Modifier.height(3.dp))
            Icon(weatherIcon(kind, slot.isDay), null, tint = weatherTint(kind, slot.isDay), modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text("${slot.temp.roundToInt()}°", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

// ── Two rows or more ──────────────────────────────────────────────────────────

@Composable
private fun WeatherTall(w: WeatherInfo, warning: String?, use24h: Boolean, accent: Color) {
    val kind = weatherKind(w.code)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val big = maxHeight >= 250.dp
        val cols = LookMath.hourCells(maxWidth.value, 64f).coerceIn(2, 8)
        val slots = remember(w, cols) {
            listOf(Slot(null, w.temperatureC, w.code, w.isDay)) +
                w.nextHours.take(cols - 1).map { Slot(it.hour, it.temperatureC, it.code, it.isDay) }
        }
        Column(Modifier.fillMaxSize()) {
            // The weather now
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(weatherIcon(kind, w.isDay), null, tint = weatherTint(kind, w.isDay), modifier = Modifier.size(if (big) 52.dp else 40.dp))
                Spacer(Modifier.width(12.dp))
                val t = if (big) 54.sp else 42.sp
                Text("${w.temperatureC.roundToInt()}°", fontSize = t, lineHeight = t * 1.05f, fontWeight = FontWeight.Light, color = TextPrimary, maxLines = 1)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(kind.label), fontSize = if (big) 19.sp else 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (warning != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Air, null, tint = WarnAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(warning, fontSize = 13.sp, color = WarnAmber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            TempCurve(slots, accent, Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth()) {
                slots.forEach { s ->
                    val k = weatherKind(s.code)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Icon(weatherIcon(k, s.isDay), null, tint = weatherTint(k, s.isDay), modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                slots.forEach { s ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        HourText(s, use24h, if (s.hour == null) accent else TextSecondary, 14.sp)
                    }
                }
            }
        }
    }
}

/** A smooth line through the temperatures, with each one written above its point. */
@Composable
private fun TempCurve(slots: List<Slot>, accent: Color, modifier: Modifier) {
    val levels = remember(slots) { LookMath.tempLevels(slots.map { it.temp }) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(modifier) {
        val labelH = 22.dp
        val top = labelH + 2.dp
        val usable = (maxHeight - top - 8.dp).coerceAtLeast(0.dp)
        fun yOf(i: Int): Dp = top + usable * (1f - levels[i])
        Canvas(Modifier.fillMaxSize()) {
            val n = slots.size
            if (n == 0) return@Canvas
            val colW = size.width / n
            val pts = List(n) { i ->
                val x = colW * (i + 0.5f)
                Offset(if (rtl) size.width - x else x, yOf(i).toPx())
            }
            fun Path.through() {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until n) {
                    val mx = (pts[i - 1].x + pts[i].x) / 2f
                    cubicTo(mx, pts[i - 1].y, mx, pts[i].y, pts[i].x, pts[i].y)
                }
            }
            val line = Path().apply { through() }
            val area = Path().apply {
                through()
                lineTo(pts.last().x, size.height)
                lineTo(pts.first().x, size.height)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0f)), startY = top.toPx(), endY = size.height)
            )
            drawPath(line, accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            pts.forEachIndexed { i, p ->
                drawCircle(accent, radius = (if (i == 0) 6.dp else 5.dp).toPx(), center = p)
                drawCircle(Color.White, radius = (if (i == 0) 3.dp else 2.5.dp).toPx(), center = p)
            }
        }
        Row(Modifier.fillMaxSize()) {
            slots.forEachIndexed { i, s ->
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Text(
                        "${s.temp.roundToInt()}°",
                        fontSize = 16.sp, maxLines = 1, color = TextPrimary,
                        fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = yOf(i) - labelH - 2.dp)
                    )
                }
            }
        }
    }
}

/** "Now", or the hour: "4 PM" / "4 م" with the PM smaller, or "16:00". Always Western digits. */
@Composable
private fun HourText(slot: Slot, use24h: Boolean, color: Color, size: TextUnit) {
    val hour = slot.hour
    if (hour == null) {
        Text(stringResource(R.string.forecast_now), fontSize = size, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
        return
    }
    if (use24h) {
        Text(String.format(Locale.ROOT, "%02d:00", hour), fontSize = size, color = color, maxLines = 1)
        return
    }
    val locale = appLocale()
    val period = remember(hour, locale) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0) }
        SimpleDateFormat("a", locale).format(cal.time)
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(((hour + 11) % 12 + 1).toString(), fontSize = size, color = color, maxLines = 1)
        Spacer(Modifier.width(2.dp))
        Text(period, fontSize = size * 0.78f, color = color, maxLines = 1, modifier = Modifier.padding(bottom = 1.dp))
    }
}
