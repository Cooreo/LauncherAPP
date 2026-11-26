package com.roomlauncher.app.ui.room

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.CallPhase
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.RoomType
import com.roomlauncher.app.data.WeatherCondition
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import com.roomlauncher.app.ui.theme.RoomVisualPalette
import com.roomlauncher.app.ui.theme.accentNeonColors
import com.roomlauncher.app.ui.theme.isBlueMood
import com.roomlauncher.app.ui.theme.mainDrawableRes
import com.roomlauncher.app.ui.theme.posterDrawableRes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders the architectural shell of the room according to the active [RoomStyleId]
 * (Cozy Minimalist, Teen Room with LED strip & custom posters, Kids Room with bunting & stars,
 * or Blocky/Voxel Minecraft Room with pixel oak planks, cobblestone & torches) combined with [palette].
 */
@Composable
fun RoomShellCanvas(
    palette: RoomVisualPalette,
    roomStyle: RoomStyleId,
    roomType: RoomType = RoomType.BEDROOM,
    gdSubTheme: GeometryDashSubTheme = GeometryDashSubTheme.SONIC_WAVE,
    posterLeftBitmap: ImageBitmap? = null,
    posterRightBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        // Atmospheric upper-wall level art layer behind the Geometry Dash neon architectural shell
        if (roomStyle == RoomStyleId.GEOMETRY_DASH && wPx > 0f && hPx > 0f) {
            Image(
                painter = painterResource(id = gdSubTheme.mainDrawableRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.24f,
                modifier = Modifier
                    .size(
                        width = maxWidth,
                        height = with(density) { (hPx * 0.62f).toDp() }
                    )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            when (roomStyle) {
                RoomStyleId.COZY_MINIMALIST -> drawCozyShell(palette)
                RoomStyleId.GIRLY_ROOM -> drawGirlyShell(palette)
                RoomStyleId.TEEN_ROOM -> drawTeenShell(palette)
                RoomStyleId.KIDS_ROOM -> drawKidsShell(palette)
                RoomStyleId.MINECRAFT -> drawMinecraftShell(palette)
                RoomStyleId.GEOMETRY_DASH -> drawGeometryDashShell(palette, gdSubTheme)
            }
            // Layer room-specific architecture (Living Room wainscoting, Kitchen tiles, Gaming Room RGB hexes)
            drawRoomTypeArchitectureAccent(roomType, palette)
        }

        // Girly Room features 2 cute pastel Polaroid wall prints taped with pastel washi tape!
        if (roomStyle == RoomStyleId.GIRLY_ROOM && roomType == RoomType.BEDROOM && wPx > 0f && hPx > 0f) {
            val isBlueGirly = palette.isBlueMood
            GirlyWallPolaroid(
                isHeartCard = true,
                isBlueGirly = isBlueGirly,
                modifier = Modifier
                    .offset { IntOffset((wPx * 0.345f).roundToInt(), (hPx * 0.285f).roundToInt()) }
                    .size(
                        width = with(density) { (wPx * 0.165f).toDp() },
                        height = with(density) { (hPx * 0.115f).toDp() }
                    )
                    .rotate(-3.2f)
            )
            GirlyWallPolaroid(
                isHeartCard = false,
                isBlueGirly = isBlueGirly,
                modifier = Modifier
                    .offset { IntOffset((wPx * 0.565f).roundToInt(), (hPx * 0.205f).roundToInt()) }
                    .size(
                        width = with(density) { (wPx * 0.145f).toDp() },
                        height = with(density) { (hPx * 0.105f).toDp() }
                    )
                    .rotate(3.6f)
            )
        }

        // Teen Room features 2 customizable taped wall posters right on the bedroom wall!
        if (roomStyle == RoomStyleId.TEEN_ROOM && roomType == RoomType.BEDROOM && wPx > 0f && hPx > 0f) {
            TeenWallPoster(
                bitmap = posterLeftBitmap,
                isLeft = true,
                modifier = Modifier
                    .offset { IntOffset((wPx * 0.34f).roundToInt(), (hPx * 0.285f).roundToInt()) }
                    .size(
                        width = with(density) { (wPx * 0.19f).toDp() },
                        height = with(density) { (hPx * 0.125f).toDp() }
                    )
                    .rotate(-3f)
            )
            TeenWallPoster(
                bitmap = posterRightBitmap,
                isLeft = false,
                modifier = Modifier
                    .offset { IntOffset((wPx * 0.56f).roundToInt(), (hPx * 0.205f).roundToInt()) }
                    .size(
                        width = with(density) { (wPx * 0.16f).toDp() },
                        height = with(density) { (hPx * 0.115f).toDp() }
                    )
                    .rotate(3.5f)
            )
        }

        // Geometry Dash room features 2 glowing neon-framed Extreme Demon level artworks on the bedroom wall!
        if (roomStyle == RoomStyleId.GEOMETRY_DASH && roomType == RoomType.BEDROOM && wPx > 0f && hPx > 0f) {
            val (neonPrimary, _, neonTertiary) = gdSubTheme.accentNeonColors()
            GeometryDashWallPoster(
                drawableRes = gdSubTheme.posterDrawableRes(),
                label = gdSubTheme.displayName.uppercase(),
                borderColor = neonPrimary,
                modifier = Modifier
                    .offset { IntOffset((wPx * 0.335f).roundToInt(), (hPx * 0.265f).roundToInt()) }
                    .size(
                        width = with(density) { (wPx * 0.22f).toDp() },
                        height = with(density) { (hPx * 0.135f).toDp() }
                    )
                    .rotate(-2.2f)
            )
            GeometryDashWallPoster(
                drawableRes = gdSubTheme.mainDrawableRes(),
                label = "EXTREME DEMON",
                borderColor = neonTertiary,
                modifier = Modifier
                    .offset { IntOffset((wPx * 0.555f).roundToInt(), (hPx * 0.185f).roundToInt()) }
                    .size(
                        width = with(density) { (wPx * 0.18f).toDp() },
                        height = with(density) { (hPx * 0.112f).toDp() }
                    )
                    .rotate(2.4f)
            )
        }
    }
}

@Composable
private fun GeometryDashWallPoster(
    drawableRes: Int,
    label: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xE0060812))
            .border(1.8.dp, borderColor.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
            .padding(2.5.dp)
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xC805070E))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = label,
                color = borderColor,
                fontSize = 6.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
private fun TeenWallPoster(
    bitmap: ImageBitmap?,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, RoundedCornerShape(3.dp))
                    .padding(3.dp)
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Custom teen room poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Built-in stylized indie / synthwave poster art
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
            ) {
                val w = size.width
                val h = size.height
                if (isLeft) {
                    // Retro sunset synthwave poster
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF2B1055), Color(0xFF752F8A), Color(0xFFFF6B6B))
                        )
                    )
                    drawCircle(
                        color = Color(0xFFFFD166),
                        radius = min(w, h) * 0.28f,
                        center = Offset(w * 0.5f, h * 0.44f)
                    )
                    // Horizontal horizon grid lines
                    for (i in 1..4) {
                        val y = h * (0.62f + i * 0.085f)
                        drawLine(Color(0xFF00F5D4).copy(alpha = 0.7f), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                    }
                } else {
                    // Geometric indie music/vinyl poster
                    drawRect(Color(0xFF1A2238))
                    drawCircle(Color(0xFFFF6A3D), radius = min(w, h) * 0.34f, center = Offset(w * 0.5f, h * 0.45f))
                    drawCircle(Color(0xFF1A2238), radius = min(w, h) * 0.14f, center = Offset(w * 0.5f, h * 0.45f))
                    drawCircle(Color(0xFFF4DB7D), radius = min(w, h) * 0.06f, center = Offset(w * 0.5f, h * 0.45f))
                    drawRect(Color(0xFF9DAAF2), Offset(w * 0.16f, h * 0.82f), Size(w * 0.68f, h * 0.07f))
                }
            }
        }
        // Tiny translucent tape strip at top center of poster
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
                .size(width = 18.dp, height = 7.dp)
                .background(Color(0xCCEEE8D5), RoundedCornerShape(1.dp))
        )
    }
}

private fun DrawScope.drawCozyShell(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.61f

    // Smooth seamless ceiling-to-wall gradient all the way from y = 0f behind the status bar
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.wallUpper, palette.wallMain, palette.wallMain),
            startY = 0f,
            endY = floorY
        ),
        topLeft = Offset.Zero,
        size = Size(w, floorY)
    )

    // Soft window daylight wash tapering smoothly downward
    val lightPath = Path().apply {
        moveTo(w * 0.04f, h * 0.08f)
        lineTo(w * 0.31f, h * 0.08f)
        lineTo(w * 0.54f, floorY)
        lineTo(w * 0.09f, floorY)
        close()
    }
    drawPath(
        path = lightPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                palette.ambientLightTint,
                palette.ambientLightTint.copy(alpha = palette.ambientLightTint.alpha * 0.55f)
            ),
            startY = h * 0.06f,
            endY = floorY
        )
    )

    drawBaseboardAndWoodFloor(palette, floorY)

    // Classic woven area rug
    val rugTopLeft = Offset(w * 0.12f, h * 0.665f)
    val rugSize = Size(w * 0.76f, h * 0.245f)
    drawRoundRect(palette.rugFill, rugTopLeft, rugSize, CornerRadius(w * 0.035f, w * 0.035f))
    drawRoundRect(
        color = palette.rugBorder.copy(alpha = 0.6f),
        topLeft = Offset(rugTopLeft.x + w * 0.03f, rugTopLeft.y + h * 0.015f),
        size = Size(rugSize.width - w * 0.06f, rugSize.height - h * 0.03f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
        style = Stroke(width = w * 0.004f)
    )
}

@Composable
private fun GirlyWallPolaroid(
    isHeartCard: Boolean,
    isBlueGirly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderCol = if (isBlueGirly) Color(0x457AB8EA) else Color(0x40D989AD)
    Box(
        modifier = modifier
            .background(if (isBlueGirly) Color(0xFFFAFDFF) else Color(0xFFFFFAFC), RoundedCornerShape(4.dp))
            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp))) {
            val w = size.width
            val h = size.height
            if (isHeartCard) {
                drawRect(
                    brush = Brush.verticalGradient(
                        if (isBlueGirly) {
                            listOf(Color(0xFFD8EEFF), Color(0xFFC9E3FF), Color(0xFFDFEAFF))
                        } else {
                            listOf(Color(0xFFFFE0EE), Color(0xFFF7C9E6), Color(0xFFE7D2FA))
                        }
                    )
                )
                drawTinyHeart(
                    Offset(w * 0.50f, h * 0.52f),
                    min(w, h) * 0.26f,
                    if (isBlueGirly) Color(0xFF5CA6E8) else Color(0xFFE8699E)
                )
                drawSparkleStar(Offset(w * 0.22f, h * 0.25f), min(w, h) * 0.08f, Color.White.copy(alpha = 0.92f))
                drawSparkleStar(Offset(w * 0.78f, h * 0.30f), min(w, h) * 0.065f, Color(0xFFFFF4C2))
            } else {
                drawRect(
                    brush = Brush.verticalGradient(
                        if (isBlueGirly) {
                            listOf(Color(0xFFE2E9FF), Color(0xFFD9EFFF))
                        } else {
                            listOf(Color(0xFFEEE0FF), Color(0xFFFFE5F0))
                        }
                    )
                )
                // Minimalist pastel flower stem
                drawLine(
                    color = Color(0xFF7BB38C),
                    start = Offset(w * 0.50f, h * 0.86f),
                    end = Offset(w * 0.50f, h * 0.42f),
                    strokeWidth = maxOf(2f, w * 0.045f),
                    cap = StrokeCap.Round
                )
                val leaf = Path().apply {
                    moveTo(w * 0.50f, h * 0.72f)
                    quadraticTo(w * 0.74f, h * 0.64f, w * 0.68f, h * 0.52f)
                    quadraticTo(w * 0.54f, h * 0.58f, w * 0.50f, h * 0.72f)
                    close()
                }
                drawPath(leaf, Color(0xFF8CC49B))
                if (isBlueGirly) {
                    drawCircle(Color(0xFF6BB5F0), min(w, h) * 0.16f, Offset(w * 0.50f, h * 0.36f))
                    drawCircle(Color(0xFFA2D4FA), min(w, h) * 0.11f, Offset(w * 0.44f, h * 0.34f))
                    drawCircle(Color(0xFFE6F4FF), min(w, h) * 0.07f, Offset(w * 0.56f, h * 0.33f))
                } else {
                    drawCircle(Color(0xFFFF88B8), min(w, h) * 0.16f, Offset(w * 0.50f, h * 0.36f))
                    drawCircle(Color(0xFFFFB5D3), min(w, h) * 0.11f, Offset(w * 0.44f, h * 0.34f))
                    drawCircle(Color(0xFFFFD6E8), min(w, h) * 0.07f, Offset(w * 0.56f, h * 0.33f))
                }
            }
        }
        // Cute pastel washi tape strip at top center
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-6).dp)
                .size(width = 18.dp, height = 6.dp)
                .background(
                    when {
                        isBlueGirly && isHeartCard -> Color(0xCC8CC8F5)
                        isBlueGirly -> Color(0xCCA8B9F7)
                        isHeartCard -> Color(0xCCF59FC6)
                        else -> Color(0xCCC9AEF5)
                    },
                    RoundedCornerShape(1.5.dp)
                )
        )
    }
}

private fun quadraticBezierPoint(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        x = u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x,
        y = u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y
    )
}

private fun DrawScope.drawGlowingFairyBulb(
    center: Offset,
    socketColor: Color,
    outerGlowColor: Color,
    innerGlowColor: Color,
    coreColor: Color,
    bulbRadius: Float,
    isDark: Boolean
) {
    val haloRadius = bulbRadius * (if (isDark) 7.4f else 6.0f)
    // Wide soft radial wall light bloom
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                innerGlowColor.copy(alpha = if (isDark) 0.72f else 0.56f),
                outerGlowColor.copy(alpha = if (isDark) 0.34f else 0.24f),
                Color.Transparent
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )
    // Tiny metallic bulb socket hanging from wire
    drawRoundRect(
        color = socketColor,
        topLeft = Offset(center.x - bulbRadius * 0.42f, center.y - bulbRadius * 1.45f),
        size = Size(bulbRadius * 0.84f, bulbRadius * 0.65f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Luminous glass bulb & hot incandescent core
    drawCircle(innerGlowColor.copy(alpha = 0.95f), radius = bulbRadius * 1.15f, center = center)
    drawCircle(coreColor, radius = bulbRadius * 0.76f, center = center)
}

private fun DrawScope.drawGirlyShell(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.61f
    val isBlue = palette.isBlueMood

    // Dreamy wall gradient extending seamlessly behind the status bar
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.wallUpper, palette.wallMain, palette.wallMain),
            startY = 0f,
            endY = floorY
        ),
        topLeft = Offset.Zero,
        size = Size(w, floorY)
    )

    // Subtle Parisian boutique vertical pastel stripes on the upper wall
    val stripeCount = 12
    val stripeW = w / stripeCount
    val stripeTint = when {
        palette.isDark -> Color(0xFFF4A2C9).copy(alpha = 0.06f)
        isBlue -> Color.White.copy(alpha = 0.42f)
        else -> Color.White.copy(alpha = 0.34f)
    }
    for (i in 0 until stripeCount step 2) {
        drawRect(stripeTint, Offset(i * stripeW, 0f), Size(stripeW, floorY))
    }

    // Soft ambient canopy glow across upper ceiling
    val glowPrimary = if (isBlue) Color(0xFF8CCBFF) else Color(0xFFFF9EC7)
    val glowSecondary = if (isBlue) Color(0xFFB8C9FF) else Color(0xFFD6B4FC)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                glowPrimary.copy(alpha = if (palette.isDark) 0.34f else 0.24f),
                glowSecondary.copy(alpha = if (palette.isDark) 0.18f else 0.12f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.27f
        ),
        topLeft = Offset.Zero,
        size = Size(w, h * 0.27f)
    )

    // Soft dreamy daylight wash spilling diagonally from the window
    val windowLightPath = Path().apply {
        moveTo(w * 0.04f, h * 0.09f)
        lineTo(w * 0.31f, h * 0.09f)
        lineTo(w * 0.54f, floorY)
        lineTo(w * 0.09f, floorY)
        close()
    }
    drawPath(
        path = windowLightPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                palette.ambientLightTint,
                palette.ambientLightTint.copy(alpha = palette.ambientLightTint.alpha * 0.45f)
            ),
            startY = h * 0.08f,
            endY = floorY
        )
    )

    // Draped pearl & warm glowing fairy lights in graceful double swags
    val swagTopY = h * 0.064f
    val leftP0 = Offset(0f, swagTopY)
    val leftP1 = Offset(w * 0.25f, swagTopY + h * 0.058f)
    val midPt = Offset(w * 0.50f, swagTopY + h * 0.012f)
    val rightP1 = Offset(w * 0.75f, swagTopY + h * 0.058f)
    val rightP2 = Offset(w, swagTopY)

    val wirePath = Path().apply {
        moveTo(leftP0.x, leftP0.y)
        quadraticTo(leftP1.x, leftP1.y, midPt.x, midPt.y)
        quadraticTo(rightP1.x, rightP1.y, rightP2.x, rightP2.y)
    }
    drawPath(
        path = wirePath,
        color = (if (isBlue) Color(0xFF9AC2E4) else Color(0xFFDFA3BD)).copy(alpha = 0.72f),
        style = Stroke(width = 2.2f)
    )

    val bulbTValues = listOf(0.18f, 0.40f, 0.62f, 0.82f)
    val allBulbs = buildList {
        bulbTValues.forEach { t -> add(quadraticBezierPoint(leftP0, leftP1, midPt, t)) }
        bulbTValues.forEach { t -> add(quadraticBezierPoint(midPt, rightP1, rightP2, t)) }
    }
    val socketCol = if (isBlue) Color(0xFF83A7CA) else Color(0xFFD18AA9)
    allBulbs.forEachIndexed { idx, wirePt ->
        val bulbCenter = Offset(wirePt.x, wirePt.y + w * 0.011f)
        val outerGlow = when {
            isBlue && idx % 2 == 0 -> Color(0xFF99D6FF)
            isBlue -> Color(0xFFFFE59D)
            idx % 2 == 0 -> Color(0xFFFFB5D6)
            else -> Color(0xFFFFE092)
        }
        val innerGlow = when {
            isBlue && idx % 2 == 0 -> Color(0xFFDFF3FF)
            idx % 2 == 0 -> Color(0xFFFFE1EE)
            else -> Color(0xFFFFF2C2)
        }
        drawGlowingFairyBulb(
            center = bulbCenter,
            socketColor = socketCol,
            outerGlowColor = outerGlow,
            innerGlowColor = innerGlow,
            coreColor = Color(0xFFFFFEFA),
            bulbRadius = w * 0.0085f,
            isDark = palette.isDark
        )
    }

    // Cute satin ribbon bow tying the center of the fairy-light swag!
    drawRibbonBow(
        center = Offset(midPt.x, midPt.y + h * 0.002f),
        scale = w * 0.042f,
        color = if (isBlue) Color(0xFF5CA6E8) else Color(0xFFEC72A8),
        highlight = if (isBlue) Color(0xFFCFEAFF) else Color(0xFFFFC1DD)
    )

    // Delicate floating sparkles & tiny pastel hearts on the wall
    val sparkleColor = when {
        palette.isDark -> Color(0xFFFFD4E8).copy(alpha = 0.70f)
        isBlue -> Color(0xFF6AB0EB).copy(alpha = 0.55f)
        else -> Color(0xFFE886B3).copy(alpha = 0.50f)
    }
    listOf(0.14f to 0.042f, 0.78f to 0.044f, 0.48f to 0.175f, 0.86f to 0.30f, 0.25f to 0.55f).forEach { (sx, sy) ->
        drawSparkleStar(Offset(w * sx, h * sy), w * 0.014f, sparkleColor)
    }
    val heartColor = when {
        palette.isDark -> Color(0xFFF48FB1).copy(alpha = 0.45f)
        isBlue -> Color(0xFF7CB7EB).copy(alpha = 0.38f)
        else -> Color(0xFFEC7EB0).copy(alpha = 0.34f)
    }
    listOf(0.32f to 0.16f, 0.68f to 0.145f, 0.79f to 0.56f).forEach { (hx, hy) ->
        drawTinyHeart(Offset(w * hx, h * hy), w * 0.015f, heartColor)
    }

    drawBaseboardAndWoodFloor(palette, floorY)

    // Plush scalloped cloud-trim pastel rug with woven heart centerpiece
    val rugTopLeft = Offset(w * 0.11f, h * 0.662f)
    val rugSize = Size(w * 0.78f, h * 0.250f)
    val laceColor = when {
        palette.isDark -> Color(0xFFF8D0E4).copy(alpha = 0.62f)
        isBlue -> Color(0xFFFAFDFF).copy(alpha = 0.94f)
        else -> Color(0xFFFFFAFC).copy(alpha = 0.92f)
    }
    val scallopR = w * 0.022f
    // Soft lace scallops along top and bottom edges of the fluffy rug
    val horizCount = 14
    for (i in 0..horizCount) {
        val sx = rugTopLeft.x + (rugSize.width * i / horizCount)
        drawCircle(laceColor, scallopR, Offset(sx, rugTopLeft.y + scallopR * 0.25f))
        drawCircle(laceColor, scallopR, Offset(sx, rugTopLeft.y + rugSize.height - scallopR * 0.25f))
    }
    val vertCount = 5
    for (j in 1 until vertCount) {
        val sy = rugTopLeft.y + (rugSize.height * j / vertCount)
        drawCircle(laceColor, scallopR, Offset(rugTopLeft.x + scallopR * 0.25f, sy))
        drawCircle(laceColor, scallopR, Offset(rugTopLeft.x + rugSize.width - scallopR * 0.25f, sy))
    }
    drawRoundRect(palette.rugFill, rugTopLeft, rugSize, CornerRadius(w * 0.055f, w * 0.055f))
    drawRoundRect(
        color = palette.rugBorder.copy(alpha = 0.58f),
        topLeft = Offset(rugTopLeft.x + w * 0.028f, rugTopLeft.y + h * 0.014f),
        size = Size(rugSize.width - w * 0.056f, rugSize.height - h * 0.028f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
        style = Stroke(width = w * 0.0045f)
    )
    // Subtle pastel heart woven right into the center of the rug
    drawTinyHeart(
        center = Offset(rugTopLeft.x + rugSize.width * 0.5f, rugTopLeft.y + rugSize.height * 0.52f),
        radius = w * 0.055f,
        color = palette.rugBorder.copy(alpha = if (palette.isDark) 0.42f else 0.34f)
    )
}

