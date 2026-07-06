package com.example.cgallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

enum class ThemeAccent { INNOCENT_SIN_RED, ETERNAL_PUNISHMENT_BLUE }
enum class GridDensity { COMFORTABLE, COMPACT }

data class AppSettings(
    val isEnforcementEnabled: Boolean = true,
    val isShizukuEnabled: Boolean = true,
    val launchAutomatically: Boolean = true,
    val requireInboxBeforeGallery: Boolean = true,
    val autoReturnToPreviousApp: Boolean = true,
    val snoozeExpirationTime: Long = 0,
    val snoozeItemThreshold: Int = 0,
    val currentSnoozeCount: Int = 0,
    val themeAccent: ThemeAccent = ThemeAccent.INNOCENT_SIN_RED,
    val gridDensity: GridDensity = GridDensity.COMFORTABLE,
    val efficiencyMode: Boolean = false,
    val isBiometricEnabled: Boolean = false
)

class AppSettingsRepository(private val context: Context) {
    private object Keys {
        val ENFORCEMENT_ENABLED = booleanPreferencesKey("enforcement_enabled")
        val SHIZUKU_ENABLED = booleanPreferencesKey("shizuku_enabled")
        val LAUNCH_AUTOMATICALLY = booleanPreferencesKey("launch_automatically")
        val REQUIRE_INBOX_BEFORE_GALLERY = booleanPreferencesKey("require_inbox_before_gallery")
        val AUTO_RETURN = booleanPreferencesKey("auto_return")
        val SNOOZE_EXPIRATION = longPreferencesKey("snooze_expiration")
        val SNOOZE_THRESHOLD = intPreferencesKey("snooze_threshold")
        val SNOOZE_COUNT = intPreferencesKey("snooze_count")
        val THEME_ACCENT = stringPreferencesKey("theme_accent")
        val GRID_DENSITY = stringPreferencesKey("grid_density")
        val EFFICIENCY_MODE = booleanPreferencesKey("efficiency_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.appDataStore.data.map { p ->
        AppSettings(
            isEnforcementEnabled = p[Keys.ENFORCEMENT_ENABLED] ?: true,
            isShizukuEnabled = p[Keys.SHIZUKU_ENABLED] ?: true,
            launchAutomatically = p[Keys.LAUNCH_AUTOMATICALLY] ?: true,
            requireInboxBeforeGallery = p[Keys.REQUIRE_INBOX_BEFORE_GALLERY] ?: true,
            autoReturnToPreviousApp = p[Keys.AUTO_RETURN] ?: true,
            snoozeExpirationTime = p[Keys.SNOOZE_EXPIRATION] ?: 0,
            snoozeItemThreshold = p[Keys.SNOOZE_THRESHOLD] ?: 0,
            currentSnoozeCount = p[Keys.SNOOZE_COUNT] ?: 0,
            themeAccent = try { ThemeAccent.valueOf(p[Keys.THEME_ACCENT] ?: ThemeAccent.INNOCENT_SIN_RED.name) } catch(e: Exception) { ThemeAccent.INNOCENT_SIN_RED },
            gridDensity = try { GridDensity.valueOf(p[Keys.GRID_DENSITY] ?: GridDensity.COMFORTABLE.name) } catch(e: Exception) { GridDensity.COMFORTABLE },
            efficiencyMode = p[Keys.EFFICIENCY_MODE] ?: false,
            isBiometricEnabled = p[Keys.BIOMETRIC_ENABLED] ?: false
        )
    }.distinctUntilChanged()

    suspend fun updateEnforcementEnabled(v: Boolean) { context.appDataStore.edit { it[Keys.ENFORCEMENT_ENABLED] = v } }
    suspend fun updateShizukuEnabled(v: Boolean) { context.appDataStore.edit { it[Keys.SHIZUKU_ENABLED] = v } }
    suspend fun setSnooze(exp: Long, threshold: Int) { context.appDataStore.edit { it[Keys.SNOOZE_EXPIRATION] = exp; it[Keys.SNOOZE_THRESHOLD] = threshold; it[Keys.SNOOZE_COUNT] = 0 } }
    suspend fun incrementSnoozeCount() { context.appDataStore.edit { val c = it[Keys.SNOOZE_COUNT] ?: 0; it[Keys.SNOOZE_COUNT] = c + 1 } }
    suspend fun updateThemeAccent(v: ThemeAccent) { context.appDataStore.edit { it[Keys.THEME_ACCENT] = v.name } }
    suspend fun updateGridDensity(v: GridDensity) { context.appDataStore.edit { it[Keys.GRID_DENSITY] = v.name } }
    suspend fun updateEfficiencyMode(v: Boolean) { context.appDataStore.edit { it[Keys.EFFICIENCY_MODE] = v } }
    suspend fun updateBiometricEnabled(v: Boolean) { context.appDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = v } }
}
