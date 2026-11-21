package com.roomlauncher.app.data

import androidx.compose.runtime.Immutable
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
enum class RoomObjectVisual {
    TV,
    CONSOLE,
    SHELF,
    PAINTING,
    TRASH_CAN,
    BED,
    CLOCK,
    WINDOW,
    STICKY_NOTE,
    CUSTOM
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
data class RoomConfig(
    val version: Int = 2,
    val onboardingCompleted: Boolean = false,
    val onboardingStep: Int = 0,
    val autoPopulated: Boolean = false,
    val roomStyleId: RoomStyleId = RoomStyleId.COZY_MINIMALIST,
    val themeId: RoomThemeId = RoomThemeId.WARM_LINEN,
    val useQuickAnimations: Boolean = false,
    val weatherEnabled: Boolean = true,
    val mirrorHomeWallpaper: Boolean = false,
    val framePhotoPath: String? = null,
    val posterLeftPath: String? = null,
    val posterRightPath: String? = null,
    val webShortcuts: List<WebShortcut> = emptyList(),
    val hotspots: List<RoomHotspot> = emptyList()
)
