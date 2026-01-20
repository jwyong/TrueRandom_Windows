package com.truerandom.ui.main

import com.truerandom.model.SpotifyTokenResponse
import com.truerandom.model.TrackDetails
import com.truerandom.util.Resource

data class MainScreenState(
    // Syncing
    val token: Resource<SpotifyTokenResponse> = Resource.Loading(),
    val tracks: Resource<Float> = Resource.Loading(),
    val playCounts: Resource<Int> = Resource.Loading(),

    // Currently playing track related
    val isPlaying: Boolean = false,
    val currentTrackDetails: TrackDetails? = null
) {
    val isLoading: Boolean get() = token.isLoading || tracks.isLoading || playCounts.isLoading
}
