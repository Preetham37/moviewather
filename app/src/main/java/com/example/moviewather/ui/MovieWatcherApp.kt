package com.example.moviewather.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moviewather.data.MovieRepository
import com.example.moviewather.data.local.AppDatabase
import com.example.moviewather.data.metadata.CompositeMovieMetadataProvider
import com.example.moviewather.data.metadata.NetworkChecker
import com.example.moviewather.data.metadata.TmdbMetadataProvider
import com.example.moviewather.data.preference.UserPreferencesRepository
import com.example.moviewather.navigation.BottomNavDestination
import com.example.moviewather.navigation.MovieWatcherNavHost
import com.example.moviewather.navigation.Screen

/**
 * Root UI scaffold for the application.
 *
 * Owns:
 * - The [NavController] and bottom navigation bar.
 * - Construction of the [AppDatabase] singleton and [MovieRepository].
 *
 * The database/repository are created once and passed down to the
 * [MovieWatcherNavHost] which creates the single [MovieViewModel] from them.
 * No composable below this level touches Room directly.
 */
@Composable
fun MovieWatcherApp(
    preferencesRepository: UserPreferencesRepository? = null,
) {
    val context = LocalContext.current
    val prefsRepo = preferencesRepository ?: remember { UserPreferencesRepository(context) }

    // Build the database singleton and repository once per composition tree.
    // `remember` ensures we don't re-create them on every recomposition.
    val database = remember { AppDatabase.getInstance(context) }
    val networkChecker = remember { NetworkChecker(context) }
    val tmdbProvider = remember {
        TmdbMetadataProvider(apiKeyProvider = { prefsRepo.getEffectiveTmdbApiKey() })
    }
    val metadataProvider = remember {
        CompositeMovieMetadataProvider(
            networkChecker = networkChecker,
            tmdbProvider = tmdbProvider,
            tmdbApiKeyProvider = { prefsRepo.getEffectiveTmdbApiKey() },
        )
    }
    val repository = remember {
        MovieRepository(
            database = database,
            movieDao = database.movieDao(),
            categoryDao = database.categoryDao(),
            metadataProvider = metadataProvider,
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Routes that are part of the bottom navigation bar
    val bottomNavRoutes = BottomNavDestination.entries.map { it.screen.route }.toSet()

    // Show the bottom bar only on main tabs, not on secondary screens
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    BottomNavDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.screen.route) {
                                    // Pop up to start destination to avoid building up a back stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        MovieWatcherNavHost(
            navController = navController,
            repository = repository,
            preferencesRepository = prefsRepo,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
