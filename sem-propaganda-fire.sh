#!/usr/bin/env bash
# Tira as propagandas e os patrocinados do Fire TV Stick.
#
# Não apaga nada: usa "disable-user", que desliga o app sem desinstalar. Tudo volta com
# "pm enable" ou com um reset de fábrica. Só mexe no que existe no aparelho.
#
# Uso, com o cabo ou pela rede (veja ACESSO-ADB-FIRE.md):
#   bash sem-propaganda-fire.sh            # desliga as propagandas
#   bash sem-propaganda-fire.sh --ota      # idem, e também trava a atualização do Fire OS
#   bash sem-propaganda-fire.sh --home     # define o nosso launcher como tela inicial
#   bash sem-propaganda-fire.sh --desfazer # liga tudo de volta
set -u

ALVO=$(adb devices | awk 'NR>1 && $2=="device" {print $1}' | head -1)
[ -z "$ALVO" ] && { echo "Nenhum Fire TV conectado. Rode 'adb connect IP:5555' antes."; exit 1; }
A() { adb -s "$ALVO" "$@"; }

LAUNCHER="com.joao.tvhome.fire/com.joao.tvhome.MainActivity"

# Propagandas, patrocinados, recomendações e apps de loja que não servem no dia a dia.
# Lista afinada para o Fire deste usuário (conferida com 'pm list packages | grep amazon').
# Nada aqui mexe em Wi-Fi, Appstore, controle remoto, Prime Video nem Amazon Music.
PROPAGANDA=(
  com.amazon.ftvads.deeplinking     # anúncios do Fire TV
  com.amazon.hybridadidservice      # ID de publicidade (rastreio)
  com.amazon.aca                    # Amazon Click Agent (rastreio de anúncios)
  com.amazon.media.recommendations  # motor de recomendações
  com.amazon.hedwig                 # recomendações de conteúdo
  com.amazon.tv.csapp               # app de conteúdo patrocinado em destaque na tela
  com.amazon.kso.blackbird          # conteúdo patrocinado da tela inicial da Amazon
  com.amazon.sneakpeek              # a prévia que toca sozinha quando o foco para num app
  com.amazon.shoptv.firetv.client   # Shop TV (compras)
  com.amazon.shoptv.client          # Shop TV (compras)
  com.amazon.minitv.android.app     # MiniTV (streaming com anúncio)
  com.amazon.gamehub                # vitrine de jogos
  com.amazon.recess                 # proteção de tela com promoções da Amazon
  com.amazon.tmm.tutorial           # tutorial/propaganda da primeira vez
  com.amazon.tv.legal.notices       # avisos legais recorrentes
)

# Atualização do Fire OS. Travar evita que uma atualização volte o launcher de fábrica e
# refaça a brecha — mas também segura correções de segurança. Por isso é opcional (--ota).
OTA=(
  com.amazon.device.software.ota
  com.amazon.device.software.ota.override
)

existe() { A shell "pm list packages $1" 2>/dev/null | tr -d '\r' | grep -qx "package:$1"; }

desligar() {
  local lista=("$@") mexeu=0
  for p in "${lista[@]}"; do
    if existe "$p"; then
      r=$(A shell "pm disable-user --user 0 $p" 2>&1 | tr -d '\r')
      echo "  desligado: $p"
      mexeu=1
    else
      echo "  (não existe neste aparelho, pulei): $p"
    fi
  done
  return $mexeu
}

case "${1:-}" in
  --desfazer)
    echo "Religando tudo o que o script desliga…"
    for p in "${PROPAGANDA[@]}" "${OTA[@]}"; do
      existe "$p" && { A shell "pm enable $p" >/dev/null 2>&1; echo "  religado: $p"; }
    done
    echo "Pronto. Se algo não voltar, um reset de fábrica religa tudo."
    exit 0
    ;;
  --home)
    echo "Definindo o nosso launcher como tela inicial…"
    A shell "cmd package set-home-activity --user 0 $LAUNCHER" | tr -d '\r' | sed 's/^/  /'
    echo "  Se o Fire voltar sozinho ao launcher de fábrica, instale o Launcher Manager"
    echo "  (veja ACESSO-ADB-FIRE.md) e deixe-o rodando."
    exit 0
    ;;
esac

echo "===== desligando propagandas e patrocinados"
desligar "${PROPAGANDA[@]}"

if [ "${1:-}" = "--ota" ]; then
  echo
  echo "===== travando a atualização do Fire OS (--ota)"
  desligar "${OTA[@]}"
fi

echo
echo "Pronto. Reinicie o Fire (Configurações › Meu Fire TV › Reiniciar) para valer tudo."
echo "Para religar qualquer coisa:  bash sem-propaganda-fire.sh --desfazer"
