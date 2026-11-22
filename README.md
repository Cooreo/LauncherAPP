# Room Launcher

**Room Launcher** is a minimalist Android home screen launcher built with **Kotlin** and **Jetpack Compose**. Instead of a traditional grid of icons, your home screen is a customizable, illustrated room where every piece of furniture is an interactive, movable object.

## Features (v3.1.4 — Larger App Drawer DPI & Simple % Scale Setting)

- **Larger App Drawer Scale (DPI) & Percentage Setting (v3.1.4)**:
  - Increased the default App Drawer DPI scale to **115%** (scoped strictly to the App Drawer so your home screen & navigation bar insets stay at native 100% DPI), backed by sharper `180px` cached icons and adaptive `5 / 4 / 3` grid columns.
  - Added **Settings → App Drawer → Drawer Size (DPI Scale)** in plain **`%`** (`75%`–`140%` in `5%` increments, 1-tap `85% / 100% / 115% / 130%` presets, and a live visual icon preview) so adjusting drawer size is effortless and never confusing.
- **Restored A–Z Sorting Headers & Swipe-Down-To-Close Everywhere (v3.1.3)**:
  - Restored visible A–Z alphabetical section headers in Grid View (alongside List View and A–Z / Newest / Recent sorting) while keeping uniform `30dp` label boxes and crisp `46–50dp` aligned icons.
  - Added full-drawer nested-scroll swipe-down-to-close: pulling down anywhere on the App Grid or List when at the top smoothly slides the drawer closed with velocity flick support, and swiping down on the header, search bar, or filter chips closes it immediately from any scroll depth.
- **Native DPI, Aligned App Grid & Bottom Pill Fix (v3.1.2)**:
  - Restored 100% native system DPI (removed the global `LocalDensity` override that shrunk UI too far) while keeping a gentle `0.93×` room breathing margin.
  - Fixed the bottom **All Apps** button clipping on 3-button & gesture navigation bars by applying `navigationBarsPadding()` outside the unconstrained bottom bar container.
  - Fixed scattered/uneven apps in the All Apps Grid by removing mid-grid A–Z header breaks (continuous 4-column rows with uniform `30dp` label height and crisp `46–50dp` icons, while keeping the A–Z fast-scroll rail working in both Grid and List views).
- **Adaptive High-DPI Scaling & Room Perspective Polish (v3.1.1)**:
  - Automatically normalizes oversized OEM display densities (such as Oppo Reno 6 5G / ColorOS running `1080×2400` at `480dpi / 3.0x = 360dp`) to a spacious `412dp` logical reference width while retaining native `1080×2400` pixel sharpness.
  - Gently pulled back room furniture perspective (`0.88×` horizon-anchored across View Mode, Edit Mode & Wallpaper Mirror) for clean breathing room away from screen bezels.
  - Rescaled the redesigned All Apps drawer with adaptive `40–46dp` icons backed by crisp `144px` cached bitmaps, a compact `44dp` pill search bar, tighter header & action modal sizing, and a slimmer `22dp` A–Z fast-scroll rail.
- **Redesigned All Apps Drawer with 1-Tap Uninstall & Pinning (v3.1.0)**:
  - **4-Column Icon Grid & List View Toggle** (persisted), **Pinned Favorites dock strip**, interactive **Filter Chips** (*All*, *Pinned*, *Recent*, *Games*, *Unsorted*), **Sort Mode Switcher** (*A–Z*, *Newest*, *Recent*), **Enter-to-Launch Search**, A–Z Fast-Scroll Alphabet Rail working across both views, and a rich **Long-Press App Actions Modal** with **Open**, **Pin / Unpin**, **App Info**, **1-Tap Uninstall** (`REQUEST_DELETE_PACKAGES`), and **Room Object Assignment**.
- **Boot-Cached & Pre-Warmed Instant Performance (v3.1.0)**:
  - Loads all installed apps and crisp `96px` rounded icons **once on launcher boot**, keeps the All Apps sheet persistently warm off-screen (moving its layout bounds below viewport when closed so there is **zero** mount/unmount hitch and zero touch leakage), and updates incrementally in milliseconds **only** when apps are installed or uninstalled (`PackageChangeReceiver`).
- **Interactive Visual Onboarding (v3.1.0)**:
  - Upgraded the 4-step setup with a **Live Miniature Illustrated Room Preview**, interactive **Room Style & Theme Palette pickers**, **Live Window Weather toggle**, boot-cache readiness status, and Oppo/ColorOS battery & Default Home setup.
- **Bold, Readable Window Temperature Badge (v3.1.0)**:
  - Enlarged the room window temperature pill (`13.5sp`–`15sp` bold high-contrast frosted glass pill with weather glyph & `°C` unit) and mirrored it onto Home Screen wallpapers.
