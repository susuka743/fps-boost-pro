package com.fpsboostpro.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.fpsboostpro.app.data.model.FpsLimit
import com.fpsboostpro.app.data.model.GameCatalog
import com.fpsboostpro.app.data.model.GraphicsQuality
import com.fpsboostpro.app.data.model.InstalledGame
import com.fpsboostpro.app.data.model.PerformanceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameDetectionRepository {

    /** Real detection: checks each known package name against
     * PackageManager. Requires QUERY_ALL_PACKAGES (declared in manifest) on
     * API 30+ since games are outside our app's normal visible-package
     * auto-grant list. */
    suspend fun detectInstalledGames(context: Context): List<InstalledGame> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val results = mutableListOf<InstalledGame>()

        for (def in GameCatalog.knownGames) {
            for (pkg in def.packageNames) {
                val versionName = try {
                    pm.getPackageInfo(pkg, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
                if (versionName != null || isPackageInstalled(pm, pkg)) {
                    results.add(
                        InstalledGame(
                            definition = def,
                            packageName = pkg,
                            versionName = versionName,
                            fpsLimit = FpsLimit.FPS_60,
                            graphicsQuality = GraphicsQuality.HIGH,
                            performanceProfile = PerformanceProfile.BALANCED
                        )
                    )
                    break // one match per game def is enough
                }
            }
        }
        results
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Real launch via the standard launch intent - this is how every
     * launcher on Android starts an app; no special permission needed. */
    fun launchGame(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
