package com.roomlauncher.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Privacy-first visualizer signal engine.
 * Intentionally does NOT request [android.Manifest.permission.RECORD_AUDIO] so Room Launcher
 * never accesses the device microphone and avoids Play Protect audio-spyware heuristics.
 * The Music Visualizer renders smooth, album-tinted procedural waves synced to live playback state.
 */
@Suppress("UNUSED_PARAMETER")
class AudioVisualizerEngine(context: Context) {

    private val _bands = MutableStateFlow(FloatArray(BAND_COUNT))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    private val _hasLiveSignal = MutableStateFlow(false)
    val hasLiveSignal: StateFlow<Boolean> = _hasLiveSignal.asStateFlow()

    fun hasAudioPermission(): Boolean = false

    fun setActive(shouldRun: Boolean) = Unit

    fun stop() = Unit

    companion object {
        const val BAND_COUNT = 16
    }
}
