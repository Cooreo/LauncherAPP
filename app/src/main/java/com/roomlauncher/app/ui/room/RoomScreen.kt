package com.roomlauncher.app.ui.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.R
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.RoomUiState
import com.roomlauncher.app.ui.allapps.AllAppsSheet
import com.roomlauncher.app.ui.theme.RoomInk
import com.roomlauncher.app.ui.theme.RoomSage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoomScreen(
    uiState: RoomUiState,
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
    onOpenAllApps: () -> Unit,
    onCloseAllApps: () -> Unit,
    onResetLayout: () -> Unit,
    onReopenOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Reset zoom when entering edit mode so hotspot coordinates remain crisp and predictable
    LaunchedEffect(uiState.isEditMode) {
        if (uiState.isEditMode) {
            zoomScale = 1f
            panOffset = Offset.Zero
        }
    }

    val handleHotspotTap: (RoomHotspot) -> Unit = { hotspot ->
        when (hotspot.actionType) {
            HotspotActionType.PANEL,
            HotspotActionType.UNSORTED_PANEL -> onHotspotPanelOpen(hotspot.id)
            HotspotActionType.DIRECT_GALLERY -> SystemIntentHelper.openGallery(context)
            HotspotActionType.DIRECT_ALARM -> SystemIntentHelper.openAlarms(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main interactive room canvas with percentage-mapped hotspots
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
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            ) {
                // Default minimalist illustrated room background
                Image(
                    painter = painterResource(id = R.drawable.bg_room_default),
                    contentDescription = "Room home screen illustration",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // Render hotspots at percentage coordinates
                uiState.config.hotspots.forEach { hotspot ->
                    if (uiState.isEditMode) {
                        EditableHotspotBox(
                            hotspot = hotspot,
                            roomWidthPx = roomWidthPx,
                            roomHeightPx = roomHeightPx,
                            appCount = uiState.appsFor(hotspot).size,
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
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (hotspot.x * roomWidthPx).roundToInt(),
                                        y = (hotspot.y * roomHeightPx).roundToInt()
                                    )
                                }
                                .size(width = boxWidthDp, height = boxHeightDp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { handleHotspotTap(hotspot) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onEnterEditMode()
                                    }
                                )
                        )
                    }
                }
            }
        }

        // Top minimalist clock/date chip (outside Edit Mode) OR Edit Mode toolbar
        if (!uiState.isEditMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AmbientDateTimePill()

                if (zoomScale > 1.05f) {
                    Surface(
                        shape = CircleShape,
                        color = RoomInk.copy(alpha = 0.72f),
                        modifier = Modifier.clickable {
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
            }
        } else {
            EditModeTopBar(
                onDone = onExitEditMode,
                onAddObject = onAddHotspotOpen,
                onResetClick = { showResetConfirm = true },
                onOpenSetup = onReopenOnboarding,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }

        // Bottom subtle swipe-up zone & All Apps A-Z fallback pill
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
                    color = RoomInk.copy(alpha = 0.62f),
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

        // Animated Panel Overlay (expands outward from tapped hotspot)
        val activeHotspot = uiState.activeHotspot
        if (activeHotspot != null) {
            HotspotPanelOverlay(
                hotspot = activeHotspot,
                apps = uiState.appsFor(activeHotspot),
                allInstalledApps = uiState.installedApps,
                otherPanelHotspots = uiState.panelHotspots.filter { it.id != activeHotspot.id },
                onLaunchApp = { app ->
                    onHotspotPanelClose()
                    SystemIntentHelper.launchApp(context, app.packageName)
                },
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

        // Fast A–Z Searchable All Apps Fallback Drawer
        AnimatedVisibility(
            visible = uiState.showAllAppsSheet,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            AllAppsSheet(
                installedApps = uiState.installedApps,
                panelHotspots = uiState.panelHotspots,
                ownerForPackage = { pkg -> uiState.ownerFor(pkg) },
                onLaunchApp = { app ->
                    onCloseAllApps()
                    SystemIntentHelper.launchApp(context, app.packageName)
                },
                onMoveApp = onMoveAppToHotspot,
                onDismiss = onCloseAllApps
            )
        }
    }

    // Edit Object Dialog (rename, change action, manage apps, delete)
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

    // Add New Hotspot Dialog
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
                Text("This restores the 6 default room objects (TV, Shelf, Painting, Trash Can, Bed, Clock) and their default positions.")
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
private fun AmbientDateTimePill() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000L)
        }
    }
    val timeText = remember(now) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    }
    val dateText = remember(now) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = RoomInk.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RoomInk
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = RoomInk.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun EditModeTopBar(
    onDone: () -> Unit,
    onAddObject: () -> Unit,
    onResetClick: () -> Unit,
    onOpenSetup: () -> Unit,
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
                    IconButton(onClick = onOpenSetup, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Launcher setup & permissions",
                            tint = Color.White
                        )
                    }
                }

                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoomSage),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    )
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
            color = RoomInk.copy(alpha = 0.72f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Drag boxes to move • Drag corner to resize • Tap to rename or edit apps",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}
