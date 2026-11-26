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
            val normalized = syncLegacyHotspots(config)
            val parent = configFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val serialized = json.encodeToString(normalized)
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

    fun encodeConfig(config: RoomConfig): String = json.encodeToString(syncLegacyHotspots(config))

    fun decodeConfig(raw: String): RoomConfig = migrateIfNeeded(json.decodeFromString(raw))

    companion object {
        const val CONFIG_FILE_NAME = "room_config.json"

        // Default Room IDs
        const val ROOM_ID_BEDROOM = "room_bedroom"
        const val ROOM_ID_LIVING_ROOM = "room_living"
        const val ROOM_ID_KITCHEN = "room_kitchen"
        const val ROOM_ID_GAMING_ROOM = "room_gaming"

        // Bedroom Hotspot IDs
        const val ID_WINDOW = "window"
        const val ID_PAINTING = "painting"
        const val ID_CLOCK = "clock"
        const val ID_STICKY_NOTE = "sticky_note"
        const val ID_TV = "tv"
        const val ID_CONSOLE = "console"
        const val ID_SHELF = "shelf"
        const val ID_BED = "bed"
        const val ID_NIGHTSTAND = "nightstand"
        const val ID_TRASH_CAN = "trash_can"

        // Living Room Hotspot IDs
        const val ID_LIVING_WINDOW = "living_window"
        const val ID_LIVING_FIREPLACE = "living_fireplace"
        const val ID_LIVING_VINYL = "living_vinyl"
        const val ID_LIVING_SOFA = "living_sofa"
        const val ID_LIVING_COFFEE_TABLE = "living_coffee_table"

        // Kitchen Hotspot IDs
        const val ID_KITCHEN_CLOCK = "kitchen_clock"
        const val ID_KITCHEN_FRIDGE = "kitchen_fridge"
        const val ID_KITCHEN_COFFEE = "kitchen_coffee"
        const val ID_KITCHEN_STOVE = "kitchen_stove"
        const val ID_KITCHEN_COUNTER = "kitchen_counter"

        // Gaming Room Hotspot IDs
        const val ID_GAMING_PC = "gaming_pc"
        const val ID_GAMING_ARCADE = "gaming_arcade"
        const val ID_GAMING_DESK = "gaming_desk"
        const val ID_GAMING_TROPHY = "gaming_trophy"

        fun createDefaultRoomConfig(): RoomConfig {
            val defaultRooms = createDefaultRooms()
            return RoomConfig(
                version = 3,
                onboardingCompleted = false,
                onboardingStep = 0,
                autoPopulated = false,
                multiRoomAutoPopulated = false,
                rooms = defaultRooms,
                hotspots = defaultRooms.first().hotspots
            )
        }

        fun createDefaultRooms(): List<RoomPage> = listOf(
            RoomPage(
                id = ROOM_ID_BEDROOM,
                name = "Bedroom",
                type = RoomType.BEDROOM,
                hotspots = createDefaultHotspots()
            ),
            RoomPage(
                id = ROOM_ID_LIVING_ROOM,
                name = "Living Room",
                type = RoomType.LIVING_ROOM,
                hotspots = createLivingRoomHotspots()
            ),
            RoomPage(
                id = ROOM_ID_KITCHEN,
                name = "Kitchen",
                type = RoomType.KITCHEN,
                hotspots = createKitchenHotspots()
            ),
            RoomPage(
                id = ROOM_ID_GAMING_ROOM,
                name = "Gaming Room",
                type = RoomType.GAMING_ROOM,
                hotspots = createGamingRoomHotspots()
            )
        )

        fun createPresetRoom(type: RoomType, id: String, name: String = type.displayName): RoomPage {
            val prefix = if (id in setOf(ROOM_ID_BEDROOM, ROOM_ID_LIVING_ROOM, ROOM_ID_KITCHEN, ROOM_ID_GAMING_ROOM)) {
                ""
            } else {
                "${id}_"
            }
            val baseHotspots = when (type) {
                RoomType.BEDROOM -> createDefaultHotspots()
                RoomType.LIVING_ROOM -> createLivingRoomHotspots()
                RoomType.KITCHEN -> createKitchenHotspots()
                RoomType.GAMING_ROOM -> createGamingRoomHotspots()
            }
            val mapped = if (prefix.isEmpty()) baseHotspots else baseHotspots.map { it.copy(id = "$prefix${it.id}") }
            return RoomPage(id = id, name = name, type = type, hotspots = mapped)
        }

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
                id = ID_NIGHTSTAND,
                label = "Night Table",
                subtitle = "Calls, Messages & Chat",
                x = 0.60f,
                y = 0.655f,
                width = 0.17f,
                height = 0.14f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.NIGHTSTAND
            ),
            RoomHotspot(
                id = ID_TRASH_CAN,
                label = "Trash Can",
                subtitle = "Unsorted Apps",
                x = 0.79f,
                y = 0.765f,
                width = 0.12f,
                height = 0.09f,
                actionType = HotspotActionType.UNSORTED_PANEL,
                visualType = RoomObjectVisual.TRASH_CAN
            )
        )

        fun createLivingRoomHotspots(): List<RoomHotspot> = listOf(
            RoomHotspot(
                id = ID_LIVING_WINDOW,
                label = "Lounge Window",
                subtitle = "Live Weather",
                x = 0.05f,
                y = 0.11f,
                width = 0.30f,
                height = 0.165f,
                actionType = HotspotActionType.DIRECT_WEATHER,
                visualType = RoomObjectVisual.WINDOW
            ),
            RoomHotspot(
                id = ID_LIVING_FIREPLACE,
                label = "Fireplace",
                subtitle = "Favorites & Chill",
                x = 0.39f,
                y = 0.33f,
                width = 0.54f,
                height = 0.245f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.FIREPLACE
            ),
            RoomHotspot(
                id = ID_LIVING_VINYL,
                label = "Vinyl Hi-Fi",
                subtitle = "Music, Radio & Podcasts",
                x = 0.05f,
                y = 0.36f,
                width = 0.31f,
                height = 0.21f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.VINYL_PLAYER
            ),
            RoomHotspot(
                id = ID_LIVING_SOFA,
                label = "Lounge Sofa",
                subtitle = "Social & Messaging",
                x = 0.06f,
                y = 0.61f,
                width = 0.62f,
                height = 0.20f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.SOFA
            ),
            RoomHotspot(
                id = ID_LIVING_COFFEE_TABLE,
                label = "Coffee Table",
                subtitle = "News, Books & Reading",
                x = 0.69f,
                y = 0.68f,
                width = 0.26f,
                height = 0.15f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.COFFEE_TABLE
            )
        )

        fun createKitchenHotspots(): List<RoomHotspot> = listOf(
            RoomHotspot(
                id = ID_KITCHEN_CLOCK,
                label = "Kitchen Timer",
                subtitle = "Alarms & Timers",
                x = 0.69f,
                y = 0.11f,
                width = 0.24f,
                height = 0.125f,
                actionType = HotspotActionType.DIRECT_ALARM,
                visualType = RoomObjectVisual.CLOCK
            ),
            RoomHotspot(
                id = ID_KITCHEN_FRIDGE,
                label = "Fridge",
                subtitle = "Food & Delivery",
                x = 0.045f,
                y = 0.24f,
                width = 0.33f,
                height = 0.35f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.FRIDGE
            ),
            RoomHotspot(
                id = ID_KITCHEN_COFFEE,
                label = "Espresso Bar",
                subtitle = "Finance, Maps & Morning",
                x = 0.42f,
                y = 0.27f,
                width = 0.27f,
                height = 0.155f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.COFFEE_MAKER
            ),
            RoomHotspot(
                id = ID_KITCHEN_STOVE,
                label = "Stove & Oven",
                subtitle = "Health, Fitness & Recipes",
                x = 0.42f,
                y = 0.435f,
                width = 0.51f,
                height = 0.165f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.STOVE
            ),
            RoomHotspot(
                id = ID_KITCHEN_COUNTER,
                label = "Pantry Counter",
                subtitle = "Shopping & Groceries",
                x = 0.08f,
                y = 0.65f,
                width = 0.84f,
                height = 0.195f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.KITCHEN_COUNTER
            )
        )

        fun createGamingRoomHotspots(): List<RoomHotspot> = listOf(
            RoomHotspot(
                id = ID_GAMING_TROPHY,
                label = "Trophy Shelf",
                subtitle = "Utilities, Camera & Mods",
                x = 0.06f,
                y = 0.13f,
                width = 0.56f,
                height = 0.145f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.TROPHY_SHELF
            ),
            RoomHotspot(
                id = ID_GAMING_ARCADE,
                label = "Arcade Cabinet",
                subtitle = "Arcade & Action Games",
                x = 0.65f,
                y = 0.23f,
                width = 0.30f,
                height = 0.35f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.ARCADE_CABINET
            ),
            RoomHotspot(
                id = ID_GAMING_PC,
                label = "Gaming PC",
                subtitle = "Dev, Tools & Cloud Gaming",
                x = 0.05f,
                y = 0.33f,
                width = 0.57f,
                height = 0.255f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.GAMING_PC
            ),
            RoomHotspot(
                id = ID_GAMING_DESK,
                label = "Stream Desk",
                subtitle = "Discord, Voice & Streams",
                x = 0.11f,
                y = 0.645f,
                width = 0.78f,
                height = 0.20f,
                actionType = HotspotActionType.PANEL,
                visualType = RoomObjectVisual.STREAMING_DESK
            )
        )

        fun syncLegacyHotspots(config: RoomConfig): RoomConfig {
            val rooms = config.effectiveRooms
            val primaryHotspots = rooms.firstOrNull()?.hotspots ?: config.hotspots
            return config.copy(rooms = rooms, hotspots = primaryHotspots)
        }

        /**
         * Migrates v1/v2 saved JSON cleanly into v3 multi-room config without ever discarding
         * the user's custom Bedroom layout or app assignments.
         */
        fun migrateIfNeeded(parsed: RoomConfig): RoomConfig {
            val migratedBedroomHotspots = migrateBedroomHotspots(parsed.version, parsed.hotspots)

            if (parsed.rooms.isEmpty()) {
                val rooms = listOf(
                    RoomPage(
                        id = ROOM_ID_BEDROOM,
                        name = "Bedroom",
                        type = RoomType.BEDROOM,
                        hotspots = migratedBedroomHotspots
                    ),
                    RoomPage(
                        id = ROOM_ID_LIVING_ROOM,
                        name = "Living Room",
                        type = RoomType.LIVING_ROOM,
                        hotspots = createLivingRoomHotspots()
                    ),
                    RoomPage(
                        id = ROOM_ID_KITCHEN,
                        name = "Kitchen",
                        type = RoomType.KITCHEN,
                        hotspots = createKitchenHotspots()
                    ),
                    RoomPage(
                        id = ROOM_ID_GAMING_ROOM,
                        name = "Gaming Room",
                        type = RoomType.GAMING_ROOM,
                        hotspots = createGamingRoomHotspots()
                    )
                )
                return parsed.copy(
                    version = 3,
                    rooms = rooms,
                    hotspots = migratedBedroomHotspots
                )
            }

            // Already v3 multi-room config — ensure every room has valid visuals and hotspots synced
            val normalizedRooms = parsed.rooms.map { room ->
                if (room.type == RoomType.BEDROOM && room.hotspots.isEmpty()) {
                    room.copy(hotspots = createDefaultHotspots())
                } else {
                    room
                }
            }
            return parsed.copy(
                version = 3,
                rooms = normalizedRooms,
                hotspots = normalizedRooms.first().hotspots
            )
        }

        private fun migrateBedroomHotspots(version: Int, hotspots: List<RoomHotspot>): List<RoomHotspot> {
            if (hotspots.isEmpty()) return createDefaultHotspots()
            val defaultsById = createDefaultHotspots().associateBy { it.id }

            val updatedExisting = hotspots.map { h ->
                val inferredVisual = if (h.visualType == RoomObjectVisual.CUSTOM) {
                    when (h.id) {
                        ID_TV -> RoomObjectVisual.TV
                        ID_CONSOLE -> RoomObjectVisual.CONSOLE
                        ID_SHELF -> RoomObjectVisual.SHELF
                        ID_PAINTING -> RoomObjectVisual.PAINTING
                        ID_TRASH_CAN -> RoomObjectVisual.TRASH_CAN
                        ID_BED -> RoomObjectVisual.BED
                        ID_NIGHTSTAND -> RoomObjectVisual.NIGHTSTAND
                        ID_CLOCK -> RoomObjectVisual.CLOCK
                        ID_WINDOW -> RoomObjectVisual.WINDOW
                        ID_STICKY_NOTE -> RoomObjectVisual.STICKY_NOTE
                        else -> RoomObjectVisual.CUSTOM
                    }
                } else {
                    h.visualType
                }

                if (version < 2 && h.id == ID_TV && h.height > 0.19f) {
                    h.copy(visualType = inferredVisual, y = 0.305f, height = 0.145f)
                } else {
                    h.copy(visualType = inferredVisual)
                }
            }.toMutableList()

            val existingIds = updatedExisting.map { it.id }.toSet()
            if (version < 2) {
                listOf(ID_WINDOW, ID_CONSOLE, ID_STICKY_NOTE).forEach { newId ->
                    if (newId !in existingIds) {
                        defaultsById[newId]?.let { updatedExisting.add(it) }
                    }
                }
            }
            return updatedExisting
        }

        private val TV_KNOWN_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.disney.disneyplus",
            "com.amazon.avod.thirdpartyclient",
            "org.videolan.vlc",
            "com.plexapp.android",
            "com.mxtech.videoplayer.ad",
            "com.mxtech.videoplayer.pro",
            "com.hbo.hbonow",
            "com.wbd.stream"
        )
        private val TV_KEYWORDS = listOf("youtube", "netflix", "video", "player", "cinema", "primevideo", "disney")

        private val VINYL_KNOWN_PACKAGES = setOf(
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.soundcloud.android",
            "com.apple.android.music",
            "au.com.shiftyjelly.pocketcasts",
            "com.audible.application",
            "de.danoeh.antennapod",
            "com.google.android.apps.podcasts",
            "com.bambuna.podcastaddict",
            "com.aspiro.tidal",
            "deezer.android.app"
        )
        private val VINYL_KEYWORDS = listOf("spotify", "music", "podcast", "radio", "soundcloud", "tidal", "deezer", "audio")

        private val SOCIAL_KNOWN_PACKAGES = setOf(
            "com.whatsapp",
            "com.instagram.android",
            "org.telegram.messenger",
            "com.facebook.orca",
            "com.facebook.katana",
            "com.snapchat.android",
            "com.twitter.android",
            "com.zhiliaoapp.musically",
            "com.reddit.frontpage",
            "org.thoughtcrime.securesms",
            "com.pinterest",
            "com.instagram.barcelona"
        )
        private val SOCIAL_KEYWORDS = listOf("whatsapp", "instagram", "telegram", "messenger", "snapchat", "tiktok", "reddit", "signal", "chat", "social")

        private val NEWS_KEYWORDS = listOf("news", "kindle", "book", "reader", "medium", "wikipedia", "feedly", "nytimes", "bbc", "pocket")

        private val FOOD_KEYWORDS = listOf("ubereats", "ubercab.eats", "uber eats", "doordash", "glovo", "wolt", "pyszne", "deliveroo", "grubhub", "starbucks", "mcdonald", "recipe", "food", "yelp", "zomato", "justeat")

        private val SHOPPING_KEYWORDS = listOf("amazon.mshop", "allegro", "ebay", "aliexpress", "ikea", "etsy", "vinted", "temu", "shein", "target", "walmart", "shop", "store", "lidl", "zabka", "biedronka")

        private val MORNING_FINANCE_KEYWORDS = listOf("bank", "paypal", "revolut", "wallet", "venmo", "cashapp", "wise", "maps", "uber", "bolt", "transit", "commute")

        private val FITNESS_KEYWORDS = listOf("strava", "fitbit", "fitness", "health", "workout", "run", "myfitnesspal", "garmin", "nike.plus", "samsunghealth")

        private val STREAMING_KNOWN_PACKAGES = setOf(
            "com.discord",
            "tv.twitch.android.app",
            "com.kick.mobile",
            "com.teamspeak.ts3client"
        )
        private val STREAMING_KEYWORDS = listOf("discord", "twitch", "kick", "stream", "streamlabs", "obsblade", "obs.studio", "teamspeak")

        private val GAMING_PC_KEYWORDS = listOf("termux", "github", "steam", "geforcenow", "parsec", "moonlight", "vpn", "wireguard", "tailscale", "code", "terminal", "ssh", "files")

        private val TROPHY_KEYWORDS = listOf("shizuku", "tasker", "speedtest", "authenticator", "bitwarden", "keepass", "1password", "camera", "benchmark")

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
            "com.robtopx.",
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
            "game", "chess", "sudoku", "minecraft", "roblox", "geometry", "xbox", "playstation", "emulator"
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
            "com.ticktick.task",
            "com.trello",
            "com.dropbox.android",
            "com.google.android.apps.docs.editors.keep",
            "com.microsoft.office.word",
            "com.microsoft.office.excel",
            "com.microsoft.office.onenote",
            "ch.protonmail.android"
        )
        private val SHELF_KEYWORDS = listOf(
            "notes", "mail", "calendar", "docs", "office", "notion", "obsidian", "todo", "drive", "pdf"
        )

        /**
         * Communicator apps that live on the Night Table phone: dialers, SMS/MMS apps and
         * the big cross-platform chat clients (WhatsApp, Telegram, Signal, Discord, …).
         */
        private val COMMS_KNOWN_PACKAGES = setOf(
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.phone",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.oneplus.dialer",
            "com.oplus.dialer",
            "com.miui.dialer",
            "com.android.incallui",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
            "com.oneplus.mms",
            "com.miui.smsextra",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.plus",
            "org.thoughtcrime.securesms",
            "com.discord",
            "com.facebook.orca",
            "com.facebook.mlite",
            "com.viber.voip",
            "jp.naver.line.android",
            "com.tencent.mm",
            "com.skype.raider",
            "org.thunderdog.challegram",
            "com.kakao.talk",
            "com.instagram.android",
            "com.snapchat.android",
            "com.google.android.apps.messaging.messaging",
            "com.microsoft.teams.mobile",
            "com.slack"
        )
        private val COMMS_KEYWORDS = listOf(
            "dialer", "incallui", "telecom", "telephony",
            "messaging", "message", "sms", "mms", "chat",
            "whatsapp", "telegram", "signal", "discord", "messenger",
            "viber", "wechat", "skype", "threema", "wickr", "kakao", "wecom"
        )
        private val COMMS_EXCLUDED_KEYWORDS = listOf(
            "keyboard", "gboard", "swiftkey", "callrecorder", "phoneclean",
            "phonelink", "phonepe", "phonetransfer", "wallpaper", "launcher"
        )

        private val BED_KNOWN_PACKAGES = setOf(
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.oneplus.deskclock",
            "com.android.deskclock",
            "com.urbandroid.sleep",
            "com.calm.android",
            "com.getsomeheadspace.android",
            "com.ouraring.oura"
        )
        private val BED_KEYWORDS = listOf("clock", "alarm", "sleep", "calm", "meditat")

        private val STICKY_NOTE_KNOWN_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.brave.browser",
            "com.opera.browser",
            "com.microsoft.emmx",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.sec.android.app.sbrowser",
            "com.kiwibrowser.browser"
        )
        private val STICKY_NOTE_KEYWORDS = listOf("browser", "chrome", "firefox", "brave", "opera", "vivaldi", "duckduckgo")

        /**
         * v3.2.4 furniture pass (idempotent, runs for existing users too):
         *  - every Bedroom gets a Night Table hotspot holding all communicator apps (the phone
         *    on the table reacts to incoming calls), and
         *  - the Trash Can is scaled down and tucked into the corner so it no longer dominates
         *    the room.
         */
        fun migrateBedroomFurniture(config: RoomConfig, installedApps: List<AppInfo>): RoomConfig {
            val rooms = config.effectiveRooms
            var anyRoomChanged = false

            val updatedRooms = rooms.map { room ->
                if (room.type != RoomType.BEDROOM) return@map room
                var roomChanged = false
                val prefix = if (room.id == ROOM_ID_BEDROOM) "" else "${room.id}_"
                val nightstandId = "$prefix$ID_NIGHTSTAND"

                // 1. Scale the trash can down into the bottom-right corner
                var hotspots = room.hotspots.map { hotspot ->
                    val isTrash = hotspot.visualType == RoomObjectVisual.TRASH_CAN ||
                        hotspot.id == "$prefix$ID_TRASH_CAN" || hotspot.id == ID_TRASH_CAN
                    if (isTrash && (hotspot.width > 0.155f || hotspot.height > 0.115f)) {
                        roomChanged = true
                        hotspot.copy(x = 0.79f, y = 0.765f, width = 0.12f, height = 0.09f)
                    } else {
                        hotspot
                    }
                }

                // 2. Add the Night Table with every installed communicator app on its phone
                val hasNightstand = hotspots.any {
                    it.visualType == RoomObjectVisual.NIGHTSTAND || it.id == nightstandId
                }
                if (!hasNightstand) {
                    val commsPackages = installedApps
                        .filter { isCommsApp(it.packageName, it.packageName.lowercase(), it.label.lowercase()) }
                        .map { it.packageName }
                    if (commsPackages.isNotEmpty()) {
                        // Never duplicate a communicator app on another object in the same room
                        hotspots = hotspots.map { hotspot ->
                            if (hotspot.actionType == HotspotActionType.PANEL) {
                                hotspot.copy(packages = hotspot.packages.filterNot { it in commsPackages })
                            } else {
                                hotspot
                            }
                        }
                    }
                    hotspots = hotspots + RoomHotspot(
                        id = nightstandId,
                        label = "Night Table",
                        subtitle = "Calls, Messages & Chat",
                        x = 0.60f,
                        y = 0.655f,
                        width = 0.17f,
                        height = 0.14f,
                        actionType = HotspotActionType.PANEL,
                        visualType = RoomObjectVisual.NIGHTSTAND,
                        packages = commsPackages
                    )
                    roomChanged = true
                }

                if (roomChanged) {
                    anyRoomChanged = true
                    room.copy(hotspots = hotspots)
                } else {
                    room
                }
            }

            if (!anyRoomChanged) return config
            return syncLegacyHotspots(
                config.copy(
                    rooms = updatedRooms,
                    bedroomFurnitureMigrated = true
                )
            )
        }

        fun autoPopulateIfNeeded(config: RoomConfig, installedApps: List<AppInfo>): RoomConfig {
            // v3.2.4 furniture pass — add the Bedroom Night Table phone & shrink the Trash Can
            val normalized = migrateBedroomFurniture(syncLegacyHotspots(config), installedApps)
            if (normalized.autoPopulated && normalized.multiRoomAutoPopulated) return normalized
            if (installedApps.isEmpty()) return normalized

            val existingRooms = normalized.effectiveRooms
            val assigned = existingRooms
                .flatMap { it.hotspots }
                .filter { it.actionType == HotspotActionType.PANEL }
                .flatMap { it.packages }
                .toMutableSet()

            val buckets = mutableMapOf<String, MutableList<String>>()
            fun addTo(hotspotId: String, pkg: String) {
                buckets.getOrPut(hotspotId) { mutableListOf() }.add(pkg)
                assigned.add(pkg)
            }

            val allExistingHotspots = existingRooms.flatMap { it.hotspots }
            val existingHotspotIds = allExistingHotspots.map { it.id }.toSet()
            val nightstandHotspotId = allExistingHotspots
                .firstOrNull { it.visualType == RoomObjectVisual.NIGHTSTAND }
                ?.id
            val populateBedroom = !normalized.autoPopulated

            for (app in installedApps) {
                if (app.packageName in assigned) continue
                val pkgLower = app.packageName.lowercase()
                val labelLower = app.label.lowercase()

                when {
                    // Gaming Room & Console
                    isStreamingApp(app.packageName, pkgLower, labelLower) && ID_GAMING_DESK in existingHotspotIds ->
                        addTo(ID_GAMING_DESK, app.packageName)

                    isGameApp(app.packageName, pkgLower, labelLower, app.category, app.isGameFlag) -> {
                        if (populateBedroom && ID_CONSOLE in existingHotspotIds) {
                            addTo(ID_CONSOLE, app.packageName)
                        } else if (ID_GAMING_ARCADE in existingHotspotIds) {
                            addTo(ID_GAMING_ARCADE, app.packageName)
                        }
                    }

                    // Night Table phone — every communicator app (dialer, SMS, WhatsApp, Telegram, Signal…)
                    isCommsApp(app.packageName, pkgLower, labelLower) && nightstandHotspotId != null ->
                        addTo(nightstandHotspotId, app.packageName)

                    // Kitchen categories
                    isFoodApp(pkgLower, labelLower) && ID_KITCHEN_FRIDGE in existingHotspotIds ->
                        addTo(ID_KITCHEN_FRIDGE, app.packageName)

                    isShoppingApp(pkgLower, labelLower) && ID_KITCHEN_COUNTER in existingHotspotIds ->
                        addTo(ID_KITCHEN_COUNTER, app.packageName)

                    isFitnessApp(pkgLower, labelLower) && ID_KITCHEN_STOVE in existingHotspotIds ->
                        addTo(ID_KITCHEN_STOVE, app.packageName)

                    isMorningFinanceApp(pkgLower, labelLower) && ID_KITCHEN_COFFEE in existingHotspotIds ->
                        addTo(ID_KITCHEN_COFFEE, app.packageName)

                    // Living Room categories
                    isVinylMusicApp(app.packageName, pkgLower, labelLower, app.category) && ID_LIVING_VINYL in existingHotspotIds ->
                        addTo(ID_LIVING_VINYL, app.packageName)

                    isSocialApp(app.packageName, pkgLower, labelLower, app.category) && ID_LIVING_SOFA in existingHotspotIds ->
                        addTo(ID_LIVING_SOFA, app.packageName)

                    isNewsApp(pkgLower, labelLower, app.category) && ID_LIVING_COFFEE_TABLE in existingHotspotIds ->
                        addTo(ID_LIVING_COFFEE_TABLE, app.packageName)

                    // Gaming Room tools & utilities
                    isGamingPcToolApp(pkgLower, labelLower) && ID_GAMING_PC in existingHotspotIds ->
                        addTo(ID_GAMING_PC, app.packageName)

                    isTrophyUtilApp(pkgLower, labelLower) && ID_GAMING_TROPHY in existingHotspotIds ->
                        addTo(ID_GAMING_TROPHY, app.packageName)

                    // Classic Bedroom categories (only on initial population so user's Bedroom stays untouched!)
                    populateBedroom && isBedApp(app.packageName, pkgLower, labelLower) && ID_BED in existingHotspotIds ->
                        addTo(ID_BED, app.packageName)

                    populateBedroom && isStickyNoteApp(app.packageName, pkgLower, labelLower) && ID_STICKY_NOTE in existingHotspotIds ->
                        addTo(ID_STICKY_NOTE, app.packageName)

                    populateBedroom && isTvApp(app.packageName, pkgLower, labelLower, app.category) && ID_TV in existingHotspotIds ->
                        addTo(ID_TV, app.packageName)

                    populateBedroom && isShelfApp(app.packageName, pkgLower, labelLower, app.category) && ID_SHELF in existingHotspotIds ->
                        addTo(ID_SHELF, app.packageName)
                }
            }

            val updatedRooms = existingRooms.map { room ->
                room.copy(
                    hotspots = room.hotspots.map { hotspot ->
                        val extra = buckets[hotspot.id]
                        if (extra.isNullOrEmpty()) {
                            hotspot
                        } else {
                            hotspot.copy(packages = (hotspot.packages + extra).distinct())
                        }
                    }
                )
            }

            return syncLegacyHotspots(
                normalized.copy(
                    autoPopulated = true,
                    multiRoomAutoPopulated = true,
                    rooms = updatedRooms
                )
            )
        }

        private fun isGameApp(pkg: String, pkgLower: String, labelLower: String, category: Int, isGameFlag: Boolean): Boolean {
            if (category == ApplicationInfo.CATEGORY_GAME || isGameFlag) return true
            if (CONSOLE_KNOWN_PREFIXES.any { pkg.startsWith(it, ignoreCase = true) }) return true
            return CONSOLE_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        private fun isTvApp(pkg: String, pkgLower: String, labelLower: String, category: Int): Boolean {
            if (pkg in TV_KNOWN_PACKAGES) return true
            if (category == ApplicationInfo.CATEGORY_VIDEO) return true
            return TV_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        private fun isVinylMusicApp(pkg: String, pkgLower: String, labelLower: String, category: Int): Boolean {
            if (pkg in VINYL_KNOWN_PACKAGES) return true
            if (category == ApplicationInfo.CATEGORY_AUDIO) return true
            return VINYL_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        private fun isSocialApp(pkg: String, pkgLower: String, labelLower: String, category: Int): Boolean {
            if (pkg in SOCIAL_KNOWN_PACKAGES) return true
            if (category == ApplicationInfo.CATEGORY_SOCIAL) return true
            return SOCIAL_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        private fun isNewsApp(pkgLower: String, labelLower: String, category: Int): Boolean {
            if (category == ApplicationInfo.CATEGORY_NEWS) return true
            return NEWS_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        private fun isFoodApp(pkgLower: String, labelLower: String): Boolean =
            FOOD_KEYWORDS.any { it in pkgLower || it in labelLower }

        private fun isShoppingApp(pkgLower: String, labelLower: String): Boolean =
            SHOPPING_KEYWORDS.any { it in pkgLower || it in labelLower }

        private fun isMorningFinanceApp(pkgLower: String, labelLower: String): Boolean =
            MORNING_FINANCE_KEYWORDS.any { it in pkgLower || it in labelLower }

        private fun isFitnessApp(pkgLower: String, labelLower: String): Boolean =
            FITNESS_KEYWORDS.any { it in pkgLower || it in labelLower }

        private fun isStreamingApp(pkg: String, pkgLower: String, labelLower: String): Boolean {
            if (pkg in STREAMING_KNOWN_PACKAGES) return true
            return STREAMING_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        private fun isGamingPcToolApp(pkgLower: String, labelLower: String): Boolean =
            GAMING_PC_KEYWORDS.any { it in pkgLower || it in labelLower }

        private fun isTrophyUtilApp(pkgLower: String, labelLower: String): Boolean =
            TROPHY_KEYWORDS.any { it in pkgLower || it in labelLower }

        private fun isShelfApp(pkg: String, pkgLower: String, labelLower: String, category: Int): Boolean {
            if (pkg in SHELF_KNOWN_PACKAGES) return true
            if (category == ApplicationInfo.CATEGORY_PRODUCTIVITY) return true
            return SHELF_KEYWORDS.any { it in pkgLower || it in labelLower }
        }

        fun isCommsApp(pkg: String, pkgLower: String, labelLower: String): Boolean {
            if (COMMS_EXCLUDED_KEYWORDS.any { it in pkgLower || it in labelLower }) return false
            if (pkg in COMMS_KNOWN_PACKAGES) return true
            return COMMS_KEYWORDS.any { it in pkgLower || it in labelLower }
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
                    val assigned = config.allHotspots
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
            val rooms = config.effectiveRooms
            val multiRoom = rooms.size > 1
            rooms.forEach { room ->
                room.hotspots.forEach { hotspot ->
                    if (hotspot.actionType == HotspotActionType.PANEL) {
                        val ownerLabel = if (multiRoom) "${room.name} • ${hotspot.label}" else hotspot.label
                        hotspot.packages.forEach { pkg ->
                            map[pkg] = ownerLabel
                        }
                    }
                }
            }
            return map
        }

        fun defaultUnsortedLabel(config: RoomConfig): String {
            return config.allHotspots
                .firstOrNull { it.actionType == HotspotActionType.UNSORTED_PANEL }
                ?.label ?: "Trash Can"
        }
    }
}
