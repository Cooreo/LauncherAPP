package com.roomlauncher.app.ui.room

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.roomlauncher.app.data.RoomType
import com.roomlauncher.app.ui.theme.RoomVisualPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Adds room-specific architectural details (Living Room wainscoting & spotlights, Kitchen subway tile
 * backsplash & checkered ceramic floor, Gaming Room RGB acoustic hexagons & neon desk floor)
 * seamlessly on top of the active Room Style shell.
 */
internal fun DrawScope.drawRoomTypeArchitectureAccent(
    roomType: RoomType,
    palette: RoomVisualPalette
) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.61f

    when (roomType) {
        RoomType.BEDROOM -> Unit // Classic bedroom shell is already complete

        RoomType.LIVING_ROOM -> {
            // Subtle architectural wainscoting wall trim panels
            val panelTop = h * 0.31f
            val panelBottom = floorY - h * 0.03f
            val panelStroke = palette.baseboardShadow.copy(alpha = if (palette.isDark) 0.45f else 0.32f)
            val cols = 3
            val margin = w * 0.045f
            val gap = w * 0.03f
            val panelW = (w - margin * 2f - gap * (cols - 1)) / cols
            for (i in 0 until cols) {
                val left = margin + i * (panelW + gap)
                drawRoundRect(
                    color = panelStroke,
                    topLeft = Offset(left, panelTop),
                    size = Size(panelW, panelBottom - panelTop),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 2.2f)
                )
            }
            // Chair rail molding across mid-wall
            drawLine(
                color = palette.baseboard.copy(alpha = 0.72f),
                start = Offset(0f, h * 0.295f),
                end = Offset(w, h * 0.295f),
                strokeWidth = 5f
            )
            // Warm recessed ceiling spotlight cones
            listOf(0.22f, 0.50f, 0.78f).forEach { sx ->
                val cx = w * sx
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x36FFE3A3), Color.Transparent),
                        center = Offset(cx, h * 0.075f),
                        radius = w * 0.22f
                    ),
                    radius = w * 0.22f,
                    center = Offset(cx, h * 0.075f)
                )
            }
        }

        RoomType.KITCHEN -> {
            // Crisp subway-tile backsplash band across mid-wall
            val tileTop = h * 0.25f
            val tileBottom = floorY - h * 0.018f
            val tileBg = if (palette.isDark) {
                palette.wallUpper.copy(alpha = 0.65f)
            } else {
                Color(0xFFFDFBF7).copy(alpha = 0.65f)
            }
            drawRect(tileBg, Offset(0f, tileTop), Size(w, tileBottom - tileTop))

            val grout = if (palette.isDark) {
                palette.consoleAccent.copy(alpha = 0.16f)
            } else {
                palette.baseboardShadow.copy(alpha = 0.45f)
            }
            val rowCount = 10
            val colCount = 7
            val rowH = (tileBottom - tileTop) / rowCount
            val colW = w / colCount
            for (r in 0..rowCount) {
                val y = tileTop + r * rowH
                drawLine(grout, Offset(0f, y), Offset(w, y), strokeWidth = 1.3f)
                if (r < rowCount) {
                    val stagger = if (r % 2 == 0) 0f else colW * 0.5f
                    for (c in 0..colCount) {
                        val x = c * colW - stagger
                        if (x in 0f..w) {
                            drawLine(grout, Offset(x, y), Offset(x, y + rowH), strokeWidth = 1.2f)
                        }
                    }
                }
            }
            // Hanging pendant kitchen lamps near top ceiling
            listOf(0.28f, 0.54f).forEach { lx ->
                val cx = w * lx
                drawLine(palette.woodDark, Offset(cx, 0f), Offset(cx, h * 0.095f), strokeWidth = 2.4f)
                val shade = Path().apply {
                    moveTo(cx - w * 0.048f, h * 0.125f)
                    lineTo(cx - w * 0.022f, h * 0.092f)
                    lineTo(cx + w * 0.022f, h * 0.092f)
                    lineTo(cx + w * 0.048f, h * 0.125f)
                    close()
                }
                drawPath(shade, palette.woodDark)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x45FFE59D), Color.Transparent),
                        center = Offset(cx, h * 0.135f),
                        radius = w * 0.14f
                    ),
                    radius = w * 0.14f,
                    center = Offset(cx, h * 0.135f)
                )
            }
            // Subtle ceramic checkered tile tint on kitchen floor
            val tileCols = 8
            val tileRows = 6
            val tW = w / tileCols
            val tH = (h - floorY) / tileRows
            val checkTint = if (palette.isDark) Color.White.copy(alpha = 0.045f) else Color.White.copy(alpha = 0.20f)
            for (r in 0 until tileRows) {
                for (c in 0 until tileCols) {
                    if ((r + c) % 2 == 0) {
                        drawRect(checkTint, Offset(c * tW, floorY + r * tH), Size(tW, tH))
                    }
                }
            }
        }

        RoomType.GAMING_ROOM -> {
            val accent = palette.consoleAccent
            val secondary = palette.clockAccent
            // Glowing ambient RGB wall wash along top wall
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = if (palette.isDark) 0.26f else 0.15f),
                        secondary.copy(alpha = if (palette.isDark) 0.12f else 0.06f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.30f
                ),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.30f)
            )
            // Hexagonal RGB acoustic wall tiles cluster on upper-right wall
            val hexR = w * 0.046f
            val hexCenters = listOf(
                0.70f to 0.12f, 0.79f to 0.12f, 0.88f to 0.12f,
                0.745f to 0.165f, 0.835f to 0.165f,
                0.79f to 0.21f
            )
            hexCenters.forEachIndexed { idx, (hx, hy) ->
                val cx = w * hx
                val cy = h * hy
                val hexPath = Path()
                for (i in 0 until 6) {
                    val angle = (i * PI / 3.0 - PI / 6.0).toFloat()
                    val px = cx + cos(angle) * hexR
                    val py = cy + sin(angle) * hexR
                    if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
                }
                hexPath.close()
                drawPath(hexPath, Color(0xFF10131C).copy(alpha = 0.65f))
                drawPath(
                    hexPath,
                    color = (if (idx % 2 == 0) accent else secondary).copy(alpha = 0.70f),
                    style = Stroke(width = 2.2f)
                )
            }
            // Neon baseboard trim strip
            drawLine(
                color = accent.copy(alpha = 0.78f),
                start = Offset(0f, floorY - 2f),
                end = Offset(w, floorY - 2f),
                strokeWidth = 3.5f
            )
        }
    }
}

