package com.carhome.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.carhome.launcher.Astro
import com.carhome.launcher.CardType
import com.carhome.launcher.ClockFace
import com.carhome.launcher.CustomEdit
import com.carhome.launcher.CustomGrid
import com.carhome.launcher.CustomLayout
import com.carhome.launcher.GpsState
import com.carhome.launcher.GridSize
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.MAX_PAGES
import com.carhome.launcher.NetState
import com.carhome.launcher.Qibla
import com.carhome.launcher.R
import com.carhome.launcher.TILE_PLACE
import com.carhome.launcher.TimeWords
import com.carhome.launcher.Widget
import com.carhome.launcher.WidgetStyle
import com.carhome.launcher.WidgetType
import com.carhome.launcher.prayerName
import com.carhome.launcher.speedInUnit
import com.carhome.launcher.weatherKind
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// The custom home (1.3): a grid the user fills with widgets. Press and hold anywhere
// to edit it: drag widgets to move them, drag the coloured corner to resize, and pick
// each widget's size and look in the panel beside the shrunken screen.

/** "8×4", kept left-to-right in Arabic too. */
internal fun sizeLabel(w: Int, h: Int): String = "⁦$w×$h⁩"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomHome(st: HomeState, act: HomeActions) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullW = maxWidth
        val fullH = maxHeight
        val edit = st.customEdit
        if (edit != null) {
            CustomEditor(st, act, edit, fullW, fullH)
        } else {
            val driving = st.settings.customDriveLayout && st.moving
            val layout = if (driving) st.customDrive else st.custom
            Box(Modifier.fillMaxSize().longPressAnywhere { act.startCustomEdit() }) {
                if (layout.pages > 1) {
                    val pager = rememberPagerState(pageCount = { layout.pages })
                    Column(Modifier.fillMaxSize()) {
                        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                            WidgetGrid(layout, page, driving, st, act, null, Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp))
                        }
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.Center) {
                            repeat(layout.pages) { i ->
                                Box(
                                    Modifier.padding(horizontal = 5.dp).size(if (i == pager.currentPage) 10.dp else 8.dp).clip(CircleShape)
                                        .background(if (i == pager.currentPage) st.accent else Color.White.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                } else {
                    WidgetGrid(layout, 0, driving, st, act, null, Modifier.fillMaxSize().padding(16.dp))
                }
            }
            NoticeBanner(st.notices.firstOrNull(), st, act, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
            FavoritesLayer(st, act, showHandle = false, edgeSwipe = false)
        }
    }
}

/**
 * Press and hold anywhere (over widgets too) to edit the screen. Watches the touch
 * before the widgets do; once the hold fires, the rest of that touch is used up, so
 * the widget under the finger isn't also tapped.
 */
private fun Modifier.longPressAnywhere(onLongPress: () -> Unit): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val ended: Boolean? = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var done = false
            while (!done) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val c = ev.changes.firstOrNull { it.id == down.id }
                done = c == null || !c.pressed || (c.position - down.position).getDistance() > viewConfiguration.touchSlop
            }
            true
        }
        if (ended == null) {
            onLongPress()
            do {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                ev.changes.forEach { it.consume() }
            } while (ev.changes.any { it.pressed })
        }
    }
}

/** What the grid needs while it's being edited. */
private class GridEditor(
    val selected: String?,
    val onSelect: (String?) -> Unit,
    val onMove: (String, Int, Int) -> Unit,
    val onResize: (String, Int, Int) -> Unit,
    val onRemove: (String) -> Unit,
    val onEmptyCell: () -> Unit
)

@Composable
private fun WidgetGrid(
    layout: CustomLayout, page: Int, driving: Boolean, st: HomeState, act: HomeActions,
    editor: GridEditor?, modifier: Modifier
) {
    val g = layout.grid
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(modifier) {
        val gap = if (g == GridSize.LARGE) 10.dp else 12.dp
        val cellW = (maxWidth - gap * (g.cols - 1)) / g.cols
        val cellH = (maxHeight - gap * (g.rows - 1)) / g.rows
        val density = LocalDensity.current
        val pitchX = with(density) { (cellW + gap).toPx() }
        val pitchY = with(density) { (cellH + gap).toPx() }
        val widgets = layout.widgets.filter { it.page == page }
        if (editor != null) {
            // Free cells: dashed outlines that show the grid; tap one to add a widget
            val taken = Array(g.rows) { BooleanArray(g.cols) }
            widgets.forEach { w ->
                for (r in w.row until minOf(w.row + w.h, g.rows)) for (c in w.col until minOf(w.col + w.w, g.cols)) taken[r][c] = true
            }
            for (r in 0 until g.rows) for (c in 0 until g.cols) {
                if (taken[r][c]) continue
                Box(
                    Modifier
                        .offset((cellW + gap) * c, (cellH + gap) * r)
                        .size(cellW, cellH)
                        .dashedOutline(st.accent.copy(alpha = 0.55f))
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { editor.onEmptyCell() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Add, stringResource(R.string.add_widget), tint = st.accent.copy(alpha = 0.8f), modifier = Modifier.size(26.dp)) }
            }
        }
        widgets.forEach { w ->
            key(w.id) { WidgetBox(w, driving, st, act, editor, cellW, cellH, gap, pitchX, pitchY, rtl) }
        }
    }
}

private fun Modifier.dashedOutline(color: Color): Modifier = this.drawBehind {
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(18.dp.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
    )
}

