package com.roomlauncher.app

import android.content.pm.ApplicationInfo
import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.ChangelogData
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.MusicDisplayMode
import com.roomlauncher.app.data.RoomConfig
import com.roomlauncher.app.data.RoomConfigRepository
import com.roomlauncher.app.data.RoomHotspot
import com.roomlauncher.app.data.RoomObjectVisual
import com.roomlauncher.app.data.RoomType
import com.roomlauncher.app.data.VisualizerStyle
import com.roomlauncher.app.data.WebShortcut
import com.roomlauncher.app.ui.theme.ChargingAnimationStyle
import com.roomlauncher.app.ui.theme.GeometryDashSubTheme
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
            musicVisualizerEnabled = true,
            musicVisualizerStyle = VisualizerStyle.VINYL_GROOVE,
            musicDefaultMode = MusicDisplayMode.LYRICS,
            audioReactiveVisualizerEnabled = true,
            autoCheckUpdatesEnabled = false,
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

        val byId = populated.allHotspots.associateBy { it.id }
        val tvPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_TV), populated, apps).map { it.packageName }
        val consolePkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_CONSOLE), populated, apps).map { it.packageName }
        val shelfPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_SHELF), populated, apps).map { it.packageName }
        val bedPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_BED), populated, apps).map { it.packageName }
        val stovePkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_KITCHEN_STOVE), populated, apps).map { it.packageName }
        val stickyPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_STICKY_NOTE), populated, apps).map { it.packageName }
        val trashPkgs = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_TRASH_CAN), populated, apps).map { it.packageName }

        assertEquals(listOf("com.google.android.youtube"), tvPkgs)
        assertEquals(listOf("com.mojang.minecraftpe"), consolePkgs)
        assertEquals(listOf("md.obsidian"), shelfPkgs)
        assertTrue("com.calm.android" in bedPkgs)
        assertTrue("com.google.android.deskclock" in bedPkgs)
        assertTrue("com.sec.android.app.shealth" in stovePkgs)
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

        assertEquals(3, migrated.version)
        assertEquals(4, migrated.effectiveRooms.size)
        assertEquals(RoomType.BEDROOM, migrated.effectiveRooms[0].type)
        assertEquals(RoomType.LIVING_ROOM, migrated.effectiveRooms[1].type)
        assertEquals(RoomType.KITCHEN, migrated.effectiveRooms[2].type)
        assertEquals(RoomType.GAMING_ROOM, migrated.effectiveRooms[3].type)
        assertEquals(listOf("com.google.android.youtube"), byId[RoomConfigRepository.ID_TV]?.packages)
        assertEquals(RoomObjectVisual.TV, byId[RoomConfigRepository.ID_TV]?.visualType)
        assertTrue(RoomConfigRepository.ID_WINDOW in byId)
        assertTrue(RoomConfigRepository.ID_CONSOLE in byId)
        assertTrue(RoomConfigRepository.ID_STICKY_NOTE in byId)
        assertFalse(byId[RoomConfigRepository.ID_TV]!!.height > 0.19f)
    }

    @Test
    fun defaultHouseContainsFourRoomsWithDistinctFurniture() {
        val config = RoomConfigRepository.createDefaultRoomConfig()
        assertEquals(3, config.version)
        assertEquals(4, config.effectiveRooms.size)

        val livingVisuals = config.effectiveRooms[1].hotspots.map { it.visualType }.toSet()
        val kitchenVisuals = config.effectiveRooms[2].hotspots.map { it.visualType }.toSet()
        val gamingVisuals = config.effectiveRooms[3].hotspots.map { it.visualType }.toSet()

        assertTrue(RoomObjectVisual.SOFA in livingVisuals)
        assertTrue(RoomObjectVisual.FIREPLACE in livingVisuals)
        assertTrue(RoomObjectVisual.VINYL_PLAYER in livingVisuals)
        assertTrue(RoomObjectVisual.COFFEE_TABLE in livingVisuals)

        assertTrue(RoomObjectVisual.FRIDGE in kitchenVisuals)
        assertTrue(RoomObjectVisual.COFFEE_MAKER in kitchenVisuals)
        assertTrue(RoomObjectVisual.STOVE in kitchenVisuals)
        assertTrue(RoomObjectVisual.KITCHEN_COUNTER in kitchenVisuals)

        assertTrue(RoomObjectVisual.GAMING_PC in gamingVisuals)
        assertTrue(RoomObjectVisual.ARCADE_CABINET in gamingVisuals)
        assertTrue(RoomObjectVisual.STREAMING_DESK in gamingVisuals)
        assertTrue(RoomObjectVisual.TROPHY_SHELF in gamingVisuals)
    }

    @Test
    fun multiRoomAutoPopulateDistributesAppsAcrossAllRooms() {
        val apps = listOf(
            AppInfo(packageName = "com.whatsapp", label = "WhatsApp"),
            AppInfo(packageName = "com.spotify.music", label = "Spotify"),
            AppInfo(packageName = "com.ubercab.eats", label = "Uber Eats"),
            AppInfo(packageName = "com.paypal.android.p2pmobile", label = "PayPal"),
            AppInfo(packageName = "com.amazon.mShop.android.shopping", label = "Amazon Shopping"),
            AppInfo(packageName = "com.discord", label = "Discord"),
            AppInfo(packageName = "com.github.android", label = "GitHub"),
            AppInfo(packageName = "com.example.mystery", label = "Mystery Tool")
        )

        val populated = RoomConfigRepository.autoPopulateIfNeeded(
            RoomConfigRepository.createDefaultRoomConfig(),
            apps
        )

        val byId = populated.allHotspots.associateBy { it.id }
        assertTrue("com.whatsapp" in byId.getValue(RoomConfigRepository.ID_LIVING_SOFA).packages)
        assertTrue("com.spotify.music" in byId.getValue(RoomConfigRepository.ID_LIVING_VINYL).packages)
        assertTrue("com.ubercab.eats" in byId.getValue(RoomConfigRepository.ID_KITCHEN_FRIDGE).packages)
        assertTrue("com.paypal.android.p2pmobile" in byId.getValue(RoomConfigRepository.ID_KITCHEN_COFFEE).packages)
        assertTrue("com.amazon.mShop.android.shopping" in byId.getValue(RoomConfigRepository.ID_KITCHEN_COUNTER).packages)
        assertTrue("com.discord" in byId.getValue(RoomConfigRepository.ID_GAMING_DESK).packages)
        assertTrue("com.github.android" in byId.getValue(RoomConfigRepository.ID_GAMING_PC).packages)

        val trashApps = RoomConfigRepository.appsForHotspot(
            byId.getValue(RoomConfigRepository.ID_TRASH_CAN),
            populated,
            apps
        ).map { it.packageName }
        assertEquals(listOf("com.example.mystery"), trashApps)

        val owners = RoomConfigRepository.buildPackageOwnerLabelMap(populated)
        assertTrue(owners["com.whatsapp"]!!.contains("Living Room"))
        assertTrue(owners["com.ubercab.eats"]!!.contains("Kitchen"))
        assertTrue(owners["com.github.android"]!!.contains("Gaming Room"))
    }

    @Test
    fun changelogStartsWithLatestVersion() {
        assertEquals("3.0.2", ChangelogData.CURRENT_VERSION)
        assertTrue(ChangelogData.entries.isNotEmpty())
        assertEquals("3.0.2", ChangelogData.entries.first().version)
        assertTrue(ChangelogData.entries.first().highlights.size >= 3)
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

    @Test
    fun corruptedPrimaryConfigRecoversFromBackupWithoutLosingUserData() = runTest {
        val file = File(tempFolder.root, "room_config.json")
        val repo = RoomConfigRepository(file)

        val saved = RoomConfigRepository.createDefaultRoomConfig().copy(
            onboardingCompleted = true,
            onboardingStep = 3,
            roomStyleId = RoomStyleId.MINECRAFT,
            themeId = RoomThemeId.OLED_PITCH,
            webShortcuts = listOf(WebShortcut("w9", "Docs", "https://example.com"))
        )
        repo.saveConfig(saved)

        // Simulate abrupt truncation / corruption of the primary file during an OS update or crash
        file.writeText("{ truncated json ", Charsets.UTF_8)

        val recovered = repo.loadConfig()
        assertEquals(saved, recovered)
    }

    @Test
    fun unknownEnumValuesCoerceToDefaultsInsteadOfWipingConfig() = runTest {
        val file = File(tempFolder.root, "room_config.json")
        val repo = RoomConfigRepository(file)

        file.writeText(
            """
            {
              "version": 2,
              "onboardingCompleted": true,
              "onboardingStep": 3,
              "roomStyleId": "FUTURE_UNKNOWN_STYLE",
              "themeId": "OLED_PITCH",
              "webShortcuts": [{"id": "s1", "title": "News", "url": "https://news.ycombinator.com"}]
            }
            """.trimIndent(),
            Charsets.UTF_8
        )

        val loaded = repo.loadConfig()
        assertTrue(loaded.onboardingCompleted)
        assertEquals(RoomStyleId.COZY_MINIMALIST, loaded.roomStyleId)
        assertEquals(RoomThemeId.OLED_PITCH, loaded.themeId)
        assertEquals(1, loaded.webShortcuts.size)
    }

    @Test
    fun geometryDashSubThemesPersistAndProvideDistinctPalettes() = runTest {
        val file = File(tempFolder.root, "room_config.json")
        val repo = RoomConfigRepository(file)

        for (subTheme in GeometryDashSubTheme.entries) {
            val config = RoomConfigRepository.createDefaultRoomConfig().copy(
                roomStyleId = RoomStyleId.GEOMETRY_DASH,
                geometryDashSubTheme = subTheme
            )
            repo.saveConfig(config)
            val loaded = repo.loadConfig()
            assertEquals(RoomStyleId.GEOMETRY_DASH, loaded.roomStyleId)
            assertEquals(subTheme, loaded.geometryDashSubTheme)

            val palette = RoomThemes.effectivePalette(
                roomStyle = loaded.roomStyleId,
                themeId = loaded.themeId,
                gdSubTheme = loaded.geometryDashSubTheme
            )
            assertTrue(palette.isDark)
        }

        val sw = RoomThemes.geometryDashPalette(GeometryDashSubTheme.SONIC_WAVE)
        val ad = RoomThemes.geometryDashPalette(GeometryDashSubTheme.ASTRAL_DIVINITY)
        val dc = RoomThemes.geometryDashPalette(GeometryDashSubTheme.DEATH_CORRIDOR)
        assertTrue(sw.wallMain != ad.wallMain && ad.wallMain != dc.wallMain && sw.wallMain != dc.wallMain)
    }
}
