package com.carhome.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OilBarrel
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.AppEntry
import com.carhome.launcher.Astro
import com.carhome.launcher.CardType
import com.carhome.launcher.FavSide
import com.carhome.launcher.Geo
import com.carhome.launcher.IconShape
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.ModernCenter
import com.carhome.launcher.ModernFill
import com.carhome.launcher.NetState
import com.carhome.launcher.Place
import com.carhome.launcher.R
import com.carhome.launcher.TILE_PLACE
import com.carhome.launcher.TILE_SMART
import com.carhome.launcher.weatherKind
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

// The modern home (1.3): no app tiles, a big clock (or the speed while moving), shortcuts
// or a "Today" panel, a row of cards, and the favourite apps behind a button on the side.

@Composable
fun ModernHome(st: HomeState, act: HomeActions) {
    val s = st.settings
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val showSpeed = when (s.modernCenter) {
        ModernCenter.AUTO -> st.moving
        ModernCenter.CLOCK -> false
        ModernCenter.SPEED -> true
    }
    BoxWithConstraints(Modifier.fillMaxSize().musicSwipe(s.swipeMusic, rtl, act)) {
        val short = maxHeight < 520.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = if (short) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (short) 10.dp else 14.dp)
        ) {
            ModernTopBar(st, act)
            Box(Modifier.fillMaxWidth().weight(1f)) { ModernMiddle(st, act, showSpeed) }
            // While driving only the first two cards, a little bigger
            val cards = if (st.driving) s.modernCards.take(2) else s.modernCards
            if (cards.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().height(if (short) 132.dp else 156.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    cards.forEach { c ->
                        HomeCard(c, st, act, Modifier.weight(if (c == CardType.MUSIC) 1.35f else 1f).fillMaxHeight(), compact = true)
                    }
                }
            }
        }
        // A car reminder already shown under the clock or in Today needs no banner as well
        val reminderOnScreen = !st.driving && !showSpeed &&
            (s.modernFill == ModernFill.TODAY || (s.modernFill == ModernFill.SHORTCUTS && s.modernReminders))
        val notice = st.notices.firstOrNull { !(reminderOnScreen && it.kind == com.carhome.launcher.NoticeKind.REMINDER) }
        NoticeBanner(notice, st, act, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
        FavoritesLayer(st, act, showHandle = true, edgeSwipe = s.edgeSwipe)
    }
}

/** Swipe sideways anywhere to change the song (the "Swipe for music" setting). */
internal fun Modifier.musicSwipe(on: Boolean, rtl: Boolean, act: HomeActions): Modifier =
    if (!on) this else this.pointerInput(rtl) {
        var total = 0f
        val needed = 140.dp.toPx()
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if (abs(total) > needed) {
                    val forward = if (rtl) total > 0 else total < 0
                    if (forward) act.next() else act.previous()
                }
            }
        ) { change, dx ->
            total += dx
            change.consume()
        }
    }

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ModernTopBar(st: HomeState, act: HomeActions) {
    val s = st.settings
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WeatherChip(st, act)
        PrayerChip(st.prayer, s.use24h, st.accent)
        Spacer(Modifier.weight(1f))
        if (s.internetBadge) InternetButton(st.net, act.openInternet)
        GlassIconButton(Icons.Rounded.Settings, stringResource(R.string.settings), act.openSettings)
    }
}

