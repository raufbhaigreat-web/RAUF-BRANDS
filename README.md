# RAUF BRANDS

RAUF BRANDS is a native Kotlin Android companion for keeping Free Fire MAX practice settings and local profiles. It does not connect to, modify, or automatically apply settings to the game. The app requests no special permissions and stores its data in Android `SharedPreferences`.

## Features

- Sensitivity controls for General, Red Dot, 2X Scope, 4X Scope, Sniper Scope, and Free Camera (0–200).
- Device brand/model, Android version, refresh rate, preferred FPS, finger layout, six tuning profiles, and aim feedback based starting values.
- Named DPI profiles, a custom DPI value, and the device's Android smallest-width information. Saving a value here does not change system DPI.
- Fire button size controls (30–100), a live visual preview, saved size profiles, and placement tips for the game's HUD editor.
- Weapon recommendations and personal practice notes for Desert Eagle, Woodpecker, SVD, M1887, UMP, MP40, M4A1, SCAR, AK, AWM, and other weapons.
- Local setup profiles with create, rename, load, update, delete, and compare actions.
- Practice session tracking and a responsive black-and-red native interface.
- Offline arena practice with moving targets, local head/body scoring, and an adjustable touch target size. Rendering follows the device display refresh; actual frame rate depends on device capability.

## Build a debug APK

From this directory, with JDK 17 and Android SDK platform 35/build tools installed:

```sh
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Install it by transferring the APK to your Android phone, opening it, and approving installation from that file manager when Android prompts. You can also install over USB with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Android may require enabling “Install unknown apps” for the app used to open the APK.

## Build a signed release APK

Set the signing environment variables described below, then run:

```sh
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`. Keep the upload keystore private and backed up; builds without the signing variables are not suitable for distribution. The same key must be retained for future app updates.

## Build a Google Play AAB

Create and protect an upload key. Keep the keystore and passwords out of source control and back up the keystore securely:

```sh
keytool -genkeypair -v -keystore rauf-brands-upload.jks -alias rauf-brands -keyalg RSA -keysize 2048 -validity 10000
```

Set these environment variables in your shell or CI secret store, using the values for that key:

```sh
export RAUF_KEYSTORE_PATH="$PWD/rauf-brands-upload.jks"
export RAUF_KEYSTORE_PASSWORD='your-keystore-password'
export RAUF_KEY_ALIAS='rauf-brands'
export RAUF_KEY_PASSWORD='your-key-password'
```

Then build the signed release bundle:

```sh
./gradlew bundleRelease
```

AAB output: `app/build/outputs/bundle/release/app-release.aab`. If the signing variables are absent, Gradle still creates an unsigned release bundle for local inspection; Google Play requires a signed upload bundle.

## Other useful commands

```sh
./gradlew test
./gradlew lint
./gradlew assembleDebug bundleRelease
```

Minimum Android version: Android 8.0 (API 26). Target SDK: 35.
