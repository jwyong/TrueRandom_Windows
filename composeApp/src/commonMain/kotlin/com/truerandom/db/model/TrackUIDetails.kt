package com.truerandom.db.model

data class TrackUIDetails(
    // Note: These field names MUST match the names used in your SELECT statement (and entity fields)
    val trackName: String?,
    val artistName: String?,
    val albumCoverUrl: String?
)
