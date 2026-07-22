package com.fpsboostpro.app.core.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile

/**
 * Reads real CPU utilization from /proc/stat (world-readable on Android,
 * no permission or root required). Some OEM builds (Samsung One UI,
 * newer MIUI) restrict /proc/stat for non-system apps; in that case this
 * returns null and the UI must show "Unavailable on this device" rather
 * than a fabricated number.
 */
object CpuMonitor {

    data class CpuSnapshot(val idle: Long, val total: Long)
    private var lastSnapshot: CpuSnapshot? = null

    /** Number of logical cores - always available via Runtime, no /proc needed. */
    fun coreCount(): Int = Runtime.getRuntime().availableProcessors()

    private fun readStatLine(): CpuSnapshot? {
        return try {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val load = reader.readLine() ?: return null
                val toks = load.split(" ").filter { it.isNotBlank() }
                // cpu  user nice system idle iowait irq softirq steal
                if (toks.size < 8 || toks[0] != "cpu") return null
                val user = toks[1].toLong()
                val nice = toks[2].toLong()
                val system = toks[3].toLong()
                val idle = toks[4].toLong()
                val iowait = toks[5].toLong()
                val irq = toks[6].toLong()
                val softirq = toks[7].toLong()
                val total = user + nice + system + idle + iowait + irq + softirq
                CpuSnapshot(idle = idle + iowait, total = total)
            }
        } catch (e: Exception) {
            null // Permission denied or file absent on this OEM/Android version
        }
    }

    /** Returns 0f..100f, or null if /proc/stat is unreadable on this device. */
    suspend fun currentUsagePercent(): Float? = withContext(Dispatchers.IO) {
        val current = readStatLine() ?: return@withContext null
        val previous = lastSnapshot
        lastSnapshot = current
        if (previous == null) return@withContext null // need two samples

        val totalDelta = current.total - previous.total
        val idleDelta = current.idle - previous.idle
        if (totalDelta <= 0) return@withContext null

        val usage = (1f - idleDelta.toFloat() / totalDelta.toFloat()) * 100f
        usage.coerceIn(0f, 100f)
    }

    /** Per-core current frequency in kHz, read from cpufreq (readable without
     * root on most stock kernels; returns empty list if blocked). */
    suspend fun perCoreFrequenciesKHz(): List<Int?> = withContext(Dispatchers.IO) {
        (0 until coreCount()).map { core ->
            try {
                RandomAccessFile("/sys/devices/system/cpu/cpu$core/cpufreq/scaling_cur_freq", "r")
                    .use { it.readLine()?.trim()?.toIntOrNull() }
            } catch (e: Exception) {
                null
            }
        }
    }
}
