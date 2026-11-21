package com.roomlauncher.app.data

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class AppInfo(
    val packageName: String,
    val label: String,
    val category: Int = ApplicationInfo.CATEGORY_UNDEFINED,
    val isGameFlag: Boolean = false,
    val icon: ImageBitmap? = null
)
