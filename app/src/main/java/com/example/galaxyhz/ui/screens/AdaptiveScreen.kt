package com.example.galaxyhz.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxyhz.data.AdaptiveMode
import com.example.galaxyhz.theme.ExpressiveShapes
import com.example.galaxyhz.theme.NeonAmber
import com.example.galaxyhz.theme.NeonCyan
import com.example.galaxyhz.theme.NeonEmerald
import com.example.galaxyhz.theme.WarnOrange
import com.example.galaxyhz.ui.GalaxyHzViewModel
import com.example.galaxyhz.ui.components.InfoNote
import com.example.galaxyhz.ui.components.ModeCard
import com.example.galaxyhz.ui.components.SectionHeader
import com.example.galaxyhz.ui.components.StatusHero

@Composable
fun AdaptiveScreen(viewModel: GalaxyHzViewModel) {
    val status by viewModel.status.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var selected by remember { mutableStateOf(AdaptiveMode.FIXED) }
    var minV by remember { mutableFloatStateOf(60f) }
    var maxV by remember { mutableFloatStateOf(120f) }

    StatusHero(
        fps = if (status.currentFps > 0) status.currentFps.toInt() else 0,
        suffix = "Hz",
        line1 = "Current: ${status.adaptiveMode.label}",
        line2 = "Min ${status.minRefreshRate} / Peak ${status.peakRefreshRate}",
        accent = NeonCyan,
        badge = "ADAPTIVE BEHAVIOR"
    )

    Spacer(Modifier.height(16.dp))
    SectionHeader("CHOOSE BEHAVIOR")
    Spacer(Modifier.height(8.dp))

    AdaptiveMode.entries.forEach { mode ->
        val accent = when (mode) {
            AdaptiveMode.FIXED -> NeonEmerald
            AdaptiveMode.ADAPTIVE -> NeonAmber
            AdaptiveMode.ADAPTIVE_RANGE -> NeonCyan
        }
        ModeCard(
            title = mode.label,
            subtitle = mode.description,
            details = if (mode == AdaptiveMode.ADAPTIVE_RANGE) "min ${minV.toInt()} - max ${maxV.toInt()} Hz"
            else if (mode == AdaptiveMode.FIXED) "recommended to avoid flicker" else "",
            isSelected = selected == mode,
            accent = accent,
            enabled = !busy,
            onClick = { selected = mode }
        )
        Spacer(Modifier.height(8.dp))
    }

    if (selected == AdaptiveMode.ADAPTIVE_RANGE) {
        Spacer(Modifier.height(12.dp))
        TitledCardRange(minV, maxV, onMin = { minV = it }, onMax = { maxV = it })
    }

    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            viewModel.applyAdaptive(selected, minV.toInt(), maxV.toInt())
        },
        enabled = !busy && status.hasRoot,
        shape = ExpressiveShapes.pill,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Apply \"${selected.label}\"", fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.height(12.dp))
    InfoNote(
        "On S20 OLED panels, adaptive transitions between HS and NS clocks are the " +
            "main flicker source. If you see static or bands, switch back to Fixed " +
            "and use the anti-flicker tools.",
        color = WarnOrange
    )
}

@Composable
private fun TitledCardRange(min: Float, max: Float, onMin: (Float) -> Unit, onMax: (Float) -> Unit) {
    androidx.compose.material3.Card(
        shape = com.example.galaxyhz.theme.ExpressiveShapes.card,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ADAPTIVE RANGE", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text("Minimum rate: ${min.toInt()} Hz", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Slider(value = min, onValueChange = onMin, valueRange = 24f..(max - 12f))
            Text("Maximum rate: ${max.toInt()} Hz", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Slider(value = max, onValueChange = onMax, valueRange = (min + 12f)..120f)
            InfoNote(
                "The system adapts inside this window. Example: 60-120 for smooth UI " +
                    "that saves power when idle, or 96-96 to emulate a fixed 96 Hz."
            )
        }
    }
}
