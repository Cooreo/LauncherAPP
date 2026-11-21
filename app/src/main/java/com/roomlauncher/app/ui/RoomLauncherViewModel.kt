package com.roomlauncher.app.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roomlauncher.app.BuildConfig
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.AppRepository
import com.roomlauncher.app.data.AppUpdateRepository
import com.roomlauncher.app.data.AppUpdateUiState
import com.roomlauncher.app.data.AudioVisualizerEngine
import com.roomlauncher.app.data.BatteryMonitor
import com.roomlauncher.app.data.BatteryStatus
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.LyricsRepository
import com.roomlauncher.app.data.LyricsState
import com.roomlauncher.app.data.MusicDisplayMode
import com.roomlauncher.app.data.MusicMediaMonitor
import com.roomlauncher.app.data.NowPlayingState
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomConfigRepository
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.UpdateCheckResult
import com.roomlauncher.app.data.VisualizerStyle
import com.roomlauncher.app.data.WeatherRepository
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.data.WebShortcut
import com.roomlauncher.app.system.AppUpdateInstaller
import com.roomlauncher.app.system.RoomNotificationListenerService
import com.roomlauncher.app.system.RoomWallpaperRenderer
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import com.roomlauncher.app.ui.theme.RoomThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.UUID

@Immutable
data class RoomUiState(
    val isLoaded: Boolean = false,
    val config: RoomConfig = RoomConfigRepository.createDefaultRoomConfig(),
    val installedApps: List<AppInfo> = emptyList(),
    val packageOwnerLabels: Map<String, String> = emptyMap(),
    val defaultUnsortedLabel: String = "Trash Can",
    val weatherState: WeatherState = WeatherState(),
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val activeChargingSplashKey: Long = 0L,
    val nowPlaying: NowPlayingState = NowPlayingState(),
    val lyricsState: LyricsState = LyricsState.Idle,
    val musicDisplayMode: MusicDisplayMode = MusicDisplayMode.VISUALIZER,
    val isMusicExpanded: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val updateState: AppUpdateUiState = AppUpdateUiState(currentVersion = BuildConfig.VERSION_NAME),
    val currentHourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val frameImageBitmap: ImageBitmap? = null,
    val posterLeftBitmap: ImageBitmap? = null,
    val posterRightBitmap: ImageBitmap? = null,
    val isEditMode: Boolean = false,
    val activeHotspotId: String? = null,
    val editingHotspotId: String? = null,
    val showAddHotspotDialog: Boolean = false,
    val showAllAppsSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val isSyncingWallpaper: Boolean = false
) {
    val effectiveThemeId: RoomThemeId
        get() = if (config.dynamicThemeEnabled) {
            RoomThemes.dynamicThemeForHour(currentHourOfDay)
        } else {
            config.themeId
        }

    val activeHotspot: RoomHotspot?
        get() = config.hotspots.firstOrNull { it.id == activeHotspotId }

    val editingHotspot: RoomHotspot?
        get() = config.hotspots.firstOrNull { it.id == editingHotspotId }

    fun appsFor(hotspot: RoomHotspot): List<AppInfo> =
        RoomConfigRepository.appsForHotspot(hotspot, config, installedApps)

    val panelHotspots: List<RoomHotspot>
        get() = config.hotspots.filter {
            it.actionType == HotspotActionType.PANEL || it.actionType == HotspotActionType.UNSORTED_PANEL
        }
}

class RoomLauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val configRepository = RoomConfigRepository(appContext)
    private val appRepository = AppRepository(appContext, viewModelScope)
    private val weatherRepository = WeatherRepository(appContext)
    private val batteryMonitor = BatteryMonitor(appContext)
    private val musicMediaMonitor = MusicMediaMonitor(appContext)
    private val lyricsRepository = LyricsRepository()
    private val audioVisualizerEngine = AudioVisualizerEngine(appContext)
    private val appUpdateRepository = AppUpdateRepository()

    val fftBands: StateFlow<FloatArray> = audioVisualizerEngine.bands
    val hasLiveFftSignal: StateFlow<Boolean> = audioVisualizerEngine.hasLiveSignal

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    private var lastObservedPlugInId = 0L
    private var lastLyricsTrackKey = ""
    private var lyricsJob: Job? = null
    private var updateCheckJob: Job? = null
    private var updateDownloadJob: Job? = null
    private var dismissedUpdateVersion: String? = null
    private var isAppInForeground = true

    init {
        batteryMonitor.start()
        musicMediaMonitor.startOrRefresh()
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                refreshDynamicThemeHour()
            }
        }
        viewModelScope.launch {
            musicMediaMonitor.nowPlaying.collect { track ->
                val trackChanged = track.trackKey != lastLyricsTrackKey
                _uiState.update { state ->
                    state.copy(
                        nowPlaying = track,
                        isMusicExpanded = if (track.hasSession) state.isMusicExpanded else false,
                        lyricsState = if (!track.hasSession) {
                            LyricsState.Idle
                        } else if (trackChanged) {
                            LyricsState.Idle
                        } else {
                            state.lyricsState
                        }
                    )
                }
                if (trackChanged) {
                    lyricsJob?.cancel()
                    lastLyricsTrackKey = track.trackKey
                    val cur = _uiState.value
                    if (track.hasSession && cur.isMusicExpanded && cur.musicDisplayMode == MusicDisplayMode.LYRICS) {
                        fetchLyricsForCurrentTrack(forceRefresh = false)
                    }
                }
                syncAudioVisualizerState()
            }
        }
        viewModelScope.launch {
            batteryMonitor.status.collect { status ->
                val triggerSplash = status.plugInEventId > lastObservedPlugInId &&
                    _uiState.value.config.chargingAnimationEnabled &&
                    _uiState.value.config.onboardingCompleted
                lastObservedPlugInId = status.plugInEventId
                _uiState.update { state ->
                    state.copy(
                        batteryStatus = status,
                        activeChargingSplashKey = if (triggerSplash) {
                            System.nanoTime()
                        } else {
                            state.activeChargingSplashKey
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            val loadedConfig = configRepository.loadConfig()
            val frameBmp = loadFrameBitmap(loadedConfig.framePhotoPath)
            val posterLeftBmp = loadFrameBitmap(loadedConfig.posterLeftPath)
            val posterRightBmp = loadFrameBitmap(loadedConfig.posterRightPath)
            val isDefault = SystemIntentHelper.isDefaultHomeLauncher(appContext)
            val isBatteryIgnored = SystemIntentHelper.isIgnoringBatteryOptimizations(appContext)
            val hasNotifAccess = musicMediaMonitor.isNotificationAccessGranted()
            val hasAudioPerm = audioVisualizerEngine.hasAudioPermission()

            // If the user just became default launcher while on the final onboarding step, finish onboarding cleanly
            val effectiveConfig = if (!loadedConfig.onboardingCompleted && loadedConfig.onboardingStep >= 3 && isDefault) {
                loadedConfig.copy(onboardingCompleted = true).also {
                    configRepository.saveConfig(it)
                }
            } else {
                loadedConfig
            }

            _uiState.update {
                it.copy(
                    isLoaded = true,
                    config = effectiveConfig,
                    musicDisplayMode = effectiveConfig.musicDefaultMode,
                    packageOwnerLabels = RoomConfigRepository.buildPackageOwnerLabelMap(effectiveConfig),
                    defaultUnsortedLabel = RoomConfigRepository.defaultUnsortedLabel(effectiveConfig),
                    frameImageBitmap = frameBmp,
                    posterLeftBitmap = posterLeftBmp,
                    posterRightBitmap = posterRightBmp,
                    isBatteryOptimizationIgnored = isBatteryIgnored,
                    isDefaultLauncher = isDefault,
                    hasNotificationAccess = hasNotifAccess,
                    hasAudioPermission = hasAudioPerm
                )
            }

            if (effectiveConfig.weatherEnabled) {
                refreshWeather(forceRefresh = false)
            }
            if (effectiveConfig.autoCheckUpdatesEnabled) {
                checkForUpdates(userInitiated = false)
            }

            appRepository.installedApps.collect { apps ->
                val current = _uiState.value.config
                val populated = if (!current.autoPopulated && apps.isNotEmpty()) {
                    RoomConfigRepository.autoPopulateIfNeeded(current, apps).also { updated ->
                        configRepository.saveConfig(updated)
                    }
                } else {
                    current
                }
                _uiState.update {
                    it.copy(
                        installedApps = apps,
                        config = populated,
                        packageOwnerLabels = RoomConfigRepository.buildPackageOwnerLabelMap(populated),
                        defaultUnsortedLabel = RoomConfigRepository.defaultUnsortedLabel(populated)
                    )
                }
            }
        }
        appRepository.startListening()
    }

    override fun onCleared() {
        lyricsJob?.cancel()
        updateCheckJob?.cancel()
        updateDownloadJob?.cancel()
        audioVisualizerEngine.stop()
        musicMediaMonitor.stop()
        appRepository.stopListening()
        batteryMonitor.stop()
        super.onCleared()
    }

    fun setAppForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (inForeground) {
            musicMediaMonitor.startOrRefresh()
        }
        syncAudioVisualizerState()
    }

    /**
     * Refreshes permission & launcher status on ON_RESUME without triggering laggy full PackageManager reloads.
     * Also prevents the onboarding reset bug when Android fires HOME intent right after ROLE_HOME is granted.
     */
    fun refreshSystemStatus() {
        refreshDynamicThemeHour()
        val isBatteryIgnored = SystemIntentHelper.isIgnoringBatteryOptimizations(appContext)
        val isDefault = SystemIntentHelper.isDefaultHomeLauncher(appContext)
        val hasNotifAccess = musicMediaMonitor.isNotificationAccessGranted()
        val hasAudioPerm = audioVisualizerEngine.hasAudioPermission()
        val state = _uiState.value

        val shouldAutoCompleteOnboarding =
            state.isLoaded &&
                !state.config.onboardingCompleted &&
                state.config.onboardingStep >= 3 &&
                isDefault

        if (shouldAutoCompleteOnboarding) {
            updateConfig { it.copy(onboardingCompleted = true) }
        }

        _uiState.update {
            it.copy(
                isBatteryOptimizationIgnored = isBatteryIgnored,
                isDefaultLauncher = isDefault,
                hasNotificationAccess = hasNotifAccess,
                hasAudioPermission = hasAudioPerm
            )
        }

        musicMediaMonitor.startOrRefresh()
        syncAudioVisualizerState()

        // If the user just returned from granting "Install unknown apps", finish launching the APK installer!
        val upd = _uiState.value.updateState
        if (upd.needsInstallPermission && AppUpdateInstaller.canInstallPackages(appContext)) {
            _uiState.update {
                it.copy(updateState = it.updateState.copy(needsInstallPermission = false))
            }
            val apkPath = upd.downloadedApkPath
            if (!apkPath.isNullOrBlank()) {
                val apkFile = File(apkPath)
                if (apkFile.exists()) {
                    AppUpdateInstaller.launchApkInstaller(appContext, apkFile)
                }
            }
        }

        if (state.installedApps.isEmpty()) {
            viewModelScope.launch { appRepository.refreshInstalledApps() }
        }
        if (state.config.weatherEnabled) {
            refreshWeather(forceRefresh = false)
        }
    }

    fun setOnboardingStep(step: Int) {
        val clamped = step.coerceIn(0, 3)
        if (_uiState.value.config.onboardingStep != clamped) {
            updateConfig { it.copy(onboardingStep = clamped) }
        }
    }

    fun completeOnboarding() {
        updateConfig { it.copy(onboardingCompleted = true, onboardingStep = 3) }
    }

    fun reopenOnboarding() {
        _uiState.update {
            it.copy(
                isEditMode = false,
                activeHotspotId = null,
                editingHotspotId = null,
                isMusicExpanded = false,
                showAllAppsSheet = false,
                showSettingsSheet = false
            )
        }
        syncAudioVisualizerState()
        updateConfig { it.copy(onboardingCompleted = false, onboardingStep = 0) }
    }

    fun enterEditMode() {
        _uiState.update {
            it.copy(
                isEditMode = true,
                activeHotspotId = null,
                isMusicExpanded = false,
                showAllAppsSheet = false,
                showSettingsSheet = false
            )
        }
        syncAudioVisualizerState()
    }

    fun exitEditMode() {
        _uiState.update {
            it.copy(
                isEditMode = false,
                editingHotspotId = null,
                showAddHotspotDialog = false
            )
        }
        if (_uiState.value.config.mirrorHomeWallpaper) {
            syncHomeWallpaperNow(showToast = false)
        }
    }

    fun openHotspotPanel(hotspotId: String) {
        _uiState.update {
            it.copy(
                activeHotspotId = hotspotId,
                isMusicExpanded = false,
                showAllAppsSheet = false,
                showSettingsSheet = false
            )
        }
        syncAudioVisualizerState()
    }

    fun closeHotspotPanel() {
        _uiState.update { it.copy(activeHotspotId = null) }
    }

    fun openEditHotspot(hotspotId: String) {
        _uiState.update { it.copy(editingHotspotId = hotspotId) }
    }

    fun closeEditHotspot() {
        _uiState.update { it.copy(editingHotspotId = null) }
    }

    fun openAddHotspotDialog() {
        _uiState.update { it.copy(showAddHotspotDialog = true) }
    }

    fun closeAddHotspotDialog() {
        _uiState.update { it.copy(showAddHotspotDialog = false) }
    }

    fun openAllApps() {
        _uiState.update {
            it.copy(
                showAllAppsSheet = true,
                activeHotspotId = null,
                isMusicExpanded = false,
                showSettingsSheet = false
            )
        }
        syncAudioVisualizerState()
    }

    fun closeAllApps() {
        _uiState.update { it.copy(showAllAppsSheet = false) }
    }

    fun openSettings() {
        val current = _uiState.value
        val knownUpdate = current.updateState.availableUpdate
        val shouldPopupImmediately = knownUpdate != null &&
            dismissedUpdateVersion != knownUpdate.versionName

        _uiState.update {
            it.copy(
                showSettingsSheet = true,
                showAllAppsSheet = false,
                activeHotspotId = null,
                isMusicExpanded = false,
                isEditMode = false,
                updateState = if (shouldPopupImmediately) {
                    it.updateState.copy(showUpdateDialog = true)
                } else {
                    it.updateState
                }
            )
        }
        syncAudioVisualizerState()

        val stale = (System.currentTimeMillis() - current.updateState.lastCheckedAtMs) > AUTO_UPDATE_STALE_MS
        if (current.config.autoCheckUpdatesEnabled && stale && !current.updateState.isChecking) {
            checkForUpdates(userInitiated = false)
        }
    }

    fun closeSettings() {
        _uiState.update {
            it.copy(
                showSettingsSheet = false,
                updateState = it.updateState.copy(showUpdateDialog = false)
            )
        }
    }

    fun handleBackPressed(): Boolean {
        val state = _uiState.value
        return when {
            state.showAddHotspotDialog -> {
                closeAddHotspotDialog()
                true
            }
            state.editingHotspotId != null -> {
                closeEditHotspot()
                true
            }
            state.showSettingsSheet -> {
                closeSettings()
                true
            }
            state.isMusicExpanded -> {
                collapseMusicVisualizer()
                true
            }
            state.activeHotspotId != null -> {
                closeHotspotPanel()
                true
            }
            state.showAllAppsSheet -> {
                closeAllApps()
                true
            }
            state.isEditMode -> {
                exitEditMode()
                true
            }
            else -> false
        }
    }

    // ---- Music Visualizer & Open-Source Synced Lyrics (LRCLIB) ----

    fun expandMusicVisualizer(targetMode: MusicDisplayMode? = null) {
        val mode = targetMode ?: _uiState.value.musicDisplayMode
        _uiState.update {
            it.copy(
                isMusicExpanded = true,
                musicDisplayMode = mode,
                activeHotspotId = null,
                showAllAppsSheet = false,
                showSettingsSheet = false
            )
        }
        if (mode == MusicDisplayMode.LYRICS) {
            fetchLyricsForCurrentTrack(forceRefresh = false)
        }
        syncAudioVisualizerState()
    }

    fun collapseMusicVisualizer() {
        _uiState.update { it.copy(isMusicExpanded = false) }
        syncAudioVisualizerState()
    }

    fun setMusicDisplayMode(mode: MusicDisplayMode) {
        _uiState.update { it.copy(musicDisplayMode = mode) }
        if (mode == MusicDisplayMode.LYRICS) {
            fetchLyricsForCurrentTrack(forceRefresh = false)
        }
        syncAudioVisualizerState()
    }

    fun setMusicVisualizerEnabled(enabled: Boolean) {
        updateConfig { it.copy(musicVisualizerEnabled = enabled) }
        if (!enabled) {
            musicMediaMonitor.stopDemoPreview()
            _uiState.update { it.copy(isMusicExpanded = false) }
        }
        syncAudioVisualizerState()
    }

    fun setMusicVisualizerStyle(style: VisualizerStyle) {
        updateConfig { it.copy(musicVisualizerStyle = style) }
    }

    fun setMusicDefaultMode(mode: MusicDisplayMode) {
        updateConfig { it.copy(musicDefaultMode = mode) }
        _uiState.update { it.copy(musicDisplayMode = mode) }
    }

    fun setAudioReactiveVisualizerEnabled(enabled: Boolean) {
        val hasPerm = audioVisualizerEngine.hasAudioPermission()
        _uiState.update { it.copy(hasAudioPermission = hasPerm) }
        updateConfig { it.copy(audioReactiveVisualizerEnabled = enabled && hasPerm) }
        syncAudioVisualizerState()
    }

    fun openNotificationAccessSettings() {
        RoomNotificationListenerService.openNotificationListenerSettings(appContext)
    }

    fun previewMusicVisualizer() {
        musicMediaMonitor.startDemoPreview()
        val startMode = _uiState.value.config.musicDefaultMode
        _uiState.update {
            it.copy(
                showSettingsSheet = false,
                showAllAppsSheet = false,
                activeHotspotId = null,
                isEditMode = false,
                musicDisplayMode = startMode,
                isMusicExpanded = true,
                lyricsState = LyricsRepository.demoSyncedLyrics()
            )
        }
        syncAudioVisualizerState()
    }

    fun stopMusicDemoPreview() {
        musicMediaMonitor.stopDemoPreview()
        _uiState.update { it.copy(isMusicExpanded = false) }
        syncAudioVisualizerState()
    }

    fun toggleMusicPlayPause() = musicMediaMonitor.togglePlayPause()

    fun skipMusicNext() = musicMediaMonitor.skipNext()

    fun skipMusicPrevious() = musicMediaMonitor.skipPrevious()

    fun seekMusicTo(positionMs: Long) = musicMediaMonitor.seekTo(positionMs)

    fun openActiveMusicApp() = musicMediaMonitor.openActivePlayer()

    fun retryFetchLyrics() {
        fetchLyricsForCurrentTrack(forceRefresh = true)
    }

    private fun fetchLyricsForCurrentTrack(forceRefresh: Boolean) {
        val track = _uiState.value.nowPlaying
        if (!track.hasSession || track.title.isBlank()) {
            _uiState.update { it.copy(lyricsState = LyricsState.Idle) }
            return
        }
        if (track.isDemo) {
            _uiState.update { it.copy(lyricsState = LyricsRepository.demoSyncedLyrics()) }
            return
        }
        val currentLyrics = _uiState.value.lyricsState
        if (!forceRefresh &&
            track.trackKey == lastLyricsTrackKey &&
            (currentLyrics is LyricsState.Synced ||
                currentLyrics is LyricsState.Plain ||
                currentLyrics is LyricsState.Instrumental ||
                currentLyrics is LyricsState.Loading)
        ) {
            return
        }

        lastLyricsTrackKey = track.trackKey
        lyricsJob?.cancel()
        _uiState.update { it.copy(lyricsState = LyricsState.Loading) }
        lyricsJob = viewModelScope.launch {
            val fetched = lyricsRepository.fetchLyrics(
                trackKey = track.trackKey,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                forceRefresh = forceRefresh
            )
            // Only apply if track hasn't changed while fetching
            if (_uiState.value.nowPlaying.trackKey == track.trackKey) {
                _uiState.update { it.copy(lyricsState = fetched) }
            }
        }
    }

    private fun syncAudioVisualizerState() {
        val state = _uiState.value
        val shouldCapture = isAppInForeground &&
            state.config.musicVisualizerEnabled &&
            state.config.audioReactiveVisualizerEnabled &&
            state.nowPlaying.hasSession &&
            state.nowPlaying.isPlaying &&
            !state.nowPlaying.isDemo &&
            state.isMusicExpanded &&
            state.musicDisplayMode == MusicDisplayMode.VISUALIZER
        audioVisualizerEngine.setActive(shouldCapture)
    }

    // ---- Auto-Updates from GitHub (Cooreo/LauncherAPP) ----

    fun setAutoCheckUpdatesEnabled(enabled: Boolean) {
        updateConfig { it.copy(autoCheckUpdatesEnabled = enabled) }
        if (enabled) {
            checkForUpdates(userInitiated = false)
        }
    }

    fun checkForUpdates(userInitiated: Boolean) {
        if (_uiState.value.updateState.isChecking || _uiState.value.updateState.isDownloading) return
        if (userInitiated) {
            dismissedUpdateVersion = null
        }

        val currentVersion = _uiState.value.updateState.currentVersion.ifBlank { BuildConfig.VERSION_NAME }
        _uiState.update {
            it.copy(
                updateState = it.updateState.copy(
                    isChecking = true,
                    errorMessage = null
                )
            )
        }

        updateCheckJob?.cancel()
        updateCheckJob = viewModelScope.launch {
            val result = appUpdateRepository.checkForUpdate(currentVersion)
            val now = System.currentTimeMillis()
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    val info = result.info
                    val shouldShowPopup = userInitiated ||
                        (_uiState.value.showSettingsSheet && dismissedUpdateVersion != info.versionName)
                    _uiState.update { state ->
                        val sameVersion = state.updateState.availableUpdate?.versionName == info.versionName
                        state.copy(
                            updateState = state.updateState.copy(
                                isChecking = false,
                                availableUpdate = info,
                                isUpToDate = false,
                                lastCheckedAtMs = now,
                                errorMessage = null,
                                downloadedApkPath = if (sameVersion) state.updateState.downloadedApkPath else null,
                                showUpdateDialog = shouldShowPopup || state.updateState.showUpdateDialog
                            )
                        )
                    }
                }

                is UpdateCheckResult.UpToDate -> {
                    _uiState.update { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                isChecking = false,
                                availableUpdate = null,
                                isUpToDate = true,
                                lastCheckedAtMs = now,
                                errorMessage = null,
                                showUpdateDialog = false
                            )
                        )
                    }
                    if (userInitiated) {
                        Toast.makeText(
                            appContext,
                            "Room Launcher v$currentVersion is up to date",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                is UpdateCheckResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                isChecking = false,
                                errorMessage = if (userInitiated) result.message else state.updateState.errorMessage
                            )
                        )
                    }
                    if (userInitiated) {
                        Toast.makeText(
                            appContext,
                            "Update check failed: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun showUpdatePopup() {
        if (_uiState.value.updateState.availableUpdate != null) {
            _uiState.update {
                it.copy(updateState = it.updateState.copy(showUpdateDialog = true))
            }
        }
    }

    fun dismissUpdatePopup() {
        val version = _uiState.value.updateState.availableUpdate?.versionName
        if (version != null) {
            dismissedUpdateVersion = version
        }
        _uiState.update {
            it.copy(updateState = it.updateState.copy(showUpdateDialog = false))
        }
    }

    fun downloadAndInstallUpdate() {
        val state = _uiState.value.updateState
        val info = state.availableUpdate ?: return
        if (state.isDownloading) return

        // If already downloaded, go straight to install (or permission prompt)
        val existingPath = state.downloadedApkPath
        if (!existingPath.isNullOrBlank()) {
            val file = File(existingPath)
            if (file.exists()) {
                triggerInstallOrRequestPermission(file)
                return
            }
        }

        _uiState.update {
            it.copy(
                updateState = it.updateState.copy(
                    isDownloading = true,
                    downloadProgress = 0f,
                    errorMessage = null,
                    needsInstallPermission = false
                )
            )
        }

        updateDownloadJob?.cancel()
        updateDownloadJob = viewModelScope.launch {
            runCatching {
                AppUpdateInstaller.downloadApk(
                    context = appContext,
                    downloadUrl = info.downloadUrl,
                    versionName = info.versionName,
                    onProgress = { progress ->
                        _uiState.update { s ->
                            s.copy(updateState = s.updateState.copy(downloadProgress = progress))
                        }
                    }
                )
            }.onSuccess { apkFile ->
                _uiState.update { s ->
                    s.copy(
                        updateState = s.updateState.copy(
                            isDownloading = false,
                            downloadProgress = 1f,
                            downloadedApkPath = apkFile.absolutePath
                        )
                    )
                }
                triggerInstallOrRequestPermission(apkFile)
            }.onFailure { err ->
                val msg = err.message?.take(100) ?: "Download failed"
                _uiState.update { s ->
                    s.copy(
                        updateState = s.updateState.copy(
                            isDownloading = false,
                            downloadProgress = 0f,
                            errorMessage = msg
                        )
                    )
                }
                Toast.makeText(appContext, "Update download failed: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun triggerInstallOrRequestPermission(apkFile: File) {
        if (AppUpdateInstaller.canInstallPackages(appContext)) {
            _uiState.update {
                it.copy(updateState = it.updateState.copy(needsInstallPermission = false))
            }
            AppUpdateInstaller.launchApkInstaller(appContext, apkFile)
        } else {
            _uiState.update {
                it.copy(updateState = it.updateState.copy(needsInstallPermission = true))
            }
            AppUpdateInstaller.openInstallUnknownAppsSettings(appContext)
        }
    }

    fun openUpdateReleasePage() {
        val url = _uiState.value.updateState.availableUpdate?.releasePageUrl
            ?: AppUpdateRepository.RELEASES_WEB_URL
        SystemIntentHelper.openWebUrl(appContext, url)
    }

    // ---- Theme, Room Style, Animation, Weather, Wallpaper, Photos Settings ----

    fun setRoomStyle(styleId: RoomStyleId) {
        updateConfig { it.copy(roomStyleId = styleId) }
        if (_uiState.value.config.mirrorHomeWallpaper) {
            syncHomeWallpaperNow(showToast = false)
        }
    }

    fun setTheme(themeId: RoomThemeId) {
        updateConfig { it.copy(themeId = themeId, dynamicThemeEnabled = false) }
        if (_uiState.value.config.mirrorHomeWallpaper) {
            syncHomeWallpaperNow(showToast = false)
        }
    }

    fun setDynamicThemeEnabled(enabled: Boolean) {
        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _uiState.update { it.copy(currentHourOfDay = nowHour) }
        updateConfig { it.copy(dynamicThemeEnabled = enabled) }
        if (_uiState.value.config.mirrorHomeWallpaper) {
            syncHomeWallpaperNow(showToast = false)
        }
    }

    private fun refreshDynamicThemeHour() {
        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val before = _uiState.value
        if (before.currentHourOfDay != nowHour) {
            val prevEffective = before.effectiveThemeId
            _uiState.update { it.copy(currentHourOfDay = nowHour) }
            val nextEffective = _uiState.value.effectiveThemeId
            if (before.config.dynamicThemeEnabled &&
                prevEffective != nextEffective &&
                before.config.mirrorHomeWallpaper
            ) {
                syncHomeWallpaperNow(showToast = false)
            }
        }
    }

    fun setTeenPoster(isLeft: Boolean, uri: Uri) {
        viewModelScope.launch {
            val fileName = if (isLeft) "poster_left.jpg" else "poster_right.jpg"
            val savedPath = SystemIntentHelper.saveUserImage(appContext, uri, fileName)
            if (savedPath != null) {
                val bmp = loadFrameBitmap(savedPath)
                _uiState.update { state ->
                    if (isLeft) state.copy(posterLeftBitmap = bmp) else state.copy(posterRightBitmap = bmp)
                }
                updateConfig { cfg ->
                    if (isLeft) cfg.copy(posterLeftPath = savedPath) else cfg.copy(posterRightPath = savedPath)
                }
                if (_uiState.value.config.mirrorHomeWallpaper) {
                    syncHomeWallpaperNow(showToast = false)
                }
                Toast.makeText(appContext, "Poster updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearTeenPoster(isLeft: Boolean) {
        val path = if (isLeft) _uiState.value.config.posterLeftPath else _uiState.value.config.posterRightPath
        path?.let { runCatching { File(it).delete() } }
        _uiState.update { state ->
            if (isLeft) state.copy(posterLeftBitmap = null) else state.copy(posterRightBitmap = null)
        }
        updateConfig { cfg ->
            if (isLeft) cfg.copy(posterLeftPath = null) else cfg.copy(posterRightPath = null)
        }
        if (_uiState.value.config.mirrorHomeWallpaper) {
            syncHomeWallpaperNow(showToast = false)
        }
    }

    fun setUseQuickAnimations(enabled: Boolean) {
        updateConfig { it.copy(useQuickAnimations = enabled) }
    }

    fun setChargingAnimationEnabled(enabled: Boolean) {
        updateConfig { it.copy(chargingAnimationEnabled = enabled) }
    }

    fun setChargingAnimationStyle(style: ChargingAnimationStyle) {
        updateConfig { it.copy(chargingAnimationStyle = style) }
    }

    fun triggerChargingAnimationPreview() {
        _uiState.update {
            it.copy(
                showSettingsSheet = false,
                showAllAppsSheet = false,
                activeChargingSplashKey = System.nanoTime()
            )
        }
    }

    fun dismissChargingAnimation() {
        _uiState.update { it.copy(activeChargingSplashKey = 0L) }
    }

    fun setWeatherEnabled(enabled: Boolean) {
        updateConfig { it.copy(weatherEnabled = enabled) }
        if (enabled) {
            refreshWeather(forceRefresh = true)
        }
    }

    fun refreshWeather(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            val state = weatherRepository.fetchWeather(forceRefresh = forceRefresh)
            _uiState.update { it.copy(weatherState = state) }
        }
    }

    fun setMirrorHomeWallpaper(enabled: Boolean) {
        updateConfig { it.copy(mirrorHomeWallpaper = enabled) }
        if (enabled) {
            syncHomeWallpaperNow(showToast = true)
        }
    }

    fun syncHomeWallpaperNow(showToast: Boolean = true) {
        if (_uiState.value.isSyncingWallpaper) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingWallpaper = true) }
            val state = _uiState.value
            val ok = RoomWallpaperRenderer.applyHomeScreenWallpaper(
                context = appContext,
                config = state.config.copy(themeId = state.effectiveThemeId),
                weather = state.weatherState
            )
            _uiState.update { it.copy(isSyncingWallpaper = false) }
            if (showToast) {
                Toast.makeText(
                    appContext,
                    if (ok) "Home screen wallpaper synced to your room" else "Could not update home wallpaper",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun setFramePhoto(uri: Uri) {
        viewModelScope.launch {
            val savedPath = SystemIntentHelper.saveFramePhoto(appContext, uri)
            if (savedPath != null) {
                val bmp = loadFrameBitmap(savedPath)
                _uiState.update { it.copy(frameImageBitmap = bmp) }
                updateConfig { it.copy(framePhotoPath = savedPath) }
                if (_uiState.value.config.mirrorHomeWallpaper) {
                    syncHomeWallpaperNow(showToast = false)
                }
                Toast.makeText(appContext, "Picture frame updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, "Could not load selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearFramePhoto() {
        _uiState.value.config.framePhotoPath?.let { path ->
            runCatching { File(path).delete() }
        }
        _uiState.update { it.copy(frameImageBitmap = null) }
        updateConfig { it.copy(framePhotoPath = null) }
        if (_uiState.value.config.mirrorHomeWallpaper) {
            syncHomeWallpaperNow(showToast = false)
        }
    }

    // ---- Sticky Note Web Shortcuts ----

    fun addWebShortcut(title: String, url: String) {
        val cleanTitle = title.trim()
        val cleanUrl = url.trim()
        if (cleanTitle.isEmpty() || cleanUrl.isEmpty()) return
        val shortcut = WebShortcut(
            id = "web_${UUID.randomUUID().toString().take(8)}",
            title = cleanTitle,
            url = cleanUrl
        )
        updateConfig { it.copy(webShortcuts = it.webShortcuts + shortcut) }
    }

    fun deleteWebShortcut(id: String) {
        updateConfig { config ->
            config.copy(webShortcuts = config.webShortcuts.filterNot { it.id == id })
        }
    }

    // ---- Room Hotspot Layout & App Assignment ----

    fun moveHotspot(hotspotId: String, newX: Float, newY: Float) {
        updateConfig { config ->
            config.copy(
                hotspots = config.hotspots.map { hotspot ->
                    if (hotspot.id == hotspotId) {
                        val clampedX = newX.coerceIn(0f, (1f - hotspot.width).coerceAtLeast(0f))
                        val clampedY = newY.coerceIn(0f, (1f - hotspot.height).coerceAtLeast(0f))
                        hotspot.copy(x = clampedX, y = clampedY)
                    } else {
                        hotspot
                    }
                }
            )
        }
    }

    fun resizeHotspot(hotspotId: String, newWidth: Float, newHeight: Float) {
        updateConfig { config ->
            config.copy(
                hotspots = config.hotspots.map { hotspot ->
                    if (hotspot.id == hotspotId) {
                        val maxW = (1f - hotspot.x).coerceAtLeast(MIN_HOTSPOT_WIDTH)
                        val maxH = (1f - hotspot.y).coerceAtLeast(MIN_HOTSPOT_HEIGHT)
                        val clampedW = newWidth.coerceIn(MIN_HOTSPOT_WIDTH, maxW)
                        val clampedH = newHeight.coerceIn(MIN_HOTSPOT_HEIGHT, maxH)
                        hotspot.copy(width = clampedW, height = clampedH)
                    } else {
                        hotspot
                    }
                }
            )
        }
    }

    fun updateHotspotDetails(
        hotspotId: String,
        newLabel: String,
        newSubtitle: String,
        newActionType: HotspotActionType
    ) {
        val trimmedLabel = newLabel.trim().ifEmpty { return }
        val trimmedSubtitle = newSubtitle.trim()
        updateConfig { config ->
            val unsortedCount = config.hotspots.count { it.actionType == HotspotActionType.UNSORTED_PANEL }
            config.copy(
                hotspots = config.hotspots.map { hotspot ->
                    if (hotspot.id == hotspotId) {
                        val safeType = if (
                            hotspot.actionType == HotspotActionType.UNSORTED_PANEL &&
                            newActionType != HotspotActionType.UNSORTED_PANEL &&
                            unsortedCount <= 1
                        ) {
                            HotspotActionType.UNSORTED_PANEL
                        } else {
                            newActionType
                        }
                        hotspot.copy(
                            label = trimmedLabel,
                            subtitle = trimmedSubtitle,
                            actionType = safeType,
                            packages = if (safeType == HotspotActionType.PANEL) hotspot.packages else emptyList()
                        )
                    } else {
                        hotspot
                    }
                }
            )
        }
    }

    fun addHotspot(
        label: String,
        subtitle: String,
        actionType: HotspotActionType,
        visualType: RoomObjectVisual = RoomObjectVisual.CUSTOM
    ) {
        val trimmedLabel = label.trim().ifEmpty { return }
        val newHotspot = RoomHotspot(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            label = trimmedLabel,
            subtitle = subtitle.trim(),
            x = 0.28f,
            y = 0.42f,
            width = 0.34f,
            height = 0.14f,
            actionType = actionType,
            visualType = visualType
        )
        updateConfig { config ->
            config.copy(hotspots = config.hotspots + newHotspot)
        }
        closeAddHotspotDialog()
    }

    fun deleteHotspot(hotspotId: String): Boolean {
        val current = _uiState.value.config
        val target = current.hotspots.firstOrNull { it.id == hotspotId } ?: return false
        val unsortedCount = current.hotspots.count { it.actionType == HotspotActionType.UNSORTED_PANEL }
        if (target.actionType == HotspotActionType.UNSORTED_PANEL && unsortedCount <= 1) {
            return false
        }
        updateConfig { config ->
            config.copy(hotspots = config.hotspots.filterNot { it.id == hotspotId })
        }
        closeEditHotspot()
        return true
    }

    fun moveAppToHotspot(packageName: String, targetHotspotId: String) {
        updateConfig { config ->
            val target = config.hotspots.firstOrNull { it.id == targetHotspotId } ?: return@updateConfig config
            if (target.actionType != HotspotActionType.PANEL && target.actionType != HotspotActionType.UNSORTED_PANEL) {
                return@updateConfig config
            }
            config.copy(
                hotspots = config.hotspots.map { hotspot ->
                    when {
                        hotspot.id == targetHotspotId && hotspot.actionType == HotspotActionType.PANEL -> {
                            val updated = (hotspot.packages.filterNot { it == packageName }) + packageName
                            hotspot.copy(packages = updated)
                        }
                        hotspot.actionType == HotspotActionType.PANEL -> {
                            hotspot.copy(packages = hotspot.packages.filterNot { it == packageName })
                        }
                        else -> hotspot
                    }
                }
            )
        }
    }

    fun removeAppFromHotspot(packageName: String, hotspotId: String) {
        updateConfig { config ->
            config.copy(
                hotspots = config.hotspots.map { hotspot ->
                    if (hotspot.id == hotspotId && hotspot.actionType == HotspotActionType.PANEL) {
                        hotspot.copy(packages = hotspot.packages.filterNot { it == packageName })
                    } else {
                        hotspot
                    }
                }
            )
        }
    }

    fun resetRoomLayout() {
        val current = _uiState.value.config
        val installed = _uiState.value.installedApps
        val base = RoomConfigRepository.createDefaultRoomConfig().copy(
            onboardingCompleted = true,
            onboardingStep = 3,
            roomStyleId = current.roomStyleId,
            themeId = current.themeId,
            dynamicThemeEnabled = current.dynamicThemeEnabled,
            useQuickAnimations = current.useQuickAnimations,
            chargingAnimationEnabled = current.chargingAnimationEnabled,
            chargingAnimationStyle = current.chargingAnimationStyle,
            musicVisualizerEnabled = current.musicVisualizerEnabled,
            musicVisualizerStyle = current.musicVisualizerStyle,
            musicDefaultMode = current.musicDefaultMode,
            audioReactiveVisualizerEnabled = current.audioReactiveVisualizerEnabled,
            autoCheckUpdatesEnabled = current.autoCheckUpdatesEnabled,
            weatherEnabled = current.weatherEnabled,
            mirrorHomeWallpaper = current.mirrorHomeWallpaper,
            framePhotoPath = current.framePhotoPath,
            posterLeftPath = current.posterLeftPath,
            posterRightPath = current.posterRightPath,
            webShortcuts = current.webShortcuts
        )
        val populated = RoomConfigRepository.autoPopulateIfNeeded(base, installed)
        _uiState.update {
            it.copy(
                config = populated,
                packageOwnerLabels = RoomConfigRepository.buildPackageOwnerLabelMap(populated),
                defaultUnsortedLabel = RoomConfigRepository.defaultUnsortedLabel(populated),
                editingHotspotId = null,
                activeHotspotId = null
            )
        }
        viewModelScope.launch {
            configRepository.saveConfig(populated)
            if (populated.mirrorHomeWallpaper) {
                syncHomeWallpaperNow(showToast = false)
            }
        }
    }

    fun exportRoomBackup(targetUri: Uri) {
        viewModelScope.launch {
            val serialized = configRepository.encodeConfig(_uiState.value.config)
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    appContext.contentResolver.openOutputStream(targetUri)?.use { os ->
                        os.write(serialized.toByteArray(Charsets.UTF_8))
                        os.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
            }
            Toast.makeText(
                appContext,
                if (saved) "Room backup exported!" else "Could not export backup",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun importRoomBackup(sourceUri: Uri) {
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = appContext.contentResolver.openInputStream(sourceUri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: return@runCatching null
                    configRepository.decodeConfig(raw)
                }.getOrNull()
            }
            if (imported == null) {
                Toast.makeText(appContext, "Invalid Room Launcher backup file", Toast.LENGTH_LONG).show()
                return@launch
            }
            val current = _uiState.value.config
            val sanitized = imported.copy(
                onboardingCompleted = true,
                onboardingStep = 3,
                autoPopulated = true,
                framePhotoPath = imported.framePhotoPath?.takeIf { File(it).exists() } ?: current.framePhotoPath,
                posterLeftPath = imported.posterLeftPath?.takeIf { File(it).exists() } ?: current.posterLeftPath,
                posterRightPath = imported.posterRightPath?.takeIf { File(it).exists() } ?: current.posterRightPath
            )
            configRepository.saveConfig(sanitized)
            _uiState.update { state ->
                state.copy(
                    config = sanitized,
                    musicDisplayMode = sanitized.musicDefaultMode,
                    packageOwnerLabels = RoomConfigRepository.buildPackageOwnerLabelMap(sanitized),
                    defaultUnsortedLabel = RoomConfigRepository.defaultUnsortedLabel(sanitized),
                    editingHotspotId = null,
                    activeHotspotId = null
                )
            }
            if (sanitized.mirrorHomeWallpaper) {
                syncHomeWallpaperNow(showToast = false)
            }
            Toast.makeText(appContext, "Room backup restored!", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun loadFrameBitmap(path: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        val f = File(path)
        if (!f.exists()) return@withContext null
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }

    private fun updateConfig(transform: (RoomConfig) -> RoomConfig) {
        var updatedConfig: RoomConfig? = null
        _uiState.update { state ->
            if (!state.isLoaded) return@update state
            val next = transform(state.config)
            updatedConfig = next
            state.copy(
                config = next,
                packageOwnerLabels = RoomConfigRepository.buildPackageOwnerLabelMap(next),
                defaultUnsortedLabel = RoomConfigRepository.defaultUnsortedLabel(next)
            )
        }
        updatedConfig?.let { toSave ->
            viewModelScope.launch {
                configRepository.saveConfig(toSave)
            }
        }
    }

    companion object {
        const val MIN_HOTSPOT_WIDTH = 0.14f
        const val MIN_HOTSPOT_HEIGHT = 0.08f
        private const val AUTO_UPDATE_STALE_MS = 30 * 60 * 1000L
    }
}
