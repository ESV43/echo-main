package dev.brahmkshatriya.echo.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentQueueBinding
import dev.brahmkshatriya.echo.playback.PlayerViewModel
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.setupPlayerMoreBehavior
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.player.PlayerTrackAdapter
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class QueueFragment : Fragment(R.layout.fragment_queue) {

    private val playerViewModel by activityViewModel<PlayerViewModel>()
    private val uiViewModel by activityViewModel<UiViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentQueueBinding.bind(view)
        setupTransition(view, false, MaterialSharedAxis.Y)
        
        val adapter = PlayerTrackAdapter(uiViewModel, playerViewModel.playerState.current, object : PlayerTrackAdapter.Listener {
            override fun onClick() = uiViewModel.expandPlayer()
        })

        binding.recyclerView.adapter = adapter
        
        observe(playerViewModel.queueFlow) {
            adapter.submitList(it)
        }

        binding.actionFuse.setOnClickListener { playerViewModel.fuseQueueSources() }
        binding.actionClear.setOnClickListener { playerViewModel.clearQueue() }

        applyInsets(binding.recyclerView, binding.appBarOutline, 0) { }
        applyBackPressCallback()
    }
}
