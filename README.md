# Spotify Widget

Widget Android 3×1 pour contrôler Spotify depuis l'écran d'accueil : pochette, titre, artiste, précédent / play-pause / suivant.

Le widget lit la session média de Spotify (`MediaSessionManager`), ce qui nécessite l'accès aux notifications pour l'app. Aucun compte développeur Spotify n'est requis.

## Installation (APK déjà construit)

Si tu as déjà `spotify-widget.apk` (voir plus bas pour l'obtenir), l'installation prend quelques minutes. L'app Spotify doit être installée sur le téléphone (Android 8.0+).

1. **Copier l'APK sur le téléphone** : Drive, mail, câble USB, etc.
2. **L'ouvrir** depuis le gestionnaire de fichiers. Android demande d'autoriser « Installer des applis inconnues » pour l'app utilisée (Fichiers, Chrome, Drive…) : accepter, puis **Installer**. Play Protect peut avertir que l'app est inconnue : choisir **Installer quand même**.
   - Alternative par câble : `adb install -r spotify-widget.apk`.
3. **Accorder l'accès aux notifications** : ouvrir **Spotify Widget**, appuyer sur **Ouvrir les réglages**, activer **Spotify Widget** et accepter l'avertissement.
   - Si l'option est grisée : `Réglages > Applis > Spotify Widget > ⋮ > Autoriser les paramètres restreints`, puis réessayer.
4. **Ajouter le widget** : appui long sur l'écran d'accueil, **Widgets**, **Spotify Widget**, le poser (3×1 par défaut).
5. Lancer une lecture dans Spotify : le titre, la pochette et les boutons s'affichent.

Sans l'accès aux notifications, le widget affiche « Appuyer pour activer l'accès » et un tap ouvre directement les réglages. Pour mettre à jour, installer le nouvel APK par-dessus (même clé de signature, voir [Signature de l'APK](#signature-de-lapk)).

## Obtenir l'APK : build dans le cloud (GitHub Actions)

Aucun outil Android à installer en local : le workflow [`.github/workflows/build.yml`](.github/workflows/build.yml) compile l'APK sur GitHub.

1. Pousser le projet sur un dépôt GitHub (le dépôt peut être privé) :

   ```bash
   git add -A && git commit -m "Spotify widget"
   git remote add origin <url-du-depot>
   git push -u origin HEAD
   ```

2. Onglet **Actions** du dépôt, ouvrir le dernier run **Build APK** (ou le lancer via **Run workflow**), puis télécharger l'artefact `spotify-widget` (zip contenant `spotify-widget.apk`, APK **release** signé).
3. Dézipper l'artefact, puis suivre [Installation](#installation-apk-déjà-construit). Pour l'option `adb`, installer l'outil avec `brew install --cask android-platform-tools`.

### Signature de l'APK

Un APK release doit être signé pour être installable. Sans configuration, le workflow génère une clé temporaire à chaque run : l'APK s'installe, mais Android refusera de le mettre à jour par-dessus une version signée avec une autre clé (désinstaller d'abord).

Pour une signature stable, créer un keystore une fois (n'importe quel JDK, ou `docker run --rm -it -v "$PWD":/k eclipse-temurin:17-jdk bash -c 'cd /k && keytool ...'`) :

```bash
keytool -genkeypair -keystore release.jks -alias spotify-widget \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Spotify Widget"
base64 -i release.jks | pbcopy
```

Puis, dans le dépôt GitHub, `Settings > Secrets and variables > Actions`, ajouter :

- `KEYSTORE_BASE64` : le contenu base64 copié ci-dessus
- `KEYSTORE_PASSWORD` : le mot de passe choisi (le même pour le keystore et la clé)

Ne jamais committer `release.jks` ni son mot de passe, et le sauvegarder : le perdre empêche les mises à jour.

Les sections suivantes décrivent le build local, utile pour modifier le code et itérer vite. Les builds locaux et Docker produisent un APK **debug** (`assembleDebug`) ; l'APK final `spotify-widget.apk` vient du workflow GitHub.

## Méthode Docker

Seul Docker est requis (Docker Desktop, OrbStack ou Colima). L'image contient JDK 17, Gradle et le SDK Android.

```bash
docker build -t spotify-widget-build .
docker run --rm \
  -v "$PWD":/project \
  -v spotify-widget-gradle-cache:/root/.gradle \
  spotify-widget-build
```

L'APK est produit dans `app/build/outputs/apk/debug/app-debug.apk`, puis installé comme décrit dans [Déploiement](#déploiement-sur-un-téléphone) ou en l'envoyant au téléphone.

- Le premier `docker build` télécharge le SDK (plusieurs Go) ; les builds suivants sont rapides grâce au volume de cache Gradle.
- L'image est forcée en `linux/amd64` car `aapt2` n'existe pas pour Linux ARM. Sur un Mac Apple Silicon, elle tourne donc en émulation (plus lente, mais fonctionnelle).

## Prérequis (build local)

- Android Studio (Ladybug ou plus récent) avec un JDK 17+
- Un téléphone Android 8.0+ (minSdk 26) ou un émulateur
- L'app Spotify installée sur l'appareil

### Installation des prérequis (macOS, Homebrew)

```bash
brew install --cask android-studio
brew install --cask android-platform-tools   # adb
brew install openjdk@17                      # JDK pour la ligne de commande
brew install gradle                          # uniquement pour générer le wrapper
```

1. Lancer Android Studio une première fois et suivre l'assistant : il installe le SDK dans `~/Library/Android/sdk`.
2. Dans `Settings > Languages & Frameworks > Android SDK`, vérifier que **Android 15 (API 35)** et les **Android SDK Build-Tools** sont installés.
3. Pour la ligne de commande, ajouter à `~/.zshrc` :

   ```bash
   export ANDROID_HOME="$HOME/Library/Android/sdk"
   export PATH="$PATH:$ANDROID_HOME/platform-tools"
   export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
   ```

   Si `java_home` ne trouve pas le JDK 17 installé par Homebrew : `sudo ln -sfn "$(brew --prefix openjdk@17)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-17.jdk`.

4. Vérifier :

   ```bash
   java -version     # 17.x
   adb version
   ```

Ne pas utiliser un JDK trop récent (ex. 27) avec ce projet : Gradle/AGP ne le supportent pas forcément. Android Studio embarque son propre JDK, donc rien à configurer si tu build depuis l'IDE.

## Build

### Avec Android Studio (recommandé)

1. `File > Open` et choisir ce dossier.
2. Laisser la synchro Gradle se terminer (elle télécharge le SDK et génère le wrapper).
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

### En ligne de commande

Le wrapper Gradle n'est pas versionné. Le générer une fois (nécessite Gradle installé, par exemple `brew install gradle`) :

```bash
gradle wrapper
```

Puis, avec `ANDROID_HOME` pointant vers le SDK (ou `sdk.dir` dans `local.properties`) :

```bash
./gradlew assembleDebug
```

L'APK est produit dans `app/build/outputs/apk/debug/app-debug.apk`.

## Déploiement sur un téléphone

1. Activer les options développeur et le débogage USB sur le téléphone, puis le brancher.
2. Vérifier qu'il est détecté :

   ```bash
   adb devices
   ```

3. Installer l'APK :

   ```bash
   ./gradlew installDebug
   ```

   ou, avec un APK déjà construit :

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

Depuis Android Studio, le bouton Run fait la même chose.

Après un déploiement de développement, la configuration sur le téléphone est la même que pour l'[Installation](#installation-apk-déjà-construit) (étapes 3 à 5).

## Dépannage

- **« Rien en lecture »** : lancer une lecture dans Spotify ; le widget se met à jour via callback.
- **Le widget ne se met plus à jour** : désactiver puis réactiver l'accès aux notifications, ou redémarrer l'app Spotify Widget. Certains constructeurs tuent les services en arrière-plan : exclure l'app de l'optimisation de batterie.
- **Les boutons ne contrôlent pas Spotify** : sans session Spotify active, un événement média global est envoyé et peut cibler une autre app.

## Structure

| Fichier | Rôle |
| --- | --- |
| `SpotifyWidgetProvider.kt` | Construit le widget et traite les clics |
| `SpotifyController.kt` | Trouve la session Spotify et envoie les commandes |
| `SpotifyNotificationListener.kt` | Suit la session et rafraîchit le widget |
| `SetupActivity.kt` | Écran de départ vers les réglages d'accès |
