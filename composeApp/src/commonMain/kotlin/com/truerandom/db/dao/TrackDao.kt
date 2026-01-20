package com.truerandom.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.model.TrackDetails

/**
 * Data Access Object for the LikedTrackEntity.
 * Defines the methods for interacting with the database.
 */
@Dao
interface TrackDao {
    // Get list of trackUris only
    @Query("SELECT trackUri FROM liked_tracks")
    suspend fun getAllTrackUris(): List<String>

    // Get tracks count
    @Query("SELECT COUNT(trackUri) FROM liked_tracks")
    suspend fun getTrackCount(): Int

    @Query("DELETE FROM liked_tracks")
    suspend fun deleteAllTracks()

    /**
     * Inserts a list of LikedTrackEntity objects into the database.
     * If a conflict occurs (based on the primary key, which is the track ID),
     * the existing row is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<LikedTrackEntity>)

    /**
     * OPTIMIZED QUERY: Fetches only the URIs of tracks that have the minimum playCount.
     * This minimizes the data transfer overhead for large "unplayed" pools. if trackUri doesn't
     * exist on the play_count table, that playCount will be 0
     *
     * @return A list of track URIs (String) sharing the lowest play count.
     */
    @Query(
        """
        SELECT lt.trackUri 
        FROM liked_tracks lt
        LEFT JOIN play_count pc ON lt.trackUri = pc.trackUri
        WHERE COALESCE(pc.playCount, 0) = (
            SELECT MIN(COALESCE(playCount, 0)) 
            FROM liked_tracks lt2
            LEFT JOIN play_count pc2 ON lt2.trackUri = pc2.trackUri
        )
    """
    )
    suspend fun getLeastPlayedTrackUris(): List<String>

    /**
     * Retrieves the track name, artist name, and album cover URL
     * based on the unique Spotify track URI.
     * * @param uri The unique identifier (trackUri) for the track.
     * @return LikedTrackEntity object containing the requested fields, or null if not found.
     */
    @Query(
        """
    SELECT 
        lt.trackUri, 
        lt.trackName, 
        lt.artistName, 
        lt.albumCoverUrl, 
        COALESCE(pc.playCount, 0) AS playCount
    FROM liked_tracks lt
    LEFT JOIN play_count pc ON lt.trackUri = pc.trackUri
    WHERE lt.trackUri = :uri
    LIMIT 1
    """
    )
    suspend fun getTrackDetailsByUri(uri: String): TrackDetails?

//    /**
//     * Get a paged list of liked tracks along with play count, to be displayed on main page liked
//     * tracks (paged)
//     **/
//    @Query("""
//        SELECT
//            lt.trackUri,
//            lt.trackName,
//            lt.artistName,
//            lt.albumCoverUrl,
//            lt.isLocal,
//            lt.addedAt,
//            COALESCE(pc.playCount, 0) AS playCount
//        FROM liked_tracks lt
//        LEFT JOIN play_count pc ON lt.trackUri = pc.trackUri
//        ORDER BY lt.addedAt DESC
//    """)
//    fun getPagedLikedTracks(): PagingSource<Int, LikedTrackWithCount>
}
