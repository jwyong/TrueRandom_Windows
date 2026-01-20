package com.truerandom.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truerandom.api.AuthApiRepository
import com.truerandom.api.TrackApiRepository
import com.truerandom.data.DatastoreRepository
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.db.entity.PlayCountEntity
import com.truerandom.db.repository.PlayCountDbRepository
import com.truerandom.db.repository.SupabaseRepository
import com.truerandom.db.repository.TrackDbRepository
import com.truerandom.model.PlayerStateResponse
import com.truerandom.model.SpotifyErrorResponse
import com.truerandom.model.SpotifyTokenResponse
import com.truerandom.util.PlaybackManager
import com.truerandom.util.Resource
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import truerandomwindows.composeapp.generated.resources.Res
import truerandomwindows.composeapp.generated.resources.access_token_expired
import truerandomwindows.composeapp.generated.resources.connected_to_spotify
import truerandomwindows.composeapp.generated.resources.fetch_liked_tracks
import truerandomwindows.composeapp.generated.resources.fetch_liked_tracks_success
import truerandomwindows.composeapp.generated.resources.logout_success
import truerandomwindows.composeapp.generated.resources.pause_failed
import truerandomwindows.composeapp.generated.resources.play_failed
import truerandomwindows.composeapp.generated.resources.sync_play_counts_success

