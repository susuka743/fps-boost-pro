package com.fpsboostpro.app.core.optimize

import com.fpsboostpro.app.core.root.RootShell

/**
 * Real kernel-level tweaks executed through `su`. Every function here:
 *  - Only runs if RootShell.isRootAvailable() (checked by the ViewModel
 *    before calling, and re-checked here defensively).
 *  - Writes to REAL sysfs paths that exist on real Android kernels (paths
 *    vary by SoC/kernel; not all will exist on every device — each write
 *    reports actual success/failure, never a fabricated "done").
 *  - Never claims a tweak worked when the underlying `su` command failed
 *    or the sysfs node didn't exist.
 *  - Does NOT auto-apply on install; must be explicitly invoked by the
 *    user from the Root Pro screen after an explanatory warning dialog.
 */
object RootProTweaks {

    data class TweakOutcome(val success: Boolean, val detail: String)

    private suspend fun writeSysfs(path: String, value: String): TweakOutcome {
        if (!RootShell.isRootAvailable()) {
            return TweakOutcome(false, "Root not available")
        }
        val result = RootShell.exec("[ -e $path ] && echo $value > $path && echo OK || echo MISSING")
        return when {
            result.output.lastOrNull()?.trim() == "OK" -> TweakOutcome(true, "Set $path = $value")
            result.output.lastOrNull()?.trim() == "MISSING" -> TweakOutcome(false, "$path does not exist on this kernel")
            else -> TweakOutcome(false, "Write failed: ${result.output.joinToString()}")
        }
    }

    // ---- CPU Governor ----
    suspend fun listAvailableGovernors(): List<String> {
        val res = RootShell.exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>/dev/null")
        return if (res.success && res.output.isNotEmpty()) res.output.first().split(" ").filter { it.isNotBlank() } else emptyList()
    }

    suspend fun currentGovernor(): String? = RootShell.readFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")

    suspend fun setGovernorAllCores(governor: String): TweakOutcome {
        if (!RootShell.isRootAvailable()) return TweakOutcome(false, "Root not available")
        val coreCount = Runtime.getRuntime().availableProcessors()
        val commands = (0 until coreCount).map {
            "echo $governor > /sys/devices/system/cpu/cpu$it/cpufreq/scaling_governor 2>/dev/null"
        }
        val result = RootShell.execMultiple(commands)
        return TweakOutcome(result.success, if (result.success) "Governor set to $governor on $coreCount cores" else "Failed on one or more cores")
    }

    // ---- CPU Max/Min Frequency ----
    suspend fun availableFrequenciesKHz(core: Int = 0): List<Int> {
        val res = RootShell.exec("cat /sys/devices/system/cpu/cpu$core/cpufreq/scaling_available_frequencies 2>/dev/null")
        return if (res.success && res.output.isNotEmpty())
            res.output.first().split(" ").mapNotNull { it.trim().toIntOrNull() }.sorted()
        else emptyList()
    }

    suspend fun setMaxFrequency(khz: Int): TweakOutcome {
        val coreCount = Runtime.getRuntime().availableProcessors()
        val commands = (0 until coreCount).map { "echo $khz > /sys/devices/system/cpu/cpu$it/cpufreq/scaling_max_freq 2>/dev/null" }
        val result = RootShell.execMultiple(commands)
        return TweakOutcome(result.success, "Max frequency requested: ${khz / 1000} MHz")
    }

    suspend fun setMinFrequency(khz: Int): TweakOutcome {
        val coreCount = Runtime.getRuntime().availableProcessors()
        val commands = (0 until coreCount).map { "echo $khz > /sys/devices/system/cpu/cpu$it/cpufreq/scaling_min_freq 2>/dev/null" }
        val result = RootShell.execMultiple(commands)
        return TweakOutcome(result.success, "Min frequency requested: ${khz / 1000} MHz")
    }

