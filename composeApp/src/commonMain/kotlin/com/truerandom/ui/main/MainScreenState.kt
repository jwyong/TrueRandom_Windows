package com.truerandom.ui.main

import com.truerandom.db.model.SpotifyTokenResponse
import com.truerandom.util.Resource

data class MainScreenState(
    val token: Resource<SpotifyTokenResponse> = Resource.Loading(),
    val tracks: Resource<Float> = Resource.Loading(),
)
