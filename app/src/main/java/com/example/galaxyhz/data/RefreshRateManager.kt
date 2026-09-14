package com.example.galaxyhz.data

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.example.galaxyhz.util.RootHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How the system picks the refresh rate. */
enum class AdaptiveMode(val label: String, val description: String) {
    FIXED("Fixed", "One rate, never changes - zero flicker, more battery drain"),
    ADAPTIVE("Adaptive (stock)", "System lowers FPS for static content - may flicker on S20 OLED"),
    ADAPTIVE_RANGE("Adaptive with custom range", "System adapts, but stays inside a min/max you choose");
}

/** One of the physical modes the panel driver exposes. */
data class PanelMode(
    val width: Int,
    val height: Int,
    val hz: Int,
    val scanout: String,
    val panelIndex: String,
    val experimental: Boolean = false
) {
    val label: String get() = "${width}x${height} @ $hz Hz"
    val subLabel: String
        get() = when {
            experimental -> "$scanout mode - experimental (set by panel driver)"
            else -> "$scanout mode"
        }
}

data class ResolutionOption(val width: Int, val height: Int, val label: String) {
    val key: String get() = "${width}x$height"
}

data class LockState(
    val applied: Boolean,
    val idleTimerZero: Boolean,
    val contentDetectionOff: Boolean,
    val minEqualsPeak: Boolean
) {
    val verified: Boolean get() = idleTimerZero && contentDetectionOff && minEqualsPeak
}

data class DisplayStatus(
    val currentFps: Float = 0f,
    val fpsSource: String = "",        // "display" or "surfaceflinger"
    val activeModeHz: Int = 60,
    val resolution: String = "?",
    val density: Int = 0,
    val panelModeLabel: String = "unknown",
    val minRefreshRate: String = "?",
    val peakRefreshRate: String = "?",
    val refreshRateLocked: Boolean = false,
    val adaptiveMode: AdaptiveMode = AdaptiveMode.FIXED,
    val isAodEnabled: Boolean = false,
    val showRefreshRateOverlay: Boolean = false,
    val hasRoot: Boolean = false,
    val deviceModel: String = "",
    val deviceTitle: String = "",
    val conflictApp: String = "",
    val availableModes: List<PanelMode> = emptyList(),
    val availableResolutions: List<ResolutionOption> = emptyList()
)

/**
 * Display engine for GalaxyHz. All panel mode indices are discovered at runtime
 * from the device's own mode table, so any Samsung `panel_drv` device works.
 */
object RefreshRateManager {

    private const val CONF_FILE = "/data/adb/force_hz.conf"

    // ------------------------------------------------------------ discovery

    /**
     * Parses `cat <panel>/display_mode` lines like
     * `pdm:1 1080x2400_120HS` (primary) and `cpdm:N ...` (compatible modes).
     */
    @JvmStatic
    fun parsePanelTable(raw: String): List<PanelMode> {
        val out = LinkedHashMap<String, PanelMode>()
        Regex("(pdm|cpdm):(\\d+)\\s+(\\d+)x(\\d+)_(\\d+)(HS|NS)").findAll(raw).forEach { m ->
            val isPrimary = m.groupValues[1] == "pdm"
            val mode = PanelMode(
                width = m.groupValues[3].toInt(),
                height = m.groupValues[4].toInt(),
                hz = m.groupValues[5].toInt(),
                scanout = m.groupValues[6],
                panelIndex = m.groupValues[2],
                experimental = !isPrimary
            )
            val key = "${mode.width}x${mode.height}_${mode.hz}${mode.scanout}"
            val prev = out[key]
            if (prev == null || (prev.experimental && !mode.experimental)) out[key] = mode
        }
        return out.values
            .sortedWith(compareByDescending<PanelMode> { it.hz }.thenByDescending { it.width })
    }

    suspend fun discoverPanelModes(profile: DeviceProfile): List<PanelMode> =
        withContext(Dispatchers.IO) {
            val raw = RootHelper.runCommand("cat ${profile.panelBase}/display_mode 2>/dev/null").stdout
            val parsed = parsePanelTable(raw)
            if (parsed.isNotEmpty()) parsed else profile.fallbackModes
        }

