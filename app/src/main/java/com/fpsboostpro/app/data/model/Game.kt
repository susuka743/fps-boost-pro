package com.fpsboostpro.app.data.model

// Plain data classes + Gson (see build.gradle) for JSON, not kotlinx.serialization -
// keeps the dependency surface smaller since Gson is already pulled in for backups/export.

enum class FpsLimit(val value: Int) {
    FPS_30(30), FPS_45(45), FPS_60(60), FPS_90(90),
    FPS_120(120), FPS_144(144), FPS_240(240)
}

enum class GraphicsQuality { LOW, MEDIUM, HIGH, HDR, ULTRA_HDR }

enum class PerformanceProfile {
    BATTERY_SAVER, BALANCED, PERFORMANCE, EXTREME_PERFORMANCE, ULTRA_GAMING
}

/**
 * Static catalog entry: the package names known games are commonly
 * published under. Detection matches against installed packages - we do
 * NOT claim to detect a game we haven't verified is actually installed.
 */
data class KnownGameDefinition(
    val displayName: String,
    val packageNames: List<String>, // some games ship under multiple package IDs by region
    val iconRes: Int? = null,
    val maxSupportedFps: FpsLimit = FpsLimit.FPS_90
)

data class InstalledGame(
    val definition: KnownGameDefinition,
    val packageName: String, // the actual matched package on this device
    val versionName: String?,
    var fpsLimit: FpsLimit,
    var graphicsQuality: GraphicsQuality,
    var performanceProfile: PerformanceProfile
)

object GameCatalog {
    /** Real, publicly known package IDs. Detection is a simple installed-
     * package lookup (PackageManager) - honest and accurate, but limited to
     * this fixed list; games not in the catalog won't be auto-detected
     * (the "App Manager" screen lets users add any app manually instead). */
    val knownGames = listOf(
        KnownGameDefinition("PUBG Mobile", listOf("com.tencent.ig", "com.pubg.krmobile", "com.rekoo.pubgm"), maxSupportedFps = FpsLimit.FPS_90),
        KnownGameDefinition("Free Fire", listOf("com.dts.freefireth", "com.dts.freefiremax"), maxSupportedFps = FpsLimit.FPS_60),
        KnownGameDefinition("Call of Duty Mobile", listOf("com.activision.callofduty.shooter"), maxSupportedFps = FpsLimit.FPS_90),
        KnownGameDefinition("Mobile Legends: Bang Bang", listOf("com.mobile.legends"), maxSupportedFps = FpsLimit.FPS_120),
        KnownGameDefinition("Genshin Impact", listOf("com.miHoYo.GenshinImpact", "com.miHoYo.Yuanshen"), maxSupportedFps = FpsLimit.FPS_60),
        KnownGameDefinition("Minecraft", listOf("com.mojang.minecraftpe"), maxSupportedFps = FpsLimit.FPS_120),
        KnownGameDefinition("Roblox", listOf("com.roblox.client"), maxSupportedFps = FpsLimit.FPS_60),
        KnownGameDefinition("eFootball", listOf("com.konami.pesam", "jp.konami.pesam"), maxSupportedFps = FpsLimit.FPS_60)
    )
}
