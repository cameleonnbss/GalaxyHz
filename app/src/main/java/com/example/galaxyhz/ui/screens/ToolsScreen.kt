package com.example.galaxyhz.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxyhz.theme.ErrorRed
import com.example.galaxyhz.theme.ExpressiveShapes
import com.example.galaxyhz.theme.NeonEmerald
import com.example.galaxyhz.theme.WarnOrange
import com.example.galaxyhz.ui.GalaxyHzViewModel
import com.example.galaxyhz.ui.components.IconStat
import com.example.galaxyhz.ui.components.InfoNote
import com.example.galaxyhz.ui.components.SectionHeader
import com.example.galaxyhz.ui.components.StatusHero
import com.example.galaxyhz.ui.components.SwitchRow
import com.example.galaxyhz.ui.components.TitledCard

@Composable
fun ToolsScreen(viewModel: GalaxyHzViewModel) {
    val status by viewModel.status.collectAsState()
    val lock by viewModel.lockResult.collectAsState()
    val busy by viewModel.busy.collectAsState()

    StatusHero(
        fps = if (status.currentFps > 0) status.currentFps.toInt() else 0,
        suffix = "Hz",
        line1 = "AOD: ${if (status.isAodEnabled) "ON (flicker risk)" else "off (recommended)"}",
        line2 = "Lock: ${if (status.refreshRateLocked) "active" else "not verified"}",
        accent = NeonEmerald,
        badge = "FLICKER PROTECTION"
    )

    Spacer(Modifier.height(16.dp))
    SectionHeader("LOCK REFRESH RATE")
    Spacer(Modifier.height(8.dp))
    TitledCard("APPLY + VERIFY") {
        InfoNote(
            "Disables SurfaceFlinger idle/content timers, then READS BACK the props " +
                "and settings to confirm every layer really took effect."
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { viewModel.lockRefreshRate() },
            enabled = !busy && status.hasRoot,
            shape = ExpressiveShapes.pill,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lock refresh rate", fontWeight = FontWeight.Bold)
        }
        lock?.let { st ->
            Spacer(Modifier.height(10.dp))
            IconStat(st.idleTimerZero, "Idle timer disabled", "Idle timer still active")
            IconStat(st.contentDetectionOff, "Content detection off", "Content detection on")
            IconStat(st.minEqualsPeak, "min = peak (fixed)", "min != peak (floating)")
            if (!st.verified) {
                Spacer(Modifier.height(6.dp))
                InfoNote(
                    "Not fully applied. Install the Magisk module so resetprop works, " +
                        "then retry.", color = WarnOrange
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("DEVELOPER OVERLAY")
    Spacer(Modifier.height(8.dp))
    TitledCard("SHOW REFRESH RATE") {
        SwitchRow(
            title = if (status.showRefreshRateOverlay) "FPS overlay is ON" else "FPS overlay is off",
            subtitle = "Same as Developer options > 'Show refresh rate'. Draws the live " +
                "rate in a corner overlay - handy to verify modes actually stick.",
            checked = status.showRefreshRateOverlay,
            onCheckedChange = { viewModel.setShowHzOverlay(it) }
        )
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("ALWAYS-ON DISPLAY")
    Spacer(Modifier.height(8.dp))
    TitledCard("AOD") {
        SwitchRow(
            title = "AOD is ${if (status.isAodEnabled) "ON" else "OFF"}",
            subtitle = "AOD runs the panel in its 60Hz/AID clock region - main flicker source on S20 OLED",
            checked = status.isAodEnabled,
            onCheckedChange = { viewModel.toggleAod(it) }
        )
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("EMERGENCY")
    Spacer(Modifier.height(8.dp))
    TitledCard("PANEL RECOVERY") {
        InfoNote(
            "Power-cycles the display DDIC (screen blinks once). Clears static, " +
                "sparkle and mid-frame glitch bands instantly."
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { viewModel.resetPanel() },
            enabled = !busy && status.hasRoot,
            shape = ExpressiveShapes.pill,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset display panel (DDI)", color = ErrorRed, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(Modifier.height(16.dp))
    TitledCard("MAGISK MODULE") {
        InfoNote(
            "Install the companion module so locks and props survive every reboot. " +
                "Export the installed module's zip to your Download folder to inspect " +
                "or re-flash it:"
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { viewModel.exportModuleZip() },
            enabled = !busy && status.hasRoot,
            shape = ExpressiveShapes.pill,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export module zip to Download", fontWeight = FontWeight.Bold)
        }
    }
}
