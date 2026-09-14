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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxyhz.theme.ExpressiveShapes
import com.example.galaxyhz.theme.NeonCyan
import com.example.galaxyhz.theme.NeonPink
import com.example.galaxyhz.ui.GalaxyHzViewModel
import com.example.galaxyhz.ui.components.InfoNote
import com.example.galaxyhz.ui.components.SectionHeader
import com.example.galaxyhz.ui.components.StatusHero
import com.example.galaxyhz.ui.components.TitledCard

@Composable
fun CustomRateScreen(viewModel: GalaxyHzViewModel) {
    val status by viewModel.status.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var slider by remember { mutableFloatStateOf(120f) }
    val hz = slider.toInt()

    StatusHero(
        fps = if (status.currentFps > 0) status.currentFps.toInt() else 0,
        suffix = "Hz",
        line1 = "Active: ${status.panelModeLabel}",
        line2 = "Min ${status.minRefreshRate} / Peak ${status.peakRefreshRate}",
        accent = NeonCyan,
        badge = "CUSTOM RATE"
    )

    Spacer(Modifier.height(16.dp))
    SectionHeader("SLIDE TO ANY REFRESH RATE")
    Spacer(Modifier.height(8.dp))

    TitledCard("${hz} Hz") {
        Slider(
            value = slider,
            onValueChange = { slider = it },
            valueRange = 24f..240f,
            steps = 0,
            enabled = !busy && status.hasRoot
        )
        Text(
            "Selected: ${hz} Hz",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )
        Spacer(Modifier.height(6.dp))
        InfoNote(
            "The framework applies this rate; the panel driver will snap to the " +
                "closest mode it actually supports (typically 60/96/120 on the S20). " +
                "Use the Experimental Modes tab to try the panel's hidden HS clocks."
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.applyCustomRate(hz) },
            enabled = !busy && status.hasRoot,
            shape = ExpressiveShapes.pill,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply ${hz} Hz", fontWeight = FontWeight.Bold)
        }
    }

    Spacer(Modifier.height(12.dp))
    TitledCard("HOW IT WORKS") {
        InfoNote(
            "Android advertises any rate in 0.1 Hz steps to apps, but the physical " +
                "panel only scans out at its own fixed clocks. Rates between two " +
                "supported clocks are delivered with frame pacing (some frames last " +
                "longer than others). This is useful for video (24/30/48 Hz) and " +
                "battery tuning."
        )
    }

    Spacer(Modifier.height(12.dp))
    Text(
        "Tip: 24 Hz for movies, 48 Hz for 24fps video, 90-110 Hz for battery-aware gaming.",
        fontSize = 12.sp,
        color = NeonPink,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
