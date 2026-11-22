package com.roomlauncher.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class ChargingAnimationStyle(
    val displayName: String,
    val description: String
) {
    MATCH_ROOM("Match Room Style", "Adapts automatically to your active Room Style"),
    LIQUID_GLASS("Liquid Orb", "Glowing animated liquid & bubbles filling to your battery %"),
    AURORA_WAVE("Aurora Ribbons", "Flowing northern-lights waves around a glass energy core"),
    CYBER_PULSE("Cyber Plasma", "Dual rotating neon energy rings & electric scan pulses"),
    COSMIC_ORBIT("Cosmic Orbit", "Glowing golden eclipse core with orbiting stars & comets"),
    VOXEL_REDSTONE("Redstone Voxel", "Minecraft Redstone sparks & emerald XP charge bar")
}

@Serializable
enum class RoomStyleId(
    val displayName: String,
    val description: String
) {
    COZY_MINIMALIST("Cozy Minimalist", "Calm Scandinavian room & clean wood furniture"),
    GIRLY_ROOM("Girly Room (Alpha)", "Blush striped walls, pearl fairy lights, ribbon bows, plush heart rug & dreamy vanity accents"),
    TEEN_ROOM("Teen Room", "LED wall glow, fairy lights & 2 custom wall posters"),
    KIDS_ROOM("Kids Room", "Bunting garland, starry walls & cheerful play rug"),
    MINECRAFT("Blocky / Voxel", "Pixelated oak walls, torches & blocky furniture"),
    GEOMETRY_DASH("Geometry Dash", "Extreme Demon room with Sonic Wave, Astral Divinity & Death Corridor sub-themes")
}

@Serializable
enum class GeometryDashSubTheme(
    val displayName: String,
    val creatorTag: String,
    val description: String
) {
    SONIC_WAVE(
        displayName = "Sonic Wave",
        creatorTag = "Cyclic • Extreme Demon",
        description = "Electric cyan Nine Circles wave, spinning cobalt saws & neon grid"
    ),
    ASTRAL_DIVINITY(
        displayName = "Astral Divinity",
        creatorTag = "Knobbelboy • Extreme Demon",
        description = "Cosmic violet crystals, rainbow prism eyes & celestial nebula glow"
    ),
    DEATH_CORRIDOR(
        displayName = "Death Corridor",
        creatorTag = "KaotikJumper • Extreme Demon",
        description = "Pitch-black diamond wave corridor, blazing crimson spikes & hell saws"
    )
}

@Serializable
enum class RoomThemeId(
    val displayName: String,
    val description: String,
    val isDark: Boolean
) {
    WARM_LINEN("Warm Linen", "Cozy Scandinavian cream & oak", false),
    BLUSH_ROSE("Blush Rose", "Soft pastel pink, warm cream & rose-gold", false),
    BOTANICAL_SAGE("Botanical Sage", "Soft sunlit sage & birch", false),
    LAVENDER_NIGHT("Lavender Night", "Dreamy twilight lilac, soft rose & starlight", true),
    MIDNIGHT_COZY("Midnight Cozy", "Deep slate walls & warm lamplight", true),
    FOREST_NIGHT("Forest Night", "Rich pine shadows & brass accents", true),
    WARM_EMBER("Warm Ember", "Dark espresso wood & terracotta glow", true),
    OLED_PITCH("OLED Pitch", "True black AMOLED minimal room", true)
}

@Immutable
data class RoomVisualPalette(
    val themeId: RoomThemeId,
    val isDark: Boolean,
    val wallMain: Color,
    val wallUpper: Color,
    val baseboard: Color,
    val baseboardShadow: Color,
    val floorMain: Color,
    val floorPlankLine: Color,
    val rugFill: Color,
    val rugBorder: Color,
    val woodDark: Color,
    val woodMid: Color,
    val woodLight: Color,
    val duvetPrimary: Color,
    val throwBlanket: Color,
    val pillowColor: Color,
    val tvBezel: Color,
    val tvScreen: Color,
    val consoleBody: Color,
    val consoleAccent: Color,
    val trashBody: Color,
    val trashRim: Color,
    val stickyNoteFill: Color,
    val stickyNoteFold: Color,
    val clockFace: Color,
    val clockHands: Color,
    val clockAccent: Color,
    val ambientLightTint: Color,
    val hudTextColor: Color,
    val colorScheme: ColorScheme
)

object RoomThemes {

