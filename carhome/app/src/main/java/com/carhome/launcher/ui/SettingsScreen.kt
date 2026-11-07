package com.carhome.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.ACCENTS
import com.carhome.launcher.AUTO_DELAYS
import com.carhome.launcher.AlertMode
import com.carhome.launcher.AppEntry
import com.carhome.launcher.AppLanguage
import com.carhome.launcher.BREAK_AFTER
import com.carhome.launcher.Background
import com.carhome.launcher.BubbleSize
import com.carhome.launcher.CardType
import com.carhome.launcher.ColumnWidth
import com.carhome.launcher.DRIVING_SPEEDS
import com.carhome.launcher.DayScore
import com.carhome.launcher.GpsDiag
import com.carhome.launcher.SpeedSource
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.MAX_CARDS
import com.carhome.launcher.NIGHT_DIMS
import com.carhome.launcher.NightMode
import com.carhome.launcher.NoRoadData
import com.carhome.launcher.OIL_INTERVALS
import com.carhome.launcher.ObdStatus
import com.carhome.launcher.PARKED_AFTER
import com.carhome.launcher.PairedDevice
import com.carhome.launcher.Place
import com.carhome.launcher.Prayer
import com.carhome.launcher.PrayerInfo
import com.carhome.launcher.PrayerMethod
import com.carhome.launcher.R
import com.carhome.launcher.REMINDER_MINUTES
import com.carhome.launcher.SPEED_LIMIT_MAX
import com.carhome.launcher.SPEED_LIMIT_MIN
import com.carhome.launcher.SPEED_MARGINS
import com.carhome.launcher.SettingsPage
import com.carhome.launcher.Side
import com.carhome.launcher.SpeedStyle
import com.carhome.launcher.TILE_COUNTS
import com.carhome.launcher.TextSize
import com.carhome.launcher.TripReset
import com.carhome.launcher.VoiceStatus
import com.carhome.launcher.VolumeBoost
import com.carhome.launcher.prayerName
import java.util.Locale
import kotlin.math.roundToInt

data class SettingsState(
    val settings: LauncherSettings,
    val accent: Color,
    val page: SettingsPage,
    val hasMusicAccess: Boolean,
    val hasLocation: Boolean,
    val hasFineLocation: Boolean,
    val canOverlay: Boolean,
    val hasNotifications: Boolean,
    val canWriteSystem: Boolean,
    val hasBluetooth: Boolean,
    val voiceStatus: VoiceStatus,
    val appsByPackage: Map<String, AppEntry>,
    val places: List<Place>,
    val odometerKm: Double,
    val oilChangedAtKm: Double,
    val obdStatus: ObdStatus,
    val pairedDevices: List<PairedDevice>,
    val scores: List<DayScore>,
    val prayer: PrayerInfo?,
    val parkedOnly: Set<String>,
    val gps: GpsDiag
)

class SettingsActions(
    val back: () -> Unit,
    val openPage: (SettingsPage) -> Unit,
    val update: ((LauncherSettings) -> LauncherSettings) -> Unit,
    val setDefaultHome: () -> Unit,
    val musicAccess: () -> Unit,
    val locationAccess: () -> Unit,
    val openLocationSettings: () -> Unit,
    val overlayAccess: () -> Unit,
    val notificationAccess: () -> Unit,
    val writeSystemAccess: () -> Unit,
    val bluetoothAccess: () -> Unit,
    val testAlert: () -> Unit,
    val testChime: () -> Unit,
    val testVoice: () -> Unit,
    val newTrip: () -> Unit,
    val oilChanged: () -> Unit,
    val adjustOil: (Double) -> Unit,
    val pickAutoApp: () -> Unit,
    val pickPhoto: () -> Unit,
    val addPlace: (String, Boolean, String) -> Boolean,
    val removePlace: (String) -> Unit,
    val toggleParkedOnly: (String) -> Unit
)

