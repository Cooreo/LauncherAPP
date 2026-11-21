# Room Launcher

**Room Launcher** is a minimalist Android home screen launcher built with **Kotlin** and **Jetpack Compose**. Instead of a traditional grid of icons, your home screen is a customizable, illustrated room where every piece of furniture is an interactive, movable object.

## Features (v2.6)

- **Geometry Dash Room Theme & 3 Extreme Demon Sub-Themes (v2.6)**:
  - New **Geometry Dash** Room Style featuring real bundled level artwork, neon perspective GD grid floor, laser ground line, decorative spikes & sawblades, glowing wall posters, matching Picture Frame art, Home Wallpaper Mirror support, and neon GD charging ring.
  - 3 full sub-themes selectable with live artwork cards in Settings:
    - **Sonic Wave** (*Cyclic*) — Electric cyan Nine Circles wave stripes, cobalt saws, cyan wave gameplay & SONIC WAVE poster art.
    - **Astral Divinity** (*Knobbelboy*) — Cosmic violet nebula, rainbow prism beams, golden stars, Clubstep monster & crystal corridor art.
    - **Death Corridor** (*KaotikJumper*) — Pitch-black & crimson hell glow, iconic diamond wave corridor, red hell saws & blazing spike corridor art.
- **Bulletproof In-App Updater & Package Conflict Protection (v2.6)**:
  - Upgrades installer to Android's native `PackageInstaller.Session` API with structured status callbacks (`UpdateInstallReceiver`), pre-install APK signature & version verification, and verified signing-key display in Settings.
  - Detects signing-key conflicts before Android fails and walks through a 1-tap guided migration (**Export Room Backup** → **Save APK to Downloads** → **Uninstall Old Build**), plus an automated CI signing-certificate verification guard so every release shares the exact same persistent key (`app/roomlauncher-debug.keystore`) for seamless in-place updates.
- **Persistent In-Place Update Signing & Hardened Saves (v2.5.1)**:
  - Uses a permanent project signing keystore (`app/roomlauncher-debug.keystore`) instead of ephemeral per-build CI debug keys so every future update upgrades cleanly in-place without uninstalling or losing saved data.
  - Hardened `room_config.json` persistence with serialized `Mutex` writes, `fsync` + rolling `.bak` crash recovery, `coerceInputValues = true` forward/backward enum safety, and 1-tap **Export / Restore Backup** right in Settings.
