package com.roomlauncher.app.ui.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.WebShortcut
import com.roomlauncher.app.ui.components.AppIconView
import com.roomlauncher.app.ui.theme.LocalRoomPalette
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun HotspotPanelOverlay(
    hotspot: RoomHotspot,
    apps: List<AppInfo>,
    allInstalledApps: List<AppInfo>,
    otherPanelHotspots: List<RoomHotspot>,
    webShortcuts: List<WebShortcut>,
    useQuickAnimations: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenWebShortcut: (WebShortcut) -> Unit,
    onAddWebShortcut: (title: String, url: String) -> Unit,
    onDeleteWebShortcut: (String) -> Unit,
    onMoveAppToHotspot: (packageName: String, targetHotspotId: String) -> Unit,
    onRemoveAppFromPanel: (packageName: String) -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenEditHotspot: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val palette = LocalRoomPalette.current
    val density = LocalDensity.current
    val animProgress = remember(hotspot.id, useQuickAnimations) { Animatable(0f) }

    val richVisual = !useQuickAnimations
    val isTvRich = richVisual && hotspot.visualType == RoomObjectVisual.TV
    val isBookRich = richVisual && hotspot.visualType == RoomObjectVisual.SHELF
    val isTrashRich = richVisual && hotspot.visualType == RoomObjectVisual.TRASH_CAN
    val isConsoleRich = richVisual && hotspot.visualType == RoomObjectVisual.CONSOLE
    val isStickyRich = hotspot.visualType == RoomObjectVisual.STICKY_NOTE

    LaunchedEffect(hotspot.id, useQuickAnimations) {
        animProgress.snapTo(0f)
        when {
            useQuickAnimations -> {
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            isTvRich -> {
                // Let the room camera zoom squarely onto the TV first, then power the TV screen ON!
                delay(170L)
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                )
            }
            isBookRich -> {
                // Stage 1 (0..0.46): book pops out from Shelf to center; Stage 2 (0.46..1): opens in center
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 660, easing = FastOutSlowInEasing)
                )
            }
            isTrashRich -> {
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            else -> {
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    var draggingApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragFingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetId by remember { mutableStateOf<String?>(null) }
    val dropZoneBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var isEditingItems by remember(hotspot.id) { mutableStateOf(false) }
    var showAddAppsDialog by remember { mutableStateOf(false) }
    var showAddWebShortcutDialog by remember { mutableStateOf(false) }

    val p = animProgress.value.coerceIn(0f, 1.25f)
    val bookFlyPhase = (p / 0.46f).coerceIn(0f, 1f)
    val bookOpenPhase = ((p - 0.44f) / 0.56f).coerceIn(0f, 1f)

    // Choose tailored surface styling per object when Rich Animations are active
    val containerColor = when {
        isTvRich -> Color(0xFF14191F)
        isConsoleRich -> Color(0xFF161B24)
        isStickyRich -> if (palette.isDark) Color(0xFF3A331C) else Color(0xFFFFF5C2)
        isBookRich -> if (palette.isDark) Color(0xFF26211B) else Color(0xFFFBF6EA)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentOnContainer = when {
        isTvRich || isConsoleRich -> Color(0xFFF3F6FA)
        isStickyRich -> if (palette.isDark) Color(0xFFFFF3C4) else Color(0xFF3A2F12)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val mutedOnContainer = contentOnContainer.copy(alpha = 0.72f)
    val borderColor = when {
        isTvRich -> Color(0xFF2D3846)
        isConsoleRich -> palette.consoleAccent.copy(alpha = 0.55f)
        isBookRich -> palette.woodMid.copy(alpha = 0.7f)
        isStickyRich -> palette.stickyNoteFold.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootOffset = coords.positionInRoot()
                rootSize = coords.size
            }
            .background(Color.Black.copy(alpha = (if (isTvRich) 0.42f else 0.48f) * p.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(
                when {
                    isTvRich -> 16.dp
                    isBookRich -> 18.dp
                    isStickyRich -> 14.dp
                    else -> 28.dp
                }
            ),
            color = containerColor,
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(if (isTvRich) 0.90f else 0.93f)
                .heightIn(min = 270.dp, max = if (isTvRich) 520.dp else 600.dp)
                .border(
                    width = when {
                        isTvRich -> 8.dp
                        borderColor != Color.Transparent -> 2.5.dp
                        else -> 0.dp
                    },
                    color = if (isTvRich) palette.tvBezel else borderColor,
                    shape = RoundedCornerShape(
                        when {
                            isTvRich -> 16.dp
                            isBookRich -> 18.dp
                            isStickyRich -> 14.dp
                            else -> 28.dp
                        }
                    )
                )
                .graphicsLayer {
                    when {
                        useQuickAnimations -> {
                            val scale = 0.2f + 0.8f * p
                            scaleX = scale
                            scaleY = scale
                            alpha = p.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(hotspot.centerX, hotspot.centerY)
                        }
                        isTvRich -> {
                            // Classic CRT/OLED TV Power-On centered inside the zoomed TV bezel!
                            val widthPhase = (p / 0.40f).coerceIn(0.04f, 1f)
                            val heightPhase = ((p - 0.28f) / 0.72f).coerceIn(0.025f, 1f)
                            scaleX = widthPhase
                            scaleY = heightPhase
                            alpha = (p * 3.0f).coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        isBookRich -> {
                            // Stage 2: Open book spread unfolds right in the middle of the screen!
                            cameraDistance = 16f * density.density
                            scaleX = 0.48f + 0.52f * bookOpenPhase
                            scaleY = 0.82f + 0.18f * bookOpenPhase
                            alpha = bookOpenPhase
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        isTrashRich -> {
                            // Pops straight up out of the Trash Can!
                            val clamped = p.coerceIn(0f, 1.15f)
                            translationY = (1f - clamped) * 320f
                            scaleX = 0.35f + 0.65f * clamped.coerceIn(0f, 1f)
                            scaleY = 0.25f + 0.75f * clamped.coerceIn(0f, 1f)
                            alpha = (p * 1.8f).coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(hotspot.centerX, 0.95f)
                        }
                        isConsoleRich -> {
                            translationY = (1f - p.coerceIn(0f, 1f)) * 180f
                            scaleX = 0.85f + 0.15f * p.coerceIn(0f, 1f)
                            scaleY = 0.85f + 0.15f * p.coerceIn(0f, 1f)
                            alpha = p.coerceIn(0f, 1f)
                        }
                        isStickyRich -> {
                            rotationZ = (1f - p.coerceIn(0f, 1f)) * -8f
                            val scale = 0.35f + 0.65f * p.coerceIn(0f, 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = p.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(hotspot.centerX, hotspot.centerY)
                        }
                        else -> {
                            val scale = 0.2f + 0.8f * p
                            scaleX = scale
                            scaleY = scale
                            alpha = p.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(hotspot.centerX, hotspot.centerY)
                        }
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Console glowing accent LED bar
                    if (isConsoleRich) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(4.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(CircleShape)
                                .background(palette.consoleAccent)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Trash Can popped lid accent bar
                    if (isTrashRich) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(palette.trashRim)
                            )
                        }
                    }

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
                                color = contentOnContainer,
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
                                color = mutedOnContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isStickyRich) {
                                IconButton(onClick = { showAddWebShortcutDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddLink,
                                        contentDescription = "Add web shortcut",
                                        tint = contentOnContainer
                                    )
                                }
                            }
                            if (hotspot.actionType == HotspotActionType.PANEL) {
                                IconButton(onClick = { showAddAppsDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Add apps to ${hotspot.label}",
                                        tint = contentOnContainer
                                    )
                                }
                            }
                            if (isEditingItems) {
                                IconButton(onClick = onOpenEditHotspot) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = "Customize ${hotspot.label} object",
                                        tint = mutedOnContainer
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (hotspot.actionType == HotspotActionType.PANEL || isStickyRich) {
                                        isEditingItems = !isEditingItems
                                    } else {
                                        onOpenEditHotspot()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isEditingItems) Icons.Rounded.Check else Icons.Rounded.Edit,
                                    contentDescription = if (isEditingItems) "Done editing icons" else "Edit ${hotspot.label}",
                                    tint = if (isEditingItems) MaterialTheme.colorScheme.secondary else mutedOnContainer
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close panel",
                                    tint = contentOnContainer
                                )
                            }
                        }
                    }

                    // Drag-and-drop destination bar (other panels / Trash Can)
                    if (otherPanelHotspots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (draggingApp != null) {
                                "Drop \"${draggingApp?.label}\" on an object to move it:"
                            } else {
                                "Hold & drag any app onto another object to move:"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (draggingApp != null) MaterialTheme.colorScheme.secondary else mutedOnContainer
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
                                    isHovered -> MaterialTheme.colorScheme.secondary
                                    isDragging -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> contentOnContainer.copy(alpha = 0.10f)
                                }
                                val textColor = when {
                                    isHovered -> MaterialTheme.colorScheme.onSecondary
                                    isDragging -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> contentOnContainer
                                }

                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            dropZoneBounds[target.id] = coords.boundsInRoot()
                                        }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgColor)
                                        .border(
                                            1.dp,
                                            if (isHovered || isDragging) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
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

                    // Sticky Note Web Shortcuts Section!
                    if (isStickyRich) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = contentOnContainer.copy(alpha = 0.18f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pinned Web Shortcuts",
                                style = MaterialTheme.typography.labelLarge,
                                color = contentOnContainer
                            )
                            TextButton(
                                onClick = { showAddWebShortcutDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Rounded.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add URL", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (webShortcuts.isEmpty()) {
                            Text(
                                text = "Pin your favorite websites here for one-tap access.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = mutedOnContainer,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(webShortcuts, key = { it.id }) { shortcut ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(contentOnContainer.copy(alpha = 0.12f))
                                                .clickable { onOpenWebShortcut(shortcut) }
                                                .padding(
                                                    start = 10.dp,
                                                    end = if (isEditingItems) 4.dp else 10.dp,
                                                    top = 6.dp,
                                                    bottom = 6.dp
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Language,
                                                contentDescription = null,
                                                tint = contentOnContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = shortcut.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = contentOnContainer
                                            )
                                            if (isEditingItems) {
                                                IconButton(
                                                    onClick = { onDeleteWebShortcut(shortcut.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Remove ${shortcut.title}",
                                                        tint = mutedOnContainer,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = contentOnContainer.copy(alpha = 0.18f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Content Body: Either Open Two-Page Book Spread (for Shelf!) or Standard 4-Column Grid
                    if (apps.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
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
                                color = mutedOnContainer,
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
                    } else if (isBookRich) {
                        // Shelf Two-Page Open Book Spread!
                        ShelfBookSpread(
                            apps = apps,
                            textColor = contentOnContainer,
                            mutedColor = mutedOnContainer,
                            spineColor = palette.woodMid.copy(alpha = 0.45f),
                            draggingPackage = draggingApp?.packageName,
                            showQuickRemove = isEditingItems && hotspot.actionType == HotspotActionType.PANEL,
                            onTapApp = onLaunchApp,
                            onRemoveToTrash = onRemoveAppFromPanel,
                            onDragStart = { app, pos ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                draggingApp = app
                                dragFingerRootPos = pos
                                hoveredTargetId = findDropTarget(pos, dropZoneBounds)
                            },
                            onDragMove = { pos ->
                                dragFingerRootPos = pos
                                hoveredTargetId = findDropTarget(pos, dropZoneBounds)
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
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            items(apps, key = { it.packageName }) { app ->
                                DraggablePanelAppTile(
                                    app = app,
                                    labelColor = contentOnContainer,
                                    isBeingDragged = draggingApp?.packageName == app.packageName,
                                    showQuickRemove = isEditingItems && hotspot.actionType == HotspotActionType.PANEL,
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

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = contentOnContainer.copy(alpha = 0.18f))

                    // Footer — A–Z List Fallback + TV Power LED
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
                                tint = contentOnContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Apps (A–Z)", color = contentOnContainer)
                        }

                        if (isTvRich) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF7CC49E))
                                )
                                Text(
                                    text = "ON",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF7CC49E)
                                )
                            }
                        }

                        if (hotspot.actionType == HotspotActionType.PANEL) {
                            TextButton(onClick = { showAddAppsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = contentOnContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Apps", color = contentOnContainer)
                            }
                        }
                    }
                }

                // Bright white CRT flash veil during TV Power-On opening phase!
                if (isTvRich && p < 0.72f) {
                    val flashAlpha = (1f - (p / 0.72f)).coerceIn(0f, 0.92f)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = flashAlpha))
                    )
                }
            }
        }

        // Stage 1 -> Stage 2 Closed Hardcover Book popping out from the Shelf and swinging open in center!
        if (isBookRich && p < 0.92f) {
            val startDx = (hotspot.centerX - 0.5f) * rootSize.width.toFloat()
            val startDy = (hotspot.centerY - 0.5f) * rootSize.height.toFloat()
            val bookCoverColor = palette.duvetPrimary
            val bookSpineColor = palette.woodDark
            val coverFadeOut = if (bookOpenPhase > 0.45f) {
                (1f - ((bookOpenPhase - 0.45f) / 0.55f)).coerceIn(0f, 1f)
            } else {
                1f
            }

            Box(
                modifier = Modifier
                    .width(210.dp)
                    .height(290.dp)
                    .graphicsLayer {
                        cameraDistance = 16f * density.density
                        translationX = startDx * (1f - bookFlyPhase)
                        translationY = startDy * (1f - bookFlyPhase)
                        val flyScale = 0.24f + 0.76f * bookFlyPhase
                        scaleX = flyScale
                        scaleY = flyScale
                        rotationY = -165f * bookOpenPhase
                        transformOrigin = TransformOrigin(0.04f, 0.5f)
                        alpha = coverFadeOut
                    }
                    .shadow(16.dp, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                    .background(bookCoverColor)
                    .border(
                        2.dp,
                        Color(0xFFE2B97B).copy(alpha = 0.75f),
                        RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
                // Book spine band on left edge
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(22.dp)
                        .fillMaxHeight()
                        .background(bookSpineColor)
                )
                // Cream pages edge on right
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(7.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFFF6EFE0))
                )
                // Embossed gold title on book cover
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 24.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(2.dp)
                            .background(Color(0xFFE2B97B))
                    )
                    Text(
                        text = hotspot.label,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFAF4E6),
                        textAlign = TextAlign.Center
                    )
                    if (hotspot.subtitle.isNotBlank()) {
                        Text(
                            text = hotspot.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFAF4E6).copy(alpha = 0.78f),
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(2.dp)
                            .background(Color(0xFFE2B97B))
                    )
                }
            }
        }

        // Floating dragged app icon
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
            onAddApp = { pkg -> onMoveAppToHotspot(pkg, hotspot.id) },
            onDismiss = { showAddAppsDialog = false }
        )
    }

    if (showAddWebShortcutDialog) {
        AddWebShortcutDialog(
            onAdd = { title, url ->
                onAddWebShortcut(title, url)
                showAddWebShortcutDialog = false
            },
            onDismiss = { showAddWebShortcutDialog = false }
        )
    }
}

