package com.roomlauncher.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

@Serializable
private data class LrcLibRecord(
    val id: Long = 0L,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

/**
 * Fetches time-synced (.lrc) and plain lyrics from the free, open-source LRCLIB API (lrclib.net).
 * No API key required; includes an in-memory cache and smart search fallback.
 */
class LyricsRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = object : LinkedHashMap<String, LyricsState>(48, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LyricsState>?): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }

    suspend fun fetchLyrics(
        trackKey: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        forceRefresh: Boolean = false
    ): LyricsState = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext LyricsState.Idle

        if (!forceRefresh) {
            synchronized(cache) {
                cache[trackKey]?.let { cached ->
                    if (cached !is LyricsState.Error) return@withContext cached
                }
            }
        }

        val durationSec = (durationMs / 1000L).coerceAtLeast(0L)
        val result = runCatching {
            // 1. Exact lookup via LRCLIB /api/get when we have artist & duration
            val exactRecord = if (artist.isNotBlank()) {
                fetchExact(title, artist, album, durationSec)
            } else {
                null
            }
            val exactState = exactRecord?.let { toLyricsState(it) }
            if (exactState is LyricsState.Synced || exactState is LyricsState.Instrumental) {
                exactState
            } else {
                // 2. Smart fallback via LRCLIB /api/search using cleaned track & artist names
                val cleanedTitle = LrcParser.cleanTrackTitle(title)
                val primaryArtist = artist.split(",", "&", "feat.", "ft.", ignoreCase = true)
                    .firstOrNull()?.trim().orEmpty()
                val candidates = fetchSearch(cleanedTitle, primaryArtist)
                val best = pickBestMatch(candidates, cleanedTitle, primaryArtist, durationSec)
                best?.let { toLyricsState(it) } ?: exactState ?: LyricsState.NotFound
            }
        }.getOrElse { error ->
            LyricsState.Error(error.message?.take(80) ?: "Could not reach LRCLIB")
        }

        if (result !is LyricsState.Error) {
            synchronized(cache) {
                cache[trackKey] = result
            }
        }
        result
    }

    private fun fetchExact(
        title: String,
        artist: String,
        album: String,
        durationSec: Long
    ): LrcLibRecord? {
        val params = buildList {
            add("track_name=${encode(title)}")
            add("artist_name=${encode(artist)}")
            if (album.isNotBlank()) add("album_name=${encode(album)}")
            if (durationSec > 0L) add("duration=$durationSec")
        }.joinToString("&")

        val body = httpGet("https://lrclib.net/api/get?$params") ?: return null
        return runCatching { json.decodeFromString<LrcLibRecord>(body) }.getOrNull()
    }

    private fun fetchSearch(cleanedTitle: String, artist: String): List<LrcLibRecord> {
        val params = buildList {
            add("track_name=${encode(cleanedTitle)}")
            if (artist.isNotBlank()) add("artist_name=${encode(artist)}")
        }.joinToString("&")

        val body = httpGet("https://lrclib.net/api/search?$params")
            ?: httpGet("https://lrclib.net/api/search?q=${encode("$cleanedTitle $artist".trim())}")
            ?: return emptyList()

        return runCatching { json.decodeFromString<List<LrcLibRecord>>(body) }.getOrDefault(emptyList())
    }

    private fun pickBestMatch(
        candidates: List<LrcLibRecord>,
        cleanedTitle: String,
        artist: String,
        durationSec: Long
    ): LrcLibRecord? {
        if (candidates.isEmpty()) return null
        val targetTitle = cleanedTitle.lowercase()
        val targetArtist = artist.lowercase()

        return candidates.maxByOrNull { record ->
            var score = 0
            if (!record.syncedLyrics.isNullOrBlank()) score += 120
            else if (!record.plainLyrics.isNullOrBlank()) score += 45
            else if (record.instrumental) score += 20

            val recTitle = record.trackName.orEmpty().lowercase()
            if (recTitle == targetTitle) score += 50
            else if (recTitle.contains(targetTitle) || targetTitle.contains(recTitle)) score += 25

            val recArtist = record.artistName.orEmpty().lowercase()
            if (targetArtist.isNotBlank()) {
                if (recArtist == targetArtist) score += 45
                else if (recArtist.contains(targetArtist) || targetArtist.contains(recArtist)) score += 25
            }

            if (durationSec > 0L && record.duration != null) {
                val diff = abs(record.duration - durationSec.toDouble())
                when {
                    diff <= 2.5 -> score += 45
                    diff <= 6.0 -> score += 25
                    diff <= 15.0 -> score += 5
                    else -> score -= 30
                }
            }
            score
        }
    }

    private fun toLyricsState(record: LrcLibRecord): LyricsState {
        val syncedRaw = record.syncedLyrics
        if (!syncedRaw.isNullOrBlank()) {
            val parsed = LrcParser.parseSyncedLyrics(syncedRaw)
            if (parsed.isNotEmpty()) {
                return LyricsState.Synced(parsed)
            }
        }
        val plainRaw = record.plainLyrics
        if (!plainRaw.isNullOrBlank()) {
            val parsedPlain = LrcParser.parsePlainLyrics(plainRaw)
            if (parsedPlain.isNotEmpty()) {
                return LyricsState.Plain(parsedPlain)
            }
        }
        if (record.instrumental) {
            return LyricsState.Instrumental
        }
        return LyricsState.NotFound
    }

    private fun httpGet(urlString: String): String? {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private const val MAX_CACHE_ENTRIES = 40
        private const val USER_AGENT = "RoomLauncher/2.4 (https://github.com/Cooreo/LauncherAPP)"

        fun demoSyncedLyrics(): LyricsState.Synced = LyricsState.Synced(
            lines = listOf(
                LyricsLine(0L, "Warm lamp glow on the wooden floor"),
                LyricsLine(3500L, "Rain taps softly at the windowpane"),
                LyricsLine(7200L, "Vinyl spinning in the midnight quiet"),
                LyricsLine(11000L, "Every melody slows down the night"),
                LyricsLine(15000L, "Tap any line to jump right to the beat"),
                LyricsLine(19000L, "Switch back to Visualizer anytime you like"),
                LyricsLine(23500L, "Grant Notification Access for Spotify & YouTube Music"),
                LyricsLine(28000L, "Live synced lyrics powered by open-source LRCLIB ♪")
            ),
            source = "Demo Preview"
        )
    }
}