private fun DrawScope.drawTinyHeart(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + radius * 0.92f)
        cubicTo(
            center.x - radius * 1.45f, center.y + radius * 0.12f,
            center.x - radius * 1.15f, center.y - radius * 1.05f,
            center.x, center.y - radius * 0.32f
        )
        cubicTo(
            center.x + radius * 1.15f, center.y - radius * 1.05f,
            center.x + radius * 1.45f, center.y + radius * 0.12f,
            center.x, center.y + radius * 0.92f
        )
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSparkleStar(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawRibbonBow(center: Offset, scale: Float, color: Color, highlight: Color) {
    // Left & right flowing ribbon tails
    val leftTail = Path().apply {
        moveTo(center.x - scale * 0.15f, center.y + scale * 0.10f)
        quadraticTo(center.x - scale * 0.55f, center.y + scale * 0.72f, center.x - scale * 0.78f, center.y + scale * 1.05f)
        lineTo(center.x - scale * 0.38f, center.y + scale * 0.98f)
        quadraticTo(center.x - scale * 0.20f, center.y + scale * 0.58f, center.x, center.y + scale * 0.22f)
        close()
    }
    val rightTail = Path().apply {
        moveTo(center.x + scale * 0.15f, center.y + scale * 0.10f)
        quadraticTo(center.x + scale * 0.55f, center.y + scale * 0.72f, center.x + scale * 0.78f, center.y + scale * 1.05f)
        lineTo(center.x + scale * 0.38f, center.y + scale * 0.98f)
        quadraticTo(center.x + scale * 0.20f, center.y + scale * 0.58f, center.x, center.y + scale * 0.22f)
        close()
    }
    drawPath(leftTail, color.copy(alpha = 0.88f))
    drawPath(rightTail, color.copy(alpha = 0.88f))

    // Left & right satin bow loops
    val leftLoop = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(
            center.x - scale * 0.95f, center.y - scale * 0.85f,
            center.x - scale * 1.15f, center.y + scale * 0.58f,
            center.x, center.y + scale * 0.15f
        )
        close()
    }
    val rightLoop = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(
            center.x + scale * 0.95f, center.y - scale * 0.85f,
            center.x + scale * 1.15f, center.y + scale * 0.58f,
            center.x, center.y + scale * 0.15f
        )
        close()
    }
    drawPath(leftLoop, color)
    drawPath(rightLoop, color)
    drawCircle(highlight, scale * 0.17f, Offset(center.x - scale * 0.42f, center.y - scale * 0.04f))
    drawCircle(highlight, scale * 0.17f, Offset(center.x + scale * 0.42f, center.y - scale * 0.04f))
    // Center knot
    drawCircle(color, scale * 0.24f, Offset(center.x, center.y + scale * 0.05f))
    drawCircle(highlight.copy(alpha = 0.75f), scale * 0.11f, Offset(center.x - scale * 0.04f, center.y))
}

/**
 * Small glossy collectible "mini diva" figurine — an original chibi idol character with
 * teal twin-tails, a glowing cyan headset, sailor-collar top, pleated skirt and a glass
 * display base. It is displayed on the shelf whenever a blue-mood palette
 * ([com.roomlauncher.app.ui.theme.isBlueMood] — Baby Blue or Deep Sapphire) is active,
 * and is framed by a soft teal display glow so it reads at a glance.
 */
private fun DrawScope.drawMiniDivaFigurine(
    palette: RoomVisualPalette,
    baseCenter: Offset,
    height: Float
) {
    val h = height
    val cx = baseCenter.x
    val baseY = baseCenter.y

    val hair = Color(0xFF34D6CB)
    val hairShade = Color(0xFF0FA9A4)
    val hairLight = Color(0xFF9BF5EC)
    val skin = Color(0xFFFFE7D6)
    val shirt = Color(0xFFF8FCFF)
    val accent = Color(0xFF3FC5F2)
    val skirt = Color(0xFF1C2B47)

    // Soft teal display glow so the collectible stands out on darker walls
    val glowCenter = Offset(cx, baseY - h * 0.52f)
    val glowR = h * 0.85f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF5CE0F5).copy(alpha = if (palette.isDark) 0.30f else 0.20f),
                Color(0xFF5C7BF5).copy(alpha = if (palette.isDark) 0.12f else 0.08f),
                Color.Transparent
            ),
            center = glowCenter,
            radius = glowR
        ),
        radius = glowR,
        center = glowCenter
    )

    // Glossy round display base (drawn before the feet so it sits underneath them)
    drawOval(
        color = Color(0xFF0B1729),
        topLeft = Offset(cx - h * 0.27f, baseY - h * 0.05f),
        size = Size(h * 0.54f, h * 0.10f)
    )
    drawOval(
        color = accent.copy(alpha = 0.75f),
        topLeft = Offset(cx - h * 0.27f, baseY - h * 0.05f),
        size = Size(h * 0.54f, h * 0.10f),
        style = Stroke(width = h * 0.012f)
    )
    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(cx - h * 0.17f, baseY - h * 0.038f),
        size = Size(h * 0.22f, h * 0.026f)
    )

    // Long teal twin-tails sweeping down behind the body
    val leftTail = Path().apply {
        moveTo(cx - h * 0.095f, baseY - h * 0.735f)
        cubicTo(
            cx - h * 0.31f, baseY - h * 0.700f,
            cx - h * 0.31f, baseY - h * 0.420f,
            cx - h * 0.245f, baseY - h * 0.185f
        )
        lineTo(cx - h * 0.165f, baseY - h * 0.170f)
        cubicTo(
            cx - h * 0.205f, baseY - h * 0.420f,
            cx - h * 0.190f, baseY - h * 0.620f,
            cx - h * 0.155f, baseY - h * 0.710f
        )
        close()
    }
    val rightTail = Path().apply {
        moveTo(cx + h * 0.095f, baseY - h * 0.735f)
        cubicTo(
            cx + h * 0.31f, baseY - h * 0.700f,
            cx + h * 0.31f, baseY - h * 0.420f,
            cx + h * 0.245f, baseY - h * 0.185f
        )
        lineTo(cx + h * 0.165f, baseY - h * 0.170f)
        cubicTo(
            cx + h * 0.205f, baseY - h * 0.420f,
            cx + h * 0.190f, baseY - h * 0.620f,
            cx + h * 0.155f, baseY - h * 0.710f
        )
        close()
    }
    drawPath(leftTail, hair)
    drawPath(rightTail, hair)
    // Cooler inner shading + bright edge highlights on both tails
    drawPath(
        leftTail,
        hairShade.copy(alpha = 0.45f),
        style = Stroke(width = h * 0.016f)
    )
    drawPath(
        rightTail,
        hairShade.copy(alpha = 0.45f),
        style = Stroke(width = h * 0.016f)
    )
    drawLine(
        hairLight.copy(alpha = 0.85f),
        Offset(cx - h * 0.205f, baseY - h * 0.680f),
        Offset(cx - h * 0.215f, baseY - h * 0.300f),
        strokeWidth = h * 0.014f
    )
    drawLine(
        hairLight.copy(alpha = 0.85f),
        Offset(cx + h * 0.205f, baseY - h * 0.680f),
        Offset(cx + h * 0.215f, baseY - h * 0.300f),
        strokeWidth = h * 0.014f
    )

    // Legs & little boots
    listOf(-1f, 1f).forEach { side ->
        val legX = cx + side * h * 0.058f
        drawRoundRect(
            color = skin,
            topLeft = Offset(legX - h * 0.028f, baseY - h * 0.255f),
            size = Size(h * 0.056f, h * 0.185f),
            cornerRadius = CornerRadius(h * 0.026f, h * 0.026f)
        )
        drawRoundRect(
            color = skirt,
            topLeft = Offset(legX - h * 0.032f, baseY - h * 0.098f),
            size = Size(h * 0.064f, h * 0.062f),
            cornerRadius = CornerRadius(h * 0.024f, h * 0.024f)
        )
        drawLine(
            accent.copy(alpha = 0.85f),
            Offset(legX - h * 0.030f, baseY - h * 0.072f),
            Offset(legX + h * 0.030f, baseY - h * 0.072f),
            strokeWidth = h * 0.010f
        )
    }

    // Pleated skirt
    val skirtPath = Path().apply {
        moveTo(cx - h * 0.125f, baseY - h * 0.420f)
        lineTo(cx + h * 0.125f, baseY - h * 0.420f)
        lineTo(cx + h * 0.200f, baseY - h * 0.262f)
        lineTo(cx - h * 0.200f, baseY - h * 0.262f)
        close()
    }
    drawPath(skirtPath, skirt)
    for (i in 1..4) {
        val t = i / 5f
        drawLine(
            accent.copy(alpha = 0.45f),
            Offset(cx - h * 0.125f + h * 0.25f * t, baseY - h * 0.418f),
            Offset(cx - h * 0.200f + h * 0.40f * t, baseY - h * 0.266f),
            strokeWidth = h * 0.009f
        )
    }
    drawLine(
        accent.copy(alpha = 0.9f),
        Offset(cx - h * 0.198f, baseY - h * 0.272f),
        Offset(cx + h * 0.198f, baseY - h * 0.272f),
        strokeWidth = h * 0.012f
    )

    // Sailor-collar top with cyan neckerchief
    drawRoundRect(
        color = shirt,
        topLeft = Offset(cx - h * 0.128f, baseY - h * 0.585f),
        size = Size(h * 0.256f, h * 0.175f),
        cornerRadius = CornerRadius(h * 0.045f, h * 0.045f)
    )
    val collar = Path().apply {
        moveTo(cx - h * 0.118f, baseY - h * 0.585f)
        lineTo(cx, baseY - h * 0.520f)
        lineTo(cx + h * 0.118f, baseY - h * 0.585f)
        lineTo(cx + h * 0.075f, baseY - h * 0.600f)
        lineTo(cx, baseY - h * 0.552f)
        lineTo(cx - h * 0.075f, baseY - h * 0.600f)
        close()
    }
    drawPath(collar, accent)
    drawPath(
        Path().apply {
            moveTo(cx - h * 0.035f, baseY - h * 0.545f)
            lineTo(cx + h * 0.035f, baseY - h * 0.545f)
            lineTo(cx, baseY - h * 0.470f)
            close()
        },
        Color(0xFF2AAEE0)
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.85f),
        topLeft = Offset(cx - h * 0.128f, baseY - h * 0.432f),
        size = Size(h * 0.256f, h * 0.026f),
        cornerRadius = CornerRadius(h * 0.012f, h * 0.012f)
    )

    // Arms — left resting, right cheerfully raised
    drawRoundRect(
        color = shirt,
        topLeft = Offset(cx - h * 0.185f, baseY - h * 0.570f),
        size = Size(h * 0.062f, h * 0.150f),
        cornerRadius = CornerRadius(h * 0.030f, h * 0.030f)
    )
    drawCircle(skin, h * 0.030f, Offset(cx - h * 0.154f, baseY - h * 0.425f))
    val raisedArm = Path().apply {
        moveTo(cx + h * 0.115f, baseY - h * 0.565f)
        quadraticTo(cx + h * 0.215f, baseY - h * 0.615f, cx + h * 0.205f, baseY - h * 0.700f)
    }
    drawPath(raisedArm, shirt, style = Stroke(width = h * 0.055f, cap = StrokeCap.Round))
    drawCircle(skin, h * 0.032f, Offset(cx + h * 0.203f, baseY - h * 0.712f))

    // Head — hair cap behind, face on top
    drawCircle(hair, h * 0.132f, Offset(cx, baseY - h * 0.760f))
    drawCircle(skin, h * 0.112f, Offset(cx, baseY - h * 0.742f))
    // Side-swept bangs
    val bangs = Path().apply {
        moveTo(cx - h * 0.128f, baseY - h * 0.775f)
        quadraticTo(cx - h * 0.060f, baseY - h * 0.860f, cx + h * 0.010f, baseY - h * 0.845f)
        quadraticTo(cx + h * 0.090f, baseY - h * 0.835f, cx + h * 0.126f, baseY - h * 0.760f)
        quadraticTo(cx + h * 0.045f, baseY - h * 0.800f, cx - h * 0.020f, baseY - h * 0.795f)
        quadraticTo(cx - h * 0.080f, baseY - h * 0.790f, cx - h * 0.128f, baseY - h * 0.775f)
        close()
    }
    drawPath(bangs, hair)
    drawLine(
        hairLight.copy(alpha = 0.8f),
        Offset(cx - h * 0.055f, baseY - h * 0.845f),
        Offset(cx + h * 0.030f, baseY - h * 0.838f),
        strokeWidth = h * 0.011f
    )
    // Twin-tail hair ties
    drawCircle(accent, h * 0.030f, Offset(cx - h * 0.125f, baseY - h * 0.735f))
    drawCircle(accent, h * 0.030f, Offset(cx + h * 0.125f, baseY - h * 0.735f))
    // Eyes
    drawOval(
        color = Color(0xFF14707E),
        topLeft = Offset(cx - h * 0.072f, baseY - h * 0.758f),
        size = Size(h * 0.034f, h * 0.044f)
    )
    drawOval(
        color = Color(0xFF14707E),
        topLeft = Offset(cx + h * 0.038f, baseY - h * 0.758f),
        size = Size(h * 0.034f, h * 0.044f)
    )
    drawCircle(Color.White.copy(alpha = 0.9f), h * 0.010f, Offset(cx - h * 0.062f, baseY - h * 0.748f))
    drawCircle(Color.White.copy(alpha = 0.9f), h * 0.010f, Offset(cx + h * 0.048f, baseY - h * 0.748f))
    drawLine(
        Color(0xFFD9798C).copy(alpha = 0.55f),
        Offset(cx - h * 0.028f, baseY - h * 0.722f),
        Offset(cx + h * 0.028f, baseY - h * 0.722f),
        strokeWidth = h * 0.010f,
        cap = StrokeCap.Round
    )

    // Glowing cyan headset — band, ear cups & mic boom
    drawArc(
        color = accent,
        startAngle = 192f,
        sweepAngle = 156f,
        useCenter = false,
        topLeft = Offset(cx - h * 0.150f, baseY - h * 0.845f),
        size = Size(h * 0.300f, h * 0.300f),
        style = Stroke(width = h * 0.024f, cap = StrokeCap.Round)
    )
    listOf(-1f, 1f).forEach { side ->
        drawRoundRect(
            color = accent,
            topLeft = Offset(cx + side * h * 0.150f - h * 0.030f, baseY - h * 0.775f),
            size = Size(h * 0.060f, h * 0.078f),
            cornerRadius = CornerRadius(h * 0.026f, h * 0.026f)
        )
        drawCircle(
            Color.White.copy(alpha = 0.85f),
            h * 0.012f,
            Offset(cx + side * h * 0.150f, baseY - h * 0.748f)
        )
    }
    drawLine(
        Color(0xFF2AAEE0),
        Offset(cx - h * 0.150f, baseY - h * 0.740f),
        Offset(cx - h * 0.088f, baseY - h * 0.706f),
        strokeWidth = h * 0.014f,
        cap = StrokeCap.Round
    )
    drawCircle(Color(0xFF2AAEE0), h * 0.020f, Offset(cx - h * 0.082f, baseY - h * 0.702f))

    // Tiny sparkle glints around the figurine
    drawCircle(Color.White.copy(alpha = 0.85f), h * 0.016f, Offset(cx + h * 0.245f, baseY - h * 0.800f))
    drawCircle(Color(0xFF8CF3EA).copy(alpha = 0.8f), h * 0.012f, Offset(cx - h * 0.250f, baseY - h * 0.630f))
    drawCircle(Color.White.copy(alpha = 0.6f), h * 0.010f, Offset(cx - h * 0.215f, baseY - h * 0.520f))
}

