// src/commonMain/kotlin/com/truerandom/data/DataStore.kt
package com.truerandom.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        // Use 'produceFile' here because that is the name in the library's signature
        produceFile = { producePath().toPath() }
    )
}