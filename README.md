# GalaxyHz — Force 120 / 96 / 60 Hz & more on Samsung Galaxy devices

A Material 3 **Expressive** root app + Magisk module that **forces any refresh rate your
panel supports** — 120 Hz, 96 Hz, 60 Hz, even the *hidden* intermediate HS clocks
(100 / 104 / 110 / 112 Hz) — on Samsung Galaxy phones running AOSP-based ROMs
(Evolution X, LineageOS, crDroid…) that lack Samsung's One UI refresh-rate service.

Panel modes are **discovered at runtime** from the device's own mode table
(`/sys/.../panel/display_mode`), so the app adapts to your exact hardware instead of
hardcoding one phone. Verified live on a Galaxy S20 (SM-G981B, `x1s`) on
Evolution X / Android 16: SurfaceFlinger reports `renderRate=120.00 Hz` with the panel
locked in the HS clock region — no more mid-frame desync, sparkle or static.

## Screens

| | |
|---|---|
| **Home** — live FPS, three main modes, quick lock | **Resolution** — FHD+ / WQHD+ switching |
| **Custom rate** — 24–240 Hz slider (frame-paced) | **Adaptive** — fixed / stock / custom range |
| **Experimental** — hidden HS panel modes | **Tools** — verified lock, AOD, DDI reset |

A modal navigation drawer (hamburger menu) holds everything, and a
**Quick Settings tile** cycles 120 → 96 → 60 Hz without opening the app.

## Features

### Modes
- **One-tap panel modes** — every primary mode your driver exposes (S20: 120HS / 96HS /
  60NS), each written through four layers so it sticks:
  1. `cmd display set-user-preferred-display-mode` (DisplayManager / SurfaceFlinger)
  2. `settings put system|secure|global ...` (framework refresh policy)
  3. `wm size` (render resolution)
  4. `echo <index> > .../panel/display_mode` (direct panel driver write, index resolved
     at runtime from the device's mode table)
- **Custom rate (24–240 Hz slider)** — Android advertises any 0.1 Hz-step rate to apps
  while the panel scans at its nearest real clock, with frame pacing in between.
  Great for 24 fps video (24/48 Hz) or fine battery tuning.
- **Hidden / experimental modes** — panels ship with intermediate HS clocks Samsung
  never exposed (S20: 100/104/110/112 Hz). The app lists what *your* driver reports
  and writes them directly, then reads back whether the panel took the mode.
- **Resolution switching** — FHD+ ↔ WQHD+ on panels that support both.

### Adaptive behavior
- **Fixed** — kills SurfaceFlinger idle/content detection: the anti-flicker default.
  The app *verifies* the result by reading back props and settings and shows exactly
  which layer failed, instead of pretending it worked.
- **Adaptive (stock)** — restores content detection for battery saving.
- **Adaptive with custom range** — the system adapts but stays inside a min/max you
  choose (e.g. 60–120 Hz, or 96–96 to emulate a fixed rate).

### Flicker protection & recovery
- **Lock refresh rate (apply + verify)** — idle timer 0, content detection off,
  min = peak; each layer checked and reported.
- **AOD toggle** — Always-On Display runs the panel in its 60 Hz/AID clock region and
  is the main flicker source on S20 OLED; toggle it safely from the app.
- **Emergency DDI reset** — power-cycles the display controller; clears static,
  sparkle and glitch bands instantly.

### Setup & polish
- **First-run setup wizard** — explains what the app does, offers a direct shortcut to
  Magisk for the superuser prompt, and can be skipped for non-root status reading.
- **Language setting** — English base with French translation built in.
- **Real Material 3 Expressive** — `material3 1.5.0-alpha` with
  `ExperimentalMaterial3ExpressiveApi`: `MotionScheme.expressive()` spring physics,
  `LoadingIndicator`, expressive shapes, drawer-based navigation.

## The truth about "overclocking" phone panels

Phone DDICs (display controllers) ship with a **fixed mode table burned in at the
factory**. Unlike PC monitors, there is no free-running pixel clock: a mode that is not
in the table physically cannot scan out. What *is* possible — and what this app does —
is use **every mode already in the table**, including the hidden intermediate HS clocks
Samsung never exposed in settings. If a mode is rejected, the panel simply ignores the
write; nothing breaks.

## Requirements
- Samsung Galaxy with a `panel_drv` sysfs interface (S20 / S20+ / S20 Ultra / Note 10+
  family verified; other Samsung models work through runtime discovery).
- Root with **Magisk**.
- An AOSP-based ROM (One UI already manages rates natively).

## Install

### App
```bash
adb install GalaxyHz_v2.0.apk
```
First launch shows the setup wizard; grant the Magisk superuser prompt when it appears.

### Module
Flash `force_120hz_x1s.zip` in the Magisk app (Modules → Install from storage), or:
```bash
adb push force_120hz_x1s.zip /sdcard/Download/
su -c "magisk --install-module /sdcard/Download/force_120hz_x1s.zip"
```
Reboot. The module re-applies the persisted mode at every boot, applies anti-flicker
props early, and adds an **Action button** in the Magisk app for on-demand re-apply.

**Module config** — `/data/adb/force_hz.conf`:
```text
120              # 120 Hz FHD+
96               # 96 Hz FHD+
60 1440x3200     # 60 Hz WQHD+
```

## Building
```bash
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests (panel-table parser, CI runs these)
```
Requires JDK 17+ and the Android SDK (API 36). CI (GitHub Actions) builds and tests
every push.

## Troubleshooting
| Symptom | Fix |
|---|---|
| Screen static / sparkle after a while | Tools → *Lock refresh rate* (verified), disable AOD |
| Glitch bands mid-frame | Tools → *Reset DDI*; screen blinks once and clears |
| Rate drops after unlock | Install the Magisk module so props are set at boot |
| 1440p after a ROM update | Home → tap any mode; `wm size` is re-applied |
| Experimental mode does nothing | Your unit rejected the mode; pick another |

## License
MIT — see [LICENSE](LICENSE).
