# NFC Audio Recorder

App Android minimo para validar a ideia: aproximar uma tag NFC inicia a gravacao de audio; aproximar a mesma tag de novo para e salva o arquivo.

## Fluxo

1. Abre o app: "Aproxime a tag NFC para iniciar"
2. 1a aproximacao da tag -> inicia gravacao (microfone)
3. 2a aproximacao da **mesma** tag -> para e salva o audio
4. Dialogo mostra nome, caminho e tamanho do arquivo

Aproximar uma tag diferente durante a gravacao exibe um aviso e nao interrompe a gravacao em curso.

## Onde o audio e salvo

`Android/data/com.example.nfcaudiorecorder/files/Music/REC_yyyyMMdd_HHmmss.3gp`

Diretorio externo privado do app. Acessivel pelo gerenciador de arquivos em
`Android/data/...` (Android 11+ pode exigir um gerenciador com acesso a dados de apps,
ou cabo USB pelo PC). Gravar direto em `Downloads/monitor` exigiria MediaStore ou
MANAGE_EXTERNAL_STORAGE - nao vale para um prototipo.

## Requisitos

- Android 8.0+ (API 26), aparelho com hardware NFC
- Permissoes: NFC (instalacao), microfone e notificacoes (runtime)

## Build (GitHub Actions, nada instalado no PC)

1. Push na branch `main` ou aba Actions -> "Build APK" -> Run workflow
2. Abra a execucao concluida e baixe o artifact **app-debug** (contem `app-debug.apk`)
3. No celular, permita "instalar apps de fontes desconhecidas" e instale o APK

## Build local (opcional)

Precisa de JDK 17 + Android SDK:

```cmd
gradlew.bat assembleDebug
```

APK em `app/build/outputs/apk/debug/app-debug.apk`.

## Estrutura

```
app/src/main/
  AndroidManifest.xml
  java/com/example/nfcaudiorecorder/MainActivity.kt
  res/layout/activity_main.xml
  res/values/{strings,colors,themes}.xml
  res/drawable/ic_launcher_foreground.xml
  res/mipmap-anydpi-v26/ic_launcher*.xml
  res/xml/nfc_tech_filter.xml
.github/workflows/build.yml
```

## Limitacoes conhecidas

- Tela bloqueada: o Android nao garante entrega da tag ao app em todos os fabricantes
- Nao abre app de terceiro nem executa acao interna de outro app (exigiria deep link/API)
- Formato `.3gp` (AMR-NB): simples e compativel, qualidade baixa
- Gravacao so enquanto o app esta em primeiro plano