private fun DrawScope.drawTeenShell(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.61f

    // Seamless wall gradient right to y = 0f (zero cutoff behind notification bar!)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.wallUpper, palette.wallMain, palette.wallMain),
            startY = 0f,
            endY = floorY
        ),
        topLeft = Offset.Zero,
        size = Size(w, floorY)
    )

    val ledY = h * 0.068f
    // RGB LED ambient ceiling wash glowing seamlessly ALL THE WAY from y = 0f past the LED tube!
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF9B5DE5).copy(alpha = if (palette.isDark) 0.42f else 0.22f),
                Color(0xFFF15BB5).copy(alpha = if (palette.isDark) 0.46f else 0.24f),
                Color(0xFF00BBF9).copy(alpha = if (palette.isDark) 0.20f else 0.10f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.28f
        ),
        topLeft = Offset.Zero,
        size = Size(w, h * 0.28f)
    )
    // Crisp neon LED tube strip with luminous bloom & hot white core
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFF15BB5).copy(alpha = if (palette.isDark) 0.55f else 0.32f),
                Color.Transparent
            ),
            startY = ledY - h * 0.018f,
            endY = ledY + h * 0.024f
        ),
        topLeft = Offset(0f, ledY - h * 0.018f),
        size = Size(w, h * 0.042f)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color(0xFF00F5D4), Color(0xFF9B5DE5), Color(0xFFF15BB5), Color(0xFF00BBF9))
        ),
        topLeft = Offset(0f, ledY),
        size = Size(w, h * 0.0065f)
    )
    drawRect(
        color = Color.White.copy(alpha = 0.65f),
        topLeft = Offset(0f, ledY + h * 0.0018f),
        size = Size(w, h * 0.0022f)
    )

    // Warm string fairy lights draped accurately along quadratic swags with real radial blooms
    val tLeft0 = Offset(0f, ledY + h * 0.008f)
    val tLeft1 = Offset(w * 0.25f, ledY + h * 0.054f)
    val tMid = Offset(w * 0.50f, ledY + h * 0.016f)
    val tRight1 = Offset(w * 0.75f, ledY + h * 0.054f)
    val tRight2 = Offset(w, ledY + h * 0.008f)
    val wirePath = Path().apply {
        moveTo(tLeft0.x, tLeft0.y)
        quadraticTo(tLeft1.x, tLeft1.y, tMid.x, tMid.y)
        quadraticTo(tRight1.x, tRight1.y, tRight2.x, tRight2.y)
    }
    drawPath(wirePath, Color(0xFF555B6E).copy(alpha = 0.62f), style = Stroke(width = 2f))
    val teenBulbs = buildList {
        listOf(0.20f, 0.48f, 0.76f).forEach { t -> add(quadraticBezierPoint(tLeft0, tLeft1, tMid, t)) }
        add(tMid)
        listOf(0.24f, 0.52f, 0.80f).forEach { t -> add(quadraticBezierPoint(tMid, tRight1, tRight2, t)) }
    }
    teenBulbs.forEach { wirePt ->
        drawGlowingFairyBulb(
            center = Offset(wirePt.x, wirePt.y + w * 0.011f),
            socketColor = Color(0xFF3E4452),
            outerGlowColor = Color(0xFFFFB703),
            innerGlowColor = Color(0xFFFFE59D),
            coreColor = Color(0xFFFFFEFA),
            bulbRadius = w * 0.0088f,
            isDark = palette.isDark
        )
    }

    drawBaseboardAndWoodFloor(palette, floorY)

    // Modern geometric graphic rug
    val rugTopLeft = Offset(w * 0.11f, h * 0.665f)
    val rugSize = Size(w * 0.78f, h * 0.245f)
    drawRoundRect(palette.rugFill, rugTopLeft, rugSize, CornerRadius(w * 0.03f, w * 0.03f))
    // Bold diagonal stripe accents on teen rug
    val stripeColor = Color(0xFF9B5DE5).copy(alpha = if (palette.isDark) 0.35f else 0.22f)
    drawRect(stripeColor, Offset(rugTopLeft.x, rugTopLeft.y + rugSize.height * 0.18f), Size(rugSize.width, rugSize.height * 0.11f))
    drawRect(Color(0xFF00BBF9).copy(alpha = if (palette.isDark) 0.30f else 0.18f), Offset(rugTopLeft.x, rugTopLeft.y + rugSize.height * 0.72f), Size(rugSize.width, rugSize.height * 0.11f))
}

private fun DrawScope.drawKidsShell(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.61f

    // Smooth pastel ceiling-to-wall gradient extending all the way behind the status bar
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.wallUpper, palette.wallMain, palette.wallMain),
            startY = 0f,
            endY = floorY
        ),
        topLeft = Offset.Zero,
        size = Size(w, floorY)
    )

    // Painted playful wall stars & soft pastel dots (spanning from behind status bar down to floor)
    val starColor = Color(0xFFF4B942).copy(alpha = if (palette.isDark) 0.50f else 0.45f)
    val stars = listOf(
        0.12f to 0.045f, 0.42f to 0.035f, 0.76f to 0.048f,
        0.16f to 0.29f, 0.34f to 0.27f, 0.64f to 0.26f, 0.89f to 0.33f,
        0.53f to 0.38f, 0.24f to 0.56f, 0.79f to 0.57f
    )
    stars.forEach { (sx, sy) ->
        drawCircle(starColor, radius = w * 0.015f, center = Offset(w * sx, h * sy))
        drawCircle(Color.White.copy(alpha = 0.5f), radius = w * 0.006f, center = Offset(w * sx, h * sy))
    }

    // Colorful pennant bunting garland strung across upper wall!
    val buntingColors = listOf(
        Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF06D6A0),
        Color(0xFF118AB2), Color(0xFFFF9F1C), Color(0xFF9B5DE5)
    )
    val flagCount = 9
    val stringY = h * 0.075f
    drawLine(Color(0xFF8C7A6B), Offset(0f, stringY), Offset(w, stringY), strokeWidth = 2.2f)
    val flagW = w / flagCount
    for (i in 0 until flagCount) {
        val x1 = i * flagW + flagW * 0.08f
        val x2 = (i + 1) * flagW - flagW * 0.08f
        val flagPath = Path().apply {
            moveTo(x1, stringY)
            lineTo(x2, stringY)
            lineTo((x1 + x2) / 2f, stringY + h * 0.036f)
            close()
        }
        drawPath(flagPath, buntingColors[i % buntingColors.size].copy(alpha = 0.88f))
    }

    drawBaseboardAndWoodFloor(palette, floorY)

    // Cheerful polka-dot playroom rug!
    val rugTopLeft = Offset(w * 0.10f, h * 0.66f)
    val rugSize = Size(w * 0.80f, h * 0.255f)
    drawRoundRect(palette.rugFill, rugTopLeft, rugSize, CornerRadius(w * 0.08f, w * 0.08f))
    drawRoundRect(
        color = Color(0xFF06D6A0).copy(alpha = 0.55f),
        topLeft = rugTopLeft,
        size = rugSize,
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        style = Stroke(width = w * 0.012f)
    )
    val dots = listOf(
        Triple(0.22f, 0.72f, Color(0xFFFF6B6B)),
        Triple(0.45f, 0.70f, Color(0xFFFFD166)),
        Triple(0.74f, 0.73f, Color(0xFF118AB2)),
        Triple(0.32f, 0.84f, Color(0xFF9B5DE5)),
        Triple(0.62f, 0.85f, Color(0xFF06D6A0)),
        Triple(0.81f, 0.82f, Color(0xFFFF9F1C))
    )
    dots.forEach { (dx, dy, c) ->
        drawCircle(c.copy(alpha = 0.48f), radius = w * 0.024f, center = Offset(w * dx, h * dy))
    }
}

private fun DrawScope.drawMinecraftShell(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.61f

    // Pixelated oak plank wall blocks (tinted darker in dark themes!)
    val oakBase = if (palette.isDark) Color(0xFF4A3723) else Color(0xFFB8945F)
    val oakAlt = if (palette.isDark) Color(0xFF3E2D1C) else Color(0xFFA6824F)
    val oakSeam = if (palette.isDark) Color(0xFF2A1E12) else Color(0xFF7D5F35)

    drawRect(oakBase, Offset.Zero, Size(w, floorY))
    val brickCols = 9
    val brickRows = 14
    val cellW = w / brickCols
    val cellH = floorY / brickRows
    for (row in 0 until brickRows) {
        val offset = if (row % 2 == 0) 0f else cellW * 0.5f
        for (col in -1..brickCols) {
            val shade = if ((row + col) % 3 == 0) oakAlt else oakBase
            val left = col * cellW + offset
            drawRect(shade, Offset(left, row * cellH), Size(cellW, cellH))
            drawRect(oakSeam.copy(alpha = 0.45f), Offset(left, row * cellH), Size(cellW, cellH), style = Stroke(width = 1.5f))
        }
    }

    // Cobblestone lower wainscoting strip right above floor!
    val cobbleTop = floorY - h * 0.055f
    val cobbleCols = 12
    val cobbleW = w / cobbleCols
    val cobbleH = h * 0.055f
    val stoneShades = if (palette.isDark) {
        listOf(Color(0xFF3A3D42), Color(0xFF4C5057), Color(0xFF2D3035))
    } else {
        listOf(Color(0xFF7D8084), Color(0xFF94989E), Color(0xFF63666B))
    }
    for (c in 0 until cobbleCols) {
        drawRect(stoneShades[c % stoneShades.size], Offset(c * cobbleW, cobbleTop), Size(cobbleW, cobbleH))
        drawRect(Color.Black.copy(alpha = 0.35f), Offset(c * cobbleW, cobbleTop), Size(cobbleW, cobbleH), style = Stroke(width = 2f))
    }

    // Glowing Pixel Wall Torches with real warm radial torchlight cast onto oak planks!
    listOf(0.32f to 0.30f, 0.94f to 0.25f).forEach { (tx, ty) ->
        val cx = w * tx
        val cy = h * ty
        val flameCenter = Offset(cx, cy - h * 0.008f)
        val torchGlowR = w * (if (palette.isDark) 0.20f else 0.14f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE57F).copy(alpha = if (palette.isDark) 0.62f else 0.44f),
                    Color(0xFFFF9800).copy(alpha = if (palette.isDark) 0.30f else 0.20f),
                    Color.Transparent
                ),
                center = flameCenter,
                radius = torchGlowR
            ),
            radius = torchGlowR,
            center = flameCenter
        )
        // Torch wooden stick
        drawRect(Color(0xFF6D4C2F), Offset(cx - w * 0.01f, cy), Size(w * 0.02f, h * 0.042f))
        // Pixel flame squares
        drawRect(Color(0xFFFF7043), Offset(cx - w * 0.013f, cy - h * 0.016f), Size(w * 0.026f, h * 0.018f))
        drawRect(Color(0xFFFFEE58), Offset(cx - w * 0.008f, cy - h * 0.012f), Size(w * 0.016f, h * 0.010f))
        drawRect(Color(0xFFFFFDE7), Offset(cx - w * 0.004f, cy - h * 0.009f), Size(w * 0.008f, h * 0.005f))
    }

    // Blocky spruce floor grid
    val floorBase = if (palette.isDark) Color(0xFF2B2016) else Color(0xFF7A5A3A)
    val floorAlt = if (palette.isDark) Color(0xFF231A11) else Color(0xFF6B4E31)
    drawRect(floorBase, Offset(0f, floorY), Size(w, h - floorY))
    val fCols = 8
    val fRows = 8
    val fW = w / fCols
    val fH = (h - floorY) / fRows
    for (r in 0 until fRows) {
        for (c in 0 until fCols) {
            if ((r + c) % 2 == 0) {
                drawRect(floorAlt, Offset(c * fW, floorY + r * fH), Size(fW, fH))
            }
            drawRect(Color.Black.copy(alpha = 0.18f), Offset(c * fW, floorY + r * fH), Size(fW, fH), style = Stroke(width = 1.5f))
        }
    }

    // Checkered blocky wool carpet rug!
    val rugLeft = w * 0.12f
    val rugTop = h * 0.67f
    val rugW = w * 0.76f
    val rugH = h * 0.24f
    val wool1 = if (palette.isDark) Color(0xFF7A2828) else Color(0xFFC63D3D)
    val wool2 = if (palette.isDark) Color(0xFF9C3434) else Color(0xFFDD5252)
    val woolBorder = Color(0xFFEAE2D0)
    drawRect(woolBorder, Offset(rugLeft - 4f, rugTop - 4f), Size(rugW + 8f, rugH + 8f))
    val rCols = 6
    val rRows = 4
    val rcW = rugW / rCols
    val rcH = rugH / rRows
    for (r in 0 until rRows) {
        for (c in 0 until rCols) {
            drawRect(if ((r + c) % 2 == 0) wool1 else wool2, Offset(rugLeft + c * rcW, rugTop + r * rcH), Size(rcW, rcH))
        }
    }
}

