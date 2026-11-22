package com.roomlauncher.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.RoomType
import com.roomlauncher.app.data.WeatherCondition
import com.roomlauncher.app.data.WeatherState
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.room.RoomObjectGraphic
import com.roomlauncher.app.ui.room.RoomShellCanvas
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
import com.roomlauncher.app.ui.theme.LocalRoomPalette
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import com.roomlauncher.app.ui.theme.RoomThemes
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    currentStep: Int,
    installedAppCount: Int,
    roomStyle: RoomStyleId,
    themeId: RoomThemeId,
    gdSubTheme: GeometryDashSubTheme,
    weatherEnabled: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    isDefaultLauncher: Boolean,
    onSelectRoomStyle: (RoomStyleId) -> Unit,
    onSelectTheme: (RoomThemeId) -> Unit,
    onSelectGeometryDashSubTheme: (GeometryDashSubTheme) -> Unit,
    onToggleWeather: (Boolean) -> Unit,
    onStepChange: (Int) -> Unit,
    onRefreshStatus: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val totalSteps = 4

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshStatus()
        if (SystemIntentHelper.isDefaultHomeLauncher(context)) {
            onComplete()
        }
    }

    val stepTitles = listOf("Welcome", "Style", "Speed", "Home")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Top Step Progress Bar + Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalSteps) { index ->
                            val active = index == currentStep
                            val passed = index < currentStep
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    active -> MaterialTheme.colorScheme.primary
                                    passed -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onStepChange(index) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = if (active) 10.dp else 6.dp,
                                        vertical = 4.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (passed) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    if (active) {
                                        Text(
                                            text = "${index + 1}. ${stepTitles[index]}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TextButton(onClick = onComplete) {
                        Text(
                            text = if (currentStep == totalSteps - 1) "Finish" else "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Animated Step Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it / 5 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it / 5 } + fadeOut())
                            } else {
                                (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it / 5 } + fadeOut())
                            }
                        },
                        label = "onboarding_step_content"
                    ) { step ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (step) {
                                0 -> WelcomeShowcaseStep(
                                    roomStyle = roomStyle,
                                    gdSubTheme = gdSubTheme,
                                    weatherEnabled = weatherEnabled
                                )
                                1 -> PersonalizeRoomStep(
                                    roomStyle = roomStyle,
                                    themeId = themeId,
                                    gdSubTheme = gdSubTheme,
                                    weatherEnabled = weatherEnabled,
                                    onSelectRoomStyle = onSelectRoomStyle,
                                    onSelectTheme = onSelectTheme,
                                    onSelectGeometryDashSubTheme = onSelectGeometryDashSubTheme
                                )
                                2 -> SpeedAndReliabilityStep(
                                    installedAppCount = installedAppCount,
                                    weatherEnabled = weatherEnabled,
                                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                                    onToggleWeather = onToggleWeather,
                                    onRequestIgnoreBattery = {
                                        SystemIntentHelper.requestIgnoreBatteryOptimizations(context)
                                    },
                                    onOpenAppInfo = {
                                        SystemIntentHelper.openAppInfoSettings(context)
                                    }
                                )
                                else -> MoveInDefaultHomeStep(
                                    roomStyle = roomStyle,
                                    themeId = themeId,
                                    gdSubTheme = gdSubTheme,
                                    weatherEnabled = weatherEnabled,
                                    installedAppCount = installedAppCount,
                                    isDefaultLauncher = isDefaultLauncher,
                                    onSetDefaultLauncher = {
                                        onStepChange(3)
                                        val roleIntent = SystemIntentHelper.createDefaultHomeRoleIntent(context)
                                        if (roleIntent != null) {
                                            roleLauncher.launch(roleIntent)
                                        } else {
                                            SystemIntentHelper.openLegacyHomeChooser(context)
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { onStepChange(currentStep - 1) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                onStepChange(currentStep + 1)
                            } else {
                                onComplete()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = when (currentStep) {
                                0 -> "Customize My Room"
                                1 -> "Next: Speed & Weather"
                                2 -> "Final Step"
                                else -> "Enter My Room"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveMiniRoomPreview(
    roomStyle: RoomStyleId,
    gdSubTheme: GeometryDashSubTheme,
    weatherEnabled: Boolean,
    badgeText: String,
    heightDp: Int = 215
) {
    val palette = LocalRoomPalette.current
    val density = LocalDensity.current
    val sampleWeather = WeatherState(
        temperatureC = 21,
        condition = WeatherCondition.PARTLY_CLOUDY,
        description = "Partly Cloudy"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
                shape = RoundedCornerShape(26.dp)
            )
    ) {
        RoomShellCanvas(
            palette = palette,
            roomStyle = roomStyle,
            roomType = RoomType.BEDROOM,
            gdSubTheme = gdSubTheme,
            modifier = Modifier.fillMaxSize()
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }

            @Composable
            fun miniObj(
                visual: RoomObjectVisual,
                x: Float,
                y: Float,
                w: Float,
                h: Float
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset((x * wPx).roundToInt(), (y * hPx).roundToInt())
                        }
                        .size(
                            width = with(density) { (w * wPx).toDp() },
                            height = with(density) { (h * hPx).toDp() }
                        )
                ) {
                    RoomObjectGraphic(
                        visualType = visual,
                        palette = palette,
                        roomStyle = roomStyle,
                        gdSubTheme = gdSubTheme,
                        weatherState = sampleWeather,
                        weatherEnabled = weatherEnabled,
                        frameImageBitmap = null,
                        currentMinutesOfDay = 10 * 60 + 10,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            miniObj(RoomObjectVisual.WINDOW, 0.06f, 0.08f, 0.25f, 0.30f)
            miniObj(RoomObjectVisual.PAINTING, 0.37f, 0.09f, 0.22f, 0.24f)
            miniObj(RoomObjectVisual.CLOCK, 0.68f, 0.08f, 0.14f, 0.22f)
            miniObj(RoomObjectVisual.TV, 0.06f, 0.43f, 0.32f, 0.24f)
            miniObj(RoomObjectVisual.SHELF, 0.67f, 0.36f, 0.26f, 0.34f)
            miniObj(RoomObjectVisual.BED, 0.10f, 0.70f, 0.46f, 0.25f)
            miniObj(RoomObjectVisual.TRASH_CAN, 0.72f, 0.73f, 0.16f, 0.20f)
        }

        // Floating Live Preview Tag
        Surface(
            shape = CircleShape,
            color = Color(0xCC11161F),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFD86B),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun WelcomeShowcaseStep(
    roomStyle: RoomStyleId,
    gdSubTheme: GeometryDashSubTheme,
    weatherEnabled: Boolean
) {
    LiveMiniRoomPreview(
        roomStyle = roomStyle,
        gdSubTheme = gdSubTheme,
        weatherEnabled = weatherEnabled,
        badgeText = "Interactive Room",
        heightDp = 225
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Welcome to Room Launcher",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Turn your phone into a living multi-room home instead of a cluttered grid of app icons.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    FeatureHighlightCard(
        icon = Icons.Rounded.TouchApp,
        title = "Every Object Opens Your Apps",
        description = "Tap the TV for media, Console for games, Window for live weather, Painting for photos, Clock for alarms & Trash Can for unsorted apps."
    )
    FeatureHighlightCard(
        icon = Icons.Rounded.SwipeLeft,
        title = "4 Swipeable Rooms Included",
        description = "Swipe sideways to walk between Bedroom, Living Room, Kitchen & Gaming Room—or long-press anywhere to move & resize furniture."
    )
    FeatureHighlightCard(
        icon = Icons.Rounded.SwipeUp,
        title = "Instant All Apps Drawer",
        description = "Swipe up from the bottom for the redesigned Grid & List app drawer with search, pinned favorites, filters & 1-tap uninstall."
    )
}

@Composable
private fun PersonalizeRoomStep(
    roomStyle: RoomStyleId,
    themeId: RoomThemeId,
    gdSubTheme: GeometryDashSubTheme,
    weatherEnabled: Boolean,
    onSelectRoomStyle: (RoomStyleId) -> Unit,
    onSelectTheme: (RoomThemeId) -> Unit,
    onSelectGeometryDashSubTheme: (GeometryDashSubTheme) -> Unit
) {
    LiveMiniRoomPreview(
        roomStyle = roomStyle,
        gdSubTheme = gdSubTheme,
        weatherEnabled = weatherEnabled,
        badgeText = roomStyle.displayName,
        heightDp = 195
    )

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Pick Your Room Style",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "Watch your room update live above. You can switch anytime in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Room Style Selector Cards
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(RoomStyleId.entries, key = { it.name }) { style ->
            val selected = style == roomStyle
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                modifier = Modifier
                    .width(156.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectRoomStyle(style) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = style.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Color Palette OR Geometry Dash Sub-Theme Selector
    if (roomStyle == RoomStyleId.GEOMETRY_DASH) {
        Text(
            text = "Geometry Dash Level Vibe",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GeometryDashSubTheme.entries.forEach { sub ->
                val selected = sub == gdSubTheme
                val previewPalette = RoomThemes.geometryDashPalette(sub)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectGeometryDashSubTheme(sub) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(previewPalette.wallMain)
                                .border(2.5.dp, previewPalette.consoleAccent, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sub.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = sub.creatorTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    } else {
        Text(
            text = "Color Palette",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(RoomThemeId.entries, key = { it.name }) { theme ->
                val selected = theme == themeId
                val preview = RoomThemes.paletteFor(theme)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier
                        .width(126.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectTheme(theme) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(preview.wallMain)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.35f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(preview.duvetPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(preview.throwBlanket)
                            )
                        }
                        Text(
                            text = theme.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedAndReliabilityStep(
    installedAppCount: Int,
    weatherEnabled: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onToggleWeather: (Boolean) -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    StepBadge(
        icon = Icons.Rounded.Bolt,
        label = "Performance & Live Features"
    )
    Text(
        text = "Built to Feel Instant",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Room Launcher pre-loads your apps once at boot and keeps your room awake for instant Home presses.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    StatusCard(
        isReady = installedAppCount > 0,
        readyTitle = "$installedAppCount Apps Cached at Boot",
        pendingTitle = "Warming Up App Cache…",
        detail = "Your app icons and A–Z list stay warm in memory and only update when an app is installed or uninstalled."
    )

    // Live Window Weather toggle right in onboarding
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Live Window Weather & Temp",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Shows live temperature badge, sky conditions, frost & rain in your room window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = weatherEnabled,
                onCheckedChange = onToggleWeather
            )
        }
    }

    // Battery Optimization Exemption
    StatusCard(
        isReady = isBatteryOptimizationIgnored,
        readyTitle = "Unrestricted Background Ready",
        pendingTitle = "Recommended: Disable Battery Optimization",
        detail = if (isBatteryOptimizationIgnored) {
            "Room Launcher is exempt from battery sleep and will stay snappy."
        } else {
            "Prevents Android / ColorOS from closing your room in the background when you switch apps."
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onRequestIgnoreBattery,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Rounded.BatteryChargingFull,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Unrestricted", maxLines = 1)
        }
        OutlinedButton(
            onClick = onOpenAppInfo,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("App Info", maxLines = 1)
        }
    }

    Text(
        text = "Tip for Oppo / ColorOS, OnePlus, Xiaomi & Samsung: open App Info → Battery usage → Allow background activity.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun MoveInDefaultHomeStep(
    roomStyle: RoomStyleId,
    themeId: RoomThemeId,
    gdSubTheme: GeometryDashSubTheme,
    weatherEnabled: Boolean,
    installedAppCount: Int,
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit
) {
    LiveMiniRoomPreview(
        roomStyle = roomStyle,
        gdSubTheme = gdSubTheme,
        weatherEnabled = weatherEnabled,
        badgeText = "Ready to Move In",
        heightDp = 195
    )

    Text(
        text = "Make It Your Home Screen",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Set Room Launcher as your default Home app so pressing Home or swiping up always brings you straight back to your room.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Summary Pill Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            label = "Style",
            value = roomStyle.displayName,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = "Palette",
            value = if (roomStyle == RoomStyleId.GEOMETRY_DASH) gdSubTheme.displayName else themeId.displayName,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = "Apps",
            value = "$installedAppCount Ready",
            modifier = Modifier.weight(0.85f)
        )
    }

    StatusCard(
        isReady = isDefaultLauncher,
        readyTitle = "Room Launcher Is Your Default Home!",
        pendingTitle = "Not Set as Default Home Yet",
        detail = if (isDefaultLauncher) {
            "Everything is set up! Tap \"Enter My Room\" below to start exploring."
        } else {
            "Tap below and select Room Launcher from the Android chooser. You'll enter your room automatically."
        }
    )

    Button(
        onClick = onSetDefaultLauncher,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Icon(Icons.Rounded.Home, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isDefaultLauncher) "Change Default Home App" else "Set as Default Launcher",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StepBadge(
    icon: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    isReady: Boolean,
    readyTitle: String,
    pendingTitle: String,
    detail: String
) {
    val containerColor = if (isReady) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val accentColor = if (isReady) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isReady) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (isReady) readyTitle else pendingTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
