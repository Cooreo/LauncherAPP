package com.roomlauncher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roomlauncher.app.ui.RoomLauncherViewModel
import com.roomlauncher.app.ui.onboarding.OnboardingScreen
import com.roomlauncher.app.ui.room.RoomScreen
import com.roomlauncher.app.ui.theme.RoomLauncherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RoomLauncherTheme {
                RoomLauncherRoot()
            }
        }
    }
}

@Composable
private fun RoomLauncherRoot(
    viewModel: RoomLauncherViewModel = viewModel()
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
                installedAppCount = uiState.installedApps.size,
                isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                isDefaultLauncher = uiState.isDefaultLauncher,
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
                onAddHotspot = viewModel::addHotspot,
                onMoveHotspot = viewModel::moveHotspot,
                onResizeHotspot = viewModel::resizeHotspot,
                onUpdateHotspotDetails = viewModel::updateHotspotDetails,
                onDeleteHotspot = { id -> viewModel.deleteHotspot(id) },
                onMoveAppToHotspot = viewModel::moveAppToHotspot,
                onRemoveAppFromHotspot = viewModel::removeAppFromHotspot,
                onOpenAllApps = viewModel::openAllApps,
                onCloseAllApps = viewModel::closeAllApps,
                onResetLayout = viewModel::resetRoomLayout,
                onReopenOnboarding = viewModel::reopenOnboarding
            )
        }
    }
}
