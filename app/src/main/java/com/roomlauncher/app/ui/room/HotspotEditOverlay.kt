package com.roomlauncher.app.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.ui.RoomLauncherViewModel
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.LocalRoomPalette
import com.roomlauncher.app.ui.theme.RoomEditAccent
import com.roomlauncher.app.ui.theme.RoomInk
import com.roomlauncher.app.ui.theme.RoomSage
import com.roomlauncher.app.ui.theme.RoomStyleId
import kotlin.math.roundToInt

/**
 * Renders both the actual illustrated room object AND its semi-transparent drag/resize handle
 * frame so moving or resizing in Edit Mode moves the real furniture object live!
 */
@Composable
fun EditableHotspotBox(
    hotspot: RoomHotspot,
    roomWidthPx: Float,
    roomHeightPx: Float,
    cameraScale: Float = ROOM_CAMERA_SCALE,
    cameraAnchorY: Float = ROOM_CAMERA_ANCHOR_Y,
    appCount: Int,
    roomStyle: RoomStyleId,
    gdSubTheme: GeometryDashSubTheme = GeometryDashSubTheme.SONIC_WAVE,
    weatherState: WeatherState,
    weatherEnabled: Boolean,
    frameImageBitmap: ImageBitmap?,
    currentMinutesOfDay: Int,
    onMoveFinished: (newX: Float, newY: Float) -> Unit,
    onResizeFinished: (newWidth: Float, newHeight: Float) -> Unit,
    onTapEdit: () -> Unit
) {
    val density = LocalDensity.current
    val palette = LocalRoomPalette.current

    var localX by remember(hotspot.id) { mutableFloatStateOf(hotspot.x) }
    var localY by remember(hotspot.id) { mutableFloatStateOf(hotspot.y) }
    var localW by remember(hotspot.id) { mutableFloatStateOf(hotspot.width) }
    var localH by remember(hotspot.id) { mutableFloatStateOf(hotspot.height) }

    LaunchedEffect(hotspot.x, hotspot.y, hotspot.width, hotspot.height) {
        localX = hotspot.x
        localY = hotspot.y
        localW = hotspot.width
        localH = hotspot.height
    }

    val safeScale = cameraScale.coerceAtLeast(0.5f)
    val viewX = 0.5f + (localX - 0.5f) * safeScale
    val viewY = cameraAnchorY + (localY - cameraAnchorY) * safeScale
    val widthDp = with(density) { (localW * safeScale * roomWidthPx).toDp() }
    val heightDp = with(density) { (localH * safeScale * roomHeightPx).toDp() }
    val accent = when (hotspot.actionType) {
        HotspotActionType.UNSORTED_PANEL -> RoomEditAccent
        HotspotActionType.DIRECT_GALLERY,
        HotspotActionType.DIRECT_ALARM,
        HotspotActionType.DIRECT_WEATHER -> Color(0xFF4F6D7A)
        HotspotActionType.PANEL -> RoomSage
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (viewX * roomWidthPx).roundToInt(),
                    y = (viewY * roomHeightPx).roundToInt()
                )
            }
            .size(width = widthDp, height = heightDp)
            .pointerInput(hotspot.id, roomWidthPx, roomHeightPx, safeScale) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (roomWidthPx > 0f && roomHeightPx > 0f) {
                            val dx = dragAmount.x / (roomWidthPx * safeScale)
                            val dy = dragAmount.y / (roomHeightPx * safeScale)
                            localX = (localX + dx).coerceIn(0f, (1f - localW).coerceAtLeast(0f))
                            localY = (localY + dy).coerceIn(0f, (1f - localH).coerceAtLeast(0f))
                        }
                    },
                    onDragEnd = {
                        onMoveFinished(localX, localY)
                    }
                )
            }
            .clickable(onClick = onTapEdit)
    ) {
        // The actual illustrated object moves & resizes live with the bounding box!
        RoomObjectGraphic(
            visualType = hotspot.visualType,
            palette = palette,
            roomStyle = roomStyle,
            gdSubTheme = gdSubTheme,
            weatherState = weatherState,
            weatherEnabled = weatherEnabled,
            frameImageBitmap = frameImageBitmap,
            currentMinutesOfDay = currentMinutesOfDay,
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent edit bounding overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.22f))
                .border(2.dp, accent, RoundedCornerShape(14.dp))
        )

        // Object label chip & count
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RoomInk.copy(alpha = 0.80f))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = hotspot.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit ${hotspot.label}",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(11.dp)
                )
            }
            val badgeText = when (hotspot.actionType) {
                HotspotActionType.PANEL -> "$appCount apps"
                HotspotActionType.UNSORTED_PANEL -> "$appCount unsorted"
                HotspotActionType.DIRECT_GALLERY -> "Photos"
                HotspotActionType.DIRECT_ALARM -> "Alarms"
                HotspotActionType.DIRECT_WEATHER -> "Weather"
            }
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1
            )
        }

        // Bottom-right corner resize handle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(accent)
                .border(1.5.dp, Color.White, CircleShape)
                .pointerInput(hotspot.id, roomWidthPx, roomHeightPx, safeScale) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (roomWidthPx > 0f && roomHeightPx > 0f) {
                                val dw = dragAmount.x / (roomWidthPx * safeScale)
                                val dh = dragAmount.y / (roomHeightPx * safeScale)
                                val maxW = (1f - localX).coerceAtLeast(RoomLauncherViewModel.MIN_HOTSPOT_WIDTH)
                                val maxH = (1f - localY).coerceAtLeast(RoomLauncherViewModel.MIN_HOTSPOT_HEIGHT)
                                localW = (localW + dw).coerceIn(RoomLauncherViewModel.MIN_HOTSPOT_WIDTH, maxW)
                                localH = (localH + dh).coerceIn(RoomLauncherViewModel.MIN_HOTSPOT_HEIGHT, maxH)
                            }
                        },
                        onDragEnd = {
                            onResizeFinished(localW, localH)
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Icons.Rounded.OpenInFull,
                contentDescription = "Resize ${hotspot.label}",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun EditHotspotDialog(
    hotspot: RoomHotspot,
    canDelete: Boolean,
    onSave: (label: String, subtitle: String, actionType: HotspotActionType, visualType: RoomObjectVisual) -> Unit,
    onOpenAppsPanel: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember(hotspot.id) { mutableStateOf(hotspot.label) }
    var subtitle by remember(hotspot.id) { mutableStateOf(hotspot.subtitle) }
    var actionType by remember(hotspot.id) { mutableStateOf(hotspot.actionType) }
    var visualType by remember(hotspot.id) { mutableStateOf(hotspot.visualType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Object") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Object Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Furniture Appearance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FurnitureVisualPicker(
                    selected = visualType,
                    onSelect = { visualType = it }
                )

                Text(
                    text = "Action on Tap",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ActionTypePicker(
                    selected = actionType,
                    allowUnsortedChange = canDelete || actionType != HotspotActionType.UNSORTED_PANEL,
                    onSelect = { actionType = it }
                )

                if (actionType == HotspotActionType.PANEL || actionType == HotspotActionType.UNSORTED_PANEL) {
                    OutlinedButton(
                        onClick = onOpenAppsPanel,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Apps, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage & Drag Apps in ${label.ifBlank { hotspot.label }}")
                    }
                }

                if (canDelete) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Object (apps move to Trash Can)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank()) {
                        onSave(label, subtitle, actionType, visualType)
                        onDismiss()
                    }
                },
                enabled = label.isNotBlank()
            ) {
                Text("Save")
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
fun AddHotspotDialog(
    roomName: String = "Room",
    onAdd: (label: String, subtitle: String, actionType: HotspotActionType, visualType: RoomObjectVisual) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf(HotspotActionType.PANEL) }
    var visualType by remember { mutableStateOf(RoomObjectVisual.SOFA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Furniture to $roomName") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Object Name (e.g. Sofa, Fridge, PC)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Category Subtitle (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Furniture Style",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FurnitureVisualPicker(
                    selected = visualType,
                    onSelect = { chosen ->
                        visualType = chosen
                        if (label.isBlank()) {
                            label = chosen.displayLabel()
                        }
                    }
                )
                Text(
                    text = "Action on Tap",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ActionTypePicker(
                    selected = actionType,
                    allowUnsortedChange = true,
                    onSelect = { actionType = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank()) {
                        onAdd(label, subtitle, actionType, visualType)
                    }
                },
                enabled = label.isNotBlank()
            ) {
                Text("Add Object")
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
private fun FurnitureVisualPicker(
    selected: RoomObjectVisual,
    onSelect: (RoomObjectVisual) -> Unit
) {
    val visuals = remember { RoomObjectVisual.entries.toList() }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(visuals, key = { it.name }) { visual ->
            FilterChip(
                selected = selected == visual,
                onClick = { onSelect(visual) },
                label = {
                    Text(
                        text = visual.displayLabel(),
                        fontWeight = if (selected == visual) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

private fun RoomObjectVisual.displayLabel(): String = when (this) {
    RoomObjectVisual.WINDOW -> "Window"
    RoomObjectVisual.PAINTING -> "Picture Frame"
    RoomObjectVisual.CLOCK -> "Clock"
    RoomObjectVisual.STICKY_NOTE -> "Sticky Note"
    RoomObjectVisual.TV -> "TV"
    RoomObjectVisual.CONSOLE -> "Console"
    RoomObjectVisual.SHELF -> "Shelf"
    RoomObjectVisual.BED -> "Bed"
    RoomObjectVisual.NIGHTSTAND -> "Night Table"
    RoomObjectVisual.TRASH_CAN -> "Trash Can"
    RoomObjectVisual.SOFA -> "Sofa"
    RoomObjectVisual.FIREPLACE -> "Fireplace"
    RoomObjectVisual.COFFEE_TABLE -> "Coffee Table"
    RoomObjectVisual.VINYL_PLAYER -> "Vinyl Player"
    RoomObjectVisual.FRIDGE -> "Fridge"
    RoomObjectVisual.COFFEE_MAKER -> "Coffee Maker"
    RoomObjectVisual.STOVE -> "Stove"
    RoomObjectVisual.KITCHEN_COUNTER -> "Kitchen Counter"
    RoomObjectVisual.GAMING_PC -> "Gaming PC"
    RoomObjectVisual.ARCADE_CABINET -> "Arcade Cabinet"
    RoomObjectVisual.STREAMING_DESK -> "Streaming Desk"
    RoomObjectVisual.TROPHY_SHELF -> "Trophy Shelf"
    RoomObjectVisual.CUSTOM -> "Custom Box"
}

@Composable
private fun ActionTypePicker(
    selected: HotspotActionType,
    allowUnsortedChange: Boolean,
    onSelect: (HotspotActionType) -> Unit
) {
    val options = listOf(
        HotspotActionType.PANEL to "App Panel",
        HotspotActionType.UNSORTED_PANEL to "Unsorted",
        HotspotActionType.DIRECT_GALLERY to "Gallery",
        HotspotActionType.DIRECT_ALARM to "Alarms",
        HotspotActionType.DIRECT_WEATHER to "Weather"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { (type, title) ->
                    val enabled = allowUnsortedChange || type == selected
                    FilterChip(
                        selected = selected == type,
                        onClick = { if (enabled) onSelect(type) },
                        enabled = enabled,
                        label = {
                            Text(
                                text = title,
                                fontWeight = if (selected == type) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