/**
 * Splits Shelf apps into a crisp two-page open book spread (4 apps on Left Page, 4 on Right Page)
 * with pagination controls when there are more than 8 apps.
 */
@Composable
private fun ShelfBookSpread(
    apps: List<AppInfo>,
    textColor: Color,
    mutedColor: Color,
    spineColor: Color,
    draggingPackage: String?,
    showQuickRemove: Boolean,
    onTapApp: (AppInfo) -> Unit,
    onRemoveToTrash: (String) -> Unit,
    onDragStart: (AppInfo, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appsPerSpread = 8
    val totalSpreads = ((apps.size - 1) / appsPerSpread + 1).coerceAtLeast(1)
    var currentSpread by remember(apps.size) { mutableIntStateOf(0) }
    val safeSpread = currentSpread.coerceIn(0, totalSpreads - 1)

    val startIdx = safeSpread * appsPerSpread
    val spreadApps = apps.subList(startIdx, minOf(startIdx + appsPerSpread, apps.size))
    val leftPageApps = spreadApps.take(4)
    val rightPageApps = spreadApps.drop(4)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 210.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Book Page
            BookPageColumn(
                pageNumber = safeSpread * 2 + 1,
                apps = leftPageApps,
                textColor = textColor,
                mutedColor = mutedColor,
                draggingPackage = draggingPackage,
                showQuickRemove = showQuickRemove,
                onTapApp = onTapApp,
                onRemoveToTrash = onRemoveToTrash,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                modifier = Modifier.weight(1f)
            )

            // Center Book Spine Divider
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(210.dp)
                    .clip(CircleShape)
                    .background(spineColor)
            )

            // Right Book Page
            BookPageColumn(
                pageNumber = safeSpread * 2 + 2,
                apps = rightPageApps,
                textColor = textColor,
                mutedColor = mutedColor,
                draggingPackage = draggingPackage,
                showQuickRemove = showQuickRemove,
                onTapApp = onTapApp,
                onRemoveToTrash = onRemoveToTrash,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                modifier = Modifier.weight(1f)
            )
        }

        if (totalSpreads > 1) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { if (safeSpread > 0) currentSpread = safeSpread - 1 },
                    enabled = safeSpread > 0
                ) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous page")
                    Text("Prev")
                }
                Text(
                    text = "Pages ${safeSpread * 2 + 1}–${safeSpread * 2 + 2} of ${totalSpreads * 2}",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor
                )
                TextButton(
                    onClick = { if (safeSpread < totalSpreads - 1) currentSpread = safeSpread + 1 },
                    enabled = safeSpread < totalSpreads - 1
                ) {
                    Text("Next")
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next page")
                }
            }
        }
    }
}

