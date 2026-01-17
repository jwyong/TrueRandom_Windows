package com.truerandom.api

import com.truerandom.db.model.LikedSongsResponse
import com.truerandom.db.model.PlayRequest
import com.truerandom.util.JsonUtil
import com.truerandom.util.JsonUtil.jsonObj
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

class TrackApiRepository {

    // Fetch the FULL list of liked tracks by paging (max 50 per)
    suspend fun fetchLikedTracksPaged(accessToken: String, offset: Int): LikedSongsResponse? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(JsonUtil.jsonObj) }
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

    // Play a specific trackUri on spotify
    suspend fun playTrack(accessToken: String, trackUri: String): Boolean {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(jsonObj)
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