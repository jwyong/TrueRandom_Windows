package com.truerandom.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    
    @SerialName("token_type")
    val tokenType: String,
    
    @SerialName("expires_in")
    val expiresIn: Int,
    
    @SerialName("refresh_token")
    val refreshToken: String? = null, // Optional because it's only sent during rotation or first exchange
    
    @SerialName("scope")
    val scope: String
)