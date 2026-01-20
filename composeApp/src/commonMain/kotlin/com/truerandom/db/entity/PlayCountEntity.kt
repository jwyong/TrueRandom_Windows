package com.truerandom.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * play count tied to trackUri (primary key in liked tracks table)
 */
@Serializable // Required for Supabase
@Entity(tableName = "play_count")
data class PlayCountEntity(
    @PrimaryKey
    val trackUri: String,
    val playCount: Int = 0
)