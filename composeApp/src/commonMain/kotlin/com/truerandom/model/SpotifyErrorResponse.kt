package com.truerandom.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyErrorResponse(
    val error: SpotifyErrorDetails
)

@Serializable
data class SpotifyErrorDetails(
    val status: Int,
    val message: String,
    val reason: String? = null // Optional because not all errors include a 'reason'
)