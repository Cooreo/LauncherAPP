package com.roomlauncher.app.data

object LrcParser {

    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val OFFSET_REGEX = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val CLEAN_BRACKETS_REGEX = Regex("""\s*[\[(][^)\]]*(?:feat\.|ft\.|remaster|official|video|audio|lyric|version|explicit|deluxe)[^)\]]*[\])]""", RegexOption.IGNORE_CASE)
    private val CLEAN_DASH_SUFFIX_REGEX = Regex("""\s+-\s+(?:remaster|live|single|radio|acoustic|mono|stereo).*$""", RegexOption.IGNORE_CASE)

    /**
     * Parses standard LRC synced lyrics into chronologically ordered [LyricsLine] entries.
     * Supports [mm:ss.xx], [mm:ss.xxx], [mm:ss], multiple timestamps on one line, and [offset:ms].
     */
    fun parseSyncedLyrics(rawLrc: String): List<LyricsLine> {
        if (rawLrc.isBlank()) return emptyList()

        var offsetMs = 0L
        OFFSET_REGEX.find(rawLrc)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let {
            offsetMs = it
        }

        val result = mutableListOf<LyricsLine>()
        rawLrc.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isEmpty()) return@forEach

            val lyricText = TIMESTAMP_REGEX.replace(trimmed, "").trim()
            if (lyricText.isEmpty()) return@forEach

            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fracRaw = match.groupValues.getOrNull(3).orEmpty()
                val millis = when (fracRaw.length) {
                    0 -> 0L
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    else -> fracRaw.take(3).toLongOrNull() ?: 0L
                }
                val totalMs = ((minutes * 60L + seconds) * 1000L + millis - offsetMs).coerceAtLeast(0L)
                result.add(LyricsLine(timeMs = totalMs, text = lyricText))
            }
        }

        return result.sortedBy { it.timeMs }
    }

    /**
     * Splits plain unsynced lyrics into displayable lines while trimming excessive blank runs.
     */
    fun parsePlainLyrics(rawPlain: String): List<String> {
        if (rawPlain.isBlank()) return emptyList()
        val lines = mutableListOf<String>()
        var lastBlank = true
        rawPlain.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (!lastBlank) {
                    lines.add("")
                    lastBlank = true
                }
            } else {
                lines.add(trimmed)
                lastBlank = false
            }
        }
        if (lines.lastOrNull()?.isEmpty() == true) {
            lines.removeAt(lines.lastIndex)
        }
        return lines
    }

    /**
     * Returns the index of the currently active lyric line for [positionMs], or -1 before the first line.
     */
    fun findActiveLineIndex(lines: List<LyricsLine>, positionMs: Long): Int {
        if (lines.isEmpty() || positionMs < lines.first().timeMs) return -1
        var low = 0
        var high = lines.lastIndex
        var active = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                active = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return active
    }

    /**
     * Cleans common streaming-service suffixes like "(Official Video)" or "- Remastered 2011"
     * to improve search match rates on LRCLIB when exact match misses.
     */
    fun cleanTrackTitle(rawTitle: String): String {
        val noBrackets = CLEAN_BRACKETS_REGEX.replace(rawTitle, "")
        val noDashSuffix = CLEAN_DASH_SUFFIX_REGEX.replace(noBrackets, "")
        return noDashSuffix.trim().ifEmpty { rawTitle.trim() }
    }
}
