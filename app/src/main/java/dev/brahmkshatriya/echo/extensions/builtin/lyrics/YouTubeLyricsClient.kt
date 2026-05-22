package dev.brahmkshatriya.echo.extensions.builtin.lyrics

import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class YouTubeLyricsClient : BaseLyricsClient() {

    override suspend fun searchTrackLyrics(clientId: String, track: Track): dev.brahmkshatriya.echo.common.models.Feed<Lyrics> {
        // This usually requires the YouTube video ID.
        // In Echo, the track id might be the YouTube ID if it's from DabYeet extension.
        val videoId = track.id
        if (videoId.length != 11) return emptyList<Lyrics>().toFeed()

        val url = "https://www.youtube.com/api/timedtext?v=$videoId&lang=en"
        val request = Request.Builder().url(url).build()
        val response = CallWait(request)
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return emptyList<Lyrics>().toFeed()
            if (body.isBlank()) return emptyList<Lyrics>().toFeed()
            
            val lyrics = Lyrics(
                id = "yt_$videoId",
                title = track.title,
                subtitle = "YouTube Transcript",
                lyrics = parseTranscript(body)
            )
            return listOf(lyrics).toFeed()
        }
        return emptyList<Lyrics>().toFeed()
    }

    private fun parseTranscript(xml: String): Lyrics.Lyric {
        val items = mutableListOf<Lyrics.Item>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xml))
            
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.name == "text") {
                    val startStr = xpp.getAttributeValue(null, "start")
                    val durStr = xpp.getAttributeValue(null, "dur")
                    val start = ((startStr?.toDoubleOrNull() ?: 0.0) * 1000).toLong()
                    val dur = ((durStr?.toDoubleOrNull() ?: 0.0) * 1000).toLong()
                    val text = xpp.nextText().trim()
                    items.add(Lyrics.Item(text, start, start + dur))
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            return Lyrics.Simple("Error parsing transcript")
        }
        
        return if (items.isEmpty()) Lyrics.Simple("") else Lyrics.Timed(items)
    }
}
