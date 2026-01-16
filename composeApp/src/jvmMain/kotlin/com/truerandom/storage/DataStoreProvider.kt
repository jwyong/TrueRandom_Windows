// src/jvmMain/kotlin/com/truerandom/storage/DataStoreProvider.kt
package com.truerandom.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toOkioPath
import java.io.File

object DataStoreProvider {
    val dataStore: DataStore<Preferences> by lazy {
        createDataStore() 
    }

    // ACCESS TOKEN
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("ACCESS_TOKEN_KEY")
    suspend fun saveAccessToken(token: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
        }
    }
    suspend fun getAccessToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }.first()
    }

    private val ACCESS_TOKEN_EXPIRY_KEY = stringPreferencesKey("ACCESS_TOKEN_EXPIRY_KEY")
    suspend fun saveAccessTokenExpiry(expiryEpochMs: Long) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_EXPIRY_KEY] = expiryEpochMs.toString()
        }
    }
    suspend fun getAccessTokenExpiry(): String? {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_EXPIRY_KEY]
        }.first()
    }

    // REFRESH TOKEN
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("spotify_refresh_token")
    suspend fun saveRefreshToken(token: String) {
        dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN_KEY] = token
        }
    }
    suspend fun getRefreshToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }.first()
    }


}

private fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val appData = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
            File(appData, "TrueRandom/settings.preferences_pb").toOkioPath()
        }
    )
}