class MainViewModel(
    private val supabaseRepository: SupabaseRepository,
    private val authApiRepository: AuthApiRepository,
    private val trackDbRepository: TrackDbRepository,
    private val playCountDbRepository: PlayCountDbRepository,
    private val trackApiRepository: TrackApiRepository,
    private val datastoreRepository: DatastoreRepository
) : ViewModel() {
    init {
        launchSpotifyDesktop()
        startCheckAuthFlow()
        startCheckSupabaseFlow()
    }

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState = _uiState.asStateFlow()

    /**
     * Auth related
     **/
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

    // Save relevant token data to datatstore
    private suspend fun saveTokenResponseToDatastore(tokenResponse: SpotifyTokenResponse) {
        println("Saving token response to datastore: $tokenResponse")

        datastoreRepository.saveAccessToken(tokenResponse.accessToken)
        val accessTokenExpiry = System.currentTimeMillis() + tokenResponse.expiresIn * 1000
        datastoreRepository.saveAccessTokenExpiry(accessTokenExpiry)

        tokenResponse.refreshToken?.let { datastoreRepository.saveRefreshToken(it) }
    }

    // Get a working access token either from datastore or refresh
    private suspend fun getWorkingAccessToken(): String {
        val expiryTime = datastoreRepository.getAccessTokenExpiry() ?: 0L
        val currentTime = System.currentTimeMillis()

        // 5 minutes in milliseconds
        val buffer = 5 * 60 * 1000

        if (currentTime + buffer >= expiryTime) {
            println("Token is close to expiring. Refreshing...")

            val refreshToken = datastoreRepository.getRefreshToken()
            if (refreshToken?.isNotBlank() == true) {
                // Refresh token available - use it to refresh accessToken
                println("RefreshToken available - refreshing accessToken...")

                val refreshTokenResponse = authApiRepository.refreshAccessToken(refreshToken)

                println("Refresh token response: $refreshTokenResponse")

                if (refreshTokenResponse.isSuccess) {
                    // Success - save to datastore
                    return refreshTokenResponse.data?.let {
                        saveTokenResponseToDatastore(it)
                        it.accessToken
                    } ?: ""
                } else {
                    println("Refresh token response was not successful: $refreshTokenResponse")
                }
            } else {
                println("Refresh token is null or blank, need re-do full auth flow")
                showSnackbar(getString(Res.string.access_token_expired))
            }

        } else {
            // Token is still healthy
            return datastoreRepository.getAccessToken() ?: ""
        }

        return ""
    }

    /**
     * Supabase related
     **/
    // Start checking and syncing data from supabase (play_count table)
    private fun startCheckSupabaseFlow() {
        // Sync from cloud first (upsert to local), then sync to cloud (upsert to cloud)
        viewModelScope.launch {
            syncPlayCountsFromCloud()
            syncPlayCountsToCloud()
        }
    }

    private suspend fun syncPlayCountsFromCloud() {
        // Get full list of play counts from supabase
        val playCountEntities = supabaseRepository.getAllPlayCounts()
        println("Play counts from Supabase: ${playCountEntities.size}")

        // Upsert to local db
        if (playCountEntities.isNotEmpty()) {
            val upserted = playCountDbRepository.upsertPlayCounts(playCountEntities)
            println("Upserted supabase play counts to local db: ${upserted.size}")
        }
    }

    private suspend fun syncPlayCountsToCloud(): List<PlayCountEntity> {
        // Upsert whole local db to supabase for syncing
        val allPlayCounts = playCountDbRepository.getAllPlayCounts()
        val upsertSupabaseResult = supabaseRepository.upsertPlayCounts(allPlayCounts)
        println("upsertSupabaseResult = $upsertSupabaseResult")

        _uiState.update { it.copy(playCounts = Resource.Success(upsertSupabaseResult.data)) }
        showSnackbar(getString(Res.string.sync_play_counts_success))

        return allPlayCounts
    }

    /**
     * Sync tracks related
     **/
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
    suspend fun fetchAndSyncLikedTracks() {
        println("fetchAndSyncLikedTracks")
        showSnackbar(getString(Res.string.fetch_liked_tracks))

        // Clear off db first
        trackDbRepository.deleteAllTracks()

        val accessToken = getWorkingAccessToken()
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
        showSnackbar(getString(Res.string.fetch_liked_tracks_success, totalTracks))
        println("Full sync complete, removing old trackUris from playCount table...")

        // Get list of trackUris from LikedTracks and compare in playCount table
        val likedTrackUris = trackDbRepository.getAllTrackUris()
        val unlikedTrackUris = playCountDbRepository.getOrphanedPlayCountUris(likedTrackUris)
        println("Unliked trackUris: ${unlikedTrackUris.size}")

        val deletedPlayCount = playCountDbRepository.deletePlayCountsNotInList(likedTrackUris)

        println("Deleted $deletedPlayCount playCount rows from local playCount table.")

        // Then sync local db to cloud + cleanup old items on cloud
        supabaseRepository.deletePlayCountsFromCloud(unlikedTrackUris)

        println("Deleted ${unlikedTrackUris.size} playCount rows from cloud playCount table.")
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

    // Logout - clear dataStore and...? TODO: JAY_LOG - just clear datastore for now
    fun logoutBtnOnClick() {
        viewModelScope.launch {
            datastoreRepository.clearDatastore()
            showSnackbar(getString(Res.string.logout_success))
        }
    }

    /**
     * Play logics
     **/
    fun playPauseBtnOnClick() {
        viewModelScope.launch {
            val accessToken = getWorkingAccessToken()

            // Small delay for spotify app to launch and connect properly
//            delay(5000)
//            val activeDeviceId = trackApiRepository.getActiveDeviceId(accessToken)
//            println("Active device ID: $activeDeviceId")
//
//            if (activeDeviceId.isNullOrBlank()) {
//                println("No active device found")
//                showSnackbar(getString(Res.string.no_device_found))
//            }

            if (_uiState.value.isPlaying) {
                // Is playing - just send pause
                stopPlaybackEndCheck()

                val pauseResult = trackApiRepository.pausePlayback(accessToken)
                println("Pause success: $pauseResult")

                if (!pauseResult.isSuccess) {
                    showSnackbar(
                        getString(
                            Res.string.pause_failed,
                            pauseResult.data?.error?.message ?: ""
                        )
                    )

                } else {
                    // Update UI
                    _uiState.update {
                        it.copy(isPlaying = !_uiState.value.isPlaying)
                    }
                }

            } else {
                // Is paused or stopped - check currentTrackUri and play
                // TODO: JAY_LOG - just start play for now
                val playResult = playNextRandomTrack(accessToken, false)
                println("Play success: ${playResult.isSuccess}")

                if (!playResult.isSuccess) {
                    val rawMessage = playResult.data?.error?.message ?: ""
                    showSnackbar(
                        getString(Res.string.play_failed, rawMessage)
                    )
                } else {
                    // Update UI only if play was successful
                    _uiState.update {
                        it.copy(isPlaying = !_uiState.value.isPlaying)
                    }
                }
            }
        }
    }

    // Increment current track play count, then play next random track
    private suspend fun playNextRandomTrack(
        accessToken: String,
        shouldIncrementCount: Boolean
    ): Resource<SpotifyErrorResponse> {
        // Increment currently playing track details in parallel (only for when track end)
        if (shouldIncrementCount) {
            coroutineScope {
                launch {
                    val currentTrackUri =
                        _uiState.value.currentTrackDetails?.trackUri ?: return@launch
                    val incrementedPlayCount =
                        playCountDbRepository.incrementPlayCount(currentTrackUri)

                    println("Incremented playCount for $incrementedPlayCount")

                    // Sync the incremented playCount to cloud
                    if (incrementedPlayCount != null) {
                        supabaseRepository.upsertPlayCount(incrementedPlayCount)
                        println("Synced incremented playCount to cloud")
                    }
                }
            }
        }

        // Get next random trackUri to be played
        val nextTrackToPlayUri = trackDbRepository.getRandomLeastPlayedTrack() ?: ""
        println("nextTrackToPlay URI: $nextTrackToPlayUri")

        return playTrackFromStart(accessToken, nextTrackToPlayUri)
    }

    // Play a specific trackUri on spotify (from start of track)
    private suspend fun playTrackFromStart(
        accessToken: String,
        trackUri: String
    ): Resource<SpotifyErrorResponse> {
        val playResult = trackApiRepository.playTrackFromStart(accessToken, trackUri)
        println("playTrackFromStart: playResult = ${playResult.isSuccess}")

        if (playResult.isSuccess) {
            // Update UI first
            val nextTrackDetails = trackDbRepository.getTrackDetailsByUri(trackUri)
            println("Next track details: $nextTrackDetails")
            _uiState.update { it.copy(currentTrackDetails = nextTrackDetails) }

            // THEN schedule playback end for this track, coz this will kill off the current coroutine job
            schedulePlaybackEndCheck(trackUri)
        }

        return playResult
    }

    // Schedule a playback check
    private fun schedulePlaybackEndCheck(trackUri: String) {
        println("Scheduling playback end check for $trackUri...")

        PlaybackManager.playbackEndCheckJob?.cancel()
        PlaybackManager.playbackEndCheckJob = PlaybackManager.playbackEndCheckScope.launch {
            // Just-start playbackEnd check
            recursivePlaybackEndCheck(trackUri, true)
        }
    }

    private var currentTrackUri: String? = null
    private suspend fun recursivePlaybackEndCheck(trackUri: String, isFirstCheck: Boolean) {
        // 1. Get the current state once to find out the duration
        println("recursivePlaybackEndCheck: starting to check player state...")
        var accessToken = getWorkingAccessToken()
        val state = trackApiRepository.getPlayerState(accessToken)
        println("Player state: $state")

        val duration = state?.item?.durationMs ?: return
        val progress = state.progressMs

        // 2. Calculate "Safe Sleep"
        var timeToWait = (duration - progress) - 5000
        println("Time to wait: $timeToWait ms")

        if (timeToWait > 0) {
            // Delay max 1 min before next check
            if (timeToWait > 60000) {
                timeToWait = 60000
                println("Time to wait capped at 1 min: $timeToWait ms")
            }

            delay(timeToWait)
        }

        // Get fresh accessToken and state again after delay
        println("Waited $timeToWait, verifying playerState for $trackUri...")
        checkPlayerState(trackUri, false)
    }

    // Check playerState of trackUri and do trackEnd actions accordingly
    private suspend fun checkPlayerState(
        trackUri: String,
        isFirstCheck: Boolean
    ): PlayerStateResponse? {
        var accessToken = getWorkingAccessToken()
        val state = trackApiRepository.getPlayerState(accessToken)
        println("Player state: $state")

        if (state != null) {
            // For first check, just return the response to process
            if (isFirstCheck) return state

            // For recursive check, check if track has ended
            if (!state.isPlaying && state.progressMs == 0L) {
                // Paused at progress 0 - check if track did play and has now ended
                if (currentTrackUri == trackUri) {
                    // This track has been played before - means track has ended
                    println("Track ended - playing next random...")
                    incrementAndPlayNextRandom(accessToken)
                } else {
                    // This track has NOT been played before - do nothing
                    println("Track has not been played before - do nothing...")
                }

            } else {
                // Still playing - update currentTrackUri to indicate already played once
                currentTrackUri = trackUri

                // Check remaining time to determine playbackEnd
                val remaining = (state.item?.durationMs ?: 0) - state.progressMs
                println("Remaining time: $remaining ms")

                if (remaining <= 5000) {
                    // Near end - just delay then play next random
                    val delayWithBuffer = remaining + 1000
                    println("Near end - playing next track after $delayWithBuffer ms")

                    delay(delayWithBuffer)
                    incrementAndPlayNextRandom(accessToken)

                } else {
                    // Still far from end - reschedule check
                    println("Still far from end - reschedule check...")
                    recursivePlaybackEndCheck(trackUri, false)
                }
            }
        } else {
            println("PlayerState is null.")
        }

        return state
    }

    // Increment current track and play next random (at track End)
    private suspend fun incrementAndPlayNextRandom(accessToken: String) {
        println("getting next random track to play...")

        val playResult = playNextRandomTrack(accessToken, true)
        println("Play result after delay: ${playResult.isSuccess}")

        if (!playResult.isSuccess) {
            showSnackbar(
                getString(
                    Res.string.play_failed,
                    playResult.data?.error?.message ?: ""
                )
            )
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    private fun stopPlaybackEndCheck() {
        println("Stopping playback end check...")
        PlaybackManager.playbackEndCheckJob?.cancel()
    }

    /**
     * Playback state poll observer
     **/
    private var playbackObserverJob: Job? = null

    // TODO: JAY_LOG - polling method, remove if unnecessary
//    private fun startPlaybackObserver(currentTrackUri: String) {
//        println("Starting playback observer for $currentTrackUri...")
//
//        playbackObserverJob?.cancel() // Reset if one is running
//        playbackObserverJob = viewModelScope.launch {
//            while (isActive) {
//                val accessToken = getWorkingAccessToken()
//                if (accessToken.isBlank()) break
//
//                val state = trackApiRepository.getPlayerState(accessToken)
//                println("Player state: $state")
//
//                if (state != null) {
//                    // 2. Check if the track is finished
//                    // We assume it's finished if there's less than 2 seconds left
//                    val duration = state.item?.durationMs ?: 0
//                    val remaining = duration - state.progressMs
//
//                    println("Remaining time: $remaining ms")
//
//                    if (remaining < 2000 && state.isPlaying) {
//                        delay(3000)
//
//                        // Increment play count of current track
//                        trackDbRepository.incrementPlayCount(currentTrackUri)
//
//                        val currentTrackLabel = with(_uiState.value.currentTrackDetails) {
//                            if (this != null) {
//                                "$trackName - $artistName"
//                            } else {
//                                getString(Res.string.unknown_track)
//                            }
//                        }
//                        println("Track finished - incrementing play count for $currentTrackLabel ($currentTrackUri)")
//
//                        playNextRandomTrack(accessToken)
//
//                        // Cancel off this job after playing next track
//                        playbackObserverJob?.cancel()
//                    }
//                }
//
//                // Poll every 2 seconds for a balance between responsiveness and battery life
//                delay(2000)
//            }
//        }
//    }
//
//    private fun stopPlaybackObserver() {
//        playbackObserverJob?.cancel()
//    }

    // For launching spotify 1 time
    private fun launchSpotifyDesktop() {
        try {
            // This uses the Windows shell to open the spotify "link"
            // which triggers the installed desktop app.
            ProcessBuilder("cmd", "/c", "start spotify:").start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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