    fun resolutionsFrom(modes: List<PanelMode>, profile: DeviceProfile): List<ResolutionOption> {
        val seen = LinkedHashMap<String, ResolutionOption>()
        modes.sortedByDescending { it.width }.forEach { m ->
            val key = "${m.width}x${m.height}"
            if (!seen.containsKey(key)) {
                seen[key] = ResolutionOption(
                    m.width, m.height,
                    if (m.height >= 3000) "WQHD+ ${m.width} x ${m.height}"
                    else "FHD+ ${m.width} x ${m.height}"
                )
            }
        }
        profile.fallbackResolutions.forEach { r -> seen.putIfAbsent(r.key, r) }
        return seen.values.toList()
    }

    // --------------------------------------------------------------- apply

    /** Exact floats Android's DisplayManager matches against its mode table. */
    private fun rateFloat(hz: Int): String = when (hz) {
        120 -> "120.00001"
        96 -> "96.00001"
        60 -> "60.000004"
        else -> "$hz.000004"
    }

    fun applyModeCommands(
        profile: DeviceProfile,
        width: Int,
        height: Int,
        hz: Int,
        panelIndex: String?,
        density: Int? = null
    ): List<String> {
        val cmds = mutableListOf(
            "cmd power set-mode 0",
            "settings put global low_power 0",
            "settings put global low_power_sticky 0",
            "settings put global adaptive_battery_management_enabled 0",
            "settings put global automatically_reduce_refresh_rate 0",
            "wm size ${width}x$height"
        )
        // Density: explicit override wins; otherwise auto-scale for the resolution.
        cmds += "wm density ${density ?: autoDensity(profile, height)}"
        cmds += listOf(
            "settings put system peak_refresh_rate $hz.0",
            "settings put system min_refresh_rate $hz.0",
            "settings put system user_refresh_rate $hz",
            "settings put system display_refresh_rate $hz",
            "settings put system default_peak_refresh_rate $hz.0",
            "settings put system default_refresh_rate $hz.0",
            "settings put system motion_smoothness 2",
            "settings put secure refresh_rate_mode 2",
            "settings put secure screen_resolution_mode ${if (height >= 3000) "0" else "1"}",
            "settings put global match_content_frame_rate 2",
            "cmd display set-match-content-frame-rate-pref 2",
            "cmd display set-user-preferred-display-mode $width $height ${rateFloat(hz)} 0 false"
        )
        if (!panelIndex.isNullOrBlank()) {
            cmds += "echo $panelIndex > ${profile.panelBase}/display_mode 2>/dev/null"
        }
        cmds += "echo $hz ${width}x$height > $CONF_FILE"
        return cmds
    }

    /** Density that keeps the same physical UI size across resolutions. */
    fun autoDensity(profile: DeviceProfile, height: Int): Int =
        Math.round(profile.defaultDensity * height / 2400f / 10f) * 10

    suspend fun applyMode(
        profile: DeviceProfile,
        width: Int,
        height: Int,
        hz: Int,
        panelIndex: String?,
        density: Int? = null
    ): RootHelper.CommandResult = withContext(Dispatchers.IO) {
        val res = RootHelper.runCommandsRetried(
            applyModeCommands(profile, width, height, hz, panelIndex, density), 20_000
        )
        // Verify: let the framework settle, then compare the actual rate.
        kotlinx.coroutines.delay(2_000)
        val actual = actualRenderRate()
        if (res.isSuccess && actual in (hz - 2)..(hz + 2)) res
        else if (res.isSuccess) res.copy(
            isSuccess = false,
            stderr = "Applied, but the panel is rendering at $actual Hz instead of $hz. " +
                "Another tuner app may be fighting for control, or lock/unlock the " +
                "screen once to settle the panel."
        )
        else res
    }

    /**
     * Best-effort custom rate: a hybrid min/max window around the requested
     * value. The framework paces towards it while the panel snaps to its
     * nearest physical clock - and it actually holds (unlike a peak-only hint).
     */
    suspend fun applyCustomRate(profile: DeviceProfile, hz: Int): RootHelper.CommandResult =
        withContext(Dispatchers.IO) {
            val lo = maxOf(24, hz - 12)
            val hi = minOf(240, hz + 12)
            RootHelper.runCommands(
                listOf(
                    "cmd power set-mode 0",
                    "settings put global low_power 0",
                    "settings put global automatically_reduce_refresh_rate 0",
                    "settings put system peak_refresh_rate $hi.0",
                    "settings put system min_refresh_rate $lo.0",
                    "settings put system user_refresh_rate $hz",
                    "settings put system display_refresh_rate $hz",
                    "settings put system motion_smoothness 2",
                    "cmd display set-match-content-frame-rate-pref 1"
                )
            )
        }

