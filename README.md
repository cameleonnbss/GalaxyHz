# GalaxyHz — Force 120 / 96 / 60 Hz on Samsung Galaxy S20 (x1s)

A tiny root app + Magisk module that **forces 120 Hz, 96 Hz or 60 Hz** on Galaxy S20-series
phones running AOSP-based ROMs (Evolution X, LineageOS, crDroid, /e/OS…) that lack Samsung's
One UI refresh-rate service.

Everything is written through **four layers** so the rate really sticks:

1. `cmd display set-user-preferred-display-mode` (DisplayManager / SurfaceFlinger)
2. `settings put system|secure|global ...` (framework refresh-rate policy)
3. `wm size` (render resolution)
4. `/sys/devices/platform/panel_drv@0/lcd/panel/display_mode` (direct panel driver mode)

Verified live on a Galaxy S20 (SM-G981B, `x1s`) running Evolution X / Android 16:
SurfaceFlinger reports `renderRate=120.00 Hz` (or 96.00) with the panel locked in the
HS clock region — no more mid-frame clock desync, sparkle or static.

## Features

### 📱 GalaxyHz app
- **One-tap switching** between **120 Hz** (ultra smooth), **96 Hz** (eco smooth —
  recommended: feels like 120 Hz with far less heat and +30% battery) and **60 Hz**
  (battery saver).
- **Live status** — current FPS, active panel mode (`1080x2400_120HS`…), resolution
  and root state, refreshed every 2 s.
- **Adaptive behavior settings**
  - *Lock refresh rate* — kills SurfaceFlinger idle/content detection so the panel
    never drops to 60 Hz when content is static (flicker source #1).
  - *AOD toggle* — Always-On Display is the main flicker source on S20 OLED
    (60 Hz / AID clock region); toggle it safely from the app.
- **Flicker protection tools** (for when static/sparkle already happened)
  1. *Apply anti-flicker props* — resets SurfaceFlinger idle/touch/power timers.
  2. *Repair mode on all layers* — re-writes DisplayManager, settings, wm and panel mode.
  3. *Emergency panel reset (Reset DDI)* — power-cycles the display controller;
     clears mid-frame artifacts instantly.
- **Quick Settings tile** — cycles 120 → 96 → 60 Hz from the notification shade,
  no need to open the app.

### 📦 Magisk module (`force_120hz_x1s.zip`)
- Re-applies the persisted mode (default **120 Hz**) at every boot.
- Applies anti-flicker SurfaceFlinger props early (`post-fs-data`) and again at boot.
- **Action button** in the Magisk app (Modules tab) re-applies the mode on demand.
- Config file `/data/adb/force_hz.conf` — write `96` or `60` to switch, `120` to go back.
- Log: `/data/local/tmp/force_120hz_x1s.log` (contains a full state dump).

## Requirements
- Samsung Galaxy S20 series (S20 / S20+ / S20 Ultra — Exynos `x1s` family; Snapdragon
  `y2s` should work the same) or any device exposing the same panel sysfs nodes.
- A rooted ROM with **Magisk**.
- An AOSP-based ROM. On One UI the stock service already manages rates.

## Install

### App
```bash
adb install GalaxyHz_v1.1.apk
```
Open the app once and grant the Magisk superuser prompt.

### Module
Flash `force_120hz_x1s.zip` in the Magisk app (Modules → Install from storage), or:
```bash
adb push force_120hz_x1s.zip /sdcard/Download/
su -c "magisk --install-module /sdcard/Download/force_120hz_x1s.zip"
```
Reboot. Check `/data/local/tmp/force_120hz_x1s.log` for the state dump.

## Switching rates from a terminal
```bash
# From the app: one tap. From a root shell:
echo 96 > /data/adb/force_hz.conf    # 96, 60 or 120
sh /data/adb/modules/force_120hz_x1s/action.sh
```

## Build the app yourself
```bash
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests (CI runs these too)
```
Requires JDK 17+ and the Android SDK (API 36).

## Troubleshooting
| Symptom | Fix |
|---|---|
| Screen static / sparkle after a while | Apply *anti-flicker props*, then *Repair mode*; disable AOD |
| Glitch bands mid-frame | *Reset DDI* (emergency panel reset); screen blinks once and clears |
| Rate drops after unlock | Make sure the Magisk module is installed so props are set at boot |
| 1440p after a ROM update | Tap any rate in the app — `wm size` is re-applied |

## License
MIT — see [LICENSE](LICENSE).
