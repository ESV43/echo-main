package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentDownloadBinding
import dev.brahmkshatriya.echo.download.Info
import dev.brahmkshatriya.echo.ui.common.ExceptionFragment
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.GridAdapter.Companion.configureGridLayout
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.download.DownloadsAdapter.Companion.toItems
import dev.brahmkshatriya.echo.ui.download.DownloadsAdapter.Filter
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.applyInsets
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import org.koin.androidx.viewmodel.ext.android.viewModel

class DownloadFragment : Fragment(R.layout.fragment_download) {

    private val vm by viewModel<DownloadViewModel>()
    private var filter = Filter.Active
    private var latestInfos = emptyList<Info>()
    
    private val downloadsAdapter by lazy {
        DownloadsAdapter(object : DownloadsAdapter.Listener {
            override fun onCancel(trackId: Long) = vm.cancel(trackId)
            override fun onRestart(trackId: Long) = vm.restart(trackId)
            override fun onExceptionClicked(data: ExceptionUtils.Data) = requireActivity()
                .openFragment<ExceptionFragment>(null, ExceptionFragment.getBundle(data))
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentDownloadBinding.bind(view)
        setupTransition(view)
        
        binding.appBarLayout.configureAppBar { offset ->
            binding.toolbarOutline.alpha = offset
        }

        binding.downloadTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                filter = Filter.entries[tab.position]
                updateItems(binding)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        configureGridLayout(binding.recyclerView, downloadsAdapter)

        observe(vm.flow) {
            latestInfos = it
            updateItems(binding)
        }

        applyBackPressCallback()
        with(MainFragment) {
            applyInsets(binding.recyclerView, binding.toolbarOutline)
        }

        binding.fabCancel.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cancel_all)
                .setMessage(getString(R.string.delete_playlist_confirmation, getString(R.string.downloads)))
                .setPositiveButton(R.string.delete) { _, _ ->
                    vm.cancelAll()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun updateItems(binding: FragmentDownloadBinding) {
        downloadsAdapter.submitList(latestInfos.toItems(vm.extensions.music.value, filter))
        binding.fabCancel.isVisible = filter == Filter.Active && latestInfos.isNotEmpty()
    }
}
