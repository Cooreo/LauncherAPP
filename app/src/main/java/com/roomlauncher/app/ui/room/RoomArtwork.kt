package com.roomlauncher.app.ui.room

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.RoomType
import com.roomlauncher.app.data.WeatherCondition
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomVisualPalette
import com.roomlauncher.app.ui.theme.accentNeonColors
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
                RoomStyleId.TEEN_ROOM -> drawTeenShell(palette)
                RoomStyleId.KIDS_ROOM -> drawKidsShell(palette)
                RoomStyleId.MINECRAFT -> drawMinecraftShell(palette)
                RoomStyleId.GEOMETRY_DASH -> drawGeometryDashShell(palette, gdSubTheme)
            }
            // Layer room-specific architecture (Living Room wainscoting, Kitchen tiles, Gaming Room RGB hexes)
            drawRoomTypeArchitectureAccent(roomType, palette)
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
    // Crisp neon LED tube strip along upper wall
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color(0xFF00F5D4), Color(0xFF9B5DE5), Color(0xFFF15BB5), Color(0xFF00BBF9))
        ),
        topLeft = Offset(0f, ledY),
        size = Size(w, h * 0.0065f)
    )

    // Warm string fairy lights draped across top wall
    val wirePath = Path().apply {
        moveTo(0f, ledY + h * 0.008f)
        quadraticTo(w * 0.25f, ledY + h * 0.052f, w * 0.50f, ledY + h * 0.016f)
        quadraticTo(w * 0.75f, ledY + h * 0.052f, w, ledY + h * 0.008f)
    }
    drawPath(wirePath, Color(0xFF555B6E).copy(alpha = 0.55f), style = Stroke(width = 2f))
    val bulbPoints = listOf(
        0.10f to 0.092f, 0.22f to 0.101f, 0.35f to 0.097f,
        0.50f to 0.084f, 0.65f to 0.097f, 0.78f to 0.101f, 0.90f to 0.092f
    )
    bulbPoints.forEach { (bx, by) ->
        drawCircle(Color(0xFFFFD166).copy(alpha = 0.28f), radius = w * 0.025f, center = Offset(w * bx, h * by))
        drawCircle(Color(0xFFFFF3B0), radius = w * 0.009f, center = Offset(w * bx, h * by))
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

    // Glowing Pixel Wall Torches!
    listOf(0.32f to 0.30f, 0.94f to 0.25f).forEach { (tx, ty) ->
        val cx = w * tx
        val cy = h * ty
        // Warm pixel glow halo
        drawCircle(Color(0xFFFFA726).copy(alpha = 0.24f), radius = w * 0.085f, center = Offset(cx, cy))
        drawCircle(Color(0xFFFFD54F).copy(alpha = 0.32f), radius = w * 0.045f, center = Offset(cx, cy))
        // Torch wooden stick
        drawRect(Color(0xFF6D4C2F), Offset(cx - w * 0.01f, cy), Size(w * 0.02f, h * 0.042f))
        // Pixel flame squares
        drawRect(Color(0xFFFF7043), Offset(cx - w * 0.013f, cy - h * 0.016f), Size(w * 0.026f, h * 0.018f))
        drawRect(Color(0xFFFFEE58), Offset(cx - w * 0.008f, cy - h * 0.012f), Size(w * 0.016f, h * 0.010f))
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
    modifier: Modifier = Modifier
) {
    val isVoxel = roomStyle == RoomStyleId.MINECRAFT
    Box(modifier = modifier) {
        when (visualType) {
            RoomObjectVisual.PAINTING -> {
                PaintingGraphic(
                    palette = palette,
                    isVoxel = isVoxel,
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
                    weatherState = weatherState,
                    weatherEnabled = weatherEnabled,
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
                            RoomObjectVisual.CLOCK -> drawClock(palette, currentMinutesOfDay)
                            RoomObjectVisual.STICKY_NOTE -> drawStickyNote(palette)
                            RoomObjectVisual.TV -> drawTv(palette)
                            RoomObjectVisual.CONSOLE -> drawGameConsole(palette)
                            RoomObjectVisual.SHELF -> drawShelf(palette, isKids = roomStyle == RoomStyleId.KIDS_ROOM)
                            RoomObjectVisual.BED -> drawBed(palette, isKids = roomStyle == RoomStyleId.KIDS_ROOM)
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

            val frameColor = if (isVoxel) Color(0xFF5C3E24) else palette.woodLight
            val frameStroke = min(w, h) * (if (isVoxel) 0.075f else 0.055f)
            drawRect(frameColor, Offset(padX, padTop), Size(paneW, paneH), style = Stroke(width = frameStroke))
            drawLine(frameColor, Offset(padX + paneW / 2f, padTop), Offset(padX + paneW / 2f, padTop + paneH), strokeWidth = frameStroke * 0.8f)
            drawLine(frameColor, Offset(padX, padTop + paneH / 2f), Offset(padX + paneW, padTop + paneH / 2f), strokeWidth = frameStroke * 0.8f)

            drawRoundRect(
                color = if (isVoxel) Color(0xFF472F1A) else palette.woodMid,
                topLeft = Offset(w * 0.02f, padTop + paneH - frameStroke * 0.2f),
                size = Size(w * 0.96f, h * 0.09f),
                cornerRadius = CornerRadius(if (isVoxel) 0f else 4f, if (isVoxel) 0f else 4f)
            )
        }

        if (weatherEnabled && weatherState.temperatureC != null) {
            val t = weatherState.temperatureC
            val badgeBg = when {
                t < 0 -> Color(0xBF1E4B6E)
                t < 10 -> Color(0xB33B5368)
                else -> Color(0x8814171D)
            }
            val label = if (t < 0) "❄ $t°" else "$t°"
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 9.dp, bottom = 13.dp)
                    .clip(RoundedCornerShape(if (isVoxel) 2.dp else 8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
    gdDefaultDrawableRes: Int? = null,
    frameImageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        // 1. Back layer: realistic drop shadow, beveled wooden outer frame & warm cream gallery mat
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val corner = if (isVoxel) 0f else 6f

            // Soft gallery wall shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = if (palette.isDark) 0.44f else 0.16f),
                topLeft = Offset(w * 0.055f, h * 0.065f),
                size = Size(w * 0.92f, h * 0.91f),
                cornerRadius = CornerRadius(corner, corner)
            )
            // Symmetrical outer wooden frame
            val frameLeft = w * 0.03f
            val frameTop = h * 0.03f
            val frameW = w * 0.94f
            val frameH = h * 0.94f
            drawRoundRect(
                color = if (isVoxel) Color(0xFF6B4828) else palette.woodDark,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameW, frameH),
                cornerRadius = CornerRadius(corner, corner)
            )
            // Subtle inner wood bevel highlight so frame has real depth
            drawRoundRect(
                color = (if (isVoxel) Color(0xFF91673F) else palette.woodLight).copy(alpha = 0.55f),
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
            drawRect(Color(0xFFFAF6EE), Offset(matLeft, matTop), Size(matW, matH))

            // Recessed inner aperture border around the artwork/photo
            val artLeft = w * 0.155f
            val artTop = h * 0.155f
            val artW = w * 0.69f
            val artH = h * 0.69f
            drawRect(
                color = Color(0xFFD5CCB9),
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

private fun DrawScope.drawClock(palette: RoomVisualPalette, minutesOfDay: Int) {
    val radius = min(size.width, size.height) * 0.43f
    val cx = size.width / 2f
    val cy = size.height / 2f

    drawCircle(Color.Black.copy(alpha = if (palette.isDark) 0.36f else 0.12f), radius, Offset(cx + radius * 0.08f, cy + radius * 0.10f))
    drawCircle(palette.woodDark, radius, Offset(cx, cy))
    drawCircle(palette.clockFace, radius * 0.85f, Offset(cx, cy))

    val tickStroke = radius * 0.065f
    for (i in 0 until 12) {
        val angle = Math.toRadians(i * 30.0)
        val inner = if (i % 3 == 0) radius * 0.64f else radius * 0.72f
        val outer = radius * 0.78f
        drawLine(
            color = palette.clockHands.copy(alpha = if (i % 3 == 0) 0.85f else 0.35f),
            start = Offset(cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat()),
            end = Offset(cx + (cos(angle) * outer).toFloat(), cy + (sin(angle) * outer).toFloat()),
            strokeWidth = if (i % 3 == 0) tickStroke else tickStroke * 0.55f,
            cap = StrokeCap.Round
        )
    }

    val hourAngle = Math.toRadians(((minutesOfDay % 720) / 720.0) * 360.0 - 90.0)
    val minAngle = Math.toRadians(((minutesOfDay % 60) / 60.0) * 360.0 - 90.0)
    drawLine(palette.clockHands, Offset(cx, cy), Offset(cx + (cos(hourAngle) * radius * 0.45f).toFloat(), cy + (sin(hourAngle) * radius * 0.45f).toFloat()), radius * 0.095f, StrokeCap.Round)
    drawLine(palette.clockHands, Offset(cx, cy), Offset(cx + (cos(minAngle) * radius * 0.65f).toFloat(), cy + (sin(minAngle) * radius * 0.65f).toFloat()), radius * 0.065f, StrokeCap.Round)
    drawCircle(palette.clockAccent, radius * 0.09f, Offset(cx, cy))
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

private fun DrawScope.drawShelf(palette: RoomVisualPalette, isKids: Boolean) {
    val w = size.width
    val h = size.height
    val upperY = h * 0.40f
    val shelfH = h * 0.065f

    drawRect(Color.Black.copy(alpha = 0.16f), Offset(w * 0.06f, upperY + shelfH), Size(w * 0.90f, h * 0.03f))
    drawRoundRect(if (isKids) Color(0xFFFFB703) else palette.woodMid, Offset(w * 0.04f, upperY), Size(w * 0.92f, shelfH), CornerRadius(4f, 4f))

    drawRect(if (isKids) Color(0xFFFF6B6B) else palette.duvetPrimary, Offset(w * 0.09f, h * 0.13f), Size(w * 0.09f, upperY - h * 0.13f))
    drawRect(if (isKids) Color(0xFF06D6A0) else palette.throwBlanket, Offset(w * 0.20f, h * 0.18f), Size(w * 0.08f, upperY - h * 0.18f))
    drawRect(if (isKids) Color(0xFF118AB2) else Color(0xFFE2B97B), Offset(w * 0.30f, h * 0.10f), Size(w * 0.09f, upperY - h * 0.10f))
    val leaning = Path().apply {
        moveTo(w * 0.41f, upperY); lineTo(w * 0.48f, h * 0.16f); lineTo(w * 0.55f, h * 0.18f); lineTo(w * 0.48f, upperY); close()
    }
    drawPath(leaning, if (isKids) Color(0xFF9B5DE5) else Color(0xFF596E79))
    val pot = Path().apply {
        moveTo(w * 0.72f, h * 0.28f); lineTo(w * 0.88f, h * 0.28f); lineTo(w * 0.85f, upperY); lineTo(w * 0.75f, upperY); close()
    }
    drawPath(pot, Color(0xFFC66E4E))
    drawCircle(Color(0xFF5D8768), w * 0.065f, Offset(w * 0.80f, h * 0.22f))

    val lowerY = h * 0.86f
    drawRect(Color.Black.copy(alpha = 0.16f), Offset(w * 0.06f, lowerY + shelfH), Size(w * 0.90f, h * 0.03f))
    drawRoundRect(if (isKids) Color(0xFFFFB703) else palette.woodMid, Offset(w * 0.04f, lowerY), Size(w * 0.92f, shelfH), CornerRadius(4f, 4f))

    drawRect(if (isKids) Color(0xFF118AB2) else Color(0xFF3E4E56), Offset(w * 0.09f, h * 0.60f), Size(w * 0.24f, lowerY - h * 0.60f))
    drawRect(Color(0xFFFAF7F2), Offset(w * 0.14f, h * 0.69f), Size(w * 0.14f, h * 0.04f))
    drawRect(Color(0xFFD9A76A), Offset(w * 0.38f, h * 0.79f), Size(w * 0.34f, lowerY - h * 0.79f))
    drawRect(if (isKids) Color(0xFFFF6B6B) else palette.duvetPrimary, Offset(w * 0.41f, h * 0.72f), Size(w * 0.28f, h * 0.07f))
    drawRect(Color(0xFFF0E5D6), Offset(w * 0.78f, h * 0.71f), Size(w * 0.12f, lowerY - h * 0.71f))
    drawRect(palette.throwBlanket, Offset(w * 0.81f, h * 0.63f), Size(w * 0.025f, h * 0.08f))
    drawRect(palette.duvetPrimary, Offset(w * 0.855f, h * 0.65f), Size(w * 0.02f, h * 0.06f))
}

private fun DrawScope.drawBed(palette: RoomVisualPalette, isKids: Boolean) {
    val w = size.width
    val h = size.height

    drawRoundRect(Color.Black.copy(alpha = if (palette.isDark) 0.40f else 0.16f), Offset(w * 0.04f, h * 0.90f), Size(w * 0.92f, h * 0.09f), CornerRadius(10f, 10f))
    drawRoundRect(if (isKids) Color(0xFF118AB2) else palette.woodDark, Offset(w * 0.03f, h * 0.03f), Size(w * 0.86f, h * 0.34f), CornerRadius(14f, 14f))
    drawRect(Color.Black.copy(alpha = 0.14f), Offset(w * 0.07f, h * 0.11f), Size(w * 0.78f, h * 0.022f))
    drawRect(Color.Black.copy(alpha = 0.14f), Offset(w * 0.07f, h * 0.19f), Size(w * 0.78f, h * 0.022f))

    drawRoundRect(palette.pillowColor.copy(alpha = 0.94f), Offset(w * 0.03f, h * 0.30f), Size(w * 0.94f, h * 0.50f), CornerRadius(10f, 10f))
    drawRoundRect(palette.pillowColor, Offset(w * 0.07f, h * 0.21f), Size(w * 0.38f, h * 0.18f), CornerRadius(14f, 14f))
    drawRoundRect(palette.pillowColor.copy(alpha = 0.90f), Offset(w * 0.48f, h * 0.21f), Size(w * 0.38f, h * 0.18f), CornerRadius(14f, 14f))
    drawRect(palette.pillowColor.copy(alpha = 0.82f), Offset(w * 0.03f, h * 0.39f), Size(w * 0.94f, h * 0.05f))

    drawRect(if (isKids) Color(0xFF2EC4B6) else palette.duvetPrimary, Offset(w * 0.03f, h * 0.43f), Size(w * 0.94f, h * 0.37f))
    if (isKids) {
        // Cheerful stars on kids duvet
        listOf(0.22f to 0.52f, 0.50f to 0.55f, 0.78f to 0.51f).forEach { (sx, sy) ->
            drawCircle(Color(0xFFFFD166), w * 0.032f, Offset(w * sx, h * sy))
        }
    }
    drawRect(if (isKids) Color(0xFFFF6B6B) else palette.throwBlanket, Offset(w * 0.03f, h * 0.64f), Size(w * 0.94f, h * 0.16f))
    drawRect(if (isKids) Color(0xFF118AB2) else palette.woodDark, Offset(w * 0.02f, h * 0.79f), Size(w * 0.96f, h * 0.085f))
    drawRect(if (isKids) Color(0xFF0B6887) else palette.woodDark, Offset(w * 0.06f, h * 0.875f), Size(w * 0.055f, h * 0.10f))
    drawRect(if (isKids) Color(0xFF0B6887) else palette.woodDark, Offset(w * 0.885f, h * 0.875f), Size(w * 0.055f, h * 0.10f))
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