private fun DrawScope.drawGeometryDashShell(
    palette: RoomVisualPalette,
    subTheme: GeometryDashSubTheme
) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.61f
    val (neonPrimary, neonSecondary, neonTertiary) = subTheme.accentNeonColors()

    // 1. Dark atmospheric wall vignette over the level backdrop so furniture pops cleanly
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.wallUpper.copy(alpha = 0.84f),
                palette.wallMain.copy(alpha = 0.66f),
                palette.wallMain.copy(alpha = 0.88f)
            ),
            startY = 0f,
            endY = floorY
        ),
        topLeft = Offset.Zero,
        size = Size(w, floorY)
    )

    // 2. Sub-theme signature Geometry Dash wall motifs
    when (subTheme) {
        GeometryDashSubTheme.SONIC_WAVE -> {
            // Iconic Nine Circles diagonal neon stripes & cyan wave laser
            val stripeW = w * 0.085f
            for (i in -3..12) {
                val x0 = i * stripeW * 1.45f
                val stripePath = Path().apply {
                    moveTo(x0, 0f)
                    lineTo(x0 + stripeW * 0.42f, 0f)
                    lineTo(x0 - h * 0.28f + stripeW * 0.42f, floorY)
                    lineTo(x0 - h * 0.28f, floorY)
                    close()
                }
                drawPath(
                    stripePath,
                    color = if (i % 2 == 0) neonSecondary.copy(alpha = 0.14f) else neonPrimary.copy(alpha = 0.08f)
                )
            }
            // Electric cyan GD wave trail zipping across upper wall
            val wavePath = Path().apply {
                moveTo(0f, h * 0.115f)
                lineTo(w * 0.18f, h * 0.075f)
                lineTo(w * 0.34f, h * 0.125f)
                lineTo(w * 0.52f, h * 0.072f)
                lineTo(w * 0.72f, h * 0.122f)
                lineTo(w * 0.88f, h * 0.080f)
                lineTo(w, h * 0.108f)
            }
            drawPath(wavePath, neonPrimary.copy(alpha = 0.24f), style = Stroke(width = 14f, cap = StrokeCap.Round))
            drawPath(wavePath, neonPrimary.copy(alpha = 0.82f), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
            drawPath(wavePath, Color.White.copy(alpha = 0.75f), style = Stroke(width = 1.4f, cap = StrokeCap.Round))

            // Spinning cobalt sawblade accents near upper corners
            drawGdSawblade(Offset(w * 0.08f, h * 0.14f), w * 0.075f, neonPrimary, neonSecondary)
            drawGdSawblade(Offset(w * 0.92f, h * 0.13f), w * 0.068f, neonPrimary, neonSecondary)
        }

        GeometryDashSubTheme.ASTRAL_DIVINITY -> {
            // Cosmic nebula aura + rainbow prism beams + twinkling celestial stars
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(neonPrimary.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.22f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.5f, h * 0.22f)
            )
            // Subtle vertical rainbow prism columns inspired by Astral Divinity
            val prismColors = listOf(
                Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981),
                Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFFEC4899)
            )
            prismColors.forEachIndexed { idx, col ->
                val x = w * (0.08f + idx * 0.165f)
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(col.copy(alpha = 0.14f), col.copy(alpha = 0.03f), Color.Transparent)
                    ),
                    topLeft = Offset(x, 0f),
                    size = Size(w * 0.045f, floorY * 0.75f)
                )
            }
            // Golden celestial stars & diamond sparkles
            val stars = listOf(
                0.10f to 0.09f, 0.24f to 0.14f, 0.42f to 0.08f, 0.58f to 0.12f,
                0.76f to 0.09f, 0.89f to 0.16f, 0.18f to 0.36f, 0.84f to 0.34f
            )
            stars.forEachIndexed { idx, (sx, sy) ->
                val cx = w * sx
                val cy = h * sy
                val r = if (idx % 2 == 0) w * 0.012f else w * 0.008f
                val starCol = if (idx % 3 == 0) neonTertiary else Color.White
                drawLine(starCol.copy(alpha = 0.8f), Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1.6f)
                drawLine(starCol.copy(alpha = 0.8f), Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 1.6f)
                drawCircle(neonPrimary.copy(alpha = 0.3f), r * 1.4f, Offset(cx, cy))
            }
            drawGdSawblade(Offset(w * 0.91f, h * 0.14f), w * 0.07f, neonPrimary, neonSecondary)
        }

        GeometryDashSubTheme.DEATH_CORRIDOR -> {
            // Iconic black-and-white / crimson diamond wave corridor pattern along upper wall
            val dSize = w * 0.09f
            val cols = (w / dSize).toInt() + 2
            for (c in 0 until cols) {
                val cx = c * dSize
                val cy = h * 0.095f
                val diamond = Path().apply {
                    moveTo(cx, cy - dSize * 0.48f)
                    lineTo(cx + dSize * 0.48f, cy)
                    lineTo(cx, cy + dSize * 0.48f)
                    lineTo(cx - dSize * 0.48f, cy)
                    close()
                }
                drawPath(diamond, Color.White.copy(alpha = if (c % 2 == 0) 0.10f else 0.04f))
                drawPath(diamond, neonPrimary.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
            }
            // Blazing crimson wave spam zigzag
            val dcWave = Path().apply {
                moveTo(0f, h * 0.105f)
                for (i in 1..12) {
                    val x = w * (i / 12f)
                    val y = if (i % 2 == 0) h * 0.122f else h * 0.082f
                    lineTo(x, y)
                }
            }
            drawPath(dcWave, neonPrimary.copy(alpha = 0.28f), style = Stroke(width = 12f, cap = StrokeCap.Round))
            drawPath(dcWave, neonTertiary.copy(alpha = 0.85f), style = Stroke(width = 3.2f, cap = StrokeCap.Round))

            // Crimson hell saws in top corners
            drawGdSawblade(Offset(w * 0.09f, h * 0.15f), w * 0.072f, neonPrimary, Color(0xFF450A0A))
            drawGdSawblade(Offset(w * 0.91f, h * 0.15f), w * 0.072f, neonPrimary, Color(0xFF450A0A))
        }
    }

    // 3. Top neon portal laser rail (seamless full-bleed glow)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(neonPrimary.copy(alpha = 0.30f), Color.Transparent),
            startY = 0f,
            endY = h * 0.18f
        ),
        topLeft = Offset.Zero,
        size = Size(w, h * 0.18f)
    )

    // 4. GD Baseboard + signature Neon Ground Line + decorative GD Spikes along baseboard
    drawRect(palette.baseboard, Offset(0f, floorY - h * 0.020f), Size(w, h * 0.020f))
    // Decorative mini GD spikes resting on baseboard trim
    val spikeW = w * 0.036f
    val spikeH = h * 0.017f
    listOf(0.04f, 0.076f, 0.112f, 0.84f, 0.876f, 0.912f).forEach { sx ->
        val left = w * sx
        val spike = Path().apply {
            moveTo(left, floorY - h * 0.020f)
            lineTo(left + spikeW / 2f, floorY - h * 0.020f - spikeH)
            lineTo(left + spikeW, floorY - h * 0.020f)
            close()
        }
        drawPath(spike, Color(0xFF06070B))
        drawPath(spike, neonPrimary.copy(alpha = 0.88f), style = Stroke(width = 1.6f))
    }

    // Glowing horizontal GD Ground Laser Line
    drawLine(neonPrimary.copy(alpha = 0.35f), Offset(0f, floorY), Offset(w, floorY), strokeWidth = 10f)
    drawLine(Color.White.copy(alpha = 0.92f), Offset(0f, floorY), Offset(w, floorY), strokeWidth = 2.4f)

    // 5. Geometry Dash Neon Perspective Grid Floor!
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.floorMain, palette.wallUpper),
            startY = floorY,
            endY = h
        ),
        topLeft = Offset(0f, floorY + 2f),
        size = Size(w, h - floorY)
    )
    // Horizontal neon grid rows
    listOf(0.665f, 0.725f, 0.795f, 0.875f, 0.955f).forEach { fy ->
        drawLine(
            color = neonPrimary.copy(alpha = 0.32f),
            start = Offset(0f, h * fy),
            end = Offset(w, h * fy),
            strokeWidth = 1.6f
        )
    }
    // Perspective vertical GD tile lines radiating subtly from center horizon
    for (i in -4..4) {
        val topX = w * 0.5f + i * (w * 0.115f)
        val botX = w * 0.5f + i * (w * 0.175f)
        drawLine(
            color = neonPrimary.copy(alpha = 0.24f),
            start = Offset(topX, floorY + 2f),
            end = Offset(botX, h),
            strokeWidth = 1.5f
        )
    }

    // 6. Neon-bordered Geometry Dash Portal Rug
    val rugTopLeft = Offset(w * 0.12f, h * 0.665f)
    val rugSize = Size(w * 0.76f, h * 0.245f)
    drawRoundRect(palette.rugFill.copy(alpha = 0.90f), rugTopLeft, rugSize, CornerRadius(w * 0.025f, w * 0.025f))
    drawRoundRect(
        color = neonPrimary.copy(alpha = 0.72f),
        topLeft = rugTopLeft,
        size = rugSize,
        cornerRadius = CornerRadius(w * 0.025f, w * 0.025f),
        style = Stroke(width = 2.6f)
    )
    drawRoundRect(
        color = neonTertiary.copy(alpha = 0.40f),
        topLeft = Offset(rugTopLeft.x + w * 0.025f, rugTopLeft.y + h * 0.014f),
        size = Size(rugSize.width - w * 0.05f, rugSize.height - h * 0.028f),
        cornerRadius = CornerRadius(w * 0.018f, w * 0.018f),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawGdSawblade(
    center: Offset,
    radius: Float,
    rimColor: Color,
    coreColor: Color
) {
    val teeth = 12
    val sawPath = Path()
    for (i in 0 until teeth * 2) {
        val angle = (i * PI / teeth).toFloat()
        val r = if (i % 2 == 0) radius else radius * 0.74f
        val px = center.x + cos(angle) * r
        val py = center.y + sin(angle) * r
        if (i == 0) sawPath.moveTo(px, py) else sawPath.lineTo(px, py)
    }
    sawPath.close()
    drawPath(sawPath, coreColor.copy(alpha = 0.65f))
    drawPath(sawPath, rimColor.copy(alpha = 0.55f), style = Stroke(width = 1.8f))
    drawCircle(Color(0xFF05060A), radius * 0.42f, center)
    drawCircle(rimColor.copy(alpha = 0.75f), radius * 0.42f, center, style = Stroke(width = 1.6f))
}

private fun DrawScope.drawBaseboardAndWoodFloor(palette: RoomVisualPalette, floorY: Float) {
    val w = size.width
    val h = size.height
    drawRect(palette.baseboard, Offset(0f, floorY - h * 0.018f), Size(w, h * 0.018f))
    drawRect(palette.baseboardShadow, Offset(0f, floorY), Size(w, h * 0.005f))
    drawRect(palette.floorMain, Offset(0f, floorY + h * 0.005f), Size(w, h - floorY))

    val plankAlpha = if (palette.isDark) 0.5f else 0.35f
    val seamColor = palette.floorPlankLine.copy(alpha = plankAlpha)
    val seamStroke = w * 0.0032f
    listOf(0.68f, 0.75f, 0.83f, 0.91f).forEach { fy ->
        drawLine(seamColor, Offset(0f, h * fy), Offset(w, h * fy), strokeWidth = seamStroke)
    }
    listOf(
        Triple(0.26f, 0.615f, 0.68f),
        Triple(0.67f, 0.68f, 0.75f),
        Triple(0.41f, 0.75f, 0.83f),
        Triple(0.78f, 0.83f, 0.91f),
        Triple(0.31f, 0.91f, 1.0f)
    ).forEach { (fx, y1, y2) ->
        drawLine(seamColor.copy(alpha = plankAlpha * 0.75f), Offset(w * fx, h * y1), Offset(w * fx, h * y2), strokeWidth = seamStroke * 0.85f)
    }
}

/**
 * Renders the visual furniture/object illustration directly inside its movable bounding box.
 * Adapts automatically to both [palette] and [roomStyle] (e.g., voxel/blocky furniture in Minecraft style!).
 */
@Composable
fun RoomObjectGraphic(
    visualType: RoomObjectVisual,
    palette: RoomVisualPalette,
    roomStyle: RoomStyleId,
    gdSubTheme: GeometryDashSubTheme = GeometryDashSubTheme.SONIC_WAVE,
    weatherState: WeatherState,
    weatherEnabled: Boolean,
    frameImageBitmap: ImageBitmap?,
    currentMinutesOfDay: Int,
    callPhase: CallPhase = CallPhase.IDLE,
    modifier: Modifier = Modifier
) {
    val isVoxel = roomStyle == RoomStyleId.MINECRAFT
    val isGirly = roomStyle == RoomStyleId.GIRLY_ROOM
    Box(modifier = modifier) {
        when (visualType) {
            RoomObjectVisual.PAINTING -> {
                PaintingGraphic(
                    palette = palette,
                    isVoxel = isVoxel,
                    isGirly = isGirly,
                    gdDefaultDrawableRes = if (roomStyle == RoomStyleId.GEOMETRY_DASH) {
                        gdSubTheme.mainDrawableRes()
                    } else {
                        null
                    },
                    frameImageBitmap = frameImageBitmap,
                    modifier = Modifier.fillMaxSize()
                )
            }
            RoomObjectVisual.WINDOW -> {
                WindowGraphic(
                    palette = palette,
                    isVoxel = isVoxel,
                    isGirly = isGirly,
                    weatherState = weatherState,
                    weatherEnabled = weatherEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Night table with the communicator phone — reacts live to incoming calls
            RoomObjectVisual.NIGHTSTAND -> {
                NightstandGraphic(
                    palette = palette,
                    callPhase = callPhase,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (isVoxel) {
                        when (visualType) {
                            RoomObjectVisual.CLOCK -> drawVoxelClock(currentMinutesOfDay)
                            RoomObjectVisual.STICKY_NOTE -> drawVoxelOakSign()
                            RoomObjectVisual.TV -> drawVoxelTv()
                            RoomObjectVisual.CONSOLE -> drawVoxelCraftingAndJukebox()
                            RoomObjectVisual.SHELF -> drawVoxelBookshelf()
                            RoomObjectVisual.BED -> drawVoxelRedBed()
                            RoomObjectVisual.TRASH_CAN -> drawVoxelLavaCauldron()
                            RoomObjectVisual.SOFA -> drawSofa(palette, isVoxel = true)
                            RoomObjectVisual.FIREPLACE -> drawFireplace(palette, isVoxel = true)
                            RoomObjectVisual.COFFEE_TABLE -> drawCoffeeTable(palette, isVoxel = true)
                            RoomObjectVisual.VINYL_PLAYER -> drawVinylPlayer(palette, isVoxel = true)
                            RoomObjectVisual.FRIDGE -> drawFridge(palette, isVoxel = true)
                            RoomObjectVisual.COFFEE_MAKER -> drawCoffeeMaker(palette, isVoxel = true)
                            RoomObjectVisual.STOVE -> drawStove(palette, isVoxel = true)
                            RoomObjectVisual.KITCHEN_COUNTER -> drawKitchenCounter(palette, isVoxel = true)
                            RoomObjectVisual.GAMING_PC -> drawGamingPc(palette, isVoxel = true)
                            RoomObjectVisual.ARCADE_CABINET -> drawArcadeCabinet(palette, isVoxel = true)
                            RoomObjectVisual.STREAMING_DESK -> drawStreamingDesk(palette, isVoxel = true)
                            RoomObjectVisual.TROPHY_SHELF -> drawTrophyShelf(palette, isVoxel = true)
                            RoomObjectVisual.CUSTOM -> drawCustomObject(palette)
                            else -> Unit
                        }
                    } else {
                        when (visualType) {
                            RoomObjectVisual.CLOCK -> drawClock(palette, currentMinutesOfDay, isGirly = isGirly)
                            RoomObjectVisual.STICKY_NOTE -> drawStickyNote(palette)
                            RoomObjectVisual.TV -> drawTv(palette)
                            RoomObjectVisual.CONSOLE -> drawGameConsole(palette)
                            RoomObjectVisual.SHELF -> drawShelf(
                                palette = palette,
                                isKids = roomStyle == RoomStyleId.KIDS_ROOM,
                                isGirly = isGirly
                            )
                            RoomObjectVisual.BED -> drawBed(
                                palette = palette,
                                isKids = roomStyle == RoomStyleId.KIDS_ROOM,
                                isGirly = isGirly
                            )
                            RoomObjectVisual.TRASH_CAN -> drawTrashCan(palette)
                            RoomObjectVisual.SOFA -> drawSofa(palette, isVoxel = false)
                            RoomObjectVisual.FIREPLACE -> drawFireplace(palette, isVoxel = false)
                            RoomObjectVisual.COFFEE_TABLE -> drawCoffeeTable(palette, isVoxel = false)
                            RoomObjectVisual.VINYL_PLAYER -> drawVinylPlayer(palette, isVoxel = false)
                            RoomObjectVisual.FRIDGE -> drawFridge(palette, isVoxel = false)
                            RoomObjectVisual.COFFEE_MAKER -> drawCoffeeMaker(palette, isVoxel = false)
                            RoomObjectVisual.STOVE -> drawStove(palette, isVoxel = false)
                            RoomObjectVisual.KITCHEN_COUNTER -> drawKitchenCounter(palette, isVoxel = false)
                            RoomObjectVisual.GAMING_PC -> drawGamingPc(palette, isVoxel = false)
                            RoomObjectVisual.ARCADE_CABINET -> drawArcadeCabinet(palette, isVoxel = false)
                            RoomObjectVisual.STREAMING_DESK -> drawStreamingDesk(palette, isVoxel = false)
                            RoomObjectVisual.TROPHY_SHELF -> drawTrophyShelf(palette, isVoxel = false)
                            RoomObjectVisual.CUSTOM -> drawCustomObject(palette)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowGraphic(
    palette: RoomVisualPalette,
    isVoxel: Boolean,
    isGirly: Boolean = false,
    weatherState: WeatherState,
    weatherEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val activeCondition = if (weatherEnabled) weatherState.condition else WeatherCondition.CLEAR_DAY
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.08f
            val padTop = h * 0.06f
            val paneW = w - padX * 2f
            val paneH = h * 0.80f

            val skyColor = when (activeCondition) {
                WeatherCondition.CLEAR_DAY -> if (isVoxel) Color(0xFF78B7FF) else Color(0xFF9ED4EC)
                WeatherCondition.CLEAR_NIGHT -> Color(0xFF192445)
                WeatherCondition.PARTLY_CLOUDY -> Color(0xFFABC9D9)
                WeatherCondition.CLOUDY -> Color(0xFF8C9DA8)
                WeatherCondition.RAIN -> Color(0xFF5D748B)
                WeatherCondition.SNOW -> Color(0xFFB5C7D4)
                WeatherCondition.THUNDERSTORM -> Color(0xFF364256)
            }
            drawRect(skyColor, Offset(padX, padTop), Size(paneW, paneH))

            // Sun / Moon / Weather inside pane (square blocky sun/clouds in Minecraft mode!)
            when (activeCondition) {
                WeatherCondition.CLEAR_DAY -> {
                    if (isVoxel) {
                        val s = min(paneW, paneH) * 0.26f
                        drawRect(Color(0xFFFFEE58), Offset(padX + paneW * 0.58f, padTop + paneH * 0.14f), Size(s, s))
                        drawRect(Color.White.copy(alpha = 0.85f), Offset(padX + paneW * 0.12f, padTop + paneH * 0.22f), Size(paneW * 0.34f, paneH * 0.12f))
                    } else {
                        drawCircle(Color(0xFFFFD86B), min(paneW, paneH) * 0.16f, Offset(padX + paneW * 0.72f, padTop + paneH * 0.28f))
                    }
                }
                WeatherCondition.CLEAR_NIGHT -> {
                    if (isVoxel) {
                        val s = min(paneW, paneH) * 0.24f
                        drawRect(Color(0xFFF6EBB7), Offset(padX + paneW * 0.60f, padTop + paneH * 0.16f), Size(s, s))
                    } else {
                        val moonCenter = Offset(padX + paneW * 0.70f, padTop + paneH * 0.28f)
                        val moonR = min(paneW, paneH) * 0.14f
                        drawCircle(Color(0xFFF6EBB7), moonR, moonCenter)
                        drawCircle(skyColor, moonR * 0.85f, Offset(moonCenter.x + moonR * 0.4f, moonCenter.y - moonR * 0.15f))
                    }
                    listOf(0.22f to 0.24f, 0.44f to 0.18f, 0.28f to 0.62f, 0.76f to 0.68f).forEach { (sx, sy) ->
                        drawRect(Color.White.copy(alpha = 0.85f), Offset(padX + paneW * sx, padTop + paneH * sy), Size(4f, 4f))
                    }
                }
                WeatherCondition.PARTLY_CLOUDY -> {
                    drawCircle(Color(0xFFFFD86B), min(paneW, paneH) * 0.15f, Offset(padX + paneW * 0.72f, padTop + paneH * 0.26f))
                    drawCloud(padX + paneW * 0.24f, padTop + paneH * 0.46f, paneW * 0.46f, Color.White.copy(alpha = 0.88f))
                }
                WeatherCondition.CLOUDY -> {
                    drawCloud(padX + paneW * 0.20f, padTop + paneH * 0.34f, paneW * 0.55f, Color(0xFFDDE5EB))
                    drawCloud(padX + paneW * 0.44f, padTop + paneH * 0.64f, paneW * 0.50f, Color(0xFFCBD5DE))
                }
                WeatherCondition.RAIN -> {
                    drawCloud(padX + paneW * 0.22f, padTop + paneH * 0.26f, paneW * 0.58f, Color(0xFF99AABB))
                    val rainColor = Color(0xFFCDE5F7).copy(alpha = 0.85f)
                    listOf(0.24f to 0.44f, 0.44f to 0.52f, 0.64f to 0.42f, 0.32f to 0.68f, 0.56f to 0.72f).forEach { (rx, ry) ->
                        val start = Offset(padX + paneW * rx, padTop + paneH * ry)
                        drawLine(rainColor, start, Offset(start.x - paneW * 0.06f, start.y + paneH * 0.13f), strokeWidth = min(paneW, paneH) * 0.025f, cap = StrokeCap.Round)
                    }
                }
                WeatherCondition.SNOW -> {
                    drawCloud(padX + paneW * 0.22f, padTop + paneH * 0.24f, paneW * 0.56f, Color(0xFFE7EFF5))
                    listOf(0.22f to 0.48f, 0.42f to 0.56f, 0.66f to 0.46f, 0.28f to 0.74f, 0.54f to 0.76f).forEach { (sx, sy) ->
                        drawCircle(Color.White, min(paneW, paneH) * 0.032f, Offset(padX + paneW * sx, padTop + paneH * sy))
                    }
                }
                WeatherCondition.THUNDERSTORM -> {
                    drawCloud(padX + paneW * 0.20f, padTop + paneH * 0.24f, paneW * 0.62f, Color(0xFF5D6C80))
                    val bolt = Path().apply {
                        moveTo(padX + paneW * 0.54f, padTop + paneH * 0.34f)
                        lineTo(padX + paneW * 0.42f, padTop + paneH * 0.58f)
                        lineTo(padX + paneW * 0.54f, padTop + paneH * 0.58f)
                        lineTo(padX + paneW * 0.44f, padTop + paneH * 0.82f)
                    }
                    drawPath(bolt, Color(0xFFFFE066), style = Stroke(width = min(paneW, paneH) * 0.045f, cap = StrokeCap.Round))
                }
            }

            // Glass pixel glint lines in Minecraft mode
            if (isVoxel) {
                drawLine(Color.White.copy(alpha = 0.6f), Offset(padX + paneW * 0.16f, padTop + paneH * 0.18f), Offset(padX + paneW * 0.28f, padTop + paneH * 0.08f), strokeWidth = 3f)
                drawLine(Color.White.copy(alpha = 0.6f), Offset(padX + paneW * 0.66f, padTop + paneH * 0.76f), Offset(padX + paneW * 0.78f, padTop + paneH * 0.66f), strokeWidth = 3f)
            }

            // Visual Temperature Glass Effects:
            //   < 0°C  -> Frozen glass with icy frost crystals creeping inward from all corners!
            //   0..9°C -> Fogged-up condensation mist & tiny water droplets on the chilly panes!
            //   >= 28°C -> Subtle warm sunlit shimmer on the glass!
            val tempC = if (weatherEnabled) weatherState.temperatureC else null
            if (tempC != null) {
                val centerGlass = Offset(padX + paneW / 2f, padTop + paneH / 2f)
                val maxR = min(paneW, paneH) * 0.85f
                when {
                    tempC < 0 -> {
                        // Icy frozen radial frost glaze thicker at edges
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x22DDF3FF),
                                    Color(0x66C7EBFF),
                                    Color(0xB8E6F7FF)
                                ),
                                center = centerGlass,
                                radius = maxR
                            ),
                            topLeft = Offset(padX, padTop),
                            size = Size(paneW, paneH)
                        )
                        // Crystalline frost fern branches creeping from all 4 corners!
                        val frostColor = Color(0xFFEAF8FF)
                        val frostShadow = Color(0xFF9AD4F5).copy(alpha = 0.7f)
                        val cornerPts = listOf(
                            Offset(padX, padTop) to Offset(1f, 1f),
                            Offset(padX + paneW, padTop) to Offset(-1f, 1f),
                            Offset(padX, padTop + paneH) to Offset(1f, -1f),
                            Offset(padX + paneW, padTop + paneH) to Offset(-1f, -1f)
                        )
                        val branchLen = min(paneW, paneH) * 0.28f
                        cornerPts.forEach { (origin, dir) ->
                            val tip = Offset(origin.x + dir.x * branchLen, origin.y + dir.y * branchLen)
                            drawLine(frostShadow, origin, tip, strokeWidth = 3.2f, cap = StrokeCap.Round)
                            drawLine(frostColor, origin, tip, strokeWidth = 1.6f, cap = StrokeCap.Round)
                            // Side crystalline twigs
                            val mid1 = Offset(origin.x + dir.x * branchLen * 0.45f, origin.y + dir.y * branchLen * 0.45f)
                            val mid2 = Offset(origin.x + dir.x * branchLen * 0.72f, origin.y + dir.y * branchLen * 0.72f)
                            drawLine(frostColor, mid1, Offset(mid1.x + dir.x * branchLen * 0.28f, mid1.y), strokeWidth = 1.3f)
                            drawLine(frostColor, mid1, Offset(mid1.x, mid1.y + dir.y * branchLen * 0.28f), strokeWidth = 1.3f)
                            drawLine(frostColor, mid2, Offset(mid2.x + dir.x * branchLen * 0.18f, mid2.y), strokeWidth = 1.1f)
                            drawLine(frostColor, mid2, Offset(mid2.x, mid2.y + dir.y * branchLen * 0.18f), strokeWidth = 1.1f)
                            // Edge icicle crystal rim
                            drawCircle(frostColor.copy(alpha = 0.72f), radius = branchLen * 0.22f, center = origin)
                        }
                        // Shimmering ice glints on glass
                        listOf(0.20f to 0.18f, 0.80f to 0.24f, 0.24f to 0.82f, 0.76f to 0.78f).forEach { (gx, gy) ->
                            val cx = padX + paneW * gx
                            val cy = padTop + paneH * gy
                            drawLine(Color.White.copy(alpha = 0.9f), Offset(cx - 4f, cy), Offset(cx + 4f, cy), strokeWidth = 1.4f)
                            drawLine(Color.White.copy(alpha = 0.9f), Offset(cx, cy - 4f), Offset(cx, cy + 4f), strokeWidth = 1.4f)
                        }
                    }
                    tempC < 10 -> {
                        // Fogged-up chilly glass condensation mist!
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x18FFFFFF),
                                    Color(0x55EBF4FA),
                                    Color(0x9CE2EEF7)
                                ),
                                center = centerGlass,
                                radius = maxR
                            ),
                            topLeft = Offset(padX, padTop),
                            size = Size(paneW, paneH)
                        )
                        // Extra condensation mist pooling gently along bottom pane sill
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x8CEAF4FC)),
                                startY = padTop + paneH * 0.56f,
                                endY = padTop + paneH
                            ),
                            topLeft = Offset(padX, padTop + paneH * 0.56f),
                            size = Size(paneW, paneH * 0.44f)
                        )
                        // Subtle condensation droplets on the foggy glass
                        val droplets = listOf(
                            Triple(0.18f, 0.74f, 2.3f), Triple(0.32f, 0.84f, 1.8f),
                            Triple(0.68f, 0.78f, 2.4f), Triple(0.82f, 0.68f, 1.9f),
                            Triple(0.24f, 0.30f, 1.6f), Triple(0.75f, 0.28f, 1.7f)
                        )
                        droplets.forEach { (dx, dy, r) ->
                            val pt = Offset(padX + paneW * dx, padTop + paneH * dy)
                            drawCircle(Color(0x668AAEC9), radius = r, center = Offset(pt.x, pt.y + 0.8f))
                            drawCircle(Color.White.copy(alpha = 0.78f), radius = r * 0.82f, center = pt)
                        }
                    }
                    tempC >= 28 -> {
                        // Warm golden sunlit glow on hot days
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x38FFD479), Color.Transparent),
                                startY = padTop,
                                endY = padTop + paneH * 0.65f
                            ),
                            topLeft = Offset(padX, padTop),
                            size = Size(paneW, paneH * 0.65f)
                        )
                    }
                }
            }

            val isBlueGirly = palette.isBlueMood
            val frameColor = when {
                isVoxel -> Color(0xFF5C3E24)
                isGirly && isBlueGirly -> Color(0xFFF6FBFF)
                isGirly -> Color(0xFFFFF6F9)
                else -> palette.woodLight
            }
            val frameStroke = min(w, h) * (if (isVoxel) 0.075f else 0.055f)
            drawRect(frameColor, Offset(padX, padTop), Size(paneW, paneH), style = Stroke(width = frameStroke))
            drawLine(frameColor, Offset(padX + paneW / 2f, padTop), Offset(padX + paneW / 2f, padTop + paneH), strokeWidth = frameStroke * 0.8f)
            drawLine(frameColor, Offset(padX, padTop + paneH / 2f), Offset(padX + paneW, padTop + paneH / 2f), strokeWidth = frameStroke * 0.8f)

            if (isGirly) {
                // Sheer pastel tied-back curtains with satin ribbon tiebacks!
                val curtainColor = if (isBlueGirly) Color(0xCCDEF0FF) else Color(0xCCFFE3EF)
                val curtainFold = if (isBlueGirly) Color(0x9995C6EC) else Color(0x99F3AFCB)
                val tiebackCol = if (isBlueGirly) Color(0xFF5CA6E8) else Color(0xFFEC72A8)
                val leftCurtain = Path().apply {
                    moveTo(padX, padTop)
                    lineTo(padX + paneW * 0.28f, padTop)
                    quadraticTo(padX + paneW * 0.24f, padTop + paneH * 0.42f, padX + paneW * 0.12f, padTop + paneH * 0.56f)
                    quadraticTo(padX + paneW * 0.20f, padTop + paneH * 0.78f, padX + paneW * 0.16f, padTop + paneH)
                    lineTo(padX, padTop + paneH)
                    close()
                }
                val rightCurtain = Path().apply {
                    moveTo(padX + paneW, padTop)
                    lineTo(padX + paneW * 0.72f, padTop)
                    quadraticTo(padX + paneW * 0.76f, padTop + paneH * 0.42f, padX + paneW * 0.88f, padTop + paneH * 0.56f)
                    quadraticTo(padX + paneW * 0.80f, padTop + paneH * 0.78f, padX + paneW * 0.84f, padTop + paneH)
                    lineTo(padX + paneW, padTop + paneH)
                    close()
                }
                drawPath(leftCurtain, curtainColor)
                drawPath(rightCurtain, curtainColor)
                drawPath(leftCurtain, curtainFold, style = Stroke(width = 1.6f))
                drawPath(rightCurtain, curtainFold, style = Stroke(width = 1.6f))
                // Cute satin ribbon tiebacks
                drawLine(tiebackCol, Offset(padX, padTop + paneH * 0.56f), Offset(padX + paneW * 0.14f, padTop + paneH * 0.56f), strokeWidth = 3.2f, cap = StrokeCap.Round)
                drawLine(tiebackCol, Offset(padX + paneW * 0.86f, padTop + paneH * 0.56f), Offset(padX + paneW, padTop + paneH * 0.56f), strokeWidth = 3.2f, cap = StrokeCap.Round)
            }

            drawRoundRect(
                color = when {
                    isVoxel -> Color(0xFF472F1A)
                    isGirly && isBlueGirly -> Color(0xFFCDE3F5)
                    isGirly -> Color(0xFFF3CEDD)
                    else -> palette.woodMid
                },
                topLeft = Offset(w * 0.02f, padTop + paneH - frameStroke * 0.2f),
                size = Size(w * 0.96f, h * 0.09f),
                cornerRadius = CornerRadius(if (isVoxel) 0f else 4f, if (isVoxel) 0f else 4f)
            )
        }

        if (weatherEnabled && weatherState.temperatureC != null) {
            val t = weatherState.temperatureC
            val badgeBg = when {
                t < 0 -> Color(0xE0113B5E)
                t < 10 -> Color(0xDB233E57)
                t >= 28 -> Color(0xDB593112)
                else -> Color(0xD9101621)
            }
            val borderTint = when {
                t < 0 -> Color(0xAA9EDBFF)
                t >= 28 -> Color(0xAAFFD080)
                else -> Color.White.copy(alpha = 0.28f)
            }
            val weatherGlyph = when {
                t < 0 -> "❄"
                activeCondition == WeatherCondition.THUNDERSTORM -> "⛈"
                activeCondition == WeatherCondition.RAIN -> "🌧"
                activeCondition == WeatherCondition.SNOW -> "❄"
                activeCondition == WeatherCondition.CLEAR_NIGHT -> "🌙"
                activeCondition == WeatherCondition.CLOUDY -> "☁"
                activeCondition == WeatherCondition.PARTLY_CLOUDY -> "⛅"
                else -> "☀"
            }
            val label = "$weatherGlyph ${t}°C"
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val badgeFontSize = when {
                    maxWidth >= 105.dp -> 13.5.sp
                    maxWidth >= 78.dp -> 12.sp
                    else -> 10.5.sp
                }
                val shape = RoundedCornerShape(if (isVoxel) 3.dp else 10.dp)
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(badgeBg)
                        .border(1.dp, borderTint, shape)
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = badgeFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCloud(left: Float, top: Float, width: Float, color: Color) {
    val r = width * 0.22f
    drawCircle(color, r * 0.85f, Offset(left + r * 0.9f, top + r * 0.9f))
    drawCircle(color, r * 1.1f, Offset(left + width * 0.46f, top + r * 0.65f))
    drawCircle(color, r * 0.85f, Offset(left + width - r * 0.9f, top + r * 0.95f))
    drawRoundRect(color, Offset(left + r * 0.4f, top + r * 0.75f), Size(width - r * 0.8f, r * 1.0f), CornerRadius(r, r))
}

@Composable
private fun PaintingGraphic(
    palette: RoomVisualPalette,
    isVoxel: Boolean,
    isGirly: Boolean = false,
    gdDefaultDrawableRes: Int? = null,
    frameImageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        // 1. Back layer: realistic drop shadow, beveled outer frame & warm cream gallery mat
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val corner = if (isVoxel) 0f else if (isGirly) 9f else 6f

            // Soft gallery wall shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = if (palette.isDark) 0.44f else 0.16f),
                topLeft = Offset(w * 0.055f, h * 0.065f),
                size = Size(w * 0.92f, h * 0.91f),
                cornerRadius = CornerRadius(corner, corner)
            )
            val isBlueGirly = palette.isBlueMood
            // Symmetrical outer wooden / rose-gold / silver-blue frame
            val frameLeft = w * 0.03f
            val frameTop = h * 0.03f
            val frameW = w * 0.94f
            val frameH = h * 0.94f
            drawRoundRect(
                color = when {
                    isVoxel -> Color(0xFF6B4828)
                    isGirly && isBlueGirly -> Color(0xFF8AB6DB)
                    isGirly -> Color(0xFFD994AE)
                    else -> palette.woodDark
                },
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameW, frameH),
                cornerRadius = CornerRadius(corner, corner)
            )
            // Subtle inner bevel highlight so frame has real depth
            drawRoundRect(
                color = when {
                    isVoxel -> Color(0xFF91673F).copy(alpha = 0.55f)
                    isGirly && isBlueGirly -> Color(0xFFE6F4FF).copy(alpha = 0.88f)
                    isGirly -> Color(0xFFFFE5F0).copy(alpha = 0.85f)
                    else -> palette.woodLight.copy(alpha = 0.55f)
                },
                topLeft = Offset(frameLeft + w * 0.015f, frameTop + h * 0.015f),
                size = Size(frameW - w * 0.03f, frameH - h * 0.03f),
                cornerRadius = CornerRadius((corner - 1f).coerceAtLeast(0f), (corner - 1f).coerceAtLeast(0f)),
                style = Stroke(width = 2f)
            )
            // Symmetrical cream gallery mat
            val matLeft = w * 0.10f
            val matTop = h * 0.10f
            val matW = w * 0.80f
            val matH = h * 0.80f
            drawRect(
                when {
                    isGirly && isBlueGirly -> Color(0xFFF7FBFF)
                    isGirly -> Color(0xFFFFF8FA)
                    else -> Color(0xFFFAF6EE)
                },
                Offset(matLeft, matTop),
                Size(matW, matH)
            )

            // Recessed inner aperture border around the artwork/photo
            val artLeft = w * 0.155f
            val artTop = h * 0.155f
            val artW = w * 0.69f
            val artH = h * 0.69f
            drawRect(
                color = when {
                    isGirly && isBlueGirly -> Color(0xFFC2DDF2)
                    isGirly -> Color(0xFFEBC3D4)
                    else -> Color(0xFFD5CCB9)
                },
                topLeft = Offset(artLeft - 1.5f, artTop - 1.5f),
                size = Size(artW + 3f, artH + 3f)
            )

            if (frameImageBitmap == null && gdDefaultDrawableRes == null) {
                if (isVoxel) {
                    // Iconic blocky pixel landscape painting
                    drawRect(Color(0xFF5FA8D3), Offset(artLeft, artTop), Size(artW, artH * 0.6f))
                    drawRect(Color(0xFF6A994E), Offset(artLeft, artTop + artH * 0.6f), Size(artW, artH * 0.4f))
                    drawRect(Color(0xFFFFD166), Offset(artLeft + artW * 0.65f, artTop + artH * 0.12f), Size(artW * 0.2f, artH * 0.2f))
                    drawRect(Color(0xFF386641), Offset(artLeft + artW * 0.14f, artTop + artH * 0.32f), Size(artW * 0.24f, artH * 0.28f))
                    drawRect(Color(0xFF6F4E37), Offset(artLeft + artW * 0.22f, artTop + artH * 0.56f), Size(artW * 0.08f, artH * 0.22f))
                } else if (isGirly) {
                    // Dreamy pastel sunset & clouds/hills adapted to Baby Blue or Blush Rose
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = if (isBlueGirly) {
                                listOf(Color(0xFFCFE6FF), Color(0xFFE0ECFF), Color(0xFFFFF3D6))
                            } else {
                                listOf(Color(0xFFE7CEFA), Color(0xFFFFCBE0), Color(0xFFFFE5B8))
                            },
                            startY = artTop,
                            endY = artTop + artH
                        ),
                        topLeft = Offset(artLeft, artTop),
                        size = Size(artW, artH)
                    )
                    drawCircle(Color(0xFFFFF2C6), min(artW, artH) * 0.17f, Offset(artLeft + artW * 0.66f, artTop + artH * 0.32f))
                    drawSparkleStar(Offset(artLeft + artW * 0.26f, artTop + artH * 0.24f), min(artW, artH) * 0.06f, Color.White.copy(alpha = 0.9f))
                    val blossomHill = Path().apply {
                        moveTo(artLeft, artTop + artH)
                        lineTo(artLeft, artTop + artH * 0.72f)
                        quadraticTo(artLeft + artW * 0.42f, artTop + artH * 0.50f, artLeft + artW, artTop + artH * 0.76f)
                        lineTo(artLeft + artW, artTop + artH)
                        close()
                    }
                    drawPath(blossomHill, if (isBlueGirly) Color(0xFF8CC2EE) else Color(0xFFE58FB5))
                    val softMist = Path().apply {
                        moveTo(artLeft, artTop + artH)
                        lineTo(artLeft, artTop + artH * 0.84f)
                        quadraticTo(artLeft + artW * 0.60f, artTop + artH * 0.68f, artLeft + artW, artTop + artH * 0.85f)
                        lineTo(artLeft + artW, artTop + artH)
                        close()
                    }
                    drawPath(softMist, if (isBlueGirly) Color(0xFF5D9AD4) else Color(0xFFC26E9B))
                } else {
                    drawRect(if (palette.isDark) Color(0xFF42384A) else Color(0xFFE9C7A6), Offset(artLeft, artTop), Size(artW, artH))
                    drawCircle(if (palette.isDark) Color(0xFFE8A87C) else Color(0xFFD97D54), min(artW, artH) * 0.17f, Offset(artLeft + artW * 0.66f, artTop + artH * 0.32f))
                    val farPeak = Path().apply {
                        moveTo(artLeft, artTop + artH * 0.78f)
                        lineTo(artLeft + artW * 0.38f, artTop + artH * 0.40f)
                        lineTo(artLeft + artW * 0.76f, artTop + artH * 0.78f)
                        close()
                    }
                    drawPath(farPeak, Color(0xFF7B9382))
                    val frontHill = Path().apply {
                        moveTo(artLeft, artTop + artH)
                        lineTo(artLeft, artTop + artH * 0.76f)
                        lineTo(artLeft + artW * 0.52f, artTop + artH * 0.58f)
                        lineTo(artLeft + artW, artTop + artH * 0.80f)
                        lineTo(artLeft + artW, artTop + artH)
                        close()
                    }
                    drawPath(frontHill, Color(0xFF4B5D52))
                }
            }

            // Cute satin ribbon bow perched on top center of the Girly Room frame!
            if (isGirly) {
                drawRibbonBow(
                    center = Offset(w * 0.50f, frameTop + h * 0.01f),
                    scale = min(w, h) * 0.11f,
                    color = if (isBlueGirly) Color(0xFF5CA6E8) else Color(0xFFEC72A8),
                    highlight = if (isBlueGirly) Color(0xFFCFEAFF) else Color(0xFFFFCCE4)
                )
            }
        }

        // 2. Perfectly centered custom photo (or Geometry Dash level art) seated directly inside the museum mat aperture!
        if ((frameImageBitmap != null || gdDefaultDrawableRes != null) && wPx > 0f && hPx > 0f) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (wPx * 0.155f).roundToInt(),
                            y = (hPx * 0.155f).roundToInt()
                        )
                    }
                    .size(
                        width = with(density) { (wPx * 0.69f).toDp() },
                        height = with(density) { (hPx * 0.69f).toDp() }
                    )
                    .clip(RoundedCornerShape(1.5.dp))
            ) {
                if (frameImageBitmap != null) {
                    Image(
                        bitmap = frameImageBitmap,
                        contentDescription = "Picture frame artwork",
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.High,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (gdDefaultDrawableRes != null) {
                    Image(
                        painter = painterResource(id = gdDefaultDrawableRes),
                        contentDescription = "Geometry Dash level artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Delicate gallery glass reflection & top inner-mat shadow so the photo blends naturally into the frame!
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x28000000), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.12f
                        ),
                        size = Size(size.width, size.height * 0.12f)
                    )
                    val sheen = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width * 0.48f, 0f)
                        lineTo(0f, size.height * 0.42f)
                        close()
                    }
                    drawPath(sheen, Color.White.copy(alpha = 0.09f))
                }
            }
        }
    }
}

