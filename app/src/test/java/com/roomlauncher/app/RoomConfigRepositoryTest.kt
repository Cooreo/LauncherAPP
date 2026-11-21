package com.roomlauncher.app

import android.content.pm.ApplicationInfo
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomConfigRepository
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.WebShortcut
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.RoomStyleId
import com.roomlauncher.app.ui.theme.RoomThemeId
import com.roomlauncher.app.ui.theme.RoomThemes
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RoomConfigRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun defaultRoomContainsAllNineObjects() {
        val config = RoomConfigRepository.createDefaultRoomConfig()
        val byId = config.hotspots.associateBy { it.id }

        assertEquals(9, config.hotspots.size)
        assertEquals(HotspotActionType.DIRECT_WEATHER, byId[RoomConfigRepository.ID_WINDOW]?.actionType)
        assertEquals(RoomObjectVisual.WINDOW, byId[RoomConfigRepository.ID_WINDOW]?.visualType)
        assertEquals(HotspotActionType.DIRECT_GALLERY, byId[RoomConfigRepository.ID_PAINTING]?.actionType)
        assertEquals(HotspotActionType.DIRECT_ALARM, byId[RoomConfigRepository.ID_CLOCK]?.actionType)
        assertEquals(HotspotActionType.PANEL, byId[RoomConfigRepository.ID_STICKY_NOTE]?.actionType)
        assertEquals(HotspotActionType.PANEL, byId[RoomConfigRepository.ID_TV]?.actionType)
        assertEquals(HotspotActionType.PANEL, byId[RoomConfigRepository.ID_CONSOLE]?.actionType)
        assertEquals(HotspotActionType.PANEL, byId[RoomConfigRepository.ID_SHELF]?.actionType)
        assertEquals(HotspotActionType.PANEL, byId[RoomConfigRepository.ID_BED]?.actionType)
        assertEquals(HotspotActionType.UNSORTED_PANEL, byId[RoomConfigRepository.ID_TRASH_CAN]?.actionType)
    }

    @Test
    fun jsonPersistenceRoundTripPreservesSettingsAndWebShortcuts() = runTest {
        val file = File(tempFolder.root, "room_config.json")
        val repo = RoomConfigRepository(file)

        val initial = RoomConfigRepository.createDefaultRoomConfig().copy(
            onboardingCompleted = true,
            onboardingStep = 3,
            autoPopulated = true,
            roomStyleId = RoomStyleId.TEEN_ROOM,
            themeId = RoomThemeId.MIDNIGHT_COZY,
            dynamicThemeEnabled = true,
            useQuickAnimations = true,
            chargingAnimationEnabled = true,
            chargingAnimationStyle = ChargingAnimationStyle.LIQUID_GLASS,
            weatherEnabled = true,
            mirrorHomeWallpaper = true,
            posterLeftPath = "/tmp/poster_left.jpg",
            posterRightPath = "/tmp/poster_right.jpg",
            webShortcuts = listOf(WebShortcut("w1", "GitHub", "https://github.com"))
        )
        repo.saveConfig(initial)

        val reloaded = repo.loadConfig()
        assertEquals(initial, reloaded)
    }

    @Test
    fun autoPopulateCategorizesGamesHealthClockWebAndNothingGetsLost() {
        val apps = listOf(
            AppInfo(packageName = "com.google.android.youtube", label = "YouTube"),
            AppInfo(packageName = "com.mojang.minecraftpe", label = "Minecraft", category = ApplicationInfo.CATEGORY_GAME),
            AppInfo(packageName = "md.obsidian", label = "Obsidian"),
            AppInfo(packageName = "com.calm.android", label = "Calm"),
            AppInfo(packageName = "com.sec.android.app.shealth", label = "Samsung Health"),
            AppInfo(packageName = "com.google.android.deskclock", label = "Clock"),
            AppInfo(packageName = "com.android.chrome", label = "Chrome"),
            AppInfo(packageName = "com.example.unmatched", label = "Random Utility")
        )

        val populated = RoomConfigRepository.autoPopulateIfNeeded(
            RoomConfigRepository.createDefaultRoomConfig(),
            apps
        )

        val byId = populated.hotspots.associateBy { it.id }
        val tvPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_TV), populated, apps).map { it.packageName }
        val consolePkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_CONSOLE), populated, apps).map { it.packageName }
        val shelfPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_SHELF), populated, apps).map { it.packageName }
        val bedPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_BED), populated, apps).map { it.packageName }
        val stickyPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_STICKY_NOTE), populated, apps).map { it.packageName }
        val trashPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_TRASH_CAN), populated, apps).map { it.packageName }

        assertEquals(listOf("com.google.android.youtube"), tvPkgs)
        assertEquals(listOf("com.mojang.minecraftpe"), consolePkgs)
        assertEquals(listOf("md.obsidian"), shelfPkgs)
        assertTrue("com.calm.android" in bedPkgs)
        assertTrue("com.sec.android.app.shealth" in bedPkgs)
        assertTrue("com.google.android.deskclock" in bedPkgs)
        assertEquals(listOf("com.android.chrome"), stickyPkgs)
        assertEquals(listOf("com.example.unmatched"), trashPkgs)
    }

    @Test
    fun legacyV1ConfigMigratesCleanlyWithoutLosingUserApps() {
        val v1Config = RoomConfig(
            version = 1,
            onboardingCompleted = true,
            autoPopulated = true,
            hotspots = listOf(
                RoomHotspot(
                    id = RoomConfigRepository.ID_TV,
                    label = "TV",
                    x = 0.045f,
                    y = 0.32f,
                    width = 0.50f,
                    height = 0.23f,
                    actionType = HotspotActionType.PANEL,
                    packages = listOf("com.google.android.youtube")
                ),
                RoomHotspot(
                    id = RoomConfigRepository.ID_TRASH_CAN,
                    label = "Trash Can",
                    x = 0.69f,
                    y = 0.685f,
                    width = 0.24f,
                    height = 0.155f,
                    actionType = HotspotActionType.UNSORTED_PANEL
                )
            )
        )

        val migrated = RoomConfigRepository.migrateIfNeeded(v1Config)
        val byId = migrated.hotspots.associateBy { it.id }

        assertEquals(2, migrated.version)
        assertEquals(listOf("com.google.android.youtube"), byId[RoomConfigRepository.ID_TV]?.packages)
        assertEquals(RoomObjectVisual.TV, byId[RoomConfigRepository.ID_TV]?.visualType)
        assertTrue(RoomConfigRepository.ID_WINDOW in byId)
        assertTrue(RoomConfigRepository.ID_CONSOLE in byId)
        assertTrue(RoomConfigRepository.ID_STICKY_NOTE in byId)
        assertFalse(byId[RoomConfigRepository.ID_TV]!!.height > 0.19f)
    }

    @Test
    fun dynamicThemeFollowsDayAfternoonAndNightSchedule() {
        assertEquals(RoomThemeId.WARM_LINEN, RoomThemes.dynamicThemeForHour(9))
        assertEquals(RoomThemeId.WARM_LINEN, RoomThemes.dynamicThemeForHour(14))
        assertEquals(RoomThemeId.MIDNIGHT_COZY, RoomThemes.dynamicThemeForHour(15))
        assertEquals(RoomThemeId.MIDNIGHT_COZY, RoomThemes.dynamicThemeForHour(20))
        assertEquals(RoomThemeId.OLED_PITCH, RoomThemes.dynamicThemeForHour(21))
        assertEquals(RoomThemeId.OLED_PITCH, RoomThemes.dynamicThemeForHour(3))
    }
}
