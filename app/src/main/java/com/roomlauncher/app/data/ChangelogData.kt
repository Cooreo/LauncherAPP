package com.roomlauncher.app.data

import androidx.compose.runtime.Immutable

@Immutable
data class ChangelogEntry(
    val version: String,
    val title: String,
    val tag: String,
    val highlights: List<String>
)

object ChangelogData {
    const val CURRENT_VERSION = "3.0.1"

    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "3.0.1",
            title = "Smooth All Apps, Fast Settings & Play Protect Cleanup",
            tag = "BUGFIX",
            highlights = listOf(
                "Fixed All Apps drawer lag and the glitchy exit animation: removed competing close transforms, detached keyboard padding from the outer sheet, dropped 27 idle A–Z rail spring animations, and cut extra background room overdraw.",
                "Rebuilt Settings on a virtualized LazyColumn with a 1-tap Quick-Jump category strip, clean collapsible sections, and moved App Updates right to the top.",
                "Removed high-scrutiny RECORD_AUDIO and REQUEST_DELETE_PACKAGES permissions to reduce Google Play Protect warnings while keeping the smooth Music Visualizer, Synced Lyrics, and seamless in-place updates intact."
            )
        ),
        ChangelogEntry(
            version = "3.0",
            title = "The Multi-Room House Update",
            tag = "MAJOR RELEASE",
            highlights = listOf(
                "Swipe left & right seamlessly across multiple rooms just like normal phone pages: Bedroom, Living Room, Kitchen & Gaming Room!",
                "12 brand-new custom-illustrated furniture pieces: Lounge Sofa, Fireplace, Vinyl Hi-Fi, Coffee Table, Fridge, Espresso Maker, Stove, Kitchen Counter, Gaming PC, Arcade Cabinet, Streaming Desk & Trophy Shelf.",
                "New smart app categories across your whole house (Social, Food & Delivery, Shopping, Health & Fitness, Voice & Streams, Tools & Cloud, News & Reading) — without touching your existing Bedroom setup.",
                "Floating Room Switcher bar at the bottom to see your current room or jump to any room in 1 tap, plus Room Management & furniture picker in Edit Mode and Settings.",
                "Built-in What's New Changelog screen right inside Settings (and automatic release summary after updates)."
            )
        ),
        ChangelogEntry(
            version = "2.6.1",
            title = "Silky Predictive Back & Gesture Fix",
            tag = "BUGFIX",
            highlights = listOf(
                "Added native Android 14/15 Predictive Back finger-tracking animations for Settings and All Apps.",
                "Eliminated the blank flash when dismissing Settings or All Apps by keeping the Room mounted cleanly underneath.",
                "Home button / swipe-up-to-Home now smoothly returns directly to your Room from any open sheet or panel."
            )
        ),
        ChangelogEntry(
            version = "2.6",
            title = "Geometry Dash Theme & Updater Overhaul",
            tag = "FEATURE",
            highlights = listOf(
                "Added Geometry Dash Room Style with Sonic Wave, Astral Divinity, and Death Corridor sub-themes, bundled level art, neon perspective floor grid, spikes & sawblades.",
                "Upgraded in-app updater to Android's native PackageInstaller.Session API with pre-install signature verification and guided conflict recovery."
            )
        ),
        ChangelogEntry(
            version = "2.5.1",
            title = "Persistent Update Signing & Backup",
            tag = "BUGFIX",
            highlights = listOf(
                "Switched to a permanent signing keystore so all updates install in-place without losing saved layouts or settings.",
                "Hardened config persistence with atomic writes, rolling .bak crash recovery, and 1-tap Export / Restore Backup in Settings."
            )
        ),
        ChangelogEntry(
            version = "2.5",
            title = "In-App GitHub Auto-Updates",
            tag = "FEATURE",
            highlights = listOf(
                "Added automatic & manual update checks against Cooreo/LauncherAPP with in-app APK download progress and 1-tap install.",
                "Locked launcher orientation to portrait for crisp, consistent room proportions."
            )
        ),
        ChangelogEntry(
            version = "2.4",
            title = "Live Music Visualizer & Synced Lyrics",
            tag = "FEATURE",
            highlights = listOf(
                "Added floating Now-Playing music pill with animated spectrum visualizer styles and real-time open-source LRCLIB synced lyrics."
            )
        ),
        ChangelogEntry(
            version = "2.3",
            title = "Dynamic Themes, Charging Animations & Wallpaper Mirror",
            tag = "FEATURE",
            highlights = listOf(
                "Added Dynamic Time-of-Day themes (Warm Linen → Midnight Cozy → OLED Pitch), 6 full-screen charging animations, Home Screen Wallpaper Mirroring, and EXIF-corrected Picture Frame & Teen Room posters."
            )
        ),
        ChangelogEntry(
            version = "2.0",
            title = "Room Styles & Movable Furniture",
            tag = "MAJOR RELEASE",
            highlights = listOf(
                "Introduced Cozy Minimalist, Teen Room, Kids Room & Blocky/Voxel room styles, full drag-and-resize Edit Mode, Live Weather Window, and A–Z Searchable All Apps drawer."
            )
        )
    )
}
