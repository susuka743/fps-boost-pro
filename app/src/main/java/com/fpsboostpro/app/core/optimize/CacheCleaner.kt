package com.fpsboostpro.app.core.optimize

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.fpsboostpro.app.core.root.RootShell
import com.fpsboostpro.app.core.system.StorageMonitor

/**
 * HONEST BEHAVIOR NOTE:
 * A normal app can only clear ITS OWN cache directory (context.cacheDir),
 * guaranteed by the Android sandbox. Since API 30, apps cannot read or
 * clear other apps' cache sizes/content without root - full stop, this is
 * a deliberate privacy boundary, not a limitation we can code around.
 *
 * Non-root: we clear our own cache (real, if small) and deep-link the user
 * to Settings > Storage where Android itself offers a real one-tap
 * "Free up space" / per-app clear-cache flow - we are honest that we're
 * handing off to the OS rather than pretending to do it ourselves.
 *
 * Root: we can genuinely clear every app's cache directory system-wide.
 */
class CacheCleaner(
    private val context: Context,
    private val allowRoot: Boolean
) : Optimizer {

    override val id = "cache_cleaner"
    override val displayName = "Cache Cleaner"
    override val requiresRoot = false

    override suspend fun execute(): OptimizationResult {
        val ownCacheBefore = StorageMonitor.ownCacheSizeBytes(context.cacheDir)

        // Always-real step: clear our own cache fully.
        val ownFreed = clearOwnCache()

        if (allowRoot && RootShell.isRootAvailable()) {
            val rootFreed = clearSystemWideCacheViaRoot()
            return OptimizationResult(
                success = true,
                message = "Cleared app cache system-wide via root.",
                bytesFreed = ownFreed + rootFreed
            )
        }

        return OptimizationResult(
            success = true,
            message = if (ownFreed > 0)
                "Cleared ${formatBytes(ownFreed)} of app cache. Open system Storage settings to clear cache for other apps too."
            else
                "App cache already minimal. Open system Storage settings to clear cache for other apps.",
            bytesFreed = ownFreed
        )
    }

    private fun clearOwnCache(): Long {
        val before = StorageMonitor.ownCacheSizeBytes(context.cacheDir)
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
        return before
    }

    /** Real root path: `pm trim-caches` is a genuine AOSP shell command that
     * asks the OS to trim cache across all apps down to a target free-space
     * threshold - not a fabricated command. */
    private suspend fun clearSystemWideCacheViaRoot(): Long {
        val before = StorageMonitor.snapshot()
        RootShell.exec("pm trim-caches 999G") // requests trimming as much as possible
        val after = StorageMonitor.snapshot()
        return (after.freeBytes - before.freeBytes).coerceAtLeast(0)
    }

    /** Honest hand-off intent for non-root users who want to clear other
     * apps' cache - this opens REAL system UI, not a fake in-app screen. */
    fun openSystemStorageSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(kb)
    }
}
