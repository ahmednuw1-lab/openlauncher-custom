package com.carhome.launcher.ui

import com.carhome.launcher.AccentColors
import com.carhome.launcher.CardFill
import com.carhome.launcher.Corners
import com.carhome.launcher.IconShape
import com.carhome.launcher.IconSize
import com.carhome.launcher.TopBar
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
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
import androidx.compose.material.icons.rounded.Notifications
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
import com.carhome.launcher.UpdateStage
import com.carhome.launcher.WhatsNew
import com.carhome.launcher.UpdateState
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
import androidx.compose.ui.text.input.TextFieldValue
import com.carhome.launcher.PastTrip
import com.carhome.launcher.TripHistory
import com.carhome.launcher.TripTotals
import androidx.compose.ui.res.pluralStringResource
import com.carhome.launcher.CarReminder
import com.carhome.launcher.ReminderType
import com.carhome.launcher.Reminders
import androidx.compose.ui.platform.LocalContext

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
    val gps: GpsDiag,
    val update: UpdateState = UpdateState(),
    val tripHistory: List<PastTrip> = emptyList(),
    val reminders: List<CarReminder> = emptyList()
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
    val toggleParkedOnly: (String) -> Unit,
    val checkUpdate: () -> Unit = {},
    val installUpdate: () -> Unit = {},
    val showWhatsNew: () -> Unit = {},
    val saveBackup: () -> Unit = {},
    val loadBackup: () -> Unit = {},
    val clearTripHistory: () -> Unit = {},
    val addReminder: (CarReminder) -> Unit = {},
    val removeReminder: (String) -> Unit = {},
    val renewReminder: (String) -> Unit = {}
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = onAccent(accent), checkedTrackColor = accent)
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
                        .height(56.dp)
                        .pressable(RoundedCornerShape(28.dp)) { onSelect(i) }
                        .background(if (on) accent else Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 16.sp, color = if (on) onAccent(accent) else TextPrimary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun Stepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        GlassIconButton(Icons.Rounded.KeyboardArrowDown, "−", onMinus, size = 56.dp)
        Text(value, fontSize = 22.sp, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp))
        GlassIconButton(Icons.Rounded.KeyboardArrowUp, "+", onPlus, size = 56.dp)
    }
}

@Composable
private fun ActionButton(label: String, accent: Color, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(56.dp)
            .pressable(RoundedCornerShape(28.dp), onClick)
            .background(if (filled) accent else accent.copy(alpha = 0.18f))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, color = if (filled) onAccent(accent) else TextPrimary, fontWeight = FontWeight.Medium)
    }
}

/** A permission or system screen the user may need to open, with its status. */
@Composable
private fun AccessRow(icon: ImageVector, title: String, granted: Boolean?, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).clickable { onSelect(i) }.padding(vertical = 2.dp),
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

private val RAINBOW = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color(AccentColors.hsvToArgb(it % 360f, 1f, 1f)) }

private fun accentLabel(key: String): Int = when (key) {
    "blue" -> R.string.accent_blue
    "ice" -> R.string.accent_ice
    "cyan" -> R.string.accent_cyan
    "teal" -> R.string.accent_teal
    "mint" -> R.string.accent_mint
    "green" -> R.string.accent_green
    "lime" -> R.string.accent_lime
    "amber" -> R.string.accent_amber
    "orange" -> R.string.accent_orange
    "coral" -> R.string.accent_coral
    "red" -> R.string.accent_red
    "magenta" -> R.string.accent_magenta
    "violet" -> R.string.accent_violet
    "indigo" -> R.string.accent_indigo
    "gold" -> R.string.accent_gold
    "pearl" -> R.string.accent_pearl
    else -> R.string.accent_custom
}

private fun themeLabel(key: String): Int = when (key) {
    "midnight" -> R.string.theme_midnight
    "ice" -> R.string.theme_ice
    "sunset" -> R.string.theme_sunset
    "carbon" -> R.string.theme_carbon
    "emerald" -> R.string.theme_emerald
    else -> R.string.theme_royal
}

/**
 * A slider over a colour gradient: tap or drag anywhere on it. [value] is 0–1.
 * Never mirrored, so the colours stay where the finger is in Arabic too.
 */