- **Glitch-Free App Drawer Open & Swipe-Close (v3.0.2)**:
  - Replaced frame-by-frame Predictive Back `Surface` shape/shadow thrashing and `AnimatedVisibility` alpha bleed-through with a pure-GPU 100% opaque vertical slide (`translationY` only, 0 recompositions during drag/close), 1:1 finger-following swipe-down with flick detection, pointer-blocking during exit, and clean `BackHandler` dispatch compatible with ColorOS / Oppo gesture navigation.
- **Buttery-Smooth All Apps Drawer & Zero-Glitch Close (v3.0.1)**:
  - Removed competing close transforms (`slideOutVertically` vs Predictive Back), moved IME padding onto the `LazyColumn` list bottom so dismissing the keyboard never jumps the sheet, eliminated 27 idle A–Z rail spring animations and per-header animations, and cut extra background room overdraw (`beyondViewportPageCount = 0`).
- **Fast Virtualized Settings & Updates at the Top (v3.0.1)**:
  - Rebuilt Settings on `LazyColumn` with a 1-tap **Quick-Jump category chip bar**, clean **collapsible sections** with live status summaries, and moved **App Updates** right to the top.
- **Play Protect Permission Cleanup (v3.0.1)**:
  - Removed `RECORD_AUDIO` and `REQUEST_DELETE_PACKAGES` from the manifest (using standard App Info if manual uninstall is ever needed and keeping the smooth procedural Music Visualizer + LRCLIB Synced Lyrics working with zero microphone access).
- **Swipeable Multi-Room House (v3.0)**:
  - Swipe left and right on your home screen just like a classic multi-page Android launcher (`HorizontalPager`) to walk seamlessly between your rooms: **Bedroom**, **Living Room**, **Kitchen**, and **Gaming Room** (plus add, rename, or remove custom rooms in **Settings → House & Rooms**).
  - Sleek floating **Room Switcher indicator pill** with quick-jump dots, non-blocking 2-finger pinch-zoom, and native Back/Home return to your main room.
- **12 Brand-New Custom-Drawn Furniture Objects & App Categories (v3.0)**:
  - **Living Room**: **Sofa** (Social & Chat), **Fireplace** (News & Reading), **Vinyl Player** (Music & Podcasts), **Coffee Table** (Shopping & Deals), plus Living Room wall wainscoting & spotlights.
  - **Kitchen**: **Fridge** (Food & Delivery), **Coffee Maker** (Daily Routines & Habits), **Stove** (Maps, Transit & Travel), **Kitchen Counter** (Finance, Banking & Utilities), plus ceramic subway tile backsplash & checkered tiles.
  - **Gaming Room**: **Gaming PC** (Launchers, Cloud Gaming & Mods), **Arcade Cabinet** (Arcade & Indie Games), **Streaming Desk** (Discord, Twitch & Voice Chat), **Trophy Shelf** (Esports, Stats & Guides), plus RGB hexagonal wall panels & neon wash.
  - Every new furniture piece works across all Room Styles (including Blocky / Voxel mode!) and can be added or swapped onto any room in **Edit Mode**.
  - Automatic lossless migration from v1/v2 preserves your Bedroom layout & apps while smartly populating your new rooms.
- **Built-In In-App Changelog (v3.0)**:
  - Automatically greets you once after updating to v3.0 with a sleek **What's New** showcase, and stays accessible anytime via **Settings → What's New** or **View Changelog** with full release history.
- **Silky Predictive Back Gesture & Hardware Back Button Fix (v2.6.1)**:
  - Enabled native Android 14/15 Predictive Back (`android:enableOnBackInvokedCallback="true"`) with live finger-tracking `PredictiveBackHandler` inside **Settings** and **All Apps** (smooth scale, rounded-corner peek, swipe-edge glide, clean cancel spring-back, and automatic keyboard dismissal).
  - Removed the background room unmount flash so the Room is immediately visible underneath with zero stutter on Back, and wired `HOME` intent (`onNewIntent`) to cleanly return to the Home Room from any open sheet or panel.
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

Pre-built APKs are stored in `/releases/` (`RoomLauncher-v2.6.1.apk`, `RoomLauncher-v2.6.apk`, `RoomLauncher-v2.5.1.apk`, `RoomLauncher-v2.5.apk`, `RoomLauncher-v2.4.apk`, `RoomLauncher-v2.3.1.apk`, `RoomLauncher-v2.3.apk`, `RoomLauncher-v2.2.apk`, `RoomLauncher-v2.1.apk`, `RoomLauncher-v2.0.apk`, `RoomLauncher-v1.0.apk`).
