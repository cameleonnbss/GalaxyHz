package com.example.galaxyhz.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxyhz.theme.ExpressiveShapes
import com.example.galaxyhz.ui.L10n
import com.example.galaxyhz.ui.GalaxyHzViewModel
import com.example.galaxyhz.ui.components.InfoNote
import com.example.galaxyhz.ui.components.SectionHeader
import com.example.galaxyhz.ui.components.StatusHero
import com.example.galaxyhz.ui.components.TitledCard

@Composable
fun SettingsScreen(viewModel: GalaxyHzViewModel) {
    val status by viewModel.status.collectAsState()
    val lang = viewModel.language
    val context = LocalContext.current

    StatusHero(
        fps = if (status.currentFps > 0) status.currentFps.toInt() else 0,
        suffix = "Hz",
        line1 = status.deviceModel,
        line2 = status.deviceTitle,
        accent = MaterialTheme.colorScheme.primary,
        badge = "SETTINGS"
    )

    Spacer(Modifier.height(16.dp))
    SectionHeader(L10n.t("language", lang).uppercase())
    Spacer(Modifier.height(8.dp))
    TitledCard("APP LANGUAGE") {
        InfoNote("More translations welcome on GitHub.")
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.layout.Row {
            L10n.languages.forEach { (code, name) ->
                FilterChip(
                    selected = lang == code,
                    onClick = { viewModel.language = code },
                    label = { Text(name) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("DEVICE")
    Spacer(Modifier.height(8.dp))
    TitledCard("DETECTED PANEL") {
        InfoNote("Model: ${status.deviceModel}")
        InfoNote("Family: ${status.deviceTitle}")
        InfoNote("Modes found: " + status.availableModes.joinToString { "${it.hz}${it.scanout}" })
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader(L10n.t("about", lang).uppercase())
    Spacer(Modifier.height(8.dp))
    TitledCard("GalaxyHz v2.0") {
        InfoNote(
            "Forces 120/96/60 Hz and any panel-supported rate on Samsung Galaxy " +
                "S20-series devices on AOSP ROMs. Material 3 Expressive UI."
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cameleonnbss/GalaxyHz"))
                )
            },
            shape = ExpressiveShapes.pill
        ) {
            Text(L10n.t("repo", lang), fontWeight = FontWeight.Bold)
        }
    }
}
