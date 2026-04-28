package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network

import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.AlbumResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.ArtistResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.AuthResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.FavouriteResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.FeaturedResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.GenericResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.LibrariesResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.LibraryResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.LoginRequest
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.RegisterRequest
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.SearchResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.Stream
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Response

class ApiService(client: OkHttpClient, json: Json) : BaseHttpClient(client, json) {

    suspend fun getAlbum(baseUrl: String, id: String): AlbumResponse {
        return get(
            url = "${baseUrl}/album",
            params = mapOf("albumId" to id)
        )
    }

    suspend fun getArtist(baseUrl: String, id: String): ArtistResponse {
        return get(
            url = "${baseUrl}/discography",
            params = mapOf("artistId" to id)
        )
    }

    suspend fun search(
        baseUrl: String,
        query: String,
        offset: Int = 0,
        type: String,
        session: String? = null
    ): SearchResponse {
        return get(
            url = "${baseUrl}/search",
            params = mapOf(
                "query" to query,
                "offset" to offset.toString(),
                "type" to type
            ),
            headers = session?.let { mapOf("Cookie" to it) }
        )
    }

    suspend fun getStream(baseUrl: String, trackId: String): Stream {
        return get(
            url = "${baseUrl}/stream",
            params = mapOf("trackId" to trackId)
        )
    }

    suspend fun login(baseUrl: String, username: String, password: String): Response {
        return postResponse(
            url = "${baseUrl}/auth/login",
            jsonBody = LoginRequest(username, password).toJsonString()
        )
    }

    suspend fun register(baseUrl: String, username: String, email: String, password: String, inviteCode: String?): Response {
        return postResponse(
            url = "${baseUrl}/auth/register",
            jsonBody = RegisterRequest(username, email, password, inviteCode).toJsonString()
        )
    }

    suspend fun getPlaylists(baseUrl: String, session: String): LibrariesResponse {
        return get(
            url = "${baseUrl}/libraries",
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun getPlaylist(baseUrl: String, id: String, session: String, page: Int = 1, limit: Int? = null): LibraryResponse {
        return get(
            url = "${baseUrl}/libraries/${id}",
            params = mapOf(
                "page" to page.toString(),
                "limit" to (limit ?: "").toString()
            ),
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun getFeaturedAlbums(baseUrl: String, type: String, offset: Int? = 0, limit: Int? = 25, session: String?): FeaturedResponse {
        return get(
            url = "${baseUrl}/featured-albums",
            params = mapOf(
                "type" to type,
                "offset" to (offset ?: "").toString(),
                "limit" to (limit ?: "").toString(),
            ),
            headers = session?.let { mapOf("Cookie" to it) }
        )
    }

    suspend fun getAuth(baseUrl: String, session: String): AuthResponse {
        return get(
            url = "${baseUrl}/auth/me",
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun createLibrary(baseUrl: String, json: String, session: String): LibraryResponse {
        return post<LibraryResponse>(
            url = "${baseUrl}/libraries",
            jsonBody = json,
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun editLibraryMetadata(baseUrl: String, id: String, json: String, session: String): GenericResponse {
        return patch<GenericResponse>(
            url = "${baseUrl}/libraries/${id}",
            jsonBody = json,
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun deleteLibrary(baseUrl: String, id: String, session: String): GenericResponse {
        return delete(
            url = "${baseUrl}/libraries/${id}",
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun addToLibrary(baseUrl: String, id: String, json: String, session: String): GenericResponse {
        return post(
            url = "${baseUrl}/libraries/${id}/tracks",
            jsonBody = json,
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun removeFromLibrary(baseUrl: String, id: String, trackId: String, session: String): GenericResponse {
        return delete(
            url = "${baseUrl}/libraries/${id}/tracks/${trackId}",
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun getFavourites(baseUrl: String, session: String): FavouriteResponse {
        return get(
            url = "${baseUrl}/favorites",
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun addFavourite(baseUrl: String, json: String, session: String): GenericResponse {
        return post(
            url = "${baseUrl}/favorites",
            jsonBody = json,
            headers = mapOf("Cookie" to session)
        )
    }

    suspend fun removeFavourite(baseUrl: String, id: String, session: String): GenericResponse {
        return delete(
            url = "${baseUrl}/favorites",
            params = mapOf("trackId" to id),
            headers = mapOf("Cookie" to session)
        )
    }
}