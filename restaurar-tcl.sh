#!/usr/bin/env bash
# Desfaz a limpeza da TV TCL: reativa tudo o que o limpar-tcl.sh desativou.
# Uso: ANDROID_SERIAL=IP_DA_TCL:5555 ./restaurar-tcl.sh
set -u
adb get-state >/dev/null 2>&1 || { echo "TV não conectada. Rode: adb connect IP_DA_TCL:5555"; exit 1; }
for p in $(grep -E '^\s+[a-z]+\.[A-Za-z0-9_.]+$' limpar-tcl.sh | tr -d ' '); do
  adb shell pm enable --user 0 "$p" >/dev/null 2>&1 && echo "  reativado  $p"
done
echo "Tudo reativado. Para voltar também a tela inicial do Google: ./restaurar-launcher.sh tcl"
