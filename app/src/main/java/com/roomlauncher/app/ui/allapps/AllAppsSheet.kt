package com.roomlauncher.app.ui.allapps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.components.AppIconView
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class AllAppsFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Rounded.Apps),
    PINNED("Pinned", Icons.Rounded.Star),
    RECENT("Recent", Icons.Rounded.History),
    GAMES("Games", Icons.Rounded.SportsEsports),
    UNSORTED("Unsorted", Icons.Rounded.Workspaces)
}

private enum class AllAppsSort(val label: String, val icon: ImageVector) {
    AZ("A–Z", Icons.Rounded.SortByAlpha),
    NEWEST("Newest", Icons.Rounded.NewReleases),
    RECENT("Recent", Icons.Rounded.History)
}

@Immutable
private sealed interface AllAppsEntry {
    @Immutable
    data class PinnedDock(val apps: List<AppInfo>) : AllAppsEntry

    @Immutable
    data class Header(val letter: String, val count: Int) : AllAppsEntry

    @Immutable
    data class AppItem(
        val app: AppInfo,
        val ownerLabel: String,
        val isPinned: Boolean,
        val isAssignedToRoom: Boolean
    ) : AllAppsEntry

    data object SettingsCard : AllAppsEntry
}

/**
 * Redesigned, boot-warmed All Apps Drawer.
 *
 * Kept persistently composed in memory while off-screen (placed below viewport bounds with
 * alpha 0 when settled closed) so opening and closing perform ZERO composition/teardown work
 * and animate strictly via pure-GPU translationY.
 */
