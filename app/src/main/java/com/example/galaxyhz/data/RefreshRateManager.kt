package com.example.galaxyhz.data

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.example.galaxyhz.util.RootHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One of the three supported refresh-rate modes. */
enum class HzMode(val hz: Int, val label: String, val description: String) {
    H120(120, "120 Hz", "Ultra smooth - maximum fluidity"),
    H96(96, "96 Hz", "Eco smooth - same fluid feel, less heat and battery drain"),
    H60(60, "60 Hz", "Standard - maximum battery life");

    companion object {
        @JvmStatic
        fun fromHz(hz: Int): HzMode = when (hz) {
            96 -> H96
            120 -> H120
            else -> H60
        }
    }
}

data class DisplayStatus(
    val currentFps: Float = 0f,
    val activeModeHz: Int = 60,
    val targetMode: HzMode = HzMode.H60,
    val resolution: String = "?",
    val panelMode: String = "unknown",
    val minRefreshRate: String = "?",
    val peakRefreshRate: String = "?",
    val antiFlickerActive: Boolean = false,
    val adaptiveTileEnabled: Boolean = false,
    val isAodEnabled: Boolean = false,
    val hasRoot: Boolean = false,
    val deviceModel: String = ""
)

/**
 * Applies refresh-rate modes and display fixes on Samsung Galaxy S20-series
 * devices (x1s / SM-G981B verified) running AOSP-based ROMs such as Evolution X.
 *
 * Every mode change is written to four layers, matching what the previous
 * proven v3 workflow did from a root shell:
 *  1. `cmd display set-user-preferred-display-mode`  (DisplayManager switch)
 *  2. `settings put system/secure ...`               (framework refresh policy)
 *  3. `wm size`                                      (render resolution)
 *  4. `/sys/.../panel/display_mode`                  (direct panel driver mode)
 */
object RefreshRateManager {

    private const val PANEL_BASE = "/sys/devices/platform/panel_drv@0/lcd/panel"
    private const val CONF_FILE = "/data/adb/force_hz.conf"
    private const val WIDTH = 1080
    private const val HEIGHT = 2400

    /**
     * Panel display-mode indices from the device's own mode table
     * (`cat .../panel/display_mode`): pdm:1 = 1080x2400_120HS,
     * pdm:2 = 1080x2400_96HS, pdm:3 = 1080x2400_60NS.
     */
    private val panelModeIndex = mapOf(120 to "1", 96 to "2", 60 to "3")

    // ---------------------------------------------------------------- mode

    fun applyRefreshRateCommands(hz: Int): List<String> {
        val rateFloat = when (hz) {
            96 -> "96.00001"
            60 -> "60.000004"
            else -> "120.00001"
        }
        return listOf(
            // Keep battery saver / adaptive throttling out of the way
            "cmd power set-mode 0",
            "cmd power set-fixed-performance-mode-enabled false",
            "settings put global low_power 0",
            "settings put global low_power_sticky 0",
            "settings put global adaptive_battery_management_enabled 0",
            "settings put global automatically_reduce_refresh_rate 0",

            // Render resolution
            "wm size ${WIDTH}x$HEIGHT",
            "wm density 480",

            // Framework refresh policy (system + secure + global tables)
            "settings put system peak_refresh_rate $hz.0",
            "settings put system min_refresh_rate $hz.0",
            "settings put system user_refresh_rate $hz",
            "settings put system display_refresh_rate $hz",
            "settings put system default_peak_refresh_rate $hz.0",
            "settings put system default_refresh_rate $hz.0",
            "settings put system motion_smoothness 2",
            "settings put secure refresh_rate_mode 2",
            "settings put secure screen_resolution_mode 1",
            "settings put global match_content_frame_rate 2",

            // DisplayManager: switching across groups + preferred physical mode
            "cmd display set-match-content-frame-rate-pref 2",
            "cmd display set-user-preferred-display-mode $WIDTH $HEIGHT $rateFloat 0 false",

            // Direct panel driver write (ignored gracefully where unavailable)
            "echo ${panelModeIndex[hz] ?: "1"} > $PANEL_BASE/display_mode 2>/dev/null",

            // Persist for the Magisk module's boot service
            "echo $hz > $CONF_FILE"
        )
    }

    suspend fun applyRefreshRate(hz: Int): RootHelper.CommandResult = withContext(Dispatchers.IO) {
        RootHelper.runCommands(applyRefreshRateCommands(hz), timeoutMs = 20_000)
    }

    // ---------------------------------------------------------- anti-flicker

