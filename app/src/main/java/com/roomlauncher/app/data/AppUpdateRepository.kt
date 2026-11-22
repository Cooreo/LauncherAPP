package com.roomlauncher.app.data

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class AppUpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val releaseNotes: String = "",
    val sizeBytes: Long = 0L,
    val sourceLabel: String = "GitHub",
    val isPreRelease: Boolean = false
)

@Immutable
data class AppUpdateUiState(
    val currentVersion: String = "",
    val installedCertFingerprint: String? = null,
    val isChecking: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val isUpToDate: Boolean = false,
    val lastCheckedAtMs: Long = 0L,
    val errorMessage: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isInstalling: Boolean = false,
    val downloadedApkPath: String? = null,
    val signatureVerified: Boolean = false,
    val signatureConflict: Boolean = false,
    val savedToDownloadsPath: String? = null,
    val needsInstallPermission: Boolean = false,
    val showUpdateDialog: Boolean = false
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val latestVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

@Serializable
private data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

@Serializable
private data class GitHubReleaseAsset(
    val name: String = "",
    val size: Long = 0L,
    @SerialName("browser_download_url") val browserDownloadUrl: String = ""
)

@Serializable
private data class GitHubContentItem(
    val name: String = "",
    val type: String = "",
    val size: Long = 0L,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)

object AppVersionComparator {
    private val APK_VERSION_REGEX =
        Regex("""RoomLauncher-v(\d+(?:\.\d+)*(?:-?[a-z]+\d*)?)\.apk""", RegexOption.IGNORE_CASE)
    private val TAG_VERSION_REGEX =
        Regex("""(\d+(?:\.\d+)*(?:-?[a-z]+\d*)?)""", RegexOption.IGNORE_CASE)
    private val SPLIT_VERSION_REGEX =
        Regex("""^(\d+(?:\.\d+)*)(?:-?([a-z]+)(\d*))?$""", RegexOption.IGNORE_CASE)

    fun normalizeVersion(raw: String): String {
        val trimmed = raw.trim().removePrefix("v").removePrefix("V")
        return TAG_VERSION_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.lowercase() ?: trimmed.lowercase()
    }

    fun extractVersionFromApkName(fileName: String): String? {
        return APK_VERSION_REGEX.find(fileName.trim())?.groupValues?.getOrNull(1)?.lowercase()
    }

    fun isPreReleaseVersion(version: String): Boolean {
        val norm = normalizeVersion(version)
        val suffix = SPLIT_VERSION_REGEX.matchEntire(norm)?.groupValues?.getOrNull(2).orEmpty()
        return suffix.isNotEmpty()
    }

    fun compareVersions(a: String, b: String): Int {
        val normA = normalizeVersion(a)
        val normB = normalizeVersion(b)
        val matchA = SPLIT_VERSION_REGEX.matchEntire(normA)
        val matchB = SPLIT_VERSION_REGEX.matchEntire(normB)

        val partsA = (matchA?.groupValues?.getOrNull(1) ?: normA).split('.').map { it.toIntOrNull() ?: 0 }
        val partsB = (matchB?.groupValues?.getOrNull(1) ?: normB).split('.').map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }

        // Numeric components are equal: stable release (no suffix) > prerelease suffix (e.g. 3.2 > 3.2b > 3.2a)
        val suffixA = matchA?.groupValues?.getOrNull(2).orEmpty()
        val suffixB = matchB?.groupValues?.getOrNull(2).orEmpty()
        if (suffixA.isEmpty() && suffixB.isEmpty()) return 0
        if (suffixA.isEmpty()) return 1
        if (suffixB.isEmpty()) return -1
        val letterCmp = suffixA.compareTo(suffixB)
        if (letterCmp != 0) return letterCmp
        val preNumA = matchA?.groupValues?.getOrNull(3)?.toIntOrNull() ?: 1
        val preNumB = matchB?.groupValues?.getOrNull(3)?.toIntOrNull() ?: 1
        return preNumA.compareTo(preNumB)
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        return compareVersions(remote, current) > 0
    }
}

/**
 * Checks Cooreo/LauncherAPP on GitHub for newer Room Launcher releases.
 * Supports both official GitHub Releases (/releases) AND compiled APKs committed in
 * the repository's /releases/ directory on main, always picking the newest eligible version.
 * Alpha / pre-release builds (e.g. v3.2a, v3.3a) are skipped unless [includePreReleases] is true.
 */
