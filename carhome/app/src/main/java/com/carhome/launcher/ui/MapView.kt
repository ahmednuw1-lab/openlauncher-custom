package com.carhome.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.CarHomeApp
import com.carhome.launcher.MapColours
import com.carhome.launcher.MapLook
import com.carhome.launcher.NavInfo
import com.carhome.launcher.R
import com.carhome.launcher.TileMath
import com.carhome.launcher.speedInUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

// The map (1.4): OpenStreetMap tiles with your position, saved places and speed cameras.
// Used by the dashboard home and by the custom home's map widget.

private val DarkMapBg = Color(0xFF0D1424)
private val LightMapBg = Color(0xFFE9E5DE)
private val CameraOrange = Color(0xFFFF7A45)

/** Whether the map is drawn dark right now. */
internal fun mapIsDark(look: MapLook, night: Boolean): Boolean = when (look) {
    MapLook.DARK -> true
    MapLook.LIGHT -> false
    MapLook.NIGHT -> night
}

/**
 * The map. [compact] (the widget) keeps only the "back to my position" button.
 * Drag to look around (the map stops following you until you tap the button); pinch or
 * use + and − to zoom. With automatic zoom it goes further out the faster you drive.
 */
@Composable
fun CarMap(st: HomeState, act: HomeActions, compact: Boolean, modifier: Modifier = Modifier) {
    val s = st.settings
    val ctx = LocalContext.current
    val tiles = remember { CarHomeApp.core(ctx).tiles }
    val tileVersion by tiles.version.collectAsState()
    val dark = mapIsDark(s.mapLook, st.night)
    val filter = remember(dark) { if (dark) ColorFilter.colorMatrix(ColorMatrix(MapColours.darkMatrix())) else null }
    val kmh = st.speedMps?.let { speedInUnit(it, true) }

    var zoom by remember { mutableIntStateOf(TileMath.zoomForSpeed(kmh)) }
    var follow by remember { mutableStateOf(true) }
    var manualZoom by remember { mutableStateOf(false) }
    // Where the map looks while not following: a position in tile units at [zoom]
    var lookX by remember { mutableStateOf(Double.NaN) }
    var lookY by remember { mutableStateOf(Double.NaN) }
    var pinch by remember { mutableStateOf(1f) }

    // Automatic zoom follows the speed while the map follows the car and nobody zoomed by hand
    LaunchedEffect(kmh?.let { (it / 5f).roundToInt() }, follow, manualZoom, s.mapAutoZoom) {
        if (s.mapAutoZoom && follow && !manualZoom) zoom = TileMath.autoZoom(kmh, zoom)
    }

    val meX = st.lon?.let { TileMath.x(it, zoom) }
    val meY = st.lat?.let { TileMath.y(it, zoom) }
    val cx = if (follow || lookX.isNaN()) meX else lookX
    val cy = if (follow || lookY.isNaN()) meY else lookY
    val curZoom by rememberUpdatedState(zoom)
    val curCx by rememberUpdatedState(cx)
    val curCy by rememberUpdatedState(cy)

    fun setZoom(z: Int) {
        val nz = TileMath.clampZoom(z)
        if (nz == zoom) return
        if (!lookX.isNaN()) { lookX = TileMath.rescale(lookX, zoom, nz); lookY = TileMath.rescale(lookY, zoom, nz) }
        zoom = nz
        manualZoom = true
    }

    BoxWithConstraints(
        modifier
            .clip(CardShape)
            .background(if (dark) DarkMapBg else LightMapBg)
            .border(1.dp, GlassStroke, CardShape)
    ) {
        val density = LocalDensity.current
        val tilePx = with(density) { TILE_DP.toPx() }
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        if (cx == null || cy == null) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.NearMe, null, tint = st.accent, modifier = Modifier.size(32.dp))
                Text(stringResource(R.string.map_waiting), color = TextSecondary, fontSize = 15.sp)
                if (!st.hasLocation) SmallPill(Icons.Rounded.NearMe, stringResource(R.string.tap_to_allow), st.accent, act.requestLocation)
            }
            return@BoxWithConstraints
        }

        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    val x0 = curCx ?: return@detectTransformGestures
                    val y0 = curCy ?: return@detectTransformGestures
                    if (pan != Offset.Zero) {
                        follow = false
                        lookX = x0 - pan.x / tilePx
                        lookY = (y0 - pan.y / tilePx).coerceIn(0.0, TileMath.tilesAcross(curZoom).toDouble())
                    }
                    if (zoomChange != 1f) {
                        pinch *= zoomChange
                        if (pinch > 1.6f) { pinch = 1f; setZoom(curZoom + 1) }
                        else if (pinch < 0.62f) { pinch = 1f; setZoom(curZoom - 1) }
                    }
                }
            }
        ) {
            if (tileVersion < 0) return@Canvas   // reading it draws the map again when a tile arrives
            val side = ceil(tilePx).toInt() + 1
            for ((tx, ty) in TileMath.visible(cx, cy, size.width / tilePx.toDouble(), size.height / tilePx.toDouble(), zoom)) {
                val left = ((tx - cx) * tilePx + size.width / 2).toFloat()
                val top = ((ty - cy) * tilePx + size.height / 2).toFloat()
                val img = tiles.get(zoom, tx, ty)
                if (img != null) {
                    drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(left.roundToInt(), top.roundToInt()), IntSize(side, side), colorFilter = filter)
                } else {
                    // While a tile loads, the one a level out (already on the box) stands in, enlarged
                    val px = floor(TileMath.wrapX(tx, zoom) / 2.0).toInt()
                    val parent = if (zoom > TileMath.MIN_ZOOM) tiles.peek(zoom - 1, px, ty / 2) else null
                    if (parent != null) {
                        val half = parent.width / 2
                        val sx = if (TileMath.wrapX(tx, zoom) % 2 == 0) 0 else half
                        val sy = if (ty % 2 == 0) 0 else half
                        drawImage(parent, IntOffset(sx, sy), IntSize(half, half), IntOffset(left.roundToInt(), top.roundToInt()), IntSize(side, side), colorFilter = filter)
                    }
                }
            }
            // Speed cameras
            if (s.mapCameras) for (c in st.cameras) {
                val p = Offset(((TileMath.x(c.lon, zoom) - cx) * tilePx + size.width / 2).toFloat(), ((TileMath.y(c.lat, zoom) - cy) * tilePx + size.height / 2).toFloat())
                if (p.x < -20 || p.y < -20 || p.x > size.width + 20 || p.y > size.height + 20) continue
                val r = 11.dp.toPx()
                drawRoundRect(CameraOrange, Offset(p.x - r, p.y - r), Size(2 * r, 2 * r), androidx.compose.ui.geometry.CornerRadius(r * 0.55f))
                drawRoundRect(Color(0xFF0B1020), Offset(p.x - r * 0.5f, p.y - r * 0.32f), Size(r, r * 0.72f), androidx.compose.ui.geometry.CornerRadius(r * 0.15f))
                drawCircle(CameraOrange, r * 0.2f, Offset(p.x, p.y + 0.04f * r))
            }
            // You
            if (meX != null && meY != null) {
                val me = Offset(((meX - cx) * tilePx + size.width / 2).toFloat(), ((meY - cy) * tilePx + size.height / 2).toFloat())
                val heading = st.heading
                if (heading != null) rotate(heading, me) {
                    val cone = Path().apply {
                        moveTo(me.x, me.y)
                        lineTo(me.x - 40.dp.toPx(), me.y - 70.dp.toPx())
                        quadraticBezierTo(me.x, me.y - 86.dp.toPx(), me.x + 40.dp.toPx(), me.y - 70.dp.toPx())
                        close()
                    }
                    drawPath(cone, st.accent.copy(alpha = 0.16f))
                }
                drawCircle(st.accent.copy(alpha = 0.2f), 26.dp.toPx(), me)
                drawCircle(st.accent, 14.dp.toPx(), me)
                drawCircle(Color.White, 14.dp.toPx(), me, style = Stroke(3.5f.dp.toPx()))
                rotate(heading ?: 0f, me) {
                    val a = Path().apply {
                        moveTo(me.x, me.y - 8.dp.toPx())
                        lineTo(me.x + 6.dp.toPx(), me.y + 6.dp.toPx())
                        lineTo(me.x, me.y + 3.dp.toPx())
                        lineTo(me.x - 6.dp.toPx(), me.y + 6.dp.toPx())
                        close()
                    }
                    if (heading != null) drawPath(a, Color.White) else drawCircle(Color.White, 4.dp.toPx(), me)
                }
            }
        }

        // Saved places: tap one to start navigating there
        if (s.mapPlaces) st.places.forEach { p ->
            val plat = p.lat ?: return@forEach
            val plon = p.lon ?: return@forEach
            val x = ((TileMath.x(plon, zoom) - cx) * tilePx + wPx / 2).toFloat()
            val y = ((TileMath.y(plat, zoom) - cy) * tilePx + hPx / 2).toFloat()
            if (x < -30 || y < -30 || x > wPx + 30 || y > hPx + 30) return@forEach
            val pin = with(density) { 40.dp.toPx() }
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .offset { IntOffset((x - pin / 2).roundToInt(), (y - pin / 2).roundToInt()) }
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(st.accent)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { act.openPlace(p) },
                contentAlignment = Alignment.Center
            ) { Icon(placeIcon(p), p.name, tint = Color.White, modifier = Modifier.size(20.dp)) }
        }

        // Buttons: zoom and "back to my position" on the left (near the driver), credit on the right
        Column(
            Modifier.align(AbsoluteAlignment.BottomLeft).padding(if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!compact) {
                MapButton(Icons.Rounded.Add, stringResource(R.string.map_zoom_in), dark) { setZoom(zoom + 1) }
                MapButton(Icons.Rounded.Remove, stringResource(R.string.map_zoom_out), dark) { setZoom(zoom - 1) }
            }
            if (!compact || !follow) MapButton(Icons.Rounded.Navigation, stringResource(R.string.map_recenter), dark, tint = if (follow) null else st.accent) {
                follow = true
                manualZoom = false
                lookX = Double.NaN; lookY = Double.NaN
            }
        }
        Text(
            stringResource(R.string.map_credit),
            fontSize = 10.sp,
            color = if (dark) Color(0xFFC9D0DC) else Color(0xFF333333),
            modifier = Modifier
                .align(AbsoluteAlignment.BottomRight)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (dark) Color(0x990B1020) else Color(0xCCFFFFFF))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private val TILE_DP: Dp = 256.dp

