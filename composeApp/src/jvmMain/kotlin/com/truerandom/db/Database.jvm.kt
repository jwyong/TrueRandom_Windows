package com.truerandom.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

// src/jvmMain/kotlin/com/truerandom/db/Database.jvm.kt
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    // Create the folder first to prevent Room from crashing if it doesn't exist
    val folder = File("D:/TrueRandom")
    if (!folder.exists()) folder.mkdirs()

    val dbFile = File(folder, "truerandom.db")

    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
        factory = { AppDatabaseConstructor.initialize() }
    )
}