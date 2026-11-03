# Release signing

Android only installs an update if it is signed with **the same key** as the version already on the phone.
If the key is lost, you can never update the app for existing users, including on Google Play. Keep it safe.

## Your key

The key was generated for this project and delivered **only** to your computer, in
`Desktop\foodtruck\signing\`. It is never committed to Git.

| File | What it is |
|---|---|
| `ftc-release.jks` | The release keystore (PKCS12, RSA 4096, valid ~30 years) |
| `keystore.properties` | Store password, key alias and key password |
| `ftc-release.jks.base64.txt` | The keystore as text, for the GitHub secret |

Certificate: `CN=Food Truck Centre, O=Food Truck Centre, L=Sydney, ST=NSW, C=AU`
SHA-256 fingerprint: `6B:DC:8F:DF:BD:4A:E1:00:AC:CF:89:40:71:8B:8C:85:5E:79:C7:66:AE:78:18:F1:B0:03:4B:25:60:0E:09:45`

### Back it up today

1. Copy the whole `signing` folder to **two** safe places, for example a password manager (as attachments) and an encrypted USB drive or private cloud folder.
2. Don't email it, don't put it in a public place, and never add it to the GitHub repository.

## GitHub Actions secrets

Repository → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Value |
|---|---|
| `FTC_KEYSTORE_BASE64` | entire contents of `ftc-release.jks.base64.txt` |
| `FTC_KEYSTORE_PASSWORD` | `storePassword` from `keystore.properties` |
| `FTC_KEY_ALIAS` | `ftc-release` |
| `FTC_KEY_PASSWORD` | `keyPassword` from `keystore.properties` |

The workflow decodes the keystore into the runner's temporary folder for the build, then deletes it.
GitHub masks secret values in logs. If a secret is missing, the build fails rather than producing an unsigned "release".

## Building a signed APK locally (optional)

Copy `ftc-release.jks` and `keystore.properties` into the `android` folder (both are git-ignored), then run:

```
cd android
./gradlew assembleRelease
```

The signed APK is `android/app/build/outputs/apk/release/app-release.apk`.

## New versions

Increase `versionCode` (e.g. 1 → 2) and `versionName` in `android/app/build.gradle.kts`, then push.
Each push to `main` produces a new GitHub Release with both APKs.

## Google Play (later)

If you publish on Google Play, enrol in **Play App Signing** and use this key as the *upload key*.
