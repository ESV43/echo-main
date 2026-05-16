package dev.brahmkshatriya.echo.ui.player

import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.ThumbRating
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.serverWithDownloads
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourceIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerCommands.addToNextCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.addToQueueCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.playCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.radioCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.resumeCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.sleepTimer
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.getController
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.settings.V4LabFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.listenFuture
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@OptIn(UnstableApi::class)
class PlayerViewModel(
    val app: App,
    val playerState: PlayerState,
    val settings: SharedPreferences,
    val cache: SimpleCache,
    val extensions: ExtensionLoader,
    downloader: Downloader,
) : ViewModel() {
    private val downloadFlow = downloader.flow

    val browser = MutableStateFlow<MediaController?>(null)
    private fun withBrowser(block: suspend (MediaController) -> Unit) {
        viewModelScope.launch {
            val browser = browser.first { it != null }!!
            block(browser)
        }
    }

    var queue: List<MediaItem> = emptyList()
    val queueFlow = MutableSharedFlow<Unit>()
    private val context = app.context
    private var playerUiListener: PlayerUiListener? = null
    val controllerFutureRelease = getController(context) { player ->
        browser.value = player
        PlayerUiListener(player, this).also {
            playerUiListener = it
            player.addListener(it)
        }

        if (player.mediaItemCount != 0) return@getController
        if (!settings.getBoolean(KEEP_QUEUE, true)) return@getController

        player.sendCustomCommand(resumeCommand, Bundle.EMPTY)
    }

    override fun onCleared() {
        super.onCleared()
        playerUiListener?.let {
            browser.value?.removeListener(it)
        }
        controllerFutureRelease()
    }

    fun play(position: Int) {
        withBrowser {
            it.seekTo(position, 0)
            it.playWhenReady = true
        }
    }

    fun seek(position: Int) {
        withBrowser { it.seekTo(position, 0) }
    }

    fun removeQueueItem(position: Int) {
        withBrowser { it.removeMediaItem(position) }
    }

    fun moveQueueItems(fromPos: Int, toPos: Int) {
        withBrowser { it.moveMediaItem(fromPos, toPos) }
    }

    fun clearQueue() {
        withBrowser { it.clearMediaItems() }
    }

    fun applySmartQueue() {
        withBrowser { controller ->
            val currentIndex = controller.currentMediaItemIndex.takeIf { it >= 0 } ?: return@withBrowser
            val list = controller.mediaItems()
            val upcoming = list.drop(currentIndex + 1)
            if (upcoming.size < 2) return@withBrowser

            val mode = settings.getString(V4LabFragment.Keys.SMART_QUEUE_MODE, "vibe") ?: "vibe"
            val current = list.getOrNull(currentIndex)
            val reordered = when (mode) {
                "no_repeats" -> upcoming.spreadByArtist()
                "energy_ramp" -> upcoming.sortedBy { it.track.duration ?: Long.MAX_VALUE }
                "chill_down" -> upcoming.sortedByDescending { it.track.duration ?: 0L }
                "deep_cuts" -> upcoming.sortedWith(
                    compareBy<MediaItem> { it.track.plays ?: 0L }
                        .thenBy { it.track.playlistAddedDate?.toString().orEmpty() }
                )
                else -> upcoming.sameVibeAs(current)
            }
            controller.replaceQueue(list.take(currentIndex + 1) + reordered, currentIndex)
            app.messageFlow.emit(Message(context.getString(R.string.v4_smart_queue_applied)))
        }
    }

    fun dedupeQueue() {
        withBrowser { controller ->
            val currentIndex = controller.currentMediaItemIndex.takeIf { it >= 0 } ?: return@withBrowser
            val list = controller.mediaItems()
            val deduped = buildList {
                val seen = mutableSetOf<String>()
                list.forEachIndexed { index, item ->
                    val key = item.fusionKey()
                    if (index == currentIndex || seen.add(key)) add(item)
                }
            }
            if (deduped.size == list.size) return@withBrowser
            val newIndex = deduped.indexOfFirst { it.mediaId == list[currentIndex].mediaId }
                .takeIf { it >= 0 } ?: currentIndex.coerceAtMost(deduped.lastIndex)
            controller.replaceQueue(deduped, newIndex)
            app.messageFlow.emit(
                Message(context.getString(R.string.v4_queue_deduped, list.size - deduped.size))
            )
        }
    }

    fun fuseQueueSources() {
        withBrowser { controller ->
            val currentIndex = controller.currentMediaItemIndex.takeIf { it >= 0 } ?: return@withBrowser
            val list = controller.mediaItems()
            val fused = list
                .groupBy { it.fusionKey() }
                .values
                .map { group ->
                    group.maxWithOrNull(
                        compareBy<MediaItem> { it.track.servers.size }
                            .thenBy { it.track.streamables.size }
                            .thenBy { if (it.extensionId == list[currentIndex].extensionId) 1 else 0 }
                    ) ?: group.first()
                }
            if (fused.size == list.size) return@withBrowser
            val newIndex = fused.indexOfFirst { it.mediaId == list[currentIndex].mediaId }
                .takeIf { it >= 0 } ?: currentIndex.coerceAtMost(fused.lastIndex)
            controller.replaceQueue(fused, newIndex)
            app.messageFlow.emit(
                Message(context.getString(R.string.v4_queue_sources_fused, list.size - fused.size))
            )
        }
    }

    fun seekTo(pos: Long) {
        withBrowser { it.seekTo(pos) }
    }

    fun seekToAdd(position: Int) {
        withBrowser { it.seekTo(max(0, it.currentPosition + position)) }
    }

    fun setPlaying(isPlaying: Boolean) {
        withBrowser {
            it.prepare()
            it.playWhenReady = isPlaying
        }
    }

    fun next() {
        withBrowser { it.seekToNextMediaItem() }
    }

    fun previous() {
        withBrowser { it.seekToPrevious() }
    }

    fun setShuffle(isShuffled: Boolean, changeCurrent: Boolean = false) {
        withBrowser {
            it.shuffleModeEnabled = isShuffled
            if (changeCurrent) it.seekTo(0, 0)
        }
    }

    fun setRepeat(repeatMode: Int) {
        withBrowser { it.repeatMode = repeatMode }
    }

    suspend fun isLikeClient(extensionId: String): Boolean = withContext(Dispatchers.IO) {
        extensions.music.getExtension(extensionId)?.isClient<LikeClient>() ?: false
    }

    private fun createException(throwable: Throwable) {
        viewModelScope.launch { app.throwFlow.emit(throwable) }
    }

    fun likeCurrent(isLiked: Boolean) = withBrowser { controller ->
        val future = controller.setRating(ThumbRating(isLiked))
        app.context.listenFuture(future) { sessionResult ->
            sessionResult.getOrElse { createException(it) }
        }
    }

    fun setSleepTimer(timer: Long) {
        withBrowser { it.sendCustomCommand(sleepTimer, bundleOf("ms" to timer)) }
    }

    fun changeTrackSelection(trackGroup: TrackGroup, index: Int) {
        withBrowser {
            it.trackSelectionParameters = it.trackSelectionParameters
                .buildUpon()
                .clearOverride(trackGroup)
                .addOverride(TrackSelectionOverride(trackGroup, index))
                .build()
        }
    }

    private fun changeCurrent(newItem: MediaItem) {
        withBrowser { player ->
            val oldPosition = player.currentPosition
            player.replaceMediaItem(player.currentMediaItemIndex, newItem)
            player.prepare()
            player.seekTo(oldPosition)
        }
    }

    fun changeServer(server: Streamable) {
        val item = playerState.current.value?.mediaItem ?: return
        val index = item.serverWithDownloads(app.context).indexOf(server).takeIf { it != -1 }
            ?: return
        changeCurrent(MediaItemUtils.buildServer(item, index))
    }

    fun changeBackground(background: Streamable?) {
        val item = playerState.current.value?.mediaItem ?: return
        val index = item.track.backgrounds.indexOf(background)
        changeCurrent(MediaItemUtils.buildBackground(item, index))
    }

    fun changeSubtitle(subtitle: Streamable?) {
        val item = playerState.current.value?.mediaItem ?: return
        val index = item.track.subtitles.indexOf(subtitle)
        changeCurrent(MediaItemUtils.buildSubtitle(item, index))
    }

    fun changeCurrentSource(index: Int) {
        val item = playerState.current.value?.mediaItem ?: return
        changeCurrent(MediaItemUtils.buildSource(item, index))
    }

    fun setQueue(id: String, list: List<Track>, index: Int, context: EchoMediaItem?) {
        withBrowser { controller ->
            val mediaItems = list.map {
                MediaItemUtils.build(
                    app,
                    downloadFlow.value,
                    MediaState.Unloaded(id, it),
                    context
                )
            }
            controller.setMediaItems(mediaItems, index, list[index].playedDuration ?: 0)
            controller.prepare()
        }
    }

    fun radio(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        app.messageFlow.emit(
            Message(app.context.getString(R.string.loading_radio_for_x, item.title))
        )
        withBrowser {
            it.sendCustomCommand(radioCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
            })
        }
    }

    fun play(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        if (item !is Track) app.messageFlow.emit(
            Message(app.context.getString(R.string.playing_x, item.title))
        )
        withBrowser {
            it.sendCustomCommand(playCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
                putBoolean("shuffle", false)
            })
        }
    }

    fun shuffle(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        if (item !is Track) app.messageFlow.emit(
            Message(app.context.getString(R.string.shuffling_x, item.title))
        )
        withBrowser {
            it.sendCustomCommand(playCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
                putBoolean("shuffle", true)
            })
        }
    }


    fun addToQueue(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        if (item !is Track) app.messageFlow.emit(
            Message(app.context.getString(R.string.adding_x_to_queue, item.title))
        )
        withBrowser {
            it.sendCustomCommand(addToQueueCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
            })
        }
    }

    fun addToNext(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        if (!(browser.value?.mediaItemCount == 0 && item is Track)) app.messageFlow.emit(
            Message(app.context.getString(R.string.adding_x_to_next, item.title))
        )
        withBrowser {
            it.sendCustomCommand(addToNextCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
            })
        }
    }

    val progress = MutableStateFlow(0L to 0L)
    val discontinuity = MutableStateFlow(0L)
    val totalDuration = MutableStateFlow<Long?>(null)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(0)
    val shuffleMode = MutableStateFlow(false)

    val tracksFlow = MutableStateFlow<Tracks?>(null)
    val serverAndTracks = tracksFlow.combine(playerState.serverChanged) { tracks, _ -> tracks }
        .combine(playerState.current) { tracks, current ->
            val server = playerState.servers[current?.mediaItem?.mediaId]?.getOrNull()
            val index = current?.mediaItem?.sourceIndex
            Triple(tracks, server, index)
        }.stateIn(viewModelScope, SharingStarted.Lazily, Triple(null, null, null))

    companion object {
        const val KEEP_QUEUE = "keep_queue"
    }
}

