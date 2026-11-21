package com.roomlauncher.app.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads release APKs safely into cache/updates/ and hands them off to Android's native
 * Package Installer via [FileProvider].
 */
object AppUpdateInstaller {

    private const val UPDATES_SUBDIR = "updates"
    private const val MAX_REDIRECTS = 6

    fun canInstallPackages(context: Context): Boolean {
        return runCatching { context.packageManager.canRequestPackageInstalls() }
            .getOrDefault(false)
    }

    fun openInstallUnknownAppsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Enable 'Install unknown apps' for Room Launcher in Android Settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        versionName: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, UPDATES_SUBDIR).apply {
            if (!exists()) mkdirs()
        }

        // Clean up any stale APK downloads so cache stays small
        updatesDir.listFiles()?.forEach { old ->
            if (old.isFile) runCatching { old.delete() }
        }

        val safeVersion = versionName.replace(Regex("[^0-9A-Za-z._-]"), "_")
        val partFile = File(updatesDir, "RoomLauncher-v$safeVersion.apk.part")
        val finalFile = File(updatesDir, "RoomLauncher-v$safeVersion.apk")

        val connection = openFollowingRedirects(downloadUrl)
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Download failed (HTTP $code)")
            }

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L
            var lastReportedPercent = -1

            connection.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0L) {
                            val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            if (pct != lastReportedPercent) {
                                lastReportedPercent = pct
                                onProgress(pct / 100f)
                            }
                        } else if (lastReportedPercent < 0) {
                            lastReportedPercent = 0
                            onProgress(-1f)
                        }
                    }
                    output.flush()
                }
            }

            if (downloadedBytes <= 0L || !partFile.exists()) {
                throw IllegalStateException("Downloaded APK file was empty")
            }

            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
            onProgress(1f)
            finalFile
        } catch (t: Throwable) {
            runCatching { partFile.delete() }
            throw t
        } finally {
            connection.disconnect()
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open Android package installer", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun openFollowingRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "RoomLauncher-Updater")
            }
            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 ||
                status == 308
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect missing Location header")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
            } else {
                return conn
            }
        }
        throw IllegalStateException("Too many redirects while downloading APK")
    }
}
