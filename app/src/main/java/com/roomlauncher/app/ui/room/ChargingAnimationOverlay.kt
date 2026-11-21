package com.roomlauncher.app.ui.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.BatteryStatus
import com.roomlauncher.app.data.ChargeSource
import com.roomlauncher.app.ui.theme.RoomStyleId
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom full-screen plug-in charging animation (plays when screen is on).
 * Adapts visually to the user's active [RoomStyleId]:
 *  - Cozy Minimalist: warm amber lamp ripples & glowing battery arc
 *  - Teen Room: electric RGB neon pulse rings & cyan/magenta energy beam
 *  - Kids Room: twinkling rainbow stars & playful golden orb
 *  - Blocky / Voxel (Minecraft): rising Redstone/XP pixel particles & voxel energy bar
 */
@Composable
fun ChargingSplashOverlay(
    batteryStatus: BatteryStatus,
    roomStyle: RoomStyleId,
    triggerKey: Long,
    onFinished: () -> Unit
) {
    val entrance = remember(triggerKey) { Animatable(0f) }
    val chargeSweep = remember(triggerKey) { Animatable(0f) }
    val exitAlpha = remember(triggerKey) { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "charging_loop")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charging_pulse"
    )
    val rotationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charging_spin"
    )

    LaunchedEffect(triggerKey) {
        entrance.snapTo(0f)
        chargeSweep.snapTo(0f)
        exitAlpha.snapTo(1f)

        entrance.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        chargeSweep.animateTo(
            targetValue = (batteryStatus.level.coerceIn(1, 100)) / 100f,
            animationSpec = tween(820, easing = FastOutSlowInEasing)
        )
        delay(1750L)
        exitAlpha.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
        onFinished()
    }

    val (primaryGlow, secondaryGlow, accentGlow) = styleChargeColors(roomStyle)
    val isVoxel = roomStyle == RoomStyleId.MINECRAFT
    val p = entrance.value
    val fade = exitAlpha.value * p

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade }
            .background(Color(0xCC090B10))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background energy beam from USB port at bottom center + style particles & halo rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.46f
            val baseRadius = min(w, h) * 0.27f

            // Ambient radial room lighting wash
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.34f),
                        secondaryGlow.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = baseRadius * 2.2f
                ),
                radius = baseRadius * 2.2f,
                center = Offset(cx, cy)
            )

            // Energy beam rising straight up from the bottom charging port!
            val beamTop = cy + baseRadius * 0.92f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryGlow.copy(alpha = 0.72f),
                        accentGlow.copy(alpha = 0.95f)
                    ),
                    startY = beamTop,
                    endY = h
                ),
                topLeft = Offset(cx - w * 0.012f, beamTop),
                size = Size(w * 0.024f, h - beamTop)
            )

            // Rising energy pulses climbing up the bottom beam
            for (i in 0..3) {
                val frac = (pulsePhase + i * 0.25f) % 1f
                val py = h - frac * (h - beamTop)
                val pAlpha = sin(frac * PI).toFloat().coerceIn(0f, 1f)
                if (isVoxel) {
                    val ps = w * 0.024f
                    drawRect(
                        color = accentGlow.copy(alpha = pAlpha),
                        topLeft = Offset(cx - ps / 2f, py - ps / 2f),
                        size = Size(ps, ps)
                    )
                } else {
                    drawCircle(
                        color = accentGlow.copy(alpha = pAlpha * 0.95f),
                        radius = w * 0.014f * (0.7f + 0.5f * pAlpha),
                        center = Offset(cx, py)
                    )
                }
            }

            // Expanding ripple rings / particles
            for (rIdx in 0..2) {
                val rf = (pulsePhase + rIdx * 0.33f) % 1f
                val rippleAlpha = (1f - rf) * 0.48f
                val rippleRadius = baseRadius * (1.02f + rf * 0.75f)
                if (isVoxel) {
                    val half = rippleRadius * 0.92f
                    drawRect(
                        color = primaryGlow.copy(alpha = rippleAlpha),
                        topLeft = Offset(cx - half, cy - half),
                        size = Size(half * 2f, half * 2f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = if (rIdx % 2 == 0) primaryGlow.copy(alpha = rippleAlpha) else secondaryGlow.copy(alpha = rippleAlpha),
                        radius = rippleRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            // Style-specific floating particles around the orb
            val particleCount = if (isVoxel) 16 else 12
            for (i in 0 until particleCount) {
                val angleDeg = rotationPhase * (if (i % 2 == 0) 0.55f else -0.4f) + i * (360f / particleCount)
                val rad = Math.toRadians(angleDeg.toDouble())
                val orbitDist = baseRadius * (1.18f + 0.22f * sin((pulsePhase * 2f * PI + i).toFloat()))
                val px = cx + cos(rad).toFloat() * orbitDist
                val py = cy + sin(rad).toFloat() * orbitDist
                val pColor = when (i % 3) {
                    0 -> primaryGlow
                    1 -> secondaryGlow
                    else -> accentGlow
                }
                if (isVoxel) {
                    // Redstone / XP floating pixel cubes
                    val sq = 7.dp.toPx()
                    drawRect(
                        color = pColor.copy(alpha = 0.85f),
                        topLeft = Offset(px - sq / 2f, py - sq / 2f),
                        size = Size(sq, sq)
                    )
                } else if (roomStyle == RoomStyleId.KIDS_ROOM) {
                    // Twinkling 4-point sparkles
                    val starR = 7.dp.toPx()
                    drawLine(pColor, Offset(px - starR, py), Offset(px + starR, py), strokeWidth = 2.5f)
                    drawLine(pColor, Offset(px, py - starR), Offset(px, py + starR), strokeWidth = 2.5f)
                } else {
                    drawCircle(
                        color = pColor.copy(alpha = 0.82f),
                        radius = (3.5f + (i % 3) * 1.4f).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            // Main center battery meter: Voxel XP bar for Minecraft OR glowing circular arc for other styles
            if (isVoxel) {
                val boxSize = baseRadius * 1.72f
                val boxLeft = cx - boxSize / 2f
                val boxTop = cy - boxSize / 2f
                drawRect(
                    color = Color(0xFF1A1E24),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxSize, boxSize)
                )
                drawRect(
                    color = primaryGlow,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxSize, boxSize),
                    style = Stroke(width = 6.dp.toPx())
                )
                // Segmented voxel XP charge bar near bottom of square frame
                val segments = 10
                val barW = boxSize * 0.78f
                val segW = barW / segments
                val barLeft = cx - barW / 2f
                val barTop = boxTop + boxSize * 0.74f
                val filledSegments = (chargeSweep.value * segments).toInt().coerceIn(1, segments)
                for (s in 0 until segments) {
                    drawRect(
                        color = if (s < filledSegments) secondaryGlow else Color(0xFF2E343D),
                        topLeft = Offset(barLeft + s * segW + 2f, barTop),
                        size = Size(segW - 4f, 14.dp.toPx())
                    )
                }
            } else {
                // Dark glass inner orb
                drawCircle(
                    color = Color(0xE011151E),
                    radius = baseRadius * 0.94f,
                    center = Offset(cx, cy)
                )
                // Background track ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = baseRadius * 0.94f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 10.dp.toPx())
                )
                // Animated charging sweep arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryGlow, secondaryGlow, accentGlow, primaryGlow),
                        center = Offset(cx, cy)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * chargeSweep.value,
                    useCenter = false,
                    topLeft = Offset(cx - baseRadius * 0.94f, cy - baseRadius * 0.94f),
                    size = Size(baseRadius * 1.88f, baseRadius * 1.88f),
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Center text readout (bolt icon, animated percentage, charge source label)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = 64.dp)
                .graphicsLayer {
                    val s = 0.84f + 0.16f * p
                    scaleX = s
                    scaleY = s
                }
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "Charging",
                tint = accentGlow,
                modifier = Modifier.size(34.dp)
            )
            val animatedPct = (chargeSweep.value * 100f).toInt().coerceIn(0, batteryStatus.level.coerceAtLeast(1))
            Text(
                text = "$animatedPct%",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = when {
                isVoxel -> "REDSTONE CHARGE"
                batteryStatus.chargeSource != ChargeSource.NONE -> batteryStatus.chargeSource.label
                else -> "Charging"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = accentGlow.copy(alpha = 0.92f)
            )
        }

        // Subtle tap-to-dismiss hint near bottom
        Text(
            text = "Tap anywhere to continue",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp)
        )
    }
}

