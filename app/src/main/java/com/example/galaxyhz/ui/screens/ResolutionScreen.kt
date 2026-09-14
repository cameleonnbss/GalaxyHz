package com.example.galaxyhz.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.galaxyhz.theme.NeonCyan
import com.example.galaxyhz.theme.NeonViolet
import com.example.galaxyhz.ui.GalaxyHzViewModel
import com.example.galaxyhz.ui.components.InfoNote
import com.example.galaxyhz.ui.components.ModeCard
import com.example.galaxyhz.ui.components.SectionHeader
import com.example.galaxyhz.ui.components.StatusHero

@Composable
fun ResolutionScreen(viewModel: GalaxyHzViewModel) {
    val status by viewModel.status.collectAsState()
    val busy by viewModel.busy.collectAsState()

    StatusHero(
        fps = if (status.currentFps > 0) status.currentFps.toInt() else 0,
        suffix = "Hz",
        line1 = "Active resolution: ${status.resolution}",
        line2 = "Panel: ${status.panelModeLabel}",
        accent = NeonViolet,
        badge = "RESOLUTION"
    )

    Spacer(Modifier.height(16.dp))
    SectionHeader("AVAILABLE RESOLUTIONS")
    Spacer(Modifier.height(8.dp))

    status.availableResolutions.forEach { res ->
        val isWqhd = res.height >= 3000
        ModeCard(
            title = res.label,
            subtitle = if (isWqhd) "Sharpest display, highest GPU load and battery drain"
            else "Best battery life, recommended with high refresh rates",
            details = "${res.width} x ${res.height}",
            isSelected = status.resolution == res.key,
            accent = if (isWqhd) NeonViolet else NeonCyan,
            enabled = !busy && status.hasRoot,
            onClick = { viewModel.applyResolution(res.width, res.height) }
        )
        Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.height(8.dp))
    InfoNote(
        "Changing resolution keeps the current refresh rate. The panel driver " +
            "may briefly blank the screen while switching scanout buffers.",
        color = MaterialTheme.colorScheme.outline
    )
}
