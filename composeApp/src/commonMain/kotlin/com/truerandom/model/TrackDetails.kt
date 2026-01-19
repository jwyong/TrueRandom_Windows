package com.truerandom.model

data class TrackDetails(
    val trackUri: String,
    val trackName: String?,
    val artistName: String?,
    val albumCoverUrl: String?,
    val duration: Long? = null,
    val playCount: Int = 0
)
