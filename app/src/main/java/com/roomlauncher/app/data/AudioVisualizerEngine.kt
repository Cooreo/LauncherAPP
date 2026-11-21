package com.roomlauncher.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow

/**
 * Optional real-time audio FFT capture via Android's [Visualizer] on global output mix (session 0).
 * Only attaches when explicitly enabled by the user AND [Manifest.permission.RECORD_AUDIO] is granted.
 * Automatically tracks whether non-silent signal is arriving so the UI can seamlessly blend to
 * procedural ambient waves on Bluetooth codecs or DRM streams that zero out session 0.
 */
class AudioVisualizerEngine(private val context: Context) {

    private val appContext = context.applicationContext
    private var visualizer: Visualizer? = null

    private val _bands = MutableStateFlow(FloatArray(BAND_COUNT))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    private val _hasLiveSignal = MutableStateFlow(false)
    val hasLiveSignal: StateFlow<Boolean> = _hasLiveSignal.asStateFlow()

    private val smoothed = FloatArray(BAND_COUNT)
    private var lastSignalTimeMs = 0L

    fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun setActive(shouldRun: Boolean) {
        if (shouldRun && hasAudioPermission()) {
            startIfNeeded()
        } else {
            stop()
        }
    }

    private fun startIfNeeded() {
        if (visualizer != null) return
        runCatching {
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = range[1].coerceAtMost(1024).coerceAtLeast(range[0])
            val rate = (Visualizer.getMaxCaptureRate() / 2).coerceAtLeast(1000)

            val viz = Visualizer(0).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || fft.size < 4) return
                            processFft(fft)
                        }
                    },
                    rate,
                    false,
                    true
                )
                enabled = true
            }
            visualizer = viz
        }.onFailure {
            stop()
        }
    }

    fun stop() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        smoothed.fill(0f)
        _bands.value = FloatArray(BAND_COUNT)
        _hasLiveSignal.value = false
    }

    private fun processFft(fft: ByteArray) {
        val binCount = fft.size / 2
        if (binCount <= 2) return

        val next = FloatArray(BAND_COUNT)
        var peakEnergy = 0f

        for (band in 0 until BAND_COUNT) {
            // Logarithmic mapping from low bass bins to higher treble bins
            val startFrac = band.toFloat() / BAND_COUNT.toFloat()
            val endFrac = (band + 1).toFloat() / BAND_COUNT.toFloat()
            val startBin = (1 + ((binCount - 2) * startFrac.pow(1.85f)).toInt()).coerceIn(1, binCount - 1)
            val endBin = (1 + ((binCount - 2) * endFrac.pow(1.85f)).toInt()).coerceIn(startBin, binCount - 1)

            var sumMag = 0f
            var count = 0
            for (k in startBin..endBin) {
                val re = fft[2 * k].toFloat()
                val im = fft[2 * k + 1].toFloat()
                val mag = hypot(re, im)
                sumMag += mag
                count++
            }
            val avgMag = if (count > 0) sumMag / count else 0f
            // Compress dynamic range with log curve so quiet passages still look lively
            val normalized = (ln(1f + avgMag * 0.45f) / 3.9f).coerceIn(0f, 1f)
            if (normalized > peakEnergy) peakEnergy = normalized

            val prev = smoothed[band]
            val smooth = if (normalized > prev) {
                prev * 0.35f + normalized * 0.65f
            } else {
                prev * 0.72f + normalized * 0.28f
            }
            smoothed[band] = smooth
            next[band] = smooth
        }

        val now = SystemClock.elapsedRealtime()
        if (peakEnergy > 0.04f) {
            lastSignalTimeMs = now
            _hasLiveSignal.value = true
        } else if (now - lastSignalTimeMs > 1600L) {
            _hasLiveSignal.value = false
        }

        _bands.value = next
    }

    companion object {
        const val BAND_COUNT = 16
    }
}