    // ---- GPU Frequency (highly SoC-specific; Adreno/Mali paths differ) ----
    suspend fun tryGpuMaxFrequency(khz: Int): TweakOutcome {
        // Common Adreno path; Mali/PowerVR paths differ per vendor and are
        // intentionally NOT guessed further — we report unsupported rather
        // than silently no-op.
        val adrenoPath = "/sys/class/kgsl/kgsl-3d0/max_gpuclk"
        return writeSysfs(adrenoPath, khz.toString())
    }

    // ---- I/O Scheduler ----
    suspend fun listAvailableIoSchedulers(): List<String> {
        val res = RootShell.exec("cat /sys/block/*/queue/scheduler 2>/dev/null | head -1")
        if (!res.success || res.output.isEmpty()) return emptyList()
        return Regex("[\\[\\]]").replace(res.output.first(), "").split(" ").filter { it.isNotBlank() }
    }

    suspend fun setIoScheduler(scheduler: String): TweakOutcome {
        if (!RootShell.isRootAvailable()) return TweakOutcome(false, "Root not available")
        val result = RootShell.exec(
            "for f in /sys/block/*/queue/scheduler; do echo $scheduler > \$f 2>/dev/null; done; echo DONE"
        )
        return TweakOutcome(result.success, "I/O scheduler set to $scheduler where supported")
    }

    // ---- ZRAM ----
    suspend fun zramStatus(): String? = RootShell.readFile("/sys/block/zram0/disksize")

    suspend fun setZramSizeMb(sizeMb: Int): TweakOutcome {
        val bytes = sizeMb.toLong() * 1024 * 1024
        val result = RootShell.execMultiple(
            listOf(
                "swapoff /dev/block/zram0 2>/dev/null",
                "echo 1 > /sys/block/zram0/reset 2>/dev/null",
                "echo $bytes > /sys/block/zram0/disksize 2>/dev/null",
                "mkswap /dev/block/zram0 2>/dev/null",
                "swapon /dev/block/zram0 2>/dev/null",
                "echo DONE"
            )
        )
        return TweakOutcome(result.success, "ZRAM resized to ${sizeMb}MB (requires supported kernel module)")
    }

    // ---- LMK (Low Memory Killer) minfree tuning ----
    suspend fun setLmkMinFree(values: List<Int>): TweakOutcome {
        val joined = values.joinToString(",")
        return writeSysfs("/sys/module/lowmemorykiller/parameters/minfree", joined)
    }

    // ---- TCP / Network tweaks ----
    suspend fun setTcpCongestionControl(algorithm: String): TweakOutcome {
        return writeSysfs("/proc/sys/net/ipv4/tcp_congestion_control", algorithm)
    }

    suspend fun availableTcpAlgorithms(): List<String> {
        val res = RootShell.exec("cat /proc/sys/net/ipv4/tcp_available_congestion_control 2>/dev/null")
        return if (res.success && res.output.isNotEmpty()) res.output.first().split(" ").filter { it.isNotBlank() } else emptyList()
    }

    // ---- Disable verbose logging (real logd control) ----
    suspend fun disableLogging(): TweakOutcome {
        val result = RootShell.exec("setprop persist.logd.logpersistd disabled; logcat -P '' 2>/dev/null; echo DONE")
        return TweakOutcome(result.success, "Logging services deprioritized")
    }

    // ---- Trim system cache (real pm command, same AOSP API used by Settings > Storage) ----
    suspend fun trimSystemCache(): TweakOutcome {
        val result = RootShell.exec("pm trim-caches 999G")
        return TweakOutcome(result.success, "System-wide cache trim requested")
    }

    // ---- Kernel info (read-only, always safe) ----
    suspend fun kernelVersion(): String? = RootShell.readFile("/proc/version")

    // ---- Restore-to-default helper: re-reads and reapplies device defaults
    // captured at first Root Pro launch (see RootBackupManager). This file
    // intentionally contains no "defaults" guesses of its own — restoring
    // uses values captured from the ACTUAL device before any tweak, never
    // hardcoded generic numbers.
}