// =========================================================================
// LIVING ROOM FURNITURE (Sofa, Fireplace, Coffee Table, Vinyl Hi-Fi)
// =========================================================================

internal fun DrawScope.drawSofa(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else w * 0.045f

    // Ground shadow
    drawRoundRect(
        Color.Black.copy(alpha = if (palette.isDark) 0.38f else 0.14f),
        Offset(w * 0.05f, h * 0.82f),
        Size(w * 0.90f, h * 0.14f),
        CornerRadius(r, r)
    )
    // Tapered wooden legs
    val legW = w * 0.045f
    val legH = h * 0.13f
    drawRoundRect(palette.woodDark, Offset(w * 0.10f, h * 0.79f), Size(legW, legH), CornerRadius(2f, 2f))
    drawRoundRect(palette.woodDark, Offset(w * 0.855f, h * 0.79f), Size(legW, legH), CornerRadius(2f, 2f))

    // Backrest frame
    drawRoundRect(palette.duvetPrimary, Offset(w * 0.08f, h * 0.14f), Size(w * 0.84f, h * 0.48f), CornerRadius(r, r))
    // 3 plush back cushions
    val cushionGap = w * 0.018f
    val cushionW = (w * 0.76f - cushionGap * 2f) / 3f
    for (i in 0 until 3) {
        val cx = w * 0.12f + i * (cushionW + cushionGap)
        drawRoundRect(
            palette.duvetPrimary.copy(alpha = 0.88f),
            Offset(cx, h * 0.19f),
            Size(cushionW, h * 0.34f),
            CornerRadius(r * 0.8f, r * 0.8f)
        )
        drawRoundRect(
            Color.White.copy(alpha = 0.10f),
            Offset(cx, h * 0.19f),
            Size(cushionW, h * 0.34f),
            CornerRadius(r * 0.8f, r * 0.8f),
            style = Stroke(width = 1.6f)
        )
    }

    // Accent throw pillows on left & right corners
    drawRoundRect(palette.throwBlanket, Offset(w * 0.14f, h * 0.31f), Size(w * 0.15f, h * 0.22f), CornerRadius(r * 0.7f, r * 0.7f))
    drawRoundRect(palette.pillowColor, Offset(w * 0.71f, h * 0.31f), Size(w * 0.15f, h * 0.22f), CornerRadius(r * 0.7f, r * 0.7f))

    // Seat cushions & lower base
    drawRoundRect(palette.duvetPrimary, Offset(w * 0.08f, h * 0.50f), Size(w * 0.84f, h * 0.22f), CornerRadius(r * 0.7f, r * 0.7f))
    drawLine(Color.Black.copy(alpha = 0.18f), Offset(w * 0.36f, h * 0.52f), Offset(w * 0.36f, h * 0.70f), strokeWidth = 2f)
    drawLine(Color.Black.copy(alpha = 0.18f), Offset(w * 0.64f, h * 0.52f), Offset(w * 0.64f, h * 0.70f), strokeWidth = 2f)
    drawRoundRect(palette.woodMid, Offset(w * 0.07f, h * 0.70f), Size(w * 0.86f, h * 0.12f), CornerRadius(r * 0.5f, r * 0.5f))

    // Rounded side armrests
    drawRoundRect(palette.duvetPrimary, Offset(w * 0.03f, h * 0.36f), Size(w * 0.11f, h * 0.40f), CornerRadius(r, r))
    drawRoundRect(palette.duvetPrimary, Offset(w * 0.86f, h * 0.36f), Size(w * 0.11f, h * 0.40f), CornerRadius(r, r))
}

