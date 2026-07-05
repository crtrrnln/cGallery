package com.example.cgallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favourites")

class FavouritesManager(private val context: Context) {
    private val FAVOURITES_KEY = stringSetPreferencesKey("favourite_ids")

    val favouriteIds: Flow<Set<Long>> = context.dataStore.data
        .map { preferences ->
            preferences[FAVOURITES_KEY]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun toggleFavourite(id: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVOURITES_KEY] ?: emptySet()
            val idString = id.toString()
            val next = if (current.contains(idString)) {
                current - idString
            } else {
                current + idString
            }
            preferences[FAVOURITES_KEY] = next
        }
    }

    suspend fun addFavourite(id: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVOURITES_KEY] ?: emptySet()
            preferences[FAVOURITES_KEY] = current + id.toString()
        }
    }

    suspend fun removeFavourite(id: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVOURITES_KEY] ?: emptySet()
            preferences[FAVOURITES_KEY] = current - id.toString()
        }
    }
}
