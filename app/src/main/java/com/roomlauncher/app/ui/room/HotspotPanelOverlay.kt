package com.roomlauncher.app.ui.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.ui.components.AppIconView
import com.roomlauncher.app.ui.theme.RoomSage
import com.roomlauncher.app.ui.theme.RoomSageContainer
import com.roomlauncher.app.ui.theme.RoomTerracotta
import com.roomlauncher.app.ui.theme.RoomTerracottaContainer
import kotlin.math.roundToInt

@Composable
fun HotspotPanelOverlay(
    hotspot: RoomHotspot,
    apps: List<AppInfo>,
    allInstalledApps: List<AppInfo>,
    otherPanelHotspots: List<RoomHotspot>,
    onLaunchApp: (AppInfo) -> Unit,
    onMoveAppToHotspot: (packageName: String, targetHotspotId: String) -> Unit,
    onRemoveAppFromPanel: (packageName: String) -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenEditHotspot: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val animProgress = remember(hotspot.id) { Animatable(0f) }

    LaunchedEffect(hotspot.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    var draggingApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragFingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetId by remember { mutableStateOf<String?>(null) }
    val dropZoneBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    var showAddAppsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootOffset = coords.positionInRoot()
            }
            .background(Color.Black.copy(alpha = 0.45f * animProgress.value.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(min = 260.dp, max = 580.dp)
                .graphicsLayer {
                    val scale = 0.2f + 0.8f * animProgress.value
                    scaleX = scale
                    scaleY = scale
                    alpha = animProgress.value.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(hotspot.centerX, hotspot.centerY)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // absorb clicks inside panel
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Panel Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hotspot.label,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = buildString {
                            if (hotspot.subtitle.isNotBlank()) {
                                append(hotspot.subtitle)
                                append(" • ")
                            }
                            append("${apps.size} ")
                            append(if (apps.size == 1) "app" else "apps")
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hotspot.actionType == HotspotActionType.PANEL) {
                            IconButton(onClick = { showAddAppsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add apps to ${hotspot.label}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onOpenEditHotspot) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit ${hotspot.label}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close panel"
                            )
                        }
                    }
                }

                // Drag-and-drop destination bar (other panels / Trash Can)
                if (otherPanelHotspots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (draggingApp != null) {
                            "Drop \"${draggingApp?.label}\" on an object to move it:"
                        } else {
                            "Hold & drag any app onto another object to move:"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (draggingApp != null) RoomTerracotta else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(otherPanelHotspots, key = { it.id }) { target ->
                            val isHovered = hoveredTargetId == target.id
                            val isDragging = draggingApp != null
                            val bgColor = when {
                                isHovered -> RoomTerracotta
                                isDragging -> RoomTerracottaContainer
                                else -> RoomSageContainer.copy(alpha = 0.65f)
                            }
                            val textColor = when {
                                isHovered -> Color.White
                                isDragging -> MaterialTheme.colorScheme.onSurface
                                else -> RoomSage
                            }
                            val borderColor = when {
                                isHovered -> RoomTerracotta
                                isDragging -> RoomTerracotta.copy(alpha = 0.7f)
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        dropZoneBounds[target.id] = coords.boundsInRoot()
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgColor)
                                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = target.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Apps Grid
                if (apps.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (hotspot.actionType == HotspotActionType.UNSORTED_PANEL) {
                                "All apps are neatly organized!"
                            } else {
                                "No apps placed in ${hotspot.label} yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (hotspot.actionType == HotspotActionType.PANEL) {
                            TextButton(onClick = { showAddAppsDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Apps to ${hotspot.label}")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            DraggablePanelAppTile(
                                app = app,
                                isBeingDragged = draggingApp?.packageName == app.packageName,
                                showQuickRemove = hotspot.actionType == HotspotActionType.PANEL,
                                onTap = { onLaunchApp(app) },
                                onRemoveToTrash = { onRemoveAppFromPanel(app.packageName) },
                                onDragStart = { startRootPos ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingApp = app
                                    dragFingerRootPos = startRootPos
                                    hoveredTargetId = findDropTarget(startRootPos, dropZoneBounds)
                                },
                                onDragMove = { currentRootPos ->
                                    dragFingerRootPos = currentRootPos
                                    hoveredTargetId = findDropTarget(currentRootPos, dropZoneBounds)
                                },
                                onDragEnd = {
                                    val targetId = hoveredTargetId
                                    val droppedApp = draggingApp
                                    if (targetId != null && droppedApp != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onMoveAppToHotspot(droppedApp.packageName, targetId)
                                    }
                                    draggingApp = null
                                    hoveredTargetId = null
                                },
                                onDragCancel = {
                                    draggingApp = null
                                    hoveredTargetId = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Panel Footer — A-Z List Fallback
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onOpenAllApps) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All Apps (A–Z)")
                    }
                    if (hotspot.actionType == HotspotActionType.PANEL) {
                        TextButton(onClick = { showAddAppsDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Apps")
                        }
                    }
                }
            }
        }

        // Floating dragged app icon following the user's finger across the overlay
        val activeDragApp = draggingApp
        if (activeDragApp != null) {
            val localPos = dragFingerRootPos - rootOffset
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (localPos.x - 32.dp.toPx()).roundToInt(),
                            (localPos.y - 44.dp.toPx()).roundToInt()
                        )
                    }
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(6.dp)
            ) {
                AppIconView(app = activeDragApp, size = 52.dp)
            }
        }
    }

    if (showAddAppsDialog && hotspot.actionType == HotspotActionType.PANEL) {
        AddAppsToPanelDialog(
            hotspotLabel = hotspot.label,
            allApps = allInstalledApps,
            alreadyInPanel = apps.map { it.packageName }.toSet(),
            onAddApp = { pkg ->
                onMoveAppToHotspot(pkg, hotspot.id)
            },
            onDismiss = { showAddAppsDialog = false }
        )
    }
}

private fun findDropTarget(point: Offset, zones: Map<String, Rect>): String? {
    return zones.entries.firstOrNull { (_, rect) ->
        rect.inflate(16f).contains(point)
    }?.key
}

@Composable
private fun DraggablePanelAppTile(
    app: AppInfo,
    isBeingDragged: Boolean,
    showQuickRemove: Boolean,
    onTap: () -> Unit,
    onRemoveToTrash: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var tileRootPosition by remember { mutableStateOf(Offset.Zero) }
    var currentPointerInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                tileRootPosition = coords.positionInRoot()
            }
            .graphicsLayer {
                alpha = if (isBeingDragged) 0.28f else 1f
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onTap)
            .pointerInput(app.packageName) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localStart ->
                        currentPointerInRoot = tileRootPosition + localStart
                        onDragStart(currentPointerInRoot)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentPointerInRoot += dragAmount
                        onDragMove(currentPointerInRoot)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragCancel()
                    }
                )
            }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                AppIconView(app = app, size = 48.dp)
                if (showQuickRemove) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                            .clickable(onClick = onRemoveToTrash)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Send ${app.label} to Trash Can",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddAppsToPanelDialog(
    hotspotLabel: String,
    allApps: List<AppInfo>,
    alreadyInPanel: Set<String>,
    onAddApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val candidates = remember(allApps, alreadyInPanel, query) {
        val q = query.trim().lowercase()
        allApps.filter { app ->
            app.packageName !in alreadyInPanel &&
                (q.isEmpty() || app.label.lowercase().contains(q))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Apps to $hotspotLabel") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Filter apps…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (candidates.isEmpty()) {
                    Text(
                        text = "No matching apps available to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(candidates, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onAddApp(app.packageName) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconView(app = app, size = 36.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add ${app.label}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
