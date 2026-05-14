package dev.brahmkshatriya.echo.extensions

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object ExtensionSandboxInspector {

    data class Failure(val extensionId: String, val error: Throwable, val timestamp: Long = System.currentTimeMillis())

    val recentFailures = MutableStateFlow<List<Failure>>(emptyList())

    fun reportFailure(extensionId: String, error: Throwable) {
        recentFailures.update { (listOf(Failure(extensionId, error)) + it).take(50) }
    }

    fun inspect(extension: MusicExtension): String {
        val meta = extension.metadata
        val clients = mutableListOf<String>()
        val instance = extension.instance.value().getOrNull()
        
        if (instance is LoginClient) clients.add("Login")
        if (instance is HomeFeedClient) clients.add("Home")
        if (instance is SearchFeedClient) clients.add("Search")
        if (instance is LibraryFeedClient) clients.add("Library")
        if (instance is TrackClient) clients.add("Playback")
        if (instance is LyricsClient) clients.add("Lyrics")
        
        val failures = recentFailures.value.count { it.extensionId == meta.id }
        
        return """
            Name: ${meta.name}
            Version: ${meta.version}
            ID: ${meta.id}
            Capabilities: ${clients.joinToString(", ")}
            Recent Failures: $failures
        """.trimIndent()
    }
}
