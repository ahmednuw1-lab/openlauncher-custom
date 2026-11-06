package com.carhome.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.carhome.launcher.LauncherSettings
import com.carhome.launcher.R
import com.carhome.launcher.TILE_COUNTS

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    accent: Color,
    hasMusicAccess: Boolean,
    hasLocation: Boolean,
    onBack: () -> Unit,
    onUpdate: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onSetDefault: () -> Unit,
    onMusicAccess: () -> Unit,
    onLocationAccess: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
            Text(stringResource(R.string.settings), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Look ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Section(stringResource(R.string.tiles)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TILE_COUNTS.forEach { n ->
                            val selected = settings.tileCount == n
                            Box(
                                modifier = Modifier
                                    .size(width = 76.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) accent else GlassFill)
                                    .border(1.dp, if (selected) Color.Transparent else GlassStroke, RoundedCornerShape(16.dp))
                                    .clickable { onUpdate { it.copy(tileCount = n) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$n", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else TextPrimary)
                            }
                        }
                    }
                }
                Section(stringResource(R.string.accent)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ACCENTS.forEach { argb ->
                            val c = Color(argb)
                            val selected = settings.accent == argb
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(3.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                                    .clickable { onUpdate { it.copy(accent = argb) } },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                ToggleRow(stringResource(R.string.clock_24h), settings.use24h, accent) { v -> onUpdate { it.copy(use24h = v) } }
                ToggleRow(stringResource(R.string.show_weather), settings.showWeather, accent) { v -> onUpdate { it.copy(showWeather = v) } }
            }

            // ── Permissions ─────────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionRow(Icons.Rounded.Home, stringResource(R.string.set_default), null, accent, onSetDefault)
                ActionRow(Icons.Rounded.MusicNote, stringResource(R.string.music_access), hasMusicAccess, accent, onMusicAccess)
                ActionRow(Icons.Rounded.LocationOn, stringResource(R.string.location_access), hasLocation, accent, onLocationAccess)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().glass().padding(20.dp)) {
        Text(title, fontSize = 15.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().glass().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 17.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White)
        )
    }
}

/** [granted] null = plain action with no status line. */
@Composable
private fun ActionRow(icon: ImageVector, label: String, granted: Boolean?, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().glass().clickable(onClick = onClick).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 17.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            if (granted != null) {
                Text(
                    stringResource(if (granted) R.string.granted else R.string.not_granted),
                    fontSize = 14.sp,
                    color = if (granted) Color(0xFF22C55E) else Color(0xFFF59E0B)
                )
            }
        }
        if (granted != null) {
            Icon(
                if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                null,
                tint = if (granted) Color(0xFF22C55E) else Color(0xFFF59E0B)
            )
        }
    }
}
