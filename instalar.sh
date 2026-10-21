#!/usr/bin/env bash
# Instala o launcher e o coloca como tela inicial.
# Uso: ./instalar.sh tvhome-box.apk    (Mi Box)
#      ./instalar.sh tvhome-tcl.apk    (TV TCL)
# Com a TV já conectada pelo "adb connect IP:5555" (se houver mais de uma, use ANDROID_SERIAL=IP:5555 ./instalar.sh ...)
set -u
APK="${1:-tvhome-box.apk}"
case "$APK" in
  *tcl*) PKG=com.joao.tvhome.tcl; STOCK=com.google.android.apps.tv.launcherx ;;
  *)     PKG=com.joao.tvhome;     STOCK=com.google.android.tvlauncher ;;
esac
ACT="$PKG/com.joao.tvhome.MainActivity"

home_atual() {
  adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r'
}

adb get-state >/dev/null 2>&1 || { echo "Box não conectada. Rode: adb connect IP_DA_BOX:5555"; exit 1; }
[ -f "$APK" ] || { echo "Arquivo $APK não encontrado."; exit 1; }

echo "1/4  Instalando $APK..."
saida=$(adb install -r "$APK" 2>&1)
echo "     $(echo "$saida" | tail -1)"
echo "$saida" | grep -q Success || { echo "A instalação falhou. Nada foi alterado na tela inicial."; exit 1; }

echo "2/4  Testando se o launcher novo abre..."
teste=$(adb shell am start -W -n "$ACT" 2>&1 | tr -d '\r')
echo "$teste" | grep -q "Status: ok" || { echo "O launcher novo não abriu:"; echo "$teste"; echo "Nada foi alterado na tela inicial."; exit 1; }
echo "     Abriu normalmente."

echo "3/4  Definindo como tela inicial..."
adb shell cmd package set-home-activity "$ACT" >/dev/null
# O launcher do Google tem prioridade de sistema: enquanto estiver ativo, o Android continua escolhendo ele.
adb shell pm disable-user --user 0 "$STOCK" >/dev/null
# Google TV: sem a tela do Google, o assistente de configuração assume o botão Home; desativa ele também
case "$PKG" in *tcl) adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith >/dev/null ;; esac
atual=$(home_atual)
echo "     Tela inicial agora: $atual"
case "$atual" in
  "$PKG"/*) ;;
  *)
    echo "Não consegui definir como tela inicial. Reativando o launcher original..."
    adb shell pm enable --user 0 "$STOCK" >/dev/null
    case "$PKG" in *tcl) adb shell pm enable --user 0 com.google.android.tungsten.setupwraith >/dev/null ;; esac
    exit 1 ;;
esac

echo "4/4  Abrindo a nova tela inicial..."
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
echo
echo "Pronto! Aperte o botão Home do controle para conferir."
echo "Para voltar ao launcher original: ./restaurar-launcher.sh"