@Composable
private fun WidgetBox(
    w: Widget, driving: Boolean, st: HomeState, act: HomeActions, editor: GridEditor?,
    cellW: Dp, cellH: Dp, gap: Dp, pitchX: Float, pitchY: Float, rtl: Boolean
) {
    val width = cellW * w.w + gap * (w.w - 1)
    val height = cellH * w.h + gap * (w.h - 1)
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var resize by remember { mutableStateOf(Offset.Zero) }
    val ed by rememberUpdatedState(editor)
    val selected = editor != null && editor.selected == w.id
    val shape = RoundedCornerShape(Look.radius(28.dp))
    Box(
        Modifier
            .offset((cellW + gap) * w.col, (cellH + gap) * w.row)
            .size(width, height)
            .zIndex(if (dragging) 2f else if (selected) 1f else 0f)
            .graphicsLayer {
                translationX = drag.x
                translationY = drag.y
                val sc = if (dragging) 1.03f else 1f
                scaleX = sc
                scaleY = sc
            }
    ) {
        WidgetContent(w, driving, st, act, Modifier.fillMaxSize())
        if (editor != null) {
            // On top of the widget: a tap selects it, a drag moves it. The widget
            // itself isn't used while editing.
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (selected) st.accent.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.12f))
                    .border(if (selected) 3.dp else 1.5.dp, if (selected) st.accent else Color.White.copy(alpha = 0.35f), shape)
                    .pointerInput(w.id) { detectTapGestures(onTap = { ed?.onSelect(w.id) }) }
                    .pointerInput(w, pitchX, pitchY, rtl) {
                        detectDragGestures(
                            onDragStart = { dragging = true; ed?.onSelect(w.id) },
                            onDragEnd = {
                                val dc = (drag.x / pitchX).roundToInt() * (if (rtl) -1 else 1)
                                val dr = (drag.y / pitchY).roundToInt()
                                drag = Offset.Zero
                                dragging = false
                                if (dc != 0 || dr != 0) ed?.onMove(w.id, w.col + dc, w.row + dr)
                            },
                            onDragCancel = { drag = Offset.Zero; dragging = false }
                        ) { change, amount ->
                            change.consume()
                            drag += amount
                        }
                    }
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AlertRed)
                        .clickable { ed?.onRemove(w.id) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Close, stringResource(R.string.remove), tint = Color.White, modifier = Modifier.size(20.dp)) }
                // Drag this corner to make the widget bigger or smaller
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .graphicsLayer { translationX = resize.x; translationY = resize.y }
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(st.accent)
                        .border(4.dp, Color.White, CircleShape)
                        .pointerInput(w, pitchX, pitchY, rtl) {
                            detectDragGestures(
                                onDragEnd = {
                                    val dw = ((resize.x / pitchX) * (if (rtl) -1 else 1)).roundToInt()
                                    val dh = (resize.y / pitchY).roundToInt()
                                    resize = Offset.Zero
                                    if (dw != 0 || dh != 0) {
                                        val (nw, nh) = CustomGrid.nearestSize(w.type, w.w + dw, w.h + dh)
                                        ed?.onResize(w.id, nw, nh)
                                    }
                                },
                                onDragCancel = { resize = Offset.Zero }
                            ) { change, amount ->
                                change.consume()
                                resize += amount
                            }
                        }
                )
            }
        }
    }
}

// ── The widgets ───────────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(w: Widget, driving: Boolean, st: HomeState, act: HomeActions, modifier: Modifier) {
    val s = st.settings
    CompositionLocalProvider(LocalGlassLook provides glassLookFor(w.style, st.accent)) {
        BoxWithConstraints(modifier) {
            val m = Modifier.fillMaxSize()
            when (w.type) {
                WidgetType.CLOCK -> when (w.face) {
                    ClockFace.DIGITAL -> DigitalClockWidget(s, m)
                    ClockFace.ANALOG -> AnalogClockWidget(s, st.accent, m)
                    ClockFace.WORDS -> WordsClockWidget(m)
                }
                WidgetType.DATE -> DateWidget(st.accent, m)
                WidgetType.SPEED -> if (maxWidth < 170.dp) SpeedMini(st, act, m) else HomeCard(CardType.SPEED, st, act, m, compact = true)
                WidgetType.MUSIC -> MusicCard(
                    nowPlaying = st.nowPlaying, hasAccess = st.hasMusicAccess, accent = st.accent,
                    driving = st.driving, doubleTapToPause = s.swipeMusic,
                    onRequestAccess = act.requestMusicAccess, onOpenApp = act.openMusicApp,
                    onPlayPause = act.playPause, onNext = act.next, onPrevious = act.previous, modifier = m,
                    showArt = w.showArt, showControls = w.showControls, minimal = maxWidth < 300.dp
                )
                WidgetType.WEATHER -> if (maxWidth < 170.dp) WeatherMini(st, act, m) else HomeCard(CardType.WEATHER, st, act, m, compact = true)
                WidgetType.PRAYER -> if (maxWidth < 230.dp) PrayerMini(st, m) else HomeCard(CardType.PRAYER, st, act, m, compact = true)
                WidgetType.TRIP -> HomeCard(CardType.TRIP, st, act, m, compact = true)
                WidgetType.CAR -> HomeCard(CardType.CAR, st, act, m, compact = true)
                WidgetType.TODAY -> TodayPanel(st, act, m)
                WidgetType.SHORTCUT -> ShortcutWidget(w, driving, st, act, m)
                WidgetType.FAVORITES -> FavFolderWidget(st, act, m)
                WidgetType.ALL_APPS -> IconWidget(Icons.Rounded.Apps, stringResource(R.string.all_apps), st.accent, act.openApps, m)
                WidgetType.SETTINGS -> IconWidget(Icons.Rounded.Settings, stringResource(R.string.settings), st.accent, act.openSettings, m)
                WidgetType.INTERNET -> InternetWidget(st.net, act.openInternet, m)
                WidgetType.QIBLA -> QiblaWidget(st, m)
                WidgetType.DUA -> DuaWidget(st.accent, m)
            }
        }
    }
}

