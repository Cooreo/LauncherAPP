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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.data.MusicDisplayMode
import com.roomlauncher.app.data.VisualizerStyle
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
    onToggleMusicVisualizerEnabled: (Boolean) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onSelectMusicDefaultMode: (MusicDisplayMode) -> Unit,
    onToggleAudioReactiveVisualizer: (Boolean) -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onPreviewMusicVisualizer: () -> Unit,
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
    onEnterEditMode: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
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

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleAudioReactiveVisualizer(granted)
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

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackup(uri)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportBackup(uri)
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
                        text = "Room Launcher v${uiState.updateState.currentVersion}",
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

                // 3.5 Music Visualizer & Open-Source Synced Lyrics (LRCLIB)
                SettingsSectionCard(
                    icon = Icons.Rounded.GraphicEq,
                    title = "Music Visualizer & Lyrics",
                    subtitle = "Live song title, progress bar, animated visualizer, and open-source LRCLIB synced lyrics"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Show Now-Playing Bar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically appears when Spotify, YouTube Music, or any media player is active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.config.musicVisualizerEnabled,
                            onCheckedChange = onToggleMusicVisualizerEnabled
                        )
                    }

                    if (uiState.config.musicVisualizerEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Notification Access Permission Status
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (uiState.hasNotificationAccess) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            },
                            modifier = Modifier.fillMaxWidth()
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
                                        text = if (uiState.hasNotificationAccess) {
                                            "Notification Access Connected ✓"
                                        } else {
                                            "Notification Access Needed"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (uiState.hasNotificationAccess) {
                                            "Room Launcher can read active song title, artist, album art & progress."
                                        } else {
                                            "Android requires Notification Access so the launcher can detect playing music."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!uiState.hasNotificationAccess) {
                                    Button(
                                        onClick = onOpenNotificationAccess,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Enable")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visualizer Style Picker
                        Text(
                            text = "Visualizer Style",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VisualizerStyle.entries.forEach { style ->
                                val selected = uiState.config.musicVisualizerStyle == style
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectVisualizerStyle(style) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = style.displayName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = style.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected style",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Default Expanded View: Visualizer vs Lyrics
                        Text(
                            text = "Default Expanded View",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MusicDisplayMode.entries.forEach { mode ->
                                val selected = uiState.config.musicDefaultMode == mode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectMusicDefaultMode(mode) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Optional Real-Time Audio-Reactive FFT Capture
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Real-Time Audio Reactive",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Optional: taps live audio FFT (requires Android audio permission; automatically falls back to smooth ambient waves on Bluetooth/DRM streams)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.config.audioReactiveVisualizerEnabled && uiState.hasAudioPermission,
                                onCheckedChange = { wantEnabled ->
                                    if (wantEnabled && !uiState.hasAudioPermission) {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        onToggleAudioReactiveVisualizer(wantEnabled)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onPreviewMusicVisualizer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview Visualizer & Synced Lyrics")
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

                // 6. Auto-Updates (GitHub: Cooreo/LauncherAPP)
                val upd = uiState.updateState
                SettingsSectionCard(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "App Updates",
                    subtitle = "Checks GitHub (Cooreo/LauncherAPP) for newer Room Launcher releases"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Auto-Check for Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val statusText = when {
                                upd.isDownloading -> {
                                    if (upd.downloadProgress >= 0f) {
                                        "Downloading update… ${(upd.downloadProgress * 100).toInt()}%"
                                    } else {
                                        "Downloading update…"
                                    }
                                }
                                upd.isChecking -> "Checking GitHub for new releases…"
                                upd.availableUpdate != null ->
                                    "Update available: v${upd.availableUpdate.versionName} (installed v${upd.currentVersion})"
                                upd.errorMessage != null -> upd.errorMessage
                                upd.isUpToDate -> "You're up to date (v${upd.currentVersion})"
                                else -> "Installed version: v${upd.currentVersion}"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (upd.availableUpdate != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (!upd.installedCertFingerprint.isNullOrBlank()) {
                                Text(
                                    text = "Signing key: ${upd.installedCertFingerprint} (in-place upgrades enabled)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.config.autoCheckUpdatesEnabled,
                            onCheckedChange = onToggleAutoCheckUpdates
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCheckForUpdates,
                            enabled = !upd.isChecking && !upd.isDownloading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (upd.isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking…")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check for Updates")
                            }
                        }

                        if (upd.availableUpdate != null) {
                            Button(
                                onClick = onShowUpdatePopup,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update v${upd.availableUpdate.versionName}")
                            }
                        }
                    }
                }

                // 7. Room & Launcher Controls
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { exportBackupLauncher.launch("roomlauncher-backup.json") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export Backup")
                            }
                            OutlinedButton(
                                onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Restore Backup")
                            }
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

    // Update Available Popup inside Settings
    val availableUpdate = uiState.updateState.availableUpdate
    if (uiState.updateState.showUpdateDialog && availableUpdate != null) {
        val upd = uiState.updateState
        val sizeText = if (availableUpdate.sizeBytes > 0L) {
            val mb = availableUpdate.sizeBytes.toDouble() / (1024.0 * 1024.0)
            String.format(java.util.Locale.US, " • %.1f MB", mb)
        } else {
            ""
        }
        AlertDialog(
            onDismissRequest = {
                if (!upd.isDownloading && !upd.isInstalling) onDismissUpdatePopup()
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = if (upd.signatureConflict) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            },
            title = {
                Text(
                    text = if (upd.signatureConflict) {
                        "One-Time Key Upgrade"
                    } else {
                        "Update Available (v${availableUpdate.versionName})"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Installed: v${upd.currentVersion}  →  Latest: v${availableUpdate.versionName}$sizeText",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (upd.signatureConflict) {
                        Text(
                            text = "Android blocked an in-place install because your currently installed build was signed with an older/different key than this release. Follow these 3 quick steps once so all future updates install automatically in-place:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = { exportBackupLauncher.launch("roomlauncher-backup.json") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("1. Export Room Backup (.json)")
                        }
                        OutlinedButton(
                            onClick = onSaveUpdateApkToDownloads,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (upd.savedToDownloadsPath != null) {
                                    "2. Saved: ${upd.savedToDownloadsPath} ✓"
                                } else {
                                    "2. Save v${availableUpdate.versionName} APK to Downloads"
                                }
                            )
                        }
                        Button(
                            onClick = onUninstallForCleanUpdate,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("3. Uninstall Old Build, Then Open Downloads")
                        }
                    } else {
                        if (availableUpdate.releaseNotes.isNotBlank()) {
                            Text(
                                text = availableUpdate.releaseNotes.take(420),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (upd.signatureVerified && !upd.isDownloading) {
                            Text(
                                text = "Signing key verified ✓ • Ready for in-place upgrade",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (upd.isDownloading) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (upd.downloadProgress >= 0f) {
                                    LinearProgressIndicator(
                                        progress = { upd.downloadProgress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Downloading APK… ${(upd.downloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(
                                        text = "Downloading APK…",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (upd.isInstalling) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(
                                    text = "Verifying signing key & preparing installer…",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (upd.needsInstallPermission) {
                            Text(
                                text = "Allow 'Install unknown apps' for Room Launcher in Android Settings, then return here to finish installing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (upd.errorMessage != null && !upd.isDownloading && !upd.isInstalling) {
                            Text(
                                text = upd.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(
                        onClick = onOpenUpdateReleasePage,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Open Release on GitHub")
                    }
                }
            },
            confirmButton = {
                if (!upd.signatureConflict) {
                    Button(
                        onClick = onDownloadAndInstallUpdate,
                        enabled = !upd.isDownloading && !upd.isInstalling
                    ) {
                        Text(
                            when {
                                upd.isDownloading -> "Downloading…"
                                upd.isInstalling -> "Installing…"
                                upd.needsInstallPermission -> "Allow & Install"
                                !upd.downloadedApkPath.isNullOrBlank() -> "Install Now"
                                else -> "Download & Install"
                            }
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissUpdatePopup,
                    enabled = !upd.isDownloading && !upd.isInstalling
                ) {
                    Text(if (upd.signatureConflict) "Close" else "Later")
                }
            }
        )
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
