package com.truerandom.db.repository

import com.truerandom.db.dao.PlayCountDao
import com.truerandom.db.entity.PlayCountEntity

class PlayCountDbRepository(private val playCountDao: PlayCountDao) {
    suspend fun getAllPlayCounts() = playCountDao.getAllPlayCounts()
    suspend fun getOrphanedPlayCountUris(likedUris: List<String>) = playCountDao.getOrphanedPlayCountUris(likedUris)
    suspend fun upsertPlayCounts(playCounts: List<PlayCountEntity>) = playCountDao.upsertAll(playCounts)
    suspend fun incrementPlayCount(trackUri: String) = playCountDao.incrementPlayCount(trackUri)
    suspend fun deletePlayCountsNotInList(likedUris: List<String>) = playCountDao.deletePlayCountsNotInList(likedUris)
}