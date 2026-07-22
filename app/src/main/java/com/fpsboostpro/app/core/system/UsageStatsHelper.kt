package com.fpsboostpro.app.core.system

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.core.content.getSystemService

data class RecentAppUsage(
    val packageName: String,
    val lastTimeUsedMillis: Long,
    val totalTimeInForegroundMillis: Long
)

object UsageStatsHelper {

    /** Whether the user has granted "Usage Access" in system settings.
     * This is a REAL permission check - PACKAGE_USAGE_STATS cannot be
     * granted via a normal runtime dialog, only through Settings, so the
     * UI must deep-link the user there (see SettingsScreen / onboarding). */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun recentForegroundApps(context: Context, windowMillis: Long = 60_000L * 60): List<RecentAppUsage> {
        if (!hasUsageAccess(context)) return emptyList()
        val usm = context.getSystemService<UsageStatsManager>() ?: return emptyList()
        val end = System.currentTimeMillis()
        val start = end - windowMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end) ?: return emptyList()

        return stats
            .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .sortedByDescending { it.lastTimeUsed }
            .map {
                RecentAppUsage(
                    packageName = it.packageName,
                    lastTimeUsedMillis = it.lastTimeUsed,
                    totalTimeInForegroundMillis = it.totalTimeInForeground
                )
            }
    }
}
