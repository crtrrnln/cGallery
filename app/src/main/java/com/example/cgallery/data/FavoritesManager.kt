package com.example.cgallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesManager(private val context: Context) {
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_ids")

    val favoriteIds: Flow<Set<Long>> = context.dataStore.data
        .map { preferences ->
            preferences[FAVORITES_KEY]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun toggleFavorite(id: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            val idString = id.toString()
            val next = if (current.contains(idString)) {
                current - idString
            } else {
                current + idString
            }
            preferences[FAVORITES_KEY] = next
        }
    }

    suspend fun addFavorite(id: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = current + id.toString()
        }
    }

    suspend fun removeFavorite(id: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = current - id.toString()
        }
    }
}
