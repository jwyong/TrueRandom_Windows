package com.truerandom.db.repository

import com.truerandom.db.dao.TrackDao
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.db.model.LikedSongsResponse
import com.truerandom.storage.DataStoreProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TrackDbRepository(private val trackDao: TrackDao) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Checks if local database is empty. If so, fetches tracks from Spotify and caches them.
     */
    suspend fun checkAndFetchLikedSongs(accessToken: String) {
        val count = trackDao.getTrackCount()

        if (count == 0) {
            println("Syncing all liked songs...")

            var currentOffset = 0
            var totalTracks = 1 // Placeholder to start the loop

            while (currentOffset < totalTracks) {
                val response = fetchPage(accessToken, currentOffset) ?: break

                totalTracks = response.total // Update total from Spotify
                val pageTracks = response.items.mapNotNull { it.track }.map { track ->
                    LikedTrackEntity(
                        trackUri = track.uri ?: "",
                        trackName = track.name,
                        artistName = track.artists.joinToString(", ") { it.name ?: "" },
                        isLocal = false,
                        isPlayable = true,
                        addedAt = null,
                        albumCoverUrl = null
                    )
                }

                // Insert into DB in batches of 50 for better performance and UI updates
                trackDao.insertAll(pageTracks)

                currentOffset += pageTracks.size
                println("Progress: $currentOffset / $totalTracks tracks synced.")

                // Safety: If Spotify returns 0 items but claims a total, break to avoid infinite loop
                if (pageTracks.isEmpty()) break
            }

            println("Full sync complete.")
        } else {
            println("Database already contains $count tracks.")
        }
    }

    private suspend fun fetchPage(accessToken: String, offset: Int): LikedSongsResponse? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
        }
        return try {
            val httpResponse: HttpResponse = client.get("https://api.spotify.com/v1/me/tracks") {
                header("Authorization", "Bearer $accessToken")
                parameter("limit", 50)
                parameter("offset", offset) // Start from this position
            }

            if (httpResponse.status.isSuccess()) {
                json.decodeFromString<LikedSongsResponse>(httpResponse.bodyAsText())
            } else null
        } catch (e: Exception) {
            null
        } finally {
            client.close()
        }
    }

//    here
    suspend fun playRandomLeastPlayedTrack() {
        // 1. Get the list from the DAO
        val leastPlayedUris = trackDao.getLeastPlayedTrackUris()

        // 2. Pick a random URI safely
        val randomUri = leastPlayedUris.randomOrNull()

        // 3. Handle the result
        if (randomUri != null) {
            // Pass this URI to your MediaPlayer/Spotify controller
            println("Now playing random least played track: $randomUri")

            // TODO: JAY_LOG - just play this track for now
            playTrack(DataStoreProvider.getAccessToken()?: return, randomUri)
        } else {
            println("No tracks found in the database to play.")
        }
    }

    /**
     * Starts playback of a specific track on the user's active device.
     */
    suspend fun playTrack(accessToken: String, trackUri: String): Boolean {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        return try {
            println("Attempting to play track: $trackUri")
            val response: HttpResponse = client.put("https://api.spotify.com/v1/me/player/play") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(PlayRequest(uris = listOf(trackUri)))
            }

            if (response.status == HttpStatusCode.NoContent) {
                println("Playback started successfully.")
                trackDao.incrementPlayCount(trackUri) // Track local usage
                true
            } else {
                val errorBody = response.bodyAsText()
                println("Failed to play track. Status: ${response.status}. Body: $errorBody")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            client.close()
        }
    }
}

@Serializable
private data class PlayRequest(
    val uris: List<String>
)
