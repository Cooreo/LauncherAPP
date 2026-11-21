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
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.AppRepository
import com.roomlauncher.app.data.BatteryMonitor
import com.roomlauncher.app.data.BatteryStatus
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomConfigRepository
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.WeatherRepository
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.data.WebShortcut
import com.roomlauncher.app.system.RoomWallpaperRenderer
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    private var lastObservedPlugInId = 0L

    init {
        batteryMonitor.start()
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
                    packageOwnerLabels = RoomConfigRepository.buildPackageOwnerLabelMap(effectiveConfig),
                    defaultUnsortedLabel = RoomConfigRepository.defaultUnsortedLabel(effectiveConfig),
                    frameImageBitmap = frameBmp,
                    posterLeftBitmap = posterLeftBmp,
                    posterRightBitmap = posterRightBmp,
                    isBatteryOptimizationIgnored = isBatteryIgnored,
                    isDefaultLauncher = isDefault
                )
            }

            if (effectiveConfig.weatherEnabled) {
                refreshWeather(forceRefresh = false)
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
        appRepository.stopListening()
        super.onCleared()
    }

    /**
     * Refreshes permission & launcher status on ON_RESUME without triggering laggy full PackageManager reloads.
     * Also prevents the onboarding reset bug when Android fires HOME intent right after ROLE_HOME is granted.
     */
    fun refreshSystemStatus() {
        val isBatteryIgnored = SystemIntentHelper.isIgnoringBatteryOptimizations(appContext)
        val isDefault = SystemIntentHelper.isDefaultHomeLauncher(appContext)
        val state = _uiState.value

        val shouldAutoCompleteOnboarding =
            !state.config.onboardingCompleted &&
                state.config.onboardingStep >= 3 &&
                isDefault

        if (shouldAutoCompleteOnboarding) {
            updateConfig { it.copy(onboardingCompleted = true) }
        }

        _uiState.update {
            it.copy(
                isBatteryOptimizationIgnored = isBatteryIgnored,
                isDefaultLauncher = isDefault
            )
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
                showAllAppsSheet = false,
                showSettingsSheet = false
            )
        }
        updateConfig { it.copy(onboardingCompleted = false, onboardingStep = 0) }
    }

    fun enterEditMode() {
        _uiState.update {
            it.copy(
                isEditMode = true,
                activeHotspotId = null,
                showAllAppsSheet = false,
                showSettingsSheet = false
            )
        }
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
                showAllAppsSheet = false,
                showSettingsSheet = false
            )
        }
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
                showSettingsSheet = false
            )
        }
    }

    fun closeAllApps() {
        _uiState.update { it.copy(showAllAppsSheet = false) }
    }

    fun openSettings() {
        _uiState.update {
            it.copy(
                showSettingsSheet = true,
                showAllAppsSheet = false,
                activeHotspotId = null,
                isEditMode = false
            )
        }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettingsSheet = false) }
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

    // ---- Theme, Room Style, Animation, Weather, Wallpaper, Photos Settings ----

    fun setRoomStyle(styleId: RoomStyleId) {
        updateConfig { it.copy(roomStyleId = styleId) }
        if (_uiState.value.config.mirrorHomeWallpaper) {
            syncHomeWallpaperNow(showToast = false)
        }
    }

    fun setTheme(themeId: RoomThemeId) {
        updateConfig { it.copy(themeId = themeId) }
        if (_uiState.value.config.mirrorHomeWallpaper) {
            syncHomeWallpaperNow(showToast = false)
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
                config = state.config,
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
            useQuickAnimations = current.useQuickAnimations,
            chargingAnimationEnabled = current.chargingAnimationEnabled,
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

    private suspend fun loadFrameBitmap(path: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        val f = File(path)
        if (!f.exists()) return@withContext null
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }

    private fun updateConfig(transform: (RoomConfig) -> RoomConfig) {
        var updatedConfig: RoomConfig? = null
        _uiState.update { state ->
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

    override fun onCleared() {
        batteryMonitor.stop()
        super.onCleared()
    }

    companion object {
        const val MIN_HOTSPOT_WIDTH = 0.14f
        const val MIN_HOTSPOT_HEIGHT = 0.08f
    }
}
