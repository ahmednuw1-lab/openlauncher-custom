package com.carhome.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.AppEntry
import com.carhome.launcher.Place
import com.carhome.launcher.R
import com.carhome.launcher.TILE_PLACE
import com.carhome.launcher.TILE_SMART

private enum class PickTab { APPS, PLACES, SMART }

/**
 * Every installed app. In picker mode it can also offer places and the smart
 * tile (when those features are on), and a tap returns the choice.
 */
@Composable
fun AppsScreen(
    apps: List<AppEntry>,
    accent: Color,
    pickerMode: Boolean,
    allowPlaces: Boolean,
    allowSmart: Boolean,
    places: List<Place>,
    parkedOnlyOn: Boolean,
    parkedOnly: Set<String>,
    driving: Boolean,
    onBack: () -> Unit,
    onLaunch: (String) -> Unit,
    onPick: (String) -> Unit,
    onAddToHome: (String) -> Unit,
    onToggleParked: (String) -> Unit,
    onAddPlace: (name: String, useCurrent: Boolean, address: String) -> Boolean,
    onRemovePlace: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(PickTab.APPS) }
    var addingPlace by remember { mutableStateOf(false) }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val showTabs = pickerMode && (allowPlaces || allowSmart)

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(if (pickerMode) R.string.choose_app else R.string.all_apps),
                fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
            )
            Spacer(Modifier.width(24.dp))
            if (showTabs) {
                TabChip(stringResource(R.string.tab_apps), tab == PickTab.APPS, accent) { tab = PickTab.APPS }
                if (allowPlaces) { Spacer(Modifier.width(8.dp)); TabChip(stringResource(R.string.tab_places), tab == PickTab.PLACES, accent) { tab = PickTab.PLACES } }
                if (allowSmart) { Spacer(Modifier.width(8.dp)); TabChip(stringResource(R.string.smart_tile), tab == PickTab.SMART, accent) { tab = PickTab.SMART } }
                Spacer(Modifier.width(16.dp))
            }
            // Typing while driving is unsafe, so driving mode hides search
            if (tab == PickTab.APPS && !driving) {
                Row(
                    modifier = Modifier.weight(1f).height(56.dp).glass(RoundedCornerShape(28.dp)).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text(stringResource(R.string.search_apps), color = TextSecondary, fontSize = 17.sp)
                        BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp),
                            cursorBrush = SolidColor(accent), modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(20.dp))

        val minCell = if (driving) 168.dp else 132.dp
        when (tab) {
            PickTab.APPS -> {
                if (apps.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.loading_apps), color = TextSecondary, fontSize = 18.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = minCell),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppCell(
                                app = app,
                                pickerMode = pickerMode,
                                parkedOnlyOn = parkedOnlyOn,
                                isParkedOnly = app.packageName in parkedOnly,
                                onClick = { if (pickerMode) onPick(app.packageName) else onLaunch(app.packageName) },
                                onAddToHome = { onAddToHome(app.packageName) },
                                onToggleParked = { onToggleParked(app.packageName) }
                            )
                        }
                    }
                }
            }
            PickTab.PLACES -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "add") {
                    IconCell(Icons.Rounded.AddCircle, stringResource(R.string.add_place), accent) { addingPlace = true }
                }
                items(places, key = { it.id }) { p ->
                    PlaceCell(p, accent, onPick = { onPick(TILE_PLACE + p.id) }, onRemove = { onRemovePlace(p.id) })
                }
            }
            PickTab.SMART -> Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(stringResource(R.string.smart_tile_explain), color = TextSecondary, fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
                Box(Modifier.size(180.dp)) {
                    IconCell(Icons.Rounded.AutoAwesome, stringResource(R.string.smart_tile), accent) { onPick(TILE_SMART) }
                }
            }
        }
    }

    if (addingPlace) {
        AddPlaceDialog(
            accent = accent,
            onDismiss = { addingPlace = false },
            onSave = { name, current, address ->
                if (onAddPlace(name, current, address)) addingPlace = false
            }
        )
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(56.dp)
            .pressable(RoundedCornerShape(28.dp), onClick)
            .background(if (selected) accent else Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) onAccent(accent) else TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(
    app: AppEntry,
    pickerMode: Boolean,
    parkedOnlyOn: Boolean,
    isParkedOnly: Boolean,
    onClick: () -> Unit,
    onAddToHome: () -> Unit,
    onToggleParked: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .scale(scale)
            .glass(RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = { if (!pickerMode) menuOpen = true }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
            Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(60.dp))
            Spacer(Modifier.height(10.dp))
            Text(app.label, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
        if (parkedOnlyOn && isParkedOnly) {
            Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Rounded.Lock, stringResource(R.string.parked_only), tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_to_home)) },
                leadingIcon = { Icon(Icons.Rounded.Add, null) },
                onClick = { menuOpen = false; onAddToHome() }
            )
            if (parkedOnlyOn) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.parked_only)) },
                    leadingIcon = { Icon(if (isParkedOnly) Icons.Rounded.Check else Icons.Rounded.Lock, null) },
                    onClick = { menuOpen = false; onToggleParked() }
                )
            }
        }
    }
}

@Composable
private fun IconCell(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp).glass(RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(label, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaceCell(p: Place, accent: Color, onPick: () -> Unit, onRemove: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .glass(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onPick, onLongClick = { menuOpen = true }),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Place, null, tint = accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(p.name, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (p.lat != null) stringResource(R.string.place_saved_location) else p.address,
                color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = { menuOpen = false; onRemove() }
            )
        }
    }
}

@Composable
fun AddPlaceDialog(accent: Color, onDismiss: () -> Unit, onSave: (String, Boolean, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var useCurrent by remember { mutableStateOf(true) }
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_place)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; error = null }, singleLine = true,
                    label = { Text(stringResource(R.string.place_name)) },
                    placeholder = { Text(stringResource(R.string.place_name_example)) }
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { useCurrent = true }) {
                    RadioButton(selected = useCurrent, onClick = { useCurrent = true })
                    Icon(Icons.Rounded.MyLocation, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.place_use_current))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { useCurrent = false }) {
                    RadioButton(selected = !useCurrent, onClick = { useCurrent = false })
                    Text(stringResource(R.string.place_type_address))
                }
                if (!useCurrent) {
                    OutlinedTextField(
                        value = address, onValueChange = { address = it; error = null }, singleLine = true,
                        label = { Text(stringResource(R.string.place_address)) }
                    )
                }
                error?.let { Text(stringResource(it), color = AlertRed, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> error = R.string.place_need_name
                    !useCurrent && address.isBlank() -> error = R.string.place_need_address
                    else -> {
                        error = if (useCurrent) R.string.place_no_location else null
                        onSave(name, useCurrent, address)
                    }
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
