package com.example.galaxyhz.data

import android.os.Build

/**
 * Everything the app needs to drive one device family. The panel mode list is
 * normally auto-discovered at runtime; the static lists are fallbacks.
 */
data class DeviceProfile(
    val key: String,
    val title: String,
    val panelBase: String,
    val defaultDensity: Int,
    val fallbackModes: List<PanelMode>,
    val fallbackResolutions: List<ResolutionOption>
)

/**
 * Supported device families. `matches` checks Build.DEVICE / Build.PRODUCT.
 * Any Samsung device exposing panel_drv sysfs works through auto-discovery
 * even when no profile matches (generic profile).
 */
object DeviceProfiles {

    private val SAMSUNG_PANEL = "/sys/devices/platform/panel_drv@0/lcd/panel"

    private val x1s = DeviceProfile(
        key = "x1s",
        title = "Galaxy S20 (x1s)",
        panelBase = SAMSUNG_PANEL,
        defaultDensity = 480,
        fallbackModes = listOf(
            PanelMode(1080, 2400, 120, "HS", "1"),
            PanelMode(1080, 2400, 96, "HS", "2"),
            PanelMode(1080, 2400, 60, "NS", "3"),
            PanelMode(1440, 3200, 60, "NS", "0")
        ),
        fallbackResolutions = listOf(
            ResolutionOption(1080, 2400, "FHD+ 1080 x 2400"),
            ResolutionOption(1440, 3200, "WQHD+ 1440 x 3200")
        )
    )

    private val generic = DeviceProfile(
        key = "generic",
        title = "Samsung panel (auto-detected)",
        panelBase = SAMSUNG_PANEL,
        defaultDensity = 480,
        fallbackModes = listOf(
            PanelMode(1080, 2400, 120, "HS", ""),
            PanelMode(1080, 2400, 96, "HS", ""),
            PanelMode(1080, 2400, 60, "NS", "")
        ),
        fallbackResolutions = listOf(ResolutionOption(1080, 2400, "FHD+ 1080 x 2400"))
    )

    /** Profile -> codename fragments -> friendly family name. */
    private val known = listOf(
        Triple(
            x1s,
            listOf("x1s", "x1q", "y2s", "c1s", "c2s", "t2s"),
            "Galaxy S20 / S20+ / S20 Ultra / Note 10+"
        )
    )

    fun detect(): DeviceProfile {
        val dev = (Build.DEVICE + " " + Build.PRODUCT).lowercase()
        for ((profile, keys, _) in known) {
            if (keys.any { dev.contains(it) }) return profile
        }
        return generic
    }

    fun familyTitle(profile: DeviceProfile): String =
        known.firstOrNull { it.first.key == profile.key }?.third ?: profile.title
}
