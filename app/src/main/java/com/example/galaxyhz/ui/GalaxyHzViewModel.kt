package com.example.galaxyhz.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.galaxyhz.data.AdaptiveMode
import com.example.galaxyhz.data.AppPrefs
import com.example.galaxyhz.data.DeviceProfile
import com.example.galaxyhz.data.DeviceProfiles
import com.example.galaxyhz.data.DisplayStatus
import com.example.galaxyhz.data.LockState
import com.example.galaxyhz.data.PanelMode
import com.example.galaxyhz.data.RefreshRateManager
import com.example.galaxyhz.util.RootHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Destination screens shown in the navigation drawer. */
enum class Dest {
    HOME, RESOLUTION, CUSTOM_RATE, ADAPTIVE, OVERCLOCK, TOOLS, SETTINGS
}

class GalaxyHzViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPrefs(app)

    private val _status = MutableStateFlow(DisplayStatus())
    val status: StateFlow<DisplayStatus> = _status.asStateFlow()

    private val _log = MutableStateFlow("Ready.")
    val log: StateFlow<String> = _log.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _setupDone = MutableStateFlow(prefs.setupDone)
    val setupDone: StateFlow<Boolean> = _setupDone.asStateFlow()

    private val _lockResult = MutableStateFlow<LockState?>(null)
    val lockResult: StateFlow<LockState?> = _lockResult.asStateFlow()

    private val _selectedDest = MutableStateFlow(Dest.HOME)
    val selectedDest: StateFlow<Dest> = _selectedDest.asStateFlow()

    var language: String
        get() = prefs.language
        set(value) {
            prefs.language = value
        }

    fun selectDest(dest: Dest) {
        _selectedDest.value = dest
    }

    fun finishSetup() {
        prefs.setupDone = true
        _setupDone.value = true
    }

    init {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(2500)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { _status.value = RefreshRateManager.getStatus(getApplication()) }
    }

    private fun profile(): DeviceProfile = DeviceProfiles.detect()

    fun openMagiskThread() {
        getApplication<Application>().startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/topjohnwu/Magisk"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ------------------------------------------------------------- actions

    fun applyMode(mode: PanelMode) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Applying ${mode.label} (${mode.scanout})..."
            val res = RefreshRateManager.applyMode(
                profile(), mode.width, mode.height, mode.hz, mode.panelIndex
            )
            _log.value = if (res.isSuccess) "Mode ${mode.label} applied!"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(600)
            refresh()
            _busy.value = false
        }
    }

    fun applyCustomRate(hz: Int) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Applying custom rate ${hz} Hz..."
            val p = profile()
            val res = RefreshRateManager.applyMode(p, 1080, 2400, hz, null)
            _log.value = if (res.isSuccess) "Custom ${hz} Hz sent (panel only accepts its own modes)"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(600)
            refresh()
            _busy.value = false
        }
    }

    fun applyResolution(width: Int, height: Int) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Switching resolution to ${width}x$height..."
            val st = _status.value
            val res = RefreshRateManager.applyResolution(
                profile(), width, height, st.activeModeHz, null
            )
            _log.value = if (res.isSuccess) "Resolution set to ${width}x$height!"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(600)
            refresh()
            _busy.value = false
        }
    }

    fun applyAdaptive(mode: AdaptiveMode, min: Int, max: Int) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Applying ${mode.label}..."
            val res = RefreshRateManager.applyAdaptiveMode(profile(), mode, min, max)
            _log.value = if (res.isSuccess) "${mode.label} applied!"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(400)
            refresh()
            _busy.value = false
        }
    }

    fun lockRefreshRate() {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Locking refresh rate (apply + verify)..."
            val st = RefreshRateManager.lockRefreshRate(profile())
            _lockResult.value = st
            _log.value = if (st.verified) "Refresh rate locked and verified!"
            else "Lock incomplete: idle=${st.idleTimerZero} detection=${st.contentDetectionOff} min=peak:${st.minEqualsPeak}"
            delay(400)
            refresh()
            _busy.value = false
        }
    }

    fun toggleAod(enable: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = if (enable) "Enabling AOD..." else "Disabling AOD (flicker-safe)..."
            RefreshRateManager.toggleAlwaysOnDisplay(enable)
            delay(300)
            refresh()
            _busy.value = false
        }
    }

    fun resetPanel() {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Power-cycling the display panel..."
            RefreshRateManager.resetDisplayPanel(profile())
            delay(300)
            refresh()
            _busy.value = false
        }
    }

    fun tryExperimentalMode(mode: PanelMode) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Attempting experimental ${mode.label}..."
            val res = RefreshRateManager.applyExperimentalRate(profile(), mode)
            _log.value = if (res.isSuccess && res.stderr.isBlank()) "Experimental ${mode.label} active!"
            else if (res.isSuccess) res.stderr
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(400)
            refresh()
            _busy.value = false
        }
    }
}
