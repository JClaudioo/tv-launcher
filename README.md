# Início — launcher da TV box e da TV TCL

Um projeto, dois APKs (variantes do Gradle):

| APK | TV | Onde fica o que é só dela |
|---|---|---|
| `tvhome-box.apk` | Mi Box (Android TV 11) — 6 posições escolhidas na TV, foto do Monte Fuji | `app/src/box` |
| `tvhome-tcl.apk` | TV TCL (Google TV, Android 12) — todos os apps na tela, menu Sistema no topo, clima com previsão, foto da praia | `app/src/tcl` |

O que é comum (relógio, clima de Ceres, abrir apps, pedidos do celular, lista de todos os apps) fica em `app/src/main`
e vale para as duas. A versão da box antes da divisão está marcada como `box-1.2` no Git.

## TV TCL

```bash
adb connect IP_DA_TCL:5555
export ANDROID_SERIAL=IP_DA_TCL:5555
./limpar-tcl.sh                 # desativa propaganda, espelhamento e apps sem uso (lista em apps-tcl-original.txt)
./instalar.sh tvhome-tcl.apk    # instala e coloca como tela inicial
```

Desfazer: `./restaurar-tcl.sh` (reativa os apps) e `./restaurar-launcher.sh tcl` (volta a tela inicial do Google).

---


Tela inicial própria para a Mi Box (Android TV 11): foto fixa do Monte Fuji com o pagode,
relógio, data, clima de Ceres-GO, dock de apps e detalhes em vermelho. Sem propaganda,
sem recomendações, sem serviços rodando em segundo plano.

## Tela inicial: 6 posições

- **Segure OK** num app da tela inicial: Trocar app, Mover para a esquerda/direita ou Deixar vazio
- Posição vazia aparece com um **+**; OK nela abre a lista para escolher
- A escolha fica salva na box

## Pedidos do celular

O controle no launcher do S24 abre apps e a lista de apps por estes links:

```bash
adb shell am start -a android.intent.action.VIEW -d "tvlauncher://abrir?app=com.netflix.ninja"
adb shell am start -a android.intent.action.VIEW -d "tvlauncher://apps"
```

## Compilar

O APK é compilado pelo GitHub Actions a cada push na branch `main`
(ou manualmente em **Actions → Compilar APK → Run workflow**).
Quando terminar, baixe o artefato **tvhome-box** (ou **tvhome-tcl**) na página da execução e extraia o APK.

## Instalar na box

Com a box conectada (`adb connect IP_DA_BOX:5555`):

```bash
chmod +x instalar.sh restaurar-launcher.sh
./instalar.sh tvhome-box.apk
```

O script instala, define como tela inicial e só então desativa o launcher original do Google.
Para atualizar depois, rode o mesmo comando com o APK novo: a assinatura é fixa,
então ele instala por cima sem perder nada.

## Voltar ao original

```bash
./restaurar-launcher.sh
```

## Onde mudar as coisas

| O quê | Onde |
|---|---|
| Apps do dock e a ordem | Na própria TV: segure OK num app da tela inicial (padrão em `Dock.java`) |
| Nome na saudação | `updateClock()` em `MainActivity.java` |
| Cor de destaque | `ACCENT` em `Ui.java` |
| Foto de fundo | `app/src/main/res/drawable-nodpi/fundo.jpg` (1920x1080) |
| Cidade do clima | `Weather.java` (latitude, longitude e `CITY`) |
| Intensidade do escurecido | `new ShadeView(this, 0.8f)` em `MainActivity.java` |

O clima vem do [Open-Meteo](https://open-meteo.com) (gratuito, sem cadastro) e atualiza a cada 30 minutos.
Sem internet, mostra a última leitura.

Fonte: Outfit, licença SIL OFL (`FONT-LICENSE-OFL.txt`).
