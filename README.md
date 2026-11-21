# Room Launcher

**Room Launcher** is a minimalist Android home screen launcher built with **Kotlin** and **Jetpack Compose**. Instead of a traditional grid of icons, your home screen is a customizable, illustrated room where every piece of furniture is an interactive, movable object.

## Features (v2.2)

- **True Edge-to-Edge Full-Screen Room & Lighting**: Extends seamlessly behind camera cutouts, notches, status bar, and gesture navigation bar across every Room Style and mirrored Home wallpaper.
- **Custom Room-Styled Charging Animation (Screen On)**: Plugging in your charger triggers a custom animated energy splash & subtle ambient charging glow tailored to your active Room Style (Cozy warm amber lamp ripples, Teen Room RGB neon pulse, Kids twinkling stars, or Minecraft Redstone & XP voxel bar), with a Settings toggle and live preview button.
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

Pre-built APKs are stored in `/releases/` (`RoomLauncher-v2.2.apk`, `RoomLauncher-v2.1.apk`, `RoomLauncher-v2.0.apk`, `RoomLauncher-v1.0.apk`).
