package com.example.galaxyhz.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.galaxyhz.data.PanelMode
import com.example.galaxyhz.data.RefreshRateManager
import com.example.galaxyhz.data.DeviceProfiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile that cycles 120 -> 96 -> 60 Hz (whatever standard modes
 * the panel exposes), using the same engine as the app.
 */
class GalaxyHzTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch { updateTileState() }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        serviceScope.launch { updateTileState() }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val context = applicationContext
            val status = RefreshRateManager.getStatus(context)
            val standard = status.availableModes
                .filter { !it.experimental && it.width == 1080 }
                .sortedByDescending { it.hz }
                .ifEmpty { listOf(PanelMode(1080, 2400, 120, "HS", "1"), PanelMode(1080, 2400, 96, "HS", "2"), PanelMode(1080, 2400, 60, "NS", "3")) }

            val currentIdx = standard.indexOfFirst { it.hz == status.activeModeHz }
            val next = standard[(currentIdx + 1).mod(standard.size)]

            updateTileState(next.hz)
            RefreshRateManager.applyMode(
                DeviceProfiles.detect(), next.width, next.height, next.hz, next.panelIndex
            )
            updateTileState(next.hz)
        }
    }

    private fun updateTileState(overrideHz: Int? = null) {
        val tile = qsTile ?: return
        serviceScope.launch {
            val hz = overrideHz ?: RefreshRateManager.getStatus(applicationContext).activeModeHz
            tile.state = Tile.STATE_ACTIVE
            tile.label = "$hz Hz"
            tile.subtitle = when (hz) {
                120 -> "Ultra Smooth"
                96 -> "Eco Smooth"
                else -> "Battery Saver"
            }
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
