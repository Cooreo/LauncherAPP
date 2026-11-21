package com.roomlauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RoomLightColorScheme = lightColorScheme(
    primary = RoomSage,
    onPrimary = Color.White,
    primaryContainer = RoomSageContainer,
    onPrimaryContainer = RoomInk,
    secondary = RoomTerracotta,
    onSecondary = Color.White,
    secondaryContainer = RoomTerracottaContainer,
    onSecondaryContainer = RoomInk,
    background = RoomLinen,
    onBackground = RoomInk,
    surface = RoomCardSurface,
    onSurface = RoomInk,
    surfaceVariant = Color(0xFFEDE4D7),
    onSurfaceVariant = RoomMutedInk,
    outline = RoomOutline
)

@Composable
fun RoomLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoomLightColorScheme,
        typography = RoomTypography,
        content = content
    )
}
