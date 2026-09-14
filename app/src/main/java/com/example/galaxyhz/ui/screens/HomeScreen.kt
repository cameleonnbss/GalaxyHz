package com.example.galaxyhz.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxyhz.data.PanelMode
import com.example.galaxyhz.theme.ErrorRed
import com.example.galaxyhz.theme.ExpressiveShapes
import com.example.galaxyhz.theme.NeonAmber
import com.example.galaxyhz.theme.NeonCyan
import com.example.galaxyhz.theme.NeonEmerald
import com.example.galaxyhz.ui.Dest
import com.example.galaxyhz.ui.L10n
import com.example.galaxyhz.ui.GalaxyHzViewModel
import com.example.galaxyhz.ui.components.ModeCard
import com.example.galaxyhz.ui.components.SectionHeader
import com.example.galaxyhz.ui.components.StatusHero
import com.example.galaxyhz.ui.components.TitledCard

@Composable
fun HomeScreen(viewModel: GalaxyHzViewModel) {
    val status by viewModel.status.collectAsState()
    val log by viewModel.log.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val lang = viewModel.language

    val accent = when (status.activeModeHz) {
        120 -> NeonCyan
        96 -> NeonEmerald
        else -> NeonAmber
    }

    StatusHero(
        fps = if (status.currentFps > 0) status.currentFps.toInt() else 0,
        suffix = "Hz",
        line1 = "Panel: ${status.panelModeLabel}",
        line2 = "Resolution: ${status.resolution}  •  ${status.deviceTitle}",
        accent = accent,
        badge = L10n.t("active_rate", lang)
    )

    if (!status.hasRoot) {
        Spacer(Modifier.height(8.dp))
        TitledCard("ROOT") {
            Text(
                L10n.t("not_root", lang),
                color = ErrorRed,
                fontSize = 13.sp
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("REFRESH RATE")

    val fallback = listOf(
        PanelMode(1080, 2400, 120, "HS", "1"),
        PanelMode(1080, 2400, 96, "HS", "2"),
        PanelMode(1080, 2400, 60, "NS", "3")
    )
    val mainModes = status.availableModes.filter { !it.experimental }
        .sortedByDescending { it.hz }
        .ifEmpty { fallback }
        .take(3)

    mainModes.forEachIndexed { i, mode ->
        val cardAccent = when (i) {
            0 -> NeonCyan
            1 -> NeonEmerald
            else -> NeonAmber
        }
        Spacer(Modifier.height(8.dp))
        ModeCard(
            title = when (mode.hz) {
                120 -> "120 Hz - Ultra Smooth"
                96 -> "96 Hz - Eco Smooth (recommended)"
                else -> "${mode.hz} Hz - Battery Saver"
            },
            subtitle = when (mode.hz) {
                120 -> "Maximum fluidity for gaming and scrolling"
                96 -> "Feels like 120 Hz, less heat, +30% battery"
                else -> "Standard rate for maximum battery life"
            },
            details = mode.label,
            isSelected = status.activeModeHz == mode.hz && !mode.experimental,
            accent = cardAccent,
            enabled = !busy,
            onClick = { viewModel.applyMode(mode) }
        )
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("QUICK ACTIONS")
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { viewModel.lockRefreshRate() },
        enabled = !busy && status.hasRoot,
        shape = ExpressiveShapes.pill,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(L10n.t("lock_rate", lang), fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { viewModel.selectDest(Dest.TOOLS) },
        shape = ExpressiveShapes.pill,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(L10n.t("tools", lang))
    }

    Spacer(Modifier.height(16.dp))
    TitledCard("OUTPUT") {
        Text(
            log,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = if (log.contains("Error", true) || log.contains("incomplete", true)) ErrorRed
            else NeonEmerald
        )
    }
}
