#!/usr/bin/env bash
# Limpeza do tablet da criança (Galaxy Tab 2, LineageOS 13 / Android 6).
# Fica só: NewPipe (vídeos), Câmera, Galeria e o sistema. Nada é apagado: tudo volta com ./restaurar-tablet.sh
# Uso: com o tablet no cabo, ./limpar-tablet.sh
set -u
adb -d get-state >/dev/null 2>&1 || { echo "Tablet não conectado no cabo (ou depuração USB desligada)."; exit 1; }

PACOTES=(
  # Telefone e mensagens
  com.android.dialer
  com.android.messaging
  com.android.contacts
  com.android.stk
  # Apps sem uso para criança
  com.android.browser
  com.android.email
  com.android.exchange
  com.android.calendar
  com.android.calculator2
  com.android.deskclock
  com.android.soundrecorder
  com.cyanogenmod.eleven
  com.cyanogenmod.filemanager
  com.android.terminal
  com.google.android.youtube
  # Papéis de parede animados e proteções de tela
  com.android.galaxy4
  com.android.wallpaper.holospiral
  com.android.phasebeam
  com.android.noisefield
  org.cyanogenmod.wallpapers.photophase
  com.cyanogenmod.wallpapers
  org.cyanogenmod.hexolibre
  com.android.wallpaper.livepicker
  org.cyanogenmod.livelockscreen.service
  com.android.dreams.phototable
  # Coisas que rodam em segundo plano sem necessidade
  org.cyanogenmod.theme.chooser2
  org.cyanogenmod.weatherservice
  org.cyanogenmod.weather.provider
  com.cyanogenmod.lockclock
  org.cyanogenmod.screencast
  org.lineageos.updater
  me.twrp.twrpapp
  com.android.printspooler
  com.android.development
  com.cyanogenmod.samsungservicemode
)

ok=0; falhou=()
for p in "${PACOTES[@]}"; do
  if adb -d shell pm disable-user "$p" 2>&1 | grep -q "new state: disabled"; then
    ok=$((ok+1)); echo "  desativado  $p"
  else
    falhou+=("$p")
  fi
done

# Menos animações: o tablet parece bem mais rápido (0.5 = metade do tempo; 1 = normal)
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb -d shell settings put global $k 0.5
done
echo "  animações em 0.5x"

echo
echo "Pronto: $ok de ${#PACOTES[@]} desativados."
if [ ${#falhou[@]} -gt 0 ]; then
  echo "Não deu para desativar (o sistema não deixa ou não existe neste tablet):"
  printf '  %s\n' "${falhou[@]}"
fi
echo "Para desfazer tudo: ./restaurar-tablet.sh"
