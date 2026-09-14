package com.example.galaxyhz.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxyhz.theme.ExpressiveShapes
import com.example.galaxyhz.theme.NeonPink
import com.example.galaxyhz.theme.NeonViolet
import com.example.galaxyhz.theme.WarnOrange
import com.example.galaxyhz.ui.GalaxyHzViewModel
import com.example.galaxyhz.ui.components.InfoNote
import com.example.galaxyhz.ui.components.SectionHeader
import com.example.galaxyhz.ui.components.StatusHero
import com.example.galaxyhz.ui.components.TitledCard

@Composable
fun OverclockScreen(viewModel: GalaxyHzViewModel) {
    val status by viewModel.status.collectAsState()
    val busy by viewModel.busy.collectAsState()

    StatusHero(
        fps = if (status.currentFps > 0) status.currentFps.toInt() else 0,
        suffix = "Hz",
        line1 = "${status.availableModes.size} modes discovered on this panel",
        line2 = status.availableModes.joinToString("  ") { "${it.hz}${it.scanout}" },
        accent = NeonViolet,
        badge = "EXPERIMENTAL MODES"
    )

    Spacer(Modifier.height(16.dp))
    TitledCard("THE TRUTH ABOUT \"OVERCLOCKING\" PHONE PANELS") {
        InfoNote(
            "Phone DDICs (display controllers) ship with a FIXED mode table burned " +
                "in at the factory. Unlike PC monitors, there is no free-running pixel " +
                "clock: a mode that is not in the table physically cannot scan out. " +
                "What IS possible: panels often contain hidden intermediate HS clocks " +
                "(100/104/110/112 Hz on the S20 family) that Samsung never exposed in " +
                "settings. Below are the modes this device's own driver reported. " +
                "Modes marked experimental come from the compatible-mode list and may " +
                "be rejected by your specific unit."
        )
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("HIDDEN & HIGH-RATE MODES")
    Spacer(Modifier.height(8.dp))

    val candidates = status.availableModes
        .filter { it.hz >= 96 }
        .sortedByDescending { it.hz }

    if (candidates.isEmpty()) {
        InfoNote("No extra modes discovered. Root is required to read the panel table.")
    }

    candidates.forEach { mode ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                Modifier.padding(16.dp)
            ) {
                Text(
                    mode.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (mode.hz > 120) NeonPink else NeonViolet
                )
                Text(
                    mode.subLabel + "  •  driver index ${mode.panelIndex.ifBlank { "?" }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.tryExperimentalMode(mode) },
                    enabled = !busy && status.hasRoot,
                    shape = ExpressiveShapes.pill
                ) {
                    Text(if (mode.hz > 120) "Try ${mode.hz} Hz" else "Apply ${mode.hz} Hz")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.height(8.dp))
    Text(
        "If a mode fails, the panel simply ignores it - nothing breaks. " +
            "Black screen? Press the power button twice; the panel re-inits.",
        fontSize = 12.sp,
        color = WarnOrange
    )
}
