package com.example.moviewather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.moviewather.data.preference.AppTheme
import com.example.moviewather.data.preference.UserPreferencesRepository
import com.example.moviewather.ui.MovieWatcherApp
import com.example.moviewather.ui.theme.MovieWatcherTheme

/**
 * Entry point for the application.
 *
 * Responsibilities:
 * - Enable edge-to-edge rendering
 * - Observe user appearance theme preferences (System, Light, Dark)
 * - Apply [MovieWatcherTheme]
 * - Launch [MovieWatcherApp]
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesRepository = UserPreferencesRepository(this)

        setContent {
            val themePreference by preferencesRepository.themeFlow.collectAsState(initial = AppTheme.SYSTEM)
            val isDark = when (themePreference) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            MovieWatcherTheme(darkTheme = isDark) {
                MovieWatcherApp(preferencesRepository = preferencesRepository)
            }
        }
    }
}