// ==================== CLASSIC FURNITURE ====================

private fun DrawScope.drawClock(palette: RoomVisualPalette, minutesOfDay: Int, isGirly: Boolean = false) {
    val radius = min(size.width, size.height) * 0.43f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val isBlueGirly = palette.isBlueMood

    drawCircle(Color.Black.copy(alpha = if (palette.isDark) 0.36f else 0.12f), radius, Offset(cx + radius * 0.08f, cy + radius * 0.10f))
    drawCircle(
        when {
            isGirly && isBlueGirly -> Color(0xFF91BCE0)
            isGirly -> Color(0xFFE3A4BC)
            else -> palette.woodDark
        },
        radius,
        Offset(cx, cy)
    )
    if (isGirly) {
        drawCircle(
            if (isBlueGirly) Color(0xFFE3F2FF) else Color(0xFFFFE5F0),
            radius * 0.93f,
            Offset(cx, cy),
            style = Stroke(width = maxOf(1.5f, radius * 0.06f))
        )
    }
    drawCircle(
        when {
            isGirly && isBlueGirly -> Color(0xFFFAFDFF)
            isGirly -> Color(0xFFFFF9FB)
            else -> palette.clockFace
        },
        radius * 0.85f,
        Offset(cx, cy)
    )

    val tickStroke = radius * 0.065f
    for (i in 0 until 12) {
        val angle = Math.toRadians(i * 30.0)
        val inner = if (i % 3 == 0) radius * 0.64f else radius * 0.72f
        val outer = radius * 0.78f
        drawLine(
            color = (when {
                isGirly && isBlueGirly -> Color(0xFF4A98E0)
                isGirly -> Color(0xFFC95C8E)
                else -> palette.clockHands
            }).copy(alpha = if (i % 3 == 0) 0.85f else 0.35f),
            start = Offset(cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat()),
            end = Offset(cx + (cos(angle) * outer).toFloat(), cy + (sin(angle) * outer).toFloat()),
            strokeWidth = if (i % 3 == 0) tickStroke else tickStroke * 0.55f,
            cap = StrokeCap.Round
        )
    }

    val hourAngle = Math.toRadians(((minutesOfDay % 720) / 720.0) * 360.0 - 90.0)
    val minAngle = Math.toRadians(((minutesOfDay % 60) / 60.0) * 360.0 - 90.0)
    val handColor = when {
        isGirly && isBlueGirly -> Color(0xFF26405C)
        isGirly -> Color(0xFF5A3146)
        else -> palette.clockHands
    }
    drawLine(handColor, Offset(cx, cy), Offset(cx + (cos(hourAngle) * radius * 0.45f).toFloat(), cy + (sin(hourAngle) * radius * 0.45f).toFloat()), radius * 0.095f, StrokeCap.Round)
    drawLine(handColor, Offset(cx, cy), Offset(cx + (cos(minAngle) * radius * 0.65f).toFloat(), cy + (sin(minAngle) * radius * 0.65f).toFloat()), radius * 0.065f, StrokeCap.Round)
    drawCircle(
        when {
            isGirly && isBlueGirly -> Color(0xFF5CA6E8)
            isGirly -> Color(0xFFEC72A8)
            else -> palette.clockAccent
        },
        radius * 0.09f,
        Offset(cx, cy)
    )
}

