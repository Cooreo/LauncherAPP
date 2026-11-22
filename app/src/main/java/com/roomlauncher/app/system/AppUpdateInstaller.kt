package com.roomlauncher.app.system

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface ApkVerificationResult {
    data class Compatible(
        val versionName: String,
        val versionCode: Long,
        val certSha256: String
    ) : ApkVerificationResult

    data class SignatureMismatch(
        val installedDigest: String,
        val apkDigest: String
    ) : ApkVerificationResult

    data class DowngradeBlocked(
        val installedVersionCode: Long,
        val apkVersionCode: Long
    ) : ApkVerificationResult

    data class InvalidApk(val reason: String) : ApkVerificationResult
}

/**
 * Downloads release APKs safely into cache/updates/, verifies package & signing certificate
 * compatibility BEFORE invoking Android's PackageInstaller session API, and provides 1-tap
 * migration helpers (Save to Downloads + Uninstall) if a signing key mismatch is ever detected.
 */
object AppUpdateInstaller {

    private const val UPDATES_SUBDIR = "updates"
    private const val MAX_REDIRECTS = 6
    private const val SESSION_REQUEST_CODE = 2601

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

    fun getInstalledShortFingerprint(context: Context): String? {
        val pkgInfo = getInstalledPackageInfo(context) ?: return null
        val digest = extractPrimarySigningDigest(pkgInfo) ?: return null
        return formatShortFingerprint(digest)
    }

    suspend fun verifyApkForInstall(context: Context, apkFile: File): ApkVerificationResult =
        withContext(Dispatchers.IO) {
            if (!apkFile.exists() || apkFile.length() <= 0L) {
                return@withContext ApkVerificationResult.InvalidApk("Downloaded APK file is missing or empty.")
            }

            val apkInfo = getArchivePackageInfo(context, apkFile.absolutePath)
                ?: return@withContext ApkVerificationResult.InvalidApk("Could not parse downloaded APK package.")

            if (apkInfo.packageName != context.packageName) {
                return@withContext ApkVerificationResult.InvalidApk(
                    "Unexpected package ID (${apkInfo.packageName})."
                )
            }

            val installedInfo = getInstalledPackageInfo(context)
                ?: return@withContext ApkVerificationResult.InvalidApk("Could not inspect installed app.")

            val installedVersionCode = installedInfo.longVersionCodeCompat()
            val apkVersionCode = apkInfo.longVersionCodeCompat()
            if (apkVersionCode < installedVersionCode) {
                return@withContext ApkVerificationResult.DowngradeBlocked(
                    installedVersionCode = installedVersionCode,
                    apkVersionCode = apkVersionCode
                )
            }

            val installedDigests = extractSigningDigests(installedInfo)
            val apkDigests = extractSigningDigests(apkInfo)
            if (installedDigests.isNotEmpty() && apkDigests.isNotEmpty()) {
                val sharesKey = installedDigests.any { it in apkDigests }
                if (!sharesKey) {
                    return@withContext ApkVerificationResult.SignatureMismatch(
                        installedDigest = formatShortFingerprint(installedDigests.first()),
                        apkDigest = formatShortFingerprint(apkDigests.first())
                    )
                }
            }

            ApkVerificationResult.Compatible(
                versionName = apkInfo.versionName.orEmpty(),
                versionCode = apkVersionCode,
                certSha256 = apkDigests.firstOrNull()?.let(::formatShortFingerprint).orEmpty()
            )
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

    /**
     * Streams the verified APK into an Android [PackageInstaller.Session] so installation
     * reports structured status events to [UpdateInstallReceiver]. Falls back to [FileProvider]
     * ACTION_VIEW if session installation is unavailable on a vendor ROM.
     */
    suspend fun installApkViaSession(context: Context, apkFile: File): Boolean =
        withContext(Dispatchers.IO) {
            if (!apkFile.exists()) return@withContext false

            val sessionCommitted = runCatching {
                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL
                ).apply {
                    setAppPackageName(context.packageName)
                    setSize(apkFile.length())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }

                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)
                try {
                    session.openWrite("room_launcher_update.apk", 0L, apkFile.length()).use { out ->
                        FileInputStream(apkFile).use { input ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buf)
                                if (read <= 0) break
                                out.write(buf, 0, read)
                            }
                        }
                        session.fsync(out)
                    }

