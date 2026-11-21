package com.roomlauncher.app.data

import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val packageName: String,
    val label: String,
    val category: Int = ApplicationInfo.CATEGORY_UNDEFINED,
    val icon: ImageBitmap? = null
)
