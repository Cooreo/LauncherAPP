package com.roomlauncher.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roomlauncher.app.system.SystemIntentHelper
import com.roomlauncher.app.ui.theme.RoomSage
import com.roomlauncher.app.ui.theme.RoomSageContainer
import com.roomlauncher.app.ui.theme.RoomTerracotta

@Composable
fun OnboardingScreen(
    installedAppCount: Int,
    isBatteryOptimizationIgnored: Boolean,
    isDefaultLauncher: Boolean,
    onRefreshStatus: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 4

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshStatus()
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
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Top bar: step indicator + skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(totalSteps) { index ->
                        val active = index == currentStep
                        val passed = index < currentStep
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (active) 28.dp else 10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        active -> MaterialTheme.colorScheme.primary
                                        passed -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                    }
                }
                TextButton(onClick = onComplete) {
                    Text(
                        text = if (currentStep == totalSteps - 1) "Done" else "Skip",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated step body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "onboarding_step"
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (step) {
                            0 -> WelcomeStep()
                            1 -> AppVisibilityStep(installedAppCount = installedAppCount)
                            2 -> BatteryOptimizationStep(
                                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                                onRequestIgnoreBattery = {
                                    SystemIntentHelper.requestIgnoreBatteryOptimizations(context)
                                },
                                onOpenAppInfo = {
                                    SystemIntentHelper.openAppInfoSettings(context)
                                }
                            )
                            else -> DefaultLauncherStep(
                                isDefaultLauncher = isDefaultLauncher,
                                onSetDefaultLauncher = {
                                    val roleIntent = SystemIntentHelper.createDefaultHomeRoleIntent(context)
                                    if (roleIntent != null) {
                                        roleLauncher.launch(roleIntent)
                                    } else {
                                        SystemIntentHelper.openLegacyHomeChooser(context)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            onComplete()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (currentStep < totalSteps - 1) "Continue" else "Enter My Room",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    StepHeroBadge(
        icon = Icons.Rounded.Home,
        tint = RoomSage,
        stepLabel = "Step 1 of 4"
    )
    Text(
        text = "Welcome to Room Launcher",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Your phone home screen is now a calm, illustrated room instead of an endless grid of icons.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    FeatureCard(
        icon = Icons.Rounded.TouchApp,
        title = "Tap Objects in Your Room",
        description = "Tap the TV for media, Shelf for work, Bed for wind-down apps, Clock for alarms, Painting for your gallery, and Trash Can for unsorted apps."
    )
    FeatureCard(
        icon = Icons.Rounded.Tune,
        title = "Long-Press Anywhere to Edit",
        description = "Move and resize hotspots, rename objects, or drag apps cleanly between room panels."
    )
    FeatureCard(
        icon = Icons.Rounded.SwipeUp,
        title = "Swipe Up for All Apps (A–Z)",
        description = "Need an app immediately? Swipe up from the bottom or tap the subtle All Apps button for a fast searchable list."
    )
}

@Composable
private fun AppVisibilityStep(installedAppCount: Int) {
    StepHeroBadge(
        icon = Icons.Rounded.Apps,
        tint = RoomSage,
        stepLabel = "Step 2 of 4"
    )
    Text(
        text = "App Visibility",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Room Launcher declares QUERY_ALL_PACKAGES permission so it can discover every installed app, auto-organize your defaults, and guarantee nothing ever gets lost.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    StatusCard(
        isReady = installedAppCount > 0,
        readyTitle = "$installedAppCount Installed Apps Discovered",
        pendingTitle = "Scanning Installed Apps…",
        detail = "Any newly installed app—or any app you remove from another object—will automatically appear in the Trash Can until you place it elsewhere."
    )
}

@Composable
private fun BatteryOptimizationStep(
    isBatteryOptimizationIgnored: Boolean,
    onRequestIgnoreBattery: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    StepHeroBadge(
        icon = Icons.Rounded.BatteryChargingFull,
        tint = RoomTerracotta,
        stepLabel = "Step 3 of 4"
    )
    Text(
        text = "Keep Your Room Alive",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Android aggressively kills background apps to save battery. Disabling battery optimizations prevents your room from constantly reloading whenever you press Home.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    StatusCard(
        isReady = isBatteryOptimizationIgnored,
        readyTitle = "Battery Optimization Disabled",
        pendingTitle = "Battery Optimization Active",
        detail = if (isBatteryOptimizationIgnored) {
            "Room Launcher is exempt from battery optimizations and will stay responsive."
        } else {
            "Tap below to allow unrestricted background execution so your room opens instantaneously."
        }
    )

    Button(
        onClick = onRequestIgnoreBattery,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Disable Battery Optimization", modifier = Modifier.padding(vertical = 4.dp))
    }

    OutlinedButton(
        onClick = onOpenAppInfo,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Open App Info (Unrestricted Background)",
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }

    Text(
        text = "Tip: On Samsung, Xiaomi, or OnePlus devices, open App Info → Battery and select \"Unrestricted\".",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DefaultLauncherStep(
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit
) {
    StepHeroBadge(
        icon = Icons.Rounded.Home,
        tint = RoomSage,
        stepLabel = "Step 4 of 4"
    )
    Text(
        text = "Set as Default Home App",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Make Room Launcher your default Home app so pressing the Home button always returns you to your room.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    StatusCard(
        isReady = isDefaultLauncher,
        readyTitle = "Room Launcher Is Your Default Home",
        pendingTitle = "Not Set as Default Home Yet",
        detail = if (isDefaultLauncher) {
            "Everything is ready! Tap \"Enter My Room\" below to start using Room Launcher."
        } else {
            "Tap the button below and choose Room Launcher from the system prompt. You can always change this later in Android Settings."
        }
    )

    Button(
        onClick = onSetDefaultLauncher,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Rounded.Home, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isDefaultLauncher) "Change Default Home App" else "Set as Default Launcher",
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun StepHeroBadge(
    icon: ImageVector,
    tint: Color,
    stepLabel: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tint.copy(alpha = 0.14f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = stepLabel.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = tint
        )
    }
}

@Composable
private fun FeatureCard(
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
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RoomSageContainer)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RoomSage,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
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
    val containerColor = if (isReady) RoomSageContainer else MaterialTheme.colorScheme.surfaceVariant
    val accentColor = if (isReady) RoomSage else RoomTerracotta

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isReady) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isReady) readyTitle else pendingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
