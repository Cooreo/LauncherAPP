package com.roomlauncher.app.ui.room

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.MusicDisplayMode
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.VisualizerStyle
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.RoomUiState
import com.roomlauncher.app.ui.allapps.AllAppsSheet
import com.roomlauncher.app.ui.settings.SettingsSheet
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.LocalRoomPalette
import com.roomlauncher.app.ui.theme.RoomInk
import com.roomlauncher.app.ui.theme.RoomSage
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoomScreen(
    uiState: RoomUiState,
    fftBandsFlow: StateFlow<FloatArray>,
    hasLiveFftSignalFlow: StateFlow<Boolean>,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit,
    onHotspotPanelOpen: (String) -> Unit,
    onHotspotPanelClose: () -> Unit,
    onEditHotspotOpen: (String) -> Unit,
    onEditHotspotClose: () -> Unit,
    onAddHotspotOpen: () -> Unit,
    onAddHotspotClose: () -> Unit,
    onAddHotspot: (label: String, subtitle: String, actionType: HotspotActionType) -> Unit,
    onMoveHotspot: (hotspotId: String, newX: Float, newY: Float) -> Unit,
    onResizeHotspot: (hotspotId: String, newW: Float, newH: Float) -> Unit,
    onUpdateHotspotDetails: (hotspotId: String, label: String, subtitle: String, actionType: HotspotActionType) -> Unit,
    onDeleteHotspot: (hotspotId: String) -> Unit,
    onMoveAppToHotspot: (packageName: String, targetHotspotId: String) -> Unit,
    onRemoveAppFromHotspot: (packageName: String, hotspotId: String) -> Unit,
    onAddWebShortcut: (title: String, url: String) -> Unit,
    onDeleteWebShortcut: (String) -> Unit,
    onOpenAllApps: () -> Unit,
    onCloseAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSelectRoomStyle: (RoomStyleId) -> Unit,
    onSelectTheme: (RoomThemeId) -> Unit,
    onToggleDynamicTheme: (Boolean) -> Unit,
    onPickTeenPoster: (isLeft: Boolean, uri: Uri) -> Unit,
    onClearTeenPoster: (isLeft: Boolean) -> Unit,
    onToggleQuickAnimations: (Boolean) -> Unit,
    onToggleChargingAnimation: (Boolean) -> Unit,
    onSelectChargingAnimationStyle: (ChargingAnimationStyle) -> Unit,
    onPreviewChargingAnimation: () -> Unit,
    onDismissChargingAnimation: () -> Unit,
    onExpandMusic: () -> Unit,
    onExpandMusicToLyrics: () -> Unit,
    onCollapseMusic: () -> Unit,
    onSelectMusicDisplayMode: (MusicDisplayMode) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onToggleMusicVisualizerEnabled: (Boolean) -> Unit,
    onSelectMusicDefaultMode: (MusicDisplayMode) -> Unit,
    onToggleAudioReactiveVisualizer: (Boolean) -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onPreviewMusicVisualizer: () -> Unit,
    onExitMusicDemo: () -> Unit,
    onMusicTogglePlayPause: () -> Unit,
    onMusicSkipNext: () -> Unit,
    onMusicSkipPrevious: () -> Unit,
    onMusicSeekTo: (Long) -> Unit,
    onOpenActiveMusicApp: () -> Unit,
    onRetryLyrics: () -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onShowUpdatePopup: () -> Unit,
    onDismissUpdatePopup: () -> Unit,
    onDownloadAndInstallUpdate: () -> Unit,
    onOpenUpdateReleasePage: () -> Unit,
    onToggleWeather: (Boolean) -> Unit,
    onRefreshWeather: () -> Unit,
    onToggleMirrorWallpaper: (Boolean) -> Unit,
    onSyncWallpaperNow: () -> Unit,
    onPickFramePhoto: (Uri) -> Unit,
    onClearFramePhoto: () -> Unit,
    onResetLayout: () -> Unit,
    onReopenOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val palette = LocalRoomPalette.current

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Live minutes of day for analog Clock wall hands
    var currentMinutesOfDay by remember {
        val cal = Calendar.getInstance()
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
    }
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            delay(30_000L)
        }
    }

    // Reset manual zoom when entering edit mode
    LaunchedEffect(uiState.isEditMode) {
        if (uiState.isEditMode) {
            zoomScale = 1f
            panOffset = Offset.Zero
        }
    }

    // Rich TV camera zoom: zooms directly onto the TV's center so the TV fills screen center!
    val activeHotspot = uiState.activeHotspot
    val isTvZoomActive = !uiState.config.useQuickAnimations &&
        activeHotspot?.visualType == RoomObjectVisual.TV
    var lastTvCenterX by remember { mutableFloatStateOf(0.29f) }
    var lastTvCenterY by remember { mutableFloatStateOf(0.37f) }
    var lastTvScale by remember { mutableFloatStateOf(2.0f) }
    if (isTvZoomActive && activeHotspot != null) {
        lastTvCenterX = activeHotspot.centerX
        lastTvCenterY = activeHotspot.centerY
        lastTvScale = (0.94f / activeHotspot.width.coerceAtLeast(0.26f)).coerceIn(1.75f, 2.75f)
    }
    val tvZoomProgress by animateFloatAsState(
        targetValue = if (isTvZoomActive) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "tv_camera_zoom_progress"
    )

    val isFullScreenSheetOpen = uiState.showAllAppsSheet || uiState.showSettingsSheet
    // Keep the Room mounted during the 240ms slide-up/down transition so opening feels silky smooth,
    // then unmount it once covered so All Apps scrolling has 0% background GPU overdraw!
    var isFullScreenSheetSettled by remember { mutableStateOf(false) }
    LaunchedEffect(isFullScreenSheetOpen) {
        if (isFullScreenSheetOpen) {
            delay(250L)
            isFullScreenSheetSettled = true
        } else {
            isFullScreenSheetSettled = false
        }
    }

    val handleHotspotTap: (RoomHotspot) -> Unit = { hotspot ->
        when (hotspot.actionType) {
            HotspotActionType.PANEL,
            HotspotActionType.UNSORTED_PANEL -> onHotspotPanelOpen(hotspot.id)
            HotspotActionType.DIRECT_GALLERY -> SystemIntentHelper.openGallery(context)
            HotspotActionType.DIRECT_ALARM -> SystemIntentHelper.openAlarms(context)
            HotspotActionType.DIRECT_WEATHER -> {
                val installedPairs = uiState.installedApps.map { it.packageName to it.label }
                SystemIntentHelper.openWeather(context, installedPairs)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Keep Room visible during sheet transition, unmount once sheet fully covers screen!
        if (!isFullScreenSheetSettled) {
            // Main interactive room canvas with movable themed objects
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.isEditMode) {
                        if (!uiState.isEditMode) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onEnterEditMode()
                                },
                                onDoubleTap = {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                }
                            )
                        }
                    }
                    .pointerInput(uiState.isEditMode) {
                        if (!uiState.isEditMode) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (zoomScale * zoom).coerceIn(1f, 2.2f)
                                if (newScale > 1.01f) {
                                    val maxPanX = (size.width * (newScale - 1f)) / 2f
                                    val maxPanY = (size.height * (newScale - 1f)) / 2f
                                    val nextPan = panOffset + pan
                                    zoomScale = newScale
                                    panOffset = Offset(
                                        x = nextPan.x.coerceIn(-maxPanX, maxPanX),
                                        y = nextPan.y.coerceIn(-maxPanY, maxPanY)
                                    )
                                } else {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                }
                            }
                        }
                    }
            ) {
                val roomWidthPx = with(density) { maxWidth.toPx() }
                val roomHeightPx = with(density) { maxHeight.toPx() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val currentTvScale = 1f + (lastTvScale - 1f) * tvZoomProgress
                            val combinedScale = zoomScale * currentTvScale
                            val tvTx = (0.5f - lastTvCenterX) * roomWidthPx * currentTvScale * tvZoomProgress
                            val tvTy = (0.5f - lastTvCenterY) * roomHeightPx * currentTvScale * tvZoomProgress
                            scaleX = combinedScale
                            scaleY = combinedScale
                            translationX = panOffset.x * (1f - tvZoomProgress) + tvTx
                            translationY = panOffset.y * (1f - tvZoomProgress) + tvTy
                            transformOrigin = TransformOrigin.Center
                        }
                ) {
                    // 1. Empty Themed Room Shell (walls, floor, rug, style decor)
                    RoomShellCanvas(
                        palette = palette,
                        roomStyle = uiState.config.roomStyleId,
                        posterLeftBitmap = uiState.posterLeftBitmap,
                        posterRightBitmap = uiState.posterRightBitmap,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. Movable Illustrated Objects (both artwork & tap/drag hitbox live together!)
                    uiState.config.hotspots.forEach { hotspot ->
                        if (uiState.isEditMode) {
                            EditableHotspotBox(
                                hotspot = hotspot,
                                roomWidthPx = roomWidthPx,
                                roomHeightPx = roomHeightPx,
                                appCount = uiState.appsFor(hotspot).size,
                                roomStyle = uiState.config.roomStyleId,
                                weatherState = uiState.weatherState,
                                weatherEnabled = uiState.config.weatherEnabled,
                                frameImageBitmap = uiState.frameImageBitmap,
                                currentMinutesOfDay = currentMinutesOfDay,
                                onMoveFinished = { newX, newY ->
                                    onMoveHotspot(hotspot.id, newX, newY)
                                },
                                onResizeFinished = { newW, newH ->
                                    onResizeHotspot(hotspot.id, newW, newH)
                                },
                                onTapEdit = { onEditHotspotOpen(hotspot.id) }
                            )
                        } else {
                            val boxWidthDp = with(density) { (hotspot.width * roomWidthPx).toDp() }
                            val boxHeightDp = with(density) { (hotspot.height * roomHeightPx).toDp() }
                            val interactionSource = remember(hotspot.id) { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val pressScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "obj_press_${hotspot.id}"
                            )
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (hotspot.x * roomWidthPx).roundToInt(),
                                            y = (hotspot.y * roomHeightPx).roundToInt()
                                        )
                                    }
                                    .size(width = boxWidthDp, height = boxHeightDp)
                                    .graphicsLayer {
                                        scaleX = pressScale
                                        scaleY = pressScale
                                    }
                                    .combinedClickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { handleHotspotTap(hotspot) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onEnterEditMode()
                                        }
                                    )
                            ) {
                                RoomObjectGraphic(
                                    visualType = hotspot.visualType,
                                    palette = palette,
                                    roomStyle = uiState.config.roomStyleId,
                                    weatherState = uiState.weatherState,
                                    weatherEnabled = uiState.config.weatherEnabled,
                                    frameImageBitmap = uiState.frameImageBitmap,
                                    currentMinutesOfDay = currentMinutesOfDay,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Discreet Reset Zoom chip only when manually zoomed, OR Edit Mode toolbar (no redundant top clock!)
            if (!uiState.isEditMode) {
                if (zoomScale > 1.05f) {
                    Surface(
                        shape = CircleShape,
                        color = RoomInk.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .clickable {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomOutMap,
                                contentDescription = "Reset zoom",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Reset Zoom",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                EditModeTopBar(
                    onDone = onExitEditMode,
                    onAddObject = onAddHotspotOpen,
                    onResetClick = { showResetConfirm = true },
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )
            }

            // Bottom subtle swipe-up zone & All Apps A–Z fallback pill
            if (!uiState.isEditMode) {
                var swipeAccumulated by remember { mutableFloatStateOf(0f) }
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(88.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { swipeAccumulated = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    swipeAccumulated += dragAmount
                                    if (swipeAccumulated < -60f) {
                                        swipeAccumulated = 0f
                                        onOpenAllApps()
                                    }
                                }
                            )
                        }
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (palette.isDark) {
                            Color.White.copy(alpha = 0.14f)
                        } else {
                            RoomInk.copy(alpha = 0.62f)
                        },
                        modifier = Modifier.clickable(onClick = onOpenAllApps)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "Open all apps",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "All Apps",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Floating Now-Playing compact music pill (visible only when music is active!)
            if (!uiState.isEditMode &&
                zoomScale <= 1.05f &&
                uiState.config.musicVisualizerEnabled &&
                uiState.nowPlaying.hasSession &&
                !uiState.isMusicExpanded &&
                activeHotspot == null
            ) {
                NowPlayingCompactPill(
                    nowPlaying = uiState.nowPlaying,
                    displayMode = uiState.musicDisplayMode,
                    onExpand = onExpandMusic,
                    onExpandToLyrics = onExpandMusicToLyrics,
                    onTogglePlayPause = onMusicTogglePlayPause,
                    onExitDemo = onExitMusicDemo,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // Sleek clickable charging badge while plugged in on the room screen (no bottom edge bar!)
            if (!uiState.isEditMode &&
                uiState.config.chargingAnimationEnabled &&
                uiState.batteryStatus.isCharging
            ) {
                AmbientChargingIndicator(
                    batteryStatus = uiState.batteryStatus,
                    roomStyle = uiState.config.roomStyleId,
                    animationStyle = uiState.config.chargingAnimationStyle,
                    onReplayAnimation = onPreviewChargingAnimation
                )
            }

            // Expanded Interactive Music Visualizer & Synced Lyrics Overlay
            if (uiState.isMusicExpanded &&
                uiState.config.musicVisualizerEnabled &&
                uiState.nowPlaying.hasSession
            ) {
                val fftBands by fftBandsFlow.collectAsStateWithLifecycle()
                val hasLiveFftSignal by hasLiveFftSignalFlow.collectAsStateWithLifecycle()
                MusicVisualizerOverlay(
                    nowPlaying = uiState.nowPlaying,
                    displayMode = uiState.musicDisplayMode,
                    visualizerStyle = uiState.config.musicVisualizerStyle,
                    lyricsState = uiState.lyricsState,
                    fftBands = fftBands,
                    hasLiveFftSignal = hasLiveFftSignal,
                    onSelectDisplayMode = onSelectMusicDisplayMode,
                    onSelectVisualizerStyle = onSelectVisualizerStyle,
                    onTogglePlayPause = onMusicTogglePlayPause,
                    onSkipNext = onMusicSkipNext,
                    onSkipPrevious = onMusicSkipPrevious,
                    onSeekTo = onMusicSeekTo,
                    onOpenMusicApp = onOpenActiveMusicApp,
                    onRetryLyrics = onRetryLyrics,
                    onExitDemo = onExitMusicDemo,
                    onDismiss = onCollapseMusic
                )
            }

            // Animated Panel Overlay (with per-object Rich Animations or Quick Animations)
            if (activeHotspot != null) {
                HotspotPanelOverlay(
                    hotspot = activeHotspot,
                    apps = uiState.appsFor(activeHotspot),
                    allInstalledApps = uiState.installedApps,
                    otherPanelHotspots = uiState.panelHotspots.filter { it.id != activeHotspot.id },
                    webShortcuts = uiState.config.webShortcuts,
                    useQuickAnimations = uiState.config.useQuickAnimations,
                    onLaunchApp = { app ->
                        onHotspotPanelClose()
                        SystemIntentHelper.launchApp(context, app.packageName)
                    },
                    onOpenWebShortcut = { shortcut ->
                        onHotspotPanelClose()
                        SystemIntentHelper.openWebUrl(context, shortcut.url)
                    },
                    onAddWebShortcut = onAddWebShortcut,
                    onDeleteWebShortcut = onDeleteWebShortcut,
                    onMoveAppToHotspot = onMoveAppToHotspot,
                    onRemoveAppFromPanel = { pkg ->
                        onRemoveAppFromHotspot(pkg, activeHotspot.id)
                    },
                    onOpenAllApps = {
                        onHotspotPanelClose()
                        onOpenAllApps()
                    },
                    onOpenEditHotspot = {
                        onHotspotPanelClose()
                        onEnterEditMode()
                        onEditHotspotOpen(activeHotspot.id)
                    },
                    onDismiss = onHotspotPanelClose
                )
            }
        }

        // Smooth slide-up A–Z Searchable All Apps Drawer + Settings Card (0% overdraw once settled!)
        AnimatedVisibility(
            visible = uiState.showAllAppsSheet && !uiState.showSettingsSheet,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 5 }
            ) + fadeIn(tween(190)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 5 }
            ) + fadeOut(tween(150))
        ) {
            AllAppsSheet(
                installedApps = uiState.installedApps,
                packageOwnerLabels = uiState.packageOwnerLabels,
                defaultUnsortedLabel = uiState.defaultUnsortedLabel,
                panelHotspots = uiState.panelHotspots,
                onLaunchApp = { app ->
                    onCloseAllApps()
                    SystemIntentHelper.launchApp(context, app.packageName)
                },
                onMoveApp = onMoveAppToHotspot,
                onOpenSettings = onOpenSettings,
                onDismiss = onCloseAllApps
            )
        }

        // Smooth slide-up Room Launcher Settings Screen
        AnimatedVisibility(
            visible = uiState.showSettingsSheet,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 5 }
            ) + fadeIn(tween(190)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 5 }
            ) + fadeOut(tween(150))
        ) {
            SettingsSheet(
                uiState = uiState,
                onSelectRoomStyle = onSelectRoomStyle,
                onSelectTheme = onSelectTheme,
                onToggleDynamicTheme = onToggleDynamicTheme,
                onPickTeenPoster = onPickTeenPoster,
                onClearTeenPoster = onClearTeenPoster,
                onToggleQuickAnimations = onToggleQuickAnimations,
                onToggleChargingAnimation = onToggleChargingAnimation,
                onSelectChargingAnimationStyle = onSelectChargingAnimationStyle,
                onPreviewChargingAnimation = onPreviewChargingAnimation,
                onToggleMusicVisualizerEnabled = onToggleMusicVisualizerEnabled,
                onSelectVisualizerStyle = onSelectVisualizerStyle,
                onSelectMusicDefaultMode = onSelectMusicDefaultMode,
                onToggleAudioReactiveVisualizer = onToggleAudioReactiveVisualizer,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onPreviewMusicVisualizer = onPreviewMusicVisualizer,
                onToggleAutoCheckUpdates = onToggleAutoCheckUpdates,
                onCheckForUpdates = onCheckForUpdates,
                onShowUpdatePopup = onShowUpdatePopup,
                onDismissUpdatePopup = onDismissUpdatePopup,
                onDownloadAndInstallUpdate = onDownloadAndInstallUpdate,
                onOpenUpdateReleasePage = onOpenUpdateReleasePage,
                onToggleWeather = onToggleWeather,
                onRefreshWeather = onRefreshWeather,
                onToggleMirrorWallpaper = onToggleMirrorWallpaper,
                onSyncWallpaperNow = onSyncWallpaperNow,
                onPickFramePhoto = onPickFramePhoto,
                onClearFramePhoto = onClearFramePhoto,
                onEnterEditMode = onEnterEditMode,
                onReopenOnboarding = onReopenOnboarding,
                onResetRoom = {
                    onCloseSettings()
                    showResetConfirm = true
                },
                onDismiss = onCloseSettings
            )
        }

        // Custom Room-Styled Charging Animation Overlay (triggers on plug-in while screen is on, or preview!)
        if (uiState.activeChargingSplashKey != 0L) {
            ChargingSplashOverlay(
                batteryStatus = uiState.batteryStatus,
                roomStyle = uiState.config.roomStyleId,
                animationStyle = uiState.config.chargingAnimationStyle,
                triggerKey = uiState.activeChargingSplashKey,
                onFinished = onDismissChargingAnimation
            )
        }
    }

    // Edit Object Dialog
    val editingHotspot = uiState.editingHotspot
    if (editingHotspot != null) {
        val unsortedCount = uiState.config.hotspots.count { it.actionType == HotspotActionType.UNSORTED_PANEL }
        val canDelete = !(editingHotspot.actionType == HotspotActionType.UNSORTED_PANEL && unsortedCount <= 1)

        EditHotspotDialog(
            hotspot = editingHotspot,
            canDelete = canDelete,
            onSave = { label, subtitle, actionType ->
                onUpdateHotspotDetails(editingHotspot.id, label, subtitle, actionType)
            },
            onOpenAppsPanel = {
                onEditHotspotClose()
                onHotspotPanelOpen(editingHotspot.id)
            },
            onDelete = { onDeleteHotspot(editingHotspot.id) },
            onDismiss = onEditHotspotClose
        )
    }

    // Add New Object Dialog
    if (uiState.showAddHotspotDialog) {
        AddHotspotDialog(
            onAdd = onAddHotspot,
            onDismiss = onAddHotspotClose
        )
    }

    // Confirm Reset Layout Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Room Layout?") },
            text = {
                Text("This restores all default room objects (Window, Painting, Clock, Sticky Note, TV, Game Console, Shelf, Bed, Trash Can) and their default positions.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetLayout()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EditModeTopBar(
    onDone: () -> Unit,
    onAddObject: () -> Unit,
    onResetClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = RoomInk.copy(alpha = 0.90f),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Edit Room",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp, end = 6.dp)
                    )
                    IconButton(onClick = onAddObject, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add room object",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onResetClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset default room",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Room Launcher settings",
                            tint = Color.White
                        )
                    }
                }

                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoomSage),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = RoomInk.copy(alpha = 0.76f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Drag objects anywhere to rearrange • Corner handle resizes • Tap to edit",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}
