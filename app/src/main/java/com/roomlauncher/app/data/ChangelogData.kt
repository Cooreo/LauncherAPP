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
    const val CURRENT_VERSION = "3.2.4"

    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "3.2.4",
            title = "Night Table Phone That Reacts to Calls & Tidier Bedroom",
            tag = "MINOR UPDATE",
            highlights = listOf(
                "New Night Table beside the bed with a bedside lamp and a little phone that holds all your communicator apps — dialer, SMS, WhatsApp, Telegram, Signal, Discord, Messenger, Viber, WeChat and more are auto-assigned to it so one tap opens your calls & chats.",
                "The Night Table phone now reacts to real calls: it lights up, vibrates/rocks on its stand and shows big answer & decline buttons while ringing, then switches to a calm glowing in-call screen. Enable it once with the phone-state permission (no call content or numbers are ever read).",
                "The Trash Can is now much smaller and tucked into the bedroom corner, so the room feels calmer and the bed and night table get the space they deserve — plus a one-time popup explains the Bedroom furniture update, and every manual layout rebuild now shows a clear confirmation first."
            )
        ),
        ChangelogEntry(
            version = "3.2.3",
            title = "Deep Sapphire Dark Blue Theme & Mini Diva Figurine",
            tag = "MINOR UPDATE",
            highlights = listOf(
                "Added the brand-new Deep Sapphire palette — a rich midnight-blue room with sapphire glass furniture, icy cyan lamp glow, navy velvet bedding and glowing blue accents across every room style, animated Home Wallpaper Mirror and charging animation.",
                "Girly Room now has four vibes: Blush Rose, Baby Blue (light), Lavender Night and the new Deep Sapphire dark blue — switch instantly with the 2×2 Vibe quick-switcher in Settings → Room Style.",
                "Added a collectible mini diva figurine — an original chibi idol collectible with glowing teal twin-tails, a cyan headset, sailor-collar top and pleated skirt on a glass display base — standing on the shelf (and mirrored in the Home Wallpaper) whenever a blue mood (Baby Blue or Deep Sapphire) is active."
            )
        ),
        ChangelogEntry(
            version = "3.2.2",
            title = "Girly Room Baby Blue Theme & Fixed Room Light Sources",
            tag = "MINOR UPDATE",
            highlights = listOf(
                "Added the brand-new Baby Blue (Pastel Sky) theme for Girly Room — powder-blue Parisian stripes, satin blue ribbon bows, sheer sky curtains, baby-blue tufted velvet headboard & heart pillow, blue Polaroid prints, matching sky-blue charging animation & Home Wallpaper Mirror, plus 1-tap Blush / Baby Blue / Lavender switching in Settings → Room Style.",
                "Fixed room light sources that were missing real emitted lights: added glowing recessed ceiling spotlight housings, lenses & downward light beams in the Living Room; lit filament bulbs & warm downward light cones under Kitchen hanging pendant lamps; and soft neon radial wall glow behind Gaming Room RGB hexagonal panels & Streaming Desk ring light.",
                "Upgraded Girly Room & Teen Room fairy lights (bulbs now hang precisely on the wire curve with metallic sockets & multi-layer radial light blooms), added glowing flame & radial candlelight halos to the Girly shelf candle and Fireplace mantel candle, plus smooth torchlight pools in Blocky/Voxel mode."
            )
        ),
        ChangelogEntry(
            version = "3.2.1",
            title = "Opt-In Alpha Updates Checkbox & Stable Release Channel",
            tag = "FEATURE",
            highlights = listOf(
                "Added an \"Include Alpha Versions\" checkbox right inside Settings → App Updates (unchecked by default) so ordinary users stay strictly on stable releases unless they choose to opt in.",
                "Updated the in-app GitHub updater to filter out alpha/pre-release APKs and GitHub pre-releases when unchecked, and automatically include early alpha test builds (e.g. v3.3a) with clear ALPHA badges when checked.",
                "Includes the full Girly Room theme & Blush Rose / Lavender Night palettes and allows devices on v3.1.5 or v3.2a to update seamlessly in-place."
            )
        ),
        ChangelogEntry(
            version = "3.2a",
            title = "Girly Room Theme (Alpha Preview) + Blush Rose & Lavender Night Palettes",
            tag = "ALPHA",
            highlights = listOf(
                "Added the brand-new Girly Room (Alpha) style: Parisian pastel striped wallpaper, draped pearl & rose-gold fairy lights tied with a satin ribbon bow, floating sparkles & tiny hearts, pastel Polaroid wall prints, and a fluffy scalloped-lace heart rug!",
                "Styled Girly Room furniture details: tufted pastel velvet headboard with pearl buttons & plush heart throw pillow, lace-ruffle duvet, sheer tied-back pastel window curtains, rose-gold bow picture frame with cherry-blossom sunset art, rose-gold pearl clock, and pastel bookshelf with tulips, jewelry box & glowing scented candle.",
                "Added 2 matching Color Palettes — Blush Rose (light pastel pink & rose-gold) and Lavender Night (cozy dark twilight lilac & rose glow) — with 1-tap switching in Settings → Room Style, full Home Wallpaper Mirror & matching pink/lavender charging animation, plus alpha/beta version support in the GitHub updater."
            )
        ),
        ChangelogEntry(
            version = "3.1.5",
            title = "Fixed App Drawer Auto-Close on Swipe Down",
            tag = "BUGFIX",
            highlights = listOf(
                "Fixed a stale visibility closure in the pre-warmed All Apps drawer that prevented swipe-down gestures (and close triggers) from automatically finishing the close animation.",
                "Upgraded top-edge and nested-scroll swipe-down dismissal to track finger motion synchronously at full refresh rate with accurate screen-space velocity detection and pre-overscroll interception at the top of the app grid/list.",
                "Tuned auto-close threshold (8% pull or quick downward flick) so swiping down from the top effortlessly glides the drawer all the way closed every time."
            )
        ),
        ChangelogEntry(
            version = "3.1.4",
            title = "Larger App Drawer Scale (DPI) & Simple % Size Setting",
            tag = "FEATURE",
            highlights = listOf(
                "Increased the default App Drawer DPI scale to 115% so app icons, labels, search bar, and touch targets inside All Apps are larger, crisper, and easier to read while keeping your home screen at native 100% DPI.",
                "Added an intuitive App Drawer Size setting in Settings → App Drawer expressed cleanly in % (75%–140% in 5% steps, with 1-tap 85% / 100% / 115% / 130% presets and a live visual icon preview).",
                "Upgraded cached app icons to 180px and adaptive Grid columns (5 / 4 / 3 columns depending on %) so icons stay razor-sharp and neatly aligned at every scale."
            )
        ),
        ChangelogEntry(
            version = "3.1.3",
            title = "App Drawer A–Z Sorting Headers & Full-Screen Swipe-Down Close",
            tag = "BUGFIX",
            highlights = listOf(
                "Restored clear A–Z alphabetical section sorting & letter headers in Grid View (alongside List View, A–Z / Newest / Recent sort cycling, and uniform aligned grid tiles).",
                "Added full-drawer swipe-down-to-close: pulling down anywhere on the App Grid or List when at the top now glides the drawer down 1:1 with your finger and closes on flick or release—and swiping down anywhere on the top header, search bar, or filter chips closes it immediately from any scroll depth.",
                "Silently resets drawer scroll back to the top after closing so reopening starts cleanly at the top ready for instant swipe-down close."
            )
        ),
        ChangelogEntry(
            version = "3.1.2",
            title = "Native DPI Restore, All Apps Grid Alignment & Bottom Button Fix",
            tag = "BUGFIX",
            highlights = listOf(
                "Restored 100% native system DPI (removed the v3.1.1 LocalDensity override that made UI/text look way too tiny on Oppo Reno 6 5G) while keeping a gentle 0.93× room breathing margin.",
                "Fixed the missing bottom All Apps button: moved navigationBarsPadding() outside the bottom pill container and removed its fixed 112dp height cap so both the Room Switcher and All Apps pill are always fully visible above 3-button and gesture nav bars.",
                "Fixed scattered/misaligned apps in the All Apps Grid: removed mid-grid A–Z header breaks so all 4 columns flow continuously row-by-row with uniform 30dp label height and crisp 46–50dp icons (while keeping the A–Z fast-scroll rail working smoothly in both Grid and List views)."
            )
        ),
        ChangelogEntry(
            version = "3.1.1",
            title = "Oppo Reno 6 5G / Compact-Width Display Scaling Fix",
            tag = "BUGFIX",
            highlights = listOf(
                "Added adaptive high-DPI display scaling for 360dp-class ColorOS / Oppo Reno 6 5G screens (normalizing oversized 480dpi / 3.0x density cleanly to a roomy 412dp logical reference while keeping native 1080×2400 pixel sharpness).",
                "Gently stepped back room furniture perspective (0.88× horizon-anchored camera pull-back across View Mode, Edit Mode & Wallpaper Mirror) so the room feels spacious instead of pressed up against the bezels.",
                "Rescaled the redesigned All Apps drawer with adaptive icon sizing (40–46dp), razor-sharp 144px cached app icons, a sleeker 44dp pill search bar, compact header & action tiles, and a slimmer A–Z fast-scroll rail."
            )
        ),
        ChangelogEntry(
            version = "3.1.0",
            title = "Redesigned All Apps, Boot-Cached Speed, Rich Onboarding & Big Window Temp",
            tag = "FEATURE",
            highlights = listOf(
                "Completely redesigned the All Apps drawer: 4-column Icon Grid & List view toggle, Pinned Favorites dock strip, instant filter pills (All, Pinned, Recent, Games, Unsorted), sort switcher (A–Z, Newest, Recent), Enter-to-launch search, A–Z fast-scroll rail in both views, and a rich long-press App Actions sheet with 1-tap Uninstall, Pin, App Info & Room assignment.",
                "Boot-cached & pre-warmed App Drawer: loads all installed apps and crisp 96px icons once on launcher boot, keeps the drawer persistently warm off-screen for zero mount/teardown hitch, and updates incrementally in milliseconds ONLY when an app is installed or uninstalled.",
                "Overhauled Onboarding into a visual, interactive 4-step setup featuring a live miniature illustrated Room Preview, instant Room Style & Theme Palette pickers, Live Window Weather toggle, boot-cache status, and guided Oppo / ColorOS battery & Default Home setup.",
                "Enlarged and restyled the Room Window temperature badge (13.5–15sp bold high-contrast frosted pill with weather icon & °C unit, also mirrored onto Home Screen wallpaper)."
            )
        ),
        ChangelogEntry(
            version = "3.0.2",
            title = "App Drawer Swipe-Close Glitch Fix",
            tag = "BUGFIX",
            highlights = listOf(
                "Completely eliminated the App Drawer close glitch when using swipe gestures (including on ColorOS / Oppo devices): replaced frame-by-frame Predictive Back shape/shadow thrashing with a pure-GPU 100% opaque vertical slide (0 recompositions during drag or close).",
                "Added smooth 1:1 finger-following swipe-down on the All Apps header with velocity flick support and pointer-blocking during exit so touches never leak or bounce back.",
                "Switched both All Apps and Settings to clean single-fire BackHandler dispatch without ColorOS enableOnBackInvokedCallback window-scale conflicts."
            )
        ),
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
