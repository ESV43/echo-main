package dev.brahmkshatriya.echo.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentQueueBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.player.PlayerTrackAdapter
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class QueueFragment : Fragment(R.layout.fragment_queue) {

    private val playerViewModel by activityViewModel<PlayerViewModel>()
    private val uiViewModel by activityViewModel<UiViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentQueueBinding.bind(view)
        setupTransition(view, false, MaterialSharedAxis.Y)
        
        val adapter = PlayerTrackAdapter(object : PlayerTrackAdapter.Listener {
            override fun onClick() = uiViewModel.expandPlayer()
            override fun onStartDoubleClick() {}
            override fun onEndDoubleClick() {}
        }, true)

        binding.recyclerView.adapter = adapter
        
        playerViewModel.queueFlow.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        binding.actionSmartQueue.setOnClickListener { playerViewModel.applySmartQueue() }
        binding.actionDedupe.setOnClickListener { playerViewModel.dedupeQueue() }
        binding.actionFuse.setOnClickListener { playerViewModel.fuseQueueSources() }
        binding.actionClear.setOnClickListener { playerViewModel.clearQueue() }

        applyInsets(binding.recyclerView, binding.appBarOutline)
        applyBackPressCallback()
    }
}
