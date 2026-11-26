package com.roomlauncher.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live telephony call phase, used by the Bedroom Night Table phone so the little illustrated
 * phone actually lights up, shakes and shows an answer/decline screen when a call comes in.
 */
enum class CallPhase {
    IDLE,
    RINGING,
    ONGOING
}

/**
 * Watches the system call state through [TelephonyManager].
 *
 * [Manifest.permission.READ_PHONE_STATE] is a runtime permission: until it is granted the
 * monitor stays [CallPhase.IDLE] and simply does nothing (no crashes, no battery cost).
 * Uses [TelephonyCallback] on Android 12+ and the legacy [PhoneStateListener] below that.
 */
class CallMonitor(private val context: Context) {

    private val _phase = MutableStateFlow(CallPhase.IDLE)
    val phase: StateFlow<CallPhase> = _phase.asStateFlow()

    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    private fun telephonyManager(): TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    /**
     * (Re)registers the call-state listener. Safe to call repeatedly — for example right after
     * the user grants the phone-state permission from Settings.
     */
    @Suppress("DEPRECATION")
    fun startOrRefresh() {
        unregister()
        if (!hasPermission()) {
            _phase.value = CallPhase.IDLE
            return
        }
        val manager = telephonyManager() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        publish(state)
                    }
                }
                manager.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback)
                telephonyCallback = callback
            } else {
                val listener = object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        publish(state)
                    }
                }
                manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                phoneStateListener = listener
            }
            // Seed with the current state so a call that starts before we register still shows
            publish(manager.callState)
        } catch (_: Throwable) {
            unregister()
            _phase.value = CallPhase.IDLE
        }
    }

    private fun publish(state: Int) {
        val next = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> CallPhase.RINGING
            TelephonyManager.CALL_STATE_OFFHOOK -> CallPhase.ONGOING
            else -> CallPhase.IDLE
        }
        if (_phase.value != next) {
            _phase.value = next
        }
    }

    @Suppress("DEPRECATION")
    private fun unregister() {
        val manager = telephonyManager()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { manager?.unregisterTelephonyCallback(it) }
            } else {
                phoneStateListener?.let { manager?.listen(it, PhoneStateListener.LISTEN_NONE) }
            }
        } catch (_: Throwable) {
            // Telephony manager can be unavailable on some tablets / emulators — ignore.
        }
        telephonyCallback = null
        phoneStateListener = null
    }

    /** Fully stops watching calls (called from [RoomLauncherViewModel.onCleared]). */
    fun stop() {
        unregister()
        _phase.value = CallPhase.IDLE
    }
}
