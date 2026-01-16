package com.truerandom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Room
import com.truerandom.api.AuthApiRepository
import com.truerandom.api.AuthApiRepository.refreshAccessToken
import com.truerandom.api.AuthApiRepository.startSpotifyAuth
import com.truerandom.db.AppDatabase
import com.truerandom.db.getRoomDatabase
import com.truerandom.db.model.TokenResponse
import com.truerandom.db.repository.TrackDbRepository
import com.truerandom.storage.DataStoreProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import truerandomwindows.composeapp.generated.resources.Res
import truerandomwindows.composeapp.generated.resources.compose_multiplatform
import java.io.File

const val CLIENT_ID = "e61d6a48cd14457c97e43850f03eb35c"
const val CLIENT_SECRET = "a699313bb43743029848c2e4d320e448"
const val REDIRECT_URI = "http://127.0.0.1:8888/callback"
val AUTH_SCOPE = listOf(
    "user-read-email",
    "user-read-private",
    "streaming",
    "user-library-read",
    "user-modify-playback-state"
).joinToString(" ")

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()

    // Initialize Database and Repository
    var accessTokenRmb = remember<String?> { null }
    val trackDbRepository = remember {
        val appData = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
        val dbFile = File(appData, "TrueRandom/truerandom.db")
        if (!dbFile.parentFile.exists()) dbFile.parentFile.mkdirs()

        val dbBuilder = Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        )
        val db = getRoomDatabase(dbBuilder)
        TrackDbRepository(db.trackDao())
    }

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                scope.launch {
                    // Get refresh token from dataStore
                    val refreshToken = DataStoreProvider.getRefreshToken()

                    if (refreshToken?.isNotBlank() == true) {
                        // Refresh token available - use it to refresh accessToken
                        println("RefreshToken available - refreshing accessToken...")
                        val refreshTokenResponse = refreshAccessToken(refreshToken)

                        // Save NEW refreshToken to dataStore
                        refreshTokenResponse?.refreshToken?.let { refreshToken ->
                            DataStoreProvider.saveRefreshToken(refreshToken)
                        }

                        refreshTokenResponse?.accessToken?.let { accessToken ->
                            val accessTokenExpiry =
                                System.currentTimeMillis() + refreshTokenResponse.expiresIn

                            println(
                                "AccessToken refreshed: $accessToken, expiresIn = ${refreshTokenResponse.expiresIn}, " +
                                        "accessTokenExpiry = $accessTokenExpiry"
                            )

                            // Save accessToken + expiry ms to dataStore
                            accessTokenRmb = accessToken
                            DataStoreProvider.saveAccessToken(accessToken)
                            DataStoreProvider.saveAccessTokenExpiry(accessTokenExpiry)

                            // Start checking for liked songs to fetch
                            trackDbRepository.checkAndFetchLikedSongs(accessToken)
                        }
                    } else {
                        // Refresh token unavailable - start full auth flow
                        println("RefreshToken unavailable - starting auth flow...")
                        listenForSpotifyCallback { code ->
                            scope.launch {
                                exchangeCodeForToken(code)?.let { tokenResponse ->
                                    // Save refreshToken to dataStore
                                    tokenResponse.refreshToken?.let { refreshToken ->
                                        DataStoreProvider.saveRefreshToken(refreshToken)
                                        println("saved refreshToken to dataStore: $refreshToken")
                                    } ?: run {
                                        println("refreshToken from exchangeCode is NULL.")
                                    }

                                    // Save accessToken to dataStore
                                    val accessToken = tokenResponse.accessToken
                                    accessTokenRmb = accessToken
                                    val accessTokenExpiry =
                                        System.currentTimeMillis() + tokenResponse.expiresIn

                                    println(
                                        "AccessToken refreshed: $accessToken, expiresIn = ${tokenResponse.expiresIn}, " +
                                                "accessTokenExpiry = $accessTokenExpiry"
                                    )

                                    // Save accessToken + expiry ms to dataStore
                                    DataStoreProvider.saveAccessToken(accessToken)
                                    DataStoreProvider.saveAccessTokenExpiry(accessTokenExpiry)

                                    // Start checking to fetch liked songs
                                    trackDbRepository.checkAndFetchLikedSongs(accessToken)

                                } ?: run {
                                    println("Error exchanging code for token - response is NULL.")
                                }
                            }
                        }
                        startSpotifyAuth(CLIENT_ID, REDIRECT_URI, AUTH_SCOPE)
                    }
                }
            }) {
                Text("Connect Spotify")

                if (accessTokenRmb != null) {
                    Text("Connected!")
                }
            }

            // Play btn
            Button(onClick = {
                scope.launch {
                    trackDbRepository.playRandomLeastPlayedTrack()
                }
            }) {
                Text("Start playing")
            }

            // Re-auth btn
            Button(onClick = {
                scope.launch {
                    DataStoreProvider.saveRefreshToken("")
                }
            }) {
                Text("Clear refresh token")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}

// Listen to browser auth done callback (loopback)
private fun listenForSpotifyCallback(onCodeReceived: (String) -> Unit) {
    val server = embeddedServer(Netty, port = 8888, host = "127.0.0.1") {
        routing {
            get("/callback") {
                val code = call.parameters["code"]
                if (code != null) {
                    call.respondText("Login successful! Return to TrueRandom.")
                    onCodeReceived(code)
                }
            }
        }
    }
    server.start(wait = false)
}

// Get accessToken from temp auth code (~10 mins)
suspend fun exchangeCodeForToken(code: String): TokenResponse? {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    val verifier = AuthApiRepository.getVerifier() ?: return null

    return try {
        val response: TokenResponse = client.post("https://accounts.spotify.com/api/token") {
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
        }.body()

        response
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        client.close()
    }
}
