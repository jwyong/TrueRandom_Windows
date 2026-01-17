package com.truerandom.ui.main

import com.truerandom.model.SpotifyTokenResponse
import com.truerandom.util.Resource

data class MainScreenState(
    val token: Resource<SpotifyTokenResponse> = Resource.Loading(),
    val tracks: Resource<Float> = Resource.Loading(),

    val isPlaying: Boolean = false,
)
