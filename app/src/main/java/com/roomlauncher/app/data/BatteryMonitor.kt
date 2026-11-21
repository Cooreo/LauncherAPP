package com.roomlauncher.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ChargeSource(val label: String) {
    NONE("Battery"),
    AC("Fast Charging"),
    USB("USB Charging"),
    WIRELESS("Wireless Charging")
}

@Immutable
data class BatteryStatus(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val chargeSource: ChargeSource = ChargeSource.NONE,
    val plugInEventId: Long = 0L
)

/**
 * Observes live Android battery level & power-connected events while the screen is on.
 * Zero extra permissions required.
 */
class BatteryMonitor(private val context: Context) {

    private val _status = MutableStateFlow(readInitialBatteryStatus())
    val status: StateFlow<BatteryStatus> = _status.asStateFlow()

    private var registered = false
    private var lastChargingState: Boolean = _status.value.isCharging

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    val fresh = readInitialBatteryStatus()
                    lastChargingState = true
                    _status.update { current ->
                        fresh.copy(
                            isCharging = true,
                            plugInEventId = current.plugInEventId + 1L
                        )
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    lastChargingState = false
                    _status.update { current ->
                        current.copy(
                            isCharging = false,
                            chargeSource = ChargeSource.NONE
                        )
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val parsed = parseBatteryIntent(intent)
                    val justPluggedIn = !lastChargingState && parsed.isCharging
                    lastChargingState = parsed.isCharging
                    _status.update { current ->
                        parsed.copy(
                            plugInEventId = if (justPluggedIn) current.plugInEventId + 1L else current.plugInEventId
                        )
                    }
                }
            }
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        runCatching {
            context.registerReceiver(receiver, filter)
            registered = true
        }
        val initial = readInitialBatteryStatus()
        lastChargingState = initial.isCharging
        _status.value = initial
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    private fun readInitialBatteryStatus(): BatteryStatus {
        val sticky = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        return parseBatteryIntent(sticky)
    }

    private fun parseBatteryIntent(intent: Intent?): BatteryStatus {
        if (intent == null) return BatteryStatus()
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            plugged != 0

        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargeSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargeSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargeSource.WIRELESS
            else -> if (isCharging) ChargeSource.AC else ChargeSource.NONE
        }

        return BatteryStatus(
            level = pct,
            isCharging = isCharging,
            chargeSource = source
        )
    }
}