    /** Properties that stop SurfaceFlinger from dropping to 60Hz on idle (flicker source #1). */
    fun antiFlickerPropCommands(): List<String> = listOf(
        "resetprop -n ro.surface_flinger.use_content_detection_for_refresh_rate false",
        "resetprop -n ro.surface_flinger.set_idle_timer_ms 0",
        "resetprop -n ro.surface_flinger.set_touch_timer_ms 0",
        "resetprop -n ro.surface_flinger.set_display_power_timer_ms 0",
        "resetprop -n debug.sf.frame_rate_multiple_threshold 120",
        "resetprop -n debug.sf.disable_client_composition_cache 1"
    )

    suspend fun applyAntiFlickerFix(): RootHelper.CommandResult = withContext(Dispatchers.IO) {
        // resetprop needs the Magisk binary directory on PATH inside su shells.
        val commands = listOf("export PATH=/data/adb/magisk:\$PATH:/system/bin") + antiFlickerPropCommands()
        RootHelper.runCommands(commands)
    }

    // ------------------------------------------------------------------ AOD

    suspend fun toggleAlwaysOnDisplay(enable: Boolean): RootHelper.CommandResult =
        withContext(Dispatchers.IO) {
            val commands = buildList {
                add("settings put secure doze_always_on ${if (enable) "1" else "0"}")
                if (enable) {
                    // AOD renders at 60Hz/NS: raise its refresh cap so it never
                    // drags the panel out of the HS clock region.
                    add("settings put secure doze_always_on_refresh_rate 60.0")
                }
            }
            RootHelper.runCommands(commands)
        }

    // ------------------------------------------------------- panel recovery

    /** Power-cycles the panel DDIC - clears mid-frame clock desync artifacts. */
    suspend fun resetDisplayPanel(): RootHelper.CommandResult = withContext(Dispatchers.IO) {
        RootHelper.runCommands(
            listOf(
                "echo 0 > $PANEL_BASE/lcd_power 2>/dev/null",
                "sleep 0.3",
                "echo 1 > $PANEL_BASE/lcd_power 2>/dev/null"
            ),
            timeoutMs = 6_000
        )
    }

    // ---------------------------------------------------------- status read

    suspend fun getStatus(context: Context): DisplayStatus = withContext(Dispatchers.IO) {
        val hasRoot = RootHelper.isRootAvailable()

        // Framework-visible refresh rate and resolution work without root.
        var currentFps = 0f
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
        } catch (_: Exception) {
        }

        var minRate = "?"
        var peakRate = "?"
        var panelMode = "unknown"
        var isAod = false
        var isAntiFlicker = false

        if (hasRoot) {
            RootHelper.runCommand("settings get system min_refresh_rate").stdout
                .takeIf { it.isNotBlank() && it != "null" }?.let { minRate = it }
            RootHelper.runCommand("settings get system peak_refresh_rate").stdout
                .takeIf { it.isNotBlank() && it != "null" }?.let { peakRate = it }

            // Panel driver reports its active mode as "panel_mode:<index>"; map it to Hz.
            val pdm = RootHelper.runCommand("cat $PANEL_BASE/display_mode 2>/dev/null").stdout
            val idx = Regex("panel_mode:(\\d+)").find(pdm)?.groupValues?.get(1)
            panelMode = when (idx) {
                "1" -> "1080x2400_120HS"
                "2" -> "1080x2400_96HS"
                "3" -> "1080x2400_60NS"
                "0" -> "1440x3200_60NS"
                else -> if (pdm.isNotBlank()) pdm else "unknown"
            }

            isAod = RootHelper.runCommand("settings get secure doze_always_on").stdout.trim() == "1"
            isAntiFlicker = RootHelper.runCommand(
                "getprop ro.surface_flinger.set_idle_timer_ms"
            ).stdout.trim() == "0"
        }

        val activeHz = when {
            peakRate.startsWith("96") || minRate.startsWith("96") -> 96
            peakRate.startsWith("120") || minRate.startsWith("120") -> 120
            currentFps >= 115f -> 120
            currentFps >= 90f -> 96
            else -> 60
        }

        DisplayStatus(
            currentFps = currentFps,
            activeModeHz = activeHz,
            targetMode = HzMode.fromHz(activeHz),
            resolution = resolution,
            panelMode = panelMode,
            minRefreshRate = minRate,
            peakRefreshRate = peakRate,
            antiFlickerActive = isAntiFlicker,
            isAodEnabled = isAod,
            hasRoot = hasRoot,
            deviceModel = Build.MODEL
        )
    }
}