private fun DrawScope.drawStickyNote(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val left = w * 0.08f
    val top = h * 0.14f
    val noteW = w * 0.84f
    val noteH = h * 0.78f
    val fold = min(noteW, noteH) * 0.22f

    drawRoundRect(Color.Black.copy(alpha = if (palette.isDark) 0.34f else 0.13f), Offset(left + w * 0.04f, top + h * 0.05f), Size(noteW, noteH), CornerRadius(4f, 4f))
    val notePath = Path().apply {
        moveTo(left, top)
        lineTo(left + noteW, top)
        lineTo(left + noteW, top + noteH - fold)
        lineTo(left + noteW - fold, top + noteH)
        lineTo(left, top + noteH)
        close()
    }
    drawPath(notePath, palette.stickyNoteFill)
    val foldPath = Path().apply {
        moveTo(left + noteW - fold, top + noteH)
        lineTo(left + noteW - fold, top + noteH - fold)
        lineTo(left + noteW, top + noteH - fold)
        close()
    }
    drawPath(foldPath, palette.stickyNoteFold)

    val ink = Color(0xFF4A3D1F).copy(alpha = 0.55f)
    val lineStroke = maxOf(2f, h * 0.032f)
    listOf(0.38f to 0.72f, 0.56f to 0.80f, 0.74f to 0.54f).forEach { (ly, lw) ->
        drawLine(ink, Offset(left + noteW * 0.14f, top + noteH * ly), Offset(left + noteW * lw, top + noteH * ly), lineStroke, StrokeCap.Round)
    }

    val pinRadius = min(w, h) * 0.085f
    drawCircle(Color.Black.copy(alpha = 0.22f), pinRadius, Offset(left + noteW * 0.52f, top + pinRadius * 0.85f))
    drawCircle(palette.clockAccent, pinRadius, Offset(left + noteW * 0.50f, top + pinRadius * 0.65f))
}

private fun DrawScope.drawTv(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val bezelLeft = w * 0.03f
    val bezelTop = h * 0.04f
    val bezelW = w * 0.94f
    val bezelH = h * 0.82f

    drawRoundRect(Color.Black.copy(alpha = if (palette.isDark) 0.4f else 0.13f), Offset(bezelLeft + w * 0.02f, bezelTop + h * 0.04f), Size(bezelW, bezelH), CornerRadius(12f, 12f))
    drawRoundRect(palette.tvBezel, Offset(bezelLeft, bezelTop), Size(bezelW, bezelH), CornerRadius(12f, 12f))
    val screenLeft = bezelLeft + bezelW * 0.038f
    val screenTop = bezelTop + bezelH * 0.06f
    val screenW = bezelW * 0.924f
    val screenH = bezelH * 0.84f
    drawRect(palette.tvScreen, Offset(screenLeft, screenTop), Size(screenW, screenH))

    val sheen = Path().apply {
        moveTo(screenLeft + screenW * 0.06f, screenTop)
        lineTo(screenLeft + screenW * 0.30f, screenTop)
        lineTo(screenLeft + screenW * 0.12f, screenTop + screenH)
        lineTo(screenLeft, screenTop + screenH)
        lineTo(screenLeft, screenTop + screenH * 0.25f)
        close()
    }
    drawPath(sheen, Color.White.copy(alpha = if (palette.isDark) 0.07f else 0.12f))
    drawCircle(Color(0xFF7CC49E), min(w, h) * 0.018f, Offset(bezelLeft + bezelW * 0.93f, bezelTop + bezelH * 0.95f))
    drawRect(palette.tvBezel, Offset(w * 0.45f, bezelTop + bezelH), Size(w * 0.10f, h * 0.10f))
    drawRoundRect(palette.tvBezel, Offset(w * 0.34f, h * 0.93f), Size(w * 0.32f, h * 0.06f), CornerRadius(4f, 4f))
}

private fun DrawScope.drawGameConsole(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val cabTop = h * 0.06f
    val cabH = h * 0.68f

    drawRoundRect(palette.woodMid, Offset(w * 0.02f, cabTop), Size(w * 0.96f, cabH), CornerRadius(10f, 10f))
    drawRect(palette.woodDark.copy(alpha = 0.72f), Offset(w * 0.06f, cabTop + cabH * 0.14f), Size(w * 0.22f, cabH * 0.72f))
    drawRect(palette.woodDark.copy(alpha = 0.72f), Offset(w * 0.72f, cabTop + cabH * 0.14f), Size(w * 0.22f, cabH * 0.72f))
    drawCircle(palette.woodLight, min(w, h) * 0.03f, Offset(w * 0.24f, cabTop + cabH * 0.50f))
    drawCircle(palette.woodLight, min(w, h) * 0.03f, Offset(w * 0.76f, cabTop + cabH * 0.50f))

    val bayLeft = w * 0.31f
    val bayW = w * 0.38f
    drawRect(Color(0xFF191512), Offset(bayLeft, cabTop + cabH * 0.14f), Size(bayW, cabH * 0.72f))
    val conLeft = bayLeft + bayW * 0.08f
    val conTop = cabTop + cabH * 0.26f
    val conW = bayW * 0.84f
    val conH = cabH * 0.44f
    drawRoundRect(palette.consoleBody, Offset(conLeft, conTop), Size(conW, conH), CornerRadius(6f, 6f))
    drawRoundRect(palette.consoleAccent, Offset(conLeft + conW * 0.12f, conTop + conH * 0.38f), Size(conW * 0.52f, conH * 0.18f), CornerRadius(3f, 3f))
    drawCircle(Color.White.copy(alpha = 0.85f), conH * 0.13f, Offset(conLeft + conW * 0.82f, conTop + conH * 0.48f))

    val leftLeg = Path().apply {
        moveTo(w * 0.09f, cabTop + cabH); lineTo(w * 0.13f, cabTop + cabH)
        lineTo(w * 0.10f, h * 0.96f); lineTo(w * 0.06f, h * 0.96f); close()
    }
    val rightLeg = Path().apply {
        moveTo(w * 0.87f, cabTop + cabH); lineTo(w * 0.91f, cabTop + cabH)
        lineTo(w * 0.94f, h * 0.96f); lineTo(w * 0.90f, h * 0.96f); close()
    }
    drawPath(leftLeg, palette.woodDark)
    drawPath(rightLeg, palette.woodDark)
}

private fun DrawScope.drawShelf(palette: RoomVisualPalette, isKids: Boolean, isGirly: Boolean = false) {
    val w = size.width
    val h = size.height
    val upperY = h * 0.40f
    val shelfH = h * 0.065f
    val isBlueGirly = palette.isBlueMood
    val shelfColor = when {
        isKids -> Color(0xFFFFB703)
        isGirly && isBlueGirly -> Color(0xFFF5FAFF)
        isGirly -> Color(0xFFFFF5F9)
        else -> palette.woodMid
    }

    drawRect(Color.Black.copy(alpha = 0.16f), Offset(w * 0.06f, upperY + shelfH), Size(w * 0.90f, h * 0.03f))
    drawRoundRect(shelfColor, Offset(w * 0.04f, upperY), Size(w * 0.92f, shelfH), CornerRadius(4f, 4f))

    drawRect(
        color = when {
            isKids -> Color(0xFFFF6B6B)
            isGirly && isBlueGirly -> Color(0xFF8CC5F2)
            isGirly -> Color(0xFFF49AC1)
            else -> palette.duvetPrimary
        },
        topLeft = Offset(w * 0.09f, h * 0.13f),
        size = Size(w * 0.09f, upperY - h * 0.13f)
    )
    drawRect(
        color = when {
            isKids -> Color(0xFF06D6A0)
            isGirly && isBlueGirly -> Color(0xFFB3C3FA)
            isGirly -> Color(0xFFCBB2F6)
            else -> palette.throwBlanket
        },
        topLeft = Offset(w * 0.20f, h * 0.18f),
        size = Size(w * 0.08f, upperY - h * 0.18f)
    )
    drawRect(
        color = when {
            isKids -> Color(0xFF118AB2)
            isGirly && isBlueGirly -> Color(0xFFFFE8B8)
            isGirly -> Color(0xFFFFE0B5)
            else -> Color(0xFFE2B97B)
        },
        topLeft = Offset(w * 0.30f, h * 0.10f),
        size = Size(w * 0.09f, upperY - h * 0.10f)
    )
    val leaning = Path().apply {
        moveTo(w * 0.41f, upperY); lineTo(w * 0.48f, h * 0.16f); lineTo(w * 0.55f, h * 0.18f); lineTo(w * 0.48f, upperY); close()
    }
    drawPath(
        leaning,
        when {
            isKids -> Color(0xFF9B5DE5)
            isGirly -> Color(0xFFB5EAD7)
            else -> Color(0xFF596E79)
        }
    )
    val pot = Path().apply {
        moveTo(w * 0.72f, h * 0.28f); lineTo(w * 0.88f, h * 0.28f); lineTo(w * 0.85f, upperY); lineTo(w * 0.75f, upperY); close()
    }
    drawPath(
        pot,
        when {
            isGirly && isBlueGirly -> Color(0xFFC2DFF7)
            isGirly -> Color(0xFFF6C6DA)
            else -> Color(0xFFC66E4E)
        }
    )
    if (isGirly) {
        // Blooming pastel bouquet in ceramic vase
        drawCircle(Color(0xFF7CB88E), w * 0.055f, Offset(w * 0.80f, h * 0.24f))
        if (isBlueGirly) {
            drawCircle(Color(0xFF6BB5F0), w * 0.042f, Offset(w * 0.77f, h * 0.20f))
            drawCircle(Color(0xFFD6EEFF), w * 0.038f, Offset(w * 0.83f, h * 0.19f))
        } else {
            drawCircle(Color(0xFFFF7EB6), w * 0.042f, Offset(w * 0.77f, h * 0.20f))
            drawCircle(Color(0xFFFFB5D6), w * 0.038f, Offset(w * 0.83f, h * 0.19f))
        }
    } else {
        drawCircle(Color(0xFF5D8768), w * 0.065f, Offset(w * 0.80f, h * 0.22f))
    }

    // Collectible hero figurine on the shelf — only in the blue moods (Baby Blue / Deep Sapphire)
    if (palette.isBlueMood) {
        drawMiniDivaFigurine(
            palette = palette,
            baseCenter = Offset(w * 0.635f, upperY),
            height = h * 0.175f
        )
    }

    val lowerY = h * 0.86f
    drawRect(Color.Black.copy(alpha = 0.16f), Offset(w * 0.06f, lowerY + shelfH), Size(w * 0.90f, h * 0.03f))
    drawRoundRect(shelfColor, Offset(w * 0.04f, lowerY), Size(w * 0.92f, shelfH), CornerRadius(4f, 4f))

    if (isGirly) {
        // Pastel ribbon jewelry box on left
        drawRoundRect(
            if (isBlueGirly) Color(0xFFA2CFF4) else Color(0xFFF3B0CF),
            Offset(w * 0.09f, h * 0.64f),
            Size(w * 0.24f, lowerY - h * 0.64f),
            CornerRadius(5f, 5f)
        )
        val boxRibbon = if (isBlueGirly) Color(0xFFE6F4FF) else Color(0xFFFFE5F0)
        drawRect(boxRibbon, Offset(w * 0.195f, h * 0.64f), Size(w * 0.03f, lowerY - h * 0.64f))
        drawRect(boxRibbon, Offset(w * 0.09f, h * 0.73f), Size(w * 0.24f, h * 0.035f))
        // Stacked pastel journals in middle
        drawRect(if (isBlueGirly) Color(0xFFC2CEFA) else Color(0xFFD6B8FA), Offset(w * 0.38f, h * 0.79f), Size(w * 0.34f, lowerY - h * 0.79f))
        drawRect(if (isBlueGirly) Color(0xFF8CC5F2) else Color(0xFFF7A1C4), Offset(w * 0.41f, h * 0.72f), Size(w * 0.28f, h * 0.07f))
        // Lit scented candle on right with real radial warm candlelight halo & teardrop flame!
        val flameCenter = Offset(w * 0.84f, h * 0.635f)
        val candleHaloR = w * 0.18f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE57F).copy(alpha = if (palette.isDark) 0.78f else 0.60f),
                    Color(0xFFFFB703).copy(alpha = if (palette.isDark) 0.34f else 0.24f),
                    Color.Transparent
                ),
                center = flameCenter,
                radius = candleHaloR
            ),
            radius = candleHaloR,
            center = flameCenter
        )
        drawRoundRect(if (isBlueGirly) Color(0xFFF6FBFF) else Color(0xFFFFF4F8), Offset(w * 0.77f, h * 0.70f), Size(w * 0.14f, lowerY - h * 0.70f), CornerRadius(4f, 4f))
        drawRect(if (isBlueGirly) Color(0xFF7CB7EB) else Color(0xFFF49AC1), Offset(w * 0.79f, h * 0.76f), Size(w * 0.10f, h * 0.055f))
        // Wick + outer amber flame + bright white-gold core
        drawLine(Color(0xFF4A3728), Offset(w * 0.84f, h * 0.70f), Offset(w * 0.84f, h * 0.665f), strokeWidth = 1.6f)
        val candleFlame = Path().apply {
            moveTo(w * 0.84f, h * 0.675f)
            quadraticTo(w * 0.805f, h * 0.64f, w * 0.84f, h * 0.595f)
            quadraticTo(w * 0.875f, h * 0.64f, w * 0.84f, h * 0.675f)
            close()
        }
        drawPath(candleFlame, Color(0xFFFFA726))
        drawCircle(Color(0xFFFFF8DC), w * 0.017f, Offset(w * 0.84f, h * 0.648f))
    } else {
        drawRect(if (isKids) Color(0xFF118AB2) else Color(0xFF3E4E56), Offset(w * 0.09f, h * 0.60f), Size(w * 0.24f, lowerY - h * 0.60f))
        drawRect(Color(0xFFFAF7F2), Offset(w * 0.14f, h * 0.69f), Size(w * 0.14f, h * 0.04f))
        drawRect(Color(0xFFD9A76A), Offset(w * 0.38f, h * 0.79f), Size(w * 0.34f, lowerY - h * 0.79f))
        drawRect(if (isKids) Color(0xFFFF6B6B) else palette.duvetPrimary, Offset(w * 0.41f, h * 0.72f), Size(w * 0.28f, h * 0.07f))
        drawRect(Color(0xFFF0E5D6), Offset(w * 0.78f, h * 0.71f), Size(w * 0.12f, lowerY - h * 0.71f))
        drawRect(palette.throwBlanket, Offset(w * 0.81f, h * 0.63f), Size(w * 0.025f, h * 0.08f))
        drawRect(palette.duvetPrimary, Offset(w * 0.855f, h * 0.65f), Size(w * 0.02f, h * 0.06f))
    }
}

