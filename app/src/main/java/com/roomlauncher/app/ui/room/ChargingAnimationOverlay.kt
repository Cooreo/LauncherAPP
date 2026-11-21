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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.BatteryStatus
import com.roomlauncher.app.data.ChargeSource
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.accentNeonColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom full-screen plug-in charging animation (plays while screen is on, holds ~5.5 seconds,
 * tap anywhere to dismiss early). Supports 6 selectable styles via [animationStyle].
 */
@Composable
fun ChargingSplashOverlay(
    batteryStatus: BatteryStatus,
    roomStyle: RoomStyleId,
    gdSubTheme: GeometryDashSubTheme = GeometryDashSubTheme.SONIC_WAVE,
    animationStyle: ChargingAnimationStyle,
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
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charging_pulse"
    )
    val rotationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charging_spin"
    )

    LaunchedEffect(triggerKey) {
        entrance.snapTo(0f)
        chargeSweep.snapTo(0f)
        exitAlpha.snapTo(1f)

        entrance.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        chargeSweep.animateTo(
            targetValue = (batteryStatus.level.coerceIn(1, 100)) / 100f,
            animationSpec = tween(900, easing = FastOutSlowInEasing)
        )
        // Hold generously on screen (~4.5s) so you can really enjoy the animation (tap anytime to dismiss early)
        delay(4500L)
        exitAlpha.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        onFinished()
    }

    val effectiveVoxel = animationStyle == ChargingAnimationStyle.VOXEL_REDSTONE ||
        (animationStyle == ChargingAnimationStyle.MATCH_ROOM && roomStyle == RoomStyleId.MINECRAFT)
    val effectiveGdCyber = animationStyle == ChargingAnimationStyle.MATCH_ROOM &&
        roomStyle == RoomStyleId.GEOMETRY_DASH

    val (primaryGlow, secondaryGlow, accentGlow) = animationPalette(animationStyle, roomStyle, gdSubTheme)
    val p = entrance.value
    val fade = exitAlpha.value * p

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade }
            .background(Color(0xD407090F))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.46f
            val baseRadius = min(w, h) * 0.28f

            when {
                effectiveVoxel -> {
                    drawVoxelRedstoneCharging(
                        w = w, h = h, cx = cx, cy = cy, baseRadius = baseRadius,
                        chargeFill = chargeSweep.value,
                        pulsePhase = pulsePhase,
                        rotationPhase = rotationPhase,
                        primaryGlow = primaryGlow,
                        secondaryGlow = secondaryGlow,
                        accentGlow = accentGlow
                    )
                }
                animationStyle == ChargingAnimationStyle.LIQUID_GLASS -> {
                    drawLiquidGlassCharging(
                        w = w, h = h, cx = cx, cy = cy, baseRadius = baseRadius,
                        chargeFill = chargeSweep.value,
                        pulsePhase = pulsePhase,
                        primaryGlow = primaryGlow,
                        secondaryGlow = secondaryGlow,
                        accentGlow = accentGlow
                    )
                }
                animationStyle == ChargingAnimationStyle.AURORA_WAVE -> {
                    drawAuroraWaveCharging(
                        w = w, h = h, cx = cx, cy = cy, baseRadius = baseRadius,
                        chargeFill = chargeSweep.value,
                        pulsePhase = pulsePhase,
                        primaryGlow = primaryGlow,
                        secondaryGlow = secondaryGlow,
                        accentGlow = accentGlow
                    )
                }
                animationStyle == ChargingAnimationStyle.CYBER_PULSE || effectiveGdCyber -> {
                    drawCyberPulseCharging(
                        w = w, h = h, cx = cx, cy = cy, baseRadius = baseRadius,
                        chargeFill = chargeSweep.value,
                        pulsePhase = pulsePhase,
                        rotationPhase = rotationPhase,
                        primaryGlow = primaryGlow,
                        secondaryGlow = secondaryGlow,
                        accentGlow = accentGlow
                    )
                }
                animationStyle == ChargingAnimationStyle.COSMIC_ORBIT -> {
                    drawCosmicOrbitCharging(
                        w = w, h = h, cx = cx, cy = cy, baseRadius = baseRadius,
                        chargeFill = chargeSweep.value,
                        pulsePhase = pulsePhase,
                        rotationPhase = rotationPhase,
                        primaryGlow = primaryGlow,
                        secondaryGlow = secondaryGlow,
                        accentGlow = accentGlow
                    )
                }
                else -> {
                    drawRoomMatchedCharging(
                        w = w, h = h, cx = cx, cy = cy, baseRadius = baseRadius,
                        chargeFill = chargeSweep.value,
                        pulsePhase = pulsePhase,
                        rotationPhase = rotationPhase,
                        isKids = roomStyle == RoomStyleId.KIDS_ROOM,
                        primaryGlow = primaryGlow,
                        secondaryGlow = secondaryGlow,
                        accentGlow = accentGlow
                    )
                }
            }
        }

        // Center text readout (bolt icon, animated percentage, charge source label)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = if (effectiveVoxel) 74.dp else 64.dp)
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
                effectiveVoxel -> "REDSTONE CHARGE"
                animationStyle == ChargingAnimationStyle.COSMIC_ORBIT -> "SOLAR CHARGE"
                animationStyle == ChargingAnimationStyle.AURORA_WAVE -> "AURORA CHARGE"
                batteryStatus.chargeSource != ChargeSource.NONE -> batteryStatus.chargeSource.label
                else -> "Charging"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = accentGlow.copy(alpha = 0.94f)
            )
        }

        // Subtle tap-to-dismiss hint near bottom
        Text(
            text = "Tap anywhere to dismiss",
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
 * Clean, compact clickable charging badge shown in the bottom-right corner while plugged in.
 * (No bottom edge bar — just the little charging pill!)
 * Tapping the badge replays the full charging animation.
 */
@Composable
fun AmbientChargingIndicator(
    batteryStatus: BatteryStatus,
    roomStyle: RoomStyleId,
    animationStyle: ChargingAnimationStyle,
    onReplayAnimation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (_, _, accentGlow) = animationPalette(animationStyle, roomStyle)

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color(0xC211151E),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 14.dp)
                .clickable(onClick = onReplayAnimation)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
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

// ==================== 6 DISTINCT CHARGING ANIMATION RENDERERS ====================

private fun DrawScope.drawBottomEnergyBeam(
    w: Float,
    h: Float,
    cx: Float,
    beamTop: Float,
    pulsePhase: Float,
    primaryGlow: Color,
    accentGlow: Color,
    isVoxel: Boolean
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, primaryGlow.copy(alpha = 0.72f), accentGlow.copy(alpha = 0.95f)),
            startY = beamTop,
            endY = h
        ),
        topLeft = Offset(cx - w * 0.011f, beamTop),
        size = Size(w * 0.022f, h - beamTop)
    )
    for (i in 0..3) {
        val frac = (pulsePhase + i * 0.25f) % 1f
        val py = h - frac * (h - beamTop)
        val pAlpha = sin(frac * PI).toFloat().coerceIn(0f, 1f)
        if (isVoxel) {
            val ps = w * 0.024f
            drawRect(accentGlow.copy(alpha = pAlpha), Offset(cx - ps / 2f, py - ps / 2f), Size(ps, ps))
        } else {
            drawCircle(accentGlow.copy(alpha = pAlpha * 0.95f), w * 0.014f * (0.7f + 0.5f * pAlpha), Offset(cx, py))
        }
    }
}

