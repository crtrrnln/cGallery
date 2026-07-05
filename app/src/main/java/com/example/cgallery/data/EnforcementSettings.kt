package com.example.cgallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.enforcementDataStore: DataStore<Preferences> by preferencesDataStore(name = "enforcement_settings")

data class EnforcementSettings(
    val isEnforcementEnabled: Boolean = true,
    val isShizukuEnabled: Boolean = false,
    val launchAutomatically: Boolean = true,
    val requireInboxBeforeGallery: Boolean = true,
    val autoReturnToPreviousApp: Boolean = true,
    val snoozeExpirationTime: Long = 0,
    val snoozeItemThreshold: Int = 0,
    val currentSnoozeCount: Int = 0
)

class EnforcementSettingsRepository(private val context: Context) {
    private object PreferencesKeys {
        val ENFORCEMENT_ENABLED = booleanPreferencesKey("enforcement_enabled")
        val SHIZUKU_ENABLED = booleanPreferencesKey("shizuku_enabled")
        val LAUNCH_AUTOMATICALLY = booleanPreferencesKey("launch_automatically")
        val REQUIRE_INBOX_BEFORE_GALLERY = booleanPreferencesKey("require_inbox_before_gallery")
        val AUTO_RETURN = booleanPreferencesKey("auto_return")
        val SNOOZE_EXPIRATION = longPreferencesKey("snooze_expiration")
        val SNOOZE_THRESHOLD = intPreferencesKey("snooze_threshold")
        val SNOOZE_COUNT = intPreferencesKey("snooze_count")
    }

    val settingsFlow: Flow<EnforcementSettings> = context.enforcementDataStore.data
        .map { preferences ->
            EnforcementSettings(
                isEnforcementEnabled = preferences[PreferencesKeys.ENFORCEMENT_ENABLED] ?: true,
                isShizukuEnabled = preferences[PreferencesKeys.SHIZUKU_ENABLED] ?: false,
                launchAutomatically = preferences[PreferencesKeys.LAUNCH_AUTOMATICALLY] ?: true,
                requireInboxBeforeGallery = preferences[PreferencesKeys.REQUIRE_INBOX_BEFORE_GALLERY] ?: true,
                autoReturnToPreviousApp = preferences[PreferencesKeys.AUTO_RETURN] ?: true,
                snoozeExpirationTime = preferences[PreferencesKeys.SNOOZE_EXPIRATION] ?: 0,
                snoozeItemThreshold = preferences[PreferencesKeys.SNOOZE_THRESHOLD] ?: 0,
                currentSnoozeCount = preferences[PreferencesKeys.SNOOZE_COUNT] ?: 0
            )
        }

    suspend fun updateEnforcementEnabled(enabled: Boolean) {
        context.enforcementDataStore.edit { it[PreferencesKeys.ENFORCEMENT_ENABLED] = enabled }
    }

    suspend fun updateShizukuEnabled(enabled: Boolean) {
        context.enforcementDataStore.edit { it[PreferencesKeys.SHIZUKU_ENABLED] = enabled }
    }

    suspend fun updateLaunchAutomatically(enabled: Boolean) {
        context.enforcementDataStore.edit { it[PreferencesKeys.LAUNCH_AUTOMATICALLY] = enabled }
    }

    suspend fun setSnooze(expirationTime: Long, itemThreshold: Int) {
        context.enforcementDataStore.edit {
            it[PreferencesKeys.SNOOZE_EXPIRATION] = expirationTime
            it[PreferencesKeys.SNOOZE_THRESHOLD] = itemThreshold
            it[PreferencesKeys.SNOOZE_COUNT] = 0
        }
    }

    suspend fun incrementSnoozeCount() {
        context.enforcementDataStore.edit {
            val current = it[PreferencesKeys.SNOOZE_COUNT] ?: 0
            it[PreferencesKeys.SNOOZE_COUNT] = current + 1
        }
    }

    suspend fun clearSnooze() {
        context.enforcementDataStore.edit {
            it[PreferencesKeys.SNOOZE_EXPIRATION] = 0
            it[PreferencesKeys.SNOOZE_THRESHOLD] = 0
            it[PreferencesKeys.SNOOZE_COUNT] = 0
        }
    }
}
