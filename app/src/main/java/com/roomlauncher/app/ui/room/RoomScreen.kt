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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.roomlauncher.app.data.RoomType
import com.roomlauncher.app.data.VisualizerStyle
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.RoomUiState
import com.roomlauncher.app.ui.allapps.AllAppsSheet
import com.roomlauncher.app.ui.settings.ChangelogDialog
import com.roomlauncher.app.ui.settings.SettingsSheet
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.LocalRoomPalette
import com.roomlauncher.app.ui.theme.RoomInk
import com.roomlauncher.app.ui.theme.RoomSage
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val ROOM_CAMERA_SCALE = 0.93f
internal const val ROOM_CAMERA_ANCHOR_Y = 0.58f

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
    onAddHotspot: (label: String, subtitle: String, actionType: HotspotActionType, visualType: RoomObjectVisual) -> Unit,
    onMoveHotspot: (hotspotId: String, newX: Float, newY: Float) -> Unit,
    onResizeHotspot: (hotspotId: String, newW: Float, newH: Float) -> Unit,
    onUpdateHotspotDetails: (hotspotId: String, label: String, subtitle: String, actionType: HotspotActionType, visualType: RoomObjectVisual) -> Unit,
    onDeleteHotspot: (hotspotId: String) -> Unit,
    onSelectRoom: (Int) -> Unit,
    onAddRoom: (RoomType, String) -> Unit,
    onRenameRoom: (roomId: String, newName: String) -> Unit,
    onDeleteRoom: (roomId: String) -> Unit,
    onOpenChangelog: () -> Unit,
    onDismissChangelog: () -> Unit,
    onMoveAppToHotspot: (packageName: String, targetHotspotId: String) -> Unit,
    onRemoveAppFromHotspot: (packageName: String, hotspotId: String) -> Unit,
    onAddWebShortcut: (title: String, url: String) -> Unit,
    onDeleteWebShortcut: (String) -> Unit,
    onOpenAllApps: () -> Unit,
    onCloseAllApps: () -> Unit,
    onToggleAllAppsGridMode: (Boolean) -> Unit,
    onSetAllAppsScalePercent: (Int) -> Unit,
    onTogglePinApp: (String) -> Unit,
    onRecordAppLaunch: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSelectRoomStyle: (RoomStyleId) -> Unit,
    onSelectGeometryDashSubTheme: (GeometryDashSubTheme) -> Unit,
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
    onSaveUpdateApkToDownloads: () -> Unit,
    onUninstallForCleanUpdate: () -> Unit,
    onOpenUpdateReleasePage: () -> Unit,
    onToggleWeather: (Boolean) -> Unit,
    onRefreshWeather: () -> Unit,
    onToggleMirrorWallpaper: (Boolean) -> Unit,
    onSyncWallpaperNow: () -> Unit,
    onPickFramePhoto: (Uri) -> Unit,
    onClearFramePhoto: () -> Unit,
    onResetLayout: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val palette = LocalRoomPalette.current
    val coroutineScope = rememberCoroutineScope()

    val rooms = uiState.rooms
    val pagerState = rememberPagerState(
        initialPage = uiState.safeCurrentRoomIndex,
        pageCount = { rooms.size.coerceAtLeast(1) }
    )

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Sync user swipes from HorizontalPager -> ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != uiState.safeCurrentRoomIndex) {
                    zoomScale = 1f
                    panOffset = Offset.Zero
                    onSelectRoom(page)
                }
            }
    }

    // Sync programmatic room changes (Settings jump, Back/Home reset, Add Room) -> HorizontalPager
    LaunchedEffect(uiState.safeCurrentRoomIndex, rooms.size) {
        val target = uiState.safeCurrentRoomIndex.coerceIn(0, (rooms.size - 1).coerceAtLeast(0))
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

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
        lastTvCenterX = 0.5f + (activeHotspot.centerX - 0.5f) * ROOM_CAMERA_SCALE
        lastTvCenterY = ROOM_CAMERA_ANCHOR_Y + (activeHotspot.centerY - ROOM_CAMERA_ANCHOR_Y) * ROOM_CAMERA_SCALE
        lastTvScale = (0.94f / (activeHotspot.width * ROOM_CAMERA_SCALE).coerceAtLeast(0.24f)).coerceIn(1.75f, 2.75f)
    }
    val tvZoomProgress by animateFloatAsState(
        targetValue = if (isTvZoomActive) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "tv_camera_zoom_progress"
    )

    val isFullScreenSheetOpen = uiState.showAllAppsSheet || uiState.showSettingsSheet

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
        // Multi-Room HorizontalPager — swipe right-to-left like a normal Android launcher to walk between rooms!
        // Kept mounted underneath Settings & All Apps so Predictive Back reveals are 100% glitch-free.
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !uiState.isEditMode &&
                !isFullScreenSheetOpen &&
                activeHotspot == null &&
                !uiState.isMusicExpanded &&
                zoomScale <= 1.02f,
            beyondViewportPageCount = 0,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val roomPage = rooms.getOrElse(pageIndex) { uiState.currentRoom }
            val isThisPageActive = pageIndex == pagerState.currentPage

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.isEditMode, isFullScreenSheetOpen) {
                        if (!uiState.isEditMode && !isFullScreenSheetOpen) {
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
                    // Pinch-zoom only engages on 2+ fingers OR when already zoomed in,
                    // so 1-finger horizontal swipes NEVER fight HorizontalPager!
                    .pointerInput(uiState.isEditMode, isFullScreenSheetOpen, isThisPageActive) {
                        if (!uiState.isEditMode && !isFullScreenSheetOpen && isThisPageActive) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val pointerCount = event.changes.count { it.pressed }
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val isPinching = pointerCount >= 2 && abs(zoomChange - 1f) > 0.002f
                                    val isZoomedPan = zoomScale > 1.02f && panChange != Offset.Zero
                                    if (isPinching || isZoomedPan) {
                                        val newScale = (zoomScale * zoomChange).coerceIn(1f, 2.2f)
                                        if (newScale > 1.02f) {
                                            val maxPanX = (size.width * (newScale - 1f)) / 2f
                                            val maxPanY = (size.height * (newScale - 1f)) / 2f
                                            val nextPan = panOffset + panChange
                                            zoomScale = newScale
                                            panOffset = Offset(
                                                x = nextPan.x.coerceIn(-maxPanX, maxPanX),
                                                y = nextPan.y.coerceIn(-maxPanY, maxPanY)
                                            )
                                        } else {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    }
            ) {
                val roomWidthPx = with(density) { maxWidth.toPx() }
                val roomHeightPx = with(density) { maxHeight.toPx() }
                val activeZoom = if (isThisPageActive) zoomScale else 1f
                val activePan = if (isThisPageActive) panOffset else Offset.Zero
                val activeTvZoom = if (isThisPageActive) tvZoomProgress else 0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val currentTvScale = 1f + (lastTvScale - 1f) * activeTvZoom
                            val combinedScale = activeZoom * currentTvScale
                            val tvTx = (0.5f - lastTvCenterX) * roomWidthPx * currentTvScale * activeTvZoom
                            val tvTy = (0.5f - lastTvCenterY) * roomHeightPx * currentTvScale * activeTvZoom
                            scaleX = combinedScale
                            scaleY = combinedScale
                            translationX = activePan.x * (1f - activeTvZoom) + tvTx
                            translationY = activePan.y * (1f - activeTvZoom) + tvTy
                            transformOrigin = TransformOrigin.Center
                        }
                ) {
                    // 1. Themed Architectural Room Shell (with Room-Specific Accents!)
                    RoomShellCanvas(
                        palette = palette,
                        roomStyle = uiState.config.roomStyleId,
                        roomType = roomPage.type,
                        gdSubTheme = uiState.config.geometryDashSubTheme,
                        posterLeftBitmap = uiState.posterLeftBitmap,
                        posterRightBitmap = uiState.posterRightBitmap,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. Movable Illustrated Objects inside this Room (gently stepped back for comfortable perspective!)
                    roomPage.hotspots.forEach { hotspot ->
                        if (uiState.isEditMode && isThisPageActive) {
                            EditableHotspotBox(
                                hotspot = hotspot,
                                roomWidthPx = roomWidthPx,
                                roomHeightPx = roomHeightPx,
                                cameraScale = ROOM_CAMERA_SCALE,
                                cameraAnchorY = ROOM_CAMERA_ANCHOR_Y,
                                appCount = uiState.appsFor(hotspot).size,
                                roomStyle = uiState.config.roomStyleId,
                                gdSubTheme = uiState.config.geometryDashSubTheme,
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
                            val viewX = 0.5f + (hotspot.x - 0.5f) * ROOM_CAMERA_SCALE
                            val viewY = ROOM_CAMERA_ANCHOR_Y + (hotspot.y - ROOM_CAMERA_ANCHOR_Y) * ROOM_CAMERA_SCALE
                            val viewW = hotspot.width * ROOM_CAMERA_SCALE
                            val viewH = hotspot.height * ROOM_CAMERA_SCALE
                            val boxWidthDp = with(density) { (viewW * roomWidthPx).toDp() }
                            val boxHeightDp = with(density) { (viewH * roomHeightPx).toDp() }
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
                                            x = (viewX * roomWidthPx).roundToInt(),
                                            y = (viewY * roomHeightPx).roundToInt()
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
                                    gdSubTheme = uiState.config.geometryDashSubTheme,
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
                    roomName = uiState.currentRoom.name,
                    onDone = onExitEditMode,
                    onAddObject = onAddHotspotOpen,
                    onResetClick = { showResetConfirm = true },
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )
            }

            // Bottom subtle swipe-up zone: Room Switcher Dots + All Apps pill
            // Apply navigationBarsPadding() BEFORE height/content so neither pill can ever be clipped!
            if (!uiState.isEditMode) {
                var swipeAccumulated by remember { mutableFloatStateOf(0f) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
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
                        .padding(top = 18.dp, bottom = 12.dp)
                ) {
                    if (rooms.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (palette.isDark) {
                                Color.Black.copy(alpha = 0.42f)
                            } else {
                                RoomInk.copy(alpha = 0.56f)
                            },
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = uiState.currentRoom.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    rooms.forEachIndexed { idx, room ->
                                        val isSelected = idx == pagerState.currentPage
                                        Box(
                                            modifier = Modifier
                                                .height(6.dp)
                                                .width(if (isSelected) 16.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.38f)
                                                )
                                                .clickable {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(idx)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                        onRecordAppLaunch(app.packageName)
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

        // Pre-warmed, persistently mounted All Apps Drawer (loaded on boot, zero mount/unmount hitch!)
        val isAllAppsVisible = uiState.showAllAppsSheet && !uiState.showSettingsSheet
        AllAppsSheet(
            isVisible = isAllAppsVisible,
            installedApps = uiState.installedApps,
            pinnedPackages = uiState.config.pinnedAppPackages,
            recentPackages = uiState.config.recentAppPackages,
            isGridMode = uiState.config.allAppsGridMode,
            drawerScalePercent = uiState.config.safeAllAppsScalePercent,
            packageOwnerLabels = uiState.packageOwnerLabels,
            defaultUnsortedLabel = uiState.defaultUnsortedLabel,
            panelHotspotChoices = uiState.panelHotspotChoices,
            onLaunchApp = { app ->
                onCloseAllApps()
                onRecordAppLaunch(app.packageName)
                SystemIntentHelper.launchApp(context, app.packageName)
            },
            onToggleGridMode = onToggleAllAppsGridMode,
            onTogglePinApp = onTogglePinApp,
            onMoveApp = onMoveAppToHotspot,
            onUninstallApp = onUninstallApp,
            onOpenSettings = onOpenSettings,
            onDismiss = onCloseAllApps
        )

        // Smooth Room Launcher Settings Screen + clean opaque exit!
        AnimatedVisibility(
            visible = uiState.showSettingsSheet,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 10 }
            ) + fadeIn(tween(150)),
            exit = fadeOut(tween(130))
        ) {
            SettingsSheet(
                uiState = uiState,
                onSelectRoomStyle = onSelectRoomStyle,
                onSelectGeometryDashSubTheme = onSelectGeometryDashSubTheme,
                onSelectTheme = onSelectTheme,
                onToggleDynamicTheme = onToggleDynamicTheme,
                onToggleAllAppsGridMode = onToggleAllAppsGridMode,
                onSetAllAppsScalePercent = onSetAllAppsScalePercent,
                onSelectRoomIndex = onSelectRoom,
                onAddRoom = onAddRoom,
                onRenameRoom = onRenameRoom,
                onDeleteRoom = onDeleteRoom,
                onOpenChangelog = onOpenChangelog,
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
                onSaveUpdateApkToDownloads = onSaveUpdateApkToDownloads,
                onUninstallForCleanUpdate = onUninstallForCleanUpdate,
                onOpenUpdateReleasePage = onOpenUpdateReleasePage,
                onToggleWeather = onToggleWeather,
                onRefreshWeather = onRefreshWeather,
                onToggleMirrorWallpaper = onToggleMirrorWallpaper,
                onSyncWallpaperNow = onSyncWallpaperNow,
                onPickFramePhoto = onPickFramePhoto,
                onClearFramePhoto = onClearFramePhoto,
                onEnterEditMode = onEnterEditMode,
                onReopenOnboarding = onReopenOnboarding,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
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
                gdSubTheme = uiState.config.geometryDashSubTheme,
                animationStyle = uiState.config.chargingAnimationStyle,
                triggerKey = uiState.activeChargingSplashKey,
                onFinished = onDismissChargingAnimation
            )
        }
    }

    // Edit Object Dialog
    val editingHotspot = uiState.editingHotspot
    if (editingHotspot != null) {
        val unsortedCount = uiState.config.allHotspots.count { it.actionType == HotspotActionType.UNSORTED_PANEL }
        val canDelete = !(editingHotspot.actionType == HotspotActionType.UNSORTED_PANEL && unsortedCount <= 1)

        EditHotspotDialog(
            hotspot = editingHotspot,
            canDelete = canDelete,
            onSave = { label, subtitle, actionType, visualType ->
                onUpdateHotspotDetails(editingHotspot.id, label, subtitle, actionType, visualType)
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
            roomName = uiState.currentRoom.name,
            onAdd = onAddHotspot,
            onDismiss = onAddHotspotClose
        )
    }

    // In-App Changelog ("What's New") Dialog
    if (uiState.showChangelogDialog) {
        ChangelogDialog(
            currentVersion = uiState.updateState.currentVersion,
            isAutoWelcome = uiState.isAutoWelcomeChangelog,
            onDismiss = onDismissChangelog
        )
    }

    // Confirm Reset Layout Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset House Layout?") },
            text = {
                Text("This restores all default rooms (Bedroom, Living Room, Kitchen, Gaming Room) and their default furniture positions.")
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
    roomName: String,
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
                        text = "Edit $roomName",
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
