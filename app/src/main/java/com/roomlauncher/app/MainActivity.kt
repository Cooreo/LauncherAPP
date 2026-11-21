package com.roomlauncher.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

        setContent {
            val uiState by launcherViewModel.uiState.collectAsStateWithLifecycle()

            RoomLauncherTheme(themeId = uiState.config.themeId) {
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
                }

                RoomLauncherRoot(viewModel = launcherViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Pressing Home or accepting ROLE_HOME delivers a new HOME intent; refresh status cleanly
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
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemStatus()
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
                onSelectTheme = viewModel::setTheme,
                onPickTeenPoster = viewModel::setTeenPoster,
                onClearTeenPoster = viewModel::clearTeenPoster,
                onToggleQuickAnimations = viewModel::setUseQuickAnimations,
                onToggleWeather = viewModel::setWeatherEnabled,
                onRefreshWeather = { viewModel.refreshWeather(forceRefresh = true) },
                onToggleMirrorWallpaper = viewModel::setMirrorHomeWallpaper,
                onSyncWallpaperNow = { viewModel.syncHomeWallpaperNow(showToast = true) },
                onPickFramePhoto = viewModel::setFramePhoto,
                onClearFramePhoto = viewModel::clearFramePhoto,
                onResetLayout = viewModel::resetRoomLayout,
                onReopenOnboarding = viewModel::reopenOnboarding
            )
        }
    }
}