@Composable
fun SettingsScreen(st: SettingsState, act: SettingsActions) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), act.back)
            Spacer(Modifier.width(16.dp))
            Text(stringResource(R.string.settings), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SideMenu(st.page, st.accent, act.openPage, Modifier.width(250.dp).fillMaxHeight())
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (st.page) {
                    SettingsPage.HOME_LAYOUT -> HomeLayoutPage(st, act)
                    SettingsPage.SPEED -> SpeedPage(st, act)
                    SettingsPage.PRAYER -> PrayerPage(st, act)
                    SettingsPage.TRIP -> TripPage(st, act)
                    SettingsPage.STARTUP -> StartupPage(st, act)
                    SettingsPage.DISPLAY -> DisplayPage(st, act)
                    SettingsPage.SMART -> SmartPage(st, act)
                    SettingsPage.PERMISSIONS -> PermissionsPage(st, act)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun pageIcon(p: SettingsPage): ImageVector = when (p) {
    SettingsPage.HOME_LAYOUT -> Icons.Rounded.Home
    SettingsPage.SPEED -> Icons.Rounded.Speed
    SettingsPage.PRAYER -> Icons.Rounded.Mosque
    SettingsPage.TRIP -> Icons.Rounded.DirectionsCar
    SettingsPage.STARTUP -> Icons.Rounded.PlayCircle
    SettingsPage.DISPLAY -> Icons.Rounded.Palette
    SettingsPage.SMART -> Icons.Rounded.AutoAwesome
    SettingsPage.PERMISSIONS -> Icons.Rounded.Security
}

private fun pageLabel(p: SettingsPage): Int = when (p) {
    SettingsPage.HOME_LAYOUT -> R.string.page_home
    SettingsPage.SPEED -> R.string.page_speed
    SettingsPage.PRAYER -> R.string.page_prayer
    SettingsPage.TRIP -> R.string.page_trip
    SettingsPage.STARTUP -> R.string.page_startup
    SettingsPage.DISPLAY -> R.string.page_display
    SettingsPage.SMART -> R.string.page_smart
    SettingsPage.PERMISSIONS -> R.string.page_permissions
}

@Composable
private fun SideMenu(current: SettingsPage, accent: Color, onOpen: (SettingsPage) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.glass().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SettingsPage.entries.forEach { p ->
            val selected = p == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onOpen(p) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(pageIcon(p), null, tint = if (selected) accent else TextSecondary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(pageLabel(p)), fontSize = 16.sp, color = TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}

// ── Building blocks ───────────────────────────────────────────────────────────

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().glass().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        content()
    }
}

@Composable
private fun Hint(text: String) = Text(text, fontSize = 14.sp, color = TextSecondary)

@Composable
private fun Toggle(title: String, checked: Boolean, accent: Color, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
        )
    }
}

/** A row of choice chips; [selected] is the index of the current choice. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(title: String?, labels: List<String>, selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) Text(title, fontSize = 15.sp, color = TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            labels.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (on) accent else Color.White.copy(alpha = 0.06f))
                        .clickable { onSelect(i) }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 16.sp, color = if (on) Color.White else TextPrimary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun Stepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        GlassIconButton(Icons.Rounded.KeyboardArrowDown, "−", onMinus, size = 48.dp)
        Text(value, fontSize = 22.sp, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp))
        GlassIconButton(Icons.Rounded.KeyboardArrowUp, "+", onPlus, size = 48.dp)
    }
}

@Composable
private fun ActionButton(label: String, accent: Color, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (filled) accent else accent.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, color = if (filled) Color.White else TextPrimary, fontWeight = FontWeight.Medium)
    }
}

/** A permission or system screen the user may need to open, with its status. */
@Composable
private fun AccessRow(icon: ImageVector, title: String, granted: Boolean?, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 17.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        when (granted) {
            true -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = OkGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.allowed), fontSize = 14.sp, color = OkGreen)
            }
            false -> Text(stringResource(R.string.tap_to_allow), fontSize = 14.sp, color = WarnAmber)
            null -> Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = TextSecondary)
        }
    }
}

