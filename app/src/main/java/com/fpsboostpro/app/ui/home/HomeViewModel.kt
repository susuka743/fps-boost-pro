package com.fpsboostpro.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.fpsboostpro.app.core.optimize.CacheCleaner
import com.fpsboostpro.app.core.optimize.RamCleaner
import com.fpsboostpro.app.core.root.RootShell
import com.fpsboostpro.app.core.system.BatteryMonitor
import com.fpsboostpro.app.core.system.CpuMonitor
import com.fpsboostpro.app.core.system.MemoryMonitor
import com.fpsboostpro.app.core.system.ThermalMonitor
import com.fpsboostpro.app.core.system.ThermalStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class HomeUiState(
    val cpuPercent: Float? = null,
    val ramPercent: Float = 0f,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val thermalStatus: ThermalStatus = ThermalStatus.UNKNOWN,
    val thermalIsEstimate: Boolean = true,
    val batteryPercent: Int = 0,
    val batteryHealth: String = "Unknown",
    val batteryTempC: Float? = null,
    val isRooted: Boolean = false,
    val isBoosting: Boolean = false,
    val lastBoostFreedMb: Long? = null,
    val deviceModel: String = android.os.Build.MODEL,
    val androidVersion: String = android.os.Build.VERSION.RELEASE
)

/**
 * Note on FPS + GPU usage: Android exposes no public, permission-free API
 * for live per-app FPS or GPU utilization percent (unlike CPU/RAM/battery).
 * Real FPS requires either:
 *   (a) the opt-in Gaming Overlay (SurfaceFlinger frame-stats via the
 *       accessibility/overlay service, only while a game is foregrounded), or
 *   (b) root, reading GPU busy time from /sys/class/kgsl on Adreno devices.
 * The Home dashboard therefore shows FPS/GPU as "—" with a real state
 * (Overlay Off / Root Required) instead of inventing numbers. See
 * OverlayService for the honest in-game implementation.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            // Root check once per session-entry, not on every tick
            val rooted = try { RootShell.isRootAvailable() } catch (e: Exception) { false }
            // First CPU sample primes the delta calculation
            CpuMonitor.currentUsagePercent()

            while (true) {
                val cpu = CpuMonitor.currentUsagePercent()
                val mem = MemoryMonitor.snapshot(getApplication())
                val thermal = ThermalMonitor.snapshot(getApplication())
                val battery = BatteryMonitor.snapshot(getApplication())

                val current = _uiState.value ?: HomeUiState()
                _uiState.value = current.copy(
                    cpuPercent = cpu,
                    ramPercent = mem.usedPercent,
                    ramUsedGb = mem.usedBytes / 1_073_741_824f,
                    ramTotalGb = mem.totalBytes / 1_073_741_824f,
                    thermalStatus = thermal.status,
                    thermalIsEstimate = thermal.isEstimate,
                    batteryPercent = battery.levelPercent,
                    batteryHealth = battery.healthDescription,
                    batteryTempC = battery.temperatureCelsius,
                    isRooted = rooted
                )
                delay(2000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    fun runBoost() {
        val current = _uiState.value ?: HomeUiState()
        if (current.isBoosting) return
        _uiState.value = current.copy(isBoosting = true)

        viewModelScope.launch {
            val rooted = current.isRooted
            val app: Application = getApplication()
            val freedBytes = try {
                val ramResult = RamCleaner(app, allowRoot = rooted).execute()
                val cacheResult = CacheCleaner(app, allowRoot = rooted).execute()
                (ramResult.bytesFreed ?: 0L) + (cacheResult.bytesFreed ?: 0L)
            } catch (e: Exception) {
                0L
            }
            // brief delay so the boost animation reads as a real, deliberate action
            delay(1400)
            val latest = _uiState.value ?: HomeUiState()
            _uiState.value = latest.copy(
                isBoosting = false,
                lastBoostFreedMb = freedBytes / 1_048_576L
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
