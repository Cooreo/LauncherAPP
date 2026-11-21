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
    val sourceLabel: String = "GitHub"
)

@Immutable
data class AppUpdateUiState(
    val currentVersion: String = "",
    val isChecking: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val isUpToDate: Boolean = false,
    val lastCheckedAtMs: Long = 0L,
    val errorMessage: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedApkPath: String? = null,
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
    private val APK_VERSION_REGEX = Regex("""RoomLauncher-v(\d+(?:\.\d+)*)\.apk""", RegexOption.IGNORE_CASE)
    private val TAG_VERSION_REGEX = Regex("""(\d+(?:\.\d+)*)""")

    fun normalizeVersion(raw: String): String {
        val trimmed = raw.trim().removePrefix("v").removePrefix("V")
        return TAG_VERSION_REGEX.find(trimmed)?.groupValues?.getOrNull(1) ?: trimmed
    }

    fun extractVersionFromApkName(fileName: String): String? {
        return APK_VERSION_REGEX.find(fileName.trim())?.groupValues?.getOrNull(1)
    }

    fun compareVersions(a: String, b: String): Int {
        val partsA = normalizeVersion(a).split('.').map { it.toIntOrNull() ?: 0 }
        val partsB = normalizeVersion(b).split('.').map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        return compareVersions(remote, current) > 0
    }
}

/**
 * Checks Cooreo/LauncherAPP on GitHub for newer Room Launcher releases.
 * Supports both official GitHub Releases (/releases/latest) AND compiled APKs committed in
 * the repository's /releases/ directory on main, always picking the newest version.
 */
class AppUpdateRepository(
    private val repoOwner: String = DEFAULT_REPO_OWNER,
    private val repoName: String = DEFAULT_REPO_NAME
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        var lastError: String? = null

        val releaseCandidate = runCatching { fetchFromGitHubReleases() }
            .onFailure { lastError = it.message }
            .getOrNull()

        val folderCandidate = runCatching { fetchFromReleasesFolder() }
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

    private fun fetchFromGitHubReleases(): AppUpdateInfo? {
        val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        val body = httpGet(url) ?: return null
        val release = json.decodeFromString<GitHubReleaseResponse>(body)
        if (release.draft) return null

        val apkAsset = release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank()
        } ?: return null

        val version = AppVersionComparator.normalizeVersion(
            release.tagName.ifBlank {
                AppVersionComparator.extractVersionFromApkName(apkAsset.name).orEmpty()
            }
        )
        if (version.isBlank()) return null

        return AppUpdateInfo(
            versionName = version,
            downloadUrl = apkAsset.browserDownloadUrl,
            releasePageUrl = release.htmlUrl.ifBlank { "https://github.com/$repoOwner/$repoName/releases" },
            releaseNotes = release.body.orEmpty().trim(),
            sizeBytes = apkAsset.size,
            sourceLabel = "GitHub Release"
        )
    }

    private fun fetchFromReleasesFolder(): AppUpdateInfo? {
        val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/releases?ref=main"
        val body = httpGet(url) ?: return null
        val items = json.decodeFromString<List<GitHubContentItem>>(body)

        val candidates = items.mapNotNull { item ->
            if (!item.type.equals("file", ignoreCase = true)) return@mapNotNull null
            val version = AppVersionComparator.extractVersionFromApkName(item.name) ?: return@mapNotNull null
            val downloadUrl = item.downloadUrl?.takeIf { it.isNotBlank() }
                ?: "https://raw.githubusercontent.com/$repoOwner/$repoName/main/releases/${item.name}"
            AppUpdateInfo(
                versionName = version,
                downloadUrl = downloadUrl,
                releasePageUrl = item.htmlUrl?.takeIf { it.isNotBlank() }
                    ?: "https://github.com/$repoOwner/$repoName/tree/main/releases",
                releaseNotes = "Compiled release APK from $repoOwner/$repoName (${item.name}).",
                sizeBytes = item.size,
                sourceLabel = "GitHub Repo"
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
