package com.carhome.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.OilBarrel
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.AppEntry
import com.carhome.launcher.CardType
import com.carhome.launcher.GpsState
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.LimitInfo
import com.carhome.launcher.Nearby
import com.carhome.launcher.Notice
import com.carhome.launcher.NoticeKind
import com.carhome.launcher.NowPlaying
import com.carhome.launcher.ObdData
import com.carhome.launcher.ObdStatus
import com.carhome.launcher.Place
import com.carhome.launcher.PrayerInfo
import com.carhome.launcher.R
import com.carhome.launcher.Side
import com.carhome.launcher.TILE_PLACE
import com.carhome.launcher.TILE_SMART
import com.carhome.launcher.TripStats
import com.carhome.launcher.WeatherInfo
import com.carhome.launcher.prayerName
import kotlin.math.abs

/** Everything the home screen shows. */
data class HomeState(
    val settings: LauncherSettings,
    val accent: Color,
    val tiles: List<String>,
    val appsByPackage: Map<String, AppEntry>,
    val placesById: Map<String, Place>,
    val smartPick: AppEntry?,
    val weather: WeatherInfo?,
    val dustInHours: Int?,
    val hasLocation: Boolean,
    val nowPlaying: NowPlaying?,
    val hasMusicAccess: Boolean,
    val speedMps: Float?,
    val gpsState: GpsState,
    val satellites: Int,
    val speedFromCar: Boolean,
    val overspeed: Boolean,
    val limit: LimitInfo,
    val prayer: PrayerInfo?,
    val trip: TripStats,
    val lastTrip: TripStats?,
    val obdStatus: ObdStatus,
    val obdData: ObdData,
    val driving: Boolean,
    val notices: List<Notice>,
    val parkedOnly: Set<String>
)

/** What the home screen can ask for. */
class HomeActions(
    val openTile: (index: Int, value: String) -> Unit,
    val changeTile: (index: Int) -> Unit,
    val removeTile: (index: Int) -> Unit,
    val toggleParkedOnly: (pkg: String) -> Unit,
    val openApps: () -> Unit,
    val openSettings: () -> Unit,
    val openCarSettings: () -> Unit,
    val requestLocation: () -> Unit,
    val requestMusicAccess: () -> Unit,
    val openMusicApp: (String) -> Unit,
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val nearby: (Nearby) -> Unit,
    val dismissNotice: (Notice) -> Unit,
    val oilChanged: () -> Unit,
    val newTrip: () -> Unit,
    val readCodes: () -> Unit
)

@Composable
fun HomeScreen(st: HomeState, act: HomeActions) {
    val s = st.settings
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentAct by rememberUpdatedState(act)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .let { m ->
                if (!s.swipeMusic) m else m.pointerInput(rtl) {
                    var total = 0f
                    val needed = 140.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (abs(total) > needed) {
                                // Swipe toward the "next" side of the screen skips forward
                                val forward = if (rtl) total > 0 else total < 0
                                if (forward) currentAct.next() else currentAct.previous()
                            }
                        }
                    ) { change, dx ->
                        total += dx
                        change.consume()
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val frac = s.infoWidth.fraction
            val info: @Composable RowScope.() -> Unit = {
                InfoColumn(st, act, Modifier.weight(frac).fillMaxHeight())
            }
            val main: @Composable RowScope.() -> Unit = {
                MainColumn(st, act, Modifier.weight(1f - frac).fillMaxHeight())
            }
            if (s.infoSide == Side.START) { info(); main() } else { main(); info() }
        }

        NoticeBanner(
            notice = st.notices.firstOrNull(),
            st = st,
            act = act,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
        )
    }
}