@Composable
private fun Radios(labels: List<String>, selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    Column {
        labels.forEachIndexed { i, label ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onSelect(i) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = i == selected, onClick = { onSelect(i) }, colors = RadioButtonDefaults.colors(selectedColor = accent))
                Text(label, fontSize = 16.sp, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun minutes(n: Int) = stringResource(R.string.n_min, n)

// ── Pages ─────────────────────────────────────────────────────────────────────

private fun cardLabel(c: CardType): Int = when (c) {
    CardType.CLOCK -> R.string.card_clock
    CardType.SPEED -> R.string.card_speed
    CardType.MUSIC -> R.string.card_music
    CardType.WEATHER -> R.string.card_weather
    CardType.PRAYER -> R.string.card_prayer
    CardType.TRIP -> R.string.card_trip
    CardType.CAR -> R.string.card_car
}

@Composable
private fun HomeLayoutPage(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    Section(stringResource(R.string.home_tiles)) {
        Chips(null, TILE_COUNTS.map { "$it" }, TILE_COUNTS.indexOf(s.tileCount), a) { i -> act.update { it.copy(tileCount = TILE_COUNTS[i]) } }
        Toggle(stringResource(R.string.tile_labels), s.showTileLabels, a) { v -> act.update { it.copy(showTileLabels = v) } }
    }
    Section(stringResource(R.string.info_cards)) {
        Hint(stringResource(R.string.info_cards_hint, MAX_CARDS))
        val order = s.cards + CardType.entries.filter { it !in s.cards }
        order.forEach { c ->
            val on = c in s.cards
            val idx = s.cards.indexOf(c)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) a else Color.Transparent)
                        .border(2.dp, if (on) a else TextSecondary, RoundedCornerShape(10.dp))
                        .clickable {
                            act.update { cur ->
                                when {
                                    c in cur.cards -> cur.copy(cards = cur.cards - c)
                                    cur.cards.size < MAX_CARDS -> cur.copy(cards = cur.cards + c)
                                    else -> cur
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(14.dp))
                Text(stringResource(cardLabel(c)), fontSize = 17.sp, color = if (on) TextPrimary else TextSecondary, modifier = Modifier.weight(1f))
                if (on) {
                    GlassIconButton(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up), {
                        if (idx > 0) act.update { cur -> cur.copy(cards = cur.cards.toMutableList().apply { add(idx - 1, removeAt(idx)) }) }
                    }, size = 44.dp)
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down), {
                        if (idx < s.cards.size - 1) act.update { cur -> cur.copy(cards = cur.cards.toMutableList().apply { add(idx + 1, removeAt(idx)) }) }
                    }, size = 44.dp)
                }
            }
        }
    }
    Section(stringResource(R.string.top_bar)) {
        Toggle(stringResource(R.string.show_weather), s.showWeather, a) { v -> act.update { it.copy(showWeather = v) } }
        Toggle(stringResource(R.string.show_prayer_chip), s.showPrayerChip, a) { v -> act.update { it.copy(showPrayerChip = v) } }
    }
    Section(stringResource(R.string.layout)) {
        Chips(stringResource(R.string.cards_side), listOf(stringResource(R.string.side_start), stringResource(R.string.side_end)), Side.entries.indexOf(s.infoSide), a) { i ->
            act.update { it.copy(infoSide = Side.entries[i]) }
        }
        Chips(stringResource(R.string.cards_width), listOf(stringResource(R.string.narrow), stringResource(R.string.normal), stringResource(R.string.wide)), ColumnWidth.entries.indexOf(s.infoWidth), a) { i ->
            act.update { it.copy(infoWidth = ColumnWidth.entries[i]) }
        }
        Chips(stringResource(R.string.text_size), listOf(stringResource(R.string.normal), stringResource(R.string.large), stringResource(R.string.extra_large)), TextSize.entries.indexOf(s.textSize), a) { i ->
            act.update { it.copy(textSize = TextSize.entries[i]) }
        }
    }
    Section(stringResource(R.string.card_clock)) {
        Toggle(stringResource(R.string.clock_24h), s.use24h, a) { v -> act.update { it.copy(use24h = v) } }
        Toggle(stringResource(R.string.clock_seconds), s.clockSeconds, a) { v -> act.update { it.copy(clockSeconds = v) } }
        Toggle(stringResource(R.string.show_date), s.showDate, a) { v -> act.update { it.copy(showDate = v) } }
        Toggle(stringResource(R.string.show_hijri), s.showHijri, a) { v -> act.update { it.copy(showHijri = v) } }
    }
    Section(stringResource(R.string.card_speed)) {
        Chips(stringResource(R.string.speed_style), listOf(stringResource(R.string.style_gauge), stringResource(R.string.style_number)), SpeedStyle.entries.indexOf(s.speedStyle), a) { i ->
            act.update { it.copy(speedStyle = SpeedStyle.entries[i]) }
        }
        Toggle(stringResource(R.string.unit_kmh), s.useKmh, a, stringResource(R.string.unit_kmh_hint)) { v -> act.update { it.copy(useKmh = v) } }
    }
}

