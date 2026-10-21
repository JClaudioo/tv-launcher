#!/usr/bin/env bash
# Limpeza da TV TCL (Google TV): desativa propaganda, apps da TCL sem uso, espelhamento e apps que não são usados.
# Nada é apagado: tudo volta com ./restaurar-tcl.sh
# Uso: ANDROID_SERIAL=IP_DA_TCL:5555 ./limpar-tcl.sh
set -u
adb get-state >/dev/null 2>&1 || { echo "TV não conectada. Rode: adb connect IP_DA_TCL:5555"; exit 1; }

PACOTES=(
  # Propaganda e recomendações da TCL
  com.tcl.waterfall.overseas
  com.tcl.tv.tclhome_passive
  com.tcl.esticker
  com.tcl.messagebox
  com.tcl.exhibit
  com.tcl.channelplus
  com.mirakulo.astrotv.service
  com.tcl.usercenter
  com.tcl.interactive
  com.tcl.logkit
  # Apps da TCL sem uso
  com.tcl.smartalexa
  com.tcl.gamebar
  com.tcl.ocean.instructions
  com.tcl.hotelmenu
  com.tcl.browser
  com.tcl.magiconnectfree
  com.tcl.ui_mediaCenter
  # Proteção de tela do Google com sugestões (volta a simples)
  com.google.android.apps.tv.dreamx
  # Espelhamento e Chromecast
  com.google.android.apps.mediashell
  com.amlogic.airplay
  com.tcl.airplay2
  com.tcl.miracast
  # Apps que não são usados
  com.amazon.amazonvideo.livingroom
  com.viki.android
  com.google.android.videos
  com.google.android.play.games
  com.google.android.marvin.talkback
  com.google.android.feedback
)

ok=0; falhou=()
for p in "${PACOTES[@]}"; do
  if adb shell pm disable-user --user 0 "$p" 2>&1 | grep -q "new state: disabled"; then
    ok=$((ok+1)); echo "  desativado  $p"
  else
    falhou+=("$p")
  fi
done
echo
echo "Pronto: $ok de ${#PACOTES[@]} desativados."
if [ ${#falhou[@]} -gt 0 ]; then
  echo "Não deu para desativar (o sistema não deixa ou não existe nesta TV):"
  printf '  %s\n' "${falhou[@]}"
fi
echo "Para desfazer tudo: ./restaurar-tcl.sh"