    /** Reads back the rate SurfaceFlinger is actually rendering at. */
    suspend fun actualRenderRate(): Int = withContext(Dispatchers.IO) {
        val out = RootHelper.runCommand("dumpsys SurfaceFlinger").stdout
        Regex("renderRate=([0-9.]+)").find(out)?.groupValues?.get(1)
            ?.toFloatOrNull()?.toInt() ?: 0
    }

    /**
     * Experimental rate above the standard set: written straight to the panel
     * driver (its mode table defines safe HS clocks) plus settings as a hint.
     */
    suspend fun applyExperimentalRate(profile: DeviceProfile, mode: PanelMode): RootHelper.CommandResult =
        withContext(Dispatchers.IO) {
            val result = applyMode(profile, mode.width, mode.height, mode.hz, mode.panelIndex)
            val readBack = RootHelper.runCommand("cat ${profile.panelBase}/display_mode 2>/dev/null").stdout
            val tookIt = readBack.contains("panel_mode:${mode.panelIndex}")
            if (!tookIt && result.isSuccess) {
                result.copy(stderr = "Panel did not confirm mode ${mode.label} (may be rejected on this unit)")
            } else result
        }

    // -------------------------------------------------- adaptive / locking

    fun adaptiveCommands(profile: DeviceProfile, mode: AdaptiveMode, min: Int, max: Int): List<String> =
        when (mode) {
            AdaptiveMode.FIXED -> listOf(
                "resetprop -n ro.surface_flinger.use_content_detection_for_refresh_rate false",
                "resetprop -n ro.surface_flinger.set_idle_timer_ms 0",
                "resetprop -n ro.surface_flinger.set_touch_timer_ms 0",
                "resetprop -n ro.surface_flinger.set_display_power_timer_ms 0",
                "settings put global automatically_reduce_refresh_rate 0",
                "cmd display set-match-content-frame-rate-pref 0"
            )
            AdaptiveMode.ADAPTIVE -> listOf(
                "resetprop -n ro.surface_flinger.use_content_detection_for_refresh_rate true",
                "resetprop -n ro.surface_flinger.set_idle_timer_ms -1",
                "resetprop -n ro.surface_flinger.set_touch_timer_ms -1",
                "settings put system peak_refresh_rate $max.0",
                "settings put system min_refresh_rate ${if (max >= 120) 60 else max}.0",
                "settings put global automatically_reduce_refresh_rate 1",
                "cmd display set-match-content-frame-rate-pref 1"
            )
            AdaptiveMode.ADAPTIVE_RANGE -> listOf(
                "resetprop -n ro.surface_flinger.use_content_detection_for_refresh_rate true",
                "resetprop -n ro.surface_flinger.set_idle_timer_ms -1",
                "resetprop -n ro.surface_flinger.set_touch_timer_ms -1",
                "settings put system peak_refresh_rate $max.0",
                "settings put system min_refresh_rate $min.0",
                "cmd display set-match-content-frame-rate-pref 1"
            )
        } + "export PATH=/data/adb/magisk:\$PATH:/system/bin"

    suspend fun applyAdaptiveMode(
        profile: DeviceProfile,
        mode: AdaptiveMode,
        min: Int = 60,
        max: Int = 120
    ): RootHelper.CommandResult = withContext(Dispatchers.IO) {
        RootHelper.runCommands(adaptiveCommands(profile, mode, min, max))
    }