@Composable
private fun SpeedPage(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    val unit = stringResource(if (s.useKmh) R.string.kmh else R.string.mph)
    GpsStatusSection(st, act)
    Section(stringResource(R.string.speed_source)) {
        Chips(null, listOf(stringResource(R.string.src_auto), stringResource(R.string.src_gps), stringResource(R.string.src_car)), SpeedSource.entries.indexOf(s.speedSource), a) { i ->
            act.update { it.copy(speedSource = SpeedSource.entries[i]) }
            if (SpeedSource.entries[i] == SpeedSource.CAR && !s.obd) act.update { it.copy(obd = true) }
        }
        Hint(stringResource(R.string.speed_source_hint))
        Toggle(stringResource(R.string.approx_speed_toggle), s.approxSpeed, a, stringResource(R.string.approx_speed_hint)) { v ->
            act.update { it.copy(approxSpeed = v) }
        }
    }
    Section(stringResource(R.string.speed_alert)) {
        Toggle(stringResource(R.string.speed_alert_on), s.speedAlert, a) { v -> act.update { it.copy(speedAlert = v) } }
        if (s.speedAlert) BackgroundNotice(st, act)
        Stepper(
            stringResource(R.string.speed_limit), "${s.speedLimit} $unit",
            onMinus = { act.update { it.copy(speedLimit = (it.speedLimit - 5).coerceAtLeast(SPEED_LIMIT_MIN)) } },
            onPlus = { act.update { it.copy(speedLimit = (it.speedLimit + 5).coerceAtMost(SPEED_LIMIT_MAX)) } }
        )
        Text(stringResource(R.string.alert_type), fontSize = 15.sp, color = TextSecondary)
        Radios(
            listOf(stringResource(R.string.alert_screen), stringResource(R.string.alert_beep_once), stringResource(R.string.alert_beep_repeat)),
            AlertMode.entries.indexOf(s.alertMode), a
        ) { i -> act.update { it.copy(alertMode = AlertMode.entries[i]) } }
        Chips(stringResource(R.string.grace_margin), SPEED_MARGINS.map { if (it == 0) "0" else "+$it" }, SPEED_MARGINS.indexOf(s.speedMargin), a) { i ->
            act.update { it.copy(speedMargin = SPEED_MARGINS[i]) }
        }
        Chips(stringResource(R.string.alert_channel), listOf(stringResource(R.string.channel_nav), stringResource(R.string.channel_media)),
            com.carhome.launcher.SoundChannel.entries.indexOf(s.alertChannel), a) { i ->
            act.update { it.copy(alertChannel = com.carhome.launcher.SoundChannel.entries[i]) }
        }
        Hint(stringResource(R.string.alert_channel_hint))
        ActionButton(stringResource(R.string.test_alert), a, onClick = act.testAlert)
    }
    Section(stringResource(R.string.road_limit)) {
        Toggle(stringResource(R.string.road_limit_on), s.roadLimit, a, stringResource(R.string.road_limit_hint)) { v -> act.update { it.copy(roadLimit = v) } }
        if (s.roadLimit) {
            Chips(stringResource(R.string.road_no_data), listOf(stringResource(R.string.road_use_mine), stringResource(R.string.road_no_alert)), NoRoadData.entries.indexOf(s.roadNoData), a) { i ->
                act.update { it.copy(roadNoData = NoRoadData.entries[i]) }
            }
            Toggle(stringResource(R.string.road_cap, s.speedLimit), s.roadCap, a) { v -> act.update { it.copy(roadCap = v) } }
            Hint(stringResource(R.string.road_limit_note))
        }
    }
    Section(stringResource(R.string.bubble)) {
        Toggle(stringResource(R.string.bubble_on), s.bubble, a, stringResource(R.string.bubble_hint)) { v ->
            act.update { it.copy(bubble = v) }
            if (v && !st.canOverlay) act.overlayAccess()
        }
        if (s.bubble) {
            AccessRow(Icons.Rounded.Layers, stringResource(R.string.overlay_access), st.canOverlay, a, act.overlayAccess)
            Chips(stringResource(R.string.bubble_size), listOf(stringResource(R.string.small), stringResource(R.string.medium), stringResource(R.string.large)), BubbleSize.entries.indexOf(s.bubbleSize), a) { i ->
                act.update { it.copy(bubbleSize = BubbleSize.entries[i]) }
            }
            Toggle(stringResource(R.string.bubble_lock), s.bubbleLock, a) { v -> act.update { it.copy(bubbleLock = v) } }
            Toggle(stringResource(R.string.bubble_only_moving), s.bubbleOnlyMoving, a) { v -> act.update { it.copy(bubbleOnlyMoving = v) } }
        }
    }
    Section(stringResource(R.string.background_tracking)) {
        Toggle(stringResource(R.string.background_tracking_on), s.backgroundTracking, a, stringResource(R.string.background_tracking_hint)) { v ->
            act.update { it.copy(backgroundTracking = v) }
            if (v && !st.hasNotifications) act.notificationAccess()
        }
    }
    Section(stringResource(R.string.speed_volume)) {
        Hint(stringResource(R.string.speed_volume_hint))
        Chips(null, listOf(stringResource(R.string.off), stringResource(R.string.low), stringResource(R.string.medium), stringResource(R.string.high)), VolumeBoost.entries.indexOf(s.speedVolume), a) { i ->
            act.update { it.copy(speedVolume = VolumeBoost.entries[i]) }
        }
    }
}

/** Shown where a feature needs CarHome running behind other apps to keep working. */
@Composable
private fun BackgroundNotice(st: SettingsState, act: SettingsActions) {
    if (st.settings.backgroundTracking) return
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WarnAmber.copy(alpha = 0.12f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Warning, null, tint = WarnAmber, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.bg_notice), fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        ActionButton(stringResource(R.string.turn_on), st.accent, filled = true) {
            act.update { it.copy(backgroundTracking = true) }
            if (!st.hasNotifications) act.notificationAccess()
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String, good: Boolean?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 16.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value, fontSize = 16.sp, fontWeight = FontWeight.Medium,
            color = when (good) { true -> OkGreen; false -> WarnAmber; null -> TextPrimary }
        )
    }
}

/** Live GPS diagnostics, so the reason speed doesn't show is visible on the box itself. */
@Composable
private fun GpsStatusSection(st: SettingsState, act: SettingsActions) {
    val g = st.gps
    val a = st.accent
    Section(stringResource(R.string.gps_status)) {
        DiagRow(stringResource(R.string.gps_permission), stringResource(if (st.hasFineLocation) R.string.allowed else R.string.tap_to_allow), st.hasFineLocation)
        DiagRow(stringResource(R.string.gps_location_setting), stringResource(if (g.locationOn) R.string.gps_on else R.string.gps_off_value), g.locationOn)
        DiagRow(stringResource(R.string.gps_chip), stringResource(if (g.hasGpsChip) R.string.gps_found else R.string.gps_missing), g.hasGpsChip)
        DiagRow(stringResource(R.string.gps_satellites), stringResource(R.string.gps_sats_value, g.satellitesSeen, g.satellitesUsed), g.satellitesUsed > 0)
        val fix = g.lastFixSecondsAgo
        DiagRow(
            stringResource(R.string.gps_last_fix),
            if (fix == null) stringResource(R.string.gps_never)
            else stringResource(R.string.gps_last_fix_value, fix.toInt(), g.lastFixProvider ?: "?", g.lastFixAccuracy ?: 0),
            fix != null && fix <= 6
        )
        DiagRow(stringResource(R.string.gps_car_speed), stringResource(if (g.carSpeed) R.string.gps_car_yes else R.string.gps_car_no), if (g.carSpeed) true else null)
        DiagRow(stringResource(R.string.gps_approx), stringResource(if (g.approxSpeed) R.string.gps_in_use else R.string.gps_not_in_use), if (g.approxSpeed) true else null)
        DiagRow(stringResource(R.string.gps_sources), g.providers.joinToString(", ").ifEmpty { "—" }, null)
        Hint(stringResource(R.string.gps_help))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(stringResource(R.string.open_location_settings), a, onClick = act.openLocationSettings)
            if (!st.hasFineLocation) ActionButton(stringResource(R.string.location_access), a, onClick = act.locationAccess)
        }
    }
}