@Composable
private fun InfoColumn(st: HomeState, act: HomeActions, modifier: Modifier) {
    val s = st.settings
    val cards = s.cards.ifEmpty { listOf(CardType.CLOCK) }
    val onlyClock = cards.all { it == CardType.CLOCK }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        cards.forEach { card ->
            val m = if (card == CardType.CLOCK && !onlyClock) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().weight(1f)
            when (card) {
                CardType.CLOCK -> ClockCard(s, st.driving, m)
                CardType.SPEED -> SpeedCard(
                    speedMps = st.speedMps, gpsState = st.gpsState, settings = s, limit = st.limit,
                    overspeed = st.overspeed, accent = st.accent, satellites = st.satellites, fromCar = st.speedFromCar,
                    onRequestPermission = act.requestLocation, modifier = m
                )
                CardType.MUSIC -> MusicCard(
                    nowPlaying = st.nowPlaying, hasAccess = st.hasMusicAccess, accent = st.accent,
                    driving = st.driving, doubleTapToPause = s.swipeMusic,
                    onRequestAccess = act.requestMusicAccess, onOpenApp = act.openMusicApp,
                    onPlayPause = act.playPause, onNext = act.next, onPrevious = act.previous, modifier = m
                )
                CardType.WEATHER -> WeatherCard(
                    weather = st.weather, dustInHours = if (s.dustWarning) st.dustInHours else null,
                    hasLocation = st.hasLocation, use24h = s.use24h, accent = st.accent,
                    onRequestLocation = act.requestLocation, modifier = m
                )
                CardType.PRAYER -> PrayerCard(
                    prayer = st.prayer, use24h = s.use24h, accent = st.accent,
                    nearbyButtons = s.nearbyButtons, onNearby = act.nearby, modifier = m
                )
                CardType.TRIP -> TripCard(st.trip, st.lastTrip, s, st.accent, act.newTrip, m)
                CardType.CAR -> CarCard(st.obdStatus, st.obdData, s.obd, st.accent, act.readCodes, act.openCarSettings, m)
            }
        }
    }
}

@Composable
private fun MainColumn(st: HomeState, act: HomeActions, modifier: Modifier) {
    val s = st.settings
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                s.showWeather -> WeatherStrip(
                    weather = st.weather,
                    hasLocation = st.hasLocation,
                    use24h = s.use24h,
                    accent = st.accent,
                    compact = st.driving || s.showPrayerChip,
                    onRequestLocation = act.requestLocation,
                    modifier = Modifier.weight(1f)
                )
                !s.showPrayerChip -> Text(
                    text = stringResource(R.string.hint_tiles),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (s.showPrayerChip) {
                PrayerChip(
                    st.prayer, s.use24h, st.accent,
                    if (s.showWeather) Modifier.widthIn(max = 240.dp) else Modifier.weight(1f)
                )
            }
            GlassIconButton(Icons.Rounded.Apps, stringResource(R.string.all_apps), act.openApps)
            GlassIconButton(Icons.Rounded.Settings, stringResource(R.string.settings), act.openSettings)
        }
        // Driving mode shows fewer, bigger tiles
        val shown = if (st.driving) st.tiles.take(minOf(6, st.tiles.size)) else st.tiles
        TileGrid(st, act, shown, Modifier.fillMaxWidth().weight(1f))
    }
}

// ── Notices (break, oil, dust, fog, prayer) ───────────────────────────────────