@Composable
private fun clockTexts(s: LauncherSettings, now: Long): Triple<String, String, String> {
    val locale = appLocale()
    val pattern = when {
        s.use24h && s.clockSeconds -> "HH:mm:ss"
        s.use24h -> "HH:mm"
        s.clockSeconds -> "h:mm:ss"
        else -> "h:mm"
    }
    val stepKey = if (s.clockSeconds) now / 1_000L else now / 60_000L
    val time = remember(stepKey, pattern, locale) { SimpleDateFormat(pattern, locale).format(Date(now)) }
    val amPm = remember(stepKey, s.use24h, locale) { if (s.use24h) "" else SimpleDateFormat("a", locale).format(Date(now)) }
    val date = remember(now / 60_000L, locale) {
        SimpleDateFormat(if (locale.language == "ar") "EEEE، d MMMM" else "EEEE, d MMMM", locale).format(Date(now))
    }
    return Triple(time, amPm, date)
}

@Composable
private fun DigitalClockWidget(s: LauncherSettings, modifier: Modifier) {
    val now = rememberNow()
    val (time, amPm, date) = clockTexts(s, now)
    val locale = appLocale()
    val hijri = remember(now / 3_600_000L, locale, s.showHijri) { if (s.showHijri) Astro.hijriDate(now, locale) else null }
    val centred = s.cardAlign == com.carhome.launcher.CardAlign.CENTER
    BoxWithConstraints(
        modifier.glass().padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = if (centred) Alignment.Center else Alignment.CenterStart
    ) {
        val withDate = s.showDate && maxHeight >= 96.dp && maxWidth >= 150.dp
        val withHijri = withDate && hijri != null && maxHeight >= 150.dp
        val timeBox = maxHeight * (if (withHijri) 0.55f else if (withDate) 0.62f else 0.9f)
        val byWidth = maxWidth / (time.length * 0.58f + (if (amPm.isNotEmpty()) 1.3f else 0f))
        val big = with(LocalDensity.current) { minOf(timeBox / 1.12f, byWidth).toSp() }
        Column(horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time, fontSize = big, lineHeight = big * 1.05f, fontWeight = FontWeight.Light, color = TextPrimary, maxLines = 1)
                if (amPm.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(amPm, fontSize = big * 0.28f, color = TextSecondary, maxLines = 1)
                }
            }
            if (withDate) Text(date, fontSize = 16.sp, color = TextSecondary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (withHijri && hijri != null) Text(hijri, fontSize = 14.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A clock face with hands; never mirrored, like any clock. */
@Composable
private fun AnalogClockWidget(s: LauncherSettings, accent: Color, modifier: Modifier) {
    val now = rememberNow(if (s.clockSeconds) 1_000L else 10_000L)
    val hand = TextPrimary
    val faint = Color.White
    Box(modifier.glass().padding(10.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = minOf(size.width, size.height) / 2f
            val c = center
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val h = cal.get(Calendar.HOUR)
            val m = cal.get(Calendar.MINUTE)
            val sec = cal.get(Calendar.SECOND)
            drawCircle(faint.copy(alpha = 0.3f), r - 2.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0 - 90.0)
                val outer = r - 7.dp.toPx()
                val inner = outer - (if (i % 3 == 0) 10.dp else 5.dp).toPx()
                drawLine(
                    faint.copy(alpha = if (i % 3 == 0) 0.7f else 0.35f),
                    Offset(c.x + (cos(a) * inner).toFloat(), c.y + (sin(a) * inner).toFloat()),
                    Offset(c.x + (cos(a) * outer).toFloat(), c.y + (sin(a) * outer).toFloat()),
                    strokeWidth = (if (i % 3 == 0) 3.dp else 2.dp).toPx(), cap = StrokeCap.Round
                )
            }
            fun drawHand(deg: Double, len: Float, width: Dp, color: Color) {
                val a = Math.toRadians(deg - 90.0)
                drawLine(color, c, Offset(c.x + (cos(a) * len).toFloat(), c.y + (sin(a) * len).toFloat()), width.toPx(), StrokeCap.Round)
            }
            drawHand((h + m / 60.0) * 30.0, r * 0.5f, 5.dp, hand)
            drawHand((m + sec / 60.0) * 6.0, r * 0.74f, 3.5.dp, hand)
            if (s.clockSeconds) drawHand(sec * 6.0, r * 0.8f, 1.5.dp, accent)
            drawCircle(accent, 4.5.dp.toPx(), c)
        }
    }
}

/** The time in words, to the nearest five minutes. */
@Composable
private fun WordsClockWidget(modifier: Modifier) {
    val now = rememberNow(30_000L)
    val arabic = appLocale().language == "ar"
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val text = TimeWords.phrase(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), arabic)
    BoxWithConstraints(modifier.glass().padding(14.dp), contentAlignment = Alignment.Center) {
        val fs = with(LocalDensity.current) { minOf(maxHeight / 3.4f, maxWidth / 7f).coerceIn(13.dp, 42.dp).toSp() }
        Text(
            text, fontSize = fs, lineHeight = fs * 1.25f, fontWeight = FontWeight.Light, color = TextPrimary,
            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DateWidget(accent: Color, modifier: Modifier) {
    val now = rememberNow(60_000L)
    val locale = appLocale()
    val date = remember(now / 60_000L, locale) {
        SimpleDateFormat(if (locale.language == "ar") "EEEE، d MMMM" else "EEEE, d MMMM", locale).format(Date(now))
    }
    val hijri = remember(now / 3_600_000L, locale) { Astro.hijriDate(now, locale) }
    Row(modifier.glass().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(date, fontSize = 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hijri != null) Text(hijri, fontSize = 14.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Just the number, for a one-cell speed widget. */
@Composable
private fun SpeedMini(st: HomeState, act: HomeActions, modifier: Modifier) {
    val s = st.settings
    val v = st.speedMps?.takeIf { it.isFinite() }?.let { speedInUnit(it, s.useKmh) }
    val txt = when {
        v == null -> "—"
        st.speedApprox -> "~" + v.roundToInt()
        else -> v.roundToInt().toString()
    }
    val needsGps = st.gpsState == GpsState.NO_PERMISSION || st.gpsState == GpsState.LOCATION_OFF
    BoxWithConstraints(
        modifier.glass().let { if (needsGps) it.clickable(onClick = act.requestLocation) else it }
            .background(if (st.overspeed) AlertRed.copy(alpha = 0.28f) else Color.Transparent)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val big = with(LocalDensity.current) { minOf(maxHeight * 0.48f, maxWidth / (txt.length * 0.62f + 0.2f)).toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                txt, fontSize = big, lineHeight = big * 1.05f, fontWeight = FontWeight.Light, maxLines = 1,
                color = (if (st.overspeed) AlertRed else TextPrimary).copy(alpha = if (st.speedStale) 0.45f else 1f)
            )
            Text(stringResource(if (s.useKmh) R.string.kmh else R.string.mph), fontSize = 13.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun WeatherMini(st: HomeState, act: HomeActions, modifier: Modifier) {
    val w = st.weather
    Column(
        modifier.glass().let { if (!st.hasLocation) it.clickable(onClick = act.requestLocation) else it }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (w == null) {
            Icon(if (st.hasLocation) Icons.Rounded.WbSunny else Icons.Rounded.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(28.dp))
        } else {
            Icon(weatherIcon(weatherKind(w.code), w.isDay), null, tint = WarnAmber, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(4.dp))
            Text("${w.temperatureC.roundToInt()}°", fontSize = 26.sp, fontWeight = FontWeight.Light, color = TextPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun PrayerMini(st: HomeState, modifier: Modifier) {
    val now = rememberNow(30_000L)
    val locale = appLocale()
    val p = st.prayer
    BoxWithConstraints(modifier.glass().padding(10.dp), contentAlignment = Alignment.Center) {
        if (p == null) {
            Icon(Icons.Rounded.Mosque, stringResource(R.string.prayer_waiting), tint = TextSecondary, modifier = Modifier.size(28.dp))
        } else {
            val wide = maxWidth > maxHeight * 1.5f
            val name = stringResource(prayerName(p.next.prayer))
            val time = formatTime(p.next.at, st.settings.use24h, locale)
            val left = countdown(p.next.at - now)
            if (wide) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Mosque, null, tint = st.accent, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("$name $time", fontSize = 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(left, fontSize = 13.sp, color = st.accent, maxLines = 1)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Mosque, null, tint = st.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Text(time, fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Light, maxLines = 1)
                    if (maxHeight >= 90.dp) Text(left, fontSize = 12.sp, color = st.accent, maxLines = 1)
                }
            }
        }
    }
}

/** An app or a saved place; empty until one is chosen. */
@Composable
private fun ShortcutWidget(w: Widget, driving: Boolean, st: HomeState, act: HomeActions, modifier: Modifier) {
    val v = w.value
    val place = if (v.startsWith(TILE_PLACE)) st.placesById[v.removePrefix(TILE_PLACE)] else null
    val app = if (v.isNotEmpty() && !v.startsWith(TILE_PLACE)) st.appsByPackage[v] else null
    val interaction = remember { MutableInteractionSource() }
    val pressed = rememberPressScale(interaction)
    BoxWithConstraints(
        modifier
            .scale(pressed)
            .glass()
            .clickable(interactionSource = interaction, indication = null) {
                if (v.isEmpty()) act.pickForWidget(driving, w.id) else act.openShortcut(v)
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val wide = maxWidth > maxHeight * 1.5f
        val iconSize = minOf(maxHeight * (if (wide) 0.7f else 0.55f), 64.dp)
        val label = when {
            place != null -> place.name
            app != null -> app.label
            v.isEmpty() -> stringResource(R.string.choose_app_place)
            else -> "…"
        }
        val icon: @Composable () -> Unit = {
            when {
                app != null -> AppIcon(app, iconSize)
                place != null -> CircleIcon(placeIcon(place), st.accent, iconSize)
                else -> CircleIcon(Icons.Rounded.Add, st.accent, iconSize)
            }
        }
        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    Text(label, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    place?.let { p -> placeDistance(st, p)?.let { Text(it, fontSize = 13.sp, color = TextSecondary, maxLines = 1) } }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                icon()
                if (maxHeight >= 84.dp) {
                    Spacer(Modifier.height(6.dp))
                    Text(label, fontSize = 13.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Four favourite icons; opens the favourites panel. */
@Composable
private fun FavFolderWidget(st: HomeState, act: HomeActions, modifier: Modifier) {
    val apps = st.tiles.take(st.settings.favCount).mapNotNull { st.appsByPackage[it] }.take(4)
    BoxWithConstraints(modifier.glass().clickable(onClick = act.openFavs).padding(8.dp), contentAlignment = Alignment.Center) {
        val showLabel = maxHeight >= 84.dp
        val box = minOf(maxHeight - (if (showLabel) 26.dp else 0.dp), maxWidth * 0.8f, 64.dp)
        val cell = (box - 4.dp) / 2
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.size(box), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until 2) {
                            val app = apps.getOrNull(r * 2 + c)
                            if (app != null) AppIcon(app, cell)
                            else Box(Modifier.size(cell).clip(RoundedCornerShape(cell * 0.25f)).background(st.accent.copy(alpha = 0.25f)))
                        }
                    }
                }
            }
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.favorites), fontSize = 13.sp, color = TextPrimary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun IconWidget(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier.glass().clickable(onClick = onClick).padding(8.dp), contentAlignment = Alignment.Center) {
        val showLabel = maxHeight >= 84.dp
        val circle = minOf(maxHeight - (if (showLabel) 26.dp else 0.dp), 56.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircleIcon(icon, accent, circle)
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 13.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun InternetWidget(net: NetState, onClick: () -> Unit, modifier: Modifier) {
    val (tint, label) = when (net) {
        NetState.ONLINE -> OkGreen to R.string.net_online
        NetState.NO_INTERNET -> WarnAmber to R.string.net_no_internet
        NetState.OFFLINE -> TextSecondary to R.string.net_offline
    }
    BoxWithConstraints(modifier.glass().clickable(onClick = onClick).padding(8.dp), contentAlignment = Alignment.Center) {
        val showLabel = maxHeight >= 84.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InternetButton(net, onClick, minOf(maxHeight - (if (showLabel) 26.dp else 0.dp), 52.dp))
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(label), fontSize = 13.sp, color = tint, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * The Qibla: an arrow from the centre. Parked, it points as on a compass (north up);
 * moving, it turns with the car, so straight up is the way the car is heading.
 */
@Composable
private fun QiblaWidget(st: HomeState, modifier: Modifier) {
    val lat = st.lat
    val lon = st.lon
    val accent = st.accent
    BoxWithConstraints(modifier.glass().padding(10.dp), contentAlignment = Alignment.Center) {
        if (lat == null || lon == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Explore, null, tint = accent, modifier = Modifier.size(28.dp))
                if (maxHeight >= 84.dp) Text(stringResource(R.string.qibla_needs_location), fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 2)
            }
        } else {
            val q = Qibla.bearing(lat, lon)
            val heading = st.heading
            val angle = if (heading != null) Qibla.relative(q, heading.toDouble()) else q
            val showText = maxHeight >= 110.dp
            val dial = minOf(maxWidth, maxHeight - (if (showText) 26.dp else 0.dp))
            val mark = TextSecondary
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(dial)) {
                    val r = size.minDimension / 2f
                    val c = center
                    drawCircle(Color.White.copy(alpha = 0.3f), r - 2.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                    // North (or the car's nose while moving) at the top
                    drawLine(mark, Offset(c.x, c.y - r + 5.dp.toPx()), Offset(c.x, c.y - r + 13.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                    val a = Math.toRadians(angle - 90.0)
                    val tip = Offset(c.x + (cos(a) * r * 0.70).toFloat(), c.y + (sin(a) * r * 0.70).toFloat())
                    val tail = Offset(c.x - (cos(a) * r * 0.28).toFloat(), c.y - (sin(a) * r * 0.28).toFloat())
                    drawLine(Color.White.copy(alpha = 0.4f), c, tail, 3.dp.toPx(), StrokeCap.Round)
                    drawLine(accent, c, tip, 5.dp.toPx(), StrokeCap.Round)
                    drawCircle(accent, 5.dp.toPx(), tip)
                    drawCircle(Color.White, 4.dp.toPx(), c)
                }
                if (showText) {
                    Text(
                        if (heading != null) stringResource(R.string.qibla_from_car) else stringResource(R.string.qibla_degrees, q.roundToInt()),
                        fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** The travel dua; tap for the whole of it. */
@Composable
private fun DuaWidget(accent: Color, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(
        modifier.glass().clickable { open = true }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.dua_title), fontSize = 12.sp, color = accent, fontWeight = FontWeight.Medium, maxLines = 1)
        // The dua is Arabic in every language
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                stringResource(R.string.dua_text), fontSize = 17.sp, lineHeight = 27.sp, color = TextPrimary,
                maxLines = 4, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()
            )
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close_panel), color = accent) } },
            title = { Text(stringResource(R.string.dua_title), color = TextPrimary) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            stringResource(R.string.dua_text), fontSize = 21.sp, lineHeight = 36.sp, color = TextPrimary,
                            textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    val meaning = stringResource(R.string.dua_meaning)
                    if (meaning.isNotBlank()) Text(meaning, fontSize = 15.sp, lineHeight = 22.sp, color = TextSecondary)
                }
            },
            containerColor = Color(0xFF141B2B)
        )
    }
}

// ── The editor ────────────────────────────────────────────────────────────────

@Composable
private fun CustomEditor(st: HomeState, act: HomeActions, edit: CustomEdit, fullW: Dp, fullH: Dp) {
    val s = st.settings
    val driving = edit.driving && s.customDriveLayout
    val layout = if (driving) st.customDrive else st.custom
    val page = edit.page.coerceIn(0, layout.pages - 1)
    val selected = edit.selected?.let { CustomGrid.find(layout, it) }
    val change: ((CustomLayout) -> CustomLayout) -> Unit = { f -> act.changeCustom(driving, f) }
    val editor = GridEditor(
        selected = selected?.id,
        onSelect = { id -> act.changeCustomEdit { it.copy(selected = id) } },
        onMove = { id, c, r -> change { CustomGrid.move(it, id, c, r) } },
        onResize = { id, w, h -> change { CustomGrid.resize(it, id, w, h) } },
        onRemove = { id -> change { CustomGrid.remove(it, id) }; act.changeCustomEdit { it.copy(selected = null) } },
        onEmptyCell = { act.changeCustomEdit { it.copy(library = true) } }
    )
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Inspector(st, act, layout, selected, driving, change, Modifier.width(300.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorToolbar(st, act, layout, driving)
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val scale = minOf(maxWidth / fullW, maxHeight / fullH)
                    Box(Modifier.align(Alignment.Center).shrunkScreen(fullW, fullH, scale)) {
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(BgTop, BgBottom)))) {
                            WidgetGrid(layout, page, driving, st, act, editor, Modifier.fillMaxSize().padding(16.dp))
                        }
                    }
                }
                PagesRow(st, act, layout, page, change)
                Text(stringResource(R.string.custom_edit_hint), fontSize = 13.sp, color = TextSecondary, maxLines = 2)
            }
        }
        if (edit.library) WidgetLibrary(st, act, layout, Modifier.fillMaxSize())
    }
}

/**
 * The home screen at its real size, drawn [scale] times smaller. Touches are scaled
 * back, so dragging a widget here works exactly as it would on the full screen.
 */
private fun Modifier.shrunkScreen(fullW: Dp, fullH: Dp, scale: Float): Modifier = this
    .size(fullW * scale, fullH * scale)
    .clip(RoundedCornerShape(18.dp))
    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
    .layout { measurable, _ ->
        val w = fullW.roundToPx()
        val h = fullH.roundToPx()
        val p = measurable.measure(Constraints.fixed(w, h))
        layout((w * scale).roundToInt(), (h * scale).roundToInt()) {
            p.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Inspector(
    st: HomeState, act: HomeActions, layout: CustomLayout, w: Widget?, driving: Boolean,
    change: ((CustomLayout) -> CustomLayout) -> Unit, modifier: Modifier
) {
    val a = st.accent
    Column(modifier.glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (w == null) {
                Icon(Icons.Rounded.TouchApp, null, tint = a, modifier = Modifier.size(32.dp))
                Text(stringResource(R.string.custom_edit_title), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(stringResource(R.string.custom_select_hint), fontSize = 14.sp, color = TextSecondary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(a.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(widgetIcon(w.type), null, tint = a, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(widgetName(w.type)), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(sizeLabel(w.w, w.h), fontSize = 13.sp, color = TextSecondary)
                    }
                    GlassIconButton(
                        Icons.Rounded.Delete, stringResource(R.string.remove),
                        { change { CustomGrid.remove(it, w.id) }; act.changeCustomEdit { it.copy(selected = null) } },
                        size = 48.dp, tint = AlertRed
                    )
                }
                val sizes = CustomGrid.sizes(w.type).filter { it.first <= layout.grid.cols && it.second <= layout.grid.rows }
                EditChips(stringResource(R.string.widget_size), sizes.map { sizeLabel(it.first, it.second) }, sizes.indexOf(w.w to w.h), a) { i ->
                    val sz = sizes[i]
                    change { CustomGrid.resize(it, w.id, sz.first, sz.second) }
                }
                EditChips(
                    stringResource(R.string.widget_look),
                    listOf(stringResource(R.string.look_glass), stringResource(R.string.look_solid), stringResource(R.string.look_clear), stringResource(R.string.look_tinted)),
                    WidgetStyle.entries.indexOf(w.style), a
                ) { i -> change { CustomGrid.update(it, w.id) { x -> x.copy(style = WidgetStyle.entries[i]) } } }
                when (w.type) {
                    WidgetType.CLOCK -> EditChips(
                        stringResource(R.string.clock_face),
                        listOf(stringResource(R.string.face_digital), stringResource(R.string.face_analog), stringResource(R.string.face_words)),
                        ClockFace.entries.indexOf(w.face), a
                    ) { i -> change { CustomGrid.update(it, w.id) { x -> x.copy(face = ClockFace.entries[i]) } } }
                    WidgetType.MUSIC -> {
                        EditSwitch(stringResource(R.string.show_cover), w.showArt, a) { v -> change { CustomGrid.update(it, w.id) { x -> x.copy(showArt = v) } } }
                        EditSwitch(stringResource(R.string.show_controls), w.showControls, a) { v -> change { CustomGrid.update(it, w.id) { x -> x.copy(showControls = v) } } }
                    }
                    WidgetType.SHORTCUT -> PanelButton(Icons.Rounded.Edit, stringResource(R.string.change_app_place), Modifier.fillMaxWidth()) { act.pickForWidget(driving, w.id) }
                    else -> Unit
                }
                if (layout.pages > 1) {
                    EditChips(stringResource(R.string.widget_page), (1..layout.pages).map { "$it" }, w.page, a) { i ->
                        change { CustomGrid.moveToPage(it, w.id, i) }
                        act.changeCustomEdit { it.copy(page = i) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelButton(Icons.Rounded.Add, stringResource(R.string.add_widget), Modifier.weight(1f)) { act.changeCustomEdit { it.copy(library = true) } }
            AccentButton(Icons.Rounded.Check, stringResource(R.string.fav_done), a, Modifier.weight(1f)) { act.endCustomEdit() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditChips(title: String, labels: List<String>, selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { i, label -> SmallChip(label, i == selected, accent) { onSelect(i) } }
        }
    }
}

@Composable
private fun SmallChip(label: String, on: Boolean, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .pressable(shape, onClick)
            .background(if (on) accent else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (on) accent else GlassStroke, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, color = if (on) onAccent(accent) else TextPrimary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
private fun EditSwitch(title: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = onAccent(accent), checkedTrackColor = accent)
        )
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .height(48.dp)
            .pressable(shape, onClick)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
    }
}

@Composable
private fun EditorToolbar(st: HomeState, act: HomeActions, layout: CustomLayout, driving: Boolean) {
    val s = st.settings
    val a = st.accent
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (s.customDriveLayout) {
            Row(
                Modifier.clip(RoundedCornerShape(26.dp)).background(GlassFill).border(1.dp, GlassStroke, RoundedCornerShape(26.dp)).padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(R.string.layout_parked, R.string.layout_driving).forEachIndexed { i, label ->
                    val on = (i == 1) == driving
                    Box(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (on) a else Color.Transparent)
                            .clickable { act.changeCustomEdit { it.copy(driving = i == 1, selected = null, page = 0) } }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(label), fontSize = 14.sp, color = if (on) onAccent(a) else TextPrimary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        var gridMenu by remember { mutableStateOf(false) }
        Box {
            ToolButton(Icons.Rounded.GridView, stringResource(R.string.grid_label, sizeLabel(layout.grid.cols, layout.grid.rows))) { gridMenu = true }
            DropdownMenu(expanded = gridMenu, onDismissRequest = { gridMenu = false }) {
                GridSize.entries.forEach { g ->
                    DropdownMenuItem(
                        text = { Text((if (g == layout.grid) "✓ " else "") + sizeLabel(g.cols, g.rows)) },
                        onClick = { gridMenu = false; act.setCustomGrid(g) }
                    )
                }
            }
        }
        ToolButton(Icons.Rounded.Wallpaper, stringResource(R.string.background)) { act.openDisplaySettings() }
        GlassIconButton(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.undo), act.undoCustom, size = 48.dp)
    }
}

@Composable
private fun PagesRow(st: HomeState, act: HomeActions, layout: CustomLayout, page: Int, change: ((CustomLayout) -> CustomLayout) -> Unit) {
    val a = st.accent
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(layout.pages) { i ->
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable { act.changeCustomEdit { it.copy(page = i, selected = null) } },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(if (i == page) 12.dp else 9.dp).clip(CircleShape).background(if (i == page) a else Color.White.copy(alpha = 0.3f)))
            }
        }
        Text(stringResource(R.string.page_n_of, page + 1, layout.pages), fontSize = 14.sp, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        if (layout.pages > 1) {
            ToolButton(Icons.Rounded.Delete, stringResource(R.string.remove_page)) {
                change { CustomGrid.removePage(it, page) }
                act.changeCustomEdit { it.copy(page = (page - 1).coerceAtLeast(0), selected = null) }
            }
        }
        if (layout.pages < MAX_PAGES) {
            ToolButton(Icons.Rounded.Add, stringResource(R.string.add_page)) {
                change { CustomGrid.addPage(it) }
                act.changeCustomEdit { it.copy(page = layout.pages, selected = null) }
            }
        }
    }
}

// ── The widget library ────────────────────────────────────────────────────────

private enum class LibCat(val label: Int) {
    ALL(R.string.lib_all), TIME(R.string.lib_time), DRIVING(R.string.lib_driving), MEDIA(R.string.lib_media),
    PRAYER(R.string.lib_prayer), CAR(R.string.lib_car), PLACES(R.string.lib_places), SYSTEM(R.string.lib_system)
}

private fun categoryOf(t: WidgetType): LibCat = when (t) {
    WidgetType.CLOCK, WidgetType.DATE, WidgetType.WEATHER -> LibCat.TIME
    WidgetType.SPEED, WidgetType.TRIP -> LibCat.DRIVING
    WidgetType.MUSIC -> LibCat.MEDIA
    WidgetType.PRAYER, WidgetType.QIBLA, WidgetType.DUA -> LibCat.PRAYER
    WidgetType.CAR, WidgetType.TODAY -> LibCat.CAR
    WidgetType.SHORTCUT, WidgetType.FAVORITES, WidgetType.ALL_APPS -> LibCat.PLACES
    WidgetType.SETTINGS, WidgetType.INTERNET -> LibCat.SYSTEM
}

internal fun widgetName(t: WidgetType): Int = when (t) {
    WidgetType.CLOCK -> R.string.card_clock
    WidgetType.DATE -> R.string.w_date
    WidgetType.SPEED -> R.string.card_speed
    WidgetType.MUSIC -> R.string.card_music
    WidgetType.WEATHER -> R.string.card_weather
    WidgetType.PRAYER -> R.string.card_prayer
    WidgetType.TRIP -> R.string.card_trip
    WidgetType.CAR -> R.string.card_car
    WidgetType.TODAY -> R.string.today
    WidgetType.SHORTCUT -> R.string.w_shortcut
    WidgetType.FAVORITES -> R.string.favorites
    WidgetType.ALL_APPS -> R.string.all_apps
    WidgetType.SETTINGS -> R.string.settings
    WidgetType.INTERNET -> R.string.w_internet
    WidgetType.QIBLA -> R.string.w_qibla
    WidgetType.DUA -> R.string.dua_title
}

internal fun widgetIcon(t: WidgetType): ImageVector = when (t) {
    WidgetType.CLOCK -> Icons.Rounded.AccessTime
    WidgetType.DATE -> Icons.Rounded.CalendarMonth
    WidgetType.SPEED -> Icons.Rounded.Speed
    WidgetType.MUSIC -> Icons.Rounded.MusicNote
    WidgetType.WEATHER -> Icons.Rounded.WbSunny
    WidgetType.PRAYER -> Icons.Rounded.Mosque
    WidgetType.TRIP -> Icons.Rounded.Timeline
    WidgetType.CAR -> Icons.Rounded.DirectionsCar
    WidgetType.TODAY -> Icons.Rounded.EventNote
    WidgetType.SHORTCUT -> Icons.Rounded.TouchApp
    WidgetType.FAVORITES -> Icons.Rounded.Star
    WidgetType.ALL_APPS -> Icons.Rounded.Apps
    WidgetType.SETTINGS -> Icons.Rounded.Settings
    WidgetType.INTERNET -> Icons.Rounded.Wifi
    WidgetType.QIBLA -> Icons.Rounded.Explore
    WidgetType.DUA -> Icons.Rounded.MenuBook
}

@Composable
private fun WidgetLibrary(st: HomeState, act: HomeActions, layout: CustomLayout, modifier: Modifier) {
    val a = st.accent
    var cat by remember { mutableStateOf(LibCat.ALL) }
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<WidgetType?>(null) }
    var size by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var face by remember { mutableStateOf(ClockFace.DIGITAL) }
    val close = { act.changeCustomEdit { it.copy(library = false) } }
    val sheet = RoundedCornerShape(28.dp)
    Box(
        modifier
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(sheet)
                .background(Color(0xF70E1525))
                .border(1.dp, GlassStroke, sheet)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(Modifier.width(190.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LibCat.entries.forEach { c ->
                    val on = c == cat
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) a.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable { cat = c }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(stringResource(c.label), fontSize = 15.sp, color = TextPrimary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.add_widget), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                    SearchField(query, a) { query = it }
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Rounded.Close, stringResource(R.string.close_panel), { close() }, size = 48.dp)
                }
                val names = WidgetType.entries.associateWith { stringResource(widgetName(it)) }
                val list = WidgetType.entries.filter { t ->
                    (cat == LibCat.ALL || categoryOf(t) == cat) &&
                        (query.isBlank() || names.getValue(t).contains(query.trim(), ignoreCase = true))
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 170.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.name }) { t ->
                        LibraryCard(t, names.getValue(t), chosen == t, a) {
                            chosen = t
                            size = CustomGrid.defaultSize(t)
                            face = ClockFace.DIGITAL
                        }
                    }
                }
                val t = chosen
                if (t != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (t == WidgetType.CLOCK) {
                                listOf(R.string.face_digital, R.string.face_analog, R.string.face_words).forEachIndexed { i, label ->
                                    SmallChip(stringResource(label), face == ClockFace.entries[i], a) { face = ClockFace.entries[i] }
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            CustomGrid.sizes(t).filter { it.first <= layout.grid.cols && it.second <= layout.grid.rows }.forEach { sz ->
                                SmallChip(sizeLabel(sz.first, sz.second), size == sz, a) { size = sz }
                            }
                        }
                        AccentButton(Icons.Rounded.Add, stringResource(R.string.add_widget_now), a) { act.addWidget(t, size, face) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, accent: Color, onChange: (String) -> Unit) {
    Row(
        Modifier
            .width(230.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, GlassStroke, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(stringResource(R.string.search_widgets), fontSize = 14.sp, color = TextSecondary, maxLines = 1)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LibraryCard(t: WidgetType, name: String, on: Boolean, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val sizes = CustomGrid.sizes(t)
    val range = if (sizes.size == 1) sizeLabel(sizes[0].first, sizes[0].second)
    else "${sizeLabel(sizes.first().first, sizes.first().second)} – ${sizeLabel(sizes.last().first, sizes.last().second)}"
    Column(
        Modifier
            .fillMaxWidth()
            .pressable(shape, onClick)
            .background(if (on) accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
            .border(if (on) 2.dp else 1.dp, if (on) accent else GlassStroke, shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(widgetIcon(t), null, tint = accent, modifier = Modifier.size(34.dp))
            if (t == WidgetType.QIBLA || t == WidgetType.DUA) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(10.dp)).background(WarnAmber.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)
                ) { Text(stringResource(R.string.new_badge), fontSize = 11.sp, color = WarnAmber, fontWeight = FontWeight.SemiBold) }
            }
        }
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(name, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(range, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
        }
    }
}