@Composable
private fun GradientSlider(title: String, value: Float, colors: List<Color>, onChange: (Float) -> Unit) {
    val current by rememberUpdatedState(onChange)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 15.sp, color = TextSecondary)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp)) {
                val thumb = 36.dp
                val thumbPx = with(LocalDensity.current) { thumb.toPx() }
                val usable = (constraints.maxWidth - thumbPx).coerceAtLeast(1f)
                fun at(x: Float) = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(usable) { detectTapGestures { o -> current(at(o.x)) } }
                        .pointerInput(usable) {
                            detectHorizontalDragGestures { change, _ -> change.consume(); current(at(change.position.x)) }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(colors))
                    )
                    Box(
                        Modifier
                            .offset { IntOffset((value.coerceIn(0f, 1f) * usable).roundToInt(), 0) }
                            .size(thumb).clip(CircleShape).background(Color.White)
                            .border(3.dp, Color(0xFF0B1220), CircleShape)
                    )
                }
            }
        }
    }
}

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
        Toggle(stringResource(R.string.second_page), s.secondPage, a, stringResource(R.string.second_page_hint)) { v ->
            act.update { it.copy(secondPage = v) }
        }
        Toggle(stringResource(R.string.tile_labels), s.showTileLabels, a) { v -> act.update { it.copy(showTileLabels = v) } }
    }
    Section(stringResource(R.string.tile_icons)) {
        Chips(
            stringResource(R.string.icon_size),
            listOf(stringResource(R.string.icon_small), stringResource(R.string.icon_medium), stringResource(R.string.icon_large)),
            IconSize.entries.indexOf(s.iconSize), a
        ) { i -> act.update { it.copy(iconSize = IconSize.entries[i]) } }
        Chips(
            stringResource(R.string.icon_shape),
            listOf(stringResource(R.string.shape_original), stringResource(R.string.shape_circle), stringResource(R.string.shape_rounded)),
            IconShape.entries.indexOf(s.iconShape), a
        ) { i -> act.update { it.copy(iconShape = IconShape.entries[i]) } }
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
                ) { if (on) Icon(Icons.Rounded.Check, null, tint = onAccent(a), modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(14.dp))
                Text(stringResource(cardLabel(c)), fontSize = 17.sp, color = if (on) TextPrimary else TextSecondary, modifier = Modifier.weight(1f))
                if (on) {
                    GlassIconButton(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up), {
                        if (idx > 0) act.update { cur -> cur.copy(cards = cur.cards.toMutableList().apply { add(idx - 1, removeAt(idx)) }) }
                    }, size = 56.dp)
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down), {
                        if (idx < s.cards.size - 1) act.update { cur -> cur.copy(cards = cur.cards.toMutableList().apply { add(idx + 1, removeAt(idx)) }) }
                    }, size = 56.dp)
                }
            }
        }
    }
    Section(stringResource(R.string.top_bar)) {
        Chips(
            stringResource(R.string.top_bar_shows),
            listOf(
                stringResource(R.string.top_weather), stringResource(R.string.top_prayer), stringResource(R.string.top_date),
                stringResource(R.string.top_music), stringResource(R.string.top_trip), stringResource(R.string.top_none)
            ),
            TopBar.entries.indexOf(s.topBar), a
        ) { i -> act.update { it.copy(topBar = TopBar.entries[i]) } }
        if (s.topBar == TopBar.WEATHER) Hint(stringResource(R.string.top_weather_hint))
        if (s.topBar == TopBar.MUSIC && !st.hasMusicAccess) {
            AccessRow(Icons.Rounded.MusicNote, stringResource(R.string.enable_music_hint), false, a, act.musicAccess)
        }
        // The next prayer can also sit beside whatever the bar shows
        if (s.topBar != TopBar.PRAYER) {
            Toggle(stringResource(R.string.show_prayer_chip), s.showPrayerChip, a) { v -> act.update { it.copy(showPrayerChip = v) } }
        }
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
    Section(stringResource(R.string.cameras)) {
        Toggle(stringResource(R.string.cameras_on), s.speedCameras, a, stringResource(R.string.cameras_hint)) { v ->
            act.update { it.copy(speedCameras = v) }
        }
        if (s.speedCameras) {
            Toggle(stringResource(R.string.cameras_sound), s.cameraSound, a) { v -> act.update { it.copy(cameraSound = v) } }
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
    ReminderSection(st, act)
    TripHistorySection(st, act)
}

@Composable
private fun ReminderSection(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    val ctx = LocalContext.current
    val locale = appLocale()
    val unit = stringResource(if (s.useKmh) R.string.km else R.string.mi)
    fun shownKm(km: Double) = String.format(Locale.US, "%,d", (if (s.useKmh) km else km / 1.609344).roundToInt())
    Section(stringResource(R.string.reminders)) {
        Toggle(stringResource(R.string.reminders_on), s.carReminders, a, stringResource(R.string.reminders_hint)) { v ->
            act.update { it.copy(carReminders = v) }
        }
        val dateFmt = remember(locale) { java.text.SimpleDateFormat("d MMM yyyy", locale) }
        st.reminders.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(reminderIcon(r.type), null, tint = a, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(reminderName(r), fontSize = 17.sp, color = TextPrimary)
                    val parts = listOfNotNull(
                        r.dueDate?.let { dateFmt.format(java.util.Date(it)) },
                        r.dueKm?.let { "${shownKm(it)} $unit" }
                    )
                    Text(parts.joinToString("  ·  "), fontSize = 14.sp, color = TextSecondary)
                }
                // Renewed: moves it to the next year (or the next tyre / service interval)
                ActionButton(stringResource(R.string.reminder_renewed), a) { act.renewReminder(r.id) }
                Spacer(Modifier.width(10.dp))
                GlassIconButton(Icons.Rounded.Close, stringResource(R.string.reminder_delete), { act.removeReminder(r.id) }, size = 56.dp)
            }
        }

        // Adding one: pick what it is, then a date (or how many km from now)
        var adding by remember { mutableStateOf<ReminderType?>(null) }
        var customName by remember { mutableStateOf("") }
        var kmAhead by remember { mutableStateOf(10_000) }
        val type = adding
        if (type == null) {
            ActionButton(stringResource(R.string.reminder_add), a) { adding = ReminderType.INSURANCE }
        } else {
            Chips(
                stringResource(R.string.reminder_what),
                ReminderType.entries.map { stringResource(reminderNameRes(it)) },
                ReminderType.entries.indexOf(type), a
            ) { i -> adding = ReminderType.entries[i] }
            if (type == ReminderType.CUSTOM) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it.take(30) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.reminder_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (type.byKm) {
                Stepper(
                    stringResource(R.string.reminder_in_km), "${String.format(Locale.US, "%,d", kmAhead)} $unit",
                    onMinus = { kmAhead = (kmAhead - 1_000).coerceAtLeast(1_000) },
                    onPlus = { kmAhead = (kmAhead + 1_000).coerceAtMost(100_000) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (type.byKm) {
                    ActionButton(stringResource(R.string.reminder_save), a, filled = true) {
                        val ahead = if (s.useKmh) kmAhead.toDouble() else kmAhead * 1.609344
                        act.addReminder(CarReminder("r" + System.currentTimeMillis(), type, dueKm = st.odometerKm + ahead))
                        adding = null
                    }
                } else {
                    ActionButton(stringResource(R.string.reminder_pick_date), a, filled = true) {
                        val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, 1) }
                        val name = customName.trim()
                        android.app.DatePickerDialog(ctx, { _, y, m, d ->
                            act.addReminder(CarReminder("r" + System.currentTimeMillis(), type, name = name, dueDate = Reminders.dayStart(y, m, d)))
                            adding = null
                            customName = ""
                        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
                    }
                }
                ActionButton(stringResource(R.string.cancel), a) { adding = null }
            }
        }
    }
}

@Composable
private fun TripHistorySection(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    val unit = stringResource(if (s.useKmh) R.string.km else R.string.mi)
    fun dist(m: Double) = String.format(Locale.US, "%.1f", if (s.useKmh) m / 1000.0 else m / 1609.344)
    fun time(ms: Long) = (ms / 60_000L).let { String.format(Locale.US, "%d:%02d", it / 60, it % 60) }
    Section(stringResource(R.string.trip_history)) {
        Toggle(stringResource(R.string.trip_history_keep), st.settings.tripHistory, a) { on ->
            act.update { it.copy(tripHistory = on) }
        }
        val trips = st.tripHistory
        if (trips.isEmpty()) {
            Hint(stringResource(R.string.trip_history_empty))
            return@Section
        }
        val now = remember(trips) { System.currentTimeMillis() }
        val week = remember(trips) { TripHistory.lastDays(trips, now, 7) }
        val month = remember(trips) { TripHistory.thisMonth(trips, now) }
        @Composable
        fun TotalsRow(label: String, t: TripTotals) {
            val fuel = if (s.fuelCost) "  ·  " + stringResource(
                R.string.fuel_cost_value,
                String.format(Locale.US, "%.2f", TripHistory.fuelCost(t.distanceM, s.fuelConsumption, s.fuelPrice))
            ) else ""
            Text(label, fontSize = 15.sp, color = TextSecondary)
            Text(
                "${dist(t.distanceM)} $unit  ·  ${time(t.movingMs)}  ·  " +
                    pluralStringResource(R.plurals.trips_count, t.trips, t.trips) + fuel,
                fontSize = 18.sp, color = TextPrimary, fontWeight = FontWeight.Medium
            )
        }
        TotalsRow(stringResource(R.string.trip_last_7_days), week)
        TotalsRow(stringResource(R.string.trip_this_month), month)
        Spacer(Modifier.height(4.dp))
        val locale = appLocale()
        val dayFmt = remember(locale) { java.text.SimpleDateFormat("EEE d MMM", locale) }
        trips.take(15).forEach { t ->
            Text(
                dayFmt.format(java.util.Date(t.startedAt)) + "  " + formatTime(t.startedAt, s.use24h, locale) +
                    "  ·  ${dist(t.distanceM)} $unit  ·  ${time(t.movingMs)}",
                fontSize = 15.sp, color = TextPrimary
            )
        }
        if (trips.size > 15) Hint(stringResource(R.string.trip_history_more, trips.size))
        ActionButton(stringResource(R.string.trip_history_clear), a, onClick = act.clearTripHistory)
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
            // Typed text lives here (with the keyboard's composing state), so an Arabic
            // keyboard can't lose letters; the setting is saved as you type
            var name by remember { mutableStateOf(TextFieldValue(s.welcomeName)) }
            OutlinedTextField(
                value = name,
                onValueChange = { v ->
                    name = if (v.text.length > 30) v.copy(text = v.text.take(30)) else v
                    if (name.text != s.welcomeName) act.update { it.copy(welcomeName = name.text) }
                },
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
                    GlassIconButton(Icons.Rounded.Close, stringResource(R.string.remove), { act.update { it.copy(autoApp = "") } }, size = 56.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisplayPage(st: SettingsState, act: SettingsActions) {
    val s = st.settings
    val a = st.accent
    Section(stringResource(R.string.theme_presets)) {
        Hint(stringResource(R.string.theme_presets_hint))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentColors.THEMES.forEach { th ->
                val on = s.accent == th.accent && s.background == th.background && s.cardFill == th.cardFill && s.corners == th.corners
                val c = Color(th.accent)
                Row(
                    modifier = Modifier
                        .height(64.dp)
                        .pressable(RoundedCornerShape(20.dp)) {
                            act.update { it.copy(accent = th.accent, background = th.background, cardFill = th.cardFill, corners = th.corners, songColours = false) }
                        }
                        .background(Color(0xFF0E1525))
                        .border(if (on) 3.dp else 1.dp, if (on) c else Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(c))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(themeLabel(th.key)), fontSize = 16.sp, color = Color(0xFFF3F5FA), fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
    Section(stringResource(R.string.accent_color)) {
        // Sliders keep their own position: a colour stored as 8-bit loses a little of it
        var custom by remember { mutableStateOf(!AccentColors.isPreset(s.accent)) }
        var hue by remember { mutableFloatStateOf(AccentColors.sliders(s.accent).first) }
        var vivid by remember { mutableFloatStateOf(AccentColors.sliders(s.accent).second) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentColors.PRESETS.forEach { preset ->
                val color = Color(preset.argb)
                val selected = preset.argb == s.accent
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .pressable(CircleShape) { custom = false; act.update { it.copy(accent = preset.argb) } }
                        .background(color)
                        .border(if (selected) 3.dp else 0.dp, TextPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) { if (selected) Icon(Icons.Rounded.Check, stringResource(accentLabel(preset.key)), tint = onAccent(color)) }
            }
            // Any other colour: opens the two sliders
            val isCustom = !AccentColors.isPreset(s.accent)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .pressable(CircleShape) {
                        custom = true
                        val (h, v) = AccentColors.sliders(s.accent)
                        hue = h; vivid = v
                    }
                    .background(Brush.sweepGradient(RAINBOW))
                    .border(if (isCustom) 3.dp else 0.dp, TextPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Palette, stringResource(R.string.accent_custom), tint = Color.White) }
        }
        val name = AccentColors.PRESETS.firstOrNull { it.argb == s.accent }?.let { stringResource(accentLabel(it.key)) }
            ?: stringResource(R.string.accent_custom)
        Text(name, fontSize = 15.sp, color = TextSecondary)
        if (custom) {
            GradientSlider(stringResource(R.string.accent_hue), hue / 360f, RAINBOW) { f ->
                hue = (f * 360f).coerceIn(0f, 359.9f)
                act.update { it.copy(accent = AccentColors.custom(hue, vivid)) }
            }
            GradientSlider(
                stringResource(R.string.accent_vivid), vivid,
                listOf(Color(AccentColors.custom(hue, 0f)), Color(AccentColors.custom(hue, 1f)))
            ) { f ->
                vivid = f
                act.update { it.copy(accent = AccentColors.custom(hue, vivid)) }
            }
        }
        Toggle(stringResource(R.string.song_colours), s.songColours, a, stringResource(R.string.song_colours_hint)) { v -> act.update { it.copy(songColours = v) } }
    }
    Section(stringResource(R.string.card_style)) {
        Chips(
            stringResource(R.string.card_fill),
            listOf(stringResource(R.string.fill_clear), stringResource(R.string.fill_glass), stringResource(R.string.fill_frosted), stringResource(R.string.fill_solid)),
            CardFill.entries.indexOf(s.cardFill), a
        ) { i -> act.update { it.copy(cardFill = CardFill.entries[i]) } }
        Hint(stringResource(R.string.card_fill_hint))
        Chips(
            stringResource(R.string.corners),
            listOf(stringResource(R.string.corners_square), stringResource(R.string.corners_rounded), stringResource(R.string.corners_extra)),
            Corners.entries.indexOf(s.corners), a
        ) { i -> act.update { it.copy(corners = Corners.entries[i]) } }
        Toggle(stringResource(R.string.high_contrast), s.highContrast, a, stringResource(R.string.high_contrast_hint)) { v ->
            act.update { it.copy(highContrast = v) }
        }
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
                    GlassIconButton(Icons.Rounded.Close, stringResource(R.string.remove), { act.removePlace(p.id) }, size = 56.dp)
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
                        modifier = Modifier.height(56.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.06f))
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
    Section(stringResource(R.string.updates)) {
        Toggle(stringResource(R.string.update_auto), st.settings.updateCheck, a, stringResource(R.string.update_auto_hint)) { on ->
            act.update { it.copy(updateCheck = on) }
        }
        val u = st.update
        val status = when (u.stage) {
            UpdateStage.IDLE -> null
            UpdateStage.CHECKING -> stringResource(R.string.update_checking)
            UpdateStage.LATEST -> stringResource(R.string.update_latest)
            UpdateStage.AVAILABLE -> stringResource(R.string.update_available, u.version.orEmpty())
            UpdateStage.DOWNLOADING -> stringResource(R.string.update_downloading, u.version.orEmpty(), u.progress)
            UpdateStage.INSTALLING -> stringResource(R.string.update_installing)
            UpdateStage.NEED_PERMISSION -> stringResource(R.string.update_allow)
            UpdateStage.FAILED -> stringResource(R.string.update_failed)
        }
        if (status != null) {
            Text(
                status, fontSize = 16.sp,
                color = if (u.stage == UpdateStage.AVAILABLE) a else TextPrimary,
                fontWeight = if (u.stage == UpdateStage.AVAILABLE) FontWeight.Medium else FontWeight.Normal
            )
        }
        // What the new version brings, before you install it
        if (u.stage == UpdateStage.AVAILABLE || u.stage == UpdateStage.NEED_PERMISSION) {
            val lang = appLocale().language
            val items = remember(u.notes, lang) { WhatsNew.parse(u.notes, lang) }
            items.forEach { Text("•  $it", fontSize = 15.sp, color = TextSecondary) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val busy = u.stage == UpdateStage.CHECKING || u.stage == UpdateStage.DOWNLOADING || u.stage == UpdateStage.INSTALLING
            if (u.version != null && (u.stage == UpdateStage.AVAILABLE || u.stage == UpdateStage.NEED_PERMISSION ||
                    (u.stage == UpdateStage.FAILED))
            ) {
                ActionButton(stringResource(R.string.update_now), a, filled = true, onClick = act.installUpdate)
            }
            if (!busy) ActionButton(stringResource(R.string.update_check), a, onClick = act.checkUpdate)
            ActionButton(stringResource(R.string.whats_new_show), a, onClick = act.showWhatsNew)
        }
    }
    Section(stringResource(R.string.backup)) {
        Hint(stringResource(R.string.backup_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(stringResource(R.string.backup_save), a, filled = true, onClick = act.saveBackup)
            ActionButton(stringResource(R.string.backup_load), a, onClick = act.loadBackup)
        }
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