@Composable
private fun MapButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, dark: Boolean, tint: Color? = null, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .pressable(RoundedCornerShape(16.dp), onClick)
            .background(if (dark) Color(0xD90B1020) else Color(0xE6FFFFFF))
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, label, tint = tint ?: if (dark) TextPrimary else Color(0xFF1C2438), modifier = Modifier.size(22.dp)) }
}

/** The pill on top of the dashboard's map: opens Google Maps to look for a place. */
@Composable
fun WhereToPill(accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier
            .fillMaxWidth()
            .pressable(shape, onClick)
            .background(Color(0xD90B1020))
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Search, null, tint = accent, modifier = Modifier.size(22.dp))
        Text(stringResource(R.string.map_where_to), fontSize = 17.sp, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}

/**
 * The next turn while Google Maps navigates: its arrow, the distance and the instruction,
 * read from Google Maps' own notification. With [nav] null it says there's no route.
 */
@Composable
fun NextTurnCard(nav: NavInfo?, accent: Color, onOpen: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false, solid: Boolean = false) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .clip(shape)
            .background(if (solid) Color(0xE60B1020) else GlassFill)
            .border(1.dp, GlassStroke, shape)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val box = if (compact) 48.dp else 64.dp
        Box(
            Modifier.size(box).clip(RoundedCornerShape(16.dp)).background(if (nav != null) accent else Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            val icon = nav?.icon
            if (icon != null) Image(icon.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.size(box * 0.72f))
            else Icon(Icons.Rounded.Navigation, null, tint = if (nav != null) onAccent(accent) else TextSecondary, modifier = Modifier.size(box * 0.5f))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (nav == null) {
                Text(stringResource(R.string.nav_none), fontSize = if (compact) 16.sp else 18.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(stringResource(R.string.nav_none_hint), fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                if (nav.title.isNotBlank()) Text(nav.title, fontSize = if (compact) 22.sp else 30.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (nav.text.isNotBlank()) Text(nav.text, fontSize = if (compact) 14.sp else 16.sp, color = TextPrimary, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
                if (nav.sub.isNotBlank()) Text(nav.sub, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!compact && nav != null) Icon(Icons.Rounded.OpenInNew, stringResource(R.string.nav_open_maps), tint = TextSecondary, modifier = Modifier.size(22.dp))
    }
}
