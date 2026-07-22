package com.fpsboostpro.app.core.system

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usedPercent: Float,
    val isLowMemory: Boolean,
    val threshold: Long
)

/** 100% real data from ActivityManager.getMemoryInfo - no root, no permission needed. */
object MemoryMonitor {

    fun snapshot(context: Context): MemorySnapshot {
        val am = context.getSystemService<ActivityManager>()!!
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)

        val used = info.totalMem - info.availMem
        val usedPct = if (info.totalMem > 0) (used.toFloat() / info.totalMem.toFloat()) * 100f else 0f

        return MemorySnapshot(
            totalBytes = info.totalMem,
            availableBytes = info.availMem,
            usedBytes = used,
            usedPercent = usedPct,
            isLowMemory = info.lowMemory,
            threshold = info.threshold
        )
    }

    /** Real list of background processes the OS reports as running for THIS
     * app's UID visibility. Note: since Android 8+, apps cannot see other
     * apps' full process lists without special system permissions - so
     * "Background Apps Killer" for 3rd-party apps only works by:
     *   1) Using UsageStatsManager (needs PACKAGE_USAGE_STATS, user-granted)
     *      to identify recently-used apps, then
     *   2) Prompting the user through Settings > App Info > Force Stop
     *      (Android forbids one normal app from silently killing another),
     *   OR
     *   3) On rooted devices, actually killing via `am kill` / `kill -9`
     *      through RootShell - see RootAppKiller.
     * We do not fake a "killed 12 apps, freed 800MB" result on non-root
     * devices; see BackgroundAppsRepository for the honest flow.
     */
    suspend fun runningAppUsageSummary(context: Context) = withContext(Dispatchers.IO) {
        UsageStatsHelper.recentForegroundApps(context)
    }
}