internal fun DrawScope.drawFireplace(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 8f

    // Chimney breast / surround
    drawRoundRect(palette.woodDark, Offset(w * 0.08f, h * 0.16f), Size(w * 0.84f, h * 0.78f), CornerRadius(r, r))
    // Subtle stone/brick texture lines
    val brickColor = palette.woodMid.copy(alpha = 0.55f)
    for (row in 1..4) {
        val y = h * (0.16f + row * 0.15f)
        drawLine(brickColor, Offset(w * 0.09f, y), Offset(w * 0.91f, y), strokeWidth = 1.6f)
    }

    // Thick wood mantel shelf on top
    drawRoundRect(palette.woodLight, Offset(w * 0.02f, h * 0.11f), Size(w * 0.96f, h * 0.09f), CornerRadius(r * 0.5f, r * 0.5f))
    // Small candle & potted plant sitting on mantel
    drawRoundRect(Color(0xFFFFF3D6), Offset(w * 0.16f, h * 0.03f), Size(w * 0.055f, h * 0.08f), CornerRadius(2f, 2f))
    drawCircle(Color(0xFFFFB703), w * 0.018f, Offset(w * 0.187f, h * 0.02f))
    drawRoundRect(palette.throwBlanket, Offset(w * 0.76f, h * 0.045f), Size(w * 0.09f, h * 0.065f), CornerRadius(3f, 3f))
    drawCircle(Color(0xFF4F9D69), w * 0.045f, Offset(w * 0.805f, h * 0.032f))

    // Recessed firebox opening
    val boxL = w * 0.22f
    val boxT = h * 0.34f
    val boxW = w * 0.56f
    val boxH = h * 0.55f
    drawRoundRect(Color(0xFF120C0B), Offset(boxL, boxT), Size(boxW, boxH), CornerRadius(r * 1.4f, r * 1.4f))

    // Warm radial firelight glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x99FF9F1C), Color(0x40E63946), Color.Transparent),
            center = Offset(w * 0.50f, boxT + boxH * 0.65f),
            radius = boxW * 0.65f
        ),
        radius = boxW * 0.65f,
        center = Offset(w * 0.50f, boxT + boxH * 0.65f)
    )

    // Layered cozy flame tongues
    val outerFlame = Path().apply {
        moveTo(w * 0.36f, boxT + boxH * 0.82f)
        quadraticTo(w * 0.38f, boxT + boxH * 0.34f, w * 0.50f, boxT + boxH * 0.18f)
        quadraticTo(w * 0.62f, boxT + boxH * 0.34f, w * 0.64f, boxT + boxH * 0.82f)
        close()
    }
    drawPath(outerFlame, Color(0xFFFF5926))
    val innerFlame = Path().apply {
        moveTo(w * 0.42f, boxT + boxH * 0.82f)
        quadraticTo(w * 0.45f, boxT + boxH * 0.48f, w * 0.50f, boxT + boxH * 0.36f)
        quadraticTo(w * 0.56f, boxT + boxH * 0.48f, w * 0.58f, boxT + boxH * 0.82f)
        close()
    }
    drawPath(innerFlame, Color(0xFFFFD166))

    // Crossed firewood logs
    drawRoundRect(Color(0xFF5C341E), Offset(w * 0.30f, boxT + boxH * 0.78f), Size(w * 0.40f, h * 0.075f), CornerRadius(4f, 4f))
    // Base hearth slab
    drawRoundRect(palette.woodMid, Offset(w * 0.03f, h * 0.88f), Size(w * 0.94f, h * 0.09f), CornerRadius(r * 0.5f, r * 0.5f))
}

