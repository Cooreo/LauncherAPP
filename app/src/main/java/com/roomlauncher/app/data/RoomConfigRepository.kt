package com.roomlauncher.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class RoomConfigRepository(
    private val configFile: File
) {
    constructor(context: Context) : this(File(context.filesDir, CONFIG_FILE_NAME))

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun loadConfig(): RoomConfig = withContext(Dispatchers.IO) {
        if (!configFile.exists()) {
            return@withContext createDefaultRoomConfig()
        }
        runCatching {
            val raw = configFile.readText(Charsets.UTF_8)
            val parsed = json.decodeFromString<RoomConfig>(raw)
            if (parsed.hotspots.isEmpty()) {
                parsed.copy(hotspots = createDefaultHotspots())
            } else {
                parsed
            }
        }.getOrElse {
            createDefaultRoomConfig()
        }
    }

    suspend fun saveConfig(config: RoomConfig) = withContext(Dispatchers.IO) {
        val parent = configFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val tempFile = File(configFile.parentFile, "$CONFIG_FILE_NAME.tmp")
        val serialized = json.encodeToString(config)
        tempFile.writeText(serialized, Charsets.UTF_8)
        if (!tempFile.renameTo(configFile)) {
            configFile.writeText(serialized, Charsets.UTF_8)
            tempFile.delete()
        }
    }

    fun encodeConfig(config: RoomConfig): String = json.encodeToString(config)

    fun decodeConfig(raw: String): RoomConfig = json.decodeFromString(raw)

    companion object {
        const val CONFIG_FILE_NAME = "room_config.json"

        const val ID_TV = "tv"
        const val ID_SHELF = "shelf"
        const val ID_PAINTING = "painting"
        const val ID_TRASH_CAN = "trash_can"
        const val ID_BED = "bed"
        const val ID_CLOCK = "clock"

        fun createDefaultRoomConfig(): RoomConfig = RoomConfig(
            version = 1,
            onboardingCompleted = false,
            autoPopulated = false,
            hotspots = createDefaultHotspots()
        )

        fun createDefaultHotspots(): List<RoomHotspot> = listOf(
            RoomHotspot(
                id = ID_TV,
                label = "TV",
                subtitle = "Media, Video & Podcasts",
                x = 0.045f,
                y = 0.32f,
                width = 0.50f,
                height = 0.23f,
                actionType = HotspotActionType.PANEL
            ),
            RoomHotspot(
                id = ID_SHELF,
                label = "Shelf",
                subtitle = "Work, Notes & Email",
                x = 0.58f,
                y = 0.295f,
                width = 0.38f,
                height = 0.195f,
                actionType = HotspotActionType.PANEL
            ),
            RoomHotspot(
                id = ID_PAINTING,
                label = "Painting",
                subtitle = "System Gallery",
                x = 0.35f,
                y = 0.125f,
                width = 0.30f,
                height = 0.155f,
                actionType = HotspotActionType.DIRECT_GALLERY
            ),
            RoomHotspot(
                id = ID_TRASH_CAN,
                label = "Trash Can",
                subtitle = "Unsorted Apps",
                x = 0.69f,
                y = 0.685f,
                width = 0.24f,
                height = 0.155f,
                actionType = HotspotActionType.UNSORTED_PANEL
            ),
            RoomHotspot(
                id = ID_BED,
                label = "Bed",
                subtitle = "Bedtime, Meditation & Sleep",
                x = 0.035f,
                y = 0.615f,
                width = 0.60f,
                height = 0.26f,
                actionType = HotspotActionType.PANEL
            ),
            RoomHotspot(
                id = ID_CLOCK,
                label = "Clock",
                subtitle = "System Alarms",
                x = 0.65f,
                y = 0.105f,
                width = 0.24f,
                height = 0.11f,
                actionType = HotspotActionType.DIRECT_ALARM
            )
        )

        private val TV_KNOWN_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.spotify.music",
            "com.netflix.mediaclient",
            "com.disney.disneyplus",
            "com.amazon.avod.thirdpartyclient",
            "tv.twitch.android.app",
            "org.videolan.vlc",
            "au.com.shiftyjelly.pocketcasts",
            "com.audible.application",
            "com.soundcloud.android",
            "com.apple.android.music",
            "com.plexapp.android",
            "com.zhiliaoapp.musically",
            "de.danoeh.antennapod",
            "com.mxtech.videoplayer.ad",
            "com.mxtech.videoplayer.pro",
            "com.hbo.hbonow",
            "com.wbd.stream",
            "com.google.android.apps.podcasts",
            "com.bambuna.podcastaddict"
        )

        private val TV_KEYWORDS = listOf(
            "youtube", "spotify", "podcast", "music", "netflix", "twitch", "video", "player", "radio"
        )

        private val SHELF_KNOWN_PACKAGES = setOf(
            "md.obsidian",
            "notion.id",
            "com.google.android.keep",
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.Slack",
            "com.microsoft.teams",
            "com.google.android.apps.docs",
            "com.google.android.apps.docs.editors.docs",
            "com.google.android.apps.docs.editors.sheets",
            "com.google.android.apps.docs.editors.slides",
            "com.google.android.calendar",
            "com.todoist",
            "com.evernote",
            "com.microsoft.office.onenote",
            "com.trello",
            "us.zoom.videomeetings",
            "ch.protonmail.android",
            "com.standardnotes",
            "net.cozic.joplin",
            "com.linkedin.android",
            "com.samsung.android.app.notes",
            "com.simplemobiletools.notes.pro",
            "com.ticktick.task"
        )

        private val SHELF_KEYWORDS = listOf(
            "obsidian", "notepad", "notes", "mail", "gmail", "outlook", "slack", "notion", "calendar", "office", "docs"
        )

        private val BED_KNOWN_PACKAGES = setOf(
            "com.calm.android",
            "com.getsomeheadspace.android",
            "com.spotlightsix.zentimerlite2",
            "com.northcube.sleepcycle",
            "com.urbandroid.sleep",
            "com.amazon.kindle",
            "com.flyersoft.moonreader",
            "com.overdrive.mobile.android.libby",
            "ipnossoft.rma.free",
            "com.finch.finch",
            "meditofoundation.medito",
            "com.fitbit.FitbitMobile",
            "com.sec.android.app.shealth",
            "com.google.android.apps.fitness",
            "com.google.android.apps.wellbeing",
            "com.ouraring.oura"
        )

        private val BED_KEYWORDS = listOf(
            "sleep", "calm", "meditat", "headspace", "kindle", "wellbeing", "relax", "whitenoise", "bedtime"
        )

        /**
         * Auto-populates the default panels (TV, Shelf, Bed) once using installed apps.
         * Any app not matched remains unassigned so it appears in the Trash Can.
         */
        fun autoPopulateIfNeeded(config: RoomConfig, installedApps: List<AppInfo>): RoomConfig {
            if (config.autoPopulated || installedApps.isEmpty()) return config

            val assigned = config.hotspots
                .filter { it.actionType == HotspotActionType.PANEL }
                .flatMap { it.packages }
                .toMutableSet()

            val tvApps = mutableListOf<String>()
            val shelfApps = mutableListOf<String>()
            val bedApps = mutableListOf<String>()

            for (app in installedApps) {
                if (app.packageName in assigned) continue
                val pkgLower = app.packageName.lowercase()
                val labelLower = app.label.lowercase()

                when {
                    isBedApp(app.packageName, pkgLower, labelLower) -> {
                        bedApps.add(app.packageName)
                        assigned.add(app.packageName)
                    }
                    isTvApp(app.packageName, pkgLower, labelLower, app.category) -> {
                        tvApps.add(app.packageName)
                        assigned.add(app.packageName)
                    }
                    isShelfApp(app.packageName, pkgLower, labelLower, app.category) -> {
                        shelfApps.add(app.packageName)
                        assigned.add(app.packageName)
                    }
                }
            }

            val updatedHotspots = config.hotspots.map { hotspot ->
                when (hotspot.id) {
                    ID_TV -> hotspot.copy(packages = (hotspot.packages + tvApps).distinct())
                    ID_SHELF -> hotspot.copy(packages = (hotspot.packages + shelfApps).distinct())
                    ID_BED -> hotspot.copy(packages = (hotspot.packages + bedApps).distinct())
                    else -> hotspot
                }
            }

            return config.copy(
                autoPopulated = true,
                hotspots = updatedHotspots
            )
        }

        private fun isTvApp(pkg: String, pkgLower: String, labelLower: String, category: Int): Boolean {
            if (pkg in TV_KNOWN_PACKAGES) return true
            if (category == ApplicationInfo.CATEGORY_AUDIO || category == ApplicationInfo.CATEGORY_VIDEO) {
                return true
            }
            return TV_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        private fun isShelfApp(pkg: String, pkgLower: String, labelLower: String, category: Int): Boolean {
            if (pkg in SHELF_KNOWN_PACKAGES) return true
            if (category == ApplicationInfo.CATEGORY_PRODUCTIVITY) {
                return true
            }
            return SHELF_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        private fun isBedApp(pkg: String, pkgLower: String, labelLower: String): Boolean {
            if (pkg in BED_KNOWN_PACKAGES) return true
            return BED_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        /**
         * Computes apps belonging to any hotspot.
         * For UNSORTED_PANEL (Trash Can), dynamically returns every installed app
         * not currently assigned to any other PANEL hotspot ("Nothing gets lost").
         */
        fun appsForHotspot(
            hotspot: RoomHotspot,
            config: RoomConfig,
            installedApps: List<AppInfo>
        ): List<AppInfo> {
            val byPackage = installedApps.associateBy { it.packageName }
            return when (hotspot.actionType) {
                HotspotActionType.UNSORTED_PANEL -> {
                    val assigned = config.hotspots
                        .filter { it.actionType == HotspotActionType.PANEL }
                        .flatMap { it.packages }
                        .toSet()
                    installedApps.filter { it.packageName !in assigned }
                }
                HotspotActionType.PANEL -> {
                    hotspot.packages.mapNotNull { byPackage[it] }
                }
                HotspotActionType.DIRECT_GALLERY,
                HotspotActionType.DIRECT_ALARM -> emptyList()
            }
        }

        /**
         * Returns the label of the room object that currently owns [packageName].
         */
        fun ownerHotspotForPackage(
            packageName: String,
            config: RoomConfig
        ): RoomHotspot? {
            return config.hotspots.firstOrNull {
                it.actionType == HotspotActionType.PANEL && packageName in it.packages
            } ?: config.hotspots.firstOrNull {
                it.actionType == HotspotActionType.UNSORTED_PANEL
            }
        }
    }
}