@Composable
private fun BookPageColumn(
    pageNumber: Int,
    apps: List<AppInfo>,
    textColor: Color,
    mutedColor: Color,
    draggingPackage: String?,
    showQuickRemove: Boolean,
    onTapApp: (AppInfo) -> Unit,
    onRemoveToTrash: (String) -> Unit,
    onDragStart: (AppInfo, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (apps.isEmpty()) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(
                text = "Blank page",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor.copy(alpha = 0.6f)
            )
        } else {
            apps.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pair.forEach { app ->
                        Box(modifier = Modifier.weight(1f)) {
                            DraggablePanelAppTile(
                                app = app,
                                labelColor = textColor,
                                isBeingDragged = draggingPackage == app.packageName,
                                showQuickRemove = showQuickRemove,
                                onTap = { onTapApp(app) },
                                onRemoveToTrash = { onRemoveToTrash(app.packageName) },
                                onDragStart = { pos -> onDragStart(app, pos) },
                                onDragMove = onDragMove,
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragCancel
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f, fill = false))
        Text(
            text = "— $pageNumber —",
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor.copy(alpha = 0.65f)
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
    labelColor: Color,
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
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box {
                AppIconView(app = app, size = 46.dp)
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
                color = labelColor,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddWebShortcutDialog(
    onAdd: (title: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Web Shortcut") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Label (e.g. GitHub, News)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (e.g. github.com)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, url) },
                enabled = title.isNotBlank() && url.isNotBlank()
            ) {
                Text("Pin Shortcut")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
