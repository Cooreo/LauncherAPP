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
    val isSystemApp: Boolean = false,
    val firstInstallTime: Long = 0L,
    val icon: ImageBitmap? = null
) {
    val isGame: Boolean
        get() = isGameFlag || category == ApplicationInfo.CATEGORY_GAME

    val indexLetter: String
        get() {
            val firstChar = label.firstOrNull()?.uppercaseChar() ?: '#'
            return if (firstChar in 'A'..'Z') firstChar.toString() else "#"
        }
}
