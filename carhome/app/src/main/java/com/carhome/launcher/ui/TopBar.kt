package com.carhome.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.Astro
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.LookMath
import com.carhome.launcher.NowPlaying
import com.carhome.launcher.R
import com.carhome.launcher.TripStats
import com.carhome.launcher.WeatherInfo
import com.carhome.launcher.speedInUnit
import com.carhome.launcher.weatherKind
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

// The pill at the top of the home screen. What it shows is the user's choice
// (Settings → Home screen → Top bar): weather, next prayer, the date, the song
// playing, the trip, or nothing.

private val PillHeight = 64.dp
private val PillShape = RoundedCornerShape(32.dp)

/**
 * Current weather plus the coming hours, as one glass pill. It shows as many
 * hours as the screen has room for (fewer on a narrow screen, more on a wide
 * one) and the rest of the next 24 hours scroll: swipe sideways, or tap the arrow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeatherStrip(
    weather: WeatherInfo?,
    hasLocation: Boolean,
    use24h: Boolean,
    accent: Color,
    hideHours: Boolean,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = appLocale()
    BoxWithConstraints(
        modifier = modifier
            .height(PillHeight)
            .glass(PillShape)
            .let { if (!hasLocation) it.clickable(onClick = onRequestLocation) else it }
    ) {
        val pill = maxWidth
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                !hasLocation -> {
                    Icon(Icons.Rounded.LocationOn, null, tint = accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.weather_off), color = TextSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                weather == null -> {
                    Icon(Icons.Rounded.Cloud, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.weather_loading), color = TextSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                else -> {
                    val kind = weatherKind(weather.code)
                    Icon(weatherIcon(kind, weather.isDay), null, tint = accent, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("${weather.temperatureC.roundToInt()}°", fontSize = 28.sp, fontWeight = FontWeight.Light, color = TextPrimary, maxLines = 1)
                    val hours = if (hideHours) emptyList() else weather.nextHours
                    // The words ("Partly cloudy") only when there's room for them and two hours beside them
                    val showLabel = hours.isEmpty() || pill >= 430.dp
                    if (showLabel) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(kind.label), fontSize = 14.sp, color = TextSecondary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = if (hours.isEmpty()) Modifier.weight(1f) else Modifier.widthIn(max = 120.dp)
                        )
                    }
                    if (hours.isNotEmpty()) {
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.width(1.dp).height(34.dp).background(GlassStroke))
                        Spacer(Modifier.width(4.dp))
                        HourStrip(hours, use24h, locale, Modifier.weight(1f).fillMaxHeight())
                    } else if (!showLabel) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** As many whole hours as fit; the rest scroll, with an arrow to step through them. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HourStrip(hours: List<com.carhome.launcher.HourForecast>, use24h: Boolean, locale: java.util.Locale, modifier: Modifier) {
    val arrow = 52.dp
    BoxWithConstraints(modifier) {
        val room = maxWidth - arrow
        val fit = LookMath.hourCells(room.value)
        if (fit < 1) return@BoxWithConstraints   // no room for even one hour: just the current weather
        val cell = room / fit
        val list = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val more by remember { derivedStateOf { list.canScrollForward } }
        val scrollable = hours.size > fit
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                state = list,
                flingBehavior = rememberSnapFlingBehavior(list),
                userScrollEnabled = scrollable,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(room).fillMaxHeight()
            ) {
                items(hours) { h ->
                    Box(Modifier.width(cell), contentAlignment = Alignment.Center) { HourCell(h, use24h, locale) }
                }
            }
            if (scrollable) {
                // Forward one screenful; at the end, back to now
                Box(
                    modifier = Modifier.width(arrow).fillMaxHeight().clip(CircleShape).clickable {
                        scope.launch {
                            if (more) list.animateScrollToItem((list.firstVisibleItemIndex + fit).coerceAtMost(hours.size - 1))
                            else list.animateScrollToItem(0)
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (more) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        stringResource(if (more) R.string.more_hours else R.string.back_to_now),
                        tint = TextSecondary, modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/** Today's date, with the Hijri date beside it. */
@Composable
fun DatePill(accent: Color, modifier: Modifier = Modifier) {
    val now = rememberNow(30_000L)
    val locale = appLocale()
    val date = remember(now / 60_000L, locale) {
        SimpleDateFormat(if (locale.language == "ar") "EEEE، d MMMM" else "EEEE, d MMMM", locale).format(Date(now))
    }
    val hijri = remember(now / 3_600_000L, locale) { Astro.hijriDate(now, locale) }
    Row(
        modifier = modifier.height(PillHeight).glass(PillShape).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(date, fontSize = 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hijri != null) Text(hijri, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The song playing, with previous / play-pause / next. */
@Composable
fun MusicPill(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    accent: Color,
    onRequestAccess: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(PillHeight)
            .glass(PillShape)
            .let { if (!hasAccess) it.clickable(onClick = onRequestAccess) else it }
            .padding(start = 20.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        if (!hasAccess) {
            Text(stringResource(R.string.enable_music_hint), fontSize = 15.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            return@Row
        }
        Column(Modifier.weight(1f)) {
            Text(
                nowPlaying?.title?.ifEmpty { null } ?: stringResource(R.string.nothing_playing),
                fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val artist = nowPlaying?.artist.orEmpty()
            if (artist.isNotEmpty()) Text(artist, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Never mirrored: previous on the left, next on the right, as on every car stereo
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), onPrevious, size = 52.dp)
                Spacer(Modifier.width(6.dp))
                GlassIconButton(
                    icon = if (nowPlaying?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.play_pause),
                    onClick = onPlayPause, size = 52.dp, tint = onAccent(accent), filled = accent
                )
                Spacer(Modifier.width(6.dp))
                GlassIconButton(Icons.Rounded.SkipNext, stringResource(R.string.next), onNext, size = 52.dp)
            }
        }
    }
}

/** This trip in one line: distance, driving time and average speed. */
@Composable
fun TripPill(trip: TripStats, settings: LauncherSettings, accent: Color, modifier: Modifier = Modifier) {
    val unit = stringResource(if (settings.useKmh) R.string.kmh else R.string.mph)
    val avg = String.format(appLocale(), "%d %s", speedInUnit(trip.avgMps, settings.useKmh).roundToInt(), unit)
    Row(
        modifier = modifier.height(PillHeight).glass(PillShape).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Route, null, tint = accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        PillStat(stringResource(R.string.distance), distanceText(trip.distanceM, settings.useKmh), Modifier.weight(1f))
        PillStat(stringResource(R.string.drive_time), durationText(trip.movingMs), Modifier.weight(1f))
        PillStat(stringResource(R.string.avg_speed), avg, Modifier.weight(1f))
    }
}

@Composable
private fun PillStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
