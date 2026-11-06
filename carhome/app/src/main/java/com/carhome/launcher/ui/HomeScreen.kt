package com.carhome.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.AppEntry
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.NowPlaying
import com.carhome.launcher.R
import com.carhome.launcher.WeatherInfo
import com.carhome.launcher.WeatherKind
import com.carhome.launcher.weatherKind
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    settings: LauncherSettings,
    accent: Color,
    tiles: List<String>,
    appsByPackage: Map<String, AppEntry>,
    weather: WeatherInfo?,
    hasLocation: Boolean,
    nowPlaying: NowPlaying?,
    hasMusicAccess: Boolean,
    onTileClick: (index: Int, pkg: String) -> Unit,
    onTileChange: (index: Int) -> Unit,
    onTileRemove: (index: Int) -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestMusicAccess: () -> Unit,
    onOpenMusicApp: (String) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Left column: time, weather, music ──────────────────────────────
        Column(
            modifier = Modifier.weight(0.36f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ClockCard(use24h = settings.use24h, modifier = Modifier.fillMaxWidth())
            if (settings.showWeather) {
                WeatherCard(
                    weather = weather,
                    hasLocation = hasLocation,
                    accent = accent,
                    onRequestLocation = onRequestLocation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            MusicCard(
                nowPlaying = nowPlaying,
                hasAccess = hasMusicAccess,
                accent = accent,
                onRequestAccess = onRequestMusicAccess,
                onOpenApp = onOpenMusicApp,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        // ── Right side: header buttons + your app tiles ─────────────────────
        Column(
            modifier = Modifier.weight(0.64f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.hint_tiles),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                GlassIconButton(Icons.Rounded.Apps, stringResource(R.string.all_apps), onOpenApps)
                GlassIconButton(Icons.Rounded.Settings, stringResource(R.string.settings), onOpenSettings)
            }
            TileGrid(
                tiles = tiles,
                appsByPackage = appsByPackage,
                accent = accent,
                onTileClick = onTileClick,
                onTileChange = onTileChange,
                onTileRemove = onTileRemove,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

// ── Clock ─────────────────────────────────────────────────────────────────────

@Composable
private fun ClockCard(use24h: Boolean, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L - now % 1_000L)
        }
    }
    val locale = Locale.getDefault()
    val time = remember(now / 60_000L, use24h, locale) {
        SimpleDateFormat(if (use24h) "HH:mm" else "h:mm", locale).format(Date(now))
    }
    val amPm = remember(now / 60_000L, use24h, locale) {
        if (use24h) "" else SimpleDateFormat("a", locale).format(Date(now))
    }
    val date = remember(now / 60_000L, locale) {
        SimpleDateFormat("EEEE، d MMMM".takeIf { locale.language == "ar" } ?: "EEEE, d MMMM", locale).format(Date(now))
    }
    Column(modifier = modifier.glass().padding(horizontal = 24.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(time, fontSize = 72.sp, fontWeight = FontWeight.Light, color = TextPrimary, lineHeight = 76.sp)
            if (amPm.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(amPm, fontSize = 22.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))
            }
        }
        Text(date, fontSize = 18.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

// ── Weather ───────────────────────────────────────────────────────────────────

private fun weatherIcon(kind: WeatherKind, isDay: Boolean): ImageVector = when (kind) {
    WeatherKind.CLEAR -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    WeatherKind.PARTLY -> Icons.Rounded.WbCloudy
    WeatherKind.CLOUDY, WeatherKind.FOG -> Icons.Rounded.Cloud
    WeatherKind.DRIZZLE -> Icons.Rounded.Grain
    WeatherKind.RAIN -> Icons.Rounded.Umbrella
    WeatherKind.SNOW -> Icons.Rounded.AcUnit
    WeatherKind.STORM -> Icons.Rounded.FlashOn
}

@Composable
private fun WeatherCard(
    weather: WeatherInfo?,
    hasLocation: Boolean,
    accent: Color,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .glass()
            .let { if (!hasLocation) it.clickable(onClick = onRequestLocation) else it }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            !hasLocation -> {
                Icon(Icons.Rounded.LocationOn, null, tint = accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.weather_off), color = TextSecondary, fontSize = 15.sp)
            }
            weather == null -> {
                Icon(Icons.Rounded.Cloud, null, tint = TextSecondary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.weather_loading), color = TextSecondary, fontSize = 15.sp)
            }
            else -> {
                val kind = weatherKind(weather.code)
                Icon(weatherIcon(kind, weather.isDay), null, tint = accent, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(16.dp))
                Text("${weather.temperatureC.roundToInt()}°", fontSize = 36.sp, fontWeight = FontWeight.Light, color = TextPrimary)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(kind.label), fontSize = 16.sp, color = TextSecondary)
            }
        }
    }
}