    /**
     * Dynamic Time-of-Day theme schedule:
     *  - Daytime (06:00 - 14:59): Warm Linen
     *  - Afternoon / Evening (15:00 - 20:59, starting at 3 PM): Midnight Cozy
     *  - Night (21:00 - 05:59): OLED Pitch
     */
    fun dynamicThemeForHour(hourOfDay: Int): RoomThemeId = when (hourOfDay) {
        in 6..14 -> RoomThemeId.WARM_LINEN
        in 15..20 -> RoomThemeId.MIDNIGHT_COZY
        else -> RoomThemeId.OLED_PITCH
    }

    fun effectivePalette(
        roomStyle: RoomStyleId,
        themeId: RoomThemeId,
        gdSubTheme: GeometryDashSubTheme = GeometryDashSubTheme.SONIC_WAVE
    ): RoomVisualPalette {
        return if (roomStyle == RoomStyleId.GEOMETRY_DASH) {
            geometryDashPalette(gdSubTheme)
        } else {
            paletteFor(themeId)
        }
    }

    fun geometryDashPalette(subTheme: GeometryDashSubTheme): RoomVisualPalette = when (subTheme) {
        GeometryDashSubTheme.SONIC_WAVE -> RoomVisualPalette(
            themeId = RoomThemeId.OLED_PITCH,
            isDark = true,
            wallMain = Color(0xFF040B1C),
            wallUpper = Color(0xFF020612),
            baseboard = Color(0xFF0B224A),
            baseboardShadow = Color(0xFF01040B),
            floorMain = Color(0xFF051128),
            floorPlankLine = Color(0xFF00D4FF),
            rugFill = Color(0xFF081B3D),
            rugBorder = Color(0xFF00F0FF),
            woodDark = Color(0xFF0A1D3D),
            woodMid = Color(0xFF123466),
            woodLight = Color(0xFF1F58A8),
            duvetPrimary = Color(0xFF0B3B8C),
            throwBlanket = Color(0xFF00E5FF),
            pillowColor = Color(0xFFD6F7FF),
            tvBezel = Color(0xFF040914),
            tvScreen = Color(0xFF0A244D),
            consoleBody = Color(0xFF071329),
            consoleAccent = Color(0xFF00F0FF),
            trashBody = Color(0xFF0E2547),
            trashRim = Color(0xFF38BDF8),
            stickyNoteFill = Color(0xFF5CEBFF),
            stickyNoteFold = Color(0xFF18A9D6),
            clockFace = Color(0xFF071733),
            clockHands = Color(0xFFE0FAFF),
            clockAccent = Color(0xFF00E5FF),
            ambientLightTint = Color(0x2800E5FF),
            hudTextColor = Color(0xFFE8FAFF),
            colorScheme = darkColorScheme(
                primary = Color(0xFF00E5FF),
                onPrimary = Color(0xFF002029),
                primaryContainer = Color(0xFF09365E),
                onPrimaryContainer = Color(0xFFD4F8FF),
                secondary = Color(0xFF4D8DFF),
                onSecondary = Color(0xFF001A45),
                secondaryContainer = Color(0xFF102A5C),
                onSecondaryContainer = Color(0xFFD6E5FF),
                background = Color(0xFF030816),
                onBackground = Color(0xFFE8FAFF),
                surface = Color(0xFF071226),
                onSurface = Color(0xFFE8FAFF),
                surfaceVariant = Color(0xFF0F2142),
                onSurfaceVariant = Color(0xFF8CC4E8),
                outline = Color(0xFF1C467A)
            )
        )

        GeometryDashSubTheme.ASTRAL_DIVINITY -> RoomVisualPalette(
            themeId = RoomThemeId.OLED_PITCH,
            isDark = true,
            wallMain = Color(0xFF140724),
            wallUpper = Color(0xFF0B0317),
            baseboard = Color(0xFF2B104A),
            baseboardShadow = Color(0xFF07020F),
            floorMain = Color(0xFF180A2B),
            floorPlankLine = Color(0xFFD946EF),
            rugFill = Color(0xFF240F3F),
            rugBorder = Color(0xFFF472B6),
            woodDark = Color(0xFF24103B),
            woodMid = Color(0xFF3E1C63),
            woodLight = Color(0xFF6B2FA0),
            duvetPrimary = Color(0xFF5B21B6),
            throwBlanket = Color(0xFFF59E0B),
            pillowColor = Color(0xFFF5E6FF),
            tvBezel = Color(0xFF0E051A),
            tvScreen = Color(0xFF2B124C),
            consoleBody = Color(0xFF190A2E),
            consoleAccent = Color(0xFFE879F9),
            trashBody = Color(0xFF2E144D),
            trashRim = Color(0xFFFBBF24),
            stickyNoteFill = Color(0xFFFDE047),
            stickyNoteFold = Color(0xFFD97706),
            clockFace = Color(0xFF1C0C33),
            clockHands = Color(0xFFFAF0FF),
            clockAccent = Color(0xFFF59E0B),
            ambientLightTint = Color(0x2AE879F9),
            hudTextColor = Color(0xFFFAF0FF),
            colorScheme = darkColorScheme(
                primary = Color(0xFFE879F9),
                onPrimary = Color(0xFF2D0636),
                primaryContainer = Color(0xFF4A1563),
                onPrimaryContainer = Color(0xFFFCE7FF),
                secondary = Color(0xFFFBBF24),
                onSecondary = Color(0xFF332100),
                secondaryContainer = Color(0xFF523400),
                onSecondaryContainer = Color(0xFFFFEAB0),
                background = Color(0xFF0D041A),
                onBackground = Color(0xFFFAF0FF),
                surface = Color(0xFF17092B),
                onSurface = Color(0xFFFAF0FF),
                surfaceVariant = Color(0xFF281247),
                onSurfaceVariant = Color(0xFFCBB0E8),
                outline = Color(0xFF542885)
            )
        )

        GeometryDashSubTheme.DEATH_CORRIDOR -> RoomVisualPalette(
            themeId = RoomThemeId.OLED_PITCH,
            isDark = true,
            wallMain = Color(0xFF120305),
            wallUpper = Color(0xFF080102),
            baseboard = Color(0xFF33080E),
            baseboardShadow = Color(0xFF000000),
            floorMain = Color(0xFF160407),
            floorPlankLine = Color(0xFFFF2442),
            rugFill = Color(0xFF24070C),
            rugBorder = Color(0xFFFF3B30),
            woodDark = Color(0xFF26090D),
            woodMid = Color(0xFF471118),
            woodLight = Color(0xFF7A1C26),
            duvetPrimary = Color(0xFF7F1D1D),
            throwBlanket = Color(0xFFFF453A),
            pillowColor = Color(0xFFF5E6E8),
            tvBezel = Color(0xFF0A0203),
            tvScreen = Color(0xFF2B080E),
            consoleBody = Color(0xFF170508),
            consoleAccent = Color(0xFFFF2E4C),
            trashBody = Color(0xFF2E0B10),
            trashRim = Color(0xFFEF4444),
            stickyNoteFill = Color(0xFFFF6B57),
            stickyNoteFold = Color(0xFFC82828),
            clockFace = Color(0xFF1B0609),
            clockHands = Color(0xFFFFF1F2),
            clockAccent = Color(0xFFFF2E4C),
            ambientLightTint = Color(0x2CFF2442),
            hudTextColor = Color(0xFFFFF1F2),
            colorScheme = darkColorScheme(
                primary = Color(0xFFFF455B),
                onPrimary = Color(0xFF330008),
                primaryContainer = Color(0xFF5C0C18),
                onPrimaryContainer = Color(0xFFFFDCE0),
                secondary = Color(0xFFFF8A4C),
                onSecondary = Color(0xFF361200),
                secondaryContainer = Color(0xFF572107),
                onSecondaryContainer = Color(0xFFFFDFCE),
                background = Color(0xFF0A0203),
                onBackground = Color(0xFFFFF1F2),
                surface = Color(0xFF170508),
                onSurface = Color(0xFFFFF1F2),
                surfaceVariant = Color(0xFF2B0B10),
                onSurfaceVariant = Color(0xFFE0A4AC),
                outline = Color(0xFF691A25)
            )
        )
    }

