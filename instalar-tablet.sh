#!/usr/bin/env bash
# Instala no tablet o NewPipe (opcional) e o launcher da criança, e coloca o launcher como tela inicial.
# Uso: ./instalar-tablet.sh tvhome-tablet.apk [NewPipe.apk]
set -u
APK="${1:-tvhome-tablet.apk}"
NEWPIPE="${2:-}"
PKG=com.joao.tvhome.tablet
STOCK=com.cyanogenmod.trebuchet
adb -d get-state >/dev/null 2>&1 || { echo "Tablet não conectado no cabo."; exit 1; }
[ -f "$APK" ] || { echo "Arquivo $APK não encontrado."; exit 1; }

if [ -n "$NEWPIPE" ]; then
  echo "1/4  Instalando o NewPipe..."
  adb -d install -r "$NEWPIPE" | tail -1
fi

echo "2/4  Instalando o launcher..."
saida=$(adb -d install -r "$APK" 2>&1)
echo "     $(echo "$saida" | tail -1)"
echo "$saida" | grep -q Success || { echo "A instalação falhou. Nada foi alterado na tela inicial."; exit 1; }

echo "3/4  Testando se abre..."
adb -d shell am start -W -n "$PKG/com.joao.tvhome.MainActivity" | grep -q "Status: ok" || { echo "O launcher não abriu. Nada foi alterado."; exit 1; }

echo "4/4  Tirando a tela inicial antiga (Trebuchet)..."
adb -d shell pm disable-user "$STOCK" >/dev/null
adb -d shell input keyevent 3
echo
echo "Pronto! Aperte o botão Início do tablet: deve aparecer a tela da criança."
echo "Se aparecer uma pergunta de qual tela inicial usar, escolha \"Início\" e \"Sempre\"."
echo "Para voltar ao normal: ./restaurar-tablet.sh"
