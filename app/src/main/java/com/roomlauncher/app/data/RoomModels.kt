package com.roomlauncher.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class HotspotActionType {
    PANEL,
    UNSORTED_PANEL,
    DIRECT_GALLERY,
    DIRECT_ALARM
}

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
    /** Assigned package names (ignored for UNSORTED_PANEL which dynamically shows unassigned apps) */
    val packages: List<String> = emptyList()
) {
    val centerX: Float get() = (x + width / 2f).coerceIn(0f, 1f)
    val centerY: Float get() = (y + height / 2f).coerceIn(0f, 1f)
}

@Serializable
data class RoomConfig(
    val version: Int = 1,
    val onboardingCompleted: Boolean = false,
    val autoPopulated: Boolean = false,
    val hotspots: List<RoomHotspot> = emptyList()
)
