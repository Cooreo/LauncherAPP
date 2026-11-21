package com.roomlauncher.app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roomlauncher.app.ui.RoomLauncherViewModel
import com.roomlauncher.app.ui.onboarding.OnboardingScreen
import com.roomlauncher.app.ui.room.RoomScreen
import com.roomlauncher.app.ui.theme.LocalRoomPalette
import com.roomlauncher.app.ui.theme.RoomLauncherTheme

class MainActivity : ComponentActivity() {

    private val launcherViewModel: RoomLauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyFullEdgeToEdgeCutout()

        setContent {
            val uiState by launcherViewModel.uiState.collectAsStateWithLifecycle()

            RoomLauncherTheme(
                themeId = uiState.effectiveThemeId,
                roomStyle = uiState.config.roomStyleId,
                gdSubTheme = uiState.config.geometryDashSubTheme
            ) {
                val palette = LocalRoomPalette.current
                LaunchedEffect(palette.isDark) {
                    val barStyle = if (palette.isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(
                        statusBarStyle = barStyle,
                        navigationBarStyle = barStyle
                    )
                    applyFullEdgeToEdgeCutout()
                }

                RoomLauncherRoot(viewModel = launcherViewModel)
            }
        }
    }

    private fun applyFullEdgeToEdgeCutout() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Pressing Home / swiping up to Home cleanly closes Settings, All Apps, or open panels
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            launcherViewModel.returnToHomeRoom()
        }
        launcherViewModel.refreshSystemStatus()
    }
}

@Composable
private fun RoomLauncherRoot(
    viewModel: RoomLauncherViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.setAppForeground(true)
                    viewModel.refreshSystemStatus()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.setAppForeground(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // As a home launcher, Back dismisses active overlays/edit mode instead of exiting home
    BackHandler(enabled = uiState.config.onboardingCompleted) {
        viewModel.handleBackPressed()
    }

    when {
        !uiState.isLoaded -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        !uiState.config.onboardingCompleted -> {
            OnboardingScreen(
                currentStep = uiState.config.onboardingStep,
                installedAppCount = uiState.installedApps.size,
                isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                isDefaultLauncher = uiState.isDefaultLauncher,
                onStepChange = viewModel::setOnboardingStep,
                onRefreshStatus = viewModel::refreshSystemStatus,
                onComplete = viewModel::completeOnboarding
            )
        }
        else -> {
            RoomScreen(
                uiState = uiState,
                fftBandsFlow = viewModel.fftBands,
                hasLiveFftSignalFlow = viewModel.hasLiveFftSignal,
                onEnterEditMode = viewModel::enterEditMode,
                onExitEditMode = viewModel::exitEditMode,
                onHotspotPanelOpen = viewModel::openHotspotPanel,
                onHotspotPanelClose = viewModel::closeHotspotPanel,
                onEditHotspotOpen = viewModel::openEditHotspot,
                onEditHotspotClose = viewModel::closeEditHotspot,
                onAddHotspotOpen = viewModel::openAddHotspotDialog,
                onAddHotspotClose = viewModel::closeAddHotspotDialog,
                onAddHotspot = { label, subtitle, action ->
                    viewModel.addHotspot(label, subtitle, action)
                },
                onMoveHotspot = viewModel::moveHotspot,
                onResizeHotspot = viewModel::resizeHotspot,
                onUpdateHotspotDetails = viewModel::updateHotspotDetails,
                onDeleteHotspot = { id -> viewModel.deleteHotspot(id) },
                onMoveAppToHotspot = viewModel::moveAppToHotspot,
                onRemoveAppFromHotspot = viewModel::removeAppFromHotspot,
                onAddWebShortcut = viewModel::addWebShortcut,
                onDeleteWebShortcut = viewModel::deleteWebShortcut,
                onOpenAllApps = viewModel::openAllApps,
                onCloseAllApps = viewModel::closeAllApps,
                onOpenSettings = viewModel::openSettings,
                onCloseSettings = viewModel::closeSettings,
                onSelectRoomStyle = viewModel::setRoomStyle,
                onSelectGeometryDashSubTheme = viewModel::setGeometryDashSubTheme,
                onSelectTheme = viewModel::setTheme,
                onToggleDynamicTheme = viewModel::setDynamicThemeEnabled,
                onPickTeenPoster = viewModel::setTeenPoster,
                onClearTeenPoster = viewModel::clearTeenPoster,
                onToggleQuickAnimations = viewModel::setUseQuickAnimations,
                onToggleChargingAnimation = viewModel::setChargingAnimationEnabled,
                onSelectChargingAnimationStyle = viewModel::setChargingAnimationStyle,
                onPreviewChargingAnimation = viewModel::triggerChargingAnimationPreview,
                onDismissChargingAnimation = viewModel::dismissChargingAnimation,
                onExpandMusic = { viewModel.expandMusicVisualizer() },
                onExpandMusicToLyrics = { viewModel.expandMusicVisualizer(com.roomlauncher.app.data.MusicDisplayMode.LYRICS) },
                onCollapseMusic = viewModel::collapseMusicVisualizer,
                onSelectMusicDisplayMode = viewModel::setMusicDisplayMode,
                onSelectVisualizerStyle = viewModel::setMusicVisualizerStyle,
                onToggleMusicVisualizerEnabled = viewModel::setMusicVisualizerEnabled,
                onSelectMusicDefaultMode = viewModel::setMusicDefaultMode,
                onToggleAudioReactiveVisualizer = viewModel::setAudioReactiveVisualizerEnabled,
                onOpenNotificationAccess = viewModel::openNotificationAccessSettings,
                onPreviewMusicVisualizer = viewModel::previewMusicVisualizer,
                onExitMusicDemo = viewModel::stopMusicDemoPreview,
                onMusicTogglePlayPause = viewModel::toggleMusicPlayPause,
                onMusicSkipNext = viewModel::skipMusicNext,
                onMusicSkipPrevious = viewModel::skipMusicPrevious,
                onMusicSeekTo = viewModel::seekMusicTo,
                onOpenActiveMusicApp = viewModel::openActiveMusicApp,
                onRetryLyrics = viewModel::retryFetchLyrics,
                onToggleAutoCheckUpdates = viewModel::setAutoCheckUpdatesEnabled,
                onCheckForUpdates = { viewModel.checkForUpdates(userInitiated = true) },
                onShowUpdatePopup = viewModel::showUpdatePopup,
                onDismissUpdatePopup = viewModel::dismissUpdatePopup,
                onDownloadAndInstallUpdate = viewModel::downloadAndInstallUpdate,
                onSaveUpdateApkToDownloads = viewModel::saveDownloadedApkToDownloads,
                onUninstallForCleanUpdate = viewModel::uninstallCurrentAppForMigration,
                onOpenUpdateReleasePage = viewModel::openUpdateReleasePage,
                onToggleWeather = viewModel::setWeatherEnabled,
                onRefreshWeather = { viewModel.refreshWeather(forceRefresh = true) },
                onToggleMirrorWallpaper = viewModel::setMirrorHomeWallpaper,
                onSyncWallpaperNow = { viewModel.syncHomeWallpaperNow(showToast = true) },
                onPickFramePhoto = viewModel::setFramePhoto,
                onClearFramePhoto = viewModel::clearFramePhoto,
                onResetLayout = viewModel::resetRoomLayout,
                onReopenOnboarding = viewModel::reopenOnboarding,
                onExportBackup = viewModel::exportRoomBackup,
                onImportBackup = viewModel::importRoomBackup
            )
        }
    }
}
