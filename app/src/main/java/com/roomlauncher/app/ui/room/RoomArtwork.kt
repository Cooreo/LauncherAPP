package com.roomlauncher.app.ui.room

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.WeatherCondition
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.ui.theme.RoomVisualPalette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders the empty architectural shell of the room (walls, baseboard, wooden floor, area rug)
 * tinted dynamically by the active [RoomVisualPalette].
 */
@Composable
fun RoomShellCanvas(
    palette: RoomVisualPalette,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val floorY = h * 0.61f

        // Back wall
        drawRect(
            color = palette.wallMain,
            topLeft = Offset.Zero,
            size = Size(w, floorY)
        )

        // Subtle upper wall warmth band
        drawRect(
            color = palette.wallUpper,
            topLeft = Offset.Zero,
            size = Size(w, h * 0.115f)
        )

        // Soft ambient diagonal light wash
        val lightPath = Path().apply {
            moveTo(w * 0.05f, h * 0.115f)
            lineTo(w * 0.30f, h * 0.115f)
            lineTo(w * 0.52f, floorY)
            lineTo(w * 0.10f, floorY)
            close()
        }
        drawPath(path = lightPath, color = palette.ambientLightTint)

        // Baseboard & shadow seam
        drawRect(
            color = palette.baseboard,
            topLeft = Offset(0f, floorY - h * 0.018f),
            size = Size(w, h * 0.018f)
        )
        drawRect(
            color = palette.baseboardShadow,
            topLeft = Offset(0f, floorY),
            size = Size(w, h * 0.005f)
        )

        // Wooden floor
        drawRect(
            color = palette.floorMain,
            topLeft = Offset(0f, floorY + h * 0.005f),
            size = Size(w, h - floorY)
        )

        // Horizontal floor plank seams
        val plankAlpha = if (palette.isDark) 0.5f else 0.35f
        val seamColor = palette.floorPlankLine.copy(alpha = plankAlpha)
        val seamStroke = w * 0.0032f
        val rowYs = listOf(0.68f, 0.75f, 0.83f, 0.91f)
        rowYs.forEach { fy ->
            drawLine(
                color = seamColor,
                start = Offset(0f, h * fy),
                end = Offset(w, h * fy),
                strokeWidth = seamStroke
            )
        }

        // Subtle vertical floorboard joins
        val verticalJoins = listOf(
            Triple(0.26f, 0.615f, 0.68f),
            Triple(0.67f, 0.68f, 0.75f),
            Triple(0.41f, 0.75f, 0.83f),
            Triple(0.78f, 0.83f, 0.91f),
            Triple(0.31f, 0.91f, 1.0f)
        )
        verticalJoins.forEach { (fx, y1, y2) ->
            drawLine(
                color = seamColor.copy(alpha = plankAlpha * 0.75f),
                start = Offset(w * fx, h * y1),
                end = Offset(w * fx, h * y2),
                strokeWidth = seamStroke * 0.85f
            )
        }

        // Cozy area rug
        val rugTopLeft = Offset(w * 0.12f, h * 0.665f)
        val rugSize = Size(w * 0.76f, h * 0.245f)
        drawRoundRect(
            color = palette.rugFill,
            topLeft = rugTopLeft,
            size = rugSize,
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
        )
        drawRoundRect(
            color = palette.rugBorder.copy(alpha = 0.6f),
            topLeft = Offset(rugTopLeft.x + w * 0.03f, rugTopLeft.y + h * 0.015f),
            size = Size(rugSize.width - w * 0.06f, rugSize.height - h * 0.03f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
            style = Stroke(width = w * 0.004f)
        )
    }
}

/**
 * Renders the visual furniture/object illustration directly inside its movable bounding box.
 * When the user drags or resizes an object in Edit Mode, its artwork moves and scales seamlessly!
 */
