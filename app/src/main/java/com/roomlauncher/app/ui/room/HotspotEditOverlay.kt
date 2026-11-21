package com.roomlauncher.app.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.ui.RoomLauncherViewModel
import com.roomlauncher.app.ui.theme.RoomEditAccent
import com.roomlauncher.app.ui.theme.RoomInk
import com.roomlauncher.app.ui.theme.RoomSage
import kotlin.math.roundToInt

@Composable
fun EditableHotspotBox(
    hotspot: RoomHotspot,
    roomWidthPx: Float,
    roomHeightPx: Float,
    appCount: Int,
    onMoveFinished: (newX: Float, newY: Float) -> Unit,
    onResizeFinished: (newWidth: Float, newHeight: Float) -> Unit,
    onTapEdit: () -> Unit
) {
    val density = LocalDensity.current

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

    val widthDp = with(density) { (localW * roomWidthPx).toDp() }
    val heightDp = with(density) { (localH * roomHeightPx).toDp() }
    val accent = when (hotspot.actionType) {
        HotspotActionType.UNSORTED_PANEL -> RoomEditAccent
        HotspotActionType.DIRECT_GALLERY,
        HotspotActionType.DIRECT_ALARM -> Color(0xFF4F6D7A)
        HotspotActionType.PANEL -> RoomSage
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (localX * roomWidthPx).roundToInt(),
                    y = (localY * roomHeightPx).roundToInt()
                )
            }
            .size(width = widthDp, height = heightDp)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.28f))
            .border(2.dp, accent, RoundedCornerShape(16.dp))
            .pointerInput(hotspot.id, roomWidthPx, roomHeightPx) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (roomWidthPx > 0f && roomHeightPx > 0f) {
                            val dx = dragAmount.x / roomWidthPx
                            val dy = dragAmount.y / roomHeightPx
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
        // Object label chip & count
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RoomInk.copy(alpha = 0.78f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = hotspot.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit ${hotspot.label}",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
            }
            val badgeText = when (hotspot.actionType) {
                HotspotActionType.PANEL -> "$appCount apps"
                HotspotActionType.UNSORTED_PANEL -> "$appCount unsorted"
                HotspotActionType.DIRECT_GALLERY -> "Gallery"
                HotspotActionType.DIRECT_ALARM -> "Alarms"
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
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(accent)
                .border(1.5.dp, Color.White, CircleShape)
                .pointerInput(hotspot.id, roomWidthPx, roomHeightPx) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (roomWidthPx > 0f && roomHeightPx > 0f) {
                                val dw = dragAmount.x / roomWidthPx
                                val dh = dragAmount.y / roomHeightPx
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
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun EditHotspotDialog(
    hotspot: RoomHotspot,
    canDelete: Boolean,
    onSave: (label: String, subtitle: String, actionType: HotspotActionType) -> Unit,
    onOpenAppsPanel: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember(hotspot.id) { mutableStateOf(hotspot.label) }
    var subtitle by remember(hotspot.id) { mutableStateOf(hotspot.subtitle) }
    var actionType by remember(hotspot.id) { mutableStateOf(hotspot.actionType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Object") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                        onSave(label, subtitle, actionType)
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
    onAdd: (label: String, subtitle: String, actionType: HotspotActionType) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf(HotspotActionType.PANEL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Room Object") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Object Name (e.g. Desk, Lamp)") },
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
                        onAdd(label, subtitle, actionType)
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
private fun ActionTypePicker(
    selected: HotspotActionType,
    allowUnsortedChange: Boolean,
    onSelect: (HotspotActionType) -> Unit
) {
    val options = listOf(
        HotspotActionType.PANEL to "App Panel",
        HotspotActionType.UNSORTED_PANEL to "Unsorted",
        HotspotActionType.DIRECT_GALLERY to "Gallery",
        HotspotActionType.DIRECT_ALARM to "Alarms"
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
            }
        }
    }
}
