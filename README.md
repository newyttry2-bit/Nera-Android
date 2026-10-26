# Virex Music — native Android scaffold

Initial native Android version of **Virex Music v4.0**, mirroring the web app in
`the web app repo` (`index.html`, 127 KB). Jetpack Compose, Kotlin,
Gradle. `minSdk 24`, `targetSdk 34`, `compileSdk 34`, version `4.0`.

> **Status: scaffold, not a finished release.** The project structure,
> screens, data layer and GitHub Actions workflow are in place. It has
> **not** been compiled locally (see "Build" below).

## Screens (mirroring the site)

- **Home** — "Trending Mix" title, category chips (Trending / Chill /
  Party), song cards from the same search endpoint the site uses.
  Black `#000000` / `#121212` theme, green accent `#1DB954`.
- **Search** — search field, recent searches, results list.
- **Playlists** — create playlists, add songs from Search, stored
  locally with Room (the web app used localStorage).
- **Downloads** — see the scope note below.
- **About / Guide** — the site's About text and How It Works steps,
  adapted for Android.
- **Player** — mini-player above the bottom nav, plus a full player
  with artwork, seek bar, queue, shuffle, loop, HQ toggle placeholder
  and lyrics (LRCLIB, same as the site).

## APIs used (no keys)

- Search / suggestions:
  `https://jiosavan-api2.vercel.app/api/search/songs?query=…&limit=20`
  and `…/api/songs/{id}/suggestions?limit=20` — **for search and
  playback previews only**.
- Lyrics: `https://lrclib.net/api/get` and `https://lrclib.net/api/search`.

No API keys are invented, added or required. No signing config and no
secrets are committed.

## Downloads — scope limit (important)

**This Android app does NOT download tracks from any streaming service,
and it never writes streamed audio to device storage.**

The Downloads tab only:
1. Lists audio already on the device via MediaStore, and
2. Lets the user pick a local audio file via the Storage Access
   Framework (SAF) for offline playback of user-owned local files.

That is a deliberate difference from the web PWA, whose download
feature is not replicated here. Playback of search results is transient
preview streaming through `MediaPlayer` only.

## Branding

- App icon / header logo: `app/src/main/res/drawable/logo.xml`
  (vector placeholder in the site's black/green branding; replace it
  with the 512×512 PNG converted from the site's `logo.png` for a
  pixel-identical icon — the PNG is not pushed here because the
  GitHub file API used for this push handles text files only).
- Font: the site loads Outfit from Google Fonts. This scaffold uses
  the system sans-serif stack with the same weights so no font file
  needs bundling; drop a bundled Outfit font into `res/font/` for a
  pixel-identical look.

## Implemented vs stubbed

Implemented:
- Gradle project (root + app, version catalog), AndroidManifest,
  MainActivity with bottom navigation, all five tabs, mini + full player.
- Room playlists (create / list / add / play).
- MediaStore + SAF local-file playback.
- Retrofit search + lyrics clients, Coil artwork loading.
- GitHub Actions workflow (below).

Stubbed / not yet implemented:
- Real-time audio visualizer and crossfade (web app features; the
  player notes this in-app). HQ toggle is a preference placeholder —
  stream quality is chosen by the API response.
- Dynamic Radio / infinite queue from seed tracks (the site's
  suggestions endpoint client exists, but auto-queueing is not wired).
- Bottom-nav icons are single-letter placeholders — replace with
  Material icons / vector drawables.
- No release signing, no Play Store listing, no foreground-service
  notification for background playback (permission is declared, but
  playback currently lives in the ViewModel).

## Build

### GitHub Actions (the build path)

`.github/workflows/android.yml` runs on push to `main` (and on PRs /
manual dispatch):

1. Checkout
2. Set up JDK 17 (Temurin)
3. Set up Gradle
4. `./gradlew assembleDebug`
5. Upload `app/build/outputs/apk/debug/app-debug.apk` as the
   `virex-music-debug-apk` artifact

Download the APK from the workflow run's Artifacts. A debug APK needs
no signing key. A release/signed build would need a keystore and
GitHub secrets — deliberately not included here.

### Locally (Android Studio, recommended)

Open this folder in Android Studio, let it sync Gradle, then
Run. If you generate the official wrapper there (`gradle wrapper`),
replace the minimal `gradlew` in this scaffold with it — see the
comment at the top of `gradlew`.

### Why no local build was verified

The machine this scaffold was built on has **no Java / Android SDK
installed**, so `./gradlew assembleDebug` cannot run there. The
GitHub workflow above is the intended verification path. Expect the
first workflow run to surface the usual first-build issues (AGP /
Compose / Room-KSP version alignment); fix them in the workflow run
logs rather than assuming a clean first build.

## Project layout

```
settings.gradle.kts, build.gradle.kts, gradle.properties
gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties
gradlew, gradlew.bat
.github/workflows/android.yml
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/virex/music/
  MainActivity.kt
  data/Models.kt, Api.kt, AppDatabase.kt
  player/PlayerViewModel.kt
  ui/theme/Theme.kt
  ui/screens/{Common,HomeScreen,SearchScreen,PlaylistsScreen,
              DownloadsScreen,PlayerScreen,AboutScreen}.kt
app/src/main/res/{values,drawable,mipmap-anydpi-v26}/
```

## Not pushed to GitHub

Nothing in this folder has been pushed. The parent agent owns the
decision to create the Android repo and push this
scaffold (and to run the workflow) — that step is intentionally
left for the user to approve.