internal fun DrawScope.drawCoffeeTable(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 10f

    // Shadow
    drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(w * 0.10f, h * 0.80f), Size(w * 0.80f, h * 0.14f), CornerRadius(r, r))
    // Legs
    drawRoundRect(palette.woodDark, Offset(w * 0.14f, h * 0.48f), Size(w * 0.08f, h * 0.38f), CornerRadius(3f, 3f))
    drawRoundRect(palette.woodDark, Offset(w * 0.78f, h * 0.48f), Size(w * 0.08f, h * 0.38f), CornerRadius(3f, 3f))
    // Lower magazine shelf
    drawRoundRect(palette.woodDark, Offset(w * 0.16f, h * 0.67f), Size(w * 0.68f, h * 0.08f), CornerRadius(3f, 3f))
    // Tabletop
    drawRoundRect(palette.woodLight, Offset(w * 0.04f, h * 0.36f), Size(w * 0.92f, h * 0.16f), CornerRadius(r, r))
    drawRoundRect(palette.woodMid, Offset(w * 0.04f, h * 0.46f), Size(w * 0.92f, h * 0.07f), CornerRadius(r * 0.4f, r * 0.4f))

    // Stacked open magazine / book + steaming coffee mug on top
    drawRoundRect(palette.throwBlanket, Offset(w * 0.16f, h * 0.26f), Size(w * 0.34f, h * 0.10f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFFFAF6EE), Offset(w * 0.20f, h * 0.22f), Size(w * 0.26f, h * 0.05f), CornerRadius(2f, 2f))
    drawRoundRect(palette.pillowColor, Offset(w * 0.62f, h * 0.17f), Size(w * 0.18f, h * 0.19f), CornerRadius(4f, 4f))
    drawCircle(palette.pillowColor, w * 0.045f, Offset(w * 0.81f, h * 0.26f), style = Stroke(width = 3f))
}

