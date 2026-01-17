package com.truerandom.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truerandom.api.AuthApiRepository
import com.truerandom.api.TrackApiRepository
import com.truerandom.data.DatastoreRepository
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.db.model.SpotifyTokenResponse
import com.truerandom.db.repository.TrackDbRepository
import com.truerandom.util.Resource
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import truerandomwindows.composeapp.generated.resources.Res
import truerandomwindows.composeapp.generated.resources.connected_to_spotify
import truerandomwindows.composeapp.generated.resources.fetch_liked_tracks
import truerandomwindows.composeapp.generated.resources.fetch_liked_tracks_success

class MainViewModel(
    private val authApiRepository: AuthApiRepository,
    private val trackDbRepository: TrackDbRepository,
    private val trackApiRepository: TrackApiRepository,
    private val datastoreRepository: DatastoreRepository
) : ViewModel() {
    init {
        // Start checking auth flow
        startCheckAuthFlow()
    }

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState = _uiState.asStateFlow()

    // Auth flow entry point - check if refreshToken available in datastore
    private fun startCheckAuthFlow() {
        viewModelScope.launch {
            // Get refresh token from dataStore
            val refreshToken = datastoreRepository.getRefreshToken()

            if (refreshToken?.isNotBlank() == true) {
                // Refresh token available - use it to refresh accessToken
                println("RefreshToken available - refreshing accessToken...")

                val refreshTokenResponse = authApiRepository.refreshAccessToken(refreshToken)

                println("Refresh token response: $refreshTokenResponse")

                if (refreshTokenResponse.isSuccess) {
                    // Success - save to datastore
                    refreshTokenResponse.data?.let { saveTokenResponseToDatastore(it) }
                }

                // Start checking to fetch liked songs
                checkAndFetchLikedSongs()

                // Update UI
                _uiState.update { it.copy(token = refreshTokenResponse) }
                showSnackbar(getString(Res.string.connected_to_spotify))

            } else {
                // Refresh token unavailable - start full auth flow (subscribe to browser listener, then launch browser)
                println("RefreshToken unavailable - starting auth flow...")

                subscribeSpotifyCallbackListener()
                authApiRepository.launchSpotifyAuthBrowser()
            }
        }
    }

    // Subscribe to spotify browser auth callback
    private fun subscribeSpotifyCallbackListener() {
        println("Subscribing to Spotify callback...")

        listenForSpotifyCallback { code ->
            viewModelScope.launch {
                val tokenResponseResult = authApiRepository.exchangeCodeForToken(code)
                println("Token response: $tokenResponseResult")

                if (tokenResponseResult.isSuccess) {
                    // Success - save to datastore
                    tokenResponseResult.data?.let { saveTokenResponseToDatastore(it) }
                }

                // Start checking to fetch liked songs
                checkAndFetchLikedSongs()

                // Update UI
                _uiState.update { it.copy(token = tokenResponseResult) }
                showSnackbar(getString(Res.string.connected_to_spotify))
            }
        }
    }

    private suspend fun saveTokenResponseToDatastore(tokenResponse: SpotifyTokenResponse) {
        println("Saving token response to datastore: $tokenResponse")

        datastoreRepository.saveAccessToken(tokenResponse.accessToken)
        val accessTokenExpiry = System.currentTimeMillis() + tokenResponse.expiresIn
        datastoreRepository.saveAccessTokenExpiry(accessTokenExpiry)

        tokenResponse.refreshToken?.let { datastoreRepository.saveRefreshToken(it) }
    }

    // Check liked tracks flow
    suspend fun checkAndFetchLikedSongs() {
        println("Checking liked songs...")

        // Check if there are tracks in liked_tracks table
        val likedTracksCount = trackDbRepository.getLikedTracksCount()
        if (likedTracksCount == 0) {
            println("Database is empty. Fetching and syncing liked songs...")
            fetchAndSyncLikedTracks()

        } else {
            println("Database already contains $likedTracksCount tracks.")

            _uiState.update { it.copy(tracks = Resource.Success()) }
        }
    }

    // Fetch all liked songs from api (paged 50 items) and insert to db
    private suspend fun fetchAndSyncLikedTracks() {
        println("fetchAndSyncLikedTracks")
        showSnackbar(getString(Res.string.fetch_liked_tracks))

        val accessToken = datastoreRepository.getAccessToken()?: run {
            println("checkAndFetchLikedSongs: dataStore accessToken is NULL.")
            return
        }

        // Clear off db first
        trackDbRepository.deleteAllTracks()

        var currentOffset = 0
        var totalTracks = 1 // Placeholder to start the loop

        while (currentOffset < totalTracks) {
            val response = trackApiRepository.fetchLikedTracksPaged(
                accessToken, currentOffset
            ) ?: break

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
            trackDbRepository.insertTracks(pageTracks)

            currentOffset += pageTracks.size
            println("Progress: $currentOffset / $totalTracks tracks synced.")

            // Update loading percentage
            val loadingPercentage = currentOffset.toFloat() / totalTracks
            _uiState.update { it.copy(tracks = Resource.Loading(loadingPercentage)) }

            // Safety: If Spotify returns 0 items but claims a total, break to avoid infinite loop
            if (pageTracks.isEmpty()) break
        }

        _uiState.update { it.copy(tracks = Resource.Success()) }
        showSnackbar(getString(Res.string.fetch_liked_tracks_success))
        println("Full sync complete.")
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

    /**
     * Snackbar
     **/
    private val _events = Channel<String>()
    val events = _events.receiveAsFlow()

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _events.send(message)
        }
    }
}