@Composable
fun RoomObjectGraphic(
    visualType: RoomObjectVisual,
    palette: RoomVisualPalette,
    weatherState: WeatherState,
    weatherEnabled: Boolean,
    frameImageBitmap: ImageBitmap?,
    currentMinutesOfDay: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (visualType) {
            RoomObjectVisual.PAINTING -> {
                PaintingGraphic(
                    palette = palette,
                    frameImageBitmap = frameImageBitmap,
                    modifier = Modifier.fillMaxSize()
                )
            }
            RoomObjectVisual.WINDOW -> {
                WindowGraphic(
                    palette = palette,
                    weatherState = weatherState,
                    weatherEnabled = weatherEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    when (visualType) {
                        RoomObjectVisual.CLOCK -> drawClock(palette, currentMinutesOfDay)
                        RoomObjectVisual.STICKY_NOTE -> drawStickyNote(palette)
                        RoomObjectVisual.TV -> drawTv(palette)
                        RoomObjectVisual.CONSOLE -> drawGameConsole(palette)
                        RoomObjectVisual.SHELF -> drawShelf(palette)
                        RoomObjectVisual.BED -> drawBed(palette)
                        RoomObjectVisual.TRASH_CAN -> drawTrashCan(palette)
                        RoomObjectVisual.CUSTOM -> drawCustomObject(palette)
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowGraphic(
    palette: RoomVisualPalette,
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

            // Sky backdrop based on real weather condition
            val skyColor = when (activeCondition) {
                WeatherCondition.CLEAR_DAY -> Color(0xFF9ED4EC)
                WeatherCondition.CLEAR_NIGHT -> Color(0xFF192445)
                WeatherCondition.PARTLY_CLOUDY -> Color(0xFFABC9D9)
                WeatherCondition.CLOUDY -> Color(0xFF8C9DA8)
                WeatherCondition.RAIN -> Color(0xFF5D748B)
                WeatherCondition.SNOW -> Color(0xFFB5C7D4)
                WeatherCondition.THUNDERSTORM -> Color(0xFF364256)
            }
            drawRect(
                color = skyColor,
                topLeft = Offset(padX, padTop),
                size = Size(paneW, paneH)
            )

            // Weather sky details inside window glass
            when (activeCondition) {
                WeatherCondition.CLEAR_DAY -> {
                    drawCircle(
                        color = Color(0xFFFFD86B),
                        radius = min(paneW, paneH) * 0.16f,
                        center = Offset(padX + paneW * 0.72f, padTop + paneH * 0.28f)
                    )
                    // Subtle glass reflection
                    val sheen = Path().apply {
                        moveTo(padX + paneW * 0.12f, padTop)
                        lineTo(padX + paneW * 0.34f, padTop)
                        lineTo(padX, padTop + paneH * 0.45f)
                        lineTo(padX, padTop + paneH * 0.22f)
                        close()
                    }
                    drawPath(sheen, Color.White.copy(alpha = 0.30f))
                }
                WeatherCondition.CLEAR_NIGHT -> {
                    val moonCenter = Offset(padX + paneW * 0.70f, padTop + paneH * 0.28f)
                    val moonR = min(paneW, paneH) * 0.14f
                    drawCircle(Color(0xFFF6EBB7), radius = moonR, center = moonCenter)
                    drawCircle(skyColor, radius = moonR * 0.85f, center = Offset(moonCenter.x + moonR * 0.4f, moonCenter.y - moonR * 0.15f))
                    // Subtle stars
                    listOf(0.22f to 0.24f, 0.44f to 0.18f, 0.28f to 0.62f, 0.76f to 0.68f, 0.56f to 0.76f).forEach { (sx, sy) ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = min(paneW, paneH) * 0.022f,
                            center = Offset(padX + paneW * sx, padTop + paneH * sy)
                        )
                    }
                }
                WeatherCondition.PARTLY_CLOUDY -> {
                    drawCircle(
                        color = Color(0xFFFFD86B),
                        radius = min(paneW, paneH) * 0.15f,
                        center = Offset(padX + paneW * 0.72f, padTop + paneH * 0.26f)
                    )
                    drawCloud(padX + paneW * 0.24f, padTop + paneH * 0.46f, paneW * 0.46f, Color.White.copy(alpha = 0.88f))
                    drawCloud(padX + paneW * 0.52f, padTop + paneH * 0.70f, paneW * 0.38f, Color.White.copy(alpha = 0.72f))
                }
                WeatherCondition.CLOUDY -> {
                    drawCloud(padX + paneW * 0.20f, padTop + paneH * 0.34f, paneW * 0.55f, Color(0xFFDDE5EB))
                    drawCloud(padX + paneW * 0.44f, padTop + paneH * 0.64f, paneW * 0.50f, Color(0xFFCBD5DE))
                }
                WeatherCondition.RAIN -> {
                    drawCloud(padX + paneW * 0.22f, padTop + paneH * 0.26f, paneW * 0.58f, Color(0xFF99AABB))
                    val rainColor = Color(0xFFCDE5F7).copy(alpha = 0.85f)
                    listOf(0.24f to 0.44f, 0.44f to 0.52f, 0.64f to 0.42f, 0.32f to 0.68f, 0.56f to 0.72f, 0.76f to 0.64f).forEach { (rx, ry) ->
                        val start = Offset(padX + paneW * rx, padTop + paneH * ry)
                        drawLine(
                            color = rainColor,
                            start = start,
                            end = Offset(start.x - paneW * 0.06f, start.y + paneH * 0.13f),
                            strokeWidth = min(paneW, paneH) * 0.025f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                WeatherCondition.SNOW -> {
                    drawCloud(padX + paneW * 0.22f, padTop + paneH * 0.24f, paneW * 0.56f, Color(0xFFE7EFF5))
                    listOf(0.22f to 0.48f, 0.42f to 0.56f, 0.66f to 0.46f, 0.28f to 0.74f, 0.54f to 0.76f, 0.78f to 0.68f).forEach { (sx, sy) ->
                        drawCircle(
                            color = Color.White,
                            radius = min(paneW, paneH) * 0.032f,
                            center = Offset(padX + paneW * sx, padTop + paneH * sy)
                        )
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

            // Wooden window frame & muntins
            val frameStroke = min(w, h) * 0.055f
            drawRect(
                color = palette.woodLight,
                topLeft = Offset(padX, padTop),
                size = Size(paneW, paneH),
                style = Stroke(width = frameStroke)
            )
            drawLine(
                color = palette.woodLight,
                start = Offset(padX + paneW / 2f, padTop),
                end = Offset(padX + paneW / 2f, padTop + paneH),
                strokeWidth = frameStroke * 0.8f
            )
            drawLine(
                color = palette.woodLight,
                start = Offset(padX, padTop + paneH / 2f),
                end = Offset(padX + paneW, padTop + paneH / 2f),
                strokeWidth = frameStroke * 0.8f
            )

            // Window sill
            drawRoundRect(
                color = palette.woodMid,
                topLeft = Offset(w * 0.02f, padTop + paneH - frameStroke * 0.2f),
                size = Size(w * 0.96f, h * 0.09f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Subtle live temperature badge when weather data is available
        if (weatherEnabled && weatherState.temperatureC != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.46f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${weatherState.temperatureC}°",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun DrawScope.drawCloud(left: Float, top: Float, width: Float, color: Color) {
    val r = width * 0.22f
    drawCircle(color, radius = r * 0.85f, center = Offset(left + r * 0.9f, top + r * 0.9f))
    drawCircle(color, radius = r * 1.1f, center = Offset(left + width * 0.46f, top + r * 0.65f))
    drawCircle(color, radius = r * 0.85f, center = Offset(left + width - r * 0.9f, top + r * 0.95f))
    drawRoundRect(
        color = color,
        topLeft = Offset(left + r * 0.4f, top + r * 0.75f),
        size = Size(width - r * 0.8f, r * 1.0f),
        cornerRadius = CornerRadius(r, r)
    )
}

@Composable
private fun PaintingGraphic(
    palette: RoomVisualPalette,
    frameImageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Soft shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = if (palette.isDark) 0.35f else 0.13f),
                topLeft = Offset(w * 0.05f, h * 0.06f),
                size = Size(w * 0.93f, h * 0.92f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Outer wooden frame
            drawRoundRect(
                color = palette.woodDark,
                topLeft = Offset(w * 0.02f, h * 0.02f),
                size = Size(w * 0.94f, h * 0.94f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Cream gallery mat
            drawRect(
                color = Color(0xFFFAF7F2),
                topLeft = Offset(w * 0.09f, h * 0.09f),
                size = Size(w * 0.80f, h * 0.80f)
            )

            // Default landscape artwork when no custom photo is set
            if (frameImageBitmap == null) {
                val artLeft = w * 0.15f
                val artTop = h * 0.15f
                val artW = w * 0.68f
                val artH = h * 0.68f

                drawRect(
                    color = if (palette.isDark) Color(0xFF42384A) else Color(0xFFE9C7A6),
                    topLeft = Offset(artLeft, artTop),
                    size = Size(artW, artH)
                )
                drawCircle(
                    color = if (palette.isDark) Color(0xFFE8A87C) else Color(0xFFD97D54),
                    radius = min(artW, artH) * 0.17f,
                    center = Offset(artLeft + artW * 0.66f, artTop + artH * 0.32f)
                )
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

        if (frameImageBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(11.dp)
                    .clip(RoundedCornerShape(2.dp))
            ) {
                Image(
                    bitmap = frameImageBitmap,
                    contentDescription = "Picture frame artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun DrawScope.drawClock(palette: RoomVisualPalette, minutesOfDay: Int) {
    val radius = min(size.width, size.height) * 0.43f
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Wall shadow
    drawCircle(
        color = Color.Black.copy(alpha = if (palette.isDark) 0.36f else 0.12f),
        radius = radius,
        center = Offset(cx + radius * 0.08f, cy + radius * 0.10f)
    )
    // Outer wood rim
    drawCircle(color = palette.woodDark, radius = radius, center = Offset(cx, cy))
    // Clock face
    drawCircle(color = palette.clockFace, radius = radius * 0.85f, center = Offset(cx, cy))

    // 4 cardinal hour ticks
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

    // Live current time hands!
    val hourFraction = (minutesOfDay % 720) / 720.0
    val minuteFraction = (minutesOfDay % 60) / 60.0
    val hourAngle = Math.toRadians(hourFraction * 360.0 - 90.0)
    val minAngle = Math.toRadians(minuteFraction * 360.0 - 90.0)

    drawLine(
        color = palette.clockHands,
        start = Offset(cx, cy),
        end = Offset(cx + (cos(hourAngle) * radius * 0.45f).toFloat(), cy + (sin(hourAngle) * radius * 0.45f).toFloat()),
        strokeWidth = radius * 0.095f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = palette.clockHands,
        start = Offset(cx, cy),
        end = Offset(cx + (cos(minAngle) * radius * 0.65f).toFloat(), cy + (sin(minAngle) * radius * 0.65f).toFloat()),
        strokeWidth = radius * 0.065f,
        cap = StrokeCap.Round
    )
    drawCircle(color = palette.clockAccent, radius = radius * 0.09f, center = Offset(cx, cy))
}

private fun DrawScope.drawStickyNote(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val left = w * 0.08f
    val top = h * 0.14f
    val noteW = w * 0.84f
    val noteH = h * 0.78f
    val fold = min(noteW, noteH) * 0.22f

    // Subtle paper shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = if (palette.isDark) 0.34f else 0.13f),
        topLeft = Offset(left + w * 0.04f, top + h * 0.05f),
        size = Size(noteW, noteH),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Note paper shape with bottom-right folded corner
    val notePath = Path().apply {
        moveTo(left, top)
        lineTo(left + noteW, top)
        lineTo(left + noteW, top + noteH - fold)
        lineTo(left + noteW - fold, top + noteH)
        lineTo(left, top + noteH)
        close()
    }
    drawPath(notePath, palette.stickyNoteFill)

    // Folded corner triangle
    val foldPath = Path().apply {
        moveTo(left + noteW - fold, top + noteH)
        lineTo(left + noteW - fold, top + noteH - fold)
        lineTo(left + noteW, top + noteH - fold)
        close()
    }
    drawPath(foldPath, palette.stickyNoteFold)

    // Neat handwritten lines on note
    val ink = Color(0xFF4A3D1F).copy(alpha = 0.55f)
    val lineStroke = maxOf(2f, h * 0.032f)
    listOf(0.38f to 0.72f, 0.56f to 0.80f, 0.74f to 0.54f).forEach { (ly, lw) ->
        drawLine(
            color = ink,
            start = Offset(left + noteW * 0.14f, top + noteH * ly),
            end = Offset(left + noteW * lw, top + noteH * ly),
            strokeWidth = lineStroke,
            cap = StrokeCap.Round
        )
    }

    // Tiny pushpin at top center
    val pinRadius = min(w, h) * 0.085f
    drawCircle(
        color = Color.Black.copy(alpha = 0.22f),
        radius = pinRadius,
        center = Offset(left + noteW * 0.52f, top + pinRadius * 0.85f)
    )
    drawCircle(
        color = palette.clockAccent,
        radius = pinRadius,
        center = Offset(left + noteW * 0.50f, top + pinRadius * 0.65f)
    )
}

private fun DrawScope.drawTv(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val bezelLeft = w * 0.03f
    val bezelTop = h * 0.04f
    val bezelW = w * 0.94f
    val bezelH = h * 0.82f

    // Shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = if (palette.isDark) 0.4f else 0.13f),
        topLeft = Offset(bezelLeft + w * 0.02f, bezelTop + h * 0.04f),
        size = Size(bezelW, bezelH),
        cornerRadius = CornerRadius(12f, 12f)
    )
    // Outer TV bezel
    drawRoundRect(
        color = palette.tvBezel,
        topLeft = Offset(bezelLeft, bezelTop),
        size = Size(bezelW, bezelH),
        cornerRadius = CornerRadius(12f, 12f)
    )
    // Screen surface
    val screenLeft = bezelLeft + bezelW * 0.038f
    val screenTop = bezelTop + bezelH * 0.06f
    val screenW = bezelW * 0.924f
    val screenH = bezelH * 0.84f
    drawRect(
        color = palette.tvScreen,
        topLeft = Offset(screenLeft, screenTop),
        size = Size(screenW, screenH)
    )
    // Diagonal screen sheen
    val sheen = Path().apply {
        moveTo(screenLeft + screenW * 0.06f, screenTop)
        lineTo(screenLeft + screenW * 0.30f, screenTop)
        lineTo(screenLeft + screenW * 0.12f, screenTop + screenH)
        lineTo(screenLeft, screenTop + screenH)
        lineTo(screenLeft, screenTop + screenH * 0.25f)
        close()
    }
    drawPath(sheen, Color.White.copy(alpha = if (palette.isDark) 0.07f else 0.12f))

    // Standby LED
    drawCircle(
        color = Color(0xFF7CC49E),
        radius = min(w, h) * 0.018f,
        center = Offset(bezelLeft + bezelW * 0.93f, bezelTop + bezelH * 0.95f)
    )

    // Pedestal stand neck & base
    drawRect(
        color = palette.tvBezel,
        topLeft = Offset(w * 0.45f, bezelTop + bezelH),
        size = Size(w * 0.10f, h * 0.10f)
    )
    drawRoundRect(
        color = palette.tvBezel,
        topLeft = Offset(w * 0.34f, h * 0.93f),
        size = Size(w * 0.32f, h * 0.06f),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawGameConsole(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    val cabTop = h * 0.06f
    val cabH = h * 0.68f

    // Wooden media console cabinet
    drawRoundRect(
        color = palette.woodMid,
        topLeft = Offset(w * 0.02f, cabTop),
        size = Size(w * 0.96f, cabH),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Left & right cabinet doors
    drawRect(
        color = palette.woodDark.copy(alpha = 0.72f),
        topLeft = Offset(w * 0.06f, cabTop + cabH * 0.14f),
        size = Size(w * 0.22f, cabH * 0.72f)
    )
    drawRect(
        color = palette.woodDark.copy(alpha = 0.72f),
        topLeft = Offset(w * 0.72f, cabTop + cabH * 0.14f),
        size = Size(w * 0.22f, cabH * 0.72f)
    )
    // Door knobs
    drawCircle(
        color = palette.woodLight,
        radius = min(w, h) * 0.03f,
        center = Offset(w * 0.24f, cabTop + cabH * 0.50f)
    )
    drawCircle(
        color = palette.woodLight,
        radius = min(w, h) * 0.03f,
        center = Offset(w * 0.76f, cabTop + cabH * 0.50f)
    )

    // Center open bay holding the sleek Game Console!
    val bayLeft = w * 0.31f
    val bayW = w * 0.38f
    drawRect(
        color = Color(0xFF191512),
        topLeft = Offset(bayLeft, cabTop + cabH * 0.14f),
        size = Size(bayW, cabH * 0.72f)
    )
    // Sleek game console chassis
    val conLeft = bayLeft + bayW * 0.08f
    val conTop = cabTop + cabH * 0.26f
    val conW = bayW * 0.84f
    val conH = cabH * 0.44f
    drawRoundRect(
        color = palette.consoleBody,
        topLeft = Offset(conLeft, conTop),
        size = Size(conW, conH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Glowing console power strip
    drawRoundRect(
        color = palette.consoleAccent,
        topLeft = Offset(conLeft + conW * 0.12f, conTop + conH * 0.38f),
        size = Size(conW * 0.52f, conH * 0.18f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Controller indicator dot
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = conH * 0.13f,
        center = Offset(conLeft + conW * 0.82f, conTop + conH * 0.48f)
    )

    // Tapered mid-century cabinet legs
    val leftLeg = Path().apply {
        moveTo(w * 0.09f, cabTop + cabH)
        lineTo(w * 0.13f, cabTop + cabH)
        lineTo(w * 0.10f, h * 0.96f)
        lineTo(w * 0.06f, h * 0.96f)
        close()
    }
    val rightLeg = Path().apply {
        moveTo(w * 0.87f, cabTop + cabH)
        lineTo(w * 0.91f, cabTop + cabH)
        lineTo(w * 0.94f, h * 0.96f)
        lineTo(w * 0.90f, h * 0.96f)
        close()
    }
    drawPath(leftLeg, palette.woodDark)
    drawPath(rightLeg, palette.woodDark)
}

private fun DrawScope.drawShelf(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height

    // Upper wooden shelf & shadow
    val upperY = h * 0.40f
    val shelfH = h * 0.065f
    drawRect(Color.Black.copy(alpha = 0.16f), Offset(w * 0.06f, upperY + shelfH), Size(w * 0.90f, h * 0.03f))
    drawRoundRect(palette.woodMid, Offset(w * 0.04f, upperY), Size(w * 0.92f, shelfH), CornerRadius(4f, 4f))

    // Books on upper shelf
    drawRect(palette.duvetPrimary, Offset(w * 0.09f, h * 0.13f), Size(w * 0.09f, upperY - h * 0.13f))
    drawRect(palette.throwBlanket, Offset(w * 0.20f, h * 0.18f), Size(w * 0.08f, upperY - h * 0.18f))
    drawRect(Color(0xFFE2B97B), Offset(w * 0.30f, h * 0.10f), Size(w * 0.09f, upperY - h * 0.10f))
    // Leaning book
    val leaning = Path().apply {
        moveTo(w * 0.41f, upperY)
        lineTo(w * 0.48f, h * 0.16f)
        lineTo(w * 0.55f, h * 0.18f)
        lineTo(w * 0.48f, upperY)
        close()
    }
    drawPath(leaning, Color(0xFF596E79))
    // Potted succulent
    val pot = Path().apply {
        moveTo(w * 0.72f, h * 0.28f)
        lineTo(w * 0.88f, h * 0.28f)
        lineTo(w * 0.85f, upperY)
        lineTo(w * 0.75f, upperY)
        close()
    }
    drawPath(pot, Color(0xFFC66E4E))
    drawCircle(Color(0xFF5D8768), radius = w * 0.065f, center = Offset(w * 0.80f, h * 0.22f))

    // Lower wooden shelf & shadow
    val lowerY = h * 0.86f
    drawRect(Color.Black.copy(alpha = 0.16f), Offset(w * 0.06f, lowerY + shelfH), Size(w * 0.90f, h * 0.03f))
    drawRoundRect(palette.woodMid, Offset(w * 0.04f, lowerY), Size(w * 0.92f, shelfH), CornerRadius(4f, 4f))

    // Organizer box
    drawRect(Color(0xFF3E4E56), Offset(w * 0.09f, h * 0.60f), Size(w * 0.24f, lowerY - h * 0.60f))
    drawRect(Color(0xFFFAF7F2), Offset(w * 0.14f, h * 0.69f), Size(w * 0.14f, h * 0.04f))

    // Stacked notebooks
    drawRect(Color(0xFFD9A76A), Offset(w * 0.38f, h * 0.79f), Size(w * 0.34f, lowerY - h * 0.79f))
    drawRect(palette.duvetPrimary, Offset(w * 0.41f, h * 0.72f), Size(w * 0.28f, h * 0.07f))

    // Pen cup
    drawRect(Color(0xFFF0E5D6), Offset(w * 0.78f, h * 0.71f), Size(w * 0.12f, lowerY - h * 0.71f))
    drawRect(palette.throwBlanket, Offset(w * 0.81f, h * 0.63f), Size(w * 0.025f, h * 0.08f))
    drawRect(palette.duvetPrimary, Offset(w * 0.855f, h * 0.65f), Size(w * 0.02f, h * 0.06f))
}

private fun DrawScope.drawBed(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height

    // Soft floor shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = if (palette.isDark) 0.40f else 0.16f),
        topLeft = Offset(w * 0.04f, h * 0.90f),
        size = Size(w * 0.92f, h * 0.09f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Wooden headboard
    drawRoundRect(
        color = palette.woodDark,
        topLeft = Offset(w * 0.03f, h * 0.03f),
        size = Size(w * 0.86f, h * 0.34f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    // Slatted headboard lines
    drawRect(Color.Black.copy(alpha = 0.14f), Offset(w * 0.07f, h * 0.11f), Size(w * 0.78f, h * 0.022f))
    drawRect(Color.Black.copy(alpha = 0.14f), Offset(w * 0.07f, h * 0.19f), Size(w * 0.78f, h * 0.022f))

    // Crisp mattress base
    drawRoundRect(
        color = palette.pillowColor.copy(alpha = 0.94f),
        topLeft = Offset(w * 0.03f, h * 0.30f),
        size = Size(w * 0.94f, h * 0.50f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Pillows
    drawRoundRect(
        color = palette.pillowColor,
        topLeft = Offset(w * 0.07f, h * 0.21f),
        size = Size(w * 0.38f, h * 0.18f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = palette.pillowColor.copy(alpha = 0.90f),
        topLeft = Offset(w * 0.48f, h * 0.21f),
        size = Size(w * 0.38f, h * 0.18f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    // Folded sheet lip
    drawRect(
        color = palette.pillowColor.copy(alpha = 0.82f),
        topLeft = Offset(w * 0.03f, h * 0.39f),
        size = Size(w * 0.94f, h * 0.05f)
    )
    // Duvet cover
    drawRect(
        color = palette.duvetPrimary,
        topLeft = Offset(w * 0.03f, h * 0.43f),
        size = Size(w * 0.94f, h * 0.37f)
    )
    // Folded throw blanket at foot of bed
    drawRect(
        color = palette.throwBlanket,
        topLeft = Offset(w * 0.03f, h * 0.64f),
        size = Size(w * 0.94f, h * 0.16f)
    )
    // Wooden bed frame rails & feet
    drawRect(
        color = palette.woodDark,
        topLeft = Offset(w * 0.02f, h * 0.79f),
        size = Size(w * 0.96f, h * 0.085f)
    )
    drawRect(palette.woodDark, Offset(w * 0.06f, h * 0.875f), Size(w * 0.055f, h * 0.10f))
    drawRect(palette.woodDark, Offset(w * 0.885f, h * 0.875f), Size(w * 0.055f, h * 0.10f))
}

private fun DrawScope.drawTrashCan(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height

    // Floor shadow
    drawOval(
        color = Color.Black.copy(alpha = if (palette.isDark) 0.42f else 0.18f),
        topLeft = Offset(w * 0.14f, h * 0.84f),
        size = Size(w * 0.72f, h * 0.13f)
    )
    // Tapered bin body
    val body = Path().apply {
        moveTo(w * 0.14f, h * 0.18f)
        lineTo(w * 0.86f, h * 0.18f)
        lineTo(w * 0.77f, h * 0.89f)
        lineTo(w * 0.23f, h * 0.89f)
        close()
    }
    drawPath(body, palette.trashBody)

    // Subtle vertical rib highlights
    val rib = Color.White.copy(alpha = 0.16f)
    val ribW = maxOf(2f, w * 0.025f)
    drawLine(rib, Offset(w * 0.31f, h * 0.28f), Offset(w * 0.35f, h * 0.82f), strokeWidth = ribW, cap = StrokeCap.Round)
    drawLine(rib, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.50f, h * 0.83f), strokeWidth = ribW, cap = StrokeCap.Round)
    drawLine(rib, Offset(w * 0.69f, h * 0.28f), Offset(w * 0.65f, h * 0.82f), strokeWidth = ribW, cap = StrokeCap.Round)

    // Dark top interior
    drawOval(
        color = Color(0xFF1F2426),
        topLeft = Offset(w * 0.14f, h * 0.11f),
        size = Size(w * 0.72f, h * 0.15f)
    )
    // Warm wood/brass rim
    drawOval(
        color = palette.trashRim,
        topLeft = Offset(w * 0.14f, h * 0.11f),
        size = Size(w * 0.72f, h * 0.15f),
        style = Stroke(width = min(w, h) * 0.038f)
    )
}

private fun DrawScope.drawCustomObject(palette: RoomVisualPalette) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = palette.woodMid.copy(alpha = 0.88f),
        topLeft = Offset(w * 0.06f, h * 0.10f),
        size = Size(w * 0.88f, h * 0.80f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = palette.woodLight,
        topLeft = Offset(w * 0.06f, h * 0.10f),
        size = Size(w * 0.88f, h * 0.80f),
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(width = 3f)
    )
}
