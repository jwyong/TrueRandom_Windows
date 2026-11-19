package com.truerandom.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.truerandom.data.createDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<DataStore<Preferences>> {
        createDataStore {
            androidContext().filesDir.resolve("settings.preferences_pb").absolutePath
        }
    }
}
