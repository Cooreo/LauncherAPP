package com.roomlauncher.app.ui.room

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.AudioVisualizerEngine
import com.roomlauncher.app.data.LrcParser
import com.roomlauncher.app.data.LyricsState
import com.roomlauncher.app.data.MusicDisplayMode
import com.roomlauncher.app.data.NowPlayingState
import com.roomlauncher.app.data.VisualizerStyle
import com.roomlauncher.app.data.formatMusicTime
import com.roomlauncher.app.ui.theme.LocalRoomPalette
import com.roomlauncher.app.ui.theme.RoomInk
import com.roomlauncher.app.ui.theme.RoomSage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Compact floating Now-Playing capsule tucked neatly under the status bar on the Room Screen.
 * Shows album art, live mini equalizer, song title, artist, live progress bar, and quick controls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingCompactPill(
    nowPlaying: NowPlayingState,
    displayMode: MusicDisplayMode,
    onExpand: () -> Unit,
    onExpandToLyrics: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onExitDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalRoomPalette.current
    val accent = remember(nowPlaying.accentColorArgb, palette.isDark) {
        nowPlaying.accentColorArgb?.let { Color(it.toInt()) }
            ?: if (palette.isDark) Color(0xFF8CD1B3) else RoomSage
    }

    var livePosMs by remember(nowPlaying.trackKey, nowPlaying.positionMs, nowPlaying.positionUpdateTimeMs) {
        mutableLongStateOf(nowPlaying.currentPositionMs(SystemClock.elapsedRealtime()))
    }
    var eqPhase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(nowPlaying.isPlaying, nowPlaying.trackKey, nowPlaying.positionMs, nowPlaying.positionUpdateTimeMs) {
        livePosMs = nowPlaying.currentPositionMs(SystemClock.elapsedRealtime())
        while (nowPlaying.isPlaying) {
            delay(180L)
            val now = SystemClock.elapsedRealtime()
            val pos = nowPlaying.currentPositionMs(now)
            livePosMs = pos
            eqPhase = (eqPhase + 0.48f) % (2f * PI.toFloat() * 100f)
            if (nowPlaying.isDemo && nowPlaying.durationMs > 0L && pos >= nowPlaying.durationMs) {
                // Loop demo gracefully
                livePosMs = 0L
            }
        }
    }

    val progress = if (nowPlaying.durationMs > 0L) {
        (livePosMs.toFloat() / nowPlaying.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val pillBg = if (palette.isDark) {
        Color(0xD6141923)
    } else {
        RoomInk.copy(alpha = 0.84f)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = pillBg,
        shadowElevation = 8.dp,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 6.dp, start = 14.dp, end = 14.dp)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (nowPlaying.isPlaying) 0.45f else 0.22f),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onExpand)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 6.dp, top = 7.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // Album art or vinyl disc badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.22f))
                ) {
                    if (nowPlaying.albumArt != null) {
                        Image(
                            bitmap = nowPlaying.albumArt,
                            contentDescription = "Album art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Song title, artist & mini equalizer
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiniEqualizerBars(
                            isPlaying = nowPlaying.isPlaying,
                            phase = eqPhase,
                            color = accent,
                            modifier = Modifier.size(width = 14.dp, height = 12.dp)
                        )
                        Text(
                            text = nowPlaying.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = nowPlaying.artist.ifBlank {
                                if (nowPlaying.isDemo) "Demo Preview" else "Now Playing"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (nowPlaying.durationMs > 0L) {
                            Text(
                                text = "• ${formatMusicTime(livePosMs)} / ${formatMusicTime(nowPlaying.durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.copy(alpha = 0.92f),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Quick Lyrics / Visualizer jump button
                IconButton(
                    onClick = onExpandToLyrics,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (displayMode == MusicDisplayMode.LYRICS) {
                            Icons.Rounded.GraphicEq
                        } else {
                            Icons.Rounded.QueueMusic
                        },
                        contentDescription = "Open Lyrics or Visualizer",
                        tint = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Play / Pause button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.90f))
                        .clickable(onClick = onTogglePlayPause)
                ) {
                    Icon(
                        imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (nowPlaying.isPlaying) "Pause" else "Play",
                        tint = Color(0xFF10141A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (nowPlaying.isDemo) {
                    IconButton(
                        onClick = onExitDemo,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Exit demo",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Sleek edge-to-edge song progress indicator along bottom of pill
            if (nowPlaying.durationMs > 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accent.copy(alpha = 0.72f), accent)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniEqualizerBars(
    isPlaying: Boolean,
    phase: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 4
        val gap = size.width * 0.12f
        val barW = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val hFrac = if (isPlaying) {
                (0.28f + 0.68f * (0.5f + 0.5f * sin(phase * (1.2f + i * 0.37f) + i * 1.4f))).coerceIn(0.2f, 1f)
            } else {
                0.26f + (i % 2) * 0.14f
            }
            val barH = size.height * hFrac
            val x = i * (barW + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

/**
 * Expanded interactive Music Visualizer & Synced Lyrics overlay over the Room screen.
 * Lets the user toggle seamlessly between 3 animated visualizer modes and real-time LRCLIB synced lyrics,
 * scrub playback position, and control music transport.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicVisualizerOverlay(
    nowPlaying: NowPlayingState,
    displayMode: MusicDisplayMode,
    visualizerStyle: VisualizerStyle,
    lyricsState: LyricsState,
    fftBands: FloatArray,
    hasLiveFftSignal: Boolean,
    onSelectDisplayMode: (MusicDisplayMode) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenMusicApp: () -> Unit,
    onRetryLyrics: () -> Unit,
    onExitDemo: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalRoomPalette.current
    val accent = remember(nowPlaying.accentColorArgb, palette.isDark) {
        nowPlaying.accentColorArgb?.let { Color(it.toInt()) }
            ?: if (palette.isDark) Color(0xFF8CD1B3) else RoomSage
    }
    val secondaryAccent = remember(accent) {
        Color(
            red = (accent.red * 0.72f + 0.28f).coerceIn(0f, 1f),
            green = (accent.green * 0.85f + 0.10f).coerceIn(0f, 1f),
            blue = (accent.blue * 0.90f + 0.25f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    var livePosMs by remember(nowPlaying.trackKey, nowPlaying.positionMs, nowPlaying.positionUpdateTimeMs) {
        mutableLongStateOf(nowPlaying.currentPositionMs(SystemClock.elapsedRealtime()))
    }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    // Keep live timestamp smoothly updated for both the progress scrubber and synced lyrics highlighting
    LaunchedEffect(nowPlaying.isPlaying, nowPlaying.trackKey, nowPlaying.positionMs, nowPlaying.positionUpdateTimeMs) {
        livePosMs = nowPlaying.currentPositionMs(SystemClock.elapsedRealtime())
        while (nowPlaying.isPlaying) {
            delay(120L)
            if (!isScrubbing) {
                val now = SystemClock.elapsedRealtime()
                val pos = nowPlaying.currentPositionMs(now)
                livePosMs = pos
                if (nowPlaying.isDemo && nowPlaying.durationMs > 0L && pos >= nowPlaying.durationMs - 100L) {
                    onSeekTo(0L)
                }
            }
        }
    }

    val displayedPosMs = if (isScrubbing && nowPlaying.durationMs > 0L) {
        (scrubValue * nowPlaying.durationMs).toLong()
    } else {
        livePosMs
    }
    val sliderFraction = if (isScrubbing) {
        scrubValue
    } else if (nowPlaying.durationMs > 0L) {
        (livePosMs.toFloat() / nowPlaying.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val cardColor = if (palette.isDark) Color(0xF2141822) else Color(0xF51D222B)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 16.dp, vertical = 28.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cardColor,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.25.dp,
                    brush = Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.55f), Color.White.copy(alpha = 0.12f))
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header: Album Art + Track Title/Artist + Collapse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accent.copy(alpha = 0.20f))
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .clickable(enabled = !nowPlaying.isDemo, onClick = onOpenMusicApp)
                    ) {
                        if (nowPlaying.albumArt != null) {
                            Image(
                                bitmap = nowPlaying.albumArt,
                                contentDescription = "Open music player",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nowPlaying.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Text(
                            text = buildString {
                                append(nowPlaying.artist.ifBlank { "Unknown Artist" })
                                if (nowPlaying.album.isNotBlank()) {
                                    append(" • ")
                                    append(nowPlaying.album)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nowPlaying.isDemo) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "DEMO PREVIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Exit Demo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable(onClick = onExitDemo)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse music panel",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // 2. Segmented Switcher: Visualizer ↔ Synced Lyrics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModeToggleTab(
                        label = "Visualizer",
                        icon = Icons.Rounded.GraphicEq,
                        selected = displayMode == MusicDisplayMode.VISUALIZER,
                        accent = accent,
                        onClick = { onSelectDisplayMode(MusicDisplayMode.VISUALIZER) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeToggleTab(
                        label = "Lyrics",
                        icon = Icons.Rounded.QueueMusic,
                        selected = displayMode == MusicDisplayMode.LYRICS,
                        accent = accent,
                        onClick = { onSelectDisplayMode(MusicDisplayMode.LYRICS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Stage: Either Live Visualizer OR Synced Lyrics
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0C1018),
                                    accent.copy(alpha = 0.12f),
                                    Color(0xFF0B0E15)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
                ) {
                    Crossfade(
                        targetState = displayMode,
                        animationSpec = tween(durationMillis = 220),
                        label = "music_stage_crossfade"
                    ) { mode ->
                        when (mode) {
                            MusicDisplayMode.VISUALIZER -> {
                                MusicVisualizerStage(
                                    nowPlaying = nowPlaying,
                                    style = visualizerStyle,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    fftBands = fftBands,
                                    hasLiveFftSignal = hasLiveFftSignal,
                                    onSelectStyle = onSelectVisualizerStyle
                                )
                            }
                            MusicDisplayMode.LYRICS -> {
                                SyncedLyricsStage(
                                    lyricsState = lyricsState,
                                    positionMs = displayedPosMs,
                                    accent = accent,
                                    onSeekToLine = onSeekTo,
                                    onRetry = onRetryLyrics,
                                    onSwitchToVisualizer = { onSelectDisplayMode(MusicDisplayMode.VISUALIZER) }
                                )
                            }
                        }
                    }
                }

                // 4. Progress Scrubber + Timestamps
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = sliderFraction,
                        onValueChange = { next ->
                            isScrubbing = true
                            scrubValue = next
                        },
                        onValueChangeFinished = {
                            if (nowPlaying.durationMs > 0L) {
                                val target = (scrubValue * nowPlaying.durationMs).toLong()
                                livePosMs = target
                                onSeekTo(target)
                            }
                            isScrubbing = false
                        },
                        enabled = nowPlaying.durationMs > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMusicTime(displayedPosMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Text(
                            text = if (nowPlaying.durationMs > 0L) formatMusicTime(nowPlaying.durationMs) else "--:--",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }

                // 5. Playback Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .clickable(onClick = onTogglePlayPause)
                    ) {
                        Icon(
                            imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (nowPlaying.isPlaying) "Pause" else "Play",
                            tint = Color(0xFF0E131A),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next track",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggleTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF0F141B) else Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color(0xFF0F141B) else Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun MusicVisualizerStage(
    nowPlaying: NowPlayingState,
    style: VisualizerStyle,
    accent: Color,
    secondaryAccent: Color,
    fftBands: FloatArray,
    hasLiveFftSignal: Boolean,
    onSelectStyle: (VisualizerStyle) -> Unit
) {
    var animTimeSec by remember { mutableFloatStateOf(0f) }
    val peakCaps = remember { FloatArray(AudioVisualizerEngine.BAND_COUNT) }
    val energyScale by animateFloatAsState(
        targetValue = if (nowPlaying.isPlaying) 1f else 0.14f,
        animationSpec = tween(durationMillis = 360),
        label = "visualizer_energy"
    )
    val trackSeed = remember(nowPlaying.trackKey) {
        (abs(nowPlaying.title.hashCode()) % 97) * 0.13f
    }

    LaunchedEffect(nowPlaying.isPlaying) {
        var lastNanos = 0L
        while (nowPlaying.isPlaying) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    animTimeSec += dt
                }
                lastNanos = frameNanos
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Compute 16 normalized spectrum bands (real-time FFT if live signal, else rich organic beat wave)
        val computedBands = remember(animTimeSec, energyScale, fftBands, hasLiveFftSignal, trackSeed) {
            FloatArray(AudioVisualizerEngine.BAND_COUNT) { idx ->
                val raw = if (hasLiveFftSignal && idx < fftBands.size) {
                    fftBands[idx]
                } else {
                    val norm = idx.toFloat() / (AudioVisualizerEngine.BAND_COUNT - 1).toFloat()
                    // Bass-heavier envelope that feels genuinely musical
                    val bassWeight = 1.12f - norm * 0.42f
                    val beat = (0.5f + 0.5f * sin(animTimeSec * 4.3f + trackSeed))
                    val subBeat = (0.5f + 0.5f * sin(animTimeSec * 8.6f - idx * 0.45f))
                    val wave1 = 0.5f + 0.5f * sin(animTimeSec * (2.6f + idx * 0.24f) + idx * 0.85f + trackSeed)
                    val wave2 = 0.5f + 0.5f * cos(animTimeSec * (4.1f - idx * 0.12f) + idx * 1.3f)
                    val combined = (wave1 * 0.45f + wave2 * 0.30f + beat * subBeat * 0.40f) * bassWeight
                    combined.coerceIn(0.08f, 0.98f)
                }
                val scaled = (raw * energyScale + 0.05f).coerceIn(0.05f, 1f)
                if (scaled >= peakCaps[idx]) {
                    peakCaps[idx] = scaled
                } else {
                    peakCaps[idx] = (peakCaps[idx] - 0.012f).coerceAtLeast(scaled)
                }
                scaled
            }
        }

        when (style) {
            VisualizerStyle.SPECTRUM_BARS -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(top = 42.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)) {
                    val count = computedBands.size
                    val gap = size.width * 0.022f
                    val barW = (size.width - gap * (count - 1)) / count
                    val baselineY = size.height * 0.78f
                    val maxBarH = size.height * 0.72f
                    val maxReflectH = size.height * 0.18f

                    // Soft ambient radial glow behind bars
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.22f * energyScale), Color.Transparent),
                            center = Offset(size.width * 0.5f, baselineY * 0.75f),
                            radius = size.width * 0.55f
                        )
                    )

                    for (i in 0 until count) {
                        val level = computedBands[i]
                        val peak = peakCaps[i]
                        val barH = maxBarH * level
                        val x = i * (barW + gap)
                        val topY = baselineY - barH

                        // Main glowing bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(secondaryAccent, accent, accent.copy(alpha = 0.55f)),
                                startY = topY,
                                endY = baselineY
                            ),
                            topLeft = Offset(x, topY),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barW * 0.42f, barW * 0.42f)
                        )

                        // Peak cap floating slightly above bar
                        val capY = (baselineY - maxBarH * peak - 4.dp.toPx()).coerceAtLeast(0f)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.82f),
                            topLeft = Offset(x, capY),
                            size = Size(barW, 3.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )

                        // Soft floor reflection
                        val reflH = maxReflectH * level
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(accent.copy(alpha = 0.26f), Color.Transparent),
                                startY = baselineY + 3.dp.toPx(),
                                endY = baselineY + 3.dp.toPx() + reflH
                            ),
                            topLeft = Offset(x, baselineY + 3.dp.toPx()),
                            size = Size(barW, reflH),
                            cornerRadius = CornerRadius(barW * 0.3f, barW * 0.3f)
                        )
                    }
                }
            }

            VisualizerStyle.SOUND_WAVE -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(top = 38.dp, bottom = 10.dp)) {
                    val w = size.width
                    val h = size.height
                    val midY = h * 0.54f
                    val avgEnergy = computedBands.average().toFloat()

                    // Ambient glow center
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.26f * energyScale), Color.Transparent),
                            center = Offset(w * 0.5f, midY),
                            radius = w * 0.5f
                        )
                    )

                    val layerConfigs = listOf(
                        Triple(accent.copy(alpha = 0.32f), 1.0f, 0f),
                        Triple(secondaryAccent.copy(alpha = 0.55f), 1.55f, 1.8f),
                        Triple(Color.White.copy(alpha = 0.85f), 2.15f, 3.3f)
                    )

                    layerConfigs.forEachIndexed { layerIdx, (color, freqMul, phaseShift) ->
                        val path = Path()
                        val steps = 64
                        for (s in 0..steps) {
                            val t = s.toFloat() / steps.toFloat()
                            val x = t * w
                            val bandIdx = (t * (computedBands.size - 1)).toInt().coerceIn(0, computedBands.lastIndex)
                            val bandAmp = computedBands[bandIdx]
                            // Hanning window so wave tapers smoothly at both edges
                            val edgeWindow = sin(t * PI).toFloat()
                            val wave = sin(t * PI.toFloat() * (3.5f * freqMul) + animTimeSec * (3.4f + layerIdx * 1.1f) + phaseShift)
                            val amp = h * 0.34f * edgeWindow * (0.35f * avgEnergy + 0.65f * bandAmp)
                            val y = midY + wave * amp
                            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = if (layerIdx == 2) 3.dp.toPx() else 5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }

            VisualizerStyle.VINYL_GROOVE -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 34.dp, bottom = 6.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = min(size.width, size.height) * 0.33f
                        val spinAngle = animTimeSec * 48f

                        // Outer radial spectrum spikes pulsing around the vinyl record
                        val rayCount = 36
                        for (r in 0 until rayCount) {
                            val angleRad = (r.toFloat() / rayCount.toFloat()) * 2f * PI.toFloat() + animTimeSec * 0.35f
                            val band = computedBands[r % computedBands.size]
                            val startR = baseRadius + 6.dp.toPx()
                            val endR = startR + (baseRadius * 0.44f * band)
                            val start = Offset(center.x + cos(angleRad) * startR, center.y + sin(angleRad) * startR)
                            val end = Offset(center.x + cos(angleRad) * endR, center.y + sin(angleRad) * endR)
                            drawLine(
                                color = if (r % 2 == 0) accent.copy(alpha = 0.85f) else secondaryAccent.copy(alpha = 0.72f),
                                start = start,
                                end = end,
                                strokeWidth = 3.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Dark vinyl disc body
                        drawCircle(color = Color(0xFF14161D), radius = baseRadius, center = center)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.14f),
                            radius = baseRadius,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Subtle vinyl grooves
                        for (g in 1..4) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.07f),
                                radius = baseRadius * (0.55f + g * 0.095f),
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Spinning specular reflection wedge
                        val highlightRad = Math.toRadians(spinAngle.toDouble()).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(
                                center.x + cos(highlightRad) * baseRadius * 0.48f,
                                center.y + sin(highlightRad) * baseRadius * 0.48f
                            ),
                            end = Offset(
                                center.x + cos(highlightRad) * baseRadius * 0.92f,
                                center.y + sin(highlightRad) * baseRadius * 0.92f
                            ),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Center label & spindle hole
                        drawCircle(color = accent, radius = baseRadius * 0.38f, center = center)
                        drawCircle(color = Color(0xFF10131A), radius = baseRadius * 0.10f, center = center)
                    }

                    // If we have album art, spin it right inside the vinyl center label!
                    if (nowPlaying.albumArt != null) {
                        Image(
                            bitmap = nowPlaying.albumArt,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer { rotationZ = (animTimeSec * 42f) % 360f }
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF10131A), CircleShape)
                        )
                    }
                }
            }
        }

        // Top overlay bar inside visualizer stage: style switcher chips + live signal badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                VisualizerStyle.entries.forEach { option ->
                    val selected = option == style
                    val shortLabel = when (option) {
                        VisualizerStyle.SPECTRUM_BARS -> "Bars"
                        VisualizerStyle.SOUND_WAVE -> "Wave"
                        VisualizerStyle.VINYL_GROOVE -> "Vinyl"
                    }
                    Text(
                        text = shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color(0xFF0E131A) else Color.White.copy(alpha = 0.78f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) accent else Color.White.copy(alpha = 0.11f))
                            .clickable { onSelectStyle(option) }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.09f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (hasLiveFftSignal) Color(0xFF4ADE80) else accent.copy(alpha = 0.85f))
                )
                Text(
                    text = if (hasLiveFftSignal) "Live Audio" else "Ambient",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SyncedLyricsStage(
    lyricsState: LyricsState,
    positionMs: Long,
    accent: Color,
    onSeekToLine: (Long) -> Unit,
    onRetry: () -> Unit,
    onSwitchToVisualizer: () -> Unit
) {
    when (lyricsState) {
        LyricsState.Idle, LyricsState.Loading -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(20.dp)
            ) {
                CircularProgressIndicator(
                    color = accent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Fetching synced lyrics from LRCLIB…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
            }
        }

        is LyricsState.Synced -> {
            val lines = lyricsState.lines
            val activeIndex = remember(lines, positionMs) {
                LrcParser.findActiveLineIndex(lines, positionMs)
            }
            val listState = rememberLazyListState()

            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0 && !listState.isScrollInProgress) {
                    runCatching {
                        listState.animateScrollToItem(
                            index = activeIndex,
                            scrollOffset = -150
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 38.dp, bottom = 52.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(lines, key = { idx, line -> "${idx}_${line.timeMs}" }) { index, line ->
                        val isActive = index == activeIndex
                        val isPast = activeIndex >= 0 && index < activeIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) accent.copy(alpha = 0.16f) else Color.Transparent
                                )
                                .clickable { onSeekToLine(line.timeMs) }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(22.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = line.text,
                                style = if (isActive) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isActive -> Color.White
                                    isPast -> Color.White.copy(alpha = 0.42f)
                                    else -> Color.White.copy(alpha = 0.66f)
                                }
                            )
                        }
                    }
                }

                // Subtle top-right source & tap-to-seek hint badge
                Text(
                    text = "Synced • ${lyricsState.source} • Tap line to seek",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC0D1119))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        is LyricsState.Plain -> {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 36.dp, bottom = 28.dp, start = 18.dp, end = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(lyricsState.lines) { _, line ->
                        if (line.isEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.86f)
                            )
                        }
                    }
                }
                Text(
                    text = "Unsynced • ${lyricsState.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC0D1119))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        LyricsState.Instrumental -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Instrumental Track ♪",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enjoy the music in Visualizer mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(onClick = onSwitchToVisualizer, shape = RoundedCornerShape(12.dp)) {
                    Text("Switch to Visualizer", color = accent)
                }
            }
        }

        LyricsState.NotFound, is LyricsState.Error -> {
            val titleText = if (lyricsState is LyricsState.Error) {
                "Couldn't load lyrics right now"
            } else {
                "No lyrics found on LRCLIB"
            }
            val subText = if (lyricsState is LyricsState.Error) {
                lyricsState.message
            } else {
                "Some remixes or newly released tracks aren't indexed yet."
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(22.dp)
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", color = Color.White)
                    }
                    OutlinedButton(onClick = onSwitchToVisualizer, shape = RoundedCornerShape(12.dp)) {
                        Text("Visualizer", color = accent)
                    }
                }
            }
        }
    }
}
