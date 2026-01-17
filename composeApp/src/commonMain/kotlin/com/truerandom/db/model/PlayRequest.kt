package com.truerandom.db.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayRequest(
    val uris: List<String>
)
