package com.example.moviewather.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Supported application appearance themes.
 */
enum class AppTheme(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark"),
}

/**
 * Modern Android preferences repository backed by DataStore.
 *
 * Persists user configuration (e.g. Theme selection) independently of Room data,
 * ensuring preferences survive clearing movie collection data.
 */
class UserPreferencesRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme")
    private val tmdbApiKeyKey = stringPreferencesKey("tmdb_api_key")

    val themeFlow: Flow<AppTheme> = context.userDataStore.data.map { preferences ->
        val themeName = preferences[themeKey] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeName)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.userDataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    @Volatile
    private var inMemoryApiKey: String = com.example.moviewather.BuildConfig.TMDB_API_KEY.trim()

    init {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            context.userDataStore.data.collect { preferences ->
                val savedKey = preferences[tmdbApiKeyKey]?.trim().orEmpty()
                inMemoryApiKey = savedKey
            }
        }
    }

    val tmdbApiKeyFlow: Flow<String> = context.userDataStore.data.map { preferences ->
        val key = preferences[tmdbApiKeyKey]?.trim() ?: ""
        inMemoryApiKey = key
        key
    }

    suspend fun getEffectiveTmdbApiKey(): String {
        val userSavedKey = try {
            context.userDataStore.data.first()[tmdbApiKeyKey]?.trim()
        } catch (_: Exception) {
            null
        }
        return if (!userSavedKey.isNullOrBlank()) {
            userSavedKey
        } else {
            com.example.moviewather.BuildConfig.TMDB_API_KEY.trim()
        }
    }

    fun getEffectiveTmdbApiKeySync(): String {
        return inMemoryApiKey.ifBlank { com.example.moviewather.BuildConfig.TMDB_API_KEY.trim() }
    }

    suspend fun setTmdbApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        inMemoryApiKey = trimmed
        context.userDataStore.edit { preferences ->
            preferences[tmdbApiKeyKey] = trimmed
        }
    }
}