internal fun DrawScope.drawVinylPlayer(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 8f

    // Record console cabinet legs & body
    drawRoundRect(palette.woodDark, Offset(w * 0.12f, h * 0.85f), Size(w * 0.07f, h * 0.12f), CornerRadius(2f, 2f))
    drawRoundRect(palette.woodDark, Offset(w * 0.81f, h * 0.85f), Size(w * 0.07f, h * 0.12f), CornerRadius(2f, 2f))
    drawRoundRect(palette.woodMid, Offset(w * 0.06f, h * 0.42f), Size(w * 0.88f, h * 0.46f), CornerRadius(r, r))
    drawRoundRect(palette.woodDark, Offset(w * 0.12f, h * 0.49f), Size(w * 0.76f, h * 0.33f), CornerRadius(r * 0.5f, r * 0.5f))

    // Colorful upright vinyl sleeves inside cabinet
    val sleeveColors = listOf(Color(0xFFE76F51), Color(0xFF2A9D8F), Color(0xFFE9C46A), Color(0xFF8ECAE6), Color(0xFFF4A261), Color(0xFF9B5DE5))
    sleeveColors.forEachIndexed { idx, col ->
        val sx = w * (0.16f + idx * 0.11f)
        drawRect(col, Offset(sx, h * 0.53f), Size(w * 0.075f, h * 0.27f))
    }

    // Turntable plinth resting on top
    drawRoundRect(palette.consoleBody, Offset(w * 0.10f, h * 0.28f), Size(w * 0.80f, h * 0.16f), CornerRadius(r * 0.6f, r * 0.6f))
    // Spinning vinyl disc peeking above plinth
    val discCenter = Offset(w * 0.45f, h * 0.25f)
    val discR = min(w, h) * 0.22f
    drawCircle(Color(0xFF14161D), discR, discCenter)
    drawCircle(Color.White.copy(alpha = 0.18f), discR * 0.76f, discCenter, style = Stroke(width = 1.4f))
    drawCircle(palette.consoleAccent, discR * 0.34f, discCenter)
    drawCircle(Color(0xFF14161D), discR * 0.09f, discCenter)
    // Tonearm
    drawLine(Color(0xFFE2E8F0), Offset(w * 0.78f, h * 0.16f), Offset(w * 0.56f, h * 0.26f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawCircle(palette.consoleAccent, 4f, Offset(w * 0.82f, h * 0.36f))
}

// =========================================================================
// KITCHEN FURNITURE (Fridge, Espresso Maker, Stove & Oven, Pantry Counter)
// =========================================================================

internal fun DrawScope.drawFridge(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else w * 0.09f

    val bodyColor = if (palette.isDark) Color(0xFF2B3442) else Color(0xFFEFF4F7)
    val edgeColor = if (palette.isDark) Color(0xFF475569) else Color(0xFFC5D2DC)

    // Drop shadow
    drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(w * 0.10f, h * 0.06f), Size(w * 0.86f, h * 0.91f), CornerRadius(r, r))
    // Main fridge chassis
    drawRoundRect(bodyColor, Offset(w * 0.06f, h * 0.04f), Size(w * 0.88f, h * 0.92f), CornerRadius(r, r))
    drawRoundRect(edgeColor, Offset(w * 0.06f, h * 0.04f), Size(w * 0.88f, h * 0.92f), CornerRadius(r, r), style = Stroke(width = 2.4f))

    // Freezer / Fridge divider seam
    val seamY = h * 0.37f
    drawLine(edgeColor, Offset(w * 0.07f, seamY), Offset(w * 0.93f, seamY), strokeWidth = 3f)

    // Sleek vertical handles
    val handleCol = if (palette.isDark) palette.consoleAccent else Color(0xFF64748B)
    drawRoundRect(handleCol, Offset(w * 0.78f, h * 0.14f), Size(w * 0.055f, h * 0.16f), CornerRadius(4f, 4f))
    drawRoundRect(handleCol, Offset(w * 0.78f, h * 0.44f), Size(w * 0.055f, h * 0.28f), CornerRadius(4f, 4f))

    // Water & ice dispenser screen on upper door
    drawRoundRect(Color(0xFF151C26), Offset(w * 0.22f, h * 0.13f), Size(w * 0.30f, h * 0.17f), CornerRadius(6f, 6f))
    drawRoundRect(palette.consoleAccent.copy(alpha = 0.75f), Offset(w * 0.26f, h * 0.16f), Size(w * 0.22f, h * 0.035f), CornerRadius(2f, 2f))

    // Cute colorful fridge magnets & pinned note on lower door
    drawRoundRect(palette.stickyNoteFill, Offset(w * 0.18f, h * 0.48f), Size(w * 0.24f, h * 0.12f), CornerRadius(3f, 3f))
    drawCircle(Color(0xFFFF595E), w * 0.045f, Offset(w * 0.30f, h * 0.48f))
    drawCircle(Color(0xFF8AC926), w * 0.04f, Offset(w * 0.52f, h * 0.56f))
    drawCircle(Color(0xFF1982C4), w * 0.04f, Offset(w * 0.42f, h * 0.68f))
}

internal fun DrawScope.drawCoffeeMaker(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 9f

    // Espresso machine base & back column
    drawRoundRect(palette.woodMid, Offset(w * 0.04f, h * 0.84f), Size(w * 0.92f, h * 0.12f), CornerRadius(4f, 4f))
    drawRoundRect(palette.consoleBody, Offset(w * 0.14f, h * 0.14f), Size(w * 0.72f, h * 0.72f), CornerRadius(r, r))
    // Brushed top warmer plate
    drawRoundRect(palette.trashRim, Offset(w * 0.16f, h * 0.12f), Size(w * 0.68f, h * 0.10f), CornerRadius(4f, 4f))
    // Brew bay recess
    drawRoundRect(Color(0xFF0E1117), Offset(w * 0.24f, h * 0.38f), Size(w * 0.52f, h * 0.42f), CornerRadius(5f, 5f))
    // Pressure gauge & glowing power button
    drawCircle(Color(0xFFFAF6EE), min(w, h) * 0.09f, Offset(w * 0.50f, h * 0.28f))
    drawLine(Color(0xFFE63946), Offset(w * 0.50f, h * 0.28f), Offset(w * 0.54f, h * 0.24f), strokeWidth = 2f)
    drawCircle(palette.consoleAccent, 4.5f, Offset(w * 0.30f, h * 0.28f))
    drawCircle(Color(0xFFFFB703), 4.5f, Offset(w * 0.70f, h * 0.28f))
    // Portafilter spout
    drawRect(Color(0xFF94A3B8), Offset(w * 0.41f, h * 0.38f), Size(w * 0.18f, h * 0.09f))
    // Coffee cup sitting on drip tray
    drawRoundRect(palette.throwBlanket, Offset(w * 0.37f, h * 0.56f), Size(w * 0.26f, h * 0.22f), CornerRadius(5f, 5f))
    drawCircle(palette.throwBlanket, w * 0.05f, Offset(w * 0.65f, h * 0.67f), style = Stroke(width = 3f))
    // Golden espresso stream
    drawLine(Color(0xFFC68B59), Offset(w * 0.50f, h * 0.47f), Offset(w * 0.50f, h * 0.56f), strokeWidth = 3f)
}

internal fun DrawScope.drawStove(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 8f

    val bodyColor = if (palette.isDark) Color(0xFF232833) else Color(0xFFE2E8F0)
    // Main range cabinet
    drawRoundRect(bodyColor, Offset(w * 0.05f, h * 0.16f), Size(w * 0.90f, h * 0.80f), CornerRadius(r, r))
    // Stovetop surface
    drawRoundRect(Color(0xFF141821), Offset(w * 0.06f, h * 0.14f), Size(w * 0.88f, h * 0.12f), CornerRadius(4f, 4f))
    // Glowing cooking burners & simmer pot
    drawRoundRect(Color(0xFFFF6B35), Offset(w * 0.18f, h * 0.17f), Size(w * 0.16f, h * 0.045f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFFFF6B35), Offset(w * 0.64f, h * 0.17f), Size(w * 0.16f, h * 0.045f), CornerRadius(3f, 3f))
    // Cooking pot on left burner
    drawRoundRect(palette.throwBlanket, Offset(w * 0.16f, h * 0.03f), Size(w * 0.20f, h * 0.13f), CornerRadius(4f, 4f))
    drawLine(Color(0xFF94A3B8), Offset(w * 0.14f, h * 0.03f), Offset(w * 0.38f, h * 0.03f), strokeWidth = 3f)

    // Control dials bar
    listOf(0.18f, 0.32f, 0.68f, 0.82f).forEach { dx ->
        drawCircle(Color(0xFF475569), min(w, h) * 0.042f, Offset(w * dx, h * 0.33f))
    }
    // Digital oven clock
    drawRoundRect(Color(0xFF0A0D12), Offset(w * 0.42f, h * 0.28f), Size(w * 0.16f, h * 0.09f), CornerRadius(3f, 3f))
    drawRect(palette.consoleAccent, Offset(w * 0.45f, h * 0.31f), Size(w * 0.10f, h * 0.03f))

    // Oven handle & warm glowing glass door
    drawRoundRect(Color(0xFF94A3B8), Offset(w * 0.18f, h * 0.42f), Size(w * 0.64f, h * 0.04f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1A1412), Offset(w * 0.15f, h * 0.50f), Size(w * 0.70f, h * 0.36f), CornerRadius(6f, 6f))
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0x55FF9F1C), Color(0x18FF5926))),
        topLeft = Offset(w * 0.18f, h * 0.53f),
        size = Size(w * 0.64f, h * 0.30f),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