private fun DrawScope.drawBed(palette: RoomVisualPalette, isKids: Boolean, isGirly: Boolean = false) {
    val w = size.width
    val h = size.height
    val isBlueGirly = palette.isBlueMood

    drawRoundRect(Color.Black.copy(alpha = if (palette.isDark) 0.40f else 0.16f), Offset(w * 0.04f, h * 0.90f), Size(w * 0.92f, h * 0.09f), CornerRadius(10f, 10f))
    if (isGirly) {
        // Curved tufted pastel velvet headboard with pearl buttons!
        val headboardColor = when {
            palette.isDark -> Color(0xFF6A4175)
            isBlueGirly -> Color(0xFFB3D6F5)
            else -> Color(0xFFF2B6CF)
        }
        val headboardTrim = when {
            palette.isDark -> Color(0xFFE5A4CB)
            isBlueGirly -> Color(0xFFF6FBFF)
            else -> Color(0xFFFFF6FA)
        }
        drawRoundRect(headboardColor, Offset(w * 0.03f, h * 0.02f), Size(w * 0.86f, h * 0.35f), CornerRadius(w * 0.12f, w * 0.12f))
        drawRoundRect(headboardTrim, Offset(w * 0.055f, h * 0.045f), Size(w * 0.81f, h * 0.28f), CornerRadius(w * 0.10f, w * 0.10f), style = Stroke(width = 2.2f))
        // Pearl button tufting dots
        listOf(0.20f, 0.38f, 0.56f, 0.74f).forEach { tx ->
            drawCircle(headboardTrim.copy(alpha = 0.88f), w * 0.015f, Offset(w * tx, h * 0.13f))
        }
    } else {
        drawRoundRect(if (isKids) Color(0xFF118AB2) else palette.woodDark, Offset(w * 0.03f, h * 0.03f), Size(w * 0.86f, h * 0.34f), CornerRadius(14f, 14f))
        drawRect(Color.Black.copy(alpha = 0.14f), Offset(w * 0.07f, h * 0.11f), Size(w * 0.78f, h * 0.022f))
        drawRect(Color.Black.copy(alpha = 0.14f), Offset(w * 0.07f, h * 0.19f), Size(w * 0.78f, h * 0.022f))
    }

    drawRoundRect(palette.pillowColor.copy(alpha = 0.94f), Offset(w * 0.03f, h * 0.30f), Size(w * 0.94f, h * 0.50f), CornerRadius(10f, 10f))
    drawRoundRect(palette.pillowColor, Offset(w * 0.07f, h * 0.21f), Size(w * 0.38f, h * 0.18f), CornerRadius(14f, 14f))
    drawRoundRect(palette.pillowColor.copy(alpha = 0.90f), Offset(w * 0.48f, h * 0.21f), Size(w * 0.38f, h * 0.18f), CornerRadius(14f, 14f))
    if (isGirly) {
        // Cute plush heart throw pillow right in the middle!
        drawTinyHeart(
            Offset(w * 0.47f, h * 0.32f),
            min(w, h) * 0.115f,
            if (isBlueGirly) Color(0xFF5CA6E8) else Color(0xFFEC72A8)
        )
        drawTinyHeart(
            Offset(w * 0.46f, h * 0.305f),
            min(w, h) * 0.055f,
            if (isBlueGirly) Color(0xFFB8E1FF) else Color(0xFFFFB5D6)
        )
    }
    drawRect(palette.pillowColor.copy(alpha = 0.82f), Offset(w * 0.03f, h * 0.39f), Size(w * 0.94f, h * 0.05f))

    drawRect(if (isKids) Color(0xFF2EC4B6) else palette.duvetPrimary, Offset(w * 0.03f, h * 0.43f), Size(w * 0.94f, h * 0.37f))
    if (isKids) {
        // Cheerful stars on kids duvet
        listOf(0.22f to 0.52f, 0.50f to 0.55f, 0.78f to 0.51f).forEach { (sx, sy) ->
            drawCircle(Color(0xFFFFD166), w * 0.032f, Offset(w * sx, h * sy))
        }
    } else if (isGirly) {
        // Delicate cloud-cream lace ruffle trim along top edge of duvet + subtle sparkle dots
        val ruffleCount = 12
        val ruffleStep = (w * 0.94f) / ruffleCount
        for (i in 0..ruffleCount) {
            drawCircle(Color(0xFFFFFAFC).copy(alpha = 0.90f), ruffleStep * 0.42f, Offset(w * 0.03f + i * ruffleStep, h * 0.43f))
        }
        listOf(0.20f to 0.53f, 0.48f to 0.56f, 0.76f to 0.52f).forEach { (sx, sy) ->
            drawSparkleStar(Offset(w * sx, h * sy), w * 0.022f, Color.White.copy(alpha = 0.65f))
        }
    }
    drawRect(if (isKids) Color(0xFFFF6B6B) else palette.throwBlanket, Offset(w * 0.03f, h * 0.64f), Size(w * 0.94f, h * 0.16f))
    val baseFrameColor = when {
        isKids -> Color(0xFF118AB2)
        isGirly && palette.isDark -> Color(0xFF5D3A69)
        isGirly && isBlueGirly -> Color(0xFFA2C7E8)
        isGirly -> Color(0xFFE7B6C7)
        else -> palette.woodDark
    }
    drawRect(baseFrameColor, Offset(w * 0.02f, h * 0.79f), Size(w * 0.96f, h * 0.085f))
    drawRect(if (isKids) Color(0xFF0B6887) else baseFrameColor, Offset(w * 0.06f, h * 0.875f), Size(w * 0.055f, h * 0.10f))
    drawRect(if (isKids) Color(0xFF0B6887) else baseFrameColor, Offset(w * 0.885f, h * 0.875f), Size(w * 0.055f, h * 0.10f))
}

private fun DrawScope.drawTrashCan(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    drawOval(Color.Black.copy(alpha = if (palette.isDark) 0.42f else 0.18f), Offset(w * 0.14f, h * 0.84f), Size(w * 0.72f, h * 0.13f))
    val body = Path().apply {
        moveTo(w * 0.14f, h * 0.18f); lineTo(w * 0.86f, h * 0.18f)
        lineTo(w * 0.77f, h * 0.89f); lineTo(w * 0.23f, h * 0.89f); close()
    }
    drawPath(body, palette.trashBody)
    val rib = Color.White.copy(alpha = 0.16f)
    val ribW = maxOf(2f, w * 0.025f)
    drawLine(rib, Offset(w * 0.31f, h * 0.28f), Offset(w * 0.35f, h * 0.82f), ribW, StrokeCap.Round)
    drawLine(rib, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.50f, h * 0.83f), ribW, StrokeCap.Round)
    drawLine(rib, Offset(w * 0.69f, h * 0.28f), Offset(w * 0.65f, h * 0.82f), ribW, StrokeCap.Round)
    drawOval(Color(0xFF1F2426), Offset(w * 0.14f, h * 0.11f), Size(w * 0.72f, h * 0.15f))
    drawOval(palette.trashRim, Offset(w * 0.14f, h * 0.11f), Size(w * 0.72f, h * 0.15f), style = Stroke(width = min(w, h) * 0.038f))
}

// ==================== NIGHT TABLE & CALL-REACTIVE PHONE ====================

/**
 * Bedroom night table with a bedside lamp and a phone that carries every communicator app.
 * The phone fully reacts to real calls: it shakes, bursts light rings and shows a big
 * answer / decline screen while [CallPhase.RINGING], and a calm glowing call indicator
 * while [CallPhase.ONGOING].
 */
@Composable
private fun NightstandGraphic(
    palette: RoomVisualPalette,
    callPhase: CallPhase,
    modifier: Modifier = Modifier
) {
    // Only animate while a call is actually happening — no idle battery drain.
    val shake: Float
    val ring: Float
    if (callPhase == CallPhase.IDLE) {
        shake = 0.5f
        ring = 0f
    } else {
        val transition = rememberInfiniteTransition(label = "nightstand_call")
        val animatedShake by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 240, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nightstand_shake"
        )
        val animatedRing by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "nightstand_ring"
        )
        shake = animatedShake
        ring = animatedRing
    }

    Canvas(modifier = modifier) {
        drawNightstand(palette = palette, callPhase = callPhase, shake = shake, ring = ring)
    }
}

private fun DrawScope.drawNightstand(
    palette: RoomVisualPalette,
    callPhase: CallPhase,
    shake: Float,
    ring: Float
) {
    val w = size.width
    val h = size.height
    val tableTopY = h * 0.52f

    val woodTop = palette.woodLight
    val woodBody = palette.woodMid
    val woodShade = palette.woodDark

    // Soft floor shadow
    drawOval(
        color = Color.Black.copy(alpha = if (palette.isDark) 0.40f else 0.16f),
        topLeft = Offset(w * 0.03f, h * 0.90f),
        size = Size(w * 0.94f, h * 0.10f)
    )

    // Legs
    listOf(0.16f, 0.80f).forEach { lx ->
        drawRoundRect(
            color = woodShade,
            topLeft = Offset(w * lx, h * 0.855f),
            size = Size(w * 0.055f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.018f, w * 0.018f)
        )
    }

    // Cabinet body
    drawRoundRect(
        color = woodBody,
        topLeft = Offset(w * 0.08f, tableTopY),
        size = Size(w * 0.84f, h * 0.365f),
        cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
    )
    // Drawer face + knob
    drawRoundRect(
        color = woodTop.copy(alpha = 0.55f),
        topLeft = Offset(w * 0.14f, tableTopY + h * 0.045f),
        size = Size(w * 0.72f, h * 0.145f),
        cornerRadius = CornerRadius(w * 0.030f, w * 0.030f)
    )
    drawCircle(
        color = palette.clockAccent.copy(alpha = 0.85f),
        radius = w * 0.030f,
        center = Offset(w * 0.50f, tableTopY + h * 0.118f)
    )
    // Open shelf below the drawer
    drawRoundRect(
        color = woodShade.copy(alpha = 0.55f),
        topLeft = Offset(w * 0.15f, tableTopY + h * 0.215f),
        size = Size(w * 0.70f, h * 0.105f),
        cornerRadius = CornerRadius(w * 0.020f, w * 0.020f)
    )
    // Tabletop slab (drawn last so lamp & phone sit on it)
    drawRoundRect(
        color = woodTop,
        topLeft = Offset(w * 0.05f, tableTopY - h * 0.055f),
        size = Size(w * 0.90f, h * 0.075f),
        cornerRadius = CornerRadius(w * 0.030f, w * 0.030f)
    )
    drawLine(
        color = Color.White.copy(alpha = 0.30f),
        start = Offset(w * 0.09f, tableTopY - h * 0.045f),
        end = Offset(w * 0.91f, tableTopY - h * 0.045f),
        strokeWidth = maxOf(1f, h * 0.008f)
    )

    // ---- Big bedside lamp: tall stem, wide shade and a generous warm glow ----
    val lampX = w * 0.245f
    val lampBaseY = tableTopY - h * 0.045f
    val shadeTop = h * 0.055f
    val shadeBottom = h * 0.275f
    val shadeHalfTop = w * 0.095f
    val shadeHalfBottom = w * 0.185f
    val lampGlow = Offset(lampX, shadeBottom + h * 0.02f)
    // Wide ambient halo filling the corner of the room
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE9A8).copy(alpha = if (palette.isDark) 0.78f else 0.60f),
                Color(0xFFFFC46B).copy(alpha = if (palette.isDark) 0.34f else 0.22f),
                Color.Transparent
            ),
            center = lampGlow,
            radius = w * 0.78f
        ),
        radius = w * 0.78f,
        center = lampGlow
    )
    // Warm light pool spreading across the tabletop
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE3A0).copy(alpha = 0.58f),
                Color(0xFFFFD07A).copy(alpha = 0.22f),
                Color.Transparent
            ),
            center = Offset(lampX, lampBaseY),
            radius = w * 0.62f
        ),
        topLeft = Offset(lampX - w * 0.62f, lampBaseY - h * 0.10f),
        size = Size(w * 1.24f, h * 0.13f)
    )
    // Chunky ceramic base + brushed stem
    drawOval(
        color = woodShade,
        topLeft = Offset(lampX - w * 0.125f, lampBaseY - h * 0.085f),
        size = Size(w * 0.25f, h * 0.085f)
    )
    drawOval(
        color = palette.clockAccent.copy(alpha = 0.45f),
        topLeft = Offset(lampX - w * 0.095f, lampBaseY - h * 0.10f),
        size = Size(w * 0.19f, h * 0.06f)
    )
    drawRoundRect(
        color = palette.clockAccent.copy(alpha = 0.80f),
        topLeft = Offset(lampX - w * 0.020f, shadeBottom - h * 0.005f),
        size = Size(w * 0.040f, lampBaseY - shadeBottom - h * 0.05f),
        cornerRadius = CornerRadius(w * 0.018f, w * 0.018f)
    )
    // Glowing bulb peeking under the shade
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFDF0), Color(0xFFFFE08A), Color.Transparent),
            center = Offset(lampX, shadeBottom + h * 0.010f),
            radius = w * 0.12f
        ),
        radius = w * 0.12f,
        center = Offset(lampX, shadeBottom + h * 0.010f)
    )
    drawCircle(Color(0xFFFFFBE8), radius = w * 0.045f, center = Offset(lampX, shadeBottom + h * 0.006f))
    // Trapezoid lampshade with a warm gradient interior
    val shade = Path().apply {
        moveTo(lampX - shadeHalfTop, shadeTop)
        lineTo(lampX + shadeHalfTop, shadeTop)
        lineTo(lampX + shadeHalfBottom, shadeBottom)
        lineTo(lampX - shadeHalfBottom, shadeBottom)
        close()
    }
    drawPath(
        path = shade,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF6DE), Color(0xFFFFD98F), Color(0xFFFFB74D)),
            startY = shadeTop,
            endY = shadeBottom
        )
    )
    drawPath(shade, Color(0x99FFA726), style = Stroke(width = maxOf(1.2f, w * 0.010f)))
    // Fabric ribs on the shade
    listOf(0.30f, 0.55f, 0.80f).forEach { fraction ->
        val ry = shadeTop + (shadeBottom - shadeTop) * fraction
        val halfW = shadeHalfTop + (shadeHalfBottom - shadeHalfTop) * fraction
        drawLine(
            color = Color(0x33B26A00),
            start = Offset(lampX - halfW * 0.94f, ry),
            end = Offset(lampX + halfW * 0.94f, ry),
            strokeWidth = maxOf(1f, h * 0.005f)
        )
    }
    // Bright bottom rim
    drawLine(
        color = Color(0xFFFFFBE8),
        start = Offset(lampX - shadeHalfBottom, shadeBottom),
        end = Offset(lampX + shadeHalfBottom, shadeBottom),
        strokeWidth = maxOf(2f, h * 0.016f)
    )
    drawRoundRect(
        color = woodShade,
        topLeft = Offset(lampX - shadeHalfTop * 0.9f, shadeTop - h * 0.030f),
        size = Size(shadeHalfTop * 1.8f, h * 0.030f),
        cornerRadius = CornerRadius(h * 0.014f, h * 0.014f)
    )

    // ---- Classic retro telephone (handset, keypad, coiled cord) ----
    drawClassicTelephone(
        palette = palette,
        callPhase = callPhase,
        shake = shake,
        ring = ring,
        left = w * 0.42f,
        right = w * 1.00f,
        baseBottomY = tableTopY - h * 0.045f
    )
}

/**
 * Classic desk telephone — glossy shell, resting handset, 3×4 keypad, lit display and a
 * coiled cord trailing off the table. Rings (wobble + sound arcs + pulsing halo) when a
 * call comes in and glows green with a waveform while the call is connected.
 */