    fun paletteFor(id: RoomThemeId): RoomVisualPalette = when (id) {
        RoomThemeId.WARM_LINEN -> RoomVisualPalette(
            themeId = id,
            isDark = false,
            wallMain = Color(0xFFF5EFE6),
            wallUpper = Color(0xFFEFE6D8),
            baseboard = Color(0xFFE5DAC8),
            baseboardShadow = Color(0xFFD4C5AE),
            floorMain = Color(0xFFDEC6A4),
            floorPlankLine = Color(0xFFB99971),
            rugFill = Color(0xFFEAE1D3),
            rugBorder = Color(0xFFCBBBA4),
            woodDark = Color(0xFF6D4C34),
            woodMid = Color(0xFF8D633E),
            woodLight = Color(0xFFA9825A),
            duvetPrimary = Color(0xFF6C8776),
            throwBlanket = Color(0xFFC97B5D),
            pillowColor = Color(0xFFFFFFFF),
            tvBezel = Color(0xFF262D30),
            tvScreen = Color(0xFF343F45),
            consoleBody = Color(0xFF2B3137),
            consoleAccent = Color(0xFF5CE1E6),
            trashBody = Color(0xFF475154),
            trashRim = Color(0xFFC2A37D),
            stickyNoteFill = Color(0xFFFFE57F),
            stickyNoteFold = Color(0xFFE8C85B),
            clockFace = Color(0xFFFCF9F4),
            clockHands = Color(0xFF2C3531),
            clockAccent = Color(0xFFC96A4B),
            ambientLightTint = Color(0x44FFFDF8),
            hudTextColor = Color(0xFF231F1C),
            colorScheme = lightColorScheme(
                primary = Color(0xFF3B5246),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDCE7DF),
                onPrimaryContainer = Color(0xFF1F2B24),
                secondary = Color(0xFFC97B5D),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF6DFD4),
                onSecondaryContainer = Color(0xFF3A1E14),
                background = Color(0xFFF5EFE6),
                onBackground = Color(0xFF231F1C),
                surface = Color(0xFFFAF7F2),
                onSurface = Color(0xFF231F1C),
                surfaceVariant = Color(0xFFEDE4D7),
                onSurfaceVariant = Color(0xFF665C54),
                outline = Color(0xFFD6C9B8)
            )
        )

        RoomThemeId.BOTANICAL_SAGE -> RoomVisualPalette(
            themeId = id,
            isDark = false,
            wallMain = Color(0xFFE7EFE8),
            wallUpper = Color(0xFFDAE6DC),
            baseboard = Color(0xFFD0DDD1),
            baseboardShadow = Color(0xFFBAC8BC),
            floorMain = Color(0xFFE3D3BA),
            floorPlankLine = Color(0xFFC2B094),
            rugFill = Color(0xFFF2ECE1),
            rugBorder = Color(0xFFC9D4C5),
            woodDark = Color(0xFF5D4B3B),
            woodMid = Color(0xFF8C7258),
            woodLight = Color(0xFFB3987C),
            duvetPrimary = Color(0xFF4F6D5A),
            throwBlanket = Color(0xFFD6A86E),
            pillowColor = Color(0xFFFCFBF8),
            tvBezel = Color(0xFF242D28),
            tvScreen = Color(0xFF324038),
            consoleBody = Color(0xFF2B352F),
            consoleAccent = Color(0xFF7CE0A3),
            trashBody = Color(0xFF4A5851),
            trashRim = Color(0xFFBBA382),
            stickyNoteFill = Color(0xFFFFEC99),
            stickyNoteFold = Color(0xFFE3CE72),
            clockFace = Color(0xFFFBFDFB),
            clockHands = Color(0xFF25322A),
            clockAccent = Color(0xFF5C8D6B),
            ambientLightTint = Color(0x40F8FFF9),
            hudTextColor = Color(0xFF1E2922),
            colorScheme = lightColorScheme(
                primary = Color(0xFF2F5D46),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD0E8D9),
                onPrimaryContainer = Color(0xFF153022),
                secondary = Color(0xFF9C7846),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF0E4CF),
                onSecondaryContainer = Color(0xFF332613),
                background = Color(0xFFE7EFE8),
                onBackground = Color(0xFF1E2922),
                surface = Color(0xFFF6FAF6),
                onSurface = Color(0xFF1E2922),
                surfaceVariant = Color(0xFFDCE7DE),
                onSurfaceVariant = Color(0xFF4F5E54),
                outline = Color(0xFFBCC9BE)
            )
        )

        RoomThemeId.MIDNIGHT_COZY -> RoomVisualPalette(
            themeId = id,
            isDark = true,
            wallMain = Color(0xFF1C2331),
            wallUpper = Color(0xFF161C28),
            baseboard = Color(0xFF252E40),
            baseboardShadow = Color(0xFF111620),
            floorMain = Color(0xFF2D2520),
            floorPlankLine = Color(0xFF1D1714),
            rugFill = Color(0xFF2B3242),
            rugBorder = Color(0xFF3E485E),
            woodDark = Color(0xFF3A2A1E),
            woodMid = Color(0xFF573F2D),
            woodLight = Color(0xFF735640),
            duvetPrimary = Color(0xFF3D5278),
            throwBlanket = Color(0xFFC27B58),
            pillowColor = Color(0xFFD9DCE6),
            tvBezel = Color(0xFF0E1218),
            tvScreen = Color(0xFF1A2433),
            consoleBody = Color(0xFF121720),
            consoleAccent = Color(0xFF64B5F6),
            trashBody = Color(0xFF343D4B),
            trashRim = Color(0xFF8C735B),
            stickyNoteFill = Color(0xFFD4B85A),
            stickyNoteFold = Color(0xFFB3963F),
            clockFace = Color(0xFFE6E9F0),
            clockHands = Color(0xFF19202C),
            clockAccent = Color(0xFFE08B5E),
            ambientLightTint = Color(0x1EE8C28B),
            hudTextColor = Color(0xFFF1F4FA),
            colorScheme = darkColorScheme(
                primary = Color(0xFF8EB8E5),
                onPrimary = Color(0xFF102438),
                primaryContainer = Color(0xFF273A54),
                onPrimaryContainer = Color(0xFFDCEBFF),
                secondary = Color(0xFFE09772),
                onSecondary = Color(0xFF361A0C),
                secondaryContainer = Color(0xFF4D2E1E),
                onSecondaryContainer = Color(0xFFFFDEC9),
                background = Color(0xFF141923),
                onBackground = Color(0xFFECEFF6),
                surface = Color(0xFF1D2433),
                onSurface = Color(0xFFECEFF6),
                surfaceVariant = Color(0xFF283145),
                onSurfaceVariant = Color(0xFFAAB4C8),
                outline = Color(0xFF3E4961)
            )
        )

        RoomThemeId.FOREST_NIGHT -> RoomVisualPalette(
            themeId = id,
            isDark = true,
            wallMain = Color(0xFF18241F),
            wallUpper = Color(0xFF121C18),
            baseboard = Color(0xFF21312A),
            baseboardShadow = Color(0xFF0E1713),
            floorMain = Color(0xFF2A231C),
            floorPlankLine = Color(0xFF1C1712),
            rugFill = Color(0xFF25352E),
            rugBorder = Color(0xFF3A5247),
            woodDark = Color(0xFF38291D),
            woodMid = Color(0xFF563F2C),
            woodLight = Color(0xFF74573E),
            duvetPrimary = Color(0xFF355947),
            throwBlanket = Color(0xFFC49456),
            pillowColor = Color(0xFFDDE5DF),
            tvBezel = Color(0xFF0E1411),
            tvScreen = Color(0xFF1A2822),
            consoleBody = Color(0xFF141D18),
            consoleAccent = Color(0xFF6BE09E),
            trashBody = Color(0xFF2F3E37),
            trashRim = Color(0xFFB5925B),
            stickyNoteFill = Color(0xFFD6BE65),
            stickyNoteFold = Color(0xFFB29B47),
            clockFace = Color(0xFFE6ECE8),
            clockHands = Color(0xFF17241E),
            clockAccent = Color(0xFFD4A259),
            ambientLightTint = Color(0x1CD8E8C8),
            hudTextColor = Color(0xFFEBF3EE),
            colorScheme = darkColorScheme(
                primary = Color(0xFF81C79E),
                onPrimary = Color(0xFF0E291A),
                primaryContainer = Color(0xFF234232),
                onPrimaryContainer = Color(0xFFD3F4E0),
                secondary = Color(0xFFD6AA6A),
                onSecondary = Color(0xFF2E200B),
                secondaryContainer = Color(0xFF47361C),
                onSecondaryContainer = Color(0xFFFCE6C5),
                background = Color(0xFF121B17),
                onBackground = Color(0xFFEAF2ED),
                surface = Color(0xFF1B2721),
                onSurface = Color(0xFFEAF2ED),
                surfaceVariant = Color(0xFF26362E),
                onSurfaceVariant = Color(0xFFA7BDB0),
                outline = Color(0xFF3B4F44)
            )
        )

        RoomThemeId.WARM_EMBER -> RoomVisualPalette(
            themeId = id,
            isDark = true,
            wallMain = Color(0xFF261C19),
            wallUpper = Color(0xFF1E1513),
            baseboard = Color(0xFF332521),
            baseboardShadow = Color(0xFF140E0C),
            floorMain = Color(0xFF36241B),
            floorPlankLine = Color(0xFF241711),
            rugFill = Color(0xFF3B2B25),
            rugBorder = Color(0xFF5A4138),
            woodDark = Color(0xFF3C2318),
            woodMid = Color(0xFF5E3927),
            woodLight = Color(0xFF805038),
            duvetPrimary = Color(0xFF7D4536),
            throwBlanket = Color(0xFFD4925A),
            pillowColor = Color(0xFFEDE0D6),
            tvBezel = Color(0xFF140F0D),
            tvScreen = Color(0xFF271D1A),
            consoleBody = Color(0xFF1B1412),
            consoleAccent = Color(0xFFFF9E6D),
            trashBody = Color(0xFF42332E),
            trashRim = Color(0xFFC49262),
            stickyNoteFill = Color(0xFFE0BA63),
            stickyNoteFold = Color(0xFFBA9444),
            clockFace = Color(0xFFF2E6DC),
            clockHands = Color(0xFF261B17),
            clockAccent = Color(0xFFE07A5F),
            ambientLightTint = Color(0x20FFAE73),
            hudTextColor = Color(0xFFF7ECE4),
            colorScheme = darkColorScheme(
                primary = Color(0xFFE89874),
                onPrimary = Color(0xFF381608),
                primaryContainer = Color(0xFF522A1B),
                onPrimaryContainer = Color(0xFFFFDFD1),
                secondary = Color(0xFFE0B872),
                onSecondary = Color(0xFF332308),
                secondaryContainer = Color(0xFF4D3918),
                onSecondaryContainer = Color(0xFFFFECC9),
                background = Color(0xFF1A1311),
                onBackground = Color(0xFFF5EAE2),
                surface = Color(0xFF251B18),
                onSurface = Color(0xFFF5EAE2),
                surfaceVariant = Color(0xFF362823),
                onSurfaceVariant = Color(0xFFC7B0A5),
                outline = Color(0xFF523E37)
            )
        )

        RoomThemeId.BLUSH_ROSE -> RoomVisualPalette(
            themeId = id,
            isDark = false,
            wallMain = Color(0xFFFCEBF1),
            wallUpper = Color(0xFFF6D9E5),
            baseboard = Color(0xFFFFF8FA),
            baseboardShadow = Color(0xFFE7C5D3),
            floorMain = Color(0xFFEED8CC),
            floorPlankLine = Color(0xFFD5B7A8),
            rugFill = Color(0xFFFAD3E3),
            rugBorder = Color(0xFFE993B6),
            woodDark = Color(0xFFB37E8D),
            woodMid = Color(0xFFD9AAB6),
            woodLight = Color(0xFFF6DFE5),
            duvetPrimary = Color(0xFFF09EC1),
            throwBlanket = Color(0xFFCDB4F6),
            pillowColor = Color(0xFFFFFDFE),
            tvBezel = Color(0xFF4A3340),
            tvScreen = Color(0xFF66485A),
            consoleBody = Color(0xFFFDF4F7),
            consoleAccent = Color(0xFFFF7EB6),
            trashBody = Color(0xFFEAB8CB),
            trashRim = Color(0xFFF6D6B0),
            stickyNoteFill = Color(0xFFFFD6E8),
            stickyNoteFold = Color(0xFFF4A9CB),
            clockFace = Color(0xFFFFFAFC),
            clockHands = Color(0xFF4D2D3D),
            clockAccent = Color(0xFFE85D99),
            ambientLightTint = Color(0x40FFF0F6),
            hudTextColor = Color(0xFF3A1F2D),
            colorScheme = lightColorScheme(
                primary = Color(0xFFC94B84),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFD8E8),
                onPrimaryContainer = Color(0xFF400E27),
                secondary = Color(0xFF9667C9),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFEEE1FF),
                onSecondaryContainer = Color(0xFF2D1548),
                background = Color(0xFFFDF2F6),
                onBackground = Color(0xFF361D2A),
                surface = Color(0xFFFFF9FB),
                onSurface = Color(0xFF361D2A),
                surfaceVariant = Color(0xFFF7E2EB),
                onSurfaceVariant = Color(0xFF704E5F),
                outline = Color(0xFFE3BED0)
            )
        )

        RoomThemeId.LAVENDER_NIGHT -> RoomVisualPalette(
            themeId = id,
            isDark = true,
            wallMain = Color(0xFF231936),
            wallUpper = Color(0xFF191129),
            baseboard = Color(0xFF35264F),
            baseboardShadow = Color(0xFF130C20),
            floorMain = Color(0xFF312238),
            floorPlankLine = Color(0xFF201526),
            rugFill = Color(0xFF3E2952),
            rugBorder = Color(0xFFF4A2C9),
            woodDark = Color(0xFF462D52),
            woodMid = Color(0xFF674475),
            woodLight = Color(0xFF8C629C),
            duvetPrimary = Color(0xFFB36BE2),
            throwBlanket = Color(0xFFF48FB1),
            pillowColor = Color(0xFFF8EEFA),
            tvBezel = Color(0xFF140D20),
            tvScreen = Color(0xFF291B40),
            consoleBody = Color(0xFF1E132E),
            consoleAccent = Color(0xFFFF8EC4),
            trashBody = Color(0xFF493159),
            trashRim = Color(0xFFF4B6D2),
            stickyNoteFill = Color(0xFFF8BBD0),
            stickyNoteFold = Color(0xFFE48EB0),
            clockFace = Color(0xFFF4E8F8),
            clockHands = Color(0xFF291838),
            clockAccent = Color(0xFFFF7EB6),
            ambientLightTint = Color(0x28F4A2C9),
            hudTextColor = Color(0xFFFBF2FF),
            colorScheme = darkColorScheme(
                primary = Color(0xFFF48FB1),
                onPrimary = Color(0xFF3D0C24),
                primaryContainer = Color(0xFF5A2342),
                onPrimaryContainer = Color(0xFFFFD8E8),
                secondary = Color(0xFFD0A2F7),
                onSecondary = Color(0xFF2B1145),
                secondaryContainer = Color(0xFF442466),
                onSecondaryContainer = Color(0xFFF1E3FF),
                background = Color(0xFF191129),
                onBackground = Color(0xFFF9EEFF),
                surface = Color(0xFF241A38),
                onSurface = Color(0xFFF9EEFF),
                surfaceVariant = Color(0xFF33254D),
                onSurfaceVariant = Color(0xFFCBB8DE),
                outline = Color(0xFF564178)
            )
        )

        RoomThemeId.OLED_PITCH -> RoomVisualPalette(
            themeId = id,
            isDark = true,
            wallMain = Color(0xFF000000),
            wallUpper = Color(0xFF050507),
            baseboard = Color(0xFF121216),
            baseboardShadow = Color(0xFF000000),
            floorMain = Color(0xFF09090B),
            floorPlankLine = Color(0xFF1A1A20),
            rugFill = Color(0xFF101116),
            rugBorder = Color(0xFF262833),
            woodDark = Color(0xFF1C1C22),
            woodMid = Color(0xFF2B2C35),
            woodLight = Color(0xFF3E404D),
            duvetPrimary = Color(0xFF1E293B),
            throwBlanket = Color(0xFF7C4D3A),
            pillowColor = Color(0xFFCBD5E1),
            tvBezel = Color(0xFF0A0A0D),
            tvScreen = Color(0xFF141821),
            consoleBody = Color(0xFF111217),
            consoleAccent = Color(0xFF38BDF8),
            trashBody = Color(0xFF1E2028),
            trashRim = Color(0xFF64748B),
            stickyNoteFill = Color(0xFFCBB25B),
            stickyNoteFold = Color(0xFF9F883B),
            clockFace = Color(0xFF14161D),
            clockHands = Color(0xFFE2E8F0),
            clockAccent = Color(0xFF38BDF8),
            ambientLightTint = Color(0x0AFFFFFF),
            hudTextColor = Color(0xFFF8FAFC),
            colorScheme = darkColorScheme(
                primary = Color(0xFF7DD3FC),
                onPrimary = Color(0xFF052233),
                primaryContainer = Color(0xFF122636),
                onPrimaryContainer = Color(0xFFDDF3FF),
                secondary = Color(0xFFF0AB89),
                onSecondary = Color(0xFF36190B),
                secondaryContainer = Color(0xFF361F15),
                onSecondaryContainer = Color(0xFFFFE1D1),
                background = Color(0xFF000000),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF0B0C10),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF171921),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF292D3A)
            )
        )
    }
}
