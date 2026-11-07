package com.carhome.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.Astro
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.PrayerInfo
import com.carhome.launcher.R
import com.carhome.launcher.TripStats
import com.carhome.launcher.WeatherInfo
import com.carhome.launcher.prayerName
import com.carhome.launcher.weatherKind
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

/** A few seconds of "Good morning" when the car starts. Tap to skip. */
@Composable
fun WelcomeOverlay(
    visible: Boolean,
    settings: LauncherSettings,
    weather: WeatherInfo?,
    prayer: PrayerInfo?,
    lastTrip: TripStats?,
    accent: Color,
    onDismiss: () -> Unit
) {
    // Timed from the first frame on screen, so a slow start doesn't eat the greeting
    LaunchedEffect(visible) {
        if (visible) { delay(4_500); onDismiss() }
    }
    AnimatedVisibility(visible, enter = fadeIn(tween(500)), exit = fadeOut(tween(800))) {
        val locale = appLocale()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = stringResource(
            when (hour) {
                in 4..11 -> R.string.good_morning
                in 12..16 -> R.string.good_afternoon
                else -> R.string.good_evening
            }
        )
        val name = settings.welcomeName.trim()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2070A12))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (name.isEmpty()) greeting else stringResource(R.string.greeting_name, greeting, name),
                    fontSize = 52.sp, fontWeight = FontWeight.Light, color = TextPrimary
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    weather?.let { w ->
                        val kind = weatherKind(w.code)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(weatherIcon(kind, w.isDay), null, tint = accent, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("${w.temperatureC.roundToInt()}°  ${stringResource(kind.label)}", fontSize = 24.sp, color = TextSecondary)
                        }
                    }
                    prayer?.let { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Mosque, null, tint = accent, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${stringResource(prayerName(p.next.prayer))} ${formatTime(p.next.at, settings.use24h, locale)}",
                                fontSize = 24.sp, color = TextSecondary
                            )
                        }
                    }
                    lastTrip?.let { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Route, null, tint = accent, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.last_trip_line, distanceText(t.distanceM, settings.useKmh), durationText(t.movingMs)),
                                fontSize = 22.sp, color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Calm full-screen display after a few minutes parked. Any touch brings the home screen back. */
@Composable
fun ParkedOverlay(
    visible: Boolean,
    settings: LauncherSettings,
    weather: WeatherInfo?,
    prayer: PrayerInfo?,
    accent: Color,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible, enter = fadeIn(tween(1200)), exit = fadeOut(tween(300))) {
        val now = rememberNow()
        val locale = appLocale()
        val time = SimpleDateFormat(if (settings.use24h) "HH:mm" else "h:mm", locale).format(Date(now))
        val date = SimpleDateFormat(if (locale.language == "ar") "EEEE، d MMMM" else "EEEE, d MMMM", locale).format(Date(now))
        val hijri = remember(now / 3_600_000L, locale) { Astro.hijriDate(now, locale) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF03050A))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time, fontSize = 150.sp, fontWeight = FontWeight.Thin, color = TextPrimary, lineHeight = 150.sp)
                Text(date, fontSize = 24.sp, color = TextSecondary)
                if (hijri != null && settings.showHijri) Text(hijri, fontSize = 20.sp, color = TextSecondary)
                Spacer(Modifier.height(36.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    weather?.let { w ->
                        val kind = weatherKind(w.code)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(weatherIcon(kind, w.isDay), null, tint = accent, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("${w.temperatureC.roundToInt()}°", fontSize = 30.sp, fontWeight = FontWeight.Light, color = TextPrimary)
                        }
                    }
                    prayer?.let { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Mosque, null, tint = accent, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "${stringResource(prayerName(p.next.prayer))} ${formatTime(p.next.at, settings.use24h, locale)}",
                                    fontSize = 24.sp, color = TextPrimary
                                )
                                Text(countdown(p.next.at - now), fontSize = 15.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Night mode: a dark layer over everything. Touches pass straight through. */
@Composable
fun NightDim(active: Boolean, dimPercent: Int) {
    if (!active) return
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (dimPercent / 100f) * 0.8f)))
}
