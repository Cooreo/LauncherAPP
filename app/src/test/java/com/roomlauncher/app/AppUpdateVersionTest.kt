package com.roomlauncher.app

import com.roomlauncher.app.data.AppVersionComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateVersionTest {

    @Test
    fun extractsVersionFromReleaseApkFilenames() {
        assertEquals("2.5", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v2.5.apk"))
        assertEquals("2.3.1", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v2.3.1.apk"))
        assertEquals("10.0.2", AppVersionComparator.extractVersionFromApkName("roomlauncher-v10.0.2.apk"))
        assertNull(AppVersionComparator.extractVersionFromApkName("README.md"))
    }

    @Test
    fun comparesSemanticVersionsNumerically() {
        assertTrue(AppVersionComparator.isNewerVersion("2.5", "2.4"))
        assertTrue(AppVersionComparator.isNewerVersion("v2.10", "2.4"))
        assertTrue(AppVersionComparator.isNewerVersion("2.4.1", "2.4"))
        assertTrue(AppVersionComparator.isNewerVersion("3.0", "2.9.9"))
        assertFalse(AppVersionComparator.isNewerVersion("2.5", "2.5"))
        assertFalse(AppVersionComparator.isNewerVersion("2.5.0", "2.5"))
        assertFalse(AppVersionComparator.isNewerVersion("2.3.1", "2.4"))
    }
}