class AppUpdateRepository(
    private val repoOwner: String = DEFAULT_REPO_OWNER,
    private val repoName: String = DEFAULT_REPO_NAME
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(
        currentVersion: String,
        includePreReleases: Boolean = false
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        var lastError: String? = null

        val releaseCandidate = runCatching { fetchFromGitHubReleases(includePreReleases) }
            .onFailure { lastError = it.message }
            .getOrNull()

        val folderCandidate = runCatching { fetchFromReleasesFolder(includePreReleases) }
            .onFailure { if (lastError == null) lastError = it.message }
            .getOrNull()

        val best = pickNewerCandidate(releaseCandidate, folderCandidate)
            ?: return@withContext UpdateCheckResult.Error(
                lastError?.take(100) ?: "Could not reach GitHub releases"
            )

        if (AppVersionComparator.isNewerVersion(best.versionName, currentVersion)) {
            UpdateCheckResult.UpdateAvailable(best)
        } else {
            UpdateCheckResult.UpToDate(best.versionName)
        }
    }

    private fun pickNewerCandidate(
        releaseCandidate: AppUpdateInfo?,
        folderCandidate: AppUpdateInfo?
    ): AppUpdateInfo? {
        if (releaseCandidate == null) return folderCandidate
        if (folderCandidate == null) return releaseCandidate
        val cmp = AppVersionComparator.compareVersions(
            releaseCandidate.versionName,
            folderCandidate.versionName
        )
        // Prefer GitHub Release on tie because it usually carries rich release notes
        return if (cmp >= 0) releaseCandidate else folderCandidate
    }

    private fun fetchFromGitHubReleases(includePreReleases: Boolean): AppUpdateInfo? {
        val releases: List<GitHubReleaseResponse> = if (includePreReleases) {
            val url = "https://api.github.com/repos/$repoOwner/$repoName/releases?per_page=10"
            val body = httpGet(url) ?: return null
            json.decodeFromString<List<GitHubReleaseResponse>>(body)
        } else {
            val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val body = httpGet(url) ?: return null
            listOf(json.decodeFromString<GitHubReleaseResponse>(body))
        }

        val candidates = releases.mapNotNull { release ->
            if (release.draft) return@mapNotNull null
            val apkAsset = release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank()
            } ?: return@mapNotNull null

            val version = AppVersionComparator.normalizeVersion(
                release.tagName.ifBlank {
                    AppVersionComparator.extractVersionFromApkName(apkAsset.name).orEmpty()
                }
            )
            if (version.isBlank()) return@mapNotNull null

            val isPre = release.prerelease || AppVersionComparator.isPreReleaseVersion(version)
            if (isPre && !includePreReleases) return@mapNotNull null

            AppUpdateInfo(
                versionName = version,
                downloadUrl = apkAsset.browserDownloadUrl,
                releasePageUrl = release.htmlUrl.ifBlank { "https://github.com/$repoOwner/$repoName/releases" },
                releaseNotes = release.body.orEmpty().trim(),
                sizeBytes = apkAsset.size,
                sourceLabel = if (isPre) "GitHub Alpha Release" else "GitHub Release",
                isPreRelease = isPre
            )
        }

        return candidates.maxWithOrNull { a, b ->
            AppVersionComparator.compareVersions(a.versionName, b.versionName)
        }
    }

    private fun fetchFromReleasesFolder(includePreReleases: Boolean): AppUpdateInfo? {
        val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/releases?ref=main"
        val body = httpGet(url) ?: return null
        val items = json.decodeFromString<List<GitHubContentItem>>(body)

        val candidates = items.mapNotNull { item ->
            if (!item.type.equals("file", ignoreCase = true)) return@mapNotNull null
            val version = AppVersionComparator.extractVersionFromApkName(item.name) ?: return@mapNotNull null
            val isPre = AppVersionComparator.isPreReleaseVersion(version)
            if (isPre && !includePreReleases) return@mapNotNull null

            val downloadUrl = item.downloadUrl?.takeIf { it.isNotBlank() }
                ?: "https://raw.githubusercontent.com/$repoOwner/$repoName/main/releases/${item.name}"
            AppUpdateInfo(
                versionName = version,
                downloadUrl = downloadUrl,
                releasePageUrl = item.htmlUrl?.takeIf { it.isNotBlank() }
                    ?: "https://github.com/$repoOwner/$repoName/tree/main/releases",
                releaseNotes = if (isPre) {
                    "Alpha / pre-release test build from $repoOwner/$repoName (${item.name})."
                } else {
                    "Compiled release APK from $repoOwner/$repoName (${item.name})."
                },
                sizeBytes = item.size,
                sourceLabel = if (isPre) "GitHub Repo (Alpha)" else "GitHub Repo",
                isPreRelease = isPre
            )
        }

        return candidates.maxWithOrNull { a, b ->
            AppVersionComparator.compareVersions(a.versionName, b.versionName)
        }
    }

    private fun httpGet(urlString: String): String? {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "RoomLauncher-Updater")
        }
        return try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                403 -> throw IllegalStateException("GitHub API rate limit reached — try again in a few minutes")
                else -> throw IllegalStateException("GitHub returned HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_REPO_OWNER = "Cooreo"
        const val DEFAULT_REPO_NAME = "LauncherAPP"
        const val RELEASES_WEB_URL = "https://github.com/Cooreo/LauncherAPP/tree/main/releases"
    }
}
