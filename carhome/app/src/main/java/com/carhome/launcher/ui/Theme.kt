package com.carhome.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgTop = Color(0xFF070A12)
val BgBottom = Color(0xFF111A2E)
val GlassFill = Color.White.copy(alpha = 0.06f)
val GlassStroke = Color.White.copy(alpha = 0.09f)
val TextPrimary = Color(0xFFF3F5FA)
val TextSecondary = Color(0xFFA7B0C2)

val CardShape = RoundedCornerShape(28.dp)

@Composable
fun CarHomeTheme(accent: Color, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
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

/** Deep-navy gradient with a soft accent glow in the top corner. */
@Composable
fun GradientBackground(accent: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 1400f
                )
            ),
        content = content
    )
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