                    // Explicit receiver Intent is required with FLAG_MUTABLE on Android 14+ (targetSdk 35)
                    val callbackIntent = Intent(context, UpdateInstallReceiver::class.java).apply {
                        action = UpdateInstallReceiver.ACTION_INSTALL_STATUS
                    }
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PendingIntent.FLAG_MUTABLE
                        } else {
                            0
                        }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        SESSION_REQUEST_CODE,
                        callbackIntent,
                        flags
                    )
                    session.commit(pendingIntent.intentSender)
                    true
                } catch (t: Throwable) {
                    runCatching { session.abandon() }
                    throw t
                } finally {
                    runCatching { session.close() }
                }
            }.getOrDefault(false)

            if (sessionCommitted) {
                true
            } else {
                withContext(Dispatchers.Main) {
                    launchApkInstallerFallback(context, apkFile)
                }
            }
        }

    fun launchApkInstallerFallback(context: Context, apkFile: File): Boolean {
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

    /**
     * Copies the downloaded APK to Android's public Downloads folder so it remains available
     * even if the user uninstalls an incompatible legacy build before installing the new one.
     */
    suspend fun saveApkToPublicDownloads(
        context: Context,
        apkFile: File,
        versionName: String
    ): String? = withContext(Dispatchers.IO) {
        if (!apkFile.exists()) return@withContext null
        val safeVersion = versionName.replace(Regex("[^0-9A-Za-z._-]"), "_")
        val displayName = "RoomLauncher-v$safeVersion.apk"

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching null
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        FileInputStream(apkFile).use { input ->
                            input.copyTo(out, bufferSize = 32 * 1024)
                        }
                        out.flush()
                    } ?: return@runCatching null
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    "Downloads/$displayName"
                } catch (t: Throwable) {
                    runCatching { resolver.delete(itemUri, null, null) }
                    throw t
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val outFile = File(dir, displayName)
                apkFile.copyTo(outFile, overwrite = true)
                "Downloads/$displayName"
            }
        }.getOrNull()
    }

    fun launchUninstallCurrentApp(context: Context) {
        // Uses standard App Info screen instead of ACTION_DELETE so REQUEST_DELETE_PACKAGES is not needed
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Open Android Settings → Apps → Room Launcher to uninstall the old version",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getInstalledPackageInfo(context: Context): PackageInfo? {
        val pm = context.packageManager
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
        }.getOrNull()
    }

    private fun getArchivePackageInfo(context: Context, archivePath: String): PackageInfo? {
        val pm = context.packageManager
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    archivePath,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNATURES)
            }
        }.getOrNull()
    }

    private fun extractPrimarySigningDigest(info: PackageInfo): String? =
        extractSigningDigests(info).firstOrNull()

    private fun extractSigningDigests(info: PackageInfo): Set<String> {
        val rawSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo
            if (signingInfo != null) {
                val list = mutableListOf<ByteArray>()
                signingInfo.apkContentsSigners?.forEach { list += it.toByteArray() }
                signingInfo.signingCertificateHistory?.forEach { list += it.toByteArray() }
                list
            } else {
                emptyList()
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.map { it.toByteArray() }.orEmpty()
        }
        return rawSignatures.map(::sha256Hex).toSet()
    }

    internal fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    internal fun formatShortFingerprint(hex: String): String {
        val clean = hex.uppercase().take(16)
        return clean.chunked(2).joinToString(":")
    }

    private fun PackageInfo.longVersionCodeCompat(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
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