private fun MediaController.mediaItems() = (0 until mediaItemCount).map { getMediaItemAt(it) }

private fun MediaController.replaceQueue(items: List<MediaItem>, currentIndex: Int) {
    val position = currentPosition
    setMediaItems(items, currentIndex.coerceIn(items.indices), position)
    prepare()
}

private fun MediaItem.fusionKey(): String {
    val track = track
    track.isrc?.takeIf { it.isNotBlank() }?.let { return "isrc:${it.lowercase()}" }
    return listOf(
        track.title.normalizedForMatch(),
        track.artists.joinToString(",") { it.name.normalizedForMatch() },
        track.duration?.div(5000)
    ).joinToString("|")
}

private fun String.normalizedForMatch() = lowercase()
    .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), "")
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

private fun MediaItem.primaryArtist() =
    track.artists.firstOrNull()?.name?.normalizedForMatch().orEmpty()

private fun List<MediaItem>.spreadByArtist(): List<MediaItem> {
    val buckets = groupBy { it.primaryArtist() }
        .values
        .map { it.toMutableList() }
        .sortedByDescending { it.size }
        .toMutableList()
    return buildList {
        while (buckets.any { it.isNotEmpty() }) {
            buckets.sortByDescending { it.size }
            val bucket = buckets.firstOrNull { it.isNotEmpty() } ?: break
            add(bucket.removeAt(0))
        }
    }
}

private fun List<MediaItem>.sameVibeAs(current: MediaItem?): List<MediaItem> {
    val currentTrack = current?.track
    val currentGenres = currentTrack?.genres.orEmpty().map { it.normalizedForMatch() }.toSet()
    val currentArtists = currentTrack?.artists.orEmpty().map { it.name.normalizedForMatch() }.toSet()
    val currentAlbum = currentTrack?.album?.title?.normalizedForMatch()
    val currentSource = current?.extensionId
    return sortedByDescending { item ->
        val track = item.track
        val genreScore = track.genres.count { it.normalizedForMatch() in currentGenres } * 4
        val artistScore = track.artists.count { it.name.normalizedForMatch() in currentArtists } * 3
        val albumScore = if (track.album?.title?.normalizedForMatch() == currentAlbum) 2 else 0
        val sourceScore = if (item.extensionId == currentSource) 1 else 0
        genreScore + artistScore + albumScore + sourceScore
    }
}
