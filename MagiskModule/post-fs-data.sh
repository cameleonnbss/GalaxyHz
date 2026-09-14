#!/system/bin/sh
# GalaxyHz - early SurfaceFlinger hints (anti-flicker).
# Sets the props the framework reads when SurfaceFlinger starts, so the panel
# never idles down to 60Hz and desyncs the OLED clock.

# Magisk's resetprop must be reachable
export PATH=/data/adb/magisk:$PATH

resetprop -n ro.surface_flinger.use_content_detection_for_refresh_rate false
resetprop -n ro.surface_flinger.set_idle_timer_ms 0
resetprop -n ro.surface_flinger.set_touch_timer_ms 0
resetprop -n ro.surface_flinger.set_display_power_timer_ms 0
resetprop -n debug.sf.frame_rate_multiple_threshold 120
resetprop -n debug.sf.disable_client_composition_cache 1
