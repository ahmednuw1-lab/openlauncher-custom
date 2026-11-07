package com.carhome.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carhome.launcher.R

/** Shown once after CarHome updated itself, and again from Settings → Updates. */
@Composable
fun WhatsNewOverlay(items: List<String>?, version: String, accent: Color, onClose: () -> Unit) {
    AnimatedVisibility(visible = !items.isNullOrEmpty(), enter = fadeIn(), exit = fadeOut()) {
        val list = items.orEmpty()
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                // A tap outside the card closes it too
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xF2151C2E))
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}   // taps on the card stay here
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.NewReleases, null, tint = accent, modifier = Modifier.size(32.dp))
                    Text(
                        stringResource(R.string.whats_new_title, version),
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                    )
                }
                Column(
                    Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    list.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.padding(top = 9.dp).size(8.dp).clip(CircleShape).background(accent))
                            Text(item, fontSize = 18.sp, color = TextPrimary, lineHeight = 25.sp)
                        }
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.End)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(accent)
                        .clickable(onClick = onClose)
                        .padding(horizontal = 32.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.ok), fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