@Composable
private fun PrayerPage(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    val locale = appLocale()
    Section(stringResource(R.string.prayer_times)) {
        Chips(
            stringResource(R.string.prayer_method),
            listOf(stringResource(R.string.m_umm_al_qura), stringResource(R.string.m_mwl), stringResource(R.string.m_egypt), stringResource(R.string.m_karachi)),
            PrayerMethod.entries.indexOf(s.prayerMethod), a
        ) { i -> act.update { it.copy(prayerMethod = PrayerMethod.entries[i]) } }
        Chips(stringResource(R.string.asr_method), listOf(stringResource(R.string.asr_standard), stringResource(R.string.asr_hanafi)), if (s.asrHanafi) 1 else 0, a) { i ->
            act.update { it.copy(asrHanafi = i == 1) }
        }
        val p = st.prayer
        if (p == null) Hint(stringResource(R.string.prayer_waiting))
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Prayer.entries.forEach { pr ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(prayerName(pr)), fontSize = 14.sp, color = TextSecondary)
                    Text(formatTime(p.day.timeOf(pr), s.use24h, locale), fontSize = 18.sp, color = TextPrimary)
                }
            }
        }
        Hint(stringResource(R.string.prayer_offline_note))
    }
    Section(stringResource(R.string.prayer_reminder)) {
        Chips(null, REMINDER_MINUTES.map { if (it == 0) stringResource(R.string.off) else minutes(it) }, REMINDER_MINUTES.indexOf(s.prayerReminder), a) { i ->
            act.update { it.copy(prayerReminder = REMINDER_MINUTES[i]) }
        }
        if (s.prayerReminder > 0) {
            Toggle(stringResource(R.string.prayer_chime), s.prayerChime, a) { v -> act.update { it.copy(prayerChime = v) } }
            ActionButton(stringResource(R.string.test_chime), a, onClick = act.testChime)
        }
    }
    Section(stringResource(R.string.nearby)) {
        Toggle(stringResource(R.string.nearby_on), s.nearbyButtons, a, stringResource(R.string.nearby_hint)) { v -> act.update { it.copy(nearbyButtons = v) } }
    }
}

