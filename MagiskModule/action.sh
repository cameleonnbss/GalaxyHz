#!/system/bin/sh
# GalaxyHz - Magisk Action button: re-applies the persisted refresh-rate mode.

MODDIR=${0%/*}
. "$MODDIR/common/force_120hz.sh"

force_hz_now