    /** Applies the anti-flicker props AND verifies them. */
    suspend fun lockRefreshRate(profile: DeviceProfile): LockState = withContext(Dispatchers.IO) {
        RootHelper.runCommands(
            listOf("export PATH=/data/adb/magisk:\$PATH:/system/bin") + adaptiveCommands(
                profile, AdaptiveMode.FIXED, 60, 120
            )
        )
        val idle = RootHelper.runCommand("getprop ro.surface_flinger.set_idle_timer_ms").stdout.trim()
        val content = RootHelper.runCommand(
            "getprop ro.surface_flinger.use_content_detection_for_refresh_rate"
        ).stdout.trim()
        val min = RootHelper.runCommand("settings get system min_refresh_rate").stdout.trim()
        val peak = RootHelper.runCommand("settings get system peak_refresh_rate").stdout.trim()
        LockState(
            applied = true,
            idleTimerZero = idle == "0",
            contentDetectionOff = content.equals("false", true) || content == "0",
            minEqualsPeak = min == peak && min.isNotBlank()
        )
    }

    // ---------------------------------------------------------- resolution

    suspend fun applyResolution(
        profile: DeviceProfile,
        width: Int,
        height: Int,
        currentHz: Int,
        density: Int? = null
    ): RootHelper.CommandResult = withContext(Dispatchers.IO) {
        RootHelper.runCommands(
            listOf(
                "wm size ${width}x$height",
                "wm density ${density ?: autoDensity(profile, height)}",
                "settings put secure screen_resolution_mode ${if (height >= 3000) "0" else "1"}",
                "cmd display set-user-preferred-display-mode $width $height ${rateFloat(currentHz)} 0 false",
                "echo $currentHz ${width}x$height > $CONF_FILE"
            ),
            timeoutMs = 15_000
        )
    }

    /** Manual density override (applies immediately, does not touch the rate). */
    suspend fun applyDensity(density: Int): RootHelper.CommandResult =
        withContext(Dispatchers.IO) {
            RootHelper.runCommands(listOf("wm density $density"))
        }

    // ------------------------------------------------- developer options

    suspend fun setShowRefreshRateOverlay(enable: Boolean): RootHelper.CommandResult =
        withContext(Dispatchers.IO) {
            // NOTE: `cmd surfaceflinger` does not exist on modern Android builds.
            // The settings key is the persistent switch; the SurfaceFlinger
            // transaction (1035) applies it immediately without a restart.
            val res = RootHelper.runCommands(
                listOf(
                    "settings put global show_refresh_rate_overlay ${if (enable) "1" else "0"}",
                    "service call SurfaceFlinger 1035 i32 ${if (enable) "1" else "0"}"
                )
            )
            val readBack = RootHelper.runCommand(
                "settings get global show_refresh_rate_overlay"
            ).stdout.trim()
            if (readBack == if (enable) "1" else "0") {
                if (enable) res.copy(
                    stdout = "Overlay enabled. If nothing appears, reboot once - " +
                        "some ROM builds draw it only after a restart."
                ) else res
            } else res.copy(
                isSuccess = false,
                stderr = "Overlay setting did not stick (read back: '$readBack')"
            )
        }

    // ---------------------------------------------------------- AOD / panel

    suspend fun toggleAlwaysOnDisplay(enable: Boolean): RootHelper.CommandResult =
        withContext(Dispatchers.IO) {
            RootHelper.runCommands(listOf("settings put secure doze_always_on ${if (enable) "1" else "0"}"))
        }

    suspend fun resetDisplayPanel(profile: DeviceProfile): RootHelper.CommandResult =
        withContext(Dispatchers.IO) {
            RootHelper.runCommands(
                listOf(
                    "echo 0 > ${profile.panelBase}/lcd_power 2>/dev/null",
                    "sleep 0.3",
                    "echo 1 > ${profile.panelBase}/lcd_power 2>/dev/null"
                ),
                timeoutMs = 6_000
            )
        }

    // --------------------------------------------------------------- status

    private fun panelLabelFromReadout(raw: String, modes: List<PanelMode>): String {
        val idx = Regex("panel_mode:(\\d+)").find(raw)?.groupValues?.get(1)
        modes.firstOrNull { it.panelIndex == idx }?.let { return "${it.width}x${it.height} @ ${it.hz} Hz" }
        return if (raw.isNotBlank()) raw.trim() else "unknown"
    }

