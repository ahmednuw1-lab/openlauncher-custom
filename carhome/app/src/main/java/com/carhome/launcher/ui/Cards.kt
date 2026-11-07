package com.carhome.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.Astro
import com.carhome.launcher.GpsState
import com.carhome.launcher.HourForecast
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.LimitInfo
import com.carhome.launcher.Nearby
import com.carhome.launcher.NowPlaying
import com.carhome.launcher.ObdData
import com.carhome.launcher.ObdStatus
import com.carhome.launcher.Prayer
import com.carhome.launcher.PrayerInfo
import com.carhome.launcher.R
import com.carhome.launcher.SpeedStyle
import com.carhome.launcher.TripStats
import com.carhome.launcher.WeatherInfo
import com.carhome.launcher.WeatherKind
import com.carhome.launcher.prayerName
import com.carhome.launcher.speedInUnit
import com.carhome.launcher.weatherKind
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun appLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

fun formatTime(ms: Long, use24h: Boolean, locale: Locale): String =
    SimpleDateFormat(if (use24h) "HH:mm" else "h:mm a", locale).format(Date(ms))

/** "1h 25m" / "25m" style countdown. */
@Composable
fun countdown(ms: Long): String {
    val mins = ((ms + 59_999) / 60_000).coerceAtLeast(0)
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) stringResource(R.string.in_h_m, h.toInt(), m.toInt()) else stringResource(R.string.in_m, m.toInt())
}

/** Ticks every second; every card that shows time reads this. */
@Composable
fun rememberNow(stepMs: Long = 1_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(stepMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(stepMs - now % stepMs)
        }
    }
    return now
}

// ── Clock ─────────────────────────────────────────────────────────────────────

@Composable
fun ClockCard(settings: LauncherSettings, driving: Boolean, modifier: Modifier = Modifier) {
    val now = rememberNow()
    val locale = appLocale()
    val pattern = when {
        settings.use24h && settings.clockSeconds -> "HH:mm:ss"
        settings.use24h -> "HH:mm"
        settings.clockSeconds -> "h:mm:ss"
        else -> "h:mm"
    }
    val stepKey = if (settings.clockSeconds) now / 1_000L else now / 60_000L
    val time = remember(stepKey, pattern, locale) { SimpleDateFormat(pattern, locale).format(Date(now)) }
    val amPm = remember(stepKey, settings.use24h, locale) {
        if (settings.use24h) "" else SimpleDateFormat("a", locale).format(Date(now))
    }
    val date = remember(now / 60_000L, locale) {
        SimpleDateFormat(if (locale.language == "ar") "EEEE، d MMMM" else "EEEE, d MMMM", locale).format(Date(now))
    }
    val hijri = remember(now / 3_600_000L, locale, settings.showHijri) {
        if (settings.showHijri) Astro.hijriDate(now, locale) else null
    }
    Column(
        modifier = modifier.glass().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(time, fontSize = if (settings.clockSeconds) 58.sp else 72.sp, fontWeight = FontWeight.Light, color = TextPrimary, lineHeight = 76.sp, maxLines = 1)
            if (amPm.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(amPm, fontSize = 22.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))
            }
        }
        if (settings.showDate && !driving) Text(date, fontSize = 18.sp, color = TextSecondary, fontWeight = FontWeight.Medium, maxLines = 1)
        if (hijri != null && !driving) Text(hijri, fontSize = 15.sp, color = TextSecondary, maxLines = 1)
    }
}

// ── Weather ───────────────────────────────────────────────────────────────────

fun weatherIcon(kind: WeatherKind, isDay: Boolean): ImageVector = when (kind) {
    WeatherKind.CLEAR -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    WeatherKind.PARTLY -> Icons.Rounded.WbCloudy
    WeatherKind.CLOUDY, WeatherKind.FOG -> Icons.Rounded.Cloud
    WeatherKind.DRIZZLE -> Icons.Rounded.Grain
    WeatherKind.RAIN -> Icons.Rounded.Umbrella
    WeatherKind.SNOW -> Icons.Rounded.AcUnit
    WeatherKind.STORM -> Icons.Rounded.FlashOn
}

private fun hourLabel(hour: Int, use24h: Boolean, locale: Locale): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
    }
    return SimpleDateFormat(if (use24h) "HH:mm" else "h a", locale).format(cal.time)
}

