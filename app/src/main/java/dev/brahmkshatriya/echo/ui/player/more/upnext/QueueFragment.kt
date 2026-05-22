package dev.brahmkshatriya.echo.ui.player.more.upnext

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentPlayerQueueBinding
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoClearedNullable
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class QueueFragment : Fragment() {

    private var binding by autoClearedNullable<FragmentPlayerQueueBinding>()
    private val viewModel by activityViewModel<PlayerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerQueueBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    private var queueAdapter: QueueAdapter? = null

    private val touchHelper by lazy {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                viewModel.moveQueueItems(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                viewModel.removeQueueItem(pos)
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.START
                )
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view, false, axis = MaterialSharedAxis.Y)

        val binding = binding!!
        val adapter = QueueAdapter(object : QueueAdapter.Listener() {
            override fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }

            override fun onItemClicked(position: Int) {
                val a = queueAdapter ?: return
                if (a.selectionMode) {
                    a.toggleSelection(position)
                } else {
                    viewModel.play(position)
                }
            }

            override fun onItemClosedClicked(position: Int) {
                viewModel.removeQueueItem(position)
            }

            override fun onItemLongClicked(position: Int) {
                val a = queueAdapter ?: return
                if (!a.selectionMode) {
                    a.selectionMode = true
                    a.toggleSelection(position)
                }
            }
        })
        queueAdapter = adapter
        val recyclerView = binding.queueList
        recyclerView.adapter = adapter
        touchHelper.attachToRecyclerView(recyclerView)
        val manager = recyclerView.layoutManager as LinearLayoutManager
        val screenHeight = view.resources.displayMetrics.heightPixels / 3

        fun submit() {
            val a = queueAdapter ?: return
            val current = viewModel.playerState.current.value
            val currentIndex = current?.index
            val items = viewModel.queue.mapIndexed { index, mediaItem ->
                if (currentIndex == index) current.isPlaying to current.mediaItem
                else null to mediaItem
            }
            binding.emptyView.isVisible = items.isEmpty()
            binding.queueActions.isVisible = items.isNotEmpty()
            a.submitList(items) {
                currentIndex ?: return@submitList
                binding.queueList.scrollToPosition(currentIndex)
            }
        }

        binding.queueActions.setOnClickListener { btn ->
            val a = queueAdapter ?: return@setOnClickListener
            val popup = android.widget.PopupMenu(requireContext(), btn)
            popup.menu.add(0, 1, 0, R.string.v4_smart_queue_short)
            popup.menu.add(0, 2, 0, R.string.v4_dedupe_short)
            popup.menu.add(0, 3, 0, R.string.v4_fuse_sources_short)
            popup.menu.add(0, 6, 0, R.string.v4_save_queue)
            if (a.selectionMode) {
                popup.menu.add(0, 5, 0, R.string.v4_select_none)
                val selectedCount = a.selectedItems.size
                if (selectedCount > 0) {
                    popup.menu.add(0, 4, 0,
                        getString(R.string.v4_remove_selected_x, selectedCount))
                }
            } else {
                popup.menu.add(0, 5, 0, R.string.v4_select)
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> viewModel.applySmartQueue()
                    2 -> viewModel.dedupeQueue()
                    3 -> viewModel.fuseQueueSources()
                    6 -> {
                        val input = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        input.setTitle(R.string.v4_save_queue)
                        val editText = android.widget.EditText(requireContext()).apply {
                            setHint(R.string.playlist_name)
                            setText("Queue ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date())}")
                        }
                        input.setView(editText)
                        input.setPositiveButton(R.string.save) { _, _ ->
                            viewModel.saveQueueAsPlaylist(editText.text.toString())
                        }
                        input.setNegativeButton(R.string.cancel, null)
                        input.show()
                    }
                    4 -> {
                        val indices = a.selectedItems.sortedDescending()
                        indices.forEach { viewModel.removeQueueItem(it) }
                        a.clearSelection()
                        a.selectionMode = false
                        submit()
                    }
                    5 -> {
                        a.selectionMode = !a.selectionMode
                        if (!a.selectionMode) a.clearSelection()
                        submit()
                    }
                }
                true
            }
            popup.show()
        }

        observe(viewModel.playerState.current) { submit() }
        observe(viewModel.queueFlow) { submit() }

        val index = viewModel.playerState.current.value?.index ?: return
        manager.scrollToPositionWithOffset(index + 1, screenHeight)
    }
}