internal fun DrawScope.drawKitchenCounter(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 7f

    // Lower island cabinetry
    drawRoundRect(palette.woodMid, Offset(w * 0.04f, h * 0.30f), Size(w * 0.92f, h * 0.64f), CornerRadius(r, r))
    // Butcher-block oak countertop
    drawRoundRect(palette.woodLight, Offset(w * 0.01f, h * 0.22f), Size(w * 0.98f, h * 0.12f), CornerRadius(r * 0.7f, r * 0.7f))

    // 3 tidy cabinet doors & brass pulls
    val doors = 3
    val pad = w * 0.03f
    val doorW = (w * 0.88f - pad * (doors - 1)) / doors
    for (i in 0 until doors) {
        val dx = w * 0.06f + i * (doorW + pad)
        drawRoundRect(palette.woodDark, Offset(dx, h * 0.39f), Size(doorW, h * 0.48f), CornerRadius(4f, 4f))
        drawRoundRect(palette.trashRim, Offset(dx + doorW * 0.35f, h * 0.44f), Size(doorW * 0.30f, h * 0.04f), CornerRadius(2f, 2f))
    }

    // Colorful fruit bowl + grocery bag / spice jars on top of counter
    drawRoundRect(palette.duvetPrimary, Offset(w * 0.12f, h * 0.11f), Size(w * 0.22f, h * 0.11f), CornerRadius(8f, 8f))
    drawCircle(Color(0xFFFF595E), min(w, h) * 0.065f, Offset(w * 0.18f, h * 0.11f))
    drawCircle(Color(0xFFFFCA3A), min(w, h) * 0.06f, Offset(w * 0.24f, h * 0.09f))
    drawCircle(Color(0xFF8AC926), min(w, h) * 0.06f, Offset(w * 0.29f, h * 0.11f))

    // Ceramic canisters on right
    drawRoundRect(Color(0xFFFAF6EE), Offset(w * 0.66f, h * 0.06f), Size(w * 0.10f, h * 0.16f), CornerRadius(4f, 4f))
    drawRoundRect(palette.throwBlanket, Offset(w * 0.79f, h * 0.09f), Size(w * 0.09f, h * 0.13f), CornerRadius(4f, 4f))
}

// =========================================================================
// GAMING ROOM FURNITURE (Gaming PC, Arcade Cabinet, Streaming Desk, Trophy Shelf)
// =========================================================================

