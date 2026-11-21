package com.roomlauncher.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private var receiver: PackageChangeReceiver? = null
    private var isRegistered = false

    fun startListening() {
        if (!isRegistered) {
            val newReceiver = PackageChangeReceiver { action, packageName ->
                if (packageName != null && (action == Intent.ACTION_PACKAGE_REMOVED || action == Intent.ACTION_PACKAGE_REPLACED)) {
                    iconCache.remove(packageName)
                }
                scope.launch {
                    refreshInstalledApps()
                }
            }
            receiver = newReceiver
            PackageChangeReceiver.register(context, newReceiver)
            isRegistered = true
        }
        scope.launch {
            refreshInstalledApps()
        }
    }

    fun stopListening() {
        if (isRegistered) {
            receiver?.let {
                runCatching { context.unregisterReceiver(it) }
            }
            receiver = null
            isRegistered = false
        }
    }

    suspend fun refreshInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackage = context.packageName

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        }

        val seenPackages = HashSet<String>()
        val apps = ArrayList<AppInfo>(resolvedActivities.size)

        for (resolveInfo in resolvedActivities) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val pkg = activityInfo.packageName ?: continue
            if (pkg == selfPackage || !seenPackages.add(pkg)) continue

            val label = runCatching {
                resolveInfo.loadLabel(pm)?.toString()?.trim()
            }.getOrNull().orEmpty().ifEmpty { pkg }

            val appInfo = activityInfo.applicationInfo
            val category = runCatching {
                appInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED
            }.getOrDefault(ApplicationInfo.CATEGORY_UNDEFINED)

            @Suppress("DEPRECATION")
            val isGameFlag = runCatching {
                appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            }.getOrDefault(false)

            val cachedIcon = iconCache[pkg] ?: runCatching {
                val drawable = resolveInfo.loadIcon(pm)
                drawableToImageBitmap(drawable, ICON_SIZE_PX).also { bitmap ->
                    iconCache[pkg] = bitmap
                }
            }.getOrNull()

            apps.add(
                AppInfo(
                    packageName = pkg,
                    label = label,
                    category = category,
                    isGameFlag = isGameFlag,
                    icon = cachedIcon
                )
            )
        }

        iconCache.keys.retainAll(seenPackages)

        val sorted = apps.sortedBy { it.label.lowercase() }
        _installedApps.value = sorted
        sorted
    }

    private fun drawableToImageBitmap(drawable: Drawable, sizePx: Int): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= sizePx && src.height <= sizePx) {
                return src.asImageBitmap()
            }
            val scaled = Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
            return scaled.asImageBitmap()
        }

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    companion object {
        private const val ICON_SIZE_PX = 96
    }
}
