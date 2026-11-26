package com.roomlauncher.app.ui.settings

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.MusicDisplayMode
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomPage
import com.roomlauncher.app.data.RoomType
import com.roomlauncher.app.data.VisualizerStyle
import com.roomlauncher.app.ui.RoomUiState
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import com.roomlauncher.app.ui.theme.RoomThemes
import com.roomlauncher.app.ui.theme.accentNeonColors
import com.roomlauncher.app.ui.theme.mainDrawableRes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class SettingsSectionKey(val chipLabel: String) {
    UPDATES("Updates"),
    ROOMS("Rooms"),
    DRAWER("App Drawer"),
    STYLE("Style"),
    THEME("Theme"),
    ANIMATIONS("Animations"),
    MUSIC("Music"),
    WEATHER("Weather & Wall"),
    BACKUP("Backup & Setup")
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsSheet(
    uiState: RoomUiState,
    onSelectRoomStyle: (RoomStyleId) -> Unit,
    onSelectGeometryDashSubTheme: (GeometryDashSubTheme) -> Unit,
    onSelectTheme: (RoomThemeId) -> Unit,
    onToggleDynamicTheme: (Boolean) -> Unit,
    onToggleAllAppsGridMode: (Boolean) -> Unit,
    onSetAllAppsScalePercent: (Int) -> Unit,
    onSelectRoomIndex: (Int) -> Unit,
    onAddRoom: (RoomType, String) -> Unit,
    onRenameRoom: (roomId: String, newName: String) -> Unit,
    onDeleteRoom: (roomId: String) -> Unit,
    onOpenChangelog: () -> Unit,
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
    onToggleIncludeAlphaUpdates: (Boolean) -> Unit,
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
        if (granted) onRefreshWeather()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onPickFramePhoto(uri)
    }

    val leftPosterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onPickTeenPoster(true, uri)
    }

    val rightPosterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onPickTeenPoster(false, uri)
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackup(uri)
    }

    var showAddRoomDialog by remember { mutableStateOf(false) }
    var renamingRoom by remember { mutableStateOf<RoomPage?>(null) }

    // Lazy list & section expansion state (only top essentials start expanded -> instant first frame!)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val expandedSections = remember {
        mutableStateMapOf(
            SettingsSectionKey.UPDATES to true,
            SettingsSectionKey.ROOMS to true,
            SettingsSectionKey.DRAWER to true,
            SettingsSectionKey.STYLE to true,
            SettingsSectionKey.THEME to false,
            SettingsSectionKey.ANIMATIONS to false,
            SettingsSectionKey.MUSIC to false,
            SettingsSectionKey.WEATHER to false,
            SettingsSectionKey.BACKUP to false
        )
    }
    var activeJumpChip by remember { mutableStateOf(SettingsSectionKey.UPDATES) }

    BackHandler(enabled = uiState.showSettingsSheet) {
        when {
            showAddRoomDialog -> showAddRoomDialog = false
            renamingRoom != null -> renamingRoom = null
            uiState.updateState.showUpdateDialog -> {
                if (!uiState.updateState.isDownloading && !uiState.updateState.isInstalling) {
                    onDismissUpdatePopup()
                }
            }
            else -> onDismiss()
        }
    }

    val sections = remember { SettingsSectionKey.entries }

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
                    .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 4.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenChangelog) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "What's New",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("What's New")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close settings")
                    }
                }
            }

            // Quick-Jump Category Strip (1 tap expands & scrolls straight to any section!)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(sections, key = { _, item -> item.name }) { index, section ->
                    val selected = activeJumpChip == section
                    val hasUpdateDot = section == SettingsSectionKey.UPDATES &&
                        uiState.updateState.availableUpdate != null
                    FilterChip(
                        selected = selected,
                        onClick = {
                            activeJumpChip = section
                            expandedSections[section] = true
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = section.chipLabel,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (hasUpdateDot) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Virtualized LazyColumn — composes only visible cards for instant, silky scrolling!
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 0. APP UPDATES (Always right at the top!)
                item(key = SettingsSectionKey.UPDATES.name) {
                    val upd = uiState.updateState
                    val statusSummary = when {
                        upd.isDownloading -> {
                            if (upd.downloadProgress >= 0f) {
                                "Downloading update… ${(upd.downloadProgress * 100).toInt()}%"
                            } else {
                                "Downloading update…"
                            }
                        }
                        upd.isChecking -> "Checking GitHub for new releases…"
                        upd.availableUpdate != null -> {
                            val tag = if (upd.availableUpdate.isPreRelease) " (Alpha)" else ""
                            "New update ready: v${upd.availableUpdate.versionName}$tag!"
                        }
                        upd.errorMessage != null -> upd.errorMessage
                        upd.isUpToDate -> {
                            val channel = if (uiState.config.includeAlphaUpdates) "Alpha + Stable" else "Stable"
                            "Up to date (v${upd.currentVersion} • $channel)"
                        }
                        else -> "Installed v${upd.currentVersion} • Cooreo/LauncherAPP"
                    }

                    SettingsSectionCard(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "App Updates",
                        subtitle = statusSummary,
                        highlighted = upd.availableUpdate != null
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (upd.availableUpdate != null) {
                                    Button(
                                        onClick = onShowUpdatePopup,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SystemUpdate,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (upd.availableUpdate.isPreRelease) {
                                                "Alpha v${upd.availableUpdate.versionName}"
                                            } else {
                                                "Update v${upd.availableUpdate.versionName}"
                                            }
                                        )
                                    }
                                }
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
                                        Text("Check Now")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = "Auto-Check on GitHub",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!upd.installedCertFingerprint.isNullOrBlank()) {
                                        Text(
                                            text = "Key: ${upd.installedCertFingerprint} • In-place updates",
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

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Opt-in Alpha / Pre-Release checkbox (unchecked by default)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onToggleIncludeAlphaUpdates(!uiState.config.includeAlphaUpdates)
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Checkbox(
                                    checked = uiState.config.includeAlphaUpdates,
                                    onCheckedChange = onToggleIncludeAlphaUpdates,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Include Alpha Versions",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (uiState.config.includeAlphaUpdates) {
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                            }
                                        ) {
                                            Text(
                                                text = if (uiState.config.includeAlphaUpdates) "ALPHA ON" else "STABLE ONLY",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uiState.config.includeAlphaUpdates) {
                                                    MaterialTheme.colorScheme.onTertiaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Opt in to early alpha test builds (e.g. v3.3a). Leave unchecked to only receive stable updates.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. HOUSE & ROOMS
                item(key = SettingsSectionKey.ROOMS.name) {
                    val expanded = expandedSections[SettingsSectionKey.ROOMS] == true
                    CollapsibleSettingsSectionCard(
                        icon = Icons.Rounded.MeetingRoom,
                        title = "House & Rooms",
                        subtitle = "${uiState.rooms.size} rooms • Active: ${uiState.currentRoom.name}",
                        expanded = expanded,
                        onToggle = { expandedSections[SettingsSectionKey.ROOMS] = !expanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.rooms.forEachIndexed { idx, room ->
                                val isCurrent = idx == uiState.safeCurrentRoomIndex
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isCurrent) 2.dp else 1.dp,
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSelectRoomIndex(idx)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = room.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isCurrent) {
                                                    Text(
                                                        text = "Current",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${room.type.displayName} • ${room.hotspots.size} objects",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { renamingRoom = room },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Edit,
                                                    contentDescription = "Rename ${room.name}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            if (uiState.rooms.size > 1) {
                                                IconButton(
                                                    onClick = { onDeleteRoom(room.id) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = "Remove ${room.name}",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { showAddRoomDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Room (Living Room, Kitchen, Gaming…)")
                            }
                        }
                    }
                }

                // 2. APP DRAWER (Scale / DPI in % + Grid/List default)
                item(key = SettingsSectionKey.DRAWER.name) {
                    val expanded = expandedSections[SettingsSectionKey.DRAWER] == true
                    val scalePercent = uiState.config.safeAllAppsScalePercent
                    val layoutLabel = if (uiState.config.allAppsGridMode) "Grid View" else "List View"
                    val scaleDescription = when {
                        scalePercent < 90 -> "Compact • Smaller icons & text"
                        scalePercent < 100 -> "Slightly smaller than phone default"
                        scalePercent == 100 -> "Phone default size (100%)"
                        scalePercent <= 120 -> "Larger & clearer (recommended)"
                        else -> "Extra large icons & text"
                    }
                    val presets = listOf(85, 100, 115, 130)

                    CollapsibleSettingsSectionCard(
                        icon = Icons.Rounded.Apps,
                        title = "App Drawer",
                        subtitle = "Size: $scalePercent% • $layoutLabel",
                        expanded = expanded,
                        onToggle = { expandedSections[SettingsSectionKey.DRAWER] = !expanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Prominent % header + badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = "Drawer Size (DPI Scale)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = scaleDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "$scalePercent%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Live scaled mini-preview inside Settings so % changes are immediately visual!
                            val baseDensity = LocalDensity.current
                            val previewDensity = remember(baseDensity, scalePercent) {
                                Density(
                                    density = baseDensity.density * (scalePercent / 100f),
                                    fontScale = baseDensity.fontScale
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Live Preview ($scalePercent%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    CompositionLocalProvider(LocalDensity provides previewDensity) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            val sampleItems = listOf(
                                                Triple("Camera", Icons.Rounded.CameraAlt, MaterialTheme.colorScheme.primaryContainer),
                                                Triple("Messages", Icons.Rounded.ChatBubble, MaterialTheme.colorScheme.secondaryContainer),
                                                Triple("Music", Icons.Rounded.MusicNote, MaterialTheme.colorScheme.tertiaryContainer)
                                            )
                                            sampleItems.forEach { (label, icon, bg) ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.width(62.dp)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(bg)
                                                    ) {
                                                        Icon(
                                                            imageVector = icon,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Smooth % Slider (75% .. 140% in 5% increments)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Slider(
                                    value = scalePercent.toFloat(),
                                    onValueChange = { raw ->
                                        val snapped = (raw / RoomConfig.ALL_APPS_SCALE_STEP_PERCENT).roundToInt() *
                                            RoomConfig.ALL_APPS_SCALE_STEP_PERCENT
                                        onSetAllAppsScalePercent(snapped)
                                    },
                                    valueRange = RoomConfig.MIN_ALL_APPS_SCALE_PERCENT.toFloat()..
                                        RoomConfig.MAX_ALL_APPS_SCALE_PERCENT.toFloat(),
                                    steps = ((RoomConfig.MAX_ALL_APPS_SCALE_PERCENT - RoomConfig.MIN_ALL_APPS_SCALE_PERCENT) /
                                        RoomConfig.ALL_APPS_SCALE_STEP_PERCENT) - 1,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${RoomConfig.MIN_ALL_APPS_SCALE_PERCENT}% Smaller",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "100% Normal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${RoomConfig.MAX_ALL_APPS_SCALE_PERCENT}% Larger",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Quick % Preset Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presets.forEach { preset ->
                                    val selected = scalePercent == preset
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = if (selected) 0.dp else 1.dp,
                                                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSetAllAppsScalePercent(preset) }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "$preset%",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Default Grid / List Layout Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(true to "Grid View", false to "List View").forEach { (isGrid, label) ->
                                    val selected = uiState.config.allAppsGridMode == isGrid
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
                                            .clickable { onToggleAllAppsGridMode(isGrid) }
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isGrid) Icons.Rounded.GridView else Icons.Rounded.ViewList,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. ROOM STYLE (Cozy, Teen, Kids, Minecraft, Geometry Dash)
                item(key = SettingsSectionKey.STYLE.name) {
                    val expanded = expandedSections[SettingsSectionKey.STYLE] == true
                    val styleSummary = if (uiState.config.roomStyleId == RoomStyleId.GEOMETRY_DASH) {
                        "Geometry Dash • ${uiState.config.geometryDashSubTheme.displayName}"
                    } else {
                        uiState.config.roomStyleId.displayName
                    }

                    CollapsibleSettingsSectionCard(
                        icon = Icons.Rounded.Home,
                        title = "Room Style",
                        subtitle = styleSummary,
                        expanded = expanded,
                        onToggle = { expandedSections[SettingsSectionKey.STYLE] = !expanded }
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

                            if (uiState.config.roomStyleId == RoomStyleId.GIRLY_ROOM) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Girly Room Vibe (Alpha Preview)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Quick-switch between Blush Rose, Baby Blue (light), Lavender Night & Deep Sapphire (dark blue), or pick any palette below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        RoomThemeId.BLUSH_ROSE to "Blush Rose",
                                        RoomThemeId.PASTEL_SKY to "Baby Blue",
                                        RoomThemeId.LAVENDER_NIGHT to "Lavender Night",
                                        RoomThemeId.DEEP_SAPPHIRE to "Deep Sapphire"
                                    ).chunked(2).forEach { chipRow ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            chipRow.forEach { (themeChoice, label) ->
                                                val isChosen = !uiState.config.dynamicThemeEnabled &&
                                                    uiState.config.themeId == themeChoice
                                                val swatch = RoomThemes.paletteFor(themeChoice)
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isChosen) {
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(
                                                            width = if (isChosen) 2.dp else 1.dp,
                                                            color = if (isChosen) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.outlineVariant
                                                            },
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { onSelectTheme(themeChoice) }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(13.dp)
                                                                .clip(CircleShape)
                                                                .background(swatch.wallMain)
                                                                .border(1.dp, swatch.rugBorder, CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.config.roomStyleId == RoomStyleId.TEEN_ROOM) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Teen Room Wall Posters",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
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

                            if (uiState.config.roomStyleId == RoomStyleId.GEOMETRY_DASH) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Geometry Dash Sub-Themes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GeometryDashSubTheme.entries.forEach { subTheme ->
                                        GeometryDashSubThemeCard(
                                            subTheme = subTheme,
                                            selected = uiState.config.geometryDashSubTheme == subTheme,
                                            onClick = { onSelectGeometryDashSubTheme(subTheme) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. COLOR PALETTES & DYNAMIC THEME
                item(key = SettingsSectionKey.THEME.name) {
                    val expanded = expandedSections[SettingsSectionKey.THEME] == true
                    val themeSummary = when {
                        uiState.config.roomStyleId == RoomStyleId.GEOMETRY_DASH ->
                            "Geometry Dash neon (${uiState.config.geometryDashSubTheme.displayName})"
                        uiState.config.dynamicThemeEnabled ->
                            "Dynamic • Now: ${uiState.effectiveThemeId.displayName}"
                        else -> uiState.effectiveThemeId.displayName
                    }

                    CollapsibleSettingsSectionCard(
                        icon = Icons.Rounded.Palette,
                        title = "Color Palettes",
                        subtitle = themeSummary,
                        expanded = expanded,
                        onToggle = { expandedSections[SettingsSectionKey.THEME] = !expanded }
                    ) {
                        val themes = RoomThemeId.entries
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                            text = "6 AM Warm Linen • 3 PM Midnight Cozy • 9 PM OLED Pitch",
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
                }

                // 4. OBJECT & CHARGING ANIMATIONS
                item(key = SettingsSectionKey.ANIMATIONS.name) {
                    val expanded = expandedSections[SettingsSectionKey.ANIMATIONS] == true
                    val animSummary = buildString {
                        append(if (uiState.config.useQuickAnimations) "Quick Popups" else "Rich Object Animations")
                        append(" • Charging: ")
                        append(
                            if (uiState.config.chargingAnimationEnabled) {
                                uiState.config.chargingAnimationStyle.displayName
                            } else {
                                "Off"
                            }
                        )
                    }

                    CollapsibleSettingsSectionCard(
                        icon = Icons.Rounded.Animation,
                        title = "Animations & Charging",
                        subtitle = animSummary,
                        expanded = expanded,
                        onToggle = { expandedSections[SettingsSectionKey.ANIMATIONS] = !expanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Quick Animations",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (uiState.config.useQuickAnimations) {
                                            "Simple fast popups for all objects"
                                        } else {
                                            "Rich TV zoom, Shelf book-open & Trash Can pop-out"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.config.useQuickAnimations,
                                    onCheckedChange = onToggleQuickAnimations
                                )
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Custom Charging Animation",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Plays on plug-in while screen is on (~5.5s, tap to dismiss)",
                                        style = MaterialTheme.typography.bodySmall,
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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview Charging Animation")
                            }
                        }
                    }
                }

                // 5. MUSIC VISUALIZER & SYNCED LYRICS (Privacy-first: no microphone permission!)
                item(key = SettingsSectionKey.MUSIC.name) {
                    val expanded = expandedSections[SettingsSectionKey.MUSIC] == true
                    val musicSummary = if (uiState.config.musicVisualizerEnabled) {
                        "${uiState.config.musicVisualizerStyle.displayName} • LRCLIB Synced Lyrics"
                    } else {
                        "Disabled"
                    }

                    CollapsibleSettingsSectionCard(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Music Visualizer & Lyrics",
                        subtitle = musicSummary,
                        expanded = expanded,
                        onToggle = { expandedSections[SettingsSectionKey.MUSIC] = !expanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Show Now-Playing Bar",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Appears automatically when Spotify, YouTube Music, or any player is active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.config.musicVisualizerEnabled,
                                    onCheckedChange = onToggleMusicVisualizerEnabled
                                )
                            }

                            if (uiState.config.musicVisualizerEnabled) {
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
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (uiState.hasNotificationAccess) {
                                                    "Reads active track title, album art & progress (no microphone used)."
                                                } else {
                                                    "Required by Android so the launcher can detect playing music."
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

                                Text(
                                    text = "Visualizer Style",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                                        fontWeight = FontWeight.SemiBold
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

                                Text(
                                    text = "Default Expanded View",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = onPreviewMusicVisualizer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preview Visualizer & Synced Lyrics")
                                }
                            }
                        }
                    }
                }

                // 6. WEATHER, WALLPAPER MIRROR & PICTURE FRAME
                item(key = SettingsSectionKey.WEATHER.name) {
                    val expanded = expandedSections[SettingsSectionKey.WEATHER] == true
                    val weatherSummary = buildString {
                        append(
                            when {
                                !uiState.config.weatherEnabled -> "Weather Off"
                                uiState.weatherState.temperatureC != null -> "${uiState.weatherState.temperatureC}°C"
                                else -> "Weather On"
                            }
                        )
                        append(" • Wallpaper Mirror: ")
                        append(if (uiState.config.mirrorHomeWallpaper) "Auto" else "Manual")
                    }

                    CollapsibleSettingsSectionCard(
                        icon = Icons.Rounded.Cloud,
                        title = "Weather, Wallpaper & Frame",
                        subtitle = weatherSummary,
                        expanded = expanded,
                        onToggle = { expandedSections[SettingsSectionKey.WEATHER] = !expanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Weather
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Window Live Weather",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val detail = when {
                                        !uiState.config.weatherEnabled -> "Static clear window"
                                        !uiState.weatherState.hasLocationPermission -> "Grant coarse location for local weather"
                                        uiState.weatherState.temperatureC != null ->
                                            "${uiState.weatherState.temperatureC}°C • ${uiState.weatherState.description}"
                                        else -> uiState.weatherState.description
                                    }
                                    Text(
                                        text = detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.config.weatherEnabled,
                                    onCheckedChange = onToggleWeather
                                )
                            }

                            if (uiState.config.weatherEnabled) {
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

                            HorizontalDivider()

                            // Home Wallpaper Mirror
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Home Wallpaper Auto-Mirror",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Syncs Home screen wallpaper only (Lock screen stays untouched)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.config.mirrorHomeWallpaper,
                                    onCheckedChange = onToggleMirrorWallpaper
                                )
                            }

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

                            HorizontalDivider()

                            // Picture Frame Photo
                            Text(
                                text = "Picture Frame Photo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pick any photo to display inside your wall Picture Frame.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    }
                }

                // 7. BACKUP, CHANGELOG & SETUP
                item(key = SettingsSectionKey.BACKUP.name) {
                    val expanded = expandedSections[SettingsSectionKey.BACKUP] == true
                    CollapsibleSettingsSectionCard(
                        icon = Icons.Rounded.Tune,
                        title = "Backup, Edit & Setup",
                        subtitle = "Edit Mode, Export/Restore Backup, Changelog & Reset",
                        expanded = expanded,
                        onToggle = { expandedSections[SettingsSectionKey.BACKUP] = !expanded }
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
                                onClick = onOpenChangelog,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Changelog (v${uiState.updateState.currentVersion})")
                            }
                            OutlinedButton(
                                onClick = onResetRoom,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset Default House Layout")
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Add Room Dialog
    if (showAddRoomDialog) {
        var selectedType by remember { mutableStateOf(RoomType.LIVING_ROOM) }
        var customName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = { Text("Add a New Room") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pick a room preset with tailored furniture & app categories:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RoomType.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = {
                                        Text(
                                            text = type.displayName,
                                            fontWeight = if (selectedType == type) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Text(
                        text = selectedType.description,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Custom Room Name (optional)") },
                        placeholder = { Text(selectedType.displayName) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddRoom(selectedType, customName)
                        showAddRoomDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Add Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Room Dialog
    val roomToRename = renamingRoom
    if (roomToRename != null) {
        var draftName by remember(roomToRename.id) { mutableStateOf(roomToRename.name) }
        AlertDialog(
            onDismissRequest = { renamingRoom = null },
            title = { Text("Rename Room") },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    label = { Text("Room Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftName.isNotBlank()) {
                            onRenameRoom(roomToRename.id, draftName)
                            renamingRoom = null
                        }
                    },
                    enabled = draftName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingRoom = null }) {
                    Text("Cancel")
                }
            }
        )
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
                    text = when {
                        upd.signatureConflict -> "One-Time Key Upgrade"
                        availableUpdate.isPreRelease -> "Alpha Update Available (v${availableUpdate.versionName})"
                        else -> "Update Available (v${availableUpdate.versionName})"
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
                            Text("3. Open App Info to Uninstall Old Build")
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
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        RoundedCornerShape(22.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CollapsibleSettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
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
    val palette = RoomThemes.paletteFor(themeId)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = modifier
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SwatchDot(palette.wallMain)
                    SwatchDot(palette.floorMain)
                    SwatchDot(palette.duvetPrimary)
                    SwatchDot(palette.throwBlanket)
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
            Spacer(modifier = Modifier.height(6.dp))
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
private fun GeometryDashSubThemeCard(
    subTheme: GeometryDashSubTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (neonPrimary, neonSecondary, neonTertiary) = subTheme.accentNeonColors()
    val borderColor = if (selected) neonPrimary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            neonSecondary.copy(alpha = 0.24f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, neonPrimary.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(id = subTheme.mainDrawableRes()),
                    contentDescription = "${subTheme.displayName} level art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subTheme.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SwatchDot(neonPrimary)
                    SwatchDot(neonTertiary)
                }
                Text(
                    text = subTheme.creatorTag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = neonPrimary
                )
                Text(
                    text = subTheme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(neonPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected sub-theme",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
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
