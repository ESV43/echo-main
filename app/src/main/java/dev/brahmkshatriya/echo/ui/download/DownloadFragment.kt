package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.databinding.FragmentDownloadBinding
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.Info
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.getFeed
import dev.brahmkshatriya.echo.ui.common.ExceptionFragment
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.ui.common.GridAdapter.Companion.configureGridLayout
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.download.DownloadsAdapter.Companion.toItems
import dev.brahmkshatriya.echo.ui.download.DownloadsAdapter.Filter
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getFeedAdapter
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getTouchHelper
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener.Companion.getFeedListener
import dev.brahmkshatriya.echo.ui.feed.FeedData
import dev.brahmkshatriya.echo.ui.feed.FeedViewModel
import dev.brahmkshatriya.echo.ui.media.LineAdapter
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
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
        configureAppBar(binding.appBarLayout, binding.toolbar, binding.appBarOutline)
        FastScrollerHelper.applyTo(binding.recyclerView)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                filter = Filter.entries[tab.position]
                updateItems()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        val extensionId = "offline"
        val feedData = FeedData("downloads", FeedViewModel.Args.Normal(extensionId)) {
            vm.downloadFeed
        }
        val feedAdapter = getFeedAdapter(feedData)
        val feedListener = getFeedListener(extensionId, "downloads")
        getTouchHelper(feedListener).attachToRecyclerView(binding.recyclerView)

        val headerAdapter = LineAdapter()
        val loadingAdapter = GridAdapter.Loading(this, feedData)
        configureGridLayout(
            binding.recyclerView,
            GridAdapter.Concat(
                headerAdapter,
                downloadsAdapter,
                feedAdapter,
                loadingAdapter
            )
        )

        observe(vm.downloadFeed) {
            headerAdapter.submitList(if (it.tabs.isNotEmpty()) listOf(SimpleItemSpan) else listOf())
        }

        observe(vm.downloadFlow) {
            latestInfos = it
            updateItems()
        }

        observe(feedData.loadStateFlow) {
            if (it is LoadState.Error) {
                headerAdapter.submitList(listOf())
            }
        }

        binding.swipeRefresh.run {
            setOnRefreshListener { feedData.refresh() }
            observe(feedData.isRefreshingFlow) { isRefreshing = it }
        }

        applyBackPressCallback()
        applyInsets(binding.recyclerView, binding.appBarOutline)
        applyContentInsets(binding.tabLayout)
        applyFabInsets(binding.actionFab, binding.appBarOutline)

        binding.actionFab.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_all_downloads)
                .setMessage(R.string.delete_all_downloads_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    vm.cancelAll()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun updateItems() {
        downloadsAdapter.submitList(latestInfos.toItems(requireContext(), filter))
        binding?.actionFab?.isVisible = filter == Filter.Active && latestInfos.isNotEmpty()
    }
}