/**
 * Subtle living ambient glow + compact battery badge shown on the Room Home screen while plugged in.
 * Tapping the badge replays the full charging animation.
 */
@Composable
fun AmbientChargingIndicator(
    batteryStatus: BatteryStatus,
    roomStyle: RoomStyleId,
    onReplayAnimation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (primaryGlow, _, accentGlow) = styleChargeColors(roomStyle)
    val infinite = rememberInfiniteTransition(label = "ambient_charge")
    val breath by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_charge_breath"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Soft breathing charging aura right along the bottom charging edge
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(84.dp)
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryGlow.copy(alpha = 0.22f * breath)
                    )
                )
            )
            // Tiny glowing power bar right at the bottom center charging port
            val barW = size.width * 0.24f
            drawRoundRect(
                color = accentGlow.copy(alpha = 0.65f * breath),
                topLeft = Offset((size.width - barW) / 2f, size.height - 6.dp.toPx()),
                size = Size(barW, 3.5.dp.toPx()),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Compact clickable charging badge in the bottom-right corner above nav bar
        Surface(
            shape = CircleShape,
            color = Color(0xB811151E),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 14.dp)
                .clickable(onClick = onReplayAnimation)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "Charging",
                    tint = accentGlow,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${batteryStatus.level}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun styleChargeColors(roomStyle: RoomStyleId): Triple<Color, Color, Color> = when (roomStyle) {
    RoomStyleId.COZY_MINIMALIST -> Triple(
        Color(0xFFE09F3E), // Warm amber lamp glow
        Color(0xFF7CC49E), // Cozy sage energy
        Color(0xFFFFE599)  // Soft cream-gold spark
    )
    RoomStyleId.TEEN_ROOM -> Triple(
        Color(0xFF9B5DE5), // Neon violet
        Color(0xFF00F5D4), // Electric cyan
        Color(0xFFF15BB5)  // Hot magenta accent
    )
    RoomStyleId.KIDS_ROOM -> Triple(
        Color(0xFFFFB703), // Sunny gold
        Color(0xFF4CC9F0), // Bright sky blue
        Color(0xFFFF6B6B)  // Playful coral star
    )
    RoomStyleId.MINECRAFT -> Triple(
        Color(0xFFE53935), // Redstone glow
        Color(0xFF5CE65C), // Minecraft XP green
        Color(0xFFFFE066)  // Torch gold spark
    )
}
