package com.truerandom.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

// src/jvmMain/kotlin/com/truerandom/db/Database.jvm.kt
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("user.home"), "truerandom.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
        factory = { AppDatabaseConstructor.initialize() }
    )
}