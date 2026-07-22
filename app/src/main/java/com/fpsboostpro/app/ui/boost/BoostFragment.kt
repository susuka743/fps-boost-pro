package com.fpsboostpro.app.ui.boost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fpsboostpro.app.R
import com.fpsboostpro.app.databinding.FragmentStubBinding

/**
 * Full optimization suite tab (RAM Cleaner, Cache Cleaner, CPU/GPU Optimizer,
 * Gaming Mode, Thermal/Network/Storage/Battery optimization). The quick
 * "BOOST NOW" action already works from Home; this tab will expose each
 * optimizer individually. Stubbed for now.
 */
class BoostFragment : Fragment() {

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
        binding.txtStubTitle.text = getString(R.string.nav_boost)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
