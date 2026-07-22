package com.fpsboostpro.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fpsboostpro.app.R
import com.fpsboostpro.app.databinding.FragmentStubBinding

/**
 * Settings tab (Dark Mode, Language, Notifications, Auto Boost, Auto Clean
 * RAM, Startup Optimization, Gaming Overlay). Also the entry point to the
 * ROOT PRO section once root access is detected. Stubbed for now.
 */
class SettingsFragment : Fragment() {

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
        binding.txtStubTitle.text = getString(R.string.nav_settings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
