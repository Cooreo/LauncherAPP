# Room Launcher

**Room Launcher** is a minimalist Android home screen launcher built with **Kotlin** and **Jetpack Compose**. Instead of a traditional grid of icons, your home screen is an illustrated room where every object acts as an interactive hotspot.

## Features

- **Illustrated Room Home Screen**: Comes with a custom minimalist vector room (`bg_room_default.xml`) containing six interactive objects aligned via responsive percentage coordinates:
  - **TV** — Media panel (auto-populates YouTube, music, video & podcast apps)
  - **Shelf** — Work panel (auto-populates Obsidian, notes, email & productivity apps)
  - **Painting** — Direct System Gallery intent (`Intent.ACTION_VIEW` with `image/*`)
  - **Trash Can** — Unsorted panel containing every installed app not placed in another object
  - **Bed** — Bedtime panel (wind-down, meditation, reading & sleep tracking apps)
  - **Clock** — Direct System Alarm intent (`AlarmClock.ACTION_SHOW_ALARMS`)
- **Nothing Gets Lost**: Newly installed apps (tracked live via `BroadcastReceiver` on `ACTION_PACKAGE_ADDED` / `REMOVED` / `REPLACED`) and apps removed from any panel automatically appear in the **Trash Can**.
- **Multi-Step Onboarding & System Permissions**:
  1. Welcome & gesture walkthrough
  2. App visibility (`QUERY_ALL_PACKAGES`) with live discovered app count
  3. Battery optimization exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) + quick link to App Info (`ACTION_APPLICATION_DETAILS_SETTINGS`) for unrestricted background usage
  4. Default Home role prompt (`RoleManager.ROLE_HOME` on Android 10+ with legacy fallback)
- **Full Edit Mode**: Long-press anywhere in the room to reveal semi-transparent hotspot bounding boxes, drag to move, drag the corner handle to resize, rename objects, add/delete objects, or long-press and drag apps directly between object panels.
- **Fast A–Z Fallback Drawer**: Swipe up from the bottom or tap the persistent **All Apps** pill (or the footer inside any panel) for an alphabetical A–Z searchable app list.
- **JSON Persistence**: Room layout, hotspot coordinates, labels, actions, and app assignments are stored in `context.filesDir/room_config.json` via `kotlinx.serialization`.

## Project Structure

```
app/src/main/java/com/roomlauncher/app/
├── MainActivity.kt
├── data/
│   ├── AppInfo.kt
│   ├── AppRepository.kt
│   ├── PackageChangeReceiver.kt
│   ├── RoomConfigRepository.kt
│   └── RoomModels.kt
├── system/
│   └── SystemIntentHelper.kt
└── ui/
    ├── RoomLauncherViewModel.kt
    ├── allapps/AllAppsSheet.kt
    ├── components/AppIconView.kt
    ├── onboarding/OnboardingScreen.kt
    ├── room/
    │   ├── HotspotEditOverlay.kt
    │   ├── HotspotPanelOverlay.kt
    │   └── RoomScreen.kt
    └── theme/
```

## Building from Source

```bash
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Pre-built APKs are also stored in `/releases/RoomLauncher-v1.0.apk`.