/** Current weather plus the next 5 hours, as one glass pill across the top. */
@Composable
fun WeatherStrip(
    weather: WeatherInfo?,
    hasLocation: Boolean,
    use24h: Boolean,
    accent: Color,
    compact: Boolean,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = appLocale()
    Row(
        modifier = modifier
            .height(64.dp)
            .glass(RoundedCornerShape(32.dp))
            .let { if (!hasLocation) it.clickable(onClick = onRequestLocation) else it }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            !hasLocation -> {
                Icon(Icons.Rounded.LocationOn, null, tint = accent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.weather_off), color = TextSecondary, fontSize = 15.sp, maxLines = 1)
            }
            weather == null -> {
                Icon(Icons.Rounded.Cloud, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.weather_loading), color = TextSecondary, fontSize = 15.sp, maxLines = 1)
            }
            else -> {
                val kind = weatherKind(weather.code)
                Icon(weatherIcon(kind, weather.isDay), null, tint = accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text("${weather.temperatureC.roundToInt()}°", fontSize = 28.sp, fontWeight = FontWeight.Light, color = TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(kind.label), fontSize = 14.sp, color = TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                if (weather.nextHours.isNotEmpty() && !compact) {
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.width(1.dp).height(34.dp).background(GlassStroke))
                    Spacer(Modifier.width(6.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        weather.nextHours.forEach { h -> HourCell(h, use24h, locale) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourCell(h: HourForecast, use24h: Boolean, locale: Locale) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(hourLabel(h.hour, use24h, locale), fontSize = 11.sp, color = TextSecondary, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(weatherIcon(weatherKind(h.code), h.isDay), null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text("${h.temperatureC.roundToInt()}°", fontSize = 14.sp, color = TextPrimary)
        }
    }
}

/** Bigger weather card for the info column, with any dust or fog warning. */
@Composable
fun WeatherCard(
    weather: WeatherInfo?,
    dustInHours: Int?,
    hasLocation: Boolean,
    use24h: Boolean,
    accent: Color,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = appLocale()
    BoxWithConstraints(
        modifier = modifier.glass().let { if (!hasLocation) it.clickable(onClick = onRequestLocation) else it }.padding(18.dp)
    ) {
        val tall = maxHeight >= 190.dp
        when {
            !hasLocation -> CardMessage(Icons.Rounded.LocationOn, stringResource(R.string.weather_off), accent)
            weather == null -> CardMessage(Icons.Rounded.Cloud, stringResource(R.string.weather_loading), TextSecondary)
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                val kind = weatherKind(weather.code)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(weatherIcon(kind, weather.isDay), null, tint = accent, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("${weather.temperatureC.roundToInt()}°", fontSize = 46.sp, fontWeight = FontWeight.Light, color = TextPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(kind.label), fontSize = 16.sp, color = TextSecondary, maxLines = 2)
                }
                val warning = when {
                    dustInHours != null -> if (dustInHours == 0) stringResource(R.string.dust_now) else stringResource(R.string.dust_in, dustInHours)
                    weather.fogInHours != null -> if (weather.fogInHours == 0) stringResource(R.string.fog_now) else stringResource(R.string.fog_in, weather.fogInHours)
                    else -> null
                }
                if (warning != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Air, null, tint = WarnAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(warning, fontSize = 13.sp, color = WarnAmber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (tall && weather.nextHours.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        weather.nextHours.forEach { h -> HourCell(h, use24h, locale) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardMessage(icon: ImageVector, text: String, tint: Color) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = TextSecondary, fontSize = 15.sp, maxLines = 2)
    }
}

// ── Speed ─────────────────────────────────────────────────────────────────────

/** Round road sign with the posted limit (red ring on white). */
@Composable
fun RoadSign(limit: Int, size: androidx.compose.ui.unit.Dp = 46.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(size * 0.12f, AlertRed, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$limit",
            color = Color(0xFF111111),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * if (limit >= 100) 0.34f else 0.42f).sp
        )
    }
}

/** Live GPS speed with a 270° arc gauge (or a big number); red while over the limit. */
@Composable
fun SpeedCard(
    speedMps: Float?,
    gpsState: GpsState,
    settings: LauncherSettings,
    limit: LimitInfo,
    overspeed: Boolean,
    accent: Color,
    satellites: Int,
    fromCar: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(if (overspeed) AlertRed.copy(alpha = 0.28f) else Color.Transparent, label = "overBg")
    Box(
        modifier = modifier
            .glass()
            .background(bg)
            .let {
                if (gpsState == GpsState.NO_PERMISSION || gpsState == GpsState.LOCATION_OFF) it.clickable(onClick = onRequestPermission) else it
            }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        when (gpsState) {
            GpsState.NO_PERMISSION -> CardMessage(Icons.Rounded.Speed, stringResource(R.string.speed_needs_gps), accent)
            GpsState.LOCATION_OFF -> CardMessage(Icons.Rounded.LocationOn, stringResource(R.string.location_off), WarnAmber)
            GpsState.NO_HARDWARE -> CardMessage(Icons.Rounded.Speed, stringResource(R.string.no_gps_hw), TextSecondary)
            else -> {
                val value = speedMps?.let { speedInUnit(it, settings.useKmh) }
                val shown by animateFloatAsState(value ?: 0f, tween(700), label = "speed")
                val maxScale = if (settings.useKmh) 200f else 130f
                val arcColor = if (overspeed) AlertRed else accent
                val unit = stringResource(if (settings.useKmh) R.string.kmh else R.string.mph)
                val gauge = settings.speedStyle == SpeedStyle.GAUGE

                if (gauge) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 10.dp.toPx()
                        val d = minOf(size.width, size.height) - stroke
                        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                        val arcSize = Size(d, d)
                        drawArc(
                            color = Color.White.copy(alpha = 0.08f), startAngle = 135f, sweepAngle = 270f,
                            useCenter = false, topLeft = topLeft, size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        val frac = (shown / maxScale).coerceIn(0f, 1f)
                        if (frac > 0f) {
                            drawArc(
                                color = arcColor, startAngle = 135f, sweepAngle = 270f * frac,
                                useCenter = false, topLeft = topLeft, size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (value == null) "—" else shown.roundToInt().toString(),
                        fontSize = if (gauge) 54.sp else 84.sp,
                        fontWeight = FontWeight.Light,
                        color = if (overspeed) AlertRed else TextPrimary,
                        lineHeight = if (gauge) 56.sp else 86.sp
                    )
                    Text(if (fromCar) "$unit · ${stringResource(R.string.from_car)}" else unit, fontSize = 15.sp, color = TextSecondary)
                    when {
                        value == null ->
                            Text(stringResource(R.string.waiting_gps_sats, satellites), fontSize = 12.sp, color = TextSecondary)
                        overspeed ->
                            Text(stringResource(R.string.over_limit), fontSize = 13.sp, color = AlertRed, fontWeight = FontWeight.SemiBold)
                        settings.speedAlert && limit.limit != null ->
                            Text(stringResource(R.string.limit_short, limit.limit), fontSize = 12.sp, color = TextSecondary)
                        settings.speedAlert && settings.roadLimit ->
                            Text(stringResource(R.string.no_road_limit), fontSize = 12.sp, color = TextSecondary)
                    }
                }
                if (settings.roadLimit && limit.roadLimit != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                        RoadSign(limit.roadLimit)
                    }
                }
            }
        }
    }
}

// ── Music ─────────────────────────────────────────────────────────────────────

@Composable
fun MusicCard(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    accent: Color,
    driving: Boolean,
    doubleTapToPause: Boolean,
    onRequestAccess: () -> Unit,
    onOpenApp: (String) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .glass()
            .let { m ->
                if (doubleTapToPause && hasAccess) m.pointerInput(Unit) { detectTapGestures(onDoubleTap = { onPlayPause() }) } else m
            }
    ) {
        val compact = maxHeight < 210.dp
        val art = nowPlaying?.art
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.85f)))
                )
            )
        }

        val titleBlock: @Composable (Modifier) -> Unit = { m ->
            AnimatedContent(
                targetState = nowPlaying?.title.orEmpty() to nowPlaying?.artist.orEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "track",
                modifier = m
            ) { (title, artist) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { mm ->
                            val pkg = nowPlaying?.appPackage
                            if (!pkg.isNullOrEmpty()) mm.clickable { onOpenApp(pkg) } else mm
                        }
                ) {
                    Text(
                        text = title.ifEmpty { stringResource(R.string.nothing_playing) },
                        fontSize = if (compact) 17.sp else 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artist.isNotEmpty()) {
                        Text(artist, fontSize = 15.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        val big = if (driving) 84.dp else if (compact) 60.dp else 72.dp
        val small = if (driving) 64.dp else if (compact) 48.dp else 56.dp
        val controls: @Composable () -> Unit = {
            GlassIconButton(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), onPrevious, size = small)
            GlassIconButton(
                icon = if (nowPlaying?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.play_pause),
                onClick = onPlayPause,
                size = big,
                tint = Color.White,
                filled = accent
            )
            GlassIconButton(Icons.Rounded.SkipNext, stringResource(R.string.next), onNext, size = small)
        }

        when {
            !hasAccess -> Column(
                modifier = Modifier.fillMaxSize().clickable(onClick = onRequestAccess).padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.enable_music), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (!compact) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.enable_music_hint), fontSize = 14.sp, color = TextSecondary)
                }
            }
            compact -> Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                titleBlock(Modifier.weight(1f))
                controls()
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                titleBlock(Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) { controls() }
            }
        }
    }
}

