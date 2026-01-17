package com.truerandom.api

import com.truerandom.model.DeviceResponse
import com.truerandom.model.LikedSongsResponse
import com.truerandom.model.PlayRequest
import com.truerandom.model.PlayerStateResponse
import com.truerandom.model.SpotifyErrorResponse
import com.truerandom.util.JsonUtil
import com.truerandom.util.JsonUtil.jsonObj
import com.truerandom.util.Resource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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

class TrackApiRepository {
    // Fetch the FULL list of liked tracks by paging (max 50 per)
    suspend fun fetchLikedTracksPaged(accessToken: String, offset: Int): LikedSongsResponse? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonObj) }
        }
        return try {
            val httpResponse: HttpResponse = client.get("https://api.spotify.com/v1/me/tracks") {
                header("Authorization", "Bearer $accessToken")
                parameter("limit", 50)
                parameter("offset", offset) // Start from this position
            }

            if (httpResponse.status.isSuccess()) {
                JsonUtil.jsonObj.decodeFromString<LikedSongsResponse>(httpResponse.bodyAsText())
            } else null
        } catch (e: Exception) {
            null
        } finally {
            client.close()
        }
    }

    // Play a specific trackUri on spotify (from start of track)
    suspend fun playTrackFromStart(accessToken: String, trackUri: String, deviceId: String? = null): Resource<SpotifyErrorResponse> {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonObj) }
        }
        return try {
            println("Attempting to play track: $trackUri")
            val response: HttpResponse = client.put("https://api.spotify.com/v1/me/player/play") {
                header("Authorization", "Bearer $accessToken")
                if (deviceId != null) {
                    parameter("device_id", deviceId)
                }
                contentType(ContentType.Application.Json)
                setBody(PlayRequest(uris = listOf(trackUri)))
            }
            println("playTrackFromStart: response.status = ${response.status}")

            if (response.status == HttpStatusCode.NoContent) {
                Resource.Success()
            } else {
                val errorBody = response.body<SpotifyErrorResponse>()
                println("Failed to play track. Status: ${response.status}. Body: $errorBody")
                Resource.Error(errorBody)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(message = e.message)
        } finally {
            client.close()
        }
    }

    suspend fun pausePlayback(accessToken: String): Resource<SpotifyErrorResponse> {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonObj) }
        }
        return try {
            val response: HttpResponse = client.put("https://api.spotify.com/v1/me/player/pause") {
                header("Authorization", "Bearer $accessToken")
            }

            // Spotify returns 204 NoContent if the pause was successful
            if (response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK) {
                Resource.Success()
            } else {
                val errorBody = response.body<SpotifyErrorResponse>()
                println("Failed to pause. Status: ${response.status}. Body: $errorBody")
                Resource.Error(errorBody)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(message = e.message)
        } finally {
            client.close()
        }
    }

    suspend fun resumePlayback(accessToken: String): Boolean {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonObj) }
        }
        return try {
            // Same URL as Play, but with NO body
            val response: HttpResponse = client.put("https://api.spotify.com/v1/me/player/play") {
                header("Authorization", "Bearer $accessToken")
                // No setBody() call here!
            }

            if (response.status == HttpStatusCode.NoContent) {
                println("Playback resumed.")
                true
            } else {
                val errorBody = response.bodyAsText()
                println("Failed to resume. Status: ${response.status}. Body: $errorBody")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            client.close()
        }
    }

    // Get device id of the active spotify app instance available
    suspend fun getActiveDeviceId(accessToken: String): String? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonObj) }
        }
        return try {
            val response: HttpResponse = client.get("https://api.spotify.com/v1/me/player/devices") {
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status == HttpStatusCode.OK) {
                val deviceResponse = response.body<DeviceResponse>()
                // Find the first active device, or the first available device if none are active
                deviceResponse.devices.find { it.isActive }?.id
                    ?: deviceResponse.devices.firstOrNull()?.id
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            client.close()
        }
    }

    suspend fun getPlayerState(accessToken: String): PlayerStateResponse? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonObj) }
        }
        return try {
            val response: HttpResponse = client.get("https://api.spotify.com/v1/me/player") {
                header("Authorization", "Bearer $accessToken")
            }
            println("Player state response: ${response.status}")
            if (response.status == HttpStatusCode.OK) {
                response.body<PlayerStateResponse>()
            } else null
        } catch (e: Exception) { null }
    }
}