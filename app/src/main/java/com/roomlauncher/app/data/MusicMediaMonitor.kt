package com.roomlauncher.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.roomlauncher.app.system.RoomNotificationListenerService
import com.roomlauncher.app.system.SystemIntentHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

/**
 * Monitors active Android system media sessions (Spotify, YouTube Music, Apple Music, etc.)
 * via [MediaSessionManager] and emits live [NowPlayingState] updates + transport controls.
 */
class MusicMediaMonitor(private val context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaSessionManager =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private val _nowPlaying = MutableStateFlow(NowPlayingState())
    val nowPlaying: StateFlow<NowPlayingState> = _nowPlaying.asStateFlow()

    private val registeredControllers = mutableMapOf<MediaController, MediaController.Callback>()
    private var activeController: MediaController? = null
    private var isListening = false
    private var isDemoActive = false

    private var cachedArtKey: String = ""
    private var cachedArtBitmap: ImageBitmap? = null
    private var cachedAccentArgb: Long? = null

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        syncControllers(controllers.orEmpty())
    }

    fun isNotificationAccessGranted(): Boolean =
        RoomNotificationListenerService.isNotificationAccessGranted(appContext)

    fun startOrRefresh() {
        val manager = mediaSessionManager ?: return
        val component = RoomNotificationListenerService.componentName(appContext)
        if (!isNotificationAccessGranted()) {
            detachSystemControllers()
            if (!isDemoActive) {
                _nowPlaying.value = NowPlayingState()
            }
            return
        }

        if (!isListening) {
            try {
                manager.addOnActiveSessionsChangedListener(sessionsChangedListener, component, mainHandler)
                isListening = true
            } catch (_: SecurityException) {
                isListening = false
                return
            } catch (_: Exception) {
                isListening = false
                return
            }
        }

        val controllers = try {
            manager.getActiveSessions(component)
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        syncControllers(controllers)
    }

    fun stop() {
        if (isListening) {
            runCatching {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            }
            isListening = false
        }
        detachSystemControllers()
    }

    fun startDemoPreview() {
        isDemoActive = true
        _nowPlaying.value = NowPlayingState(
            hasSession = true,
            isDemo = true,
            title = "Midnight Lo-Fi",
            artist = "Cozy Room Sessions",
            album = "Rainy Window EP",
            durationMs = DEMO_DURATION_MS,
            positionMs = 0L,
            positionUpdateTimeMs = SystemClock.elapsedRealtime(),
            playbackSpeed = 1f,
            isPlaying = true,
            albumArt = null,
            accentColorArgb = 0xFF7FA692L
        )
    }

    fun stopDemoPreview() {
        if (!isDemoActive) return
        isDemoActive = false
        if (activeController != null) {
            publishFromController(activeController)
        } else {
            _nowPlaying.value = NowPlayingState()
        }
    }

    fun togglePlayPause() {
        if (isDemoActive) {
            val current = _nowPlaying.value
            val now = SystemClock.elapsedRealtime()
            val pos = current.currentPositionMs(now)
            val nextPlaying = !current.isPlaying
            val restartPos = if (nextPlaying && pos >= current.durationMs - 400L) 0L else pos
            _nowPlaying.value = current.copy(
                positionMs = restartPos,
                positionUpdateTimeMs = now,
                isPlaying = nextPlaying
            )
            return
        }
        val controller = activeController ?: return
        val state = controller.playbackState?.state
        runCatching {
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        }
    }

    fun skipNext() {
        if (isDemoActive) {
            seekTo(0L)
            return
        }
        runCatching { activeController?.transportControls?.skipToNext() }
    }

    fun skipPrevious() {
        if (isDemoActive) {
            seekTo(0L)
            return
        }
        runCatching { activeController?.transportControls?.skipToPrevious() }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        if (isDemoActive) {
            val current = _nowPlaying.value
            val safe = if (current.durationMs > 0L) clamped.coerceIn(0L, current.durationMs) else clamped
            _nowPlaying.value = current.copy(
                positionMs = safe,
                positionUpdateTimeMs = SystemClock.elapsedRealtime()
            )
            return
        }
        val controller = activeController ?: return
        runCatching {
            controller.transportControls.seekTo(clamped)
            // Optimistically update local position so the scrubber & lyrics jump smoothly right away
            _nowPlaying.value = _nowPlaying.value.copy(
                positionMs = clamped,
                positionUpdateTimeMs = SystemClock.elapsedRealtime()
            )
        }
    }

    fun openActivePlayer() {
        if (isDemoActive) return
        val controller = activeController ?: return
        val sessionActivity = controller.sessionActivity
        if (sessionActivity != null) {
            val sent = runCatching {
                sessionActivity.send()
                true
            }.getOrDefault(false)
            if (sent) return
        }
        val pkg = controller.packageName
        if (!pkg.isNullOrBlank()) {
            SystemIntentHelper.launchApp(appContext, pkg)
        }
    }

    private fun detachSystemControllers() {
        registeredControllers.forEach { (controller, callback) ->
            runCatching { controller.unregisterCallback(callback) }
        }
        registeredControllers.clear()
        activeController = null
    }

    private fun syncControllers(controllers: List<MediaController>) {
        // Unregister callbacks for controllers no longer in the list
        val currentTokens = controllers.map { it.sessionToken }.toSet()
        val iterator = registeredControllers.entries.iterator()
        while (iterator.hasNext()) {
            val (controller, callback) = iterator.next()
            if (controller.sessionToken !in currentTokens) {
                runCatching { controller.unregisterCallback(callback) }
                iterator.remove()
            }
        }

        // Register callbacks for newly discovered controllers
        controllers.forEach { controller ->
            val alreadyTracked = registeredControllers.keys.any { it.sessionToken == controller.sessionToken }
            if (!alreadyTracked) {
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        reselectAndPublish()
                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        reselectAndPublish()
                    }

                    override fun onSessionDestroyed() {
                        registeredControllers.remove(controller)
                        reselectAndPublish()
                    }
                }
                runCatching {
                    controller.registerCallback(callback, mainHandler)
                    registeredControllers[controller] = callback
                }
            }
        }

        reselectAndPublish(controllers)
    }

    private fun reselectAndPublish(overrideList: List<MediaController>? = null) {
        val candidates = overrideList ?: registeredControllers.keys.toList()
        val chosen = pickBestController(candidates)
        activeController = chosen

        // If real music starts playing, seamlessly exit Demo Preview!
        if (chosen != null && isControllerPlaying(chosen) && isDemoActive) {
            isDemoActive = false
        }
        if (isDemoActive) return

        publishFromController(chosen)
    }

    private fun pickBestController(controllers: List<MediaController>): MediaController? {
        if (controllers.isEmpty()) return null
        return controllers.firstOrNull { isControllerPlaying(it) && hasReadableTitle(it) }
            ?: controllers.firstOrNull { isControllerPlaying(it) }
            ?: controllers.firstOrNull { hasReadableTitle(it) && it.playbackState?.state == PlaybackState.STATE_PAUSED }
            ?: controllers.firstOrNull { hasReadableTitle(it) }
    }

    private fun isControllerPlaying(controller: MediaController): Boolean {
        val state = controller.playbackState?.state
        return state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
    }

    private fun hasReadableTitle(controller: MediaController): Boolean {
        val meta = controller.metadata ?: return false
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        return !title.isNullOrBlank()
    }

    private fun publishFromController(controller: MediaController?) {
        if (controller == null) {
            _nowPlaying.value = NowPlayingState()
            return
        }
        val meta = controller.metadata
        val playback = controller.playbackState
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: ""

        if (title.isBlank()) {
            _nowPlaying.value = NowPlayingState()
            return
        }

        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?.takeIf { it.isNotBlank() }
            ?: ""

        val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val durationMs = (meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)
        val rawPosition = (playback?.position ?: 0L).coerceAtLeast(0L)
        val updateTime = playback?.lastPositionUpdateTime?.takeIf { it > 0L }
            ?: SystemClock.elapsedRealtime()
        val speed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f
        val isPlaying = playback?.state == PlaybackState.STATE_PLAYING ||
            playback?.state == PlaybackState.STATE_BUFFERING

        val rawBitmap = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val artKey = "$title|$artist|${rawBitmap?.width ?: 0}x${rawBitmap?.height ?: 0}|${rawBitmap?.generationId ?: 0}"
        if (artKey != cachedArtKey) {
            cachedArtKey = artKey
            if (rawBitmap != null && !rawBitmap.isRecycled) {
                val scaled = scaleBitmapForThumbnail(rawBitmap, 192)
                cachedArtBitmap = scaled.asImageBitmap()
                cachedAccentArgb = extractVibrantAccentColor(scaled)
            } else {
                cachedArtBitmap = null
                cachedAccentArgb = null
            }
        }

        _nowPlaying.value = NowPlayingState(
            hasSession = true,
            isDemo = false,
            title = title,
            artist = artist,
            album = album,
            packageName = controller.packageName.orEmpty(),
            durationMs = durationMs,
            positionMs = rawPosition,
            positionUpdateTimeMs = updateTime,
            playbackSpeed = speed,
            isPlaying = isPlaying,
            albumArt = cachedArtBitmap,
            accentColorArgb = cachedAccentArgb
        )
    }

    private fun scaleBitmapForThumbnail(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val scale = maxDim.toFloat() / max(w, h).toFloat()
        val dstW = (w * scale).toInt().coerceAtLeast(1)
        val dstH = (h * scale).toInt().coerceAtLeast(1)
        return runCatching { Bitmap.createScaledBitmap(source, dstW, dstH, true) }
            .getOrDefault(source)
    }

    private fun extractVibrantAccentColor(bitmap: Bitmap): Long? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null
        val stepX = max(1, w / 16)
        val stepY = max(1, h / 16)
        val hsv = FloatArray(3)
        var bestScore = -1f
        var bestColor = 0
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                val pixel = bitmap.getPixel(x, y)
                if (AndroidColor.alpha(pixel) < 200) continue
                AndroidColor.colorToHSV(pixel, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                // Prefer vivid, mid-bright colors that look great glowing in a visualizer
                if (value in 0.22f..0.95f) {
                    val score = sat * 1.4f + (1f - kotlin.math.abs(value - 0.65f)) * 0.6f
                    if (score > bestScore && sat > 0.25f) {
                        bestScore = score
                        bestColor = pixel
                    }
                    rSum += AndroidColor.red(pixel)
                    gSum += AndroidColor.green(pixel)
                    bSum += AndroidColor.blue(pixel)
                    count++
                }
            }
        }

        val chosen = if (bestScore > 0f) {
            bestColor
        } else if (count > 0) {
            AndroidColor.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
        } else {
            return null
        }

        // Lift brightness slightly if too dark so bars pop cleanly on dark glass
        AndroidColor.colorToHSV(chosen, hsv)
        hsv[1] = min(1f, max(hsv[1], 0.42f))
        hsv[2] = max(hsv[2], 0.68f)
        val boosted = AndroidColor.HSVToColor(255, hsv)
        return boosted.toLong() and 0xFFFFFFFFL
    }

    companion object {
        const val DEMO_DURATION_MS = 34_000L
    }
}
