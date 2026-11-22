package com.roomlauncher.app

import com.roomlauncher.app.data.AppVersionComparator
import com.roomlauncher.app.system.AppUpdateInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateVersionTest {

    @Test
    fun extractsVersionFromReleaseApkFilenames() {
        assertEquals("3.2.2", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.2.2.apk"))
        assertEquals("3.2.1", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.2.1.apk"))
        assertEquals("3.2a", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.2a.apk"))
        assertEquals("3.3-alpha1", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.3-alpha1.apk"))
        assertEquals("3.1.5", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.1.5.apk"))
        assertEquals("3.1.4", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.1.4.apk"))
        assertEquals("3.1.3", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.1.3.apk"))
        assertEquals("3.1.2", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.1.2.apk"))
        assertEquals("3.1.1", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.1.1.apk"))
        assertEquals("3.1.0", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.1.0.apk"))
        assertEquals("3.0.2", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.0.2.apk"))
        assertEquals("3.0.1", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.0.1.apk"))
        assertEquals("3.0", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v3.0.apk"))
        assertEquals("2.6", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v2.6.apk"))
        assertEquals("2.5.1", AppVersionComparator.extractVersionFromApkName("RoomLauncher-v2.5.1.apk"))
        assertEquals("10.0.2", AppVersionComparator.extractVersionFromApkName("roomlauncher-v10.0.2.apk"))
        assertNull(AppVersionComparator.extractVersionFromApkName("README.md"))
    }

    @Test
    fun identifiesPreReleaseAndStableVersionsAccurately() {
        assertTrue(AppVersionComparator.isPreReleaseVersion("3.2a"))
        assertTrue(AppVersionComparator.isPreReleaseVersion("v3.3a2"))
        assertTrue(AppVersionComparator.isPreReleaseVersion("3.3-alpha1"))
        assertTrue(AppVersionComparator.isPreReleaseVersion("3.2.2b"))
        assertFalse(AppVersionComparator.isPreReleaseVersion("3.2.1"))
        assertFalse(AppVersionComparator.isPreReleaseVersion("v3.1.5"))
        assertFalse(AppVersionComparator.isPreReleaseVersion("3.2"))
    }

    @Test
    fun comparesSemanticVersionsNumerically() {
        assertTrue(AppVersionComparator.isNewerVersion("3.2.2", "3.2.1"))
        assertTrue(AppVersionComparator.isNewerVersion("3.2.1", "3.2a"))
        assertTrue(AppVersionComparator.isNewerVersion("3.2.1", "3.1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("3.3a", "3.2.1"))
        assertTrue(AppVersionComparator.isNewerVersion("3.2a", "3.1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("3.2b", "3.2a"))
        assertTrue(AppVersionComparator.isNewerVersion("3.2", "3.2a"))
        assertTrue(AppVersionComparator.isNewerVersion("3.2", "3.2b"))
        assertFalse(AppVersionComparator.isNewerVersion("3.2a", "3.2a"))
        assertFalse(AppVersionComparator.isNewerVersion("3.2a", "3.2"))
        assertFalse(AppVersionComparator.isNewerVersion("3.2a", "3.2.1"))
        assertTrue(AppVersionComparator.isNewerVersion("2.6.1", "2.6"))
        assertTrue(AppVersionComparator.isNewerVersion("2.6", "2.5.1"))
        assertTrue(AppVersionComparator.isNewerVersion("2.5.1", "2.5"))
        assertTrue(AppVersionComparator.isNewerVersion("v2.10", "2.6.1"))
        assertTrue(AppVersionComparator.isNewerVersion("3.0", "2.9.9"))
        assertFalse(AppVersionComparator.isNewerVersion("2.6.1", "2.6.1"))
        assertFalse(AppVersionComparator.isNewerVersion("2.6.0", "2.6"))
        assertFalse(AppVersionComparator.isNewerVersion("2.5.1", "2.6.1"))
    }

    @Test
    fun formatsSigningKeyFingerprintsDeterministically() {
        val hash = AppUpdateInstaller.sha256Hex("room-launcher".toByteArray(Charsets.UTF_8))
        assertEquals(64, hash.length)
        val shortFp = AppUpdateInstaller.formatShortFingerprint(hash)
        // 8 hex bytes separated by 7 colons -> 23 chars
        assertEquals(23, shortFp.length)
        assertTrue(shortFp.all { it in '0'..'9' || it in 'A'..'F' || it == ':' })
    }
}
