#!/system/bin/sh
# GalaxyHz - shared logic for applying display modes on Samsung panel_drv devices.
# Panel mode indices are resolved at runtime from the device's own mode table,
# so any supported phone (S20 family and beyond) works without hardcoding.

LOG=/data/local/tmp/force_120hz_x1s.log
DISPLAY_ID=0
CONF_FILE=/data/adb/force_hz.conf
PANEL=/sys/devices/platform/panel_drv@0/lcd/panel
DEFAULT_W=1080
DEFAULT_H=2400
DENSITY_SCALE=480   # density at DEFAULT_H; auto-scaled for other resolutions

log_msg() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOG"
}

run_cmd() {
  log_msg "+ $*"
  "$@" >> "$LOG" 2>&1
  rc=$?
  log_msg "rc=$rc"
  return $rc
}

# Config file: first integer = refresh rate, optional "WxH" token = resolution.
# Examples: "120", "96", "60 1440x3200"
get_conf() {
  if [ -f "$CONF_FILE" ]; then
    cat "$CONF_FILE" 2>/dev/null | tr -dc '0-9x \n'
  fi
}

get_target_rate() {
  RATE=$(get_conf | awk '{print $1}')
  case "$RATE" in
    96|60|120|110|112|100|104) echo "$RATE" ;;
    *) echo 120 ;;
  esac
}

get_target_res() {
  RES=$(get_conf | grep -oE '[0-9]+x[0-9]+' | head -1)
  if [ -n "$RES" ]; then
    echo "$RES"
  else
    echo "${DEFAULT_W}x${DEFAULT_H}"
  fi
}

