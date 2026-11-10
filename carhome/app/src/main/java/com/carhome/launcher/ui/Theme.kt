package com.carhome.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.AccentColors
import com.carhome.launcher.Background
import com.carhome.launcher.CardFill
import com.carhome.launcher.Corners
import com.carhome.launcher.IconShape
import com.carhome.launcher.IconSize
import com.carhome.launcher.LauncherSettings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import com.carhome.launcher.WeatherKind
import com.carhome.launcher.weatherKind
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.random.Random

val BgTop = Color(0xFF070A12)
val BgBottom = Color(0xFF111A2E)
val AlertRed = Color(0xFFEF4444)
val OkGreen = Color(0xFF22C55E)
val WarnAmber = Color(0xFFF59E0B)

/**
 * The look the user chose in Settings → Display: how solid the cards are, how
 * round the corners, and the high-contrast mode. Held as Compose state, so every
 * card, tile and text follows a change at once without being passed around.
 */
object Look {
    var cardFill by mutableStateOf(CardFill.GLASS)
    var corners by mutableStateOf(Corners.ROUNDED)
    var highContrast by mutableStateOf(false)
    var iconSize by mutableStateOf(IconSize.MEDIUM)
    var iconShape by mutableStateOf(IconShape.ORIGINAL)

    fun apply(s: LauncherSettings) {
        cardFill = s.cardFill; corners = s.corners; highContrast = s.highContrast
        iconSize = s.iconSize; iconShape = s.iconShape
    }

    /** A corner radius that follows the corner style (28 dp is the standard card). */
    fun radius(base: Dp): Dp = base * (corners.dp / 28f)
}

val GlassFill: Color
    get() = when (Look.cardFill) {
        CardFill.CLEAR -> Color.White.copy(alpha = if (Look.highContrast) 0.05f else 0.025f)
        CardFill.GLASS -> if (Look.highContrast) Color(0xB30A0F1C) else Color.White.copy(alpha = 0.06f)
        CardFill.FROSTED -> Color(0xFF0E1525).copy(alpha = if (Look.highContrast) 0.82f else 0.62f)
        CardFill.SOLID -> Color(0xFF111827).copy(alpha = 0.96f)
    }
val GlassStroke: Color
    get() = Color.White.copy(alpha = if (Look.highContrast) 0.34f else if (Look.cardFill == CardFill.CLEAR) 0.14f else 0.09f)
val TextPrimary: Color get() = if (Look.highContrast) Color.White else Color(0xFFF3F5FA)
val TextSecondary: Color get() = if (Look.highContrast) Color(0xFFDDE3EE) else Color(0xFFA7B0C2)

val CardShape: Shape get() = RoundedCornerShape(Look.radius(28.dp))

/** Text and icons drawn on top of the accent colour: dark on light accents, white on the rest. */
fun onAccent(accent: Color): Color =
    if (AccentColors.darkTextOn(accent.toArgb().toLong() and 0xFFFFFFFFL)) Color(AccentColors.DARK_TEXT) else Color.White

@Composable
fun CarHomeTheme(accent: Color, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = onAccent(accent),
            background = BgTop,
            surface = Color(0xFF141B2B),
            onSurface = TextPrimary,
            onBackground = TextPrimary,
            surfaceVariant = Color(0xFF1C2438),
            onSurfaceVariant = TextSecondary
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 16.sp, color = TextPrimary),
            titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        ),
        content = content
    )
}

/** The screen background: classic gradient, living sky, or the user's photo. */
@Composable
fun AppBackground(
    style: Background,
    accent: Color,
    photo: ImageBitmap?,
    sun: Pair<Long, Long>?,
    weatherCode: Int?,
    dust: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(BgTop)) {
        when {
            style == Background.SKY -> LivingSky(sun, weatherCode, dust)
            style == Background.PHOTO && photo != null -> {
                Image(photo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            }
            else -> Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = 1400f
                        )
                    )
            )
        }
        content()
    }
}

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float)
private class Drop(val x: Float, val y: Float, val speed: Float)
private class Cloud(val x: Float, val y: Float, val scale: Float, val speed: Float)

