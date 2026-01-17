package com.truerandom.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.truerandom.data.createDataStore
import org.koin.dsl.module

actual val platformModule = module {
    single<DataStore<Preferences>> {
        createDataStore {
            val dataDir = System.getProperty("user.home") + "/.truerandom"
            // We return a String, which createDataStore converts to a Path
            "$dataDir/settings.preferences_pb"
        }
    }
}