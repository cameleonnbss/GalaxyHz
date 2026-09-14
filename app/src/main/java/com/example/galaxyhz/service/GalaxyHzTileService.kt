package com.example.galaxyhz.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.galaxyhz.data.HzMode
import com.example.galaxyhz.data.RefreshRateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile that cycles 120 -> 96 -> 60 Hz on tap.
 * Label shows the current/last-applied rate; the tile is unavailable without root.
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
            val current = RefreshRateManager.getStatus(applicationContext).targetMode
            val next = when (current) {
                HzMode.H120 -> HzMode.H96
                HzMode.H96 -> HzMode.H60
                HzMode.H60 -> HzMode.H120
            }
            updateTileState(next)
            RefreshRateManager.applyRefreshRate(next.hz)
            updateTileState(next)
        }
    }

    private fun updateTileState(override: HzMode? = null) {
        val tile = qsTile ?: return
        serviceScope.launch {
            val mode = override ?: RefreshRateManager.getStatus(applicationContext).targetMode
            tile.state = Tile.STATE_ACTIVE
            tile.label = "${mode.hz} Hz"
            tile.subtitle = when (mode) {
                HzMode.H120 -> "Ultra Smooth"
                HzMode.H96 -> "Eco Smooth"
                HzMode.H60 -> "Battery Saver"
            }
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