private fun DrawScope.drawClassicTelephone(
    palette: RoomVisualPalette,
    callPhase: CallPhase,
    shake: Float,
    ring: Float,
    left: Float,
    right: Float,
    baseBottomY: Float
) {
    val isRinging = callPhase == CallPhase.RINGING
    val isOnCall = callPhase == CallPhase.ONGOING
    val activeColor = when {
        isRinging -> Color(0xFF4FD8FF)
        isOnCall -> Color(0xFF4ADE80)
        else -> palette.consoleAccent
    }

    val bodyWidth = right - left
    val bodyHeight = bodyWidth * 0.44f
    val bodyTop = baseBottomY - bodyHeight
    val cx = (left + right) / 2f
    val handsetHeight = bodyHeight * 0.78f
    val phoneCenter = Offset(cx, bodyTop - handsetHeight * 0.35f)

    // Halo spilling from the phone (pulses while ringing)
    val haloStrength = when {
        isRinging -> 0.55f + 0.22f * ring
        isOnCall -> 0.38f
        else -> 0.16f
    }
    val haloRadius = bodyWidth * (if (isRinging) 0.95f + 0.10f * ring else 0.72f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                activeColor.copy(alpha = haloStrength),
                activeColor.copy(alpha = haloStrength * 0.35f),
                Color.Transparent
            ),
            center = phoneCenter,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = phoneCenter
    )

    // Expanding "brrring" rings while it rings
    if (isRinging) {
        repeat(2) { index ->
            val t = ((ring + index * 0.5f) % 1f)
            drawCircle(
                color = activeColor.copy(alpha = (1f - t) * 0.38f),
                radius = bodyWidth * (0.42f + t * 0.60f),
                center = phoneCenter,
                style = Stroke(width = maxOf(1.2f, bodyWidth * 0.030f * (1f - t * 0.6f)))
            )
        }
    }

    // The whole set rattles on its feet while ringing
    val wobble = if (isRinging) (shake - 0.5f) * 10f else 0f
    rotate(degrees = wobble, pivot = phoneCenter) {

        // Coiled cord looping from the shell down onto the tabletop
        val cordStart = Offset(right - bodyWidth * 0.06f, bodyTop + bodyHeight * 0.55f)
        val cordPath = Path().apply {
            moveTo(cordStart.x, cordStart.y)
            var py = cordStart.y
            var px = cordStart.x
            repeat(6) { loop ->
                val nextX = px - bodyWidth * (0.055f + 0.012f * loop)
                val nextY = py + bodyHeight * 0.16f
                quadraticTo(px - bodyWidth * 0.075f, py + bodyHeight * 0.08f, nextX, nextY)
                quadraticTo(px - bodyWidth * 0.020f, py + bodyHeight * 0.09f, nextX, nextY + bodyHeight * 0.02f)
                px = nextX
                py = nextY + bodyHeight * 0.02f
            }
        }
        drawPath(
            path = cordPath,
            color = Color(0xFF2B3440),
            style = Stroke(width = maxOf(1.5f, bodyHeight * 0.075f), cap = StrokeCap.Round)
        )
        drawPath(
            path = cordPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(width = maxOf(1f, bodyHeight * 0.028f), cap = StrokeCap.Round)
        )

        // Telephone shell (slightly wider at the front like a classic desk set)
        val shell = Path().apply {
            moveTo(left, baseBottomY)
            lineTo(left + bodyWidth * 0.06f, bodyTop)
            lineTo(right - bodyWidth * 0.06f, bodyTop)
            lineTo(right, baseBottomY)
            close()
        }
        drawPath(
            path = shell,
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.woodLight,
                    palette.woodMid,
                    palette.woodDark
                ),
                startY = bodyTop,
                endY = baseBottomY
            )
        )
        drawPath(
            shell,
            activeColor.copy(alpha = if (isRinging) 0.85f else 0.35f),
            style = Stroke(width = maxOf(1.2f, bodyHeight * 0.045f))
        )
        // Glossy top highlight
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(left + bodyWidth * 0.12f, bodyTop + bodyHeight * 0.06f),
            size = Size(bodyWidth * 0.22f, bodyHeight * 0.10f),
            cornerRadius = CornerRadius(bodyHeight * 0.05f, bodyHeight * 0.05f)
        )

        // Lit display window above the keypad
        val displayW = bodyWidth * 0.52f
        val displayH = bodyHeight * 0.19f
        val displayLeft = cx - displayW / 2f
        val displayTop = bodyTop + bodyHeight * 0.16f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = if (isRinging) {
                    listOf(Color(0xFF9FF0FF), Color(0xFF3FC5F2))
                } else if (isOnCall) {
                    listOf(Color(0xFFB9F6D0), Color(0xFF34D399))
                } else {
                    listOf(Color(0xFF3A4A5E), Color(0xFF223040))
                },
                startY = displayTop,
                endY = displayTop + displayH
            ),
            topLeft = Offset(displayLeft, displayTop),
            size = Size(displayW, displayH),
            cornerRadius = CornerRadius(displayH * 0.28f, displayH * 0.28f)
        )
        if (isRinging) {
            // Blinking "incoming" bars on the display
            repeat(3) { i ->
                drawRoundRect(
                    color = Color(0xFF0B2536).copy(alpha = 0.75f),
                    topLeft = Offset(
                        displayLeft + displayW * (0.14f + i * 0.26f),
                        displayTop + displayH * 0.32f
                    ),
                    size = Size(displayW * 0.16f, displayH * 0.36f),
                    cornerRadius = CornerRadius(displayH * 0.10f, displayH * 0.10f)
                )
            }
        } else if (isOnCall) {
            // Live-looking call waveform on the display
            val bars = 5
            for (i in 0 until bars) {
                val phase = (ring + i * 0.21f) % 1f
                val barH = displayH * (0.20f + 0.55f * kotlin.math.abs(0.5f - phase) * 2f)
                val bx = displayLeft + displayW * 0.12f + i * (displayW * 0.16f)
                drawRoundRect(
                    color = Color(0xFF0B3B26).copy(alpha = 0.85f),
                    topLeft = Offset(bx, displayTop + displayH * 0.78f - barH),
                    size = Size(displayW * 0.09f, barH),
                    cornerRadius = CornerRadius(displayW * 0.045f, displayW * 0.045f)
                )
            }
        } else {
            // Idle: soft clock-ish line
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(displayLeft + displayW * 0.12f, displayTop + displayH * 0.38f),
                size = Size(displayW * 0.46f, displayH * 0.24f),
                cornerRadius = CornerRadius(displayH * 0.12f, displayH * 0.12f)
            )
        }

        // 3×4 push-button keypad, lit while ringing
        val keypadTop = displayTop + displayH + bodyHeight * 0.06f
        val keyW = bodyWidth * 0.115f
        val keyH = bodyHeight * 0.10f
        val gapX = bodyWidth * 0.045f
        val gapY = bodyHeight * 0.032f
        val keypadWidth = keyW * 3f + gapX * 2f
        val keypadLeft = cx - keypadWidth / 2f
        for (row in 0 until 4) {
            for (col in 0 until 3) {
                val keyLeft = keypadLeft + col * (keyW + gapX)
                val keyTop = keypadTop + row * (keyH + gapY)
                val keyColor = if (isRinging) {
                    activeColor.copy(alpha = 0.92f)
                } else {
                    Color(0xFFF5EDDC).copy(alpha = 0.90f)
                }
                drawRoundRect(
                    color = keyColor,
                    topLeft = Offset(keyLeft, keyTop),
                    size = Size(keyW, keyH),
                    cornerRadius = CornerRadius(keyH * 0.30f, keyH * 0.30f)
                )
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.16f),
                    topLeft = Offset(keyLeft, keyTop + keyH * 0.72f),
                    size = Size(keyW, keyH * 0.28f),
                    cornerRadius = CornerRadius(keyH * 0.30f, keyH * 0.30f)
                )
            }
        }

        // Resting handset: two cups joined by a curved grip in a cradle
        val cupW = bodyWidth * 0.22f
        val cupH = handsetHeight * 0.86f
        val cupY = bodyTop - cupH * 0.74f
        listOf(left + bodyWidth * 0.17f, right - bodyWidth * 0.17f).forEach { cupCx ->
            // Cradle shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.22f),
                topLeft = Offset(cupCx - cupW * 0.62f, cupY + cupH * 0.78f),
                size = Size(cupW * 1.24f, cupH * 0.34f)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF3C4657), Color(0xFF222A36)),
                    startY = cupY,
                    endY = cupY + cupH
                ),
                topLeft = Offset(cupCx - cupW / 2f, cupY),
                size = Size(cupW, cupH),
                cornerRadius = CornerRadius(cupW * 0.42f, cupW * 0.42f)
            )
            drawRoundRect(
                color = activeColor.copy(alpha = 0.45f),
                topLeft = Offset(cupCx - cupW / 2f, cupY),
                size = Size(cupW, cupH),
                cornerRadius = CornerRadius(cupW * 0.42f, cupW * 0.42f),
                style = Stroke(width = maxOf(1f, cupW * 0.10f))
            )
        }
        // Grip bar between the cups
        val gripLeft = left + bodyWidth * 0.17f + cupW * 0.20f
        val gripRight = right - bodyWidth * 0.17f - cupW * 0.20f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF46536A), Color(0xFF242C38)),
                startY = cupY + cupH * 0.18f,
                endY = cupY + cupH * 0.58f
            ),
            topLeft = Offset(gripLeft, cupY + cupH * 0.18f),
            size = Size(gripRight - gripLeft, cupH * 0.40f),
            cornerRadius = CornerRadius(cupH * 0.20f, cupH * 0.20f)
        )

        // Sound arcs radiating from the handset while ringing
        if (isRinging) {
            repeat(2) { side ->
                val baseX = if (side == 0) left - bodyWidth * 0.02f else right + bodyWidth * 0.02f
                val dir = if (side == 0) -1f else 1f
                repeat(2) { index ->
                    val t = ((ring + index * 0.5f) % 1f)
                    val r = bodyWidth * (0.10f + 0.16f * index + 0.10f * t)
                    drawArc(
                        color = activeColor.copy(alpha = (1f - t) * 0.75f),
                        startAngle = if (dir < 0) 130f else -50f,
                        sweepAngle = 80f,
                        useCenter = false,
                        topLeft = Offset(baseX - r, cupY + cupH * 0.10f - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = maxOf(1.2f, bodyWidth * 0.022f), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

// ==================== MINECRAFT / VOXEL FURNITURE ====================

private fun DrawScope.drawVoxelClock(minutesOfDay: Int) {
    val s = min(size.width, size.height) * 0.84f
    val left = (size.width - s) / 2f
    val top = (size.height - s) / 2f
    // Chunky gold rimmed square pixel clock
    drawRect(Color(0xFFD4A017), Offset(left, top), Size(s, s))
    drawRect(Color(0xFF8A6508), Offset(left, top), Size(s, s), style = Stroke(width = 4f))
    val inner = s * 0.74f
    val iLeft = (size.width - inner) / 2f
    val iTop = (size.height - inner) / 2f
    drawRect(Color(0xFF284B7A), Offset(iLeft, iTop), Size(inner, inner / 2f))
    drawRect(Color(0xFF5FA8D3), Offset(iLeft, iTop + inner / 2f), Size(inner, inner / 2f))
    val cx = size.width / 2f
    val cy = size.height / 2f
    val hourAngle = Math.toRadians(((minutesOfDay % 720) / 720.0) * 360.0 - 90.0)
    val minAngle = Math.toRadians(((minutesOfDay % 60) / 60.0) * 360.0 - 90.0)
    drawLine(Color(0xFFFFEE58), Offset(cx, cy), Offset(cx + (cos(hourAngle) * inner * 0.34f).toFloat(), cy + (sin(hourAngle) * inner * 0.34f).toFloat()), 5f)
    drawLine(Color.White, Offset(cx, cy), Offset(cx + (cos(minAngle) * inner * 0.42f).toFloat(), cy + (sin(minAngle) * inner * 0.42f).toFloat()), 3.5f)
    drawRect(Color(0xFFE53935), Offset(cx - 3f, cy - 3f), Size(6f, 6f))
}

private fun DrawScope.drawVoxelOakSign() {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFF9C7B49), Offset(w * 0.06f, h * 0.14f), Size(w * 0.88f, h * 0.72f))
    drawRect(Color(0xFF5C4422), Offset(w * 0.06f, h * 0.14f), Size(w * 0.88f, h * 0.72f), style = Stroke(width = 3.5f))
    listOf(0.35f, 0.52f, 0.69f).forEach { y ->
        drawRect(Color(0xFF2B2115), Offset(w * 0.16f, h * y), Size(w * 0.68f, h * 0.06f))
    }
}

private fun DrawScope.drawVoxelTv() {
    val w = size.width
    val h = size.height
    // Chunky obsidian monitor
    drawRect(Color(0xFF1A1426), Offset(w * 0.03f, h * 0.04f), Size(w * 0.94f, h * 0.84f))
    drawRect(Color(0xFF3E2C59), Offset(w * 0.03f, h * 0.04f), Size(w * 0.94f, h * 0.84f), style = Stroke(width = 4f))
    drawRect(Color(0xFF242B36), Offset(w * 0.08f, h * 0.10f), Size(w * 0.84f, h * 0.70f))
    // Pixel sheen squares
    drawRect(Color.White.copy(alpha = 0.12f), Offset(w * 0.12f, h * 0.16f), Size(w * 0.14f, h * 0.14f))
    drawRect(Color.White.copy(alpha = 0.08f), Offset(w * 0.22f, h * 0.28f), Size(w * 0.10f, h * 0.10f))
    // Redstone power LED
    drawRect(Color(0xFFFF3D00), Offset(w * 0.86f, h * 0.81f), Size(w * 0.05f, h * 0.05f))
    // Blocky base
    drawRect(Color(0xFF2B223B), Offset(w * 0.36f, h * 0.88f), Size(w * 0.28f, h * 0.10f))
}

private fun DrawScope.drawVoxelCraftingAndJukebox() {
    val w = size.width
    val h = size.height
    // Left half: Crafting Table block
    drawRect(Color(0xFF9E7342), Offset(w * 0.04f, h * 0.08f), Size(w * 0.44f, h * 0.84f))
    drawRect(Color(0xFF5A3D1E), Offset(w * 0.04f, h * 0.08f), Size(w * 0.44f, h * 0.84f), style = Stroke(width = 3.5f))
    // Crafting grid lines
    drawRect(Color(0xFF6B4C2A), Offset(w * 0.10f, h * 0.18f), Size(w * 0.32f, h * 0.44f))
    // Right half: Jukebox block with glowing diamond disc slot!
    drawRect(Color(0xFF6E3B28), Offset(w * 0.52f, h * 0.08f), Size(w * 0.44f, h * 0.84f))
    drawRect(Color(0xFF422115), Offset(w * 0.52f, h * 0.08f), Size(w * 0.44f, h * 0.84f), style = Stroke(width = 3.5f))
    drawRect(Color(0xFF1A1412), Offset(w * 0.60f, h * 0.34f), Size(w * 0.28f, h * 0.14f))
    drawRect(Color(0xFF4DD0E1), Offset(w * 0.64f, h * 0.38f), Size(w * 0.20f, h * 0.06f))
}

private fun DrawScope.drawVoxelBookshelf() {
    val w = size.width
    val h = size.height
    // Iconic Minecraft Bookshelf Block (two rows of chunky pixel books in oak frame)
    drawRect(Color(0xFFB58E58), Offset(w * 0.04f, h * 0.04f), Size(w * 0.92f, h * 0.92f))
    drawRect(Color(0xFF5E4524), Offset(w * 0.04f, h * 0.04f), Size(w * 0.92f, h * 0.92f), style = Stroke(width = 4f))

    val bookColors = listOf(Color(0xFFC62828), Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFFF9A825), Color(0xFF6A1B9A), Color(0xFFE65100))
    val rows = listOf(h * 0.14f to h * 0.32f, h * 0.54f to h * 0.32f)
    rows.forEachIndexed { rIdx, (topY, rowH) ->
        drawRect(Color(0xFF3E2D1A), Offset(w * 0.09f, topY), Size(w * 0.82f, rowH))
        val bookCount = 6
        val bw = (w * 0.82f) / bookCount
        for (i in 0 until bookCount) {
            val c = bookColors[(i + rIdx * 2) % bookColors.size]
            drawRect(c, Offset(w * 0.09f + i * bw + 2f, topY + 3f), Size(bw - 4f, rowH - 6f))
            drawRect(Color.White.copy(alpha = 0.55f), Offset(w * 0.09f + i * bw + 4f, topY + rowH * 0.3f), Size(bw - 8f, 3f))
        }
    }
}

private fun DrawScope.drawVoxelRedBed() {
    val w = size.width
    val h = size.height
    // Classic iconic Red & White Minecraft Bed!
    // Oak wooden frame base & legs
    drawRect(Color(0xFF7D5632), Offset(w * 0.03f, h * 0.72f), Size(w * 0.94f, h * 0.14f))
    drawRect(Color(0xFF5C3E22), Offset(w * 0.05f, h * 0.86f), Size(w * 0.10f, h * 0.12f))
    drawRect(Color(0xFF5C3E22), Offset(w * 0.85f, h * 0.86f), Size(w * 0.10f, h * 0.12f))
    // White blocky pillow top section
    drawRect(Color(0xFFF5F5F5), Offset(w * 0.04f, h * 0.16f), Size(w * 0.92f, h * 0.24f))
    drawRect(Color(0xFFDCDCDC), Offset(w * 0.08f, h * 0.22f), Size(w * 0.84f, h * 0.12f))
    // Iconic vibrant red wool blanket block
    drawRect(Color(0xFFC62828), Offset(w * 0.04f, h * 0.38f), Size(w * 0.92f, h * 0.35f))
    drawRect(Color(0xFFE53935), Offset(w * 0.08f, h * 0.42f), Size(w * 0.84f, h * 0.24f))
    // Crisp dark pixel border
    drawRect(Color(0xFF3A1A1A), Offset(w * 0.04f, h * 0.16f), Size(w * 0.92f, h * 0.57f), style = Stroke(width = 3.5f))
}

private fun DrawScope.drawVoxelLavaCauldron() {
    val w = size.width
    val h = size.height
    // Chunky dark iron cauldron filled with glowing pixel lava!
    drawRect(Color(0xFF3E4247), Offset(w * 0.10f, h * 0.14f), Size(w * 0.80f, h * 0.72f))
    drawRect(Color(0xFF232629), Offset(w * 0.10f, h * 0.14f), Size(w * 0.80f, h * 0.72f), style = Stroke(width = 4f))
    // Glowing pixel lava inside top rim
    drawRect(Color(0xFFFF5722), Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.16f))
    drawRect(Color(0xFFFFC107), Offset(w * 0.26f, h * 0.22f), Size(w * 0.22f, h * 0.08f))
    drawRect(Color(0xFFFFC107), Offset(w * 0.58f, h * 0.20f), Size(w * 0.16f, h * 0.08f))
    // Blocky cauldron feet
    drawRect(Color(0xFF2B2E33), Offset(w * 0.14f, h * 0.86f), Size(w * 0.20f, h * 0.10f))
    drawRect(Color(0xFF2B2E33), Offset(w * 0.66f, h * 0.86f), Size(w * 0.20f, h * 0.10f))
}

private fun DrawScope.drawCustomObject(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    drawRoundRect(palette.woodMid.copy(alpha = 0.88f), Offset(w * 0.06f, h * 0.10f), Size(w * 0.88f, h * 0.80f), CornerRadius(14f, 14f))
    drawRoundRect(palette.woodLight, Offset(w * 0.06f, h * 0.10f), Size(w * 0.88f, h * 0.80f), CornerRadius(14f, 14f), style = Stroke(width = 3f))
}
