#!/system/bin/sh
# GalaxyHz - boot service: applies the persisted refresh-rate mode after boot.

MODDIR=${0%/*}
. "$MODDIR/common/force_120hz.sh"

force_hz_boot
