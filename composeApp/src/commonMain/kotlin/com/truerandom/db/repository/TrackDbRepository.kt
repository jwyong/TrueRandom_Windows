package com.truerandom.db.repository

import com.truerandom.db.dao.TrackDao
import com.truerandom.db.entity.LikedTrackEntity

class TrackDbRepository(private val trackDao: TrackDao) {
    suspend fun getAllTrackUris() = trackDao.getAllTrackUris()
    suspend fun getLikedTracksCount() = trackDao.getTrackCount()
    suspend fun getLeastPlayedTrackUris() = trackDao.getLeastPlayedTrackUris()
    suspend fun getTrackDetailsByUri(uri: String) = trackDao.getTrackDetailsByUri(uri)

    suspend fun insertTracks(tracks: List<LikedTrackEntity>) = trackDao.insertAll(tracks)

    suspend fun deleteAllTracks() = trackDao.deleteAllTracks()

    // Get trackUri of a random least played track and start playing it
    suspend fun getRandomLeastPlayedTrack(): String? {
        // 1. Get the list from the DAO
        val leastPlayedUris = getLeastPlayedTrackUris()

        // 2. Pick a random URI safely
        return leastPlayedUris.randomOrNull()
    }
}