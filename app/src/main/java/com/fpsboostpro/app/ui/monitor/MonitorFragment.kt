package com.fpsboostpro.app.ui.monitor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fpsboostpro.app.R
import com.fpsboostpro.app.databinding.FragmentStubBinding

/**
 * Real-time monitoring dashboard tab (CPU/GPU/RAM/FPS/Temp/Battery/Storage
 * charts via MPAndroidChart). Will reuse HomeViewModel-style polling.
 * Stubbed for now.
 */
class MonitorFragment : Fragment() {

    private var _binding: FragmentStubBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgStubIcon.setImageResource(R.drawable.ic_bolt_small)
        binding.txtStubTitle.text = getString(R.string.nav_monitor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
