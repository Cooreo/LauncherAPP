package com.roomlauncher.app.system

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SystemIntentHelper {

    private val KNOWN_GALLERY_PACKAGES = listOf(
        "com.google.android.apps.photos",
        "com.sec.android.gallery3d",
        "com.miui.gallery",
        "com.oneplus.gallery",
        "com.coloros.gallery3d",
        "org.fossify.gallery",
        "com.simplemobiletools.gallery.pro",
        "com.simplemobiletools.gallery",
        "com.android.gallery3d",
        "com.motorola.MotGallery2",
        "com.vivo.gallery",
        "com.sonyericsson.album",
        "com.asus.gallery",
        "com.huawei.photos"
    )

    private val KNOWN_WEATHER_PACKAGES = listOf(
        "com.google.android.apps.weather",
        "com.sec.android.daemonapp",
        "com.samsung.android.weather",
        "com.miui.weather2",
        "net.oneplus.weather",
        "com.coloros.weather2",
        "com.oplus.weather",
        "org.breezyweather",
        "com.accuweather.android",
        "com.weather.Weather",
        "com.twc.android",
        "com.weawow",
        "com.yahoo.mobile.client.android.weather",
        "com.handmark.expressweather",
        "com.overdrop.app",
        "com.windyty.android",
        "com.motorola.timeweatherwidget"
    )

    fun launchApp(context: Context, packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(context, "Unable to launch app", Toast.LENGTH_SHORT).show()
            return false
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to launch app", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Launches the installed Gallery / Photos app directly without triggering the
     * ambiguous "Open with / Choose default photos app" system picker.
     */
    fun openGallery(context: Context) {
        // 1. Official Android selector for launching the primary Gallery app directly
        val selectorIntent = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN,
            Intent.CATEGORY_APP_GALLERY
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (startActivitySafely(context, selectorIntent)) return

        // 2. Direct launch of well-known installed gallery packages
        for (pkg in KNOWN_GALLERY_PACKAGES) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (startActivitySafely(context, intent)) return
            }
        }

        // 3. Last-resort MediaStore images URI
        val mediaStoreIntent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (startActivitySafely(context, mediaStoreIntent)) return

        Toast.makeText(context, "No gallery app found on this device", Toast.LENGTH_SHORT).show()
    }

    fun openWeather(context: Context, installedPackages: List<Pair<String, String>> = emptyList()) {
        // 1. Try known weather app packages directly
        for (pkg in KNOWN_WEATHER_PACKAGES) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (startActivitySafely(context, intent)) return
            }
        }

        // 2. Try any installed app whose label or package includes "weather"
        val discovered = installedPackages.firstOrNull { (pkg, label) ->
            pkg.lowercase().contains("weather") || label.lowercase().contains("weather")
        }
        if (discovered != null && launchApp(context, discovered.first)) {
            return
        }

        // 3. Fallback: open weather forecast in browser
        openWebUrl(context, "https://www.google.com/search?q=weather")
    }

    fun openWebUrl(context: Context, rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return
        val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!startActivitySafely(context, intent)) {
            Toast.makeText(context, "No browser found to open $normalized", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAlarms(context: Context) {
        val showAlarmsIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val setAlarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val launched = startFirstAvailable(context, listOf(showAlarmsIntent, setAlarmIntent))
        if (!launched) {
            Toast.makeText(context, "No clock or alarm app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!startFirstAvailable(context, listOf(directIntent, fallbackIntent))) {
            Toast.makeText(
                context,
                "Open Battery Settings manually to disable optimization",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openAppInfoSettings(context: Context, packageName: String = context.packageName) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!startActivitySafely(context, intent)) {
            Toast.makeText(context, "Unable to open App Info settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun isDefaultHomeLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun createDefaultHomeRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return null
    }

    fun openLegacyHomeChooser(context: Context) {
        val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!startFirstAvailable(context, listOf(homeSettings, homeIntent))) {
            Toast.makeText(
                context,
                "Open Default Apps in Settings to select Room Launcher",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    suspend fun saveFramePhoto(context: Context, sourceUri: Uri): String? =
        saveUserImage(context, sourceUri, "frame_photo.jpg")

    suspend fun saveUserImage(context: Context, sourceUri: Uri, fileName: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
                val decoded = rawStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                val maxDim = 720
                val scale = minOf(1f, maxDim.toFloat() / maxOf(decoded.width, decoded.height).toFloat())
                val finalBmp = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * scale).toInt().coerceAtLeast(1),
                        (decoded.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    decoded
                }
                val outFile = File(context.filesDir, fileName)
                FileOutputStream(outFile).use { fos ->
                    finalBmp.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                }
                if (finalBmp !== decoded) {
                    decoded.recycle()
                }
                finalBmp.recycle()
                outFile.absolutePath
            }.getOrNull()
        }

    private fun startFirstAvailable(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            if (startActivitySafely(context, intent)) return true
        }
        return false
    }

    private fun startActivitySafely(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