private fun smooth(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

/**
 * A quiet animated sky that follows the real time of day and the weather:
 * stars at night, warm light at sunrise and sunset, drifting clouds, rain.
 * Redraws ~20 times a second without recomposing.
 */
@Composable
fun LivingSky(sun: Pair<Long, Long>?, weatherCode: Int?, dust: Boolean) {
    var t by remember { mutableFloatStateOf(0f) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        val start = System.nanoTime()
        var lastMinute = 0L
        var lastFrame = 0L
        while (true) {
            // Frame clock: pauses by itself while CarHome isn't on screen; ~20 fps is plenty
            withFrameNanos { frame ->
                if (frame - lastFrame >= 50_000_000L) {
                    lastFrame = frame
                    t = (System.nanoTime() - start) / 1e9f
                    val ms = System.currentTimeMillis()
                    if (ms - lastMinute > 60_000L) { now = ms; lastMinute = ms }
                }
            }
            // Sleep between frames instead of waking on every screen refresh
            delay(45L)
        }
    }
    val rng = remember { Random(7) }
    val stars = remember { List(90) { Star(rng.nextFloat(), rng.nextFloat() * 0.75f, 0.6f + rng.nextFloat() * 1.4f, rng.nextFloat() * 6.28f) } }
    val drops = remember { List(80) { Drop(rng.nextFloat(), rng.nextFloat(), 0.7f + rng.nextFloat() * 0.6f) } }
    val clouds = remember { List(6) { Cloud(rng.nextFloat(), 0.05f + rng.nextFloat() * 0.35f, 0.7f + rng.nextFloat() * 0.8f, 6f + rng.nextFloat() * 10f) } }

    // Daylight 0..1 and sunrise/sunset glow 0..1, from minutes to the sun events
    val cal = java.util.Calendar.getInstance()
    val (rise, set) = sun ?: run {
        val c = cal.apply { timeInMillis = now; set(java.util.Calendar.HOUR_OF_DAY, 6); set(java.util.Calendar.MINUTE, 0) }
        c.timeInMillis to c.timeInMillis + 12 * 3_600_000L
    }
    val mRise = (now - rise) / 60_000f
    val mSet = (now - set) / 60_000f
    val day = smooth(-30f, 45f, mRise) * (1f - smooth(-45f, 30f, mSet))
    val glow = maxOf(exp(-(mRise / 50f) * (mRise / 50f)), exp(-(mSet / 50f) * (mSet / 50f)))

    val top = lerp(lerp(Color(0xFF04060E), Color(0xFF0E3A66), day), Color(0xFF3A2A55), glow * 0.6f)
    val bottom = lerp(lerp(Color(0xFF0E1630), Color(0xFF3E7CB1), day), Color(0xFFD9774B), glow * 0.75f)
    val kind = weatherCode?.let { weatherKind(it) }
    val cloudCount = when (kind) {
        WeatherKind.PARTLY -> 3
        WeatherKind.CLOUDY, WeatherKind.FOG, WeatherKind.DRIZZLE, WeatherKind.RAIN, WeatherKind.STORM, WeatherKind.SNOW -> 5
        else -> 0
    }
    val raining = kind == WeatherKind.RAIN || kind == WeatherKind.DRIZZLE || kind == WeatherKind.STORM
    val starAlpha = (1f - day) * (1f - glow) * if (cloudCount >= 5) 0.3f else 1f

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val time = t
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        if (starAlpha > 0.02f) {
            stars.forEach { s ->
                val tw = 0.55f + 0.45f * kotlin.math.sin(time * 1.3f + s.phase)
                drawCircle(Color.White.copy(alpha = starAlpha * tw * 0.8f), s.r, Offset(s.x * w, s.y * h))
            }
        }
        val cloudAlpha = if (cloudCount >= 5) 0.10f else 0.07f
        for (i in 0 until cloudCount) {
            val c = clouds[i]
            val span = w + 800f
            val cx = ((c.x * span + time * c.speed) % span) - 400f
            val cy = c.y * h
            val r = 150f * c.scale
            // Soft-edged puffs: a radial fade instead of hard circles
            listOf(Offset(cx, cy) to r, Offset(cx - r * 0.8f, cy + r * 0.2f) to r * 0.75f, Offset(cx + r * 0.85f, cy + r * 0.15f) to r * 0.8f)
                .forEach { (o, rr) ->
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = cloudAlpha), Color.Transparent), center = o, radius = rr),
                        radius = rr, center = o
                    )
                }
        }
        if (raining) {
            val streak = Color(0xFFB8C7E0).copy(alpha = 0.22f)
            drops.forEach { d ->
                val x = (d.x * w + time * 40f) % w
                val y = (d.y * h + time * 650f * d.speed) % h
                drawLine(streak, Offset(x, y), Offset(x - 5f, y + 20f), strokeWidth = 1.6f)
            }
            // No lightning flash: a screen flash is a distraction while driving (worse at night)
        }
        if (dust) drawRect(Color(0xFFC2A27A).copy(alpha = 0.14f))
        else if (kind == WeatherKind.FOG) drawRect(Color(0xFF9AA4B5).copy(alpha = 0.10f))
        // Keep cards readable on a bright day sky
        drawRect(Color.Black.copy(alpha = 0.22f + 0.18f * day))
    }
}

/** Frosted-glass style card: translucent fill, hairline border, big rounded corners. */
fun Modifier.glass(shape: Shape = CardShape): Modifier =
    this.clip(shape).background(GlassFill).border(1.dp, GlassStroke, shape)

/** Shrinks slightly while pressed — a small touch that makes taps feel responsive. */
@Composable
fun rememberPressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "press")
    return scale
}

/**
 * A tappable pill (chip, button): shrinks slightly while pressed, so a tap is felt
 * without looking twice. Put it before the clip and background of the pill.
 */
@Composable
fun Modifier.pressable(shape: Shape, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    return this.scale(scale).clip(shape).clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
}

/** Round glass icon button used in headers. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 56.dp,
    tint: Color = TextPrimary,
    filled: Color? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(filled ?: GlassFill)
            .border(1.dp, if (filled != null) Color.Transparent else GlassStroke, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}
