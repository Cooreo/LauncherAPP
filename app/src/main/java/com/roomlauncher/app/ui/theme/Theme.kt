package com.roomlauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalRoomPalette = staticCompositionLocalOf {
    RoomThemes.paletteFor(RoomThemeId.WARM_LINEN)
}

@Composable
fun RoomLauncherTheme(
    themeId: RoomThemeId = RoomThemeId.WARM_LINEN,
    roomStyle: RoomStyleId = RoomStyleId.COZY_MINIMALIST,
    gdSubTheme: GeometryDashSubTheme = GeometryDashSubTheme.SONIC_WAVE,
    content: @Composable () -> Unit
) {
    val palette = RoomThemes.effectivePalette(
        roomStyle = roomStyle,
        themeId = themeId,
        gdSubTheme = gdSubTheme
    )
    CompositionLocalProvider(LocalRoomPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.colorScheme,
            typography = RoomTypography,
            content = content
        )
    }
}
