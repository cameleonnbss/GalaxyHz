#!/system/bin/sh

ui_print "****************************************"
ui_print "  GalaxyHz - Force 120/96/60Hz v4.0"
ui_print "****************************************"
ui_print "- Samsung Galaxy S20 series (x1s) + more"
ui_print "- Panel modes auto-resolved at runtime"
ui_print "- Modes: 120 / 96 / 60 Hz (or hidden HS)"
ui_print "- FHD+ / WQHD+ resolution switching"
ui_print "- Anti-flicker props at every boot"
ui_print "- Config: /data/adb/force_hz.conf"
ui_print "  e.g. 120 | 96 | 60 1440x3200"
ui_print "- Magisk Action button for on-demand apply"
ui_print "- Log: /data/local/tmp/force_120hz_x1s.log"
ui_print "----------------------------------------"

chmod -R 0755 "$MODPATH"
set_perm_recursive "$MODPATH" 0 0 0755 0755
