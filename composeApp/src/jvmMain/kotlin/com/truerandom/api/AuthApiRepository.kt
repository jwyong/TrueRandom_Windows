package com.truerandom.api

import com.truerandom.CLIENT_ID
import com.truerandom.CLIENT_SECRET
import com.truerandom.db.model.SpotifyTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

object AuthApiRepository {
    private var currentCodeVerifier: String? = null

    // Start auth to get accessToken and refreshToken - this step needed if no valid refresh token
    fun startSpotifyAuth(clientId: String, redirectUri: String, scopes: String) {
        val encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString())
        val encodedScopes = URLEncoder.encode(scopes, StandardCharsets.UTF_8.toString())

        // 1. Generate PKCE values
        val verifier = generateCodeVerifier()
        currentCodeVerifier = verifier
        val challenge = generateCodeChallenge(verifier)

        // 2. Build the Auth URL with PKCE parameters
        // 2. Construct the URL using the encoded values
        val authUrl = "https://accounts.spotify.com/authorize?" +
                "client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=$encodedRedirect" +
                "&scope=$encodedScopes" +
                "&code_challenge_method=S256" +
                "&code_challenge=$challenge"

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(authUrl))
        }
    }

    // Use refresh token to get new access token
    suspend fun refreshAccessToken(savedRefreshToken: String): SpotifyTokenResponse? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        return try {
            val httpResponse = client.post("https://accounts.spotify.com/api/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", savedRefreshToken)
                    append("client_id", CLIENT_ID)
                    append("client_secret", CLIENT_SECRET)
                }))
            }

            if (httpResponse.status.value in 200..299) {
                httpResponse.body<SpotifyTokenResponse>()
            } else {
                // This will show you the REAL error from Spotify (e.g., "Invalid refresh token")
                val errorBody = httpResponse.bodyAsText()
                println("Spotify Error (${httpResponse.status}): $errorBody")
                null
            }
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            null
        } finally {
            client.close()
        }
    }

    fun getVerifier() = currentCodeVerifier

    private fun generateCodeVerifier(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + '-' + '.' + '_' + '~'
        return (1..64).map { allowedChars.random() }.joinToString("")
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}