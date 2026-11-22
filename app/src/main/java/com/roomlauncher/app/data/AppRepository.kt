package com.roomlauncher.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Boot-warmed, event-driven app catalog.
 *
 * Loads all launchable apps and pre-renders their rounded icons ONCE on launcher boot,
 * then updates the cached list incrementally ONLY when Android broadcasts a package
 * install, uninstall, replace, or state change—keeping All Apps instant with zero reloads.
 */
class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isCacheReady = MutableStateFlow(false)
    val isCacheReady: StateFlow<Boolean> = _isCacheReady.asStateFlow()

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val updateMutex = Mutex()
    private var receiver: PackageChangeReceiver? = null
    private var isRegistered = false
    private var hasBootLoaded = false

    fun startListening() {
        if (!isRegistered) {
            val newReceiver = PackageChangeReceiver { action, packageName, isReplacing ->
                scope.launch {
                    handlePackageEvent(action, packageName, isReplacing)
                }
            }
            receiver = newReceiver
            PackageChangeReceiver.register(context, newReceiver)
            isRegistered = true
        }
        if (!hasBootLoaded) {
            hasBootLoaded = true
            scope.launch {
                refreshInstalledApps()
            }
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

    private suspend fun handlePackageEvent(
        action: String,
        packageName: String?,
        isReplacing: Boolean
    ) {
        if (packageName.isNullOrBlank() || packageName == context.packageName) return

        // If full boot cache hasn't completed yet, fall back to a clean full scan
        if (!_isCacheReady.value) {
            refreshInstalledApps()
            return
        }

        when (action) {
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (isReplacing) {
                    // App is being updated in-place; wait for PACKAGE_ADDED/REPLACED to refresh icon/label
                    return
                }
                // Instant O(N) in-memory removal — ZERO PackageManager queries!
                updateMutex.withLock {
                    iconCache.remove(packageName)
                    val current = _installedApps.value
                    val filtered = current.filterNot { it.packageName == packageName }
                    if (filtered.size != current.size) {
                        _installedApps.value = filtered
                    }
                }
            }

            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_CHANGED -> {
                patchSinglePackage(packageName)
            }
        }
    }

    /**
     * Queries ONLY [packageName] from PackageManager and inserts, updates, or removes it
     * in the sorted in-memory app list without scanning every other installed app.
     */
    private suspend fun patchSinglePackage(packageName: String) = withContext(Dispatchers.IO) {
        updateMutex.withLock {
            val pm = context.packageManager
            iconCache.remove(packageName)

            val queryIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            val resolved = queryLauncherActivities(pm, queryIntent)
            val firstMatch = resolved.firstOrNull { it.activityInfo?.packageName == packageName }

            val current = _installedApps.value.filterNot { it.packageName == packageName }
            if (firstMatch == null) {
                if (current.size != _installedApps.value.size) {
                    _installedApps.value = current
                }
                return@withLock
            }

            val updatedApp = buildAppInfo(pm, firstMatch)
            if (updatedApp != null) {
                val merged = ArrayList<AppInfo>(current.size + 1)
                merged.addAll(current)
                merged.add(updatedApp)
                merged.sortBy { it.label.lowercase() }
                _installedApps.value = merged
            }
        }
    }

    suspend fun refreshInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        updateMutex.withLock {
            val pm = context.packageManager
            val selfPackage = context.packageName

            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedActivities = queryLauncherActivities(pm, mainIntent)

            val seenPackages = HashSet<String>(resolvedActivities.size)
            val apps = ArrayList<AppInfo>(resolvedActivities.size)

            for (resolveInfo in resolvedActivities) {
                val pkg = resolveInfo.activityInfo?.packageName ?: continue
                if (pkg == selfPackage || !seenPackages.add(pkg)) continue
                buildAppInfo(pm, resolveInfo)?.let { apps.add(it) }
            }

            iconCache.keys.retainAll(seenPackages)

            val sorted = apps.sortedBy { it.label.lowercase() }
            _installedApps.value = sorted
            _isCacheReady.value = true
            sorted
        }
    }

    private fun queryLauncherActivities(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
    }

    private fun buildAppInfo(pm: PackageManager, resolveInfo: ResolveInfo): AppInfo? {
        val activityInfo = resolveInfo.activityInfo ?: return null
        val pkg = activityInfo.packageName ?: return null

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

        // Pure ROM system apps cannot be uninstalled (only disabled/updated via App Info)
        val isSystemApp = runCatching {
            appInfo != null &&
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        }.getOrDefault(false)

        val firstInstallTime = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L)).firstInstallTime
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0).firstInstallTime
            }
        }.getOrDefault(0L)

        val cachedIcon = iconCache[pkg] ?: runCatching {
            val drawable = resolveInfo.loadIcon(pm)
            drawableToImageBitmap(drawable, ICON_SIZE_PX).also { bitmap ->
                iconCache[pkg] = bitmap
            }
        }.getOrNull()

        return AppInfo(
            packageName = pkg,
            label = label,
            category = category,
            isGameFlag = isGameFlag,
            isSystemApp = isSystemApp,
            firstInstallTime = firstInstallTime,
            icon = cachedIcon
        )
    }

    private fun drawableToImageBitmap(drawable: Drawable, sizePx: Int): ImageBitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cornerRadius = sizePx * 0.24f
        val clipPath = android.graphics.Path().apply {
            addRoundRect(
                0f,
                0f,
                sizePx.toFloat(),
                sizePx.toFloat(),
                cornerRadius,
                cornerRadius,
                android.graphics.Path.Direction.CW
            )
        }
        canvas.clipPath(clipPath)

        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            val paint = android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
            )
            val dst = android.graphics.Rect(0, 0, sizePx, sizePx)
            canvas.drawBitmap(src, null, dst, paint)
        } else {
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
        }
        return output.asImageBitmap()
    }

    companion object {
        // 96px stays ultra-lightweight in memory while looking razor-sharp in the 4-column grid
        private const val ICON_SIZE_PX = 96
    }
}