internal fun DrawScope.drawGamingPc(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 7f
    val neon = palette.consoleAccent
    val neon2 = palette.clockAccent

    // Desk legs & tabletop
    drawRect(palette.tvBezel, Offset(w * 0.07f, h * 0.62f), Size(w * 0.06f, h * 0.34f))
    drawRect(palette.tvBezel, Offset(w * 0.87f, h * 0.62f), Size(w * 0.06f, h * 0.34f))
    drawRoundRect(palette.woodDark, Offset(w * 0.02f, h * 0.56f), Size(w * 0.96f, h * 0.09f), CornerRadius(4f, 4f))
    // Glowing RGB desk edge strip
    drawLine(neon, Offset(w * 0.03f, h * 0.64f), Offset(w * 0.97f, h * 0.64f), strokeWidth = 3f)

    // Wide main gaming monitor + vertical side monitor
    drawRoundRect(palette.tvBezel, Offset(w * 0.08f, h * 0.12f), Size(w * 0.48f, h * 0.34f), CornerRadius(r, r))
    drawRoundRect(
        brush = Brush.linearGradient(listOf(neon.copy(alpha = 0.55f), neon2.copy(alpha = 0.45f), palette.tvScreen)),
        topLeft = Offset(w * 0.105f, h * 0.15f),
        size = Size(w * 0.43f, h * 0.28f),
        cornerRadius = CornerRadius(r * 0.5f, r * 0.5f)
    )
    // Monitor stand
    drawRect(palette.tvBezel, Offset(w * 0.29f, h * 0.46f), Size(w * 0.06f, h * 0.10f))

    // Tempered-glass RGB PC tower on right of desk
    drawRoundRect(Color(0xFF0E1118), Offset(w * 0.62f, h * 0.14f), Size(w * 0.29f, h * 0.42f), CornerRadius(r, r))
    drawRoundRect(neon.copy(alpha = 0.75f), Offset(w * 0.62f, h * 0.14f), Size(w * 0.29f, h * 0.42f), CornerRadius(r, r), style = Stroke(width = 2f))
    // 3 glowing RGB cooling fans inside tower
    listOf(0.24f, 0.35f, 0.46f).forEachIndexed { i, fy ->
        val fanCol = if (i % 2 == 0) neon else neon2
        drawCircle(fanCol.copy(alpha = 0.30f), min(w, h) * 0.07f, Offset(w * 0.765f, h * fy))
        drawCircle(fanCol, min(w, h) * 0.055f, Offset(w * 0.765f, h * fy), style = Stroke(width = 2.2f))
    }

    // Mechanical RGB keyboard + mouse on desk
    drawRoundRect(Color(0xFF141821), Offset(w * 0.14f, h * 0.51f), Size(w * 0.30f, h * 0.055f), CornerRadius(3f, 3f))
    drawLine(neon2, Offset(w * 0.16f, h * 0.54f), Offset(w * 0.42f, h * 0.54f), strokeWidth = 2.4f)
    drawRoundRect(neon, Offset(w * 0.48f, h * 0.51f), Size(w * 0.065f, h * 0.05f), CornerRadius(4f, 4f))
}

internal fun DrawScope.drawArcadeCabinet(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 8f
    val neon = palette.consoleAccent
    val neon2 = palette.clockAccent

    // Classic upright arcade silhouette
    drawRoundRect(Color(0xFF121520), Offset(w * 0.10f, h * 0.04f), Size(w * 0.80f, h * 0.92f), CornerRadius(r, r))
    drawRoundRect(neon.copy(alpha = 0.80f), Offset(w * 0.10f, h * 0.04f), Size(w * 0.80f, h * 0.92f), CornerRadius(r, r), style = Stroke(width = 2.4f))

    // Lit neon marquee header
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(neon, neon2)),
        topLeft = Offset(w * 0.15f, h * 0.08f),
        size = Size(w * 0.70f, h * 0.11f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // CRT arcade screen with retro geometric invader/wave glow
    drawRoundRect(Color(0xFF060B18), Offset(w * 0.16f, h * 0.23f), Size(w * 0.68f, h * 0.32f), CornerRadius(6f, 6f))
    val waveLine = Path().apply {
        moveTo(w * 0.22f, h * 0.42f)
        lineTo(w * 0.38f, h * 0.30f)
        lineTo(w * 0.52f, h * 0.44f)
        lineTo(w * 0.68f, h * 0.32f)
        lineTo(w * 0.78f, h * 0.38f)
    }
    drawPath(waveLine, neon, style = Stroke(width = 3.2f, cap = StrokeCap.Round))

    // Slanted control deck with joystick & glowing buttons
    drawRoundRect(palette.woodDark, Offset(w * 0.06f, h * 0.57f), Size(w * 0.88f, h * 0.11f), CornerRadius(5f, 5f))
    drawLine(Color(0xFFE2E8F0), Offset(w * 0.27f, h * 0.62f), Offset(w * 0.27f, h * 0.52f), strokeWidth = 3f)
    drawCircle(Color(0xFFFF3B30), min(w, h) * 0.065f, Offset(w * 0.27f, h * 0.51f))
    drawCircle(Color(0xFF06D6A0), min(w, h) * 0.045f, Offset(w * 0.55f, h * 0.62f))
    drawCircle(Color(0xFFFFD166), min(w, h) * 0.045f, Offset(w * 0.68f, h * 0.62f))
    drawCircle(neon, min(w, h) * 0.045f, Offset(w * 0.80f, h * 0.62f))

    // Coin-door slots at bottom
    drawRoundRect(Color(0xFF1F2433), Offset(w * 0.30f, h * 0.74f), Size(w * 0.40f, h * 0.16f), CornerRadius(4f, 4f))
    drawRect(Color(0xFFFF595E), Offset(w * 0.37f, h * 0.79f), Size(w * 0.09f, h * 0.06f))
    drawRect(Color(0xFFFF595E), Offset(w * 0.54f, h * 0.79f), Size(w * 0.09f, h * 0.06f))
}

