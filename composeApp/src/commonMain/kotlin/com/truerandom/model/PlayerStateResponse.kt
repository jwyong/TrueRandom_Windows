package com.truerandom.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerStateResponse(
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("progress_ms") val progressMs: Long,
    val item: TrackItem?
)

@Serializable
data class TrackItem(
    val id: String,
    val name: String,
    @SerialName("duration_ms") val durationMs: Long
)