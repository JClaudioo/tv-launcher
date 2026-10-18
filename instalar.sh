#!/usr/bin/env bash
# Instala o launcher na TV box e o coloca como tela inicial.
# Uso: ./instalar.sh tvhome.apk      (com a box já conectada pelo "adb connect IP:5555")
set -u
APK="${1:-tvhome.apk}"
PKG=com.joao.tvhome
STOCK=com.google.android.tvlauncher

adb get-state >/dev/null 2>&1 || { echo "Box não conectada. Rode: adb connect IP_DA_BOX:5555"; exit 1; }
[ -f "$APK" ] || { echo "Arquivo $APK não encontrado."; exit 1; }

echo "1/4  Instalando $APK..."
saida=$(adb install -r "$APK" 2>&1)
echo "$saida" | tail -1
echo "$saida" | grep -q Success || { echo "A instalação falhou. Nada foi alterado na tela inicial."; exit 1; }

echo "2/4  Definindo como tela inicial..."
adb shell cmd package set-home-activity "$PKG/.MainActivity"
atual=$(adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r')
echo "     Tela inicial agora: $atual"
case "$atual" in
  "$PKG"/*) ;;
  *) echo "Não consegui definir como tela inicial. O launcher original continua ativo."; exit 1 ;;
esac

echo "3/4  Desativando o launcher original do Google (dá para desfazer com ./restaurar-launcher.sh)..."
adb shell pm disable-user --user 0 "$STOCK"

echo "4/4  Abrindo a nova tela inicial..."
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
echo
echo "Pronto! Aperte o botão Home do controle para conferir."
