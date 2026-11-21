package com.roomlauncher.app.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.roomlauncher.app.R

@DrawableRes
fun GeometryDashSubTheme.mainDrawableRes(): Int = when (this) {
    GeometryDashSubTheme.SONIC_WAVE -> R.drawable.gd_sonic_wave_main
    GeometryDashSubTheme.ASTRAL_DIVINITY -> R.drawable.gd_astral_divinity_main
    GeometryDashSubTheme.DEATH_CORRIDOR -> R.drawable.gd_death_corridor_main
}

@DrawableRes
fun GeometryDashSubTheme.posterDrawableRes(): Int = when (this) {
    GeometryDashSubTheme.SONIC_WAVE -> R.drawable.gd_sonic_wave_poster
    GeometryDashSubTheme.ASTRAL_DIVINITY -> R.drawable.gd_astral_divinity_poster
    GeometryDashSubTheme.DEATH_CORRIDOR -> R.drawable.gd_death_corridor_poster
}

fun GeometryDashSubTheme.accentNeonColors(): Triple<Color, Color, Color> = when (this) {
    GeometryDashSubTheme.SONIC_WAVE -> Triple(
        Color(0xFF00E5FF),
        Color(0xFF2563EB),
        Color(0xFF7DD3FC)
    )
    GeometryDashSubTheme.ASTRAL_DIVINITY -> Triple(
        Color(0xFFE879F9),
        Color(0xFF7C3AED),
        Color(0xFFFBBF24)
    )
    GeometryDashSubTheme.DEATH_CORRIDOR -> Triple(
        Color(0xFFFF2442),
        Color(0xFF991B1B),
        Color(0xFFFF7A30)
    )
}
