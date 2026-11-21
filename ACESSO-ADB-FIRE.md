# Fire TV Stick — acesso ADB, launcher e propagandas

Guia do zero: ligar o ADB, instalar o launcher, deixá-lo como tela inicial e tirar as
propagandas. Feito para a TV de ~21" com o Fire TV Stick.

---

## 1. Ligar o ADB no Fire TV

1. **Configurações › Meu Fire TV › Sobre** — role até o nome do aparelho e clique nele
   **7 vezes**, até aparecer "Agora você é um desenvolvedor".
2. Volte uma tela: **Configurações › Meu Fire TV › Opções do desenvolvedor**.
3. Ligue **Depuração ADB**. (Se existir "Aplicativos de fontes desconhecidas", ligue também —
   é o que permite instalar o APK.)
4. Anote o IP: **Configurações › Rede** → selecione a sua Wi-Fi → o IP aparece à direita.

> Nas opções do desenvolvedor mais novas o nome pode ser "Depuração por USB/ADB". É a mesma coisa.

---

## 2. Conectar do PC

No PC (Zorin), com o Fire e o PC na mesma rede:

```
adb connect 192.168.31.XX:5555
```

Troque pelo IP que você anotou. **Na primeira vez a TV mostra um aviso** pedindo para
autorizar o computador — marque "Sempre permitir" e confirme com o controle, senão o
`adb devices` mostra `unauthorized`.

Confira:

```
adb devices
```

Tem que aparecer o aparelho como `device`.

---

## 3. Instalar o launcher

```
adb install -r tvhome-fire-1.6.apk
```

O `-r` reinstala por cima mantendo as configurações. Se der
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, é assinatura diferente da que está instalada:
`adb uninstall com.joao.tvhome.fire` e instale de novo (perde as posições salvas).

---

## 4. Deixar como tela inicial

O Fire OS não tem a opção de trocar a tela inicial no menu — é preciso o ADB e um ajudante.

**Passo 1 — apontar o Home para o nosso launcher:**

```
adb shell cmd package set-home-activity --user 0 com.joao.tvhome.fire/com.joao.tvhome.MainActivity
```

(ou, mais fácil: `bash sem-propaganda-fire.sh --home`)

**Passo 2 — impedir o Fire de voltar ao launcher de fábrica.** O sistema reverte sozinho
depois de um tempo. Para fixar, instale um destes apps gratuitos e deixe rodando:

- **Launcher Manager** (fórum XDA) — você escolhe o launcher padrão e ele segura a escolha.
- **Home on Fire** (fórum XDA) — mapeia o botão Home e o boot para o launcher escolhido;
  funciona no Fire OS 7 e 8.

Instale o APK do ajudante pelo mesmo `adb install`, abra uma vez e aponte para
**Início (com.joao.tvhome.fire)**.

> A Amazon fecha essas brechas de tempos em tempos. Se depois de uma atualização do Fire OS
> o launcher de fábrica voltar, repita o passo 2 — por isso existe a opção `--ota`, que
> trava a atualização.

---

## 5. Tirar as propagandas

```
bash sem-propaganda-fire.sh          # desliga promoções, patrocinados e telemetria
bash sem-propaganda-fire.sh --ota    # idem, e trava a atualização do Fire OS
```

Só desliga (não apaga), mexe apenas no que existe no aparelho, e é reversível:

```
bash sem-propaganda-fire.sh --desfazer
```

Reinicie o Fire depois (**Configurações › Meu Fire TV › Reiniciar**).

---

## Dúvidas rápidas

- **O botão Home abre o launcher da Amazon, não o nosso.** Falta o passo 2 (o ajudante).
  Sem ele, o `set-home-activity` dura pouco.
- **A conexão ADB cai depois de reiniciar.** Normal: o Fire desliga o ADB por rede ao
  reiniciar. É só ligar de novo em Opções do desenvolvedor e refazer o `adb connect`.
- **Quero voltar tudo ao normal.** `--desfazer` religa as propagandas; para o launcher,
  `adb shell cmd package set-home-activity --user 0 com.amazon.tv.launcher/.ui.HomeActivity_vNext`
  e desinstale o ajudante.
