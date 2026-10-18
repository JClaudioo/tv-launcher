# Início — launcher da TV box

Tela inicial própria para a Mi Box (Android TV 11): foto fixa do Monte Fuji com o pagode,
relógio, data, clima de Ceres-GO, dock de apps e detalhes em vermelho. Sem propaganda,
sem recomendações, sem serviços rodando em segundo plano.

## Compilar

O APK é compilado pelo GitHub Actions a cada push na branch `main`
(ou manualmente em **Actions → Compilar APK → Run workflow**).
Quando terminar, baixe o artefato **tvhome-apk** na página da execução e extraia o `tvhome.apk`.

## Instalar na box

Com a box conectada (`adb connect IP_DA_BOX:5555`):

```bash
chmod +x instalar.sh restaurar-launcher.sh
./instalar.sh tvhome.apk
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
| Apps do dock e a ordem | `DOCK` em `MainActivity.java` (nome e pacote) |
| Nome na saudação | `updateClock()` em `MainActivity.java` |
| Cor de destaque | `ACCENT` em `Ui.java` |
| Foto de fundo | `app/src/main/res/drawable-nodpi/fundo.jpg` (1920x1080) |
| Cidade do clima | `Weather.java` (latitude, longitude e `CITY`) |
| Intensidade do escurecido | `new ShadeView(this, 0.8f)` em `MainActivity.java` |

O clima vem do [Open-Meteo](https://open-meteo.com) (gratuito, sem cadastro) e atualiza a cada 30 minutos.
Sem internet, mostra a última leitura.

Fonte: Outfit, licença SIL OFL (`FONT-LICENSE-OFL.txt`).