// ── Music ─────────────────────────────────────────────────────────────────────

@Composable
private fun MusicCard(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    accent: Color,
    onRequestAccess: () -> Unit,
    onOpenApp: (String) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.glass()) {
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

        when {
            !hasAccess -> Column(
                modifier = Modifier.fillMaxSize().clickable(onClick = onRequestAccess).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = accent, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.enable_music), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.enable_music_hint), fontSize = 14.sp, color = TextSecondary)
            }

            else -> Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = nowPlaying?.title.orEmpty() to nowPlaying?.artist.orEmpty(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "track"
                ) { (title, artist) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { m ->
                                val pkg = nowPlaying?.appPackage
                                if (!pkg.isNullOrEmpty()) m.clickable { onOpenApp(pkg) } else m
                            }
                    ) {
                        Text(
                            text = title.ifEmpty { stringResource(R.string.nothing_playing) },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (artist.isNotEmpty()) {
                            Text(artist, fontSize = 15.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(Icons.Rounded.SkipPrevious, "Previous", onPrevious, size = 56.dp)
                    GlassIconButton(
                        icon = if (nowPlaying?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        onClick = onPlayPause,
                        size = 72.dp,
                        tint = Color.White,
                        filled = accent
                    )
                    GlassIconButton(Icons.Rounded.SkipNext, "Next", onNext, size = 56.dp)
                }
            }
        }
    }
}

// ── Tiles ─────────────────────────────────────────────────────────────────────

private fun gridShape(count: Int): Pair<Int, Int> = when (count) {
    4 -> 2 to 2
    6 -> 3 to 2
    12 -> 4 to 3
    else -> 4 to 2
}

@Composable
private fun TileGrid(
    tiles: List<String>,
    appsByPackage: Map<String, AppEntry>,
    accent: Color,
    onTileClick: (Int, String) -> Unit,
    onTileChange: (Int) -> Unit,
    onTileRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = gridShape(tiles.size)
    val cols = shape.first
    val rows = shape.second
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (c in 0 until cols) {
                    val index = r * cols + c
                    val pkg = tiles.getOrNull(index).orEmpty()
                    AppTile(
                        app = appsByPackage[pkg],
                        isEmpty = pkg.isEmpty(),
                        accent = accent,
                        onClick = { onTileClick(index, pkg) },
                        onChange = { onTileChange(index) },
                        onRemove = { onTileRemove(index) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: AppEntry?,
    isEmpty: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Box(
        modifier = modifier
            .scale(scale)
            .glass(RoundedCornerShape(26.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = { if (!isEmpty) menuOpen = true }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isEmpty -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.add_app), color = TextSecondary, fontSize = 14.sp)
            }

            app == null -> Text("…", color = TextSecondary, fontSize = 20.sp) // still loading, or app removed

            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(10.dp)
            ) {
                Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(68.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = app.label,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.change_app)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { menuOpen = false; onChange() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = { menuOpen = false; onRemove() }
            )
        }
    }
}
