package com.carhome.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.CardType
import com.carhome.launcher.FavSide
import com.carhome.launcher.R
import com.carhome.launcher.prayerName
import kotlin.math.roundToInt

// The dashboard home (1.4): a big map on the driver's side, and a column beside it with
// the cards you pick (speed and music by default), like CarPlay's dashboard. While Google
// Maps navigates, the next turn sits on top of the map.

@Composable
fun DashboardHome(st: HomeState, act: HomeActions) {
    val s = st.settings
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val short = maxHeight < 520.dp
        val gap = if (short) 12.dp else 16.dp
        val mapShare = s.dashMapWidth / 100f
        val map: @Composable RowScope.() -> Unit = {
            DashMap(st, act, Modifier.weight(mapShare).fillMaxHeight())
        }
        val column: @Composable RowScope.() -> Unit = {
            DashColumn(st, act, short, Modifier.weight(1f - mapShare).fillMaxHeight())
        }
        // The map's side is a real side of the screen (the driver's), the same in Arabic and English
        Row(
            Modifier.fillMaxSize().padding(gap),
            horizontalArrangement = Arrangement.Absolute.spacedBy(gap)
        ) {
            if (s.dashMapSide == FavSide.LEFT) { map(); column() } else { column(); map() }
        }
        NoticeBanner(st.notices.firstOrNull(), st, act, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
        FavoritesLayer(st, act, showHandle = false, edgeSwipe = false)
    }
}

@Composable
private fun DashMap(st: HomeState, act: HomeActions, modifier: Modifier) {
    Box(modifier) {
        CarMap(st, act, compact = false, modifier = Modifier.fillMaxSize())
        val nav = st.nav
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp)) {
            if (nav != null) NextTurnCard(nav, st.accent, act.openNavApp, Modifier.fillMaxWidth(), solid = true)
            else WhereToPill(st.accent, act.openNavApp)
        }
    }
}

@Composable
private fun DashColumn(st: HomeState, act: HomeActions, short: Boolean, modifier: Modifier) {
    val s = st.settings
    // While driving only the first two cards, with more room each
    val cards = (if (st.driving) s.dashCards.take(2) else s.dashCards).ifEmpty { listOf(CardType.SPEED) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (short) 10.dp else 14.dp)) {
        DashTopBar(st, act)
        cards.forEach { c ->
            val weight = when (c) {
                CardType.SPEED -> 1.25f
                CardType.CLOCK -> 0.8f
                else -> 1f
            }
            HomeCard(c, st, act, Modifier.fillMaxWidth().weight(weight), compact = true)
        }
        if (s.dashPlacesRow && !st.driving && (st.places.isNotEmpty() || st.prayer != null)) {
            Row(Modifier.fillMaxWidth().height(if (short) 64.dp else 72.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                st.places.take(2).forEach { p ->
                    DashChip(Modifier.weight(1f), onClick = { act.openPlace(p) }) {
                        CircleIcon(placeIcon(p), st.accent, 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            placeDistance(st, p)?.let { Text(it, fontSize = 12.sp, color = TextSecondary, maxLines = 1) }
                        }
                    }
                }
                st.prayer?.let { pr ->
                    DashChip(Modifier.weight(1f), onClick = {}) {
                        Icon(Icons.Rounded.Mosque, null, tint = Gold, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${stringResource(prayerName(pr.next.prayer))} ${formatTime(pr.next.at, s.use24h, appLocale())}",
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            val now = rememberNow(30_000L)
                            Text(countdown(pr.next.at - now), fontSize = 12.sp, color = Gold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private val Gold = androidx.compose.ui.graphics.Color(0xFFE7C27D)

@Composable
private fun DashTopBar(st: HomeState, act: HomeActions) {
    val s = st.settings
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val now = rememberNow(if (s.clockSeconds) 1_000L else 10_000L)
        Text(formatTime(now, s.use24h, appLocale()), fontSize = 30.sp, fontWeight = FontWeight.Light, color = TextPrimary, maxLines = 1)
        st.weather?.let { w ->
            Icon(weatherIcon(com.carhome.launcher.weatherKind(w.code), w.isDay), null, tint = weatherTint(com.carhome.launcher.weatherKind(w.code), w.isDay), modifier = Modifier.size(22.dp))
            Text("${w.temperatureC.roundToInt()}°", fontSize = 17.sp, color = TextPrimary)
        }
        Spacer(Modifier.weight(1f))
        if (s.internetBadge) InternetButton(st.net, act.openInternet, size = 44.dp)
        GlassIconButton(Icons.Rounded.Star, stringResource(R.string.favorites), act.openFavs, size = 44.dp)
        GlassIconButton(Icons.Rounded.Apps, stringResource(R.string.all_apps), act.openApps, size = 44.dp)
        GlassIconButton(Icons.Rounded.Settings, stringResource(R.string.settings), act.openSettings, size = 44.dp)
    }
}

@Composable
private fun DashChip(modifier: Modifier, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxHeight()
            .pressable(shape, onClick)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}