- **In-App GitHub Auto-Updates**:
  - Automatically checks **`Cooreo/LauncherAPP`** (both GitHub Releases and the repository's `releases/` folder on `main`) for newer APKs.
  - Displays an **Update Available popup** right inside Settings with live download progress and native Android APK installation via `FileProvider`, plus a manual **Check for Updates** button and Auto-Check toggle.
- **Portrait-Locked Home Screen**: Locks `MainActivity` to portrait orientation so accidental phone rotation never squashes the illustrated room layout or wallpaper alignment.
- **Live Music Visualizer & Synced Lyrics (LRCLIB)**:
  - Automatically detects playing music from Spotify, YouTube Music, Apple Music, SoundCloud, and local players via Android `MediaSessionManager` (with Notification Access).
  - Floating **Now-Playing pill** shows album art, live mini equalizer, scrolling song title, artist, exact elapsed/total progress bar, and Play/Pause without cluttering the room when idle.
  - Tap to expand into the interactive **Visualizer ↔ Lyrics** stage: switch between **3 animated visualizer styles** (**Spectrum Bars**, **Sound Wave**, **Vinyl Groove**, tinted to album art with optional real-time FFT audio reactivity) and **real-time time-synced `.lrc` lyrics** fetched from the free open-source **LRCLIB** library (tap any synced line to seek directly to that moment!).
- **Clean Panel Icons (v2.3.1)**: App icon & Sticky Note shortcut delete (`×`) buttons stay hidden until you tap the panel Edit (`✏️`) toggle.

- **Museum-Quality Picture Frame**: Automatic EXIF photo orientation correction, symmetrical wooden beveled frame, warm gallery cream mat, center-cropped framing, and subtle glass reflection sheen (both in-room and on mirrored Home wallpaper).
- **Dynamic Time-of-Day Themes**: Optional automatic schedule shifting your room from **Warm Linen** (daytime, 6 AM) → **Midnight Cozy** (3 PM afternoon/evening) → **OLED Pitch** (9 PM night).
- **6 Selectable Charging Animations (Screen On)**: Choose between **Match Room Style**, **Liquid Orb** (animated sloshing liquid & bubbles), **Aurora Ribbons**, **Cyber Plasma**, **Cosmic Orbit**, and **Redstone Voxel**, held on screen ~5.5s (tap to dismiss) alongside a clean clickable `⚡ %` charging badge (no bottom bar).
- **Atmospheric Temperature Window Glass**: Live local temperature visually transforms the window glass—**fogged condensation & water droplets** below 10°C, **crystalline frost branches & frozen ice glaze** below 0°C, and warm sunlit shimmer on hot days.
- **True Edge-to-Edge Full-Screen Room & Lighting**: Extends seamlessly behind camera cutouts, notches, status bar, and gesture navigation bar across every Room Style and mirrored Home wallpaper.
- **4 Architectural Room Styles**:
  - **Cozy Minimalist** — Clean wood panels, warm floorboards, woven rug, and modern furniture
  - **Teen Room** — RGB LED ceiling strip, string fairy lights, dark rug, and **two customizable wall posters** you can replace with your own gallery images
  - **Kids Room** — Colorful pennant bunting flags, starry sky wallpaper, polka-dot play rug, toy-cubby shelf, and cozy kids bed
  - **Blocky / Voxel** — Pixelated oak planks & cobblestone blocks, glowing wall torches, checkered wool rug, and full blocky furniture (oak door window, oak crafting shelf, classic red voxel bed, cauldron trash bin!)
- **6 Light & Dark Color Palettes**: Warm Linen, Botanical Sage, Midnight Cozy, Forest Night, Warm Ember, and OLED Pitch Black—combinable with any Room Style.
- **Movable Illustrated Room Objects**: Drag and resize the actual furniture anywhere in the room:
  - **Window** — Live local weather sky & temperature (Open-Meteo) → opens your Weather app
  - **Painting** — Minimalist art or your custom photo → opens Gallery/Photos directly
  - **Clock** — Live ticking analog hands → opens System Alarms directly
  - **Sticky Note** — Pin custom **Web Shortcuts** (URLs) alongside browser & PWA apps
  - **TV** — Media panel with centered camera zoom right into the TV bezel + CRT/OLED power-on animation
  - **Game Console** — Cabinet under the TV storing installed games (`CATEGORY_GAME`)
  - **Shelf** — Work, notes & email panel with a **two-stage hardcover book pop-out & open** animation
  - **Bed** — Wind-down panel for sleep, meditation, health/fitness, and clock apps
  - **Trash Can** — Unsorted panel rising out of the bin containing every installed app not assigned elsewhere
- **Silky-Smooth A–Z App Deck + Animated Letter Search**: Smooth slide-up/down drawer transitions with 0% background Room overdraw once settled, wave-magnifying A–Z rail, finger-following animated letter bubble with app counts, section highlight pulse, bold search match highlighting, swipe-down-to-close, and Settings at the bottom.
- **1:1 Home Wallpaper Mirror**: Compensates for Android's display insets and SystemUI wallpaper overscan so the Home screen wallpaper matches your room scale (`WallpaperManager.FLAG_SYSTEM` only—Lock screen untouched).
- **Nothing Gets Lost**: Live `BroadcastReceiver` (`ACTION_PACKAGE_ADDED` / `REMOVED` / `REPLACED`) guarantees newly installed or removed apps always appear in the Trash Can.

## Building from Source

```bash
./gradlew testDebugUnitTest assembleRelease
```

Pre-built APKs are stored in `/releases/` (`RoomLauncher-v2.6.apk`, `RoomLauncher-v2.5.1.apk`, `RoomLauncher-v2.5.apk`, `RoomLauncher-v2.4.apk`, `RoomLauncher-v2.3.1.apk`, `RoomLauncher-v2.3.apk`, `RoomLauncher-v2.2.apk`, `RoomLauncher-v2.1.apk`, `RoomLauncher-v2.0.apk`, `RoomLauncher-v1.0.apk`).
