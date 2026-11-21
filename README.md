# Room Launcher

**Room Launcher** is a minimalist Android home screen launcher built with **Kotlin** and **Jetpack Compose**. Instead of a traditional grid of icons, your home screen is a customizable, illustrated room where every piece of furniture is an interactive, movable object.

## Features (v2.0)

- **Movable Illustrated Room Objects**: Drag and resize the actual furniture anywhere in the room (not just invisible hitboxes!):
  - **Window** — Displays live local weather sky & temperature (via Open-Meteo) and taps to open your Weather app
  - **Painting** — Displays minimalist sunset art or your chosen custom photo and opens your Gallery/Photos app directly
  - **Clock** — Live ticking analog hands; taps directly to System Alarms (`AlarmClock.ACTION_SHOW_ALARMS`)
  - **Sticky Note** — Pin custom **Web Shortcuts** (URLs) alongside browser & PWA shortcut apps
  - **TV** — Media panel (YouTube, music, video & podcasts) with camera zoom + CRT/OLED power-on animation
  - **Game Console** — Cabinet under the TV storing installed games (`CATEGORY_GAME` auto-population)
  - **Shelf** — Work, notes & email panel opening like a **two-page book spread**
  - **Bed** — Wind-down panel for sleep, meditation, **health/fitness**, and **clock** apps
  - **Trash Can** — Unsorted panel rising out of the bin containing every installed app not assigned elsewhere
- **6 Light & Dark Themes**: Warm Linen, Botanical Sage, Midnight Cozy, Forest Night, Warm Ember, and OLED Pitch Black—dynamically styling both the room artwork and UI.
- **Rich vs Quick Animations**: Toggle between custom per-object animations and instant spring popups in Settings.
- **Mirror Room to Home Wallpaper**: Renders your live room layout & theme to Android's Home screen wallpaper (`WallpaperManager.FLAG_SYSTEM`), leaving your Lock screen untouched.
- **Fast A–Z App Deck + Settings**: Smooth 60/120fps scrolling, touch-and-drag A–Z side letter fast scroller with floating indicator bubble, and a dedicated **Settings** panel right at the bottom of the App Deck.
- **Nothing Gets Lost**: Live `BroadcastReceiver` (`ACTION_PACKAGE_ADDED` / `REMOVED` / `REPLACED`) guarantees newly installed or removed apps always appear in the Trash Can.

## Building from Source

```bash
./gradlew testDebugUnitTest assembleDebug
```

Pre-built APKs are stored in `/releases/` (`RoomLauncher-v2.0.apk`).
