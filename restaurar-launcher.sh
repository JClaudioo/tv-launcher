#!/usr/bin/env bash
# Volta para o launcher original do Google TV e remove o launcher novo.
# Uso: ./restaurar-launcher.sh        (Mi Box)
#      ./restaurar-launcher.sh tcl    (TV TCL)
set -u
if [ "${1:-}" = "tcl" ]; then
  PKG=com.joao.tvhome.tcl; STOCK=com.google.android.apps.tv.launcherx
else
  PKG=com.joao.tvhome; STOCK=com.google.android.tvlauncher
fi

adb get-state >/dev/null 2>&1 || { echo "Box não conectada. Rode: adb connect IP_DA_BOX:5555"; exit 1; }

adb shell pm enable --user 0 "$STOCK"
[ "${1:-}" = "tcl" ] && adb shell pm enable --user 0 com.google.android.tungsten.setupwraith
adb uninstall "$PKG"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
echo "Launcher original restaurado."
