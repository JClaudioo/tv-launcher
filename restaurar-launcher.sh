#!/usr/bin/env bash
# Volta para o launcher original do Google TV e remove o launcher novo.
set -u
PKG=com.joao.tvhome
STOCK=com.google.android.tvlauncher

adb get-state >/dev/null 2>&1 || { echo "Box não conectada. Rode: adb connect IP_DA_BOX:5555"; exit 1; }

adb shell pm enable --user 0 "$STOCK"
adb shell cmd package set-home-activity "$STOCK/.MainActivity" 2>/dev/null
adb uninstall "$PKG"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
echo "Launcher original restaurado."