@Composable
private fun TripPage(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    val distUnit = stringResource(if (s.useKmh) R.string.km else R.string.mi)
    fun dist(km: Double) = if (s.useKmh) km else km / 1.609344
    Section(stringResource(R.string.card_trip)) {
        Chips(stringResource(R.string.trip_reset), listOf(stringResource(R.string.trip_each_start), stringResource(R.string.trip_manual)), TripReset.entries.indexOf(s.tripReset), a) { i ->
            act.update { it.copy(tripReset = TripReset.entries[i]) }
        }
        BackgroundNotice(st, act)
        ActionButton(stringResource(R.string.new_trip), a, onClick = act.newTrip)
        Toggle(stringResource(R.string.drive_score), s.driveScore, a, stringResource(R.string.drive_score_hint)) { v -> act.update { it.copy(driveScore = v) } }
        if (s.driveScore) ScoreTrend(st.scores)
    }
    Section(stringResource(R.string.oil)) {
        Toggle(stringResource(R.string.oil_on), s.oilReminder, a) { v -> act.update { it.copy(oilReminder = v) } }
        if (s.oilReminder) {
            Chips(stringResource(R.string.oil_every), OIL_INTERVALS.map { "${dist(it.toDouble()).roundToInt()} $distUnit" }, OIL_INTERVALS.indexOf(s.oilInterval), a) { i ->
                act.update { it.copy(oilInterval = OIL_INTERVALS[i]) }
            }
            val since = (st.odometerKm - st.oilChangedAtKm).coerceAtLeast(0.0)
            Stepper(
                stringResource(R.string.oil_since), "${dist(since).roundToInt()} $distUnit",
                onMinus = { act.adjustOil(-500.0) }, onPlus = { act.adjustOil(500.0) }
            )
            ActionButton(stringResource(R.string.oil_done), a, onClick = act.oilChanged)
            Hint(stringResource(R.string.oil_hint, dist(st.odometerKm).roundToInt(), distUnit))
        }
    }
    Section(stringResource(R.string.fuel_cost)) {
        Toggle(stringResource(R.string.fuel_cost_on), s.fuelCost, a) { v -> act.update { it.copy(fuelCost = v) } }
        if (s.fuelCost) {
            Stepper(
                stringResource(R.string.fuel_consumption), String.format(Locale.US, "%.1f", s.fuelConsumption),
                onMinus = { act.update { it.copy(fuelConsumption = (it.fuelConsumption - 0.5f).coerceAtLeast(3f)) } },
                onPlus = { act.update { it.copy(fuelConsumption = (it.fuelConsumption + 0.5f).coerceAtMost(30f)) } }
            )
            Stepper(
                stringResource(R.string.fuel_price), String.format(Locale.US, "%.2f", s.fuelPrice),
                // Same 0.10–20 range the saved value is kept in, so it never goes negative
                onMinus = { act.update { it.copy(fuelPrice = (((it.fuelPrice - 0.05f) * 100).roundToInt() / 100f).coerceIn(0.1f, 20f)) } },
                onPlus = { act.update { it.copy(fuelPrice = (((it.fuelPrice + 0.05f) * 100).roundToInt() / 100f).coerceIn(0.1f, 20f)) } }
            )
            Hint(stringResource(R.string.fuel_hint))
        }
    }
    Section(stringResource(R.string.break_reminder)) {
        Toggle(stringResource(R.string.break_on), s.breakReminder, a) { v -> act.update { it.copy(breakReminder = v) } }
        if (s.breakReminder) {
            Chips(stringResource(R.string.break_after), BREAK_AFTER.map { minutes(it) }, BREAK_AFTER.indexOf(s.breakAfterMin), a) { i ->
                act.update { it.copy(breakAfterMin = BREAK_AFTER[i]) }
            }
        }
    }
    Section(stringResource(R.string.card_car)) {
        Toggle(stringResource(R.string.obd_on), s.obd, a, stringResource(R.string.obd_hint)) { v ->
            act.update { it.copy(obd = v) }
            if (v && !st.hasBluetooth) act.bluetoothAccess()
        }
        if (s.obd) {
            AccessRow(Icons.Rounded.Bluetooth, stringResource(R.string.bluetooth_access), st.hasBluetooth, a, act.bluetoothAccess)
            if (st.pairedDevices.isEmpty()) Hint(stringResource(R.string.obd_pair_first))
            else Radios(st.pairedDevices.map { it.name }, st.pairedDevices.indexOfFirst { it.address == s.obdAddress }, a) { i ->
                act.update { it.copy(obdAddress = st.pairedDevices[i].address) }
            }
            Hint(stringResource(R.string.obd_status, stringResource(obdStatusText(st.obdStatus))))
        }
    }
}

fun obdStatusText(s: ObdStatus): Int = when (s) {
    ObdStatus.OFF -> R.string.obd_off
    ObdStatus.NO_BLUETOOTH -> R.string.obd_no_bt
    ObdStatus.NO_PERMISSION -> R.string.obd_no_permission
    ObdStatus.CHOOSE_ADAPTER -> R.string.obd_choose
    ObdStatus.CONNECTING -> R.string.obd_connecting
    ObdStatus.CONNECTED -> R.string.obd_connected
    ObdStatus.FAILED -> R.string.obd_failed
}

