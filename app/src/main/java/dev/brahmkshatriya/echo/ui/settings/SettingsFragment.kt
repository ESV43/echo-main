package dev.brahmkshatriya.echo.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentSettingsBinding
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.download.DownloadFragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsBinding.bind(view)

        binding.player.setOnClickListener {
            requireActivity().openFragment<SettingsPlayerFragment>()
        }
        binding.lookAndFeel.setOnClickListener {
            requireActivity().openFragment<SettingsLookFragment>()
        }
        binding.other.setOnClickListener {
            requireActivity().openFragment<SettingsOtherFragment>()
        }
        binding.v4Lab.setOnClickListener {
            requireActivity().openFragment<V4LabFragment>()
        }
        binding.downloads.setOnClickListener {
            requireActivity().openFragment<DownloadFragment>()
        }
    }
}
