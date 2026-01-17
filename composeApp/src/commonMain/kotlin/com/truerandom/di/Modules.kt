package com.truerandom.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.truerandom.api.AuthApiRepository
import com.truerandom.api.TrackApiRepository
import com.truerandom.data.DatastoreRepository
import com.truerandom.db.AppDatabase
import com.truerandom.db.dao.TrackDao
import com.truerandom.db.getDatabaseBuilder
import com.truerandom.db.getRoomDatabase
import com.truerandom.db.repository.TrackDbRepository
import com.truerandom.ui.main.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect val platformModule: Module

// src/commonMain/kotlin/com/truerandom/di/Modules.kt
val appModule = module {
    // Provide db instance
    single<AppDatabase> {
        val dbBuilder = getDatabaseBuilder()
        dbBuilder
            .setDriver(BundledSQLiteDriver()) // Important for KMP
            .build()
        getRoomDatabase(dbBuilder)
    }

    // Daos
    single<TrackDao> { get<AppDatabase>().trackDao() }

    // Repositories (data, api and db)
    single { AuthApiRepository() }
    single { TrackApiRepository() }
    single { TrackDbRepository(get()) }
    single { DatastoreRepository(get()) }

    // ViewModels
    viewModel { MainViewModel(get(), get(),get(), get()) }
}