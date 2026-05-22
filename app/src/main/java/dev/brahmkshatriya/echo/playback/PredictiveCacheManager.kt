package dev.brahmkshatriya.echo.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.SimpleCache
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.source.StreamableLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class PredictiveCacheManager(
    private val cache: SimpleCache,
    private val loader: StreamableLoader,
    private val scope: CoroutineScope,
    private val servers: ConcurrentHashMap<String, Result<Streamable.Media.Server>>
) {
    private var cacheJob: Job? = null
    private var lastCachedMediaId: String? = null

    fun checkAndPreFetch(player: Player) {
        val duration = player.duration
        val position = player.currentPosition
        if (duration <= 0) return

        val progress = position.toFloat() / duration
        if (progress >= 0.7f) {
            val nextIndex = player.currentMediaItemIndex + 1
            if (nextIndex < player.mediaItemCount) {
                val nextItem = player.getMediaItemAt(nextIndex)
                if (nextItem.mediaId != lastCachedMediaId) {
                    lastCachedMediaId = nextItem.mediaId
                    preFetch(nextItem)
                }
            }
        }
    }

    private fun preFetch(mediaItem: MediaItem) {
        cacheJob?.cancel()
        cacheJob = scope.launch(Dispatchers.IO) {
            try {
                // 1. Load servers using the streamable loader
                val (_, serverResult) = loader.load(mediaItem)
                val server = serverResult.getOrNull() ?: return@launch
                
                // Store the result so StreamableResolver doesn't have to resolve it again
                servers[mediaItem.mediaId] = serverResult

                val firstSource = server.sources.firstOrNull() ?: return@launch
                if (firstSource is Streamable.Source.Http) {
                    val url = firstSource.request.url
                    val headers = firstSource.request.headers

                    val dataSpec = DataSpec.Builder()
                        .setUri(Uri.parse(url))
                        .setHttpRequestHeaders(headers)
                        .setLength(2500000L) // Cache first 2.5 MB for instant startup
                        .build()

                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)

                    val cacheDataSource = CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(httpDataSourceFactory)
                        .createDataSource()

                    val cacheWriter = CacheWriter(
                        cacheDataSource,
                        dataSpec,
                        null,
                        null
                    )
                    
                    cacheWriter.cache()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancel() {
        cacheJob?.cancel()
    }
}
