package com.roomlauncher.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.AppRepository
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomConfigRepository
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.system.SystemIntentHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class RoomUiState(
    val isLoaded: Boolean = false,
    val config: RoomConfig = RoomConfigRepository.createDefaultRoomConfig(),
    val installedApps: List<AppInfo> = emptyList(),
    val isEditMode: Boolean = false,
    val activeHotspotId: String? = null,
    val editingHotspotId: String? = null,
    val showAddHotspotDialog: Boolean = false,
    val showAllAppsSheet: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val isDefaultLauncher: Boolean = false
) {
    val activeHotspot: RoomHotspot?
        get() = config.hotspots.firstOrNull { it.id == activeHotspotId }

    val editingHotspot: RoomHotspot?
        get() = config.hotspots.firstOrNull { it.id == editingHotspotId }

    fun appsFor(hotspot: RoomHotspot): List<AppInfo> =
        RoomConfigRepository.appsForHotspot(hotspot, config, installedApps)

    fun ownerFor(packageName: String): RoomHotspot? =
        RoomConfigRepository.ownerHotspotForPackage(packageName, config)

    val panelHotspots: List<RoomHotspot>
        get() = config.hotspots.filter {
            it.actionType == HotspotActionType.PANEL || it.actionType == HotspotActionType.UNSORTED_PANEL
        }
}

class RoomLauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val configRepository = RoomConfigRepository(appContext)
    private val appRepository = AppRepository(appContext, viewModelScope)

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loadedConfig = configRepository.loadConfig()
            _uiState.update {
                it.copy(
                    isLoaded = true,
                    config = loadedConfig,
                    isBatteryOptimizationIgnored = SystemIntentHelper.isIgnoringBatteryOptimizations(appContext),
                    isDefaultLauncher = SystemIntentHelper.isDefaultHomeLauncher(appContext)
                )
            }

            // Collect live installed apps and auto-populate once on first load
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
                        config = populated
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

    fun refreshSystemStatus() {
        _uiState.update {
            it.copy(
                isBatteryOptimizationIgnored = SystemIntentHelper.isIgnoringBatteryOptimizations(appContext),
                isDefaultLauncher = SystemIntentHelper.isDefaultHomeLauncher(appContext)
            )
        }
        viewModelScope.launch {
            appRepository.refreshInstalledApps()
        }
    }

    fun completeOnboarding() {
        updateConfig { it.copy(onboardingCompleted = true) }
    }

    fun reopenOnboarding() {
        _uiState.update {
            it.copy(
                isEditMode = false,
                activeHotspotId = null,
                editingHotspotId = null,
                showAllAppsSheet = false
            )
        }
        updateConfig { it.copy(onboardingCompleted = false) }
    }

    fun enterEditMode() {
        _uiState.update {
            it.copy(
                isEditMode = true,
                activeHotspotId = null,
                showAllAppsSheet = false
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
    }

    fun openHotspotPanel(hotspotId: String) {
        _uiState.update {
            it.copy(
                activeHotspotId = hotspotId,
                showAllAppsSheet = false
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
                activeHotspotId = null
            )
        }
    }

    fun closeAllApps() {
        _uiState.update { it.copy(showAllAppsSheet = false) }
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
                        // Ensure at least one UNSORTED_PANEL remains so nothing gets lost
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
        actionType: HotspotActionType
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
            actionType = actionType
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
        // Removing a panel hotspot automatically causes its packages to appear in Trash Can
        updateConfig { config ->
            config.copy(hotspots = config.hotspots.filterNot { it.id == hotspotId })
        }
        closeEditHotspot()
        return true
    }

    /**
     * Moves [packageName] into [targetHotspotId].
     * - Removes it from all other PANEL hotspots.
     * - If target is a PANEL, appends it to that panel.
     * - If target is UNSORTED_PANEL (Trash Can), leaving it unassigned moves it to Trash Can.
     */
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

    /**
     * Removes [packageName] from [hotspotId], automatically returning it to Trash Can (Unsorted).
     */
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
        val installed = _uiState.value.installedApps
        val base = RoomConfigRepository.createDefaultRoomConfig().copy(onboardingCompleted = true)
        val populated = RoomConfigRepository.autoPopulateIfNeeded(base, installed)
        _uiState.update {
            it.copy(
                config = populated,
                editingHotspotId = null,
                activeHotspotId = null
            )
        }
        viewModelScope.launch {
            configRepository.saveConfig(populated)
        }
    }

    private fun updateConfig(transform: (RoomConfig) -> RoomConfig) {
        var updatedConfig: RoomConfig? = null
        _uiState.update { state ->
            val next = transform(state.config)
            updatedConfig = next
            state.copy(config = next)
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
    }
}
