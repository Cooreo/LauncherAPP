package com.roomlauncher.app.data

import androidx.compose.runtime.Immutable
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import kotlinx.serialization.Serializable

@Serializable
enum class HotspotActionType {
    PANEL,
    UNSORTED_PANEL,
    DIRECT_GALLERY,
    DIRECT_ALARM,
    DIRECT_WEATHER
}

@Serializable
enum class RoomType(
    val displayName: String,
    val description: String
) {
    BEDROOM("Bedroom", "Cozy home base — TV, Console, Shelf, Bed & Trash Can"),
    LIVING_ROOM("Living Room", "Lounge & social — Sofa, Fireplace, Vinyl Hi-Fi & Coffee Table"),
    KITCHEN("Kitchen", "Daily essentials — Fridge, Espresso, Stove & Pantry Counter"),
    GAMING_ROOM("Gaming Room", "RGB battle station — Gaming PC, Arcade, Stream Desk & Trophies")
}

@Serializable
enum class RoomObjectVisual(val displayName: String) {
    // Classic Bedroom visuals
    TV("TV"),
    CONSOLE("Game Console"),
    SHELF("Bookshelf"),
    PAINTING("Picture Frame"),
    TRASH_CAN("Trash Can"),
    BED("Bed"),
    CLOCK("Wall Clock"),
    WINDOW("Window"),
    STICKY_NOTE("Sticky Note"),
    CUSTOM("Custom Object"),

    // Living Room visuals
    SOFA("Lounge Sofa"),
    FIREPLACE("Fireplace"),
    COFFEE_TABLE("Coffee Table"),
    VINYL_PLAYER("Vinyl Hi-Fi"),

    // Kitchen visuals
    FRIDGE("Refrigerator"),
    COFFEE_MAKER("Coffee Maker"),
    STOVE("Stove & Oven"),
    KITCHEN_COUNTER("Kitchen Counter"),

    // Gaming Room visuals
    GAMING_PC("Gaming PC"),
    ARCADE_CABINET("Arcade Cabinet"),
    STREAMING_DESK("Streaming Desk"),
    TROPHY_SHELF("Trophy Shelf")
}

@Immutable
@Serializable
data class WebShortcut(
    val id: String,
    val title: String,
    val url: String
)

@Immutable
@Serializable
data class RoomHotspot(
    val id: String,
    val label: String,
    val subtitle: String = "",
    /** Normalized left coordinate in [0f, 1f] of room width */
    val x: Float,
    /** Normalized top coordinate in [0f, 1f] of room height */
    val y: Float,
    /** Normalized width in (0f, 1f] of room width */
    val width: Float,
    /** Normalized height in (0f, 1f] of room height */
    val height: Float,
    val actionType: HotspotActionType,
    val visualType: RoomObjectVisual = RoomObjectVisual.CUSTOM,
    /** Assigned package names (ignored for UNSORTED_PANEL which dynamically shows unassigned apps) */
    val packages: List<String> = emptyList()
) {
    val centerX: Float get() = (x + width / 2f).coerceIn(0f, 1f)
    val centerY: Float get() = (y + height / 2f).coerceIn(0f, 1f)
}

@Immutable
@Serializable
data class RoomPage(
    val id: String,
    val name: String,
    val type: RoomType = RoomType.BEDROOM,
    val hotspots: List<RoomHotspot> = emptyList()
)

@Immutable
@Serializable
data class RoomConfig(
    val version: Int = 3,
    val onboardingCompleted: Boolean = false,
    val onboardingStep: Int = 0,
    val autoPopulated: Boolean = false,
    val multiRoomAutoPopulated: Boolean = false,
    val lastSeenChangelogVersion: String = "",
    val roomStyleId: RoomStyleId = RoomStyleId.COZY_MINIMALIST,
    val geometryDashSubTheme: GeometryDashSubTheme = GeometryDashSubTheme.SONIC_WAVE,
    val themeId: RoomThemeId = RoomThemeId.WARM_LINEN,
    val dynamicThemeEnabled: Boolean = false,
    val useQuickAnimations: Boolean = false,
    val chargingAnimationEnabled: Boolean = true,
    val chargingAnimationStyle: ChargingAnimationStyle = ChargingAnimationStyle.MATCH_ROOM,
    val musicVisualizerEnabled: Boolean = true,
    val musicVisualizerStyle: VisualizerStyle = VisualizerStyle.SPECTRUM_BARS,
    val musicDefaultMode: MusicDisplayMode = MusicDisplayMode.VISUALIZER,
    val audioReactiveVisualizerEnabled: Boolean = false,
    val autoCheckUpdatesEnabled: Boolean = true,
    val weatherEnabled: Boolean = true,
    val mirrorHomeWallpaper: Boolean = false,
    val framePhotoPath: String? = null,
    val posterLeftPath: String? = null,
    val posterRightPath: String? = null,
    val webShortcuts: List<WebShortcut> = emptyList(),
    /** Multi-room house pages (Bedroom, Living Room, Kitchen, Gaming Room, etc.) */
    val rooms: List<RoomPage> = emptyList(),
    /** Legacy primary bedroom hotspots kept in sync for backward compatibility */
    val hotspots: List<RoomHotspot> = emptyList()
) {
    val effectiveRooms: List<RoomPage>
        get() = if (rooms.isNotEmpty()) {
            rooms
        } else {
            listOf(
                RoomPage(
                    id = RoomConfigRepository.ROOM_ID_BEDROOM,
                    name = "Bedroom",
                    type = RoomType.BEDROOM,
                    hotspots = hotspots
                )
            )
        }

    val allHotspots: List<RoomHotspot>
        get() = effectiveRooms.flatMap { it.hotspots }

    fun findHotspot(hotspotId: String): RoomHotspot? =
        effectiveRooms.asSequence().flatMap { it.hotspots.asSequence() }.firstOrNull { it.id == hotspotId }
}
