package dev.brahmkshatriya.echo.ui.playlist.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed.Companion.loadAll
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TransferPlaylistViewModel(
    private val sourceExtensionId: String,
    private val playlist: Playlist,
    private val extensionLoader: ExtensionLoader
) : ViewModel() {

    sealed class State {
        data object SelectTarget : State()
        data class Transferring(val current: Int, val total: Int, val currentTrack: String) : State()
        data class Complete(val matched: Int, val total: Int, val newPlaylist: Playlist) : State()
        data class Error(val message: String) : State()
    }

    val stateFlow = MutableStateFlow<State>(State.SelectTarget)

    fun startTransfer(targetExtensionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val sourceExt = extensionLoader.music.value.find { it.id == sourceExtensionId }
                    ?: throw Exception("Source extension not found")
                val targetExt = extensionLoader.music.value.find { it.id == targetExtensionId }
                    ?: throw Exception("Target extension not found")

                // 1. Load source tracks
                val tracks = sourceExt.getAs<PlaylistClient, List<Track>> {
                    loadTracks(playlist).loadAll()
                }.getOrThrow()

                if (tracks.isEmpty()) {
                    stateFlow.value = State.Error("Playlist is empty")
                    return@launch
                }

                // 2. Create target playlist
                val targetPlaylist = targetExt.getAs<PlaylistEditClient, Playlist> {
                    createPlaylist(playlist.title, playlist.description)
                }.getOrThrow()

                // 3. Match and add tracks
                val matchedTracks = mutableListOf<Track>()
                tracks.forEachIndexed { index, track ->
                    stateFlow.value = State.Transferring(index + 1, tracks.size, "${track.title} - ${track.artists.firstOrNull()?.name ?: ""}")
                    
                    val query = "${track.title} ${track.artists.firstOrNull()?.name ?: ""}"
                    val searchResult = targetExt.getAs<SearchFeedClient, List<EchoMediaItem>> {
                        loadSearchFeed(query).loadAll().mapNotNull { shelf ->
                            shelf.list.filterIsInstance<EchoMediaItem>()
                        }.flatten()
                    }.getOrNull()

                    val match = searchResult?.filterIsInstance<Track>()?.firstOrNull()
                    if (match != null) {
                        matchedTracks.add(match)
                    }
                }

                if (matchedTracks.isNotEmpty()) {
                    targetExt.getAs<PlaylistEditClient, Unit> {
                        addTracksToPlaylist(targetPlaylist, emptyList(), 0, matchedTracks)
                    }.getOrThrow()
                }

                stateFlow.value = State.Complete(matchedTracks.size, tracks.size, targetPlaylist)
            }.onFailure {
                stateFlow.value = State.Error(it.message ?: "Unknown error")
            }
        }
    }
}
