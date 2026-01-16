package com.truerandom.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.truerandom.db.dao.TrackDao
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.db.entity.PlayCountEntity
import kotlinx.coroutines.Dispatchers

@Database(entities = [LikedTrackEntity::class, PlayCountEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}

// Room will generate the 'actual' object for you during compilation.
// Note: initialize() is removed from the interface here.
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>

fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}