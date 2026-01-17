package com.truerandom.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val ACCESS_TOKEN_KEY = stringPreferencesKey("ACCESS_TOKEN_KEY")
private val ACCESS_TOKEN_EXPIRY_KEY = longPreferencesKey("ACCESS_TOKEN_EXPIRY_KEY")
private val REFRESH_TOKEN_KEY = stringPreferencesKey("REFRESH_TOKEN_KEY")

class DatastoreRepository(private val dataStore: DataStore<Preferences>) {
    suspend fun getAccessToken(): String? = dataStore.data.map { it[ACCESS_TOKEN_KEY] }.first()
    suspend fun saveAccessToken(token: String) {
        dataStore.edit { it[ACCESS_TOKEN_KEY] = token }
    }

    suspend fun getAccessTokenExpiry(): Long? = dataStore.data.map { it[ACCESS_TOKEN_EXPIRY_KEY] }.first()
    suspend fun saveAccessTokenExpiry(expiry: Long) {
        dataStore.edit { it[ACCESS_TOKEN_EXPIRY_KEY] = expiry }
    }

    suspend fun getRefreshToken(): String? = dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()
    suspend fun saveRefreshToken(token: String) {
        dataStore.edit { it[REFRESH_TOKEN_KEY] = token }
    }

    suspend fun clearDatastore() {
        dataStore.edit { it.clear() }
    }
}