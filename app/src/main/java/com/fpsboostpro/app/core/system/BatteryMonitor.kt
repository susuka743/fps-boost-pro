package com.fpsboostpro.app.core.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

data class BatterySnapshot(
    val levelPercent: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float?,
    val voltageMilliVolts: Int?,
    val healthDescription: String,
    val technology: String?,
    val chargeCounterMicroAh: Int?, // proxy signal for capacity/health trend
    val currentNowMicroA: Int?
)

/** Fully real: sticky battery broadcast + BatteryManager system service.
 * No permissions beyond the normal manifest declaration required. */
object BatteryMonitor {

    fun snapshot(context: Context): BatterySnapshot {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temp = if (tempTenths != null && tempTenths != Int.MIN_VALUE) tempTenths / 10f else null

        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }

        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
            else -> "Unknown"
        }

        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

        val chargeCounter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER).takeIf { it > Int.MIN_VALUE }
        } else null

        val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeIf { it != Int.MIN_VALUE }

        return BatterySnapshot(
            levelPercent = pct,
            isCharging = isCharging,
            temperatureCelsius = temp,
            voltageMilliVolts = voltage,
            healthDescription = health,
            technology = technology,
            chargeCounterMicroAh = chargeCounter,
            currentNowMicroA = currentNow
        )
    }
}
