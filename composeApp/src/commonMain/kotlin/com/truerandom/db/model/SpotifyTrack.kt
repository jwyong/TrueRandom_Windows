package com.truerandom.db.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LikedSongsResponse(
    val href: String? = null,
    val items: List<SavedTrackItem> = emptyList(),
    val limit: Int? = null,
    val next: String? = null,
    val offset: Int? = null,
    val previous: String? = null,
    val total: Int = 0
)

@Serializable
data class SavedTrackItem(
    @SerialName("added_at")
    val addedAt: String? = null,
    val track: SpotifyTrack? = null
)

@Serializable
data class SpotifyTrack(
    val id: String? = null,
    val name: String? = null,
    val uri: String? = null,
    val type: String? = null,
    val album: SpotifyAlbum? = null,
    val artists: List<SpotifyArtist> = emptyList(),

    @SerialName("available_markets")
    val availableMarkets: List<String> = emptyList(),

    @SerialName("disc_number")
    val discNumber: Int? = null,

    @SerialName("duration_ms")
    val durationMs: Long? = null,

    val explicit: Boolean? = null,

    @SerialName("external_ids")
    val externalIds: ExternalIds? = null,

    @SerialName("external_urls")
    val externalUrls: ExternalUrls? = null,

    val href: String? = null,

    @SerialName("is_local")
    val isLocal: Boolean? = null,

    @SerialName("is_playable")
    val isPlayable: Boolean? = null,

    val popularity: Int? = null,

    @SerialName("preview_url")
    val previewUrl: String? = null,

    @SerialName("track_number")
    val trackNumber: Int? = null
) {
    fun formatArtistNames(): String? {
        if (artists.isEmpty()) return null
        return artists.mapNotNull { it.name }.joinToString(separator = ", ")
    }
}

@Serializable
data class SpotifyAlbum(
    val id: String? = null,
    val name: String? = null,
    val uri: String? = null,
    val type: String? = null,

    @SerialName("album_type")
    val albumType: String? = null,

    val artists: List<SpotifyArtist> = emptyList(),

    @SerialName("available_markets")
    val availableMarkets: List<String> = emptyList(),

    @SerialName("external_urls")
    val externalUrls: ExternalUrls? = null,

    val href: String? = null,
    val images: List<SpotifyImage> = emptyList(),

    @SerialName("is_playable")
    val isPlayable: Boolean? = null,

    @SerialName("release_date")
    val releaseDate: String? = null,

    @SerialName("release_date_precision")
    val releaseDatePrecision: String? = null,

    @SerialName("total_tracks")
    val totalTracks: Int? = null
)

@Serializable
data class SpotifyArtist(
    val id: String? = null,
    val name: String? = null,
    val uri: String? = null,
    val type: String? = null,
    val href: String? = null,

    @SerialName("external_urls")
    val externalUrls: ExternalUrls? = null
)

@Serializable
data class SpotifyImage(
    val url: String? = null,
    val height: Int? = null,
    val width: Int? = null
)

@Serializable
data class ExternalUrls(
    val spotify: String? = null
)

@Serializable
data class ExternalIds(
    val isrc: String? = null
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null
)
