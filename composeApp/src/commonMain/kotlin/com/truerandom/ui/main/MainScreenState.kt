package com.truerandom.ui.main

import com.truerandom.model.SpotifyTokenResponse
import com.truerandom.model.TrackDetails
import com.truerandom.util.Resource

data class MainScreenState(
    val token: Resource<SpotifyTokenResponse> = Resource.Loading(),
    val tracks: Resource<Float> = Resource.Loading(),

    // Currently playing track related
    val isPlaying: Boolean = false,
    val currentTrackDetails: TrackDetails? = null
)