# Density: manual override with a "dpi N" token in the conf file, otherwise
# auto-scaled from DENSITY_SCALE so the UI keeps the same physical size when
# switching between FHD+ and WQHD+.
get_density() {
  D=$(get_conf | grep -oE 'dpi [0-9]+' | head -1 | grep -oE '[0-9]+')
  if [ -n "$D" ]; then
    echo "$D"
  else
    RES=$(get_target_res)
    H=${RES#*x}
    echo $(( (DENSITY_SCALE * H / DEFAULT_H + 5) / 10 * 10 ))
  fi
}

# Find the panel driver index for WxH@RATE by parsing the device's mode table.
# Prefers primary (pdm) entries over compatible (cpdm) ones, HS over NS.
resolve_panel_index() {
  W=$1; H=$2; RATE=$3
  TABLE=$(cat "$PANEL/display_mode" 2>/dev/null) || TABLE=""
  IDX=""
  if [ -n "$TABLE" ]; then
    IDX=$(echo "$TABLE" | grep -E "^pdm:[0-9]+[[:space:]]+${W}x${H}_${RATE}HS" | head -1 | sed 's/^pdm:\([0-9]*\).*/\1/')
    if [ -z "$IDX" ]; then
      IDX=$(echo "$TABLE" | grep -E "^(pdm|cpdm):[0-9]+[[:space:]]+${W}x${H}_${RATE}HS" | head -1 | sed 's/^[a-z]*:\([0-9]*\).*/\1/')
    fi
    if [ -z "$IDX" ]; then
      # NS fallback (e.g. 60Hz)
      IDX=$(echo "$TABLE" | grep -E "^pdm:[0-9]+[[:space:]]+${W}x${H}_${RATE}NS" | head -1 | sed 's/^pdm:\([0-9]*\).*/\1/')
    fi
  fi
  echo "$IDX"
}

disable_refresh_rate_limiters() {
  run_cmd cmd power set-mode 0
  run_cmd settings put global low_power 0
  run_cmd settings put global low_power_sticky 0
  run_cmd settings put global adaptive_battery_management_enabled 0
  run_cmd settings put global automatically_reduce_refresh_rate 0
}

apply_flicker_props() {
  run_cmd resetprop -n ro.surface_flinger.use_content_detection_for_refresh_rate false
  run_cmd resetprop -n ro.surface_flinger.set_idle_timer_ms 0
  run_cmd resetprop -n ro.surface_flinger.set_touch_timer_ms 0
  run_cmd resetprop -n ro.surface_flinger.set_display_power_timer_ms 0
  run_cmd resetprop -n debug.sf.frame_rate_multiple_threshold 120
  run_cmd resetprop -n debug.sf.disable_client_composition_cache 1
}

apply_hz_once() {
  RATE=$(get_target_rate)
  RES=$(get_target_res)
  W=${RES%x*}
  H=${RES#*x}
  case "$RATE" in
    96) RATE_FLOAT="96.00001" ;;
    60) RATE_FLOAT="60.000004" ;;
    120) RATE_FLOAT="120.00001" ;;
    *) RATE_FLOAT="${RATE}.000004" ;;
  esac

  log_msg "Applying target=${W}x${H}@${RATE}Hz on display=$DISPLAY_ID"

  disable_refresh_rate_limiters

  run_cmd wm size "${W}x${H}"
  run_cmd wm density "$(get_density)"

  run_cmd settings put system peak_refresh_rate "$RATE.0"
  run_cmd settings put system min_refresh_rate "$RATE.0"
  run_cmd settings put system user_refresh_rate "$RATE"
  run_cmd settings put system display_refresh_rate "$RATE"
  run_cmd settings put system default_peak_refresh_rate "$RATE.0"
  run_cmd settings put system default_refresh_rate "$RATE.0"
  run_cmd settings put system motion_smoothness 2

  run_cmd settings put secure refresh_rate_mode 2
  if [ "$H" -ge 3000 ]; then
    run_cmd settings put secure screen_resolution_mode 0
  else
    run_cmd settings put secure screen_resolution_mode 1
  fi
  run_cmd settings put global match_content_frame_rate 2
  run_cmd settings delete global restricted_device_performance

  run_cmd cmd display set-match-content-frame-rate-pref 2
  run_cmd cmd display set-user-preferred-display-mode "$W" "$H" "$RATE_FLOAT" "$DISPLAY_ID" false

  # Panel driver write, index resolved from the device's own mode table
  PMODE=$(resolve_panel_index "$W" "$H" "$RATE")
  if [ -n "$PMODE" ] && [ -e "$PANEL/display_mode" ]; then
    echo "$PMODE" > "$PANEL/display_mode" 2>/dev/null
    log_msg "panel display_mode <= $PMODE (${W}x${H}_${RATE})"
  else
    log_msg "no panel index resolved for ${W}x${H}@${RATE} (framework mode only)"
  fi
}

dump_state() {
  log_msg "--- state ---"
  {
    echo "preferred:"
    cmd display get-user-preferred-display-mode "$DISPLAY_ID"
    echo "settings:"
    settings get system peak_refresh_rate
    settings get system min_refresh_rate
    settings get secure refresh_rate_mode
    echo "wm:"
    wm size
    echo "panel:"
    cat "$PANEL/display_mode" 2>/dev/null
    echo "surfaceflinger:"
    dumpsys SurfaceFlinger | grep -E 'renderRate|activeMode' | head -4
  } >> "$LOG" 2>&1
  log_msg "--- end state ---"
}

force_hz_boot() {
  mkdir -p /data/local/tmp
  : > "$LOG"
  log_msg "GalaxyHz boot run started (target $(get_target_rate)Hz $(get_target_res))"

  export PATH=/data/adb/magisk:$PATH

  until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 2
  done
  sleep 4

  apply_flicker_props

  i=1
  while [ "$i" -le 6 ]; do
    log_msg "Boot pass $i"
    apply_hz_once
    sleep 3
    i=$((i + 1))
  done

  dump_state
  log_msg "GalaxyHz boot run finished"
}

force_hz_now() {
  mkdir -p /data/local/tmp
  : > "$LOG"
  log_msg "GalaxyHz manual run started (target $(get_target_rate)Hz $(get_target_res))"
  export PATH=/data/adb/magisk:$PATH
  apply_flicker_props
  apply_hz_once
  sleep 1
  dump_state
  log_msg "GalaxyHz manual run finished"
}
