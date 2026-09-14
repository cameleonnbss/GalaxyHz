# Magisk Module — GalaxyHz (force_120hz_x1s)

This folder is the **flashable module, unzipped and browsable** — the exact same
content as `force_120hz_x1s.zip` in the releases. Read or modify any script here;
zip the folder root (with `module.prop` at its top level) to rebuild a flashable zip.

## Files

| File | Role |
|---|---|
| `module.prop` | Module identity (name, version, description) |
| `customize.sh` | Installer: prints config help, sets permissions |
| `post-fs-data.sh` | Applies anti-flicker SurfaceFlinger props as early as possible |
| `service.sh` | Boot service: waits for boot, applies the saved mode (6 passes) |
| `action.sh` | The Magisk **Action button**: re-applies the saved mode instantly |
| `common/force_120hz.sh` | Shared engine: mode discovery, apply, verify, dump |
| `system.prop` | Additional system properties |
| `META-INF/...` | Magisk installer stubs |

## Configuration

`/data/adb/force_hz.conf` on the phone:

```text
120              # 120 Hz FHD+
96               # 96 Hz FHD+ (recommended: smooth + cool)
60 1440x3200     # 60 Hz WQHD+
120 dpi 560      # manual density override (otherwise auto-scaled per resolution)
```

## If the screen flickers after a mode change

Put the phone to **sleep and wake it once** (power button off/on): the panel
re-initializes and the sparkle/static clears. That is a panel-level quirk of
HS↔NS clock transitions on S20 OLEDs, not a misconfiguration.
