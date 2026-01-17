package com.truerandom.api

import com.truerandom.build.AppConfig
import com.truerandom.model.SpotifyTokenResponse
import com.truerandom.util.Resource
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

private const val REDIRECT_URI = "http://127.0.0.1:8888/callback"
private val AUTH_SCOPE = listOf(
    "user-read-email",
    "user-read-private",
    "streaming",
    "user-library-read",
    "user-read-playback-state",
    "user-modify-playback-state"
).joinToString(" ")

private val CLIENT_ID = AppConfig.SPOTIFY_CLIENT_ID
private val CLIENT_SECRET = AppConfig.SPOTIFY_CLIENT_SECRET

class AuthApiRepository {
    private val json by lazy { Json { ignoreUnknownKeys = true } }

    private var currentCodeVerifier: String? = null

    // Start full auth flow (needs user consent)
    fun launchSpotifyAuthBrowser() {
        val encodedRedirect = URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8.toString())
        val encodedScopes = URLEncoder.encode(AUTH_SCOPE, StandardCharsets.UTF_8.toString())

        // 1. Generate PKCE values
        val verifier = generateCodeVerifier()
        currentCodeVerifier = verifier
        val challenge = generateCodeChallenge(verifier)

        // 2. Build the Auth URL with PKCE parameters
        // 2. Construct the URL using the encoded values
        val authUrl = "https://accounts.spotify.com/authorize?" +
                "client_id=$CLIENT_ID" +
                "&response_type=code" +
                "&redirect_uri=$encodedRedirect" +
                "&scope=$encodedScopes" +
                "&code_challenge_method=S256" +
                "&code_challenge=$challenge"

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(authUrl))
        }
    }

    /**
     * Has refresh token
     **/
    // Use refresh token to get new access token (don't need user consent)
    suspend fun refreshAccessToken(savedRefreshToken: String): Resource<SpotifyTokenResponse> {
        val client = HttpClient(CIO) {
            install(ContentNegotiation.Plugin) { json(json) }
        }

        return try {
            val httpResponse = client.post("https://accounts.spotify.com/api/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.Companion.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", savedRefreshToken)
                    append("client_id", CLIENT_ID)
                    append("client_secret", CLIENT_SECRET)
                }))
            }

            if (httpResponse.status.value in 200..299) {
                Resource.Success(httpResponse.body<SpotifyTokenResponse>())

            } else {
                // This will show you the REAL error from Spotify (e.g., "Invalid refresh token")
                val errorBody = httpResponse.bodyAsText()
                Resource.Error(httpResponse.body<SpotifyTokenResponse>(), message = errorBody)
            }
        } catch (e: Exception) {
            Resource.Error(message = "Exception: $e")

        } finally {
            client.close()
        }
    }

    /**
     * No refresh token
     **/
    // Get accessToken from temp auth code (~10 mins)
    suspend fun exchangeCodeForToken(code: String): Resource<SpotifyTokenResponse> {
        // Note: It's better to reuse a single HttpClient instance instead of creating/closing one every time
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val verifier = getVerifier() ?: return Resource.Error(message = "Verifier not found")

        return try {
            // 1. Get the raw HttpResponse
            val response = client.post("https://accounts.spotify.com/api/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", code)
                        append("redirect_uri", REDIRECT_URI)
                        append("client_id", CLIENT_ID)
                        append("code_verifier", verifier)
                    })
                )
            }

            // 2. Check the Status Code
            if (response.status.value in 200..299) {
                val tokenResponse: SpotifyTokenResponse = response.body()
                Resource.Success(tokenResponse)

            } else {
                // Spotify usually returns error details in the body as well
                val errorBody = response.bodyAsText()
                Resource.Error(message = "Server returned code ${response.status}: $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error(message = e.message ?: "Network failure")

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