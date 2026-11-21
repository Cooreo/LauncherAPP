package com.roomlauncher.app.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.widget.Toast

/**
 * Lightweight NotificationListenerService used solely so Android's MediaSessionManager
 * permits Room Launcher to read active music playback metadata, progress, and transport controls
 * (Spotify, YouTube Music, Apple Music, SoundCloud, local players, etc.).
 */
class RoomNotificationListenerService : NotificationListenerService() {

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, RoomNotificationListenerService::class.java)

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val component = componentName(context)
            val fullName = component.flattenToString()
            val shortName = component.flattenToShortString()
            return enabled.split(':').any { entry ->
                entry.equals(fullName, ignoreCase = true) ||
                    entry.equals(shortName, ignoreCase = true) ||
                    ComponentName.unflattenFromString(entry) == component
            }
        }

        fun openNotificationListenerSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Open Notification Access in Android Settings to enable Music Visualizer",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
