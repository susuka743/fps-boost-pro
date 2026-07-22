package com.fpsboostpro.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.fpsboostpro.app.R
import com.fpsboostpro.app.core.system.ThermalStatus
import com.fpsboostpro.app.databinding.FragmentHomeBinding
import com.fpsboostpro.app.databinding.ItemMetricCardBinding
import kotlin.math.roundToInt

/**
 * Home dashboard. Every value shown here comes from HomeViewModel's real
 * polling of CPU/RAM/thermal/battery. FPS and GPU% are intentionally shown
 * as "—" with an honest sub-label (Overlay Off / Root Required) rather than
 * a fabricated number - see HomeViewModel's doc comment for why.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var metricFps: ItemMetricCardBinding
    private lateinit var metricCpu: ItemMetricCardBinding
    private lateinit var metricGpu: ItemMetricCardBinding
    private lateinit var metricRam: ItemMetricCardBinding
    private lateinit var metricTemp: ItemMetricCardBinding
    private lateinit var metricBattery: ItemMetricCardBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        metricFps = ItemMetricCardBinding.bind(binding.metricFps.root)
        metricCpu = ItemMetricCardBinding.bind(binding.metricCpu.root)
        metricGpu = ItemMetricCardBinding.bind(binding.metricGpu.root)
        metricRam = ItemMetricCardBinding.bind(binding.metricRam.root)
        metricTemp = ItemMetricCardBinding.bind(binding.metricTemp.root)
        metricBattery = ItemMetricCardBinding.bind(binding.metricBattery.root)

        setupStaticMetricChrome()

        binding.txtDeviceModel.text = android.os.Build.MODEL
        binding.txtAndroidVersion.text = getString(
            R.string.android_version_fmt, android.os.Build.VERSION.RELEASE
        )

        binding.btnBoostNow.setOnClickListener {
            viewModel.runBoost()
        }

        viewModel.uiState.observe(viewLifecycleOwner, Observer { state -> render(state) })
    }

    /** Icons + labels never change, so set them once instead of on every poll tick. */
    private fun setupStaticMetricChrome() {
        metricFps.imgMetricIcon.setImageResource(R.drawable.ic_fps)
        metricFps.txtMetricLabel.text = getString(R.string.current_fps)
        metricFps.progressMetric.visibility = View.GONE

        metricCpu.imgMetricIcon.setImageResource(R.drawable.ic_cpu)
        metricCpu.txtMetricLabel.text = getString(R.string.cpu_usage)

        metricGpu.imgMetricIcon.setImageResource(R.drawable.ic_gpu)
        metricGpu.txtMetricLabel.text = getString(R.string.gpu_usage)
        metricGpu.progressMetric.visibility = View.GONE

        metricRam.imgMetricIcon.setImageResource(R.drawable.ic_ram)
        metricRam.txtMetricLabel.text = getString(R.string.ram_usage)

        metricTemp.imgMetricIcon.setImageResource(R.drawable.ic_temp)
        metricTemp.txtMetricLabel.text = getString(R.string.device_temp)
        metricTemp.progressMetric.visibility = View.GONE

        metricBattery.imgMetricIcon.setImageResource(R.drawable.ic_battery)
        metricBattery.txtMetricLabel.text = getString(R.string.battery_health)
    }

    private fun render(state: HomeUiState) {
        // --- Device header ---
        binding.txtDeviceModel.text = state.deviceModel
        binding.txtAndroidVersion.text =
            getString(R.string.android_version_fmt, state.androidVersion)

        val rootTint = if (state.isRooted) R.color.neon_green else R.color.text_secondary
        binding.imgRootBadge.setColorFilter(requireContext().getColor(rootTint))
        binding.txtRootStatus.text = if (state.isRooted) {
            getString(R.string.root_detected)
        } else {
            getString(R.string.root_not_detected)
        }
        binding.txtRootStatus.setTextColor(requireContext().getColor(rootTint))
        binding.imgRootStatusIcon.setColorFilter(requireContext().getColor(rootTint))

        // --- FPS: honest placeholder, no fabricated number ---
        metricFps.txtMetricValue.text = "—"

        // --- CPU ---
        if (state.cpuPercent != null) {
            val cpuPct = state.cpuPercent.roundToInt().coerceIn(0, 100)
            metricCpu.txtMetricValue.text = getString(R.string.percent_fmt, cpuPct)
            metricCpu.progressMetric.progress = cpuPct
        } else {
            metricCpu.txtMetricValue.text = "—"
            metricCpu.progressMetric.progress = 0
        }

        // --- GPU: no public non-root API exists; honest placeholder ---
        metricGpu.txtMetricValue.text = "—"

        // --- RAM ---
        val ramPct = state.ramPercent.roundToInt().coerceIn(0, 100)
        metricRam.txtMetricValue.text = getString(R.string.percent_fmt, ramPct)
        metricRam.progressMetric.progress = ramPct

        // --- Temperature (battery temp is the closest reliable non-root proxy) ---
        if (state.batteryTempC != null) {
            metricTemp.txtMetricValue.text =
                getString(R.string.celsius_fmt, state.batteryTempC.roundToInt())
        } else {
            metricTemp.txtMetricValue.text = "—"
        }
        val thermalColor = when (state.thermalStatus) {
            ThermalStatus.SEVERE, ThermalStatus.CRITICAL -> R.color.danger_red
            ThermalStatus.MODERATE -> R.color.neon_cyan
            else -> R.color.neon_green
        }
        metricTemp.txtMetricValue.setTextColor(requireContext().getColor(thermalColor))

        // --- Battery ---
        metricBattery.txtMetricValue.text =
            getString(R.string.percent_fmt, state.batteryPercent)
        metricBattery.progressMetric.progress = state.batteryPercent
        metricBattery.txtMetricLabel.text = getString(
            R.string.battery_health_fmt, state.batteryHealth
        )

        // --- Boost button state ---
        binding.btnBoostNow.isEnabled = !state.isBoosting
        binding.progressBoosting.visibility = if (state.isBoosting) View.VISIBLE else View.GONE
        binding.btnBoostNow.text = if (state.isBoosting) "" else getString(R.string.boost_now)

        if (!state.isBoosting && state.lastBoostFreedMb != null) {
            binding.txtBoostResult.visibility = View.VISIBLE
            binding.txtBoostResult.text = getString(
                R.string.boost_result_fmt, state.lastBoostFreedMb
            )
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startPolling()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopPolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
