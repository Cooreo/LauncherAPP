package com.roomlauncher.app.ui.settings

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.ui.RoomUiState
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import com.roomlauncher.app.ui.theme.RoomThemes

@Composable
fun SettingsSheet(
    uiState: RoomUiState,
    onSelectRoomStyle: (RoomStyleId) -> Unit,
    onSelectTheme: (RoomThemeId) -> Unit,
    onToggleDynamicTheme: (Boolean) -> Unit,
    onPickTeenPoster: (isLeft: Boolean, uri: Uri) -> Unit,
    onClearTeenPoster: (isLeft: Boolean) -> Unit,
    onToggleQuickAnimations: (Boolean) -> Unit,
    onToggleChargingAnimation: (Boolean) -> Unit,
    onSelectChargingAnimationStyle: (ChargingAnimationStyle) -> Unit,
    onPreviewChargingAnimation: () -> Unit,
    onToggleWeather: (Boolean) -> Unit,
    onRefreshWeather: () -> Unit,
    onToggleMirrorWallpaper: (Boolean) -> Unit,
    onSyncWallpaperNow: () -> Unit,
    onPickFramePhoto: (Uri) -> Unit,
    onClearFramePhoto: () -> Unit,
    onEnterEditMode: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onResetRoom: () -> Unit,
    onDismiss: () -> Unit
) {
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onRefreshWeather()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onPickFramePhoto(uri)
        }
    }

    val leftPosterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onPickTeenPoster(true, uri)
        }
    }

    val rightPosterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onPickTeenPoster(false, uri)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Room Launcher v2.3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close settings")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Distinct Architectural Room Styles
                SettingsSectionCard(
                    icon = Icons.Rounded.Home,
                    title = "Room Style",
                    subtitle = "Transform your room's architecture, decor, and furniture"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoomStyleId.entries.forEach { style ->
                            val selected = uiState.config.roomStyleId == style
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectRoomStyle(style) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = style.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = style.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected style",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Teen Room customizable wall posters!
                        if (uiState.config.roomStyleId == RoomStyleId.TEEN_ROOM) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Teen Room Wall Posters",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tape your own band, movie, or anime posters onto the room wall.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { leftPosterPickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (uiState.posterLeftBitmap != null) "Change Left" else "Left Poster")
                                }
                                OutlinedButton(
                                    onClick = { rightPosterPickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (uiState.posterRightBitmap != null) "Change Right" else "Right Poster")
                                }
                            }
                            if (uiState.posterLeftBitmap != null || uiState.posterRightBitmap != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (uiState.posterLeftBitmap != null) {
                                        OutlinedButton(
                                            onClick = { onClearTeenPoster(true) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reset Left")
                                        }
                                    }
                                    if (uiState.posterRightBitmap != null) {
                                        OutlinedButton(
                                            onClick = { onClearTeenPoster(false) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reset Right")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Color Palettes (Dynamic Time-of-Day + Light & Dark Themes)
                SettingsSectionCard(
                    icon = Icons.Rounded.Palette,
                    title = "Color Palettes",
                    subtitle = "Light & dark lighting palettes that combine with any Room Style"
                ) {
                    val themes = RoomThemeId.entries
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Dynamic Time-of-Day Theme toggle
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (uiState.config.dynamicThemeEnabled) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (uiState.config.dynamicThemeEnabled) 1.5.dp else 1.dp,
                                    color = if (uiState.config.dynamicThemeEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = "Dynamic Time-of-Day Theme",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (uiState.config.dynamicThemeEnabled) {
                                            "Active now: ${uiState.effectiveThemeId.displayName} (6 AM Warm Linen • 3 PM Midnight Cozy • 9 PM OLED Pitch)"
                                        } else {
                                            "Day: Warm Linen • 3 PM: Midnight Cozy • Night: OLED Pitch"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.config.dynamicThemeEnabled,
                                    onCheckedChange = onToggleDynamicTheme
                                )
                            }
                        }

                        themes.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { themeId ->
                                    ThemeSwatchTile(
                                        themeId = themeId,
                                        selected = uiState.effectiveThemeId == themeId,
                                        onClick = { onSelectTheme(themeId) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Animations Toggle
                SettingsSectionCard(
                    icon = Icons.Rounded.Animation,
                    title = "Object Animations",
                    subtitle = "TV power-on zoom, Shelf opening book spread, Trash Can pop-out & more"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Quick Animations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (uiState.config.useQuickAnimations) {
                                    "Using fast simple spring popups for all objects"
                                } else {
                                    "Rich per-object animations enabled (TV, Shelf book, Trash Can)"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.config.useQuickAnimations,
                            onCheckedChange = onToggleQuickAnimations
                        )
                    }
                }

                // 3. Custom Room Charging Animation (when screen is on)
                SettingsSectionCard(
                    icon = Icons.Rounded.Bolt,
                    title = "Charging Animation",
                    subtitle = "6 custom plug-in animations while screen is on (~5.5s hold, tap to dismiss)"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Custom Charging Animation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (uiState.config.chargingAnimationEnabled) {
                                        "Style: ${uiState.config.chargingAnimationStyle.displayName}"
                                    } else {
                                        "Disabled"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.config.chargingAnimationEnabled,
                                onCheckedChange = onToggleChargingAnimation
                            )
                        }

                        if (uiState.config.chargingAnimationEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ChargingAnimationStyle.entries.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { style ->
                                            val isSelected = uiState.config.chargingAnimationStyle == style
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.outlineVariant
                                                        },
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { onSelectChargingAnimationStyle(style) }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = style.displayName,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = style.description,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onPreviewChargingAnimation,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview Charging Animation")
                        }
                    }
                }

                // 4. Live Weather in Window
                SettingsSectionCard(
                    icon = Icons.Rounded.Cloud,
                    title = "Window Live Weather",
                    subtitle = "Shows current weather sky & temperature through your room window"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Live Weather Sky",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val weatherSummary = when {
                                !uiState.config.weatherEnabled -> "Disabled (static clear window)"
                                !uiState.weatherState.hasLocationPermission -> "Grant coarse location to fetch local weather"
                                uiState.weatherState.temperatureC != null ->
                                    "${uiState.weatherState.temperatureC}°C • ${uiState.weatherState.description}"
                                else -> uiState.weatherState.description
                            }
                            Text(
                                text = weatherSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.config.weatherEnabled,
                            onCheckedChange = onToggleWeather
                        )
                    }

                    if (uiState.config.weatherEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!uiState.weatherState.hasLocationPermission) {
                                Button(
                                    onClick = {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enable Location")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onRefreshWeather,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refresh Weather")
                                }
                            }
                        }
                    }
                }

                // 4. Mirror Room Layout to Home Screen Wallpaper (Not Lockscreen)
                SettingsSectionCard(
                    icon = Icons.Rounded.Wallpaper,
                    title = "Home Screen Wallpaper Mirror",
                    subtitle = "Mirrors your room layout & theme onto Android's Home screen wallpaper only (Lock screen stays untouched)"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Auto-Mirror After Edits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Updates Home wallpaper automatically when you change themes or move objects",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.config.mirrorHomeWallpaper,
                            onCheckedChange = onToggleMirrorWallpaper
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onSyncWallpaperNow,
                        enabled = !uiState.isSyncingWallpaper,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSyncingWallpaper) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing Home Wallpaper…")
                        } else {
                            Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mirror Room to Home Wallpaper Now")
                        }
                    }
                }

                // 5. Picture Frame Photo (with transparent note about Android 13+ lockscreen restriction)
                SettingsSectionCard(
                    icon = Icons.Rounded.Image,
                    title = "Picture Frame Photo",
                    subtitle = "Display any photo (such as your lockscreen wallpaper image) inside the wall frame"
                ) {
                    Text(
                        text = "Note: Android 13+ blocks third-party launchers from reading the system lockscreen wallpaper directly for privacy, so you can pick the image here to display inside the frame.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.frameImageBitmap != null) "Change Photo" else "Choose Photo")
                        }
                        if (uiState.frameImageBitmap != null) {
                            OutlinedButton(
                                onClick = onClearFramePhoto,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Art")
                            }
                        }
                    }
                }

                // 6. Room & Launcher Controls
                SettingsSectionCard(
                    icon = Icons.Rounded.Tune,
                    title = "Room & Setup",
                    subtitle = "Customize furniture positions or revisit permissions"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEnterEditMode()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rearrange Room Objects (Edit Mode)")
                        }
                        OutlinedButton(
                            onClick = onReopenOnboarding,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Setup & Permissions Guide")
                        }
                        OutlinedButton(
                            onClick = onResetRoom,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Default Room Layout")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ThemeSwatchTile(
    themeId: RoomThemeId,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = RoomThemes.paletteFor(themeId)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color swatch preview: wall + floor + duvet + accent
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SwatchDot(preview.wallMain)
                SwatchDot(preview.floorMain)
                SwatchDot(preview.duvetPrimary)
                SwatchDot(preview.throwBlanket)
            }
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Column {
            Text(
                text = themeId.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (themeId.isDark) "Dark" else "Light",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwatchDot(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .border(0.75.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
    )
}