    suspend fun getStatus(context: Context): DisplayStatus = withContext(Dispatchers.IO) {
        val profile = DeviceProfiles.detect()
        val hasRoot = RootHelper.isRootAvailable()

        var currentFps = 0f
        var fpsSource = ""
        var resolution = "?"
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display: Display? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display
                else @Suppress("DEPRECATION") wm.defaultDisplay
            display?.let {
                currentFps = it.mode.refreshRate
                resolution = "${it.mode.physicalWidth}x${it.mode.physicalHeight}"
            }
            if (currentFps > 0) fpsSource = "display"
        } catch (_: Exception) {
        }

        var minRate = "?"
        var peakRate = "?"
        var panelLabel = "unknown"
        var isAod = false
        var showHz = false
        var density = 0
        var modes = profile.fallbackModes

        if (hasRoot) {
            // ONE root session for every value: spawning one su per query
            // saturates Magisk's daemon and makes root flaky for the whole
            // system. Batched with markers and parsed below.
            val batch = RootHelper.runCommands(
                listOf(
                    "echo MIN=\$(settings get system min_refresh_rate)",
                    "echo PEAK=\$(settings get system peak_refresh_rate)",
                    "echo RATE=\$(dumpsys SurfaceFlinger | grep -m1 renderRate)",
                    "echo PDM=\$(cat ${profile.panelBase}/display_mode 2>/dev/null)",
                    "echo AOD=\$(settings get secure doze_always_on)",
                    "echo SHOWHZ=\$(settings get global show_refresh_rate_overlay)",
                    "echo DENS=\$(wm density)"
                ),
                timeoutMs = 20_000
            ).stdout

            fun field(key: String): String =
                Regex("(?m)^$key=(.*)$").find(batch)?.groupValues?.get(1)?.trim() ?: ""

            field("MIN").takeIf { it.isNotBlank() && it != "null" }?.let { minRate = it }
            field("PEAK").takeIf { it.isNotBlank() && it != "null" }?.let { peakRate = it }
            Regex("renderRate=([0-9.]+)").find(field("RATE"))?.groupValues?.get(1)
                ?.toFloatOrNull()?.let {
                    if (it > 0 && currentFps <= 0f) {
                        currentFps = it
                        fpsSource = "surfaceflinger"
                    }
                }
            val pdm = field("PDM")
            modes = discoverPanelModes(profile)
            panelLabel = panelLabelFromReadout(pdm, modes)
            isAod = field("AOD") == "1"
            showHz = field("SHOWHZ") == "1"
            density = Regex("\\d+").find(field("DENS"))
                ?.value?.toIntOrNull() ?: 0
        }

        // Detect a competing tuner app: another package requesting su while we
        // run explains "applied but the panel snaps back" reports.
        var conflict = ""
        if (hasRoot) {
            val suList = RootHelper.runCommand(
                "ps -A 2>/dev/null | grep -E 'su|magisk' | grep -v grep | head -5"
            ).stdout
            if (suList.contains("s20tuner") || suList.contains("tuner")) {
                conflict = "S20 Tuner"
            }
        }

        val activeHz = when {
            peakRate.startsWith("96") || minRate.startsWith("96") -> 96
            peakRate.startsWith("120") || minRate.startsWith("120") -> 120
            currentFps >= 115f -> 120
            currentFps >= 90f -> 96
            else -> 60
        }
        val adaptive: AdaptiveMode = when {
            minRate.isNotBlank() && peakRate.isNotBlank() && minRate == peakRate -> AdaptiveMode.FIXED
            minRate != "?" && peakRate != "?" &&
                minRate.toFloatOrNull() != peakRate.toFloatOrNull() -> AdaptiveMode.ADAPTIVE_RANGE
            else -> AdaptiveMode.FIXED
        }

        DisplayStatus(
            currentFps = currentFps,
            fpsSource = fpsSource,
            activeModeHz = activeHz,
            resolution = resolution,
            density = density,
            panelModeLabel = panelLabel,
            minRefreshRate = minRate,
            peakRefreshRate = peakRate,
            refreshRateLocked = adaptive == AdaptiveMode.FIXED,
            adaptiveMode = adaptive,
            isAodEnabled = isAod,
            showRefreshRateOverlay = showHz,
            hasRoot = hasRoot,
            deviceModel = Build.MODEL,
            deviceTitle = DeviceProfiles.familyTitle(profile),
            conflictApp = conflict,
            availableModes = modes,
            availableResolutions = resolutionsFrom(modes, profile)
        )
    }
}
