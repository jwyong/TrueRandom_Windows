package com.truerandom.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    // This creates the database file in the user's home directory (e.g., C:\Users\Name\truerandom.db)
    val dbFile = File(System.getProperty("user.home"), "truerandom.db")

    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
        // The factory is provided by the generated AppDatabaseConstructor
        factory = { AppDatabaseConstructor.initialize() }
    )
}