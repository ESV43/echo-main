package dev.brahmkshatriya.echo.ui.media.more

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.DialogTrackMetadataBinding
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared

class TrackMetadataBottomSheet : BottomSheetDialogFragment() {

    private var binding by autoCleared<DialogTrackMetadataBinding>()

    private var track: Track? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        track = arguments?.getSerialized<Track>("track")?.getOrNull()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogTrackMetadataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val track = track ?: return

        binding.editTitle.setText(track.title)
        binding.editArtist.setText(track.artists.firstOrNull()?.name ?: "")
        binding.editAlbum.setText(track.album?.title ?: "")
        binding.editGenre.setText(track.genres.firstOrNull() ?: "")
        binding.editTrackNumber.setText(track.albumOrderNumber?.toString() ?: "")

        binding.btnSave.setOnClickListener {
            saveMetadata(track)
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun saveMetadata(track: Track) {
        val context = requireContext()

        val title = binding.editTitle.text?.toString()?.trim().orEmpty()
        val artist = binding.editArtist.text?.toString()?.trim().orEmpty()
        val album = binding.editAlbum.text?.toString()?.trim().orEmpty()
        val trackNumber = binding.editTrackNumber.text?.toString()?.trim().orEmpty()

        val values = android.content.ContentValues().apply {
            if (title.isNotEmpty()) put(MediaStore.Audio.Media.TITLE, title)
            if (artist.isNotEmpty()) put(MediaStore.Audio.Media.ARTIST, artist)
            if (album.isNotEmpty()) put(MediaStore.Audio.Media.ALBUM, album)
            if (trackNumber.isNotEmpty()) {
                put(MediaStore.Audio.Media.TRACK, trackNumber.toIntOrNull() ?: return@apply)
            }
        }

        if (values.size() == 0) {
            context.getString(R.string.cancel)
            return
        }

        try {
            val uri = Uri.withAppendedPath(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.id
            )
            context.contentResolver.update(uri, values, null, null)
        } catch (_: Exception) { }
    }

    companion object {
        fun newInstance(track: Track): TrackMetadataBottomSheet {
            return TrackMetadataBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerialized("track", track)
                }
            }
        }
    }
}