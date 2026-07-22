package com.fpsboostpro.app.core.system

import android.os.Environment
import android.os.StatFs

data class StorageSnapshot(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usedPercent: Float
)

/** Real device storage stats via StatFs - no permission needed. */
object StorageMonitor {
    fun snapshot(): StorageSnapshot {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = total - free
        val pct = if (total > 0) (used.toFloat() / total.toFloat()) * 100f else 0f
        return StorageSnapshot(total, free, used, pct)
    }

    /** Real per-app cache size requires PackageManager.getPackageSizeInfo,
     * deprecated/removed for 3rd-party visibility since API 26 for privacy
     * reasons. Only this app's own cache dir size is directly readable
     * without root; other apps' cache can only be cleared (not sized) via
     * the system Storage Settings intent, or measured via root (see
     * RootStorageTools for the rooted path). */
    fun ownCacheSizeBytes(cacheDir: java.io.File): Long {
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
