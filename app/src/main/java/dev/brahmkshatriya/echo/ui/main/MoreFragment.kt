package dev.brahmkshatriya.echo.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentMoreBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.settings.SettingsBottomSheet
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition

class MoreFragment : Fragment(R.layout.fragment_more) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMoreBinding.bind(view)
        setupTransition(view, false, MaterialSharedAxis.Y)
        
        binding.toolbar.title = getString(R.string.other)
        
        applyBackPressCallback()
    }
}