@Composable
private fun NoticeBanner(notice: Notice?, st: HomeState, act: HomeActions, modifier: Modifier) {
    AnimatedVisibility(
        visible = notice != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        val n = notice ?: return@AnimatedVisibility
        val s = st.settings
        val (icon, text) = when (n.kind) {
            NoticeKind.BREAK -> Icons.Rounded.Coffee to stringResource(R.string.notice_break, s.breakAfterMin)
            NoticeKind.OIL -> Icons.Rounded.OilBarrel to stringResource(R.string.notice_oil)
            NoticeKind.DUST -> Icons.Rounded.Air to
                (if (n.hours == 0) stringResource(R.string.dust_now) else stringResource(R.string.dust_in, n.hours))
            NoticeKind.FOG -> Icons.Rounded.Air to
                (if (n.hours == 0) stringResource(R.string.fog_now) else stringResource(R.string.fog_in, n.hours))
            NoticeKind.PRAYER -> Icons.Rounded.Mosque to stringResource(
                R.string.notice_prayer,
                stringResource(prayerName(n.prayer ?: com.carhome.launcher.Prayer.DHUHR)),
                formatTime(n.at, s.use24h, appLocale())
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xF0151C2E))
                .padding(start = 22.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = if (n.kind == NoticeKind.PRAYER) st.accent else WarnAmber, modifier = Modifier.size(28.dp))
            Text(text, fontSize = 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 2, modifier = Modifier.widthIn(max = 520.dp))
            when (n.kind) {
                NoticeKind.BREAK -> {
                    SmallPill(Icons.Rounded.Coffee, stringResource(R.string.coffee), st.accent) { act.nearby(Nearby.COFFEE) }
                    SmallPill(Icons.Rounded.LocalGasStation, stringResource(R.string.fuel), st.accent) { act.nearby(Nearby.FUEL) }
                }
                NoticeKind.OIL -> SmallPill(Icons.Rounded.Check, stringResource(R.string.oil_done), st.accent, act.oilChanged)
                NoticeKind.PRAYER -> if (s.nearbyButtons) {
                    SmallPill(Icons.Rounded.Mosque, stringResource(R.string.nearest_mosque), st.accent) { act.nearby(Nearby.MOSQUE) }
                }
                else -> Unit
            }
            GlassIconButton(
                if (n.kind == NoticeKind.BREAK) Icons.Rounded.Snooze else Icons.Rounded.Close,
                stringResource(R.string.dismiss),
                { act.dismissNotice(n) },
                size = 44.dp
            )
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
private fun TileGrid(st: HomeState, act: HomeActions, tiles: List<String>, modifier: Modifier) {
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
                    val value = tiles.getOrNull(index).orEmpty()
                    AppTile(
                        value = value,
                        st = st,
                        onClick = { act.openTile(index, value) },
                        onChange = { act.changeTile(index) },
                        onRemove = { act.removeTile(index) },
                        onToggleParked = { act.toggleParkedOnly(value) },
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
    value: String,
    st: HomeState,
    onClick: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    onToggleParked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val accent = st.accent
    val labels = st.settings.showTileLabels
    val iconSize = if (st.driving) 84.dp else 68.dp
    val isApp = value.isNotEmpty() && value != TILE_SMART && !value.startsWith(TILE_PLACE)
    Box(
        modifier = modifier
            .scale(scale)
            .glass(RoundedCornerShape(26.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = { if (value.isNotEmpty()) menuOpen = true }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            value.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(30.dp))
                }
                if (labels) {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.add_app), color = TextSecondary, fontSize = 14.sp)
                }
            }

            value == TILE_SMART -> {
                val app = st.smartPick
                if (app == null) {
                    TileBody(Icons.Rounded.AutoAwesome, stringResource(R.string.smart_tile), accent, labels, iconSize)
                } else {
                    TileBody(app, labels, iconSize)
                }
                Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopEnd) {
                    Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.smart_tile), tint = accent, modifier = Modifier.size(18.dp))
                }
            }

            value.startsWith(TILE_PLACE) -> {
                val p = st.placesById[value.removePrefix(TILE_PLACE)]
                TileBody(Icons.Rounded.Place, p?.name ?: "…", accent, true, iconSize)
            }

            else -> {
                val app = st.appsByPackage[value]
                if (app == null) Text("…", color = TextSecondary, fontSize = 20.sp) // still loading, or app removed
                else TileBody(app, labels, iconSize)
                if (st.settings.parkedOnly && value in st.parkedOnly) {
                    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopEnd) {
                        Icon(Icons.Rounded.Lock, stringResource(R.string.parked_only), tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
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
            if (isApp && st.settings.parkedOnly) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.parked_only)) },
                    leadingIcon = { Icon(if (value in st.parkedOnly) Icons.Rounded.Check else Icons.Rounded.Lock, null) },
                    onClick = { menuOpen = false; onToggleParked() }
                )
            }
        }
    }
}

@Composable
private fun TileBody(app: AppEntry, labels: Boolean, iconSize: androidx.compose.ui.unit.Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(iconSize))
        if (labels) {
            Spacer(Modifier.height(10.dp))
            Text(app.label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TileBody(icon: ImageVector, label: String, accent: Color, labels: Boolean, iconSize: androidx.compose.ui.unit.Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
        Box(
            modifier = Modifier.size(iconSize).clip(CircleShape).background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(iconSize * 0.55f))
        }
        if (labels) {
            Spacer(Modifier.height(10.dp))
            Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
