package com.roomlauncher.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface InstallSessionEvent {
    data object PendingUserAction : InstallSessionEvent
    data object Success : InstallSessionEvent
    data class Conflict(val message: String) : InstallSessionEvent
    data class Failed(val message: String) : InstallSessionEvent
}

/**
 * Receives structured status callbacks from Android's [PackageInstaller.Session] so Room Launcher
 * can open the system update confirmation dialog cleanly and detect signature/package conflicts
 * instead of silently failing.
 */
class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val rawMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(confirmIntent) }
                }
                _events.tryEmit(InstallSessionEvent.PendingUserAction)
            }

            PackageInstaller.STATUS_SUCCESS -> {
                _events.tryEmit(InstallSessionEvent.Success)
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                _events.tryEmit(
                    InstallSessionEvent.Conflict(
                        rawMessage.ifBlank {
                            "Signing certificate conflicts with the currently installed package."
                        }
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                _events.tryEmit(InstallSessionEvent.Failed("Installation was cancelled."))
            }

            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                _events.tryEmit(InstallSessionEvent.Failed("Not enough storage space to install update."))
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                _events.tryEmit(InstallSessionEvent.Failed("Update APK is incompatible with this device."))
            }

            PackageInstaller.STATUS_FAILURE_INVALID -> {
                _events.tryEmit(InstallSessionEvent.Failed("Downloaded APK is invalid or corrupted. Try again."))
            }

            else -> {
                val lower = rawMessage.lowercase()
                if ("conflict" in lower || "signature" in lower || "update_incompatible" in lower) {
                    _events.tryEmit(
                        InstallSessionEvent.Conflict(
                            rawMessage.ifBlank { "Package signature conflicts with installed version." }
                        )
                    )
                } else {
                    _events.tryEmit(
                        InstallSessionEvent.Failed(
                            rawMessage.ifBlank { "Installation failed (code $status)." }
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.roomlauncher.app.action.UPDATE_INSTALL_STATUS"

        private val _events = MutableSharedFlow<InstallSessionEvent>(extraBufferCapacity = 8)
        val events: SharedFlow<InstallSessionEvent> = _events.asSharedFlow()
    }
}