private fun DrawScope.drawRoomMatchedCharging(
    w: Float, h: Float, cx: Float, cy: Float, baseRadius: Float,
    chargeFill: Float, pulsePhase: Float, rotationPhase: Float,
    isKids: Boolean, primaryGlow: Color, secondaryGlow: Color, accentGlow: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(primaryGlow.copy(alpha = 0.34f), secondaryGlow.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(cx, cy), radius = baseRadius * 2.3f
        ),
        radius = baseRadius * 2.3f, center = Offset(cx, cy)
    )
    drawBottomEnergyBeam(w, h, cx, cy + baseRadius * 0.92f, pulsePhase, primaryGlow, accentGlow, isVoxel = false)

    for (rIdx in 0..2) {
        val rf = (pulsePhase + rIdx * 0.33f) % 1f
        drawCircle(
            color = (if (rIdx % 2 == 0) primaryGlow else secondaryGlow).copy(alpha = (1f - rf) * 0.48f),
            radius = baseRadius * (1.02f + rf * 0.78f),
            center = Offset(cx, cy),
            style = Stroke(width = 3.dp.toPx())
        )
    }

    for (i in 0 until 12) {
        val angleDeg = rotationPhase * (if (i % 2 == 0) 0.55f else -0.4f) + i * 30f
        val rad = Math.toRadians(angleDeg.toDouble())
        val orbitDist = baseRadius * (1.18f + 0.22f * sin((pulsePhase * 2f * PI + i).toFloat()))
        val px = cx + cos(rad).toFloat() * orbitDist
        val py = cy + sin(rad).toFloat() * orbitDist
        val pColor = when (i % 3) { 0 -> primaryGlow; 1 -> secondaryGlow; else -> accentGlow }
        if (isKids) {
            val starR = 7.dp.toPx()
            drawLine(pColor, Offset(px - starR, py), Offset(px + starR, py), strokeWidth = 2.5f)
            drawLine(pColor, Offset(px, py - starR), Offset(px, py + starR), strokeWidth = 2.5f)
        } else {
            drawCircle(pColor.copy(alpha = 0.85f), (3.5f + (i % 3) * 1.4f).dp.toPx(), Offset(px, py))
        }
    }

    drawCircle(Color(0xE011151E), baseRadius * 0.94f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.14f), baseRadius * 0.94f, Offset(cx, cy), style = Stroke(width = 10.dp.toPx()))
    drawArc(
        brush = Brush.sweepGradient(listOf(primaryGlow, secondaryGlow, accentGlow, primaryGlow), Offset(cx, cy)),
        startAngle = -90f, sweepAngle = 360f * chargeFill, useCenter = false,
        topLeft = Offset(cx - baseRadius * 0.94f, cy - baseRadius * 0.94f),
        size = Size(baseRadius * 1.88f, baseRadius * 1.88f),
        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawLiquidGlassCharging(
    w: Float, h: Float, cx: Float, cy: Float, baseRadius: Float,
    chargeFill: Float, pulsePhase: Float,
    primaryGlow: Color, secondaryGlow: Color, accentGlow: Color
) {
    val orbR = baseRadius * 1.02f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(primaryGlow.copy(alpha = 0.36f), Color.Transparent),
            center = Offset(cx, cy), radius = orbR * 2.2f
        ),
        radius = orbR * 2.2f, center = Offset(cx, cy)
    )
    drawBottomEnergyBeam(w, h, cx, cy + orbR, pulsePhase, primaryGlow, accentGlow, isVoxel = false)

    // Dark crystal orb interior
    drawCircle(Color(0xE60B141E), orbR, Offset(cx, cy))

    // Clip animated sloshing liquid waves & rising bubbles inside the circular glass orb!
    val orbClip = Path().apply {
        addOval(Rect(cx - orbR * 0.94f, cy - orbR * 0.94f, cx + orbR * 0.94f, cy + orbR * 0.94f))
    }
    clipPath(orbClip) {
        val fillTop = cy + orbR - (orbR * 2f * chargeFill.coerceIn(0.06f, 0.98f))
        val waveAmp = orbR * 0.085f
        val leftX = cx - orbR
        val rightX = cx + orbR
        val steps = 28

        // Back wave layer
        val backWave = Path().apply {
            moveTo(leftX, cy + orbR)
            for (s in 0..steps) {
                val t = s / steps.toFloat()
                val x = leftX + (rightX - leftX) * t
                val y = fillTop + sin((t * 2f * PI + pulsePhase * 2f * PI + 1.4).toFloat()) * waveAmp
                lineTo(x, y)
            }
            lineTo(rightX, cy + orbR)
            close()
        }
        drawPath(backWave, secondaryGlow.copy(alpha = 0.55f))

        // Front glowing liquid wave
        val frontWave = Path().apply {
            moveTo(leftX, cy + orbR)
            for (s in 0..steps) {
                val t = s / steps.toFloat()
                val x = leftX + (rightX - leftX) * t
                val y = fillTop + sin((t * 2f * PI - pulsePhase * 2f * PI).toFloat()) * waveAmp
                lineTo(x, y)
            }
            lineTo(rightX, cy + orbR)
            close()
        }
        drawPath(
            path = frontWave,
            brush = Brush.verticalGradient(
                listOf(accentGlow.copy(alpha = 0.85f), primaryGlow.copy(alpha = 0.92f)),
                startY = fillTop - waveAmp,
                endY = cy + orbR
            )
        )

        // Rising effervescent micro-bubbles inside the liquid
        for (b in 0..9) {
            val bf = (pulsePhase + b * 0.13f) % 1f
            val bx = cx + sin((b * 2.1 + bf * 3.0).toFloat()) * orbR * 0.58f
            val by = (cy + orbR) - bf * (cy + orbR - fillTop).coerceAtLeast(10f)
            if (by > fillTop + 6f) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - bf) * 0.65f),
                    radius = (2.5f + (b % 3) * 1.3f).dp.toPx(),
                    center = Offset(bx, by)
                )
            }
        }
    }

    // Glossy glass rim + specular highlight arc
    drawCircle(Color.White.copy(alpha = 0.35f), orbR, Offset(cx, cy), style = Stroke(width = 4.dp.toPx()))
    drawArc(
        color = Color.White.copy(alpha = 0.42f),
        startAngle = 205f, sweepAngle = 55f, useCenter = false,
        topLeft = Offset(cx - orbR * 0.86f, cy - orbR * 0.86f),
        size = Size(orbR * 1.72f, orbR * 1.72f),
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawAuroraWaveCharging(
    w: Float, h: Float, cx: Float, cy: Float, baseRadius: Float,
    chargeFill: Float, pulsePhase: Float,
    primaryGlow: Color, secondaryGlow: Color, accentGlow: Color
) {
    // 3 flowing Northern Lights aurora ribbons sweeping gently behind the orb
    val ribbonColors = listOf(primaryGlow, secondaryGlow, accentGlow)
    ribbonColors.forEachIndexed { idx, col ->
        val ribbonPath = Path().apply {
            val baseY = cy + (idx - 1) * baseRadius * 0.58f
            moveTo(0f, baseY)
            for (step in 0..24) {
                val t = step / 24f
                val x = w * t
                val wave = sin((t * 2.2 * PI + pulsePhase * 2 * PI + idx * 1.3).toFloat()) * baseRadius * 0.38f
                lineTo(x, baseY + wave)
            }
        }
        drawPath(
            path = ribbonPath,
            color = col.copy(alpha = 0.24f),
            style = Stroke(width = 42.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = ribbonPath,
            color = col.copy(alpha = 0.52f),
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    drawBottomEnergyBeam(w, h, cx, cy + baseRadius * 0.94f, pulsePhase, primaryGlow, accentGlow, isVoxel = false)

    // Frosted dark glass center orb + aurora sweep ring
    drawCircle(Color(0xD80A121C), baseRadius * 0.95f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.16f), baseRadius * 0.95f, Offset(cx, cy), style = Stroke(width = 9.dp.toPx()))
    drawArc(
        brush = Brush.sweepGradient(listOf(primaryGlow, secondaryGlow, accentGlow, primaryGlow), Offset(cx, cy)),
        startAngle = -90f, sweepAngle = 360f * chargeFill, useCenter = false,
        topLeft = Offset(cx - baseRadius * 0.95f, cy - baseRadius * 0.95f),
        size = Size(baseRadius * 1.90f, baseRadius * 1.90f),
        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCyberPulseCharging(
    w: Float, h: Float, cx: Float, cy: Float, baseRadius: Float,
    chargeFill: Float, pulsePhase: Float, rotationPhase: Float,
    primaryGlow: Color, secondaryGlow: Color, accentGlow: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(primaryGlow.copy(alpha = 0.32f), secondaryGlow.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(cx, cy), radius = baseRadius * 2.2f
        ),
        radius = baseRadius * 2.2f, center = Offset(cx, cy)
    )
    drawBottomEnergyBeam(w, h, cx, cy + baseRadius * 0.98f, pulsePhase, primaryGlow, accentGlow, isVoxel = false)

    // Expanding hexagonal cyber shockwaves
    for (ring in 0..2) {
        val rf = (pulsePhase + ring * 0.33f) % 1f
        val r = baseRadius * (1.06f + rf * 0.72f)
        val hex = Path().apply {
            for (v in 0..5) {
                val a = Math.toRadians((v * 60.0) + rotationPhase * 0.18)
                val hx = cx + cos(a).toFloat() * r
                val hy = cy + sin(a).toFloat() * r
                if (v == 0) moveTo(hx, hy) else lineTo(hx, hy)
            }
            close()
        }
        drawPath(
            path = hex,
            color = (if (ring % 2 == 0) primaryGlow else secondaryGlow).copy(alpha = (1f - rf) * 0.55f),
            style = Stroke(width = 2.6.dp.toPx())
        )
    }

    // Counter-rotating segmented plasma radar arcs
    for (seg in 0 until 6) {
        drawArc(
            color = primaryGlow.copy(alpha = 0.75f),
            startAngle = rotationPhase + seg * 60f,
            sweepAngle = 32f,
            useCenter = false,
            topLeft = Offset(cx - baseRadius * 1.15f, cy - baseRadius * 1.15f),
            size = Size(baseRadius * 2.30f, baseRadius * 2.30f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = secondaryGlow.copy(alpha = 0.65f),
            startAngle = -rotationPhase * 1.3f + seg * 60f,
            sweepAngle = 22f,
            useCenter = false,
            topLeft = Offset(cx - baseRadius * 1.04f, cy - baseRadius * 1.04f),
            size = Size(baseRadius * 2.08f, baseRadius * 2.08f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    drawCircle(Color(0xEB090C16), baseRadius * 0.92f, Offset(cx, cy))
    drawArc(
        brush = Brush.sweepGradient(listOf(primaryGlow, accentGlow, secondaryGlow, primaryGlow), Offset(cx, cy)),
        startAngle = -90f, sweepAngle = 360f * chargeFill, useCenter = false,
        topLeft = Offset(cx - baseRadius * 0.92f, cy - baseRadius * 0.92f),
        size = Size(baseRadius * 1.84f, baseRadius * 1.84f),
        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCosmicOrbitCharging(
    w: Float, h: Float, cx: Float, cy: Float, baseRadius: Float,
    chargeFill: Float, pulsePhase: Float, rotationPhase: Float,
    primaryGlow: Color, secondaryGlow: Color, accentGlow: Color
) {
    // Twinkling deep-space starfield
    for (s in 0 until 28) {
        val sx = ((s * 97 + 19) % 100) / 100f * w
        val sy = ((s * 61 + 11) % 100) / 100f * h
        val twinkle = (0.35f + 0.65f * sin((pulsePhase * 2 * PI + s * 0.9).toFloat())).coerceIn(0.15f, 1f)
        drawCircle(Color.White.copy(alpha = twinkle * 0.75f), (1.5f + (s % 3) * 0.9f).dp.toPx(), Offset(sx, sy))
    }

    // Golden solar eclipse corona
    drawCircle(
        brush = Brush.radialGradient(
            listOf(primaryGlow.copy(alpha = 0.44f), secondaryGlow.copy(alpha = 0.16f), Color.Transparent),
            center = Offset(cx, cy), radius = baseRadius * 2.1f
        ),
        radius = baseRadius * 2.1f, center = Offset(cx, cy)
    )

    // 3 orbiting planets & trails circling the solar core
    val orbitRadii = listOf(baseRadius * 1.24f, baseRadius * 1.48f, baseRadius * 1.72f)
    val planetColors = listOf(accentGlow, secondaryGlow, primaryGlow)
    orbitRadii.forEachIndexed { idx, orbitR ->
        drawCircle(Color.White.copy(alpha = 0.14f), orbitR, Offset(cx, cy), style = Stroke(width = 1.5.dp.toPx()))
        val angle = Math.toRadians((rotationPhase * (1.2f - idx * 0.25f) + idx * 120f).toDouble())
        val px = cx + cos(angle).toFloat() * orbitR
        val py = cy + sin(angle).toFloat() * orbitR
        val pCol = planetColors[idx]
        drawCircle(pCol.copy(alpha = 0.35f), 11.dp.toPx(), Offset(px, py))
        drawCircle(pCol, (5.5f - idx * 0.7f).dp.toPx(), Offset(px, py))
    }

    drawCircle(Color(0xF00B0B12), baseRadius * 0.93f, Offset(cx, cy))
    drawCircle(primaryGlow.copy(alpha = 0.25f), baseRadius * 0.93f, Offset(cx, cy), style = Stroke(width = 8.dp.toPx()))
    drawArc(
        brush = Brush.sweepGradient(listOf(primaryGlow, accentGlow, secondaryGlow, primaryGlow), Offset(cx, cy)),
        startAngle = -90f, sweepAngle = 360f * chargeFill, useCenter = false,
        topLeft = Offset(cx - baseRadius * 0.93f, cy - baseRadius * 0.93f),
        size = Size(baseRadius * 1.86f, baseRadius * 1.86f),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawVoxelRedstoneCharging(
    w: Float, h: Float, cx: Float, cy: Float, baseRadius: Float,
    chargeFill: Float, pulsePhase: Float, rotationPhase: Float,
    primaryGlow: Color, secondaryGlow: Color, accentGlow: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(primaryGlow.copy(alpha = 0.36f), secondaryGlow.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(cx, cy), radius = baseRadius * 2.2f
        ),
        radius = baseRadius * 2.2f, center = Offset(cx, cy)
    )
    drawBottomEnergyBeam(w, h, cx, cy + baseRadius * 0.90f, pulsePhase, primaryGlow, accentGlow, isVoxel = true)

    for (rIdx in 0..2) {
        val rf = (pulsePhase + rIdx * 0.33f) % 1f
        val half = baseRadius * (0.98f + rf * 0.70f)
        drawRect(
            color = primaryGlow.copy(alpha = (1f - rf) * 0.50f),
            topLeft = Offset(cx - half, cy - half),
            size = Size(half * 2f, half * 2f),
            style = Stroke(width = 4.dp.toPx())
        )
    }

    for (i in 0 until 16) {
        val angleDeg = rotationPhase * (if (i % 2 == 0) 0.55f else -0.4f) + i * 22.5f
        val rad = Math.toRadians(angleDeg.toDouble())
        val orbitDist = baseRadius * (1.18f + 0.22f * sin((pulsePhase * 2f * PI + i).toFloat()))
        val px = cx + cos(rad).toFloat() * orbitDist
        val py = cy + sin(rad).toFloat() * orbitDist
        val sq = 7.dp.toPx()
        val pColor = when (i % 3) { 0 -> primaryGlow; 1 -> secondaryGlow; else -> accentGlow }
        drawRect(pColor.copy(alpha = 0.88f), Offset(px - sq / 2f, py - sq / 2f), Size(sq, sq))
    }

    val boxSize = baseRadius * 1.76f
    val boxLeft = cx - boxSize / 2f
    val boxTop = cy - boxSize / 2f
    drawRect(Color(0xFF1A1E24), Offset(boxLeft, boxTop), Size(boxSize, boxSize))
    drawRect(primaryGlow, Offset(boxLeft, boxTop), Size(boxSize, boxSize), style = Stroke(width = 6.dp.toPx()))
    val segments = 10
    val barW = boxSize * 0.78f
    val segW = barW / segments
    val barLeft = cx - barW / 2f
    val barTop = boxTop + boxSize * 0.75f
    val filledSegments = (chargeFill * segments).toInt().coerceIn(1, segments)
    for (s in 0 until segments) {
        drawRect(
            color = if (s < filledSegments) secondaryGlow else Color(0xFF2E343D),
            topLeft = Offset(barLeft + s * segW + 2f, barTop),
            size = Size(segW - 4f, 14.dp.toPx())
        )
    }
}

private fun animationPalette(
    animationStyle: ChargingAnimationStyle,
    roomStyle: RoomStyleId,
    gdSubTheme: GeometryDashSubTheme = GeometryDashSubTheme.SONIC_WAVE
): Triple<Color, Color, Color> = when (animationStyle) {
    ChargingAnimationStyle.LIQUID_GLASS -> Triple(Color(0xFF00B4D8), Color(0xFF0077B6), Color(0xFF90E0EF))
    ChargingAnimationStyle.AURORA_WAVE -> Triple(Color(0xFF06D6A0), Color(0xFF8338EC), Color(0xFF4CC9F0))
    ChargingAnimationStyle.CYBER_PULSE -> Triple(Color(0xFF00F5D4), Color(0xFFFF006E), Color(0xFF8338EC))
    ChargingAnimationStyle.COSMIC_ORBIT -> Triple(Color(0xFFFFB703), Color(0xFF7209B7), Color(0xFFFFE599))
    ChargingAnimationStyle.VOXEL_REDSTONE -> Triple(Color(0xFFE53935), Color(0xFF5CE65C), Color(0xFFFFE066))
    ChargingAnimationStyle.MATCH_ROOM -> when (roomStyle) {
        RoomStyleId.COZY_MINIMALIST -> Triple(Color(0xFFE09F3E), Color(0xFF7CC49E), Color(0xFFFFE599))
        RoomStyleId.TEEN_ROOM -> Triple(Color(0xFF9B5DE5), Color(0xFF00F5D4), Color(0xFFF15BB5))
        RoomStyleId.KIDS_ROOM -> Triple(Color(0xFFFFB703), Color(0xFF4CC9F0), Color(0xFFFF6B6B))
        RoomStyleId.MINECRAFT -> Triple(Color(0xFFE53935), Color(0xFF5CE65C), Color(0xFFFFE066))
        RoomStyleId.GEOMETRY_DASH -> gdSubTheme.accentNeonColors()
    }
}
