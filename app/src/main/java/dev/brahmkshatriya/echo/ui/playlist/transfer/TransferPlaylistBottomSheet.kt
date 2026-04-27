package dev.brahmkshatriya.echo.ui.playlist.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.BottomSheetTransferPlaylistBinding
import dev.brahmkshatriya.echo.databinding.ItemExtensionButtonBinding
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.media.MediaFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TransferPlaylistBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(sourceExtensionId: String, playlist: Playlist) =
            TransferPlaylistBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("sourceExtensionId", sourceExtensionId)
                    putSerialized("playlist", playlist)
                }
            }
    }

    private var binding by autoCleared<BottomSheetTransferPlaylistBinding>()
    private val extensionLoader by inject<ExtensionLoader>()
    private val sourceExtensionId by lazy { requireArguments().getString("sourceExtensionId")!! }
    private val playlist by lazy { requireArguments().getSerialized<Playlist>("playlist")!!.getOrThrow() }

    private val viewModel by viewModel<TransferPlaylistViewModel> {
        parametersOf(sourceExtensionId, playlist)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetTransferPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener { dismiss() }

        observe(extensionLoader.music) { list ->
            val targets = list.filter {
                it.id != sourceExtensionId && it.isEnabled &&
                        it.isClient<PlaylistEditClient>() && it.isClient<SearchFeedClient>()
            }

            if (targets.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_target_extensions, Toast.LENGTH_SHORT).show()
                dismiss()
                return@observe
            }

            binding.extensionToggleGroup.removeAllViews()
            targets.forEach { extension ->
                val button = ItemExtensionButtonBinding.inflate(layoutInflater, binding.extensionToggleGroup, false).root
                button.text = extension.name
                extension.metadata.icon.loadAsCircle(button) {
                    if (it != null) {
                        button.icon = it
                        button.iconTint = null
                    } else button.setIconResource(R.drawable.ic_extension_32dp)
                }
                button.setOnClickListener {
                    viewModel.startTransfer(extension.id)
                }
                binding.extensionToggleGroup.addView(button)
            }
        }

        observe(viewModel.stateFlow) { state ->
            binding.extensionToggleGroup.isVisible = state is TransferPlaylistViewModel.State.SelectTarget
            binding.transferProgressContainer.isVisible = state is TransferPlaylistViewModel.State.Transferring
            binding.transferCompleteContainer.isVisible = state is TransferPlaylistViewModel.State.Complete

            when (state) {
                is TransferPlaylistViewModel.State.Transferring -> {
                    binding.progressIndicator.max = state.total
                    binding.progressIndicator.progress = state.current
                    binding.statusText.text = getString(R.string.transferring_x_of_y, state.current, state.total)
                    binding.currentTrackText.text = getString(R.string.searching_for_x, state.currentTrack)
                }
                is TransferPlaylistViewModel.State.Complete -> {
                    binding.resultSummaryText.text = getString(R.string.matched_x_of_y, state.matched, state.total)
                    binding.btnViewPlaylist.setOnClickListener {
                        requireActivity().openFragment<MediaFragment>(
                            null, MediaFragment.getBundle(state.newPlaylist.extras["extensionId"] ?: "", state.newPlaylist, true)
                        )
                        dismiss()
                    }
                }
                is TransferPlaylistViewModel.State.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    dismiss()
                }
                else -> {}
            }
        }
    }
}