// ── Prayer ────────────────────────────────────────────────────────────────────

/** Small "next prayer" pill for the top bar. */
@Composable
fun PrayerChip(prayer: PrayerInfo?, use24h: Boolean, accent: Color, modifier: Modifier = Modifier) {
    val now = rememberNow(30_000L)
    val locale = appLocale()
    Row(
        modifier = modifier.height(64.dp).glass(RoundedCornerShape(32.dp)).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Mosque, null, tint = accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        if (prayer == null) {
            Text(stringResource(R.string.prayer_waiting), fontSize = 14.sp, color = TextSecondary, maxLines = 1)
        } else {
            Column {
                Text(
                    "${stringResource(prayerName(prayer.next.prayer))} ${formatTime(prayer.next.at, use24h, locale)}",
                    fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1
                )
                Text(countdown(prayer.next.at - now), fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
fun PrayerCard(
    prayer: PrayerInfo?,
    use24h: Boolean,
    accent: Color,
    nearbyButtons: Boolean,
    onNearby: (Nearby) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = rememberNow(30_000L)
    val locale = appLocale()
    BoxWithConstraints(modifier = modifier.glass().padding(horizontal = 18.dp, vertical = 12.dp)) {
        val tall = maxHeight >= 120.dp
        if (prayer == null) {
            CardMessage(Icons.Rounded.Mosque, stringResource(R.string.prayer_waiting), accent)
        } else {
            val next = prayer.next
            val soon = next.at - now in 0..(30 * 60_000L)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Mosque, null, tint = accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.next_prayer), fontSize = 12.sp, color = TextSecondary, lineHeight = 14.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(stringResource(prayerName(next.prayer)), fontSize = 22.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(formatTime(next.at, use24h, locale), fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Light, lineHeight = 26.sp)
                        }
                    }
                    Text(countdown(next.at - now), fontSize = 14.sp, color = if (soon) accent else TextSecondary)
                }
                if (nearbyButtons && soon) {
                    SmallPill(Icons.Rounded.Mosque, stringResource(R.string.nearest_mosque), accent) { onNearby(Nearby.MOSQUE) }
                }
                if (tall) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Prayer.entries.forEach { p ->
                            val isNext = p == next.prayer && Calendar.getInstance().apply { timeInMillis = next.at }
                                .get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(prayerName(p)), fontSize = 12.sp, color = if (isNext) accent else TextSecondary, maxLines = 1)
                                Text(
                                    formatTime(prayer.day.timeOf(p), use24h, locale).replace(" ", " "),
                                    fontSize = 14.sp, color = if (isNext) accent else TextPrimary, maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallPill(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(accent.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ── Trip ──────────────────────────────────────────────────────────────────────

@Composable
fun distanceText(metres: Double, useKmh: Boolean): String {
    val v = if (useKmh) metres / 1000.0 else metres / 1609.344
    val unit = stringResource(if (useKmh) R.string.km else R.string.mi)
    return String.format(appLocale(), if (v < 100) "%.1f %s" else "%.0f %s", v, unit)
}

@Composable
fun durationText(ms: Long): String {
    val mins = ms / 60_000
    return String.format(appLocale(), "%d:%02d", mins / 60, mins % 60)
}

@Composable
fun TripCard(
    trip: TripStats,
    lastTrip: TripStats?,
    settings: LauncherSettings,
    accent: Color,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unit = stringResource(if (settings.useKmh) R.string.kmh else R.string.mph)
    BoxWithConstraints(modifier = modifier.glass().padding(16.dp)) {
        val tall = maxHeight >= 200.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.this_trip), fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                if (settings.driveScore) {
                    trip.score?.let { sc ->
                        Text(stringResource(R.string.score_short, sc), fontSize = 13.sp, color = scoreColor(sc), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Box(
                    Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onReset),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Refresh, stringResource(R.string.new_trip), tint = TextSecondary, modifier = Modifier.size(20.dp)) }
            }
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.distance), distanceText(trip.distanceM, settings.useKmh), Modifier.weight(1f))
                Stat(stringResource(R.string.drive_time), durationText(trip.movingMs), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                val loc = appLocale()
                Stat(stringResource(R.string.avg_speed), String.format(loc, "%d %s", speedInUnit(trip.avgMps, settings.useKmh).roundToInt(), unit), Modifier.weight(1f))
                Stat(stringResource(R.string.top_speed), String.format(loc, "%d %s", speedInUnit(trip.maxMps, settings.useKmh).roundToInt(), unit), Modifier.weight(1f))
            }
            if (settings.fuelCost) {
                val litres = trip.distanceM / 1000.0 * settings.fuelConsumption / 100.0
                Text(
                    stringResource(R.string.fuel_cost_value, String.format(appLocale(), "%.2f", litres * settings.fuelPrice)),
                    fontSize = 13.sp, color = accent, maxLines = 1
                )
            }
            if (tall && lastTrip != null) {
                Text(
                    stringResource(R.string.last_trip_line, distanceText(lastTrip.distanceM, settings.useKmh), durationText(lastTrip.movingMs)),
                    fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun scoreColor(score: Int): Color = when {
    score >= 85 -> OkGreen
    score >= 65 -> WarnAmber
    else -> AlertRed
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
        Text(value, fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ── Car data (OBD-II) ─────────────────────────────────────────────────────────

@Composable
fun CarCard(
    status: ObdStatus,
    data: ObdData,
    enabled: Boolean,
    accent: Color,
    onReadCodes: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.glass().padding(16.dp)) {
        if (!enabled || status != ObdStatus.CONNECTED) {
            val msg = when {
                !enabled -> R.string.obd_off
                status == ObdStatus.NO_BLUETOOTH -> R.string.obd_no_bt
                status == ObdStatus.NO_PERMISSION -> R.string.obd_no_permission
                status == ObdStatus.CHOOSE_ADAPTER -> R.string.obd_choose
                status == ObdStatus.CONNECTING -> R.string.obd_connecting
                else -> R.string.obd_failed
            }
            Column(
                Modifier.fillMaxSize().clickable(onClick = onOpenSettings),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Bluetooth, null, tint = accent, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(msg), fontSize = 14.sp, color = TextSecondary, maxLines = 2)
            }
            return@Box
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.rpm), data.rpm?.toString() ?: "—", Modifier.weight(1f))
                Stat(stringResource(R.string.engine_temp), data.coolantC?.let { "$it°C" } ?: "—", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.fuel_level), data.fuelPercent?.let { "$it%" } ?: "—", Modifier.weight(1f))
                Stat(stringResource(R.string.battery), data.voltage?.let { String.format(Locale.US, "%.1f V", it) } ?: "—", Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val codes = data.codes
                Text(
                    when {
                        codes == null -> stringResource(R.string.codes_not_read)
                        codes.isEmpty() -> stringResource(R.string.codes_none)
                        else -> stringResource(R.string.codes_found, codes.joinToString(", "))
                    },
                    fontSize = 13.sp,
                    color = if (codes.isNullOrEmpty()) TextSecondary else WarnAmber,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                SmallPill(Icons.Rounded.Refresh, stringResource(R.string.read_codes), accent, onReadCodes)
            }
        }
    }
}
