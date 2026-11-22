package com.roomlauncher.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

class PackageChangeReceiver(
    private val onPackagesChanged: (action: String, packageName: String?, isReplacing: Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action == Intent.ACTION_PACKAGE_ADDED ||
            action == Intent.ACTION_PACKAGE_REMOVED ||
            action == Intent.ACTION_PACKAGE_REPLACED ||
            action == Intent.ACTION_PACKAGE_CHANGED
        ) {
            val packageName = intent.data?.schemeSpecificPart
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            onPackagesChanged(action, packageName, isReplacing)
        }
    }

    companion object {
        fun createIntentFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }

        fun register(context: Context, receiver: PackageChangeReceiver) {
            ContextCompat.registerReceiver(
                context,
                receiver,
                createIntentFilter(),
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }
}
