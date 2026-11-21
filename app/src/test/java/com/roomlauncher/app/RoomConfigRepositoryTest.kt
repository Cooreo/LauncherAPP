package com.roomlauncher.app

import com.roomlauncher.app.data.AppInfo
import com.roomlauncher.app.data.HotspotActionType
import com.roomlauncher.app.data.RoomConfigRepository
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
    fun defaultRoomContainsAllSixRequiredObjects() {
        val config = RoomConfigRepository.createDefaultRoomConfig()
        val labels = config.hotspots.map { it.label }.toSet()

        assertEquals(6, config.hotspots.size)
        assertTrue("TV" in labels)
        assertTrue("Shelf" in labels)
        assertTrue("Painting" in labels)
        assertTrue("Trash Can" in labels)
        assertTrue("Bed" in labels)
        assertTrue("Clock" in labels)

        val byId = config.hotspots.associateBy { it.id }
        assertEquals(HotspotActionType.PANEL, byId[RoomConfigRepository.ID_TV]?.actionType)
        assertEquals(HotspotActionType.PANEL, byId[RoomConfigRepository.ID_SHELF]?.actionType)
        assertEquals(HotspotActionType.DIRECT_GALLERY, byId[RoomConfigRepository.ID_PAINTING]?.actionType)
        assertEquals(HotspotActionType.UNSORTED_PANEL, byId[RoomConfigRepository.ID_TRASH_CAN]?.actionType)
        assertEquals(HotspotActionType.PANEL, byId[RoomConfigRepository.ID_BED]?.actionType)
        assertEquals(HotspotActionType.DIRECT_ALARM, byId[RoomConfigRepository.ID_CLOCK]?.actionType)
    }

    @Test
    fun jsonPersistenceRoundTripWorksCleanly() = runTest {
        val file = File(tempFolder.root, "room_config.json")
        val repo = RoomConfigRepository(file)

        val initial = RoomConfigRepository.createDefaultRoomConfig().copy(
            onboardingCompleted = true,
            autoPopulated = true
        )
        repo.saveConfig(initial)

        val reloaded = repo.loadConfig()
        assertEquals(initial, reloaded)
    }

    @Test
    fun autoPopulateAndNothingGetsLostRule() {
        val apps = listOf(
            AppInfo(packageName = "com.google.android.youtube", label = "YouTube"),
            AppInfo(packageName = "md.obsidian", label = "Obsidian"),
            AppInfo(packageName = "com.calm.android", label = "Calm"),
            AppInfo(packageName = "com.example.randomgame", label = "Random Game")
        )

        val defaultConfig = RoomConfigRepository.createDefaultRoomConfig()
        val populated = RoomConfigRepository.autoPopulateIfNeeded(defaultConfig, apps)

        assertTrue(populated.autoPopulated)

        val byId = populated.hotspots.associateBy { it.id }
        val tvApps = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_TV), populated, apps)
        val shelfApps = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_SHELF), populated, apps)
        val bedApps = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_BED), populated, apps)
        val trashApps = RoomConfigRepository.appsForHotspot(byId.getValue(RoomConfigRepository.ID_TRASH_CAN), populated, apps)

        assertEquals(listOf("com.google.android.youtube"), tvApps.map { it.packageName })
        assertEquals(listOf("md.obsidian"), shelfApps.map { it.packageName })
        assertEquals(listOf("com.calm.android"), bedApps.map { it.packageName })
        assertEquals(listOf("com.example.randomgame"), trashApps.map { it.packageName })

        // Verify newly installed app automatically appears in Trash Can
        val withNewInstall = apps + AppInfo(packageName = "com.example.brandnew", label = "Brand New App")
        val updatedTrash = RoomConfigRepository.appsForHotspot(
            byId.getValue(RoomConfigRepository.ID_TRASH_CAN),
            populated,
            withNewInstall
        )
        assertTrue(updatedTrash.any { it.packageName == "com.example.brandnew" })

        // Verify app removed from TV automatically re-appears in Trash Can
        val removedFromTv = populated.copy(
            hotspots = populated.hotspots.map { h ->
                if (h.id == RoomConfigRepository.ID_TV) h.copy(packages = emptyList()) else h
            }
        )
        val trashAfterRemove = RoomConfigRepository.appsForHotspot(
            removedFromTv.hotspots.first { it.id == RoomConfigRepository.ID_TRASH_CAN },
            removedFromTv,
            withNewInstall
        )
        assertTrue(trashAfterRemove.any { it.packageName == "com.google.android.youtube" })
        assertFalse(trashAfterRemove.any { it.packageName == "md.obsidian" })
    }
}
