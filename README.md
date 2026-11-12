# Virex Music

Native Android music app — search and stream music, trending mixes, playlists,
a queue, synced lyrics, and playback of audio files already on your phone.

Package `com.virex.music`. Kotlin + Jetpack Compose, Material 3.

## Design — "Black glass, thin lines" (our own design)

Pure black base, one glass material (white 6–10% surfaces, 1dp hairline
borders), small precise buttons (40dp glass circles, 56dp play), colour only
from the song artwork glowing behind the glass. The player shows artwork in a
circle ringed by a thin progress ring with a small red dot as the progress
head — drag the ring to seek. No pastel colours, no chunky buttons, no fake
visualizer.

## Features

- **Home** — category chips, Trending Mix, Quick picks, Recently played
- **Search** — live search with persisted recent searches
- **Player** — queue (Up Next, auto-filled with suggestions), shuffle,
  repeat off/all/one, seek, synced lyrics (LRCLIB), sleep timer, HQ toggle
- **Library** — playlists (create, delete, add/remove songs, play all)
- **Your Files** — plays audio already on the device (MediaStore + file picker)
- **Settings** — grouped so nothing gets lost: Playback, Appearance,
  Library & files, About. Includes HQ, crossfade, animation speed, pure black.

## Scope: Your Files

Your Files plays files already on your phone. **Streamed songs are never
downloaded or written to storage.** The app has no download feature for
streamed tracks.

## Data sources (no API keys)

- Search + streaming: `https://jiosavan-api2.vercel.app/`
- Lyrics: `https://lrclib.net/` (synced LRC when available, plain otherwise)

HQ (320kbps vs 160kbps) actually switches which stream URL plays.

## Build

Push to GitHub — the Actions workflow (`.github/workflows/android.yml`)
builds the debug APK on every push to `main`. Download the
`virex-music-debug-apk` artifact and install `app-debug.apk`.

Local build: `./gradlew assembleDebug` (JDK 17, Gradle 8.7).

Build notes: Kotlin 1.9.24 with Compose compiler 1.5.14 (that pairing is
required — do not set the compiler to 1.9.24), AGP 8.5.2, compileSdk 34.
