package com.roomlauncher.app.ui.allapps

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.components.AppIconView
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
private sealed interface AllAppsEntry {
    @Immutable
    data class Header(val letter: String, val count: Int) : AllAppsEntry

    @Immutable
    data class AppItem(val app: AppInfo, val ownerLabel: String) : AllAppsEntry

    data object SettingsCard : AllAppsEntry
}

@Composable
fun AllAppsSheet(
    isVisible: Boolean = true,
    installedApps: List<AppInfo>,
    packageOwnerLabels: Map<String, String>,
    defaultUnsortedLabel: String,
    panelHotspotChoices: List<Pair<String, RoomHotspot>>,
    onLaunchApp: (AppInfo) -> Unit,
    onMoveApp: (packageName: String, targetHotspotId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }

    val dismissCleanly: () -> Unit = remember(onDismiss) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onDismiss()
        }
    }

    // Silky live Predictive Back gesture + hardware Back button handler
    val backProgress = remember { Animatable(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    PredictiveBackHandler(enabled = isVisible) { progressFlow ->
        try {
            progressFlow.collect { event ->
                backSwipeEdge = event.swipeEdge
                backProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            if (selectedAppForMenu != null) {
                backProgress.snapTo(0f)
                selectedAppForMenu = null
            } else {
                // Keep backProgress at its final position while fading out — zero snap-back glitch!
                dismissCleanly()
            }
        } catch (e: CancellationException) {
            backProgress.animateTo(0f, tween(140))
            throw e
        }
    }

    // Lightweight A–Z scrubber state (zero idle spring coroutines!)
    var activeScrubLetter by remember { mutableStateOf<String?>(null) }
    var scrubFractionY by remember { mutableFloatStateOf(0.5f) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val trimmedQuery = searchQuery.trim()
    val filteredApps = remember(installedApps, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            installedApps
        } else {
            val q = trimmedQuery.lowercase()
            installedApps.filter {
                it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
            }
        }
    }

    // Flatten into a single stable list with precomputed letter-to-index & counts
    val listModel = remember(filteredApps, packageOwnerLabels, defaultUnsortedLabel) {
        val entries = ArrayList<AllAppsEntry>(filteredApps.size + 32)
        val letterToIndex = LinkedHashMap<String, Int>()
        val letterCounts = LinkedHashMap<String, Int>()

        for (app in filteredApps) {
            val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val letter = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
            letterCounts[letter] = (letterCounts[letter] ?: 0) + 1
        }

        var lastLetter: String? = null
        for (app in filteredApps) {
            val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val letter = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
            if (letter != lastLetter) {
                letterToIndex[letter] = entries.size
                entries.add(AllAppsEntry.Header(letter, letterCounts[letter] ?: 0))
                lastLetter = letter
            }
            val owner = packageOwnerLabels[app.packageName] ?: defaultUnsortedLabel
            entries.add(AllAppsEntry.AppItem(app, owner))
        }
        entries.add(AllAppsEntry.SettingsCard)
        Triple(entries, letterToIndex, letterCounts)
    }

    val flatEntries = listModel.first
    val letterIndexMap = listModel.second
    val letterCounts = listModel.third
    val sectionLetters = remember(letterIndexMap) { letterIndexMap.keys.toList() }

    val p = backProgress.value
    val isBackDragging = p > 0.002f
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    Surface(
        shape = if (isBackDragging) RoundedCornerShape((p * 26f).dp) else RectangleShape,
        shadowElevation = if (isBackDragging) 12.dp else 0.dp,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (isBackDragging) {
                    val scale = 1f - (p * 0.08f)
                    scaleX = scale
                    scaleY = scale
                    val dir = if (backSwipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                    translationX = dir * p * size.width * 0.06f
                    alpha = 1f - (p * 0.18f)
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top swipe-down pill + Header (dragging down cleanly dismisses back to the Room!)
            var headerDragSum by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { headerDragSum = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                headerDragSum += dragAmount
                                if (headerDragSum > 72f) {
                                    headerDragSum = 0f
                                    dismissCleanly()
                                }
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 2.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "All Apps",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (trimmedQuery.isEmpty()) {
                                "${filteredApps.size} installed • A–Z"
                            } else {
                                "${filteredApps.size} matching \"$trimmedQuery\""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                onOpenSettings()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Room Launcher Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = dismissCleanly) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close all apps"
                            )
                        }
                    }
                }
            }

            // Lightweight Search bar (no extra animated subcompositions)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps by name…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Fast App List + Zero-Overhead A–Z Fast Scroller Rail + Floating Letter Bubble
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val containerHeightPx = with(density) { maxHeight.toPx() }

                if (filteredApps.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (trimmedQuery.isNotEmpty()) {
                                "No apps matching \"$trimmedQuery\""
                            } else {
                                "No launchable apps found"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        SettingsDeckCard(onClick = onOpenSettings)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 42.dp,
                            top = 4.dp,
                            bottom = 28.dp + imeBottomPadding
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = flatEntries,
                            key = { entry ->
                                when (entry) {
                                    is AllAppsEntry.Header -> "hdr_${entry.letter}"
                                    is AllAppsEntry.AppItem -> entry.app.packageName
                                    AllAppsEntry.SettingsCard -> "footer_settings"
                                }
                            },
                            contentType = { entry ->
                                when (entry) {
                                    is AllAppsEntry.Header -> "header"
                                    is AllAppsEntry.AppItem -> "app"
                                    AllAppsEntry.SettingsCard -> "settings"
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is AllAppsEntry.Header -> {
                                    FastLetterHeader(
                                        letter = entry.letter,
                                        count = entry.count,
                                        isHighlighted = activeScrubLetter == entry.letter
                                    )
                                }
                                is AllAppsEntry.AppItem -> {
                                    FastAllAppRow(
                                        app = entry.app,
                                        ownerLabel = entry.ownerLabel,
                                        highlightQuery = trimmedQuery,
                                        onLaunch = { onLaunchApp(entry.app) },
                                        onOpenMenu = { selectedAppForMenu = entry.app }
                                    )
                                }
                                AllAppsEntry.SettingsCard -> {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    SettingsDeckCard(onClick = onOpenSettings)
                                }
                            }
                        }
                    }

                    // Ultra-Lightweight A–Z Fast Scroller Rail (0 running springs when idle!)
                    if (sectionLetters.size > 1 && trimmedQuery.isEmpty()) {
                        AlphabetFastScrollerRail(
                            letters = sectionLetters,
                            activeLetter = activeScrubLetter,
                            onLetterSelected = { letter, yFraction ->
                                scrubFractionY = yFraction
                                if (letter != activeScrubLetter) {
                                    activeScrubLetter = letter
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    letterIndexMap[letter]?.let { targetIdx ->
                                        coroutineScope.launch {
                                            listState.scrollToItem(targetIdx)
                                        }
                                    }
                                }
                            },
                            onScrubEnd = { activeScrubLetter = null },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = 6.dp, top = 8.dp, bottom = 12.dp)
                        )
                    }

                    // Instant Finger-Following Letter Bubble (only composed while scrubbing!)
                    val shownLetter = activeScrubLetter
                    if (shownLetter != null) {
                        val bubbleMaxY = (containerHeightPx - with(density) { 80.dp.toPx() }).coerceAtLeast(0f)
                        val bubbleY = (scrubFractionY * bubbleMaxY).coerceIn(0f, bubbleMaxY)
                        val shownCount = letterCounts[shownLetter] ?: 0
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 32.dp,
                                bottomStart = 32.dp,
                                topEnd = 32.dp,
                                bottomEnd = 8.dp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset {
                                    IntOffset(
                                        x = -with(density) { 54.dp.toPx() }.roundToInt(),
                                        y = bubbleY.roundToInt()
                                    )
                                }
                                .size(64.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = shownLetter,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "$shownCount",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Shared App Actions dialog
    val menuApp = selectedAppForMenu
    if (menuApp != null) {
        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppIconView(app = menuApp, size = 36.dp)
                    Text(
                        text = menuApp.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Move to Room Object",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    panelHotspotChoices.forEach { (roomName, target) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onMoveApp(menuApp.packageName, target.id)
                                    selectedAppForMenu = null
                                }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DriveFileMove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (target.subtitle.isNotBlank()) {
                                        "$roomName • ${target.subtitle}"
                                    } else {
                                        roomName
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val pkg = menuApp.packageName
                                selectedAppForMenu = null
                                SystemIntentHelper.openAppInfoSettings(context, pkg)
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "App Info",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppForMenu = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun FastLetterHeader(
    letter: String,
    count: Int,
    isHighlighted: Boolean
) {
    val pillBg = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 3.dp)
            .background(pillBg, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FastAllAppRow(
    app: AppInfo,
    ownerLabel: String,
    highlightQuery: String,
    onLaunch: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val displayTitle = if (highlightQuery.isEmpty()) {
        null
    } else {
        remember(app.label, highlightQuery, primaryColor) {
            val matchStart = app.label.indexOf(highlightQuery, ignoreCase = true)
            if (matchStart >= 0) {
                val matchEnd = (matchStart + highlightQuery.length).coerceAtMost(app.label.length)
                buildAnnotatedString {
                    append(app.label.substring(0, matchStart))
                    withStyle(
                        SpanStyle(
                            color = primaryColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append(app.label.substring(matchStart, matchEnd))
                    }
                    append(app.label.substring(matchEnd))
                }
            } else {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = onOpenMenu
            )
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconView(app = app, size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (displayTitle != null) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = ownerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onOpenMenu)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "App actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AlphabetFastScrollerRail(
    letters: List<String>,
    activeLetter: String?,
    onLetterSelected: (letter: String, yFraction: Float) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun letterAndFractionAtY(y: Float, totalHeight: Int): Pair<String, Float>? {
        if (letters.isEmpty() || totalHeight <= 0) return null
        val fraction = (y / totalHeight.toFloat()).coerceIn(0f, 0.9999f)
        val index = (fraction * letters.size).toInt().coerceIn(0, letters.lastIndex)
        return letters[index] to fraction
    }

    val activeIdx = if (activeLetter == null) -1 else letters.indexOf(activeLetter)

    Column(
        modifier = modifier
            .width(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .pointerInput(letters) {
                detectTapGestures(
                    onPress = { offset ->
                        letterAndFractionAtY(offset.y, size.height)?.let { (letter, frac) ->
                            onLetterSelected(letter, frac)
                        }
                        tryAwaitRelease()
                        onScrubEnd()
                    }
                )
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        letterAndFractionAtY(offset.y, size.height)?.let { (letter, frac) ->
                            onLetterSelected(letter, frac)
                        }
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        letterAndFractionAtY(change.position.y, size.height)?.let { (letter, frac) ->
                            onLetterSelected(letter, frac)
                        }
                    },
                    onDragEnd = { onScrubEnd() },
                    onDragCancel = { onScrubEnd() }
                )
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEachIndexed { idx, letter ->
            val distance = if (activeIdx >= 0) abs(idx - activeIdx) else 99
            val scale = when (distance) {
                0 -> 1.38f
                1 -> 1.16f
                else -> 1f
            }
            val isActive = distance == 0
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (scale != 1f) {
                            scaleX = scale
                            scaleY = scale
                        }
                    }
            ) {
                Text(
                    text = letter,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsDeckCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Room Launcher Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Updates, rooms, styles, themes & wallpaper",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
