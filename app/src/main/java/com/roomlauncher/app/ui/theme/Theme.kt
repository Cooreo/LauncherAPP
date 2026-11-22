package com.roomlauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalRoomPalette = staticCompositionLocalOf {
    RoomThemes.paletteFor(RoomThemeId.WARM_LINEN)
}

/**
 * Pure scaling helper that normalizes oversized OEM display densities (e.g. ColorOS / Oppo Reno 6 5G
 * running 1080×2400 at 480dpi / 3.0x = 360dp width) toward a comfortable 412dp logical reference width
 * while preserving native physical pixel sharpness and leaving 412dp+ devices untouched.
 */
object DisplayScaling {
    const val REFERENCE_WIDTH_DP = 412f
    const val MIN_HIGH_RES_WIDTH_PX = 900
    const val MIN_SCALE_FACTOR = 0.84f
    const val MAX_FONT_SCALE = 1.15f
    const val MIN_FONT_SCALE = 0.85f

    fun computeDensityScale(screenWidthDp: Int, physicalWidthPx: Int): Float {
        if (screenWidthDp <= 0 || physicalWidthPx < MIN_HIGH_RES_WIDTH_PX) return 1f
        if (screenWidthDp >= REFERENCE_WIDTH_DP.toInt()) return 1f
        return (screenWidthDp.toFloat() / REFERENCE_WIDTH_DP).coerceIn(MIN_SCALE_FACTOR, 1f)
    }

    fun clampFontScale(rawFontScale: Float): Float =
        rawFontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
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
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val rawDensity = LocalDensity.current
    val physicalWidthPx = context.resources.displayMetrics.widthPixels

    val adaptedDensity = remember(
        rawDensity.density,
        rawDensity.fontScale,
        configuration.screenWidthDp,
        physicalWidthPx
    ) {
        val densityScale = DisplayScaling.computeDensityScale(
            screenWidthDp = configuration.screenWidthDp,
            physicalWidthPx = physicalWidthPx
        )
        Density(
            density = rawDensity.density * densityScale,
            fontScale = DisplayScaling.clampFontScale(rawDensity.fontScale)
        )
    }

    CompositionLocalProvider(
        LocalRoomPalette provides palette,
        LocalDensity provides adaptedDensity
    ) {
        MaterialTheme(
            colorScheme = palette.colorScheme,
            typography = RoomTypography,
            content = content
        )
    }
}