@Composable
private fun WeatherChip(st: HomeState, act: HomeActions) {
    val w = st.weather
    Row(
        modifier = Modifier
            .height(64.dp)
            .widthIn(max = 320.dp)
            .glass(RoundedCornerShape(32.dp))
            .let { if (!st.hasLocation) it.clickable(onClick = act.requestLocation) else it }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            !st.hasLocation -> {
                Icon(Icons.Rounded.LocationOn, null, tint = st.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.weather_off), fontSize = 14.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            w == null -> {
                Icon(Icons.Rounded.Cloud, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.weather_loading), fontSize = 14.sp, color = TextSecondary, maxLines = 1)
            }
            else -> {
                val kind = weatherKind(w.code)
                Icon(weatherIcon(kind, w.isDay), null, tint = WarnAmber, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("${w.temperatureC.roundToInt()}°", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(kind.label), fontSize = 14.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Green with internet, amber on a network without internet, grey with none. Tap: Android's internet panel. */
@Composable
internal fun InternetButton(net: NetState, onClick: () -> Unit, size: Dp = 56.dp) {
    val (icon, tint, label) = when (net) {
        NetState.ONLINE -> Triple(Icons.Rounded.Wifi, OkGreen, R.string.net_online)
        NetState.NO_INTERNET -> Triple(Icons.Rounded.Wifi, WarnAmber, R.string.net_no_internet)
        NetState.OFFLINE -> Triple(Icons.Rounded.WifiOff, TextSecondary, R.string.net_offline)
    }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val off = net == NetState.OFFLINE
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(if (off) GlassFill else tint.copy(alpha = 0.16f))
            .border(1.dp, if (off) GlassStroke else tint.copy(alpha = 0.45f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, stringResource(label), tint = tint, modifier = Modifier.size(size * 0.44f))
    }
}

// ── The middle: clock or speed, with shortcuts or the Today panel ─────────────

@Composable
private fun ModernMiddle(st: HomeState, act: HomeActions, showSpeed: Boolean) {
    val s = st.settings
    // The favourites button is on the physical left or right; which side is that here?
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val handleOnStart = (s.favSide == FavSide.LEFT) != rtl
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        when {
            showSpeed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // The speed card without its card: a big dial in the middle of the screen
                CompositionLocalProvider(LocalGlassLook provides GlassLook(Color.Transparent, Color.Transparent)) {
                    HomeCard(CardType.SPEED, st, act, Modifier.height(h).width(minOf(w, h * 1.6f)), compact = false)
                }
            }
            s.modernFill == ModernFill.TODAY && !st.driving -> Row(
                Modifier.fillMaxSize().padding(
                    start = if (handleOnStart) 36.dp else 0.dp,
                    end = if (handleOnStart) 0.dp else 36.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val placesShown = s.modernPlaces && st.places.isNotEmpty()
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    BigClock(s, (h - if (placesShown) 110.dp else 40.dp).coerceIn(48.dp, 150.dp), center = false)
                    if (placesShown) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PlaceChips(st, act, 2) }
                    }
                }
                TodayPanel(st, act, Modifier.width(minOf(390.dp, w * 0.46f)).heightIn(max = h))
            }
            else -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val chips = !st.driving && s.modernFill == ModernFill.SHORTCUTS && hasShortcuts(st)
                BigClock(s, (h - (if (chips) 74.dp else 0.dp) - 34.dp).coerceIn(48.dp, 170.dp), center = true)
                if (chips) {
                    Spacer(Modifier.height(16.dp))
                    ShortcutChips(st, act)
                }
            }
        }
    }
}

/** The clock as big as [clockHeight] allows, with the date (and Hijri date) under it. */
@Composable
private fun BigClock(s: LauncherSettings, clockHeight: Dp, center: Boolean) {
    val now = rememberNow()
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
    val hijri = remember(now / 3_600_000L, locale, s.showHijri) { if (s.showHijri) Astro.hijriDate(now, locale) else null }
    val big = with(LocalDensity.current) { (clockHeight / 1.12f).toSp() }
    Column(horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(time, fontSize = big, lineHeight = big * 1.05f, fontWeight = FontWeight.ExtraLight, color = TextPrimary, maxLines = 1)
            if (amPm.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(amPm, fontSize = 22.sp, color = TextSecondary, modifier = Modifier.padding(bottom = clockHeight * 0.14f))
            }
        }
        if (s.showDate) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(date, fontSize = 18.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
                if (hijri != null) {
                    Box(Modifier.padding(horizontal = 10.dp).size(4.dp).clip(CircleShape).background(TextSecondary))
                    Text(hijri, fontSize = 17.sp, color = TextSecondary, maxLines = 1)
                }
            }
        }
    }
}