internal fun DrawScope.drawStreamingDesk(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 8f
    val neon = palette.consoleAccent
    val neon2 = palette.clockAccent

    // Creator desk slab & legs
    drawRect(palette.tvBezel, Offset(w * 0.08f, h * 0.58f), Size(w * 0.06f, h * 0.36f))
    drawRect(palette.tvBezel, Offset(w * 0.86f, h * 0.58f), Size(w * 0.06f, h * 0.36f))
    drawRoundRect(palette.consoleBody, Offset(w * 0.03f, h * 0.50f), Size(w * 0.94f, h * 0.11f), CornerRadius(r * 0.6f, r * 0.6f))
    drawLine(neon2, Offset(w * 0.05f, h * 0.60f), Offset(w * 0.95f, h * 0.60f), strokeWidth = 3f)

    // Glowing ring light on left
    val ringCenter = Offset(w * 0.22f, h * 0.24f)
    val ringR = min(w, h) * 0.17f
    drawLine(Color(0xFF64748B), Offset(w * 0.22f, h * 0.41f), Offset(w * 0.22f, h * 0.50f), strokeWidth = 3f)
    drawCircle(Color.White.copy(alpha = 0.22f), ringR * 1.35f, ringCenter)
    drawCircle(Color.White, ringR, ringCenter, style = Stroke(width = 4f))

    // Boom-arm studio mic in center
    drawLine(Color(0xFF94A3B8), Offset(w * 0.36f, h * 0.50f), Offset(w * 0.46f, h * 0.16f), strokeWidth = 3f)
    drawLine(Color(0xFF94A3B8), Offset(w * 0.46f, h * 0.16f), Offset(w * 0.55f, h * 0.24f), strokeWidth = 3f)
    drawRoundRect(Color(0xFF1E293B), Offset(w * 0.52f, h * 0.21f), Size(w * 0.08f, h * 0.16f), CornerRadius(10f, 10f))
    drawCircle(neon, 3.5f, Offset(w * 0.56f, h * 0.32f))

    // Stream deck glowing button pad + audio mixer on right
    drawRoundRect(Color(0xFF0F131D), Offset(w * 0.66f, h * 0.30f), Size(w * 0.24f, h * 0.20f), CornerRadius(5f, 5f))
    val padColors = listOf(neon, neon2, Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFA855F7), Color(0xFF38BDF8))
    for (row in 0..1) {
        for (col in 0..2) {
            drawRoundRect(
                padColors[row * 3 + col],
                Offset(w * (0.685f + col * 0.068f), h * (0.335f + row * 0.075f)),
                Size(w * 0.052f, h * 0.055f),
                CornerRadius(2f, 2f)
            )
        }
    }
}

internal fun DrawScope.drawTrophyShelf(palette: RoomVisualPalette, isVoxel: Boolean) {
    val w = size.width
    val h = size.height
    val r = if (isVoxel) 0f else 6f
    val neon = palette.consoleAccent

    // Floating illuminated display shelf with LED underglow
    drawRect(
        brush = Brush.verticalGradient(listOf(neon.copy(alpha = 0.42f), Color.Transparent)),
        topLeft = Offset(w * 0.06f, h * 0.78f),
        size = Size(w * 0.88f, h * 0.18f)
    )
    drawRoundRect(palette.woodDark, Offset(w * 0.04f, h * 0.68f), Size(w * 0.92f, h * 0.12f), CornerRadius(r, r))
    drawLine(neon, Offset(w * 0.06f, h * 0.79f), Offset(w * 0.94f, h * 0.79f), strokeWidth = 2.6f)

    // Golden trophy cup on left
    val gold = Color(0xFFFFC300)
    drawRoundRect(gold, Offset(w * 0.14f, h * 0.24f), Size(w * 0.16f, h * 0.26f), CornerRadius(6f, 6f))
    drawRect(gold, Offset(w * 0.20f, h * 0.50f), Size(w * 0.04f, h * 0.12f))
    drawRoundRect(Color(0xFF332100), Offset(w * 0.15f, h * 0.60f), Size(w * 0.14f, h * 0.08f), CornerRadius(2f, 2f))

    // Glowing diamond crystal collectible in center
    val crystal = Path().apply {
        moveTo(w * 0.50f, h * 0.16f)
        lineTo(w * 0.58f, h * 0.44f)
        lineTo(w * 0.50f, h * 0.66f)
        lineTo(w * 0.42f, h * 0.44f)
        close()
    }
    drawPath(crystal, neon)
    drawPath(crystal, Color.White.copy(alpha = 0.6f), style = Stroke(width = 1.6f))

    // Mounted controller collectible on right
    drawRoundRect(palette.clockAccent, Offset(w * 0.69f, h * 0.34f), Size(w * 0.20f, h * 0.24f), CornerRadius(10f, 10f))
    drawCircle(Color.White, 3.5f, Offset(w * 0.74f, h * 0.46f))
    drawCircle(Color.White, 3.5f, Offset(w * 0.84f, h * 0.46f))
}
