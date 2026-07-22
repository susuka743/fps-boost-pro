package com.fpsboostpro.app.core.system

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService
import java.io.RandomAccessFile

enum class ThermalStatus { NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN, UNKNOWN }

data class ThermalSnapshot(
    val status: ThermalStatus,
    val zoneTemperatures: List<Float>, // raw thermal_zone readings, best-effort
    val isEstimate: Boolean
)

/**
 * Real thermal status via PowerManager.getCurrentThermalStatus (Android 10+,
 * API 29+ - system-classified, OEM-calibrated, no root needed).
 *
 * On API < 29 there is no public thermal API. We DO NOT fabricate a status;
 * instead we attempt a best-effort raw read of /sys/class/thermal/thermal_zone*
 * (often readable without root, varies by OEM) and mark the result as an
 * estimate. If neither works, callers must show "Unavailable".
 */
object ThermalMonitor {

    fun snapshot(context: Context): ThermalSnapshot {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService<PowerManager>()
            val status = when (pm?.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
                PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
                else -> ThermalStatus.UNKNOWN
            }
            return ThermalSnapshot(status = status, zoneTemperatures = readRawZonesBestEffort(), isEstimate = false)
        }

        val zones = readRawZonesBestEffort()
        val estimatedStatus = when {
            zones.isEmpty() -> ThermalStatus.UNKNOWN
            zones.max() > 55f -> ThermalStatus.SEVERE
            zones.max() > 45f -> ThermalStatus.MODERATE
            zones.max() > 38f -> ThermalStatus.LIGHT
            else -> ThermalStatus.NONE
        }
        return ThermalSnapshot(status = estimatedStatus, zoneTemperatures = zones, isEstimate = true)
    }

    private fun readRawZonesBestEffort(): List<Float> {
        val readings = mutableListOf<Float>()
        for (i in 0 until 15) {
            try {
                val raw = RandomAccessFile("/sys/class/thermal/thermal_zone$i/temp", "r")
                    .use { it.readLine()?.trim()?.toFloatOrNull() } ?: continue
                // Values are usually millidegrees C; normalize.
                val celsius = if (raw > 1000) raw / 1000f else raw
                if (celsius in 0f..120f) readings.add(celsius)
            } catch (e: Exception) {
                // Not readable on this zone/OEM - skip silently, this is expected on many devices
            }
        }
        return readings
    }
}