@Composable
private fun ScoreTrend(scores: List<DayScore>) {
    val last = scores.takeLast(7)
    if (last.isEmpty()) { Hint(stringResource(R.string.score_none)); return }
    Text(stringResource(R.string.score_week), fontSize = 15.sp, color = TextSecondary)
    Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        last.forEach { d ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text("${d.average}", fontSize = 13.sp, color = TextPrimary)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((d.average * 0.7f).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(scoreColor(d.average))
                )
                Text(d.day.takeLast(5), fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun StartupPage(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    Section(stringResource(R.string.welcome)) {
        Toggle(stringResource(R.string.welcome_on), s.welcome, a, stringResource(R.string.welcome_hint)) { v -> act.update { it.copy(welcome = v) } }
        if (s.welcome) {
            OutlinedTextField(
                value = s.welcomeName,
                onValueChange = { v -> act.update { it.copy(welcomeName = v.take(30)) } },
                singleLine = true,
                label = { Text(stringResource(R.string.welcome_name)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Section(stringResource(R.string.auto_start)) {
        Toggle(stringResource(R.string.auto_start_on), s.autoStart, a, stringResource(R.string.auto_start_hint)) { v -> act.update { it.copy(autoStart = v) } }
        if (s.autoStart) {
            Toggle(stringResource(R.string.auto_music), s.autoMusic, a) { v -> act.update { it.copy(autoMusic = v) } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.auto_app), fontSize = 17.sp, color = TextPrimary)
                    Text(
                        st.appsByPackage[s.autoApp]?.label ?: stringResource(R.string.none),
                        fontSize = 14.sp, color = TextSecondary
                    )
                }
                if (s.autoApp.isNotEmpty()) {
                    GlassIconButton(Icons.Rounded.Close, stringResource(R.string.remove), { act.update { it.copy(autoApp = "") } }, size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                }
                ActionButton(stringResource(R.string.choose), a, onClick = act.pickAutoApp)
            }
            Chips(stringResource(R.string.auto_delay), AUTO_DELAYS.map { stringResource(R.string.n_sec, it) }, AUTO_DELAYS.indexOf(s.autoDelay), a) { i ->
                act.update { it.copy(autoDelay = AUTO_DELAYS[i]) }
            }
        }
    }
}

@Composable
private fun DisplayPage(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    Section(stringResource(R.string.accent_color)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ACCENTS.forEach { c ->
                val color = Color(c)
                val selected = c == s.accent
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(if (selected) 3.dp else 0.dp, Color.White, CircleShape)
                        .clickable { act.update { it.copy(accent = c) } },
                    contentAlignment = Alignment.Center
                ) { if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White) }
            }
        }
        Toggle(stringResource(R.string.song_colours), s.songColours, a, stringResource(R.string.song_colours_hint)) { v -> act.update { it.copy(songColours = v) } }
    }
    Section(stringResource(R.string.background)) {
        Chips(null, listOf(stringResource(R.string.bg_classic), stringResource(R.string.bg_sky), stringResource(R.string.bg_photo)), Background.entries.indexOf(s.background), a) { i ->
            if (Background.entries[i] == Background.PHOTO) act.pickPhoto() else act.update { it.copy(background = Background.entries[i]) }
        }
        if (s.background == Background.SKY) Hint(stringResource(R.string.bg_sky_hint))
        if (s.background == Background.PHOTO) ActionButton(stringResource(R.string.choose_photo), a, onClick = act.pickPhoto)
    }
    Section(stringResource(R.string.night_mode)) {
        Chips(
            null,
            listOf(stringResource(R.string.off), stringResource(R.string.night_auto), stringResource(R.string.night_always), stringResource(R.string.night_custom)),
            NightMode.entries.indexOf(s.nightMode), a
        ) { i -> act.update { it.copy(nightMode = NightMode.entries[i]) } }
        if (s.nightMode == NightMode.AUTO) Hint(stringResource(R.string.night_auto_hint))
        if (s.nightMode == NightMode.CUSTOM) {
            Stepper(stringResource(R.string.night_from), String.format(Locale.US, "%02d:00", s.nightFrom),
                onMinus = { act.update { it.copy(nightFrom = (it.nightFrom + 23) % 24) } },
                onPlus = { act.update { it.copy(nightFrom = (it.nightFrom + 1) % 24) } })
            Stepper(stringResource(R.string.night_to), String.format(Locale.US, "%02d:00", s.nightTo),
                onMinus = { act.update { it.copy(nightTo = (it.nightTo + 23) % 24) } },
                onPlus = { act.update { it.copy(nightTo = (it.nightTo + 1) % 24) } })
        }
        if (s.nightMode != NightMode.OFF) {
            Chips(stringResource(R.string.night_dim), NIGHT_DIMS.map { "$it%" }, NIGHT_DIMS.indexOf(s.nightDim), a) { i ->
                act.update { it.copy(nightDim = NIGHT_DIMS[i]) }
            }
            Toggle(stringResource(R.string.night_system), s.nightSystem, a, stringResource(R.string.night_system_hint)) { v ->
                act.update { it.copy(nightSystem = v) }
                if (v && !st.canWriteSystem) act.writeSystemAccess()
            }
            if (s.nightSystem) AccessRow(Icons.Rounded.Brightness6, stringResource(R.string.write_settings_access), st.canWriteSystem, a, act.writeSystemAccess)
        }
    }
    Section(stringResource(R.string.language)) {
        Chips(null, listOf(stringResource(R.string.lang_system), "العربية", "English"), AppLanguage.entries.indexOf(s.language), a) { i ->
            act.update { it.copy(language = AppLanguage.entries[i]) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartPage(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    var addingPlace by remember { mutableStateOf(false) }
    Section(stringResource(R.string.driving_mode)) {
        Toggle(stringResource(R.string.driving_mode_on), s.drivingMode, a, stringResource(R.string.driving_mode_hint)) { v -> act.update { it.copy(drivingMode = v) } }
        if (s.drivingMode) {
            val unit = stringResource(if (s.useKmh) R.string.kmh else R.string.mph)
            Chips(stringResource(R.string.driving_above), DRIVING_SPEEDS.map { "$it $unit" }, DRIVING_SPEEDS.indexOf(s.drivingSpeed), a) { i ->
                act.update { it.copy(drivingSpeed = DRIVING_SPEEDS[i]) }
            }
        }
    }
    Section(stringResource(R.string.swipe_music)) {
        Toggle(stringResource(R.string.swipe_music_on), s.swipeMusic, a, stringResource(R.string.swipe_music_hint)) { v -> act.update { it.copy(swipeMusic = v) } }
    }
    Section(stringResource(R.string.place_tiles)) {
        Toggle(stringResource(R.string.place_tiles_on), s.placeTiles, a, stringResource(R.string.place_tiles_hint)) { v -> act.update { it.copy(placeTiles = v) } }
        if (s.placeTiles) {
            st.places.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = a)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontSize = 17.sp, color = TextPrimary)
                        Text(if (p.lat != null) stringResource(R.string.place_saved_location) else p.address, fontSize = 13.sp, color = TextSecondary)
                    }
                    GlassIconButton(Icons.Rounded.Close, stringResource(R.string.remove), { act.removePlace(p.id) }, size = 44.dp)
                }
            }
            ActionButton(stringResource(R.string.add_place), a, onClick = { addingPlace = true })
        }
    }
    Section(stringResource(R.string.smart_tile)) {
        Toggle(stringResource(R.string.smart_tile_on), s.smartTile, a, stringResource(R.string.smart_tile_explain)) { v -> act.update { it.copy(smartTile = v) } }
    }
    Section(stringResource(R.string.parked_only_title)) {
        Toggle(stringResource(R.string.parked_only_on), s.parkedOnly, a, stringResource(R.string.parked_only_hint)) { v -> act.update { it.copy(parkedOnly = v) } }
        if (s.parkedOnly && st.parkedOnly.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                st.parkedOnly.forEach { pkg ->
                    Row(
                        modifier = Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f))
                            .clickable { act.toggleParkedOnly(pkg) }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(st.appsByPackage[pkg]?.label ?: pkg, fontSize = 15.sp, color = TextPrimary)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Close, stringResource(R.string.remove), tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
    Section(stringResource(R.string.dust_warning)) {
        Toggle(stringResource(R.string.dust_warning_on), s.dustWarning, a, stringResource(R.string.dust_warning_hint)) { v -> act.update { it.copy(dustWarning = v) } }
    }
    Section(stringResource(R.string.parked_screen)) {
        Toggle(stringResource(R.string.parked_screen_on), s.parkedScreen, a, stringResource(R.string.parked_screen_hint)) { v -> act.update { it.copy(parkedScreen = v) } }
        if (s.parkedScreen) {
            Chips(stringResource(R.string.parked_after), PARKED_AFTER.map { minutes(it) }, PARKED_AFTER.indexOf(s.parkedAfterMin), a) { i ->
                act.update { it.copy(parkedAfterMin = PARKED_AFTER[i]) }
            }
        }
    }
    Section(stringResource(R.string.voice)) {
        Toggle(stringResource(R.string.voice_on), s.voice, a, stringResource(R.string.voice_hint)) { v -> act.update { it.copy(voice = v) } }
        if (s.voice) {
            Toggle(stringResource(R.string.voice_speed), s.voiceSpeed, a) { v -> act.update { it.copy(voiceSpeed = v) } }
            Toggle(stringResource(R.string.voice_prayer_toggle), s.voicePrayer, a) { v -> act.update { it.copy(voicePrayer = v) } }
            Toggle(stringResource(R.string.voice_reminders), s.voiceReminders, a) { v -> act.update { it.copy(voiceReminders = v) } }
            val status = when (st.voiceStatus) {
                VoiceStatus.READY -> R.string.voice_ready
                VoiceStatus.NO_LANGUAGE -> R.string.voice_no_language
                VoiceStatus.NO_ENGINE -> R.string.voice_no_engine
                else -> R.string.voice_starting
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (st.voiceStatus == VoiceStatus.NO_LANGUAGE || st.voiceStatus == VoiceStatus.NO_ENGINE) {
                    Icon(Icons.Rounded.Warning, null, tint = WarnAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(status), fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                ActionButton(stringResource(R.string.test_voice), a, onClick = act.testVoice)
            }
        }
    }
    if (addingPlace) {
        AddPlaceDialog(a, onDismiss = { addingPlace = false }) { name, current, address ->
            if (act.addPlace(name, current, address)) addingPlace = false
        }
    }
}

@Composable
private fun PermissionsPage(st: SettingsState, act: SettingsActions) {
    val a = st.accent
    Section(stringResource(R.string.page_permissions)) {
        AccessRow(Icons.Rounded.Home, stringResource(R.string.set_default), null, a, act.setDefaultHome)
        AccessRow(Icons.Rounded.LocationOn, stringResource(R.string.location_access), st.hasFineLocation, a, act.locationAccess)
        AccessRow(Icons.Rounded.MusicNote, stringResource(R.string.music_access), st.hasMusicAccess, a, act.musicAccess)
        AccessRow(Icons.Rounded.Layers, stringResource(R.string.overlay_access), st.canOverlay, a, act.overlayAccess)
        AccessRow(Icons.Rounded.Notifications, stringResource(R.string.notification_access), st.hasNotifications, a, act.notificationAccess)
        AccessRow(Icons.Rounded.Brightness6, stringResource(R.string.write_settings_access), st.canWriteSystem, a, act.writeSystemAccess)
        AccessRow(Icons.Rounded.Bluetooth, stringResource(R.string.bluetooth_access), st.hasBluetooth, a, act.bluetoothAccess)
        Hint(stringResource(R.string.permissions_hint))
    }
    Section(stringResource(R.string.about)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Settings, null, tint = a)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.version_line), fontSize = 16.sp, color = TextPrimary)
        }
        Hint(stringResource(R.string.credits))
    }
}
