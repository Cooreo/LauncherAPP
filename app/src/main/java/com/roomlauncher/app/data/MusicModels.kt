package com.roomlauncher.app.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable

@Serializable
enum class MusicDisplayMode(val displayName: String) {
    VISUALIZER("Visualizer"),
    LYRICS("Lyrics")
}

@Serializable
enum class VisualizerStyle(val displayName: String, val description: String) {
    SPECTRUM_BARS("Spectrum Bars", "Glowing equalizer bars with falling peak caps"),
    SOUND_WAVE("Sound Wave", "Layered glowing harmonic waveforms"),
    VINYL_GROOVE("Vinyl Groove", "Spinning vinyl record with radial audio pulses")
}

@Immutable
data class LyricsLine(
    val timeMs: Long,
    val text: String
)

@Immutable
sealed interface LyricsState {
    data object Idle : LyricsState
    data object Loading : LyricsState
    data class Synced(val lines: List<LyricsLine>, val source: String = "LRCLIB") : LyricsState
    data class Plain(val lines: List<String>, val source: String = "LRCLIB") : LyricsState
    data object Instrumental : LyricsState
    data object NotFound : LyricsState
    data class Error(val message: String) : LyricsState
}

@Immutable
data class NowPlayingState(
    val hasSession: Boolean = false,
    val isDemo: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val positionUpdateTimeMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val isPlaying: Boolean = false,
    val albumArt: ImageBitmap? = null,
    val accentColorArgb: Long? = null
) {
    val trackKey: String
        get() = if (!hasSession || title.isBlank()) {
            ""
        } else {
            val durSec = (durationMs / 1000L).coerceAtLeast(0L)
            "${title.trim().lowercase()}|${artist.trim().lowercase()}|$durSec|$isDemo"
        }

    fun currentPositionMs(elapsedRealtimeNow: Long): Long {
        if (!hasSession) return 0L
        val computed = if (isPlaying && positionUpdateTimeMs > 0L) {
            val delta = (elapsedRealtimeNow - positionUpdateTimeMs).coerceAtLeast(0L)
            val speed = if (playbackSpeed > 0f) playbackSpeed else 1f
            positionMs + (delta * speed).toLong()
        } else {
            positionMs
        }
        return if (durationMs > 0L) {
            computed.coerceIn(0L, durationMs)
        } else {
            computed.coerceAtLeast(0L)
        }
    }

    fun progressFraction(elapsedRealtimeNow: Long): Float {
        if (durationMs <= 0L) return 0f
        return (currentPositionMs(elapsedRealtimeNow).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
}

fun formatMusicTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
