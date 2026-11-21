package com.roomlauncher.app

import com.roomlauncher.app.data.LrcParser
import com.roomlauncher.app.data.NowPlayingState
import com.roomlauncher.app.data.formatMusicTime
import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun parsesStandardAndMultiTimestampLrcWithOffset() {
        val raw = """
            [ar:Test Artist]
            [ti:Test Song]
            [offset:500]
            [00:10.50]First line
            [00:20.00][01:05.250]Repeated chorus
            [00:15]Middle line
        """.trimIndent()

        val lines = LrcParser.parseSyncedLyrics(raw)
        assertEquals(4, lines.size)
        assertEquals(10_000L, lines[0].timeMs)
        assertEquals("First line", lines[0].text)
        assertEquals(14_500L, lines[1].timeMs)
        assertEquals("Middle line", lines[1].text)
        assertEquals(19_500L, lines[2].timeMs)
        assertEquals("Repeated chorus", lines[2].text)
        assertEquals(64_750L, lines[3].timeMs)
        assertEquals("Repeated chorus", lines[3].text)
    }

    @Test
    fun findActiveLineIndexTracksPlaybackPositionAccurately() {
        val lines = LrcParser.parseSyncedLyrics(
            """
            [00:05.00]Line one
            [00:12.00]Line two
            [00:25.00]Line three
            """.trimIndent()
        )

        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 3_000L))
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 5_000L))
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 11_999L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 12_000L))
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 99_000L))
    }

    @Test
    fun cleansStreamingSuffixesFromTrackTitles() {
        assertEquals(
            "Midnight City",
            LrcParser.cleanTrackTitle("Midnight City (Official Video)")
        )
        assertEquals(
            "Dreams",
            LrcParser.cleanTrackTitle("Dreams - Remastered 2004")
        )
        assertEquals(
            "Starboy",
            LrcParser.cleanTrackTitle("Starboy (feat. Daft Punk)")
        )
    }

    @Test
    fun nowPlayingInterpolatesPositionAndFormatsTimestamps() {
        val state = NowPlayingState(
            hasSession = true,
            title = "Midnight Lo-Fi",
            artist = "Cozy Room",
            durationMs = 180_000L,
            positionMs = 30_000L,
            positionUpdateTimeMs = 1_000L,
            playbackSpeed = 1f,
            isPlaying = true
        )

        assertEquals(35_000L, state.currentPositionMs(6_000L))
        assertEquals(180_000L, state.currentPositionMs(500_000L))
        assertEquals("0:05", formatMusicTime(5_000L))
        assertEquals("3:00", formatMusicTime(180_000L))
    }
}
