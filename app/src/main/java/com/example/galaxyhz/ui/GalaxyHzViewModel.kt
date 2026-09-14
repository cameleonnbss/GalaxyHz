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
enum class Dest { HOME, RESOLUTION, CUSTOM_RATE, ADAPTIVE, OVERCLOCK, TOOLS, SETTINGS }

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

    // Live language: StateFlow so the UI re-composes instantly, no restart.
    private val _lang = MutableStateFlow(prefs.language)
    val lang: StateFlow<String> = _lang.asStateFlow()

    fun setLanguage(code: String) {
        prefs.language = code
        _lang.value = code
    }

    fun selectDest(dest: Dest) { _selectedDest.value = dest }

    /** Recommended density for a "WxH" string, keeping physical UI size. */
    fun autoDensityFor(resolution: String, panelLabel: String): Int {
        val profile = DeviceProfiles.detect()
        val h = resolution.substringAfter('x').toIntOrNull() ?: 2400
        return RefreshRateManager.autoDensity(profile, h)
    }

    /** Applies the auto-computed density for the current resolution. */
    fun applyAutoDensity(resolution: String, panelLabel: String) {
        applyDensity(autoDensityFor(resolution, panelLabel))
    }

    fun finishSetup() { prefs.setupDone = true; _setupDone.value = true }

    /**
     * Polls live status. Rate: 2.5 s while the UI is visible, slowing to 15 s
     * in background - a root query every 2.5 s forever starves Magisk's su
     * daemon and makes root flaky for the whole system.
     */
    private var pollingPaused = false

    fun pausePolling() { pollingPaused = true }
    fun resumePolling() { pollingPaused = false }

    init {
        viewModelScope.launch {
            while (true) {
                if (!pollingPaused) {
                    refresh()
                    delay(2_500)
                } else {
                    refresh()
                    delay(15_000)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { _status.value = RefreshRateManager.getStatus(getApplication()) }
    }

    private fun profile(): DeviceProfile = DeviceProfiles.detect()

    fun openMagisk() {
        getApplication<Application>().startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/topjohnwu/Magisk"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ------------------------------------------------------------- actions

    fun applyMode(mode: PanelMode, density: Int? = null) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Applying ${mode.label} (${mode.scanout})..."
            val res = RefreshRateManager.applyMode(
                profile(), mode.width, mode.height, mode.hz, mode.panelIndex, density
            )
            val actual = RefreshRateManager.actualRenderRate()
            _log.value = if (res.isSuccess)
                "Mode ${mode.label} applied - rendering at ${actual} Hz"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(500)
            refresh()
            _busy.value = false
        }
    }

    fun applyCustomRate(hz: Int) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Applying custom ${hz} Hz..."
            val res = RefreshRateManager.applyCustomRate(profile(), hz)
            delay(900) // let the framework settle before reading back
            val actual = RefreshRateManager.actualRenderRate()
            _log.value = if (res.isSuccess)
                "Requested ${hz} Hz - panel rendering at ${actual} Hz (nearest supported clock)"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            refresh()
            _busy.value = false
        }
    }

    fun applyResolution(width: Int, height: Int, density: Int? = null) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Switching resolution to ${width}x$height..."
            val st = _status.value
            val res = RefreshRateManager.applyResolution(
                profile(), width, height, st.activeModeHz, density
            )
            _log.value = if (res.isSuccess) "Resolution set to ${width}x$height!"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(600)
            refresh()
            _busy.value = false
        }
    }

    fun applyDensity(density: Int) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = "Setting density to ${density}dpi..."
            val res = RefreshRateManager.applyDensity(density)
            _log.value = if (res.isSuccess) "Density set to ${density}dpi!"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(400)
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

    fun setShowHzOverlay(enable: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            _log.value = if (enable) "Enabling 'Show refresh rate' overlay..." else "Disabling FPS overlay..."
            val res = RefreshRateManager.setShowRefreshRateOverlay(enable)
            _log.value = if (res.isSuccess)
                if (enable) "FPS overlay on - check Developer options / any screen"
                else "FPS overlay off"
            else "Error: ${res.stderr.ifBlank { res.stdout }}"
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

    /** Copies the module zip to Download so it is explorable in any file manager. */
    fun exportModuleZip() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            _busy.value = true
            _log.value = "Copying module zip to Download..."
            val res = RootHelper.runCommands(
                listOf(
                    "mkdir -p /sdcard/Download",
                    "cp /data/adb/modules/force_120hz_x1s/module.zip /sdcard/Download/force_120hz_x1s.zip 2>/dev/null || " +
                        "cp /data/adb/modules_update/force_120hz_x1s/module.zip /sdcard/Download/force_120hz_x1s.zip 2>/dev/null || " +
                        "echo 'module zip not found - install it from the GitHub release'"
                )
            )
            _log.value = if (res.stdout.contains("not found")) res.stdout.trim()
            else "Module zip copied to /sdcard/Download/force_120hz_x1s.zip"
            _busy.value = false
        }
    }
}
