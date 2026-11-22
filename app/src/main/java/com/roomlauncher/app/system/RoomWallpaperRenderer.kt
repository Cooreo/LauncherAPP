package com.roomlauncher.app.system

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.ui.graphics.toArgb
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.WeatherCondition
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemes
import com.roomlauncher.app.ui.theme.RoomVisualPalette
import com.roomlauncher.app.ui.theme.accentNeonColors
import com.roomlauncher.app.ui.theme.mainDrawableRes
import com.roomlauncher.app.ui.theme.posterDrawableRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object RoomWallpaperRenderer {

    /**
     * Renders the current Room layout (Room Style + Color Palette + every movable object + custom photos)
     * using the true physical screen aspect ratio and Android SystemUI's ~10% wallpaper overscan zoom
     * compensation so the mirrored Home wallpaper matches the launcher's exact scale 1:1 (never zoomed in!).
     * Applies strictly via WallpaperManager.FLAG_SYSTEM so the Lock screen stays untouched.
     */
    suspend fun applyHomeScreenWallpaper(
        context: Context,
        config: RoomConfig,
        weather: WeatherState
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val (realW, realH) = getRealDisplaySize(context)
            val width = realW.coerceIn(720, 1440)
            val height = realH.coerceIn(1280, 3200)
            val palette = RoomThemes.effectivePalette(
                roomStyle = config.roomStyleId,
                themeId = config.themeId,
                gdSubTheme = config.geometryDashSubTheme
            )

            val gdMainBmp = if (config.roomStyleId == RoomStyleId.GEOMETRY_DASH) {
                runCatching {
                    BitmapFactory.decodeResource(context.resources, config.geometryDashSubTheme.mainDrawableRes())
                }.getOrNull()
            } else {
                null
            }
            val gdPosterBmp = if (config.roomStyleId == RoomStyleId.GEOMETRY_DASH) {
                runCatching {
                    BitmapFactory.decodeResource(context.resources, config.geometryDashSubTheme.posterDrawableRes())
                }.getOrNull()
            } else {
                null
            }

            val frameBitmap = decodeOptionalBitmap(config.framePhotoPath) ?: gdMainBmp
            val posterLeftBitmap = decodeOptionalBitmap(config.posterLeftPath)
            val posterRightBitmap = decodeOptionalBitmap(config.posterRightPath)

            // Android 11+ SystemUI applies ~10% overscan zoom to home wallpapers at rest.
            // By rendering the room furniture inside the central 1/1.10 safe viewport and seamlessly
            // extending the wall/floor into the 5% bleed margin, the displayed Home wallpaper matches 1:1!
            val overscanFactor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 1.10f else 1.0f
            val viewW = width / overscanFactor
            val viewH = height / overscanFactor
            val offsetX = (width - viewW) / 2f
            val offsetY = (height - viewH) / 2f

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw full-bleed background shell aligned to the inner viewport floor line
            val floorY = offsetY + viewH * 0.61f
            drawFullBleedShell(
                canvas = canvas,
                fullW = width.toFloat(),
                fullH = height.toFloat(),
                viewLeft = offsetX,
                viewTop = offsetY,
                viewW = viewW,
                viewH = viewH,
                floorY = floorY,
                palette = palette,
                style = config.roomStyleId,
                gdSubTheme = config.geometryDashSubTheme,
                gdMainBmp = gdMainBmp,
                gdPosterBmp = gdPosterBmp,
                posterLeft = posterLeftBitmap,
                posterRight = posterRightBitmap
            )

            val cal = Calendar.getInstance()
            val minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val isVoxel = config.roomStyleId == RoomStyleId.MINECRAFT

            config.hotspots.forEach { hotspot ->
                val rect = RectF(
                    offsetX + hotspot.x * viewW,
                    offsetY + hotspot.y * viewH,
                    offsetX + (hotspot.x + hotspot.width) * viewW,
                    offsetY + (hotspot.y + hotspot.height) * viewH
                )
                drawObject(canvas, hotspot, rect, palette, isVoxel, weather, frameBitmap, minutesOfDay)
            }

            val wm = WallpaperManager.getInstance(context)
            runCatching { wm.suggestDesiredDimensions(width, height) }
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)

            frameBitmap?.recycle()
            posterLeftBitmap?.recycle()
            posterRightBitmap?.recycle()
            bitmap.recycle()
            true
        }.getOrDefault(false)
    }

    private fun getRealDisplaySize(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                if (bounds.width() > 0 && bounds.height() > 0) {
                    return bounds.width() to bounds.height()
                }
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay?.getRealMetrics(metrics)
                if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
                    return metrics.widthPixels to metrics.heightPixels
                }
            }
        }
        val fallback = context.resources.displayMetrics
        return fallback.widthPixels to fallback.heightPixels
    }

    private fun decodeOptionalBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        return if (f.exists()) runCatching { BitmapFactory.decodeFile(path) }.getOrNull() else null
    }

    private fun drawFullBleedShell(
        canvas: Canvas,
        fullW: Float,
        fullH: Float,
        viewLeft: Float,
        viewTop: Float,
        viewW: Float,
        viewH: Float,
        floorY: Float,
        palette: RoomVisualPalette,
        style: RoomStyleId,
        gdSubTheme: GeometryDashSubTheme,
        gdMainBmp: Bitmap?,
        gdPosterBmp: Bitmap?,
        posterLeft: Bitmap?,
        posterRight: Bitmap?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (style == RoomStyleId.GEOMETRY_DASH) {
            val (neonPrimary, _, neonTertiary) = gdSubTheme.accentNeonColors()
            paint.color = palette.wallUpper.toArgb()
            canvas.drawRect(0f, 0f, fullW, floorY, paint)
            if (gdMainBmp != null) {
                paint.alpha = 64
                drawBitmapCenterCrop(canvas, gdMainBmp, RectF(0f, 0f, fullW, floorY), paint)
                paint.alpha = 255
            }
            paint.shader = LinearGradient(
                0f, 0f, 0f, floorY,
                intArrayOf(
                    (palette.wallUpper.toArgb() and 0x00FFFFFF) or 0xD6000000.toInt(),
                    (palette.wallMain.toArgb() and 0x00FFFFFF) or 0xA8000000.toInt(),
                    (palette.wallMain.toArgb() and 0x00FFFFFF) or 0xE0000000.toInt()
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, fullW, floorY, paint)
            paint.shader = null

            // Framed Geometry Dash posters
            val leftRect = RectF(viewLeft + viewW * 0.335f, viewTop + viewH * 0.265f, viewLeft + viewW * 0.555f, viewTop + viewH * 0.40f)
            val rightRect = RectF(viewLeft + viewW * 0.555f, viewTop + viewH * 0.185f, viewLeft + viewW * 0.735f, viewTop + viewH * 0.297f)
            if (gdPosterBmp != null) {
                drawBitmapCenterCrop(canvas, gdPosterBmp, leftRect, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = neonPrimary.toArgb()
                canvas.drawRect(leftRect, paint)
                paint.style = Paint.Style.FILL
            }
            if (gdMainBmp != null) {
                drawBitmapCenterCrop(canvas, gdMainBmp, rightRect, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = neonTertiary.toArgb()
                canvas.drawRect(rightRect, paint)
                paint.style = Paint.Style.FILL
            }

            // Neon ground laser + perspective grid floor
            paint.color = palette.floorMain.toArgb()
            canvas.drawRect(0f, floorY, fullW, fullH, paint)
            paint.color = neonPrimary.toArgb()
            paint.strokeWidth = 4f
            canvas.drawLine(0f, floorY, fullW, floorY, paint)
            paint.alpha = 85
            paint.strokeWidth = 2f
            listOf(0.665f, 0.725f, 0.795f, 0.875f, 0.955f).forEach { fy ->
                val y = viewTop + viewH * fy
                canvas.drawLine(0f, y, fullW, y, paint)
            }
            paint.alpha = 255
            val rugRect = RectF(viewLeft + viewW * 0.12f, viewTop + viewH * 0.665f, viewLeft + viewW * 0.88f, viewTop + viewH * 0.91f)
            paint.color = palette.rugFill.toArgb()
            canvas.drawRoundRect(rugRect, 20f, 20f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = neonPrimary.toArgb()
            canvas.drawRoundRect(rugRect, 20f, 20f, paint)
            paint.style = Paint.Style.FILL
            return
        }

        if (style == RoomStyleId.MINECRAFT) {
            val oakBase = if (palette.isDark) 0xFF4A3723.toInt() else 0xFFB8945F.toInt()
            val oakAlt = if (palette.isDark) 0xFF3E2D1C.toInt() else 0xFFA6824F.toInt()
            paint.color = oakBase
            canvas.drawRect(0f, 0f, fullW, floorY, paint)
            val cols = 10
            val rows = 15
            val cw = fullW / cols
            val ch = floorY / rows
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if ((r + c) % 3 == 0) {
                        paint.color = oakAlt
                        canvas.drawRect(c * cw, r * ch, (c + 1) * cw, (r + 1) * ch, paint)
                    }
                }
            }
            // Cobblestone strip
            paint.color = if (palette.isDark) 0xFF3A3D42.toInt() else 0xFF7D8084.toInt()
            canvas.drawRect(0f, floorY - viewH * 0.055f, fullW, floorY, paint)
            // Floor
            paint.color = if (palette.isDark) 0xFF2B2016.toInt() else 0xFF7A5A3A.toInt()
            canvas.drawRect(0f, floorY, fullW, fullH, paint)
            // Red wool rug
            paint.color = if (palette.isDark) 0xFF7A2828.toInt() else 0xFFC63D3D.toInt()
            canvas.drawRect(viewLeft + viewW * 0.12f, viewTop + viewH * 0.67f, viewLeft + viewW * 0.88f, viewTop + viewH * 0.91f, paint)
            return
        }

        // Standard / Teen / Kids seamless wall gradient all the way from y = 0f behind the status bar!
        paint.shader = LinearGradient(
            0f, 0f, 0f, floorY,
            intArrayOf(palette.wallUpper.toArgb(), palette.wallMain.toArgb(), palette.wallMain.toArgb()),
            floatArrayOf(0f, 0.22f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, fullW, floorY, paint)
        paint.shader = null

        if (style == RoomStyleId.TEEN_ROOM) {
            val ledY = viewTop + viewH * 0.068f
            // Seamless RGB LED ambient wash extending ALL THE WAY from y = 0f past the LED strip
            paint.shader = LinearGradient(
                0f, 0f, 0f, viewTop + viewH * 0.28f,
                intArrayOf(
                    if (palette.isDark) 0x6B9B5DE5 else 0x389B5DE5,
                    if (palette.isDark) 0x75F15BB5 else 0x3DF15BB5,
                    if (palette.isDark) 0x3300BBF9 else 0x1A00BBF9,
                    0x0000BBF9
                ),
                floatArrayOf(0f, 0.28f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, fullW, viewTop + viewH * 0.28f, paint)
            paint.shader = null

            // Crisp LED strip tube
            paint.shader = LinearGradient(
                0f, ledY, fullW, ledY,
                intArrayOf(0xFF00F5D4.toInt(), 0xFF9B5DE5.toInt(), 0xFFF15BB5.toInt(), 0xFF00BBF9.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, ledY, fullW, ledY + viewH * 0.0065f, paint)
            paint.shader = null

            // Teen posters (properly center-cropped, never stretched!)
            val leftRect = RectF(viewLeft + viewW * 0.34f, viewTop + viewH * 0.285f, viewLeft + viewW * 0.53f, viewTop + viewH * 0.41f)
            val rightRect = RectF(viewLeft + viewW * 0.56f, viewTop + viewH * 0.205f, viewLeft + viewW * 0.72f, viewTop + viewH * 0.32f)
            if (posterLeft != null) {
                drawBitmapCenterCrop(canvas, posterLeft, leftRect, paint)
            } else {
                paint.color = 0xFF752F8A.toInt()
                canvas.drawRect(leftRect, paint)
            }
            if (posterRight != null) {
                drawBitmapCenterCrop(canvas, posterRight, rightRect, paint)
            } else {
                paint.color = 0xFF1A2238.toInt()
                canvas.drawRect(rightRect, paint)
            }
        } else if (style == RoomStyleId.KIDS_ROOM) {
            val colors = intArrayOf(0xFFFF6B6B.toInt(), 0xFFFFD166.toInt(), 0xFF06D6A0.toInt(), 0xFF118AB2.toInt(), 0xFFFF9F1C.toInt())
            val flagCount = 9
            val flagW = viewW / flagCount
            val sy = viewTop + viewH * 0.075f
            for (i in 0 until flagCount) {
                paint.color = colors[i % colors.size]
                val p = Path().apply {
                    moveTo(viewLeft + i * flagW + flagW * 0.08f, sy)
                    lineTo(viewLeft + (i + 1) * flagW - flagW * 0.08f, sy)
                    lineTo(viewLeft + (i + 0.5f) * flagW, sy + viewH * 0.036f)
                    close()
                }
                canvas.drawPath(p, paint)
            }
        }

        // Baseboard & floor
        paint.color = palette.baseboard.toArgb()
        canvas.drawRect(0f, floorY - viewH * 0.018f, fullW, floorY, paint)
        paint.color = palette.baseboardShadow.toArgb()
        canvas.drawRect(0f, floorY, fullW, floorY + viewH * 0.005f, paint)
        paint.color = palette.floorMain.toArgb()
        canvas.drawRect(0f, floorY + viewH * 0.005f, fullW, fullH, paint)

        // Rug inside inner viewport
        paint.color = palette.rugFill.toArgb()
        val rugRect = RectF(viewLeft + viewW * 0.12f, viewTop + viewH * 0.665f, viewLeft + viewW * 0.88f, viewTop + viewH * 0.91f)
        canvas.drawRoundRect(rugRect, viewW * 0.035f, viewW * 0.035f, paint)
    }

    private fun drawObject(
        canvas: Canvas,
        hotspot: RoomHotspot,
        r: RectF,
        p: RoomVisualPalette,
        isVoxel: Boolean,
        weather: WeatherState,
        frameBitmap: Bitmap?,
        minutesOfDay: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = r.width()
        val h = r.height()
        when (hotspot.visualType) {
            RoomObjectVisual.WINDOW -> {
                val skyColor = when (weather.condition) {
                    WeatherCondition.CLEAR_DAY -> 0xFF9ED4EC.toInt()
                    WeatherCondition.CLEAR_NIGHT -> 0xFF1A2547.toInt()
                    WeatherCondition.PARTLY_CLOUDY -> 0xFFAECAD8.toInt()
                    WeatherCondition.CLOUDY -> 0xFF8E9EAB.toInt()
                    WeatherCondition.RAIN -> 0xFF60768C.toInt()
                    WeatherCondition.SNOW -> 0xFFBCCBD6.toInt()
                    WeatherCondition.THUNDERSTORM -> 0xFF394659.toInt()
                }
                val pane = RectF(r.left + w * 0.08f, r.top + h * 0.06f, r.right - w * 0.08f, r.bottom - h * 0.12f)
                paint.color = skyColor
                canvas.drawRect(pane, paint)
                // Mirror temperature frost (< 0°C) or fogged condensation (< 10°C) on wallpaper window!
                val tempC = weather.temperatureC
                if (tempC != null && tempC < 10) {
                    paint.shader = RadialGradient(
                        pane.centerX(), pane.centerY(),
                        min(pane.width(), pane.height()) * 0.85f,
                        if (tempC < 0) intArrayOf(0x22DDF3FF, 0x66C7EBFF, 0xB8E6F7FF.toInt())
                        else intArrayOf(0x18FFFFFF, 0x55EBF4FA, 0x9CE2EEF7.toInt()),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(pane, paint)
                    paint.shader = null
                }
                paint.style = Paint.Style.STROKE
                paint.color = if (isVoxel) 0xFF5C3E24.toInt() else p.woodLight.toArgb()
                paint.strokeWidth = min(w, h) * 0.055f
                canvas.drawRect(pane, paint)
                canvas.drawLine(pane.centerX(), pane.top, pane.centerX(), pane.bottom, paint)
                canvas.drawLine(pane.left, pane.centerY(), pane.right, pane.centerY(), paint)
                paint.style = Paint.Style.FILL
                paint.color = p.woodMid.toArgb()
                canvas.drawRoundRect(RectF(r.left + w * 0.02f, r.bottom - h * 0.13f, r.right - w * 0.02f, r.bottom - h * 0.04f), 4f, 4f, paint)
                if (tempC != null) {
                    val label = "${tempC}°C"
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFFFFFFFF.toInt()
                        textSize = (min(w, h) * 0.17f).coerceIn(22f, 52f)
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                    }
                    val textW = textPaint.measureText(label)
                    val fm = textPaint.fontMetrics
                    val pillH = (fm.descent - fm.ascent) + textPaint.textSize * 0.42f
                    val pillW = textW + textPaint.textSize * 0.95f
                    val pillBottom = pane.bottom - h * 0.04f
                    val pillRect = RectF(
                        pane.centerX() - pillW / 2f,
                        pillBottom - pillH,
                        pane.centerX() + pillW / 2f,
                        pillBottom
                    )
                    paint.color = when {
                        tempC < 0 -> 0xE0113B5E.toInt()
                        tempC < 10 -> 0xDB233E57.toInt()
                        tempC >= 28 -> 0xDB593112.toInt()
                        else -> 0xD9101621.toInt()
                    }
                    canvas.drawRoundRect(pillRect, pillH * 0.45f, pillH * 0.45f, paint)
                    val textY = pillRect.centerY() - (fm.ascent + fm.descent) / 2f
                    canvas.drawText(label, pillRect.centerX(), textY, textPaint)
                }
            }

            RoomObjectVisual.PAINTING -> {
                val frameRect = RectF(r.left + w * 0.03f, r.top + h * 0.03f, r.right - w * 0.03f, r.bottom - h * 0.03f)
                paint.color = if (isVoxel) 0xFF6B4828.toInt() else p.woodDark.toArgb()
                canvas.drawRoundRect(frameRect, if (isVoxel) 0f else 8f, if (isVoxel) 0f else 8f, paint)
                // Warm cream gallery mat
                val matRect = RectF(r.left + w * 0.10f, r.top + h * 0.10f, r.right - w * 0.10f, r.bottom - h * 0.10f)
                paint.color = 0xFFFAF6EE.toInt()
                canvas.drawRect(matRect, paint)
                // Symmetrical recessed photo aperture
                val inner = RectF(r.left + w * 0.155f, r.top + h * 0.155f, r.right - w * 0.155f, r.bottom - h * 0.155f)
                if (frameBitmap != null && !frameBitmap.isRecycled) {
                    drawBitmapCenterCrop(canvas, frameBitmap, inner, paint)
                } else {
                    paint.color = 0xFFE9C7A6.toInt()
                    canvas.drawRect(inner, paint)
                    paint.color = 0xFFD97D54.toInt()
                    canvas.drawCircle(inner.left + inner.width() * 0.66f, inner.top + inner.height() * 0.3f, inner.width() * 0.16f, paint)
                }
            }

            RoomObjectVisual.CLOCK -> {
                val radius = min(w, h) * 0.44f
                val cx = r.centerX()
                val cy = r.centerY()
                paint.color = if (isVoxel) 0xFFD4A017.toInt() else p.woodDark.toArgb()
                canvas.drawCircle(cx, cy, radius, paint)
                paint.color = p.clockFace.toArgb()
                canvas.drawCircle(cx, cy, radius * 0.85f, paint)
                val hourAngle = Math.toRadians(((minutesOfDay % 720) / 720.0) * 360.0 - 90.0)
                val minAngle = Math.toRadians(((minutesOfDay % 60) / 60.0) * 360.0 - 90.0)
                paint.color = p.clockHands.toArgb()
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = radius * 0.09f
                canvas.drawLine(cx, cy, cx + (cos(hourAngle) * radius * 0.46f).toFloat(), cy + (sin(hourAngle) * radius * 0.46f).toFloat(), paint)
                paint.strokeWidth = radius * 0.06f
                canvas.drawLine(cx, cy, cx + (cos(minAngle) * radius * 0.66f).toFloat(), cy + (sin(minAngle) * radius * 0.66f).toFloat(), paint)
            }

            RoomObjectVisual.STICKY_NOTE -> {
                paint.color = if (isVoxel) 0xFF9C7B49.toInt() else p.stickyNoteFill.toArgb()
                canvas.drawRoundRect(RectF(r.left + w * 0.08f, r.top + h * 0.12f, r.right - w * 0.08f, r.bottom - h * 0.08f), 4f, 4f, paint)
            }

            RoomObjectVisual.TV -> {
                paint.color = p.tvBezel.toArgb()
                canvas.drawRoundRect(RectF(r.left + w * 0.03f, r.top + h * 0.04f, r.right - w * 0.03f, r.bottom - h * 0.14f), 10f, 10f, paint)
                paint.color = p.tvScreen.toArgb()
                canvas.drawRect(r.left + w * 0.07f, r.top + h * 0.11f, r.right - w * 0.07f, r.bottom - h * 0.21f, paint)
            }

            RoomObjectVisual.CONSOLE -> {
                paint.color = if (isVoxel) 0xFF9E7342.toInt() else p.woodMid.toArgb()
                canvas.drawRoundRect(RectF(r.left + w * 0.02f, r.top + h * 0.06f, r.right - w * 0.02f, r.bottom - h * 0.24f), 8f, 8f, paint)
                paint.color = p.consoleBody.toArgb()
                canvas.drawRoundRect(RectF(r.left + w * 0.24f, r.top + h * 0.22f, r.right - w * 0.24f, r.bottom - h * 0.34f), 6f, 6f, paint)
                paint.color = p.consoleAccent.toArgb()
                canvas.drawRect(r.left + w * 0.30f, r.top + h * 0.36f, r.right - w * 0.30f, r.top + h * 0.43f, paint)
            }

            RoomObjectVisual.SHELF -> {
                if (isVoxel) {
                    paint.color = 0xFFB58E58.toInt()
                    canvas.drawRect(r, paint)
                    paint.color = 0xFF3E2D1A.toInt()
                    canvas.drawRect(r.left + w * 0.09f, r.top + h * 0.14f, r.right - w * 0.09f, r.top + h * 0.46f, paint)
                    canvas.drawRect(r.left + w * 0.09f, r.top + h * 0.54f, r.right - w * 0.09f, r.top + h * 0.86f, paint)
                } else {
                    paint.color = p.woodMid.toArgb()
                    canvas.drawRoundRect(RectF(r.left + w * 0.04f, r.top + h * 0.38f, r.right - w * 0.04f, r.top + h * 0.45f), 4f, 4f, paint)
                    canvas.drawRoundRect(RectF(r.left + w * 0.04f, r.top + h * 0.84f, r.right - w * 0.04f, r.top + h * 0.91f), 4f, 4f, paint)
                }
            }

            RoomObjectVisual.BED -> {
                if (isVoxel) {
                    paint.color = 0xFFF5F5F5.toInt()
                    canvas.drawRect(r.left + w * 0.04f, r.top + h * 0.16f, r.right - w * 0.04f, r.top + h * 0.40f, paint)
                    paint.color = 0xFFC62828.toInt()
                    canvas.drawRect(r.left + w * 0.04f, r.top + h * 0.38f, r.right - w * 0.04f, r.top + h * 0.74f, paint)
                    paint.color = 0xFF7D5632.toInt()
                    canvas.drawRect(r.left + w * 0.03f, r.top + h * 0.72f, r.right - w * 0.03f, r.top + h * 0.86f, paint)
                } else {
                    paint.color = p.woodDark.toArgb()
                    canvas.drawRoundRect(RectF(r.left + w * 0.03f, r.top + h * 0.04f, r.right - w * 0.06f, r.top + h * 0.38f), 14f, 14f, paint)
                    paint.color = p.pillowColor.toArgb()
                    canvas.drawRoundRect(RectF(r.left + w * 0.08f, r.top + h * 0.19f, r.left + w * 0.46f, r.top + h * 0.39f), 14f, 14f, paint)
                    canvas.drawRoundRect(RectF(r.left + w * 0.50f, r.top + h * 0.19f, r.right - w * 0.11f, r.top + h * 0.39f), 14f, 14f, paint)
                    paint.color = p.duvetPrimary.toArgb()
                    canvas.drawRect(r.left + w * 0.03f, r.top + h * 0.39f, r.right - w * 0.03f, r.bottom - h * 0.19f, paint)
                    paint.color = p.throwBlanket.toArgb()
                    canvas.drawRect(r.left + w * 0.03f, r.top + h * 0.62f, r.right - w * 0.03f, r.bottom - h * 0.19f, paint)
                }
            }

            RoomObjectVisual.TRASH_CAN -> {
                if (isVoxel) {
                    paint.color = 0xFF3E4247.toInt()
                    canvas.drawRect(r.left + w * 0.10f, r.top + h * 0.14f, r.right - w * 0.10f, r.bottom - h * 0.14f, paint)
                    paint.color = 0xFFFF5722.toInt()
                    canvas.drawRect(r.left + w * 0.18f, r.top + h * 0.18f, r.right - w * 0.18f, r.top + h * 0.34f, paint)
                } else {
                    val body = Path().apply {
                        moveTo(r.left + w * 0.14f, r.top + h * 0.18f)
                        lineTo(r.right - w * 0.14f, r.top + h * 0.18f)
                        lineTo(r.right - w * 0.22f, r.bottom - h * 0.08f)
                        lineTo(r.left + w * 0.22f, r.bottom - h * 0.08f)
                        close()
                    }
                    paint.color = p.trashBody.toArgb()
                    canvas.drawPath(body, paint)
                }
            }

            RoomObjectVisual.SOFA,
            RoomObjectVisual.FIREPLACE,
            RoomObjectVisual.COFFEE_TABLE,
            RoomObjectVisual.VINYL_PLAYER,
            RoomObjectVisual.FRIDGE,
            RoomObjectVisual.COFFEE_MAKER,
            RoomObjectVisual.STOVE,
            RoomObjectVisual.KITCHEN_COUNTER,
            RoomObjectVisual.GAMING_PC,
            RoomObjectVisual.ARCADE_CABINET,
            RoomObjectVisual.STREAMING_DESK,
            RoomObjectVisual.TROPHY_SHELF,
            RoomObjectVisual.CUSTOM -> {
                paint.color = p.woodMid.toArgb()
                canvas.drawRoundRect(r, 14f, 14f, paint)
            }
        }
    }

    private fun drawBitmapCenterCrop(canvas: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint) {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0 || dst.width() <= 0f || dst.height() <= 0f) {
            return
        }
        val bmpAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val dstAspect = dst.width() / dst.height()
        val src = if (bmpAspect > dstAspect) {
            // Bitmap is wider than destination -> crop left & right symmetrically
            val targetW = (bitmap.height * dstAspect).toInt().coerceIn(1, bitmap.width)
            val left = (bitmap.width - targetW) / 2
            Rect(left, 0, left + targetW, bitmap.height)
        } else {
            // Bitmap is taller than destination -> crop top & bottom symmetrically
            val targetH = (bitmap.width / dstAspect).toInt().coerceIn(1, bitmap.height)
            val top = (bitmap.height - targetH) / 2
            Rect(0, top, bitmap.width, top + targetH)
        }
        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, src, dst, bmpPaint)
    }
}
