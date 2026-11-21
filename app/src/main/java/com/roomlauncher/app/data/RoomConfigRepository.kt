package com.roomlauncher.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class RoomConfigRepository(
    private val configFile: File
) {
    constructor(context: Context) : this(File(context.filesDir, CONFIG_FILE_NAME))

    private val saveMutex = Mutex()
    private val backupFile: File
        get() = File(configFile.parentFile, "$CONFIG_FILE_NAME.bak")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    suspend fun loadConfig(): RoomConfig = withContext(Dispatchers.IO) {
        saveMutex.withLock {
            readConfigFile(configFile)
                ?: readConfigFile(backupFile)?.also { recovered ->
                    // Restore primary file from healthy backup so subsequent boots are fast
                    writeFileAtomically(configFile, json.encodeToString(recovered))
                }
                ?: createDefaultRoomConfig()
        }
    }

    suspend fun saveConfig(config: RoomConfig) = withContext(Dispatchers.IO) {
        saveMutex.withLock {
            val parent = configFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val serialized = json.encodeToString(config)
            // Keep a backup of the previously known-good primary file before replacing it
            if (configFile.exists() && configFile.length() > 0L) {
                runCatching { configFile.copyTo(backupFile, overwrite = true) }
            }
            writeFileAtomically(configFile, serialized)
            // Update backup to match latest committed state
            writeFileAtomically(backupFile, serialized)
        }
    }

    private fun readConfigFile(file: File): RoomConfig? {
        if (!file.exists() || file.length() <= 0L) return null
        return runCatching {
            val raw = file.readText(Charsets.UTF_8)
            val parsed = json.decodeFromString<RoomConfig>(raw)
            migrateIfNeeded(parsed)
        }.getOrNull()
    }

    private fun writeFileAtomically(target: File, content: String) {
        val parent = target.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val tempFile = File(parent, "${target.name}.tmp")
        FileOutputStream(tempFile).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
            fos.flush()
            runCatching { fos.fd.sync() }
        }
        if (!tempFile.renameTo(target)) {
            FileOutputStream(target).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
                runCatching { fos.fd.sync() }
            }
            runCatching { tempFile.delete() }
        }
    }

    fun encodeConfig(config: RoomConfig): String = json.encodeToString(config)

    fun decodeConfig(raw: String): RoomConfig = migrateIfNeeded(json.decodeFromString(raw))

    companion object {
        const val CONFIG_FILE_NAME = "room_config.json"

        const val ID_WINDOW = "window"
        const val ID_PAINTING = "painting"
        const val ID_CLOCK = "clock"
        const val ID_STICKY_NOTE = "sticky_note"
        const val ID_TV = "tv"
        const val ID_CONSOLE = "console"
        const val ID_SHELF = "shelf"
        const val ID_BED = "bed"
        const val ID_TRASH_CAN = "trash_can"

        fun createDefaultRoomConfig(): RoomConfig = RoomConfig(
            version = 2,
            onboardingCompleted = false,
            onboardingStep = 0,
            autoPopulated = false,
            hotspots = createDefaultHotspots()
        )

        fun createDefaultHotspots(): List<RoomHotspot> = listOf(
            RoomHotspot(
                id = ID_WINDOW,
                label = "Window",
                subtitle = "Live Weather",
                x = 0.04f,
                y = 0.11f,
                width = 0.28f,
                height = 0.16f,
                actionType = HotspotActionType.DIRECT_WEATHER,
                visualType = RoomObjectVisual.WINDOW
            ),
            RoomHotspot(
                id = ID_PAINTING,
                label = "Painting",
                subtitle = "Photos & Gallery",
                x = 0.36f,
                y = 0.115f,
                width = 0.28f,
                height = 0.155f,
                actionType = HotspotActionType.DIRECT_GALLERY,
                visualType = RoomObjectVisual.PAINTING
            ),
            RoomHotspot(
                id = ID_CLOCK,
                label = "Clock",
                subtitle = "System Alarms",
                x = 0.69f,
                y = 0.105f,
                width = 0.24f,
                height = 0.125f,
                actionType = HotspotActionType.DIRECT_ALARM,
                visualType = RoomObjectVisual.CLOCK
            ),
            RoomHotspot(
                id = ID_STICKY_NOTE,
                label = "Sticky Note",
                subtitle = "Web Shortcuts & Browsers",
                x = 0.72f,
                y = 0.245f,
                width = 0.21f,
                height = 0.095f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.STICKY_NOTE
            ),
            RoomHotspot(
                id = ID_TV,
                label = "TV",
                subtitle = "Media, Video & Podcasts",
                x = 0.045f,
                y = 0.305f,
                width = 0.49f,
                height = 0.145f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.TV
            ),
            RoomHotspot(
                id = ID_CONSOLE,
                label = "Game Console",
                subtitle = "Games",
                x = 0.045f,
                y = 0.455f,
                width = 0.49f,
                height = 0.115f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.CONSOLE
            ),
            RoomHotspot(
                id = ID_SHELF,
                label = "Shelf",
                subtitle = "Work, Notes & Email",
                x = 0.58f,
                y = 0.355f,
                width = 0.38f,
                height = 0.195f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.SHELF
            ),
            RoomHotspot(
                id = ID_BED,
                label = "Bed",
                subtitle = "Sleep, Health & Clock",
                x = 0.035f,
                y = 0.625f,
                width = 0.60f,
                height = 0.245f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.BED
            ),
            RoomHotspot(
                id = ID_TRASH_CAN,
                label = "Trash Can",
                subtitle = "Unsorted Apps",
                x = 0.70f,
                y = 0.695f,
                width = 0.23f,
                height = 0.15f,
                actionType = HotspotActionType.UNSORTED_PANEL,
                visualType = RoomObjectVisual.TRASH_CAN
            )
        )

        /**
         * Migrates v1 saved JSON cleanly into v2 without discarding the user's app assignments:
         * assigns visualType to legacy IDs and appends Window, Game Console, and Sticky Note if missing.
         */
        fun migrateIfNeeded(parsed: RoomConfig): RoomConfig {
            if (parsed.hotspots.isEmpty()) {
                return parsed.copy(version = 2, hotspots = createDefaultHotspots())
            }
            val defaultsById = createDefaultHotspots().associateBy { it.id }

            val updatedExisting = parsed.hotspots.map { h ->
                val inferredVisual = if (h.visualType == RoomObjectVisual.CUSTOM) {
                    when (h.id) {
                        ID_TV -> RoomObjectVisual.TV
                        ID_CONSOLE -> RoomObjectVisual.CONSOLE
                        ID_SHELF -> RoomObjectVisual.SHELF
                        ID_PAINTING -> RoomObjectVisual.PAINTING
                        ID_TRASH_CAN -> RoomObjectVisual.TRASH_CAN
                        ID_BED -> RoomObjectVisual.BED
                        ID_CLOCK -> RoomObjectVisual.CLOCK
                        ID_WINDOW -> RoomObjectVisual.WINDOW
                        ID_STICKY_NOTE -> RoomObjectVisual.STICKY_NOTE
                        else -> RoomObjectVisual.CUSTOM
                    }
                } else {
                    h.visualType
                }

                // If upgrading from v1 where TV occupied both TV + console height, trim TV slightly so Console fits neatly below
                if (parsed.version < 2 && h.id == ID_TV && h.height > 0.19f) {
                    h.copy(
                        visualType = inferredVisual,
                        y = 0.305f,
                        height = 0.145f
                    )
                } else {
                    h.copy(visualType = inferredVisual)
                }
            }.toMutableList()

            val existingIds = updatedExisting.map { it.id }.toSet()
            if (parsed.version < 2) {
                listOf(ID_WINDOW, ID_CONSOLE, ID_STICKY_NOTE).forEach { newId ->
                    if (newId !in existingIds) {
                        defaultsById[newId]?.let { updatedExisting.add(it) }
                    }
                }
            }

            return parsed.copy(
                version = 2,
                hotspots = updatedExisting
            )
        }

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

        private val CONSOLE_KNOWN_PREFIXES = listOf(
            "com.supercell.",
            "com.mojang.",
            "com.roblox.",
            "com.miHoYo.",
            "com.HoYoverse.",
            "com.riotgames.",
            "com.innersloth.",
            "com.nianticlabs.",
            "com.activision.",
            "com.epicgames.",
            "com.king.",
            "com.ea.gp.",
            "com.ubisoft.",
            "com.valvesoftware.android.steam",
            "com.playstation.",
            "com.microsoft.xbox",
            "com.nintendo.",
            "com.retroarch",
            "org.ppsspp.",
            "org.dolphinemu.",
            "xyz.aethersx2."
        )

        private val CONSOLE_KEYWORDS = listOf(
            "game", "chess", "sudoku", "minecraft", "roblox", "steam", "xbox", "playstation", "emulator"
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
            // Sleep, meditation & reading
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
            "com.google.android.apps.wellbeing",
            // Health & fitness apps
            "com.fitbit.FitbitMobile",
            "com.sec.android.app.shealth",
            "com.google.android.apps.fitness",
            "com.google.android.apps.healthdata",
            "com.ouraring.oura",
            "com.garmin.android.apps.connectmobile",
            "com.strava",
            "com.xiaomi.wearable",
            "com.huami.watch.hmwatchmanager",
            "com.huawei.health",
            "com.whoop.android",
            "org.iggymedia.periodtracker",
            "com.clue.android",
            // Clock & alarm apps (included in Bed panel too in case user looks for Clock app!)
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.deskclock",
            "com.oneplus.deskclock",
            "com.miui.clock",
            "com.coloros.alarmclock",
            "com.asus.deskclock",
            "com.motorola.timeweatherwidget",
            "org.fossify.clock",
            "com.simplemobiletools.clock"
        )

        private val BED_KEYWORDS = listOf(
            "sleep", "calm", "meditat", "headspace", "kindle", "wellbeing",
            "relax", "whitenoise", "bedtime", "health", "fitness", "clock", "alarm", "timer"
        )

        private val STICKY_NOTE_KNOWN_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "org.mozilla.fenix",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.vivaldi.browser",
            "com.duckduckgo.mobile.android",
            "com.sec.android.app.sbrowser",
            "com.kiwibrowser.browser",
            "com.chimbori.hermitcrab",
            "com.tobykurien.webapps",
            "org.chromium.webapk"
        )

        private val STICKY_NOTE_KEYWORDS = listOf(
            "browser", "chrome", "firefox", "brave", "webapk", "shortcut", "hermit", "bookmark"
        )

        /**
         * Auto-populates the default panels once using installed apps.
         * Any app not matched remains unassigned so it appears in the Trash Can.
         */
        fun autoPopulateIfNeeded(config: RoomConfig, installedApps: List<AppInfo>): RoomConfig {
            if (config.autoPopulated || installedApps.isEmpty()) return config

            val assigned = config.hotspots
                .filter { it.actionType == HotspotActionType.PANEL }
                .flatMap { it.packages }
                .toMutableSet()

            val tvApps = mutableListOf<String>()
            val consoleApps = mutableListOf<String>()
            val shelfApps = mutableListOf<String>()
            val bedApps = mutableListOf<String>()
            val stickyNoteApps = mutableListOf<String>()

            for (app in installedApps) {
                if (app.packageName in assigned) continue
                val pkgLower = app.packageName.lowercase()
                val labelLower = app.label.lowercase()

                when {
                    isGameApp(app.packageName, pkgLower, labelLower, app.category, app.isGameFlag) -> {
                        consoleApps.add(app.packageName)
                        assigned.add(app.packageName)
                    }
                    isBedApp(app.packageName, pkgLower, labelLower) -> {
                        bedApps.add(app.packageName)
                        assigned.add(app.packageName)
                    }
                    isStickyNoteApp(app.packageName, pkgLower, labelLower) -> {
                        stickyNoteApps.add(app.packageName)
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
                    ID_CONSOLE -> hotspot.copy(packages = (hotspot.packages + consoleApps).distinct())
                    ID_SHELF -> hotspot.copy(packages = (hotspot.packages + shelfApps).distinct())
                    ID_BED -> hotspot.copy(packages = (hotspot.packages + bedApps).distinct())
                    ID_STICKY_NOTE -> hotspot.copy(packages = (hotspot.packages + stickyNoteApps).distinct())
                    else -> hotspot
                }
            }

            return config.copy(
                autoPopulated = true,
                hotspots = updatedHotspots
            )
        }

        private fun isGameApp(
            pkg: String,
            pkgLower: String,
            labelLower: String,
            category: Int,
            isGameFlag: Boolean
        ): Boolean {
            if (category == ApplicationInfo.CATEGORY_GAME || isGameFlag) return true
            if (CONSOLE_KNOWN_PREFIXES.any { pkg.startsWith(it, ignoreCase = true) }) return true
            return CONSOLE_KEYWORDS.any { it in pkgLower || it in labelLower }
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

        private fun isStickyNoteApp(pkg: String, pkgLower: String, labelLower: String): Boolean {
            if (pkg in STICKY_NOTE_KNOWN_PACKAGES || pkgLower.startsWith("org.chromium.webapk")) return true
            return STICKY_NOTE_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

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
                HotspotActionType.DIRECT_ALARM,
                HotspotActionType.DIRECT_WEATHER -> emptyList()
            }
        }

        fun buildPackageOwnerLabelMap(config: RoomConfig): Map<String, String> {
            val map = HashMap<String, String>()
            config.hotspots.forEach { hotspot ->
                if (hotspot.actionType == HotspotActionType.PANEL) {
                    hotspot.packages.forEach { pkg ->
                        map[pkg] = hotspot.label
                    }
                }
            }
            return map
        }

        fun defaultUnsortedLabel(config: RoomConfig): String {
            return config.hotspots
                .firstOrNull { it.actionType == HotspotActionType.UNSORTED_PANEL }
                ?.label ?: "Trash Can"
        }
    }
}
