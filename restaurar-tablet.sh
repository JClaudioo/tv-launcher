#!/usr/bin/env bash
# Desfaz tudo no tablet: reativa os apps, a tela inicial antiga e as animações normais.
set -u
adb -d get-state >/dev/null 2>&1 || { echo "Tablet não conectado no cabo."; exit 1; }
for p in $(grep -E '^\s+[a-z]+\.[A-Za-z0-9_.]+$' limpar-tablet.sh | tr -d ' ') com.cyanogenmod.trebuchet; do
  adb -d shell pm enable "$p" >/dev/null 2>&1 && echo "  reativado  $p"
done
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb -d shell settings put global $k 1
done
echo "Tudo de volta. O launcher da criança continua instalado; para tirar: adb -d uninstall com.joao.tvhome.tablet"