private fun hasShortcuts(st: HomeState): Boolean {
    val s = st.settings
    return (s.modernPlaces && st.places.isNotEmpty()) ||
        (s.modernSuggest && st.smartPick != null) ||
        (s.modernReminders && st.dueReminders.isNotEmpty())
}

/** Places, the suggested app and a due reminder, under the clock. */
@Composable
private fun ShortcutChips(st: HomeState, act: HomeActions) {
    val s = st.settings
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (s.modernPlaces) PlaceChips(st, act, 2)
        if (s.modernSuggest) {
            st.smartPick?.let { app ->
                Chip(onClick = { act.openShortcut(app.packageName) }) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = st.accent, modifier = Modifier.size(18.dp))
                    AppIcon(app, 28.dp)
                    Text(app.label, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (s.modernReminders) {
            st.dueReminders.firstOrNull()?.let { d ->
                Chip(tint = WarnAmber, onClick = act.openReminders) {
                    Icon(reminderIcon(d.reminder.type), null, tint = WarnAmber, modifier = Modifier.size(20.dp))
                    Text(reminderDueText(d, s.useKmh), fontSize = 15.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PlaceChips(st: HomeState, act: HomeActions, max: Int) {
    st.places.take(max).forEach { p ->
        Chip(onClick = { act.openPlace(p) }) {
            Icon(placeIcon(p), null, tint = st.accent, modifier = Modifier.size(22.dp))
            Text(
                p.name, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
            )
            placeDistance(st, p)?.let { Text(it, fontSize = 14.sp, color = TextSecondary, maxLines = 1) }
        }
    }
}

/** A home or work icon for places named that way; a pin for the rest. */
internal fun placeIcon(p: Place): ImageVector {
    val n = p.name.lowercase()
    return when {
        listOf("بيت", "منزل", "home", "house").any { it in n } -> Icons.Rounded.Home
        listOf("عمل", "دوام", "شغل", "مكتب", "work", "office").any { it in n } -> Icons.Rounded.Work
        else -> Icons.Rounded.Place
    }
}

/** Straight-line distance from here, when both positions are known. */
@Composable
internal fun placeDistance(st: HomeState, p: Place): String? {
    val lat = st.lat ?: return null
    val lon = st.lon ?: return null
    val plat = p.lat ?: return null
    val plon = p.lon ?: return null
    return distanceText(Geo.distanceM(lat, lon, plat, plon), st.settings.useKmh)
}

@Composable
private fun Chip(tint: Color? = null, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .height(56.dp)
            .widthIn(max = 300.dp)
            .pressable(shape, onClick)
            .background(tint?.copy(alpha = 0.14f) ?: GlassFill)
            .border(1.dp, tint?.copy(alpha = 0.4f) ?: GlassStroke, shape)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

// ── Today: reminders, oil and the last trip ───────────────────────────────────

@Composable
internal fun TodayPanel(st: HomeState, act: HomeActions, modifier: Modifier) {
    val s = st.settings
    Column(modifier.glass().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.today), fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 2.dp))
        val rows = mutableListOf<TodayItem>()
        for (d in st.dueReminders.take(2)) {
            rows.add(TodayItem(reminderIcon(d.reminder.type), WarnAmber, reminderDueText(d, s.useKmh), null, act.openReminders))
        }
        val left = st.oilLeftKm
        if (left != null) {
            rows.add(
                TodayItem(
                    Icons.Rounded.OilBarrel, Color(0xFF2DD4BF), stringResource(R.string.today_oil),
                    if (left <= 0) stringResource(R.string.today_oil_due)
                    else stringResource(R.string.today_in, distanceText(left * 1000.0, s.useKmh)),
                    act.openCarSettings
                )
            )
        }
        val trip = st.lastTrip
        if (trip != null) {
            rows.add(
                TodayItem(
                    Icons.Rounded.DirectionsCar, st.accent, stringResource(R.string.today_last_trip),
                    "${distanceText(trip.distanceM, s.useKmh)} · ${durationText(trip.movingMs)}", null
                )
            )
        }
        if (rows.isEmpty()) rows.add(TodayItem(Icons.Rounded.CheckCircle, OkGreen, stringResource(R.string.today_all_clear), null, null))
        rows.forEachIndexed { i, r ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(GlassStroke))
            TodayRow(r.icon, r.tint, r.title, r.value, r.onClick)
        }
    }
}

private class TodayItem(val icon: ImageVector, val tint: Color, val title: String, val value: String?, val onClick: (() -> Unit)?)

@Composable
private fun TodayRow(icon: ImageVector, tint: Color, title: String, value: String?, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .let { if (onClick != null) it.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 15.sp, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, fontSize = 14.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

// ── Favourites: the button on the side, the edge swipe and the panel ─────────

/**
 * Everything about the favourites panel. Laid out left-to-right on purpose, so "left"
 * really is the left edge in Arabic too; the panel itself is in the app's language.
 */
@Composable
internal fun FavoritesLayer(st: HomeState, act: HomeActions, showHandle: Boolean, edgeSwipe: Boolean) {
    val s = st.settings
    val left = s.favSide == FavSide.LEFT
    val dir = LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxSize()) {
            val edge = if (left) Alignment.CenterStart else Alignment.CenterEnd
            if (edgeSwipe && !st.favOpen) {
                Box(
                    Modifier
                        .align(edge)
                        .width(22.dp)
                        .fillMaxHeight()
                        .pointerInput(left) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragEnd = { if ((if (left) total else -total) > 48.dp.toPx()) act.openFavs() }
                            ) { change, dx ->
                                total += dx
                                change.consume()
                            }
                        }
                )
            }
            if (showHandle && !st.favOpen) FavHandle(left, st.driving, st.accent, act.openFavs, Modifier.align(edge))

            AnimatedVisibility(visible = st.favOpen, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = act.closeFavs)
                )
            }
            AnimatedVisibility(
                visible = st.favOpen,
                enter = slideInHorizontally { if (left) -it else it } + fadeIn(),
                exit = slideOutHorizontally { if (left) -it else it } + fadeOut(),
                modifier = Modifier.align(edge)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides dir) {
                    FavPanel(st, act, Modifier.padding(16.dp).width(392.dp).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun FavHandle(left: Boolean, big: Boolean, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    // Laid out left-to-right (see FavoritesLayer), so End is the right side
    val shape = if (left) RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp) else RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp)
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Column(
        modifier = modifier
            .width(if (big) 58.dp else 48.dp)
            .height(if (big) 168.dp else 140.dp)
            .scale(scale)
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(Icons.Rounded.Star, stringResource(R.string.favorites), tint = accent, modifier = Modifier.size(if (big) 28.dp else 24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(TextSecondary)) }
        }
        Icon(Icons.Rounded.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(20.dp).rotate(if (left) -90f else 90f))
    }
}

@Composable
private fun FavPanel(st: HomeState, act: HomeActions, modifier: Modifier) {
    val s = st.settings
    var editing by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF0E1525))
            .border(1.dp, GlassStroke, shape)
            // Taps on the panel itself don't reach the dark area behind it (which closes it)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, tint = st.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.favorites), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            GlassIconButton(
                if (editing) Icons.Rounded.Check else Icons.Rounded.Edit,
                stringResource(if (editing) R.string.fav_done else R.string.fav_edit),
                { editing = !editing },
                size = 48.dp,
                tint = if (editing) st.accent else TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            GlassIconButton(Icons.Rounded.Close, stringResource(R.string.close_panel), act.closeFavs, size = 48.dp)
        }
        val slots = st.tiles.take(s.favCount)
        val rows = slots.chunked(3)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // Every row fits the panel's height: the icons shrink on a low screen
            val gap = 8.dp
            val rowH = (maxHeight - gap * (rows.size - 1).coerceAtLeast(0)) / rows.size.coerceAtLeast(1)
            val icon = (rowH - 40.dp).coerceIn(36.dp, (60.dp * s.iconSize.scale).coerceAtMost(76.dp))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                rows.forEachIndexed { r, row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEachIndexed { c, value -> FavItem(value, r * 3 + c, st, act, editing, icon, Modifier.weight(1f)) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelButton(Icons.Rounded.Apps, stringResource(R.string.all_apps), Modifier.weight(1f)) { act.closeFavs(); act.openApps() }
            PanelButton(Icons.Rounded.Settings, stringResource(R.string.settings), Modifier.weight(1f)) { act.closeFavs(); act.openSettings() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavItem(value: String, index: Int, st: HomeState, act: HomeActions, editing: Boolean, icon: Dp, modifier: Modifier) {
    var menu by remember { mutableStateOf(false) }
    val s = st.settings
    val app = st.appsByPackage[value]
    val isApp = value.isNotEmpty() && value != TILE_SMART && !value.startsWith(TILE_PLACE)
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = {
                        if (editing || value.isEmpty()) act.changeTile(index)
                        else { act.closeFavs(); act.openTile(index, value) }
                    },
                    onLongClick = { if (value.isNotEmpty()) menu = true }
                )
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val label: String
            when {
                value.isEmpty() -> {
                    Box(
                        Modifier.size(icon).clip(RoundedCornerShape(20.dp)).border(1.5.dp, TextSecondary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Add, null, tint = st.accent, modifier = Modifier.size(28.dp)) }
                    label = stringResource(R.string.add_app)
                }
                value == TILE_SMART -> {
                    val pick = st.smartPick
                    if (pick != null) AppIcon(pick, icon) else CircleIcon(Icons.Rounded.AutoAwesome, st.accent, icon)
                    label = pick?.label ?: stringResource(R.string.smart_tile)
                }
                value.startsWith(TILE_PLACE) -> {
                    val p = st.placesById[value.removePrefix(TILE_PLACE)]
                    CircleIcon(p?.let { placeIcon(it) } ?: Icons.Rounded.Place, st.accent, icon)
                    label = p?.name ?: "…"
                }
                app != null -> {
                    AppIcon(app, icon)
                    label = app.label
                }
                else -> {
                    Box(Modifier.size(icon), contentAlignment = Alignment.Center) { Text("…", color = TextSecondary, fontSize = 20.sp) }
                    label = ""
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                label, fontSize = 14.sp, color = if (value.isEmpty()) TextSecondary else TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (s.parkedOnly && isApp && value in st.parkedOnly && !editing) {
            Icon(
                Icons.Rounded.Lock, stringResource(R.string.parked_only), tint = TextSecondary,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp)
            )
        }
        if (editing && value.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AlertRed)
                    .clickable { act.removeTile(index) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Close, stringResource(R.string.remove), tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.change_app)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { menu = false; act.changeTile(index) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = { menu = false; act.removeTile(index) }
            )
            if (isApp && s.parkedOnly) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.parked_only)) },
                    leadingIcon = { Icon(if (value in st.parkedOnly) Icons.Rounded.Check else Icons.Rounded.Lock, null) },
                    onClick = { menu = false; act.toggleParkedOnly(value) }
                )
            }
        }
    }
}

/** An app's icon, in the shape chosen in Settings. */
@Composable
internal fun AppIcon(app: AppEntry, size: Dp) {
    Image(
        bitmap = app.icon, contentDescription = app.label,
        contentScale = if (Look.iconShape == IconShape.ORIGINAL) ContentScale.Fit else ContentScale.Crop,
        modifier = Modifier.size(size).let { m ->
            when (Look.iconShape) {
                IconShape.ORIGINAL -> m
                IconShape.CIRCLE -> m.clip(CircleShape)
                IconShape.ROUNDED -> m.clip(RoundedCornerShape(size * 0.24f))
            }
        }
    )
}

@Composable
internal fun CircleIcon(icon: ImageVector, accent: Color, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
internal fun PanelButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .pressable(shape, onClick)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, GlassStroke, shape)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun AccentButton(icon: ImageVector, label: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .pressable(shape, onClick)
            .background(accent)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = onAccent(accent), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 15.sp, color = onAccent(accent), fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