@Composable
fun AllAppsSheet(
    isVisible: Boolean,
    installedApps: List<AppInfo>,
    pinnedPackages: List<String>,
    recentPackages: List<String>,
    isGridMode: Boolean,
    drawerScalePercent: Int = RoomConfig.DEFAULT_ALL_APPS_SCALE_PERCENT,
    packageOwnerLabels: Map<String, String>,
    defaultUnsortedLabel: String,
    panelHotspotChoices: List<Pair<String, RoomHotspot>>,
    onLaunchApp: (AppInfo) -> Unit,
    onToggleGridMode: (Boolean) -> Unit,
    onTogglePinApp: (String) -> Unit,
    onMoveApp: (packageName: String, targetHotspotId: String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val baseDensity = LocalDensity.current
    val scaleFactor = RoomConfig.clampAllAppsScalePercent(drawerScalePercent) / 100f
    val drawerDensity = remember(baseDensity, scaleFactor) {
        Density(
            density = baseDensity.density * scaleFactor,
            fontScale = baseDensity.fontScale
        )
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(AllAppsFilter.ALL) }
    var activeSort by remember { mutableStateOf(AllAppsSort.AZ) }
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var isClosing by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    var activeScrubLetter by remember { mutableStateOf<String?>(null) }
    var scrubFractionY by remember { mutableFloatStateOf(0.5f) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // Always read live visibility/callbacks inside remembered gesture & close lambdas
    val currentIsVisible by rememberUpdatedState(isVisible)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentIsGridMode by rememberUpdatedState(isGridMode)

    // Pure-GPU vertical slide progress: 0f = fully open, 1f = completely off-screen below.
    val slideOffsetFraction = remember { Animatable(if (isVisible) 0f else 1f) }
    var isUserDragging by remember { mutableStateOf(false) }
    var activeDragFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isVisible) {
        isUserDragging = false
        activeDragFraction = 0f
        if (isVisible) {
            isClosing = false
            isAnimating = true
            slideOffsetFraction.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 195, easing = FastOutSlowInEasing)
            )
            isAnimating = false
        } else {
            selectedAppForMenu = null
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            if (slideOffsetFraction.value < 0.999f) {
                isAnimating = true
                slideOffsetFraction.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                )
                isAnimating = false
            }
            isClosing = false
            // Reset transient search & scroll position silently once completely off-screen
            searchQuery = ""
            runCatching { gridState.scrollToItem(0) }
            runCatching { listState.scrollToItem(0) }
        }
    }

    val isSettledClosed = !isVisible && !isAnimating

    val animateCloseAndDismiss: () -> Unit = remember {
        {
            if (!isClosing && currentIsVisible) {
                val startFraction = if (isUserDragging) activeDragFraction else slideOffsetFraction.value
                isClosing = true
                isUserDragging = false
                activeDragFraction = 0f
                selectedAppForMenu = null
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                coroutineScope.launch {
                    isAnimating = true
                    if (slideOffsetFraction.value != startFraction) {
                        slideOffsetFraction.snapTo(startFraction)
                    }
                    val remaining = (1f - startFraction).coerceIn(0f, 1f)
                    val durationMs = (remaining * 175f).roundToInt().coerceIn(75, 175)
                    slideOffsetFraction.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                    )
                    isAnimating = false
                    currentOnDismiss()
                }
            }
        }
    }

    val settleAfterDrag: (Float) -> Unit = remember {
        { velocityY ->
            if (!isClosing && currentIsVisible) {
                val dragged = activeDragFraction
                val shouldDismiss = dragged > 0.08f || (velocityY > 420f && dragged > 0.015f)
                if (shouldDismiss) {
                    animateCloseAndDismiss()
                } else {
                    isUserDragging = false
                    activeDragFraction = 0f
                    coroutineScope.launch {
                        slideOffsetFraction.snapTo(dragged)
                        slideOffsetFraction.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                        )
                    }
                }
            } else {
                isUserDragging = false
                activeDragFraction = 0f
            }
        }
    }

    // Single-fire Back handler active ONLY when drawer is visible
    BackHandler(enabled = isVisible && !isClosing) {
        when {
            selectedAppForMenu != null -> selectedAppForMenu = null
            searchQuery.isNotEmpty() -> {
                searchQuery = ""
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
            activeFilter != AllAppsFilter.ALL -> activeFilter = AllAppsFilter.ALL
            else -> animateCloseAndDismiss()
        }
    }

    val pinnedSet = remember(pinnedPackages) { pinnedPackages.toHashSet() }
    val recentRank = remember(recentPackages) {
        recentPackages.withIndex().associate { it.value to it.index }
    }
    val appsByPackage = remember(installedApps) {
        installedApps.associateBy { it.packageName }
    }
    val pinnedApps = remember(pinnedPackages, appsByPackage) {
        pinnedPackages.mapNotNull { appsByPackage[it] }
    }
    val gamesCount = remember(installedApps) { installedApps.count { it.isGame } }
    val unsortedCount = remember(installedApps, packageOwnerLabels) {
        installedApps.count { it.packageName !in packageOwnerLabels }
    }

    val trimmedQuery = searchQuery.trim()
    val filteredApps = remember(
        installedApps,
        trimmedQuery,
        activeFilter,
        activeSort,
        pinnedSet,
        recentRank,
        packageOwnerLabels
    ) {
        val base = when (activeFilter) {
            AllAppsFilter.ALL -> installedApps
            AllAppsFilter.PINNED -> installedApps.filter { it.packageName in pinnedSet }
            AllAppsFilter.RECENT -> installedApps
                .filter { it.packageName in recentRank }
                .sortedBy { recentRank[it.packageName] ?: Int.MAX_VALUE }
            AllAppsFilter.GAMES -> installedApps.filter { it.isGame }
            AllAppsFilter.UNSORTED -> installedApps.filter { it.packageName !in packageOwnerLabels }
        }

        val queried = if (trimmedQuery.isEmpty()) {
            base
        } else {
            val q = trimmedQuery.lowercase()
            base.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }.sortedByDescending { it.label.startsWith(q, ignoreCase = true) }
        }

        if (activeFilter == AllAppsFilter.RECENT && trimmedQuery.isEmpty()) {
            queried
        } else {
            when (activeSort) {
                AllAppsSort.AZ -> queried.sortedBy { it.label.lowercase() }
                AllAppsSort.NEWEST -> queried.sortedByDescending { it.firstInstallTime }
                AllAppsSort.RECENT -> queried.sortedWith(
                    compareBy<AppInfo> { recentRank[it.packageName] ?: Int.MAX_VALUE }
                        .thenBy { it.label.lowercase() }
                )
            }
        }
    }

    val showAlphabetIndex = activeSort == AllAppsSort.AZ &&
        activeFilter != AllAppsFilter.RECENT &&
        trimmedQuery.isEmpty()

    // Show crisp A–Z letter headers in BOTH Grid View and List View so alphabetical sorting is always clear!
    val insertSectionHeaders = showAlphabetIndex

    val showPinnedDock = activeFilter == AllAppsFilter.ALL &&
        trimmedQuery.isEmpty() &&
        pinnedApps.isNotEmpty()

    val listModel = remember(
        filteredApps,
        pinnedApps,
        pinnedSet,
        showPinnedDock,
        showAlphabetIndex,
        insertSectionHeaders,
        packageOwnerLabels,
        defaultUnsortedLabel
    ) {
        val entries = ArrayList<AllAppsEntry>(filteredApps.size + 36)
        val letterToIndex = LinkedHashMap<String, Int>()
        val letterCounts = LinkedHashMap<String, Int>()

        if (showPinnedDock) {
            entries.add(AllAppsEntry.PinnedDock(pinnedApps))
        }

        if (showAlphabetIndex) {
            for (app in filteredApps) {
                val letter = app.indexLetter
                letterCounts[letter] = (letterCounts[letter] ?: 0) + 1
            }
            var lastLetter: String? = null
            for (app in filteredApps) {
                val letter = app.indexLetter
                if (letter != lastLetter) {
                    letterToIndex[letter] = entries.size
                    if (insertSectionHeaders) {
                        entries.add(AllAppsEntry.Header(letter, letterCounts[letter] ?: 0))
                    }
                    lastLetter = letter
                }
                val isAssigned = app.packageName in packageOwnerLabels
                val owner = packageOwnerLabels[app.packageName] ?: defaultUnsortedLabel
                entries.add(
                    AllAppsEntry.AppItem(
                        app = app,
                        ownerLabel = owner,
                        isPinned = app.packageName in pinnedSet,
                        isAssignedToRoom = isAssigned
                    )
                )
            }
        } else {
            for (app in filteredApps) {
                val isAssigned = app.packageName in packageOwnerLabels
                val owner = packageOwnerLabels[app.packageName] ?: defaultUnsortedLabel
                entries.add(
                    AllAppsEntry.AppItem(
                        app = app,
                        ownerLabel = owner,
                        isPinned = app.packageName in pinnedSet,
                        isAssignedToRoom = isAssigned
                    )
                )
            }
        }
        entries.add(AllAppsEntry.SettingsCard)
        Triple(entries, letterToIndex, letterCounts)
    }

    val flatEntries = listModel.first
    val letterIndexMap = listModel.second
    val letterCounts = listModel.third
    val sectionLetters = remember(letterIndexMap) { letterIndexMap.keys.toList() }

    val showScrubRail = sectionLetters.size > 1 && showAlphabetIndex

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    // Place completely below viewport when settled closed so hit-testing NEVER steals room touches
                    if (isSettledClosed) {
                        placeable.place(0, constraints.maxHeight)
                    } else {
                        placeable.place(0, 0)
                    }
                }
            }
            .graphicsLayer {
                val effectiveOffset = if (isUserDragging) activeDragFraction else slideOffsetFraction.value
                translationY = effectiveOffset * size.height
                alpha = if (isSettledClosed) 0f else 1f
            }
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (isClosing) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val sheetHeightPx = with(baseDensity) { maxHeight.toPx() }.coerceAtLeast(1f)

        // Full-drawer nested-scroll swipe-down-to-close:
        // Synchronously intercepts downward pulls at the top before Android stretch overscroll can steal them,
        // and automatically glides closed on release or downward flick!
        val dismissNestedScroll = remember(sheetHeightPx) {
            object : NestedScrollConnection {
                private fun isScrolledToTop(): Boolean = if (currentIsGridMode) {
                    gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                } else {
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (isClosing || !currentIsVisible || source != NestedScrollSource.UserInput) return Offset.Zero
                    if (isUserDragging && available.y != 0f) {
                        val next = (activeDragFraction + available.y / sheetHeightPx).coerceIn(0f, 1f)
                        val consumedY = (next - activeDragFraction) * sheetHeightPx
                        activeDragFraction = next
                        if (next == 0f && available.y < 0f) {
                            isUserDragging = false
                        }
                        return Offset(0f, consumedY)
                    }
                    if (!isUserDragging && available.y > 0f && isScrolledToTop()) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        isUserDragging = true
                        val next = (available.y / sheetHeightPx).coerceIn(0f, 1f)
                        activeDragFraction = next
                        return Offset(0f, next * sheetHeightPx)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (isClosing || !currentIsVisible || source != NestedScrollSource.UserInput) return Offset.Zero
                    if (available.y > 0f) {
                        if (!isUserDragging) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            isUserDragging = true
                        }
                        val next = (activeDragFraction + available.y / sheetHeightPx).coerceIn(0f, 1f)
                        val consumedY = (next - activeDragFraction) * sheetHeightPx
                        activeDragFraction = next
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (isClosing || !currentIsVisible) return Velocity.Zero
                    if (isUserDragging || activeDragFraction > 0f) {
                        settleAfterDrag(available.y)
                        return available
                    }
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (isClosing || !currentIsVisible) return Velocity.Zero
                    if (isUserDragging || activeDragFraction > 0f) {
                        settleAfterDrag(available.y)
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

        // Keep system bar insets & swipe gesture coordinates at native system density,
        // and apply the user's App Drawer Scale (DPI %) cleanly inside the drawer content.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CompositionLocalProvider(LocalDensity provides drawerDensity) {
                val innerDensity = LocalDensity.current
                val imeBottomPadding = if (isClosing || isSettledClosed) {
                    0.dp
                } else {
                    WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
            // Finger-following swipe-down chrome (header + search bar + filter strip close immediately from any scroll depth!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(sheetHeightPx) {
                        val velocityTracker = VelocityTracker()
                        var cumulativeDragY = 0f
                        detectVerticalDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                                cumulativeDragY = 0f
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (isClosing || !currentIsVisible) return@detectVerticalDragGestures
                                if (dragAmount > 0f || isUserDragging) {
                                    change.consume()
                                    isUserDragging = true
                                    cumulativeDragY += dragAmount
                                    velocityTracker.addPosition(change.uptimeMillis, Offset(0f, cumulativeDragY))
                                    activeDragFraction = (activeDragFraction + dragAmount / sheetHeightPx)
                                        .coerceIn(0f, 1f)
                                }
                            },
                            onDragEnd = {
                                if (isUserDragging || activeDragFraction > 0f) {
                                    val velocityY = velocityTracker.calculateVelocity().y
                                    settleAfterDrag(velocityY)
                                }
                            },
                            onDragCancel = {
                                if (isUserDragging || activeDragFraction > 0f) {
                                    settleAfterDrag(0f)
                                }
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 2.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "All Apps",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${filteredApps.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                trimmedQuery.isNotEmpty() -> "Matching \"$trimmedQuery\" • Enter to launch"
                                activeFilter != AllAppsFilter.ALL -> "${activeFilter.label} • Hold app for actions"
                                else -> "${activeSort.label} • Hold any app to pin or uninstall"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Compact Sort Cycle Pill (A-Z -> Newest -> Recent)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val all = AllAppsSort.entries
                                    activeSort = all[(activeSort.ordinal + 1) % all.size]
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = activeSort.icon,
                                    contentDescription = "Sort mode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = activeSort.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        // Grid / List Toggle
                        IconButton(
                            onClick = { onToggleGridMode(!isGridMode) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isGridMode) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                                contentDescription = if (isGridMode) "Switch to list view" else "Switch to grid view",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings
                        IconButton(
                            onClick = {
                                if (!isClosing) {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onOpenSettings()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Room Launcher Settings",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close
                        IconButton(
                            onClick = animateCloseAndDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close all apps",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Sleek Compact Pill Search Bar with Instant Enter-to-Launch (also part of swipe-down chrome!)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .height(44.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search apps…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        val firstMatch = filteredApps.firstOrNull()
                                        if (firstMatch != null && !isClosing) {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            onLaunchApp(firstMatch)
                                        }
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Compact Interactive Filter Pill Strip (vertical downward swipes here also dismiss!)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AllAppsFilter.entries, key = { it.name }) { filter ->
                        val selected = activeFilter == filter
                        val countBadge = when (filter) {
                            AllAppsFilter.ALL -> null
                            AllAppsFilter.PINNED -> pinnedApps.size
                            AllAppsFilter.RECENT -> recentPackages.count { it in appsByPackage }
                            AllAppsFilter.GAMES -> gamesCount
                            AllAppsFilter.UNSORTED -> unsortedCount
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    activeFilter = filter
                                    coroutineScope.launch {
                                        if (isGridMode) gridState.scrollToItem(0) else listState.scrollToItem(0)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(13.5.dp)
                                )
                                Text(
                                    text = if (countBadge != null && countBadge > 0) {
                                        "${filter.label} ($countBadge)"
                                    } else {
                                        filter.label
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Main Content: Adaptive Grid or Enhanced List + A–Z Fast-Scroll Rail + Swipe-Down Nested Scroll
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(dismissNestedScroll)
            ) {
                val containerHeightPx = with(innerDensity) { maxHeight.toPx() }

                if (filteredApps.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = activeFilter.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when {
                                trimmedQuery.isNotEmpty() -> "No apps matching \"$trimmedQuery\""
                                activeFilter == AllAppsFilter.PINNED -> "No pinned apps yet"
                                activeFilter == AllAppsFilter.RECENT -> "No recently launched apps yet"
                                activeFilter == AllAppsFilter.GAMES -> "No games detected"
                                activeFilter == AllAppsFilter.UNSORTED -> "All apps are neatly organized in rooms!"
                                else -> "Loading your apps…"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (activeFilter == AllAppsFilter.PINNED) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Long-press any app in All Apps to pin it here for 1-tap access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                textAlign = TextAlign.Center
                            )
                        }
                        if (activeFilter != AllAppsFilter.ALL || trimmedQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    activeFilter = AllAppsFilter.ALL
                                }
                            ) {
                                Text("Show All Apps")
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        SettingsDeckCard(onClick = { if (!isClosing) onOpenSettings() })
                    }
                } else {
                    val startContentPad = 10.dp
                    val endContentPad = if (showScrubRail) 30.dp else 10.dp
                    val availableGridWidth = (maxWidth - startContentPad - endContentPad).coerceAtLeast(180.dp)
                    val gridColumns = when {
                        availableGridWidth >= 420.dp -> 5
                        availableGridWidth >= 260.dp -> 4
                        else -> 3
                    }
                    val cellWidth = ((availableGridWidth - (4.dp * (gridColumns - 1))) / gridColumns)
                        .coerceAtLeast(48.dp)
                    val gridIconSize = minOf(48.dp, (cellWidth - 10.dp).coerceAtLeast(38.dp))

                    if (isGridMode) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            state = gridState,
                            contentPadding = PaddingValues(
                                start = startContentPad,
                                end = endContentPad,
                                top = 8.dp,
                                bottom = 28.dp + imeBottomPadding
                            ),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            gridItems(
                                items = flatEntries,
                                key = { entry ->
                                    when (entry) {
                                        is AllAppsEntry.PinnedDock -> "pinned_dock"
                                        is AllAppsEntry.Header -> "hdr_${entry.letter}"
                                        is AllAppsEntry.AppItem -> entry.app.packageName
                                        AllAppsEntry.SettingsCard -> "footer_settings"
                                    }
                                },
                                span = { entry ->
                                    when (entry) {
                                        is AllAppsEntry.AppItem -> GridItemSpan(1)
                                        else -> GridItemSpan(maxLineSpan)
                                    }
                                },
                                contentType = { entry ->
                                    when (entry) {
                                        is AllAppsEntry.PinnedDock -> "pinned_dock"
                                        is AllAppsEntry.Header -> "header"
                                        is AllAppsEntry.AppItem -> "grid_app"
                                        AllAppsEntry.SettingsCard -> "settings"
                                    }
                                }
                            ) { entry ->
                                when (entry) {
                                    is AllAppsEntry.PinnedDock -> {
                                        PinnedAppsDockCard(
                                            pinnedApps = entry.apps,
                                            onLaunch = { if (!isClosing) onLaunchApp(it) },
                                            onLongPress = {
                                                if (!isClosing) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    selectedAppForMenu = it
                                                }
                                            }
                                        )
                                    }
                                    is AllAppsEntry.Header -> {
                                        FastLetterHeader(
                                            letter = entry.letter,
                                            count = entry.count,
                                            isHighlighted = activeScrubLetter == entry.letter
                                        )
                                    }
                                    is AllAppsEntry.AppItem -> {
                                        GridAppTile(
                                            app = entry.app,
                                            iconSize = gridIconSize,
                                            isPinned = entry.isPinned,
                                            isAssignedToRoom = entry.isAssignedToRoom,
                                            highlightQuery = trimmedQuery,
                                            onLaunch = {
                                                if (!isClosing) onLaunchApp(entry.app)
                                            },
                                            onLongPress = {
                                                if (!isClosing) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    selectedAppForMenu = entry.app
                                                }
                                            }
                                        )
                                    }
                                    AllAppsEntry.SettingsCard -> {
                                        Box(modifier = Modifier.padding(top = 14.dp)) {
                                            SettingsDeckCard(onClick = { if (!isClosing) onOpenSettings() })
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = endContentPad,
                                top = 4.dp,
                                bottom = 28.dp + imeBottomPadding
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = flatEntries,
                                key = { entry ->
                                    when (entry) {
                                        is AllAppsEntry.PinnedDock -> "pinned_dock"
                                        is AllAppsEntry.Header -> "hdr_${entry.letter}"
                                        is AllAppsEntry.AppItem -> entry.app.packageName
                                        AllAppsEntry.SettingsCard -> "footer_settings"
                                    }
                                },
                                contentType = { entry ->
                                    when (entry) {
                                        is AllAppsEntry.PinnedDock -> "pinned_dock"
                                        is AllAppsEntry.Header -> "header"
                                        is AllAppsEntry.AppItem -> "list_app"
                                        AllAppsEntry.SettingsCard -> "settings"
                                    }
                                }
                            ) { entry ->
                                when (entry) {
                                    is AllAppsEntry.PinnedDock -> {
                                        PinnedAppsDockCard(
                                            pinnedApps = entry.apps,
                                            onLaunch = { if (!isClosing) onLaunchApp(it) },
                                            onLongPress = {
                                                if (!isClosing) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    selectedAppForMenu = it
                                                }
                                            }
                                        )
                                    }
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
                                            isPinned = entry.isPinned,
                                            highlightQuery = trimmedQuery,
                                            onLaunch = {
                                                if (!isClosing) onLaunchApp(entry.app)
                                            },
                                            onTogglePin = {
                                                if (!isClosing) onTogglePinApp(entry.app.packageName)
                                            },
                                            onOpenMenu = {
                                                if (!isClosing) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    selectedAppForMenu = entry.app
                                                }
                                            }
                                        )
                                    }
                                    AllAppsEntry.SettingsCard -> {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        SettingsDeckCard(onClick = { if (!isClosing) onOpenSettings() })
                                    }
                                }
                            }
                        }
                    }

                    // Ultra-Lightweight A–Z Fast Scroller Rail (works in BOTH Grid and List mode!)
                    if (showScrubRail) {
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
                                            if (isGridMode) {
                                                gridState.scrollToItem(targetIdx)
                                            } else {
                                                listState.scrollToItem(targetIdx)
                                            }
                                        }
                                    }
                                }
                            },
                            onScrubEnd = { activeScrubLetter = null },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = 5.dp, top = 6.dp, bottom = 10.dp)
                        )
                    }

                    val shownLetter = activeScrubLetter
                    if (shownLetter != null) {
                        val bubbleMaxY = (containerHeightPx - with(innerDensity) { 80.dp.toPx() }).coerceAtLeast(0f)
                        val bubbleY = (scrubFractionY * bubbleMaxY).coerceIn(0f, bubbleMaxY)
                        val shownCount = letterCounts[shownLetter] ?: 0
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 28.dp,
                                bottomStart = 28.dp,
                                topEnd = 28.dp,
                                bottomEnd = 6.dp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset {
                                    IntOffset(
                                        x = -with(innerDensity) { 40.dp.toPx() }.roundToInt(),
                                        y = bubbleY.roundToInt()
                                    )
                                }
                                .size(54.dp)
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
        }
    }

    // Modern App Actions Modal (Open, Pin, App Info, Uninstall + Quick Room Assignment)
    val menuApp = selectedAppForMenu
    if (menuApp != null && isVisible && !isClosing) {
        AppActionsModal(
            app = menuApp,
            ownerLabel = packageOwnerLabels[menuApp.packageName] ?: defaultUnsortedLabel,
            isPinned = menuApp.packageName in pinnedSet,
            panelHotspotChoices = panelHotspotChoices,
            onLaunch = {
                selectedAppForMenu = null
                onLaunchApp(menuApp)
            },
            onTogglePin = {
                onTogglePinApp(menuApp.packageName)
            },
            onOpenAppInfo = {
                val pkg = menuApp.packageName
                selectedAppForMenu = null
                SystemIntentHelper.openAppInfoSettings(context, pkg)
            },
            onUninstall = {
                val pkg = menuApp.packageName
                selectedAppForMenu = null
                onUninstallApp(pkg)
            },
            onMoveToHotspot = { hotspotId ->
                onMoveApp(menuApp.packageName, hotspotId)
                selectedAppForMenu = null
            },
            onDismiss = { selectedAppForMenu = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedAppsDockCard(
    pinnedApps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Pinned Favorites",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pinnedApps, key = { "pin_${it.packageName}" }) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { onLaunch(app) },
                                onLongClick = { onLongPress(app) }
                            )
                            .padding(vertical = 3.dp, horizontal = 2.dp)
                    ) {
                        AppIconView(app = app, size = 40.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            fontSize = 10.5.sp,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridAppTile(
    app: AppInfo,
    iconSize: Dp,
    isPinned: Boolean,
    isAssignedToRoom: Boolean,
    highlightQuery: String,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val highlightedLabel = rememberHighlightedLabel(app.label, highlightQuery, primaryColor)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = onLongPress
            )
            .padding(horizontal = 3.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIconView(app = app, size = iconSize)
            if (isPinned) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(9.5.dp)
                    )
                }
            } else if (isAssignedToRoom) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            if (highlightedLabel != null) {
                Text(
                    text = highlightedLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = app.label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun rememberHighlightedLabel(
    label: String,
    highlightQuery: String,
    primaryColor: Color
) = if (highlightQuery.isEmpty()) {
    null
} else {
    remember(label, highlightQuery, primaryColor) {
        val matchStart = label.indexOf(highlightQuery, ignoreCase = true)
        if (matchStart >= 0) {
            val matchEnd = (matchStart + highlightQuery.length).coerceAtMost(label.length)
            buildAnnotatedString {
                append(label.substring(0, matchStart))
                withStyle(
                    SpanStyle(
                        color = primaryColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append(label.substring(matchStart, matchEnd))
                }
                append(label.substring(matchEnd))
            }
        } else {
            null
        }
    }
}

@Composable
private fun AppActionsModal(
    app: AppInfo,
    ownerLabel: String,
    isPinned: Boolean,
    panelHotspotChoices: List<Pair<String, RoomHotspot>>,
    onLaunch: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onMoveToHotspot: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Icon + Name + Package + Current Room Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppIconView(app = app, size = 44.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = ownerLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close actions")
                    }
                }

                // 4 Prominent Quick Actions (Open, Pin, App Info, Uninstall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Rounded.OpenInNew,
                        label = "Open",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onLaunch,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = if (isPinned) Icons.Rounded.Star else Icons.Rounded.PushPin,
                        label = if (isPinned) "Unpin" else "Pin",
                        containerColor = if (isPinned) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (isPinned) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = onTogglePin,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.Info,
                        label = "App Info",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onOpenAppInfo,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.DeleteOutline,
                        label = if (app.isSystemApp) "Disable" else "Uninstall",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = if (app.isSystemApp) onOpenAppInfo else onUninstall,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Move / Assign to any Room Object
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Move to Room Object",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        panelHotspotChoices.forEach { (roomName, target) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .clickable { onMoveToHotspot(target.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DriveFileMove,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = target.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
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
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            .padding(top = 8.dp, bottom = 2.dp)
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
    isPinned: Boolean,
    highlightQuery: String,
    onLaunch: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val displayTitle = rememberHighlightedLabel(app.label, highlightQuery, primaryColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = onOpenMenu
            )
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconView(app = app, size = 38.dp)
        Spacer(modifier = Modifier.width(10.dp))
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
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (isPinned) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                contentDescription = if (isPinned) "Unpin app" else "Pin app",
                tint = if (isPinned) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
                },
                modifier = Modifier.size(19.dp)
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
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
            .width(20.dp)
            .clip(RoundedCornerShape(10.dp))
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
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
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
