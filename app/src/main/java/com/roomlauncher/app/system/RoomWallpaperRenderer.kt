package com.roomlauncher.app.system

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.WeatherCondition
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.ui.theme.RoomThemes
import com.roomlauncher.app.ui.theme.RoomVisualPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object RoomWallpaperRenderer {

    /**
     * Renders the current Room layout (theme + every object at its live normalized coordinates)
     * into a screen-sized Bitmap and applies it strictly to the Home screen wallpaper
     * via WallpaperManager.FLAG_SYSTEM (leaving the Lock screen untouched).
     */
    suspend fun applyHomeScreenWallpaper(
        context: Context,
        config: RoomConfig,
        weather: WeatherState
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels.coerceIn(720, 1440)
            val height = metrics.heightPixels.coerceIn(1280, 3120)
            val palette = RoomThemes.paletteFor(config.themeId)

            val frameBitmap = config.framePhotoPath?.let { path ->
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(path) else null
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawRoomShell(canvas, width.toFloat(), height.toFloat(), palette)

            val cal = Calendar.getInstance()
            val minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

            config.hotspots.forEach { hotspot ->
                val rect = RectF(
                    hotspot.x * width,
                    hotspot.y * height,
                    (hotspot.x + hotspot.width) * width,
                    (hotspot.y + hotspot.height) * height
                )
                drawObject(canvas, hotspot, rect, palette, weather, frameBitmap, minutesOfDay)
            }

            val wm = WallpaperManager.getInstance(context)
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)

            frameBitmap?.recycle()
            bitmap.recycle()
            true
        }.getOrDefault(false)
    }

    private fun drawRoomShell(canvas: Canvas, w: Float, h: Float, p: RoomVisualPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val floorY = h * 0.61f

        // Wall
        paint.color = p.wallMain.toArgb()
        canvas.drawRect(0f, 0f, w, floorY, paint)

        // Upper wall band
        paint.color = p.wallUpper.toArgb()
        canvas.drawRect(0f, 0f, w, h * 0.115f, paint)

        // Baseboard
        paint.color = p.baseboard.toArgb()
        canvas.drawRect(0f, floorY - h * 0.018f, w, floorY, paint)
        paint.color = p.baseboardShadow.toArgb()
        canvas.drawRect(0f, floorY, w, floorY + h * 0.005f, paint)

        // Floor
        paint.color = p.floorMain.toArgb()
        canvas.drawRect(0f, floorY + h * 0.005f, w, h, paint)

        // Floor plank seams
        paint.color = p.floorPlankLine.toArgb()
        paint.alpha = 95
        paint.strokeWidth = w * 0.003f
        listOf(0.68f, 0.75f, 0.83f, 0.91f).forEach { fy ->
            canvas.drawLine(0f, h * fy, w, h * fy, paint)
        }

        // Rug
        paint.alpha = 255
        paint.color = p.rugFill.toArgb()
        val rugRect = RectF(w * 0.12f, h * 0.665f, w * 0.88f, h * 0.91f)
        canvas.drawRoundRect(rugRect, w * 0.035f, w * 0.035f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = p.rugBorder.toArgb()
        paint.strokeWidth = w * 0.004f
        canvas.drawRoundRect(
            RectF(rugRect.left + w * 0.03f, rugRect.top + h * 0.015f, rugRect.right - w * 0.03f, rugRect.bottom - h * 0.015f),
            w * 0.02f,
            w * 0.02f,
            paint
        )
    }

    private fun drawObject(
        canvas: Canvas,
        hotspot: RoomHotspot,
        r: RectF,
        p: RoomVisualPalette,
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
                paint.color = skyColor
                canvas.drawRect(r.left + w * 0.08f, r.top + h * 0.06f, r.right - w * 0.08f, r.bottom - h * 0.12f, paint)

                // Sun or Moon
                if (weather.condition == WeatherCondition.CLEAR_DAY || weather.condition == WeatherCondition.PARTLY_CLOUDY) {
                    paint.color = 0xFFFFD166.toInt()
                    canvas.drawCircle(r.left + w * 0.7f, r.top + h * 0.28f, min(w, h) * 0.13f, paint)
                } else if (weather.condition == WeatherCondition.CLEAR_NIGHT) {
                    paint.color = 0xFFF3E9B8.toInt()
                    canvas.drawCircle(r.left + w * 0.7f, r.top + h * 0.28f, min(w, h) * 0.11f, paint)
                }

                // Frame
                paint.style = Paint.Style.STROKE
                paint.color = p.woodLight.toArgb()
                paint.strokeWidth = min(w, h) * 0.055f
                val pane = RectF(r.left + w * 0.08f, r.top + h * 0.06f, r.right - w * 0.08f, r.bottom - h * 0.12f)
                canvas.drawRect(pane, paint)
                canvas.drawLine(pane.centerX(), pane.top, pane.centerX(), pane.bottom, paint)
                canvas.drawLine(pane.left, pane.centerY(), pane.right, pane.centerY(), paint)

                // Sill
                paint.style = Paint.Style.FILL
                paint.color = p.woodMid.toArgb()
                canvas.drawRoundRect(
                    RectF(r.left + w * 0.02f, r.bottom - h * 0.13f, r.right - w * 0.02f, r.bottom - h * 0.04f),
                    4f, 4f, paint
                )
            }

            RoomObjectVisual.PAINTING -> {
                paint.color = p.woodDark.toArgb()
                canvas.drawRoundRect(r, 8f, 8f, paint)
                val inner = RectF(r.left + w * 0.09f, r.top + h * 0.09f, r.right - w * 0.09f, r.bottom - h * 0.09f)
                if (frameBitmap != null && !frameBitmap.isRecycled) {
                    canvas.drawBitmap(frameBitmap, null, inner, paint)
                } else {
                    paint.color = 0xFFE9C7A6.toInt()
                    canvas.drawRect(inner, paint)
                    paint.color = 0xFFD97D54.toInt()
                    canvas.drawCircle(inner.left + inner.width() * 0.66f, inner.top + inner.height() * 0.3f, inner.width() * 0.16f, paint)
                    paint.color = 0xFF4B5D52.toInt()
                    val hill = Path().apply {
                        moveTo(inner.left, inner.bottom)
                        lineTo(inner.left, inner.bottom - inner.height() * 0.28f)
                        lineTo(inner.centerX(), inner.bottom - inner.height() * 0.45f)
                        lineTo(inner.right, inner.bottom - inner.height() * 0.22f)
                        lineTo(inner.right, inner.bottom)
                        close()
                    }
                    canvas.drawPath(hill, paint)
                }
            }

            RoomObjectVisual.CLOCK -> {
                val radius = min(w, h) * 0.44f
                val cx = r.centerX()
                val cy = r.centerY()
                paint.color = p.woodDark.toArgb()
                canvas.drawCircle(cx, cy, radius, paint)
                paint.color = p.clockFace.toArgb()
                canvas.drawCircle(cx, cy, radius * 0.85f, paint)

                val hourAngle = Math.toRadians(((minutesOfDay % 720) / 720.0) * 360.0 - 90.0)
                val minAngle = Math.toRadians(((minutesOfDay % 60) / 60.0) * 360.0 - 90.0)
                paint.color = p.clockHands.toArgb()
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = radius * 0.09f
                canvas.drawLine(
                    cx, cy,
                    cx + (cos(hourAngle) * radius * 0.46f).toFloat(),
                    cy + (sin(hourAngle) * radius * 0.46f).toFloat(),
                    paint
                )
                paint.strokeWidth = radius * 0.06f
                canvas.drawLine(
                    cx, cy,
                    cx + (cos(minAngle) * radius * 0.66f).toFloat(),
                    cy + (sin(minAngle) * radius * 0.66f).toFloat(),
                    paint
                )
                paint.color = p.clockAccent.toArgb()
                canvas.drawCircle(cx, cy, radius * 0.09f, paint)
            }

            RoomObjectVisual.STICKY_NOTE -> {
                paint.color = p.stickyNoteFill.toArgb()
                canvas.drawRoundRect(
                    RectF(r.left + w * 0.08f, r.top + h * 0.12f, r.right - w * 0.08f, r.bottom - h * 0.08f),
                    6f, 6f, paint
                )
                paint.color = p.clockAccent.toArgb()
                canvas.drawCircle(r.centerX(), r.top + h * 0.16f, min(w, h) * 0.09f, paint)
            }

            RoomObjectVisual.TV -> {
                paint.color = p.tvBezel.toArgb()
                canvas.drawRoundRect(
                    RectF(r.left + w * 0.03f, r.top + h * 0.04f, r.right - w * 0.03f, r.bottom - h * 0.14f),
                    12f, 12f, paint
                )
                paint.color = p.tvScreen.toArgb()
                canvas.drawRect(r.left + w * 0.07f, r.top + h * 0.11f, r.right - w * 0.07f, r.bottom - h * 0.21f, paint)
                paint.color = p.tvBezel.toArgb()
                canvas.drawRect(r.centerX() - w * 0.06f, r.bottom - h * 0.14f, r.centerX() + w * 0.06f, r.bottom - h * 0.04f, paint)
                canvas.drawRoundRect(
                    RectF(r.centerX() - w * 0.18f, r.bottom - h * 0.07f, r.centerX() + w * 0.18f, r.bottom - h * 0.02f),
                    4f, 4f, paint
                )
            }

            RoomObjectVisual.CONSOLE -> {
                paint.color = p.woodMid.toArgb()
                canvas.drawRoundRect(
                    RectF(r.left + w * 0.02f, r.top + h * 0.06f, r.right - w * 0.02f, r.bottom - h * 0.24f),
                    10f, 10f, paint
                )
                paint.color = p.consoleBody.toArgb()
                canvas.drawRoundRect(
                    RectF(r.left + w * 0.24f, r.top + h * 0.22f, r.right - w * 0.24f, r.bottom - h * 0.34f),
                    8f, 8f, paint
                )
                paint.color = p.consoleAccent.toArgb()
                canvas.drawRect(r.left + w * 0.30f, r.top + h * 0.36f, r.right - w * 0.30f, r.top + h * 0.43f, paint)
                paint.color = p.woodDark.toArgb()
                canvas.drawRect(r.left + w * 0.10f, r.bottom - h * 0.24f, r.left + w * 0.14f, r.bottom - h * 0.02f, paint)
                canvas.drawRect(r.right - w * 0.14f, r.bottom - h * 0.24f, r.right - w * 0.10f, r.bottom - h * 0.02f, paint)
            }

            RoomObjectVisual.SHELF -> {
                paint.color = p.woodMid.toArgb()
                canvas.drawRoundRect(RectF(r.left + w * 0.04f, r.top + h * 0.38f, r.right - w * 0.04f, r.top + h * 0.45f), 4f, 4f, paint)
                canvas.drawRoundRect(RectF(r.left + w * 0.04f, r.top + h * 0.84f, r.right - w * 0.04f, r.top + h * 0.91f), 4f, 4f, paint)
                paint.color = p.duvetPrimary.toArgb()
                canvas.drawRect(r.left + w * 0.10f, r.top + h * 0.10f, r.left + w * 0.20f, r.top + h * 0.38f, paint)
                paint.color = p.throwBlanket.toArgb()
                canvas.drawRect(r.left + w * 0.22f, r.top + h * 0.16f, r.left + w * 0.31f, r.top + h * 0.38f, paint)
                paint.color = p.woodLight.toArgb()
                canvas.drawRect(r.left + w * 0.33f, r.top + h * 0.08f, r.left + w * 0.42f, r.top + h * 0.38f, paint)
            }

            RoomObjectVisual.BED -> {
                paint.color = p.woodDark.toArgb()
                canvas.drawRoundRect(RectF(r.left + w * 0.03f, r.top + h * 0.04f, r.right - w * 0.06f, r.top + h * 0.38f), 14f, 14f, paint)
                paint.color = p.pillowColor.toArgb()
                canvas.drawRoundRect(RectF(r.left + w * 0.08f, r.top + h * 0.19f, r.left + w * 0.46f, r.top + h * 0.39f), 14f, 14f, paint)
                canvas.drawRoundRect(RectF(r.left + w * 0.50f, r.top + h * 0.19f, r.right - w * 0.11f, r.top + h * 0.39f), 14f, 14f, paint)
                paint.color = p.duvetPrimary.toArgb()
                canvas.drawRect(r.left + w * 0.03f, r.top + h * 0.39f, r.right - w * 0.03f, r.bottom - h * 0.19f, paint)
                paint.color = p.throwBlanket.toArgb()
                canvas.drawRect(r.left + w * 0.03f, r.top + h * 0.62f, r.right - w * 0.03f, r.bottom - h * 0.19f, paint)
                paint.color = p.woodDark.toArgb()
                canvas.drawRect(r.left + w * 0.02f, r.bottom - h * 0.20f, r.right - w * 0.02f, r.bottom - h * 0.11f, paint)
                canvas.drawRect(r.left + w * 0.07f, r.bottom - h * 0.11f, r.left + w * 0.13f, r.bottom - h * 0.02f, paint)
                canvas.drawRect(r.right - w * 0.13f, r.bottom - h * 0.11f, r.right - w * 0.07f, r.bottom - h * 0.02f, paint)
            }

            RoomObjectVisual.TRASH_CAN -> {
                val body = Path().apply {
                    moveTo(r.left + w * 0.14f, r.top + h * 0.18f)
                    lineTo(r.right - w * 0.14f, r.top + h * 0.18f)
                    lineTo(r.right - w * 0.22f, r.bottom - h * 0.08f)
                    lineTo(r.left + w * 0.22f, r.bottom - h * 0.08f)
                    close()
                }
                paint.color = p.trashBody.toArgb()
                canvas.drawPath(body, paint)
                paint.color = p.trashRim.toArgb()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = min(w, h) * 0.04f
                canvas.drawOval(RectF(r.left + w * 0.14f, r.top + h * 0.10f, r.right - w * 0.14f, r.top + h * 0.26f), paint)
            }

            RoomObjectVisual.CUSTOM -> {
                paint.color = p.woodMid.toArgb()
                canvas.drawRoundRect(r, 16f, 16f, paint)
            }
        }
    }
}
